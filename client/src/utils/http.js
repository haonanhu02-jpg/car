import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const http = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

// 请求拦截器 — 自动附带 Token
http.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截器 — 统一错误处理
http.interceptors.response.use(
  response => {
    if (response.config.responseType === 'blob') {
      return response.data
    }
    const { code, message, data } = response.data
    if (code !== 0) {
      ElMessage.error(message || '请求失败')
      return Promise.reject(new Error(message))
    }
    return data
  },
  error => {
    // silent 请求：不弹全局提示，仅 reject（用于可失败的增强类请求，如工作台待办轮询）
    if (error.config?.silent) {
      return Promise.reject(error)
    }
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      router.push('/login')
      ElMessage.error('登录已过期，请重新登录')
    } else if (error.response?.status === 403) {
      ElMessage.error('权限不足，仅管理员可执行此操作')
    } else {
      ElMessage.error(error.response?.data?.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default http
