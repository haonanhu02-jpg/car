import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { noAuth: true },
  },
  {
    path: '/',
    component: () => import('@/views/Layout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '工作台', icon: 'Monitor' },
      },
      {
        path: 'vehicles',
        name: 'Vehicles',
        component: () => import('@/views/Vehicles.vue'),
        meta: { title: '车辆台账', icon: 'Van' },
      },
      {
        path: 'vehicles/:id',
        name: 'VehicleDetail',
        component: () => import('@/views/VehicleDetail.vue'),
        meta: { title: '车辆详情', hidden: true },
      },
      {
        path: 'reminders',
        name: 'Reminders',
        component: () => import('@/views/Reminders.vue'),
        meta: { title: '提醒中心', icon: 'Bell' },
      },
      {
        path: 'reports',
        name: 'Reports',
        component: () => import('@/views/Reports.vue'),
        meta: { title: '统计报表', icon: 'DataAnalysis' },
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('@/views/Settings.vue'),
        meta: { title: '系统设置', icon: 'Setting', adminOnly: true },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 路由守卫 — 未登录跳转登录页，管理员路由权限校验
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const role = localStorage.getItem('role')

  if (to.meta.noAuth) {
    // 已登录用户访问登录页时跳转到首页
    if (token && to.path === '/login') {
      next('/dashboard')
    } else {
      next()
    }
  } else if (!token) {
    next('/login')
  } else if (to.meta.adminOnly && role !== 'ADMIN') {
    // 非管理员访问管理员路由，重定向到首页
    next('/dashboard')
  } else {
    next()
  }
})

export default router
