<template>
  <div class="home-view">
    <div class="page-header">
      <h2>赛事列表</h2>
      <p class="subtitle">选择赛事报名参赛</p>
    </div>

    <div v-loading="loading">
      <el-empty v-if="!loading && events.length === 0" description="暂无开放的赛事" />
      <div v-else class="event-grid">
        <div v-for="event in events" :key="event.id" class="event-card" @click="goCreate(event)">
          <h3 class="event-name">{{ event.name }}</h3>
          <p class="event-desc">{{ event.description || '暂无描述' }}</p>
          <div class="event-meta">
            <span>
              <el-icon><Clock /></el-icon>
              报名截止：{{ formatDate(event.registrationDeadline) }}
            </span>
            <span>
              <el-icon><Document /></el-icon>
              提交截止：{{ formatDate(event.submissionDeadline) }}
            </span>
          </div>
          <div class="event-tracks">
            <el-tag v-for="track in event.tracks" :key="track.id" size="small" type="info">
              {{ track.groupName ? track.name + ' / ' + track.groupName : track.name }}
            </el-tag>
          </div>
          <el-button type="primary" style="margin-top: 12px">立即报名</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listEvents } from '../api/event'

const router = useRouter()
const events = ref([])
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    const res = await listEvents()
    events.value = res.data || []
  } finally {
    loading.value = false
  }
})

function goCreate(event) {
  router.push({ path: '/projects/create', query: { eventId: event.id } })
}

function formatDate(date) {
  if (!date) return '待定'
  return new Date(date).toLocaleDateString('zh-CN')
}
</script>

<style scoped>
.page-header {
  margin-bottom: 24px;
}
.page-header h2 {
  font-size: 24px;
  color: #303133;
}
.subtitle {
  color: #909399;
  margin-top: 4px;
}
.event-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 20px;
}
.event-card {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  cursor: pointer;
  border: 1px solid #e4e7ed;
  transition: box-shadow 0.3s;
}
.event-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}
.event-name {
  font-size: 18px;
  color: #303133;
  margin-bottom: 8px;
}
.event-desc {
  color: #606266;
  font-size: 14px;
  margin-bottom: 12px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.event-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 13px;
  color: #909399;
  margin-bottom: 8px;
}
.event-meta span {
  display: flex;
  align-items: center;
  gap: 4px;
}
.event-tracks {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
</style>
