<template>
  <div class="advisor-editor">
    <div v-for="(a, i) in list" :key="i" class="adv-row">
      <el-select
        v-model="a.userId"
        filterable
        remote
        reserve-keyword
        :remote-method="searchTeachers"
        :loading="searching"
        placeholder="搜索姓名/工号选择指导教师"
        style="width: 240px"
        :disabled="readonly"
        @change="(val) => onPick(a, val)"
      >
        <el-option v-for="t in options" :key="t.userId" :label="`${t.name}（${t.workId}）`" :value="t.userId" />
        <!-- 保证回显已选教师 -->
        <el-option v-if="a.userId && !options.find(o => o.userId === a.userId)" :key="'cur'+i" :label="a.name || ('教师#' + a.userId)" :value="a.userId" />
      </el-select>
      <el-input v-model="a.title" placeholder="职称(选填)" style="width: 120px" :disabled="readonly" />
      <el-input v-model="a.phone" placeholder="电话(选填)" style="width: 150px" :disabled="readonly" />
      <el-button v-if="!readonly" text type="danger" @click="remove(i)">删除</el-button>
    </div>
    <el-button v-if="!readonly" size="small" @click="add">+ 添加指导教师</el-button>
    <div class="tip">指导教师须从已录入的教师账号中选择（按工号唯一），"指导教师审核"环节由所选教师本人在其审批工作台完成。</div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { getTeacherOptions } from '../../api/training'

const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  readonly: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue'])

const list = ref(normalize(props.modelValue))
const options = ref([])
const searching = ref(false)

watch(list, (v) => emit('update:modelValue', v), { deep: true })
watch(() => props.modelValue, (v) => { list.value = normalize(v) })

function normalize(v) {
  return (v || []).map(a => ({ userId: a.userId ?? null, name: a.name ?? '', title: a.title ?? '', phone: a.phone ?? '' }))
}

async function searchTeachers(kw) {
  searching.value = true
  try {
    const res = await getTeacherOptions(kw || '')
    options.value = res.data || []
  } finally { searching.value = false }
}

function onPick(a, userId) {
  const t = options.value.find(o => o.userId === userId)
  if (t) a.name = t.name
}

function add() { list.value.push({ userId: null, name: '', title: '', phone: '' }) }
function remove(i) { list.value.splice(i, 1) }

// 预加载一批,便于回显与初次点击
searchTeachers('')
</script>

<style scoped>
.adv-row { display: flex; gap: 8px; margin-bottom: 8px; align-items: center; }
.tip { color: #909399; font-size: 12px; margin-top: 4px; }
</style>
