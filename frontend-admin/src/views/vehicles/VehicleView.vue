<template>
  <div>
    <el-table :data="vehicles" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="车型" />
      <el-table-column prop="brand" label="品牌" />
      <el-table-column prop="weekdayPrice" label="工作日价" />
      <el-table-column prop="status" label="状态" />
      <el-table-column label="操作">
        <template #default="{ row }">
          <el-button size="small" @click="toggleStatus(row)">{{ row.status === 'active' ? '下架' : '上架' }}</el-button>
          <el-button size="small" type="danger" @click="deleteVehicle(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-button type="primary" @click="showAddDialog">新增车辆</el-button>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'

const vehicles = ref([])

async function loadVehicles() {
  const token = localStorage.getItem('token')
  const res = await axios.get('/api/v1/admin/vehicles', {
    headers: { Authorization: `Bearer ${token}` }
  })
  if (res.data.code === 0) {
    vehicles.value = res.data.data
  }
}

onMounted(loadVehicles)

function showAddDialog() {
  // TODO: 实现新增弹窗
}

async function toggleStatus(row) {
  const token = localStorage.getItem('token')
  await axios.post(`/api/v1/admin/vehicles/${row.id}/toggle-status`, null, {
    headers: { Authorization: `Bearer ${token}` }
  })
  loadVehicles()
}

async function deleteVehicle(id) {
  const token = localStorage.getItem('token')
  await axios.delete(`/api/v1/admin/vehicles/${id}`, {
    headers: { Authorization: `Bearer ${token}` }
  })
  loadVehicles()
}
</script>
