<template>
  <div class="vehicles-page">
    <!-- 工具栏 -->
    <div class="table-container" style="margin-bottom: 16px">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-button type="primary" :icon="Plus" @click="showCreateDialog" v-if="userStore.isAdmin">
            新增车辆
          </el-button>
          <el-button :icon="Upload" v-if="userStore.isAdmin" @click="importVisible = true">导入Excel</el-button>
          <el-button :icon="Download" :loading="exporting" @click="handleExport">导出Excel</el-button>
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
        <el-form-item v-if="editingId" label="车辆登记证">
          <div class="certificate-panel">
            <div v-if="certificateInfo" class="certificate-current">
              <span>{{ certificateInfo.fileName }}（{{ formatFileSize(certificateInfo.fileSize) }}）</span>
              <el-button link type="primary" :loading="viewingCertificate" @click="viewCertificate">查看</el-button>
              <el-button link type="danger" @click="deleteCertificate">删除</el-button>
            </div>
            <el-upload
              :show-file-list="false"
              accept="image/*,.pdf,application/pdf"
              :before-upload="uploadCertificate"
              :disabled="uploadingCertificate"
            >
              <el-button :loading="uploadingCertificate" :icon="Upload">
                {{ certificateInfo ? '替换扫描件' : '上传扫描件' }}
              </el-button>
              <template #tip>
                <div class="el-upload__tip">支持手机扫描图片或 PDF，单个文件不超过 10MB</div>
              </template>
            </el-upload>
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">
          {{ editingId ? '保存修改' : '确认新增' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 批量导入对话框 -->
    <el-dialog v-model="importVisible" title="批量导入车辆（Excel）" width="580px" @closed="resetImport">
      <el-alert type="info" :closable="false" show-icon style="margin-bottom: 14px">
        <template #title>
          支持 .xls / .xlsx。表头行可位于前 10 行任意位置（允许第一行为标题）；未提供车辆类型时默认按“小车”导入。
        </template>
      </el-alert>
      <el-upload
        ref="uploadRef"
        drag
        accept=".xlsx,.xls"
        :auto-upload="false"
        :limit="1"
        :on-change="handleFileChange"
        :on-remove="handleFileRemove"
        :on-exceed="() => ElMessage.warning('每次只能上传一个文件')"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">将 Excel 文件拖到此处，或 <em>点击上传</em></div>
        <template #tip>
          <div class="el-upload__tip">仅支持 .xlsx / .xls，单次一个文件</div>
        </template>
      </el-upload>
      <el-button link type="primary" @click="downloadTemplateFile" style="margin-top: 8px">
        下载导入模板
      </el-button>
      <template #footer>
        <el-button @click="importVisible = false">取消</el-button>
        <el-button type="primary" :loading="importing" :disabled="!importFile" @click="doImport">
          开始导入
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, Upload, Download, Search, UploadFilled } from '@element-plus/icons-vue'
import { vehicleApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { exportToExcel, downloadTemplate } from '@/utils/excel'

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
const certificateInfo = ref(null)
const uploadingCertificate = ref(false)
const viewingCertificate = ref(false)

// 批量导入 / 导出
const importVisible = ref(false)
const importFile = ref(null)
const importing = ref(false)
const exporting = ref(false)
const uploadRef = ref(null)

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

// 导入模板列（与后端 Excel 解析器映射的表头一致）
const importColumns = [
  { label: '序号', key: 'no' },
  { label: '车牌号', key: 'plateNumber' },
  { label: '车辆品牌', key: 'brand' },
  { label: '年检日期', key: 'inspectionExpire' },
  { label: '上牌时间', key: 'purchaseDate' },
  { label: '所属公司', key: 'company' },
  { label: '产权所属', key: 'owner' },
  { label: '投保公司', key: 'insuranceCompany' },
  { label: '险种', key: 'insuranceType' },
  { label: '保单号', key: 'policyNumber' },
  { label: '保险截止', key: 'insuranceExpire' },
  { label: 'ETC办理', key: 'etcBank' },
  { label: '油卡号码', key: 'oilCardNumber' },
  { label: '备忘录', key: 'remark' },
]

// 导出列（车辆类型转为中文）
const exportColumns = [
  { label: '车牌号', key: 'plateNumber' },
  { label: '车辆类型', key: 'vehicleTypeText' },
  { label: '品牌', key: 'brand' },
  { label: '上牌日期', key: 'purchaseDate' },
  { label: '保险截止日', key: 'insuranceExpire' },
  { label: '年检截止日', key: 'inspectionExpire' },
  { label: '产权所属', key: 'owner' },
  { label: '投保公司', key: 'insuranceCompany' },
  { label: '险种', key: 'insuranceType' },
  { label: '保单号', key: 'policyNumber' },
  { label: 'ETC银行', key: 'etcBank' },
  { label: '油卡号码', key: 'oilCardNumber' },
  { label: '备注', key: 'remark' },
]

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
  certificateInfo.value = null
  dialogVisible.value = true
}

async function showEditDialog(row) {
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
  certificateInfo.value = null
  try {
    certificateInfo.value = await vehicleApi.certificateInfo(row.id)
  } catch (e) {
    // 附件信息加载失败不阻止编辑车辆基础信息
  }
}

function resetForm() {
  Object.assign(form, {
    plateNumber: '', vehicleType: 0, brand: '', purchaseDate: null,
    insuranceExpire: null, inspectionExpire: null, insuranceCompany: '',
    insuranceType: '', policyNumber: '', owner: '', etcBank: '',
    oilCardNumber: '', remark: '',
  })
}

async function uploadCertificate(file) {
  if (!editingId.value) return false
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.error('车辆登记证文件不能超过 10MB')
    return false
  }
  const supported = file.type === 'application/pdf' || file.type.startsWith('image/')
  if (!supported) {
    ElMessage.error('仅支持图片或 PDF 格式')
    return false
  }
  uploadingCertificate.value = true
  try {
    certificateInfo.value = await vehicleApi.uploadCertificate(editingId.value, file)
    ElMessage.success('车辆登记证上传成功')
  } finally {
    uploadingCertificate.value = false
  }
  return false
}

async function viewCertificate() {
  viewingCertificate.value = true
  try {
    const blob = await vehicleApi.viewCertificate(editingId.value)
    const url = URL.createObjectURL(blob)
    const opened = window.open(url, '_blank', 'noopener,noreferrer')
    if (!opened) ElMessage.warning('浏览器拦截了新窗口，请允许弹窗后重试')
    setTimeout(() => URL.revokeObjectURL(url), 60000)
  } finally {
    viewingCertificate.value = false
  }
}

async function deleteCertificate() {
  try {
    await ElMessageBox.confirm('确定删除这份车辆登记证扫描件吗？', '删除车辆登记证', { type: 'warning' })
    await vehicleApi.deleteCertificate(editingId.value)
    certificateInfo.value = null
    ElMessage.success('车辆登记证已删除')
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') throw e
  }
}

function formatFileSize(bytes) {
  if (!bytes) return '0 KB'
  return bytes >= 1024 * 1024
    ? `${(bytes / 1024 / 1024).toFixed(1)} MB`
    : `${Math.ceil(bytes / 1024)} KB`
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

// ──────────────────────────────────────
//  批量导入 / 导出 Excel
// ──────────────────────────────────────
function handleFileChange(uploadFile) {
  importFile.value = uploadFile.raw
}
function handleFileRemove() {
  importFile.value = null
}
function resetImport() {
  importFile.value = null
  uploadRef.value?.clearFiles()
}
function downloadTemplateFile() {
  downloadTemplate(importColumns, '车辆导入模板.xlsx')
}
async function doImport() {
  if (!importFile.value) return
  importing.value = true
  try {
    const result = await vehicleApi.importExcel(importFile.value)
    const msg = `导入成功：新增 ${result.inserted || 0} 条，更新 ${result.updated || 0} 条`
    ElMessage.success(msg)
    importVisible.value = false
    fetchData()
  } catch (e) {
    // 错误已由 http 拦截器统一提示
  } finally {
    importing.value = false
  }
}
async function handleExport() {
  exporting.value = true
  try {
    const rows = await vehicleApi.all({
      keyword: keyword.value || undefined,
      vehicleType: vehicleType.value,
    })
    const data = rows.map(v => ({
      ...v,
      vehicleTypeText: v.vehicleType === 0 ? '小车' : v.vehicleType === 1 ? '大巴' : v.vehicleType,
    }))
    exportToExcel(data, exportColumns, `车辆台账_${new Date().toISOString().slice(0, 10)}.xlsx`)
    ElMessage.success(`已导出 ${data.length} 条车辆`)
  } catch (e) {
    // 错误已由 http 拦截器统一提示
  } finally {
    exporting.value = false
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

<style scoped>
.certificate-panel {
  width: 100%;
}

.certificate-current {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
</style>
