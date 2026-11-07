<template>
  <div class="page">
    <div class="page-header"><h2>自定义字段配置</h2></div>

    <div class="filters">
      <el-select v-model="stage" style="width: 150px" @change="load">
        <el-option label="选题" value="TOPIC" />
        <el-option label="立项" value="PROJECT" />
        <el-option label="中期" value="MIDTERM" />
        <el-option label="结题" value="CONCLUSION" />
      </el-select>
      <el-select v-model="batchId" placeholder="批次（留空=全局默认）" clearable style="width: 260px" @change="load">
        <el-option v-for="b in batches" :key="b.id" :label="b.name" :value="b.id" />
      </el-select>
      <el-button type="primary" @click="openCreate">新增字段</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" border stripe>
      <el-table-column prop="sortOrder" label="排序" width="70" />
      <el-table-column prop="label" label="显示名" min-width="140" />
      <el-table-column prop="fieldKey" label="字段Key" min-width="140" />
      <el-table-column label="类型" width="120"><template #default="{ row }">{{ typeLabel(row.fieldType) }}</template></el-table-column>
      <el-table-column label="必填" width="80"><template #default="{ row }"><el-tag :type="row.required ? 'danger' : 'info'" size="small">{{ row.required ? '是' : '否' }}</el-tag></template></el-table-column>
      <el-table-column prop="options" label="选项(JSON)" min-width="160" />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-popconfirm title="确定删除该字段？" @confirm="remove(row.id)">
            <template #reference><el-button size="small" type="danger">删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editId ? '编辑字段' : '新增字段'" width="520px">
      <el-form ref="formRef" :model="form" label-width="100px" :rules="rules">
        <el-form-item label="显示名" prop="label"><el-input v-model="form.label" /></el-form-item>
        <el-form-item label="字段Key" prop="fieldKey"><el-input v-model="form.fieldKey" placeholder="英文/数字，唯一" /></el-form-item>
        <el-form-item label="类型" prop="fieldType">
          <el-select v-model="form.fieldType" style="width: 100%">
            <el-option label="单行文本" value="TEXT" />
            <el-option label="多行文本" value="TEXTAREA" />
            <el-option label="数字" value="NUMBER" />
            <el-option label="日期" value="DATE" />
            <el-option label="下拉单选" value="SELECT" />
            <el-option label="下拉多选" value="MULTISELECT" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="['SELECT','MULTISELECT'].includes(form.fieldType)" label="选项">
          <el-input v-model="optionsText" type="textarea" :rows="2" placeholder='每行一个选项，或 JSON 数组' />
        </el-form-item>
        <el-form-item label="必填"><el-switch v-model="form.required" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sortOrder" :controls="false" style="width: 120px" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { listCustomFieldsAdmin, saveCustomField, deleteCustomField, listBatches } from '../../../api/training'

const stage = ref('TOPIC')
const batchId = ref('')
const batches = ref([])
const rows = ref([])
const loading = ref(false)

const dialogVisible = ref(false)
const saving = ref(false)
const editId = ref(null)
const formRef = ref(null)
const form = reactive({ label: '', fieldKey: '', fieldType: 'TEXT', required: false, sortOrder: 0 })
const optionsText = ref('')
const rules = {
  label: [{ required: true, message: '请输入显示名', trigger: 'blur' }],
  fieldKey: [{ required: true, message: '请输入字段Key', trigger: 'blur' }]
}

const TYPE = { TEXT: '单行文本', TEXTAREA: '多行文本', NUMBER: '数字', DATE: '日期', SELECT: '下拉单选', MULTISELECT: '下拉多选' }
function typeLabel(t) { return TYPE[t] || t }

async function loadBatches() {
  const res = await listBatches({ current: 1, size: 100 })
  batches.value = res.data?.records || []
}
async function load() {
  loading.value = true
  try {
    const res = await listCustomFieldsAdmin(stage.value, batchId.value || undefined)
    rows.value = res.data || []
  } finally { loading.value = false }
}

function openCreate() {
  editId.value = null
  Object.assign(form, { label: '', fieldKey: '', fieldType: 'TEXT', required: false, sortOrder: rows.value.length })
  optionsText.value = ''
  dialogVisible.value = true
}
function openEdit(row) {
  editId.value = row.id
  Object.assign(form, { label: row.label, fieldKey: row.fieldKey, fieldType: row.fieldType, required: !!row.required, sortOrder: row.sortOrder })
  optionsText.value = row.options || ''
  dialogVisible.value = true
}
function buildOptions() {
  const t = optionsText.value.trim()
  if (!t) return null
  try {
    const parsed = JSON.parse(t)
    if (Array.isArray(parsed)) return JSON.stringify(parsed)
  } catch { /* not json, treat as lines */ }
  return JSON.stringify(t.split('\n').map(s => s.trim()).filter(Boolean))
}
async function save() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const payload = {
      id: editId.value, stage: stage.value, batchId: batchId.value || null,
      label: form.label, fieldKey: form.fieldKey, fieldType: form.fieldType,
      required: form.required, sortOrder: form.sortOrder,
      options: ['SELECT', 'MULTISELECT'].includes(form.fieldType) ? buildOptions() : null
    }
    await saveCustomField(payload)
    ElMessage.success('已保存')
    dialogVisible.value = false
    load()
  } finally { saving.value = false }
}
async function remove(id) { await deleteCustomField(id); ElMessage.success('已删除'); load() }

loadBatches(); load()
</script>

<style scoped>
.page-header h2 { margin: 0 0 16px; }
.filters { display: flex; gap: 10px; margin-bottom: 16px; }
</style>
