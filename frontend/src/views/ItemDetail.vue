<template>
  <div class="item-detail">
    <div class="back-btn">
      <el-button link @click="$router.back()">
        <el-icon><ArrowLeft /></el-icon> 返回列表
      </el-button>
    </div>

    <el-card v-if="item" shadow="never" class="detail-card">
      <div class="detail-header">
        <div class="type-tags">
          <el-tag :type="item.type === 0 ? 'warning' : 'success'" size="large">
            {{ item.type === 0 ? '寻物启事' : '失物招领' }}
          </el-tag>
          <el-tag type="info" size="large" class="status-tag">
            {{ statusText }}
          </el-tag>
        </div>
        <h2 class="title">{{ item.title }}</h2>
        <div class="meta-info">
          <span v-if="item.location">
            <el-icon><Location /></el-icon> {{ item.location }}
          </span>
          <span v-if="item.lostTime">
            <el-icon><Clock /></el-icon> {{ formatDate(item.lostTime) }}
          </span>
          <span>
            <el-icon><Calendar /></el-icon> 发布于 {{ formatDate(item.createTime) }}
          </span>
        </div>
      </div>

      <el-divider />

      <div class="detail-content">
        <h4>详细描述</h4>
        <p class="description">{{ item.description }}</p>
      </div>

      <el-divider />

      <div class="contact-section">
        <h4>联系方式</h4>
        <div class="contact-list">
          <div v-if="item.contactPhone" class="contact-item">
            <el-icon><Phone /></el-icon>
            <span>电话：{{ item.contactPhone }}</span>
          </div>
          <div v-if="item.contactWechat" class="contact-item">
            <el-icon><ChatDotRound /></el-icon>
            <span>微信：{{ item.contactWechat }}</span>
          </div>
          <div v-if="item.contactQq" class="contact-item">
            <el-icon><Message /></el-icon>
            <span>QQ：{{ item.contactQq }}</span>
          </div>
        </div>
      </div>

      <div class="action-section">
        <el-button v-if="statusText !== '已下架'" type="primary" size="large" @click="handleContact">
          <el-icon><Phone /></el-icon> 联系发布者
        </el-button>
        <el-button v-if="isOwner && item.status !== 1 && item.status !== 2" type="success" size="large" @click="handleMarkComplete">
          <el-icon><Check /></el-icon> 标记已{{ item.type === 0 ? '找回' : '认领' }}
        </el-button>
        <el-button v-if="isOwner" size="large" type="danger" @click="handleDelete">
          <el-icon><Delete /></el-icon> 删除
        </el-button>
      </div>
    </el-card>

    <!-- 留言区 -->
    <el-card v-if="item" shadow="never" class="comment-card">
      <div class="comment-header">
        <h4>留言交流 ({{ commentTotal }})</h4>
      </div>

      <!-- 发布留言 -->
      <div v-if="userStore.isLoggedIn" class="comment-form">
        <el-input
          v-model="newComment"
          type="textarea"
          :rows="3"
          placeholder="说点什么吧..."
          maxlength="200"
          show-word-limit
        />
        <el-button type="primary" :loading="commentLoading" @click="submitComment" class="comment-submit">
          发布留言
        </el-button>
      </div>
      <el-alert v-else type="info" :closable="false" title="登录后可以发布留言和参与交流" />

      <el-divider />

      <!-- 留言列表 -->
      <div v-if="comments.length === 0" class="empty-comments">
        <el-empty description="暂无留言，快来抢沙发吧~" />
      </div>
      <div v-else class="comment-list">
        <div v-for="comment in comments" :key="comment.id" class="comment-item">
          <div class="comment-info">
            <el-avatar :size="40" :style="{ backgroundColor: avatarColor(comment.userId) }">
              {{ (comment.userId || 'U').toString().charAt(0) }}
            </el-avatar>
            <div class="comment-meta">
              <span class="comment-user">用户 #{{ comment.userId }}</span>
              <span class="comment-time">{{ formatDate(comment.createTime) }}</span>
            </div>
            <el-button
              v-if="canDeleteComment(comment)"
              link
              type="danger"
              size="small"
              @click="handleDeleteComment(comment)"
            >删除</el-button>
          </div>
          <div class="comment-content">{{ comment.content }}</div>
        </div>
      </div>

      <div v-if="commentTotal > 10" class="pagination-wrapper">
        <el-pagination
          v-model:current-page="commentPage"
          :page-size="10"
          :total="commentTotal"
          layout="prev, pager, next"
          background
          @current-change="loadComments"
        />
      </div>
    </el-card>

    <el-empty v-else description="物品信息不存在或已下架" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Location, Clock, Calendar, Phone, ChatDotRound, Message, Check, Delete } from '@element-plus/icons-vue'
import { getItemDetail, deleteItem, changeItemStatus } from '@/api/item'
import { getItemComments, createComment, deleteComment } from '@/api/comment'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const item = ref(null)
const comments = ref([])
const commentPage = ref(1)
const commentTotal = ref(0)
const commentLoading = ref(false)
const newComment = ref('')

const statusText = computed(() => {
  if (!item.value) return ''
  if (item.value.status === 0) return item.value.type === 0 ? '待找回' : '待认领'
  if (item.value.status === 1) return item.value.type === 0 ? '已找回' : '已认领'
  return '已下架'
})

const isOwner = computed(() => {
  if (!item.value || !userStore.userInfo) return false
  return item.value.userId === userStore.userInfo.id
})

function formatDate(dateStr) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  const h = String(date.getHours()).padStart(2, '0')
  const min = String(date.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${d} ${h}:${min}`
}

function avatarColor(userId) {
  const colors = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399', '#8e44ad', '#27ae60', '#2980b9']
  return colors[(userId || 0) % colors.length]
}

function handleContact() {
  const contacts = []
  if (item.value.contactPhone) contacts.push('电话：' + item.value.contactPhone)
  if (item.value.contactWechat) contacts.push('微信：' + item.value.contactWechat)
  if (item.value.contactQq) contacts.push('QQ：' + item.value.contactQq)
  if (contacts.length === 0) {
    ElMessage.info('发布者未留联系方式，请留言交流')
  } else {
    ElMessage.success('请通过以下方式联系：' + contacts.join('，'))
  }
}

function handleMarkComplete() {
  ElMessageBox.confirm(
    `确认将此物品标记为已${item.value.type === 0 ? '找回' : '认领'}吗？`,
    '确认操作',
    { type: 'warning' }
  ).then(() => {
    changeItemStatus(item.value.id, 1, null).then(() => {
      ElMessage.success('状态已更新')
      item.value.status = 1
    })
  }).catch(() => {})
}

function handleDelete() {
  ElMessageBox.confirm('确认删除此物品信息吗？删除后不可恢复。', '确认删除', {
    type: 'error'
  }).then(() => {
    deleteItem(item.value.id).then(() => {
      ElMessage.success('删除成功')
      router.push('/items')
    })
  }).catch(() => {})
}

// ========= 留言功能 =========
function canDeleteComment(comment) {
  if (!userStore.isLoggedIn) return false
  if (userStore.isAdmin) return true
  return comment.userId === userStore.userInfo.id
}

function submitComment() {
  if (!newComment.value.trim()) {
    ElMessage.warning('请输入留言内容')
    return
  }
  commentLoading.value = true
  createComment(item.value.id, newComment.value.trim(), null).then(() => {
    ElMessage.success('留言成功')
    newComment.value = ''
    commentPage.value = 1
    loadComments()
  }).finally(() => {
    commentLoading.value = false
  })
}

function handleDeleteComment(comment) {
  ElMessageBox.confirm('确认删除此条留言吗？', '确认删除', { type: 'warning' })
    .then(() => {
      deleteComment(comment.id).then(() => {
        ElMessage.success('删除成功')
        loadComments()
      })
    }).catch(() => {})
}

function loadComments() {
  getItemComments(item.value.id, commentPage.value, 10).then((data) => {
    comments.value = data.records || []
    commentTotal.value = data.total || 0
  })
}

function loadDetail() {
  const id = route.params.id
  if (!id) {
    ElMessage.error('物品ID无效')
    return
  }
  getItemDetail(id).then((data) => {
    item.value = data
  }).then(() => {
    // 加载留言
    loadComments()
  }).catch(() => {
    item.value = null
  })
}

onMounted(() => {
  loadDetail()
})
</script>

<style scoped>
.item-detail {
  padding: 20px;
  max-width: 900px;
  margin: 0 auto;
}
.back-btn {
  margin-bottom: 15px;
}
.detail-card, .comment-card {
  border-radius: 8px;
  margin-bottom: 20px;
}
.detail-header .type-tags {
  margin-bottom: 16px;
}
.status-tag {
  margin-left: 8px;
}
.title {
  margin: 0 0 16px 0;
  font-size: 24px;
  color: #303133;
}
.meta-info {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  color: #909399;
  font-size: 14px;
}
.meta-info span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}
.description {
  color: #606266;
  font-size: 15px;
  line-height: 1.8;
  margin: 10px 0 0 0;
  white-space: pre-wrap;
}
h4 {
  margin: 0 0 15px 0;
  color: #303133;
}
.contact-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.contact-item {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #606266;
  font-size: 14px;
}
.action-section {
  display: flex;
  gap: 12px;
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
  flex-wrap: wrap;
}
.comment-header {
  margin-bottom: 20px;
}
.comment-form {
  margin-bottom: 20px;
}
.comment-submit {
  margin-top: 12px;
}
.comment-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.comment-item {
  padding: 16px;
  background: #f8fafc;
  border-radius: 8px;
}
.comment-info {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}
.comment-meta {
  flex: 1;
  display: flex;
  flex-direction: column;
}
.comment-user {
  font-weight: 500;
  color: #303133;
  font-size: 14px;
}
.comment-time {
  color: #909399;
  font-size: 12px;
}
.comment-content {
  color: #606266;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
}
.empty-comments {
  padding: 30px 0;
}
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>
