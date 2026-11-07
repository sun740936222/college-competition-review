<template>
  <div class="page">
    <div class="page-header">
      <h2>{{ title }}管理 / 审批</h2>
      <div>
        <el-button @click="doExport">导出{{ title }}</el-button>
        <el-button v-if="stagePath === 'conclusion'" type="primary" @click="doExportGrades">导出结题等级汇总</el-button>
      </div>
    </div>

    <div class="filters">
      <el-select v-model="batchId" placeholder="选择批次" clearable style="width: 240px" @change="load">
        <el-option v-for="b in batches" :key="b.id" :label="b.name" :value="b.id" />
      </el-select>
      <el-select v-model="status" placeholder="状态" clearable style="width: 130px" @change="load">
        <el-option v-for="(l, k) in STATUS_LABELS" :key="k" :label="l" :value="k" />
      </el-select>
      <el-input v-model="keyword" placeholder="搜索项目名称" clearable style="width: 200px" @keyup.enter="load" @clear="load" />
      <el-button @click="load">查询</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" border stripe>
      <el-table-column prop="snapshotName" label="项目名称" min-width="220" />
      <el-table-column prop="studentCount" label="学生数" width="90" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }"><el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag></template>
      </el-table-column>
      <el-table-column v-if="stagePath === 'conclusion'" label="结题等级" width="150">
        <template #default="{ row }"><el-tag v-if="row.conclusionGrade" :type="gradeTagType(row.conclusionGrade)" size="small">{{ gradeLabel(row.conclusionGrade) }}</el-tag><span v-else class="muted">—</span></template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openDetail(row)">详情/审批</el-button>
          <el-button v-if="stagePath === 'conclusion'" size="small" type="primary" @click="openAssign(row)">分配评审专家</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      style="margin-top: 16px; justify-content: flex-end"
      layout="total, prev, pager, next"
      :total="total" :current-page="current" :page-size="size"
      @current-change="(p) => { current = p; load() }"
    />

    <el-drawer v-model="drawerVisible" :title="title + '详情'" size="560px">
      <div v-if="detail.report" class="detail">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="项目名称">{{ detail.report.snapshotName }}</el-descriptions-item>
          <el-descriptions-item label="学生人数">{{ detail.report.studentCount }}</el-descriptions-item>
          <el-descriptions-item label="状态"><el-tag :type="statusType(detail.report.status)">{{ statusLabel(detail.report.status) }}</el-tag></el-descriptions-item>
          <el-descriptions-item v-if="stagePath === 'conclusion'" label="结题等级">
            <el-tag v-if="detail.report.conclusionGrade" :type="gradeTagType(detail.report.conclusionGrade)" size="small">{{ gradeLabel(detail.report.conclusionGrade) }}</el-tag>
            <span v-else class="muted">— (校专家尚未评审)</span>
          </el-descriptions-item>
          <el-descriptions-item :label="title + '内容'">{{ detail.report.description }}</el-descriptions-item>
        </el-descriptions>

        <h4>团队成员</h4>
        <el-table :data="detail.members" size="small" border>
          <el-table-column prop="name" label="姓名" /><el-table-column prop="studentNo" label="学号" />
          <el-table-column label="角色"><template #default="{ row }">{{ row.memberRole === 'LEADER' ? '负责人' : '成员' }}</template></el-table-column>
        </el-table>

        <h4>指导教师</h4>
        <el-table :data="detail.advisors" size="small" border>
          <el-table-column prop="name" label="姓名" /><el-table-column prop="title" label="职称" />
        </el-table>

        <h4>附件</h4>
        <AttachmentPanel :stage="stagePath" :target-id="detail.report.id" readonly />

        <template v-if="stagePath === 'conclusion'">
          <h4>已指派评审专家</h4>
          <el-table :data="detail.expertAssignments || []" size="small" border>
            <el-table-column prop="expertName" label="专家" width="120" />
            <el-table-column prop="expertTitle" label="职称" width="120" />
            <el-table-column label="级别" width="80">
              <template #default="{ row }">{{ row.expertLevel === 'SCHOOL' ? '校级' : (row.expertLevel === 'COLLEGE' ? '院级' : row.expertLevel) }}</template>
            </el-table-column>
            <el-table-column label="打分" width="80">
              <template #default="{ row }"><span :class="scoreClass(row.score)">{{ row.score ?? '—' }}</span></template>
            </el-table-column>
            <el-table-column label="结论" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="assignStatusType(row.reviewStatus)">{{ assignStatusLabel(row.reviewStatus) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="意见" min-width="120">
              <template #default="{ row }">{{ row.reviewComment || '—' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="90">
              <template #default="{ row }">
                <el-popconfirm v-if="row.reviewStatus === 'PENDING'" title="撤销该专家的评审指派？" @confirm="doRemoveAssignment(row)">
                  <template #reference><el-button size="small" text type="danger">撤销</el-button></template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!detail.expertAssignments?.length" description="尚未指派评审专家；请先点上方「分配评审专家」指派校专" :image-size="50" />

          <!-- 汇总提示 + 学校管理员收尾按钮 -->
          <div v-if="detail.report.currentStepName?.includes('校专家') || (detail.expertAssignments || []).some(a => a.reviewStatus !== 'PENDING')" class="finalize-box">
            <div class="finalize-summary">
              <span>已评审 {{ finishedCount }} / {{ (detail.expertAssignments || []).length }} 位专家</span>
              <span v-if="finishedCount > 0">，平均分 <b>{{ avgScoreDisplay }}</b></span>
              <span v-if="finishedCount > 0">，预计等级：<el-tag size="small" :type="gradeTagType(derivedGrade)">{{ derivedGrade === null ? '不同意结题' : gradeLabel(derivedGrade) }}</el-tag></span>
            </div>
            <div v-if="isAtConclusionExpertStep" class="finalize-hint">
              收尾规则：任一专家给"不同意"或平均分 &lt; 60 → 整体驳回申报人；60-74 合格；75-84 良好；≥85 优秀
            </div>
            <el-input v-if="isAtConclusionExpertStep" v-model="finalizeComment" type="textarea" :rows="2" placeholder="收尾意见（可选，将附在审批时间线上）" style="margin-top: 8px" />
            <div v-if="isAtConclusionExpertStep" style="margin-top: 8px">
              <el-button type="primary" :loading="finalizing" :disabled="finishedCount === 0" @click="doFinalize">结束校专家评审 & 推进/驳回</el-button>
            </div>
          </div>
        </template>

        <h4>审批记录</h4>
        <el-timeline>
          <el-timeline-item v-for="h in detail.history" :key="h.id" :timestamp="h.createdAt" :type="h.action === 'APPROVE' ? 'success' : 'danger'">
            {{ h.stepName }} — {{ h.action === 'APPROVE' ? '通过' : '驳回' }}<span v-if="h.comment">：{{ h.comment }}</span>
          </el-timeline-item>
        </el-timeline>

        <div v-if="detail.report.status === 'IN_REVIEW' && !isAtConclusionExpertStep" class="approve-box">
          <el-input v-model="comment" type="textarea" :rows="2" placeholder="审批意见（驳回必填）" />
          <div class="reject-target">
            <span class="lbl">驳回退回到：</span>
            <el-select v-model="rejectTarget" size="small" style="width: 220px">
              <el-option :value="0" label="退回申报人（从头重走）" />
              <el-option v-for="n in priorNodes" :key="n.stepOrder" :value="n.stepOrder" :label="'退回：' + n.stepName" />
            </el-select>
          </div>
          <div class="approve-actions">
            <el-button type="success" :loading="acting" @click="doApprove">通过</el-button>
            <el-button type="danger" :loading="acting" @click="doReject">驳回</el-button>
          </div>
        </div>
      </div>
    </el-drawer>

    <el-dialog v-model="assignVisible" title="分配结题评审专家" width="520px">
      <p class="hint">被指派的专家会在其审批工作台看到本记录，并给出 4 档评审结论(不同意 / 合格 / 良好 / 优秀)；未被指派的校专家看不到本项目。</p>
      <el-select v-model="chosenExpert" filterable placeholder="选择校级专家" style="width: 100%">
        <el-option v-for="e in schoolExperts" :key="e.id"
          :label="`${e.name}${e.title ? '（' + e.title + '）' : ''}`"
          :value="e.id"
          :disabled="assignedExpertIds.includes(e.id)" />
      </el-select>
      <div class="assigned-summary" v-if="assignedExpertIds.length">已指派 {{ assignedExpertIds.length }} 位专家；同一位专家不可重复指派</div>
      <template #footer>
        <el-button @click="assignVisible = false">关闭</el-button>
        <el-button type="primary" :loading="assigning" @click="doAssign">确认指派</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { adminReports, getReport, approveReport, rejectReport, listBatches, listExperts, exportReports, exportConclusionGrades, getFlow, assignReportExpert, listReportAssignments, removeReportAssignment, finalizeConclusionExpertReview, STATUS_LABELS, STATUS_TYPES, CONCLUSION_GRADE_LABELS } from '../../../api/training'
import AttachmentPanel from '../../../components/training/AttachmentPanel.vue'

const route = useRoute()
const stagePath = computed(() => route.meta.stagePath)
const title = computed(() => route.meta.title || '报告')

const rows = ref([]); const batches = ref([]); const loading = ref(false)
const batchId = ref(''); const status = ref(''); const keyword = ref('')
const current = ref(1); const size = ref(10); const total = ref(0)

const drawerVisible = ref(false)
const detail = reactive({ report: null, members: [], advisors: [], history: [], expertAssignments: [] })
const comment = ref(''); const acting = ref(false)
const rejectTarget = ref(0)
const priorNodes = ref([])

// 分配评审专家对话框
const assignVisible = ref(false)
const assigning = ref(false)
const schoolExperts = ref([])
const chosenExpert = ref(null)
const currentAssignReportId = ref(null)
const assignedExpertIds = ref([])
const ASSIGN_STATUS_LABEL = { PENDING: '待评审', APPROVED: '已通过', REJECTED: '已不同意' }
const ASSIGN_STATUS_TYPE = { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger' }
function assignStatusLabel(s) { return ASSIGN_STATUS_LABEL[s] || s || '—' }
function assignStatusType(s) { return ASSIGN_STATUS_TYPE[s] || 'info' }

function statusLabel(s) { return STATUS_LABELS[s] || s }
function statusType(s) { return STATUS_TYPES[s] || 'info' }

async function loadBatches() {
  const res = await listBatches({ current: 1, size: 100 })
  batches.value = res.data?.records || []
}
async function load() {
  loading.value = true
  try {
    const res = await adminReports(stagePath.value, { batchId: batchId.value || undefined, status: status.value || undefined, keyword: keyword.value, current: current.value, size: size.value })
    rows.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally { loading.value = false }
}
async function openDetail(row) {
  comment.value = ''
  rejectTarget.value = 0
  priorNodes.value = []
  finalizeComment.value = ''
  const res = await getReport(stagePath.value, row.id)
  detail.report = res.data.report
  detail.members = res.data.members || []
  detail.advisors = res.data.advisors || []
  detail.history = res.data.history || []
  detail.expertAssignments = res.data.expertAssignments || []
  if (detail.report.status === 'IN_REVIEW') {
    const f = await getFlow(stagePath.value.toUpperCase(), detail.report.batchId)
    const nodes = f.data || []
    priorNodes.value = nodes.filter(n => n.stepOrder < detail.report.currentStep)
    const cur = nodes.find(n => n.stepOrder === detail.report.currentStep)
    detail.report.currentStepName = cur ? cur.stepName : ''
  }
  drawerVisible.value = true
}

async function openAssign(row) {
  currentAssignReportId.value = row.id
  chosenExpert.value = null
  // 拉可选的校级专家(状态 APPROVED)
  const experts = await listExperts({ level: 'SCHOOL', status: 'APPROVED', current: 1, size: 500 })
  schoolExperts.value = experts.data?.records || []
  // 拉当前已指派名单，用于禁用重复
  const assignments = await listReportAssignments(stagePath.value, row.id)
  assignedExpertIds.value = (assignments.data || []).map(a => a.expertId)
  assignVisible.value = true
}

async function doAssign() {
  if (!chosenExpert.value) { ElMessage.warning('请选择专家'); return }
  assigning.value = true
  try {
    await assignReportExpert(stagePath.value, currentAssignReportId.value, chosenExpert.value)
    ElMessage.success('已指派')
    // 刷新已指派列表以便再指派下一位
    const assignments = await listReportAssignments(stagePath.value, currentAssignReportId.value)
    assignedExpertIds.value = (assignments.data || []).map(a => a.expertId)
    chosenExpert.value = null
  } finally { assigning.value = false }
}

async function doRemoveAssignment(row) {
  await removeReportAssignment(row.id)
  ElMessage.success('已撤销')
  // 刷新详情里的指派列表
  const assignments = await listReportAssignments(stagePath.value, detail.report.id)
  detail.expertAssignments = assignments.data || []
}
async function doApprove() {
  acting.value = true
  try { await approveReport(stagePath.value, detail.report.id, comment.value); ElMessage.success('已通过'); drawerVisible.value = false; load() }
  finally { acting.value = false }
}
async function doReject() {
  if (!comment.value.trim()) { ElMessage.warning('驳回必须填写意见'); return }
  acting.value = true
  try { await rejectReport(stagePath.value, detail.report.id, comment.value, rejectTarget.value); ElMessage.success('已驳回'); drawerVisible.value = false; load() }
  finally { acting.value = false }
}

function doExport() { exportReports(stagePath.value, batchId.value || undefined) }
function doExportGrades() { exportConclusionGrades(batchId.value || undefined) }
function gradeLabel(g) { return CONCLUSION_GRADE_LABELS[g] || g }
function gradeTagType(g) { return g === 'EXCELLENT' ? 'primary' : g === 'GOOD' ? 'warning' : g === null ? 'danger' : 'success' }
function scoreClass(s) {
  if (s === null || s === undefined) return 'muted'
  if (s >= 85) return 'score-excellent'
  if (s >= 75) return 'score-good'
  if (s >= 60) return 'score-pass'
  return 'score-fail'
}

// ==== 收尾"校专家结题评审"相关 ====
const finalizeComment = ref('')
const finalizing = ref(false)
const finishedCount = computed(() => (detail.expertAssignments || []).filter(a => a.reviewStatus !== 'PENDING').length)
const avgScore = computed(() => {
  const done = (detail.expertAssignments || []).filter(a => a.reviewStatus !== 'PENDING' && a.score != null)
  if (!done.length) return null
  return done.reduce((s, a) => s + a.score, 0) / done.length
})
const avgScoreDisplay = computed(() => avgScore.value == null ? '—' : avgScore.value.toFixed(1))
const derivedGrade = computed(() => {
  const done = (detail.expertAssignments || []).filter(a => a.reviewStatus !== 'PENDING' && a.score != null)
  if (!done.length) return null
  const anyReject = done.some(a => a.reviewStatus === 'REJECTED')
  const avg = done.reduce((s, a) => s + a.score, 0) / done.length
  if (anyReject || avg < 60) return null
  if (avg >= 85) return 'EXCELLENT'
  if (avg >= 75) return 'GOOD'
  return 'PASS'
})
const isAtConclusionExpertStep = computed(() =>
  stagePath.value === 'conclusion' && (detail.report?.currentStepName?.includes('校专家') || false)
)
async function doFinalize() {
  if (finishedCount.value === 0) { ElMessage.warning('至少需要一位专家完成评审后才能收尾'); return }
  finalizing.value = true
  try {
    const res = await finalizeConclusionExpertReview(detail.report.id, finalizeComment.value)
    ElMessage.success(res.message || '已收尾')
    drawerVisible.value = false
    load()
  } finally { finalizing.value = false }
}

loadBatches(); load()
</script>

<style scoped>
.page-header h2 { margin: 0 0 16px; }
.filters { display: flex; gap: 10px; margin-bottom: 16px; }
.detail h4 { margin: 18px 0 8px; }
.approve-box { margin-top: 20px; padding-top: 16px; border-top: 1px solid #eee; }
.reject-target { margin: 10px 0; display: flex; align-items: center; gap: 8px; }
.reject-target .lbl { font-size: 13px; color: #606266; }
.approve-actions { margin-top: 10px; display: flex; gap: 10px; }
.muted { color: #909399; }
.hint { color: #909399; font-size: 12px; margin-bottom: 8px; }
.assigned-summary { color: #909399; font-size: 12px; margin-top: 8px; }
.finalize-box { margin-top: 12px; padding: 12px; background: #f5f7fa; border-radius: 4px; }
.finalize-summary { font-size: 13px; color: #303133; }
.finalize-hint { color: #909399; font-size: 12px; margin-top: 6px; }
.score-excellent { color: #409eff; font-weight: 600; }
.score-good { color: #e6a23c; font-weight: 600; }
.score-pass { color: #67c23a; font-weight: 600; }
.score-fail { color: #f56c6c; font-weight: 600; }
</style>
