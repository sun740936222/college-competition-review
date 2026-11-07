<template>
  <div class="dynamic-fields" v-if="fields.length">
    <el-form-item
      v-for="f in fields"
      :key="f.id"
      :label="f.label"
      :required="f.required"
    >
      <el-input
        v-if="f.fieldType === 'TEXT'"
        v-model="model[f.fieldKey]"
        :placeholder="'请输入' + f.label"
      />
      <el-input
        v-else-if="f.fieldType === 'TEXTAREA'"
        v-model="model[f.fieldKey]"
        type="textarea"
        :rows="3"
        :placeholder="'请输入' + f.label"
      />
      <el-input-number
        v-else-if="f.fieldType === 'NUMBER'"
        v-model="model[f.fieldKey]"
        :controls="false"
        style="width: 100%"
      />
      <el-date-picker
        v-else-if="f.fieldType === 'DATE'"
        v-model="model[f.fieldKey]"
        type="date"
        value-format="YYYY-MM-DD"
        style="width: 100%"
      />
      <el-select
        v-else-if="f.fieldType === 'SELECT'"
        v-model="model[f.fieldKey]"
        :placeholder="'请选择' + f.label"
        style="width: 100%"
      >
        <el-option v-for="opt in parseOptions(f.options)" :key="opt" :label="opt" :value="opt" />
      </el-select>
      <el-select
        v-else-if="f.fieldType === 'MULTISELECT'"
        v-model="model[f.fieldKey]"
        multiple
        :placeholder="'请选择' + f.label"
        style="width: 100%"
      >
        <el-option v-for="opt in parseOptions(f.options)" :key="opt" :label="opt" :value="opt" />
      </el-select>
      <el-input v-else v-model="model[f.fieldKey]" :placeholder="'请输入' + f.label" />
    </el-form-item>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { getCustomFields } from '../../api/training'

const props = defineProps({
  stage: { type: String, required: true },
  batchId: { type: [Number, String], default: null },
  modelValue: { type: Object, default: () => ({}) }
})
const emit = defineEmits(['update:modelValue'])

const fields = ref([])
const model = ref({ ...props.modelValue })

// 内部 model → 父组件：仅在内容真正变化时向上 emit，避免与下面的 props→model 同步互相触发形成死循环
watch(model, (v) => {
  const propJson = JSON.stringify(props.modelValue || {})
  const nextJson = JSON.stringify(v || {})
  if (propJson !== nextJson) emit('update:modelValue', v)
}, { deep: true })

// 父组件 → 内部 model：只有传入值和内部实际值不同才 assign，避免因 emit 后父组件回传相同内容再次触发同步
watch(() => props.modelValue, (v) => {
  const nextJson = JSON.stringify(v || {})
  const curJson = JSON.stringify(model.value || {})
  if (nextJson !== curJson) model.value = v ? { ...v } : {}
})
watch(() => props.batchId, () => load())

function parseOptions(options) {
  if (!options) return []
  try {
    const arr = typeof options === 'string' ? JSON.parse(options) : options
    return Array.isArray(arr) ? arr : []
  } catch { return [] }
}

async function load() {
  try {
    const res = await getCustomFields(props.stage, props.batchId)
    fields.value = res.data || []
  } catch { fields.value = [] }
}

onMounted(load)
</script>
