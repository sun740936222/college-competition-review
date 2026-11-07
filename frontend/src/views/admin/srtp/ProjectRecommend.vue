<template>
  <div class="page">
    <div class="page-header"><h2>项目推荐 / 晋级</h2></div>
    <el-alert type="info" :closable="false" show-icon style="margin-bottom: 12px"
      title="从源批次(如校级)中勾选【已通过】的立项项目，推荐到更高级别的目标批次(省级/国家级)。推荐后将在目标批次生成对应项目并归档，可在目标批次的列表/统计/追踪中查看。" />

    <div class="filters">
      <span class="lbl">源批次：</span>
      <el-select v-model="fromBatchId" placeholder="选择源批次" clearable style="width: 240px" @change="load">
        <el-option v-for="b in batches" :key="b.id" :label="batchLabel(b)" :value="b.id" />
      </el-select>
      <span class="lbl">推荐到：</span>
      <el-select v-model="toBatchId" placeholder="选择目标批次(更高级别)" clearable style="width: 240px" :disabled="!fromBatchId">
        <el-option v-for="b in eligibleTargets" :key="b.id" :label="batchLabel(b)" :value="b.id" />
      </el-select>
      <el-button type="primary" :disabled="!selected.length || !toBatchId" :loading="promoting" @click="doPromote">
        推荐所选（{{ selected.length }}）
      </el-button>
    </div>
    <el-alert v-if="fromBatchId && !eligibleTargets.length" type="warning" show-icon :closable="false" style="margin-bottom: 12px"
      :title="`当前系统里没有级别高于源批次(${sourceLevelLabel || '未知'})的目标批次，请先到「批次管理」新建一个${sourceLevelLabel === '校级' ? '省级' : (sourceLevelLabel === '省级' ? '国家级' : '省级/国家级')}批次后再来推荐`" />

    <el-table :data="rows" v-loading="loading" border stripe @selection-change="onSel" ref="tableRef" row-key="id">
      <el-table-column type="selection" width="50" :selectable="row => !promotedIds.includes(row.id)" />
      <el-table-column prop="projectNo" label="项目编号" width="150" />
      <el-table-column prop="name" label="项目名称" min-width="220" />
      <el-table-column label="类别" width="150">
        <template #default="{ row }">{{ CATEGORY_LABELS[row.projectType] || row.projectType || '—' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="130">
        <template #default="{ row }">
          <el-tag v-if="promotedIds.includes(row.id)" type="success">已推荐</el-tag>
          <el-tag v-else type="info">可推荐</el-tag>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && !rows.length" description="请选择源批次；仅显示已通过立项的项目" />
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminProjects, listBatches, promoteProjects, getPromotedIds, CATEGORY_LABELS } from '../../../api/training'

const LEVEL_RANK = { '校级': 1, '省级': 2, '国家级': 3 }
function levelRank(lv) { return LEVEL_RANK[lv] || 0 }

const batches = ref([])
const rows = ref([])
const loading = ref(false)
const promoting = ref(false)
const fromBatchId = ref('')
const toBatchId = ref('')
const selected = ref([])
const promotedIds = ref([])
const tableRef = ref(null)

function batchLabel(b) { return b.level ? `${b.name}（${b.level}）` : b.name }
function onSel(rows) { selected.value = rows }

const sourceBatch = computed(() => batches.value.find(b => b.id === fromBatchId.value) || null)
const sourceLevelLabel = computed(() => sourceBatch.value?.level || '')
// 目标批次候选：级别严格高于源批次(级别未知时不限制方向,只排除自身)
const eligibleTargets = computed(() => {
  const src = sourceBatch.value
  if (!src) return []
  const srcRank = levelRank(src.level)
  return batches.value.filter(b => {
    if (b.id === src.id) return false
    if (srcRank === 0 || levelRank(b.level) === 0) return true
    return levelRank(b.level) > srcRank
  })
})
// 源批次变化时清掉已选目标,避免残留一个不合规的 toBatchId
watch(fromBatchId, () => { toBatchId.value = '' })

async function loadBatches() {
  const res = await listBatches({ current: 1, size: 100 })
  batches.value = res.data?.records || []
}
async function load() {
  if (!fromBatchId.value) { rows.value = []; promotedIds.value = []; return }
  loading.value = true
  try {
    const [pr, pid] = await Promise.all([
      adminProjects({ batchId: fromBatchId.value, status: 'APPROVED', current: 1, size: 500 }),
      getPromotedIds(fromBatchId.value)
    ])
    rows.value = pr.data?.records || []
    promotedIds.value = pid.data || []
  } finally { loading.value = false }
}
async function doPromote() {
  if (!toBatchId.value) { ElMessage.warning('请选择目标批次'); return }
  const ids = selected.value.map(r => r.id)
  if (!ids.length) { ElMessage.warning('请勾选要推荐的项目'); return }
  try {
    await ElMessageBox.confirm(`确定将所选 ${ids.length} 个项目推荐到目标批次？`, '确认推荐', { type: 'warning' })
  } catch { return }
  promoting.value = true
  try {
    const res = await promoteProjects(toBatchId.value, ids)
    ElMessage.success(res.message || '推荐完成')
    if (res.data?.errors?.length) ElMessageBox.alert(res.data.errors.join('\n'), '推荐明细', { type: 'warning' })
    tableRef.value?.clearSelection()
    load()
  } finally { promoting.value = false }
}

loadBatches()
</script>

<style scoped>
.page-header h2 { margin: 0 0 16px; }
.filters { display: flex; align-items: center; gap: 8px; margin-bottom: 16px; flex-wrap: wrap; }
.filters .lbl { font-size: 13px; color: #606266; }
</style>
