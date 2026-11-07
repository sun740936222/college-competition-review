<template>
  <div class="judge-review">
    <div class="page-header">
      <el-button text @click="$router.back()">
        <el-icon><ArrowLeft /></el-icon>返回任务列表
      </el-button>
      <h2>{{ project?.name }}</h2>
    </div>

    <div v-loading="loading">
      <!-- Basic Info -->
      <el-card class="section">
        <template #header><span class="section-title">项目信息</span></template>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="项目名称">{{ project?.name }}</el-descriptions-item>
          <el-descriptions-item label="所属赛事">{{ project?.eventName }}</el-descriptions-item>
          <el-descriptions-item label="赛道/组别">{{ project?.trackName }}</el-descriptions-item>

          <el-descriptions-item label="项目简介" :span="2">{{ project?.description || '-' }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- PDF Materials with Watermark -->
      <el-card v-if="pdfMaterials.length > 0" class="section">
        <template #header><span class="section-title">项目材料</span></template>

        <el-tabs v-model="activePdfTab" type="card" lazy>
          <el-tab-pane
            v-for="(mat, idx) in pdfMaterials"
            :key="mat.id"
            :label="materialLabel(mat.materialType)"
            :name="String(mat.id)"
          >
            <div class="pdf-viewer-wrapper" @contextmenu.prevent>
              <div class="watermark-layer">
                <span
                  v-for="w in watermarkSpans"
                  :key="w.i"
                  class="watermark-text"
                  :style="{
                    left: w.left,
                    top: w.top,
                    transform: 'rotate(-25deg)',
                    opacity: 0.10
                  }"
                >{{ watermarkText }}</span>
              </div>
              <iframe :src="getJudgeFileUrl(mat.id)" class="pdf-iframe" />
            </div>
            <div class="material-info">文件名：{{ mat.originalFilename }}（{{ formatSize(mat.fileSize) }}）</div>
          </el-tab-pane>
        </el-tabs>
      </el-card>

      <!-- Video Player -->
      <el-card v-if="videoMaterial" class="section">
        <template #header><span class="section-title">项目视频</span></template>
        <div class="video-wrapper">
          <video
            :src="getJudgeFileUrl(videoMaterial.id)"
            controls
            controlsList="nodownload"
            style="width:100%;max-height:500px;background:#000"
            @contextmenu.prevent
          >
            您的浏览器不支持视频播放
          </video>
        </div>
        <div class="material-info">文件名：{{ videoMaterial.originalFilename }}（{{ formatSize(videoMaterial.fileSize) }}）</div>
      </el-card>

      <!-- Scoring Form -->
      <el-card v-if="criteria.length > 0" class="section">
        <template #header>
          <div style="display:flex;align-items:center;gap:12px;width:100%">
            <span class="section-title">评分</span>
            <el-tag v-if="project?.allocationStatus === 'COMPLETED'" :type="isLocked ? 'success' : 'warning'" style="margin-left:0">
              {{ isLocked ? '已提交' : '已提交（可修改）' }}
            </el-tag>
            <a v-if="project?.eventAttachmentName" :href="getJudgeEventAttachmentUrl(project.eventId)" target="_blank" class="criteria-att-link">
              <el-icon><Link /></el-icon>{{ project.eventAttachmentName }}
            </a>
          </div>
        </template>

        <el-alert v-if="reviewTimeStatus === 'pending'" title="评审尚未开始" type="info" show-icon :closable="false" style="margin-bottom: 12px">
          评审开始时间：{{ formatTime(project?.reviewStartTime) }}，请等待评审开始后提交评分
        </el-alert>
        <el-alert v-if="reviewTimeStatus === 'ended'" title="评审已结束" type="warning" show-icon :closable="false" style="margin-bottom: 12px">
          评审结束时间：{{ formatTime(project?.reviewEndTime) }}，已无法提交评分
        </el-alert>

        <el-form ref="scoreFormRef" :model="scoreForm" label-position="top">
          <div v-for="(c, idx) in criteria" :key="c.id" class="criteria-item">
            <div class="criteria-header">
              <span class="criteria-name">{{ idx + 1 }}. {{ c.name }}</span>
              <span class="criteria-max">（满分 {{ c.maxScore }} 分）</span>
            </div>
            <div v-if="c.description" class="criteria-desc">{{ c.description }}</div>
            <el-input-number
              v-model="scoreForm.scores[c.id]"
              :min="0"
              :max="c.maxScore"
              :step="0.5"
              :precision="1"
              :disabled="isLocked"
              style="width:200px"
              placeholder="请打分"
            />
          </div>

          <el-form-item label="评语">
            <el-input
              v-model="scoreForm.overallComment"
              type="textarea"
              :rows="4"
              :disabled="isLocked"
              :placeholder="project?.requireComment ? '请填写评审评语（必填）' : '请填写评审评语（选填）'"
            />
          </el-form-item>

          <div v-if="!isLocked" class="score-actions">
            <el-button @click="handleSave" :loading="saving" :disabled="isOutsideReviewWindow">暂存</el-button>
            <el-button type="primary" @click="handleSubmit" :loading="submitting" :disabled="isOutsideReviewWindow">提交评分</el-button>
          </div>
          <div v-else class="completed-tip">
            <el-alert title="评分已提交" type="success" :closable="false" show-icon>
              <template #default>
                您已于 {{ formatTime(project?.submittedAt) }} 提交评分{{ project?.allowResubmit ? '，如有需要可联系管理员开放修改权限' : '，不可修改' }}。
              </template>
            </el-alert>
          </div>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getJudgeProjectDetail, submitJudgeScores, getJudgeFileUrl, getJudgeEventAttachmentUrl } from '../../api/judge'


const route = useRoute()
const router = useRouter()

const loading = ref(false)
const saving = ref(false)
const submitting = ref(false)
const project = ref(null)
const criteria = ref([])
const activePdfTab = ref('')
const scoreFormRef = ref(null)

const scoreForm = reactive({
  scores: {},
  overallComment: ''
})

const isLocked = computed(() => {
  if (project.value?.allocationStatus !== 'COMPLETED') return false
  return !project.value?.allowResubmit
})

const reviewTimeStatus = computed(() => {
  const start = project.value?.reviewStartTime
  const end = project.value?.reviewEndTime
  if (!start && !end) return 'unset'
  const now = new Date()
  if (start && now < new Date(start)) return 'pending'
  if (end && now > new Date(end)) return 'ended'
  return 'active'
})

const isOutsideReviewWindow = computed(() => {
  return reviewTimeStatus.value === 'pending' || reviewTimeStatus.value === 'ended'
})

const pdfMaterials = computed(() => {
  if (!project.value?.materials) return []
  return project.value.materials.filter(m => m.materialType !== 'VIDEO_MP4')
})

const videoMaterial = computed(() => {
  if (!project.value?.materials) return null
  return project.value.materials.find(m => m.materialType === 'VIDEO_MP4') || null
})

const watermarkText = computed(() => {
  const p = project.value
  const name = p?.judgeName || '评委'
  const phone = p?.judgePhone || ''
  return `${name}  ${phone}`.trim()
})

// Sparse grid pattern avoiding overlap even with long watermark text
const watermarkSpans = computed(() => {
  const spans = []
  const cols = [12, 37, 62, 87]
  const rows = [10, 35, 60, 85]
  for (let col = 0; col < cols.length; col++) {
    for (let row = 0; row < rows.length; row++) {
      // stagger alternating rows
      const left = (cols[col] + (row % 2 === 0 ? 0 : 6)) + '%'
      const top = rows[row] + '%'
      spans.push({ i: col * rows.length + row, left, top })
    }
  }
  return spans
})

onMounted(() => loadDetail())

async function loadDetail() {
  loading.value = true
  try {
    const res = await getJudgeProjectDetail(route.params.id)
    const data = res.data
    project.value = data
    criteria.value = data.criteria || []

    // Initialize score form with existing values
    scoreForm.scores = {}
    if (data.scores) {
      data.scores.forEach(s => { scoreForm.scores[s.criteriaId] = s.score })
    }
    scoreForm.overallComment = data.overallComment || ''

    // Set default PDF tab
    if (pdfMaterials.value.length > 0) {
      activePdfTab.value = String(pdfMaterials.value[0].id)
    }
  } finally {
    loading.value = false
  }
}

function formatTime(t) {
  if (!t) return '-'
  const d = new Date(t)
  return d.toLocaleDateString('zh-CN') + ' ' + d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

function formatSize(bytes) {
  if (!bytes) return '0 B'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

function materialLabel(type) {
  const map = {
    PITCH_PDF: '路演材料',
    PROJECT_PLAN_PDF: '项目计划书',
    SUPPORTING_PDF: '佐证材料',
    VIDEO_MP4: '视频'
  }
  return map[type] || type
}

function buildSubmitData(action) {
  return {
    action,
    overallComment: scoreForm.overallComment || null,
    scores: criteria.value.map(c => ({
      criteriaId: c.id,
      score: scoreForm.scores[c.id] ?? 0
    }))
  }
}

async function handleSave() {
  const valid = validateScores()
  if (!valid) return

  saving.value = true
  try {
    await submitJudgeScores(route.params.id, buildSubmitData('SAVE'))
    ElMessage.success('已暂存')
  } finally {
    saving.value = false
  }
}

async function handleSubmit() {
  const valid = validateScores()
  if (!valid) return

  if (project.value?.requireComment && (!scoreForm.overallComment || !scoreForm.overallComment.trim())) {
    ElMessage.warning('当前赛事要求提交时必须填写评语')
    return
  }

  const resubmitMsg = project.value?.allowResubmit ? '提交后仍可修改' : '提交后不可修改'
  await ElMessageBox.confirm(`${resubmitMsg}，确认提交评分？`, '确认提交', {
    confirmButtonText: '确认提交',
    cancelButtonText: '再检查一下',
    type: 'warning'
  })

  submitting.value = true
  try {
    await submitJudgeScores(route.params.id, buildSubmitData('SUBMIT'))
    ElMessage.success('评分已提交')
    await loadDetail()
  } finally {
    submitting.value = false
  }
}

function validateScores() {
  for (const c of criteria.value) {
    const val = scoreForm.scores[c.id]
    if (val == null || val === undefined) {
      ElMessage.warning(`请填写"${c.name}"维度的分值`)
      return false
    }
    if (val < 0 || val > c.maxScore) {
      ElMessage.warning(`"${c.name}"分值应在 0-${c.maxScore} 之间`)
      return false
    }
  }
  return true
}
</script>

<style scoped>
.page-header {
  display: flex; align-items: center; gap: 16px; margin-bottom: 16px;
}
.page-header h2 { font-size: 20px; color: #303133; margin: 0; }

.section { margin-bottom: 16px; }
.section-title { font-weight: 600; }

/* PDF viewer */
.pdf-viewer-wrapper {
  position: relative;
  width: 100%;
  height: 650px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  overflow: hidden;
}

.watermark-layer {
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  pointer-events: none;
  z-index: 10;
  overflow: hidden;
}

.watermark-text {
  position: absolute;
  font-size: 14px;
  color: #000;
  white-space: nowrap;
  user-select: none;
}

.pdf-iframe {
  width: 100%;
  height: 100%;
  border: none;
  position: relative;
  z-index: 1;
}

.material-info {
  margin-top: 8px; font-size: 13px; color: #909399;
}

/* Video wrapper */
.video-wrapper { margin-bottom: 4px; }

/* Criteria */
.criteria-item {
  padding: 12px;
  margin-bottom: 12px;
  background: #f9f9fa;
  border-radius: 6px;
}

.criteria-header {
  display: flex; align-items: baseline; margin-bottom: 4px;
}
.criteria-name { font-size: 15px; font-weight: 500; color: #303133; }
.criteria-max { font-size: 13px; color: #909399; margin-left: 8px; }
.criteria-desc { font-size: 13px; color: #909399; margin-bottom: 8px; }
.criteria-att-link {
  font-size: 12px; color: #409eff; text-decoration: none;
  display: inline-flex; align-items: center; gap: 2px;
  margin-left: auto;
}
.criteria-att-link:hover { text-decoration: underline; }

.score-actions { display: flex; gap: 12px; margin-top: 16px; }

.completed-tip { margin-top: 8px; }
</style>
