<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-header">
        <el-icon :size="40" color="#409EFF">
          <Document />
        </el-icon>
        <h1>校园失物招领系统</h1>
        <p>{{ activeTab === 'login' ? '欢迎登录' : '注册新账号' }}</p>
      </div>

      <el-tabs v-model="activeTab" class="auth-tabs" @tab-change="handleTabChange">
        <el-tab-pane label="登 录" name="login">
          <el-form :model="loginForm" :rules="loginRules" ref="loginFormRef" label-width="0" @submit.prevent="handleLogin">
            <el-form-item prop="username">
              <el-input v-model="loginForm.username" placeholder="用户名" prefix-icon="User" size="large" :disabled="loading" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="loginForm.password" placeholder="密码" prefix-icon="Lock" size="large" type="password" show-password :disabled="loading" />
            </el-form-item>
            <el-button type="primary" size="large" class="login-btn" :loading="loading" @click="handleLogin">登 录</el-button>
          </el-form>
          <div class="login-tips">
            <p>测试账号: admin / 123456 (超级管理员)</p>
            <p>测试账号: manager / 123456 (管理员)</p>
          </div>
        </el-tab-pane>

        <el-tab-pane label="注 册" name="register">
          <el-form :model="registerForm" :rules="registerRules" ref="registerFormRef" label-width="80px">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="registerForm.username" placeholder="请输入用户名（登录账号）" size="large" :disabled="loading" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input v-model="registerForm.password" placeholder="6位以上" size="large" type="password" show-password :disabled="loading" />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="registerForm.confirmPassword" placeholder="请再次输入密码" size="large" type="password" show-password :disabled="loading" />
            </el-form-item>
            <el-form-item label="真实姓名">
              <el-input v-model="registerForm.realName" placeholder="选填" size="large" :disabled="loading" />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="registerForm.phone" placeholder="选填" size="large" :disabled="loading" />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="registerForm.email" placeholder="选填" size="large" :disabled="loading" />
            </el-form-item>
            <el-form-item label="学号">
              <el-input v-model="registerForm.studentId" placeholder="选填" size="large" :disabled="loading" />
            </el-form-item>
            <el-form-item label="学院">
              <el-input v-model="registerForm.college" placeholder="选填" size="large" :disabled="loading" />
            </el-form-item>
            <el-button type="primary" size="large" class="login-btn" :loading="loading" @click="handleRegister">注 册</el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { register as apiRegister } from '@/api/auth'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('login')
const loginFormRef = ref()
const registerFormRef = ref()
const loading = ref(false)

// ========= 登录 =========
const loginForm = ref({
  username: '',
  password: ''
})
const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  if (!loginForm.value.username || !loginForm.value.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    await userStore.login(loginForm.value.username, loginForm.value.password)
    ElMessage.success('登录成功')
    router.push('/')
  } catch (err) {
    console.error('登录失败:', err)
  } finally {
    loading.value = false
  }
}

// ========= 注册 =========
const registerForm = ref({
  username: '',
  password: '',
  confirmPassword: '',
  realName: '',
  phone: '',
  email: '',
  studentId: '',
  college: ''
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== registerForm.value.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度 3-20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 30, message: '密码长度 6-30 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

async function handleRegister() {
  registerFormRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const data = {
        username: registerForm.value.username,
        password: registerForm.value.password,
        realName: registerForm.value.realName,
        phone: registerForm.value.phone,
        email: registerForm.value.email,
        studentId: registerForm.value.studentId,
        college: registerForm.value.college
      }
      await apiRegister(data)
      ElMessage.success('注册成功，请登录')
      // 自动切换到登录 tab，并填充用户名
      activeTab.value = 'login'
      loginForm.value.username = registerForm.value.username
      loginForm.value.password = ''
    } catch (err) {
      console.error('注册失败:', err)
    } finally {
      loading.value = false
    }
  })
}

function handleTabChange() {
  loading.value = false
}
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-box {
  width: 460px;
  padding: 40px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 15px 50px rgba(0, 0, 0, 0.15);
}

.login-header {
  text-align: center;
  margin-bottom: 20px;
}

.login-header h1 {
  font-size: 22px;
  color: #303133;
  margin: 15px 0 8px;
}

.login-header p {
  color: #909399;
  margin: 0;
}

.auth-tabs {
  margin-top: 20px;
}

.login-btn {
  width: 100%;
  margin-top: 10px;
}

.login-tips {
  margin-top: 20px;
  padding: 15px;
  background: #f0f9ff;
  border-radius: 8px;
  font-size: 12px;
  color: #606266;
}

.login-tips p {
  margin: 5px 0;
}

:deep(.el-tab-pane) {
  padding-top: 10px;
}
</style>
