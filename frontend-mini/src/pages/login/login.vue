<template>
  <view class="login-container">
    <view class="logo">租车服务</view>
    <button class="login-btn" @getphonenumber="onGetPhoneNumber" open-type="getPhoneNumber" @click="onLogin">
      微信一键登录
    </button>
    <view class="agreement">
      <checkbox-group @change="onAgreeChange">
        <label>
          <checkbox :checked="agreed" />
          <text>我已阅读并同意</text>
          <text class="link" @click="goAgreement">《用户协议》</text>
        </label>
      </checkbox-group>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { useUserStore } from '../../store/user.js'

const agreed = ref(false)
const userStore = useUserStore()

function onAgreeChange(e) {
  agreed.value = e.detail.value.length > 0
}

async function onLogin() {
  if (!agreed.value) {
    uni.showToast({ title: '请先同意用户协议', icon: 'none' })
    return
  }

  try {
    // 先调用 wx.login 获取 loginCode
    const loginRes = await uni.login()
    const loginCode = loginRes.code

    // 用户授权手机号
    // phoneCode 将通过 @getphonenumber 事件获取
  } catch (err) {
    console.error('登录失败', err)
    uni.showToast({ title: '登录失败，请重试', icon: 'none' })
  }
}

function onGetPhoneNumber(e) {
  if (e.detail.code) {
    userStore.login(e.detail.code).then(() => {
      uni.switchTab({ url: '/pages/index/index' })
    })
  } else {
    uni.showToast({ title: '授权失败', icon: 'none' })
  }
}

function goAgreement() {
  uni.navigateTo({ url: '/pages/agreement/agreement' })
}
</script>

<style scoped>
.login-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100vh;
  padding: 0 60rpx;
}

.logo {
  font-size: 48rpx;
  font-weight: bold;
  margin-bottom: 100rpx;
}

.login-btn {
  width: 100%;
  background-color: #07c160;
  color: #fff;
  border-radius: 12rpx;
  font-size: 32rpx;
  padding: 24rpx 0;
}

.agreement {
  margin-top: 40rpx;
  font-size: 24rpx;
  color: #666;
}

.link {
  color: #576b95;
}
</style>
