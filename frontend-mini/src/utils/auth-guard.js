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
    uni.redirectTo({ url: redirectUrl })
    return false
  }

  return true
}
