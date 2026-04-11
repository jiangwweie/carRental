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
          <el-button size="small" @click="showEditDialog(row)">编辑</el-button>
          <el-button size="small" @click="toggleStatus(row)">{{ row.status === 'active' ? '下架' : '上架' }}</el-button>
          <el-button size="small" type="danger" @click="deleteVehicle(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-button type="primary" @click="showAddDialog">新增车辆</el-button>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑车辆' : '新增车辆'" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="车型名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="品牌" prop="brand">
          <el-input v-model="form.brand" />
        </el-form-item>
        <el-form-item label="座位数" prop="seats">
          <el-input-number v-model="form.seats" :min="1" />
        </el-form-item>
        <el-form-item label="变速箱" prop="transmission">
          <el-select v-model="form.transmission" placeholder="请选择">
            <el-option label="自动挡" value="自动挡" />
            <el-option label="手动挡" value="手动挡" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="图片" prop="images">
          <el-input v-model="imagesInput" type="textarea" :rows="3" placeholder="每行一个图片 URL/base64" />
        </el-form-item>
        <el-form-item label="标签" prop="tags">
          <el-select v-model="form.tags" multiple filterable allow-create placeholder="请选择或输入标签" style="width: 100%">
            <el-option label="SUV" value="SUV" />
            <el-option label="经济型" value="经济型" />
            <el-option label="豪华型" value="豪华型" />
            <el-option label="新能源" value="新能源" />
            <el-option label="7座" value="7座" />
            <el-option label="敞篷" value="敞篷" />
          </el-select>
        </el-form-item>
        <el-form-item label="工作日价" prop="weekdayPrice">
          <el-input-number v-model="form.weekdayPrice" :min="0.01" :precision="2" />
        </el-form-item>
        <el-form-item label="周末价" prop="weekendPrice">
          <el-input-number v-model="form.weekendPrice" :min="0.01" :precision="2" />
        </el-form-item>
        <el-form-item label="节假日价" prop="holidayPrice">
          <el-input-number v-model="form.holidayPrice" :min="0.01" :precision="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'

const vehicles = ref([])

async function loadVehicles() {
  const token = localStorage.getItem('token')
  const res = await axios.get('/api/v1/admin/vehicles', {
    headers: { Authorization: `Bearer ${token}` }
  })
  if (res.data.code === 0) {
    vehicles.value = res.data.data
  } else {
    ElMessage.error(res.data.message || '加载失败')
  }
}

onMounted(loadVehicles)

const dialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref(null)
const formRef = ref(null)
const imagesInput = ref('')

const emptyForm = () => ({
  name: '',
  brand: '',
  seats: 2,
  transmission: '',
  description: '',
  tags: [],
  weekdayPrice: 0,
  weekendPrice: 0,
  holidayPrice: 0
})

const form = ref(emptyForm())

const rules = {
  name: [{ required: true, message: '请输入车型名称', trigger: 'blur' }],
  brand: [{ required: true, message: '请输入品牌', trigger: 'blur' }],
  seats: [{ required: true, message: '请输入座位数', trigger: 'blur' }],
  transmission: [{ required: true, message: '请选择变速箱', trigger: 'change' }],
  weekdayPrice: [{ required: true, message: '请输入工作日价', trigger: 'blur' }],
  weekendPrice: [{ required: true, message: '请输入周末价', trigger: 'blur' }],
  holidayPrice: [{ required: true, message: '请输入节假日价', trigger: 'blur' }]
}

function showAddDialog() {
  isEdit.value = false
  editingId.value = null
  form.value = emptyForm()
  imagesInput.value = ''
  dialogVisible.value = true
}

function showEditDialog(row) {
  isEdit.value = true
  editingId.value = row.id
  form.value = {
    name: row.name || '',
    brand: row.brand || '',
    seats: row.seats ?? 2,
    transmission: row.transmission || '',
    description: row.description || '',
    tags: Array.isArray(row.tags) ? [...row.tags] : [],
    weekdayPrice: row.weekdayPrice ?? 0,
    weekendPrice: row.weekendPrice ?? 0,
    holidayPrice: row.holidayPrice ?? 0
  }
  imagesInput.value = Array.isArray(row.images) ? row.images.join('\n') : ''
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  const token = localStorage.getItem('token')
  const payload = {
    ...form.value,
    images: imagesInput.value.split('\n').map(s => s.trim()).filter(Boolean)
  }

  try {
    let res
    if (isEdit.value) {
      res = await axios.put(`/api/v1/admin/vehicles/${editingId.value}`, payload, {
        headers: { Authorization: `Bearer ${token}` }
      })
    } else {
      res = await axios.post('/api/v1/admin/vehicles', payload, {
        headers: { Authorization: `Bearer ${token}` }
      })
    }
    if (res.data.code !== 0) {
      ElMessage.error(res.data.message || '操作失败')
      return
    }
    ElMessage.success(isEdit.value ? '更新成功' : '新增成功')
    dialogVisible.value = false
    loadVehicles()
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '操作失败')
  }
}

async function toggleStatus(row) {
  try {
    const token = localStorage.getItem('token')
    await axios.post(`/api/v1/admin/vehicles/${row.id}/toggle-status`, null, {
      headers: { Authorization: `Bearer ${token}` }
    })
    ElMessage.success('状态已更新')
    loadVehicles()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '操作失败')
  }
}

async function deleteVehicle(id) {
  try {
    await ElMessageBox.confirm('确定删除该车辆吗？此操作不可撤销。', '删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return // user cancelled
  }
  try {
    const token = localStorage.getItem('token')
    await axios.delete(`/api/v1/admin/vehicles/${id}`, {
      headers: { Authorization: `Bearer ${token}` }
    })
    ElMessage.success('删除成功')
    loadVehicles()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '删除失败')
  }
}
</script>
