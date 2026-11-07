<template>
  <div class="org-manage">
    <div class="page-header">
      <h2>单位管理</h2>
      <div class="header-actions">
        <el-button v-if="adminLevel === 'NATIONAL' || isSuperAdmin" type="success" @click="showLookupDialog('province')">
          <el-icon><Search /></el-icon>查询省份
        </el-button>
        <el-button type="primary" @click="showCreateDialog">
          <el-icon><Plus /></el-icon>创建单位
        </el-button>
        <el-button type="danger" :disabled="selectedOrgIds.length === 0" @click="handleBatchDelete">
          <el-icon><Delete /></el-icon>批量删除
        </el-button>
      </div>
    </div>

    <el-table :data="orgs" v-loading="loading" stripe @selection-change="onSelectionChange">
      <el-table-column type="selection" width="40" />
      <el-table-column prop="name" label="单位名称" min-width="200" />
      <el-table-column prop="inviteCode" label="邀请码" width="160" />
      <el-table-column prop="description" label="描述" min-width="200">
        <template #default="{ row }">{{ row.description || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="showEditDialog(row)">编辑</el-button>
          <el-button size="small" type="success" @click="showAccountDialog(row)">创建账号</el-button>
          <el-button size="small" type="info" @click="openAccounts(row.id)">账号</el-button>
          <el-popconfirm title="确认删除该单位？" @confirm="handleDelete(row.id)">
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
        @current-change="loadOrgs"
        @size-change="handleSizeChange"
      />
    </div>

    <!-- Create/Edit Dialog -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑单位' : '创建单位'" width="500px">
      <el-form ref="formRef" :model="form" label-position="top" :rules="rules">
        <el-form-item v-if="!isEdit && isSuperAdmin" label="上级单位" prop="parentId">
          <el-select
            v-model="form.parentId"
            filterable
            remote
            reserve-keyword
            placeholder="搜索并选择上级单位"
            :remote-method="searchParentOrgs"
            :loading="parentSearchLoading"
            style="width: 100%"
          >
            <el-option
              v-for="o in parentOrgOptions"
              :key="o.id"
              :label="`${o.name}（${levelLabelMap[o.level] || o.level}）`"
              :value="o.id"
            />
          </el-select>
          <div style="font-size: 12px; color: #909399; margin-top: 4px;">
            不选择上级单位时，可手动指定组织层级作为赛事顶级单位
          </div>
        </el-form-item>
        <el-form-item v-if="!isEdit && !isSuperAdmin" label="上级单位">
          <el-input :model-value="myOrgName" disabled />
        </el-form-item>
        <el-form-item v-if="!isEdit && isSuperAdmin && !form.parentId" label="组织层级" prop="level">
          <el-select v-model="form.level" placeholder="请选择组织层级" style="width: 100%">
            <el-option label="国家级" value="NATIONAL" />
            <el-option label="省级" value="PROVINCE" />
            <el-option label="校级" value="SCHOOL" />
            <el-option label="院系级" value="COLLEGE" />
          </el-select>
          <div style="font-size: 12px; color: #909399; margin-top: 4px;">
            该赛事的最高组织层级（选择上级单位后将自动推算，无需手动指定）
          </div>
        </el-form-item>
        <el-form-item label="单位名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入单位名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="单位描述（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">{{ isEdit ? '保存' : '创建' }}</el-button>
      </template>
    </el-dialog>

    <!-- Create Admin Account Dialog -->
    <el-dialog v-model="accountDialogVisible" title="创建下级管理员账号" width="450px">
      <p style="margin-bottom: 12px; color: #606266;">
        为「{{ accountOrgName }}」创建下级管理员账号
      </p>
      <el-form ref="accountFormRef" :model="accountForm" label-position="top" :rules="accountRules">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="accountForm.username" placeholder="数字、字母或二者组合" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="accountForm.password" type="password" placeholder="登录密码" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="accountForm.realName" placeholder="管理员姓名（可选）" />
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="accountForm.phone" placeholder="联系电话（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="accountDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="creatingAccount" @click="handleCreateAccount">创建</el-button>
      </template>
    </el-dialog>

    <!-- View Accounts Dialog -->
    <el-dialog v-model="accountsListDialogVisible" title="管理员账号" width="700px">
      <el-table :data="accounts" stripe>
        <el-table-column prop="username" label="用户名" width="150" />
        <el-table-column prop="realName" label="姓名" width="120">
          <template #default="{ row }">{{ row.realName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="phone" label="电话" width="150">
          <template #default="{ row }">{{ row.phone || '-' }}</template>
        </el-table-column>
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">{{ row.createdAt || '-' }}</template>
        </el-table-column>
      </el-table>
      <el-empty v-if="accounts.length === 0" description="暂无账号" />
    </el-dialog>

    <!-- Province Lookup Dialog -->
    <el-dialog v-model="provinceDialogVisible" title="选择省份" width="650px">
      <div v-if="isSuperAdmin && nationalOrgOptions.length > 1" style="margin-bottom: 16px;">
        <p style="color: #606266; margin-bottom: 8px;">选择上级国家级单位：</p>
        <el-select v-model="batchParentOrgId" placeholder="请选择上级单位" style="width: 100%">
          <el-option
            v-for="o in nationalOrgOptions"
            :key="o.id"
            :label="`${o.name}（${levelLabelMap[o.level] || o.level}）`"
            :value="o.id"
          />
        </el-select>
      </div>
      <div v-if="provinces.length > 0">
        <div class="lookup-result-header" style="margin-bottom:12px;">
          <span>共 {{ provinces.length }} 个省级行政区</span>
          <el-button size="small" text @click="selectAllProvinces">
            {{ selectedProvinces.length === provinces.length ? '取消全选' : '全选' }}
          </el-button>
        </div>
        <el-checkbox-group v-model="selectedProvinces" class="college-list">
          <el-checkbox v-for="p in provinces" :key="p" :value="p" :label="p" />
        </el-checkbox-group>
        <div class="lookup-batch-actions" v-if="selectedProvinces.length > 0">
          <p style="color: #606266;">
            已选择 {{ selectedProvinces.length }} 个省份，将创建为「{{ isSuperAdmin ? (nationalOrgOptions.find(o => o.id === batchParentOrgId)?.name || '未选择上级单位') : myOrgName }}」的下级单位
          </p>
          <el-button type="success" :loading="batchCreating" @click="handleBatchCreateProvinces">
            批量创建 {{ selectedProvinces.length }} 个省份
          </el-button>
        </div>
      </div>
      <div v-else style="text-align:center;padding:20px;color:#909399;">
        加载中...
      </div>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listAllOrgs, createOrg, updateOrg, deleteOrg, batchDeleteOrgs, createOrgAccount,
  lookupProvinces, batchCreateColleges, listOrgAccounts
} from '../../api/admin'
import { useUserStore } from '../../stores/user'

const userStore = useUserStore()
const adminLevel = computed(() => userStore.user?.orgLevel || 'COLLEGE')
const isSuperAdmin = computed(() => adminLevel.value === 'SUPER_ADMIN')
const myOrgName = computed(() => userStore.user?.organizationName || '当前单位')
const myOrgId = computed(() => userStore.user?.organizationId || null)

const levelLabelMap = { NATIONAL: '国家级', PROVINCE: '省级', SCHOOL: '校级', COLLEGE: '院系级' }

// Parent org selector state (SUPER_ADMIN only)
const parentOrgOptions = ref([])
const parentSearchLoading = ref(false)

async function searchParentOrgs(query) {
  if (!query || query.length < 1) {
    parentOrgOptions.value = []
    return
  }
  parentSearchLoading.value = true
  try {
    const res = await listAllOrgs({ current: 1, size: 20, keyword: query })
    parentOrgOptions.value = res.data?.records || []
  } catch {
    parentOrgOptions.value = []
  } finally {
    parentSearchLoading.value = false
  }
}

const loading = ref(false)
const orgs = ref([])
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const formRef = ref(null)
const saving = ref(false)

const accountFormRef = ref(null)
const accountDialogVisible = ref(false)
const accountOrgId = ref(null)
const accountOrgName = ref('')
const creatingAccount = ref(false)

const accountsListDialogVisible = ref(false)
const accounts = ref([])
const currentOrgId = ref(null)

// Province lookup state
const provinceDialogVisible = ref(false)
const provinces = ref([])
const selectedProvinces = ref([])
const batchParentOrgId = ref(null)
const nationalOrgOptions = ref([])

const batchCreating = ref(false)

// Batch delete
const selectedOrgIds = ref([])

const form = reactive({
  name: '',
  description: '',
  parentId: null,
  level: 'NATIONAL'
})

const rules = {
  name: [{ required: true, message: '请输入单位名称', trigger: 'blur' }],
  level: [{ required: true, message: '请选择组织层级', trigger: 'change' }]
}

const accountForm = reactive({
  username: '',
  password: '',
  realName: '',
  phone: ''
})

const accountRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9]+$/, message: '用户名只能包含数字和字母', trigger: 'blur' }
  ],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

onMounted(() => loadOrgs())

function handleSizeChange() {
  currentPage.value = 1
  loadOrgs()
}

async function loadOrgs() {
  loading.value = true
  try {
    const params = { current: currentPage.value, size: pageSize.value, direct: true }
    const res = await listAllOrgs(params)
    const data = res.data
    orgs.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function showCreateDialog() {
  isEdit.value = false
  editId.value = null
  form.name = ''
  form.description = ''
  form.parentId = null
  form.level = 'NATIONAL'
  parentOrgOptions.value = []
  dialogVisible.value = true
}

function showEditDialog(row) {
  isEdit.value = true
  editId.value = row.id
  form.name = row.name
  form.description = row.description || ''
  dialogVisible.value = true
}

async function handleSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    if (isEdit.value) {
      await updateOrg(editId.value, form)
      ElMessage.success('单位已更新')
    } else {
      const parentId = isSuperAdmin.value ? form.parentId : myOrgId.value
      await createOrg({ ...form, parentId })
      ElMessage.success('单位已创建')
    }
    dialogVisible.value = false
    await loadOrgs()
  } catch {
    // error already shown by axios interceptor
  } finally {
    saving.value = false
  }
}

async function handleDelete(id) {
  try {
    await deleteOrg(id)
    ElMessage.success('单位已删除')
    await loadOrgs()
  } catch (e) {
    // Error message is handled by the API interceptor
  }
}

function onSelectionChange(selection) {
  selectedOrgIds.value = selection.map(s => s.id)
}

async function handleBatchDelete() {
  if (selectedOrgIds.value.length === 0) return
  try {
    await ElMessageBox.confirm(
      `确认删除选中的 ${selectedOrgIds.value.length} 个单位及下属所有组织？此操作将同时删除这些单位下的所有账号，且不可恢复！`,
      '批量删除确认',
      { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await batchDeleteOrgs(selectedOrgIds.value)
    ElMessage.success(`成功删除 ${selectedOrgIds.value.length} 个单位`)
    selectedOrgIds.value = []
    await loadOrgs()
  } catch {
    // error shown by interceptor
  }
}

function showAccountDialog(row) {
  accountOrgId.value = row.id
  accountOrgName.value = row.name
  accountForm.username = ''
  accountForm.password = ''
  accountForm.realName = ''
  accountForm.phone = ''
  accountDialogVisible.value = true
}

async function handleCreateAccount() {
  const valid = await accountFormRef.value.validate().catch(() => false)
  if (!valid) return

  creatingAccount.value = true
  try {
    await createOrgAccount(accountOrgId.value, accountForm)
    ElMessage.success('管理员账号创建成功')
    accountDialogVisible.value = false
  } catch {
    // error already shown by axios interceptor
  } finally {
    creatingAccount.value = false
  }
}

async function openAccounts(orgId) {
  currentOrgId.value = orgId
  accounts.value = []
  accountsListDialogVisible.value = true
  try {
    const res = await listOrgAccounts(orgId, { current: 1, size: 100 })
    accounts.value = res.data?.records || []
  } catch {
    accounts.value = []
  }
}

// ===== Province Lookup =====

function showLookupDialog(mode) {
  if (mode === 'province') {
    provinceDialogVisible.value = true
    selectedProvinces.value = []
    batchParentOrgId.value = null
    nationalOrgOptions.value = []
    if (isSuperAdmin.value) {
      loadNationalOrgs()
    }
    if (provinces.value.length === 0) {
      loadProvinces()
    }
  }
}

async function loadProvinces() {
  try {
    const res = await lookupProvinces()
    provinces.value = res.data || []
  } catch {
    ElMessage.error('加载省份列表失败')
  }
}

async function loadNationalOrgs() {
  try {
    const res = await listAllOrgs({ current: 1, size: 100 })
    const allOrgs = res.data?.records || []
    nationalOrgOptions.value = allOrgs.filter(o => o.level === 'NATIONAL')
    if (nationalOrgOptions.value.length === 1) {
      batchParentOrgId.value = nationalOrgOptions.value[0].id
    }
  } catch {
    nationalOrgOptions.value = []
  }
}

function selectAllProvinces() {
  if (selectedProvinces.value.length === provinces.value.length) {
    selectedProvinces.value = []
  } else {
    selectedProvinces.value = [...provinces.value]
  }
}

async function handleBatchCreateProvinces() {
  const parentOrgId = isSuperAdmin.value ? batchParentOrgId.value : myOrgId.value
  if (selectedProvinces.value.length === 0 || !parentOrgId) {
    if (isSuperAdmin.value && !parentOrgId) {
      ElMessage.warning('请先选择上级国家级单位')
    }
    return
  }

  batchCreating.value = true
  try {
    await batchCreateColleges({
      parentOrgId,
      collegeNames: selectedProvinces.value,
      createAdminAccounts: false
    })
    ElMessage.success(`成功创建 ${selectedProvinces.value.length} 个省份`)
    provinceDialogVisible.value = false
    await loadOrgs()
  } catch {
    // error shown by interceptor
  } finally {
    batchCreating.value = false
  }
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
.header-actions {
  display: flex;
  gap: 10px;
}

/* Lookup Dialogs */
.lookup-result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-weight: 500;
  color: #303133;
}
.college-list {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  max-height: 300px;
  overflow-y: auto;
  padding: 8px;
  background: #f5f7fa;
  border-radius: 4px;
}
.lookup-batch-actions {
  margin-top: 16px;
  text-align: right;
}
</style>
