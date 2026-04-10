<template>
  <view class="container">
    <view v-if="loading" class="loading">加载中...</view>
    <view v-else class="vehicle-list">
      <view v-for="vehicle in vehicles" :key="vehicle.id" class="vehicle-card" @click="goDetail(vehicle.id)">
        <image class="cover" :src="vehicle.coverImage" mode="aspectFill" />
        <view class="info">
          <text class="name">{{ vehicle.name }}</text>
          <text class="price">¥{{ vehicle.weekdayPrice }}/天起</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getVehicleList } from '../../api/vehicle.js'

const vehicles = ref([])
const loading = ref(true)

onMounted(async () => {
  try {
    const res = await getVehicleList()
    vehicles.value = res.items
  } catch (err) {
    console.error('加载车辆列表失败', err)
  } finally {
    loading.value = false
  }
})

function goDetail(id) {
  uni.navigateTo({ url: `/pages/vehicle-detail/vehicle-detail?id=${id}` })
}
</script>

<style scoped>
.container {
  padding: 20rpx;
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
}

.price {
  font-size: 28rpx;
  color: #e4393c;
}
</style>
