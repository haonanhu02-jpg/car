<template>
  <div class="reminders-page">
    <div class="table-container">
      <div class="page-title-row">
        <h3>📋 提醒中心</h3>
        <el-button v-if="userStore.isAdmin" type="primary" :icon="Refresh" @click="handleScan">
          立即扫描
        </el-button>
      </div>

      <el-tabs v-model="activeScope" class="reminder-tabs" @tab-change="handleScopeChange">
        <el-tab-pane label="待办提醒" name="todo" />
        <el-tab-pane label="已处理记录" name="history" />
      </el-tabs>

      <div class="filter-bar">
        <el-input
          v-model="keyword"
          placeholder="输入车牌号搜索"
          clearable
          style="width: 190px"
          @keyup.enter="handleQuery"
          @clear="handleQuery"
        />
        <el-select v-model="filterType" placeholder="提醒类型" clearable style="width: 130px">
          <el-option label="保险" :value="0" />
          <el-option label="年检" :value="1" />
        </el-select>
        <el-select
          v-if="activeScope === 'todo'"
          v-model="filterStatus"
          placeholder="处理状态"
          clearable
          style="width: 150px"
        >
          <el-option label="待处理" :value="0" />
          <el-option label="超时未处理" :value="2" />
        </el-select>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          range-separator="至"
          start-placeholder="提醒开始日期"
          end-placeholder="提醒结束日期"
          unlink-panels
          style="width: 270px"
        />
        <el-button type="primary" @click="handleQuery">查询</el-button>
        <el-button @click="resetFilters">重置</el-button>
      </div>

      <el-table
        :data="reminders"
        stripe
        v-loading="loading"
        :empty-text="activeScope === 'todo' ? '暂无待办提醒 🎉' : '暂无已处理记录'"
      >
        <el-table-column label="车牌号" min-width="125">
          <template #default="{ row }">
            <el-link type="primary" @click="$router.push(`/vehicles/${row.vehicleId}`)">
              {{ row.plateNumber || `车辆#${row.vehicleId}` }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column label="提醒类型" width="90">
          <template #default="{ row }">{{ row.type === 0 ? '保险' : '年检' }}</template>
        </el-table-column>
        <el-table-column label="当前提醒节点" width="125">
          <template #default="{ row }">提前 {{ row.nodeDays }} 天</template>
        </el-table-column>
        <el-table-column label="到期日期 / 到期情况" min-width="175">
          <template #default="{ row }">
            <div>{{ row.expireDate || '-' }}</div>
            <div class="expire-state" :class="expireStateClass(row.remainingDays)">
              {{ row.expireStatus || '-' }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="remindDate" label="最近提醒日期" width="125" />
        <el-table-column label="提醒方式" min-width="130">
          <template #default="{ row }">
            <el-tag
              v-for="method in reminderMethods(row.remindMethod)"
              :key="method"
              size="small"
              class="method-tag"
            >
              {{ methodLabel(method) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="处理状态" width="130">
          <template #default="{ row }">
            <el-tag v-if="row.status === 0" type="warning">⏳ 待处理</el-tag>
            <el-tag v-else-if="row.status === 1" type="success">✅ 已处理</el-tag>
            <el-tag v-else type="danger">⏰ 超时未处理</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="处理时间" width="175">
          <template #default="{ row }">{{ formatTime(row.handledAt) }}</template>
        </el-table-column>
        <el-table-column
          v-if="activeScope === 'todo' && userStore.isAdmin"
          label="操作"
          width="125"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button
              v-if="row.status === 0 || row.status === 2"
              type="primary"
              size="small"
              @click="handleReminder(row.id)"
            >
              标记已处理
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-row">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          @change="fetchData"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { reminderApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const loading = ref(false)
const reminders = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const activeScope = ref('todo')
const keyword = ref('')
const filterType = ref(null)
const filterStatus = ref(null)
const dateRange = ref([])

onMounted(fetchData)

async function fetchData() {
  loading.value = true
  try {
    const result = await reminderApi.list({
      scope: activeScope.value,
      keyword: keyword.value || undefined,
      type: filterType.value,
      status: activeScope.value === 'todo' ? filterStatus.value : undefined,
      startDate: dateRange.value?.[0],
      endDate: dateRange.value?.[1],
      page: page.value,
      size: size.value,
    })
    reminders.value = result.records || []
    total.value = result.total || 0
  } finally {
    loading.value = false
  }
}

function handleScopeChange() {
  page.value = 1
  filterStatus.value = null
  fetchData()
}

function handleQuery() {
  page.value = 1
  fetchData()
}

function resetFilters() {
  keyword.value = ''
  filterType.value = null
  filterStatus.value = null
  dateRange.value = []
  handleQuery()
}

async function handleReminder(id) {
  await reminderApi.handle(id)
  ElMessage.success('已标记为处理')
  fetchData()
}

async function handleScan() {
  try {
    await reminderApi.scan()
    ElMessage.success('到期扫描完成')
    page.value = 1
    fetchData()
  } catch (e) {
    ElMessage.error('扫描失败：' + (e.message || '未知错误'))
  }
}

function reminderMethods(methods) {
  return (methods || '').split(',').map(item => item.trim()).filter(Boolean)
}

function methodLabel(method) {
  const map = {
    system: '系统内',
    email: '邮件',
  }
  return map[method] || method
}

function formatTime(value) {
  return value ? value.replace('T', ' ') : '-'
}

function expireStateClass(remainingDays) {
  if (remainingDays == null) return ''
  if (remainingDays < 0) return 'is-overdue'
  if (remainingDays <= 30) return 'is-warning'
  return 'is-normal'
}
</script>

<style scoped>
.page-title-row,
.filter-bar,
.pagination-row {
  display: flex;
  align-items: center;
}

.page-title-row {
  justify-content: space-between;
  margin-bottom: 8px;
}

.page-title-row h3 {
  margin: 0;
}

.reminder-tabs {
  margin-bottom: 14px;
}

.filter-bar {
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 18px;
}

.pagination-row {
  justify-content: flex-end;
  margin-top: 18px;
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

.method-tag {
  margin-right: 4px;
  margin-bottom: 3px;
}
</style>
