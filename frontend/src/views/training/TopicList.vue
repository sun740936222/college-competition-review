<template>
  <div class="page">
    <div class="page-header">
      <h2>我的选题</h2>
      <div>
        <el-button @click="claimVisible = true">选择老师题目</el-button>
        <el-button type="primary" @click="openCreate">新建选题</el-button>
      </div>
    </div>

    <el-table :data="rows" v-loading="loading" border stripe>
      <el-table-column prop="name" label="选题名称" min-width="220" />
      <el-table-column label="项目类别" width="150">
        <template #default="{ row }">{{ CATEGORY_LABELS[row.projectType] || row.projectType || '—' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }"><el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag></template>
      </el-table-column>
      <el-table-column label="驳回意见" min-width="160">
        <template #default="{ row }"><span class="reject">{{ row.rejectReason }}</span></template>
      </el-table-column>
      <el-table-column label="操作" width="240" fixed="right">
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

    <!-- 编辑/新建选题 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="720px" top="6vh">
      <el-form ref="formRef" :model="form" label-width="110px" :rules="readonly ? {} : rules" :disabled="readonly">
        <el-form-item label="所属批次" prop="batchId">
          <el-select v-model="form.batchId" placeholder="请选择批次" style="width: 100%">
            <el-option v-for="b in batches" :key="b.id" :label="b.name" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="选题名称" prop="name"><el-input v-model="form.name" /></el-form-item>
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
        <el-form-item label="创意研究类型"><el-input v-model="form.researchType" /></el-form-item>
        <el-form-item label="联系方式"><el-input v-model="form.initiatorContact" /></el-form-item>
        <el-form-item label="选题简介"><el-input v-model="form.info" type="textarea" :rows="3" /></el-form-item>

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
          <DynamicFields stage="TOPIC" :batch-id="form.batchId" v-model="form.extraData" />
        </template>

        <el-divider content-position="left">附件</el-divider>
        <AttachmentPanel v-if="editId" stage="topic" :target-id="editId" :readonly="readonly" />
        <div v-else class="attach-hint">请先点击下方「保存」，保存为草稿后即可在此处上传附件（支持 PDF、Word、Excel、ZIP 等任意格式）</div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ readonly ? '关闭' : '取消' }}</el-button>
        <el-button v-if="!readonly" type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- 认领老师出的题目 -->
    <el-dialog v-model="claimVisible" title="选择 / 认领老师题目" width="680px">
      <el-alert type="info" :closable="false" show-icon style="margin-bottom: 10px"
        title="认领后，到“创新训练立项”里选择该题目立项，指导教师会自动设为出题老师。" />
      <el-select v-model="claimBatch" placeholder="按批次筛选" clearable style="width: 260px; margin-bottom: 12px" @change="loadClaimable">
        <el-option v-for="b in batches" :key="b.id" :label="b.name" :value="b.id" />
      </el-select>
      <el-table :data="claimable" border>
        <el-table-column prop="name" label="选题名称" min-width="200" />
        <el-table-column label="出题教师" width="120">
          <template #default="{ row }">{{ row.teacherName || '—' }}</template>
        </el-table-column>
        <el-table-column label="类别" width="140">
          <template #default="{ row }">{{ CATEGORY_LABELS[row.projectType] || row.projectType || '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90">
          <template #default="{ row }"><el-button size="small" type="primary" @click="doClaim(row)">认领</el-button></template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import DynamicFields from '../../components/training/DynamicFields.vue'
import AdvisorEditor from '../../components/training/AdvisorEditor.vue'
import AttachmentPanel from '../../components/training/AttachmentPanel.vue'
import {
  myTopics, createTopic, updateTopic, deleteTopic, submitTopic,
  claimableTopics, claimTopic, getBatchOptions, getDisciplines, getTopic,
  STATUS_LABELS, STATUS_TYPES, PROJECT_CATEGORIES, CATEGORY_LABELS
} from '../../api/training'

const rows = ref([])
const loading = ref(false)
const current = ref(1); const size = ref(10); const total = ref(0)
const batches = ref([])
const disciplines = ref([])

const dialogVisible = ref(false)
const readonly = ref(false)
const saving = ref(false)
const editId = ref(null)
const formRef = ref(null)
const form = reactive(emptyForm())
const rules = {
  batchId: [{ required: true, message: '请选择批次', trigger: 'change' }],
  projectType: [{ required: true, message: '请选择项目类别', trigger: 'change' }],
  name: [{ required: true, message: '请输入选题名称', trigger: 'blur' }]
}

const claimVisible = ref(false)
const claimable = ref([])
const claimBatch = ref('')

const level1 = computed(() => disciplines.value.filter(d => !d.parentId))
const level2 = computed(() => disciplines.value.filter(d => d.parentId === form.disciplineL1Id))
const dialogTitle = computed(() => readonly.value ? '选题详情' : (editId.value ? '编辑选题' : '新建选题'))

function emptyForm() {
  return { batchId: null, name: '', projectType: '', disciplineL1Id: null, disciplineL2Id: null,
    researchType: '', initiatorContact: '', info: '', members: [], advisors: [], extraData: {} }
}
function statusLabel(s) { return STATUS_LABELS[s] || s }
function statusType(s) { return STATUS_TYPES[s] || 'info' }
function editable(row) { return row.status === 'DRAFT' || row.status === 'RETURNED' }

async function load() {
  loading.value = true
  try {
    const res = await myTopics({ current: current.value, size: size.value })
    rows.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally { loading.value = false }
}
async function loadRefs() {
  const [b, d] = await Promise.all([getBatchOptions(), getDisciplines()])
  batches.value = b.data || []
  disciplines.value = d.data || []
}

function openCreate() {
  editId.value = null; readonly.value = false
  Object.assign(form, emptyForm())
  dialogVisible.value = true
}
async function openEdit(row, ro = false) {
  editId.value = row.id; readonly.value = ro
  let members = [], advisors = []
  try {
    const d = (await getTopic(row.id)).data
    members = (d.members || []).map(m => ({ userId: m.userId, name: m.name, studentNo: m.studentNo, memberRole: m.memberRole }))
    advisors = (d.advisors || []).map(a => ({ userId: a.userId, name: a.name, title: a.title, phone: a.phone }))
  } catch {}
  Object.assign(form, {
    batchId: row.batchId, name: row.name, projectType: row.projectType,
    disciplineL1Id: row.disciplineL1Id, disciplineL2Id: row.disciplineL2Id,
    researchType: row.researchType, initiatorContact: row.initiatorContact, info: row.info,
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
    const payload = { ...form }
    if (editId.value) {
      await updateTopic(editId.value, payload)
      ElMessage.success('已保存')
    } else {
      const res = await createTopic(payload)
      editId.value = res.data.id
      ElMessage.success('已保存为草稿，可在下方「附件」上传材料；完成后关闭，并在列表点“提交”')
    }
    load()
  } finally { saving.value = false }
}
async function doSubmit(row) {
  await submitTopic(row.id); ElMessage.success('已提交'); load()
}
async function remove(id) {
  await deleteTopic(id); ElMessage.success('已删除'); load()
}
async function loadClaimable() {
  const res = await claimableTopics(claimBatch.value || undefined)
  claimable.value = res.data || []
}
async function doClaim(row) {
  await claimTopic(row.id)
  ElMessage.success('认领成功')
  loadClaimable(); load()
}

onMounted(() => { loadRefs(); load() })
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h2 { margin: 0; }
.row-line { display: flex; gap: 8px; margin-bottom: 8px; align-items: center; }
.reject { color: #f56c6c; font-size: 12px; }
.attach-hint { color: #909399; font-size: 13px; }
</style>
