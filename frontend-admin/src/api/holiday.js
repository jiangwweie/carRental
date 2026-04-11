import axios from 'axios'

function getToken() {
  return localStorage.getItem('token')
}

export function listHolidays(year) {
  return axios.get('/api/v1/admin/pricing/holidays', {
    headers: { Authorization: `Bearer ${getToken()}` },
    params: { year }
  })
}

export function createHoliday(data) {
  return axios.post('/api/v1/admin/pricing/holidays', data, {
    headers: { Authorization: `Bearer ${getToken()}` }
  })
}

export function deleteHoliday(id) {
  return axios.delete(`/api/v1/admin/pricing/holidays/${id}`, {
    headers: { Authorization: `Bearer ${getToken()}` }
  })
}
