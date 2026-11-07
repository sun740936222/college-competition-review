<template>
  <div class="register-national-page">
    <div class="register-card">
      <div class="card-header">
        <el-button text @click="$router.push('/login')" class="back-link">
          <el-icon><ArrowLeft /></el-icon>返回登录
        </el-button>
        <h2 class="card-title">注册国赛组委会</h2>
        <p class="card-subtitle">创建国家级赛事管理机构，独立管理赛事的报名、评审全流程</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="handleRegister">
        <el-form-item label="赛事名称" prop="competitionName">
          <el-input v-model="form.competitionName" placeholder="例如：中国国际大学生创新大赛" size="large" />
        </el-form-item>

        <el-form-item label="赛事描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="赛事简介（可选）" />
        </el-form-item>

        <el-divider />

        <el-form-item label="管理员用户名" prop="username">
          <el-input v-model="form.username" placeholder="数字、字母或二者组合" size="large" />
        </el-form-item>

        <el-form-item label="管理员姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="真实姓名" size="large" />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="至少6位" size="large" />
        </el-form-item>

        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="（可选）" size="large" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="loading" size="large" class="submit-btn" @click="handleRegister">
            {{ loading ? '注册中...' : '创建国赛组委会' }}
          </el-button>
        </el-form-item>
      </el-form>

      <div class="card-footer">
        注册后自动生成邀请码，省级单位凭邀请码加入本赛事
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  competitionName: '',
  description: '',
  username: '',
  realName: '',
  password: '',
  phone: ''
})

const rules = {
  competitionName: [{ required: true, message: '请输入赛事名称', trigger: 'blur' }],
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度为3-50个字符', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9]+$/, message: '用户名只能包含数字和字母', trigger: 'blur' }
  ],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ]
}

async function handleRegister() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await userStore.registerNational(form)
    ElMessage.success('国赛组委会创建成功')
    router.push('/admin')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-national-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1a73e8 0%, #0d47a1 50%, #1557b0 100%);
  padding: 24px;
}

.register-card {
  width: 100%;
  max-width: 480px;
  background: #fff;
  border-radius: 16px;
  padding: 40px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}

.card-header {
  margin-bottom: 28px;
}

.back-link {
  margin-bottom: 16px;
}

.card-title {
  font-size: 24px;
  font-weight: 700;
  color: #202124;
  margin-bottom: 8px;
}

.card-subtitle {
  font-size: 14px;
  color: #5f6368;
  line-height: 1.6;
}

.submit-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 10px;
  letter-spacing: 2px;
}

.card-footer {
  text-align: center;
  color: #909399;
  font-size: 13px;
  margin-top: 8px;
}
</style>
