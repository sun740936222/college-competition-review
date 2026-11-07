package com.zhk.contest.training.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhk.contest.entity.User;
import com.zhk.contest.enums.UserRole;
import com.zhk.contest.exception.BusinessException;
import com.zhk.contest.mapper.OrganizationMapper;
import com.zhk.contest.mapper.UserMapper;
import com.zhk.contest.training.dto.request.SrtpExpertSaveRequest;
import com.zhk.contest.training.entity.SrtpExpert;
import com.zhk.contest.training.enums.ExpertLevel;
import com.zhk.contest.training.enums.ExpertSource;
import com.zhk.contest.training.enums.ExpertStatus;
import com.zhk.contest.training.mapper.SrtpExpertMapper;
import com.zhk.contest.util.AdminLevelResolver;
import com.zhk.contest.util.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 专家库管理：管理员增删改查 + 教师自主申请入库 + 申请审核。 */
@Service
public class SrtpExpertService {

    private final SrtpExpertMapper expertMapper;
    private final UserMapper userMapper;
    private final OrganizationMapper organizationMapper;

    public SrtpExpertService(SrtpExpertMapper expertMapper,
                             UserMapper userMapper,
                             OrganizationMapper organizationMapper) {
        this.expertMapper = expertMapper;
        this.userMapper = userMapper;
        this.organizationMapper = organizationMapper;
    }

    /** 管理员添加专家（直接入库，APPROVED）；可选同时创建登录账号 */
    @Transactional
    public SrtpExpert add(Long adminId, SrtpExpertSaveRequest req) {
        requireAdmin(adminId);
        SrtpExpert e = new SrtpExpert();
        apply(e, req);
        e.setSource(ExpertSource.ADMIN_ADD);
        e.setStatus(ExpertStatus.APPROVED);
        if (e.getExpertLevel() == null) e.setExpertLevel(ExpertLevel.COLLEGE);
        // 若填写了用户名且未关联账号，则为专家创建登录账号（登录身份选"参赛者"，凭大创专家身份进入审批工作台）
        if (e.getUserId() == null && req.getUsername() != null && !req.getUsername().isBlank()) {
            e.setUserId(createLoginAccount(req));
        }
        expertMapper.insert(e);
        return e;
    }

    /** 为专家创建登录账号，返回 user.id */
    private Long createLoginAccount(SrtpExpertSaveRequest req) {
        if (userMapper.existsByUsername(req.getUsername())) {
            throw new BusinessException("登录用户名已存在：" + req.getUsername());
        }
        String pwd = (req.getPassword() != null && !req.getPassword().isBlank())
                ? req.getPassword() : SecurityUtils.generateRandomPassword(10);
        User u = new User();
        u.setUsername(req.getUsername());
        u.setPassword(SecurityUtils.hashPassword(pwd));
        u.setRealName(req.getName());
        u.setRole(UserRole.CONTESTANT);
        u.setOrganizationId(req.getOrgId());
        u.setPhone(req.getPhone());
        u.setEmail(req.getEmail());
        userMapper.insert(u);
        return u.getId();
    }

    /** 教师自主申请入库（PENDING，待管理员审核） */
    @Transactional
    public SrtpExpert selfApply(Long userId, SrtpExpertSaveRequest req) {
        if (userId == null) throw new BusinessException(401, "请先登录");
        long exists = expertMapper.selectCount(new QueryWrapper<SrtpExpert>().lambda()
                .eq(SrtpExpert::getUserId, userId)
                .in(SrtpExpert::getStatus, ExpertStatus.PENDING, ExpertStatus.APPROVED));
        if (exists > 0) throw new BusinessException("你已提交申请或已是专家，请勿重复申请");
        SrtpExpert e = new SrtpExpert();
        apply(e, req);
        e.setUserId(userId);
        e.setSource(ExpertSource.SELF_APPLY);
        e.setStatus(ExpertStatus.PENDING);
        if (e.getExpertLevel() == null) e.setExpertLevel(ExpertLevel.COLLEGE);
        expertMapper.insert(e);
        return e;
    }

    /** 审核自主申请：通过或驳回 */
    @Transactional
    public SrtpExpert review(Long adminId, Long id, boolean approve, String reason) {
        requireAdmin(adminId);
        SrtpExpert e = expertMapper.selectById(id);
        if (e == null) throw new BusinessException("专家申请不存在");
        e.setStatus(approve ? ExpertStatus.APPROVED : ExpertStatus.REJECTED);
        if (!approve) e.setRejectReason(reason);
        expertMapper.updateById(e);
        return e;
    }

    @Transactional
    public SrtpExpert update(Long adminId, Long id, SrtpExpertSaveRequest req) {
        requireAdmin(adminId);
        SrtpExpert e = expertMapper.selectById(id);
        if (e == null) throw new BusinessException("专家不存在");
        apply(e, req);
        expertMapper.updateById(e);
        return e;
    }

    @Transactional
    public void delete(Long adminId, Long id) {
        requireAdmin(adminId);
        expertMapper.deleteById(id);
    }

    public IPage<SrtpExpert> list(Long adminId, Page<SrtpExpert> page, String keyword,
                                  ExpertLevel level, ExpertStatus status) {
        requireAdmin(adminId);
        var wrapper = new QueryWrapper<SrtpExpert>().lambda();
        if (keyword != null && !keyword.isBlank()) wrapper.like(SrtpExpert::getName, keyword);
        if (level != null) wrapper.eq(SrtpExpert::getExpertLevel, level);
        if (status != null) wrapper.eq(SrtpExpert::getStatus, status);
        wrapper.orderByDesc(SrtpExpert::getCreatedAt);
        return expertMapper.selectPage(page, wrapper);
    }

    private void apply(SrtpExpert e, SrtpExpertSaveRequest req) {
        e.setName(req.getName());
        e.setPhone(req.getPhone());
        e.setEmail(req.getEmail());
        e.setOrgId(req.getOrgId());
        e.setTitle(req.getTitle());
        e.setResearchField(req.getResearchField());
        if (req.getExpertLevel() != null) e.setExpertLevel(req.getExpertLevel());
        e.setApplyComment(req.getApplyComment());
        if (req.getUserId() != null) e.setUserId(req.getUserId());
    }

    private User requireAdmin(Long adminId) {
        User u = (adminId == null) ? null : userMapper.selectById(adminId);
        if (u == null) throw new BusinessException(403, "无权操作");
        AdminLevelResolver.requireAtLeast(u, com.zhk.contest.enums.OrganizationLevel.COLLEGE, organizationMapper);
        return u;
    }
}
