<template>
  <div class="login-page">
    <div class="login-content">
    <div class="login-hero">
      <div class="hero-content">
        <div class="hero-badge">管理后台</div>
        <h1 class="hero-title">竞赛评审<br />管理系统</h1>
        <p class="hero-desc">赛事管理 · 项目审核 · 评委分配 · 成绩统计</p>
        <p class="hero-scope">适用于各级赛事管理员进行赛事全流程管理</p>
        <div class="hero-features">
          <div class="hero-feature">
            <span class="feature-icon"><el-icon><Setting /></el-icon></span>
            <span>多级赛事管理</span>
          </div>
          <div class="hero-feature">
            <span class="feature-icon"><el-icon><DataAnalysis /></el-icon></span>
            <span>项目审核与评分</span>
          </div>
          <div class="hero-feature">
            <span class="feature-icon"><el-icon><UserFilled /></el-icon></span>
            <span>评委与组织管理</span>
          </div>
        </div>
      </div>
      <div class="hero-bg-shapes">
        <div class="shape shape-1"></div>
        <div class="shape shape-2"></div>
        <div class="shape shape-3"></div>
      </div>
    </div>

    <div class="login-panel">
      <div class="login-card">
        <div class="card-header">
          <h2 class="card-title">管理员登录</h2>
          <p class="card-subtitle">请选择您的管理身份进行登录</p>
        </div>

        <div class="role-section">
          <div class="role-section-title">管理员身份</div>
          <div class="role-tabs admin-tabs">
            <div
              v-for="role in adminRoles"
              :key="role.value"
              class="role-tab"
              :class="{ active: selectedRole === role.value }"
              :style="{ '--role-color': role.color }"
              @click="selectedRole = role.value"
            >
              <el-icon :size="22"><component :is="role.icon" /></el-icon>
              <span class="role-label">{{ role.label }}</span>
              <span class="role-desc">{{ role.desc }}</span>
            </div>
          </div>
        </div>

        <div class="active-role-hint" :style="{ color: currentRole?.color }">
          <el-icon><component :is="currentRole?.icon" /></el-icon>
          <span>{{ currentRole?.hint }}</span>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" class="login-form" @submit.prevent="handleLogin">
          <el-form-item prop="username">
            <template #label><span class="form-label">用户名</span></template>
            <el-input
              v-model="form.username"
              placeholder="请输入管理员用户名"
              :prefix-icon="UserIcon"
              size="large"
            />
          </el-form-item>

          <el-form-item prop="password">
            <template #label><span class="form-label">密码</span></template>
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              :prefix-icon="Lock"
              show-password
              size="large"
              @keyup.enter="handleLogin"
            />
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              :loading="loading"
              size="large"
              class="login-btn"
              :style="{ backgroundColor: currentRole?.color, borderColor: currentRole?.color }"
              @click="handleLogin"
            >
              {{ loading ? '登录中...' : '登 录' }}
            </el-button>
          </el-form-item>
        </el-form>

        <div class="card-footer text-muted" v-if="selectedRole === 'NATIONAL_ADMIN'">
          国赛管理员账号由超级管理员创建
        </div>
        <div class="card-footer text-muted" v-else-if="selectedRole === 'PROVINCE_ADMIN'">
          省级管理员账号由国赛管理员或超级管理员创建
        </div>
        <div class="card-footer text-muted" v-else-if="selectedRole === 'SCHOOL_ADMIN'">
          校级管理员账号由省级管理员或超级管理员创建
        </div>
        <div class="card-footer text-muted" v-else-if="selectedRole === 'SUPER_ADMIN'">
          超级管理员账号由系统自动创建，无需注册
        </div>
        <div class="card-footer text-muted" v-else>
          院系管理账号由校级管理员或超级管理员创建
        </div>
      </div>
    </div>
    </div>
    <footer class="copyright-footer">
      <span>版权归长沙创赛赢教育科技有限公司所有</span>
      <span class="footer-contact">合作联系：微信号327172508（添加好友时，注明单位+姓名）&nbsp;&nbsp;使用问题：微信号1175560106（添加好友时，注明单位+姓名）</span>
    </footer>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'
import {
  User as UserIcon, Lock, DataAnalysis,
  UserFilled, Setting
} from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)
const selectedRole = ref('SCHOOL_ADMIN')

const adminRoles = [
  { value: 'COLLEGE_ADMIN', label: '院系管理员', desc: '院赛管理', hint: '以院系管理员身份登录，管理院级赛事与审核', icon: Setting, color: '#67c23a' },
  { value: 'SCHOOL_ADMIN', label: '校级管理员', desc: '校赛管理', hint: '以校级管理员身份登录，管理校级赛事与审核', icon: Setting, color: '#409eff' },
  { value: 'PROVINCE_ADMIN', label: '省级管理员', desc: '省赛管理', hint: '以省级管理员身份登录，管理省级赛事与审核', icon: Setting, color: '#e6a23c' },
  { value: 'NATIONAL_ADMIN', label: '国赛管理员', desc: '国赛管理', hint: '以国赛管理员身份登录，管理国家级赛事与审核', icon: Setting, color: '#f56c6c' },
  { value: 'SUPER_ADMIN', label: '超级管理员', desc: '全局管理', hint: '以超级管理员身份登录，管理所有管理员账号与全局配置', icon: UserFilled, color: '#9c27b0' }
]

const currentRole = computed(() => adminRoles.find(r => r.value === selectedRole.value))

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await userStore.login(form.username, form.password, selectedRole.value)
    if (userStore.user?.orgLevel === 'SUPER_ADMIN') {
      router.push('/admin/orgs')
    } else {
      router.push('/admin')
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}
.login-content {
  flex: 1;
  display: flex;
}
.copyright-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 16px;
  padding: 12px 20px;
  color: #909399;
  font-size: 13px;
  background: #f8f9fa;
  border-top: 1px solid #e4e7ed;
}
.footer-contact {
  color: #a8abb2;
  font-size: 12px;
}

/* ===== Hero Panel ===== */
.login-hero {
  flex: 1;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #5e35b1 0%, #311b92 50%, #4527a0 100%);
  overflow: hidden;
  padding: 48px;
}

.hero-content {
  position: relative;
  z-index: 1;
  max-width: 480px;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  padding: 6px 16px;
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 20px;
  color: #fff;
  font-size: 13px;
  font-weight: 500;
  letter-spacing: 0.5px;
  margin-bottom: 24px;
}

.hero-title {
  font-size: 40px;
  font-weight: 700;
  color: #fff;
  line-height: 1.2;
  margin-bottom: 16px;
  letter-spacing: -0.5px;
}

.hero-desc {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.75);
  line-height: 1.6;
  margin-bottom: 12px;
}

.hero-scope {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.65);
  line-height: 1.5;
  margin-bottom: 28px;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  border-left: 3px solid rgba(255, 255, 255, 0.3);
}

.hero-features {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.hero-feature {
  display: flex;
  align-items: center;
  gap: 12px;
  color: rgba(255, 255, 255, 0.85);
  font-size: 15px;
}

.feature-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.12);
  font-size: 18px;
}

.hero-bg-shapes {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.shape {
  position: absolute;
  border-radius: 50%;
  opacity: 0.08;
}

.shape-1 {
  width: 600px;
  height: 600px;
  background: #fff;
  top: -150px;
  right: -200px;
}

.shape-2 {
  width: 300px;
  height: 300px;
  background: #fff;
  bottom: -80px;
  left: -80px;
}

.shape-3 {
  width: 200px;
  height: 200px;
  background: #fff;
  bottom: 30%;
  right: 10%;
}

/* ===== Login Panel ===== */
.login-panel {
  width: 520px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8f9fa;
  padding: 48px;
}

.login-card {
  width: 100%;
  max-width: 420px;
}

.card-header {
  margin-bottom: 28px;
  text-align: center;
}

.card-title {
  font-size: 28px;
  font-weight: 700;
  color: #202124;
  margin-bottom: 8px;
}

.card-subtitle {
  font-size: 15px;
  color: #5f6368;
}

/* ===== Role Sections ===== */
.role-section {
  margin-bottom: 16px;
}

.role-section-title {
  font-size: 12px;
  font-weight: 600;
  color: #909399;
  margin-bottom: 8px;
  letter-spacing: 1px;
}

/* ===== Role Tabs ===== */
.role-tabs {
  display: grid;
  gap: 10px;
  margin-bottom: 4px;
}

.role-tabs.admin-tabs {
  grid-template-columns: 1fr 1fr 1fr 1fr 1fr;
}

.role-tab {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 14px 8px;
  border-radius: 12px;
  border: 2px solid #e4e7ed;
  background: #fff;
  cursor: pointer;
  transition: all 0.25s ease;
}

.role-tab:hover {
  border-color: var(--role-color);
  background: rgba(255, 255, 255, 0.8);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
}

.role-tab.active {
  border-color: var(--role-color);
  background: #f0f9ff;
}

.role-tab .el-icon {
  color: var(--role-color);
  margin-bottom: 2px;
}

.role-label {
  font-size: 12px;
  font-weight: 600;
  color: #202124;
}

.role-desc {
  font-size: 10px;
  color: #909399;
}

.admin-tabs .role-tab {
  padding: 10px 4px;
}

.admin-tabs .role-label {
  font-size: 12px;
}

.admin-tabs .role-desc {
  font-size: 10px;
}

/* ===== Active Role Hint ===== */
.active-role-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  padding: 8px 12px;
  background: #fff;
  border-radius: 8px;
  margin-bottom: 20px;
  border: 1px solid #e4e7ed;
}

/* ===== Form ===== */
.login-form {
  margin-bottom: 24px;
}

:deep(.el-form-item) {
  margin-bottom: 24px;
}

:deep(.el-form-item__label) {
  padding-bottom: 6px;
  font-weight: 500;
  color: #202124;
}

:deep(.el-input__wrapper) {
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.06), 0 0 0 1px rgba(0, 0, 0, 0.08);
  border-radius: 10px;
  padding: 4px 12px;
  transition: box-shadow 0.2s;
}

:deep(.el-input__wrapper:hover) {
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.06), 0 0 0 1px rgba(0, 0, 0, 0.16);
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.06), 0 0 0 2px #5e35b1;
}

:deep(.el-input__inner) {
  height: 44px;
  font-size: 15px;
}

:deep(.el-input__prefix) {
  margin-right: 8px;
}

:deep(.el-input__prefix-inner) {
  font-size: 18px;
  color: #5f6368;
}

.login-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 10px;
  margin-top: 4px;
  letter-spacing: 2px;
}

.card-footer {
  text-align: center;
  color: #5f6368;
  font-size: 14px;
  margin-top: 4px;
}

.card-footer.text-muted {
  color: #909399;
  font-size: 13px;
}

.register-link {
  color: #5e35b1;
  font-weight: 500;
  margin-left: 4px;
}

.register-link:hover {
  text-decoration: underline;
}

/* ===== Responsive ===== */
@media (max-width: 900px) {
  .login-hero {
    display: none;
  }
  .login-panel {
    width: 100%;
  }
}
</style>
