<template>
  <div class="score-summary">
    <div class="page-header">
      <h2>成绩汇总</h2>
    </div>

    <el-card class="section">
      <template #header><span class="card-title">选择赛事</span></template>
      <div class="selector-row">
        <el-select v-model="filterEvent" placeholder="选择赛事" clearable style="width: 300px" @change="onEventChange">
          <el-option v-for="e in events" :key="e.id" :label="e.name" :value="e.id" />
        </el-select>
        <el-select v-model="filterTrack" placeholder="赛道筛选（可选）" clearable style="width: 200px" @change="currentPage = 1; loadScores()">
          <el-option v-for="t in tracks" :key="t.id" :label="trackLabel(t)" :value="t.id" />
        </el-select>
      </div>
    </el-card>

    <el-card v-if="filterEvent" class="section">
      <template #header>
        <div style="display:flex;align-items:center;justify-content:space-between;width:100%">
          <div>
            <span class="card-title">项目成绩</span>
            <span style="font-size:13px;color:#909399;font-weight:normal;margin-left:8px">仅显示已完成评审的项目成绩</span>
          </div>
          <div style="display:flex;gap:8px">
            <el-button size="small" @click="handleExport('scores')">导出成绩</el-button>
            <el-button size="small" @click="handleExport('projects')">导出项目</el-button>
            <el-button size="small" @click="handleExport('allocations')">导出分配</el-button>
          </div>
        </div>
      </template>

    <div class="pagination-bar" v-if="total > 0">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="prev, pager, next, sizes, total"
        @current-change="loadScores"
        @size-change="handleSizeChange"
      />
    </div>

    <el-table :data="scoreItems" v-loading="loading" stripe>
        <el-table-column prop="projectName" label="项目名称" min-width="200" />
        <el-table-column prop="organizationName" label="所属高校" width="150" />
        <el-table-column prop="trackName" label="赛道" width="180" />
        <el-table-column prop="judgeCount" label="评委数" width="80" align="center" />
        <el-table-column prop="averageScore" label="平均分" width="100" align="center">
          <template #default="{ row }">{{ row.averageScore != null ? row.averageScore.toFixed(1) : '-' }}</template>
        </el-table-column>
        <el-table-column prop="highestScore" label="最高分" width="100" align="center">
          <template #default="{ row }">{{ row.highestScore != null ? row.highestScore.toFixed(1) : '-' }}</template>
        </el-table-column>
        <el-table-column prop="lowestScore" label="最低分" width="100" align="center">
          <template #default="{ row }">{{ row.lowestScore != null ? row.lowestScore.toFixed(1) : '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'APPROVED' ? 'success' : 'info'" size="small">
              {{ row.status === 'APPROVED' ? '已通过' : row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="110" align="center" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.judgeCount > 0" type="primary" size="small" @click="showScoreDetail(row)">
              评分详情
            </el-button>
            <span v-else style="color:#c0c4cc;font-size:13px">暂无评分</span>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && scoreItems.length === 0" description="暂无评分数据" />
    </el-card>

    <!-- Score Detail Dialog -->
    <el-dialog v-model="detailVisible" :title="detailTitle" width="900px" top="5vh">
      <div v-loading="detailLoading">
        <template v-if="detailData">
          <el-table :data="detailData.judgeScores" border stripe size="small">
            <el-table-column label="评委" width="120">
              <template #default="{ row }">{{ row.judgeName }}</template>
            </el-table-column>
            <el-table-column v-for="c in detailData.criteria" :key="c.id" :label="c.name" width="140" align="center">
              <template #default="{ row }">
                <el-tag v-if="getJudgeScore(row, c.id)" :type="scoreTagType(getJudgeScore(row, c.id).score, c.maxScore)" size="small">
                  {{ getJudgeScore(row, c.id).score }} / {{ c.maxScore }}
                </el-tag>
                <span v-else style="color:#c0c4cc">-</span>
              </template>
            </el-table-column>
            <el-table-column label="总分" width="80" align="center">
              <template #default="{ row }">
                <span style="font-weight:600">{{ row.totalScore != null ? row.totalScore.toFixed(1) : '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="评语" min-width="160">
              <template #default="{ row }">{{ row.overallComment || '-' }}</template>
            </el-table-column>
            <el-table-column label="提交时间" width="160">
              <template #default="{ row }">{{ row.submittedAt ? formatTime(row.submittedAt) : '-' }}</template>
            </el-table-column>
          </el-table>
          <div v-if="!(detailData.judgeScores && detailData.judgeScores.length > 0)" style="text-align:center;padding:32px;color:#909399">
            暂无评委完成评分
          </div>
        </template>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { listAllEvents, getTracks, getScoreSummary, getProjectScoreDetail, exportScores, exportProjects, exportAllocations, downloadBlob } from '../../api/admin'
import { ElMessage } from 'element-plus'

const events = ref([])
const tracks = ref([])
const filterEvent = ref(null)
const filterTrack = ref(null)
const scoreItems = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)

const detailVisible = ref(false)
const detailLoading = ref(false)
const detailData = ref(null)
const detailTitle = ref('')

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
  currentPage.value = 1
  if (!val) { scoreItems.value = []; total.value = 0; return }
  try {
    const res = await getTracks(val)
    tracks.value = res.data || []
  } catch {}
  await loadScores()
}

async function loadScores() {
  if (!filterEvent.value) return
  loading.value = true
  try {
    const params = { current: currentPage.value, size: pageSize.value }
    if (filterTrack.value) params.trackId = filterTrack.value
    const res = await getScoreSummary(filterEvent.value, params)
    const data = res.data
    scoreItems.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function handleSizeChange() {
  currentPage.value = 1
  loadScores()
}

async function showScoreDetail(row) {
  detailVisible.value = true
  detailTitle.value = `评分详情 - ${row.projectName}`
  detailLoading.value = true
  detailData.value = null
  try {
    const res = await getProjectScoreDetail(filterEvent.value, row.projectId)
    detailData.value = res.data
  } catch (e) {
    detailData.value = null
  } finally {
    detailLoading.value = false
  }
}

function getJudgeScore(judgeRow, criteriaId) {
  if (!judgeRow.scores) return null
  return judgeRow.scores.find(s => s.criteriaId === criteriaId) || null
}

function scoreTagType(score, maxScore) {
  const ratio = score / maxScore
  if (ratio >= 0.8) return 'success'
  if (ratio >= 0.6) return 'warning'
  return 'danger'
}

function formatTime(t) {
  if (!t) return '-'
  const d = new Date(t)
  return d.toLocaleDateString('zh-CN') + ' ' + d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

async function handleExport(type) {
  if (!filterEvent.value) return
  try {
    let data
    let name
    if (type === 'scores') {
      data = await exportScores(filterEvent.value)
      name = '成绩.xlsx'
    } else if (type === 'projects') {
      data = await exportProjects(filterEvent.value)
      name = '项目汇总.xlsx'
    } else {
      data = await exportAllocations(filterEvent.value)
      name = '评委分配明细.xlsx'
    }
    downloadBlob(data, name)
    ElMessage.success('导出成功')
  } catch {
    ElMessage.error('导出失败')
  }
}
</script>

<style scoped>
.page-header { margin-bottom: 16px; }
.page-header h2 { font-size: 20px; color: #303133; }
.section { margin-bottom: 16px; }
.card-title { font-weight: 600; }
.selector-row { display: flex; gap: 12px; flex-wrap: wrap; }
.pagination-bar { display: flex; justify-content: center; margin-bottom: 12px; }
</style>
