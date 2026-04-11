<template>
  <view class="container">
    <!-- User Info Card - 已登录 -->
    <view v-if="isLoggedIn" class="user-card">
      <image
        v-if="userInfo.avatarUrl"
        class="avatar"
        :src="userInfo.avatarUrl"
        mode="aspectFill"
      />
      <view v-else class="avatar-wrapper">
        <text class="avatar-text">👤</text>
      </view>
      <view class="user-info">
        <text class="nickname">{{ userInfo.nickName || '租车用户' }}</text>
        <text class="phone">{{ maskPhone(userInfo.phone) }}</text>
      </view>
    </view>

    <!-- User Info Card - 未登录 -->
    <view v-else class="user-card user-card--not-login" @click="goToLogin">
      <view class="avatar-wrapper">
        <text class="avatar-text">👤</text>
      </view>
      <view class="user-info">
        <text class="nickname">点击登录</text>
        <text class="phone">登录后享受更多服务</text>
      </view>
    </view>

    <!-- Menu List -->
    <view class="menu-section">
      <view class="menu-item" @click="navigateTo('/pages/orders/orders')">
        <text class="menu-icon">📋</text>
        <text class="menu-text">我的订单</text>
        <text class="menu-arrow">></text>
      </view>
      <view class="menu-item" @click="navigateTo('/pages/agreement/agreement')">
        <text class="menu-icon">📄</text>
        <text class="menu-text">用户协议</text>
        <text class="menu-arrow">></text>
      </view>
      <view class="menu-item">
        <text class="menu-icon">⚙️</text>
        <text class="menu-text">设置</text>
        <text class="menu-arrow">></text>
      </view>
    </view>

    <!-- 管理端入口 - 仅管理员可见 -->
    <view v-if="userRole === 'admin'" class="menu-section">
      <text class="menu-section-title">管理端</text>
      <view class="menu-item" @click="navigateTo('/pages/admin-dashboard/admin-dashboard')">
        <text class="menu-icon">🛠️</text>
        <text class="menu-text">管理端</text>
        <text class="menu-arrow">></text>
      </view>
    </view>

    <!-- 退出登录 - 已登录时显示 -->
    <view v-if="isLoggedIn" class="logout-section">
      <view class="logout-btn" @click="handleLogout">
        <text class="logout-text">退出登录</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '../../store/user.js'
import { useAuthGuard } from '../../utils/auth-guard.js'

const userStore = useUserStore()

const userInfo = computed(() => userStore.userInfo || {})
const isLoggedIn = computed(() => userStore.isLoggedIn)
const userRole = computed(() => userStore.userRole)

onShow(() => {
  useAuthGuard()
})

function navigateTo(url) {
  uni.navigateTo({ url })
}

function goToLogin() {
  uni.navigateTo({ url: '/pages/login/login' })
}

function maskPhone(phone) {
  if (!phone || phone.length < 7) return phone || ''
  return phone.slice(0, 3) + '****' + phone.slice(-4)
}

function handleLogout() {
  uni.showModal({
    title: '提示',
    content: '确定要退出登录吗？',
    success: (res) => {
      if (res.confirm) {
        userStore.logout()
        uni.switchTab({ url: '/pages/index/index' })
      }
    }
  })
}
</script>

<style scoped>
.container {
  min-height: 100vh;
  background: #f5f5f5;
}

.user-card {
  display: flex;
  align-items: center;
  padding: 60rpx 30rpx 40rpx;
  background: linear-gradient(135deg, #07c160 0%, #06ad56 100%);
  color: #fff;
}

.user-card--not-login {
  cursor: pointer;
}

.avatar {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  margin-right: 24rpx;
}

.avatar-wrapper {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.3);
  margin-right: 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-text {
  font-size: 60rpx;
}

.user-info {
  display: flex;
  flex-direction: column;
}

.nickname {
  font-size: 36rpx;
  font-weight: 600;
  margin-bottom: 8rpx;
}

.phone {
  font-size: 26rpx;
  opacity: 0.8;
}

.menu-section {
  margin-top: 20rpx;
  background: #fff;
}

.menu-section-title {
  padding: 16rpx 30rpx 8rpx;
  font-size: 24rpx;
  color: #999;
}

.menu-item {
  display: flex;
  align-items: center;
  padding: 30rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.menu-item:last-child {
  border-bottom: none;
}

.menu-icon {
  font-size: 40rpx;
  margin-right: 20rpx;
  width: 50rpx;
  text-align: center;
}

.menu-text {
  flex: 1;
  font-size: 30rpx;
  color: #333;
}

.menu-arrow {
  font-size: 28rpx;
  color: #ccc;
}

.logout-section {
  margin-top: 40rpx;
  padding: 0 30rpx;
}

.logout-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 88rpx;
  background: #fff;
  border-radius: 12rpx;
}

.logout-text {
  font-size: 30rpx;
  color: #e64340;
}
</style>
