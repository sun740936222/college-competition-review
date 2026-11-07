<template>
  <div class="project-detail">
    <div class="page-header">
      <el-button @click="$router.back()" text><el-icon><ArrowLeft /></el-icon>返回</el-button>
      <h2>项目详情</h2>
      <div>
        <el-tag :type="statusTag(project.status)" size="large">{{ statusLabel(project.status) }}</el-tag>
      </div>
    </div>

    <div v-loading="loading">
      <el-empty v-if="!loading && !project.id" description="项目不存在" />

      <template v-else>
        <!-- Basic Info -->
        <el-card class="section">
          <template #header><span class="card-title">基本信息</span></template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="赛事">{{ project.eventName }}</el-descriptions-item>
            <el-descriptions-item label="赛道">{{ project.trackName }}</el-descriptions-item>
            <el-descriptions-item label="项目名称" :span="2">{{ project.name }}</el-descriptions-item>
            <el-descriptions-item label="项目简介" :span="2">{{ project.description || '暂无' }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ formatDate(project.createdAt) }}</el-descriptions-item>
            <el-descriptions-item label="更新时间">{{ formatDate(project.updatedAt) }}</el-descriptions-item>
          </el-descriptions>
          <div v-if="project.status === 'REJECTED' && project.rejectReason" class="reject-reason">
            <el-alert :title="'退回原因：' + project.rejectReason" type="error" :closable="false" />
          </div>
        </el-card>

        <!-- Submission deadline warning -->
        <el-alert
          v-if="(project.status === 'DRAFT' || project.status === 'REJECTED') && eventInfo"
          :title="deadlineTitle"
          :type="deadlinePassed ? 'error' : 'warning'"
          :closable="false"
          show-icon
          class="deadline-alert"
        >
          <template v-if="!deadlinePassed">
            提交截止时间：{{ formatDate(project.submissionDeadline || eventInfo.submissionDeadline) }}，请在此日期前完成材料上传并提交审核
          </template>
        </el-alert>

        <!-- Actions -->
        <div class="actions" v-if="project.status === 'DRAFT' || project.status === 'PENDING_REVIEW' || project.status === 'REJECTED'">
          <el-button v-if="project.status === 'DRAFT' || project.status === 'REJECTED'" type="primary" @click="$router.push(`/projects/${project.id}/edit`)">
            <el-icon><Edit /></el-icon>编辑基本信息
          </el-button>
          <el-button v-if="project.status === 'DRAFT' || project.status === 'REJECTED'" type="success" :loading="submitting" @click="handleSubmit" :disabled="deadlinePassed">
            <el-icon><Upload /></el-icon>提交审核
          </el-button>
          <el-button v-if="project.status === 'DRAFT'" type="danger" @click="handleDelete">
            <el-icon><Delete /></el-icon>删除项目
          </el-button>
          <el-button v-if="project.status === 'PENDING_REVIEW'" type="warning" @click="handleWithdraw">
            <el-icon><Back /></el-icon>撤回
          </el-button>
        </div>

        <!-- Material upload section for DRAFT/REJECTED projects -->
        <el-card class="section" v-if="project.status === 'DRAFT' || project.status === 'REJECTED'">
          <template #header>
            <div class="card-title-row">
              <span class="card-title">上传材料</span>
              <span class="card-subtitle">请在此上传项目所需材料，完成后点击上方"提交审核"按钮</span>
            </div>
          </template>
          <MaterialUploader ref="materialUploaderRef" :require-video="project.requireVideo" />
          <el-button type="primary" :loading="savingMaterials" @click="handleSaveMaterials" style="margin-top: 12px">
            保存材料
          </el-button>
        </el-card>

        <!-- Members -->
        <el-card class="section">
          <template #header><span class="card-title">团队成员（{{ project.teamMembers?.length || 0 }}人）</span></template>
          <el-empty v-if="!project.teamMembers?.length" description="暂无团队成员" />
          <el-table v-else :data="project.teamMembers" size="small">
            <el-table-column prop="name" label="姓名" />
            <el-table-column label="角色">
              <template #default="{ row }">{{ row.memberRole === 'LEADER' ? '负责人' : '成员' }}</template>
            </el-table-column>
            <el-table-column prop="studentId" label="学号" />
            <el-table-column prop="department" label="院系" />
            <el-table-column prop="major" label="专业" />
            <el-table-column prop="phone" label="电话" />
          </el-table>
        </el-card>

        <!-- Advisors -->
        <el-card class="section">
          <template #header><span class="card-title">指导老师（{{ project.advisors?.length || 0 }}人）</span></template>
          <el-empty v-if="!project.advisors?.length" description="暂无指导老师" />
          <el-table v-else :data="project.advisors" size="small">
            <el-table-column prop="name" label="姓名" />
            <el-table-column prop="phone" label="电话" />
          </el-table>
        </el-card>

        <!-- Materials -->
        <el-card class="section">
          <template #header><span class="card-title">项目材料</span></template>
          <template v-if="project.materials?.length">
            <div v-for="m in project.materials" :key="m.id" class="material-row">
              <div class="material-info">
                <el-icon><Document /></el-icon>
                <span class="material-type">{{ materialLabel(m.materialType) }}</span>
                <span>{{ m.originalFilename }}</span>
                <span class="file-size">({{ formatSize(m.fileSize) }})</span>
              </div>
              <el-button size="small" @click="previewMaterial(m)">预览</el-button>
            </div>
          </template>
          <el-empty v-else description="暂无上传材料" />
        </el-card>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getProject, submitProject, withdrawProject, deleteProject, uploadMaterial } from '../../api/project'
import { getEvent } from '../../api/event'
import MaterialUploader from '../../components/project/MaterialUploader.vue'

const route = useRoute()
const router = useRouter()
const project = ref({})
const loading = ref(false)
const submitting = ref(false)
const savingMaterials = ref(false)
const materialUploaderRef = ref(null)
const eventInfo = ref(null)

const deadlinePassed = computed(() => {
  const deadline = project.value.submissionDeadline || eventInfo.value?.submissionDeadline
  if (!deadline) return false
  return new Date() > new Date(deadline)
})

const deadlineTitle = computed(() => {
  if (deadlinePassed.value) return '提交截止时间已过，无法提交审核'
  return '请在提交截止时间前完成材料上传'
})

onMounted(async () => {
  loading.value = true
  try {
    const res = await getProject(route.params.id)
    project.value = res.data || {}
    // Load event info for deadline
    if (project.value.eventId) {
      const eventRes = await getEvent(project.value.eventId)
      eventInfo.value = eventRes.data || null
    }
  } finally {
    loading.value = false
  }
})

async function handleSubmit() {
  if (deadlinePassed.value) {
    ElMessage.error('提交截止时间已过，无法提交')
    return
  }
  try { await ElMessageBox.confirm('确认提交审核？提交后将无法修改。', '确认', { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' }) } catch { return }
  submitting.value = true
  try {
    // Save pending materials first
    const files = materialUploaderRef.value?.files || []
    for (const m of files) {
      if (m.file) {
        await uploadMaterial(project.value.id, m.materialType, m.file)
      }
    }
    await submitProject(project.value.id)
    ElMessage.success('提交成功')
    loadProject()
  } finally {
    submitting.value = false
  }
}

async function handleSaveMaterials() {
  savingMaterials.value = true
  try {
    const files = materialUploaderRef.value?.files || []
    for (const m of files) {
      if (m.file) {
        await uploadMaterial(project.value.id, m.materialType, m.file)
      }
    }
    ElMessage.success('材料保存成功')
    loadProject()
  } finally {
    savingMaterials.value = false
  }
}

async function handleWithdraw() {
  try { await ElMessageBox.confirm('确认撤回？', '确认', { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' }) } catch { return }
  await withdrawProject(project.value.id)
  ElMessage.success('已撤回')
  loadProject()
}

async function handleDelete() {
  try {
    await ElMessageBox.confirm('删除后将无法恢复，确认删除？', '删除项目', {
      confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'error'
    })
  } catch { return }
  await deleteProject(project.value.id)
  ElMessage.success('项目已删除')
  router.push('/projects')
}

async function loadProject() {
  const res = await getProject(project.value.id)
  project.value = res.data || {}
}

function previewMaterial(m) {
  window.open(`/api/files/${m.storedFilename}`, '_blank')
}

function statusTag(s) {
  const map = { DRAFT: 'info', PENDING_REVIEW: 'warning', APPROVED: 'success', REJECTED: 'danger' }
  return map[s] || 'info'
}

function statusLabel(s) {
  const map = { DRAFT: '草稿', PENDING_REVIEW: '待审核', APPROVED: '已通过', REJECTED: '已退回' }
  return map[s] || s
}

function materialLabel(t) {
  const map = { PITCH_PDF: '路演材料', PROJECT_PLAN_PDF: '项目计划书', SUPPORTING_PDF: '佐证材料', VIDEO_MP4: '展示视频' }
  return map[t] || t
}

function formatDate(d) { return d ? new Date(d).toLocaleString('zh-CN') : '' }
function formatSize(b) {
  if (!b) return '0 B'
  const u = ['B', 'KB', 'MB', 'GB']; let s = b, i = 0
  while (s >= 1024 && i < u.length - 1) { s /= 1024; i++ }
  return s.toFixed(1) + ' ' + u[i]
}
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}
.page-header h2 { flex: 1; font-size: 22px; }
.section { margin-bottom: 16px; }
.card-title { font-weight: 600; }
.reject-reason { margin-top: 12px; }
.deadline-alert { margin-bottom: 16px; }
.actions { display: flex; gap: 12px; margin-bottom: 16px; }
.card-title-row { display: flex; align-items: baseline; gap: 12px; }
.card-subtitle { font-size: 12px; color: #909399; font-weight: 400; }
.material-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 10px 0; border-bottom: 1px solid #ebeef5;
}
.material-info { display: flex; align-items: center; gap: 8px; }
.material-type { font-weight: 500; color: #606266; }
.file-size { font-size: 12px; color: #909399; }
</style>
