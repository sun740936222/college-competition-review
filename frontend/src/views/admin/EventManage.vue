<template>
  <div class="event-manage">
    <div class="page-header">
      <h2>赛事管理</h2>
      <el-button v-if="userStore.user?.orgLevel !== 'SUPER_ADMIN'" type="primary" @click="showCreateDialog">
        <el-icon><Plus /></el-icon>创建赛事
      </el-button>
    </div>

    <el-table :data="events" v-loading="loading" stripe row-key="id">
      <el-table-column prop="name" label="赛事名称" min-width="200" />
      <el-table-column label="上级赛事" width="160">
        <template #default="{ row }">
          <span v-if="row.parentEventName" style="color:#409eff">{{ row.parentEventName }}</span>
          <span v-else style="color:#909399">-</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="级别" width="80">
        <template #default="{ row }">{{ levelLabel(row.competitionLevel) }}</template>
      </el-table-column>
      <el-table-column label="报名截止时间" width="160">
        <template #default="{ row }">{{ formatDate(row.registrationDeadline) }}</template>
      </el-table-column>
      <el-table-column label="材料提交截止时间" width="160">
        <template #default="{ row }">{{ formatDate(row.submissionDeadline) }}</template>
      </el-table-column>
      <el-table-column label="赛道" min-width="200">
        <template #default="{ row }">
          <span v-if="row.tracks?.length">{{ row.tracks.map(t => t.groupName ? `${t.name}（${t.groupName}）` : t.name).join('、') }}</span>
          <span v-else style="color:#909399">未设置</span>
        </template>
      </el-table-column>
      <el-table-column v-if="userStore.user?.orgLevel !== 'SUPER_ADMIN'" label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="showEditDialog(row)">编辑</el-button>
          <el-button size="small" @click="showTrackDialog(row)">赛道</el-button>
          <el-popconfirm v-if="row.childEventCount > 0" :title="`确认删除该赛事下的 ${row.childEventCount} 个子赛事？此操作不可恢复！`" @confirm="handleDeleteChildren(row.id)">
            <template #reference>
              <el-button size="small" type="danger" plain>一键删除子赛事</el-button>
            </template>
          </el-popconfirm>
          <el-popconfirm title="确认清空该赛事下所有项目的PDF和视频材料？此操作不可恢复！" @confirm="handleClearMaterials(row.id)">
            <template #reference>
              <el-button size="small" type="warning">清空材料</el-button>
            </template>
          </el-popconfirm>
          <el-popconfirm title="确认删除该赛事？" @confirm="handleDelete(row.id)">
            <template #reference>
              <el-button size="small" type="danger">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <div style="display:flex;justify-content:center;margin-top:16px" v-if="total > 0">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="prev, pager, next, sizes, total"
        @current-change="loadEvents"
        @size-change="handleSizeChange"
      />
    </div>

    <!-- Create/Edit Dialog -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑赛事' : '创建赛事'" width="650px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" label-position="top" :rules="rules">
        <el-form-item label="赛事模板">
          <el-select v-model="selectedTemplateId" filterable clearable
            placeholder="选择赛事模板（可选，自动填充名称和赛道）" style="width: 100%"
            @change="onTemplateChange">
            <el-option v-for="tpl in competitionTemplates" :key="tpl.id" :label="tpl.name" :value="tpl.id">
              <span>{{ tpl.name }}</span>
              <span style="float:right;color:#909399;font-size:13px">{{ tpl.tracks?.length || 0 }}个赛道</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="赛事名称" prop="name">
          <el-input v-model="form.name" placeholder="输入赛事名称，如：中国国际大学生创新大赛(2026)北京大学选拔赛" />
        </el-form-item>
        <el-form-item label="赛事描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="赛事描述（可选）" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="报名截止时间">
              <el-date-picker v-model="form.registrationDeadline" type="datetime" placeholder="选择时间" style="width: 100%" format="YYYY-MM-DD HH:mm" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="材料提交截止时间">
              <el-date-picker v-model="form.submissionDeadline" type="datetime" placeholder="选择时间" style="width: 100%" format="YYYY-MM-DD HH:mm" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="评审开始时间">
              <el-date-picker v-model="form.reviewStartTime" type="datetime" placeholder="选择时间" style="width: 100%" format="YYYY-MM-DD HH:mm" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="评审结束时间">
              <el-date-picker v-model="form.reviewEndTime" type="datetime" placeholder="选择时间" style="width: 100%" format="YYYY-MM-DD HH:mm" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="赛事级别">
          <el-select v-model="form.competitionLevel" style="width: 100%">
            <el-option v-for="lvl in availableLevels" :key="lvl.value" :label="lvl.label" :value="lvl.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="父赛事（可选）">
          <el-select v-model="form.parentEventId" style="width: 100%" clearable placeholder="选择父赛事（留空为顶层赛事）">
            <el-option v-for="e in parentEventOptions" :key="e.id" :label="e.name" :value="e.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="childOrgOptions.length > 0" label="自动创建子赛事">
          <el-select v-model="form.childEventOrgIds" multiple filterable style="width: 100%"
            placeholder="选择下级单位，自动为其创建子赛事" collapse-tags collapse-tags-tooltip>
            <el-option v-for="o in childOrgOptions" :key="o.id" :label="o.name" :value="o.id" />
          </el-select>
          <div class="form-tip" v-if="form.childEventOrgIds?.length">将为 {{ form.childEventOrgIds.length }} 个单位自动创建子赛事</div>
          <div class="form-tip" v-else style="color:#909399">可选：选定下级单位后，系统自动创建子赛事并复制赛道</div>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="草稿" value="DRAFT" />
            <el-option label="开放报名" value="OPEN" />
            <el-option label="已结束" value="CLOSED" />
          </el-select>
        </el-form-item>
        <el-divider content-position="left">可选内容</el-divider>
        <el-form-item>
          <el-checkbox v-model="form.requireVideo">需要参赛者上传展示视频</el-checkbox>
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="form.allowResubmit">允许评委提交后修改评分</el-checkbox>
          <span style="color:#909399;font-size:12px;margin-left:8px">（注：评审截止后评委不能再修改分数）</span>
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="form.requireComment">提交评分时必须填写评语</el-checkbox>
        </el-form-item>

        <!-- Track selection (only when creating, shows when template matched) -->
        <el-form-item v-if="!isEdit && trackTemplateOptions.length > 0" label="赛道组别">
          <div style="width: 100%">
            <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:4px">
              <el-select v-model="selectedTemplateKeys" multiple filterable placeholder="选择赛道组别（可多选）" style="flex:1"
                collapse-tags collapse-tags-tooltip :max-collapse-tags="3">
                <el-option v-for="tpl in trackTemplateOptions" :key="tpl.key" :label="tpl.label" :value="tpl.key" />
              </el-select>
              <el-button size="small" text type="primary" style="margin-left:8px;white-space:nowrap" @click="toggleSelectAllTracks">
                {{ selectedTemplateKeys.length === trackTemplateOptions.length ? '取消全选' : '一键全选' }}
              </el-button>
            </div>
          </div>
          <div class="form-tip" v-if="selectedTemplateKeys.length > 0">已选择 {{ selectedTemplateKeys.length }} 个赛道组别</div>
          <div class="form-tip" v-else style="color: #909399">请选择赛道组别，创建后可在赛道管理中增删</div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">{{ isEdit ? '保存' : '创建' }}</el-button>
      </template>
    </el-dialog>

    <!-- Track Dialog -->
    <el-dialog v-model="trackDialogVisible" :title="'赛道管理 - ' + trackEventName" width="600px">
      <div class="track-list">
        <div v-for="(t, idx) in trackList" :key="idx" class="track-item">
          <el-input v-model="t.name" placeholder="赛道名称" style="width: 160px" />
          <el-input v-model="t.groupName" placeholder="组别（可选）" style="width: 140px" />
          <el-input v-model="t.description" placeholder="描述（可选）" style="width: 140px" />
          <el-button type="danger" size="small" text @click="trackList.splice(idx, 1)">
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
      </div>
      <el-button size="small" @click="trackList.push({ name: '', groupName: '', description: '' })">
        <el-icon><Plus /></el-icon>添加赛道
      </el-button>
      <template #footer>
        <el-button @click="trackDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingTracks" @click="saveTrackDialog">保存赛道</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../../stores/user'
import { listAllEvents, createEvent, updateEvent, deleteEvent, getTracks, saveTracks, getCompetitionTemplates, clearEventMaterials, listAllOrgs, deleteEventChildren } from '../../api/admin'

const userStore = useUserStore()

const levelOptions = [
  { value: 'COLLEGE', label: '院赛' },
  { value: 'SCHOOL', label: '校赛' },
  { value: 'PROVINCE', label: '省赛' },
  { value: 'NATIONAL', label: '国赛' }
]

const availableLevels = computed(() => {
  const adminLevel = userStore.user?.orgLevel || 'COLLEGE'
  const idx = levelOptions.findIndex(l => l.value === adminLevel)
  return levelOptions.filter((_, i) => i >= idx)
})

const loading = ref(false)
const events = ref([])
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const formRef = ref(null)
const saving = ref(false)

const trackDialogVisible = ref(false)
const trackEventId = ref(null)
const trackEventName = ref('')
const trackList = ref([])
const savingTracks = ref(false)

// Competition templates for create dialog
const competitionTemplates = ref([])
const selectedTemplateId = ref(null)
const trackTemplateOptions = ref([])
const selectedTemplateKeys = ref([])

const form = reactive({
  name: '',
  description: '',
  registrationDeadline: null,
  submissionDeadline: null,
  reviewStartTime: null,
  reviewEndTime: null,
  competitionLevel: 'SCHOOL',
  status: 'DRAFT',
  requireVideo: false,
  allowResubmit: false,
  requireComment: false,
  parentEventId: null,
  childEventOrgIds: []
})

const parentEventOptions = ref([])
const childOrgOptions = ref([])

const rules = {
  name: [{ required: true, message: '请输入赛事名称', trigger: 'blur' }]
}

onMounted(async () => {
  await loadCompetitionTemplates()
  await loadEvents()
})

async function loadCompetitionTemplates() {
  try {
    const res = await getCompetitionTemplates()
    competitionTemplates.value = res.data || []
  } catch {
    competitionTemplates.value = []
  }
}

function onTemplateChange(templateId) {
  selectedTemplateKeys.value = []
  trackTemplateOptions.value = []
  if (!templateId) return
  const template = competitionTemplates.value.find(t => t.id === templateId)
  if (template) {
    // Pre-fill name from template; user can edit freely afterward
    form.name = template.name
    if (template.tracks) {
      trackTemplateOptions.value = template.tracks.map(t => {
        const label = t.groupName ? `${t.name}（${t.groupName}）` : t.name
        return { key: label, label, name: t.name, groupName: t.groupName || null }
      })
    }
  }
}

function toggleSelectAllTracks() {
  if (selectedTemplateKeys.value.length === trackTemplateOptions.value.length) {
    selectedTemplateKeys.value = []
  } else {
    selectedTemplateKeys.value = trackTemplateOptions.value.map(t => t.key)
  }
}

async function loadParentEventOptions() {
  parentEventOptions.value = []
  try {
    const res = await listAllEvents({ current: 1, size: 1000 })
    const allEvents = res.data?.records || []
    const adminLevel = userStore.user?.orgLevel
    const adminIdx = levelOrder.indexOf(adminLevel)
    parentEventOptions.value = allEvents.filter(e => {
      if (e.id === editId.value) return false
      if (!adminLevel) return true
      const eventIdx = levelOrder.indexOf(e.competitionLevel)
      return eventIdx >= 0 && eventIdx >= adminIdx
    })
    // Ensure the currently selected parent is always in the options,
    // even if its org is outside the admin's visible scope
    const parentId = form.parentEventId
    if (parentId && !parentEventOptions.value.some(e => e.id === parentId)) {
      const parentName = form._parentEventName
      if (parentName) {
        parentEventOptions.value.unshift({ id: parentId, name: parentName })
      }
    }
  } catch {
    parentEventOptions.value = []
  }
}

const levelOrder = ['COLLEGE', 'SCHOOL', 'PROVINCE', 'NATIONAL']

async function loadChildOrgOptions() {
  const adminLevel = userStore.user?.orgLevel || 'SCHOOL'
  const idx = levelOrder.indexOf(adminLevel)
  if (idx <= 0) { childOrgOptions.value = []; return }
  const childLevel = levelOrder[idx - 1]
  try {
    const res = await listAllOrgs({ current: 1, size: 5000 })
    const allOrgs = res.data?.records || []
    childOrgOptions.value = allOrgs.filter(o => o.level === childLevel)
  } catch {
    childOrgOptions.value = []
  }
}

async function loadEvents() {
  loading.value = true
  try {
    const res = await listAllEvents({ current: currentPage.value, size: pageSize.value })
    const data = res.data
    events.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function handleSizeChange() {
  currentPage.value = 1
  loadEvents()
}

function showCreateDialog() {
  isEdit.value = false
  editId.value = null
  form.name = ''
  form.description = ''
  form.registrationDeadline = null
  form.submissionDeadline = null
  form.reviewStartTime = null
  form.reviewEndTime = null
  form.competitionLevel = availableLevels.value[0]?.value || 'SCHOOL'
  form.status = 'DRAFT'
  form.requireVideo = false
  form.allowResubmit = false
  form.requireComment = false
  form.parentEventId = null
  form.childEventOrgIds = []
  selectedTemplateId.value = null
  trackTemplateOptions.value = []
  selectedTemplateKeys.value = []
  loadParentEventOptions()
  loadChildOrgOptions()
  dialogVisible.value = true
}

function toDate(v) {
  return v ? new Date(v) : null
}

function showEditDialog(row) {
  isEdit.value = true
  editId.value = row.id
  form.name = row.name
  form.description = row.description || ''
  form.registrationDeadline = toDate(row.registrationDeadline)
  form.submissionDeadline = toDate(row.submissionDeadline)
  form.reviewStartTime = toDate(row.reviewStartTime)
  form.reviewEndTime = toDate(row.reviewEndTime)
  form.competitionLevel = row.competitionLevel || 'SCHOOL'
  form.status = row.status || 'DRAFT'
  form.requireVideo = row.requireVideo || false
  form.allowResubmit = row.allowResubmit || false
  form.requireComment = row.requireComment || false
  form.parentEventId = row.parentEventId || null
  form._parentEventName = row.parentEventName || null
  form.childEventOrgIds = []
  loadParentEventOptions()
  loadChildOrgOptions()
  dialogVisible.value = true
}

async function handleSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    if (isEdit.value) {
      const data = { ...form }
      if (form.childEventOrgIds?.length) {
        data.childEventOrgIds = form.childEventOrgIds
      }
      await updateEvent(editId.value, data)
      ElMessage.success('赛事已更新')
    } else {
      // Build tracks from selected templates
      const data = { ...form }
      if (selectedTemplateKeys.value.length > 0) {
        data.tracks = selectedTemplateKeys.value.map(key => {
          const tpl = trackTemplateOptions.value.find(t => t.key === key)
          return {
            name: tpl ? tpl.name : key,
            groupName: tpl ? tpl.groupName : null,
            description: null,
            sortOrder: null
          }
        })
      }
      await createEvent(data)
      ElMessage.success('赛事已创建')
    }

    dialogVisible.value = false
    await loadEvents()
  } catch {
    // error already shown by axios interceptor
  } finally {
    saving.value = false
  }
}

async function handleClearMaterials(id) {
  try {
    await clearEventMaterials(id)
    ElMessage.success('材料已清空')
  } catch {
    ElMessage.error('清空材料失败')
  }
}

async function handleDeleteChildren(id) {
  try {
    await deleteEventChildren(id)
    ElMessage.success('子赛事已全部删除')
    await loadEvents()
  } catch {
    // error already shown by axios interceptor
  }
}

async function handleDelete(id) {
  try {
    await deleteEvent(id)
    ElMessage.success('赛事已删除')
    await loadEvents()
  } catch {
    // error already shown by axios interceptor
  }
}

function showTrackDialog(row) {
  trackEventId.value = row.id
  trackEventName.value = row.name
  trackList.value = (row.tracks || []).map(t => ({
    id: t.id,
    name: t.name,
    groupName: t.groupName || '',
    description: t.description || ''
  }))
  trackDialogVisible.value = true
}

async function saveTrackDialog() {
  savingTracks.value = true
  try {
    const tracks = trackList.value
      .filter(t => t.name.trim())
      .map((t, i) => ({ name: t.name, groupName: t.groupName || null, description: t.description || null, sortOrder: i + 1 }))
    await saveTracks(trackEventId.value, tracks)
    ElMessage.success('赛道已保存')
    trackDialogVisible.value = false
    await loadEvents()
  } catch {
    // error already shown by axios interceptor
  } finally {
    savingTracks.value = false
  }
}

function statusTag(status) {
  const map = { DRAFT: 'info', OPEN: 'success', CLOSED: 'danger' }
  return map[status] || 'info'
}

function statusLabel(status) {
  const map = { DRAFT: '草稿', OPEN: '开放中', CLOSED: '已结束' }
  return map[status] || status
}

function formatDate(d) {
  return d ? new Date(d).toLocaleString('zh-CN') : '-'
}

function levelLabel(lvl) {
  const map = { COLLEGE: '院赛', SCHOOL: '校赛', PROVINCE: '省赛', NATIONAL: '国赛' }
  return map[lvl] || lvl || '-'
}
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.page-header h2 {
  font-size: 20px;
  color: #303133;
}
.track-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;
}
.track-item {
  display: flex;
  align-items: center;
  gap: 8px;
}
.form-tip {
  font-size: 13px;
  color: #67c23a;
  margin-top: 4px;
}
</style>
