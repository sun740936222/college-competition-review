<template>
  <div class="attachment-panel">
    <div v-if="!readonly" class="upload-row">
      <el-upload
        :show-file-list="false"
        :before-upload="handleUpload"
        :disabled="uploading"
      >
        <el-button size="small" :loading="uploading">上传附件</el-button>
      </el-upload>
      <span class="hint">支持 PDF、Word、Excel、ZIP 等任意格式，单个文件最大 200MB</span>
    </div>
    <el-table :data="files" size="small" border v-if="files.length">
      <el-table-column prop="originalFilename" label="文件名" min-width="200" />
      <el-table-column label="大小" width="110">
        <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button size="small" text type="primary" @click="download(row)">下载</el-button>
          <el-button v-if="!readonly" size="small" text type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-else description="暂无附件" :image-size="60" />
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { listAttachments, uploadAttachment, deleteAttachment, downloadAttachment } from '../../api/training'

const props = defineProps({
  stage: { type: String, required: true },     // topic | project | midterm | conclusion
  targetId: { type: [Number, String], default: null },
  readonly: { type: Boolean, default: false }
})

const files = ref([])
const uploading = ref(false)

watch(() => props.targetId, load, { immediate: true })

async function load() {
  if (!props.targetId) { files.value = []; return }
  try {
    const res = await listAttachments(props.stage, props.targetId)
    files.value = res.data || []
  } catch { files.value = [] }
}

async function handleUpload(file) {
  if (!props.targetId) { ElMessage.warning('请先保存记录后再上传附件'); return false }
  uploading.value = true
  try {
    await uploadAttachment(props.stage, props.targetId, file)
    ElMessage.success('上传成功')
    load()
  } finally { uploading.value = false }
  return false
}

async function remove(row) {
  await deleteAttachment(row.id)
  ElMessage.success('已删除')
  load()
}

function download(row) {
  downloadAttachment(row.id, row.originalFilename)
}

function formatSize(n) {
  if (!n) return '—'
  if (n < 1024) return n + ' B'
  if (n < 1024 * 1024) return (n / 1024).toFixed(1) + ' KB'
  return (n / 1024 / 1024).toFixed(1) + ' MB'
}
</script>

<style scoped>
.upload-row { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; }
.hint { color: #909399; font-size: 12px; }
</style>
