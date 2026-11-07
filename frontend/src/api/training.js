import api from './index'

// ===================== 字典 / 配置 =====================
export function getDisciplines() {
  return api.get('/srtp/dict/disciplines')
}
export function getFlow(stage, batchId) {
  return api.get('/srtp/dict/flow', { params: { stage, batchId } })
}
export function getCustomFields(stage, batchId) {
  return api.get('/srtp/dict/custom-fields', { params: { stage, batchId } })
}
export function getBatchOptions() {
  return api.get('/srtp/dict/batches')
}
export function getTeacherOptions(keyword) {
  return api.get('/srtp/dict/teachers', { params: { keyword } })
}
export function getCategoryStats(stage, batchId) {
  return api.get('/srtp/admin/stats/by-category', { params: { stage, batchId } })
}

// ===================== 项目推荐/晋级 =====================
export function promoteProjects(toBatchId, projectIds) {
  return api.post('/srtp/admin/promotion', { toBatchId, projectIds })
}
export function getPromotedIds(fromBatchId) {
  return api.get('/srtp/admin/promotion/promoted-ids', { params: { fromBatchId } })
}

// 项目类别（大创三类）
export const PROJECT_CATEGORIES = [
  { value: 'A', label: 'A类·创新训练项目' },
  { value: 'B', label: 'B类·创业训练项目' },
  { value: 'C', label: 'C类·创业实践项目' }
]
export const CATEGORY_LABELS = { A: 'A类·创新训练项目', B: 'B类·创业训练项目', C: 'C类·创业实践项目' }

// ===================== 批次（管理员）=====================
export function listBatches(params) {
  return api.get('/srtp/admin/batches', { params })
}
export function getBatch(id) {
  return api.get(`/srtp/admin/batches/${id}`)
}
export function createBatch(data) {
  return api.post('/srtp/admin/batches', data)
}
export function updateBatch(id, data) {
  return api.put(`/srtp/admin/batches/${id}`, data)
}
export function deleteBatch(id) {
  return api.delete(`/srtp/admin/batches/${id}`)
}

// ===================== 专家库 =====================
export function applyExpert(data) {
  return api.post('/srtp/experts/apply', data)
}
export function listExperts(params) {
  return api.get('/srtp/admin/experts', { params })
}
export function addExpert(data) {
  return api.post('/srtp/admin/experts', data)
}
export function updateExpert(id, data) {
  return api.put(`/srtp/admin/experts/${id}`, data)
}
export function deleteExpert(id) {
  return api.delete(`/srtp/admin/experts/${id}`)
}
export function reviewExpert(id, approve, reason) {
  return api.post(`/srtp/admin/experts/${id}/review`, null, { params: { approve, reason } })
}

// ===================== 选题 =====================
export function createTopic(data) {
  return api.post('/srtp/topics', data)
}
export function updateTopic(id, data) {
  return api.put(`/srtp/topics/${id}`, data)
}
export function deleteTopic(id) {
  return api.delete(`/srtp/topics/${id}`)
}
export function submitTopic(id) {
  return api.post(`/srtp/topics/${id}/submit`)
}
export function myTopics(params) {
  return api.get('/srtp/topics/mine', { params })
}
export function getTopic(id) {
  return api.get(`/srtp/topics/${id}`)
}
export function claimableTopics(batchId) {
  return api.get('/srtp/topics/claimable', { params: { batchId } })
}
export function myClaimedTopics() {
  return api.get('/srtp/topics/claimed-mine')
}
export function claimTopic(id) {
  return api.post(`/srtp/topics/${id}/claim`)
}
export function publishTopic(data) {
  return api.post('/srtp/admin/topics/publish', data)
}
export function approveTopic(id, comment) {
  return api.post(`/srtp/topics/${id}/approve`, { comment })
}
export function rejectTopic(id, comment, targetStep) {
  return api.post(`/srtp/topics/${id}/reject`, { comment, targetStep })
}
export function adminTopics(params) {
  return api.get('/srtp/admin/topics', { params })
}
export function adminDeleteTopic(id) {
  return api.delete(`/srtp/admin/topics/${id}`)
}
export function adminDeleteTopicsBatch(ids) {
  return api.delete('/srtp/admin/topics/batch', { data: ids })
}

// ===================== 立项 =====================
export function createProject(data) {
  return api.post('/srtp/projects', data)
}
export function updateProject(id, data) {
  return api.put(`/srtp/projects/${id}`, data)
}
export function deleteProject(id) {
  return api.delete(`/srtp/projects/${id}`)
}
export function submitProject(id) {
  return api.post(`/srtp/projects/${id}/submit`)
}
export function getProject(id) {
  return api.get(`/srtp/projects/${id}`)
}
export function approveProject(id, comment) {
  return api.post(`/srtp/projects/${id}/approve`, { comment })
}
export function rejectProject(id, comment, targetStep) {
  return api.post(`/srtp/projects/${id}/reject`, { comment, targetStep })
}
export function assignExpert(projectId, expertId) {
  return api.post(`/srtp/admin/projects/${projectId}/assign-expert`, null, { params: { expertId } })
}
export function adminProjects(params) {
  return api.get('/srtp/admin/projects', { params })
}
export function myEligibleProjectsForReport(stage) {
  return api.get('/srtp/projects/mine-eligible-for-report', { params: { stage } })
}

// ===================== 中期 / 结题报告（stage = midterm | conclusion）=====================
export function createReport(stage, data) {
  return api.post(`/srtp/reports/${stage}`, data)
}
export function updateReport(stage, id, data) {
  return api.put(`/srtp/reports/${stage}/${id}`, data)
}
export function deleteReport(stage, id) {
  return api.delete(`/srtp/reports/${stage}/${id}`)
}
export function submitReport(stage, id) {
  return api.post(`/srtp/reports/${stage}/${id}/submit`)
}
export function myReports(stage, params) {
  return api.get(`/srtp/reports/${stage}/mine`, { params })
}
export function getReport(stage, id) {
  return api.get(`/srtp/reports/${stage}/${id}`)
}
export function approveReport(stage, id, comment) {
  return api.post(`/srtp/reports/${stage}/${id}/approve`, { comment })
}
export function rejectReport(stage, id, comment, targetStep) {
  return api.post(`/srtp/reports/${stage}/${id}/reject`, { comment, targetStep })
}
/** 结题"校专家结题评审"节点专用：通过 + 结题等级 grade (PASS/GOOD/EXCELLENT) */
export function gradeConclusion(id, grade, comment) {
  return api.post(`/srtp/reports/conclusion/${id}/approve`, { grade, comment })
}
export function exportConclusionGrades(batchId) {
  return downloadFile('/srtp/admin/export/conclusion-grades', { batchId }, '结题等级汇总.xlsx')
}
export function assignReportExpert(stage, reportId, expertId) {
  return api.post(`/srtp/admin/reports/${stage}/${reportId}/assign-expert`, null, { params: { expertId } })
}
export function listReportAssignments(stage, reportId) {
  return api.get(`/srtp/admin/reports/${stage}/${reportId}/assignments`)
}
export function removeReportAssignment(assignmentId) {
  return api.delete(`/srtp/admin/reports/assignments/${assignmentId}`)
}
export const CONCLUSION_GRADE_LABELS = {
  PASS: '同意结题——合格',
  GOOD: '同意结题——良好',
  EXCELLENT: '同意结题——优秀'
}
export function adminReports(stage, params) {
  return api.get(`/srtp/admin/reports/${stage}`, { params })
}

// ===================== 审批工作台 =====================
export function myIdentity() {
  return api.get('/srtp/my-identity')
}
export function myApprovals(stage) {
  return api.get('/srtp/my-approvals', { params: { stage } })
}

// ===================== 校/院级专家：顾问评审（不阻塞主审批流）=====================
export function myExpertAssignments() {
  return api.get('/srtp/expert/my-assignments')
}
export function submitExpertReview(id, action, score, comment) {
  return api.post(`/srtp/expert/assignments/${id}/review`, { action, score, comment })
}
export function finalizeConclusionExpertReview(id, comment) {
  return api.post(`/srtp/admin/reports/conclusion/${id}/finalize-expert-review`, { comment })
}
export function listProjectAssignments(projectId) {
  return api.get(`/srtp/admin/projects/${projectId}/assignments`)
}
export function removeProjectAssignment(assignmentId) {
  return api.delete(`/srtp/admin/projects/assignments/${assignmentId}`)
}

// ===================== 教师管理（管理员）=====================
export function listTeachers(keyword) {
  return api.get('/srtp/admin/teachers', { params: { keyword } })
}
export function createTeacher(name, workId) {
  return api.post('/srtp/admin/teachers', { name, workId })
}
export function importTeachers(file) {
  const fd = new FormData(); fd.append('file', file)
  return api.post('/srtp/admin/teachers/import', fd)
}
export function downloadTeacherTemplate() {
  return downloadFile('/srtp/admin/teachers/template', {}, '教师导入模板.xlsx')
}
export function deleteTeacher(userId) {
  return api.delete(`/srtp/admin/teachers/${userId}`)
}

// ===================== 大创账号：修改密码（清除首次改密标记）=====================
export function srtpChangePassword(oldPassword, newPassword) {
  return api.post('/srtp/account/change-password', { oldPassword, newPassword })
}

// ===================== 自定义字段管理（管理员）=====================
export function listCustomFieldsAdmin(stage, batchId) {
  return api.get('/srtp/admin/custom-fields', { params: { stage, batchId } })
}
export function saveCustomField(data) {
  return api.post('/srtp/admin/custom-fields', data)
}
export function deleteCustomField(id) {
  return api.delete(`/srtp/admin/custom-fields/${id}`)
}

// ===================== 全流程追踪 =====================
export function getTracking(params) {
  return api.get('/srtp/admin/tracking', { params })
}

// ===================== 附件 =====================
export function listAttachments(stage, targetId) {
  return api.get(`/srtp/attachments/${stage}/${targetId}`)
}
export function uploadAttachment(stage, targetId, file) {
  const fd = new FormData()
  fd.append('file', file)
  return api.post(`/srtp/attachments/${stage}/${targetId}`, fd)
}
export function deleteAttachment(id) {
  return api.delete(`/srtp/attachments/${id}`)
}

// ===================== 导出 / 下载 =====================
export function downloadFile(url, params, filename) {
  return api.get(url, { params, responseType: 'blob' }).then((blob) => {
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = filename
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(link.href)
  })
}
export function exportTopics(batchId) { return downloadFile('/srtp/admin/export/topics', { batchId }, '选题列表.xlsx') }
export function exportProjects(batchId) { return downloadFile('/srtp/admin/export/projects', { batchId }, '立项列表.xlsx') }
export function exportReports(stage, batchId) {
  return downloadFile(`/srtp/admin/export/reports/${stage}`, { batchId }, (stage === 'conclusion' ? '结题报告.xlsx' : '中期报告.xlsx'))
}
export function exportUnsubmitted(batchId) { return downloadFile('/srtp/admin/export/unsubmitted', { batchId }, '未提交选题人员.xlsx') }
export function downloadAttachment(id, filename) { return downloadFile(`/srtp/attachments/${id}/download`, {}, filename || 'file') }

// ===================== Excel 批量导入 =====================
export function importTopics(batchId, file) {
  const fd = new FormData(); fd.append('file', file)
  return api.post('/srtp/admin/import/topics', fd, { params: { batchId } })
}
export function importProjects(batchId, file) {
  const fd = new FormData(); fd.append('file', file)
  return api.post('/srtp/admin/import/projects', fd, { params: { batchId } })
}
export function downloadTopicTemplate() { return downloadFile('/srtp/admin/import/template/topics', {}, '选题导入模板.xlsx') }
export function downloadProjectTemplate() { return downloadFile('/srtp/admin/import/template/projects', {}, '立项导入模板.xlsx') }

// ===================== 公共常量 =====================
export const STATUS_LABELS = {
  DRAFT: '草稿', SUBMITTED: '已提交', IN_REVIEW: '审批中',
  APPROVED: '已通过', RETURNED: '已退回', REJECTED: '已拒绝'
}
export const STATUS_TYPES = {
  DRAFT: 'info', SUBMITTED: 'warning', IN_REVIEW: 'warning',
  APPROVED: 'success', RETURNED: 'danger', REJECTED: 'danger'
}
export const BATCH_STATUS_LABELS = {
  TOPIC_OPEN: '选题中', PROJECT_OPEN: '立项中', MIDTERM_OPEN: '中期中',
  CONCLUSION_OPEN: '结题中', CLOSED: '已结束'
}
