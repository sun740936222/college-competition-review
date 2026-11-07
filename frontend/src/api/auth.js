import api from './index'

export function loginApi(username, password, role) {
  return api.post('/auth/login', { username, password, role })
}

// 教师/专家登录（大创独立身份）
export function teacherLoginApi(username, password) {
  return api.post('/srtp/auth/teacher-login', { username, password, role: 'TEACHER' })
}

export function registerApi(data) {
  return api.post('/auth/register', data)
}

export function registerNationalApi(data) {
  return api.post('/auth/register/national', data)
}

export function registerProvinceApi(data) {
  return api.post('/auth/register/province', data)
}

export function registerSchoolApi(data) {
  return api.post('/auth/register/school', data)
}

export function logoutApi() {
  return api.post('/auth/logout')
}

export function getMeApi() {
  return api.get('/auth/me')
}

export function getOrganizations() {
  return api.get('/organizations')
}

export function searchOrganizations(keyword) {
  return api.get('/organizations/search', { params: { keyword } })
}

export function updateProfile(organizationId, organizationName) {
  return api.put('/auth/profile', { organizationId, organizationName })
}

export function changePassword(oldPassword, newPassword) {
  return api.put('/auth/password', { oldPassword, newPassword })
}
