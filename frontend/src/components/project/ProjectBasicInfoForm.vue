<template>
  <div class="basic-info-form">
    <el-form-item label="赛事" prop="eventId">
      <el-select v-model="form.eventId" placeholder="请选择赛事" @change="onEventChange" style="width: 100%" :teleported="false">
        <el-option v-for="e in events" :key="e.id" :label="e.name" :value="e.id" />
      </el-select>
    </el-form-item>
    <el-form-item label="赛道/组别" prop="trackId">
      <el-select v-model="form.trackId" placeholder="请选择赛道/组别" style="width: 100%" :teleported="false">
        <el-option v-for="t in tracks" :key="t.id" :label="t.groupName ? t.name + ' — ' + t.groupName : t.name" :value="t.id" />
      </el-select>
    </el-form-item>
    <el-form-item label="项目名称" prop="name">
      <el-input v-model="form.name" placeholder="请输入项目名称" maxlength="200" show-word-limit />
    </el-form-item>
    <el-form-item label="项目简介" prop="description">
      <el-input v-model="form.description" type="textarea" :rows="4" placeholder="请输入项目简介" maxlength="2000" show-word-limit />
    </el-form-item>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { listEvents, getEvent } from '../../api/event'

const props = defineProps({
  modelValue: { type: Object, required: true }
})
const emit = defineEmits(['update:modelValue'])

const form = ref({ ...props.modelValue })
const events = ref([])
const tracks = ref([])

// Fields this component manages — only emit these, never overwrite members/advisors/materials
const BASIC_FIELDS = ['eventId', 'trackId', 'name', 'description']

// Only watch BASIC_FIELDS values — never deep-watch the entire props.modelValue
// (which also contains members/advisors/materials from the parent reactive form)
watch(
  () => BASIC_FIELDS.map(k => form.value[k]),
  () => {
    const payload = {}
    BASIC_FIELDS.forEach(k => { payload[k] = form.value[k] })
    emit('update:modelValue', payload)
  }
)

watch(
  () => BASIC_FIELDS.map(k => props.modelValue[k]),
  (newVals) => {
    const next = {}
    BASIC_FIELDS.forEach((k, i) => { next[k] = newVals[i] })
    form.value = next
  }
)

onMounted(async () => {
  const res = await listEvents()
  events.value = res.data || []
  if (form.value.eventId) {
    await loadTracks(form.value.eventId)
  }
})

async function onEventChange(eventId) {
  form.value.trackId = null
  await loadTracks(eventId)
}

async function loadTracks(eventId) {
  const res = await getEvent(eventId)
  tracks.value = res.data?.tracks || []
}
</script>
