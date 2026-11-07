<template>
  <div class="judge-allocation">
    <div class="page-header">
      <h2>评委分配</h2>
    </div>

    <el-card class="section">
      <template #header><span class="card-title">选择赛事</span></template>
      <div class="selector-row">
        <el-select v-model="filterEvent" placeholder="选择赛事" clearable style="width: 300px" @change="onEventChange">
          <el-option v-for="e in events" :key="e.id" :label="e.name" :value="e.id" />
        </el-select>
      </div>
    </el-card>

    <el-card v-if="filterEvent" class="section">
      <template #header>
        <span class="card-title">已审核项目列表</span>
        <span style="font-size:13px;color:#909399;font-weight:normal">已通过审核的项目可分配评委</span>
      </template>

      <el-collapse v-model="activeTracks">
        <el-collapse-item v-for="track in tracks" :key="track.id" :name="track.id">
          <template #title>
            {{ trackLabel(track) }}
            <span style="margin-left:8px;font-size:12px;color:#909399">
              ({{ (projectsByTrack[track.id] || []).length }} 个项目)
            </span>
          </template>

          <div style="margin-bottom:12px">
            <el-button size="small" type="primary" @click="batchAllocateTrack(track.id)">
              一键分配该赛道
            </el-button>
          </div>

          <el-table :data="projectsByTrack[track.id] || []" stripe size="small">
            <el-table-column prop="name" label="项目名称" min-width="180" />
            <el-table-column prop="organizationName" label="所属高校" width="150" />
            <el-table-column label="已分配评委" width="250">
              <template #default="{ row }">
                <el-tag v-for="j in getJudgesFor(row.id)" :key="j.judgeId" closable @close="handleRemoveAllocation(j.id)" size="small" style="margin-right:4px;margin-bottom:4px">
                  {{ j.judgeName }}
                </el-tag>
                <span v-if="getJudgesFor(row.id).length === 0" style="color:#909399">未分配</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180">
              <template #default="{ row }">
                <el-popover placement="left" :width="300" trigger="click">
                  <template #reference>
                    <el-button size="small" type="primary">分配评委</el-button>
                  </template>
                  <div class="popover-content">
                    <p style="margin-bottom:8px;font-weight:500">选择评委（可多选）</p>
                    <el-checkbox :model-value="isAllJudgesSelected(row.id)" @change="toggleAllJudges(row.id)" style="margin-bottom:6px;font-weight:600">全选 / 取消全选</el-checkbox>
                    <el-checkbox-group v-model="selectedJudges[row.id]">
                      <el-checkbox v-for="j in judges" :key="j.id" :label="j.id">
                        <el-badge :value="getAllocationCount(j.id)" :hidden="getAllocationCount(j.id) === 0">
                          {{ j.realName || j.username }}
                        </el-badge>
                      </el-checkbox>
                    </el-checkbox-group>
                    <div style="margin-top:10px;text-align:right">
                      <el-button size="small" type="primary" @click="confirmAllocate(row)">确认分配</el-button>
                    </div>
                  </div>
                </el-popover>
              </template>
            </el-table-column>
          </el-table>
        </el-collapse-item>
      </el-collapse>

      <div style="display:flex;justify-content:center;margin-top:16px" v-if="total > 0">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="prev, pager, next, sizes, total"
          @current-change="loadApprovedProjects(filterEvent)"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <!-- Batch Track Allocate Dialog -->
    <el-dialog v-model="batchTrackDialogVisible" title="批量分配" width="400px">
      <p style="margin-bottom:12px;font-weight:500">选择评委（可多选）</p>
      <el-checkbox :model-value="batchAllSelected" @change="toggleBatchAll" style="margin-bottom:6px;font-weight:600">全选 / 取消全选</el-checkbox>
      <el-checkbox-group v-model="batchTrackJudges">
        <el-checkbox v-for="j in judges" :key="j.id" :label="j.id">{{ j.realName || j.username }}</el-checkbox>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="batchTrackDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmBatchAllocate">确认分配</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listAllEvents, listAdminProjects, listJudges, allocateJudges, listAllocations, removeAllocation, batchAllocateByTrack, getTracks } from '../../api/admin'

const events = ref([])
const filterEvent = ref(null)
const loading = ref(false)
const approvedProjects = ref([])
const judges = ref([])
const allocations = ref([])
const selectedJudges = reactive({})
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const tracks = ref([])
const activeTracks = ref([])
const batchTrackDialogVisible = ref(false)
const batchTrackId = ref(null)
const batchTrackJudges = ref([])

onMounted(async () => {
  try {
    const res = await listAllEvents({ current: 1, size: 1000 })
    events.value = res.data?.records || []
  } catch {}
})

async function onEventChange(val) {
  if (!val) { approvedProjects.value = []; total.value = 0; return }
  currentPage.value = 1
  await loadTracksFn(val)
  await loadApprovedProjects(val)
  await loadJudges()
  await loadAllocations(val)
}

async function loadApprovedProjects(eventId) {
  loading.value = true
  try {
    const res = await listAdminProjects({ eventId, status: 'APPROVED', current: currentPage.value, size: pageSize.value })
    const data = res.data
    approvedProjects.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function handleSizeChange() {
  currentPage.value = 1
  loadApprovedProjects(filterEvent.value)
}

async function loadJudges() {
  try {
    const res = await listJudges({ current: 1, size: 10000 })
    judges.value = res.data?.records || []
  } catch {}
}

async function loadAllocations(eventId) {
  try {
    const res = await listAllocations({ eventId, current: 1, size: 10000 })
    allocations.value = res.data?.records || []
  } catch {}
}

function getJudgesFor(projectId) {
  return allocations.value.filter(a => a.projectId === projectId)
}

const projectsByTrack = computed(() => {
  const map = {}
  for (const p of approvedProjects.value) {
    const tid = p.trackId
    if (!map[tid]) map[tid] = []
    map[tid].push(p)
  }
  return map
})

function trackLabel(t) {
  return t.groupName ? `${t.name}（${t.groupName}）` : t.name
}

const allJudgeIds = computed(() => judges.value.map(j => j.id))

function isAllJudgesSelected(projectId) {
  const selected = selectedJudges[projectId] || []
  return judges.value.length > 0 && selected.length === judges.value.length
}

function toggleAllJudges(projectId) {
  if (isAllJudgesSelected(projectId)) {
    selectedJudges[projectId] = []
  } else {
    selectedJudges[projectId] = [...allJudgeIds.value]
  }
}

const batchAllSelected = computed(() => {
  return judges.value.length > 0 && batchTrackJudges.value.length === judges.value.length
})

function toggleBatchAll() {
  if (batchAllSelected.value) {
    batchTrackJudges.value = []
  } else {
    batchTrackJudges.value = [...allJudgeIds.value]
  }
}

function getAllocationCount(judgeId) {
  return allocations.value.filter(a => a.judgeId === judgeId).length
}

function batchAllocateTrack(trackId) {
  batchTrackId.value = trackId
  batchTrackJudges.value = []
  batchTrackDialogVisible.value = true
}

async function confirmBatchAllocate() {
  if (batchTrackJudges.value.length === 0) {
    ElMessage.warning('请选择至少一名评委')
    return
  }
  try {
    await batchAllocateByTrack({
      eventId: filterEvent.value,
      trackId: batchTrackId.value,
      judgeIds: batchTrackJudges.value
    })
    ElMessage.success('批量分配成功')
    batchTrackDialogVisible.value = false
    await loadAllocations(filterEvent.value)
  } catch {
    ElMessage.error('批量分配失败')
  }
}

async function loadTracksFn(eventId) {
  try {
    const res = await getTracks(eventId)
    tracks.value = res.data || []
  } catch {
    tracks.value = []
  }
}

async function confirmAllocate(row) {
  const selected = selectedJudges[row.id] || []
  if (selected.length === 0) {
    ElMessage.warning('请选择至少一名评委')
    return
  }

  try {
    await allocateJudges({
      allocations: [{ projectId: row.id, judgeIds: selected }]
    })
    ElMessage.success('分配成功')
    selectedJudges[row.id] = []
    await loadAllocations(filterEvent.value)
  } catch {
    ElMessage.error('分配失败')
  }
}

async function handleRemoveAllocation(allocationId) {
  try {
    await removeAllocation(allocationId)
    ElMessage.success('已移除分配')
    await loadAllocations(filterEvent.value)
  } catch {
    ElMessage.error('移除分配失败')
  }
}
</script>

<style scoped>
.page-header { margin-bottom: 16px; }
.page-header h2 { font-size: 20px; color: #303133; }
.section { margin-bottom: 16px; }
.card-title { font-weight: 600; }
.selector-row { display: flex; gap: 12px; flex-wrap: wrap; }
.popover-content { padding: 4px 0; }
</style>
