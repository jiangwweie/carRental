import { request } from './request.js'

export function getAdminOrders(params = {}) {
  return request({ url: '/api/v1/admin/orders', data: params })
}

export function confirmOrder(id) {
  return request({ url: `/api/v1/admin/orders/${id}/confirm`, method: 'POST' })
}

export function rejectOrder(id, reason) {
  return request({
    url: `/api/v1/admin/orders/${id}/reject`,
    method: 'POST',
    data: { reason }
  })
}

export function getDashboardOverview() {
  return request({ url: '/api/v1/admin/dashboard/overview' })
}
