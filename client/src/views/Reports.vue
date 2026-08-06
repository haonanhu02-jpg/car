<template>
  <div class="reports-page">
    <el-row :gutter="20">
      <!-- 保险到期清单 -->
      <el-col :span="12">
        <div class="table-container">
          <div class="toolbar">
            <h3>📋 保险到期清单</h3>
            <el-button type="primary" :icon="Download" @click="exportInsurance">导出Excel</el-button>
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
            <el-button type="primary" :icon="Download" @click="exportInspection">导出Excel</el-button>
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
import { ElMessage } from 'element-plus'
import { exportToExcel } from '@/utils/excel'

const insuranceReport = ref([])
const inspectionReport = ref([])

const insuranceColumns = [
  { label: '车牌号', key: 'plateNumber' },
  { label: '品牌', key: 'brand' },
  { label: '投保公司', key: 'insuranceCompany' },
  { label: '保单号', key: 'policyNumber' },
  { label: '保险截止日期', key: 'insuranceExpire' },
  { label: '状态', key: 'statusLabel' },
]
const inspectionColumns = [
  { label: '车牌号', key: 'plateNumber' },
  { label: '品牌', key: 'brand' },
  { label: '年检截止日期', key: 'inspectionExpire' },
  { label: '状态', key: 'statusLabel' },
]

onMounted(async () => {
  const all = await vehicleApi.all()

  insuranceReport.value = all
    .filter(v => v.insuranceExpire)
    .sort((a, b) => new Date(a.insuranceExpire) - new Date(b.insuranceExpire))

  inspectionReport.value = all
    .filter(v => v.inspectionExpire)
    .sort((a, b) => new Date(a.inspectionExpire) - new Date(b.inspectionExpire))
})

function dateStamp() {
  return new Date().toISOString().slice(0, 10)
}

function exportInsurance() {
  const rows = insuranceReport.value.map(v => ({ ...v, statusLabel: getExpireLabel(v.insuranceExpire) }))
  exportToExcel(rows, insuranceColumns, `保险到期清单_${dateStamp()}.xlsx`)
  ElMessage.success('保险到期清单已导出')
}

function exportInspection() {
  const rows = inspectionReport.value.map(v => ({ ...v, statusLabel: getExpireLabel(v.inspectionExpire) }))
  exportToExcel(rows, inspectionColumns, `年检到期清单_${dateStamp()}.xlsx`)
  ElMessage.success('年检到期清单已导出')
}

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
