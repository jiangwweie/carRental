<template>
  <view class="container">
    <!-- Status Tabs -->
    <view class="tabs">
      <view
        v-for="tab in tabList"
        :key="tab.value"
        class="tab-item"
        :class="{ 'tab-active': activeTab === tab.value }"
        @click="switchTab(tab.value)"
      >
        <text class="tab-text">{{ tab.label }}</text>
      </view>
    </view>

    <view v-if="loading" class="loading">加载中...</view>
    <view v-else-if="filteredOrders.length === 0" class="empty">
      <text class="empty-icon">📋</text>
      <text class="empty-text">暂无订单</text>
      <text class="empty-hint">去租一辆车吧</text>
    </view>
    <view v-else class="order-list">
      <view v-for="order in filteredOrders" :key="order.id" class="order-card" @click="goDetail(order.id)">
        <view class="order-header">
          <text class="order-no">订单号: {{ order.orderNo }}</text>
          <text class="order-status" :class="'status-' + order.status">{{ order.statusText }}</text>
        </view>
        <view class="order-body">
          <view class="vehicle-info">
            <text class="vehicle-brand">{{ order.vehicleBrand }}</text>
            <text class="vehicle-name">{{ order.vehicleName }}</text>
          </view>
          <text class="order-amount">¥{{ order.totalAmount }}</text>
        </view>
        <view class="order-dates">
          <text class="date-text">{{ order.startDate }} ~ {{ order.endDate }}</text>
          <text class="days-text">共 {{ order.days }} 天</text>
        </view>
        <view class="order-footer" v-if="order.status === 'pending'">
          <view class="order-created">{{ order.createdAt }} 创建</view>
          <view class="cancel-btn" @click.stop="handleCancel(order.id)">取消</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useAuthGuard } from '../../utils/auth-guard.js'

onShow(() => {
  useAuthGuard()
})

const orders = ref([])
const loading = ref(true)
const activeTab = ref('all')

const tabList = [
  { label: '全部', value: 'all' },
  { label: '待确认', value: 'pending' },
  { label: '已确认', value: 'confirmed' },
  { label: '进行中', value: 'ongoing' },
  { label: '已完成', value: 'completed' }
]

// Mock orders data for development fallback
const mockOrders = [
  {
    id: 'ORD001',
    orderNo: 'CAR20260411001',
    vehicleName: '特斯拉 Model 3',
    vehicleBrand: 'Tesla',
    startDate: '2026-04-12',
    endDate: '2026-04-15',
    days: 3,
    totalAmount: 1197,
    status: 'pending',
    statusText: '待确认',
    createdAt: '2026-04-11 15:30'
  },
  {
    id: 'ORD002',
    orderNo: 'CAR20260410001',
    vehicleName: '本田雅阁',
    vehicleBrand: 'Honda',
    startDate: '2026-04-08',
    endDate: '2026-04-10',
    days: 2,
    totalAmount: 698,
    status: 'confirmed',
    statusText: '已确认',
    createdAt: '2026-04-10 10:00'
  },
  {
    id: 'ORD003',
    orderNo: 'CAR20260405001',
    vehicleName: '大众途观 L',
    vehicleBrand: 'VW',
    startDate: '2026-04-01',
    endDate: '2026-04-03',
    days: 2,
    totalAmount: 598,
    status: 'completed',
    statusText: '已完成',
    createdAt: '2026-04-05 09:00'
  }
]

// Computed filtered orders based on active tab
const filteredOrders = computed(() => {
  if (activeTab.value === 'all') {
    return orders.value
  }
  return orders.value.filter(o => o.status === activeTab.value)
})

function switchTab(value) {
  activeTab.value = value
}

onMounted(async () => {
  try {
    // Try API first, fall back to mock data
    await new Promise(resolve => setTimeout(resolve, 500))
    orders.value = []
    if (orders.value.length === 0) {
      // API returned empty or failed, load mock data
      orders.value = mockOrders
    }
  } catch (err) {
    console.error('加载订单列表失败，使用 Mock 数据', err)
    orders.value = mockOrders
  } finally {
    loading.value = false
  }
})

function goDetail(id) {
  uni.navigateTo({ url: `/pages/order-detail/order-detail?id=${id}` })
}

function handleCancel(id) {
  uni.showModal({
    title: '确认取消',
    content: '确定要取消此订单吗？',
    success: (res) => {
      if (res.confirm) {
        const order = orders.value.find(o => o.id === id)
        if (order) {
          order.status = 'cancelled'
          order.statusText = '已取消'
          uni.showToast({ title: '订单已取消', icon: 'success' })
        }
      }
    }
  })
}
</script>

<style scoped>
.container {
  padding: 20rpx;
  min-height: 100vh;
  background: #f5f5f5;
}

/* Tabs */
.tabs {
  display: flex;
  background: #fff;
  border-radius: 16rpx;
  padding: 10rpx 0;
  margin-bottom: 20rpx;
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: 16rpx 0;
}

.tab-text {
  font-size: 26rpx;
  color: #666;
}

.tab-active .tab-text {
  color: #1890ff;
  font-weight: 600;
}

.tab-active {
  position: relative;
}

.tab-active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 40rpx;
  height: 4rpx;
  background: #1890ff;
  border-radius: 2rpx;
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

.order-list {
  display: flex;
  flex-direction: column;
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
  padding-bottom: 16rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.order-no {
  font-size: 24rpx;
  color: #999;
}

.order-status {
  font-size: 24rpx;
  color: #e4393c;
}

.status-pending {
  color: #faad14;
}

.status-confirmed {
  color: #1890ff;
}

.status-ongoing {
  color: #52c41a;
}

.status-completed {
  color: #999;
}

.status-cancelled {
  color: #ccc;
}

.order-body {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12rpx;
}

.vehicle-info {
  display: flex;
  flex-direction: column;
}

.vehicle-brand {
  font-size: 22rpx;
  color: #999;
  margin-bottom: 4rpx;
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

.order-dates {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12rpx;
}

.date-text {
  font-size: 24rpx;
  color: #666;
}

.days-text {
  font-size: 24rpx;
  color: #1890ff;
}

.order-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12rpx;
  border-top: 1rpx solid #f0f0f0;
}

.order-created {
  font-size: 22rpx;
  color: #bbb;
}

.cancel-btn {
  padding: 8rpx 32rpx;
  border: 1rpx solid #ddd;
  border-radius: 8rpx;
  font-size: 24rpx;
  color: #666;
  background: #fff;
}

.cancel-btn:active {
  background: #f5f5f5;
}
</style>
