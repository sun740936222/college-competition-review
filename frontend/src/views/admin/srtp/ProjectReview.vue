<template>
  <div class="page">
    <div class="page-header">
      <h2>立项管理 / 审批</h2>
      <div>
        <el-button @click="downloadProjectTemplate">下载导入模板</el-button>
        <el-upload :show-file-list="false" :before-upload="handleImport" accept=".xlsx" style="display:inline-block; margin: 0 8px">
          <el-button :loading="importing">批量导入</el-button>
        </el-upload>
        <el-button @click="doExport">导出立项</el-button>
      </div>
    </div>

    <div class="filters">
      <el-select v-model="batchId" placeholder="选择批次" clearable style="width: 260px" @change="load">
        <el-option v-for="b in batches" :key="b.id" :label="b.name" :value="b.id" />
      </el-select>
      <el-select v-model="status" placeholder="状态" clearable style="width: 130px" @change="load">
        <el-option v-for="(l, k) in STATUS_LABELS" :key="k" :label="l" :value="k" />
      </el-select>
      <el-input v-model="keyword" placeholder="搜索项目名称" clearable style="width: 200px" @keyup.enter="load" @clear="load" />
      <el-button @click="load">查询</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" border stripe>
      <el-table-column prop="projectNo" label="项目编号" width="150" />
      <el-table-column prop="name" label="项目名称" min-width="220" />
      <el-table-column prop="studentCount" label="学生数" width="90" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }"><el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag></template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openDetail(row)">详情</el-button>
          <el-button size="small" @click="openAssign(row)">分配校专</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      style="margin-top: 16px; justify-content: flex-end"
      layout="total, prev, pager, next"
      :total="total" :current-page="current" :page-size="size"
      @current-change="(p) => { current = p; load() }"
    />

    <el-drawer v-model="drawerVisible" title="立项详情" size="560px">
      <div v-if="detail.project" class="detail">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="项目编号">{{ detail.project.projectNo || '—' }}</el-descriptions-item>
          <el-descriptions-item label="项目名称">{{ detail.project.name }}</el-descriptions-item>
          <el-descriptions-item label="学生人数">{{ detail.project.studentCount }}</el-descriptions-item>
          <el-descriptions-item label="状态"><el-tag :type="statusType(detail.project.status)">{{ statusLabel(detail.project.status) }}</el-tag></el-descriptions-item>
          <el-descriptions-item label="简介">{{ detail.project.description }}</el-descriptions-item>
        </el-descriptions>

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
        </el-table>

        <h4>附件</h4>
        <AttachmentPanel stage="project" :target-id="detail.project.id" readonly />

        <h4>审批记录</h4>
        <el-timeline>
          <el-timeline-item v-for="h in detail.history" :key="h.id" :timestamp="h.createdAt" :type="h.action === 'APPROVE' ? 'success' : 'danger'">
            {{ h.stepName }} — {{ h.action === 'APPROVE' ? '通过' : '驳回' }}
            <span v-if="h.comment">：{{ h.comment }}</span>
          </el-timeline-item>
        </el-timeline>

        <div v-if="detail.project.status === 'IN_REVIEW'" class="approve-box">
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

    <el-dialog v-model="assignVisible" title="分配校级专家" width="420px">
      <el-select v-model="chosenExpert" placeholder="选择校级专家" filterable style="width: 100%">
        <el-option v-for="e in schoolExperts" :key="e.id" :label="`${e.name}（${e.title || '专家'}）`" :value="e.id" />
      </el-select>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" :loading="acting" @click="doAssign">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { adminProjects, getProject, approveProject, rejectProject, assignExpert, listBatches, listExperts, exportProjects, getFlow, importProjects, downloadProjectTemplate, STATUS_LABELS, STATUS_TYPES } from '../../../api/training'
import AttachmentPanel from '../../../components/training/AttachmentPanel.vue'

const rows = ref([])
const batches = ref([])
const loading = ref(false)
const batchId = ref(''); const status = ref(''); const keyword = ref('')
const current = ref(1); const size = ref(10); const total = ref(0)

const drawerVisible = ref(false)
const detail = reactive({ project: null, members: [], advisors: [], history: [] })
const comment = ref('')
const acting = ref(false)
const rejectTarget = ref(0)
const priorNodes = ref([])

const assignVisible = ref(false)
const schoolExperts = ref([])
const chosenExpert = ref(null)
let assignProjectId = null

function statusLabel(s) { return STATUS_LABELS[s] || s }
function statusType(s) { return STATUS_TYPES[s] || 'info' }

async function loadBatches() {
  const res = await listBatches({ current: 1, size: 100 })
  batches.value = res.data?.records || []
}
async function load() {
  loading.value = true
  try {
    const res = await adminProjects({ batchId: batchId.value || undefined, status: status.value || undefined, keyword: keyword.value, current: current.value, size: size.value })
    rows.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally { loading.value = false }
}
async function openDetail(row) {
  comment.value = ''
  rejectTarget.value = 0
  priorNodes.value = []
  const res = await getProject(row.id)
  detail.project = res.data.project
  detail.members = res.data.members || []
  detail.advisors = res.data.advisors || []
  detail.history = res.data.history || []
  if (detail.project.status === 'IN_REVIEW') {
    const f = await getFlow('PROJECT', detail.project.batchId)
    priorNodes.value = (f.data || []).filter(n => n.stepOrder < detail.project.currentStep)
  }
  drawerVisible.value = true
}
async function doApprove() {
  acting.value = true
  try { await approveProject(detail.project.id, comment.value); ElMessage.success('已通过'); drawerVisible.value = false; load() }
  finally { acting.value = false }
}
async function doReject() {
  if (!comment.value.trim()) { ElMessage.warning('驳回必须填写意见'); return }
  acting.value = true
  try { await rejectProject(detail.project.id, comment.value, rejectTarget.value); ElMessage.success('已驳回'); drawerVisible.value = false; load() }
  finally { acting.value = false }
}
async function openAssign(row) {
  assignProjectId = row.id
  chosenExpert.value = null
  const res = await listExperts({ level: 'SCHOOL', status: 'APPROVED', current: 1, size: 200 })
  schoolExperts.value = res.data?.records || []
  assignVisible.value = true
}
async function doAssign() {
  if (!chosenExpert.value) { ElMessage.warning('请选择专家'); return }
  acting.value = true
  try { await assignExpert(assignProjectId, chosenExpert.value); ElMessage.success('已分配'); assignVisible.value = false }
  finally { acting.value = false }
}

function doExport() { exportProjects(batchId.value || undefined) }

const importing = ref(false)
async function handleImport(file) {
  if (!batchId.value) { ElMessage.warning('请先在上方选择批次'); return false }
  importing.value = true
  try {
    const res = await importProjects(batchId.value, file)
    ElMessage.success(res.message || '导入完成')
    if (res.data?.errors?.length) ElMessage.warning(res.data.errors.join('；'))
    load()
  } finally { importing.value = false }
  return false
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
