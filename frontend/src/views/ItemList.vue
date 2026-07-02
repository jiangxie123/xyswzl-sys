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
        <el-card v-for="item in items" :key="item.id" class="item-card" shadow="hover" @click="goToDetail(item.id)">
          <div class="item-type">
            <el-tag :type="item.type === 0 ? 'warning' : 'success'" size="small">
              {{ item.type === 0 ? '寻物' : '拾物' }}
            </el-tag>
            <el-tag :type="item.status === 0 ? '' : 'info'" size="small" class="status-tag">
              {{ item.status === 0 ? '待认领' : item.status === 1 ? '已认领/找回' : '已下架' }}
            </el-tag>
          </div>
          <h4 class="item-title">{{ item.title }}</h4>
          <p class="item-description">{{ item.description }}</p>
          <div class="item-meta">
            <span v-if="item.location">
              <el-icon><Location /></el-icon> {{ item.location }}
            </span>
            <span v-if="item.lostTime">
              <el-icon><Clock /></el-icon> {{ formatDate(item.lostTime) }}
            </span>
          </div>
          <div class="item-footer">
            <span class="create-time">发布于 {{ formatDate(item.createTime) }}</span>
            <el-button type="primary" link size="small" @click.stop="goToDetail(item.id)">查看详情 →</el-button>
          </div>
        </el-card>
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Plus, Location, Clock } from '@element-plus/icons-vue'
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
.item-list {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
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
.item-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}
.item-card {
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  border-radius: 8px;
}
.item-card:hover {
  transform: translateY(-4px);
}
.item-type {
  margin-bottom: 10px;
}
.status-tag {
  margin-left: 8px;
}
.item-title {
  margin: 0 0 10px 0;
  font-size: 16px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.item-description {
  margin: 0 0 15px 0;
  color: #606266;
  font-size: 14px;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.item-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
  color: #909399;
  font-size: 13px;
  margin-bottom: 15px;
}
.item-meta span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.item-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid #f0f2f5;
}
.create-time {
  color: #c0c4cc;
  font-size: 12px;
}
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 30px;
}
</style>
