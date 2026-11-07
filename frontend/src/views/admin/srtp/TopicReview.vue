<template>
  <div class="page">
    <div class="page-header">
      <h2>选题管理 / 审批</h2>
      <div>
        <el-button type="primary" @click="openPublish">发布老师题目</el-button>
        <el-button @click="downloadTopicTemplate">下载导入模板</el-button>
        <el-upload :show-file-list="false" :before-upload="handleImport" accept=".xlsx" style="display:inline-block; margin: 0 8px">
          <el-button :loading="importing">批量导入</el-button>
        </el-upload>
        <el-popconfirm :title="`确定删除选中的 ${selectedIds.length} 条选题？`" @confirm="doBatchDelete">
          <template #reference>
            <el-button type="danger" :disabled="!selectedIds.length" :loading="batchDeleting">批量删除（{{ selectedIds.length }}）</el-button>
          </template>
        </el-popconfirm>
        <el-button @click="doExport">导出选题</el-button>
        <el-button @click="doExportUnsubmitted">导出未提交人员</el-button>
      </div>
    </div>

    <el-alert type="info" :closable="false" show-icon style="margin-bottom: 12px"
      title="选题管理用于【老师出题】：发布/导入题目时须指定出题教师(工号)，学生在“我的选题→选择老师题目”中认领；学生选中后立项时指导教师自动为该出题老师。学生自拟题目在学生端“我的选题”自行申报，不在此处管理。" />

    <div class="filters">
      <el-select v-model="batchId" placeholder="选择批次" clearable style="width: 260px" @change="load">
        <el-option v-for="b in batches" :key="b.id" :label="b.name" :value="b.id" />
      </el-select>
      <el-select v-model="status" placeholder="状态" clearable style="width: 130px" @change="load">
        <el-option v-for="(l, k) in STATUS_LABELS" :key="k" :label="l" :value="k" />
      </el-select>
      <el-input v-model="keyword" placeholder="搜索选题名称" clearable style="width: 200px" @keyup.enter="load" @clear="load" />
      <el-button @click="load">查询</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" border stripe @selection-change="onSelectionChange">
      <el-table-column type="selection" width="50" />
      <el-table-column prop="name" label="选题名称" min-width="220" />
      <el-table-column label="来源" width="100">
        <template #default="{ row }">{{ row.source === 'SCHOOL_ASSIGNED' ? '老师出题' : '学生自拟' }}</template>
      </el-table-column>
      <el-table-column label="出题教师" width="110">
        <template #default="{ row }">{{ row.teacherName || '—' }}</template>
      </el-table-column>
      <el-table-column label="类别" width="140">
        <template #default="{ row }">{{ CATEGORY_LABELS[row.projectType] || row.projectType || '—' }}</template>
      </el-table-column>
      <el-table-column label="认领" width="90">
        <template #default="{ row }">
          <el-tag v-if="row.source === 'SCHOOL_ASSIGNED'" size="small" :type="row.claimStatus === 'CLAIMED' ? 'success' : 'info'">
            {{ row.claimStatus === 'CLAIMED' ? '已认领' : '待认领' }}
          </el-tag>
          <span v-else>—</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }"><el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag></template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openDetail(row)">详情</el-button>
          <el-button v-if="row.status === 'IN_REVIEW'" size="small" type="primary" @click="openDetail(row)">审批</el-button>
          <el-popconfirm title="确定删除该选题？" @confirm="doDelete(row)">
            <template #reference><el-button size="small" type="danger">删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      style="margin-top: 16px; justify-content: flex-end"
      layout="total, prev, pager, next"
      :total="total" :current-page="current" :page-size="size"
      @current-change="(p) => { current = p; load() }"
    />

    <el-drawer v-model="drawerVisible" title="选题详情" size="560px">
      <div v-if="detail.topic" class="detail">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="选题名称">{{ detail.topic.name }}</el-descriptions-item>
          <el-descriptions-item label="来源">{{ detail.topic.source === 'SCHOOL_ASSIGNED' ? '老师出题' : '学生自拟' }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.topic.source === 'SCHOOL_ASSIGNED'" label="出题教师">{{ detail.topic.teacherName || '—' }}</el-descriptions-item>
          <el-descriptions-item label="项目类别">{{ categoryLabel(detail.topic.projectType) }}</el-descriptions-item>
          <el-descriptions-item label="创意研究类型">{{ detail.topic.researchType || '—' }}</el-descriptions-item>
          <el-descriptions-item label="状态"><el-tag :type="statusType(detail.topic.status)">{{ statusLabel(detail.topic.status) }}</el-tag></el-descriptions-item>
          <el-descriptions-item v-if="detail.topic.source === 'SCHOOL_ASSIGNED'" label="认领情况">
            <el-tag :type="detail.topic.claimStatus === 'CLAIMED' ? 'success' : 'info'">
              {{ detail.topic.claimStatus === 'CLAIMED' ? ('已被认领：' + (detail.topic.claimedByName || '')) : '待认领' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="选题简介">{{ detail.topic.info || '—' }}</el-descriptions-item>
        </el-descriptions>

        <!-- 老师出题：展示选了该题目的学生团队 -->
        <template v-if="detail.topic.source === 'SCHOOL_ASSIGNED'">
          <h4>选择该题目的学生团队</h4>
          <el-table :data="detail.selections" size="small" border>
            <el-table-column prop="projectName" label="项目名称" min-width="180" />
            <el-table-column prop="leader" label="负责人" width="100" />
            <el-table-column prop="members" label="团队成员" min-width="160" />
            <el-table-column label="立项状态" width="100">
              <template #default="{ row }"><el-tag size="small" :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag></template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!detail.selections.length" description="暂无学生选择该题目" :image-size="50" />
        </template>

        <!-- 学生自拟选题才有团队/指导教师/审批流；老师出题为题库条目，无需展示 -->
        <template v-if="detail.topic.source === 'STUDENT_PROPOSED'">
          <h4>团队成员</h4>
          <el-table :data="detail.members" size="small" border>
            <el-table-column prop="name" label="姓名" />
            <el-table-column prop="studentNo" label="学号" />
            <el-table-column label="角色"><template #default="{ row }">{{ row.memberRole === 'LEADER' ? '负责人' : '成员' }}</template></el-table-column>
          </el-table>

          <h4>指导教师</h4>
          <el-table :data="detail.advisors" size="small" border>
            <el-table-column prop="name" label="姓名" />
            <el-table-column prop="title" label="职称" />
            <el-table-column prop="phone" label="电话" />
          </el-table>

          <h4>附件</h4>
          <AttachmentPanel stage="topic" :target-id="detail.topic.id" readonly />

          <h4>审批记录</h4>
          <el-timeline>
            <el-timeline-item v-for="h in detail.history" :key="h.id" :timestamp="h.createdAt" :type="h.action === 'APPROVE' ? 'success' : 'danger'">
              {{ h.stepName }} — {{ h.action === 'APPROVE' ? '通过' : '驳回' }}
              <span v-if="h.comment">：{{ h.comment }}</span>
            </el-timeline-item>
          </el-timeline>
        </template>

        <div v-if="detail.topic.status === 'IN_REVIEW'" class="approve-box">
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

    <!-- 发布老师题目 -->
    <el-dialog v-model="publishVisible" title="发布老师题目" width="560px">
      <el-form ref="pubFormRef" :model="pubForm" label-width="100px" :rules="pubRules">
        <el-form-item label="所属批次" prop="batchId">
          <el-select v-model="pubForm.batchId" placeholder="请选择批次" style="width: 100%">
            <el-option v-for="b in batches" :key="b.id" :label="b.name" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="出题教师" prop="teacherId">
          <el-select v-model="pubForm.teacherId" filterable remote reserve-keyword
            :remote-method="searchTeachers" :loading="teacherLoading" placeholder="搜索姓名/工号" style="width: 100%">
            <el-option v-for="t in teacherOptions" :key="t.userId" :label="`${t.name}（${t.workId}）`" :value="t.userId" />
          </el-select>
        </el-form-item>
        <el-form-item label="选题名称" prop="name"><el-input v-model="pubForm.name" /></el-form-item>
        <el-form-item label="项目类别" prop="projectType">
          <el-radio-group v-model="pubForm.projectType">
            <el-radio v-for="c in PROJECT_CATEGORIES" :key="c.value" :value="c.value" border>{{ c.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="创意研究类型"><el-input v-model="pubForm.researchType" /></el-form-item>
        <el-form-item label="选题简介"><el-input v-model="pubForm.info" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="publishVisible = false">取消</el-button>
        <el-button type="primary" :loading="publishing" @click="doPublish">发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { adminTopics, getTopic, approveTopic, rejectTopic, listBatches, exportTopics, exportUnsubmitted, getFlow, importTopics, downloadTopicTemplate, publishTopic, getTeacherOptions, adminDeleteTopic, adminDeleteTopicsBatch, PROJECT_CATEGORIES, CATEGORY_LABELS, STATUS_LABELS, STATUS_TYPES } from '../../../api/training'
import AttachmentPanel from '../../../components/training/AttachmentPanel.vue'

const rows = ref([])
const batches = ref([])
const loading = ref(false)
const batchId = ref(''); const status = ref(''); const keyword = ref('')
const current = ref(1); const size = ref(10); const total = ref(0)

const drawerVisible = ref(false)
const detail = reactive({ topic: null, members: [], advisors: [], history: [], selections: [] })
const comment = ref('')
const acting = ref(false)
const rejectTarget = ref(0)
const priorNodes = ref([])

function statusLabel(s) { return STATUS_LABELS[s] || s }
function statusType(s) { return STATUS_TYPES[s] || 'info' }
function categoryLabel(s) { return CATEGORY_LABELS[s] || s || '—' }
async function doDelete(row) {
  await adminDeleteTopic(row.id)
  ElMessage.success('已删除')
  load()
}

const selectedIds = ref([])
const batchDeleting = ref(false)
function onSelectionChange(rows) { selectedIds.value = rows.map(r => r.id) }
async function doBatchDelete() {
  if (!selectedIds.value.length) return
  batchDeleting.value = true
  try {
    const res = await adminDeleteTopicsBatch(selectedIds.value)
    ElMessage.success(res.message || '已删除')
    selectedIds.value = []
    load()
  } finally { batchDeleting.value = false }
}

async function loadBatches() {
  const res = await listBatches({ current: 1, size: 100 })
  batches.value = res.data?.records || []
}
async function load() {
  loading.value = true
  try {
    const res = await adminTopics({ batchId: batchId.value || undefined, status: status.value || undefined, keyword: keyword.value, current: current.value, size: size.value })
    rows.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally { loading.value = false }
}
async function openDetail(row) {
  comment.value = ''
  rejectTarget.value = 0
  priorNodes.value = []
  const res = await getTopic(row.id)
  detail.topic = res.data.topic
  detail.members = res.data.members || []
  detail.advisors = res.data.advisors || []
  detail.history = res.data.history || []
  detail.selections = res.data.selections || []
  if (detail.topic.status === 'IN_REVIEW') {
    const f = await getFlow('TOPIC', detail.topic.batchId)
    priorNodes.value = (f.data || []).filter(n => n.stepOrder < detail.topic.currentStep)
  }
  drawerVisible.value = true
}
async function doApprove() {
  acting.value = true
  try {
    await approveTopic(detail.topic.id, comment.value)
    ElMessage.success('已通过'); drawerVisible.value = false; load()
  } finally { acting.value = false }
}
async function doReject() {
  if (!comment.value.trim()) { ElMessage.warning('驳回必须填写意见'); return }
  acting.value = true
  try {
    await rejectTopic(detail.topic.id, comment.value, rejectTarget.value)
    ElMessage.success('已驳回'); drawerVisible.value = false; load()
  } finally { acting.value = false }
}

function doExport() { exportTopics(batchId.value || undefined) }
function doExportUnsubmitted() {
  if (!batchId.value) { ElMessage.warning('请先选择批次'); return }
  exportUnsubmitted(batchId.value)
}

const importing = ref(false)
async function handleImport(file) {
  if (!batchId.value) { ElMessage.warning('请先在上方选择批次'); return false }
  importing.value = true
  try {
    const res = await importTopics(batchId.value, file)
    ElMessage.success(res.message || '导入完成')
    if (res.data?.errors?.length) ElMessage.warning(res.data.errors.join('；'))
    load()
  } finally { importing.value = false }
  return false
}

// 发布老师题目
const publishVisible = ref(false)
const publishing = ref(false)
const pubFormRef = ref(null)
const teacherOptions = ref([])
const teacherLoading = ref(false)
const pubForm = reactive({ batchId: null, teacherId: null, name: '', projectType: '', researchType: '', info: '' })
const pubRules = {
  batchId: [{ required: true, message: '请选择批次', trigger: 'change' }],
  teacherId: [{ required: true, message: '请选择出题教师', trigger: 'change' }],
  name: [{ required: true, message: '请输入选题名称', trigger: 'blur' }],
  projectType: [{ required: true, message: '请选择项目类别', trigger: 'change' }]
}
function openPublish() {
  Object.assign(pubForm, { batchId: batchId.value || null, teacherId: null, name: '', projectType: '', researchType: '', info: '' })
  publishVisible.value = true
  searchTeachers('')
}
async function searchTeachers(kw) {
  teacherLoading.value = true
  try { teacherOptions.value = (await getTeacherOptions(kw || '')).data || [] }
  finally { teacherLoading.value = false }
}
async function doPublish() {
  const valid = await pubFormRef.value.validate().catch(() => false)
  if (!valid) return
  publishing.value = true
  try {
    await publishTopic({ ...pubForm, source: 'SCHOOL_ASSIGNED' })
    ElMessage.success('已发布')
    publishVisible.value = false
    load()
  } finally { publishing.value = false }
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
</style>
