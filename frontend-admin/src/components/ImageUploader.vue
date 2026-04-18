<template>
  <div class="image-uploader">
    <!-- 已上传图片列表 -->
    <div class="image-list">
      <div
        v-for="(url, index) in modelValue"
        :key="index"
        class="image-item"
      >
        <el-image
          :src="url"
          fit="cover"
          class="image-preview"
          :preview-src-list="modelValue"
          :initial-index="index"
        >
          <template #error>
            <div class="image-error">
              <el-icon><Picture /></el-icon>
              <span>加载失败</span>
            </div>
          </template>
        </el-image>
        <div class="image-actions">
          <el-button
            type="danger"
            size="small"
            circle
            @click="removeImage(index)"
            :disabled="disabled"
          >
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
      </div>

      <!-- 上传按钮 -->
      <el-upload
        v-if="modelValue.length < maxCount"
        ref="uploadRef"
        class="upload-area"
        :action="uploadUrl"
        :headers="uploadHeaders"
        :data="uploadData"
        :show-file-list="false"
        :before-upload="beforeUpload"
        :on-success="handleSuccess"
        :on-error="handleError"
        :on-progress="handleProgress"
        :disabled="disabled || uploading"
        accept="image/jpeg,image/jpg,image/png,image/webp"
        drag
      >
        <div class="upload-content" :class="{ 'is-uploading': uploading }">
          <el-progress
            v-if="uploading"
            type="circle"
            :percentage="uploadProgress"
            :width="60"
          />
          <template v-else>
            <el-icon class="upload-icon"><Plus /></el-icon>
            <div class="upload-text">
              <span>点击或拖拽上传</span>
              <span class="upload-hint">支持 jpg、png、webp，最大 5MB</span>
            </div>
          </template>
        </div>
      </el-upload>
    </div>

    <!-- 提示信息 -->
    <div class="upload-tip">
      已上传 {{ modelValue.length }}/{{ maxCount }} 张
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Delete, Picture } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: {
    type: Array,
    default: () => []
  },
  maxCount: {
    type: Number,
    default: 5
  },
  vehicleId: {
    type: Number,
    default: null
  },
  disabled: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'upload-success', 'upload-error'])

// 上传配置
const uploadUrl = '/api/v1/admin/images/upload'
const uploadHeaders = computed(() => {
  const token = localStorage.getItem('token')
  return token ? { Authorization: `Bearer ${token}` } : {}
})
const uploadData = computed(() => {
  return props.vehicleId ? { vehicleId: props.vehicleId } : {}
})

// 上传状态
const uploading = ref(false)
const uploadProgress = ref(0)
const uploadRef = ref(null)

// 文件大小限制 (5MB)
const MAX_SIZE = 5 * 1024 * 1024

// 允许的文件类型
const ALLOWED_TYPES = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp']

// 错误码映射
const ERROR_MESSAGES = {
  4005: '仅支持 jpg、png、webp 格式的图片',
  4006: '图片大小不能超过 5MB',
  4007: '图片尺寸需在 100x100 到 4096x4096 之间',
  5001: '上传失败，请重试'
}

// 上传前校验
function beforeUpload(file) {
  // 检查文件类型
  if (!ALLOWED_TYPES.includes(file.type)) {
    ElMessage.error('仅支持 jpg、png、webp 格式的图片')
    return false
  }

  // 检查文件大小
  if (file.size > MAX_SIZE) {
    ElMessage.error('图片大小不能超过 5MB')
    return false
  }

  // 检查数量限制
  if (props.modelValue.length >= props.maxCount) {
    ElMessage.warning(`最多上传 ${props.maxCount} 张图片`)
    return false
  }

  uploading.value = true
  uploadProgress.value = 0
  return true
}

// 上传进度
function handleProgress(event) {
  uploadProgress.value = Math.round(event.percent)
}

// 上传成功
function handleSuccess(response) {
  uploading.value = false
  uploadProgress.value = 0

  if (response.code === 0 && response.data?.url) {
    // 更新图片列表
    const newImages = [...props.modelValue, response.data.url]
    emit('update:modelValue', newImages)
    emit('upload-success', response.data)
    ElMessage.success('上传成功')
  } else {
    // 业务错误
    const errorCode = response.code
    const message = ERROR_MESSAGES[errorCode] || response.message || '上传失败'
    ElMessage.error(message)
    emit('upload-error', { error: message, code: errorCode })
  }
}

// 上传失败
function handleError(error) {
  uploading.value = false
  uploadProgress.value = 0

  let message = '上传失败，请重试'

  // 尝试解析错误响应
  try {
    const response = JSON.parse(error.message || '{}')
    const errorCode = response.code
    message = ERROR_MESSAGES[errorCode] || response.message || message
  } catch {
    // 网络错误等
    if (error.message?.includes('Network Error')) {
      message = '网络错误，请检查网络连接'
    }
  }

  ElMessage.error(message)
  emit('upload-error', { error: message })
}

// 删除图片
function removeImage(index) {
  const newImages = [...props.modelValue]
  newImages.splice(index, 1)
  emit('update:modelValue', newImages)
}
</script>

<style scoped>
.image-uploader {
  width: 100%;
}

.image-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.image-item {
  position: relative;
  width: 148px;
  height: 148px;
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid var(--el-border-color);
  background-color: var(--el-fill-color-blank);
}

.image-preview {
  width: 100%;
  height: 100%;
}

.image-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--el-text-color-placeholder);
  font-size: 12px;
  gap: 4px;
}

.image-error .el-icon {
  font-size: 24px;
}

.image-actions {
  position: absolute;
  top: 4px;
  right: 4px;
  opacity: 0;
  transition: opacity 0.2s;
}

.image-item:hover .image-actions {
  opacity: 1;
}

.upload-area {
  width: 148px;
  height: 148px;
}

.upload-area :deep(.el-upload) {
  width: 100%;
  height: 100%;
}

.upload-area :deep(.el-upload-dragger) {
  width: 100%;
  height: 100%;
  border-radius: 6px;
  border-style: dashed;
  border-color: var(--el-border-color);
  background-color: var(--el-fill-color-blank);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: border-color 0.2s, background-color 0.2s;
}

.upload-area :deep(.el-upload-dragger:hover) {
  border-color: var(--el-color-primary);
  background-color: var(--el-color-primary-light-9);
}

.upload-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 12px;
}

.upload-content.is-uploading {
  opacity: 0.7;
}

.upload-icon {
  font-size: 28px;
  color: var(--el-text-color-placeholder);
  margin-bottom: 8px;
}

.upload-text {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--el-text-color-regular);
}

.upload-hint {
  color: var(--el-text-color-placeholder);
  font-size: 11px;
}

.upload-tip {
  margin-top: 8px;
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}

/* 禁用状态 */
.upload-area :deep(.el-upload--disabled .el-upload-dragger) {
  cursor: not-allowed;
  background-color: var(--el-disabled-bg-color);
}
</style>
