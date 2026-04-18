<template>
  <view class="container">
    <!-- 取车信息卡片 -->
    <view class="pickup-card">
      <view class="card-title">选择取还车时间</view>
      <view class="time-row">
        <text class="label">取车时间</text>
        <picker mode="date" :value="pickupDate" :start="minDate" @change="onPickupDateChange">
          <view class="date-picker">{{ pickupDate }}</view>
        </picker>
      </view>
      <view class="time-row">
        <text class="label">还车时间</text>
        <picker mode="date" :value="returnDate" :start="pickupDate" @change="onReturnDateChange">
          <view class="date-picker">{{ returnDate }}</view>
        </picker>
      </view>
      <button class="search-btn" @click="loadVehicles">查看可用车辆</button>
    </view>

    <!-- 价格区间筛选 -->
    <view class="price-filter">
      <view
        v-for="item in priceRanges"
        :key="item.label"
        class="price-pill"
        :class="{ 'price-pill-active': selectedPriceRange === item.label }"
        @click="selectPriceRange(item)"
      >
        <text class="pill-text">{{ item.label }}</text>
      </view>
    </view>

    <!-- 加载中 -->
    <view v-if="loading && vehicles.length === 0" class="loading">
      <text>加载中...</text>
    </view>

    <!-- 空状态 -->
    <view v-else-if="!loading && vehicles.length === 0" class="empty">
      <text class="empty-icon">🚗</text>
      <text class="empty-text">暂无可用车辆</text>
      <text class="empty-hint">请尝试更换取还车日期或价格区间</text>
      <button class="retry-btn" @click="loadVehicles">刷新试试</button>
    </view>

    <!-- 车辆列表 -->
    <scroll-view
      v-else
      class="vehicle-list"
      scroll-y
      refresher-enabled
      :refresher-triggered="refreshing"
      @refresherrefresh="onRefresh"
      @scrolltolower="onLoadMore"
    >
      <view v-for="vehicle in vehicles" :key="vehicle.id" class="vehicle-card" @click="goDetail(vehicle.id)">
        <image class="cover" :src="vehicle.coverImage" mode="aspectFill" />
        <view class="info">
          <text class="name">{{ vehicle.name }}</text>
          <text class="desc">{{ vehicle.description || '经济实用，出行无忧' }}</text>
          <view class="price-row">
            <text class="price">¥{{ vehicle.weekdayPrice }}<text class="unit">/天起</text></text>
          </view>
        </view>
      </view>
      <!-- 加载更多提示 -->
      <view v-if="loadingMore" class="load-more">加载中...</view>
      <view v-else-if="noMore && vehicles.length > 0" class="load-more">没有更多了</view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getVehicleList } from '../../api/vehicle.js'

// 日期相关
const today = new Date()
const formatDate = (d) => {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

const minDate = formatDate(today)
const pickupDate = ref(minDate)
const tomorrow = new Date(today)
tomorrow.setDate(tomorrow.getDate() + 1)
const returnDate = ref(formatDate(tomorrow))

// 列表状态
const vehicles = ref([])
const loading = ref(true)
const refreshing = ref(false)
const loadingMore = ref(false)
const noMore = ref(false)

// 价格区间筛选
const priceRanges = [
  { label: '全部', min: null, max: null },
  { label: '¥0-200', min: 0, max: 200 },
  { label: '¥200-400', min: 200, max: 400 },
  { label: '¥400+', min: 400, max: null }
]
const selectedPriceRange = ref('全部')

function selectPriceRange(item) {
  selectedPriceRange.value = item.label
  loadVehicles()
}

function onPickupDateChange(e) {
  pickupDate.value = e.detail.value
  if (returnDate.value < pickupDate.value) {
    returnDate.value = pickupDate.value
  }
}

function onReturnDateChange(e) {
  returnDate.value = e.detail.value
}

async function loadVehicles(isRefresh = false, isLoadMore = false) {
  if (isLoadMore) loadingMore.value = true
  if (isRefresh) refreshing.value = true
  if (!isRefresh && !isLoadMore) loading.value = true

  // 获取当前选中的价格区间
  const currentRange = priceRanges.find(r => r.label === selectedPriceRange.value) || priceRanges[0]
  const minPrice = currentRange.min
  const maxPrice = currentRange.max

  try {
    const params = {
      page: isLoadMore ? Math.ceil(vehicles.value.length / 10) + 1 : 1,
      pageSize: 10
    }
    if (minPrice !== null) params.minPrice = minPrice
    if (maxPrice !== null) params.maxPrice = maxPrice
    const res = await getVehicleList(params)
    // 拼接图片完整URL
    const BASE_URL = 'http://192.168.123.232:8081'
    const itemsWithFullUrl = (res.items || []).map(item => ({
      ...item,
      coverImage: item.coverImage && !item.coverImage.startsWith('data:')
        ? (item.coverImage.startsWith('http') ? item.coverImage : BASE_URL + item.coverImage)
        : item.coverImage
    }))
    if (isLoadMore) {
      vehicles.value = [...vehicles.value, ...itemsWithFullUrl]
    } else {
      vehicles.value = itemsWithFullUrl
    }
    noMore.value = (res.items || []).length < 10
  } catch (err) {
    // 错误已在 request.js 中处理并显示 Toast
    console.warn('[LOAD_VEHICLES_ERROR]', err.message)
  } finally {
    loading.value = false
    refreshing.value = false
    loadingMore.value = false
  }
}

function onRefresh() {
  noMore.value = false
  loadVehicles(true)
}

function onLoadMore() {
  if (!noMore.value && !loadingMore.value) {
    loadVehicles(false, true)
  }
}

function goDetail(id) {
  uni.navigateTo({ url: `/pages/vehicle-detail/vehicle-detail?id=${id}` })
}

onMounted(() => {
  loadVehicles()
})
</script>

<style scoped>
.container {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 20rpx;
}

/* 取车信息卡片 */
.pickup-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  margin: 20rpx;
  border-radius: 20rpx;
  padding: 30rpx;
  color: #fff;
}

.card-title {
  font-size: 32rpx;
  font-weight: 600;
  margin-bottom: 24rpx;
}

.time-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16rpx 0;
  border-bottom: 1rpx solid rgba(255, 255, 255, 0.2);
}

.time-row:last-of-type {
  border-bottom: none;
}

.label {
  font-size: 28rpx;
  opacity: 0.9;
}

.date-picker {
  font-size: 28rpx;
  background: rgba(255, 255, 255, 0.2);
  padding: 10rpx 20rpx;
  border-radius: 8rpx;
  min-width: 200rpx;
  text-align: center;
}

.search-btn {
  margin-top: 24rpx;
  background: #fff;
  color: #764ba2;
  font-size: 30rpx;
  font-weight: 600;
  border-radius: 40rpx;
  border: none;
  height: 80rpx;
  line-height: 80rpx;
}

/* 价格区间筛选 */
.price-filter {
  display: flex;
  gap: 16rpx;
  padding: 20rpx 20rpx 0;
  flex-wrap: nowrap;
}

.price-pill {
  flex-shrink: 0;
  background: #fff;
  border: 1rpx solid #e5e5e5;
  border-radius: 40rpx;
  padding: 10rpx 28rpx;
}

.price-pill-active {
  background: #764ba2;
  border-color: #764ba2;
}

.pill-text {
  font-size: 24rpx;
  color: #666;
}

.price-pill-active .pill-text {
  color: #fff;
  font-weight: 500;
}

/* 加载状态 */
.loading {
  text-align: center;
  padding: 80rpx 0;
  color: #999;
  font-size: 28rpx;
}

/* 空状态 */
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 200rpx 40rpx;
}

.empty-icon {
  font-size: 120rpx;
  margin-bottom: 24rpx;
}

.empty-text {
  font-size: 32rpx;
  color: #333;
  margin-bottom: 12rpx;
}

.empty-hint {
  font-size: 26rpx;
  color: #999;
  margin-bottom: 40rpx;
}

.retry-btn {
  background: #764ba2;
  color: #fff;
  font-size: 28rpx;
  border-radius: 40rpx;
  border: none;
  width: 300rpx;
  height: 72rpx;
  line-height: 72rpx;
}

/* 车辆列表 */
.vehicle-list {
  padding: 0 20rpx;
  height: calc(100vh - 500rpx);
}

.vehicle-card {
  display: flex;
  background: #fff;
  border-radius: 16rpx;
  margin-bottom: 20rpx;
  overflow: hidden;
}

.cover {
  width: 240rpx;
  height: 180rpx;
  flex-shrink: 0;
}

.info {
  flex: 1;
  padding: 20rpx;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.name {
  font-size: 30rpx;
  font-weight: 500;
  color: #333;
}

.desc {
  font-size: 24rpx;
  color: #999;
  margin-top: 8rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.price-row {
  display: flex;
  align-items: flex-end;
  margin-top: 12rpx;
}

.price {
  font-size: 34rpx;
  color: #e4393c;
  font-weight: 600;
}

.unit {
  font-size: 22rpx;
  font-weight: 400;
  color: #999;
}

/* 加载更多 */
.load-more {
  text-align: center;
  padding: 30rpx 0;
  color: #999;
  font-size: 24rpx;
}
</style>
