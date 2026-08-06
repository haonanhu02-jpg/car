<template>
  <div class="vehicles-page">
    <!-- 工具栏 -->
    <div class="table-container" style="margin-bottom: 16px">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-button type="primary" :icon="Plus" @click="showCreateDialog" v-if="userStore.isAdmin">
            新增车辆
          </el-button>
          <el-button :icon="Upload" v-if="userStore.isAdmin">导入Excel</el-button>
          <el-button :icon="Download">导出Excel</el-button>
        </div>
        <div class="toolbar-right">
          <el-input v-model="keyword" placeholder="输入车牌号搜索..." :prefix-icon="Search"
            clearable style="width: 240px" @keyup.enter="fetchData" />
          <el-select v-model="vehicleType" placeholder="车辆类型" clearable style="width: 120px">
            <el-option label="全部" :value="null" />
            <el-option label="小车" :value="0" />
            <el-option label="大巴" :value="1" />
          </el-select>
          <el-button type="primary" @click="fetchData">查询</el-button>
        </div>
      </div>
    </div>

    <!-- 数据表格 -->
    <div class="table-container">
      <el-table :data="vehicles" stripe v-loading="loading" @row-click="goDetail" style="cursor: pointer">
        <el-table-column prop="plateNumber" label="车牌号" width="130" fixed />
        <el-table-column label="类型" width="70">
          <template #default="{ row }">{{ row.vehicleType === 0 ? '小车' : '大巴' }}</template>
        </el-table-column>
        <el-table-column prop="brand" label="品牌" width="100" />
        <el-table-column prop="purchaseDate" label="上牌时间" width="110" />
        <el-table-column prop="inspectionExpire" label="年检截止日" width="120" sortable />
        <el-table-column prop="insuranceExpire" label="保险截止日" width="120" sortable />
        <el-table-column prop="owner" label="产权所属" width="100" />
        <el-table-column prop="insuranceCompany" label="投保公司" width="100" />
        <el-table-column prop="policyNumber" label="保单号" width="140" show-overflow-tooltip />
        <el-table-column prop="etcBank" label="ETC银行" width="90" />
        <el-table-column prop="oilCardNumber" label="油卡号码" width="110" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <span :class="getStatusClass(row)">{{ getStatusText(row) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click.stop="showEditDialog(row)"
              v-if="userStore.isAdmin">编辑</el-button>
            <el-button text type="primary" size="small" @click.stop="goDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div style="margin-top: 16px; display: flex; justify-content: flex-end">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[10, 15, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @change="fetchData"
        />
      </div>
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑车辆' : '新增车辆'" width="680px"
      @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="车牌号" prop="plateNumber">
              <el-input v-model="form.plateNumber" placeholder="如：浙J.U0055" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="车辆类型" prop="vehicleType">
              <el-select v-model="form.vehicleType" style="width: 100%">
                <el-option label="小车" :value="0" />
                <el-option label="大巴" :value="1" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="品牌" prop="brand">
              <el-input v-model="form.brand" placeholder="如：宝马760" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="上牌时间">
              <el-date-picker v-model="form.purchaseDate" type="date" style="width: 100%"
                value-format="YYYY-MM-DD" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="保险截止日">
              <el-date-picker v-model="form.insuranceExpire" type="date" style="width: 100%"
                value-format="YYYY-MM-DD" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="年检截止日">
              <el-date-picker v-model="form.inspectionExpire" type="date" style="width: 100%"
                value-format="YYYY-MM-DD" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="投保公司">
              <el-input v-model="form.insuranceCompany" placeholder="如：平安" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="保单号">
              <el-input v-model="form.policyNumber" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="产权所属">
              <el-input v-model="form.owner" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="ETC银行">
              <el-input v-model="form.etcBank" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="油卡号码">
              <el-input v-model="form.oilCardNumber" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="备注">
              <el-input v-model="form.remark" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">
          {{ editingId ? '保存修改' : '确认新增' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, Upload, Download, Search } from '@element-plus/icons-vue'
import { vehicleApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const vehicles = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(15)
const keyword = ref('')
const vehicleType = ref(null)

// 对话框
const dialogVisible = ref(false)
const saving = ref(false)
const editingId = ref(null)
const formRef = ref(null)

const form = reactive({
  plateNumber: '',
  vehicleType: 0,
  brand: '',
  purchaseDate: null,
  insuranceExpire: null,
  inspectionExpire: null,
  insuranceCompany: '',
  insuranceType: '',
  policyNumber: '',
  owner: '',
  etcBank: '',
  oilCardNumber: '',
  remark: '',
})

const rules = {
  plateNumber: [
    { required: true, message: '请输入车牌号', trigger: 'blur' },
    { pattern: /^[\u4e00-\u9fa5][A-Z]\.[A-Z0-9]{5,6}$/, message: '格式：浙J.U0055', trigger: 'blur' },
  ],
  vehicleType: [{ required: true, message: '请选择车辆类型', trigger: 'change' }],
  brand: [{ required: true, message: '请输入品牌', trigger: 'blur' }],
}

onMounted(() => fetchData())

async function fetchData() {
  loading.value = true
  try {
    const result = await vehicleApi.list({
      keyword: keyword.value || undefined,
      vehicleType: vehicleType.value,
      page: page.value,
      size: size.value,
    })
    vehicles.value = result.records
    total.value = result.total
  } finally {
    loading.value = false
  }
}

function goDetail(row) {
  router.push(`/vehicles/${row.id}`)
}

function showCreateDialog() {
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

function showEditDialog(row) {
  editingId.value = row.id
  Object.assign(form, {
    plateNumber: row.plateNumber,
    vehicleType: row.vehicleType,
    brand: row.brand,
    purchaseDate: row.purchaseDate,
    insuranceExpire: row.insuranceExpire,
    inspectionExpire: row.inspectionExpire,
    insuranceCompany: row.insuranceCompany,
    insuranceType: row.insuranceType,
    policyNumber: row.policyNumber,
    owner: row.owner,
    etcBank: row.etcBank,
    oilCardNumber: row.oilCardNumber,
    remark: row.remark,
  })
  dialogVisible.value = true
}

function resetForm() {
  Object.assign(form, {
    plateNumber: '', vehicleType: 0, brand: '', purchaseDate: null,
    insuranceExpire: null, inspectionExpire: null, insuranceCompany: '',
    insuranceType: '', policyNumber: '', owner: '', etcBank: '',
    oilCardNumber: '', remark: '',
  })
}

async function handleSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    if (editingId.value) {
      await vehicleApi.update(editingId.value, { ...form })
      ElMessage.success('更新成功')
    } else {
      await vehicleApi.create({ ...form })
      ElMessage.success('车辆注册成功')
    }
    dialogVisible.value = false
    fetchData()
  } finally {
    saving.value = false
  }
}

function getStatusText(row) {
  const today = new Date()
  const dates = []
  if (row.insuranceExpire) dates.push(new Date(row.insuranceExpire))
  if (row.inspectionExpire) dates.push(new Date(row.inspectionExpire))
  if (dates.length === 0) return '🟢 正常'

  const earliest = new Date(Math.min(...dates))
  const diff = Math.ceil((earliest - today) / (1000 * 60 * 60 * 24))
  if (diff < 0) return '🔴 已逾期'
  if (diff <= 30) return '🟡 即将到期'
  return '🟢 正常'
}

function getStatusClass(row) {
  const cls = getStatusText(row)
  if (cls.includes('逾期')) return 'status-overdue'
  if (cls.includes('即将')) return 'status-expiring'
  return 'status-normal'
}
</script>
