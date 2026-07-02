<template>
  <div class="item-list">
    <el-card class="search-card" shadow="never">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="类型">
          <el-select v-model="searchForm.type" placeholder="全部" style="width: 120px" @change="loadItems">
            <el-option label="全部" :value="null" />
            <el-option label="寻物" :value="0" />
            <el-option label="拾物" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="searchForm.categoryId" placeholder="全部分类" style="width: 160px" @change="loadItems">
            <el-option label="全部分类" :value="null" />
            <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-input v-model="searchForm.keyword" placeholder="搜索标题或描述..." style="width: 260px" clearable @keyup.enter="loadItems">
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadItems">
            <el-icon><Search /></el-icon> 搜索
          </el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="list-card" shadow="never">
      <div class="list-header">
        <h3>物品信息</h3>
        <el-button type="primary" @click="$router.push('/publish')">
          <el-icon><Plus /></el-icon> 发布新物品
        </el-button>
      </div>

      <el-empty v-if="items.length === 0 && !loading" description="暂无物品信息" />

      <div v-else class="item-grid">
        <div v-for="item in items" :key="item.id" class="item-card" @click="goToDetail(item.id)">
          <!-- 图片展示区 -->
          <div class="item-image-wrapper">
            <template v-if="getImageList(item.images).length > 0">
              <img
                :src="getImageList(item.images)[0]"
                alt="item"
                class="item-image"
                @click.stop="previewImage(getImageList(item.images)[0])"
              />
              <div v-if="getImageList(item.images).length > 1" class="image-count">
                +{{ getImageList(item.images).length - 1 }}
              </div>
            </template>
            <template v-else>
              <div class="item-image-placeholder">
                <el-icon :size="32"><Picture /></el-icon>
                <span>暂无图片</span>
              </div>
            </template>
          </div>

          <!-- 内容区 -->
          <div class="item-content">
            <div class="item-type-row">
              <el-tag :type="item.type === 0 ? 'warning' : 'success'" size="small">
                {{ item.type === 0 ? '寻物' : '拾物' }}
              </el-tag>
              <el-tag :type="item.status === 0 ? '' : 'info'" size="small">
                {{ item.status === 0 ? '待认领' : item.status === 1 ? '已认领' : '已下架' }}
              </el-tag>
            </div>

            <h4 class="item-title" :title="item.title">{{ item.title }}</h4>
            <p class="item-description" :title="item.description">{{ item.description }}</p>

            <div class="item-meta">
              <span v-if="item.location" title="物品地点">
                <el-icon><Location /></el-icon>
                <span class="meta-text">{{ item.location }}</span>
              </span>
              <span v-if="item.lostTime" title="时间">
                <el-icon><Clock /></el-icon>
                <span class="meta-text">{{ formatDate(item.lostTime) }}</span>
              </span>
            </div>

            <div class="item-footer">
              <span class="create-time">发布于 {{ formatDate(item.createTime) }}</span>
              <span class="view-detail" @click.stop="goToDetail(item.id)">查看详情 →</span>
            </div>
          </div>
        </div>
      </div>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="current"
          v-model:page-size="size"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="loadItems"
          @current-change="loadItems"
        />
      </div>
    </el-card>

    <!-- 图片大图预览 -->
    <el-dialog v-model="previewVisible" title="图片预览" width="600px" align-center>
      <img :src="previewUrl" alt="preview" style="width: 100%; border-radius: 4px;" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Plus, Location, Clock, Picture } from '@element-plus/icons-vue'
import { getItemList } from '@/api/item'
import { getAllCategories } from '@/api/category'

const router = useRouter()

const loading = ref(false)
const items = ref([])
const categories = ref([])
const current = ref(1)
const size = ref(10)
const total = ref(0)

const searchForm = reactive({
  type: null,
  categoryId: null,
  keyword: ''
})

// 图片预览
const previewVisible = ref(false)
const previewUrl = ref('')

function previewImage(url) {
  previewUrl.value = url
  previewVisible.value = true
}

/**
 * 解析后端返回的 images 字段
 */
function getImageList(images) {
  if (!images) return []
  try {
    if (typeof images === 'string') {
      const parsed = JSON.parse(images)
      if (Array.isArray(parsed)) return parsed
      return [images]
    }
    if (Array.isArray(images)) return images
    return []
  } catch {
    if (typeof images === 'string') {
      return images.split(',').filter(u => u && u.trim())
    }
    return []
  }
}

function loadItems() {
  loading.value = true
  getItemList({
    current: current.value,
    size: size.value,
    type: searchForm.type,
    categoryId: searchForm.categoryId,
    keyword: searchForm.keyword
  }).then((data) => {
    items.value = data.records || []
    total.value = data.total || 0
  }).finally(() => {
    loading.value = false
  })
}

function resetSearch() {
  searchForm.type = null
  searchForm.categoryId = null
  searchForm.keyword = ''
  current.value = 1
  loadItems()
}

function loadCategories() {
  getAllCategories().then((data) => {
    categories.value = data || []
  })
}

function goToDetail(id) {
  router.push(`/items/${id}`)
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

onMounted(() => {
  loadCategories()
  loadItems()
})
</script>

<style scoped>
/* ========== 页面基础布局 ========== */
.item-list {
  width: 100%;
  max-width: 1300px;
  margin: 0 auto;
  box-sizing: border-box;
}

.search-card {
  margin-bottom: 20px;
}

.search-form {
  margin: 0;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #ebeef5;
}

.list-header h3 {
  margin: 0;
  color: #303133;
}

/* ========== 卡片网格 ========== */
.item-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

/* ========== 卡片本体（固定高度 + flex 纵向布局）========== */
.item-card {
  display: flex;
  flex-direction: column;
  height: 430px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  border: 1px solid #ebeef5;
  box-sizing: border-box;
}

.item-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 16px rgba(0, 21, 41, 0.12);
}

/* ========== 图片区（固定高度，与卡片宽度匹配）========== */
.item-image-wrapper {
  width: 100%;
  height: 180px;
  background: #f5f7fa;
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.item-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  cursor: zoom-in;
  transition: transform 0.3s;
  display: block;
}

.item-image:hover {
  transform: scale(1.05);
}

.item-image-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: #c0c4cc;
  font-size: 13px;
}

.image-count {
  position: absolute;
  bottom: 10px;
  right: 10px;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  padding: 3px 10px;
  border-radius: 12px;
  font-size: 12px;
}

/* ========== 内容区（flex 填充剩余空间）========== */
.item-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 14px 16px;
  min-height: 0;
  box-sizing: border-box;
}

.item-type-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  flex-shrink: 0;
}

.item-title {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex-shrink: 0;
}

.item-description {
  margin: 0 0 12px 0;
  color: #606266;
  font-size: 14px;
  line-height: 1.6;
  flex-shrink: 0;
  /* 限制 2 行 */
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.item-meta {
  display: flex;
  flex-direction: column;
  gap: 6px;
  color: #909399;
  font-size: 13px;
  margin-bottom: 10px;
  flex-shrink: 0;
}

.item-meta span {
  display: flex;
  align-items: center;
  gap: 4px;
  overflow: hidden;
}

.item-meta .meta-text {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  display: inline-block;
  flex: 1;
  min-width: 0;
}

/* ========== 底部固定区（顶到卡片底部）========== */
.item-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 10px;
  margin-top: auto;
  border-top: 1px solid #f0f2f5;
  flex-shrink: 0;
}

.create-time {
  color: #c0c4cc;
  font-size: 12px;
}

.view-detail {
  color: #409EFF;
  font-size: 13px;
  cursor: pointer;
}

.view-detail:hover {
  text-decoration: underline;
}

/* ========== 分页 ========== */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 30px;
}
</style>
