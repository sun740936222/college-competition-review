package com.zhk.contest.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhk.contest.dto.request.LoginRequest;
import com.zhk.contest.dto.request.RegisterNationalRequest;
import com.zhk.contest.dto.request.RegisterProvinceRequest;
import com.zhk.contest.dto.request.RegisterRequest;
import com.zhk.contest.dto.request.RegisterSchoolRequest;
import com.zhk.contest.dto.response.LoginResponse;
import com.zhk.contest.entity.Event;
import com.zhk.contest.entity.Organization;
import com.zhk.contest.entity.Track;
import com.zhk.contest.entity.User;
import com.zhk.contest.enums.CompetitionLevel;
import com.zhk.contest.enums.OrganizationLevel;
import com.zhk.contest.enums.UserRole;
import com.zhk.contest.exception.BusinessException;
import com.zhk.contest.mapper.EventMapper;
import com.zhk.contest.mapper.OrganizationMapper;
import com.zhk.contest.mapper.TrackMapper;
import com.zhk.contest.mapper.UserMapper;
import com.zhk.contest.service.AuthService;
import com.zhk.contest.util.AdminLevelResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zhk.contest.util.SecurityUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final OrganizationMapper organizationMapper;
    private final EventMapper eventMapper;
    private final TrackMapper trackMapper;

    private static final Map<String, String> ROLE_NAMES = new HashMap<>();
    static {
        ROLE_NAMES.put("SUPER_ADMIN", "超级管理员");
        ROLE_NAMES.put("COLLEGE_ADMIN", "院系管理员");
        ROLE_NAMES.put("SCHOOL_ADMIN", "校级管理员");
        ROLE_NAMES.put("PROVINCE_ADMIN", "省级管理员");
        ROLE_NAMES.put("NATIONAL_ADMIN", "国赛管理员");
        ROLE_NAMES.put("CONTESTANT", "参赛者");
        ROLE_NAMES.put("JUDGE", "评委");
    }

    private static final Set<String> ADMIN_ROLES = Set.of(
            "SUPER_ADMIN", "COLLEGE_ADMIN", "SCHOOL_ADMIN", "PROVINCE_ADMIN", "NATIONAL_ADMIN");

    private static final Map<String, String> ADMIN_ROLE_TO_ORG_LEVEL = Map.of(
            "COLLEGE_ADMIN", "COLLEGE",
            "SCHOOL_ADMIN", "SCHOOL",
            "PROVINCE_ADMIN", "PROVINCE",
            "NATIONAL_ADMIN", "NATIONAL");

    public AuthServiceImpl(UserMapper userMapper, OrganizationMapper organizationMapper,
                          EventMapper eventMapper, TrackMapper trackMapper) {
        this.userMapper = userMapper;
        this.organizationMapper = organizationMapper;
        this.eventMapper = eventMapper;
        this.trackMapper = trackMapper;
    }

    @Override
    @Transactional
    public LoginResponse register(RegisterRequest request) {
        if (userMapper.existsByUsername(request.getUsername())) {
            throw new BusinessException("用户名已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(SecurityUtils.hashPassword(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setRole(UserRole.CONTESTANT);
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());

        if (request.getInviteCode() != null && !request.getInviteCode().isBlank()) {
            Organization org = organizationMapper.findByInviteCode(request.getInviteCode())
                    .orElseThrow(() -> new BusinessException("邀请码无效"));
            user.setOrganizationId(org.getId());
        }

        userMapper.insert(user);
        return toLoginResponse(user);
    }

    @Override
    @Transactional
    public LoginResponse registerNational(RegisterNationalRequest request) {
        if (userMapper.existsByUsername(request.getUsername())) {
            throw new BusinessException("用户名已存在");
        }

        // Create NATIONAL root org for this competition
        Organization org = new Organization();
        org.setName(request.getCompetitionName());
        org.setDescription(request.getDescription());
        org.setLevel(OrganizationLevel.NATIONAL);
        org.setInviteCode("NAT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        organizationMapper.insert(org);

        // Create admin user for this national org
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(SecurityUtils.hashPassword(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setRole(UserRole.ADMIN);
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setOrganizationId(org.getId());
        userMapper.insert(user);

        return toLoginResponse(user);
    }

    @Override
    @Transactional
    public LoginResponse registerProvince(RegisterProvinceRequest request) {
        if (userMapper.existsByUsername(request.getUsername())) {
            throw new BusinessException("用户名已存在");
        }

        Organization parent = organizationMapper.findByInviteCode(request.getParentInviteCode())
                .orElseThrow(() -> new BusinessException("上级邀请码无效"));
        if (parent.getLevel() != OrganizationLevel.NATIONAL) {
            throw new BusinessException("省级组委会注册需要国赛组委会的邀请码");
        }

        // Reuse existing org with same name under this parent, avoiding duplicates
        Organization org = organizationMapper.selectOne(
                new QueryWrapper<Organization>().lambda()
                        .eq(Organization::getParentId, parent.getId())
                        .eq(Organization::getLevel, OrganizationLevel.PROVINCE)
                        .eq(Organization::getName, request.getProvinceName()));
        boolean isNewOrg = (org == null);
        if (isNewOrg) {
            org = new Organization();
            org.setName(request.getProvinceName());
            org.setDescription(request.getDescription());
            org.setLevel(OrganizationLevel.PROVINCE);
            org.setParentId(parent.getId());
            org.setInviteCode("PROV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            organizationMapper.insert(org);
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(SecurityUtils.hashPassword(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setRole(UserRole.ADMIN);
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setOrganizationId(org.getId());
        userMapper.insert(user);

        // Auto-create PROVINCE child events under the NATIONAL parent events (only for new orgs)
        if (isNewOrg) {
            List<Event> nationalEvents = eventMapper.selectList(
                    new QueryWrapper<Event>().lambda()
                            .eq(Event::getCompetitionLevel, CompetitionLevel.NATIONAL)
                            .eq(Event::getOrganizationId, parent.getId()));
            for (Event nationalEvent : nationalEvents) {
                List<Track> parentTracks = trackMapper.findByEventIdOrderBySortOrder(nationalEvent.getId());
                Event child = new Event();
                child.setName(nationalEvent.getName() + "——" + org.getName() + "赛区");
                child.setDescription(nationalEvent.getDescription());
                child.setRegistrationDeadline(nationalEvent.getRegistrationDeadline());
                child.setSubmissionDeadline(nationalEvent.getSubmissionDeadline());
                child.setReviewStartTime(nationalEvent.getReviewStartTime());
                child.setReviewEndTime(nationalEvent.getReviewEndTime());
                child.setRequireVideo(nationalEvent.getRequireVideo());
                child.setAllowResubmit(nationalEvent.getAllowResubmit());
                child.setRequireComment(nationalEvent.getRequireComment());
                child.setOrganizationId(org.getId());
                child.setParentEventId(nationalEvent.getId());
                child.setCompetitionLevel(CompetitionLevel.PROVINCE);
                child.setStatus(nationalEvent.getStatus());
                eventMapper.insert(child);
                for (Track track : parentTracks) {
                    Track childTrack = new Track();
                    childTrack.setEventId(child.getId());
                    childTrack.setName(track.getName());
                    childTrack.setGroupName(track.getGroupName());
                    childTrack.setDescription(track.getDescription());
                    childTrack.setSortOrder(track.getSortOrder());
                    trackMapper.insert(childTrack);
                }
            }
        }

        return toLoginResponse(user);
    }

    @Override
    @Transactional
    public LoginResponse registerSchool(RegisterSchoolRequest request) {
        if (userMapper.existsByUsername(request.getUsername())) {
            throw new BusinessException("用户名已存在");
        }

        Organization parent = organizationMapper.findByInviteCode(request.getParentInviteCode())
                .orElseThrow(() -> new BusinessException("上级邀请码无效"));
        if (parent.getLevel() != OrganizationLevel.PROVINCE) {
            throw new BusinessException("校级管理员注册需要省级组委会的邀请码");
        }

        // Reuse existing org with same name under this parent, avoiding duplicates
        Organization org = organizationMapper.selectOne(
                new QueryWrapper<Organization>().lambda()
                        .eq(Organization::getParentId, parent.getId())
                        .eq(Organization::getLevel, OrganizationLevel.SCHOOL)
                        .eq(Organization::getName, request.getSchoolName()));
        boolean isNewOrg = (org == null);
        if (isNewOrg) {
            org = new Organization();
            org.setName(request.getSchoolName());
            org.setDescription(request.getDescription());
            org.setLevel(OrganizationLevel.SCHOOL);
            org.setParentId(parent.getId());
            org.setInviteCode("SCHO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            organizationMapper.insert(org);
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(SecurityUtils.hashPassword(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setRole(UserRole.ADMIN);
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setOrganizationId(org.getId());
        userMapper.insert(user);

        // Auto-create SCHOOL child events under the parent PROVINCE events (only for new orgs)
        if (isNewOrg) {
            List<Event> provinceEvents = eventMapper.selectList(
                    new QueryWrapper<Event>().lambda()
                            .eq(Event::getCompetitionLevel, CompetitionLevel.PROVINCE)
                            .eq(Event::getOrganizationId, parent.getId()));
            for (Event provinceEvent : provinceEvents) {
                List<Track> parentTracks = trackMapper.findByEventIdOrderBySortOrder(provinceEvent.getId());
                Event child = new Event();
                child.setName(provinceEvent.getName() + "——" + org.getName() + "赛区");
                child.setDescription(provinceEvent.getDescription());
                child.setRegistrationDeadline(provinceEvent.getRegistrationDeadline());
                child.setSubmissionDeadline(provinceEvent.getSubmissionDeadline());
                child.setReviewStartTime(provinceEvent.getReviewStartTime());
                child.setReviewEndTime(provinceEvent.getReviewEndTime());
                child.setRequireVideo(provinceEvent.getRequireVideo());
                child.setAllowResubmit(provinceEvent.getAllowResubmit());
                child.setRequireComment(provinceEvent.getRequireComment());
                child.setOrganizationId(org.getId());
                child.setParentEventId(provinceEvent.getId());
                child.setCompetitionLevel(CompetitionLevel.SCHOOL);
                child.setStatus(provinceEvent.getStatus());
                eventMapper.insert(child);
                for (Track track : parentTracks) {
                    Track childTrack = new Track();
                    childTrack.setEventId(child.getId());
                    childTrack.setName(track.getName());
                    childTrack.setGroupName(track.getGroupName());
                    childTrack.setDescription(track.getDescription());
                    childTrack.setSortOrder(track.getSortOrder());
                    trackMapper.insert(childTrack);
                }
            }
        }

        return toLoginResponse(user);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userMapper.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException("用户名或密码错误"));

        if (!user.getPassword().equals(SecurityUtils.hashPassword(request.getPassword()))) {
            throw new BusinessException("用户名或密码错误");
        }

        String role = request.getRole();
        // Map legacy roles
        if ("UNIT_ADMIN".equals(role)) {
            role = "COLLEGE_ADMIN";
        }

        String roleName = ROLE_NAMES.get(role);
        if (roleName == null) {
            throw new BusinessException("无效的登录身份");
        }

        // SUPER_ADMIN: special handling — no org level check
        if ("SUPER_ADMIN".equals(role)) {
            if (user.getRole() != UserRole.SUPER_ADMIN) {
                throw new BusinessException("该用户不是超级管理员，请选择正确的身份登录");
            }
            return toLoginResponse(user);
        }

        // For other admin roles, validate user is an admin and org level matches
        if (ADMIN_ROLES.contains(role)) {
            if (user.getRole() != UserRole.ADMIN) {
                throw new BusinessException("该用户不是管理员，请选择正确的身份登录");
            }
            Organization org = user.getOrganizationId() != null
                    ? organizationMapper.selectById(user.getOrganizationId()) : null;
            String expectedLevel = ADMIN_ROLE_TO_ORG_LEVEL.get(role);
            if (org == null || org.getLevel() == null || !org.getLevel().name().equals(expectedLevel)) {
                throw new BusinessException("该用户不是" + roleName + "，请选择正确的身份登录");
            }
        } else if (!user.getRole().name().equals(role)) {
            throw new BusinessException("该用户不是" + roleName + "，请选择正确的身份登录");
        }

        return toLoginResponse(user);
    }

    @Override
    public LoginResponse getMe(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(401, "用户不存在");
        return toLoginResponse(user);
    }

    @Override
    @Transactional
    public LoginResponse updateProfile(Long userId, Long organizationId, String organizationName) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(401, "用户不存在");

        if (organizationId != null) {
            Organization org = organizationMapper.selectById(organizationId);
            if (org == null) throw new BusinessException("无效的单位");
            user.setOrganizationId(org.getId());
        } else {
            throw new BusinessException("请从列表中选择所属学院");
        }

        userMapper.updateById(user);
        return toLoginResponse(user);
    }

    // ========== Password Management ==========

    @Override
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(401, "用户不存在");

        if (!user.getPassword().equals(SecurityUtils.hashPassword(oldPassword))) {
            throw new BusinessException("原密码错误");
        }

        if (newPassword == null || newPassword.length() < 6) {
            throw new BusinessException("新密码长度不能少于6位");
        }

        user.setPassword(SecurityUtils.hashPassword(newPassword));
        userMapper.updateById(user);
    }

    // ========== Judge Management (Admin) ==========

    @Override
    @Transactional
    public LoginResponse createJudge(Long adminUserId, String username, String password,
                                      String realName, String phone, String judgeOrganization) {
        User admin = userMapper.selectById(adminUserId);
        if (admin == null) throw new BusinessException(403, "无权操作");
        AdminLevelResolver.requireAtLeast(admin,
                OrganizationLevel.COLLEGE, organizationMapper);

        // Look up or create organization by name under admin's org tree
        Long organizationId = resolveJudgeOrganization(admin, judgeOrganization);

        if (userMapper.existsByUsername(username)) {
            throw new BusinessException("用户名已存在");
        }

        User judge = new User();
        judge.setUsername(username);
        judge.setPassword(SecurityUtils.hashPassword(password));
        judge.setRealName(realName);
        judge.setPhone(phone);
        judge.setRole(UserRole.JUDGE);
        judge.setOrganizationId(organizationId);
        userMapper.insert(judge);
        return toLoginResponse(judge);
    }

    /** Find or create an organization by name within the admin's judge-level org scope */
    private Long resolveJudgeOrganization(User admin, String orgName) {
        if (orgName == null || orgName.isBlank()) return admin.getOrganizationId();
        String name = orgName.trim();
        // Only search within the admin's judge-level orgs, not all visible orgs
        List<Long> judgeOrgIds = getJudgeVisibleOrgIds(admin);

        if (!judgeOrgIds.isEmpty()) {
            List<Organization> matches = organizationMapper.selectList(
                new QueryWrapper<Organization>().lambda()
                    .in(Organization::getId, judgeOrgIds)
                    .eq(Organization::getName, name));
            if (!matches.isEmpty()) return matches.get(0).getId();
        }

        // Not found — create under admin's own org at the admin's level
        Organization adminOrg = organizationMapper.selectById(admin.getOrganizationId());
        if (adminOrg == null) return null;

        OrganizationLevel judgeLevel =
                adminOrg.getLevel() != null ? adminOrg.getLevel() : OrganizationLevel.COLLEGE;

        Organization org = new Organization();
        org.setName(name);
        org.setDescription(name + "（外部评委单位）");
        org.setInviteCode("EXT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        org.setParentId(admin.getOrganizationId());
        org.setLevel(judgeLevel);
        org.setIsExternal(true);
        organizationMapper.insert(org);
        return org.getId();
    }

    @Override
    public IPage<User> listJudges(Long adminUserId, Page<User> page) {
        User admin = userMapper.selectById(adminUserId);
        if (admin == null) throw new BusinessException(403, "无权操作");
        AdminLevelResolver.requireAtLeast(admin, OrganizationLevel.COLLEGE, organizationMapper);

        List<Long> judgeOrgIds = getJudgeVisibleOrgIds(admin);
        var wrapper = new QueryWrapper<User>().lambda()
                .eq(User::getRole, UserRole.JUDGE);
        if (judgeOrgIds.isEmpty()) {
            wrapper.isNull(User::getOrganizationId);
        } else {
            wrapper.in(User::getOrganizationId, judgeOrgIds);
        }
        return userMapper.selectPage(page, wrapper);
    }

    @Override
    @Transactional
    public void deleteJudge(Long adminUserId, Long judgeId) {
        User admin = userMapper.selectById(adminUserId);
        if (admin == null) throw new BusinessException(403, "无权操作");
        AdminLevelResolver.requireAtLeast(admin, OrganizationLevel.COLLEGE, organizationMapper);

        User judge = userMapper.selectById(judgeId);
        if (judge == null || judge.getRole() != UserRole.JUDGE) {
            throw new BusinessException("评委不存在");
        }

        // Validate judge's org is within admin's judge scope
        if (judge.getOrganizationId() != null) {
            List<Long> judgeOrgIds = getJudgeVisibleOrgIds(admin);
            if (!judgeOrgIds.isEmpty() && !judgeOrgIds.contains(judge.getOrganizationId())) {
                throw new BusinessException(403, "无权删除该评委");
            }
        }

        userMapper.deleteById(judgeId);
    }

    private LoginResponse toLoginResponse(User user) {
        LoginResponse resp = new LoginResponse();
        resp.setUserId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setRealName(user.getRealName());
        resp.setRole(user.getRole().name());
        resp.setPhone(user.getPhone());
        resp.setEmail(user.getEmail());
        if (user.getRole() == UserRole.SUPER_ADMIN) {
            resp.setOrgLevel("SUPER_ADMIN");
            resp.setOrganizationName("超级管理员");
            if (user.getOrganizationId() != null) {
                resp.setOrganizationId(user.getOrganizationId());
                resp.setUniversityName(organizationMapper.findSchoolOrgName(user.getOrganizationId()));
            }
        } else if (user.getOrganizationId() != null) {
            Organization org = organizationMapper.selectById(user.getOrganizationId());
            if (org != null) {
                resp.setOrganizationId(org.getId());
                resp.setOrganizationName(org.getName());
                resp.setUniversityName(organizationMapper.findSchoolOrgName(org.getId()));
                if (org.getLevel() != null) {
                    resp.setOrgLevel(org.getLevel().name());
                }
            }
        }
        return resp;
    }

    private List<Long> getVisibleOrgIds(User admin) {
        return AdminLevelResolver.getVisibleOrgIds(admin, organizationMapper);
    }

    /**
     * Returns org IDs where judges managed by this admin should live.
     * Judges are at the admin's own org level, so each admin level
     * independently manages its own judges — not seeing those from other levels.
     */
    private List<Long> getJudgeVisibleOrgIds(User admin) {
        List<Long> visibleOrgIds = getVisibleOrgIds(admin);
        if (visibleOrgIds.isEmpty()) return visibleOrgIds;

        // SUPER_ADMIN manages judges at all levels — skip the level filter
        if (AdminLevelResolver.isSuperAdmin(admin)) return visibleOrgIds;

        OrganizationLevel adminLevel = AdminLevelResolver.getLevel(admin, organizationMapper);
        if (adminLevel == null) return visibleOrgIds;

        List<Organization> orgs = organizationMapper.selectBatchIds(visibleOrgIds);
        return orgs.stream()
                .filter(org -> org.getLevel() == adminLevel)
                .map(Organization::getId)
                .toList();
    }
}