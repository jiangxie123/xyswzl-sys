<template>
  <div class="profile-page">
    <el-card v-if="userStore.isLoggedIn">
      <template #header>
        <div class="header">
          <span>个人中心</span>
        </div>
      </template>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="用户名">{{ userStore.userInfo?.username }}</el-descriptions-item>
        <el-descriptions-item label="真实姓名">{{ userStore.userInfo?.realName || '未填写' }}</el-descriptions-item>
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

      <el-table :data="items" v-loading="loading" border stripe>
        <el-table-column prop="title" label="标题" min-width="180">
          <template #default="{ row }">
            <el-link type="primary" @click="goDetail(row.id)" :underline="false">
              {{ row.title }}
            </el-link>
          </template>
        </el-table-column>

        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag :type="row.type === 0 ? 'warning' : 'success'" size="small">
              {{ row.type === 0 ? '寻物' : '拾物' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'warning' : (row.status === 1 ? 'success' : 'info')" size="small">
              {{ statusText(row) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="审核状态" width="110">
          <template #default="{ row }">
            <el-tag v-if="row.auditStatus === 0" type="warning" size="small">待审核</el-tag>
            <el-tag v-else-if="row.auditStatus === 1" type="success" size="small">已通过</el-tag>
            <el-tag v-else type="danger" size="small">已驳回</el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="createTime" label="发布时间" width="180" />

        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="goDetail(row.id)">查看详情</el-button>
            <el-button type="primary" link size="small" @click="goEdit(row.id)">编辑</el-button>
            <el-button type="primary" link size="small" @click="handleToggleStatus(row)" v-if="row.status === 0">
              标记{{ row.type === 0 ? '已找回' : '已认领' }}
            </el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="items.length === 0 && !loading" description="您还未发布任何物品" />
    </el-card>

    <el-card v-else>
      <el-empty description="请先登录" />
      <el-button type="primary" @click="$router.push('/login')">前往登录</el-button>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getMyItems, deleteItem, changeItemStatus } from '@/api/item'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const items = ref([])

function statusText(row) {
  if (row.status === 0) return row.type === 0 ? '待找回' : '待认领'
  if (row.status === 1) return row.type === 0 ? '已找回' : '已认领'
  return '已下架'
}

function loadItems() {
  if (!userStore.isLoggedIn) {
    return
  }
  loading.value = true
  getMyItems(1, 100).then((data) => {
    items.value = data?.records || []
  }).catch((err) => {
    console.error('加载我的发布失败:', err)
    items.value = []
  }).finally(() => {
    loading.value = false
  })
}

function goDetail(id) {
  router.push('/items/' + id)
}

function goEdit(id) {
  router.push('/publish?editId=' + id)
}

function handleToggleStatus(row) {
  const newStatus = 1
  ElMessageBox.confirm(
    `确认将此物品标记为${row.type === 0 ? '已找回' : '已认领'}吗？`,
    '确认操作',
    { type: 'warning' }
  ).then(() => {
    changeItemStatus(row.id, newStatus, null).then(() => {
      ElMessage.success('操作成功')
      loadItems()
    })
  }).catch(() => {})
}

function handleDelete(row) {
  ElMessageBox.confirm(
    `确认删除《${row.title}》吗？删除后不可恢复。`,
    '确认删除',
    { type: 'error', confirmButtonText: '确认删除', cancelButtonText: '取消' }
  ).then(() => {
    deleteItem(row.id).then(() => {
      ElMessage.success('删除成功')
      loadItems()
    })
  }).catch(() => {})
}

onMounted(() => {
  loadItems()
})
</script>

<style scoped>
.profile-page {
  padding: 10px 20px;
  max-width: 1200px;
  margin: 0 auto;
  box-sizing: border-box;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

h3 {
  margin: 0 0 16px 0;
  color: #303133;
}
</style>
