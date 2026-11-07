<template>
  <div class="project-list">
    <div class="page-header">
      <h2>我的项目</h2>
      <el-button type="primary" @click="$router.push('/projects/create')">
        <el-icon><Plus /></el-icon>创建项目
      </el-button>
    </div>

    <div v-loading="loading">
      <el-empty v-if="!loading && projects.length === 0" description="暂无项目，点击上方按钮创建">
        <el-button type="primary" @click="$router.push('/projects/create')">创建项目</el-button>
      </el-empty>
      <div v-else class="project-grid">
        <div v-for="project in projects" :key="project.id" class="project-card" @click="$router.push(`/projects/${project.id}`)">
          <div class="card-top">
            <h3 class="project-name">{{ project.name }}</h3>
            <el-tag :type="statusTag(project.status)" size="small">{{ statusLabel(project.status) }}</el-tag>
          </div>
          <div class="card-body">
            <p><span>赛事：</span>{{ project.eventName }}</p>
            <p><span>赛道：</span>{{ project.trackName }}</p>
            <p v-if="getEventDeadlines(project)" class="deadline-info">
              <span>报名截止：{{ formatDate(getEventDeadlines(project).registrationDeadline) }}</span>
              <span>提交截止：{{ formatDate(getEventDeadlines(project).submissionDeadline) }}</span>
            </p>
          </div>
          <div class="card-footer">
            <span class="time">创建于 {{ formatDate(project.createdAt) }}</span>
            <el-icon><ArrowRight /></el-icon>
          </div>
        </div>
      </div>
    </div>

    <div class="pagination" v-if="total > 0">
      <el-pagination
        background
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[12, 24, 48, 96]"
        layout="prev, pager, next, sizes, total"
        @current-change="onPageChange"
        @size-change="handleSizeChange"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { listProjects } from '../../api/project'
import { listEvents } from '../../api/event'

const projects = ref([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(12)
const eventMap = ref({})

onMounted(async () => {
  // Load events for deadline info
  try {
    const eventRes = await listEvents()
    const events = eventRes.data || []
    eventMap.value = {}
    events.forEach(e => { eventMap.value[e.name] = e })
  } catch { /* non-critical */ }
  loadProjects(1)
})

function getEventDeadlines(project) {
  const event = eventMap.value[project.eventName]
  if (!event) return null
  return {
    registrationDeadline: event.registrationDeadline,
    submissionDeadline: event.submissionDeadline
  }
}

async function loadProjects(current) {
  loading.value = true
  try {
    const res = await listProjects({ current, size: pageSize.value })
    projects.value = res.data?.records || []
    total.value = res.data?.total || 0
    currentPage.value = current
  } finally {
    loading.value = false
  }
}

function onPageChange(page) {
  loadProjects(page)
}

function handleSizeChange() {
  currentPage.value = 1
  loadProjects(1)
}

function statusTag(status) {
  const map = { DRAFT: 'info', PENDING_REVIEW: 'warning', APPROVED: 'success', REJECTED: 'danger' }
  return map[status] || 'info'
}

function statusLabel(status) {
  const map = { DRAFT: '草稿', PENDING_REVIEW: '待审核', APPROVED: '已通过', REJECTED: '已退回' }
  return map[status] || status
}

function formatDate(date) {
  if (!date) return ''
  return new Date(date).toLocaleDateString('zh-CN')
}
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}
.page-header h2 { font-size: 24px; }
.project-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 16px;
}
.project-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  cursor: pointer;
  border: 1px solid #e4e7ed;
  transition: box-shadow 0.3s;
}
.project-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}
.card-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}
.project-name {
  font-size: 16px;
  color: #303133;
  flex: 1;
  margin-right: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.card-body p {
  font-size: 13px;
  color: #606266;
  margin-bottom: 4px;
}
.card-body p span { color: #909399; }
.deadline-info { display: flex; gap: 12px; font-size: 12px; color: #e6a23c; }
.deadline-info span { display: flex; align-items: center; gap: 2px; }
.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #ebeef5;
}
.time { font-size: 12px; color: #c0c4cc; }
.pagination { display: flex; justify-content: center; margin-top: 24px; }
</style>
