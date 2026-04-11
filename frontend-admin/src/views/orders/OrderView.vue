<template>
  <div>
    <el-select v-model="statusFilter" placeholder="状态筛选" @change="loadOrders">
      <el-option label="全部" value="" />
      <el-option label="待确认" value="pending" />
      <el-option label="已确认" value="confirmed" />
      <el-option label="进行中" value="in_progress" />
      <el-option label="已完成" value="completed" />
    </el-select>

    <el-table :data="orders" stripe style="margin-top: 20px">
      <el-table-column prop="id" label="订单号" width="80" />
      <el-table-column prop="startDate" label="取车日期" />
      <el-table-column prop="endDate" label="还车日期" />
      <el-table-column prop="totalPrice" label="总价" />
      <el-table-column prop="status" label="状态" />
      <el-table-column label="操作">
        <template #default="{ row }">
          <el-button v-if="row.status === 'pending'" size="small" type="success" @click="confirmOrder(row)">确认</el-button>
          <el-button v-if="row.status === 'pending'" size="small" type="danger" @click="rejectOrder(row)">拒绝</el-button>
          <el-button v-if="row.status === 'confirmed'" size="small" @click="startOrder(row)">开始</el-button>
          <el-button v-if="row.status === 'in_progress'" size="small" @click="completeOrder(row)">完成</el-button>
          <el-button size="small" @click="viewDetail(row)">查看详情</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import { ElMessage } from 'element-plus'

const router = useRouter()
const orders = ref([])
const statusFilter = ref('')

async function loadOrders() {
  const token = localStorage.getItem('token')
  const params = statusFilter.value ? { status: statusFilter.value } : {}
  const res = await axios.get('/api/v1/admin/orders', {
    headers: { Authorization: `Bearer ${token}` },
    params
  })
  if (res.data.code === 0) {
    orders.value = res.data.data.items
  }
}

onMounted(loadOrders)

async function confirmOrder(row) {
  const token = localStorage.getItem('token')
  await axios.post(`/api/v1/admin/orders/${row.id}/confirm`, null, {
    headers: { Authorization: `Bearer ${token}` }
  })
  ElMessage.success('订单已确认')
  loadOrders()
}

async function rejectOrder(row) {
  const reason = prompt('请输入拒绝原因（可选）')
  const token = localStorage.getItem('token')
  await axios.post(`/api/v1/admin/orders/${row.id}/reject`, { reason }, {
    headers: { Authorization: `Bearer ${token}` }
  })
  ElMessage.success('订单已拒绝')
  loadOrders()
}

async function startOrder(row) {
  const token = localStorage.getItem('token')
  await axios.post(`/api/v1/admin/orders/${row.id}/start`, null, {
    headers: { Authorization: `Bearer ${token}` }
  })
  ElMessage.success('订单已开始')
  loadOrders()
}

async function completeOrder(row) {
  const token = localStorage.getItem('token')
  await axios.post(`/api/v1/admin/orders/${row.id}/complete`, null, {
    headers: { Authorization: `Bearer ${token}` }
  })
  ElMessage.success('订单已完成')
  loadOrders()
}

function viewDetail(row) {
  router.push(`/orders/${row.id}`)
}
</script>
