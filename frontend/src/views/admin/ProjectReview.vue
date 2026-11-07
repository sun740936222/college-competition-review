<template>
  <div class="project-review">
    <div class="page-header">
      <h2>项目审核</h2>
      <div style="display:flex;gap:8px">
        <el-button type="success" :disabled="batchableCount === 0" @click="handleBatchApprove">
          <el-icon><Select /></el-icon>批量通过
          <template v-if="batchableCount > 0"> ({{ batchableCount }})</template>
        </el-button>
      </div>
    </div>

    <div class="filters-bar">
      <el-select v-model="filterEvent" placeholder="选择赛事" clearable style="width: 200px" @change="onEventChange">
        <el-option v-for="e in events" :key="e.id" :label="e.name" :value="e.id" />
      </el-select>
      <el-select v-model="filterTrack" placeholder="赛道" clearable style="width: 200px" @change="loadProjects">
        <el-option v-for="t in tracks" :key="t.id" :label="trackLabel(t)" :value="t.id" />
      </el-select>
      <el-select v-model="filterEduLevel" placeholder="学历层次" clearable style="width: 140px" @change="loadProjects">
        <el-option label="本科生" value="本科" />
        <el-option label="研究生" value="研究生" />
      </el-select>
      <el-select v-if="adminLevel !== 'COLLEGE'" v-model="filterOrg" placeholder="所属学院" clearable filterable style="width: 200px"
        remote :remote-method="searchOrgs" :loading="orgSearchLoading" @change="loadProjects">
        <el-option v-for="o in orgOptions" :key="o.id" :label="o.name" :value="o.id" />
      </el-select>
      <el-input v-model="filterKeyword" placeholder="搜索项目名称" clearable style="width: 220px"
        @clear="loadProjects" @keyup.enter="loadProjects" />
      <el-select v-model="filterStatus" placeholder="项目状态" clearable style="width: 140px" @change="loadProjects">
        <el-option label="待审核" value="PENDING_REVIEW" />
        <el-option label="已通过" value="APPROVED" />
        <el-option label="已退回" value="REJECTED" />
        <el-option label="草稿" value="DRAFT" />
      </el-select>
      <el-button type="primary" @click="loadProjects">搜索</el-button>
      <el-button @click="resetFilters">重置</el-button>
    </div>

    <el-table :data="projects" v-loading="loading" stripe @row-click="showDetail" @selection-change="onSelectionChange">
      <el-table-column type="selection" width="40" />
      <el-table-column prop="name" label="项目名称" min-width="180" />
      <el-table-column prop="eventName" label="赛事" width="160" />
      <el-table-column prop="trackName" label="赛道" width="180" />

      <el-table-column prop="creatorName" label="创建者" width="100" />
      <el-table-column v-if="['PROVINCE', 'NATIONAL'].includes(adminLevel)" prop="organizationName" label="所属高校" width="150" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="当前审核" width="110">
        <template #default="{ row }">
          <el-tag v-if="row.currentReviewLevel" size="small"
            :type="reviewLevelTagType(row.currentReviewLevel)"
            :effect="row.currentReviewLevel === adminLevel ? 'dark' : 'plain'">
            {{ reviewLevelLabels[row.currentReviewLevel] || row.currentReviewLevel }}
          </el-tag>
          <span v-else style="color:#909399">-</span>
        </template>
      </el-table-column>
      <el-table-column label="成员/材料" width="100">
        <template #default="{ row }">{{ row.memberCount || 0 }}人 / {{ row.materialCount || 0 }}个</template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button v-if="canReview(row)" size="small" type="success" @click.stop="handleApprove(row)">通过</el-button>
          <el-button v-if="canReview(row)" size="small" type="warning" @click.stop="handleReject(row)">退回</el-button>
          <el-button size="small" @click.stop="showDetail(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrap" v-if="total > 0">
      <el-pagination
        v-model:current-page="current"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="prev, pager, next, sizes, total"
        @current-change="loadProjects"
        @size-change="handleSizeChange"
      />
    </div>

    <!-- Detail Drawer -->
    <el-drawer v-model="detailVisible" :title="detailProject?.name || '项目详情'" size="600px">
      <div v-if="detailProject" v-loading="detailLoading">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="项目名称">{{ detailProject.name }}</el-descriptions-item>
          <el-descriptions-item label="赛事">{{ detailProject.eventName }}</el-descriptions-item>
          <el-descriptions-item label="赛道">{{ detailProject.trackName }}</el-descriptions-item>
          <el-descriptions-item label="描述">{{ detailProject.description || '暂无' }}</el-descriptions-item>

        </el-descriptions>

        <h4 style="margin: 16px 0 8px">团队成员（{{ detailProject.teamMembers?.length || 0 }}人）</h4>
        <el-table :data="detailProject.teamMembers || []" size="small">
          <el-table-column prop="name" label="姓名" />
          <el-table-column label="角色">
            <template #default="{ row }">{{ row.memberRole === 'LEADER' ? '负责人' : '成员' }}</template>
          </el-table-column>
          <el-table-column prop="department" label="院系" />
          <el-table-column prop="major" label="专业" />
        </el-table>

        <h4 style="margin: 16px 0 8px">指导老师（{{ detailProject.advisors?.length || 0 }}人）</h4>
        <el-table :data="detailProject.advisors || []" size="small">
          <el-table-column prop="name" label="姓名" />
          <el-table-column prop="phone" label="电话" />
        </el-table>

        <div v-if="detailProject.currentReviewLevel" style="margin:16px 0;padding:12px;border-radius:8px"
          :style="{ background: reviewLevelBgColor(detailProject.currentReviewLevel), border: '1px solid ' + reviewLevelBorderColor(detailProject.currentReviewLevel) }">
          <span style="font-weight:600" :style="{ color: reviewLevelTextColor(detailProject.currentReviewLevel) }">当前审核环节：</span>
          <span>{{ reviewLevelLabels[detailProject.currentReviewLevel] || detailProject.currentReviewLevel }}</span>
          <span v-if="detailProject.competitionLevel" style="margin-left:8px;font-size:12px;color:#909399">
            （终审：{{ reviewLevelLabels[detailProject.competitionLevel] || detailProject.competitionLevel }}）
          </span>
        </div>

        <h4 style="margin: 16px 0 8px">材料</h4>
        <el-table :data="detailProject.materials || []" size="small" @row-click="previewMaterial" highlight-current-row>
          <el-table-column label="类型">
            <template #default="{ row }">{{ materialLabel(row.materialType) }}</template>
          </el-table-column>
          <el-table-column prop="originalFilename" label="文件名" />
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button size="small" text type="primary" @click.stop="previewMaterial(row)">预览</el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- Material Preview -->
        <div v-if="previewMaterialItem" style="margin-top:12px;border:1px solid #e4e7ed;border-radius:4px;overflow:hidden">
          <div style="display:flex;align-items:center;justify-content:space-between;padding:8px 12px;background:#f5f7fa;border-bottom:1px solid #e4e7ed">
            <span style="font-size:13px;color:#606266">{{ materialLabel(previewMaterialItem.materialType) }}：{{ previewMaterialItem.originalFilename }}</span>
            <el-button size="small" text @click="previewMaterialItem = null"><el-icon><Close /></el-icon></el-button>
          </div>
          <div v-if="isVideoType(previewMaterialItem.materialType)" style="background:#000;text-align:center">
            <video :src="getFileUrl(previewMaterialItem.storedFilename)" controls style="width:100%;max-height:400px"
              controlsList="nodownload" @contextmenu.prevent />
          </div>
          <iframe v-else :src="getFileUrl(previewMaterialItem.storedFilename)"
            style="width:100%;height:500px;border:none" />
        </div>

        <div style="margin-top: 16px; display: flex; gap: 12px" v-if="canReview(detailProject)">
          <el-button type="success" :loading="reviewLoading" @click="handleApprove(detailProject)">审核通过（下一环节：{{ nextLevelLabel(detailProject) }}）</el-button>
          <el-button type="warning" :loading="reviewLoading" @click="handleReject(detailProject)">退回</el-button>
        </div>
      </div>
    </el-drawer>


    <!-- Reject Dialog -->
    <el-dialog v-model="rejectDialogVisible" title="退回项目" width="450px">
      <el-form ref="rejectFormRef" :model="rejectForm" label-position="top" :rules="rejectRules">
        <el-form-item label="退回原因" prop="rejectReason">
          <el-input v-model="rejectForm.rejectReason" type="textarea" :rows="4" placeholder="请填写退回原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="reviewLoading" @click="confirmReject">确认退回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '../../stores/user'
import { listAdminProjects, getAdminProject, reviewProject, batchReviewProjects } from '../../api/admin'
import { listAllEvents, getTracks } from '../../api/admin'
import { searchOrganizations } from '../../api/auth'


const userStore = useUserStore()
const adminLevel = computed(() => userStore.user?.orgLevel || 'COLLEGE')

const batchableCount = computed(() => selectedProjects.value.filter(p => canReview(p)).length)

const reviewLevelLabels = { COLLEGE: '院系审核', SCHOOL: '校级审核', PROVINCE: '省级审核', NATIONAL: '国赛审核' }
const reviewLevelOrder = ['COLLEGE', 'SCHOOL', 'PROVINCE', 'NATIONAL']

const reviewLevelTagTypes = { COLLEGE: '', SCHOOL: 'success', PROVINCE: 'warning', NATIONAL: 'danger' }
const reviewLevelColors = { COLLEGE: '#f4f4f5', SCHOOL: '#f0f9eb', PROVINCE: '#fdf6ec', NATIONAL: '#fef0f0' }
const reviewLevelBorderColors = { COLLEGE: '#e9e9eb', SCHOOL: '#e1f3d8', PROVINCE: '#faecd8', NATIONAL: '#fde2e2' }
const reviewLevelTextColors = { COLLEGE: '#909399', SCHOOL: '#67c23a', PROVINCE: '#e6a23c', NATIONAL: '#f56c6c' }

function reviewLevelTagType(level) { return reviewLevelTagTypes[level] || 'info' }
function reviewLevelBgColor(level) { return reviewLevelColors[level] || '#f4f4f5' }
function reviewLevelBorderColor(level) { return reviewLevelBorderColors[level] || '#e9e9eb' }
function reviewLevelTextColor(level) { return reviewLevelTextColors[level] || '#909399' }

function canReview(project) {
  if (project.status !== 'PENDING_REVIEW') return false
  return project.currentReviewLevel === adminLevel.value
}

function nextLevelLabel(project) {
  if (!project.currentReviewLevel) return '完成'
  const competitionLevel = project.competitionLevel
  if (!competitionLevel) return '下一级'
  const currentIdx = reviewLevelOrder.indexOf(project.currentReviewLevel)
  const finalIdx = reviewLevelOrder.indexOf(competitionLevel)
  if (currentIdx < 0 || currentIdx >= finalIdx) return '完成'
  return reviewLevelLabels[reviewLevelOrder[currentIdx + 1]] || '完成'
}

const loading = ref(false)
const projects = ref([])
const current = ref(1)
const size = ref(20)
const total = ref(0)

// Filters
const filterEvent = ref(null)
const filterTrack = ref(null)
const filterEduLevel = ref(null)
const filterOrg = ref(null)
const filterKeyword = ref('')
const filterStatus = ref(null)
const events = ref([])
const tracks = ref([])
const orgOptions = ref([])
const orgSearchLoading = ref(false)

const detailVisible = ref(false)
const detailProject = ref(null)
const detailLoading = ref(false)
const reviewLoading = ref(false)

const rejectFormRef = ref(null)
const rejectDialogVisible = ref(false)
const rejectProject = ref(null)
const rejectForm = reactive({ rejectReason: '' })
const rejectRules = {
  rejectReason: [{ required: true, message: '请填写退回原因', trigger: 'blur' }]
}

const selectedProjects = ref([])

function onSelectionChange(selection) {
  selectedProjects.value = selection
}

const previewMaterialItem = ref(null)

function getFileUrl(storedFilename) {
  return `/api/files/${encodeURIComponent(storedFilename)}`
}

function isVideoType(type) {
  return type === 'VIDEO_MP4'
}

function previewMaterial(row) {
  previewMaterialItem.value = row
}

onMounted(async () => {
  try {
    const res = await listAllEvents({ current: 1, size: 1000 })
    events.value = res.data?.records || []
  } catch {}
  await loadProjects()
})

function onEventChange(val) {
  filterTrack.value = null
  filterEduLevel.value = null
  tracks.value = []
  if (val) {
    loadTracks(val)
  }
  loadProjects()
}

async function loadTracks(eventId) {
  try {
    const res = await getTracks(eventId)
    tracks.value = res.data || []
  } catch {
    tracks.value = []
  }
}

async function searchOrgs(query) {
  if (!query || query.trim().length === 0) {
    orgOptions.value = []
    return
  }
  orgSearchLoading.value = true
  try {
    const res = await searchOrganizations(query.trim())
    orgOptions.value = (res.data || []).slice(0, 30)
  } catch {
    orgOptions.value = []
  } finally {
    orgSearchLoading.value = false
  }
}

function trackLabel(t) {
  return t.groupName ? `${t.name}（${t.groupName}）` : t.name
}

async function loadProjects() {
  loading.value = true
  try {
    const params = { current: current.value, size: size.value }
    if (filterEvent.value) params.eventId = filterEvent.value
    if (filterTrack.value) params.trackId = filterTrack.value
    if (filterOrg.value) params.organizationId = filterOrg.value
    if (filterKeyword.value.trim()) params.keyword = filterKeyword.value.trim()
    if (filterStatus.value) params.status = filterStatus.value
    const res = await listAdminProjects(params)
    let records = res.data?.records || []

    // Client-side filter by education level (track groupName)
    if (filterEduLevel.value) {
      records = records.filter(p => {
        const tn = p.trackName || ''
        return tn.includes(filterEduLevel.value)
      })
    }

    projects.value = records
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

function handleSizeChange() {
  current.value = 1
  loadProjects()
}

function resetFilters() {
  filterEvent.value = null
  filterTrack.value = null
  filterEduLevel.value = null
  filterOrg.value = null
  filterKeyword.value = ''
  filterStatus.value = null
  tracks.value = []
  orgOptions.value = []
  current.value = 1
  loadProjects()
}

async function showDetail(row) {
  detailLoading.value = true
  detailVisible.value = true
  previewMaterialItem.value = null
  try {
    const res = await getAdminProject(row.id)
    detailProject.value = res.data || row
  } finally {
    detailLoading.value = false
  }
}

async function handleApprove(row) {
  const currentLabel = reviewLevelLabels[row.currentReviewLevel] || row.currentReviewLevel || '未知'
  const nextLabel = nextLevelLabel(row)
  const confirmMessage = nextLabel === '完成'
    ? `【${currentLabel}】为最终审核环节，确认通过？项目将标记为最终通过。`
    : `【${currentLabel}】通过后，将送至【${nextLabel}】审核，确认通过？`

  try {
    await ElMessageBox.confirm(confirmMessage, '审核确认', {
      confirmButtonText: '确认通过',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }

  reviewLoading.value = true
  try {
    await reviewProject(row.id, { action: 'APPROVE' })
    ElMessage.success('项目已通过')
    detailVisible.value = false
    await loadProjects()
  } catch {
  } finally {
    reviewLoading.value = false
  }
}

async function handleBatchApprove() {
  const batchable = selectedProjects.value.filter(p => canReview(p))
  if (batchable.length === 0) {
    ElMessage.warning('所选项目中没有可审核的项目')
    return
  }

  const skipped = selectedProjects.value.length - batchable.length
  let confirmMessage = `确认批量通过 ${batchable.length} 个项目？`
  if (skipped > 0) {
    confirmMessage += `\n（已自动跳过 ${skipped} 个不可审核的项目）`
  }

  try {
    await ElMessageBox.confirm(confirmMessage, '批量审核确认', {
      confirmButtonText: '确认批量通过',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }

  reviewLoading.value = true
  try {
    await batchReviewProjects({
      projectIds: batchable.map(p => p.id),
      action: 'APPROVE'
    })
    ElMessage.success(`已批量通过 ${batchable.length} 个项目`)
    detailVisible.value = false
    await loadProjects()
  } catch {
  } finally {
    reviewLoading.value = false
  }
}

function handleReject(row) {
  rejectProject.value = row
  rejectForm.rejectReason = ''
  rejectDialogVisible.value = true
}

async function confirmReject() {
  const valid = await rejectFormRef.value.validate().catch(() => false)
  if (!valid) return

  reviewLoading.value = true
  try {
    await reviewProject(rejectProject.value.id, { action: 'REJECT', rejectReason: rejectForm.rejectReason })
    ElMessage.success('项目已退回')
    rejectDialogVisible.value = false
    detailVisible.value = false
    await loadProjects()
  } catch {
  } finally {
    reviewLoading.value = false
  }
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
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}
.page-header h2 {
  font-size: 20px;
  color: #303133;
}
.filters-bar {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}
.pagination-wrap {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
</style>
