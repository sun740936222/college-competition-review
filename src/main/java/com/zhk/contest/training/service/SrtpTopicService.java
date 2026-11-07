package com.zhk.contest.training.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhk.contest.entity.User;
import com.zhk.contest.exception.BusinessException;
import com.zhk.contest.mapper.OrganizationMapper;
import com.zhk.contest.mapper.UserMapper;
import com.zhk.contest.training.dto.request.SrtpTopicSaveRequest;
import com.zhk.contest.training.entity.SrtpApprovalFlowConfig;
import com.zhk.contest.training.entity.SrtpProject;
import com.zhk.contest.training.entity.SrtpProjectMember;
import com.zhk.contest.training.entity.SrtpTopic;
import com.zhk.contest.training.enums.ApproverRole;
import com.zhk.contest.training.enums.ClaimStatus;
import com.zhk.contest.training.enums.SrtpMemberKind;
import com.zhk.contest.training.enums.SrtpRecordStatus;
import com.zhk.contest.training.enums.SrtpStage;
import com.zhk.contest.training.enums.TopicSource;
import com.zhk.contest.training.mapper.SrtpProjectMapper;
import com.zhk.contest.training.mapper.SrtpTopicMapper;
import com.zhk.contest.util.AdminLevelResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** 选题管理：学生自拟申报 + 学校给定学生认领；提交、审批、列表、详情。 */
@Service
public class SrtpTopicService {

    private static final SrtpStage STAGE = SrtpStage.TOPIC;

    private final SrtpTopicMapper topicMapper;
    private final SrtpProjectMapper projectMapper;
    private final SrtpApprovalEngine approvalEngine;
    private final SrtpCustomFieldService customFieldService;
    private final SrtpParticipantService participantService;
    private final UserMapper userMapper;
    private final OrganizationMapper organizationMapper;

    public SrtpTopicService(SrtpTopicMapper topicMapper,
                            SrtpProjectMapper projectMapper,
                            SrtpApprovalEngine approvalEngine,
                            SrtpCustomFieldService customFieldService,
                            SrtpParticipantService participantService,
                            UserMapper userMapper,
                            OrganizationMapper organizationMapper) {
        this.topicMapper = topicMapper;
        this.projectMapper = projectMapper;
        this.approvalEngine = approvalEngine;
        this.customFieldService = customFieldService;
        this.participantService = participantService;
        this.userMapper = userMapper;
        this.organizationMapper = organizationMapper;
    }

    // ===== 学生自拟 =====

    @Transactional
    public SrtpTopic create(Long userId, SrtpTopicSaveRequest req) {
        SrtpTopic t = new SrtpTopic();
        apply(t, req);
        t.setSource(TopicSource.STUDENT_PROPOSED);
        t.setInitiatorId(userId);
        t.setStatus(SrtpRecordStatus.DRAFT);
        t.setCurrentStep(0);
        if (t.getOrgId() == null) t.setOrgId(orgOf(userId));
        topicMapper.insert(t);
        saveParticipants(t.getId(), req);
        return t;
    }

    @Transactional
    public SrtpTopic update(Long userId, Long id, SrtpTopicSaveRequest req) {
        SrtpTopic t = getOwned(userId, id);
        if (t.getStatus() != SrtpRecordStatus.DRAFT && t.getStatus() != SrtpRecordStatus.RETURNED) {
            throw new BusinessException("仅草稿或已退回的选题可编辑");
        }
        apply(t, req);
        topicMapper.updateById(t);
        saveParticipants(id, req);
        return t;
    }

    @Transactional
    public void delete(Long userId, Long id) {
        SrtpTopic t = getOwned(userId, id);
        if (t.getStatus() != SrtpRecordStatus.DRAFT) {
            throw new BusinessException("仅草稿选题可删除");
        }
        topicMapper.deleteById(id);
    }

    @Transactional
    public SrtpTopic submit(Long userId, Long id) {
        SrtpTopic t = getOwned(userId, id);
        customFieldService.validate(STAGE, t.getBatchId(), t.getExtraData());
        approvalEngine.submit(STAGE, t);
        topicMapper.updateById(t);
        return t;
    }

    // ===== 学校给定 + 学生认领 =====

    /** 管理员发布学校给定选题（进入选题库，开放认领） */
    @Transactional
    public SrtpTopic publish(Long adminId, SrtpTopicSaveRequest req) {
        requireAdmin(adminId);
        SrtpTopic t = new SrtpTopic();
        apply(t, req);
        t.setSource(TopicSource.SCHOOL_ASSIGNED);
        t.setInitiatorId(adminId);
        t.setStatus(SrtpRecordStatus.APPROVED);
        t.setCurrentStep(0);
        t.setClaimStatus(ClaimStatus.OPEN);
        topicMapper.insert(t);
        saveParticipants(t.getId(), req);
        return t;
    }

    /** 学生认领学校给定选题（乐观并发：仅当 OPEN 才能认领成功） */
    @Transactional
    public void claim(Long userId, Long id) {
        SrtpTopic t = topicMapper.selectById(id);
        if (t == null || t.getSource() != TopicSource.SCHOOL_ASSIGNED) {
            throw new BusinessException("选题不存在或不可认领");
        }
        int rows = topicMapper.update(null, new UpdateWrapper<SrtpTopic>().lambda()
                .eq(SrtpTopic::getId, id)
                .eq(SrtpTopic::getClaimStatus, ClaimStatus.OPEN)
                .set(SrtpTopic::getClaimedBy, userId)
                .set(SrtpTopic::getClaimStatus, ClaimStatus.CLAIMED));
        if (rows == 0) throw new BusinessException("该选题已被认领或不可认领");
    }

    /** 可认领的学校给定/老师题目列表 */
    public List<SrtpTopic> listClaimable(Long batchId) {
        var wrapper = new QueryWrapper<SrtpTopic>().lambda()
                .eq(SrtpTopic::getSource, TopicSource.SCHOOL_ASSIGNED)
                .eq(SrtpTopic::getClaimStatus, ClaimStatus.OPEN);
        if (batchId != null) wrapper.eq(SrtpTopic::getBatchId, batchId);
        wrapper.orderByDesc(SrtpTopic::getCreatedAt);
        return fillTeacherNames(topicMapper.selectList(wrapper));
    }

    /** 学生已认领的老师题目（用于立项时选择来源，自动带出指导教师） */
    public List<SrtpTopic> myClaimedTopics(Long userId) {
        return fillTeacherNames(topicMapper.selectList(new QueryWrapper<SrtpTopic>().lambda()
                .eq(SrtpTopic::getSource, TopicSource.SCHOOL_ASSIGNED)
                .eq(SrtpTopic::getClaimedBy, userId)
                .orderByDesc(SrtpTopic::getCreatedAt)));
    }

    // ===== 审批 =====

    @Transactional
    public SrtpTopic approve(Long approverId, Long id, String comment) {
        SrtpTopic t = requireExisting(id);
        approvalEngine.approve(STAGE, t, approverId, comment, t.getOrgId());
        topicMapper.updateById(t);
        return t;
    }

    @Transactional
    public SrtpTopic reject(Long approverId, Long id, String comment, Integer targetStep) {
        SrtpTopic t = requireExisting(id);
        approvalEngine.rejectTo(STAGE, t, approverId, comment, targetStep, t.getOrgId());
        topicMapper.updateById(t);
        return t;
    }

    // ===== 查询 =====

    public SrtpTopic get(Long id) {
        SrtpTopic t = fillTeacherName(requireExisting(id));
        if (t.getClaimedBy() != null) {
            User u = userMapper.selectById(t.getClaimedBy());
            if (u != null) t.setClaimedByName(u.getRealName());
        }
        return t;
    }

    /** 选择/认领了该题目并已立项的学生团队（老师出题详情用） */
    public List<Map<String, Object>> selectionsForTopic(Long topicId) {
        List<SrtpProject> projects = projectMapper.selectList(new QueryWrapper<SrtpProject>().lambda()
                .eq(SrtpProject::getTopicId, topicId)
                .orderByDesc(SrtpProject::getCreatedAt));
        List<Map<String, Object>> result = new ArrayList<>();
        for (SrtpProject p : projects) {
            List<SrtpProjectMember> members = participantService.listMembers(SrtpStage.PROJECT, p.getId());
            String leader = members.stream()
                    .filter(m -> m.getMemberRole() == SrtpMemberKind.LEADER)
                    .map(SrtpProjectMember::getName).filter(Objects::nonNull).findFirst().orElse(null);
            String memberNames = members.stream()
                    .map(SrtpProjectMember::getName).filter(Objects::nonNull).collect(Collectors.joining("、"));
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("projectId", p.getId());
            m.put("projectNo", p.getProjectNo());
            m.put("projectName", p.getName());
            m.put("status", p.getStatus() != null ? p.getStatus().name() : null);
            m.put("leader", leader);
            m.put("members", memberNames);
            result.add(m);
        }
        return result;
    }

    public IPage<SrtpTopic> listMine(Long userId, Page<SrtpTopic> page) {
        return topicMapper.selectPage(page, new QueryWrapper<SrtpTopic>().lambda()
                .eq(SrtpTopic::getInitiatorId, userId)
                .orderByDesc(SrtpTopic::getCreatedAt));
    }

    public IPage<SrtpTopic> listForAdmin(Long adminId, Page<SrtpTopic> page,
                                         Long batchId, SrtpRecordStatus status, String keyword) {
        User admin = requireAdmin(adminId);
        var wrapper = new QueryWrapper<SrtpTopic>().lambda();
        if (batchId != null) wrapper.eq(SrtpTopic::getBatchId, batchId);
        if (status != null) wrapper.eq(SrtpTopic::getStatus, status);
        if (keyword != null && !keyword.isBlank()) wrapper.like(SrtpTopic::getName, keyword);
        boolean isSuper = AdminLevelResolver.isSuperAdmin(admin);
        if (!isSuper) {
            List<Long> visible = AdminLevelResolver.getVisibleOrgIds(admin, organizationMapper);
            if (visible.isEmpty()) return new Page<>(page.getCurrent(), page.getSize());
            wrapper.in(SrtpTopic::getOrgId, visible);
        }
        wrapper.orderByDesc(SrtpTopic::getCreatedAt);
        if (isSuper) {
            IPage<SrtpTopic> pageResult = topicMapper.selectPage(page, wrapper);
            fillTeacherNames(pageResult.getRecords());
            return pageResult;
        }

        // 学院/学校管理员：审批中的选题仅在轮到自己这一级审核时才展示
        ApproverRole myRole = adminApproverRole(admin);
        List<SrtpTopic> matched = topicMapper.selectList(wrapper).stream()
                .filter(t -> t.getStatus() != SrtpRecordStatus.IN_REVIEW || isMyTurn(t, myRole))
                .collect(Collectors.toList());
        int size = (int) page.getSize();
        int from = (int) ((page.getCurrent() - 1) * size);
        List<SrtpTopic> records = from >= matched.size() ? List.of()
                : matched.subList(from, Math.min(matched.size(), from + size));
        fillTeacherNames(records);
        Page<SrtpTopic> result = new Page<>(page.getCurrent(), page.getSize(), matched.size());
        result.setRecords(records);
        return result;
    }

    private ApproverRole adminApproverRole(User admin) {
        com.zhk.contest.enums.OrganizationLevel level = AdminLevelResolver.getLevel(admin, organizationMapper);
        if (level == com.zhk.contest.enums.OrganizationLevel.COLLEGE) return ApproverRole.COLLEGE_ADMIN;
        if (level == com.zhk.contest.enums.OrganizationLevel.SCHOOL) return ApproverRole.SCHOOL_ADMIN;
        return null;
    }

    private boolean isMyTurn(SrtpTopic t, ApproverRole myRole) {
        if (myRole == null) return false;
        SrtpApprovalFlowConfig node = approvalEngine.currentNode(STAGE, t);
        return node != null && node.getApproverRole() == myRole;
    }

    /** 管理员删除选题（连同成员/指导教师子表） */
    @Transactional
    public void adminDelete(Long adminId, Long id) {
        requireAdmin(adminId);
        SrtpTopic t = requireExisting(id);
        topicMapper.deleteById(t.getId());
        participantService.replaceMembers(STAGE, id, null);
        participantService.replaceAdvisors(STAGE, id, null);
    }

    /** 管理员批量删除选题；忽略不存在的 id，返回实际删除数量 */
    @Transactional
    public int adminDeleteBatch(Long adminId, List<Long> ids) {
        requireAdmin(adminId);
        if (ids == null || ids.isEmpty()) return 0;
        int count = 0;
        for (Long id : ids) {
            SrtpTopic t = topicMapper.selectById(id);
            if (t == null) continue;
            topicMapper.deleteById(id);
            participantService.replaceMembers(STAGE, id, null);
            participantService.replaceAdvisors(STAGE, id, null);
            count++;
        }
        return count;
    }

    // ===== 内部 =====

    private void saveParticipants(Long topicId, SrtpTopicSaveRequest req) {
        participantService.replaceMembers(STAGE, topicId, req.getMembers());
        participantService.replaceAdvisors(STAGE, topicId, req.getAdvisors());
    }

    private void apply(SrtpTopic t, SrtpTopicSaveRequest req) {
        t.setBatchId(req.getBatchId());
        if (req.getSource() != null) t.setSource(req.getSource());
        t.setProjectType(req.getProjectType());
        t.setName(req.getName());
        t.setDisciplineL1Id(req.getDisciplineL1Id());
        t.setDisciplineL2Id(req.getDisciplineL2Id());
        t.setResearchType(req.getResearchType());
        t.setInitiatorContact(req.getInitiatorContact());
        t.setTeacherId(req.getTeacherId());
        t.setInfo(req.getInfo());
        // 保存前做一次必填校验（草稿允许留空，故此处仅在有数据时序列化）
        t.setExtraData(customFieldService.toJson(req.getExtraData()));
    }

    /** 填充出题教师姓名（展示用） */
    private SrtpTopic fillTeacherName(SrtpTopic t) {
        if (t != null && t.getTeacherId() != null) {
            User u = userMapper.selectById(t.getTeacherId());
            if (u != null) t.setTeacherName(u.getRealName());
        }
        return t;
    }

    private List<SrtpTopic> fillTeacherNames(List<SrtpTopic> list) {
        list.forEach(this::fillTeacherName);
        return list;
    }

    private SrtpTopic getOwned(Long userId, Long id) {
        SrtpTopic t = requireExisting(id);
        if (!java.util.Objects.equals(t.getInitiatorId(), userId)) {
            throw new BusinessException(403, "无权操作他人选题");
        }
        return t;
    }

    private SrtpTopic requireExisting(Long id) {
        SrtpTopic t = (id == null) ? null : topicMapper.selectById(id);
        if (t == null) throw new BusinessException("选题不存在");
        return t;
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
