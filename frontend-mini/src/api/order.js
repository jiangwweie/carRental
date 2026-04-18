import { request } from './request.js'

/**
 * 创建订单
 * @param {Object} data - 订单参数
 * @param {number} data.vehicleId - 车辆 ID
 * @param {string} data.startDate - 取车日期 (YYYY-MM-DD)
 * @param {string} data.endDate - 还车日期 (YYYY-MM-DD)
 * @param {boolean} data.agreed - 是否同意用户协议
 * @returns {Promise} 订单创建结果
 */
export function createOrder(data) {
  return request({
    url: '/api/v1/orders',
    method: 'POST',
    data
  })
}

/**
 * 获取我的订单列表
 * @param {Object} [params={}] - 查询参数
 * @param {string} [params.status] - 订单状态筛选 (pending/confirmed/in_progress/completed/cancelled/rejected)
 * @param {number} [params.page] - 页码
 * @returns {Promise} 订单列表
 */
export function getOrders(params = {}) {
  return request({
    url: '/api/v1/orders',
    method: 'GET',
    data: params
  })
}

/**
 * 获取订单详情
 * @param {number} id - 订单 ID
 * @returns {Promise} 订单详情
 */
export function getOrderDetail(id) {
  return request({
    url: `/api/v1/orders/${id}`,
    method: 'GET'
  })
}

/**
 * 取消订单
 * @param {number} id - 订单 ID
 * @returns {Promise} 取消结果
 */
export function cancelOrder(id) {
  return request({
    url: `/api/v1/orders/${id}/cancel`,
    method: 'POST'
  })
}
