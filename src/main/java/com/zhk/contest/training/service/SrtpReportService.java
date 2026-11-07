package com.zhk.contest.training.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhk.contest.entity.User;
import com.zhk.contest.exception.BusinessException;
import com.zhk.contest.mapper.OrganizationMapper;
import com.zhk.contest.mapper.UserMapper;
import com.zhk.contest.training.dto.request.SrtpReportSaveRequest;
import com.zhk.contest.training.entity.SrtpApprovalFlowConfig;
import com.zhk.contest.training.entity.SrtpConclusionReport;
import com.zhk.contest.training.entity.SrtpExpert;
import com.zhk.contest.training.entity.SrtpExpertAssignment;
import com.zhk.contest.training.entity.SrtpMidtermReport;
import com.zhk.contest.training.entity.SrtpProject;
import com.zhk.contest.training.enums.ApproverRole;
import com.zhk.contest.training.enums.SrtpRecordStatus;
import com.zhk.contest.training.enums.SrtpStage;
import com.zhk.contest.training.mapper.SrtpConclusionReportMapper;
import com.zhk.contest.training.mapper.SrtpExpertAssignmentMapper;
import com.zhk.contest.training.mapper.SrtpExpertMapper;
import com.zhk.contest.training.mapper.SrtpMidtermReportMapper;
import com.zhk.contest.training.mapper.SrtpProjectMapper;
import com.zhk.contest.util.AdminLevelResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 中期 / 结题报告服务：两阶段结构一致，用同一套逻辑按 stage 分发到对应表。
 * 复用审批引擎、自定义字段、成员/教师子表。
 */
@Service
public class SrtpReportService {

    private final SrtpMidtermReportMapper midtermMapper;
    private final SrtpConclusionReportMapper conclusionMapper;
    private final SrtpProjectMapper projectMapper;
    private final com.zhk.contest.training.mapper.SrtpBatchMapper batchMapper;
    private final SrtpExpertMapper expertMapper;
    private final SrtpExpertAssignmentMapper assignmentMapper;
    private final SrtpApprovalEngine approvalEngine;
    private final SrtpCustomFieldService customFieldService;
    private final SrtpParticipantService participantService;
    private final UserMapper userMapper;
    private final OrganizationMapper organizationMapper;

    public SrtpReportService(SrtpMidtermReportMapper midtermMapper,
                             SrtpConclusionReportMapper conclusionMapper,
                             SrtpProjectMapper projectMapper,
                             com.zhk.contest.training.mapper.SrtpBatchMapper batchMapper,
                             SrtpExpertMapper expertMapper,
                             SrtpExpertAssignmentMapper assignmentMapper,
                             SrtpApprovalEngine approvalEngine,
                             SrtpCustomFieldService customFieldService,
                             SrtpParticipantService participantService,
                             UserMapper userMapper,
                             OrganizationMapper organizationMapper) {
        this.midtermMapper = midtermMapper;
        this.conclusionMapper = conclusionMapper;
        this.projectMapper = projectMapper;
        this.batchMapper = batchMapper;
        this.expertMapper = expertMapper;
        this.assignmentMapper = assignmentMapper;
        this.approvalEngine = approvalEngine;
        this.customFieldService = customFieldService;
        this.participantService = participantService;
        this.userMapper = userMapper;
        this.organizationMapper = organizationMapper;
    }

    @Transactional
    public SrtpReportRecord create(Long userId, SrtpStage stage, SrtpReportSaveRequest req) {
        requireReportStage(stage);
        SrtpProject project = projectMapper.selectById(req.getProjectId());
        if (project == null) throw new BusinessException("关联项目不存在");

        SrtpReportRecord r = newRecord(stage);
        r.setProjectId(project.getId());
        r.setBatchId(project.getBatchId());
        r.setCreatedBy(userId);
        // 提交时快照项目核心字段
        r.setSnapshotName(project.getName());
        r.setDisciplineL1Id(project.getDisciplineL1Id());
        r.setDisciplineL2Id(project.getDisciplineL2Id());
        // 项目类别也直接沿用立项时的选择，避免学生在中期/结题重复填写
        r.setProjectType(project.getProjectType());
        applyEditable(r, req);
        r.setStatus(SrtpRecordStatus.DRAFT);
        r.setCurrentStep(0);
        insert(stage, r);
        // 团队成员与指导教师直接从关联立项继承，不接受学生在中期/结题重复录入
        int copied = participantService.copyFromProject(project.getId(), stage, r.getId());
        r.setStudentCount(copied);
        update(stage, r);
        return r;
    }

    @Transactional
    public SrtpReportRecord update(Long userId, SrtpStage stage, Long id, SrtpReportSaveRequest req) {
        SrtpReportRecord r = requireExisting(stage, id);
        if (r.getStatus() != SrtpRecordStatus.DRAFT && r.getStatus() != SrtpRecordStatus.RETURNED) {
            throw new BusinessException("仅草稿或已退回的报告可编辑");
        }
        applyEditable(r, req);
        // 成员/指导教师由立项继承，中期/结题编辑时保持不变；不消费 req.getMembers()/getAdvisors()
        // 但仍保留 studentCount 与当前成员表的一致性
        r.setStudentCount(participantService.listMembers(stage, id).size());
        update(stage, r);
        return r;
    }

    @Transactional
    public void delete(Long userId, SrtpStage stage, Long id) {
        SrtpReportRecord r = requireExisting(stage, id);
        if (r.getStatus() != SrtpRecordStatus.DRAFT) throw new BusinessException("仅草稿报告可删除");
        if (stage == SrtpStage.MIDTERM) midtermMapper.deleteById(id);
        else conclusionMapper.deleteById(id);
    }

    @Transactional
    public SrtpReportRecord submit(Long userId, SrtpStage stage, Long id) {
        SrtpReportRecord r = requireExisting(stage, id);
        participantService.requireBoundAdvisor(stage, id);
        // 中期/结题截止时间校验：过时不可提交
        if (r.getBatchId() != null) {
            com.zhk.contest.training.entity.SrtpBatch batch = batchMapper.selectById(r.getBatchId());
            if (batch != null) {
                java.time.LocalDateTime deadline = stage == SrtpStage.MIDTERM
                        ? batch.getMidtermDeadline() : batch.getConclusionDeadline();
                String stageLabel = stage == SrtpStage.MIDTERM ? "中期报告" : "结题报告";
                if (deadline != null && java.time.LocalDateTime.now().isAfter(deadline)) {
                    throw new BusinessException("本批次" + stageLabel + "截止时间为 "
                            + deadline.toString().replace('T', ' ')
                            + "，已过截止时间，无法再提交" + stageLabel);
                }
            }
        }
        customFieldService.validate(stage, r.getBatchId(), r.getExtraData());
        approvalEngine.submit(stage, r);
        update(stage, r);
        return r;
    }

    @Transactional
    public SrtpReportRecord approve(Long approverId, SrtpStage stage, Long id, String comment, String grade) {
        // grade 参数不再由主审批 approve 路径消费；校专家现在通过 SrtpExpertReviewService 独立打分/评审，
        // 学校管理员通过 finalizeConclusionExpertReview() 或 finalizeReject() 汇总后推进/驳回主流程。
        SrtpReportRecord r = requireExisting(stage, id);
        approvalEngine.approve(stage, r, approverId, comment, orgOfProject(r));
        update(stage, r);
        return r;
    }

    /**
     * 学校管理员在结题"校专家结题评审"节点汇总专家意见后收尾：
     * 至少要求 1 位专家已提交评审；从所有已完成评审的专家评分取平均分推导等级；
     * 若任一专家给"不同意"或平均分低于 60，则整体驳回申报人；否则通过推进到"学校结题审核"，并把 grade 写回结题记录。
     */
    @Transactional
    public String finalizeConclusionExpertReview(Long adminId, Long reportId, String finalComment) {
        requireAdmin(adminId);
        SrtpReportRecord r = requireExisting(SrtpStage.CONCLUSION, reportId);
        com.zhk.contest.training.entity.SrtpApprovalFlowConfig node = approvalEngine.currentNode(SrtpStage.CONCLUSION, r);
        if (node == null || node.getApproverRole() != com.zhk.contest.training.enums.ApproverRole.SCHOOL_EXPERT) {
            throw new BusinessException("当前节点不是「校专家结题评审」，无需在此收尾");
        }
        List<SrtpExpertAssignment> all = assignmentMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<SrtpExpertAssignment>().lambda()
                .eq(SrtpExpertAssignment::getStage, SrtpStage.CONCLUSION)
                .eq(SrtpExpertAssignment::getTargetId, reportId));
        if (all.isEmpty()) throw new BusinessException("请先指派至少一位校专家评审后再收尾");
        List<SrtpExpertAssignment> done = all.stream()
                .filter(a -> !"PENDING".equals(a.getReviewStatus()) && a.getScore() != null)
                .toList();
        if (done.isEmpty()) throw new BusinessException("目前尚无专家提交评审，不能收尾");

        boolean anyReject = done.stream().anyMatch(a -> "REJECTED".equals(a.getReviewStatus()));
        double avg = done.stream().mapToInt(SrtpExpertAssignment::getScore).average().orElse(0);
        String grade;
        boolean pass;
        if (anyReject || avg < 60) {
            pass = false; grade = null;
        } else if (avg >= 85) {
            pass = true; grade = "EXCELLENT";
        } else if (avg >= 75) {
            pass = true; grade = "GOOD";
        } else {
            pass = true; grade = "PASS";
        }

        String note = String.format("汇总 %d 位专家评审(平均分 %.1f)：%s",
                done.size(), avg, pass ? ("同意结题——" + gradeLabel(grade)) : "不同意结题");
        if (finalComment != null && !finalComment.isBlank()) note += "；" + finalComment;

        if (pass) {
            if (r instanceof com.zhk.contest.training.entity.SrtpConclusionReport c) {
                c.setConclusionGrade(grade);
            }
            approvalEngine.approveBypassPermission(SrtpStage.CONCLUSION, r, adminId, note);
        } else {
            approvalEngine.rejectToBypassPermission(SrtpStage.CONCLUSION, r, adminId, note, 0);
        }
        update(SrtpStage.CONCLUSION, r);
        return note;
    }

    private String gradeLabel(String g) {
        return switch (g == null ? "" : g) {
            case "PASS" -> "合格"; case "GOOD" -> "良好"; case "EXCELLENT" -> "优秀"; default -> g;
        };
    }

    @Transactional
    public SrtpReportRecord reject(Long approverId, SrtpStage stage, Long id, String comment, Integer targetStep) {
        SrtpReportRecord r = requireExisting(stage, id);
        approvalEngine.rejectTo(stage, r, approverId, comment, targetStep, orgOfProject(r));
        update(stage, r);
        return r;
    }

    public SrtpReportRecord get(SrtpStage stage, Long id) {
        return requireExisting(stage, id);
    }

    public IPage<? extends SrtpReportRecord> listMine(Long userId, SrtpStage stage, Page<?> page) {
        if (stage == SrtpStage.MIDTERM) {
            return midtermMapper.selectPage(castPage(page), new QueryWrapper<SrtpMidtermReport>().lambda()
                    .eq(SrtpMidtermReport::getCreatedBy, userId)
                    .orderByDesc(SrtpMidtermReport::getCreatedAt));
        }
        return conclusionMapper.selectPage(castPage(page), new QueryWrapper<SrtpConclusionReport>().lambda()
                .eq(SrtpConclusionReport::getCreatedBy, userId)
                .orderByDesc(SrtpConclusionReport::getCreatedAt));
    }

    public IPage<? extends SrtpReportRecord> listForAdmin(Long adminId, SrtpStage stage, Page<?> page,
                                                          Long batchId, SrtpRecordStatus status, String keyword) {
        User admin = requireAdmin(adminId);
        boolean isSuper = AdminLevelResolver.isSuperAdmin(admin);
        // 中期/结题报告没有自己的 org_id，按关联立项的 org_id 归属；非超级管理员按可见组织树先收窄到本范围内的立项 id
        List<Long> projectIdsInScope = null;
        if (!isSuper) {
            List<Long> visibleOrgIds = AdminLevelResolver.getVisibleOrgIds(admin, organizationMapper);
            if (visibleOrgIds.isEmpty()) return emptyPage(page);
            projectIdsInScope = projectMapper.selectList(new QueryWrapper<SrtpProject>().lambda()
                            .in(SrtpProject::getOrgId, visibleOrgIds)
                            .select(SrtpProject::getId)).stream()
                    .map(SrtpProject::getId).collect(Collectors.toList());
            if (projectIdsInScope.isEmpty()) return emptyPage(page);
        }

        List<SrtpReportRecord> all;
        if (stage == SrtpStage.MIDTERM) {
            var w = new QueryWrapper<SrtpMidtermReport>().lambda();
            if (batchId != null) w.eq(SrtpMidtermReport::getBatchId, batchId);
            if (status != null) w.eq(SrtpMidtermReport::getStatus, status);
            if (keyword != null && !keyword.isBlank()) w.like(SrtpMidtermReport::getSnapshotName, keyword);
            if (projectIdsInScope != null) w.in(SrtpMidtermReport::getProjectId, projectIdsInScope);
            w.orderByDesc(SrtpMidtermReport::getCreatedAt);
            if (isSuper) return midtermMapper.selectPage(castPage(page), w);
            all = new ArrayList<>(midtermMapper.selectList(w));
        } else {
            var w = new QueryWrapper<SrtpConclusionReport>().lambda();
            if (batchId != null) w.eq(SrtpConclusionReport::getBatchId, batchId);
            if (status != null) w.eq(SrtpConclusionReport::getStatus, status);
            if (keyword != null && !keyword.isBlank()) w.like(SrtpConclusionReport::getSnapshotName, keyword);
            if (projectIdsInScope != null) w.in(SrtpConclusionReport::getProjectId, projectIdsInScope);
            w.orderByDesc(SrtpConclusionReport::getCreatedAt);
            if (isSuper) return conclusionMapper.selectPage(castPage(page), w);
            all = new ArrayList<>(conclusionMapper.selectList(w));
        }

        // 学院/学校管理员：审批中的报告仅在轮到自己这一级审核时才展示
        ApproverRole myRole = adminApproverRole(admin);
        List<SrtpReportRecord> matched = all.stream()
                .filter(r -> r.getStatus() != SrtpRecordStatus.IN_REVIEW || isMyTurn(stage, r, myRole))
                .collect(Collectors.toList());
        int size = (int) page.getSize();
        int from = (int) ((page.getCurrent() - 1) * size);
        List<SrtpReportRecord> records = from >= matched.size() ? List.of()
                : matched.subList(from, Math.min(matched.size(), from + size));
        Page<SrtpReportRecord> result = new Page<>(page.getCurrent(), page.getSize(), matched.size());
        result.setRecords(records);
        return result;
    }

    private <T> Page<T> emptyPage(Page<?> page) {
        return new Page<>(page.getCurrent(), page.getSize());
    }

    private ApproverRole adminApproverRole(User admin) {
        com.zhk.contest.enums.OrganizationLevel level = AdminLevelResolver.getLevel(admin, organizationMapper);
        if (level == com.zhk.contest.enums.OrganizationLevel.COLLEGE) return ApproverRole.COLLEGE_ADMIN;
        if (level == com.zhk.contest.enums.OrganizationLevel.SCHOOL) return ApproverRole.SCHOOL_ADMIN;
        return null;
    }

    private boolean isMyTurn(SrtpStage stage, SrtpReportRecord r, ApproverRole myRole) {
        if (myRole == null) return false;
        SrtpApprovalFlowConfig node = approvalEngine.currentNode(stage, r);
        return node != null && node.getApproverRole() == myRole;
    }

    // ===== 校专家结题评审：管理员指派专家 =====

    /** 给某个结题报告指派一位校/院级专家；专家必须已在专家库中且状态 APPROVED；不重复指派同一专家 */
    @Transactional
    public void assignReportExpert(Long adminId, SrtpStage stage, Long reportId, Long expertId) {
        requireAdmin(adminId);
        if (stage != SrtpStage.CONCLUSION && stage != SrtpStage.MIDTERM) {
            throw new BusinessException("目前只支持中期/结题阶段指派评审专家");
        }
        SrtpReportRecord r = requireExisting(stage, reportId);
        SrtpExpert expert = expertMapper.selectById(expertId);
        if (expert == null || expert.getStatus() == null
                || !"APPROVED".equals(expert.getStatus().name())) {
            throw new BusinessException("专家不存在或尚未通过审核");
        }
        long dup = assignmentMapper.selectCount(new QueryWrapper<SrtpExpertAssignment>().lambda()
                .eq(SrtpExpertAssignment::getStage, stage)
                .eq(SrtpExpertAssignment::getTargetId, r.getId())
                .eq(SrtpExpertAssignment::getExpertId, expertId));
        if (dup > 0) {
            throw new BusinessException("该专家已被指派到本" + (stage == SrtpStage.MIDTERM ? "中期" : "结题") + "报告，请勿重复指派");
        }
        SrtpExpertAssignment a = new SrtpExpertAssignment();
        a.setStage(stage);
        a.setTargetId(r.getId());
        a.setExpertId(expertId);
        a.setAssignedBy(adminId);
        a.setReviewStatus("PENDING");
        assignmentMapper.insert(a);
    }

    /** 列出本报告已指派的专家（含评审状态、意见），供管理员查看/管理 */
    public List<java.util.Map<String, Object>> listReportAssignments(Long adminId, SrtpStage stage, Long reportId) {
        requireAdmin(adminId);
        List<SrtpExpertAssignment> assignments = assignmentMapper.selectList(
                new QueryWrapper<SrtpExpertAssignment>().lambda()
                        .eq(SrtpExpertAssignment::getStage, stage)
                        .eq(SrtpExpertAssignment::getTargetId, reportId)
                        .orderByAsc(SrtpExpertAssignment::getCreatedAt));
        List<java.util.Map<String, Object>> out = new ArrayList<>();
        for (SrtpExpertAssignment a : assignments) {
            SrtpExpert e = expertMapper.selectById(a.getExpertId());
            java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("id", a.getId());
            m.put("expertId", a.getExpertId());
            m.put("expertName", e != null ? e.getName() : null);
            m.put("expertLevel", e != null && e.getExpertLevel() != null ? e.getExpertLevel().name() : null);
            m.put("expertTitle", e != null ? e.getTitle() : null);
            m.put("reviewStatus", a.getReviewStatus());
            m.put("assignedAt", a.getCreatedAt());
            out.add(m);
        }
        return out;
    }

    /** 撤销某条指派（若专家尚未评审）—— 允许 school-admin 换人 */
    @Transactional
    public void removeReportAssignment(Long adminId, Long assignmentId) {
        requireAdmin(adminId);
        SrtpExpertAssignment a = assignmentMapper.selectById(assignmentId);
        if (a == null) return;
        if (a.getReviewStatus() != null && !"PENDING".equals(a.getReviewStatus())) {
            throw new BusinessException("该专家已完成评审，无法撤销指派");
        }
        assignmentMapper.deleteById(assignmentId);
    }

    // ===== 内部 =====

    @SuppressWarnings("unchecked")
    private <T> Page<T> castPage(Page<?> page) {
        return (Page<T>) page;
    }

    private void requireReportStage(SrtpStage stage) {
        if (stage != SrtpStage.MIDTERM && stage != SrtpStage.CONCLUSION) {
            throw new BusinessException("阶段参数错误，仅支持中期/结题");
        }
    }

    private SrtpReportRecord newRecord(SrtpStage stage) {
        return stage == SrtpStage.MIDTERM ? new SrtpMidtermReport() : new SrtpConclusionReport();
    }

    private void insert(SrtpStage stage, SrtpReportRecord r) {
        if (stage == SrtpStage.MIDTERM) midtermMapper.insert((SrtpMidtermReport) r);
        else conclusionMapper.insert((SrtpConclusionReport) r);
    }

    private void update(SrtpStage stage, SrtpReportRecord r) {
        if (stage == SrtpStage.MIDTERM) midtermMapper.updateById((SrtpMidtermReport) r);
        else conclusionMapper.updateById((SrtpConclusionReport) r);
    }

    private SrtpReportRecord requireExisting(SrtpStage stage, Long id) {
        requireReportStage(stage);
        SrtpReportRecord r = (id == null) ? null
                : (stage == SrtpStage.MIDTERM ? midtermMapper.selectById(id) : conclusionMapper.selectById(id));
        if (r == null) throw new BusinessException("报告不存在");
        return r;
    }

    private void applyEditable(SrtpReportRecord r, SrtpReportSaveRequest req) {
        // 项目类别由立项继承，不取用户输入；此处只写入学生可编辑的报告正文/自定义字段
        r.setDescription(req.getDescription());
        r.setExtraData(customFieldService.toJson(req.getExtraData()));
    }

    private Long orgOfProject(SrtpReportRecord r) {
        SrtpProject p = projectMapper.selectById(r.getProjectId());
        return p != null ? p.getOrgId() : null;
    }

    private User requireAdmin(Long adminId) {
        User u = (adminId == null) ? null : userMapper.selectById(adminId);
        if (u == null) throw new BusinessException(403, "无权操作");
        AdminLevelResolver.requireAtLeast(u, com.zhk.contest.enums.OrganizationLevel.COLLEGE, organizationMapper);
        return u;
    }
}
