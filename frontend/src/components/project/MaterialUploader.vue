<template>
  <div class="material-section">
    <div class="section-header">
      <span>材料上传</span>
    </div>

    <div v-for="item in materialItems" :key="item.type" class="material-item">
      <div class="material-label">
        <span>{{ item.label }}</span>
        <el-tag v-if="item.required" type="danger" size="small">必传</el-tag>
        <el-tag v-else type="info" size="small">选传</el-tag>
        <span class="size-hint">（最大 {{ item.maxSize }}MB，仅支持 {{ item.accept }}）</span>
      </div>

      <div v-if="item.file" class="file-display">
        <div class="file-info">
          <el-icon><Document /></el-icon>
          <span>{{ item.file.originalFilename || item.file.name }}</span>
          <span class="file-size">({{ formatSize(item.file.fileSize || item.file.size) }})</span>
        </div>
        <div class="file-actions">
          <el-button size="small" @click="previewFile(item)">预览</el-button>
          <el-button size="small" type="danger" @click="removeFile(item)">删除</el-button>
        </div>
      </div>

      <el-upload
        v-else
        class="upload-area"
        drag
        :action="''"
        :auto-upload="false"
        :accept="item.accept"
        :on-change="(file) => onFileChange(item, file)"
        :show-file-list="false"
      >
        <el-icon class="upload-icon"><UploadFilled /></el-icon>
        <div class="upload-text">点击或拖拽文件上传</div>
        <div class="upload-hint">{{ item.accept }} 文件，不超过 {{ item.maxSize }}MB</div>
      </el-upload>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, watchEffect } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  requireVideo: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue'])

const files = ref([...props.modelValue])

// Propagate internal changes to parent — watchEffect tracks every files.value access
watchEffect(() => {
  const snapshot = files.value.map(f => ({ ...f }))
  emit('update:modelValue', snapshot)
})

// Sync from props when data is loaded asynchronously (e.g. ProjectEdit)
watch(() => props.modelValue, (v) => {
  // Compare keys to avoid emit loop
  const key = a => a.map(f => f.materialType + f.originalFilename).join('|')
  if (key(v) !== key(files.value)) {
    files.value = v.map(f => ({ ...f }))
  }
}, { deep: true, immediate: false })

const materialItems = computed(() => {
  const items = [
    { type: 'PITCH_PDF', label: '路演材料 PDF', required: true, maxSize: 50, accept: '.pdf' },
    { type: 'PROJECT_PLAN_PDF', label: '项目计划书 PDF', required: true, maxSize: 50, accept: '.pdf' },
    { type: 'SUPPORTING_PDF', label: '佐证材料 PDF', required: false, maxSize: 100, accept: '.pdf' }
  ]
  if (props.requireVideo) {
    items.push({ type: 'VIDEO_MP4', label: '项目展示视频', required: true, maxSize: 200, accept: '.mp4' })
  } else {
    items.push({ type: 'VIDEO_MP4', label: '项目展示视频', required: false, maxSize: 200, accept: '.mp4' })
  }
  return items.map(item => ({
    ...item,
    file: files.value.find(f => f.materialType === item.type) || null
  }))
})

function onFileChange(item, uploadFile) {
  const raw = uploadFile.raw
  if (!raw) return

  const ext = raw.name.substring(raw.name.lastIndexOf('.')).toLowerCase()
  if (item.accept === '.pdf' && ext !== '.pdf') {
    ElMessage.error('仅支持 PDF 格式')
    return
  }
  if (item.accept === '.mp4' && ext !== '.mp4') {
    ElMessage.error('仅支持 MP4 格式')
    return
  }
  if (raw.size > item.maxSize * 1024 * 1024) {
    ElMessage.error(`文件大小不能超过 ${item.maxSize}MB`)
    return
  }

  const existing = files.value.findIndex(f => f.materialType === item.type)
  const fileObj = { materialType: item.type, originalFilename: raw.name, fileSize: raw.size, file: raw }
  if (existing >= 0) {
    files.value[existing] = fileObj
  } else {
    files.value.push(fileObj)
  }
}

function removeFile(item) {
  const idx = files.value.findIndex(f => f.materialType === item.type)
  if (idx >= 0) files.value.splice(idx, 1)
}

function previewFile(item) {
  const f = item.file
  if (f.file) {
    const url = URL.createObjectURL(f.file)
    window.open(url, '_blank')
  } else if (f.storedFilename) {
    window.open(`/api/files/${f.storedFilename}`, '_blank')
  }
}

function formatSize(bytes) {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let size = bytes, unitIdx = 0
  while (size >= 1024 && unitIdx < units.length - 1) { size /= 1024; unitIdx++ }
  return size.toFixed(1) + ' ' + units[unitIdx]
}

defineExpose({ files })
</script>

<style scoped>
.section-header {
  margin-bottom: 16px;
  font-weight: 600;
}
.material-item {
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #ebeef5;
}
.material-label {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 14px;
}
.size-hint {
  font-size: 12px;
  color: #909399;
}
.file-display {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #f5f7fa;
  padding: 12px;
  border-radius: 6px;
}
.file-info {
  display: flex;
  align-items: center;
  gap: 8px;
}
.file-size {
  font-size: 12px;
  color: #909399;
}
.file-actions {
  display: flex;
  gap: 8px;
}
.upload-area {
  width: 100%;
}
.upload-icon {
  font-size: 48px;
  color: #c0c4cc;
}
.upload-text {
  color: #606266;
  margin-top: 8px;
}
.upload-hint {
  font-size: 12px;
  color: #c0c4cc;
  margin-top: 4px;
}
</style>
