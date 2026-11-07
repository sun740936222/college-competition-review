<template>
  <div class="judge-layout">
    <el-container>
      <el-aside :width="isCollapsed ? '64px' : '220px'" class="judge-sidebar">
        <div class="sidebar-header">
          <span v-if="!isCollapsed" class="sidebar-title">评委工作台</span>
          <el-button :icon="isCollapsed ? Expand : Fold" text @click="isCollapsed = !isCollapsed" />
        </div>
        <el-menu
          :default-active="activeMenu"
          :collapse="isCollapsed"
          router
          class="sidebar-menu"
          background-color="#1d1e1f"
          text-color="#bfcbd9"
          active-text-color="#409eff"
        >
          <el-menu-item index="/judge">
            <el-icon><List /></el-icon>
            <span>评审任务</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <el-container>
        <el-header class="judge-header">
          <div class="header-right">
            <span class="judge-badge">评委</span>
            <el-dropdown @command="handleCommand">
              <span class="user-info">
                <el-icon><User /></el-icon>
                {{ userStore.user?.realName || userStore.user?.username || '评委' }}
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
        </el-header>

        <el-main class="judge-main">
          <router-view />
          <footer class="copyright-footer">
            <span>版权归长沙创赛赢教育科技有限公司所有</span>
            <span class="footer-contact">合作联系：微信号327172508（添加好友时，注明单位+姓名）&nbsp;&nbsp;使用问题：微信号1175560106（添加好友时，注明单位+姓名）</span>
          </footer>
        </el-main>
      </el-container>
    </el-container>

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
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../../stores/user'
import { ElMessage } from 'element-plus'
import { Expand, Fold } from '@element-plus/icons-vue'
import { changePassword } from '../../api/auth'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const isCollapsed = ref(false)

const activeMenu = computed(() => route.path.startsWith('/judge') ? '/judge' : '/judge')

const pwdDialogVisible = ref(false)
const pwdSaving = ref(false)
const pwdFormRef = ref(null)
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: (rule, value, callback) => {
      if (value !== pwdForm.newPassword) callback(new Error('两次密码不一致'))
      else callback()
    }, trigger: 'blur' }
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
.judge-layout { height: 100vh; }
.judge-sidebar { background: #1d1e1f; transition: width 0.3s; overflow: hidden; }
.sidebar-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 12px; color: #fff; font-size: 16px; font-weight: 600;
  border-bottom: 1px solid rgba(255,255,255,0.1);
}
.sidebar-title { white-space: nowrap; }
.sidebar-menu { border-right: none; }
.judge-header {
  display: flex; align-items: center; justify-content: flex-end;
  background: #fff; border-bottom: 1px solid #e4e7ed; padding: 0 20px; height: 60px;
}
.header-right { display: flex; align-items: center; gap: 12px; }
.judge-badge {
  padding: 2px 10px; border-radius: 4px; font-size: 12px; font-weight: 500;
  background: #ecf5ff; color: #409eff;
}
.user-info { display: flex; align-items: center; gap: 4px; cursor: pointer; color: #606266; }
.judge-main { background: #f5f7fa; padding: 20px; height: calc(100vh - 60px); overflow-y: auto; }
.copyright-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 16px;
  padding: 16px 20px;
  color: #909399;
  font-size: 13px;
  margin-top: 20px;
  border-top: 1px solid #dcdfe6;
}
.footer-contact {
  color: #a8abb2;
  font-size: 12px;
}
</style>