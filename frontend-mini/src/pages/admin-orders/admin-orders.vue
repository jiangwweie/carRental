<template>
  <view class="container">
    <!-- Status Tabs -->
    <scroll-view scroll-x class="tab-bar">
      <view
        v-for="tab in tabs"
        :key="tab.value"
        :class="['tab-item', { active: activeTab === tab.value }]"
        @click="onTabChange(tab.value)"
      >
        <text>{{ tab.label }}</text>
      </view>
    </scroll-view>

    <!-- Order List -->
    <view class="order-list">
      <view v-if="orders.length === 0" class="empty-state">
        <text class="empty-text">暂无订单</text>
      </view>
      <view v-for="order in orders" :key="order.id" class="order-card">
        <view class="order-header">
          <text class="order-id">订单 #{{ order.id }}</text>
          <text :class="['status-badge', `status-badge--${order.status}`]">
            {{ statusMap[order.status] || order.status }}
          </text>
        </view>
        <view class="order-body">
          <text class="vehicle-name">{{ order.vehicleName || '未知车辆' }}</text>
          <text class="order-dates">{{ formatDate(order.startDate) }} ~ {{ formatDate(order.endDate) }}</text>
          <text class="order-amount">¥{{ order.totalPrice || 0 }}</text>
          <text class="customer-info">客户: {{ order.customerPhone || '未知' }}</text>
        </view>
        <view v-if="order.status === 'pending'" class="order-actions">
          <view class="btn-reject" @click="onReject(order)">
            <text>拒绝</text>
          </view>
          <view class="btn-confirm" @click="onConfirm(order)">
            <text>确认</text>
          </view>
        </view>
      </view>
    </view>

    <!-- Loading indicator -->
    <view v-if="loading" class="loading-overlay">
      <text>加载中...</text>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { getAdminOrders, confirmOrder, rejectOrder } from '../../api/admin.js'

const tabs = [
  { label: '全部', value: '' },
  { label: '待确认', value: 'pending' },
  { label: '已确认', value: 'confirmed' },
  { label: '进行中', value: 'active' },
  { label: '已完成', value: 'completed' },
  { label: '已取消', value: 'cancelled' },
  { label: '已拒绝', value: 'rejected' }
]

const statusMap = {
  pending: '待确认',
  confirmed: '已确认',
  active: '进行中',
  completed: '已完成',
  cancelled: '已取消',
  rejected: '已拒绝'
}

const activeTab = ref('')
const orders = ref([])
const loading = ref(false)

onLoad(() => {
  loadOrders()
})

onShow(() => {
  if (orders.value.length > 0) {
    loadOrders()
  }
})

function onTabChange(status) {
  activeTab.value = status
  loadOrders()
}

async function loadOrders() {
  loading.value = true
  try {
    const params = activeTab.value ? { status: activeTab.value } : {}
    const data = await getAdminOrders(params)
    orders.value = data?.items || data || []
  } catch (err) {
    console.error('加载订单失败:', err)
    uni.showToast({ title: '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

async function onConfirm(order) {
  uni.showModal({
    title: '确认订单',
    content: `确认订单 #${order.id} 吗？`,
    success: async (res) => {
      if (res.confirm) {
        try {
          await confirmOrder(order.id)
          uni.showToast({ title: '已确认', icon: 'success' })
          loadOrders()
        } catch (err) {
          uni.showToast({ title: '确认失败', icon: 'none' })
        }
      }
    }
  })
}

async function onReject(order) {
  uni.showModal({
    title: '拒绝订单',
    content: '请输入拒绝原因',
    editable: true,
    placeholderText: '请输入拒绝原因',
    success: async (res) => {
      if (res.confirm) {
        const reason = res.content || '暂无可用车辆'
        try {
          await rejectOrder(order.id, reason)
          uni.showToast({ title: '已拒绝', icon: 'success' })
          loadOrders()
        } catch (err) {
          uni.showToast({ title: '拒绝失败', icon: 'none' })
        }
      }
    }
  })
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getMonth() + 1}/${d.getDate()}`
}
</script>

<style scoped>
.container {
  min-height: 100vh;
  background: #f5f5f5;
}

.tab-bar {
  white-space: nowrap;
  background: #fff;
  border-bottom: 1rpx solid #eee;
  padding: 0 10rpx;
}

.tab-item {
  display: inline-block;
  padding: 24rpx 20rpx;
  font-size: 26rpx;
  color: #666;
  position: relative;
}

.tab-item.active {
  color: #07c160;
  font-weight: 600;
}

.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 20rpx;
  right: 20rpx;
  height: 4rpx;
  background: #07c160;
  border-radius: 2rpx;
}

.order-list {
  padding: 20rpx;
}

.empty-state {
  padding: 200rpx 0;
  text-align: center;
}

.empty-text {
  font-size: 28rpx;
  color: #999;
}

.order-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 16rpx;
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
}

.order-id {
  font-size: 26rpx;
  color: #999;
}

.status-badge {
  font-size: 24rpx;
  padding: 6rpx 16rpx;
  border-radius: 8rpx;
}

.status-badge--pending {
  color: #ff6b35;
  background: #fff3ed;
}

.status-badge--confirmed {
  color: #1989fa;
  background: #e8f4ff;
}

.status-badge--active {
  color: #07c160;
  background: #e8f8ef;
}

.status-badge--completed {
  color: #666;
  background: #f5f5f5;
}

.status-badge--cancelled {
  color: #999;
  background: #f5f5f5;
  text-decoration: line-through;
}

.status-badge--rejected {
  color: #e64340;
  background: #fef0f0;
}

.order-body {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.vehicle-name {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
}

.order-dates {
  font-size: 24rpx;
  color: #999;
}

.order-amount {
  font-size: 32rpx;
  font-weight: 700;
  color: #ff6b35;
}

.customer-info {
  font-size: 24rpx;
  color: #666;
}

.order-actions {
  display: flex;
  gap: 20rpx;
  margin-top: 20rpx;
  padding-top: 20rpx;
  border-top: 1rpx solid #f0f0f0;
}

.btn-reject, .btn-confirm {
  flex: 1;
  height: 72rpx;
  border-radius: 12rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28rpx;
}

.btn-reject {
  background: #fff;
  border: 1rpx solid #e64340;
  color: #e64340;
}

.btn-confirm {
  background: #07c160;
  color: #fff;
}

.loading-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255,255,255,0.8);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28rpx;
  color: #666;
  z-index: 999;
}
</style>
