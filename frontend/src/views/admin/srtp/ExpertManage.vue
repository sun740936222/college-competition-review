<template>
  <div class="page">
    <div class="page-header">
      <h2>专家组管理</h2>
      <el-button type="primary" @click="openCreate">添加专家</el-button>
    </div>

    <div class="filters">
      <el-input v-model="keyword" placeholder="搜索姓名" clearable style="width: 200px" @keyup.enter="load" @clear="load" />
      <el-select v-model="level" placeholder="级别" clearable style="width: 120px" @change="load">
        <el-option label="院级" value="COLLEGE" />
        <el-option label="校级" value="SCHOOL" />
      </el-select>
      <el-select v-model="status" placeholder="状态" clearable style="width: 130px" @change="load">
        <el-option label="待审核" value="PENDING" />
        <el-option label="已通过" value="APPROVED" />
        <el-option label="已拒绝" value="REJECTED" />
      </el-select>
      <el-button @click="load">查询</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" border stripe>
      <el-table-column prop="name" label="姓名" width="110" />
      <el-table-column prop="title" label="职称" width="110" />
      <el-table-column prop="researchField" label="研究方向" min-width="160" />
      <el-table-column prop="phone" label="电话" width="130" />
      <el-table-column label="级别" width="90">
        <template #default="{ row }"><el-tag>{{ row.expertLevel === 'SCHOOL' ? '校级' : '院级' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="来源" width="100">
        <template #default="{ row }">{{ sourceLabel(row.source) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="230" fixed="right">
        <template #default="{ row }">
          <template v-if="row.status === 'PENDING'">
            <el-button size="small" type="success" @click="doReview(row, true)">通过</el-button>
            <el-button size="small" type="danger" @click="doReview(row, false)">拒绝</el-button>
          </template>
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-popconfirm title="确定删除？" @confirm="remove(row.id)">
            <template #reference><el-button size="small" type="danger" plain>删除</el-button></template>
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

    <el-dialog v-model="dialogVisible" :title="editId ? '编辑专家' : '添加专家'" width="520px">
      <el-form ref="formRef" :model="form" label-width="90px" :rules="rules">
        <el-form-item label="姓名" prop="name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="职称"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="研究方向"><el-input v-model="form.researchField" /></el-form-item>
        <el-form-item label="电话"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
        <el-form-item label="级别" prop="expertLevel">
          <el-radio-group v-model="form.expertLevel">
            <el-radio value="COLLEGE">院级</el-radio>
            <el-radio value="SCHOOL">校级</el-radio>
          </el-radio-group>
        </el-form-item>
        <template v-if="!editId">
          <el-divider content-position="left">登录账号（选填，填了专家才能登录审批工作台）</el-divider>
          <el-form-item label="登录用户名"><el-input v-model="form.username" placeholder="留空则不创建登录账号" /></el-form-item>
          <el-form-item label="登录密码"><el-input v-model="form.password" placeholder="留空则系统生成随机密码" /></el-form-item>
        </template>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { listExperts, addExpert, updateExpert, deleteExpert, reviewExpert } from '../../../api/training'

const rows = ref([])
const loading = ref(false)
const keyword = ref(''); const level = ref(''); const status = ref('')
const current = ref(1); const size = ref(10); const total = ref(0)

const dialogVisible = ref(false)
const saving = ref(false)
const editId = ref(null)
const formRef = ref(null)
const form = reactive({ name: '', title: '', researchField: '', phone: '', email: '', expertLevel: 'COLLEGE', username: '', password: '' })
const rules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  expertLevel: [{ required: true, message: '请选择级别', trigger: 'change' }]
}

const SOURCE = { ADMIN_ADD: '管理员添加', SELF_APPLY: '自主申请', IMPORT: '批量导入' }
const ST = { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已拒绝' }
const ST_TYPE = { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger' }
function sourceLabel(s) { return SOURCE[s] || s }
function statusLabel(s) { return ST[s] || s }
function statusType(s) { return ST_TYPE[s] || 'info' }

async function load() {
  loading.value = true
  try {
    const res = await listExperts({ keyword: keyword.value, level: level.value || undefined, status: status.value || undefined, current: current.value, size: size.value })
    rows.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editId.value = null
  Object.assign(form, { name: '', title: '', researchField: '', phone: '', email: '', expertLevel: 'COLLEGE', username: '', password: '' })
  dialogVisible.value = true
}
function openEdit(row) {
  editId.value = row.id
  Object.assign(form, { name: row.name, title: row.title, researchField: row.researchField, phone: row.phone, email: row.email, expertLevel: row.expertLevel })
  dialogVisible.value = true
}
async function save() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (editId.value) await updateExpert(editId.value, { ...form })
    else await addExpert({ ...form })
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } finally { saving.value = false }
}
async function remove(id) {
  await deleteExpert(id); ElMessage.success('已删除'); load()
}
async function doReview(row, approve) {
  let reason
  if (!approve) {
    const r = await ElMessageBox.prompt('请输入拒绝理由', '拒绝申请', { inputType: 'textarea' }).catch(() => null)
    if (!r) return
    reason = r.value
  }
  await reviewExpert(row.id, approve, reason)
  ElMessage.success('审核完成')
  load()
}

load()
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h2 { margin: 0; }
.filters { display: flex; gap: 10px; margin-bottom: 16px; }
</style>
