<template>
  <div class="profile-complete-page">
    <div class="complete-card">
      <div class="card-header">
        <div class="step-icon">
          <el-icon :size="32"><UserFilled /></el-icon>
        </div>
        <h2>完善个人信息</h2>
        <p class="subtitle">欢迎注册！请选择您的所属学院，以继续使用系统。</p>
      </div>

      <el-form ref="formRef" :model="form" label-position="top">
        <el-form-item label="真实姓名">
          <el-input v-model="form.realName" disabled size="large" />
        </el-form-item>

        <el-form-item label="所属学院">
          <el-autocomplete
            v-model="form.searchText"
            :fetch-suggestions="querySearch"
            :trigger-on-focus="true"
            placeholder="请输入学院名称进行搜索"
            size="large"
            clearable
            style="width: 100%"
            :loading="searchLoading"
            @select="handleSelect"
          >
            <template #default="{ item }">
              <div class="org-suggestion">
                <span class="org-name">{{ item.value }}</span>
              </div>
            </template>
          </el-autocomplete>
          <div class="form-tip" v-if="selectedName">
            已选：<strong>{{ selectedName }}</strong>
          </div>
        </el-form-item>

        <div class="invite-note">
          <el-icon><InfoFilled /></el-icon>
          <span>请从搜索结果的列表中选择所属学院。若未找到您的学院，请联系院系管理员添加。</span>
        </div>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            class="submit-btn"
            :loading="submitting"
            :disabled="!selectedId"
            @click="handleSubmit"
          >
            {{ submitting ? '提交中...' : '确认并进入系统' }}
          </el-button>
        </el-form-item>

        <div class="skip-action">
          <el-button text @click="skip">稍后再说</el-button>
        </div>
      </el-form>
    </div>
    <footer class="copyright-footer">
      <span>版权归长沙创赛赢教育科技有限公司所有</span>
      <span class="footer-contact">合作联系：微信号327172508（添加好友时，注明单位+姓名）&nbsp;&nbsp;使用问题：微信号1175560106（添加好友时，注明单位+姓名）</span>
    </footer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'
import { searchOrganizations, updateProfile } from '../../api/auth'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const submitting = ref(false)
const searchLoading = ref(false)
const selectedId = ref(null)
const selectedName = ref('')

const form = reactive({
  realName: userStore.user?.realName || '',
  searchText: ''
})

onMounted(async () => {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
})

function querySearch(queryString, cb) {
  if (!queryString || queryString.trim().length === 0) {
    cb([])
    return
  }
  searchLoading.value = true
  searchOrganizations(queryString.trim())
    .then(res => {
      const list = res.data || []
      const suggestions = list.map(org => ({
        value: org.name,
        id: org.id
      }))
      searchLoading.value = false
      cb(suggestions)
    })
    .catch(() => {
      searchLoading.value = false
      cb([])
    })
}

function handleSelect(item) {
  selectedId.value = item.id
  selectedName.value = item.value
}

async function handleSubmit() {
  if (!selectedId.value) {
    ElMessage.warning('请从搜索结果的列表中选择所属学院')
    return
  }
  submitting.value = true
  try {
    await updateProfile(selectedId.value, null)
    await userStore.fetchMe()
    ElMessage.success('信息完善成功')
    router.push('/projects')
  } finally {
    submitting.value = false
  }
}

function skip() {
  router.push('/projects')
}
</script>

<style scoped>
.profile-complete-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 24px;
}
.copyright-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 16px;
  padding: 12px 20px;
  color: rgba(255, 255, 255, 0.7);
  font-size: 13px;
  margin-top: auto;
}
.footer-contact {
  color: rgba(255, 255, 255, 0.55);
  font-size: 12px;
}

.complete-card {
  width: 100%;
  max-width: 480px;
  background: #fff;
  border-radius: 16px;
  padding: 48px 40px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}

.card-header {
  text-align: center;
  margin-bottom: 36px;
}

.step-icon {
  width: 64px;
  height: 64px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border-radius: 16px;
  color: #fff;
  margin-bottom: 20px;
}

.card-header h2 {
  font-size: 24px;
  font-weight: 700;
  color: #202124;
  margin-bottom: 8px;
}

.subtitle {
  color: #5f6368;
  font-size: 14px;
  line-height: 1.5;
}

:deep(.el-form-item) {
  margin-bottom: 24px;
}

:deep(.el-form-item__label) {
  font-weight: 500;
  color: #202124;
  padding-bottom: 6px;
}

:deep(.el-input__wrapper) {
  border-radius: 10px;
  padding: 4px 12px;
}

:deep(.el-input__inner) {
  height: 44px;
  font-size: 15px;
}

.form-tip {
  font-size: 13px;
  color: #67c23a;
  margin-top: 6px;
}

.org-suggestion {
  padding: 6px 0;
}

.org-name {
  font-weight: 500;
  color: #303133;
}

.invite-note {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #909399;
  padding: 10px 12px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 28px;
}

.submit-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 10px;
  letter-spacing: 1px;
}

.skip-action {
  text-align: center;
  margin-top: 12px;
}
</style>
