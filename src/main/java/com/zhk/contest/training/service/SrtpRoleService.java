package com.zhk.contest.training.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zhk.contest.entity.User;
import com.zhk.contest.enums.OrganizationLevel;
import com.zhk.contest.mapper.OrganizationMapper;
import com.zhk.contest.mapper.UserMapper;
import com.zhk.contest.training.entity.SrtpExpert;
import com.zhk.contest.training.entity.SrtpMemberRole;
import com.zhk.contest.training.enums.ApproverRole;
import com.zhk.contest.training.enums.ExpertLevel;
import com.zhk.contest.training.enums.ExpertStatus;
import com.zhk.contest.training.enums.SrtpRoleType;
import com.zhk.contest.training.mapper.SrtpExpertMapper;
import com.zhk.contest.training.mapper.SrtpMemberRoleMapper;
import com.zhk.contest.util.AdminLevelResolver;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * SRTP 身份解析：登录账号仍用竞赛体系 user，大创身份来自 srtp_member_role / srtp_expert；
 * 管理员身份复用竞赛的组织级别（AdminLevelResolver），支持一人多角。
 */
@Service
public class SrtpRoleService {

    private final SrtpMemberRoleMapper memberRoleMapper;
    private final SrtpExpertMapper expertMapper;
    private final UserMapper userMapper;
    private final OrganizationMapper organizationMapper;

    public SrtpRoleService(SrtpMemberRoleMapper memberRoleMapper,
                           SrtpExpertMapper expertMapper,
                           UserMapper userMapper,
                           OrganizationMapper organizationMapper) {
        this.memberRoleMapper = memberRoleMapper;
        this.expertMapper = expertMapper;
        this.userMapper = userMapper;
        this.organizationMapper = organizationMapper;
    }

    public List<SrtpMemberRole> rolesOf(Long userId) {
        return memberRoleMapper.selectList(
                new QueryWrapper<SrtpMemberRole>().lambda()
                        .eq(SrtpMemberRole::getUserId, userId)
                        .eq(SrtpMemberRole::getStatus, "ACTIVE"));
    }

    public boolean hasRole(Long userId, SrtpRoleType type) {
        return memberRoleMapper.selectCount(
                new QueryWrapper<SrtpMemberRole>().lambda()
                        .eq(SrtpMemberRole::getUserId, userId)
                        .eq(SrtpMemberRole::getRoleType, type)
                        .eq(SrtpMemberRole::getStatus, "ACTIVE")) > 0;
    }

    /** 是否为指定级别的已入库专家（以 srtp_expert 为准） */
    public boolean isExpert(Long userId, ExpertLevel level) {
        return expertMapper.selectCount(
                new QueryWrapper<SrtpExpert>().lambda()
                        .eq(SrtpExpert::getUserId, userId)
                        .eq(SrtpExpert::getExpertLevel, level)
                        .eq(SrtpExpert::getStatus, ExpertStatus.APPROVED)) > 0;
    }

    /**
     * 判断用户能否承担某审批节点角色。
     * recordOrgId 为被审批记录所属学院，用于管理员的组织范围校验。
     */
    public boolean matchesApproverRole(Long userId, ApproverRole role, Long recordOrgId) {
        if (userId == null || role == null) return false;
        return switch (role) {
            case TEACHER -> hasRole(userId, SrtpRoleType.TEACHER);
            case COLLEGE_EXPERT -> isExpert(userId, ExpertLevel.COLLEGE);
            case SCHOOL_EXPERT -> isExpert(userId, ExpertLevel.SCHOOL);
            case COLLEGE_ADMIN -> isAdminAtLevel(userId, OrganizationLevel.COLLEGE, recordOrgId);
            case SCHOOL_ADMIN -> isAdminAtLevel(userId, OrganizationLevel.SCHOOL, recordOrgId);
        };
    }

    private boolean isAdminAtLevel(Long userId, OrganizationLevel level, Long recordOrgId) {
        User u = userMapper.selectById(userId);
        if (u == null) return false;
        if (AdminLevelResolver.isSuperAdmin(u)) return true;
        OrganizationLevel adminLevel = AdminLevelResolver.getLevel(u, organizationMapper);
        if (adminLevel == null || adminLevel != level) return false;
        // 组织范围校验：被审批记录所属学院须在该管理员可见组织树内
        if (recordOrgId == null) return true;
        List<Long> visible = AdminLevelResolver.getVisibleOrgIds(u, organizationMapper);
        return visible.contains(recordOrgId);
    }
}
