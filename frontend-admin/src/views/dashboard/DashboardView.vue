<template>
  <div class="dashboard">
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card>
          <template #header>今日订单</template>
          <div class="stat-value">{{ stats.today_orders }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <template #header>今日收入</template>
          <div class="stat-value">¥{{ stats.today_revenue }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <template #header>进行中订单</template>
          <div class="stat-value">{{ stats.active_orders }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <template #header>可租车辆</template>
          <div class="stat-value">{{ stats.available_vehicles }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card>
          <template #header>本月订单数</template>
          <div class="stat-value">{{ stats.month_orders }}</div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>本月收入</template>
          <div class="stat-value">¥{{ stats.month_revenue }}</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'

const stats = ref({
  today_orders: 0,
  today_revenue: 0,
  active_orders: 0,
  available_vehicles: 0
})

onMounted(async () => {
  try {
    const token = localStorage.getItem('token')
    const res = await axios.get('/api/v1/admin/dashboard/overview', {
      headers: { Authorization: `Bearer ${token}` }
    })
    if (res.data.code === 0) {
      stats.value = res.data.data
    }
  } catch (err) {
    console.error('加载仪表盘数据失败', err)
  }
})
</script>

<style scoped>
.stat-value {
  font-size: 32px;
  font-weight: bold;
  text-align: center;
  padding: 20px 0;
}
</style>
