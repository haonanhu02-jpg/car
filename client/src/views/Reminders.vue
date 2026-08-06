<template>
  <div class="reminders-page">
    <div class="table-container">
      <div class="toolbar">
        <div class="toolbar-left">
          <h3>📋 提醒中心</h3>
        </div>
        <div class="toolbar-right">
          <el-select v-model="filterType" placeholder="提醒类型" clearable style="width: 120px" @change="fetchData">
            <el-option label="全部" :value="null" />
            <el-option label="保险" :value="0" />
            <el-option label="年检" :value="1" />
          </el-select>
          <el-select v-model="filterStatus" placeholder="处理状态" clearable style="width: 120px" @change="fetchData">
            <el-option label="全部" :value="null" />
            <el-option label="待处理" :value="0" />
            <el-option label="已处理" :value="1" />
            <el-option label="已逾期" :value="2" />
          </el-select>
          <el-button type="primary" :icon="Refresh" @click="handleScan" v-if="userStore.isAdmin">
            立即扫描
          </el-button>
        </div>
      </div>

      <el-table :data="reminders" stripe v-loading="loading">
        <el-table-column label="车牌号" width="120">
          <template #default="{ row }">
            <el-link type="primary" @click="$router.push(`/vehicles/${row.vehicleId}`)">
              {{ row.plateNumber || `车辆#${row.vehicleId}` }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column label="提醒类型" width="80">
          <template #default="{ row }">{{ row.type === 0 ? '保险' : '年检' }}</template>
        </el-table-column>
        <el-table-column label="提醒节点" width="100">
          <template #default="{ row }">提前 {{ row.nodeDays }} 天</template>
        </el-table-column>
        <el-table-column prop="remindDate" label="提醒日期" width="120" />
        <el-table-column label="提醒方式" width="120">
          <template #default="{ row }">
            <el-tag v-for="method in (row.remindMethod || '').split(',')" :key="method" size="small" style="margin-right: 4px">
              {{ methodLabel(method) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="处理状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.status === 0" type="warning">⏳ 待处理</el-tag>
            <el-tag v-else-if="row.status === 1" type="success">✅ 已处理</el-tag>
            <el-tag v-else type="danger">🔴 已逾期</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="handler" label="处理人" width="100" />
        <el-table-column prop="handledAt" label="处理时间" width="170" />
        <el-table-column label="操作" width="120" v-if="userStore.isAdmin">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" type="primary" size="small"
              @click="handleReminder(row.id)">标记已处理</el-button>
          </template>
        </el-table-column>
      </el-table>
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
const filterType = ref(null)
const filterStatus = ref(null)

onMounted(() => fetchData())

async function fetchData() {
  loading.value = true
  try {
    reminders.value = await reminderApi.list({
      type: filterType.value,
      status: filterStatus.value,
    })
  } finally {
    loading.value = false
  }
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
    fetchData()
  } catch (e) {
    ElMessage.error('扫描失败：' + (e.message || '未知错误'))
  }
}

function methodLabel(method) {
  const map = {
    system: '系统内',
    sms: '短信',
    email: '邮件',
  }
  return map[method] || method
}
</script>
