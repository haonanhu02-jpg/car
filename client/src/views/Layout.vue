<template>
  <el-container style="height: 100vh">
    <!-- 顶部导航栏 -->
    <el-header class="app-header">
      <div class="header-left">
        <h2>万盛股份 · 车辆管理系统</h2>
      </div>
      <div class="header-right">
        <span class="user-info">
          {{ userStore.realName || userStore.username }}
          <el-tag :type="userStore.isAdmin ? 'danger' : 'info'" size="small">
            {{ userStore.isAdmin ? '管理员' : '查看员' }}
          </el-tag>
        </span>
        <el-button text @click="handleLogout">退出登录</el-button>
      </div>
    </el-header>

    <el-container>
      <!-- 左侧菜单 -->
      <el-aside class="app-sidebar" :width="isCollapse ? '64px' : '220px'">
        <div class="collapse-btn" @click="isCollapse = !isCollapse">
          <el-icon><Fold v-if="!isCollapse" /><Expand v-else /></el-icon>
        </div>

        <el-menu
          :default-active="activeMenu"
          :collapse="isCollapse"
          :collapse-transition="false"
          router
          background-color="#001529"
          text-color="#ffffffa6"
          active-text-color="#fff"
        >
          <template v-for="item in menuItems" :key="item.path">
            <el-menu-item :index="item.path" v-if="!item.meta?.hidden">
              <el-icon><component :is="item.meta?.icon" /></el-icon>
              <span>{{ item.meta?.title }}</span>
            </el-menu-item>
          </template>
        </el-menu>
      </el-aside>

      <!-- 主内容区 -->
      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Fold, Expand } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const isCollapse = ref(false)

const activeMenu = computed(() => route.path)

// 菜单项（从路由配置中提取）
const menuItems = computed(() =>
  router.options.routes
    .find(r => r.path === '/')
    ?.children?.filter(item => !item.meta?.adminOnly || userStore.isAdmin) || []
)

function handleLogout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.app-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  padding: 0 24px;
  height: 60px;
  z-index: 100;
}

.header-left h2 {
  margin: 0;
  font-size: 18px;
  color: #001529;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.app-sidebar {
  background: #001529;
  overflow-x: hidden;
  transition: width 0.3s;
  position: relative;
}

.collapse-btn {
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ffffffa6;
  cursor: pointer;
  border-bottom: 1px solid #ffffff1a;
}

.collapse-btn:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.05);
}

.app-main {
  background: #f0f2f5;
  padding: 24px;
  min-height: calc(100vh - 60px);
}
</style>
