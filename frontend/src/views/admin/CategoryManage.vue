<template>
  <el-card>
    <template #header>
      <div class="header">
        <span>分类管理</span>
        <el-button type="primary" @click="handleAdd">新增分类</el-button>
      </div>
    </template>

    <el-table :data="categoryList" border stripe style="width: 100%">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="分类名称" min-width="140" />
      <el-table-column prop="sortOrder" label="排序权重" width="100" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" min-width="160" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" link @click="handleEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" link @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      background
      layout="total, prev, pager, next"
      :total="totalCount"
      :page-size="10"
      :current-page="pageNum"
      @current-change="handlePageChange"
      class="pagination"
    />
  </el-card>

  <!-- 新增/编辑分类对话框 -->
  <el-dialog v-model="dialogVisible" :title="dialogTitle" width="450px" :close-on-click-modal="false">
    <el-form :model="categoryForm" label-width="100px">
      <el-form-item label="分类名称">
        <el-input v-model="categoryForm.name" placeholder="请输入分类名称" />
      </el-form-item>
      <el-form-item label="排序权重">
        <el-input-number v-model="categoryForm.sortOrder" :min="0" :max="9999" style="width: 100%" />
      </el-form-item>
      <el-form-item label="状态">
        <el-radio-group v-model="categoryForm.status">
          <el-radio :value="1">启用</el-radio>
          <el-radio :value="0">禁用</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleSubmit">确定</el-button>
    </template>
  </el-dialog>

  <!-- 删除确认对话框 -->
  <el-dialog v-model="deleteDialogVisible" title="确认删除" width="400px" :close-on-click-modal="false">
    <p>确定删除分类 "{{ deleteCategoryName }}" 吗？此操作不可撤销。</p>
    <template #footer>
      <el-button @click="deleteDialogVisible = false">取消</el-button>
      <el-button type="danger" @click="confirmDelete">确定删除</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getCategoryPage, createCategory, updateCategory, deleteCategory as apiDeleteCategory } from '@/api/category'

const categoryList = ref([])
const totalCount = ref(0)
const pageNum = ref(1)

const dialogVisible = ref(false)
const deleteDialogVisible = ref(false)
const isEdit = ref(false)
const editCategoryId = ref(null)
const deleteCategoryId = ref(null)
const deleteCategoryName = ref('')

const categoryForm = ref({
  name: '',
  sortOrder: 0,
  status: 1
})

const dialogTitle = computed(() => isEdit.value ? '编辑分类' : '新增分类')

async function loadCategories() {
  try {
    const result = await getCategoryPage(pageNum.value, 10)
    categoryList.value = result.records || []
    totalCount.value = result.total || 0
  } catch (error) {
    categoryList.value = []
    totalCount.value = 0
  }
}

function handlePageChange(page) {
  pageNum.value = page
  loadCategories()
}

function handleAdd() {
  isEdit.value = false
  editCategoryId.value = null
  categoryForm.value = {
    name: '',
    sortOrder: 0,
    status: 1
  }
  dialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  editCategoryId.value = row.id
  categoryForm.value = {
    name: row.name,
    sortOrder: row.sortOrder,
    status: row.status
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!categoryForm.value.name || categoryForm.value.name.trim() === '') {
    ElMessage.warning('请输入分类名称')
    return
  }
  try {
    if (isEdit.value) {
      await updateCategory(editCategoryId.value, categoryForm.value)
      ElMessage.success('更新成功')
    } else {
      await createCategory(categoryForm.value)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadCategories()
  } catch (error) {
    // 错误已在请求拦截器处理
  }
}

function handleDelete(row) {
  deleteCategoryId.value = row.id
  deleteCategoryName.value = row.name
  deleteDialogVisible.value = true
}

async function confirmDelete() {
  try {
    await apiDeleteCategory(deleteCategoryId.value)
    ElMessage.success('删除成功')
    deleteDialogVisible.value = false
    loadCategories()
  } catch (error) {
    // 错误已在请求拦截器处理
  }
}

onMounted(() => {
  loadCategories()
})
</script>

<style scoped>
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
