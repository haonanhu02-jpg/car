<template>
  <div class="reports-page">
    <el-row :gutter="20">
      <!-- 保险到期清单 -->
      <el-col :span="12">
        <div class="table-container">
          <div class="toolbar">
            <h3>📋 保险到期清单</h3>
            <el-button type="primary" :icon="Download">导出Excel</el-button>
          </div>
          <el-table :data="insuranceReport" stripe>
            <el-table-column prop="plateNumber" label="车牌号" width="120" />
            <el-table-column prop="brand" label="品牌" width="100" />
            <el-table-column prop="insuranceCompany" label="投保公司" width="100" />
            <el-table-column prop="policyNumber" label="保单号" width="140" />
            <el-table-column prop="insuranceExpire" label="截止日期" width="120" sortable />
            <el-table-column label="状态" width="120">
              <template #default="{ row }">
                <span :class="getDateClass(row.insuranceExpire)">
                  {{ getExpireLabel(row.insuranceExpire) }}
                </span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-col>

      <!-- 年检到期清单 -->
      <el-col :span="12">
        <div class="table-container">
          <div class="toolbar">
            <h3>🔍 年检到期清单</h3>
            <el-button type="primary" :icon="Download">导出Excel</el-button>
          </div>
          <el-table :data="inspectionReport" stripe>
            <el-table-column prop="plateNumber" label="车牌号" width="120" />
            <el-table-column prop="brand" label="品牌" width="100" />
            <el-table-column prop="inspectionExpire" label="截止日期" width="120" sortable />
            <el-table-column label="状态" width="120">
              <template #default="{ row }">
                <span :class="getDateClass(row.inspectionExpire)">
                  {{ getExpireLabel(row.inspectionExpire) }}
                </span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { vehicleApi } from '@/api'
import { Download } from '@element-plus/icons-vue'

const insuranceReport = ref([])
const inspectionReport = ref([])

onMounted(async () => {
  const result = await vehicleApi.list({ size: 100 })
  const all = result.records || []

  insuranceReport.value = all
    .filter(v => v.insuranceExpire)
    .sort((a, b) => new Date(a.insuranceExpire) - new Date(b.insuranceExpire))

  inspectionReport.value = all
    .filter(v => v.inspectionExpire)
    .sort((a, b) => new Date(a.inspectionExpire) - new Date(b.inspectionExpire))
})

function getExpireLabel(date) {
  if (!date) return '-'
  const d = new Date(date)
  const now = new Date()
  const diff = Math.ceil((d - now) / (1000 * 60 * 60 * 24))
  if (diff < 0) return '🔴 已逾期'
  if (diff <= 30) return '🟡 即将到期'
  return '🟢 正常'
}

function getDateClass(date) {
  if (!date) return ''
  const d = new Date(date)
  const now = new Date()
  const diff = Math.ceil((d - now) / (1000 * 60 * 60 * 24))
  if (diff < 0) return 'status-overdue'
  if (diff <= 30) return 'status-expiring'
  return 'status-normal'
}
</script>
