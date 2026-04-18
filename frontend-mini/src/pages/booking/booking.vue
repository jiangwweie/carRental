<template>
  <view class="container">
    <!-- 车辆信息摘要 -->
    <view class="vehicle-card">
      <text class="vehicle-name">{{ priceData.vehicleName }}</text>
      <view class="price-row">
        <text class="daily-rate">¥{{ priceData.dailyRate }}</text>
        <text class="unit">/天</text>
      </view>
    </view>

    <!-- 租期信息 -->
    <view class="rental-card">
      <text class="section-title">租期信息</text>
      <view class="info-row">
        <text class="label">取车日期</text>
        <text class="value">{{ priceData.startDate }}</text>
      </view>
      <view class="info-row">
        <text class="label">还车日期</text>
        <text class="value">{{ priceData.endDate }}</text>
      </view>
      <view class="info-row">
        <text class="label">租赁天数</text>
        <text class="value">{{ priceData.days }} 天</text>
      </view>
      <view class="info-row">
        <text class="label">取车地址</text>
        <text class="value address">{{ pickupInfo.address }}</text>
      </view>
    </view>

    <!-- 取车指引 -->
    <view class="pickup-guide-card">
      <text class="section-title">取车指引</text>
      <view class="guide-row">
        <text class="guide-icon">📍</text>
        <view class="guide-content">
          <text class="guide-label">取车地址</text>
          <text class="guide-value">{{ pickupInfo.address }}</text>
        </view>
      </view>
      <view class="guide-row">
        <text class="guide-icon">🕐</text>
        <view class="guide-content">
          <text class="guide-label">营业时间</text>
          <text class="guide-value">{{ pickupInfo.hours }}</text>
        </view>
      </view>
      <view class="guide-row">
        <text class="guide-icon">💡</text>
        <view class="guide-content">
          <text class="guide-label">注意事项</text>
          <text class="guide-value">{{ pickupInfo.note }}</text>
        </view>
      </view>
    </view>

    <!-- 价格明细 -->
    <view class="price-card">
      <text class="section-title">价格明细</text>
      <view class="price-detail-row">
        <text class="detail-label">车辆租金</text>
        <text class="detail-value">{{ priceData.days }}天 × ¥{{ priceData.dailyRate }}</text>
      </view>
      <view class="price-detail-row">
        <text class="detail-label">合计金额</text>
        <text class="detail-value">¥{{ priceData.totalAmount }}</text>
      </view>
      <view class="price-detail-row" v-if="priceData.discount > 0">
        <text class="detail-label discount-text">优惠折扣</text>
        <text class="detail-value discount-text">-¥{{ priceData.discount }}</text>
      </view>
      <view class="divider"></view>
      <view class="final-row">
        <text class="final-label">应付金额</text>
        <text class="final-value">¥{{ priceData.finalAmount }}</text>
      </view>
    </view>

    <!-- 协议确认 -->
    <view class="agreement-card">
      <checkbox-group @change="onAgreeChange">
        <label class="agreement-label">
          <checkbox :checked="agreed" />
          <text>我已阅读并同意</text>
          <text class="link" @click="goAgreement">《租车服务协议》</text>
        </label>
      </checkbox-group>
    </view>

    <!-- 底部占位 + 固定按钮 -->
    <view class="bottom-placeholder"></view>
    <view class="bottom-bar">
      <button class="submit-btn" @click="onSubmit">提交订单</button>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { estimatePrice } from '../../api/pricing.js'
import { createOrder } from '../../api/order.js'
import { useAuthGuard } from '../../utils/auth-guard.js'

onShow(() => {
  useAuthGuard()
})

// 取车信息常量（与后端 OrderController.buildPickupAddress 保持一致）
const pickupInfo = {
  address: 'XX市XX区XX路XX号',
  hours: '周一至周日 09:00-18:00',
  note: '下单后请与车主确认取车时间'
}

// 路由参数
const vehicleId = ref(null)
const startDate = ref('')
const endDate = ref('')
const days = ref(0)
const totalPrice = ref(0)

// 协议勾选
const agreed = ref(false)

// 提交中状态
const submitting = ref(false)

// 价格数据
const priceData = ref({})

onLoad((options) => {
  vehicleId.value = options?.vehicleId || null
  startDate.value = options?.startDate || ''
  endDate.value = options?.endDate || ''
  days.value = parseInt(options?.days || '0', 10)
  totalPrice.value = parseFloat(options?.totalPrice || '0')

  fetchPrice()
})

async function fetchPrice() {
  try {
    if (vehicleId.value) {
      const res = await estimatePrice({
        vehicleId: parseInt(vehicleId.value, 10),
        startDate: startDate.value,
        endDate: endDate.value
      })
      const daysCount = res.days || days.value || 1
      const totalPrice = res.totalPrice || 0
      const dailyRate = daysCount > 0 ? Math.round(totalPrice / daysCount) : 0
      priceData.value = {
        vehicleId: res.vehicleId || vehicleId.value,
        vehicleName: res.vehicleName || '未知车辆',
        days: daysCount,
        startDate: res.startDate || startDate.value,
        endDate: res.endDate || endDate.value,
        dailyRate,
        totalAmount: totalPrice,
        discount: 0,
        finalAmount: totalPrice
      }
    } else {
      uni.showToast({ title: '车辆ID不存在', icon: 'none' })
      setTimeout(() => uni.navigateBack(), 1500)
    }
  } catch (err) {
    // 错误已在 request.js 中处理并显示 Toast
    console.warn('[FETCH_PRICE_ERROR]', err.message)
    setTimeout(() => uni.navigateBack(), 1500)
  }
}

function onAgreeChange(e) {
  agreed.value = e.detail.value.length > 0
}

function goAgreement() {
  uni.navigateTo({ url: '/pages/agreement/agreement' })
}

async function onSubmit() {
  if (!agreed.value) {
    uni.showToast({ title: '请先同意租车服务协议', icon: 'none' })
    return
  }

  if (submitting.value) return
  submitting.value = true

  try {
    const res = await createOrder({
      vehicleId: parseInt(vehicleId.value, 10),
      startDate: startDate.value,
      endDate: endDate.value,
      agreed: agreed.value
    })

    uni.showToast({ title: '订单提交成功', icon: 'success' })

    // 跳转到订单详情页
    const orderId = res?.orderId || res?.id
    setTimeout(() => {
      uni.navigateTo({ url: `/pages/order-detail/order-detail?id=${orderId}` })
    }, 800)
  } catch (err) {
    // 错误已在 request.js 中处理并显示 Toast
    // 这里仅记录日志，不再重复提示
    console.warn('[BOOKING_ERROR]', err.message)
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.container {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 160rpx;
}

/* 车辆信息摘要卡片 */
.vehicle-card {
  background: #fff;
  margin: 20rpx;
  border-radius: 16rpx;
  padding: 30rpx;
}

.vehicle-name {
  font-size: 34rpx;
  font-weight: bold;
  color: #333;
  display: block;
  margin-bottom: 12rpx;
}

.price-row {
  display: flex;
  align-items: baseline;
}

.daily-rate {
  font-size: 44rpx;
  font-weight: bold;
  color: #e4393c;
}

.unit {
  font-size: 24rpx;
  color: #999;
  margin-left: 4rpx;
}

/* 租期信息卡片 */
.rental-card {
  background: #fff;
  margin: 20rpx;
  border-radius: 16rpx;
  padding: 30rpx;
}

/* 取车指引卡片 */
.pickup-guide-card {
  background: #fff;
  margin: 20rpx;
  border-radius: 16rpx;
  padding: 30rpx;
}

.guide-row {
  display: flex;
  align-items: flex-start;
  padding: 16rpx 0;
  border-bottom: 1rpx solid #f0f0f0;
}

.guide-row:last-child {
  border-bottom: none;
}

.guide-icon {
  font-size: 32rpx;
  margin-right: 16rpx;
  flex-shrink: 0;
  width: 40rpx;
  text-align: center;
}

.guide-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}

.guide-label {
  font-size: 24rpx;
  color: #999;
}

.guide-value {
  font-size: 28rpx;
  color: #333;
  font-weight: 500;
}

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

.address {
  max-width: 400rpx;
  text-align: right;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 价格明细卡片 */
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

/* 协议确认 */
.agreement-card {
  background: #fff;
  margin: 20rpx;
  border-radius: 16rpx;
  padding: 24rpx 30rpx;
}

.agreement-label {
  display: flex;
  align-items: center;
  font-size: 26rpx;
  color: #666;
  gap: 8rpx;
}

.link {
  color: #576b95;
}

/* 底部固定按钮 */
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

.submit-btn {
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

.submit-btn::after {
  border: none;
}
</style>
