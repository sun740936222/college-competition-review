<template>
  <el-header class="app-header">
    <div class="header-left">
      <h1 class="logo" @click="goHome">{{ isTeacher ? '大学生创新创业训练计划 · 审批工作台' : '竞赛在线评审系统' }}</h1>
    </div>
    <div class="header-nav">
      <el-menu mode="horizontal" :default-active="activeMenu" router :ellipsis="false">
        <!-- 教师/专家身份：只展示审批工作台，隐藏所有参赛/学生相关菜单 -->
        <template v-if="isTeacher">
          <el-menu-item index="/training/inbox" class="inbox-link">
            <el-icon><Stamp /></el-icon>审批工作台
          </el-menu-item>
        </template>
        <template v-else>
          <el-menu-item index="/">赛事首页</el-menu-item>
          <el-menu-item index="/projects">我的项目</el-menu-item>
          <el-menu-item index="/training/topics">我的选题</el-menu-item>
          <el-menu-item index="/training/projects">创新训练立项</el-menu-item>
          <el-menu-item index="/training/midterm">中期检查</el-menu-item>
          <el-menu-item index="/training/conclusion">结题验收</el-menu-item>
          <el-menu-item v-if="canApprove" index="/training/inbox" class="inbox-link">
            <el-icon><Stamp /></el-icon>审批工作台
          </el-menu-item>
          <el-menu-item v-if="isJudge" index="/judge" class="judge-link">
            <el-icon><EditPen /></el-icon>评审工作台
          </el-menu-item>
          <el-menu-item v-if="isAdmin" index="/admin/events" class="admin-link">
            <el-icon><Setting /></el-icon>管理后台
          </el-menu-item>
        </template>
      </el-menu>
    </div>
    <div class="header-right">
      <span v-if="userStore.user?.universityName" class="org-info">
        {{ userStore.user.universityName }} / {{ userStore.user.organizationName }}
      </span>
      <span v-else-if="userStore.user?.organizationName" class="org-info">
        {{ userStore.user.organizationName }}
      </span>
      <el-dropdown @command="handleCommand">
        <span class="user-info">
          <el-icon><User /></el-icon>
          {{ userStore.user?.realName || '用户' }}
          <el-icon><ArrowDown /></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="password">
              <el-icon><Edit /></el-icon>修改密码
            </el-dropdown-item>
            <el-dropdown-item command="logout">
              <el-icon><SwitchButton /></el-icon>退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <!-- Change Password Dialog -->
    <el-dialog v-model="pwdDialogVisible" title="修改密码" width="400px" append-to-body>
      <el-form ref="pwdFormRef" :model="pwdForm" label-position="top" :rules="pwdRules">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="pwdSaving" @click="handleChangePassword">确认</el-button>
      </template>
    </el-dialog>
  </el-header>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../../stores/user'
import { ElMessage } from 'element-plus'
import { changePassword } from '../../api/auth'
import { myIdentity } from '../../api/training'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const isAdmin = computed(() => {
  const role = userStore.user?.role
  return role === 'ADMIN' || role === 'SUPER_ADMIN' || role === 'UNIT_ADMIN'
})

const isJudge = computed(() => {
  return userStore.user?.role === 'JUDGE'
})

// 教师/专家（大创审批身份）——不展示赛事、参赛者相关菜单
const isTeacher = computed(() => userStore.user?.role === 'TEACHER')

function goHome() {
  router.push(isTeacher.value ? '/training/inbox' : '/')
}

// 是否具备大创审批身份（教师/专家），用于显示「审批工作台」入口
const canApprove = ref(false)
onMounted(async () => {
  try {
    const res = await myIdentity()
    canApprove.value = !!res.data?.canApprove
  } catch { canApprove.value = false }
})

const activeMenu = computed(() => {
  if (route.path.startsWith('/projects')) return '/projects'
  if (route.path.startsWith('/judge')) return '/judge'
  if (route.path.startsWith('/admin')) return '/admin/events'
  if (route.path.startsWith('/training/inbox')) return '/training/inbox'
  if (route.path.startsWith('/training/topics')) return '/training/topics'
  if (route.path.startsWith('/training/projects')) return '/training/projects'
  if (route.path.startsWith('/training/midterm')) return '/training/midterm'
  if (route.path.startsWith('/training/conclusion')) return '/training/conclusion'
  return '/'
})

// Change password
const pwdDialogVisible = ref(false)
const pwdSaving = ref(false)
const pwdFormRef = ref(null)
const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})
const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== pwdForm.newPassword) callback(new Error('两次密码不一致'))
        else callback()
      }, trigger: 'blur'
    }
  ]
}

function handleCommand(cmd) {
  if (cmd === 'logout') {
    userStore.logout()
    router.push('/login')
  } else if (cmd === 'password') {
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirmPassword = ''
    pwdDialogVisible.value = true
  }
}

async function handleChangePassword() {
  const valid = await pwdFormRef.value.validate().catch(() => false)
  if (!valid) return
  pwdSaving.value = true
  try {
    await changePassword(pwdForm.oldPassword, pwdForm.newPassword)
    ElMessage.success('密码修改成功')
    pwdDialogVisible.value = false
  } finally {
    pwdSaving.value = false
  }
}
</script>

<style scoped>
.app-header {
  display: flex;
  align-items: center;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  padding: 0 24px;
  height: 60px;
}
.header-left {
  flex-shrink: 0;
}
.logo {
  font-size: 18px;
  color: #409eff;
  cursor: pointer;
  white-space: nowrap;
}
.header-nav {
  flex: 1;
  margin-left: 40px;
}
.header-nav .el-menu {
  border-bottom: none;
}
.admin-link .el-icon, .judge-link .el-icon {
  margin-right: 4px;
}
.header-right {
  flex-shrink: 0;
}
.org-info {
  font-size: 13px;
  color: #909399;
  margin-right: 16px;
  padding: 2px 10px;
  background: #f5f7fa;
  border-radius: 4px;
}
.user-info {
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  color: #606266;
}
.user-info:hover {
  color: #409eff;
}
</style>
