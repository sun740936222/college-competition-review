<template>
  <div class="page">
    <div class="page-header"><h2>项目全流程追踪</h2></div>

    <div class="filters">
      <el-select v-model="batchId" placeholder="选择批次" clearable style="width: 320px" @change="load">
        <el-option v-for="b in batches" :key="b.id" :label="b.level ? `${b.name}（${b.level}）` : b.name" :value="b.id" />
      </el-select>
      <el-input v-model="keyword" placeholder="搜索项目名称" clearable style="width: 200px" @keyup.enter="load" @clear="load" />
      <el-button @click="load">查询</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" border stripe>
      <el-table-column prop="projectNo" label="项目编号" width="150" />
      <el-table-column prop="name" label="项目名称" min-width="200" />
      <el-table-column label="所属批次" min-width="200">
        <template #default="{ row }">
          <div>{{ row.batchName || '—' }}</div>
          <el-tag v-if="row.batchLevel" size="small" :type="levelTagType(row.batchLevel)" style="margin-top: 2px">{{ row.batchLevel }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="选题" width="110"><template #default="{ row }"><el-tag size="small" :type="tagType(row.topicStatus)">{{ label(row.topicStatus) }}</el-tag></template></el-table-column>
      <el-table-column label="立项" width="110"><template #default="{ row }"><el-tag size="small" :type="tagType(row.projectStatus)">{{ label(row.projectStatus) }}</el-tag></template></el-table-column>
      <el-table-column label="中期" width="110"><template #default="{ row }"><el-tag size="small" :type="tagType(row.midtermStatus)">{{ label(row.midtermStatus) }}</el-tag></template></el-table-column>
      <el-table-column label="结题" width="110"><template #default="{ row }"><el-tag size="small" :type="tagType(row.conclusionStatus)">{{ label(row.conclusionStatus) }}</el-tag></template></el-table-column>
    </el-table>

    <el-pagination
      style="margin-top: 16px; justify-content: flex-end"
      layout="total, prev, pager, next"
      :total="total" :current-page="current" :page-size="size"
      @current-change="(p) => { current = p; load() }"
    />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { getTracking, listBatches, STATUS_LABELS, STATUS_TYPES } from '../../../api/training'

const rows = ref([]); const batches = ref([]); const loading = ref(false)
const batchId = ref(''); const keyword = ref('')
const current = ref(1); const size = ref(10); const total = ref(0)

function label(s) {
  if (!s || s === '—') return '—'
  if (s === '未填报') return '未填报'
  return STATUS_LABELS[s] || s
}
function tagType(s) {
  if (!s || s === '—' || s === '未填报') return 'info'
  return STATUS_TYPES[s] || 'info'
}
const LEVEL_TAG = { '校级': 'info', '省级': 'warning', '国家级': 'danger' }
function levelTagType(lv) { return LEVEL_TAG[lv] || 'info' }

async function loadBatches() {
  const res = await listBatches({ current: 1, size: 100 })
  batches.value = res.data?.records || []
}
async function load() {
  loading.value = true
  try {
    const res = await getTracking({ batchId: batchId.value || undefined, keyword: keyword.value, current: current.value, size: size.value })
    rows.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally { loading.value = false }
}

loadBatches(); load()
</script>

<style scoped>
.page-header h2 { margin: 0 0 16px; }
.filters { display: flex; gap: 10px; margin-bottom: 16px; }
</style>
