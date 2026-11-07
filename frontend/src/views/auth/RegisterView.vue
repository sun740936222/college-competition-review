<template>
  <div class="register-page">
    <div class="register-content">
    <div class="register-hero">
      <div class="hero-content">
        <div class="hero-badge">赛事平台</div>
        <h1 class="hero-title">参赛者注册<br />开启创新之旅</h1>
        <p class="hero-desc">注册账号即可报名参赛，与全校师生同台竞技。</p>
        <div class="hero-features">
          <div class="hero-feature">
            <span class="feature-icon"><el-icon><Trophy /></el-icon></span>
            <span>涵盖多学科赛事</span>
          </div>
          <div class="hero-feature">
            <span class="feature-icon"><el-icon><Users /></el-icon></span>
            <span>支持团队协作参赛</span>
          </div>
          <div class="hero-feature">
            <span class="feature-icon"><el-icon><TrendCharts /></el-icon></span>
            <span>实时查看评审进度</span>
          </div>
        </div>
      </div>
      <div class="hero-bg-shapes">
        <div class="shape shape-1"></div>
        <div class="shape shape-2"></div>
        <div class="shape shape-3"></div>
      </div>
    </div>

    <div class="register-panel">
      <div class="register-card">
        <div class="card-header">
          <h2 class="card-title">参赛者注册</h2>
          <p class="card-subtitle">填写信息完成注册</p>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          class="register-form"
          @submit.prevent="handleRegister"
        >
          <el-form-item prop="inviteCode">
            <template #label>
              <span class="form-label">邀请码 <span class="optional">（选填）</span></span>
            </template>
            <el-input
              v-model="form.inviteCode"
              placeholder="请输入院系管理员提供的邀请码，自动关联学院"
              :prefix-icon="Key"
              size="large"
            />
            <div class="form-tip">输入邀请码可自动关联所属学院；没有邀请码也可注册，登录后从列表中选择学院</div>
          </el-form-item>

          <el-form-item prop="username">
            <template #label><span class="form-label">用户名</span></template>
            <el-input
              v-model="form.username"
              placeholder="数字、字母或二者组合"
              :prefix-icon="User"
              size="large"
            />
          </el-form-item>

          <el-form-item prop="realName">
            <template #label><span class="form-label">真实姓名</span></template>
            <el-input
              v-model="form.realName"
              placeholder="请输入姓名"
              :prefix-icon="EditPen"
              size="large"
            />
          </el-form-item>

          <el-form-item prop="password">
            <template #label><span class="form-label">密码</span></template>
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请设置密码（至少6位）"
              :prefix-icon="Lock"
              show-password
              size="large"
            />
          </el-form-item>

          <el-form-item prop="phone">
            <template #label><span class="form-label">手机号</span></template>
            <el-input
              v-model="form.phone"
              placeholder="选填"
              :prefix-icon="Iphone"
              size="large"
            />
          </el-form-item>

          <el-form-item prop="email">
            <template #label><span class="form-label">邮箱</span></template>
            <el-input
              v-model="form.email"
              placeholder="选填"
              :prefix-icon="Message"
              size="large"
            />
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              :loading="loading"
              size="large"
              class="register-btn"
              @click="handleRegister"
            >
              {{ loading ? '注册中...' : '注 册' }}
            </el-button>
          </el-form-item>
        </el-form>

        <div class="card-footer">
          已有账号？
          <router-link to="/login" class="login-link">立即登录</router-link>
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
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../../stores/user'
import {
  User, Lock, Key, EditPen, Iphone, Message
} from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  inviteCode: '',
  username: '',
  password: '',
  realName: '',
  phone: '',
  email: ''
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度为3-50个字符', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9]+$/, message: '用户名只能包含数字和字母', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6个字符', trigger: 'blur' }
  ],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }]
}

async function handleRegister() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const data = { ...form }
    if (!data.inviteCode) data.inviteCode = undefined
    await userStore.register(data)
    const user = userStore.user
    if (user && !user.organizationId) {
      await router.push('/profile/complete')
    } else {
      await router.push('/projects')
    }
  } catch (e) {
    console.error('Register error:', e)
    const msg = e.response?.data?.message || e.message || '注册失败，请重试'
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-page {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}
.register-content {
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
.register-hero {
  flex: 1;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0d47a1 0%, #1a73e8 50%, #42a5f5 100%);
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
  margin-bottom: 40px;
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
  left: -200px;
}

.shape-2 {
  width: 300px;
  height: 300px;
  background: #fff;
  bottom: -80px;
  right: -80px;
}

.shape-3 {
  width: 200px;
  height: 200px;
  background: #fff;
  top: 30%;
  right: 10%;
}

/* ===== Register Panel ===== */
.register-panel {
  width: 600px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8f9fa;
  padding: 48px;
}

.register-card {
  width: 100%;
  max-width: 480px;
}

.card-header {
  margin-bottom: 32px;
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

.register-form {
  margin-bottom: 24px;
}

.optional {
  font-weight: 400;
  font-size: 12px;
  color: #909399;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  line-height: 1.4;
}

:deep(.el-form-item) {
  margin-bottom: 22px;
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
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.06), 0 0 0 2px #1a73e8;
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

.register-btn {
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
}

.login-link {
  color: #1a73e8;
  font-weight: 500;
  margin-left: 4px;
}

.login-link:hover {
  text-decoration: underline;
}

/* ===== Responsive ===== */
@media (max-width: 960px) {
  .register-hero {
    display: none;
  }
  .register-panel {
    width: 100%;
  }
}
</style>