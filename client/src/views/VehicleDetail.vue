<template>
  <div class="detail-page" v-loading="loading">
    <!-- 返回 + 标题 -->
    <div class="page-header">
      <el-button text :icon="ArrowLeft" @click="$router.back()">返回列表</el-button>
      <h3 v-if="vehicle">{{ vehicle.plateNumber }}（{{ vehicle.brand }}）</h3>
    </div>

    <!-- 基本信息卡片 -->
    <div class="table-container" v-if="vehicle" style="margin-bottom: 16px">
      <h4>基本信息</h4>
      <el-descriptions :column="4" border>
        <el-descriptions-item label="车牌号">{{ vehicle.plateNumber }}</el-descriptions-item>
        <el-descriptions-item label="车辆类型">{{ vehicle.vehicleType === 0 ? '小车' : '大巴' }}</el-descriptions-item>
        <el-descriptions-item label="品牌">{{ vehicle.brand }}</el-descriptions-item>
        <el-descriptions-item label="上牌时间">{{ vehicle.purchaseDate }}</el-descriptions-item>
        <el-descriptions-item label="产权所属">{{ vehicle.owner || '-' }}</el-descriptions-item>
        <el-descriptions-item label="投保公司">{{ vehicle.insuranceCompany || '-' }}</el-descriptions-item>
        <el-descriptions-item label="保单号">{{ vehicle.policyNumber || '-' }}</el-descriptions-item>
        <el-descriptions-item label="保险截止日">
          <span :class="getDateClass(vehicle.insuranceExpire)">{{ vehicle.insuranceExpire || '-' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="年检截止日">
          <span :class="getDateClass(vehicle.inspectionExpire)">{{ vehicle.inspectionExpire || '-' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="ETC银行">{{ vehicle.etcBank || '-' }}</el-descriptions-item>
        <el-descriptions-item label="油卡号码">{{ vehicle.oilCardNumber || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ vehicle.remark || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <span :class="getStatusClass(vehicle)">{{ getStatusText(vehicle) }}</span>
        </el-descriptions-item>
      </el-descriptions>
    </div>

    <!-- 操作区 -->
    <div class="table-container" v-if="userStore.isAdmin">
      <h4>快捷操作</h4>
      <div style="display: flex; gap: 12px; margin-top: 12px">
        <el-button type="primary" @click="showRenewDialog = true">📋 续保</el-button>
        <el-button type="success" @click="showInspectionDialog = true">🔍 更新年检</el-button>
        <el-button type="danger" plain @click="handleDelete">🗑 注销车辆</el-button>
      </div>
    </div>

    <!-- 续保对话框 -->
    <el-dialog v-model="showRenewDialog" title="续保" width="500px">
      <el-form :model="renewForm" label-width="100px">
        <el-form-item label="保险公司">
          <el-input v-model="renewForm.insuranceCompany" />
        </el-form-item>
        <el-form-item label="险种">
          <el-input v-model="renewForm.insuranceType" />
        </el-form-item>
        <el-form-item label="保单号">
          <el-input v-model="renewForm.policyNumber" />
        </el-form-item>
        <el-form-item label="截止日期">
          <el-date-picker v-model="renewForm.insuranceExpire" type="date" style="width:100%"
            value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="保费">
          <el-input-number v-model="renewForm.premium" :min="0" :precision="2" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRenewDialog = false">取消</el-button>
        <el-button type="primary" @click="handleRenew" :loading="saving">确认续保</el-button>
      </template>
    </el-dialog>

    <!-- 年检更新对话框 -->
    <el-dialog v-model="showInspectionDialog" title="更新年检" width="400px">
      <el-form label-width="100px">
        <el-form-item label="年检日期">
          <el-date-picker v-model="inspectionDate" type="date" style="width:100%" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="截止日期">
          <el-date-picker v-model="inspectionExpireDate" type="date" style="width:100%" value-format="YYYY-MM-DD" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showInspectionDialog = false">取消</el-button>
        <el-button type="primary" @click="handleInspectionUpdate" :loading="saving">确认更新</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { vehicleApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const saving = ref(false)
const vehicle = ref(null)

// 对话框
const showRenewDialog = ref(false)
const showInspectionDialog = ref(false)
const inspectionDate = ref(null)
const inspectionExpireDate = ref(null)

const renewForm = reactive({
  insuranceCompany: '',
  insuranceType: '',
  policyNumber: '',
  insuranceExpire: null,
  premium: null,
})

onMounted(async () => {
  loading.value = true
  try {
    vehicle.value = await vehicleApi.detail(route.params.id)
  } finally {
    loading.value = false
  }
})

async function handleRenew() {
  saving.value = true
  try {
    await vehicleApi.renewInsurance(vehicle.value.id, { ...renewForm })
    ElMessage.success('续保成功')
    showRenewDialog.value = false
    vehicle.value = await vehicleApi.detail(route.params.id)
  } finally {
    saving.value = false
  }
}

async function handleInspectionUpdate() {
  saving.value = true
  try {
    await vehicleApi.updateInspection(vehicle.value.id, inspectionDate.value, inspectionExpireDate.value)
    ElMessage.success('年检更新成功')
    showInspectionDialog.value = false
    vehicle.value = await vehicleApi.detail(route.params.id)
  } finally {
    saving.value = false
  }
}

async function handleDelete() {
  await ElMessageBox.confirm('确定要注销该车辆吗？', '确认操作', { type: 'warning' })
  await vehicleApi.delete(vehicle.value.id)
  ElMessage.success('车辆已注销')
  router.back()
}

function getStatusText(v) {
  const today = new Date()
  const dates = []
  if (v.insuranceExpire) dates.push(new Date(v.insuranceExpire))
  if (v.inspectionExpire) dates.push(new Date(v.inspectionExpire))
  if (dates.length === 0) return '🟢 正常'
  const earliest = new Date(Math.min(...dates))
  const diff = Math.ceil((earliest - today) / (1000 * 60 * 60 * 24))
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

function getStatusClass(v) { return getDateClass(v.insuranceExpire) || getDateClass(v.inspectionExpire) }
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.page-header h3 {
  margin: 0;
  flex: 1;
}
</style>
