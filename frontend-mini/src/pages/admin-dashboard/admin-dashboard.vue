<template>
  <view class="container">
    <!-- Stats Cards -->
    <view class="stats-section">
      <view class="stat-card stat-card--primary">
        <text class="stat-value">{{ stats.pendingOrders }}</text>
        <text class="stat-label">待处理订单</text>
      </view>
      <view class="stat-card stat-card--success">
        <text class="stat-value">¥{{ stats.todayRevenue }}</text>
        <text class="stat-label">今日营收</text>
      </view>
      <view class="stat-card stat-card--info">
        <text class="stat-value">{{ stats.activeRentals }}</text>
        <text class="stat-label">进行中订单</text>
      </view>
    </view>

    <!-- Quick Actions -->
    <view class="actions-section">
      <text class="section-title">快捷操作</text>
      <view class="action-item" @click="navigateTo('/pages/admin-orders/admin-orders')">
        <text class="action-icon">📋</text>
        <text class="action-text">处理订单</text>
        <text class="action-arrow">></text>
      </view>
      <view class="action-item" @click="goBack">
        <text class="action-icon">🏠</text>
        <text class="action-text">返回首页</text>
        <text class="action-arrow">></text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getDashboardOverview } from '../../api/admin.js'

const stats = ref({
  pendingOrders: 0,
  todayRevenue: 0,
  activeRentals: 0
})

onLoad(async () => {
  try {
    const data = await getDashboardOverview()
    stats.value = {
      pendingOrders: data.pendingOrders || 0,
      todayRevenue: data.todayRevenue || 0,
      activeRentals: data.activeRentals || 0
    }
  } catch (err) {
    console.error('获取管理数据失败:', err)
    uni.showToast({ title: '无权限或网络异常', icon: 'none' })
    setTimeout(() => {
      uni.navigateBack()
    }, 1500)
  }
})

function navigateTo(url) {
  uni.navigateTo({ url })
}

function goBack() {
  uni.switchTab({ url: '/pages/index/index' })
}
</script>

<style scoped>
.container {
  min-height: 100vh;
  background: #f5f5f5;
  padding: 20rpx;
}

.stats-section {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
  margin-bottom: 30rpx;
}

.stat-card {
  padding: 40rpx 30rpx;
  border-radius: 16rpx;
}

.stat-card--primary {
  background: linear-gradient(135deg, #ff6b35 0%, #ff8c5a 100%);
}

.stat-card--success {
  background: linear-gradient(135deg, #07c160 0%, #06ad56 100%);
}

.stat-card--info {
  background: linear-gradient(135deg, #1989fa 0%, #3ba0ff 100%);
}

.stat-value {
  font-size: 56rpx;
  font-weight: 700;
  color: #fff;
  display: block;
}

.stat-label {
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.8);
  display: block;
  margin-top: 8rpx;
}

.actions-section {
  background: #fff;
  border-radius: 16rpx;
  overflow: hidden;
}

.section-title {
  padding: 24rpx 30rpx;
  font-size: 28rpx;
  font-weight: 600;
  color: #333;
  border-bottom: 1rpx solid #f0f0f0;
  display: block;
}

.action-item {
  display: flex;
  align-items: center;
  padding: 30rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.action-item:last-child {
  border-bottom: none;
}

.action-icon {
  font-size: 40rpx;
  margin-right: 20rpx;
  width: 50rpx;
  text-align: center;
}

.action-text {
  flex: 1;
  font-size: 30rpx;
  color: #333;
}

.action-arrow {
  font-size: 28rpx;
  color: #ccc;
}
</style>
