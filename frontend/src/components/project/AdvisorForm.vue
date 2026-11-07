<template>
  <div class="advisor-form">
    <div class="section-header">
      <span>指导老师</span>
      <el-button type="primary" size="small" @click="addAdvisor">+ 添加老师</el-button>
    </div>
    <el-empty v-if="advisors.length === 0" description="请添加指导老师（可选）" />
    <div v-for="(a, idx) in advisors" :key="idx" class="advisor-card">
      <div class="card-header">
        <span>指导老师 {{ idx + 1 }}</span>
        <el-button type="danger" size="small" text @click="removeAdvisor(idx)">删除</el-button>
      </div>
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="姓名" :prop="`advisors.${idx}.name`" :rules="[{ required: true, message: '必填' }]">
            <el-input v-model="a.name" placeholder="老师姓名" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="电话">
            <el-input v-model="a.phone" placeholder="联系电话" />
          </el-form-item>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({ modelValue: { type: Array, default: () => [] } })
const emit = defineEmits(['update:modelValue'])
const advisors = ref(props.modelValue.map(a => ({ ...a })))

watch(advisors, (v) => emit('update:modelValue', v.map(a => ({ ...a }))), { deep: true })

// Sync from props with key guard to prevent emit loop
watch(() => props.modelValue, (v) => {
  const key = a => a.map(ad => ad.name + ad.phone).join('|')
  if (key(v) !== key(advisors.value)) {
    advisors.value = v.map(a => ({ ...a }))
  }
}, { deep: true, immediate: false })

function addAdvisor() {
  advisors.value.push({ name: '', phone: '' })
}

function removeAdvisor(idx) {
  advisors.value.splice(idx, 1)
}

defineExpose({ advisors })
</script>

<style scoped>
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-weight: 600;
}
.advisor-card {
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
