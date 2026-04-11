import { request } from './request.js'

/**
 * 模拟登录（Sprint 1 开发用）
 * @param {Object} data - 登录参数
 * @param {string} [data.role='user'] - 模拟登录角色
 * @returns {Promise} 登录结果
 */
export function mockLogin(data = {}) {
  return request({
    url: '/api/v1/auth/mock-login',
    method: 'POST',
    data
  })
}

/**
 * 微信小程序登录
 * @param {Object} data - 登录参数
 * @param {string} data.login_code - wx.login() 获取的 code
 * @param {string} data.phone_code - getUserPhoneNumber 获取的 code
 * @returns {Promise} 登录结果
 */
export function wxLogin(data) {
  return request({
    url: '/api/v1/auth/wx-login',
    method: 'POST',
    data
  })
}

/**
 * 刷新 Token
 * @param {string} refreshToken - 刷新令牌
 * @returns {Promise} 新的 token
 */
export function refreshToken(refreshToken) {
  return request({
    url: '/api/v1/auth/refresh',
    method: 'POST',
    data: { refreshToken }
  })
}
