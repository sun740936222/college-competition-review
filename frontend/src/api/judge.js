import api from './index'

export function listJudgeProjects(params) {
  return api.get('/judge/projects', { params })
}

export function getJudgeProjectDetail(id) {
  return api.get(`/judge/projects/${id}`)
}

export function getJudgeProjectCriteria(id) {
  return api.get(`/judge/projects/${id}/criteria`)
}

export function submitJudgeScores(id, data) {
  return api.put(`/judge/projects/${id}/scores`, data)
}

export function getJudgeFileUrl(materialId) {
  const token = sessionStorage.getItem('user_authToken') || sessionStorage.getItem('admin_authToken') || ''
  return `/api/judge/files/${materialId}?token=${encodeURIComponent(token)}`
}

export function fetchJudgePdf(materialId) {
  const token = sessionStorage.getItem('user_authToken') || sessionStorage.getItem('admin_authToken') || ''
  return api.get(`/judge/files/${materialId}`, {
    responseType: 'arraybuffer',
    params: { token }
  })
}

export function getJudgeEventAttachmentUrl(eventId) {
  const token = sessionStorage.getItem('user_authToken') || sessionStorage.getItem('admin_authToken') || ''
  return `/api/judge/events/${eventId}/attachment?token=${encodeURIComponent(token)}`
}
