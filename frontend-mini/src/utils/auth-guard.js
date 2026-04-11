import { useUserStore } from '../store/user.js'

/**
 * Auth Guard - Check login status and redirect to login page if not authenticated.
 *
 * Usage in page components:
 *   import { onShow } from '@dcloudio/uni-app'
 *   import { useAuthGuard } from '../../utils/auth-guard.js'
 *
 *   onShow(() => {
 *     useAuthGuard()
 *   })
 *
 * @param {string} [redirectUrl='/pages/login/login'] - The login page path to redirect to
 * @returns {boolean} - true if authenticated, false if redirected
 */
export function useAuthGuard(redirectUrl = '/pages/login/login') {
  const userStore = useUserStore()
  const isLogin = userStore.checkLoginStatus()

  if (!isLogin) {
    // 获取当前页面路径，作为登录后返回的来源页
    const pages = getCurrentPages()
    const currentPage = pages[pages.length - 1]
    const currentRoute = currentPage?.route || ''
    const loginUrl = currentRoute
      ? `${redirectUrl}?redirectTo=${encodeURIComponent('/' + currentRoute)}`
      : redirectUrl

    uni.redirectTo({ url: loginUrl })
    return false
  }

  return true
}
