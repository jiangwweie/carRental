import { request } from './request.js'

export function getVehicleList(params = {}) {
  return request({
    url: '/api/v1/vehicles',
    method: 'GET',
    data: params
  })
}

export function getVehicleDetail(id) {
  return request({
    url: `/api/v1/vehicles/${id}`,
    method: 'GET'
  })
}
