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
        <div class="stat-label">保险/年检已过期车辆</div>
        <div class="stat-number" style="color: #ff4d4f">{{ stats.overdue }}</div>
      </div>
    </div>

    <!-- 管理员待办：注册申请审批 -->
    <div v-if="userStore.isAdmin" class="todo-section">
      <div class="todo-card" @click="goToRegistrationApproval">
        <div class="todo-icon">📝</div>
        <div class="todo-content">
          <div class="todo-label">注册申请待审批</div>
          <div class="todo-number" :class="{ 'has-pending': pendingRegCount > 0 }">
            {{ pendingRegCount }}
          </div>
        </div>
        <div class="todo-action">
          <el-button type="primary" size="small" :disabled="pendingRegCount === 0">
            去处理
          </el-button>
        </div>
      </div>
    </div>

    <!-- 待办提醒列表 -->
    <div class="table-container">
      <div class="section-header">
        <h3>⚠️ 待办提醒列表（按紧急度排序）</h3>
        <el-button type="primary" link @click="router.push('/reminders')">
          查看全部（{{ pendingReminderTotal }}）
        </el-button>
      </div>

      <el-table :data="pendingReminders" stripe v-loading="loading" empty-text="暂无待办提醒 🎉">
        <el-table-column label="车牌号" min-width="125">
          <template #default="{ row }">
            <el-link type="primary" @click="router.push(`/vehicles/${row.vehicleId}`)">
              {{ row.plateNumber || `车辆#${row.vehicleId}` }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column label="提醒类型" width="100">
          <template #default="{ row }">{{ row.type === 0 ? '保险' : '年检' }}</template>
        </el-table-column>
        <el-table-column label="当前提醒节点" width="130">
          <template #default="{ row }">提前 {{ row.nodeDays }} 天</template>
        </el-table-column>
        <el-table-column label="到期日期 / 到期情况" min-width="180">
          <template #default="{ row }">
            <div>{{ row.expireDate || '-' }}</div>
            <div class="expire-state" :class="expireStateClass(row.remainingDays)">
              {{ row.expireStatus || '-' }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="remindDate" label="最近提醒日期" width="130" />
        <el-table-column label="处理状态" width="135">
          <template #default="{ row }">
            <el-tag v-if="row.status === 0" type="warning">⏳ 待处理</el-tag>
            <el-tag v-else type="danger">⏰ 超时未处理</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="router.push(`/vehicles/${row.vehicleId}`)">
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
import { useRouter } from 'vue-router'
import { dashboardApi, registrationApi, reminderApi } from '@/api'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const stats = ref({ totalVehicles: 0, todayExpiring: 0, expiringSoon: 0, overdue: 0 })
const pendingReminders = ref([])
const pendingReminderTotal = ref(0)
const pendingRegCount = ref(0)

onMounted(async () => {
  // 主数据先加载，不受待办请求影响
  loading.value = true
  try {
    const [s, reminderPage] = await Promise.all([
      dashboardApi.getStatistics(),
      reminderApi.list({ scope: 'todo', page: 1, size: 20 }),
    ])
    stats.value = s
    pendingReminders.value = reminderPage.records || []
    pendingReminderTotal.value = reminderPage.total || 0
  } finally {
    loading.value = false
  }

  // 注册申请待办是管理员的增强提示：独立加载、静默失败，绝不阻断工作台主体
  if (userStore.isAdmin) {
    try {
      const list = await registrationApi.list(0, { silent: true })
      pendingRegCount.value = list.length
    } catch {
      pendingRegCount.value = 0
    }
  }
})

function goToRegistrationApproval() {
  router.push({ path: '/settings', query: { tab: 'registration' } })
}

function expireStateClass(remainingDays) {
  if (remainingDays == null) return ''
  if (remainingDays < 0) return 'is-overdue'
  if (remainingDays <= 30) return 'is-warning'
  return 'is-normal'
}
</script>

<style scoped>
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.section-header h3 {
  margin: 0;
  font-size: 16px;
  color: #303133;
}

.todo-section {
  margin: 16px 0 24px;
}

.todo-card {
  display: inline-flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.06);
  cursor: pointer;
  transition: box-shadow 0.2s, transform 0.1s;
  border-left: 4px solid #409eff;
}

.todo-card:hover {
  box-shadow: 0 4px 16px 0 rgba(0, 0, 0, 0.1);
  transform: translateY(-1px);
}

.todo-icon {
  font-size: 28px;
}

.todo-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.todo-label {
  font-size: 14px;
  color: #606266;
}

.todo-number {
  font-size: 24px;
  font-weight: 600;
  color: #909399;
  line-height: 1;
}

.todo-number.has-pending {
  color: #ff4d4f;
}

.todo-action {
  margin-left: 8px;
}

.expire-state {
  margin-top: 4px;
  font-size: 12px;
}

.expire-state.is-overdue {
  color: #f56c6c;
}

.expire-state.is-warning {
  color: #e6a23c;
}

.expire-state.is-normal {
  color: #67c23a;
}
</style>
