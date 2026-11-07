<template>
  <div class="page">
    <div class="page-header">
      <h2>批次管理</h2>
      <el-button type="primary" @click="openCreate">新建批次</el-button>
    </div>

    <el-input v-model="keyword" placeholder="搜索批次名称" clearable style="width: 260px; margin-bottom: 16px" @keyup.enter="load" @clear="load">
      <template #append><el-button @click="load">搜索</el-button></template>
    </el-input>

    <el-table :data="rows" v-loading="loading" border stripe>
      <el-table-column prop="name" label="批次名称" min-width="220" />
      <el-table-column prop="code" label="编码" width="110" />
      <el-table-column prop="year" label="年度" width="90" />
      <el-table-column prop="level" label="级别" width="100" />
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag>{{ batchStatusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-popconfirm title="确定删除该批次？" @confirm="remove(row.id)">
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

    <el-dialog v-model="dialogVisible" :title="editId ? '编辑批次' : '新建批次'" width="560px">
      <el-form ref="formRef" :model="form" label-width="100px" :rules="rules">
        <el-form-item label="批次名称" prop="name">
          <el-input v-model="form.name" placeholder="如：2026年国家级大学生创新训练计划" />
        </el-form-item>
        <el-form-item label="编码" prop="code">
          <el-input v-model="form.code" placeholder="用于项目编号，如 2026GJ" />
        </el-form-item>
        <el-form-item label="年度">
          <el-input-number v-model="form.year" :controls="false" style="width: 160px" />
        </el-form-item>
        <el-form-item label="级别">
          <el-select v-model="form.level" placeholder="请选择" style="width: 160px">
            <el-option label="校级" value="校级" />
            <el-option label="省级" value="省级" />
            <el-option label="国家级" value="国家级" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 160px">
            <el-option v-for="(l, k) in BATCH_STATUS_LABELS" :key="k" :label="l" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="立项截止">
          <el-date-picker v-model="form.projectDeadline" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" placeholder="截止后学生将无法再提交立项" />
        </el-form-item>
        <el-form-item label="中期截止">
          <el-date-picker v-model="form.midtermDeadline" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" placeholder="截止后学生将无法再提交中期报告" />
        </el-form-item>
        <el-form-item label="结题截止">
          <el-date-picker v-model="form.conclusionDeadline" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" placeholder="截止后学生将无法再提交结题报告" />
        </el-form-item>
      </el-form>
      <p class="tip">提示：新建批次会自动复制默认审批流模板为本批次可独立编辑的副本。</p>
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
import { listBatches, createBatch, updateBatch, deleteBatch, BATCH_STATUS_LABELS } from '../../../api/training'

const rows = ref([])
const loading = ref(false)
const keyword = ref('')
const current = ref(1)
const size = ref(10)
const total = ref(0)

const dialogVisible = ref(false)
const saving = ref(false)
const editId = ref(null)
const formRef = ref(null)
const form = reactive({ name: '', code: '', year: null, level: '', status: 'TOPIC_OPEN', projectDeadline: null, midtermDeadline: null, conclusionDeadline: null })
const rules = { name: [{ required: true, message: '请输入批次名称', trigger: 'blur' }] }

function batchStatusLabel(s) { return BATCH_STATUS_LABELS[s] || s }

async function load() {
  loading.value = true
  try {
    const res = await listBatches({ keyword: keyword.value, current: current.value, size: size.value })
    rows.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editId.value = null
  Object.assign(form, { name: '', code: '', year: new Date().getFullYear(), level: '', status: 'TOPIC_OPEN', projectDeadline: null, midtermDeadline: null, conclusionDeadline: null })
  dialogVisible.value = true
}

function openEdit(row) {
  editId.value = row.id
  Object.assign(form, {
    name: row.name, code: row.code, year: row.year, level: row.level, status: row.status,
    projectDeadline: row.projectDeadline, midtermDeadline: row.midtermDeadline, conclusionDeadline: row.conclusionDeadline
  })
  dialogVisible.value = true
}

async function save() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (editId.value) await updateBatch(editId.value, { ...form })
    else await createBatch({ ...form })
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(id) {
  await deleteBatch(id)
  ElMessage.success('已删除')
  load()
}

load()
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h2 { margin: 0; }
.tip { color: #909399; font-size: 12px; margin: 8px 0 0; }
</style>
