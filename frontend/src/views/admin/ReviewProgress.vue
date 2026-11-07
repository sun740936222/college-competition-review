<template>
  <div class="review-progress">
    <div class="page-header">
      <h2>评审进度</h2>
    </div>

    <el-card class="section">
      <template #header><span class="card-title">选择赛事</span></template>
      <el-select v-model="filterEvent" placeholder="选择赛事" style="width: 300px" @change="loadProgress">
        <el-option v-for="e in events" :key="e.id" :label="e.name" :value="e.id" />
      </el-select>
    </el-card>

    <template v-if="progress">
      <el-card class="section">
        <template #header><span class="card-title">概览</span></template>
        <el-row :gutter="20">
          <el-col :span="4">
            <div class="stat-card">
              <div class="stat-value">{{ progress.totalProjects }}</div>
              <div class="stat-label">已提交项目</div>
            </div>
          </el-col>
          <el-col :span="4">
            <div class="stat-card">
              <div class="stat-value">{{ progress.approvedProjects }}</div>
              <div class="stat-label">已审核通过</div>
            </div>
          </el-col>
          <el-col :span="4">
            <div class="stat-card">
              <div class="stat-value">{{ progress.allocatedProjects }}</div>
              <div class="stat-label">已分配评委</div>
            </div>
          </el-col>
          <el-col :span="4">
            <div class="stat-card">
              <div class="stat-value">{{ progress.fullyReviewedProjects }}</div>
              <div class="stat-label">全部评完</div>
            </div>
          </el-col>
          <el-col :span="4">
            <div class="stat-card">
              <div class="stat-value">{{ progress.partialReviewedProjects }}</div>
              <div class="stat-label">部分评完</div>
            </div>
          </el-col>
          <el-col :span="4">
            <div class="stat-card">
              <div class="stat-value">{{ progress.unallocatedProjects }}</div>
              <div class="stat-label">未分配评委</div>
            </div>
          </el-col>
        </el-row>
        <el-row :gutter="20" style="margin-top:16px">
          <el-col :span="4">
            <div class="stat-card">
              <div class="stat-value">{{ progress.totalJudges }}</div>
              <div class="stat-label">评委总数</div>
            </div>
          </el-col>
          <el-col :span="4">
            <div class="stat-card">
              <div class="stat-value">{{ progress.activeJudges }}</div>
              <div class="stat-label">已分配评委</div>
            </div>
          </el-col>
          <el-col :span="4">
            <div class="stat-card">
              <div class="stat-value">{{ progress.completedReviews }}</div>
              <div class="stat-label">已完成评审(人次)</div>
            </div>
          </el-col>
        </el-row>
      </el-card>

      <el-card class="section">
        <template #header>
          <div style="display:flex;justify-content:space-between;align-items:center">
            <span class="card-title">项目评审明细</span>
            <div style="display:flex;gap:8px">
              <el-select v-model="statusFilter" placeholder="评审状态" style="width:140px" clearable>
                <el-option label="全部完成" value="done" />
                <el-option label="进行中" value="partial" />
                <el-option label="未分配" value="none" />
              </el-select>
              <el-select v-model="trackFilter" placeholder="赛道" style="width:160px" clearable>
                <el-option v-for="t in allTracks" :key="t" :label="t" :value="t" />
              </el-select>
            </div>
          </div>
        </template>
        <el-table :data="filteredProjects" stripe v-loading="loading" empty-text="暂无项目数据">
          <el-table-column prop="projectName" label="项目名称" min-width="180" />
          <el-table-column prop="orgName" label="所属单位" width="140" v-if="showOrgColumn" />
          <el-table-column label="赛道/分组" width="200">
            <template #default="{ row }">
              {{ row.trackName }}{{ row.groupName ? ' / ' + row.groupName : '' }}
            </template>
          </el-table-column>
          <el-table-column label="项目状态" width="100">
            <template #default="{ row }">
              <el-tag v-if="row.projectStatus === 'APPROVED'" type="success" size="small">已通过</el-tag>
              <el-tag v-else-if="row.projectStatus === 'PENDING_REVIEW'" type="warning" size="small">待审核</el-tag>
              <el-tag v-else-if="row.projectStatus === 'REJECTED'" type="danger" size="small">已退回</el-tag>
              <el-tag v-else size="small">{{ row.projectStatus }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="评委" width="100">
            <template #default="{ row }">
              <span :class="{ 'text-warning': row.totalJudges === 0 }">
                {{ row.completedJudges }} / {{ row.totalJudges }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="完成进度" min-width="160">
            <template #default="{ row }">
              <el-progress
                :percentage="row.completionPercent"
                :status="row.completionPercent === 100 ? 'success' : row.totalJudges === 0 ? 'exception' : ''"
                :stroke-width="16"
              />
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '../../stores/user'
import { listAllEvents, getReviewProgress, getTracks } from '../../api/admin'

const userStore = useUserStore()
const events = ref([])
const filterEvent = ref(null)
const progress = ref(null)
const loading = ref(false)
const statusFilter = ref('')
const trackFilter = ref('')
const allTracks = ref([])

const adminLevel = computed(() => userStore.user?.orgLevel || 'COLLEGE')
const showOrgColumn = computed(() => ['NATIONAL', 'PROVINCE'].includes(adminLevel.value))

const filteredProjects = computed(() => {
  if (!progress.value?.projects) return []
  let list = progress.value.projects
  if (statusFilter.value === 'done') {
    list = list.filter(p => p.totalJudges > 0 && p.completionPercent === 100)
  } else if (statusFilter.value === 'partial') {
    list = list.filter(p => p.totalJudges > 0 && p.completionPercent < 100)
  } else if (statusFilter.value === 'none') {
    list = list.filter(p => p.totalJudges === 0)
  }
  if (trackFilter.value) {
    list = list.filter(p => (p.trackName || '') === trackFilter.value)
  }
  return list.sort((a, b) => {
    if (a.totalJudges === 0 && b.totalJudges > 0) return 1
    if (b.totalJudges === 0 && a.totalJudges > 0) return -1
    return a.completionPercent - b.completionPercent
  })
})

onMounted(async () => {
  try {
    const res = await listAllEvents({ current: 1, size: 1000 })
    events.value = res.data?.records || []
  } catch {}
})

async function loadProgress() {
  if (!filterEvent.value) { progress.value = null; return }
  loading.value = true
  try {
    const res = await getReviewProgress(filterEvent.value)
    progress.value = res.data
    trackFilter.value = ''
    statusFilter.value = ''
    const tracks = [...new Set((res.data?.projects || []).map(p => p.trackName).filter(Boolean))]
    allTracks.value = tracks.sort()
  } catch {} finally {
    loading.value = false
  }
}
</script>

<style scoped>
.page-header { margin-bottom: 16px; }
.page-header h2 { font-size: 20px; color: #303133; }
.section { margin-bottom: 16px; }
.card-title { font-weight: 600; }
.stat-card {
  background: #f5f7fa; border-radius: 8px; padding: 16px; text-align: center;
}
.stat-value { font-size: 28px; font-weight: 700; color: #409eff; }
.stat-label { font-size: 13px; color: #909399; margin-top: 6px; }
.text-warning { color: #e6a23c; font-weight: 600; }
</style>
