<template>
  <div class="register-page">
    <div class="register-card">
      <h2>注册校级管理员</h2>
      <p class="subtitle">需要省级组委会提供的邀请码</p>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large">
        <el-form-item label="上级邀请码" prop="parentInviteCode">
          <el-input v-model="form.parentInviteCode" placeholder="输入省级组委会的邀请码（PROV-开头）" />
        </el-form-item>
        <el-form-item label="学校名称" prop="schoolName">
          <el-input v-model="form.schoolName" placeholder="如：北京航空航天大学" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="可选描述" />
        </el-form-item>
        <el-divider />
        <el-form-item label="管理员用户名" prop="username">
          <el-input v-model="form.username" placeholder="数字、字母或二者组合" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="真实姓名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="至少6位" />
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="form.phone" placeholder="联系电话（可选）" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" style="width:100%" @click="handleSubmit">
            注册校级管理员
          </el-button>
        </el-form-item>
      </el-form>
      <p class="footer-link">
        已有账号？<router-link to="/login">立即登录</router-link>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../../stores/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  parentInviteCode: '',
  schoolName: '',
  description: '',
  username: '',
  password: '',
  realName: '',
  phone: ''
})

const rules = {
  parentInviteCode: [{ required: true, message: '请输入省级组委会邀请码', trigger: 'blur' }],
  schoolName: [{ required: true, message: '请输入学校名称', trigger: 'blur' }],
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9]+$/, message: '用户名只能包含数字和字母', trigger: 'blur' }
  ],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await userStore.registerSchool({ ...form })
    ElMessage.success('校级管理员注册成功')
    router.push('/admin')
  } catch (e) {
    // error shown by interceptor
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1677ff 0%, #0958d9 100%);
  padding: 20px;
}
.register-card {
  background: #fff;
  border-radius: 8px;
  padding: 40px;
  width: 100%;
  max-width: 480px;
  box-shadow: 0 8px 24px rgba(0,0,0,.15);
}
.register-card h2 {
  text-align: center;
  margin-bottom: 4px;
  color: #303133;
}
.subtitle {
  text-align: center;
  color: #909399;
  font-size: 13px;
  margin-bottom: 24px;
}
.footer-link {
  text-align: center;
  margin-top: 16px;
  color: #909399;
  font-size: 13px;
}
.footer-link a {
  color: #1677ff;
}
</style>
