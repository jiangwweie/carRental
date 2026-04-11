<template>
  <view class="login-container">
    <!-- Logo / Title -->
    <view class="logo-section">
      <view class="logo-icon">🚗</view>
      <view class="logo-title">租车服务</view>
      <view class="logo-subtitle">便捷出行，从这里开始</view>
    </view>

    <!-- Login Form -->
    <view class="form-section">
      <!-- Phone Input -->
      <view class="input-item">
        <text class="input-icon">📱</text>
        <input
          class="input-field"
          type="number"
          maxlength="11"
          placeholder="请输入手机号"
          v-model="phone"
        />
      </view>

      <!-- Verification Code Input -->
      <view class="input-item">
        <text class="input-icon">🔑</text>
        <input
          class="input-field"
          type="number"
          maxlength="6"
          placeholder="请输入验证码"
          v-model="code"
        />
      </view>

      <!-- Login Button -->
      <button
        class="login-btn"
        :class="{ 'login-btn--disabled': !agreed }"
        :disabled="loading || !agreed"
        @click="onLogin"
      >
        <text v-if="loading">登录中...</text>
        <text v-else>登录</text>
      </button>

      <!-- Agreement -->
      <view class="agreement">
        <checkbox-group @change="onAgreeChange">
          <label class="agreement-label">
            <checkbox :checked="agreed" color="#07c160" />
            <text class="agreement-text">我已阅读并同意</text>
            <text class="link" @click="goAgreement">《用户协议》</text>
          </label>
        </checkbox-group>
      </view>
    </view>

    <!-- WeChat Login (Production) -->
    <view class="wechat-section">
      <button class="wechat-btn" open-type="getPhoneNumber" @getphonenumber="onWechatLogin">
        使用微信登录
      </button>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useUserStore } from '../../store/user.js'

const agreed = ref(false)
const phone = ref('13800138000')
const code = ref('123456')
const loading = ref(false)
const userStore = useUserStore()

// Redirect URL after login, defaults to home page
const redirectUrl = ref('/pages/index/index')

// Tab pages that require switchTab instead of redirectTo
const TAB_PAGES = [
  '/pages/index/index',
  '/pages/orders/orders',
  '/pages/me/me',
]

onLoad((options) => {
  if (options && options.redirectTo) {
    redirectUrl.value = decodeURIComponent(options.redirectTo)
  }
})

/**
 * Navigate to the target page after successful login.
 * Uses switchTab for tab pages, redirectTo for others.
 */
function navigateAfterLogin(url) {
  if (TAB_PAGES.includes(url)) {
    uni.switchTab({ url })
  } else {
    uni.redirectTo({ url })
  }
}

function onAgreeChange(e) {
  agreed.value = e.detail.value.length > 0
}

async function onLogin() {
  if (!agreed.value) {
    uni.showToast({ title: '请先同意用户协议', icon: 'none' })
    return
  }

  if (!phone.value || !code.value) {
    uni.showToast({ title: '请输入手机号和验证码', icon: 'none' })
    return
  }

  loading.value = true
  try {
    // Use backend mock-login API to get a real JWT token
    // In production, this will be replaced with real WeChat login
    const { mockLogin } = await import('../../api/auth.js')
    const res = await mockLogin({ role: 'user' })

    // Store token and user info from backend response
    uni.setStorageSync('token', res.token)
    uni.setStorageSync('userInfo', JSON.stringify(res.user))
    userStore.checkLoginStatus()

    uni.showToast({ title: '登录成功', icon: 'success' })
    setTimeout(() => {
      navigateAfterLogin(redirectUrl.value)
    }, 500)
  } catch (err) {
    console.error('登录失败', err)
    // Fallback to local mock if backend is unavailable
    try {
      await userStore.mockLogin({
        phone: phone.value,
        code: code.value
      })
      uni.showToast({ title: '后端不可用，已降级到本地 Mock', icon: 'none' })
      setTimeout(() => {
        navigateAfterLogin(redirectUrl.value)
      }, 500)
    } catch (fallbackErr) {
      uni.showToast({ title: '登录失败，请重试', icon: 'none' })
    }
  } finally {
    loading.value = false
  }
}

async function onWechatLogin(e) {
  if (!agreed.value) {
    uni.showToast({ title: '请先同意用户协议', icon: 'none' })
    return
  }

  if (e.detail.code) {
    loading.value = true
    try {
      await userStore.login(e.detail.code)
      uni.showToast({ title: '登录成功', icon: 'success' })
      setTimeout(() => {
        navigateAfterLogin(redirectUrl.value)
      }, 500)
    } catch (err) {
      console.error('微信登录失败', err)
      uni.showToast({ title: '登录失败，请重试', icon: 'none' })
    } finally {
      loading.value = false
    }
  } else {
    uni.showToast({ title: '微信授权失败', icon: 'none' })
  }
}

function goAgreement() {
  uni.navigateTo({ url: '/pages/agreement/agreement' })
}
</script>

<style scoped>
.login-container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  padding: 0 60rpx;
  background-color: #f5f5f5;
}

/* Logo Section */
.logo-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 120rpx;
  padding-bottom: 80rpx;
}

.logo-icon {
  font-size: 80rpx;
  margin-bottom: 20rpx;
}

.logo-title {
  font-size: 48rpx;
  font-weight: bold;
  color: #333;
  margin-bottom: 12rpx;
}

.logo-subtitle {
  font-size: 28rpx;
  color: #999;
}

/* Form Section */
.form-section {
  background-color: #fff;
  border-radius: 24rpx;
  padding: 40rpx 32rpx;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.05);
}

.input-item {
  display: flex;
  align-items: center;
  background-color: #f8f8f8;
  border-radius: 12rpx;
  padding: 0 24rpx;
  margin-bottom: 24rpx;
  height: 88rpx;
}

.input-icon {
  font-size: 36rpx;
  margin-right: 16rpx;
}

.input-field {
  flex: 1;
  font-size: 30rpx;
  color: #333;
}

/* Login Button */
.login-btn {
  width: 100%;
  background-color: #07c160;
  color: #fff;
  border-radius: 12rpx;
  font-size: 32rpx;
  padding: 24rpx 0;
  margin-top: 16rpx;
  border: none;
}

.login-btn--disabled {
  background-color: #ccc !important;
  color: #999 !important;
}

/* Agreement */
.agreement {
  margin-top: 32rpx;
  display: flex;
  justify-content: center;
}

.agreement-label {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  justify-content: center;
}

.agreement-text {
  font-size: 24rpx;
  color: #666;
  margin: 0 4rpx;
}

.link {
  font-size: 24rpx;
  color: #576b95;
}

/* WeChat Login */
.wechat-section {
  margin-top: auto;
  padding: 40rpx 0 60rpx;
  display: flex;
  justify-content: center;
}

.wechat-btn {
  background: none;
  color: #576b95;
  font-size: 26rpx;
  border: none;
  padding: 0;
  line-height: normal;
}

.wechat-btn::after {
  border: none;
}
</style>
