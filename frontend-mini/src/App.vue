<script setup>
import { onLaunch, onShow, onHide } from '@dcloudio/uni-app'
import { useUserStore } from './store/user.js'

onLaunch(() => {
  console.log('App Launch')

  // 检查本地登录态
  const userStore = useUserStore()
  const isLogin = userStore.checkLoginStatus()
  if (!isLogin) {
    console.log('App Launch: 用户未登录，首页和车辆详情页为公开页面，暂不强制跳转')
  } else {
    console.log('App Launch: 用户已登录')
  }
})

onShow(() => {
  console.log('App Show')
})

onHide(() => {
  console.log('App Hide')
})
</script>

<style>
page {
  background-color: #f5f5f5;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}
</style>
