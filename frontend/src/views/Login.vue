<template>
  <div class="login-container">
    <!-- 背景图片层（动态检测：/images/login-bg.jpg 存在则显示，否则默认渐变） -->
    <div class="login-bg" :class="{ 'has-image': bgImageExists }"></div>
    <div class="login-bg-overlay"></div>

    <div class="login-box">
      <div class="login-header">
        <el-icon :size="40" color="#409EFF">
          <Document />
        </el-icon>
        <h1>校园失物招领系统</h1>
        <p>{{ activeTab === 'login' ? '欢迎登录' : '注册新账号' }}</p>
      </div>

      <el-tabs v-model="activeTab" class="auth-tabs">
        <el-tab-pane label="登 录" name="login">
          <el-form :model="loginForm" :rules="loginRules" ref="loginFormRef" label-width="0" @submit.prevent="handleLogin">
            <el-form-item prop="username">
              <el-input v-model="loginForm.username" placeholder="用户名" prefix-icon="User" size="large" :disabled="loading" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="loginForm.password" placeholder="密码" prefix-icon="Lock" size="large" type="password" show-password :disabled="loading" />
            </el-form-item>
            <!-- 验证码 -->
            <el-form-item prop="captcha">
              <div class="captcha-row">
                <el-input v-model="loginForm.captcha" placeholder="验证码" prefix-icon="Key" size="large" :disabled="loading" style="flex: 1" />
                <img :src="captchaUrl" class="captcha-img" @click="refreshCaptcha" title="点击刷新验证码" />
              </div>
            </el-form-item>
            <el-button type="primary" size="large" class="login-btn" :loading="loading" @click="handleLogin">登 录</el-button>
          </el-form>
          <div class="login-tips">
            <p>测试账号: admin / 123456 (超级管理员)</p>
            <p>测试账号: manager / 123456 (管理员)</p>
            <p class="captcha-tip">提示：点击图片可以刷新验证码</p>
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
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { register as apiRegister } from '@/api/auth'
import { generateCaptcha } from '@/utils/captcha'
import { encryptPassword, encryptFormData } from '@/utils/encrypt'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('login')
const loginFormRef = ref()
const registerFormRef = ref()
const loading = ref(false)

// ========= 背景图片动态检测 =========
// 检测 /images/login-bg.jpg 是否存在，存在则显示，否则使用默认渐变
// 用法：将图片放到 frontend/public/images/login-bg.jpg 或
//      构建后放到后端 src/main/resources/static/images/login-bg.jpg
//      图片文件名必须是 login-bg.jpg，放到 images 目录即可自动生效
const bgImageExists = ref(false)

function checkBgImage() {
  const img = new Image()
  img.onload = () => { bgImageExists.value = true }
  img.onerror = () => { bgImageExists.value = false }
  img.src = '/images/login-bg.jpg?t=' + Date.now()
}

// ========= 验证码 =========
const captchaCode = ref('')
const captchaUrl = ref('')

function refreshCaptcha() {
  const { code, dataUrl } = generateCaptcha()
  captchaCode.value = code
  captchaUrl.value = dataUrl
}

onMounted(() => {
  refreshCaptcha()
  checkBgImage()
})

// ========= 登录 =========
const loginForm = ref({
  username: '',
  password: '',
  captcha: ''
})
const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captcha: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

async function handleLogin() {
  // 校验验证码（不区分大小写）
  if (!loginForm.value.captcha) {
    ElMessage.warning('请输入验证码')
    return
  }
  if (loginForm.value.captcha.toUpperCase() !== captchaCode.value.toUpperCase()) {
    ElMessage.warning('验证码错误')
    refreshCaptcha()
    return
  }
  if (!loginForm.value.username || !loginForm.value.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    // 密码加密后提交
    const encryptedPassword = encryptPassword(loginForm.value.password)
    await userStore.login(loginForm.value.username, encryptedPassword)
    ElMessage.success('登录成功')
    router.push('/')
  } catch (err) {
    console.error('登录失败:', err)
    refreshCaptcha()
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
      // 密码加密后提交
      const encrypted = encryptFormData(registerForm.value)
      await apiRegister(encrypted)
      ElMessage.success('注册成功，请登录')
      activeTab.value = 'login'
      loginForm.value.username = registerForm.value.username
      loginForm.value.password = ''
      loginForm.value.captcha = ''
      refreshCaptcha()
    } catch (err) {
      console.error('注册失败:', err)
    } finally {
      loading.value = false
    }
  })
}

function handleTabChange() {
  loading.value = false
  refreshCaptcha()
}
</script>

<style scoped>
.login-container {
  position: relative;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

/* 背景图片层（运行时动态检测 login-bg.jpg）
   用法：将图片放到 frontend/public/images/login-bg.jpg 或
        构建后放到后端 static/images/login-bg.jpg
   若图片不存在，自动使用紫色渐变作为默认背景 */
.login-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  /* 默认渐变背景（图片不存在时显示这个） */
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
}
/* 图片存在时，通过这个 class 覆盖背景 */
.login-bg.has-image {
  background-image: url('/images/login-bg.jpg');
}

.login-bg-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.15);
}

.login-box {
  position: relative;
  z-index: 10;
  width: 460px;
  max-width: calc(100% - 40px);
  padding: 40px;
  background: rgba(255, 255, 255, 0.96);
  border-radius: 12px;
  box-shadow: 0 15px 50px rgba(0, 0, 0, 0.2);
  box-sizing: border-box;
}

.login-header {
  text-align: center;
  margin-bottom: 20px;
}

.login-header h1 {
  font-size: 22px;
  color: #303133;
  margin: 15px 0 8px;
  word-break: break-word;
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

.login-tips .captcha-tip {
  color: #909399;
  font-size: 11px;
  margin-top: 8px;
}

/* 验证码图片 */
.captcha-row {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
}

.captcha-img {
  width: 120px;
  height: 40px;
  cursor: pointer;
  border-radius: 6px;
  border: 1px solid #dcdfe6;
  transition: opacity 0.2s;
  flex-shrink: 0;
}

.captcha-img:hover {
  opacity: 0.8;
}

:deep(.el-tab-pane) {
  padding-top: 10px;
}
</style>
