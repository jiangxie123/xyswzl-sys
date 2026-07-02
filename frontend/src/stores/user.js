import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as apiLogin } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || 'null'))

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
