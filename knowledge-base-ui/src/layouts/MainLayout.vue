<template>
  <el-container class="main-layout">
    <el-aside width="220px" class="sidebar">
      <div class="logo">
        <el-icon size="28" color="#409EFF"><Collection /></el-icon>
        <span class="logo-text">知识库系统</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        class="sidebar-menu"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
      >
        <el-menu-item index="/chat">
          <el-icon><ChatDotRound /></el-icon>
          <span>AI对话</span>
        </el-menu-item>
        <el-menu-item index="/knowledge">
          <el-icon><Collection /></el-icon>
          <span>知识库管理</span>
        </el-menu-item>
        <el-menu-item index="/documents">
          <el-icon><Document /></el-icon>
          <span>文档管理</span>
        </el-menu-item>
        <el-menu-item index="/user" v-if="user?.permissions?.includes('user:menu')">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/role" v-if="user?.permissions?.includes('role:menu')">
          <el-icon><UserFilled /></el-icon>
          <span>角色管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="header-right">
          <el-tag type="success" v-if="isConnected">服务正常</el-tag>
          <el-tag type="danger" v-else>服务异常</el-tag>
          
          <el-dropdown @command="handleUserMenu">
            <div class="user-info">
              <el-avatar :size="32" :src="user?.avatar || ''">{{ user?.nickname?.charAt(0) || '用' }}</el-avatar>
              <span class="user-name">{{ user?.nickname || user?.username }}</span>
              <el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { chatApi } from '@/api/chat'
import { User, UserFilled } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const isConnected = ref(false)

// 使用计算属性获取用户信息，确保响应式更新
const user = computed(() => {
  const userStr = localStorage.getItem('user')
  return userStr && userStr !== 'undefined' ? JSON.parse(userStr) : null
})

// 调试信息
console.log('User info:', user.value)
console.log('User permissions:', user.value?.permissions)
console.log('User has user:menu permission:', user.value?.permissions?.includes('user:menu'))
console.log('User has role:menu permission:', user.value?.permissions?.includes('role:menu'))

const activeMenu = computed(() => route.path)

const checkHealth = async () => {
  try {
    await chatApi.healthCheck()
    isConnected.value = true
  } catch {
    isConnected.value = false
  }
}

const handleUserMenu = (command: string) => {
  if (command === 'logout') {
    // 退出登录
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    ElMessage.success('退出成功')
    router.push('/login')
  } else if (command === 'profile') {
    // 个人中心（后续可扩展）
    ElMessage.info('个人中心功能开发中')
  }
}

onMounted(() => {
  checkHealth()
  setInterval(checkHealth, 30000)
})
</script>

<style scoped>
.main-layout {
  height: 100vh;
}

.sidebar {
  background-color: #304156;
  box-shadow: 2px 0 6px rgba(0, 21, 41, 0.35);
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 16px;
  border-bottom: 1px solid #1f2d3d;
}

.logo-text {
  color: #fff;
  font-size: 18px;
  font-weight: 600;
  margin-left: 12px;
}

.sidebar-menu {
  border-right: none;
}

.header {
  background-color: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 4px;
  transition: background-color 0.3s;
}

.user-info:hover {
  background-color: #f5f7fa;
}

.user-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.main-content {
  background-color: #f0f2f5;
  padding: 20px;
  overflow-y: auto;
}
</style>
