<template>
  <div class="team-member-form">
    <div class="section-header">
      <span>团队成员</span>
      <el-button type="primary" size="small" @click="addMember">+ 添加成员</el-button>
    </div>
    <el-empty v-if="members.length === 0" description="请添加团队成员" />
    <div v-for="(m, idx) in members" :key="idx" class="member-card">
      <div class="card-header">
        <span>成员 {{ idx + 1 }}</span>
        <el-button type="danger" size="small" text @click="removeMember(idx)">删除</el-button>
      </div>
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="姓名" :prop="`members.${idx}.name`" :rules="[{ required: true, message: '必填' }]">
            <el-input v-model="m.name" placeholder="姓名" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="角色">
            <el-select v-model="m.memberRole" style="width: 100%">
              <el-option label="负责人" value="LEADER" />
              <el-option label="成员" value="MEMBER" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="学号/工号">
            <el-input v-model="m.studentId" placeholder="学号或工号" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="院系">
            <el-input v-model="m.department" placeholder="所属院系" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="专业">
            <el-input v-model="m.major" placeholder="专业" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="电话" :prop="`members.${idx}.phone`" :rules="[{ required: true, message: '请输入联系电话' }, { pattern: /^1[3-9]\d{9}$/, message: '请输入有效的手机号码' }]">
            <el-input v-model="m.phone" placeholder="请输入手机号（必填）" />
          </el-form-item>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({ modelValue: { type: Array, default: () => [] }, defaultDepartment: { type: String, default: '' } })
const emit = defineEmits(['update:modelValue'])
const members = ref(props.modelValue.map(m => ({ ...m })))

watch(members, (v) => emit('update:modelValue', v.map(m => ({ ...m }))), { deep: true })

// Sync from props with key guard to prevent emit loop
watch(() => props.modelValue, (v) => {
  const key = a => a.map(m => m.name + m.studentId + m.memberRole).join('|')
  if (key(v) !== key(members.value)) {
    members.value = v.map(m => ({ ...m }))
  }
}, { deep: true, immediate: false })

function addMember() {
  const isFirst = members.value.length === 0
  members.value.push({ name: '', studentId: '', department: isFirst ? props.defaultDepartment : '', major: '', phone: '', memberRole: isFirst ? 'LEADER' : 'MEMBER', sortOrder: members.value.length })
}

function removeMember(idx) {
  members.value.splice(idx, 1)
}

defineExpose({ members })
</script>

<style scoped>
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-weight: 600;
}
.member-card {
  background: #f5f7fa;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 12px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
</style>
