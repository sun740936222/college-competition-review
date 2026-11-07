<template>
  <div class="judge-task-board">
    <div class="page-header">
      <h2>评审任务</h2>
    </div>

    <el-tabs v-model="activeTab" @tab-change="currentPage = 1; loadProjects()">
      <el-tab-pane label="待评项目" name="PENDING" />
      <el-tab-pane label="已评项目" name="COMPLETED" />
      <el-tab-pane label="全部" name="" />
    </el-tabs>

    <div v-loading="loading">
      <el-empty v-if="!loading && projects.length === 0" description="暂无评审任务" />

      <div v-else class="project-grid">
        <el-card
          v-for="p in projects"
          :key="p.id"
          class="project-card"
          shadow="hover"
          @click="goToReview(p.id)"
        >
          <div class="card-top">
            <h3 class="project-name">{{ p.name }}</h3>
            <el-tag :type="p.status === 'COMPLETED' ? 'success' : 'warning'" size="small">
              {{ p.status === 'COMPLETED' ? '已评' : '待评' }}
            </el-tag>
          </div>
          <div class="card-meta">
            <div class="meta-item">
              <el-icon><Trophy /></el-icon>
              <span>{{ p.eventName }}</span>
            </div>
            <div class="meta-item">
              <el-icon><Flag /></el-icon>
              <span>{{ p.trackName }}</span>
            </div>
          </div>
          <div v-if="p.totalScore != null" class="card-score">
            总分：<strong>{{ p.totalScore }}</strong>
          </div>
          <div class="card-footer">
            <span class="allocated-time">分配时间：{{ formatTime(p.allocatedAt) }}</span>
            <el-button type="primary" size="small">
              {{ p.status === 'COMPLETED' ? '查看' : '开始评审' }}
            </el-button>
          </div>
        </el-card>
      </div>

      <div style="display:flex;justify-content:center;margin-top:20px" v-if="total > 0">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[12, 24, 48, 96]"
          layout="prev, pager, next, sizes, total"
          @current-change="loadProjects"
          @size-change="handleSizeChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listJudgeProjects } from '../../api/judge'

const router = useRouter()
const loading = ref(false)
const activeTab = ref('PENDING')
const projects = ref([])
const currentPage = ref(1)
const pageSize = ref(12)
const total = ref(0)

onMounted(() => loadProjects())

async function loadProjects() {
  loading.value = true
  try {
    const params = { current: currentPage.value, size: pageSize.value }
    const status = activeTab.value
    if (status) params.status = status
    const res = await listJudgeProjects(params)
    const data = res.data
    projects.value = data.records || []
    total.value = data.total || 0
  } catch {} finally {
    loading.value = false
  }
}

function handleSizeChange() {
  currentPage.value = 1
  loadProjects()
}

function goToReview(id) {
  router.push({ name: 'JudgeProjectReview', params: { id } })
}

function formatTime(t) {
  if (!t) return '-'
  const d = new Date(t)
  return d.toLocaleDateString('zh-CN') + ' ' + d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}
</script>

<style scoped>
.page-header { margin-bottom: 16px; }
.page-header h2 { font-size: 20px; color: #303133; }

.project-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 16px;
  margin-top: 16px;
}

.project-card {
  cursor: pointer;
  transition: transform 0.15s;
}
.project-card:hover { transform: translateY(-2px); }

.card-top {
  display: flex; justify-content: space-between; align-items: flex-start;
  margin-bottom: 12px;
}
.project-name { font-size: 16px; color: #303133; margin: 0; flex: 1; margin-right: 8px; }

.card-meta { margin-bottom: 10px; }
.meta-item {
  display: flex; align-items: center; gap: 6px;
  font-size: 13px; color: #909399; margin-bottom: 4px;
}

.card-score { font-size: 14px; color: #303133; margin-bottom: 8px; }
.card-score strong { color: #409eff; font-size: 18px; }

.card-footer {
  display: flex; justify-content: space-between; align-items: center;
  margin-top: 8px; padding-top: 12px; border-top: 1px solid #ebeef5;
}
.allocated-time { font-size: 12px; color: #c0c4cc; }
</style>