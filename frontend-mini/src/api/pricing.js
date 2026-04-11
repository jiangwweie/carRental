import { request } from './request.js'

/**
 * 价格预估
 * @param {Object} data - 估价参数
 * @param {number} data.vehicle_id - 车辆 ID
 * @param {string} data.start_date - 取车日期 (YYYY-MM-DD)
 * @param {string} data.end_date - 还车日期 (YYYY-MM-DD)
 * @returns {Promise} 价格预估结果
 */
export function estimatePrice(data) {
  return request({
    url: '/api/v1/pricing/estimate',
    method: 'POST',
    data
  })
}
