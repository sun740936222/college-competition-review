<template>
  <div class="criteria-manage">
    <div class="page-header">
      <h2>评分标准配置</h2>
    </div>

    <el-card class="section">
      <template #header>
        <span class="card-title">选择赛事与赛道</span>
        <div v-if="filterEvent" style="display:flex;align-items:center;gap:8px;margin-top:8px">
          <el-upload
            :show-file-list="false"
            :before-upload="handleAttachmentUpload"
            accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.zip"
          >
            <el-button size="small" :loading="attachmentUploading">
              <el-icon><Upload /></el-icon>{{ eventAttachmentName ? ' 重传附件' : ' 上传评分附件' }}
            </el-button>
          </el-upload>
          <template v-if="eventAttachmentName">
            <a :href="getEventAttachmentUrl(filterEvent)" target="_blank" class="att-link">{{ eventAttachmentName }}</a>
            <el-button size="small" text type="danger" @click="handleDeleteAttachment">
              <el-icon><Delete /></el-icon>
            </el-button>
          </template>
        </div>
      </template>
      <div class="selector-row">
        <el-select v-model="filterEvent" placeholder="选择赛事" clearable style="width: 300px" @change="onEventChange">
          <el-option v-for="e in events" :key="e.id" :label="e.name" :value="e.id" />
        </el-select>
        <el-select v-model="filterTrack" placeholder="选择赛道" clearable style="width: 300px" @change="onTrackChange">
          <el-option v-for="t in tracks" :key="t.id" :label="trackLabel(t)" :value="t.id" />
        </el-select>
      </div>
    </el-card>

    <el-card v-if="filterTrack" class="section">
      <template #header>
        <span class="card-title">评分维度</span>
        <el-button size="small" type="primary" @click="addDimension">
          <el-icon><Plus /></el-icon>添加维度
        </el-button>
      </template>

      <div v-if="dimensions.length === 0" class="empty-tip">
        暂无评分维度，请点击"添加维度"开始配置
      </div>

      <div v-for="(dim, idx) in dimensions" :key="idx" class="dimension-row">
        <el-input v-model="dim.name" placeholder="维度名称（如：创新性）" style="width: 200px" />
        <el-input v-model="dim.description" placeholder="维度说明（可选）" style="width: 240px" />
        <el-input-number v-model="dim.maxScore" :min="1" :max="1000" :step="5" style="width: 140px" />
        <el-tag>分</el-tag>
        <el-button type="danger" size="small" text @click="dimensions.splice(idx, 1)">
          <el-icon><Delete /></el-icon>
        </el-button>
      </div>

      <div v-if="dimensions.length > 0" class="save-bar">
        <el-button type="primary" :loading="saving" @click="handleSave">保存评分标准</el-button>
        <el-button @click="loadCurrentCriteria">重置</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listAllEvents, getTracks, getTrackCriteria, saveTrackCriteria, uploadEventAttachment, deleteEventAttachment, getEventAttachmentUrl } from '../../api/admin'

const events = ref([])
const tracks = ref([])
const filterEvent = ref(null)
const filterTrack = ref(null)
const dimensions = ref([])
const saving = ref(false)
const eventAttachmentName = ref('')
const attachmentUploading = ref(false)

onMounted(async () => {
  try {
    const res = await listAllEvents({ current: 1, size: 1000 })
    events.value = res.data?.records || []
  } catch {}
})

function trackLabel(t) {
  return t.groupName ? `${t.name}（${t.groupName}）` : t.name
}

async function onEventChange(val) {
  filterTrack.value = null
  tracks.value = []
  dimensions.value = []
  eventAttachmentName.value = ''
  if (!val) return
  try {
    const res = await getTracks(val)
    tracks.value = res.data || []
  } catch {}
  // Load event attachment name from events list
  const evt = events.value.find(e => e.id === val)
  eventAttachmentName.value = evt?.attachmentName || ''
}

async function onTrackChange(val) {
  if (!val) { dimensions.value = []; return }
  await loadCurrentCriteria()
}

async function loadCurrentCriteria() {
  try {
    const res = await getTrackCriteria(filterTrack.value)
    dimensions.value = (res.data || []).map(d => ({
      id: d.id,
      name: d.name,
      description: d.description || '',
      maxScore: d.maxScore || 100,
      sortOrder: d.sortOrder || 0
    }))
  } catch {
    dimensions.value = []
  }
}

function addDimension() {
  dimensions.value.push({ name: '', description: '', maxScore: 100, sortOrder: dimensions.value.length })
}

async function handleAttachmentUpload(file) {
  if (!filterEvent.value) return false
  attachmentUploading.value = true
  try {
    await uploadEventAttachment(filterEvent.value, file)
    eventAttachmentName.value = file.name
    ElMessage.success('附件已上传')
  } catch {
    // error shown by interceptor
  } finally {
    attachmentUploading.value = false
  }
  return false // prevent default upload behavior
}

async function handleDeleteAttachment() {
  if (!filterEvent.value) return
  try {
    await deleteEventAttachment(filterEvent.value)
    eventAttachmentName.value = ''
    ElMessage.success('附件已删除')
  } catch {
    ElMessage.error('删除附件失败')
  }
}

async function handleSave() {
  const valid = dimensions.value.every(d => d.name.trim())
  if (!valid) {
    ElMessage.warning('请填写所有维度的名称')
    return
  }

  saving.value = true
  try {
    const data = dimensions.value.map((d, i) => ({
      name: d.name.trim(),
      description: d.description.trim() || null,
      maxScore: d.maxScore,
      sortOrder: i
    }))
    await saveTrackCriteria(filterTrack.value, data)
    ElMessage.success('评分标准已保存')
    await loadCurrentCriteria()
  } catch {} finally {
    saving.value = false
  }
}
</script>

<style scoped>
.page-header { margin-bottom: 16px; }
.page-header h2 { font-size: 20px; color: #303133; }
.section { margin-bottom: 16px; }
.card-title { font-weight: 600; }
.selector-row { display: flex; gap: 12px; flex-wrap: wrap; }
.empty-tip { color: #909399; padding: 20px 0; text-align: center; }
.dimension-row {
  display: flex; align-items: center; gap: 10px; margin-bottom: 10px;
  padding: 10px; border: 1px solid #ebeef5; border-radius: 4px;
}
.save-bar { margin-top: 16px; display: flex; gap: 12px; }
.att-link { font-size: 12px; color: #409eff; text-decoration: none; max-width: 120px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.att-link:hover { text-decoration: underline; }
</style>
