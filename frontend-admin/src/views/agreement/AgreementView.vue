<template>
  <div class="agreement-container">
    <!-- Header section -->
    <div class="header-section">
      <el-card shadow="never">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="当前版本">
            <el-tag v-if="version" type="success">{{ version }}</el-tag>
            <span v-else class="text-muted">--</span>
          </el-descriptions-item>
          <el-descriptions-item label="最后更新">
            <span v-if="updatedAt">{{ formatDate(updatedAt) }}</span>
            <span v-else class="text-muted">--</span>
          </el-descriptions-item>
        </el-descriptions>
      </el-card>
    </div>

    <!-- Editor section -->
    <div class="editor-section">
      <el-card shadow="never">
        <template #header>
          <span>协议内容</span>
        </template>
        <el-input
          v-model="content"
          type="textarea"
          :rows="20"
          placeholder="在此输入租车协议内容..."
          :disabled="loading"
        />
      </el-card>
    </div>

    <!-- Action bar -->
    <div class="action-bar">
      <el-button type="primary" @click="handleSave" :loading="loading">
        保存
      </el-button>
      <el-button @click="handlePreview">预览</el-button>
    </div>

    <!-- Preview dialog -->
    <el-dialog
      v-model="previewVisible"
      title="协议预览"
      width="70%"
    >
      <div class="preview-content">
        <pre v-if="content">{{ content }}</pre>
        <p v-else class="text-muted">暂无内容</p>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'

const content = ref('')
const version = ref('')
const updatedAt = ref('')
const previewVisible = ref(false)
const loading = ref(false)
const originalContent = ref('')

function formatDate(dateStr) {
  if (!dateStr) return '未知'
  const d = new Date(dateStr)
  return d.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

async function loadAgreement() {
  loading.value = true
  try {
    const token = localStorage.getItem('token')
    const res = await axios.get('/api/v1/agreement', {
      headers: { Authorization: `Bearer ${token}` }
    })
    if (res.data.code === 4004 || res.data.code === 404) {
      // No agreement yet — show empty state
      content.value = ''
      version.value = '1.0'
      updatedAt.value = null
      return
    }
    if (res.data.code === 0) {
      const data = res.data.data
      content.value = data.content || ''
      version.value = data.version || ''
      updatedAt.value = data.updatedAt || ''
      originalContent.value = data.content || ''
    }
  } catch (err) {
    ElMessage.error('加载协议失败：' + (err.response?.data?.message || err.message))
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  if (!content.value.trim()) {
    ElMessage.warning('协议内容不能为空')
    return
  }

  try {
    await ElMessageBox.confirm(
      '确定要更新协议吗？新版本将立即生效。',
      '确认更新',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
  } catch (action) {
    if (action === 'cancel' || action === 'close') return
    throw action
  }

  loading.value = true
  try {
    const token = localStorage.getItem('token')
    const res = await axios.put(
      '/api/v1/admin/agreement',
      { content: content.value },
      { headers: { Authorization: `Bearer ${token}` } }
    )
    if (res.data.code !== 0) {
      ElMessage.error(res.data.message || '更新失败')
      return
    }
    ElMessage.success('协议已更新')
    await loadAgreement()
  } catch (error) {
    content.value = originalContent.value
    ElMessage.error(error.response?.data?.message || '更新失败')
  } finally {
    loading.value = false
  }
}

function handlePreview() {
  previewVisible.value = true
}

onMounted(loadAgreement)
</script>

<style scoped>
.agreement-container {
  padding: 20px;
}

.header-section {
  margin-bottom: 16px;
}

.editor-section {
  margin-bottom: 16px;
}

.action-bar {
  display: flex;
  gap: 12px;
}

.preview-content {
  max-height: 60vh;
  overflow-y: auto;
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.8;
}

.preview-content pre {
  margin: 0;
  font-family: inherit;
}

.text-muted {
  color: #909399;
}
</style>
