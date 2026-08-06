import { defineStore } from 'pinia'
import { authApi } from '@/api'
import { ElMessage } from 'element-plus'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    username: localStorage.getItem('username') || '',
    realName: localStorage.getItem('realName') || '',
    role: localStorage.getItem('role') || '',
  }),

  getters: {
    isLoggedIn: (state) => !!state.token,
    isAdmin: (state) => state.role === 'ADMIN',
  },

  actions: {
    async login(username, password) {
      const data = await authApi.login(username, password)
      this.token = data.token
      this.username = data.username
      this.realName = data.realName
      this.role = data.role

      localStorage.setItem('token', data.token)
      localStorage.setItem('username', data.username)
      localStorage.setItem('realName', data.realName)
      localStorage.setItem('role', data.role)

      ElMessage.success(`欢迎回来，${data.realName || data.username}！`)
    },

    logout() {
      this.token = ''
      this.username = ''
      this.realName = ''
      this.role = ''
      localStorage.clear()
      ElMessage.success('已退出登录')
    },
  },
})
