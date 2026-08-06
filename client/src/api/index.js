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
}

/**
 * 提醒 API
 */
export const reminderApi = {
  list: (params) => http.get('/reminders', { params }),
  handle: (id) => http.put(`/reminders/${id}/handle`),
}
