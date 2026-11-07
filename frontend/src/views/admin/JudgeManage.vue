<template>
  <div class="judge-manage">
    <div class="page-header">
      <h2>评委管理</h2>
      <el-button type="primary" @click="showCreateDialog">
        <el-icon><Plus /></el-icon>添加评委
      </el-button>
    </div>

    <el-table :data="judges" v-loading="loading" stripe>
      <el-table-column prop="realName" label="姓名" width="120" />
      <el-table-column prop="username" label="登录账号" width="150" />
      <el-table-column prop="phone" label="电话" width="140" />
      <el-table-column label="所属单位" width="200">
        <template #default="{ row }">{{ orgName(row.organizationId) }}</template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180">
        <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-popconfirm title="确认删除该评委？" @confirm="handleDelete(row.id)">
            <template #reference>
              <el-button size="small" type="danger">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <div style="display:flex;justify-content:center;margin-top:16px" v-if="total > 0">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="prev, pager, next, sizes, total"
        @current-change="loadJudges"
        @size-change="handleSizeChange"
      />
    </div>

    <!-- Create Dialog -->
    <el-dialog v-model="dialogVisible" title="添加评委" width="500px">
      <el-form ref="formRef" :model="form" label-position="top" :rules="rules">
        <el-form-item label="登录账号" prop="username">
          <el-input v-model="form.username" placeholder="数字、字母或二者组合" />
        </el-form-item>
        <el-form-item label="登录密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="评委登录密码" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="评委姓名" />
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="form.phone" placeholder="联系电话" />
        </el-form-item>
        <el-form-item label="所属单位（选填）">
          <el-input v-model="form.judgeOrganization" placeholder="如：某某大学、某某企业（选填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listJudges, createJudge, deleteJudge, listAllOrgs } from '../../api/admin'

const loading = ref(false)
const judges = ref([])
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const orgs = ref([])
const formRef = ref(null)
const dialogVisible = ref(false)
const saving = ref(false)

const form = reactive({
  username: '',
  password: '',
  realName: '',
  phone: '',
  judgeOrganization: ''
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9]+$/, message: '用户名只能包含数字和字母', trigger: 'blur' }
  ],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入评委姓名', trigger: 'blur' }]
}

onMounted(() => {
  loadJudges()
  loadOrgs()
})

async function loadOrgs() {
  try {
    const res = await listAllOrgs({ current: 1, size: 10000 })
    orgs.value = res.data?.records || []
  } catch {}
}

async function loadJudges() {
  loading.value = true
  try {
    const res = await listJudges({ current: currentPage.value, size: pageSize.value })
    const data = res.data
    judges.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function handleSizeChange() {
  currentPage.value = 1
  loadJudges()
}

function showCreateDialog() {
  form.username = ''
  form.password = ''
  form.realName = ''
  form.phone = ''
  form.judgeOrganization = ''
  dialogVisible.value = true
}

async function handleSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    await createJudge(form)
    ElMessage.success('评委创建成功')
    dialogVisible.value = false
    await loadJudges()
  } catch {
    // error already shown by axios interceptor
  } finally {
    saving.value = false
  }
}

async function handleDelete(id) {
  try {
    await deleteJudge(id)
    ElMessage.success('评委已删除')
    await loadJudges()
  } catch {
    // error already shown by axios interceptor
  }
}

function orgName(orgId) {
  if (!orgId) return '-'
  const org = orgs.value.find(o => o.id === orgId)
  return org ? org.name : '-'
}

function formatDate(d) {
  return d ? new Date(d).toLocaleString('zh-CN') : '-'
}
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.page-header h2 {
  font-size: 20px;
  color: #303133;
}
</style>