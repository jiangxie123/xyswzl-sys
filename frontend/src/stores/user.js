import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as apiLogin } from '@/api/auth'

/**
 * 安全地从 localStorage 读取 JSON ，并在失败时返回 null 。
 * 防止有人手动污染 localStorage 。
 */
function safeParse (key) {
  try {
    const raw = localStorage.getItem(key)
    if (!raw || raw === 'null' || raw === 'undefined') return null
    return JSON.parse(raw)
  } catch (e) {
    console.warn('[userStore] 读取 ' + key + ' 解析失败: ' + e.message)
    return null
  }
}

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(safeParse('userInfo'))

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => userInfo.value && (userInfo.value.role === 1 || userInfo.value.role === 2))
  const isSuperAdmin = computed(() => userInfo.value && userInfo.value.role === 2)

  async function login(username, password) {
    const result = await apiLogin(username, password)
    token.value = result.token
    userInfo.value = result.user
    localStorage.setItem('token', result.token)
    localStorage.setItem('userInfo', JSON.stringify(result.user))
    return result
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  return { token, userInfo, isLoggedIn, isAdmin, isSuperAdmin, login, logout }
})
