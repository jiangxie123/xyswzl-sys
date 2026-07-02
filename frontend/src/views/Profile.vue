<template>
  <el-card v-if="userStore.isLoggedIn">
    <template #header>
      <div class="header">
        <span>个人中心</span>
      </div>
    </template>
    
    <el-descriptions :column="2" border>
      <el-descriptions-item label="用户名">{{ userStore.userInfo?.username }}</el-descriptions-item>
      <el-descriptions-item label="真实姓名">{{ userStore.userInfo?.realName }}</el-descriptions-item>
      <el-descriptions-item label="角色">
        <el-tag v-if="userStore.userInfo?.role === 2" type="danger">超级管理员</el-tag>
        <el-tag v-else-if="userStore.userInfo?.role === 1" type="warning">管理员</el-tag>
        <el-tag v-else>学生</el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="状态">
        <el-tag :type="userStore.userInfo?.status === 1 ? 'success' : 'info'">
          {{ userStore.userInfo?.status === 1 ? '正常' : '禁用' }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="手机号">{{ userStore.userInfo?.phone || '未填写' }}</el-descriptions-item>
      <el-descriptions-item label="邮箱">{{ userStore.userInfo?.email || '未填写' }}</el-descriptions-item>
      <el-descriptions-item label="学号">{{ userStore.userInfo?.studentId || '未填写' }}</el-descriptions-item>
      <el-descriptions-item label="学院/部门">{{ userStore.userInfo?.college || '未填写' }}</el-descriptions-item>
    </el-descriptions>

    <el-divider />
    <h3>我的发布</h3>
    <el-empty description="暂无发布的物品信息（将在物品模块完成后实现）" />
  </el-card>
  
  <el-card v-else>
    <el-empty description="请先登录" />
    <el-button type="primary" @click="$router.push('/login')">前往登录</el-button>
  </el-card>
</template>

<script setup>
import { useUserStore } from '@/stores/user'
const userStore = useUserStore()
</script>

<style scoped>
.header { display: flex; justify-content: space-between; align-items: center; }
</style>
