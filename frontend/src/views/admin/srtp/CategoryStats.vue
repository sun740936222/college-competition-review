<template>
  <div class="page">
    <div class="page-header"><h2>项目分类统计</h2></div>

    <div class="filters">
      <el-select v-model="batchId" placeholder="选择批次" clearable style="width: 260px" @change="load">
        <el-option v-for="b in batches" :key="b.id" :label="b.name" :value="b.id" />
      </el-select>
      <el-select v-model="stage" style="width: 150px" @change="load">
        <el-option label="选题" value="TOPIC" />
        <el-option label="立项" value="PROJECT" />
        <el-option label="中期" value="MIDTERM" />
        <el-option label="结题" value="CONCLUSION" />
      </el-select>
      <el-button @click="load">查询</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" border stripe style="max-width: 560px">
      <el-table-column prop="label" label="项目类别" min-width="200" />
      <el-table-column prop="count" label="数量" width="120" align="center">
        <template #default="{ row }">
          <strong :class="{ total: row.category === 'TOTAL' }">{{ row.count }}</strong>
        </template>
      </el-table-column>
      <el-table-column label="占比" min-width="200">
        <template #default="{ row }">
          <el-progress v-if="row.category !== 'TOTAL' && total > 0"
            :percentage="Math.round(row.count / total * 100)" :stroke-width="14" />
          <span v-else>—</span>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { getCategoryStats, listBatches } from '../../../api/training'

const rows = ref([])
const batches = ref([])
const loading = ref(false)
const batchId = ref('')
const stage = ref('PROJECT')

const total = computed(() => rows.value.find(r => r.category === 'TOTAL')?.count || 0)

async function loadBatches() {
  const res = await listBatches({ current: 1, size: 100 })
  batches.value = res.data?.records || []
}
async function load() {
  loading.value = true
  try {
    const res = await getCategoryStats(stage.value, batchId.value || undefined)
    rows.value = res.data || []
  } finally { loading.value = false }
}

loadBatches(); load()
</script>

<style scoped>
.page-header h2 { margin: 0 0 16px; }
.filters { display: flex; gap: 10px; margin-bottom: 16px; }
.total { color: #409eff; font-size: 16px; }
</style>
