<template>
  <div class="project-create">
    <div class="page-header">
      <el-button @click="$router.back()" text><el-icon><ArrowLeft /></el-icon>返回</el-button>
      <h2>创建项目</h2>
    </div>

    <el-steps :active="step" align-center class="steps" finish-status="success">
      <el-step title="基本信息" />
      <el-step title="团队成员" />
      <el-step title="指导老师" />
      <el-step title="确认报名" />
    </el-steps>

    <div class="step-content">
      <el-form ref="formRef" :model="form" label-position="top" @submit.prevent>

        <!-- Step 0: Basic Info -->
        <div v-show="step === 0">
          <ProjectBasicInfoForm :model-value="form" @update:model-value="Object.assign(form, $event)" />
          <div class="step-actions">
            <el-button @click="$router.back()">取消</el-button>
            <el-button type="primary" @click="nextStep">下一步</el-button>
          </div>
        </div>

        <!-- Step 1: Team Members -->
        <div v-show="step === 1">
          <TeamMemberForm ref="memberFormRef" :default-department="userStore.user?.organizationName || ''" />
          <div class="step-actions">
            <el-button @click="step--">上一步</el-button>
            <el-button type="primary" @click="handleMembersNext">下一步</el-button>
          </div>
        </div>

        <!-- Step 2: Advisors -->
        <div v-show="step === 2">
          <AdvisorForm ref="advisorFormRef" />
          <div class="step-actions">
            <el-button @click="step--">上一步</el-button>
            <el-button type="primary" @click="handleAdvisorsNext">下一步</el-button>
          </div>
        </div>

        <!-- Step 3: Confirm & Register -->
        <div v-show="step === 3" class="confirm-section">
          <h3>确认报名信息</h3>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="赛事">{{ selectedEvent?.name }}</el-descriptions-item>
            <el-descriptions-item label="赛道/组别">{{ selectedTrack ? (selectedTrack.groupName ? selectedTrack.name + '（' + selectedTrack.groupName + '）' : selectedTrack.name) : '' }}</el-descriptions-item>
            <el-descriptions-item label="项目名称">{{ form.name }}</el-descriptions-item>
            <el-descriptions-item label="项目简介">{{ form.description || '暂无' }}</el-descriptions-item>
          </el-descriptions>

          <h4 style="margin-top: 16px">团队成员（{{ memberList.length }}人）</h4>
          <el-table :data="memberList" size="small">
            <el-table-column prop="name" label="姓名" />
            <el-table-column label="角色">
              <template #default="{ row }">{{ row.memberRole === 'LEADER' ? '负责人' : '成员' }}</template>
            </el-table-column>
            <el-table-column prop="department" label="院系" />
            <el-table-column prop="major" label="专业" />
          </el-table>

          <h4 style="margin-top: 16px">指导老师（{{ advisorList.length }}人）</h4>
          <el-table :data="advisorList" size="small">
            <el-table-column prop="name" label="姓名" />
            <el-table-column prop="phone" label="电话" />
          </el-table>

          <div class="hint-box">
            <el-icon><InfoFilled /></el-icon>
            报名成功后，请在项目详情页上传参赛材料并提交审核
          </div>

          <div class="step-actions" style="margin-top: 20px">
            <el-button @click="step--">上一步</el-button>
            <el-button type="success" :loading="submitting" @click="handleSubmit">确认报名</el-button>
          </div>
        </div>

      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '../../stores/user'
import { createProject } from '../../api/project'
import { getEvent, listEvents } from '../../api/event'
import ProjectBasicInfoForm from '../../components/project/ProjectBasicInfoForm.vue'
import TeamMemberForm from '../../components/project/TeamMemberForm.vue'
import AdvisorForm from '../../components/project/AdvisorForm.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const formRef = ref(null)
const memberFormRef = ref(null)
const advisorFormRef = ref(null)
const step = ref(0)
const submitting = ref(false)
const events = ref([])
const selectedEvent = computed(() => events.value.find(e => e.id === form.eventId) || null)
const selectedTrack = computed(() => {
  if (!selectedEvent.value) return null
  return selectedEvent.value.tracks?.find(t => t.id === form.trackId) || null
})

const form = reactive({
  eventId: route.query.eventId ? Number(route.query.eventId) : null,
  trackId: null,
  name: '',
  description: '',
  members: [],
  advisors: []
})

const memberList = computed(() => form.members)

const advisorList = computed(() => form.advisors)

onMounted(async () => {
  const res = await listEvents()
  events.value = res.data || []
})

function nextStep() {
  if (step.value === 0) {
    if (!form.eventId) { ElMessage.warning('请选择赛事'); return }
    if (!form.trackId) { ElMessage.warning('请选择赛道'); return }
    if (!form.name) { ElMessage.warning('请输入项目名称'); return }
  }
  step.value++
}

function syncMembers() {
  form.members = (memberFormRef.value?.members || []).map(m => ({ ...m }))
}

function syncAdvisors() {
  form.advisors = (advisorFormRef.value?.advisors || []).map(a => ({ ...a }))
}

function handleMembersNext() {
  syncMembers()
  nextStep()
}

function handleAdvisorsNext() {
  syncAdvisors()
  nextStep()
}

async function handleSubmit() {
  try {
    await ElMessageBox.confirm('确认报名？报名后可在项目详情页上传材料并提交审核。', '确认报名', { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' })
  } catch { return }

  submitting.value = true
  try {
    // Check registration deadline
    if (form.eventId) {
      const res = await getEvent(form.eventId)
      const event = res.data
      if (event && event.registrationDeadline) {
        const deadline = new Date(event.registrationDeadline)
        if (new Date() > deadline) {
          ElMessage.error('报名已截止')
          return
        }
      }
    }

    // Ensure members and advisors are synced
    syncMembers()
    syncAdvisors()

    const projRes = await createProject({
      eventId: form.eventId,
      trackId: form.trackId,
      name: form.name,
      description: form.description,
      members: form.members,
      advisors: form.advisors
    })
    const project = projRes.data
    ElMessage.success('报名成功！请在项目详情页上传参赛材料并提交审核')
    router.push(`/projects/${project.id}`)
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}
.page-header h2 { font-size: 22px; }
.steps { margin-bottom: 32px; }
.step-content {
  background: #fff;
  border-radius: 8px;
  padding: 32px;
  min-height: 400px;
}
.step-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #ebeef5;
}
.confirm-section h3 { margin-bottom: 12px; }
.confirm-section h4 { margin-bottom: 8px; color: #606266; }
.hint-box {
  margin-top: 20px;
  padding: 12px 16px;
  background: #ecf5ff;
  color: #409eff;
  border-radius: 6px;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
