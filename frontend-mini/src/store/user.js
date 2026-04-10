import { defineStore } from 'pinia'
import { ref } from 'vue'
import { request } from '../api/request.js'

export const useUserStore = defineStore('user', () => {
  const token = ref(uni.getStorageSync('token') || '')
  const userInfo = ref(uni.getStorageSync('userInfo') || null)

  const isLoggedIn = ref(!!token.value)

  async function login(phoneCode) {
    // 先获取 loginCode
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
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    isLoggedIn.value = false
    uni.removeStorageSync('token')
    uni.removeStorageSync('userInfo')
  }

  return { token, userInfo, isLoggedIn, login, logout }
})
