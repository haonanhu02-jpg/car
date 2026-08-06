<template>
  <div class="settings-page" v-if="userStore.isAdmin">
    <el-tabs type="border-card">
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
          <el-checkbox value="sms">短信通知</el-checkbox>
          <el-checkbox value="email">邮件通知</el-checkbox>
        </el-checkbox-group>

        <el-button type="primary" @click="saveSettings" style="margin-top: 16px">保存设置</el-button>
      </el-tab-pane>

      <!-- 用户权限管理 -->
      <el-tab-pane label="权限管理">
        <div style="margin-bottom: 16px">
          <el-button type="primary" :icon="Plus" @click="openAddDialog">添加用户</el-button>
        </div>
        <el-table :data="users" stripe v-loading="loadingUsers">
          <el-table-column prop="username" label="用户名" width="120" />
          <el-table-column prop="realName" label="姓名" width="100" />
          <el-table-column label="角色" width="100">
            <template #default="{ row }">
              <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'info'">
                {{ row.role === 'ADMIN' ? '管理员' : '查看员' }}
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
          <el-table-column label="操作" width="180">
            <template #default="{ row }">
              <el-button type="warning" size="small" @click="openResetPassword(row)">重置密码</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- 添加用户对话框 -->
    <el-dialog v-model="addDialogVisible" title="添加用户" width="420px">
      <el-form :model="addForm" label-width="80px" :rules="addRules" ref="addFormRef">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="addForm.username" placeholder="登录用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="addForm.password" type="password" placeholder="至少 6 位" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="addForm.realName" placeholder="真实姓名" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="addForm.role" placeholder="请选择角色" style="width: 100%">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="查看员" value="VIEWER" />
          </el-select>
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
import { Plus } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { reminderConfigApi, userApi } from '@/api'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()

// 提醒规则
const insuranceNodes = ref([30, 15, 7, 3])
const inspectionNodes = ref([30, 15, 7, 3])
const remindMethods = ref(['system', 'sms'])

// 用户管理
const users = ref([])
const loadingUsers = ref(false)
const addDialogVisible = ref(false)
const resetDialogVisible = ref(false)
const currentUser = ref(null)

const addFormRef = ref(null)
const addForm = reactive({
  username: '',
  password: '',
  realName: '',
  role: 'VIEWER',
  phone: '',
})

const resetFormRef = ref(null)
const resetForm = reactive({
  password: '',
})

const addRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' },
  ],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
}

const resetRules = {
  password: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' },
  ],
}

onMounted(() => {
  loadSettings()
  loadUsers()
})

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
  addForm.username = ''
  addForm.password = ''
  addForm.realName = ''
  addForm.role = 'VIEWER'
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
