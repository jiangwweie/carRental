<template>
  <div>
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px">
      <div style="display: flex; align-items: center; gap: 12px">
        <h2 style="margin: 0">节假日管理</h2>
        <el-select v-model="selectedYear" style="width: 120px" @change="loadHolidays">
          <el-option v-for="y in yearOptions" :key="y" :label="y" :value="y" />
        </el-select>
      </div>
      <el-button type="primary" @click="showAddDialog">新增节假日</el-button>
    </div>

    <el-table :data="holidays" stripe>
      <el-table-column prop="name" label="名称" />
      <el-table-column prop="startDate" label="开始日期" />
      <el-table-column prop="endDate" label="结束日期" />
      <el-table-column prop="priceMultiplier" label="价格倍率" />
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-popconfirm title="确定删除该节假日配置吗？" @confirm="handleDelete(row.id)">
            <template #reference>
              <el-button size="small" type="danger">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" title="新增节假日" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="如：春节、国庆节" />
        </el-form-item>
        <el-form-item label="开始日期" prop="startDate">
          <el-date-picker
            v-model="form.startDate"
            type="date"
            placeholder="选择开始日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="结束日期" prop="endDate">
          <el-date-picker
            v-model="form.endDate"
            type="date"
            placeholder="选择结束日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="价格倍率" prop="priceMultiplier">
          <el-input-number v-model="form.priceMultiplier" :min="0.01" :precision="2" :step="0.1" />
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
import { ElMessage } from 'element-plus'
import { listHolidays, createHoliday, deleteHoliday } from '../../api/holiday'

const currentYear = new Date().getFullYear()
const yearOptions = [currentYear, currentYear + 1, currentYear + 2]
const selectedYear = ref(currentYear)

const holidays = ref([])
const dialogVisible = ref(false)
const formRef = ref(null)

const form = ref({
  name: '',
  startDate: '',
  endDate: '',
  priceMultiplier: 1.0
})

const rules = {
  name: [{ required: true, message: '请输入节假日名称', trigger: 'blur' }],
  startDate: [{ required: true, message: '请选择开始日期', trigger: 'change' }],
  endDate: [{ required: true, message: '请选择结束日期', trigger: 'change' }],
  priceMultiplier: [{ required: true, message: '请输入价格倍率', trigger: 'blur' }]
}

async function loadHolidays() {
  try {
    const res = await listHolidays(selectedYear.value)
    if (res.data.code === 0) {
      holidays.value = res.data.data
    } else {
      ElMessage.error(res.data.message || '加载失败')
    }
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '加载失败')
  }
}

function showAddDialog() {
  form.value = { name: '', startDate: '', endDate: '', priceMultiplier: 1.0 }
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  try {
    const res = await createHoliday(form.value)
    if (res.data.code !== 0) {
      ElMessage.error(res.data.message || '操作失败')
      return
    }
    ElMessage.success('新增成功')
    dialogVisible.value = false
    loadHolidays()
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '操作失败')
  }
}

async function handleDelete(id) {
  try {
    const res = await deleteHoliday(id)
    if (res.data.code === 0) {
      ElMessage.success('删除成功')
      loadHolidays()
    } else {
      ElMessage.error(res.data.message || '删除失败')
    }
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '删除失败')
  }
}

onMounted(loadHolidays)
</script>
