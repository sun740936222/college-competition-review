import api from './index'

// ===== Events =====
export function listAllEvents(params) {
  return api.get('/events/admin/all', { params })
}

export function createEvent(data) {
  return api.post('/events/admin', data)
}

export function updateEvent(id, data) {
  return api.put(`/events/admin/${id}`, data)
}

export function deleteEvent(id) {
  return api.delete(`/events/admin/${id}`)
}

// ===== Organizations =====
export function listAllOrgs(params) {
  return api.get('/organizations/admin/all', { params })
}

export function createOrg(data) {
  return api.post('/organizations/admin', data)
}

export function updateOrg(id, data) {
  return api.put(`/organizations/admin/${id}`, data)
}

export function deleteOrg(id) {
  return api.delete(`/organizations/admin/${id}`)
}

export function batchDeleteOrgs(orgIds) {
  return api.post('/organizations/admin/batch-delete', orgIds)
}

export function createOrgAccount(orgId, data) {
  return api.post(`/organizations/admin/${orgId}/accounts`, data)
}

export function lookupProvinces() {
  return api.get('/organizations/admin/province-lookup')
}

export function batchCreateColleges(data) {
  return api.post('/organizations/admin/batch-create', data)
}

export function listOrgChildren(parentId) {
  return api.get(`/organizations/admin/${parentId}/children`)
}

// ===== Projects =====
export function listAdminProjects(params) {
  return api.get('/projects/admin/list', { params })
}

export function getAdminProject(id) {
  return api.get(`/projects/admin/${id}`)
}

export function reviewProject(id, data) {
  return api.put(`/projects/admin/${id}/review`, data)
}

export function batchReviewProjects(data) {
  return api.post('/projects/admin/batch-review', data)
}

// ===== Competition Templates =====
export function getCompetitionTemplates() {
  return api.get('/events/admin/competition-templates')
}

// ===== Tracks =====
export function getTrackTemplates() {
  return api.get('/events/admin/track-templates')
}

export function getTracks(eventId) {
  return api.get(`/events/admin/${eventId}/tracks`)
}

export function saveTracks(eventId, tracks) {
  return api.put(`/events/admin/${eventId}/tracks`, tracks)
}

// ===== Judges =====
export function listJudges(params) {
  return api.get('/auth/judges', { params })
}

export function createJudge(data) {
  return api.post('/auth/judges', data)
}

export function deleteJudge(id) {
  return api.delete(`/auth/judges/${id}`)
}

// ===== Invite Code (both SUPER_ADMIN and UNIT_ADMIN) =====
export function getMyOrganization() {
  return api.get('/organizations/admin/my-org')
}

export function regenerateMyInviteCode() {
  return api.put('/organizations/admin/my-invite-code/regenerate')
}

// ===== Score Criteria (SUPER_ADMIN only) =====
export function getTrackCriteria(trackId) {
  return api.get(`/reviews/admin/tracks/${trackId}/criteria`)
}

export function saveTrackCriteria(trackId, data) {
  return api.put(`/reviews/admin/tracks/${trackId}/criteria`, data)
}

export function deleteCriteria(criteriaId) {
  return api.delete(`/reviews/admin/criteria/${criteriaId}`)
}

export function uploadEventAttachment(eventId, file) {
  const form = new FormData()
  form.append('file', file)
  return api.post(`/reviews/admin/events/${eventId}/attachment`, form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function deleteEventAttachment(eventId) {
  return api.delete(`/reviews/admin/events/${eventId}/attachment`)
}

export function getEventAttachmentUrl(eventId) {
  const token = sessionStorage.getItem('admin_authToken') || sessionStorage.getItem('user_authToken') || ''
  return `/api/reviews/admin/events/${eventId}/attachment?token=${encodeURIComponent(token)}`
}

// ===== Judge Allocation (SUPER_ADMIN only) =====
export function allocateJudges(data) {
  return api.post('/reviews/admin/allocate', data)
}

export function listAllocations(params) {
  return api.get('/reviews/admin/allocations', { params })
}

export function removeAllocation(id) {
  return api.delete(`/reviews/admin/allocations/${id}`)
}

// ===== Review Progress (SUPER_ADMIN only) =====
export function getReviewProgress(eventId) {
  return api.get(`/reviews/admin/events/${eventId}/progress`)
}

// ===== Score Summary (SUPER_ADMIN & UNIT_ADMIN with visibility) =====
export function getScoreSummary(eventId, params) {
  return api.get(`/reviews/admin/events/${eventId}/scores`, { params })
}

// ===== Score Detail (per-judge per-criteria) =====
export function getProjectScoreDetail(eventId, projectId) {
  return api.get(`/reviews/admin/events/${eventId}/projects/${projectId}/scores`)
}

// ===== Materials Cleanup (SUPER_ADMIN only) =====
export function clearEventMaterials(eventId) {
  return api.delete(`/reviews/admin/events/${eventId}/materials`)
}

export function listOrgAccounts(orgId, params) {
  return api.get(`/organizations/admin/${orgId}/accounts`, { params })
}

export function batchAllocateByTrack(data) {
  return api.post('/reviews/admin/allocate/by-track', data)
}

// ===== Event Children =====
export function listEventChildren(eventId) {
  return api.get(`/events/admin/${eventId}/children`)
}

export function deleteEventChildren(eventId) {
  return api.delete(`/events/admin/${eventId}/children`)
}

// ===== Excel Export =====
export function exportScores(eventId) {
  return api.get(`/reviews/admin/events/${eventId}/export/scores`, { responseType: 'blob' })
}
export function exportProjects(eventId) {
  return api.get(`/reviews/admin/events/${eventId}/export/projects`, { responseType: 'blob' })
}
export function exportAllocations(eventId) {
  return api.get(`/reviews/admin/events/${eventId}/export/allocations`, { responseType: 'blob' })
}

export function downloadBlob(data, filename) {
  const blob = new Blob([data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  window.URL.revokeObjectURL(url)
}
