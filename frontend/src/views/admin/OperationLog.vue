<template>
  <div class="log-page">
    <el-card shadow="never">
      <template #header>
        <h3>操作日志</h3>
      </template>

      <el-form :inline="true" :model="searchForm" class="search-bar">
        <el-form-item label="操作类型">
          <el-select v-model="searchForm.operationType" placeholder="全部" style="width: 130px" clearable @change="loadLogs">
            <el-option label="新增" value="CREATE" />
            <el-option label="修改" value="UPDATE" />
            <el-option label="删除" value="DELETE" />
            <el-option label="审核" value="AUDIT" />
            <el-option label="登录" value="LOGIN" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作模块">
          <el-select v-model="searchForm.operationModule" placeholder="全部" style="width: 130px" clearable @change="loadLogs">
            <el-option label="用户" value="USER" />
            <el-option label="物品" value="ITEM" />
            <el-option label="分类" value="CATEGORY" />
            <el-option label="留言" value="COMMENT" />
            <el-option label="系统" value="SYSTEM" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            @change="handleDateChange"
          />
        </el-form-item>
        <el-form-item>
          <el-input v-model="searchForm.keyword" placeholder="搜索操作描述" style="width: 200px" clearable @keyup.enter="loadLogs" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadLogs">
            <el-icon><Search /></el-icon> 查询
          </el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="logs" style="width: 100%" border stripe>
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="adminName" label="操作人" width="120" align="center" />
        <el-table-column prop="operationType" label="操作类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="typeTagColor(row.operationType)" size="small">
              {{ typeText(row.operationType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operationModule" label="模块" width="100" align="center">
          <template #default="{ row }">
            {{ moduleText(row.operationModule) }}
          </template>
        </el-table-column>
        <el-table-column prop="operationDesc" label="操作描述" min-width="250" show-overflow-tooltip />
        <el-table-column prop="targetId" label="目标ID" width="90" align="center" />
        <el-table-column label="结果" width="80" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.result === 1" type="success" size="small">成功</el-tag>
            <el-tag v-else type="danger" size="small">失败</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="操作时间" width="180" align="center" />
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="current"
          v-model:page-size="size"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="loadLogs"
          @current-change="loadLogs"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { getAdminLogs } from '@/api/log'

const logs = ref([])
const current = ref(1)
const size = ref(10)
const total = ref(0)
const dateRange = ref([])

const searchForm = reactive({
  operationType: '',
  operationModule: '',
  keyword: ''
})

function typeText(type) {
  const map = { CREATE: '新增', UPDATE: '修改', DELETE: '删除', AUDIT: '审核', LOGIN: '登录' }
  return map[type] || type
}

function typeTagColor(type) {
  const map = { CREATE: 'success', UPDATE: 'warning', DELETE: 'danger', AUDIT: 'info', LOGIN: '' }
  return map[type] || ''
}

function moduleText(module) {
  const map = { USER: '用户', ITEM: '物品', CATEGORY: '分类', COMMENT: '留言', SYSTEM: '系统' }
  return map[module] || module
}

function handleDateChange() {
  loadLogs()
}

function loadLogs() {
  const params = {
    current: current.value,
    size: size.value,
    operationType: searchForm.operationType || null,
    operationModule: searchForm.operationModule || null,
    keyword: searchForm.keyword || null,
    startTime: dateRange.value && dateRange.value.length > 0 ? dateRange.value[0] : null,
    endTime: dateRange.value && dateRange.value.length > 1 ? dateRange.value[1] : null
  }
  getAdminLogs(params).then((data) => {
    logs.value = data.records || []
    total.value = data.total || 0
  })
}

function resetSearch() {
  searchForm.operationType = ''
  searchForm.operationModule = ''
  searchForm.keyword = ''
  dateRange.value = []
  current.value = 1
  loadLogs()
}

onMounted(() => {
  loadLogs()
})
</script>

<style scoped>
.log-page {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}
.log-page h3 {
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
