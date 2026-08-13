<template>
  <div class="settings-page" v-if="userStore.isAdmin">
    <el-tabs type="border-card" v-model="activeTab">
      <!-- 提醒规则设置 -->
      <el-tab-pane label="提醒规则设置">
        <h4>保险提醒节点</h4>
        <el-checkbox-group v-model="insuranceNodes" style="margin: 16px 0">
          <el-checkbox :value="30">提前 30 天</el-checkbox>
          <el-checkbox :value="15">提前 15 天</el-checkbox>
          <el-checkbox :value="7">提前 7 天</el-checkbox>
          <el-checkbox :value="3">提前 3 天</el-checkbox>
        </el-checkbox-group>

        <h4>年检提醒节点</h4>
        <el-checkbox-group v-model="inspectionNodes" style="margin: 16px 0">
          <el-checkbox :value="30">提前 30 天</el-checkbox>
          <el-checkbox :value="15">提前 15 天</el-checkbox>
          <el-checkbox :value="7">提前 7 天</el-checkbox>
          <el-checkbox :value="3">提前 3 天</el-checkbox>
        </el-checkbox-group>

        <h4>提醒方式</h4>
        <el-checkbox-group v-model="remindMethods" style="margin: 16px 0">
          <el-checkbox value="system">系统内消息</el-checkbox>
          <el-checkbox value="email">邮件通知</el-checkbox>
        </el-checkbox-group>

        <h4>通知接收邮箱</h4>
        <div class="email-setting-row">
          <el-input v-model="notifyEmail" placeholder="到期提醒邮件统一发送到此邮箱" />
          <el-button :loading="testingEmail" @click="testNotifyEmail">发送测试邮件</el-button>
        </div>
        <div class="setting-tip">邮件会在保险或年检到达启用的提醒节点时发送；SMTP 专属密码由服务器环境变量管理。</div>

        <el-button type="primary" @click="saveSettings" style="margin-top: 16px">保存设置</el-button>
      </el-tab-pane>

      <!-- 用户权限管理 -->
      <el-tab-pane label="权限管理">
        <div style="margin-bottom: 16px">
          <el-button type="primary" :icon="Plus" @click="openAddDialog">添加用户</el-button>
        </div>
        <el-table :data="users" stripe v-loading="loadingUsers">
          <el-table-column prop="realName" label="姓名" width="120" />
          <el-table-column label="角色" width="100">
            <template #default="{ row }">
              <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'info'">
                {{ row.role === 'ADMIN' ? '管理员' : '普通员工' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="phone" label="手机号" width="130" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-switch
                :model-value="row.status === 1"
                active-text="启用"
                inactive-text="禁用"
                @change="(val) => toggleStatus(row, val)"
              />
            </template>
          </el-table-column>
          <el-table-column label="创建时间" width="170">
            <template #default="{ row }">{{ row.createdAt }}</template>
          </el-table-column>
          <el-table-column label="操作" min-width="260" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" size="small" @click="openEditDialog(row)">编辑</el-button>
              <el-button type="warning" size="small" @click="openResetPassword(row)">重置密码</el-button>
              <el-button
                type="danger"
                size="small"
                :disabled="row.username === userStore.username"
                @click="deleteUser(row)"
              >删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 账号审批 -->
      <el-tab-pane label="账号审批">
        <div style="margin-bottom: 16px; display: flex; align-items: center; gap: 12px">
          <el-radio-group v-model="regStatusFilter" @change="loadRegistrations">
            <el-radio-button :value="null">全部</el-radio-button>
            <el-radio-button :value="0">待审批</el-radio-button>
            <el-radio-button :value="1">已通过</el-radio-button>
            <el-radio-button :value="2">已拒绝</el-radio-button>
          </el-radio-group>
          <el-button :icon="Refresh" @click="loadRegistrations">刷新</el-button>
        </div>
        <el-table :data="registrations" stripe v-loading="loadingRegs">
          <el-table-column prop="realName" label="姓名" width="110" />
          <el-table-column prop="employeeNo" label="工号" width="120" />
          <el-table-column prop="department" label="部门" width="140" />
          <el-table-column prop="phone" label="手机号" width="130" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="regStatusTag(row.status)">{{ regStatusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="申请时间" width="170" />
          <el-table-column label="操作" width="220">
            <template #default="{ row }">
              <template v-if="row.status === 0">
                <el-button type="primary" size="small" @click="openEditRegistration(row)">编辑</el-button>
                <el-button type="success" size="small" @click="approveReg(row)">通过</el-button>
                <el-button type="danger" size="small" @click="rejectReg(row)">拒绝</el-button>
              </template>
              <span v-else-if="row.status === 2" style="color: #909399">已拒绝</span>
              <span v-else style="color: #67c23a">已通过</span>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- 添加用户对话框 -->
    <el-dialog v-model="addDialogVisible" title="添加用户" width="420px">
      <el-form :model="addForm" label-width="80px" :rules="addRules" ref="addFormRef">
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="addForm.realName" placeholder="姓名（登录时使用）" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="addForm.password" type="password" placeholder="至少 6 位" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="addForm.phone" placeholder="手机号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAddUser">确定</el-button>
      </template>
    </el-dialog>

    <!-- 编辑用户对话框 -->
    <el-dialog v-model="editDialogVisible" title="编辑用户信息" width="420px" @closed="editFormRef?.clearValidate()">
      <el-form :model="editForm" label-width="80px" :rules="editRules" ref="editFormRef">
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="editForm.realName" placeholder="真实姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="editForm.phone" placeholder="11 位手机号" maxlength="11" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingUser" @click="submitEditUser">保存</el-button>
      </template>
    </el-dialog>

    <!-- 编辑待审批申请 -->
    <el-dialog v-model="editRegistrationVisible" title="编辑申请信息" width="440px">
      <el-form ref="editRegistrationFormRef" :model="editRegistrationForm" :rules="editRegistrationRules" label-width="80px">
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="editRegistrationForm.realName" placeholder="姓名（登录时使用）" />
        </el-form-item>
        <el-form-item label="工号" prop="employeeNo">
          <el-input v-model="editRegistrationForm.employeeNo" />
        </el-form-item>
        <el-form-item label="部门" prop="department">
          <el-input v-model="editRegistrationForm.department" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="editRegistrationForm.phone" maxlength="11" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editRegistrationVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingRegistration" @click="saveRegistration">保存</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码对话框 -->
    <el-dialog v-model="resetDialogVisible" title="重置密码" width="420px">
      <el-form :model="resetForm" label-width="80px" :rules="resetRules" ref="resetFormRef">
        <el-form-item label="新密码" prop="password">
          <el-input v-model="resetForm.password" type="password" placeholder="至少 6 位" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitResetPassword">确定</el-button>
      </template>
    </el-dialog>
  </div>

  <el-empty v-else description="仅管理员可访问系统设置" />
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { reminderConfigApi, userApi, systemConfigApi, registrationApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const userStore = useUserStore()
const route = useRoute()

// Tab 控制（支持从工作台跳转时默认打开账号审批）
const activeTab = ref('0')

// 提醒规则
const insuranceNodes = ref([30, 15, 7, 3])
const inspectionNodes = ref([30, 15, 7, 3])
const remindMethods = ref(['system', 'email'])
const notifyEmail = ref('')
const testingEmail = ref(false)

// 用户管理
const users = ref([])
const loadingUsers = ref(false)
const addDialogVisible = ref(false)
const editDialogVisible = ref(false)
const resetDialogVisible = ref(false)
const currentUser = ref(null)
const savingUser = ref(false)

const addFormRef = ref(null)
const addForm = reactive({
  password: '',
  realName: '',
  phone: '',
})

const resetFormRef = ref(null)
const resetForm = reactive({
  password: '',
})

const editFormRef = ref(null)
const editForm = reactive({
  realName: '',
  phone: '',
})

const addRules = {
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' },
  ],
}

const resetRules = {
  password: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' },
  ],
}

const editRules = {
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [
    { pattern: /^1\d{10}$/, message: '请输入正确的 11 位手机号', trigger: 'blur' },
  ],
}

// 账号审批
const registrations = ref([])
const loadingRegs = ref(false)
const regStatusFilter = ref(null)
const editRegistrationVisible = ref(false)
const editRegistrationFormRef = ref(null)
const editingRegistrationId = ref(null)
const savingRegistration = ref(false)
const editRegistrationForm = reactive({ realName: '', employeeNo: '', department: '', phone: '' })
const editRegistrationRules = {
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  employeeNo: [{ required: true, message: '请输入工号', trigger: 'blur' }],
  department: [{ required: true, message: '请输入部门', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '请输入正确的 11 位手机号', trigger: 'blur' },
  ],
}

onMounted(() => {
  if (route.query.tab === 'registration') {
    activeTab.value = '2'
  }
  loadSettings()
  loadUsers()
  loadRegistrations()
})

async function loadRegistrations() {
  loadingRegs.value = true
  try {
    registrations.value = await registrationApi.list(regStatusFilter.value)
  } catch (e) {
    ElMessage.error('加载申请列表失败：' + (e.message || '未知错误'))
  } finally {
    loadingRegs.value = false
  }
}

function regStatusLabel(status) {
  if (status === 1) return '已通过'
  if (status === 2) return '已拒绝'
  return '待审批'
}

function regStatusTag(status) {
  if (status === 1) return 'success'
  if (status === 2) return 'info'
  return 'warning'
}

async function approveReg(row) {
  try {
    await registrationApi.approve(row.id)
    ElMessage.success('已通过，用户可登录')
    loadRegistrations()
  } catch (e) {
    ElMessage.error('操作失败：' + (e.message || '未知错误'))
  }
}

async function rejectReg(row) {
  let reason = ''
  try {
    const res = await ElMessageBox.prompt('请输入拒绝原因', '拒绝申请', {
      confirmButtonText: '确定拒绝',
      cancelButtonText: '取消',
      inputType: 'textarea',
      inputPlaceholder: '可选，说明拒绝原因',
    })
    reason = res.value || ''
  } catch (e) {
    return // 用户取消
  }
  try {
    await registrationApi.reject(row.id, reason)
    ElMessage.success('已拒绝')
    loadRegistrations()
  } catch (e) {
    ElMessage.error('操作失败：' + (e.message || '未知错误'))
  }
}

async function loadSettings() {
  try {
    const configs = await reminderConfigApi.list()
    if (configs && configs.length > 0) {
      insuranceNodes.value = configs
        .filter((c) => c.type === 0 && c.enabled === 1)
        .map((c) => c.nodeDays)
      inspectionNodes.value = configs
        .filter((c) => c.type === 1 && c.enabled === 1)
        .map((c) => c.nodeDays)
      const methods = configs[0]?.remindMethods
      remindMethods.value = methods ? methods.split(',') : ['system']
    }
    const email = await systemConfigApi.getNotifyEmail()
    notifyEmail.value = email || ''
  } catch (e) {
    console.error('加载提醒规则失败', e)
  }
}

async function saveSettings() {
  try {
    await reminderConfigApi.save({
      insuranceNodes: insuranceNodes.value,
      inspectionNodes: inspectionNodes.value,
      remindMethods: remindMethods.value,
    })
    await systemConfigApi.saveNotifyEmail(notifyEmail.value)
    ElMessage.success('提醒规则保存成功')
  } catch (e) {
    ElMessage.error('保存失败：' + (e.message || '未知错误'))
  }
}

async function loadUsers() {
  loadingUsers.value = true
  try {
    users.value = await userApi.list()
  } finally {
    loadingUsers.value = false
  }
}

function openAddDialog() {
  addForm.password = ''
  addForm.realName = ''
  addForm.phone = ''
  addDialogVisible.value = true
}

async function submitAddUser() {
  if (!addFormRef.value) return
  await addFormRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      await userApi.create({ ...addForm })
      ElMessage.success('用户创建成功')
      addDialogVisible.value = false
      loadUsers()
    } catch (e) {
      ElMessage.error('创建失败：' + (e.message || '未知错误'))
    }
  })
}

async function toggleStatus(row, enabled) {
  const newStatus = enabled ? 1 : 0
  try {
    await userApi.updateStatus(row.id, newStatus)
    ElMessage.success('状态更新成功')
    loadUsers()
  } catch (e) {
    ElMessage.error('状态更新失败：' + (e.message || '未知错误'))
  }
}

async function testNotifyEmail() {
  if (!/^\S+@\S+\.\S+$/.test(notifyEmail.value)) {
    ElMessage.warning('请输入正确的邮箱地址')
    return
  }
  testingEmail.value = true
  try {
    await systemConfigApi.testNotifyEmail(notifyEmail.value)
    ElMessage.success('测试邮件已发送，请检查收件箱')
  } catch (e) {
    ElMessage.error(e.message || '测试邮件发送失败')
  } finally {
    testingEmail.value = false
  }
}

function openEditRegistration(row) {
  editingRegistrationId.value = row.id
  Object.assign(editRegistrationForm, {
    realName: row.realName || '',
    employeeNo: row.employeeNo || '',
    department: row.department || '',
    phone: row.phone || '',
  })
  editRegistrationVisible.value = true
}

async function saveRegistration() {
  const valid = await editRegistrationFormRef.value?.validate().catch(() => false)
  if (!valid || !editingRegistrationId.value) return
  savingRegistration.value = true
  try {
    await registrationApi.update(editingRegistrationId.value, { ...editRegistrationForm })
    ElMessage.success('申请信息更新成功')
    editRegistrationVisible.value = false
    await loadRegistrations()
  } catch (e) {
    ElMessage.error('更新失败：' + (e.message || '未知错误'))
  } finally {
    savingRegistration.value = false
  }
}

function openEditDialog(row) {
  currentUser.value = row
  editForm.realName = row.realName || ''
  editForm.phone = row.phone || ''
  editDialogVisible.value = true
}

async function submitEditUser() {
  if (!editFormRef.value || !currentUser.value) return
  const valid = await editFormRef.value.validate().catch(() => false)
  if (!valid) return

  savingUser.value = true
  try {
    const updatedUser = await userApi.update(currentUser.value.id, { ...editForm })
    if (currentUser.value.username === userStore.username) {
      userStore.updateProfile(updatedUser.realName)
    }
    ElMessage.success('用户信息更新成功')
    editDialogVisible.value = false
    await loadUsers()
  } catch (e) {
    ElMessage.error('更新失败：' + (e.message || '未知错误'))
  } finally {
    savingUser.value = false
  }
}

async function deleteUser(row) {
  if (row.username === userStore.username) {
    ElMessage.warning('不能删除当前登录账号')
    return
  }
  try {
    await ElMessageBox.confirm(
      `删除后“${row.realName}”将无法登录，且该操作不可恢复。是否继续？`,
      '删除用户',
      {
        type: 'warning',
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
      },
    )
    await userApi.delete(row.id)
    ElMessage.success('用户删除成功')
    await loadUsers()
  } catch (e) {
    if (e === 'cancel' || e === 'close') return
    ElMessage.error('删除失败：' + (e.message || '未知错误'))
  }
}

function openResetPassword(row) {
  currentUser.value = row
  resetForm.password = ''
  resetDialogVisible.value = true
}

async function submitResetPassword() {
  if (!resetFormRef.value || !currentUser.value) return
  await resetFormRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      await userApi.resetPassword(currentUser.value.id, resetForm.password)
      ElMessage.success('密码重置成功')
      resetDialogVisible.value = false
    } catch (e) {
      ElMessage.error('重置失败：' + (e.message || '未知错误'))
    }
  })
}
</script>

<style scoped>
.email-setting-row {
  display: flex;
  gap: 12px;
  max-width: 520px;
  margin: 16px 0 8px;
}

.setting-tip {
  color: #909399;
  font-size: 13px;
}
</style>
