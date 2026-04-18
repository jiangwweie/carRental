import { request } from './request.js'

/**
 * 价格预估
 * @param {Object} data - 估价参数
 * @param {number} data.vehicleId - 车辆 ID
 * @param {string} data.startDate - 取车日期 (YYYY-MM-DD)
 * @param {string} data.endDate - 还车日期 (YYYY-MM-DD)
 * @returns {Promise} 价格预估结果
 */
export function estimatePrice(data) {
  return request({
    url: '/api/v1/pricing/estimate',
    method: 'POST',
    data
  })
}
