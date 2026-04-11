<template>
  <view class="container">
    <view v-if="loading" class="loading">加载中...</view>
    <view v-else-if="orders.length === 0" class="empty">
      <text class="empty-icon">📋</text>
      <text class="empty-text">暂无订单</text>
      <text class="empty-hint">去租一辆车吧</text>
    </view>
    <view v-else class="order-list">
      <view v-for="order in orders" :key="order.id" class="order-card" @click="goDetail(order.id)">
        <view class="order-header">
          <text class="order-no">订单号: {{ order.orderNo }}</text>
          <text class="order-status" :class="'status-' + order.status">{{ order.statusText }}</text>
        </view>
        <view class="order-body">
          <text class="vehicle-name">{{ order.vehicleName }}</text>
          <text class="order-amount">¥{{ order.totalAmount }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useAuthGuard } from '../../utils/auth-guard.js'

onShow(() => {
  useAuthGuard()
})

const orders = ref([])
const loading = ref(true)

onMounted(async () => {
  try {
    // Mock data for development
    await new Promise(resolve => setTimeout(resolve, 500))
    orders.value = []
  } catch (err) {
    console.error('加载订单列表失败', err)
  } finally {
    loading.value = false
  }
})

function goDetail(id) {
  uni.navigateTo({ url: `/pages/order-detail/order-detail?id=${id}` })
}
</script>

<style scoped>
.container {
  padding: 20rpx;
  min-height: 100vh;
  background: #f5f5f5;
}

.loading {
  text-align: center;
  padding: 40rpx;
  color: #999;
}

.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 200rpx 0;
}

.empty-icon {
  font-size: 120rpx;
  margin-bottom: 20rpx;
}

.empty-text {
  font-size: 32rpx;
  color: #333;
  margin-bottom: 10rpx;
}

.empty-hint {
  font-size: 26rpx;
  color: #999;
}

.order-card {
  background: #fff;
  border-radius: 16rpx;
  margin-bottom: 20rpx;
  padding: 24rpx;
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
}

.order-no {
  font-size: 24rpx;
  color: #999;
}

.order-status {
  font-size: 24rpx;
  color: #e4393c;
}

.order-body {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.vehicle-name {
  font-size: 30rpx;
  font-weight: 500;
  color: #333;
}

.order-amount {
  font-size: 32rpx;
  color: #e4393c;
  font-weight: 600;
}
</style>
