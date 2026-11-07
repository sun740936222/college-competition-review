<template>
  <div class="page">
    <div class="page-header">
      <h2>{{ title }}</h2>
      <el-button type="primary" :disabled="!openBatches.length" @click="openCreate">新建{{ title }}</el-button>
    </div>

    <el-alert v-if="openBatches.length" type="success" show-icon :closable="false" style="margin-bottom: 12px"
      :title="`本批次已开放${title}填报` + (openBatches[0].stageDeadline ? `（截止 ${fmtDeadline(openBatches[0].stageDeadline)}）` : '')" />
    <el-alert v-else-if="!myProjects.length" type="info" show-icon :closable="false" style="margin-bottom: 12px"
      :title="`当前没有可填报${title}的已通过立项：${title}只能针对校级已通过项目填报，请等待你的立项审批通过后再来`" />
    <el-alert v-else type="warning" show-icon :closable="false" style="margin-bottom: 12px"
      :title="`你有 ${myProjects.length} 个已通过立项，但对应批次尚未进入「${title.slice(0,2)}中」状态，请等学校管理员在批次管理里将状态切换为「${title.slice(0,2)}中」后再来填报`" />

    <el-table :data="rows" v-loading="loading" border stripe>
      <el-table-column prop="snapshotName" label="项目名称" min-width="220" />
      <el-table-column prop="studentCount" label="学生数" width="90" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }"><el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag></template>
      </el-table-column>
      <el-table-column label="驳回意见" min-width="150">
        <template #default="{ row }"><span class="reject">{{ row.rejectReason }}</span></template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button v-if="editable(row)" size="small" @click="openEdit(row)">编辑</el-button>
          <el-button v-if="editable(row)" size="small" type="primary" @click="doSubmit(row)">提交</el-button>
          <el-button v-if="row.status === 'DRAFT'" size="small" type="danger" @click="remove(row.id)">删除</el-button>
          <el-button v-if="!editable(row)" size="small" @click="openEdit(row, true)">查看</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      style="margin-top: 16px; justify-content: flex-end"
      layout="total, prev, pager, next"
      :total="total" :current-page="current" :page-size="size"
      @current-change="(p) => { current = p; load() }"
    />

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="680px" top="6vh">
      <el-form ref="formRef" :model="form" label-width="110px" :rules="readonly ? {} : rules" :disabled="readonly">
        <el-form-item label="关联项目" prop="projectId">
          <el-select v-model="form.projectId" placeholder="选择已立项的项目（仅可选批次已开放本阶段的项目）" style="width: 100%" :disabled="!!editId">
            <el-option v-for="p in myProjects" :key="p.id"
              :label="`${p.projectNo || '（未编号）'} · ${p.name} — ${p.batchName || '未知批次'}${p.batchLevel ? '（' + p.batchLevel + '）' : ''}`"
              :value="p.id"
              :disabled="p.batchStatus !== stageOpenStatus" />
          </el-select>
        </el-form-item>
        <el-form-item label="项目类别">
          <span class="derived-value">{{ CATEGORY_LABELS[form.projectType] || form.projectType || '—' }}</span>
          <span class="derived-hint">（从立项继承，无需在此处修改）</span>
        </el-form-item>
        <el-form-item label="参与学生数">
          <span class="derived-value">{{ editId ? (form.members || []).length + ' 人' : '将按立项时的团队成员自动带过来' }}</span>
        </el-form-item>
        <el-form-item :label="title + '内容'">
          <el-input v-model="form.description" type="textarea" :rows="4" />
        </el-form-item>

        <el-divider content-position="left">团队成员</el-divider>
        <template v-if="editId">
          <el-table :data="form.members" size="small" border>
            <el-table-column prop="name" label="姓名" width="120" />
            <el-table-column prop="studentNo" label="学号" width="180" />
            <el-table-column label="角色" width="100">
              <template #default="{ row }">{{ row.memberRole === 'LEADER' ? '负责人' : '成员' }}</template>
            </el-table-column>
          </el-table>
          <div class="inherit-hint">团队成员从立项时的信息继承，不可在此修改</div>
        </template>
        <div v-else class="inherit-hint">保存后将自动从关联立项继承团队成员，无需在此填写</div>

        <el-divider content-position="left">指导教师</el-divider>
        <template v-if="editId">
          <el-table :data="form.advisors" size="small" border>
            <el-table-column prop="name" label="姓名" width="120" />
            <el-table-column prop="title" label="职称" width="120" />
            <el-table-column prop="phone" label="电话" width="160" />
          </el-table>
          <div class="inherit-hint">指导教师从立项时的信息继承，不可在此修改；"指导教师审核"仍由所选教师本人在其审批工作台完成</div>
        </template>
        <div v-else class="inherit-hint">保存后将自动从关联立项继承指导教师，无需在此填写</div>

        <template v-if="form.projectId">
          <el-divider content-position="left">补充信息</el-divider>
          <DynamicFields :stage="stageKey" :batch-id="currentBatchId" v-model="form.extraData" />
        </template>

        <el-divider content-position="left">附件</el-divider>
        <AttachmentPanel v-if="editId" :stage="stagePath" :target-id="editId" :readonly="readonly" />
        <div v-else class="attach-hint">请先点击下方「保存」，保存为草稿后即可在此处上传附件（支持 PDF、Word、Excel、ZIP 等任意格式）</div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ readonly ? '关闭' : '取消' }}</el-button>
        <el-button v-if="!readonly" type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import DynamicFields from '../../components/training/DynamicFields.vue'
import AttachmentPanel from '../../components/training/AttachmentPanel.vue'
import { myReports, createReport, updateReport, deleteReport, submitReport, getReport, myEligibleProjectsForReport, STATUS_LABELS, STATUS_TYPES, CATEGORY_LABELS } from '../../api/training'

const route = useRoute()
const stageKey = computed(() => route.meta.stage)          // MIDTERM | CONCLUSION
const stagePath = computed(() => route.meta.stagePath)     // midterm | conclusion
const title = computed(() => route.meta.title || '报告')
const stageOpenStatus = computed(() => stageKey.value === 'MIDTERM' ? 'MIDTERM_OPEN' : 'CONCLUSION_OPEN')

const rows = ref([])
const loading = ref(false)
const current = ref(1); const size = ref(10); const total = ref(0)
const myProjects = ref([])

const dialogVisible = ref(false)
const readonly = ref(false)
const saving = ref(false)
const editId = ref(null)
const formRef = ref(null)
const form = reactive(emptyForm())
const rules = { projectId: [{ required: true, message: '请选择关联项目', trigger: 'change' }] }

const dialogTitle = computed(() => readonly.value ? (title.value + '详情') : (editId.value ? ('编辑' + title.value) : ('新建' + title.value)))
const currentBatchId = computed(() => myProjects.value.find(p => p.id === form.projectId)?.batchId || null)
// 选中一个立项后，把立项时的项目类别回显到表单里(展示用；真正的 projectType 在后端由 create() 从立项继承)
watch(() => form.projectId, (id) => {
  const p = myProjects.value.find(x => x.id === id)
  if (p && !editId.value) form.projectType = p.projectType || ''
})

function emptyForm() {
  return { projectId: null, projectType: '', studentCount: null, description: '', members: [], advisors: [], extraData: {} }
}
function statusLabel(s) { return STATUS_LABELS[s] || s }
function statusType(s) { return STATUS_TYPES[s] || 'info' }
function editable(row) { return row.status === 'DRAFT' || row.status === 'RETURNED' }
function safeParse(s) { try { return JSON.parse(s) } catch { return {} } }

async function load() {
  loading.value = true
  try {
    const res = await myReports(stagePath.value, { current: current.value, size: size.value })
    rows.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally { loading.value = false }
}
async function loadProjects() {
  const res = await myEligibleProjectsForReport(stageKey.value)
  myProjects.value = res.data || []
}
// 已进入本阶段(中期中/结题中)的批次
const openBatches = computed(() => {
  const seen = new Set()
  const out = []
  for (const p of myProjects.value) {
    if (p.batchStatus === stageOpenStatus.value && p.batchId && !seen.has(p.batchId)) {
      seen.add(p.batchId)
      out.push(p)
    }
  }
  return out
})
function fmtDeadline(d) {
  if (!d) return ''
  return String(d).replace('T', ' ').slice(0, 16)
}

function openCreate() {
  editId.value = null; readonly.value = false
  Object.assign(form, emptyForm())
  dialogVisible.value = true
}
async function loadDetailInto(id) {
  try {
    const res = await getReport(stagePath.value, id)
    const d = res.data || res
    const report = d.report || {}
    form.projectId = report.projectId ?? form.projectId
    form.projectType = report.projectType || form.projectType
    form.description = report.description || ''
    form.extraData = report.extraData ? safeParse(report.extraData) : {}
    form.members = Array.isArray(d.members) ? d.members : []
    form.advisors = Array.isArray(d.advisors) ? d.advisors : []
  } catch (e) {
    console.error('[ReportList] loadDetailInto failed:', e)
    ElMessage.warning('加载团队成员/指导教师失败：' + (e?.message || e))
  }
}
async function openEdit(row, ro = false) {
  editId.value = row.id; readonly.value = ro
  // 先用列表里已有的字段占位，再向服务端拉全量详情（含从立项继承过来的成员/指导教师）
  Object.assign(form, {
    projectId: row.projectId, projectType: row.projectType,
    description: row.description || '', members: [], advisors: [],
    extraData: row.extraData ? safeParse(row.extraData) : {}
  })
  dialogVisible.value = true
  await loadDetailInto(row.id)
}
async function save() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (editId.value) {
      await updateReport(stagePath.value, editId.value, { ...form })
      ElMessage.success('已保存')
    } else {
      const res = await createReport(stagePath.value, { ...form })
      editId.value = res.data.id
      ElMessage.success('已保存为草稿，团队成员和指导教师已从立项继承；可继续上传附件，完成后关闭并在列表点"提交"')
    }
    // 保存后立即把服务端的最新状态（尤其是新继承过来的成员/指导教师）拉回表单展示
    if (editId.value) await loadDetailInto(editId.value)
    load()
  } finally { saving.value = false }
}
async function doSubmit(row) { await submitReport(stagePath.value, row.id); ElMessage.success('已提交'); load() }
async function remove(id) { await deleteReport(stagePath.value, id); ElMessage.success('已删除'); load() }

onMounted(() => { loadProjects(); load() })
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h2 { margin: 0; }
.row-line { display: flex; gap: 8px; margin-bottom: 8px; align-items: center; }
.reject { color: #f56c6c; font-size: 12px; }
.attach-hint { color: #909399; font-size: 13px; }
.derived-value { color: #303133; font-weight: 500; }
.derived-hint { color: #909399; font-size: 12px; margin-left: 8px; }
.inherit-hint { color: #909399; font-size: 12px; margin-top: 6px; }
</style>
