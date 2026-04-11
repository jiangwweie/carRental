<template>
  <view class="container">
    <!-- 加载状态 -->
    <view v-if="loading" class="loading">加载中...</view>

    <view v-else-if="order">
      <!-- 状态进度条 -->
      <view class="progress-card">
        <view class="progress-bar">
          <view
            v-for="(step, idx) in progressSteps"
            :key="step.key"
            class="step-item"
          >
            <!-- 连接线 -->
            <view
              v-if="idx < progressSteps.length - 1"
              class="step-line"
              :class="{ 'line-active': idx < currentStepIndex }"
            ></view>
            <!-- 步骤点 -->
            <view
              class="step-dot"
              :class="{
                'dot-active': idx <= currentStepIndex,
                'dot-rejected': order.status === 'rejected'
              }"
            >
              <text v-if="order.status === 'rejected' && idx === 0" class="dot-icon">!</text>
            </view>
            <!-- 步骤文字 -->
            <text
              class="step-text"
              :class="{ 'text-active': idx <= currentStepIndex }"
            >{{ step.label }}</text>
          </view>
        </view>
        <!-- 已拒绝红色提示 -->
        <view v-if="order.status === 'rejected'" class="reject-tip">
          <text class="reject-text">您的订单已被拒绝</text>
        </view>
      </view>

      <!-- 订单信息区块 -->
      <view class="info-card">
        <text class="section-title">订单信息</text>
        <view class="info-row">
          <text class="label">订单编号</text>
          <text class="value">{{ order.orderNo }}</text>
        </view>
        <view class="info-row">
          <text class="label">订单状态</text>
          <text class="value status-text" :class="'status-' + order.status">{{ order.statusText }}</text>
        </view>
        <view class="info-row">
          <text class="label">创建时间</text>
          <text class="value">{{ order.createdAt }}</text>
        </view>
      </view>

      <!-- 车辆信息区块 -->
      <view class="vehicle-card">
        <text class="section-title">车辆信息</text>
        <view class="vehicle-header">
          <text class="vehicle-name">{{ order.vehicleName }}</text>
          <text class="vehicle-brand">{{ order.vehicleBrand }}</text>
        </view>
        <view class="info-row">
          <text class="label">日租金</text>
          <text class="value price">¥{{ order.dailyRate }}<text class="unit">/天</text></text>
        </view>
        <view class="info-row">
          <text class="label">取车日期</text>
          <text class="value">{{ order.startDate }}</text>
        </view>
        <view class="info-row">
          <text class="label">还车日期</text>
          <text class="value">{{ order.endDate }}</text>
        </view>
        <view class="info-row">
          <text class="label">租赁天数</text>
          <text class="value">{{ order.days }} 天</text>
        </view>
      </view>

      <!-- 价格明细区块 -->
      <view class="price-card">
        <text class="section-title">价格明细</text>
        <view class="price-detail-row">
          <text class="detail-label">车辆租金</text>
          <text class="detail-value">{{ order.priceBreakdown.days }}天 × ¥{{ order.priceBreakdown.dailyRate }}</text>
        </view>
        <view class="price-detail-row">
          <text class="detail-label">合计金额</text>
          <text class="detail-value">¥{{ order.priceBreakdown.subtotal }}</text>
        </view>
        <view class="price-detail-row" v-if="order.priceBreakdown.discount > 0">
          <text class="detail-label discount-text">优惠折扣</text>
          <text class="detail-value discount-text">-¥{{ order.priceBreakdown.discount }}</text>
        </view>
        <view class="divider"></view>
        <view class="final-row">
          <text class="final-label">应付金额</text>
          <text class="final-value">¥{{ order.priceBreakdown.total }}</text>
        </view>
      </view>

      <!-- 取车地址区块 -->
      <view class="address-card">
        <text class="section-title">取车地址</text>
        <view class="address-row">
          <text class="address-text">{{ order.pickupAddress }}</text>
          <view class="copy-btn" @click="copyAddress">
            <text class="copy-icon">📋</text>
            <text class="copy-text">复制</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 底部占位 + 固定按钮 -->
    <view class="bottom-placeholder"></view>
    <view class="bottom-bar">
      <!-- 待确认状态：取消订单 -->
      <button
        v-if="order && order.status === 'pending'"
        class="cancel-btn"
        @click="onCancelOrder"
      >取消订单</button>
      <!-- 已确认状态：立即支付（Sprint 4 实现） -->
      <button
        v-if="order && order.status === 'confirmed'"
        class="pay-btn"
        @click="onPay"
      >立即支付</button>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { useAuthGuard } from '@/utils/auth-guard'
import { getOrderDetail, cancelOrder } from '../../api/order.js'

// 路由参数
const orderId = ref(null)

// 加载状态
const loading = ref(true)

// 订单数据
const order = ref(null)

// 进度条步骤定义
const progressSteps = [
  { key: 'pending', label: '待确认' },
  { key: 'confirmed', label: '已确认' },
  { key: 'in_progress', label: '进行中' },
  { key: 'completed', label: '已完成' }
]

// 计算当前步骤索引
const currentStepIndex = ref(0)

// Mock 数据
const mockOrder = {
  id: 'ORD20260412001',
  orderNo: 'CAR20260412001',
  vehicleName: '特斯拉 Model 3',
  vehicleBrand: 'Tesla',
  startDate: '2026-04-12',
  endDate: '2026-04-15',
  days: 3,
  dailyRate: 399,
  totalAmount: 1197,
  discount: 0,
  finalAmount: 1197,
  status: 'pending',
  statusText: '待确认',
  createdAt: '2026-04-11 15:30:00',
  pickupAddress: 'XX市XX区XX路XX号',
  priceBreakdown: {
    dailyRate: 399,
    days: 3,
    subtotal: 1197,
    discount: 0,
    total: 1197
  }
}

onLoad((options) => {
  orderId.value = options?.id || null
  fetchDetail()
})

onShow(() => {
  useAuthGuard()
})

async function fetchDetail() {
  loading.value = true
  try {
    if (orderId.value) {
      const res = await getOrderDetail(orderId.value)
      order.value = res
      updateStepIndex()
    } else {
      throw new Error('no orderId')
    }
  } catch (err) {
    console.warn('API 获取订单详情失败，使用 Mock 数据', err)
    order.value = mockOrder
    updateStepIndex()
  } finally {
    loading.value = false
  }
}

function updateStepIndex() {
  if (!order.value) return
  if (order.value.status === 'cancelled' || order.value.status === 'rejected') {
    currentStepIndex.value = -1
    return
  }
  const idx = progressSteps.findIndex(s => s.key === order.value.status)
  currentStepIndex.value = idx >= 0 ? idx : 0
}

// 复制取车地址
function copyAddress() {
  if (!order.value) return
  uni.setClipboardData({
    data: order.value.pickupAddress,
    success: () => {
      uni.showToast({ title: '地址已复制', icon: 'success' })
    }
  })
}

// 取消订单
async function onCancelOrder() {
  uni.showModal({
    title: '确认取消',
    content: '确定要取消该订单吗？',
    success: async (res) => {
      if (!res.confirm) return

      try {
        await cancelOrder(orderId.value)
        uni.showToast({ title: '订单已取消', icon: 'success' })
        // 刷新订单状态
        order.value.status = 'cancelled'
        order.value.statusText = '已取消'
        currentStepIndex.value = -1
      } catch (err) {
        console.error('取消订单失败', err)
        uni.showToast({ title: '取消失败，请重试', icon: 'none' })
      }
    }
  })
}

// 立即支付（Sprint 4 占位）
function onPay() {
  uni.showToast({ title: '支付功能开发中', icon: 'none' })
}
</script>

<style scoped>
.container {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 160rpx;
}

.loading {
  text-align: center;
  padding: 200rpx 0;
  color: #999;
  font-size: 28rpx;
}

/* ========== 状态进度条 ========== */
.progress-card {
  background: #fff;
  margin: 20rpx;
  border-radius: 16rpx;
  padding: 40rpx 30rpx 30rpx;
}

.progress-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.step-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
  flex: 1;
}

.step-line {
  position: absolute;
  top: 20rpx;
  left: 50%;
  width: 100%;
  height: 4rpx;
  background: #e5e5e5;
  z-index: 0;
}

.line-active {
  background: #07c160;
}

.step-dot {
  width: 40rpx;
  height: 40rpx;
  border-radius: 50%;
  background: #e5e5e5;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1;
  transition: all 0.3s;
}

.dot-active {
  background: #07c160;
}

.dot-rejected {
  background: #e4393c;
}

.dot-icon {
  color: #fff;
  font-size: 24rpx;
  font-weight: bold;
}

.step-text {
  font-size: 22rpx;
  color: #999;
  margin-top: 12rpx;
  white-space: nowrap;
}

.text-active {
  color: #333;
  font-weight: 500;
}

.reject-tip {
  margin-top: 24rpx;
  text-align: center;
  background: #fff0f0;
  border-radius: 8rpx;
  padding: 16rpx;
}

.reject-text {
  font-size: 26rpx;
  color: #e4393c;
  font-weight: 500;
}

/* ========== 通用卡片样式 ========== */
.section-title {
  font-size: 30rpx;
  font-weight: 500;
  color: #333;
  display: block;
  margin-bottom: 24rpx;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16rpx 0;
  border-bottom: 1rpx solid #f0f0f0;
}

.info-row:last-child {
  border-bottom: none;
}

.label {
  font-size: 28rpx;
  color: #666;
}

.value {
  font-size: 28rpx;
  color: #333;
  font-weight: 500;
}

/* ========== 订单信息卡片 ========== */
.info-card {
  background: #fff;
  margin: 20rpx;
  border-radius: 16rpx;
  padding: 30rpx;
}

.status-text {
  font-weight: 600;
}

.status-pending {
  color: #ff9500;
}

.status-confirmed {
  color: #07c160;
}

.status-in_progress {
  color: #1989fa;
}

.status-completed {
  color: #999;
}

.status-cancelled {
  color: #999;
}

.status-rejected {
  color: #e4393c;
}

/* ========== 车辆信息卡片 ========== */
.vehicle-card {
  background: #fff;
  margin: 20rpx;
  border-radius: 16rpx;
  padding: 30rpx;
}

.vehicle-header {
  display: flex;
  align-items: baseline;
  gap: 16rpx;
  margin-bottom: 20rpx;
}

.vehicle-name {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
}

.vehicle-brand {
  font-size: 24rpx;
  color: #999;
  background: #f5f5f5;
  padding: 4rpx 12rpx;
  border-radius: 6rpx;
}

.price {
  color: #e4393c;
}

.unit {
  font-size: 22rpx;
  color: #999;
  margin-left: 4rpx;
}

/* ========== 价格明细卡片 ========== */
.price-card {
  background: #fff;
  margin: 20rpx;
  border-radius: 16rpx;
  padding: 30rpx;
}

.price-detail-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12rpx 0;
}

.detail-label {
  font-size: 28rpx;
  color: #666;
}

.detail-value {
  font-size: 28rpx;
  color: #333;
  font-weight: 500;
}

.discount-text {
  color: #ff6b00;
}

.divider {
  height: 1rpx;
  background: #e5e5e5;
  margin: 16rpx 0;
}

.final-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8rpx 0;
}

.final-label {
  font-size: 30rpx;
  color: #333;
  font-weight: 600;
}

.final-value {
  font-size: 44rpx;
  color: #e4393c;
  font-weight: bold;
}

/* ========== 取车地址卡片 ========== */
.address-card {
  background: #fff;
  margin: 20rpx;
  border-radius: 16rpx;
  padding: 30rpx;
}

.address-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.address-text {
  font-size: 28rpx;
  color: #333;
  flex: 1;
  margin-right: 20rpx;
}

.copy-btn {
  display: flex;
  align-items: center;
  gap: 6rpx;
  background: #f5f5f5;
  padding: 12rpx 20rpx;
  border-radius: 8rpx;
  flex-shrink: 0;
}

.copy-icon {
  font-size: 24rpx;
}

.copy-text {
  font-size: 24rpx;
  color: #666;
}

/* ========== 底部固定按钮 ========== */
.bottom-placeholder {
  height: 120rpx;
}

.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #fff;
  padding: 20rpx 30rpx;
  box-shadow: 0 -2rpx 10rpx rgba(0, 0, 0, 0.05);
}

.cancel-btn {
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  background: #e4393c;
  color: #fff;
  font-size: 32rpx;
  font-weight: 500;
  border-radius: 12rpx;
  border: none;
}

.cancel-btn::after {
  border: none;
}

.pay-btn {
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  background: #07c160;
  color: #fff;
  font-size: 32rpx;
  font-weight: 500;
  border-radius: 12rpx;
  border: none;
}

.pay-btn::after {
  border: none;
}
</style>
