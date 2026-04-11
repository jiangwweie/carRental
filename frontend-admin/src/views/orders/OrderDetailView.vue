<template>
  <div>
    <el-page-header @back="$router.back()" content="订单详情" style="margin-bottom: 20px" />

    <el-descriptions title="基本信息" :column="2" border>
      <el-descriptions-item label="订单号">{{ order.id }}</el-descriptions-item>
      <el-descriptions-item label="状态">{{ order.status }}</el-descriptions-item>
      <el-descriptions-item label="用户姓名">{{ order.userName }}</el-descriptions-item>
      <el-descriptions-item label="用户手机">{{ order.userPhone }}</el-descriptions-item>
    </el-descriptions>

    <el-descriptions title="车辆信息" :column="2" border style="margin-top: 20px">
      <el-descriptions-item label="车型">{{ order.vehicleName }}</el-descriptions-item>
      <el-descriptions-item label="品牌">{{ order.vehicleBrand }}</el-descriptions-item>
    </el-descriptions>

    <el-descriptions title="租期信息" :column="2" border style="margin-top: 20px">
      <el-descriptions-item label="取车日期">{{ order.startDate }}</el-descriptions-item>
      <el-descriptions-item label="还车日期">{{ order.endDate }}</el-descriptions-item>
      <el-descriptions-item label="取车地点">{{ order.pickupLocation }}</el-descriptions-item>
      <el-descriptions-item label="还车地点">{{ order.returnLocation }}</el-descriptions-item>
    </el-descriptions>

    <el-descriptions title="价格明细" :column="2" border style="margin-top: 20px">
      <el-descriptions-item label="总价">¥{{ order.totalPrice }}</el-descriptions-item>
      <el-descriptions-item label="押金">¥{{ order.deposit }}</el-descriptions-item>
    </el-descriptions>

    <el-alert
      v-if="order.rejectReason"
      title="拒绝原因"
      :description="order.rejectReason"
      type="error"
      show-icon
      style="margin-top: 20px"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import axios from 'axios'
import { ElMessage } from 'element-plus'

const route = useRoute()
const order = ref({})

onMounted(async () => {
  try {
    const token = localStorage.getItem('token')
    const res = await axios.get(`/api/v1/admin/orders/${route.params.id}`, {
      headers: { Authorization: `Bearer ${token}` }
    })
    if (res.data.code === 0) {
      order.value = res.data.data
    } else {
      ElMessage.error(res.data.message || '加载订单详情失败')
    }
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '加载订单详情失败')
  }
})
</script>
