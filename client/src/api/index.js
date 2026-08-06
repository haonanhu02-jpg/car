import http from '@/utils/http'

/**
 * 认证 API
 */
export const authApi = {
  login: (username, password) =>
    http.post('/auth/login', { username, password }),
}

/**
 * 仪表盘 API
 */
export const dashboardApi = {
  getStatistics: () => http.get('/dashboard/statistics'),
  getExpiringVehicles: () => http.get('/dashboard/expiring'),
}

/**
 * 车辆 API
 */
export const vehicleApi = {
  list: (params) => http.get('/vehicles', { params }),
  detail: (id) => http.get(`/vehicles/${id}`),
  create: (data) => http.post('/vehicles', data),
  update: (id, data) => http.put(`/vehicles/${id}`, data),
  delete: (id) => http.delete(`/vehicles/${id}`),
  renewInsurance: (id, data) => http.post(`/vehicles/${id}/renew-insurance`, data),
  updateInspection: (id, inspectionDate, expireDate) =>
    http.post(`/vehicles/${id}/update-inspection`, null, {
      params: { inspectionDate, expireDate },
    }),
  // 上传 Excel 批量导入（multipart/form-data，由后端 EasyExcel 解析）
  importExcel: (file) => {
    const formData = new FormData()
    formData.append('file', file)
    return http.post('/vehicles/import', formData)
  },
  // 循环分页拉取全部车辆（忽略分页大小），用于导出 Excel
  all: async (params = {}) => {
    const collected = []
    let page = 1
    const size = 200
    while (true) {
      const r = await http.get('/vehicles', { params: { ...params, page, size } })
      collected.push(...(r.records || []))
      if (collected.length >= r.total || (r.records || []).length === 0) break
      page++
    }
    return collected
  },
}

/**
 * 提醒 API
 */
export const reminderApi = {
  list: (params) => http.get('/reminders', { params }),
  handle: (id) => http.put(`/reminders/${id}/handle`),
  scan: () => http.post('/reminders/scan'),
}

/**
 * 提醒规则配置 API
 */
export const reminderConfigApi = {
  list: () => http.get('/reminder-config'),
  save: (data) => http.post('/reminder-config', data),
}

/**
 * 用户管理 API（管理员）
 */
export const userApi = {
  list: () => http.get('/users'),
  create: (data) => http.post('/users', data),
  updateStatus: (id, status) => http.put(`/users/${id}/status`, { status }),
  resetPassword: (id, password) => http.put(`/users/${id}/password`, { password }),
}

/**
 * 操作日志 API（管理员）
 */
export const operationLogApi = {
  list: (params) => http.get('/operation-logs', { params }),
  actions: () => http.get('/operation-logs/actions'),
}
