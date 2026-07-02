<template>
  <el-container class="layout-container">
    <!-- 背景层（动态检测 /images/app-bg.jpg 是否存在，不存在则用默认渐变） -->
    <div class="layout-bg" :class="{ 'has-image': bgImageExists }"></div>

    <el-aside width="220px" class="aside">
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

    <el-container class="right-container">
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
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)

// ========= 背景图片动态检测 =========
const bgImageExists = ref(false)

function checkBgImage() {
  const img = new Image()
  img.onload = () => { bgImageExists.value = true }
  img.onerror = () => { bgImageExists.value = false }
  img.src = '/images/app-bg.jpg?t=' + Date.now()
}

onMounted(() => {
  checkBgImage()
})

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
/* ========== 总体布局 ========== */
.layout-container {
  height: 100vh;
  width: 100vw;
  position: relative;
  overflow: hidden;
}

/* ========== 背景层 ========== */
.layout-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: #e0e7ff;
  background-image: linear-gradient(135deg, #e0e7ff 0%, #f3e8ff 50%, #fce7f3 100%);
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  z-index: 0;
}

.layout-bg.has-image {
  background-image: url('/images/app-bg.jpg');
  background-color: transparent;
}

/* ========== 左侧栏 ========== */
.aside {
  background-color: rgba(0, 21, 41, 0.95);
  color: white;
  position: relative;
  z-index: 1;
  height: 100%;
  overflow-y: auto;
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
  flex-shrink: 0;
}

.aside :deep(.el-menu) {
  border-right: none;
}

/* ========== 右侧内容区 ========== */
.right-container {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  height: 100vh;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgba(255, 255, 255, 0.95);
  border-bottom: 1px solid #ebeef5;
  padding: 0 20px;
  height: 60px !important;
  flex-shrink: 0;
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
  padding: 20px;
  background: rgba(248, 250, 252, 0.85);
  flex: 1;
  overflow-y: auto;
  box-sizing: border-box;
}
</style>
