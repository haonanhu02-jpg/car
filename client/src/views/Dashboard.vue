<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
    <div class="card-grid">
      <div class="stat-card">
        <div class="stat-label">总车辆数</div>
        <div class="stat-number">{{ stats.totalVehicles }}</div>
      </div>
      <div class="stat-card warning">
        <div class="stat-label">今日到期提醒</div>
        <div class="stat-number">{{ stats.todayExpiring }}</div>
      </div>
      <div class="stat-card warning">
        <div class="stat-label">即将到期（30天内）</div>
        <div class="stat-number">{{ stats.expiringSoon }}</div>
      </div>
      <div class="stat-card danger">
        <div class="stat-label">已逾期</div>
        <div class="stat-number" style="color: #ff4d4f">{{ stats.overdue }}</div>
      </div>
    </div>

    <!-- 待办提醒列表 -->
    <div class="table-container">
      <div class="section-header">
        <h3>⚠️ 待办提醒列表（按紧急度排序）</h3>
      </div>

      <el-table :data="expiringVehicles" stripe v-loading="loading" empty-text="暂无待办提醒 🎉">
        <el-table-column prop="plateNumber" label="车牌号" width="120" />
        <el-table-column prop="brand" label="品牌" width="100" />
        <el-table-column label="到期类型" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.insuranceExpire && isExpiring(row.insuranceExpire)" type="warning" size="small">保险</el-tag>
            <el-tag v-if="row.inspectionExpire && isExpiring(row.inspectionExpire)" type="warning" size="small" style="margin-left:4px">年检</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="剩余/逾期天数" width="130">
          <template #default="{ row }">
            <span :class="getExpireClass(row)">{{ getExpireText(row) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="insuranceExpire" label="保险截止日" width="120" />
        <el-table-column prop="inspectionExpire" label="年检截止日" width="120" />
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="$router.push(`/vehicles/${row.id}`)">
              去处理
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { dashboardApi } from '@/api'

const loading = ref(false)
const stats = ref({ totalVehicles: 0, todayExpiring: 0, expiringSoon: 0, overdue: 0 })
const expiringVehicles = ref([])

onMounted(async () => {
  loading.value = true
  try {
    const [s, v] = await Promise.all([
      dashboardApi.getStatistics(),
      dashboardApi.getExpiringVehicles(),
    ])
    stats.value = s
    expiringVehicles.value = v
  } finally {
    loading.value = false
  }
})

function isExpiring(date) {
  if (!date) return false
  const d = new Date(date)
  const now = new Date()
  const diff = Math.ceil((d - now) / (1000 * 60 * 60 * 24))
  return diff <= 30
}

function getExpireText(row) {
  const today = new Date()
  const dates = []
  if (row.insuranceExpire) dates.push(new Date(row.insuranceExpire))
  if (row.inspectionExpire) dates.push(new Date(row.inspectionExpire))
  if (dates.length === 0) return '正常'

  const earliest = new Date(Math.min(...dates))
  const diff = Math.ceil((earliest - today) / (1000 * 60 * 60 * 24))
  if (diff < 0) return `已逾期 ${Math.abs(diff)} 天`
  if (diff === 0) return '今日到期'
  return `剩余 ${diff} 天`
}

function getExpireClass(row) {
  const today = new Date()
  const dates = []
  if (row.insuranceExpire) dates.push(new Date(row.insuranceExpire))
  if (row.inspectionExpire) dates.push(new Date(row.inspectionExpire))
  if (dates.length === 0) return 'status-normal'

  const earliest = new Date(Math.min(...dates))
  const diff = Math.ceil((earliest - today) / (1000 * 60 * 60 * 24))
  if (diff < 0) return 'status-overdue'
  if (diff <= 30) return 'status-expiring'
  return 'status-normal'
}
</script>

<style scoped>
.section-header {
  margin-bottom: 16px;
}

.section-header h3 {
  margin: 0;
  font-size: 16px;
  color: #303133;
}
</style>
