import { request } from './request.js'

/**
 * 获取当前用户协议
 * @returns {Promise} 协议内容
 */
export function getAgreement() {
  return request({
    url: '/api/v1/agreement',
    method: 'GET'
  })
}
