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
          <el-button type="primary" :icon="Plus">添加用户</el-button>
        </div>
        <el-table :data="users" stripe>
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
              <el-switch :model-value="row.status === 1" active-text="启用" inactive-text="禁用" />
            </template>
          </el-table-column>
          <el-table-column label="创建时间" width="170">
            <template #default="{ row }">{{ row.createdAt }}</template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>

  <el-empty v-else description="仅管理员可访问系统设置" />
</template>

<script setup>
import { ref } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()

const insuranceNodes = ref([30, 15, 7, 3])
const inspectionNodes = ref([30, 15, 7, 3])
const remindMethods = ref(['system', 'sms'])
const users = ref([
  { username: 'admin', realName: '张姐', role: 'ADMIN', phone: '138****0001', status: 1, createdAt: '2024-01-01' },
  { username: 'viewer', realName: '李四', role: 'VIEWER', phone: '138****0002', status: 1, createdAt: '2024-01-15' },
])

function saveSettings() {
  ElMessage.success('提醒规则保存成功')
}
</script>
