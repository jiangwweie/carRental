// 开发环境使用局域网 IP，生产环境使用 localhost
const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://192.168.123.232:8081'

let isLoading = false

function showLoading() {
  if (!isLoading) {
    isLoading = true
    uni.showLoading({ title: '加载中...', mask: true })
  }
}

function hideLoading() {
  if (isLoading) {
    isLoading = false
    uni.hideLoading()
  }
}

function isUnauthorized(message) {
  if (!message) return false
  const lower = message.toLowerCase()
  return lower.includes('unauthorized') || (lower.includes('token') && lower.includes('expir'))
}

function handle401() {
  uni.removeStorageSync('token')
  uni.navigateTo({ url: '/pages/login/login' })
}

/**
 * Request wrapper for UniApp.
 * Backend now returns camelCase directly (since v1.7).
 */

// 错误码映射表（后端错误码 → 用户友好文案）
const ERROR_CODE_MAP = {
  // 通用错误
  4000: '输入信息有误，请检查后重试',
  4003: '登录已过期，请重新登录',
  4004: '请求的资源不存在',
  4010: '您没有权限执行此操作',
  5000: '服务器繁忙，请稍后重试',

  // 认证相关
  4001: '微信登录失败，请重试',
  4002: '获取手机号失败，请重试',

  // 订单相关
  5200: '该时间段已被预订，请选择其他时间',
  5300: '订单状态不允许此操作，请刷新页面',

  // 支付相关
  5100: '支付失败，请重试',
  5101: '退款失败，请联系客服',

  // 文件上传相关
  4005: '仅支持 jpg、png、webp 格式的图片',
  4006: '图片大小不能超过 5MB',
  4007: '图片尺寸需在 100x100 到 4096x4096 之间',
  5001: '上传失败，请重试'
}

/**
 * 获取用户友好的错误提示
 */
function getFriendlyErrorMessage(code, defaultMessage) {
  return ERROR_CODE_MAP[code] || defaultMessage || '操作失败，请重试'
}

export function request(options) {
  const needLoading = options.showLoading !== false

  return new Promise((resolve, reject) => {
    if (needLoading) {
      showLoading()
    }

    const token = uni.getStorageSync('token')
    uni.request({
      url: `${BASE_URL}${options.url}`,
      method: options.method || 'GET',
      data: options.data || {},
      timeout: 5000,
      header: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      },
      success(res) {
        if (needLoading) {
          hideLoading()
        }

        // HTTP error (non-200 status)
        if (res.statusCode && res.statusCode !== 200) {
          if (res.statusCode === 401 || (res.data && isUnauthorized(res.data.message))) {
            handle401()
          }
          uni.showToast({ title: `HTTP ${res.statusCode}`, icon: 'none' })
          reject(new Error(`HTTP ${res.statusCode}`))
          return
        }

        if (res.data.code === 0) {
          // Backend returns camelCase directly since v1.7
          resolve(res.data.data)
        } else {
          // 401 / token expired in business response
          if (isUnauthorized(res.data.message)) {
            handle401()
          }

          // 获取用户友好的错误提示
          const friendlyMessage = getFriendlyErrorMessage(res.data.code, res.data.message)

          uni.showToast({
            title: friendlyMessage,
            icon: 'none',
            duration: 2500
          })

          // 使用 console.warn 而非 console.error，避免打印堆栈
          console.warn('[API_ERROR]', {
            code: res.data.code,
            message: res.data.message,
            friendlyMessage
          })

          reject(new Error(friendlyMessage))
        }
      },
      fail(err) {
        if (needLoading) {
          hideLoading()
        }

        // Network error: no internet, timeout, etc.
        let errorMsg = '网络异常，请检查网络设置'
        if (err.errMsg && err.errMsg.includes('timeout')) {
          errorMsg = '请求超时，请稍后重试'
        } else if (err.errMsg && err.errMsg.includes('fail')) {
          errorMsg = '网络连接失败，请检查网络'
        }

        uni.showToast({ title: errorMsg, icon: 'none' })
        reject(new Error(errorMsg))
      }
    })
  })
}
