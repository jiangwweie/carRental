<template>
  <div class="login-container">
    <el-card class="login-card">
      <template #header>
        <h2>租车管理后台</h2>
      </template>
      <el-form @submit.prevent="handleLogin">
        <el-form-item label="密码">
          <el-input
            v-model="password"
            type="password"
            placeholder="请输入管理密码"
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" style="width: 100%" :loading="loading" @click="handleLogin">
            登录
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const password = ref('')
const loading = ref(false)
const router = useRouter()

async function handleLogin() {
  if (!password.value) return

  loading.value = true
  try {
    const res = await axios.post('/api/v1/auth/admin-login', { password: password.value })
    if (res.data.code === 0) {
      localStorage.setItem('token', res.data.data.token)
      router.push('/')
    }
  } catch (err) {
    console.error('登录失败', err)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100vh;
  background: #f0f2f5;
}

.login-card {
  width: 400px;
}

h2 {
  text-align: center;
}
</style>
