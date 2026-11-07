package com.zhk.contest.training.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhk.contest.entity.User;
import com.zhk.contest.exception.BusinessException;
import com.zhk.contest.mapper.OrganizationMapper;
import com.zhk.contest.mapper.UserMapper;
import com.zhk.contest.training.dto.SrtpAdvisorDTO;
import com.zhk.contest.training.dto.request.SrtpProjectSaveRequest;
import com.zhk.contest.training.entity.SrtpApprovalFlowConfig;
import com.zhk.contest.training.entity.SrtpBatch;
import com.zhk.contest.training.entity.SrtpExpertAssignment;
import com.zhk.contest.training.entity.SrtpOrgCode;
import com.zhk.contest.training.entity.SrtpProject;
import com.zhk.contest.training.entity.SrtpTopic;
import com.zhk.contest.training.enums.ApproverRole;
import com.zhk.contest.training.enums.SrtpRecordStatus;
import com.zhk.contest.training.enums.SrtpStage;
import com.zhk.contest.training.mapper.SrtpBatchMapper;
import com.zhk.contest.training.mapper.SrtpExpertAssignmentMapper;
import com.zhk.contest.training.mapper.SrtpExpertMapper;
import com.zhk.contest.training.mapper.SrtpOrgCodeMapper;
import com.zhk.contest.training.mapper.SrtpProjectMapper;
import com.zhk.contest.training.mapper.SrtpTopicMapper;
import com.zhk.contest.util.AdminLevelResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** 立项管理：申报、提交、审批（通过时自动生成项目编号）、校级专家分配、列表、详情。 */
@Service
public class SrtpProjectService {

    private static final SrtpStage STAGE = SrtpStage.PROJECT;

    private final SrtpProjectMapper projectMapper;
    private final SrtpBatchMapper batchMapper;
    private final SrtpOrgCodeMapper orgCodeMapper;
    private final SrtpExpertAssignmentMapper assignmentMapper;
    private final SrtpExpertMapper expertMapper;
    private final SrtpTopicMapper topicMapper;
    private final SrtpApprovalEngine approvalEngine;
    private final SrtpCustomFieldService customFieldService;
    private final SrtpParticipantService participantService;
    private final UserMapper userMapper;
    private final OrganizationMapper organizationMapper;

    public SrtpProjectService(SrtpProjectMapper projectMapper,
                              SrtpBatchMapper batchMapper,
                              SrtpOrgCodeMapper orgCodeMapper,
                              SrtpExpertAssignmentMapper assignmentMapper,
                              SrtpExpertMapper expertMapper,
                              SrtpTopicMapper topicMapper,
                              SrtpApprovalEngine approvalEngine,
                              SrtpCustomFieldService customFieldService,
                              SrtpParticipantService participantService,
                              UserMapper userMapper,
                              OrganizationMapper organizationMapper) {
        this.projectMapper = projectMapper;
        this.batchMapper = batchMapper;
        this.orgCodeMapper = orgCodeMapper;
        this.assignmentMapper = assignmentMapper;
        this.expertMapper = expertMapper;
        this.topicMapper = topicMapper;
        this.approvalEngine = approvalEngine;
        this.customFieldService = customFieldService;
        this.participantService = participantService;
        this.userMapper = userMapper;
        this.organizationMapper = organizationMapper;
    }

    @Transactional
    public SrtpProject create(Long userId, SrtpProjectSaveRequest req) {
        SrtpProject p = new SrtpProject();
        apply(p, req);
        p.setStatus(SrtpRecordStatus.DRAFT);
        p.setCurrentStep(0);
        p.setCreatedBy(userId);
        if (p.getOrgId() == null) p.setOrgId(orgOf(userId));
        projectMapper.insert(p);
        saveParticipants(p, req);
        projectMapper.updateById(p); // 持久化按成员数量同步得到的 studentCount
        return p;
    }

    @Transactional
    public SrtpProject update(Long userId, Long id, SrtpProjectSaveRequest req) {
        SrtpProject p = requireExisting(id);
        if (p.getStatus() != SrtpRecordStatus.DRAFT && p.getStatus() != SrtpRecordStatus.RETURNED) {
            throw new BusinessException("仅草稿或已退回的立项可编辑");
        }
        apply(p, req);
        saveParticipants(p, req);
        projectMapper.updateById(p); // 同一次 update 里同时保存基本字段和自动同步的 studentCount
        return p;
    }

    @Transactional
    public void delete(Long userId, Long id) {
        SrtpProject p = requireExisting(id);
        if (p.getStatus() != SrtpRecordStatus.DRAFT) {
            throw new BusinessException("仅草稿立项可删除");
        }
        projectMapper.deleteById(id);
    }

    @Transactional
    public SrtpProject submit(Long userId, Long id) {
        SrtpProject p = requireExisting(id);
        if (p.getProjectType() == null || p.getProjectType().isBlank()) {
            throw new BusinessException("请先选择项目类别后再提交");
        }
        participantService.requireBoundAdvisor(STAGE, id);
        // 立项截止时间校验：过时不可提交
        if (p.getBatchId() != null) {
            SrtpBatch batch = batchMapper.selectById(p.getBatchId());
            if (batch != null && batch.getProjectDeadline() != null
                    && java.time.LocalDateTime.now().isAfter(batch.getProjectDeadline())) {
                throw new BusinessException("本批次立项截止时间为 "
                        + batch.getProjectDeadline().toString().replace('T', ' ')
                        + "，已过截止时间，无法再提交立项");
            }
        }
        customFieldService.validate(STAGE, p.getBatchId(), p.getExtraData());
        approvalEngine.submit(STAGE, p);
        projectMapper.updateById(p);
        return p;
    }

    @Transactional
    public SrtpProject approve(Long approverId, Long id, String comment) {
        SrtpProject p = requireExisting(id);
        approvalEngine.approve(STAGE, p, approverId, comment, p.getOrgId());
        // 立项通过时生成项目编号
        if (p.getStatus() == SrtpRecordStatus.APPROVED
                && (p.getProjectNo() == null || p.getProjectNo().isBlank())) {
            p.setProjectNo(generateProjectNo(p));
        }
        projectMapper.updateById(p);
        return p;
    }

    @Transactional
    public SrtpProject reject(Long approverId, Long id, String comment, Integer targetStep) {
        SrtpProject p = requireExisting(id);
        approvalEngine.rejectTo(STAGE, p, approverId, comment, targetStep, p.getOrgId());
        projectMapper.updateById(p);
        return p;
    }

    /** 分配校级专家（对应"分配校级专家"环节）；同一位专家不重复分配 */
    @Transactional
    public void assignExpert(Long adminId, Long projectId, Long expertId) {
        requireAdmin(adminId);
        SrtpProject p = requireExisting(projectId);
        long dup = assignmentMapper.selectCount(new QueryWrapper<SrtpExpertAssignment>().lambda()
                .eq(SrtpExpertAssignment::getStage, STAGE)
                .eq(SrtpExpertAssignment::getTargetId, p.getId())
                .eq(SrtpExpertAssignment::getExpertId, expertId));
        if (dup > 0) {
            throw new BusinessException("该专家已被分配到本项目，请勿重复分配");
        }
        SrtpExpertAssignment a = new SrtpExpertAssignment();
        a.setStage(STAGE);
        a.setTargetId(p.getId());
        a.setExpertId(expertId);
        a.setAssignedBy(adminId);
        a.setReviewStatus("PENDING");
        assignmentMapper.insert(a);
    }

    /** 列出该项目已指派专家（含分数、评审状态、意见），供管理员查看聚合评审情况 */
    public java.util.List<java.util.Map<String, Object>> listProjectAssignments(Long adminId, Long projectId) {
        requireAdmin(adminId);
        java.util.List<SrtpExpertAssignment> assignments = assignmentMapper.selectList(
                new QueryWrapper<SrtpExpertAssignment>().lambda()
                        .eq(SrtpExpertAssignment::getStage, STAGE)
                        .eq(SrtpExpertAssignment::getTargetId, projectId)
                        .orderByAsc(SrtpExpertAssignment::getCreatedAt));
        java.util.List<java.util.Map<String, Object>> out = new java.util.ArrayList<>();
        for (SrtpExpertAssignment a : assignments) {
            com.zhk.contest.training.entity.SrtpExpert e = expertMapper.selectById(a.getExpertId());
            java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("id", a.getId());
            m.put("expertId", a.getExpertId());
            m.put("expertName", e != null ? e.getName() : null);
            m.put("expertLevel", e != null && e.getExpertLevel() != null ? e.getExpertLevel().name() : null);
            m.put("expertTitle", e != null ? e.getTitle() : null);
            m.put("reviewStatus", a.getReviewStatus());
            m.put("score", a.getScore());
            m.put("reviewComment", a.getReviewComment());
            m.put("reviewedAt", a.getReviewedAt());
            m.put("assignedAt", a.getCreatedAt());
            out.add(m);
        }
        return out;
    }

    /** 撤销一条指派（未评审时可撤销） */
    @Transactional
    public void removeProjectAssignment(Long adminId, Long assignmentId) {
        requireAdmin(adminId);
        SrtpExpertAssignment a = assignmentMapper.selectById(assignmentId);
        if (a == null) return;
        if (a.getReviewStatus() != null && !"PENDING".equals(a.getReviewStatus())) {
            throw new BusinessException("该专家已完成评审，无法撤销指派");
        }
        assignmentMapper.deleteById(assignmentId);
    }

    public SrtpProject get(Long id) {
        return requireExisting(id);
    }

    public IPage<SrtpProject> listMine(Long userId, Page<SrtpProject> page) {
        return projectMapper.selectPage(page, new QueryWrapper<SrtpProject>().lambda()
                .eq(SrtpProject::getCreatedBy, userId)
                .orderByDesc(SrtpProject::getCreatedAt));
    }

    /**
     * 学生端"可填报中期/结题的立项"清单。
     * 规则：状态=APPROVED；只保留校级原始项目——排除批次级别为省级/国家级的记录(这些是"推荐晋级"生成的副本，
     * 后续中期/结题应回到源校级项目上填报，不在省/国级流程里再走一遍)；返回时附带批次名/级别/状态/该阶段截止时间，
     * 用于前端展示"该批次已进入中期/结题阶段"提示、区分同名项目、以及展示截止时间。
     */
    public List<java.util.Map<String, Object>> listMineEligibleForReport(Long userId, SrtpStage stage) {
        if (stage != SrtpStage.MIDTERM && stage != SrtpStage.CONCLUSION) {
            throw new BusinessException("stage 必须为 MIDTERM 或 CONCLUSION");
        }
        List<SrtpProject> approved = projectMapper.selectList(new QueryWrapper<SrtpProject>().lambda()
                .eq(SrtpProject::getCreatedBy, userId)
                .eq(SrtpProject::getStatus, SrtpRecordStatus.APPROVED)
                .orderByDesc(SrtpProject::getCreatedAt));
        List<java.util.Map<String, Object>> result = new ArrayList<>();
        for (SrtpProject p : approved) {
            SrtpBatch batch = p.getBatchId() != null ? batchMapper.selectById(p.getBatchId()) : null;
            // 排除省级/国家级的推荐副本：中期/结题只在校级原始项目上进行
            if (batch != null && batch.getLevel() != null
                    && ("省级".equals(batch.getLevel()) || "国家级".equals(batch.getLevel()))) {
                continue;
            }
            java.util.Map<String, Object> row = new java.util.LinkedHashMap<>();
            row.put("id", p.getId());
            row.put("name", p.getName());
            row.put("projectNo", p.getProjectNo());
            row.put("projectType", p.getProjectType());
            row.put("batchId", p.getBatchId());
            row.put("batchName", batch != null ? batch.getName() : null);
            row.put("batchLevel", batch != null ? batch.getLevel() : null);
            row.put("batchStatus", batch != null && batch.getStatus() != null ? batch.getStatus().name() : null);
            row.put("stageDeadline", batch == null ? null
                    : (stage == SrtpStage.MIDTERM ? batch.getMidtermDeadline() : batch.getConclusionDeadline()));
            result.add(row);
        }
        return result;
    }

    public IPage<SrtpProject> listForAdmin(Long adminId, Page<SrtpProject> page,
                                           Long batchId, SrtpRecordStatus status, String keyword) {
        User admin = requireAdmin(adminId);
        var wrapper = new QueryWrapper<SrtpProject>().lambda();
        if (batchId != null) wrapper.eq(SrtpProject::getBatchId, batchId);
        if (status != null) {
            wrapper.eq(SrtpProject::getStatus, status);
        } else {
            // 未指定状态时默认不展示"已被前置流程退回"的立项：这类记录退回给申报人/指导教师修改重提，
            // 尚未轮到管理员审核，混在审核列表里会造成困扰；需要查看可显式选择"已退回"筛选。
            wrapper.ne(SrtpProject::getStatus, SrtpRecordStatus.RETURNED);
        }
        if (keyword != null && !keyword.isBlank()) wrapper.like(SrtpProject::getName, keyword);
        boolean isSuper = AdminLevelResolver.isSuperAdmin(admin);
        if (!isSuper) {
            List<Long> visible = AdminLevelResolver.getVisibleOrgIds(admin, organizationMapper);
            if (visible.isEmpty()) return new Page<>(page.getCurrent(), page.getSize());
            wrapper.in(SrtpProject::getOrgId, visible);
        }
        wrapper.orderByDesc(SrtpProject::getCreatedAt);
        if (isSuper) {
            return projectMapper.selectPage(page, wrapper);
        }

        // 学院/学校管理员：审批中的立项仅在轮到自己这一级审核时才展示，
        // 避免还没轮到自己（例如仍在"指导教师审核"环节）就能在列表里预览到。
        ApproverRole myRole = adminApproverRole(admin);
        List<SrtpProject> matched = projectMapper.selectList(wrapper).stream()
                .filter(p -> p.getStatus() != SrtpRecordStatus.IN_REVIEW || isMyTurn(p, myRole))
                .collect(Collectors.toList());
        int size = (int) page.getSize();
        int from = (int) ((page.getCurrent() - 1) * size);
        List<SrtpProject> records = from >= matched.size() ? List.of()
                : matched.subList(from, Math.min(matched.size(), from + size));
        Page<SrtpProject> result = new Page<>(page.getCurrent(), page.getSize(), matched.size());
        result.setRecords(records);
        return result;
    }

    private ApproverRole adminApproverRole(User admin) {
        com.zhk.contest.enums.OrganizationLevel level = AdminLevelResolver.getLevel(admin, organizationMapper);
        if (level == com.zhk.contest.enums.OrganizationLevel.COLLEGE) return ApproverRole.COLLEGE_ADMIN;
        if (level == com.zhk.contest.enums.OrganizationLevel.SCHOOL) return ApproverRole.SCHOOL_ADMIN;
        return null;
    }

    private boolean isMyTurn(SrtpProject p, ApproverRole myRole) {
        if (myRole == null) return false;
        SrtpApprovalFlowConfig node = approvalEngine.currentNode(STAGE, p);
        return node != null && node.getApproverRole() == myRole;
    }

    // ===== 内部 =====

    /** 项目编号：批次code + 学院编码 + 三位序号，如 2026GJ-YXY-001（可复用于推荐生成目标编号） */
    public String generateProjectNo(SrtpProject p) {
        SrtpBatch batch = p.getBatchId() != null ? batchMapper.selectById(p.getBatchId()) : null;
        String batchCode = (batch != null && batch.getCode() != null && !batch.getCode().isBlank())
                ? batch.getCode() : ("SRTP" + (p.getBatchId() != null ? p.getBatchId() : ""));
        String orgCode = resolveOrgCode(p.getOrgId());
        long seq = projectMapper.selectCount(new QueryWrapper<SrtpProject>().lambda()
                .eq(SrtpProject::getBatchId, p.getBatchId())
                .eq(SrtpProject::getOrgId, p.getOrgId())
                .isNotNull(SrtpProject::getProjectNo)) + 1;
        return String.format("%s-%s-%03d", batchCode, orgCode, seq);
    }

    private String resolveOrgCode(Long orgId) {
        if (orgId == null) return "ORG";
        SrtpOrgCode oc = orgCodeMapper.selectOne(new QueryWrapper<SrtpOrgCode>().lambda()
                .eq(SrtpOrgCode::getOrgId, orgId).last("LIMIT 1"));
        return (oc != null && oc.getCode() != null && !oc.getCode().isBlank())
                ? oc.getCode() : ("ORG" + orgId);
    }

    private void saveParticipants(SrtpProject p, SrtpProjectSaveRequest req) {
        List<com.zhk.contest.training.dto.SrtpMemberDTO> members =
                req.getMembers() != null ? req.getMembers() : List.of();
        participantService.replaceMembers(STAGE, p.getId(), members);
        participantService.replaceAdvisors(STAGE, p.getId(), effectiveAdvisors(req));
        // 学生数以实际团队成员数为准，避免依赖学生手填造成空缺或不一致
        p.setStudentCount(members.size());
    }

    /** 立项来源于老师题目时，自动把出题教师设为指导教师（若未包含） */
    private List<SrtpAdvisorDTO> effectiveAdvisors(SrtpProjectSaveRequest req) {
        List<SrtpAdvisorDTO> advisors = req.getAdvisors() != null ? new ArrayList<>(req.getAdvisors()) : new ArrayList<>();
        if (req.getTopicId() != null) {
            SrtpTopic topic = topicMapper.selectById(req.getTopicId());
            if (topic != null && topic.getTeacherId() != null) {
                boolean present = advisors.stream().anyMatch(a -> Objects.equals(a.getUserId(), topic.getTeacherId()));
                if (!present) {
                    SrtpAdvisorDTO adv = new SrtpAdvisorDTO();
                    adv.setUserId(topic.getTeacherId());
                    User u = userMapper.selectById(topic.getTeacherId());
                    adv.setName(u != null ? u.getRealName() : null);
                    advisors.add(0, adv);
                }
            }
        }
        return advisors;
    }

    private void apply(SrtpProject p, SrtpProjectSaveRequest req) {
        p.setBatchId(req.getBatchId());
        p.setTopicId(req.getTopicId());
        p.setProjectType(req.getProjectType());
        p.setName(req.getName());
        p.setDisciplineL1Id(req.getDisciplineL1Id());
        p.setDisciplineL2Id(req.getDisciplineL2Id());
        // studentCount 不再取用户输入，由 saveParticipants() 按团队成员数自动同步
        p.setDescription(req.getDescription());
        p.setExtraData(customFieldService.toJson(req.getExtraData()));
    }

    private SrtpProject requireExisting(Long id) {
        SrtpProject p = (id == null) ? null : projectMapper.selectById(id);
        if (p == null) throw new BusinessException("立项记录不存在");
        return p;
    }

    private Long orgOf(Long userId) {
        User u = userMapper.selectById(userId);
        return u != null ? u.getOrganizationId() : null;
    }

    private User requireAdmin(Long adminId) {
        User u = (adminId == null) ? null : userMapper.selectById(adminId);
        if (u == null) throw new BusinessException(403, "无权操作");
        AdminLevelResolver.requireAtLeast(u, com.zhk.contest.enums.OrganizationLevel.COLLEGE, organizationMapper);
        return u;
    }
}
