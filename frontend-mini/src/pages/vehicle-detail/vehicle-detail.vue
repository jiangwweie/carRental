<template>
  <view class="container">
    <view v-if="loading" class="loading">加载中...</view>
    <view v-else-if="vehicle">
      <!-- 图片轮播 -->
      <view class="swiper-wrapper">
        <swiper class="swiper" circular :indicator-dots="false" autoplay @change="onSwiperChange">
          <swiper-item v-for="(img, idx) in displayImages" :key="idx">
            <image class="swiper-img" :src="img" mode="aspectFill" />
          </swiper-item>
        </swiper>
        <view class="page-indicator">
          <text class="indicator-text">{{ currentImageIndex + 1 }} / {{ displayImages.length }}</text>
        </view>
      </view>

      <!-- 车辆基本信息 -->
      <view class="info-card">
        <text class="vehicle-name">{{ vehicle.name }}</text>
        <view class="tags" v-if="vehicle.tags && vehicle.tags.length > 0">
          <text class="tag" v-for="(tag, idx) in vehicle.tags" :key="idx">{{ tag }}</text>
        </view>
        <view class="price-row">
          <text class="weekday-price">¥{{ vehicle.weekdayPrice }}</text>
          <text class="weekday-unit">/天（工作日）</text>
          <text class="weekend-price" v-if="vehicle.weekendPrice">周末 ¥{{ vehicle.weekendPrice }}/天</text>
        </view>
        <view class="meta-row" v-if="vehicle.seats || vehicle.transmission">
          <text class="meta" v-if="vehicle.seats">{{ vehicle.seats }}座</text>
          <text class="meta" v-if="vehicle.transmission">{{ vehicle.transmission }}</text>
        </view>
        <text class="desc" v-if="vehicle.description">{{ vehicle.description }}</text>
      </view>

      <!-- 租期选择 -->
      <view class="rental-card">
        <text class="section-title">选择租期</text>
        <view class="picker-row">
          <picker mode="date" :value="startPickerValue || minDate" :start="minDate" @change="onStartDateChange">
            <view class="picker-item">
              <text class="picker-label">取车日期</text>
              <text class="picker-value">{{ startDate || '请选择' }}</text>
            </view>
          </picker>
          <picker mode="date" :value="endPickerValue || startPickerValue || minDate" :start="startPickerValue || minDate" @change="onEndDateChange">
            <view class="picker-item">
              <text class="picker-label">还车日期</text>
              <text class="picker-value">{{ endDate || '请选择' }}</text>
            </view>
          </picker>
        </view>
        <view class="rental-summary" v-if="days > 0">
          <text class="summary-text">共 {{ days }} 天</text>
          <text class="summary-price">预估价格：¥{{ totalPrice }}</text>
        </view>
      </view>
    </view>

    <!-- 底部固定按钮 -->
    <view class="bottom-bar" v-if="vehicle">
      <button class="book-btn" @click="goBooking">立即预订</button>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getVehicleDetail } from '../../api/vehicle.js'

const loading = ref(true)
const vehicle = ref(null)
const vehicleId = ref(null)

// 图片轮播
const currentImageIndex = ref(0)

function onSwiperChange(e) {
  currentImageIndex.value = e.detail.current
}

// 租期选择
const startDate = ref('')
const endDate = ref('')
const startPickerValue = ref('')
const endPickerValue = ref('')

// 最小日期为今天
const minDate = (() => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
})()

// 计算天数
const days = computed(() => {
  if (!startDate.value || !endDate.value) return 0
  const start = new Date(startDate.value)
  const end = new Date(endDate.value)
  const diff = (end - start) / (1000 * 60 * 60 * 24)
  return diff > 0 ? Math.ceil(diff) : 0
})

// 计算预估价格
const totalPrice = computed(() => {
  if (days.value <= 0 || !vehicle.value) return 0
  return days.value * vehicle.value.weekdayPrice
})

// 展示图片列表
const displayImages = computed(() => {
  if (!vehicle.value) return []
  if (vehicle.value.images && vehicle.value.images.length > 0) {
    // 拼接完整URL（小程序需要完整URL）
    const BASE_URL = 'http://172.20.10.6:8081'
    return vehicle.value.images.map(img => {
      // 如果已经是完整URL，直接返回
      if (img.startsWith('http://') || img.startsWith('https://')) {
        return img
      }
      // 如果是data:image格式，直接返回
      if (img.startsWith('data:')) {
        return img
      }
      // 否则拼接BASE_URL
      return BASE_URL + img
    })
  }
  return ['data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==']
})

onLoad((options) => {
  vehicleId.value = options?.id || null
  fetchDetail()
})

async function fetchDetail() {
  loading.value = true
  try {
    if (vehicleId.value) {
      const res = await getVehicleDetail(vehicleId.value)
      vehicle.value = res
    } else {
      uni.showToast({ title: '车辆ID不存在', icon: 'none' })
      setTimeout(() => uni.navigateBack(), 1500)
    }
  } catch (err) {
    // 错误已在 request.js 中处理并显示 Toast
    console.warn('[FETCH_VEHICLE_ERROR]', err.message)
    setTimeout(() => uni.navigateBack(), 1500)
  } finally {
    loading.value = false
  }
}

function onStartDateChange(e) {
  startDate.value = e.detail.value
  startPickerValue.value = e.detail.value
  if (endDate.value && endDate.value <= startDate.value) {
    endDate.value = ''
    endPickerValue.value = ''
  }
}

function onEndDateChange(e) {
  endDate.value = e.detail.value
  endPickerValue.value = e.detail.value
}

// 跳转预订页
function goBooking() {
  if (!vehicle.value) return
  uni.navigateTo({
    url: `/pages/booking/booking?vehicleId=${vehicle.value.id}&startDate=${startDate.value}&endDate=${endDate.value}&days=${days.value}&totalPrice=${totalPrice.value}`
  })
}
</script>

<style scoped>
.container {
  padding-bottom: 120rpx;
  background: #f5f5f5;
  min-height: 100vh;
}

.loading {
  text-align: center;
  padding: 200rpx 0;
  color: #999;
  font-size: 28rpx;
}

/* 图片轮播 */
.swiper-wrapper {
  position: relative;
  width: 100%;
  height: 400rpx;
  overflow: hidden;
}

.swiper {
  width: 100%;
  height: 400rpx;
}

.swiper-img {
  width: 100%;
  height: 100%;
}

.page-indicator {
  position: absolute;
  bottom: 20rpx;
  right: 20rpx;
  background: rgba(0, 0, 0, 0.5);
  border-radius: 20rpx;
  padding: 6rpx 16rpx;
}

.indicator-text {
  color: #fff;
  font-size: 22rpx;
}

/* 车辆信息卡片 */
.info-card {
  background: #fff;
  margin: 20rpx;
  border-radius: 16rpx;
  padding: 30rpx;
}

.vehicle-name {
  font-size: 36rpx;
  font-weight: bold;
  color: #333;
  display: block;
  margin-bottom: 16rpx;
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-bottom: 20rpx;
}

.tag {
  background: #e8f5e9;
  color: #07c160;
  font-size: 22rpx;
  padding: 4rpx 16rpx;
  border-radius: 8rpx;
}

.price-row {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 8rpx;
  margin-bottom: 16rpx;
}

.weekday-price {
  font-size: 48rpx;
  font-weight: bold;
  color: #e4393c;
}

.weekday-unit {
  font-size: 24rpx;
  color: #999;
}

.weekend-price {
  font-size: 24rpx;
  color: #666;
  margin-left: 16rpx;
}

.meta-row {
  display: flex;
  gap: 24rpx;
  margin-bottom: 16rpx;
}

.meta {
  font-size: 26rpx;
  color: #666;
  background: #f5f5f5;
  padding: 6rpx 20rpx;
  border-radius: 8rpx;
}

.desc {
  font-size: 26rpx;
  color: #666;
  line-height: 1.6;
  display: block;
}

/* 租期选择卡片 */
.rental-card {
  background: #fff;
  margin: 20rpx;
  border-radius: 16rpx;
  padding: 30rpx;
}

.section-title {
  font-size: 30rpx;
  font-weight: 500;
  color: #333;
  display: block;
  margin-bottom: 24rpx;
}

.picker-row {
  display: flex;
  gap: 20rpx;
  margin-bottom: 20rpx;
}

.picker-item {
  flex: 1;
  background: #f5f5f5;
  border-radius: 12rpx;
  padding: 20rpx;
}

.picker-label {
  font-size: 24rpx;
  color: #999;
  display: block;
  margin-bottom: 8rpx;
}

.picker-value {
  font-size: 28rpx;
  color: #333;
  display: block;
}

.rental-summary {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff8e1;
  border-radius: 12rpx;
  padding: 20rpx;
}

.summary-text {
  font-size: 26rpx;
  color: #333;
}

.summary-price {
  font-size: 30rpx;
  font-weight: bold;
  color: #e4393c;
}

/* 底部固定按钮 */
.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #fff;
  padding: 20rpx 30rpx;
  box-shadow: 0 -2rpx 10rpx rgba(0, 0, 0, 0.05);
}

.book-btn {
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

.book-btn::after {
  border: none;
}
</style>
