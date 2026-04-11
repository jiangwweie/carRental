const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081'

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
          resolve(res.data.data)
        } else {
          // 401 / token expired in business response
          if (isUnauthorized(res.data.message)) {
            handle401()
          }
          uni.showToast({ title: res.data.message || '请求失败', icon: 'none' })
          reject(new Error(res.data.message))
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
