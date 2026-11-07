<template>
  <div class="page">
    <div class="page-header">
      <h2>教师管理</h2>
      <div>
        <el-button @click="downloadTeacherTemplate">下载导入模板</el-button>
        <el-upload :show-file-list="false" :before-upload="handleImport" accept=".xlsx" style="display:inline-block; margin: 0 8px">
          <el-button :loading="importing">批量导入</el-button>
        </el-upload>
        <el-button type="primary" @click="openCreate">新建教师</el-button>
      </div>
    </div>

    <el-alert type="info" :closable="false" show-icon style="margin-bottom: 12px"
      title="登录用户名 = 工号（全库唯一，解决同名同姓）；初始密码 = 工号；教师首次登录会被提示修改密码。教师登录入口：登录页选「教师」身份。" />

    <el-input v-model="keyword" placeholder="搜索姓名或工号" clearable style="width: 240px; margin-bottom: 16px" @keyup.enter="load" @clear="load">
      <template #append><el-button @click="load">搜索</el-button></template>
    </el-input>

    <el-table :data="rows" v-loading="loading" border stripe>
      <el-table-column prop="name" label="姓名" width="140" />
      <el-table-column prop="workId" label="工号(登录用户名)" min-width="180" />
      <el-table-column prop="phone" label="手机号" width="150" />
      <el-table-column label="状态" width="140">
        <template #default="{ row }">
          <el-tag v-if="row.mustChangePassword" type="warning">待首次改密</el-tag>
          <el-tag v-else type="success">正常</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-popconfirm title="确定删除该教师账号？" @confirm="remove(row)">
            <template #reference><el-button size="small" type="danger">删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" title="新建教师" width="420px">
      <el-form ref="formRef" :model="form" label-width="80px" :rules="rules">
        <el-form-item label="姓名" prop="name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="工号" prop="workId"><el-input v-model="form.workId" placeholder="将作为登录用户名，也是初始密码" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listTeachers, createTeacher, importTeachers, downloadTeacherTemplate, deleteTeacher } from '../../../api/training'

const rows = ref([])
const loading = ref(false)
const keyword = ref('')
const importing = ref(false)

const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref(null)
const form = reactive({ name: '', workId: '' })
const rules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  workId: [{ required: true, message: '请输入工号', trigger: 'blur' }]
}

async function load() {
  loading.value = true
  try {
    const res = await listTeachers(keyword.value)
    rows.value = res.data || []
  } finally { loading.value = false }
}

function openCreate() {
  Object.assign(form, { name: '', workId: '' })
  dialogVisible.value = true
}

async function save() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const res = await createTeacher(form.name, form.workId)
    ElMessage.success(res.message || '创建成功')
    dialogVisible.value = false
    load()
  } finally { saving.value = false }
}

async function remove(row) {
  await deleteTeacher(row.userId)
  ElMessage.success('已删除')
  load()
}

async function handleImport(file) {
  importing.value = true
  try {
    const res = await importTeachers(file)
    ElMessage.success(res.message || '导入完成')
    if (res.data?.errors?.length) {
      ElMessageBox.alert(res.data.errors.join('\n'), '导入明细', { type: 'warning' })
    }
    load()
  } finally { importing.value = false }
  return false
}

load()
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h2 { margin: 0; }
</style>
