<template>
  <div class="operation-logs-page">
    <div class="table-container">
      <div class="toolbar">
        <div class="toolbar-left">
          <h3>📝 操作日志</h3>
          <span class="sub-title">仅管理员可查看，用于审计谁在什么时间做了什么操作</span>
        </div>
      </div>

      <!-- 筛选栏 -->
      <div class="filter-bar">
        <el-input
          v-model="filter.userName"
          placeholder="操作人账号"
          clearable
          style="width: 160px"
          @keyup.enter="handleSearch"
        />
        <el-select
          v-model="filter.action"
          placeholder="操作类型"
          clearable
          style="width: 180px"
        >
          <el-option
            v-for="action in actionOptions"
            :key="action"
            :label="actionLabel(action)"
            :value="action"
          />
        </el-select>
        <el-date-picker
          v-model="filter.timeRange"
          type="datetimerange"
          range-separator="至"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          value-format="YYYY-MM-DDTHH:mm:ss"
          style="width: 360px"
        />
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
      </div>

      <el-table :data="logs" stripe v-loading="loading" empty-text="暂无操作日志">
        <el-table-column prop="createdAt" label="操作时间" width="170" />
        <el-table-column prop="userName" label="操作人" width="120" />
        <el-table-column label="操作类型" width="160">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ actionLabel(row.action) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="操作描述" min-width="240" show-overflow-tooltip />
        <el-table-column label="车辆ID" width="90">
          <template #default="{ row }">
            <span v-if="row.vehicleId">
              <el-link type="primary" @click="$router.push(`/vehicles/${row.vehicleId}`)">
                #{{ row.vehicleId }}
              </el-link>
            </span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="ipAddress" label="IP 地址" width="130" />
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :page-sizes="[15, 30, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="handleSearch"
          @current-change="fetchData"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import { operationLogApi } from '@/api'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const logs = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(15)
const actionOptions = ref([])

const filter = ref({
  userName: '',
  action: '',
  timeRange: null,
})

onMounted(() => {
  loadActions()
  fetchData()
})

async function loadActions() {
  try {
    actionOptions.value = await operationLogApi.actions()
  } catch (e) {
    ElMessage.error('加载操作类型失败')
  }
}

async function fetchData() {
  loading.value = true
  try {
    const params = {
      page: page.value,
      size: size.value,
    }
    if (filter.value.userName) params.userName = filter.value.userName
    if (filter.value.action) params.action = filter.value.action
    if (filter.value.timeRange && filter.value.timeRange.length === 2) {
      params.startTime = filter.value.timeRange[0]
      params.endTime = filter.value.timeRange[1]
    }

    const result = await operationLogApi.list(params)
    logs.value = result.records || []
    total.value = result.total || 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 1
  fetchData()
}

function handleReset() {
  filter.value = { userName: '', action: '', timeRange: null }
  page.value = 1
  fetchData()
}

function actionLabel(action) {
  const map = {
    CREATE_VEHICLE: '新增车辆',
    UPDATE_VEHICLE: '更新车辆',
    DELETE_VEHICLE: '注销车辆',
    BATCH_CREATE_VEHICLE: '批量新增车辆',
    IMPORT_VEHICLE: 'Excel导入车辆',
    RENEW_INSURANCE: '车辆续保',
    UPDATE_INSPECTION: '更新年检',
    CREATE_USER: '创建用户',
    UPDATE_USER_STATUS: '修改用户状态',
    RESET_USER_PASSWORD: '重置用户密码',
    EMAIL_REMINDER: '邮件提醒（模拟）',
  }
  return map[action] || action
}
</script>

<style scoped>
.operation-logs-page {
  padding: 0;
}

.table-container {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.toolbar-left {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.toolbar-left h3 {
  margin: 0;
  font-size: 18px;
  color: #303133;
}

.sub-title {
  color: #909399;
  font-size: 13px;
}

.filter-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}
</style>
