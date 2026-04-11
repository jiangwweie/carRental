import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { request } from '../api/request.js'

export const useUserStore = defineStore('user', () => {
  const token = ref(uni.getStorageSync('token') || '')
  const userInfo = ref(uni.getStorageSync('userInfo') || null)

  const isLoggedIn = ref(!!token.value)

  /**
   * Check if a valid token exists in storage.
   * Returns true if logged in, false otherwise.
   */
  function checkLoginStatus() {
    const savedToken = uni.getStorageSync('token') || ''
    token.value = savedToken
    isLoggedIn.value = !!savedToken
    return !!savedToken
  }

  /**
   * Mock login for development/testing.
   * Pure local simulation, no network requests.
   */
  async function mockLogin(data = {}) {
    const phone = data.phone || '13800138000'
    const code = data.code || '123456'

    const fakeToken = 'mock_token_' + Date.now()
    const fakeUserInfo = {
      nickName: '测试用户',
      phone: phone,
      avatarUrl: '',
      role: 'user'
    }

    token.value = fakeToken
    userInfo.value = fakeUserInfo
    isLoggedIn.value = true

    uni.setStorageSync('token', fakeToken)
    uni.setStorageSync('userInfo', JSON.stringify(fakeUserInfo))

    return { token: fakeToken, userInfo: fakeUserInfo }
  }

  async function login(phoneCode) {
    // Check environment variable for mock login mode
    const useMockLogin = import.meta.env.VITE_USE_MOCK_LOGIN === 'true'

    if (useMockLogin) {
      return await mockLogin({ phone: '13800138000', code: '123456' })
    }

    // Real WeChat login flow, with fallback to mock
    try {
      const loginRes = await uni.login()

      const res = await request({
        url: '/api/v1/auth/wx-login',
        method: 'POST',
        data: {
          loginCode: loginRes.code,
          phoneCode: phoneCode
        }
      })

      token.value = res.token
      userInfo.value = res.user
      isLoggedIn.value = true

      uni.setStorageSync('token', res.token)
      uni.setStorageSync('userInfo', res.user)

      return res
    } catch (err) {
      console.warn('微信登录失败，降级到本地 Mock:', err)
      return await mockLogin({ phone: '13800138000' })
    }
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    isLoggedIn.value = false
    uni.removeStorageSync('token')
    uni.removeStorageSync('userInfo')
  }

  const userRole = computed(() => userInfo.value?.role || 'user')

  return { token, userInfo, isLoggedIn, userRole, checkLoginStatus, mockLogin, login, logout }
})
