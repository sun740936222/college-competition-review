<template>
  <div class="project-edit">
    <div class="page-header">
      <el-button @click="$router.back()" text><el-icon><ArrowLeft /></el-icon>返回</el-button>
      <h2>编辑项目</h2>
    </div>

    <div v-loading="loading">
      <el-form ref="formRef" :model="form" label-position="top">
        <el-card class="section">
          <template #header><span class="card-title">基本信息</span></template>
          <el-form-item label="项目名称" prop="name" :rules="[{ required: true, message: '必填' }]">
            <el-input v-model="form.name" />
          </el-form-item>
          <el-form-item label="项目简介">
            <el-input v-model="form.description" type="textarea" :rows="4" />
          </el-form-item>
          <el-button type="primary" :loading="saving" @click="handleSaveBasic">保存基本信息</el-button>
        </el-card>

        <el-card class="section">
          <template #header><span class="card-title">团队成员</span></template>
          <TeamMemberForm v-model="members" />
          <el-button type="primary" :loading="saving" @click="handleSaveMembers">保存成员信息</el-button>
        </el-card>

        <el-card class="section">
          <template #header><span class="card-title">指导老师</span></template>
          <AdvisorForm v-model="advisors" />
          <el-button type="primary" :loading="saving" @click="handleSaveAdvisors">保存指导老师</el-button>
        </el-card>

        <el-card class="section">
          <template #header><span class="card-title">项目材料</span></template>
          <MaterialUploader v-model="materials" :require-video="project.requireVideo" />
          <el-button type="primary" :loading="saving" @click="handleSaveMaterials">上传材料</el-button>
        </el-card>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getProject, updateProject, addMember, updateMember, deleteMember, addAdvisor, updateAdvisor, deleteAdvisor, uploadMaterial, deleteMaterial } from '../../api/project'
import TeamMemberForm from '../../components/project/TeamMemberForm.vue'
import AdvisorForm from '../../components/project/AdvisorForm.vue'
import MaterialUploader from '../../components/project/MaterialUploader.vue'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const project = ref({})

const form = reactive({ name: '', description: '' })
const members = ref([])
const advisors = ref([])
const materials = ref([])

onMounted(async () => {
  loading.value = true
  try {
    const res = await getProject(route.params.id)
    project.value = res.data || {}
    form.name = project.value.name || ''
    form.description = project.value.description || ''
    members.value = project.value.teamMembers || []
    advisors.value = project.value.advisors || []
    materials.value = project.value.materials || []
  } finally {
    loading.value = false
  }
})

async function handleSaveBasic() {
  if (!form.name) { ElMessage.warning('请输入项目名称'); return }
  saving.value = true
  try {
    await updateProject(project.value.id, {
      eventId: project.value.eventId, trackId: project.value.trackId,
      name: form.name, description: form.description
    })
    ElMessage.success('基本信息已保存')
  } finally { saving.value = false }
}

async function handleSaveMembers() {
  saving.value = true
  try {
    // Delete removed members
    for (const old of (project.value.teamMembers || [])) {
      if (!members.value.find(m => m.id === old.id)) await deleteMember(project.value.id, old.id)
    }
    // Update or add
    for (const m of members.value) {
      if (m.id) {
        await updateMember(project.value.id, m.id, m)
      } else {
        await addMember(project.value.id, m)
      }
    }
    ElMessage.success('成员信息已保存')
    // Reload
    const res = await getProject(project.value.id)
    project.value = res.data || {}
    members.value = project.value.teamMembers || []
  } finally { saving.value = false }
}

async function handleSaveAdvisors() {
  saving.value = true
  try {
    for (const old of (project.value.advisors || [])) {
      if (!advisors.value.find(a => a.id === old.id)) await deleteAdvisor(project.value.id, old.id)
    }
    for (const a of advisors.value) {
      if (a.id) {
        await updateAdvisor(project.value.id, a.id, a)
      } else if (a.name) {
        await addAdvisor(project.value.id, a)
      }
    }
    ElMessage.success('指导老师已保存')
    const res = await getProject(project.value.id)
    project.value = res.data || {}
    advisors.value = project.value.advisors || []
  } finally { saving.value = false }
}

async function handleSaveMaterials() {
  saving.value = true
  try {
    for (const m of materials.value) {
      if (m.file) {
        await uploadMaterial(project.value.id, m.materialType, m.file)
      }
    }
    ElMessage.success('材料上传完成')
    const res = await getProject(project.value.id)
    project.value = res.data || {}
    materials.value = project.value.materials || []
  } finally { saving.value = false }
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
.section { margin-bottom: 16px; }
.card-title { font-weight: 600; }
</style>
