<template>
  <div class="workspace">
    <div class="ws-header">
      <h2>审批工作台</h2>
      <el-select v-model="stageFilter" placeholder="全部阶段" clearable style="width: 160px" @change="load">
        <el-option label="选题" value="TOPIC" />
        <el-option label="立项" value="PROJECT" />
        <el-option label="中期" value="MIDTERM" />
        <el-option label="结题" value="CONCLUSION" />
      </el-select>
      <el-button @click="load">刷新</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" border stripe>
      <el-table-column label="阶段" width="90">
        <template #default="{ row }"><el-tag>{{ stageLabel(row.stage) }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="name" label="名称" min-width="240" />
      <el-table-column prop="currentStepName" label="当前待办节点" width="180" />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }"><el-button size="small" type="primary" @click="openDetail(row)">审批</el-button></template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && !rows.length && !expertAssignments.length" description="暂无待你审批的事项" />

    <!-- 校/院级专家的顾问评审（管理员在"分配校专"里下发）：与主审批流并行，仅记录意见，不阻塞流程 -->
    <template v-if="expertAssignments.length">
      <div class="section-header">
        <h3>校专顾问评审</h3>
        <span class="section-hint">管理员在"立项管理"中"分配校专"后由你在此提交意见，仅供推荐省级等场景参考，不影响主审批流的通过/驳回</span>
      </div>
      <el-table :data="expertAssignments" border stripe>
        <el-table-column label="阶段" width="90">
          <template #default="{ row }"><el-tag>{{ stageLabel(row.stage) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="projectNo" label="项目编号" width="150" />
        <el-table-column prop="targetName" label="项目名称" min-width="240" />
        <el-table-column label="立项状态" width="100">
          <template #default="{ row }"><el-tag :type="targetStatusType(row.targetStatus)" size="small">{{ targetStatusLabel(row.targetStatus) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="评审状态" width="100">
          <template #default="{ row }">
            <el-tag :type="expertStatusType(row.reviewStatus)" size="small">{{ expertStatusLabel(row.reviewStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button size="small" :type="row.reviewStatus === 'PENDING' ? 'primary' : 'default'" @click="openExpertReview(row)">
              {{ row.reviewStatus === 'PENDING' ? '评审' : '查看' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </template>

    <!-- 首次登录修改密码 -->
    <el-dialog v-model="pwdVisible" title="首次登录，请修改初始密码" width="420px" :close-on-click-modal="false">
      <el-alert type="warning" :closable="false" show-icon style="margin-bottom: 12px"
        title="你的初始密码为工号，为保障账号安全，请立即修改。" />
      <el-form ref="pwdFormRef" :model="pwdForm" label-width="90px" :rules="pwdRules">
        <el-form-item label="原密码" prop="oldPassword"><el-input v-model="pwdForm.oldPassword" type="password" show-password /></el-form-item>
        <el-form-item label="新密码" prop="newPassword"><el-input v-model="pwdForm.newPassword" type="password" show-password /></el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword"><el-input v-model="pwdForm.confirmPassword" type="password" show-password /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdVisible = false">稍后再说</el-button>
        <el-button type="primary" :loading="pwdSaving" @click="doChangePassword">确认修改</el-button>
      </template>
    </el-dialog>


    <!-- 校专顾问评审对话框 -->
    <el-dialog v-model="expertDlgVisible" :title="expertDlg.assignment && expertDlg.assignment.reviewStatus === 'PENDING' ? '校专评审' : '评审详情'" width="620px">
      <div v-if="expertDlg.assignment">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="项目编号">{{ expertDlg.assignment.projectNo || '—' }}</el-descriptions-item>
          <el-descriptions-item label="项目名称">{{ expertDlg.assignment.targetName || '—' }}</el-descriptions-item>
          <el-descriptions-item label="立项状态">
            <el-tag :type="targetStatusType(expertDlg.assignment.targetStatus)" size="small">{{ targetStatusLabel(expertDlg.assignment.targetStatus) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="简介">{{ expertDlg.detail.description || '—' }}</el-descriptions-item>
        </el-descriptions>

        <h4 style="margin: 14px 0 8px">团队成员</h4>
        <el-table :data="expertDlg.detail.members" size="small" border>
          <el-table-column prop="name" label="姓名" />
          <el-table-column prop="studentNo" label="学号" />
          <el-table-column label="角色"><template #default="{ row }">{{ row.memberRole === 'LEADER' ? '负责人' : '成员' }}</template></el-table-column>
        </el-table>

        <h4 style="margin: 14px 0 8px">指导教师</h4>
        <el-table :data="expertDlg.detail.advisors" size="small" border>
          <el-table-column prop="name" label="姓名" />
          <el-table-column prop="title" label="职称" />
        </el-table>

        <h4 style="margin: 14px 0 8px">附件</h4>
        <AttachmentPanel :stage="stagePath(expertDlg.assignment.stage)" :target-id="expertDlg.assignment.targetId" readonly />

        <!-- PENDING：评审表单；已评审：显示历史意见 -->
        <template v-if="expertDlg.assignment.reviewStatus === 'PENDING'">
          <el-divider content-position="left">评审意见与打分</el-divider>
          <el-form label-width="80px">
            <el-form-item label="结论">
              <el-radio-group v-model="expertDlg.action">
                <el-radio value="APPROVED">同意</el-radio>
                <el-radio value="REJECTED">不同意</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="总分" required>
              <el-input-number v-model="expertDlg.score" :min="0" :max="100" :step="1" :controls="false" style="width: 140px" />
              <span class="hint" style="margin-left: 10px">0-100 分；结题阶段：&lt;60 视为不同意；60-74 合格；75-84 良好；≥85 优秀</span>
            </el-form-item>
            <el-form-item label="评审意见">
              <el-input v-model="expertDlg.comment" type="textarea" :rows="3" placeholder="不同意时必填" />
            </el-form-item>
          </el-form>
          <div style="text-align: right">
            <el-button @click="expertDlgVisible = false">取消</el-button>
            <el-button type="primary" :loading="expertDlg.submitting" @click="doExpertReview">提交评审</el-button>
          </div>
        </template>
        <template v-else>
          <el-divider content-position="left">你的评审意见</el-divider>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="结论">
              <el-tag :type="expertStatusType(expertDlg.assignment.reviewStatus)" size="small">{{ expertStatusLabel(expertDlg.assignment.reviewStatus) }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="总分">{{ expertDlg.assignment.score ?? '—' }}</el-descriptions-item>
            <el-descriptions-item label="意见">{{ expertDlg.assignment.reviewComment || '—' }}</el-descriptions-item>
            <el-descriptions-item label="提交时间">{{ expertDlg.assignment.reviewedAt || '—' }}</el-descriptions-item>
          </el-descriptions>
        </template>
      </div>
    </el-dialog>

    <el-drawer v-model="drawerVisible" :title="stageLabel(current.stage) + '审批'" size="560px">
      <div v-if="detail.record" class="detail">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="名称">{{ detail.record.name || detail.record.snapshotName }}</el-descriptions-item>
          <el-descriptions-item label="当前节点">{{ current.currentStepName }}</el-descriptions-item>
          <el-descriptions-item label="简介/内容">{{ detail.record.info || detail.record.description }}</el-descriptions-item>
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
        <AttachmentPanel :stage="stagePath(current.stage)" :target-id="current.id" readonly />

        <h4>审批记录</h4>
        <el-timeline>
          <el-timeline-item v-for="h in detail.history" :key="h.id" :timestamp="h.createdAt" :type="h.action === 'APPROVE' ? 'success' : 'danger'">
            {{ h.stepName }} — {{ h.action === 'APPROVE' ? '通过' : '驳回' }}<span v-if="h.comment">：{{ h.comment }}</span>
          </el-timeline-item>
        </el-timeline>

        <div class="approve-box">
          <el-input v-model="comment" type="textarea" :rows="2" :placeholder="isConclusionExpert ? '评审意见（不同意时必填）' : '审批意见（驳回必填）'" />
          <div class="reject-target">
            <span class="lbl">{{ isConclusionExpert ? '不同意时退回到：' : '驳回退回到：' }}</span>
            <el-select v-model="rejectTarget" size="small" style="width: 220px">
              <el-option :value="0" label="退回申报人（从头重走）" />
              <el-option v-for="n in priorNodes" :key="n.stepOrder" :value="n.stepOrder" :label="'退回：' + n.stepName" />
            </el-select>
          </div>
          <!-- 结题"校专家结题评审"节点：4 档等级；其它节点：普通通过/驳回 -->
          <div v-if="isConclusionExpert" class="approve-actions">
            <el-button type="danger" plain :loading="acting" @click="doGrade('REJECTED')">不同意结题</el-button>
            <el-button type="success" plain :loading="acting" @click="doGrade('PASS')">同意——合格</el-button>
            <el-button type="warning" :loading="acting" @click="doGrade('GOOD')">同意——良好</el-button>
            <el-button type="primary" :loading="acting" @click="doGrade('EXCELLENT')">同意——优秀</el-button>
          </div>
          <div v-else class="approve-actions">
            <el-button type="success" :loading="acting" @click="doApprove">通过</el-button>
            <el-button type="danger" :loading="acting" @click="doReject">驳回</el-button>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../../stores/user'
import AttachmentPanel from '../../components/training/AttachmentPanel.vue'
import {
  myApprovals, getFlow,
  getTopic, approveTopic, rejectTopic,
  getProject, approveProject, rejectProject,
  getReport, approveReport, rejectReport, gradeConclusion,
  myExpertAssignments, submitExpertReview,
  srtpChangePassword, STATUS_LABELS, STATUS_TYPES
} from '../../api/training'

const userStore = useUserStore()

const rows = ref([])
const loading = ref(false)
const stageFilter = ref('')

const drawerVisible = ref(false)
const current = reactive({ stage: '', id: null, batchId: null, currentStepName: '' })
const detail = reactive({ record: null, members: [], advisors: [], history: [] })
const comment = ref('')
const rejectTarget = ref(0)
const priorNodes = ref([])
const acting = ref(false)

const LABELS = { TOPIC: '选题', PROJECT: '立项', MIDTERM: '中期', CONCLUSION: '结题' }
function stageLabel(s) { return LABELS[s] || s }
function stagePath(s) { return s.toLowerCase() }

// 结题的"校专家结题评审"改由专家在下方"校专顾问评审"区打分完成，学校管理员在结题管理页汇总收尾——
// 主审批抽屉里此节点不再直接暴露 4 档按钮
const isConclusionExpert = computed(() => false)

async function load() {
  loading.value = true
  try {
    const res = await myApprovals(stageFilter.value || undefined)
    rows.value = res.data || []
  } finally { loading.value = false }
  loadExpertAssignments()
}

// ===== 校专顾问评审 =====
const expertAssignments = ref([])
const expertDlgVisible = ref(false)
const expertDlg = reactive({ assignment: null, detail: { description: '', members: [], advisors: [] }, action: 'APPROVED', score: null, comment: '', submitting: false })

const EXPERT_STATUS_LABELS = { PENDING: '待评审', APPROVED: '同意', REJECTED: '不同意' }
const EXPERT_STATUS_TYPES = { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger' }
function expertStatusLabel(s) { return EXPERT_STATUS_LABELS[s] || s }
function expertStatusType(s) { return EXPERT_STATUS_TYPES[s] || 'info' }
function targetStatusLabel(s) { return STATUS_LABELS[s] || s || '—' }
function targetStatusType(s) { return STATUS_TYPES[s] || 'info' }

async function loadExpertAssignments() {
  try {
    const res = await myExpertAssignments()
    expertAssignments.value = res.data || []
  } catch { expertAssignments.value = [] }
}

async function openExpertReview(row) {
  expertDlg.assignment = row
  expertDlg.action = 'APPROVED'
  expertDlg.score = null
  expertDlg.comment = ''
  expertDlg.detail = { description: '', members: [], advisors: [] }
  expertDlgVisible.value = true
  // 加载项目/选题/报告的详情用于展示（复用主审批的详情接口）
  try {
    if (row.stage === 'PROJECT') {
      const r = (await getProject(row.targetId)).data
      expertDlg.detail = { description: r.project?.description || '', members: r.members || [], advisors: r.advisors || [] }
    } else if (row.stage === 'TOPIC') {
      const r = (await getTopic(row.targetId)).data
      expertDlg.detail = { description: r.topic?.info || '', members: r.members || [], advisors: r.advisors || [] }
    } else if (row.stage === 'MIDTERM' || row.stage === 'CONCLUSION') {
      const r = (await getReport(row.stage.toLowerCase(), row.targetId)).data
      expertDlg.detail = { description: r.report?.description || '', members: r.members || [], advisors: r.advisors || [] }
    }
  } catch { /* leave empty */ }
}

async function doExpertReview() {
  if (expertDlg.score === null || expertDlg.score === undefined) {
    ElMessage.warning('请填写 0-100 之间的总分'); return
  }
  if (expertDlg.score < 0 || expertDlg.score > 100) {
    ElMessage.warning('总分必须在 0-100 之间'); return
  }
  if (expertDlg.action === 'REJECTED' && !expertDlg.comment.trim()) {
    ElMessage.warning('不同意时必须填写评审意见'); return
  }
  expertDlg.submitting = true
  try {
    await submitExpertReview(expertDlg.assignment.id, expertDlg.action, expertDlg.score, expertDlg.comment)
    ElMessage.success('评审已提交')
    expertDlgVisible.value = false
    loadExpertAssignments()
  } finally { expertDlg.submitting = false }
}

async function openDetail(row) {
  Object.assign(current, { stage: row.stage, id: row.id, batchId: row.batchId, currentStep: row.currentStep, currentStepName: row.currentStepName })
  comment.value = ''
  rejectTarget.value = 0
  priorNodes.value = []
  // load detail by stage
  let d
  if (row.stage === 'TOPIC') { d = (await getTopic(row.id)).data; detail.record = { ...d.topic, name: d.topic.name } }
  else if (row.stage === 'PROJECT') { d = (await getProject(row.id)).data; detail.record = { ...d.project, name: d.project.name } }
  else { d = (await getReport(stagePath(row.stage), row.id)).data; detail.record = { ...d.report, name: d.report.snapshotName } }
  detail.members = d.members || []
  detail.advisors = d.advisors || []
  detail.history = d.history || []
  // prior nodes for reject-to
  const f = await getFlow(row.stage, row.batchId)
  priorNodes.value = (f.data || []).filter(n => n.stepOrder < row.currentStep)
  drawerVisible.value = true
}

async function doApprove() {
  acting.value = true
  try {
    await approveByStage(current.stage, current.id, comment.value)
    ElMessage.success('已通过'); drawerVisible.value = false; load()
  } finally { acting.value = false }
}
async function doReject() {
  if (!comment.value.trim()) { ElMessage.warning('驳回必须填写意见'); return }
  acting.value = true
  try {
    await rejectByStage(current.stage, current.id, comment.value, rejectTarget.value)
    ElMessage.success('已驳回'); drawerVisible.value = false; load()
  } finally { acting.value = false }
}
// 结题"校专家结题评审"专用：REJECTED = 不同意结题(走 reject 通道)，其它 = 通过并写入等级
async function doGrade(grade) {
  if (grade === 'REJECTED') {
    if (!comment.value.trim()) { ElMessage.warning('不同意结题时必须填写评审意见'); return }
    acting.value = true
    try {
      await rejectReport('conclusion', current.id, comment.value, rejectTarget.value)
      ElMessage.success('已驳回，项目退回申报人'); drawerVisible.value = false; load()
    } finally { acting.value = false }
    return
  }
  acting.value = true
  try {
    await gradeConclusion(current.id, grade, comment.value)
    const label = { PASS: '合格', GOOD: '良好', EXCELLENT: '优秀' }[grade] || grade
    ElMessage.success('已通过（结题等级：' + label + '），进入学校最终审核'); drawerVisible.value = false; load()
  } finally { acting.value = false }
}

function approveByStage(stage, id, c) {
  if (stage === 'TOPIC') return approveTopic(id, c)
  if (stage === 'PROJECT') return approveProject(id, c)
  return approveReport(stagePath(stage), id, c)
}
function rejectByStage(stage, id, c, target) {
  if (stage === 'TOPIC') return rejectTopic(id, c, target)
  if (stage === 'PROJECT') return rejectProject(id, c, target)
  return rejectReport(stagePath(stage), id, c, target)
}

// ===== 首次登录改密 =====
const pwdVisible = ref(false)
const pwdSaving = ref(false)
const pwdFormRef = ref(null)
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }, { min: 6, message: '至少6位', trigger: 'blur' }],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: (r, v, cb) => v !== pwdForm.newPassword ? cb(new Error('两次密码不一致')) : cb(), trigger: 'blur' }
  ]
}
async function doChangePassword() {
  const valid = await pwdFormRef.value.validate().catch(() => false)
  if (!valid) return
  pwdSaving.value = true
  try {
    await srtpChangePassword(pwdForm.oldPassword, pwdForm.newPassword)
    userStore.markPasswordChanged()
    ElMessage.success('密码修改成功')
    pwdVisible.value = false
  } finally { pwdSaving.value = false }
}

onMounted(() => {
  if (userStore.user?.mustChangePassword) pwdVisible.value = true
})

load()
</script>

<style scoped>
.ws-header { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.ws-header h2 { margin: 0; flex: 1; }
.detail h4 { margin: 18px 0 8px; }
.approve-box { margin-top: 20px; padding-top: 16px; border-top: 1px solid #eee; }
.reject-target { margin: 10px 0; display: flex; align-items: center; gap: 8px; }
.reject-target .lbl { font-size: 13px; color: #606266; }
.approve-actions { margin-top: 10px; display: flex; gap: 10px; }
.section-header { display: flex; align-items: baseline; gap: 12px; margin: 26px 0 10px; padding-top: 18px; border-top: 1px dashed #dcdfe6; }
.section-header h3 { margin: 0; }
.section-hint { color: #909399; font-size: 12px; }
</style>
