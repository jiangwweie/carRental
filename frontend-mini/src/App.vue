<script setup>
import { onLaunch, onShow, onHide } from '@dcloudio/uni-app'
import { useUserStore } from './store/user.js'

/**
 * Public pages that do not require login.
 * Users can browse these pages without authentication.
 */
const PUBLIC_PAGES = [
  '/pages/index/index',
  '/pages/vehicle-detail/vehicle-detail',
  '/pages/login/login',
  '/pages/agreement/agreement',
]

onLaunch(() => {
  console.log('App Launch')

  // 检查本地登录态
  const userStore = useUserStore()
  const isLogin = userStore.checkLoginStatus()
  if (!isLogin) {
    console.log('App Launch: 用户未登录，公开页面可正常访问')
  } else {
    console.log('App Launch: 用户已登录')
  }
})

onShow(() => {
  console.log('App Show')

  // 全局登录守卫：每次切回前台时检查登录态
  const userStore = useUserStore()
  const isLogin = userStore.checkLoginStatus()
  if (!isLogin) {
    const pages = getCurrentPages()
    const currentPage = pages[pages.length - 1]
    const route = currentPage?.route || ''

    if (!PUBLIC_PAGES.includes(`/${route}`)) {
      uni.reLaunch({
        url: '/pages/login/login'
      })
    }
  }
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
