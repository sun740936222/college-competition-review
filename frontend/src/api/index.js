import axios from 'axios'
import { ElMessage } from 'element-plus'

const api = axios.create({
  baseURL: '/api',
  timeout: 30000,
  withCredentials: true
})

api.interceptors.request.use(
  (config) => {
    const token = sessionStorage.getItem('admin_authToken') || sessionStorage.getItem('user_authToken')
    if (token) {
      config.headers.Authorization = 'Bearer ' + token
    }
    return config
  },
  (error) => Promise.reject(error)
)

api.interceptors.response.use(
  (response) => {
    return response.data
  },
  (error) => {
    if (error.response?.status === 401) {
      const wasAdmin = sessionStorage.getItem('admin_authToken') !== null
      sessionStorage.removeItem('admin_user')
      sessionStorage.removeItem('admin_authToken')
      sessionStorage.removeItem('user_user')
      sessionStorage.removeItem('user_authToken')
      window.location.hash = wasAdmin ? '#/admin/login' : '#/login'
    }
    const msg = error.response?.data?.message || error.message || '请求失败'
    ElMessage.error(msg)
    return Promise.reject(error)
  }
)

export default api
