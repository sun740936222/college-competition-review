<template>
  <div class="pdf-container">
    <div class="pdf-toolbar" v-if="totalPages > 0">
      <el-button size="small" :disabled="currentPage <= 1" @click="goToPage(currentPage - 1)">
        上一页
      </el-button>
      <span class="page-info">{{ currentPage }} / {{ totalPages }}</span>
      <el-button size="small" :disabled="currentPage >= totalPages" @click="goToPage(currentPage + 1)">
        下一页
      </el-button>
      <span class="zoom-info">{{ Math.round(scale * 100) }}%</span>
      <el-button size="small" @click="zoomOut">-</el-button>
      <el-button size="small" @click="zoomIn">+</el-button>
    </div>
    <div v-if="loading" class="pdf-loading">
      <el-icon class="is-loading" style="font-size:32px"><Loading /></el-icon>
      <span>加载文档中...</span>
    </div>
    <div v-if="error" class="pdf-error">
      <span>{{ error }}</span>
    </div>
    <div class="pdf-canvas-wrapper" @contextmenu.prevent>
      <canvas ref="canvasRef"></canvas>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { getDocument, GlobalWorkerOptions } from 'pdfjs-dist'
import { fetchJudgePdf } from '../../api/judge'

// Use local worker from npm package instead of CDN (jsdelivr blocked in China)
GlobalWorkerOptions.workerSrc = new URL('pdfjs-dist/build/pdf.worker.min.mjs', import.meta.url).href

const props = defineProps({
  materialId: { type: [Number, String], required: true }
})

const emit = defineEmits(['load-error'])

const canvasRef = ref(null)
const loading = ref(true)
const error = ref('')
const currentPage = ref(1)
const totalPages = ref(0)
const scale = ref(1.5)

let pdfDoc = null
const MAX_CANVAS = 1600

onMounted(() => loadPdf())

watch(() => props.materialId, () => {
  currentPage.value = 1
  totalPages.value = 0
  pdfDoc = null
  loadPdf()
})

onBeforeUnmount(() => {
  pdfDoc = null
})

async function loadPdf() {
  if (!props.materialId) return
  loading.value = true
  error.value = ''
  try {
    const response = await fetchJudgePdf(props.materialId)
    pdfDoc = await getDocument({ data: response }).promise
    totalPages.value = pdfDoc.numPages
    await nextTick()
    await renderPage(currentPage.value)
  } catch (e) {
    console.error('PDF loading error:', e)
    if (e instanceof Error && e.name === 'UnexpectedResponseException') {
      error.value = '文档加载失败：服务器响应异常'
    } else if (e instanceof Error && e.message?.includes('Invalid PDF')) {
      error.value = '文档加载失败：PDF文件格式无效'
    } else {
      error.value = '文档加载失败'
    }
    emit('load-error', e)
  } finally {
    loading.value = false
  }
}

async function renderPage(pageNum) {
  if (!pdfDoc || !canvasRef.value) return
  const page = await pdfDoc.getPage(pageNum)
  const viewport = page.getViewport({ scale: scale.value })

  const canvas = canvasRef.value
  const ctx = canvas.getContext('2d')

  // Clamp canvas size to avoid memory issues
  let { width, height } = viewport
  if (width > MAX_CANVAS) {
    const ratio = MAX_CANVAS / width
    width = MAX_CANVAS
    height = height * ratio
  }
  if (height > MAX_CANVAS * 1.5) {
    const ratio = (MAX_CANVAS * 1.5) / height
    height = MAX_CANVAS * 1.5
    width = width * ratio
  }

  canvas.width = width
  canvas.height = height

  const renderContext = {
    canvasContext: ctx,
    viewport: viewport,
    transform: [width / viewport.width, 0, 0, height / viewport.height, 0, 0]
  }

  const renderTask = page.render(renderContext)
  await renderTask.promise
}

async function goToPage(pageNum) {
  if (pageNum < 1 || pageNum > totalPages.value) return
  currentPage.value = pageNum
  await nextTick()
  await renderPage(pageNum)
}

function zoomIn() {
  scale.value = Math.min(scale.value + 0.25, 3)
  renderPage(currentPage.value)
}

function zoomOut() {
  scale.value = Math.max(scale.value - 0.25, 0.5)
  renderPage(currentPage.value)
}
</script>

<style scoped>
.pdf-container {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.pdf-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
  flex-shrink: 0;
}

.page-info {
  font-size: 13px;
  color: #606266;
  min-width: 60px;
  text-align: center;
}

.zoom-info {
  font-size: 12px;
  color: #909399;
  margin-left: auto;
  min-width: 40px;
  text-align: right;
}

.pdf-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 48px;
  color: #909399;
}

.pdf-error {
  text-align: center;
  padding: 48px;
  color: #f56c6c;
}

.pdf-canvas-wrapper {
  flex: 1;
  overflow: auto;
  display: flex;
  justify-content: center;
  background: #e4e7ed;
  padding: 16px;
}

.pdf-canvas-wrapper canvas {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  background: #fff;
}
</style>
