import api from './index'

export function createProject(data) {
  return api.post('/projects', data)
}

export function listProjects(params) {
  return api.get('/projects', { params })
}

export function getProject(id) {
  return api.get(`/projects/${id}`)
}

export function updateProject(id, data) {
  return api.put(`/projects/${id}`, data)
}

export function submitProject(id) {
  return api.put(`/projects/${id}/submit`)
}

export function withdrawProject(id) {
  return api.put(`/projects/${id}/withdraw`)
}

export function deleteProject(id) {
  return api.delete(`/projects/${id}`)
}

export function addMember(projectId, data) {
  return api.post(`/projects/${projectId}/members`, data)
}

export function updateMember(projectId, memberId, data) {
  return api.put(`/projects/${projectId}/members/${memberId}`, data)
}

export function deleteMember(projectId, memberId) {
  return api.delete(`/projects/${projectId}/members/${memberId}`)
}

export function addAdvisor(projectId, data) {
  return api.post(`/projects/${projectId}/advisors`, data)
}

export function updateAdvisor(projectId, advisorId, data) {
  return api.put(`/projects/${projectId}/advisors/${advisorId}`, data)
}

export function deleteAdvisor(projectId, advisorId) {
  return api.delete(`/projects/${projectId}/advisors/${advisorId}`)
}

export function uploadMaterial(projectId, materialType, file) {
  const formData = new FormData()
  formData.append('file', file)
  return api.post(`/projects/${projectId}/materials/${materialType}`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function deleteMaterial(projectId, materialId) {
  return api.delete(`/projects/${projectId}/materials/${materialId}`)
}
