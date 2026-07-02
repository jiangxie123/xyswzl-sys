<template>
  <el-card>
    <template #header>
      <div class="header">
        <span>用户管理</span>
        <el-button type="primary" @click="handleAdd">新增用户</el-button>
      </div>
    </template>

    <el-table :data="userList" border stripe style="width: 100%">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="username" label="用户名" min-width="110" />
      <el-table-column prop="realName" label="真实姓名" min-width="110" />
      <el-table-column label="角色" width="120">
        <template #default="{ row }">
          <el-tag v-if="row.role === 2" type="danger">超级管理员</el-tag>
          <el-tag v-else-if="row.role === 1" type="warning">管理员</el-tag>
          <el-tag v-else>学生</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '正常' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="phone" label="手机号" min-width="120" />
      <el-table-column prop="college" label="学院/部门" min-width="120" />
      <el-table-column prop="createTime" label="创建时间" min-width="160" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <!-- 管理员仅能编辑学生用户；超级管理员可编辑所有用户 -->
          <el-button
            size="small"
            type="primary"
            link
            :disabled="!userStore.isSuperAdmin && row.role >= 1"
            @click="handleEdit(row)"
          >
            编辑
          </el-button>
          <!-- 删除按钮仅超级管理员可见 -->
          <el-button
            v-if="userStore.isSuperAdmin"
            size="small"
            type="danger"
            link
            @click="handleDelete(row)"
          >
            删除
          </el-button>
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

  <!-- 新增/编辑用户对话框 -->
  <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" :close-on-click-modal="false">
    <el-form :model="userForm" label-width="80px">
      <el-form-item label="用户名">
        <el-input v-model="userForm.username" :disabled="isEdit" placeholder="请输入用户名" />
      </el-form-item>
      <el-form-item v-if="!isEdit" label="密码">
        <el-input v-model="userForm.password" type="password" show-password placeholder="请输入密码" />
      </el-form-item>
      <el-form-item label="真实姓名">
        <el-input v-model="userForm.realName" placeholder="请输入真实姓名" />
      </el-form-item>
      <el-form-item label="角色">
        <el-select v-model="userForm.role" style="width: 100%" :disabled="!userStore.isSuperAdmin">
          <el-option label="学生" :value="0" />
          <el-option
            v-if="userStore.isSuperAdmin"
            label="管理员"
            :value="1"
          />
          <el-option
            v-if="userStore.isSuperAdmin"
            label="超级管理员"
            :value="2"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-radio-group v-model="userForm.status">
          <el-radio :value="1">正常</el-radio>
          <el-radio :value="0">禁用</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="手机号">
        <el-input v-model="userForm.phone" placeholder="请输入手机号" />
      </el-form-item>
      <el-form-item label="邮箱">
        <el-input v-model="userForm.email" placeholder="请输入邮箱" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleSubmit">确定</el-button>
    </template>
  </el-dialog>

  <!-- 删除确认对话框 -->
  <el-dialog v-model="deleteDialogVisible" title="确认删除" width="400px" :close-on-click-modal="false">
    <p>确定删除用户 "{{ deleteUserName }}" 吗？此操作不可撤销。</p>
    <template #footer>
      <el-button @click="deleteDialogVisible = false">取消</el-button>
      <el-button type="danger" @click="confirmDelete">确定删除</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getUserList, addUser, updateUser, deleteUser as apiDeleteUser } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const userList = ref([])
const totalCount = ref(0)
const pageNum = ref(1)

const dialogVisible = ref(false)
const deleteDialogVisible = ref(false)
const isEdit = ref(false)
const editUserId = ref(null)
const deleteUserId = ref(null)
const deleteUserName = ref('')

const userForm = ref({
  username: '',
  password: '',
  realName: '',
  role: 0,
  status: 1,
  phone: '',
  email: ''
})

const dialogTitle = computed(() => isEdit.value ? '编辑用户' : '新增用户')

async function loadUsers() {
  try {
    const result = await getUserList(pageNum.value, 10)
    userList.value = result.records || []
    totalCount.value = result.total || 0
  } catch (error) {
    ElMessage.error('加载用户列表失败')
    userList.value = []
    totalCount.value = 0
  }
}

function handlePageChange(page) {
  pageNum.value = page
  loadUsers()
}

function handleAdd() {
  isEdit.value = false
  editUserId.value = null
  userForm.value = {
    username: '',
    password: '',
    realName: '',
    role: 0,
    status: 1,
    phone: '',
    email: ''
  }
  dialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  editUserId.value = row.id
  userForm.value = {
    username: row.username,
    password: '',
    realName: row.realName,
    role: row.role,
    status: row.status,
    phone: row.phone,
    email: row.email
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!userForm.value.username || userForm.value.username.trim() === '') {
    ElMessage.warning('请输入用户名')
    return
  }
  if (!isEdit.value && (!userForm.value.password || userForm.value.password.trim() === '')) {
    ElMessage.warning('请输入密码')
    return
  }
  if (!userForm.value.realName || userForm.value.realName.trim() === '') {
    ElMessage.warning('请输入真实姓名')
    return
  }

  try {
    if (isEdit.value) {
      await updateUser(editUserId.value, userForm.value)
      ElMessage.success('更新成功')
    } else {
      await addUser(userForm.value)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadUsers()
  } catch (error) {
    // 错误已在请求拦截器处理
  }
}

function handleDelete(row) {
  deleteUserId.value = row.id
  deleteUserName.value = row.username
  deleteDialogVisible.value = true
}

async function confirmDelete() {
  try {
    await apiDeleteUser(deleteUserId.value)
    ElMessage.success('删除成功')
    deleteDialogVisible.value = false
    loadUsers()
  } catch (error) {
    // 错误已在请求拦截器处理
  }
}

onMounted(() => {
  loadUsers()
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
