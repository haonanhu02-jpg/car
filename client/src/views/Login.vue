<template>
  <div class="login-container">
    <div class="login-card">
      <h1 class="login-title">万盛股份 · 车辆管理系统</h1>
      <p class="login-subtitle">PC 后台管理端</p>

      <el-form ref="formRef" :model="form" :rules="rules" size="large" @submit.prevent="handleLogin">
        <el-form-item prop="realName">
          <el-input v-model="form.realName" placeholder="请输入姓名" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码"
            :prefix-icon="Lock" show-password @keyup.enter="handleLogin" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" native-type="submit" style="width: 100%">
            登 录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="login-hint">
        <p>请使用姓名和管理员分配的密码登录</p>
        <p class="register-link" @click="registerDialogVisible = true">
          还没有账号？<span>申请注册</span>
        </p>
      </div>
    </div>

    <!-- 自助注册申请对话框 -->
    <el-dialog v-model="registerDialogVisible" title="申请注册账号" width="460px" @closed="resetRegisterForm">
      <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" label-width="84px">
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="registerForm.realName" placeholder="真实姓名（登录时使用）" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="registerForm.password" type="password" placeholder="至少 6 位" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="registerForm.confirmPassword" type="password" placeholder="再次输入密码" show-password />
        </el-form-item>
        <el-form-item label="工号" prop="employeeNo">
          <el-input v-model="registerForm.employeeNo" placeholder="员工工号" />
        </el-form-item>
        <el-form-item label="部门" prop="department">
          <el-input v-model="registerForm.department" placeholder="所属部门" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="registerForm.phone" placeholder="11 位手机号" maxlength="11" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="registerDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="registerLoading" @click="submitRegister">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { registrationApi } from '@/api'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const formRef = ref(null)

// 进入登录页先清掉可能残留的旧登录态，避免路由守卫用失效 token 自动跳进受限页面
onMounted(() => {
  userStore.token = ''
  userStore.username = ''
  userStore.realName = ''
  userStore.role = ''
  localStorage.removeItem('token')
  localStorage.removeItem('username')
  localStorage.removeItem('realName')
  localStorage.removeItem('role')
})

const form = reactive({
  realName: '',
  password: '',
})

const rules = {
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await userStore.login(form.realName, form.password)
    router.push('/dashboard')
  } finally {
    loading.value = false
  }
}

// ── 自助注册申请 ──
const registerDialogVisible = ref(false)
const registerLoading = ref(false)
const registerFormRef = ref(null)

const registerForm = reactive({
  password: '',
  confirmPassword: '',
  realName: '',
  employeeNo: '',
  department: '',
  phone: '',
})

const registerRules = {
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== registerForm.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  employeeNo: [{ required: true, message: '请输入工号', trigger: 'blur' }],
  department: [{ required: true, message: '请输入部门', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '请输入正确的 11 位手机号', trigger: 'blur' },
  ],
}

function resetRegisterForm() {
  registerFormRef.value?.resetFields()
}

async function submitRegister() {
  if (!registerFormRef.value) return
  const valid = await registerFormRef.value.validate().catch(() => false)
  if (!valid) return

  registerLoading.value = true
  try {
    await registrationApi.apply({ ...registerForm })
    ElMessage.success('申请已提交，请等待管理员审批')
    registerDialogVisible.value = false
  } catch (e) {
    ElMessage.error('提交失败：' + (e.message || '未知错误'))
  } finally {
    registerLoading.value = false
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  width: 420px;
  padding: 40px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}

.login-title {
  text-align: center;
  font-size: 24px;
  color: #303133;
  margin-bottom: 4px;
}

.login-subtitle {
  text-align: center;
  color: #909399;
  margin-bottom: 32px;
}

.login-hint {
  text-align: center;
  color: #c0c4cc;
  font-size: 12px;
  margin-top: 16px;
}

.register-link {
  margin-top: 8px;
  cursor: pointer;
}

.register-link span {
  color: #667eea;
  font-weight: 600;
}

.register-link:hover span {
  text-decoration: underline;
}
</style>
