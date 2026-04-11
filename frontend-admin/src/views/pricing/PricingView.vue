<template>
  <div>
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px">
      <h2 style="margin: 0">价格设置</h2>
      <el-button type="primary" :loading="saving" @click="saveAll">保存全部</el-button>
    </div>

    <el-table :data="vehicles" stripe>
      <el-table-column prop="name" label="车型" />
      <el-table-column prop="brand" label="品牌" />
      <el-table-column label="工作日价">
        <template #default="{ row }">
          <el-input-number
            v-model="row.weekdayPrice"
            :min="0.01"
            :precision="2"
            controls-position="right"
            size="small"
          />
        </template>
      </el-table-column>
      <el-table-column label="周末价">
        <template #default="{ row }">
          <el-input-number
            v-model="row.weekendPrice"
            :min="0.01"
            :precision="2"
            controls-position="right"
            size="small"
          />
        </template>
      </el-table-column>
      <el-table-column label="节假日价">
        <template #default="{ row }">
          <el-input-number
            v-model="row.holidayPrice"
            :min="0.01"
            :precision="2"
            controls-position="right"
            size="small"
          />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button size="small" @click="resetRow(row)">重置</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'

const vehicles = ref([])
const originalPrices = ref({})
const saving = ref(false)

function getToken() {
  return localStorage.getItem('token')
}

async function loadVehicles() {
  const res = await axios.get('/api/v1/admin/vehicles', {
    headers: { Authorization: `Bearer ${getToken()}` }
  })
  if (res.data.code === 0) {
    vehicles.value = res.data.data
    const prices = {}
    for (const v of vehicles.value) {
      prices[v.id] = {
        weekdayPrice: v.weekdayPrice,
        weekendPrice: v.weekendPrice,
        holidayPrice: v.holidayPrice
      }
    }
    originalPrices.value = prices
  }
}

function resetRow(row) {
  const original = originalPrices.value[row.id]
  if (original) {
    row.weekdayPrice = original.weekdayPrice
    row.weekendPrice = original.weekendPrice
    row.holidayPrice = original.holidayPrice
  }
}

async function saveAll() {
  const items = []
  for (const v of vehicles.value) {
    const original = originalPrices.value[v.id]
    if (!original) continue
    if (
      v.weekdayPrice !== original.weekdayPrice ||
      v.weekendPrice !== original.weekendPrice ||
      v.holidayPrice !== original.holidayPrice
    ) {
      items.push({
        id: v.id,
        weekdayPrice: v.weekdayPrice,
        weekendPrice: v.weekendPrice,
        holidayPrice: v.holidayPrice
      })
    }
  }

  if (items.length === 0) {
    ElMessage.warning('无变更')
    return
  }

  saving.value = true
  try {
    await axios.put(
      '/api/v1/admin/vehicles/prices',
      { items },
      { headers: { Authorization: `Bearer ${getToken()}` } }
    )
    ElMessage.success('价格已更新')
    // Refresh snapshot
    for (const item of items) {
      originalPrices.value[item.id] = {
        weekdayPrice: item.weekdayPrice,
        weekendPrice: item.weekendPrice,
        holidayPrice: item.holidayPrice
      }
    }
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(loadVehicles)
</script>
