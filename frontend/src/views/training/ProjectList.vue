<template>
  <div class="page">
    <div class="page-header">
      <h2>我的立项</h2>
      <el-button type="primary" @click="openCreate">新建立项</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" border stripe>
      <el-table-column prop="projectNo" label="项目编号" width="150" />
      <el-table-column prop="name" label="项目名称" min-width="220" />
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="720px" top="6vh">
      <el-form ref="formRef" :model="form" label-width="110px" :rules="readonly ? {} : rules" :disabled="readonly">
        <el-form-item v-if="!editId" label="来源">
          <el-radio-group v-model="sourceMode" @change="onSourceModeChange">
            <el-radio value="SELF">自主立项</el-radio>
            <el-radio value="TOPIC">选择老师题目立项</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="!editId && sourceMode === 'TOPIC'" label="老师题目">
          <el-select v-model="form.topicId" placeholder="选择我已认领的老师题目" style="width: 100%" @change="onPickTopic">
            <el-option v-for="t in claimedTopics" :key="t.id" :label="`${t.name}（出题：${t.teacherName || '—'}）`" :value="t.id" />
          </el-select>
          <div v-if="pickedTeacherName" class="src-tip">指导教师将自动设为出题老师：<b>{{ pickedTeacherName }}</b></div>
        </el-form-item>
        <el-form-item label="所属批次" prop="batchId">
          <el-select v-model="form.batchId" placeholder="请选择批次" style="width: 100%">
            <el-option v-for="b in batches" :key="b.id" :label="b.name" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="项目名称" prop="name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="项目类别" prop="projectType">
          <el-radio-group v-model="form.projectType">
            <el-radio v-for="c in PROJECT_CATEGORIES" :key="c.value" :value="c.value" border>{{ c.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="一级学科">
          <el-select v-model="form.disciplineL1Id" placeholder="请选择" style="width: 100%" @change="form.disciplineL2Id = null">
            <el-option v-for="d in level1" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="二级学科">
          <el-select v-model="form.disciplineL2Id" placeholder="请选择" style="width: 100%">
            <el-option v-for="d in level2" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="参与学生数">
          <span class="derived-value">{{ (form.members || []).length }} 人</span>
          <span class="derived-hint">（自动按下方“团队成员”实际人数计算，无需手填）</span>
        </el-form-item>
        <el-form-item label="项目简介"><el-input v-model="form.description" type="textarea" :rows="3" /></el-form-item>

        <el-divider content-position="left">团队成员</el-divider>
        <div v-for="(m, i) in form.members" :key="i" class="row-line">
          <el-input v-model="m.name" placeholder="姓名" style="width: 120px" />
          <el-input v-model="m.studentNo" placeholder="学号" style="width: 140px" />
          <el-select v-model="m.memberRole" style="width: 110px">
            <el-option label="负责人" value="LEADER" />
            <el-option label="成员" value="MEMBER" />
          </el-select>
          <el-button v-if="!readonly" text type="danger" @click="form.members.splice(i, 1)">删除</el-button>
        </div>
        <el-button v-if="!readonly" size="small" @click="form.members.push({ name: '', studentNo: '', memberRole: 'MEMBER' })">+ 添加成员</el-button>

        <el-divider content-position="left">指导教师</el-divider>
        <AdvisorEditor v-model="form.advisors" :readonly="readonly" />

        <template v-if="form.batchId">
          <el-divider content-position="left">补充信息</el-divider>
          <DynamicFields stage="PROJECT" :batch-id="form.batchId" v-model="form.extraData" />
        </template>

        <el-divider content-position="left">附件</el-divider>
        <AttachmentPanel v-if="editId" stage="project" :target-id="editId" :readonly="readonly" />
        <div v-else class="attach-hint">请先点击下方「保存」，保存为草稿后即可在此处上传项目申报书等附件（支持 PDF、Word、Excel、ZIP 等任意格式）</div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ readonly ? '关闭' : '取消' }}</el-button>
        <el-button v-if="!readonly" type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import DynamicFields from '../../components/training/DynamicFields.vue'
import AttachmentPanel from '../../components/training/AttachmentPanel.vue'
import AdvisorEditor from '../../components/training/AdvisorEditor.vue'
import {
  createProject, updateProject, deleteProject, submitProject,
  getBatchOptions, getDisciplines, myClaimedTopics,
  STATUS_LABELS, STATUS_TYPES, PROJECT_CATEGORIES, CATEGORY_LABELS
} from '../../api/training'
import api from '../../api/index'

const rows = ref([])
const loading = ref(false)
const current = ref(1); const size = ref(10); const total = ref(0)
const batches = ref([])
const disciplines = ref([])
const claimedTopics = ref([])
const sourceMode = ref('SELF')
const pickedTeacherName = ref('')

const dialogVisible = ref(false)
const readonly = ref(false)
const saving = ref(false)
const editId = ref(null)
const formRef = ref(null)
const form = reactive(emptyForm())
const rules = {
  batchId: [{ required: true, message: '请选择批次', trigger: 'change' }],
  projectType: [{ required: true, message: '请选择项目类别', trigger: 'change' }],
  name: [{ required: true, message: '请输入项目名称', trigger: 'blur' }]
}

const level1 = computed(() => disciplines.value.filter(d => !d.parentId))
const level2 = computed(() => disciplines.value.filter(d => d.parentId === form.disciplineL1Id))
const dialogTitle = computed(() => readonly.value ? '立项详情' : (editId.value ? '编辑立项' : '新建立项'))

function emptyForm() {
  return { batchId: null, topicId: null, projectType: '', name: '', disciplineL1Id: null, disciplineL2Id: null,
    studentCount: null, description: '', members: [], advisors: [], extraData: {} }
}
function statusLabel(s) { return STATUS_LABELS[s] || s }
function statusType(s) { return STATUS_TYPES[s] || 'info' }
function editable(row) { return row.status === 'DRAFT' || row.status === 'RETURNED' }

async function load() {
  loading.value = true
  try {
    // 学生查看自己的立项：复用 mine 接口
    const res = await api.get('/srtp/projects/mine', { params: { current: current.value, size: size.value } })
    rows.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally { loading.value = false }
}
async function loadRefs() {
  const [b, d, ct] = await Promise.all([getBatchOptions(), getDisciplines(), myClaimedTopics()])
  batches.value = b.data || []
  disciplines.value = d.data || []
  claimedTopics.value = ct.data || []
}

function onSourceModeChange() {
  pickedTeacherName.value = ''
  form.topicId = null
}
function onPickTopic(topicId) {
  const t = claimedTopics.value.find(x => x.id === topicId)
  if (!t) return
  form.batchId = t.batchId || form.batchId
  form.name = t.name
  form.projectType = t.projectType
  form.disciplineL1Id = t.disciplineL1Id
  form.disciplineL2Id = t.disciplineL2Id
  pickedTeacherName.value = t.teacherName || ''
  // 预置指导教师=出题老师（后端也会强制），学生可再补充其它成员
  form.advisors = t.teacherId ? [{ userId: t.teacherId, name: t.teacherName, title: '', phone: '' }] : []
}

function openCreate() {
  editId.value = null; readonly.value = false
  sourceMode.value = 'SELF'; pickedTeacherName.value = ''
  Object.assign(form, emptyForm())
  dialogVisible.value = true
}
async function openEdit(row, ro = false) {
  editId.value = row.id; readonly.value = ro
  let members = [], advisors = []
  try {
    const d = (await api.get(`/srtp/projects/${row.id}`)).data
    members = (d.members || []).map(m => ({ userId: m.userId, name: m.name, studentNo: m.studentNo, memberRole: m.memberRole }))
    advisors = (d.advisors || []).map(a => ({ userId: a.userId, name: a.name, title: a.title, phone: a.phone }))
  } catch {}
  Object.assign(form, {
    batchId: row.batchId, topicId: row.topicId, projectType: row.projectType, name: row.name,
    disciplineL1Id: row.disciplineL1Id, disciplineL2Id: row.disciplineL2Id,
    studentCount: row.studentCount, description: row.description,
    members, advisors, extraData: row.extraData ? safeParse(row.extraData) : {}
  })
  dialogVisible.value = true
}
function safeParse(s) { try { return JSON.parse(s) } catch { return {} } }

async function save() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (editId.value) {
      await updateProject(editId.value, { ...form })
      ElMessage.success('已保存')
    } else {
      const res = await createProject({ ...form })
      editId.value = res.data.id
      ElMessage.success('已保存为草稿，可在下方「附件」上传项目申报书等材料；完成后关闭，并在列表点“提交”')
    }
    load()
  } finally { saving.value = false }
}
async function doSubmit(row) { await submitProject(row.id); ElMessage.success('已提交'); load() }
async function remove(id) { await deleteProject(id); ElMessage.success('已删除'); load() }

onMounted(() => { loadRefs(); load() })
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h2 { margin: 0; }
.row-line { display: flex; gap: 8px; margin-bottom: 8px; align-items: center; }
.reject { color: #f56c6c; font-size: 12px; }
.src-tip { color: #409eff; font-size: 12px; margin-top: 4px; }
.attach-hint { color: #909399; font-size: 13px; }
.derived-value { color: #303133; font-weight: 500; }
.derived-hint { color: #909399; font-size: 12px; margin-left: 8px; }
</style>
