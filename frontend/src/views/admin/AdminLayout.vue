<template>
  <div class="admin-layout">
    <el-container>
      <!-- Sidebar -->
      <el-aside :width="isCollapsed ? '64px' : '220px'" class="admin-sidebar">
        <div class="sidebar-header">
          <span v-if="!isCollapsed" class="sidebar-title">管理后台</span>
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
          <el-menu-item v-if="!isSuperAdmin" index="/admin/events">
            <el-icon><Trophy /></el-icon>
            <span>赛事管理</span>
          </el-menu-item>
          <el-menu-item v-if="isAtLeastSchool" index="/admin/orgs">
            <el-icon><School /></el-icon>
            <span>单位管理</span>
          </el-menu-item>
          <el-menu-item v-if="!isSuperAdmin" index="/admin/projects">
            <el-icon><Document /></el-icon>
            <span>项目审核</span>
          </el-menu-item>
          <el-menu-item v-if="isAtLeastSchool && !isSuperAdmin" index="/admin/judges">
            <el-icon><UserFilled /></el-icon>
            <span>评委管理</span>
          </el-menu-item>

          <!-- Review Management (SCHOOL level and above, excluding SUPER_ADMIN) -->
          <el-sub-menu v-if="isAtLeastSchool && !isSuperAdmin" index="review">
            <template #title>
              <el-icon><DataAnalysis /></el-icon>
              <span>评审管理</span>
            </template>
            <el-menu-item index="/admin/criteria">
              <el-icon><Edit /></el-icon>
              <span>评分标准</span>
            </el-menu-item>
            <el-menu-item index="/admin/allocation">
              <el-icon><Connection /></el-icon>
              <span>评委分配</span>
            </el-menu-item>
            <el-menu-item index="/admin/scores">
              <el-icon><TrendCharts /></el-icon>
              <span>成绩汇总</span>
            </el-menu-item>
          </el-sub-menu>

          <!-- COLLEGE level: only see scores -->
          <el-menu-item v-if="!isAtLeastSchool" index="/admin/scores">
            <el-icon><TrendCharts /></el-icon>
            <span>成绩汇总</span>
          </el-menu-item>

          <!-- 创新训练计划（SRTP）-->
          <el-sub-menu v-if="!isSuperAdmin" index="srtp">
            <template #title>
              <el-icon><Notebook /></el-icon>
              <span>创新训练计划</span>
            </template>
            <el-menu-item index="/admin/srtp/batches">
              <el-icon><Calendar /></el-icon>
              <span>批次管理</span>
            </el-menu-item>
            <el-menu-item index="/admin/srtp/experts">
              <el-icon><UserFilled /></el-icon>
              <span>专家组管理</span>
            </el-menu-item>
            <el-menu-item index="/admin/srtp/teachers">
              <el-icon><Avatar /></el-icon>
              <span>教师管理</span>
            </el-menu-item>
            <el-menu-item index="/admin/srtp/topics">
              <el-icon><Collection /></el-icon>
              <span>选题管理</span>
            </el-menu-item>
            <el-menu-item index="/admin/srtp/projects">
              <el-icon><Files /></el-icon>
              <span>立项管理</span>
            </el-menu-item>
            <el-menu-item index="/admin/srtp/midterm">
              <el-icon><DataLine /></el-icon>
              <span>中期管理</span>
            </el-menu-item>
            <el-menu-item index="/admin/srtp/conclusion">
              <el-icon><CircleCheck /></el-icon>
              <span>结题管理</span>
            </el-menu-item>
            <el-menu-item index="/admin/srtp/tracking">
              <el-icon><Odometer /></el-icon>
              <span>全流程追踪</span>
            </el-menu-item>
            <el-menu-item index="/admin/srtp/stats">
              <el-icon><PieChart /></el-icon>
              <span>分类统计</span>
            </el-menu-item>
            <el-menu-item index="/admin/srtp/promotion">
              <el-icon><Promotion /></el-icon>
              <span>项目推荐</span>
            </el-menu-item>
            <el-menu-item index="/admin/srtp/fields">
              <el-icon><SetUp /></el-icon>
              <span>自定义字段</span>
            </el-menu-item>
          </el-sub-menu>
        </el-menu>
      </el-aside>

      <!-- Main Content -->
      <el-container>
        <el-header class="admin-header">
          <div class="header-left">
            <el-button text @click="$router.push('/')">
              <el-icon><ArrowLeft /></el-icon>返回前台
            </el-button>
          </div>
          <div class="header-right">
            <span v-if="userStore.user?.organizationName" class="org-name">{{ userStore.user.organizationName }}</span>
            <span v-if="orgInviteCode" class="invite-code">
              <el-icon><Link /></el-icon>
              邀请码: <strong>{{ orgInviteCode }}</strong>
              <el-button size="small" text type="primary" @click="copyInviteCode">
                <el-icon><CopyDocument /></el-icon>
              </el-button>
              <el-popconfirm title="重新生成后原邀请码将失效，确定继续？" @confirm="handleRegenerate">
                <template #reference>
                  <el-button size="small" text type="warning">
                    <el-icon><Refresh /></el-icon>
                  </el-button>
                </template>
              </el-popconfirm>
            </span>
            <span class="user-badge" :class="roleClass">
              {{ roleLabel }}
            </span>
            <el-dropdown @command="handleCommand">
              <span class="user-info">
                <el-icon><User /></el-icon>
                {{ userStore.user?.realName || '管理员' }}
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

        <el-main class="admin-main">
          <router-view />
          <footer class="copyright-footer">
            <span>版权归长沙创赛赢教育科技有限公司所有</span>
            <span class="footer-contact">合作联系：微信号327172508（添加好友时，注明单位+姓名）&nbsp;&nbsp;使用问题：微信号1175560106（添加好友时，注明单位+姓名）</span>
          </footer>
        </el-main>
      </el-container>
    </el-container>

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
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../../stores/user'
import { ElMessage } from 'element-plus'
import { Expand, Fold } from '@element-plus/icons-vue'
import { changePassword } from '../../api/auth'
import { getMyOrganization, regenerateMyInviteCode } from '../../api/admin'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const isCollapsed = ref(false)

const activeMenu = computed(() => {
  const path = route.path
  if (path.startsWith('/admin/events')) return '/admin/events'
  if (path.startsWith('/admin/orgs')) return '/admin/orgs'
  if (path.startsWith('/admin/projects')) return '/admin/projects'
  if (path.startsWith('/admin/judges')) return '/admin/judges'
  return isSuperAdmin.value ? '/admin/orgs' : '/admin/events'
})

const adminLevel = computed(() => userStore.user?.orgLevel || 'COLLEGE')
const isAtLeastSchool = computed(() => ['COLLEGE', 'SCHOOL', 'PROVINCE', 'NATIONAL', 'SUPER_ADMIN'].includes(adminLevel.value))
const isSuperAdmin = computed(() => adminLevel.value === 'SUPER_ADMIN')
const orgInviteCode = ref('')

async function loadMyOrg() {
  if (isSuperAdmin.value) return
  try {
    const res = await getMyOrganization()
    orgInviteCode.value = res.data?.inviteCode || ''
  } catch {}
}

function copyInviteCode() {
  if (!orgInviteCode.value) return
  navigator.clipboard.writeText(orgInviteCode.value).then(() => {
    ElMessage.success('邀请码已复制')
  }).catch(() => {
    ElMessage.error('复制失败，请手动复制')
  })
}

async function handleRegenerate() {
  try {
    const res = await regenerateMyInviteCode()
    orgInviteCode.value = res.data || ''
    ElMessage.success('邀请码已重新生成')
  } catch {
    ElMessage.error('重新生成邀请码失败')
  }
}

onMounted(loadMyOrg)

const levelLabels = { COLLEGE: '院系管理员', SCHOOL: '校级管理员', PROVINCE: '省级管理员', NATIONAL: '国赛管理员', SUPER_ADMIN: '超级管理员' }
const levelClasses = { COLLEGE: 'badge-info', SCHOOL: 'badge-blue', PROVINCE: 'badge-orange', NATIONAL: 'badge-red', SUPER_ADMIN: 'badge-red' }

const roleLabel = computed(() => levelLabels[adminLevel.value] || '管理员')

const roleClass = computed(() => levelClasses[adminLevel.value] || 'badge-blue')

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
    router.push('/admin/login')
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
.admin-layout {
  height: 100vh;
}
.admin-sidebar {
  background: #1d1e1f;
  transition: width 0.3s;
  overflow: hidden;
}
.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 12px;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  border-bottom: 1px solid rgba(255,255,255,0.1);
}
.sidebar-title {
  white-space: nowrap;
}
.sidebar-menu {
  border-right: none;
}
.admin-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  padding: 0 20px;
  height: 60px;
}
.header-left, .header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.org-name {
  color: #909399;
  font-size: 13px;
  margin-right: 4px;
}
.invite-code {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #606266;
  margin-right: 8px;
}
.invite-code strong {
  color: #409eff;
  letter-spacing: 1px;
}
.user-badge {
  padding: 2px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}
.badge-info {
  background: #e8f5e9;
  color: #67c23a;
}
.badge-blue {
  background: #ecf5ff;
  color: #409eff;
}
.badge-orange {
  background: #fdf6ec;
  color: #e6a23c;
}
.badge-red {
  background: #fef0f0;
  color: #f56c6c;
}
.user-info {
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  color: #606266;
}
.admin-main {
  background: #f5f7fa;
  padding: 20px;
  height: calc(100vh - 60px);
  overflow-y: auto;
}
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