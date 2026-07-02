<template>
  <div class="item-audit">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <h3>物品审核管理</h3>
        </div>
      </template>

      <el-form :inline="true" :model="searchForm" class="search-bar">
        <el-form-item label="审核状态">
          <el-select v-model="searchForm.auditStatus" placeholder="全部" style="width: 140px" @change="loadList">
            <el-option label="全部" :value="null" />
            <el-option label="待审核" :value="0" />
            <el-option label="已通过" :value="1" />
            <el-option label="已驳回" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="物品类型">
          <el-select v-model="searchForm.type" placeholder="全部" style="width: 120px" @change="loadList">
            <el-option label="全部" :value="null" />
            <el-option label="寻物" :value="0" />
            <el-option label="拾物" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-input v-model="searchForm.keyword" placeholder="搜索标题或描述" style="width: 240px" clearable @keyup.enter="loadList" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadList">
            <el-icon><Search /></el-icon> 查询
          </el-button>
        </el-form-item>
      </el-form>

      <el-table :data="items" style="width: 100%" border stripe>
        <el-table-column label="类型" width="80">
          <template #default="{ row }">
            <el-tag :type="row.type === 0 ? 'warning' : 'success'" size="small">
              {{ row.type === 0 ? '寻物' : '拾物' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="description" label="描述" min-width="250" show-overflow-tooltip />
        <el-table-column prop="userId" label="发布者ID" width="100" align="center" />
        <el-table-column label="审核状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.auditStatus === 0" type="warning" size="small">待审核</el-tag>
            <el-tag v-else-if="row.auditStatus === 1" type="success" size="small">已通过</el-tag>
            <el-tag v-else type="danger" size="small">已驳回</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="物品状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.status === 0" size="small">待认领</el-tag>
            <el-tag v-else-if="row.status === 1" type="success" size="small">已认领</el-tag>
            <el-tag v-else type="info" size="small">已下架</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="发布时间" width="180" align="center" />
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="viewDetail(row)">详情</el-button>
            <el-button
              v-if="row.auditStatus === 0"
              link
              type="success"
              size="small"
              @click="handleApprove(row)"
            >通过</el-button>
            <el-button
              v-if="row.auditStatus === 0"
              link
              type="danger"
              size="small"
              @click="handleReject(row)"
            >驳回</el-button>
            <el-button link type="info" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="current"
          v-model:page-size="size"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="loadList"
          @current-change="loadList"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { getAdminItemPage, auditItem, deleteItem } from '@/api/item'

const router = useRouter()
const items = ref([])
const current = ref(1)
const size = ref(10)
const total = ref(0)

const searchForm = reactive({
  type: null,
  auditStatus: null,
  keyword: ''
})

function loadList() {
  getAdminItemPage({
    current: current.value,
    size: size.value,
    type: searchForm.type,
    auditStatus: searchForm.auditStatus,
    keyword: searchForm.keyword
  }).then((data) => {
    items.value = data.records || []
    total.value = data.total || 0
  })
}

function viewDetail(row) {
  router.push(`/items/${row.id}`)
}

function handleApprove(row) {
  ElMessageBox.confirm('确认审核通过此物品信息吗？', '审核确认', {
    type: 'success'
  }).then(() => {
    auditItem(row.id, 1, null).then(() => {
      ElMessage.success('审核通过')
      loadList()
    })
  }).catch(() => {})
}

function handleReject(row) {
  ElMessageBox.prompt('请输入驳回原因', '驳回确认', {
    confirmButtonText: '确认驳回',
    cancelButtonText: '取消',
    inputPlaceholder: '请输入驳回原因...',
    inputValidator: (value) => {
      if (!value || !value.trim()) return '驳回原因不能为空'
      return true
    },
    type: 'warning'
  }).then(({ value }) => {
    auditItem(row.id, 2, value).then(() => {
      ElMessage.success('已驳回')
      loadList()
    })
  }).catch(() => {})
}

function handleDelete(row) {
  ElMessageBox.confirm('确认删除此物品信息吗？删除后不可恢复。', '确认删除', {
    type: 'error'
  }).then(() => {
    deleteItem(row.id).then(() => {
      ElMessage.success('删除成功')
      loadList()
    })
  }).catch(() => {})
}

onMounted(() => {
  loadList()
})
</script>

<style scoped>
.item-audit {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}
.card-header h3 {
  margin: 0;
  color: #303133;
}
.search-bar {
  margin-bottom: 20px;
}
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>
