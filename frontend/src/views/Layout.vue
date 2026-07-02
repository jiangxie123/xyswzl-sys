<template>
  <el-container class="layout-container">
    <el-aside width="200px" class="aside">
      <div class="logo">
        <el-icon :size="24"><Document /></el-icon>
        <span>失物招领</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        background-color="#001529"
        text-color="rgba(255,255,255,0.85)"
        active-text-color="#409EFF"
      >
        <el-menu-item index="/items">
          <el-icon><Goods /></el-icon>
          <span>物品列表</span>
        </el-menu-item>
        <el-menu-item index="/publish">
          <el-icon><EditPen /></el-icon>
          <span>发布物品</span>
        </el-menu-item>
        <el-menu-item v-if="userStore.isLoggedIn" index="/profile">
          <el-icon><User /></el-icon>
          <span>个人中心</span>
        </el-menu-item>
        <el-menu-item-group v-if="userStore.isAdmin">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>管理功能</span>
          </template>
          <el-menu-item index="/admin/users">
            <el-icon><UserFilled /></el-icon>
            <span>用户管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/categories">
            <el-icon><Collection /></el-icon>
            <span>分类管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/audit">
            <el-icon><Check /></el-icon>
            <span>物品审核</span>
          </el-menu-item>
          <el-menu-item index="/admin/logs">
            <el-icon><Tickets /></el-icon>
            <span>操作日志</span>
          </el-menu-item>
        </el-menu-item-group>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <h2>校园失物招领系统</h2>
        </div>
        <div class="header-right">
          <template v-if="userStore.isLoggedIn">
            <span class="user-info">
              <el-icon><User /></el-icon>
              {{ userStore.userInfo?.realName || userStore.userInfo?.username }}
              <el-tag v-if="userStore.isSuperAdmin" type="danger" size="small" style="margin-left: 8px;">超级管理员</el-tag>
              <el-tag v-else-if="userStore.userInfo?.role === 1" type="warning" size="small" style="margin-left: 8px;">管理员</el-tag>
              <el-tag v-else size="small" style="margin-left: 8px;">学生</el-tag>
            </span>
            <el-button type="primary" plain @click="handleLogout">退出</el-button>
          </template>
          <el-button v-else type="primary" @click="$router.push('/login')">登录</el-button>
        </div>
      </el-header>

      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)

function handleLogout() {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  }).catch(() => {})
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

.aside {
  background-color: #001529;
  color: white;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 18px;
  font-weight: bold;
  gap: 10px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.aside :deep(.el-menu) {
  border-right: none;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: white;
  border-bottom: 1px solid #ebeef5;
  padding: 0 20px;
}

.header-left h2 {
  margin: 0;
  font-size: 18px;
  color: #303133;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 15px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #606266;
}

.main-content {
  background: #f0f2f5;
  padding: 20px;
}
</style>
