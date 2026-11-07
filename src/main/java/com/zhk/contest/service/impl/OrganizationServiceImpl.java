package com.zhk.contest.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhk.contest.dto.request.CreateAdminAccountRequest;
import com.zhk.contest.dto.request.OrgSaveRequest;
import com.zhk.contest.dto.response.LoginResponse;
import com.zhk.contest.entity.Organization;
import com.zhk.contest.entity.User;
import com.zhk.contest.enums.OrganizationLevel;
import com.zhk.contest.enums.UserRole;
import com.zhk.contest.exception.BusinessException;
import com.zhk.contest.mapper.OrganizationMapper;
import com.zhk.contest.mapper.UserMapper;
import com.zhk.contest.service.OrganizationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zhk.contest.util.SecurityUtils;
import com.zhk.contest.util.AdminLevelResolver;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationMapper organizationMapper;
    private final UserMapper userMapper;

    /** 自动创建单位管理员时的初始密码；来自环境变量，未配置则回退为随机密码 */
    private final String defaultAdminPassword;

    public OrganizationServiceImpl(OrganizationMapper organizationMapper,
                                   UserMapper userMapper,
                                   @Value("${app.init.default-admin-password:}") String defaultAdminPassword) {
        this.organizationMapper = organizationMapper;
        this.userMapper = userMapper;
        this.defaultAdminPassword = defaultAdminPassword;
    }

    @Override
    public List<Organization> listAll() {
        return organizationMapper.selectList(
                new QueryWrapper<Organization>().lambda()
                        .eq(Organization::getIsExternal, false));
    }

    @Override
    public List<Organization> searchByName(String keyword) {
        QueryWrapper<Organization> wrapper = new QueryWrapper<>();
        wrapper.lambda()
                .eq(Organization::getIsExternal, false)
                .like(Organization::getName, keyword);
        return organizationMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public Organization createOrg(Long userId, OrgSaveRequest request) {
        User admin = checkAdmin(userId);
        boolean isSuperAdmin = admin.getRole() == UserRole.SUPER_ADMIN;

        OrganizationLevel childLevel;
        Long parentId = request.getParentId();

        if (parentId == null) {
            if (!isSuperAdmin) {
                throw new BusinessException(403, "无法创建顶级单位，请联系平台管理员");
            }
            // SUPER_ADMIN 创建无上级单位的组织，层级由前端指定
            if (request.getLevel() != null && !request.getLevel().isBlank()) {
                try {
                    childLevel = OrganizationLevel.valueOf(request.getLevel());
                } catch (IllegalArgumentException e) {
                    throw new BusinessException("无效的组织层级：" + request.getLevel());
                }
            } else {
                // 未指定时默认国家级
                childLevel = OrganizationLevel.NATIONAL;
            }
        } else {
            // 有上级单位时，执行常规的权限和层级校验
            List<Long> visibleOrgIds = getVisibleOrgIds(admin);
            if (!visibleOrgIds.contains(parentId)) {
                throw new BusinessException(403, "父单位不在管辖范围内");
            }
            Organization parentOrg = organizationMapper.selectById(parentId);
            if (parentOrg != null && Boolean.TRUE.equals(parentOrg.getIsExternal())) {
                throw new BusinessException("不能在外部队列单位下创建子单位");
            }
            childLevel = OrganizationLevel.COLLEGE;
            if (parentOrg != null && parentOrg.getLevel() != null) {
                childLevel = parentOrg.getLevel().nextDown();
                if (childLevel == null) {
                    throw new BusinessException("已达最低层级，无法继续创建下级单位");
                }
            }
        }

        Organization org = new Organization();
        org.setName(request.getName());
        org.setDescription(request.getDescription());
        org.setLevel(childLevel);
        org.setInviteCode("ORG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        org.setParentId(parentId);
        organizationMapper.insert(org);
        return org;
    }

    @Override
    @Transactional
    public Organization updateOrg(Long userId, Long orgId, OrgSaveRequest request) {
        User admin = checkAdmin(userId);

        Organization org = organizationMapper.selectById(orgId);
        if (org == null) throw new BusinessException("单位不存在");

        List<Long> visibleOrgIds = getVisibleOrgIds(admin);
        if (!visibleOrgIds.contains(orgId)) {
            throw new BusinessException(403, "无权编辑该单位");
        }
        if (request.getParentId() != null && !visibleOrgIds.contains(request.getParentId())) {
            throw new BusinessException(403, "父单位不在管辖范围内");
        }

        org.setName(request.getName());
        org.setDescription(request.getDescription());
        org.setParentId(request.getParentId());
        organizationMapper.updateById(org);
        return org;
    }

    @Override
    @Transactional
    public void deleteOrg(Long userId, Long orgId) {
        User admin = checkAdmin(userId);

        Organization org = organizationMapper.selectById(orgId);
        if (org == null) throw new BusinessException("单位不存在");

        List<Long> visibleOrgIds = getVisibleOrgIds(admin);
        if (!visibleOrgIds.contains(orgId)) {
            throw new BusinessException(403, "无权删除该单位");
        }

        // 删除该单位下的所有账号（管理员、参赛者、评委）
        userMapper.delete(
                new QueryWrapper<User>().lambda().eq(User::getOrganizationId, orgId));

        organizationMapper.deleteById(orgId);
    }

    @Override
    @Transactional
    public void batchDeleteOrgs(Long userId, List<Long> orgIds) {
        User admin = checkAdmin(userId);
        List<Long> visibleOrgIds = getVisibleOrgIds(admin);

        // 收集所有待删除的 org ID（含子单位）
        Set<Long> allToDelete = new HashSet<>();
        for (Long orgId : orgIds) {
            if (!visibleOrgIds.contains(orgId)) {
                throw new BusinessException(403, "无权删除单位：" + orgId);
            }
            List<Long> treeIds = organizationMapper.findOrgTreeIds(orgId);
            allToDelete.addAll(treeIds);
        }

        if (allToDelete.isEmpty()) return;

        // 先删除这些单位下的所有用户 → 级联删除其项目
        userMapper.delete(
                new QueryWrapper<User>().lambda().in(User::getOrganizationId, allToDelete));

        // 再删除单位本身
        organizationMapper.deleteBatchIds(new ArrayList<>(allToDelete));
    }

    @Override
    @Transactional
    public LoginResponse createAdminAccount(Long userId, Long orgId, CreateAdminAccountRequest request) {
        checkAdmin(userId);

        Organization org = organizationMapper.selectById(orgId);
        if (org == null) throw new BusinessException("单位不存在");

        if (userMapper.existsByUsername(request.getUsername())) {
            throw new BusinessException("用户名已存在");
        }

        User admin = new User();
        admin.setUsername(request.getUsername());
        admin.setPassword(SecurityUtils.hashPassword(request.getPassword()));
        admin.setRealName(request.getRealName());
        admin.setPhone(request.getPhone());
        admin.setRole(UserRole.ADMIN);
        admin.setOrganizationId(orgId);
        admin.setCreatedBy(userId);
        userMapper.insert(admin);

        return toLoginResponse(admin);
    }

    @Override
    public List<Organization> searchByLevel(String keyword, OrganizationLevel level) {
        QueryWrapper<Organization> wrapper = new QueryWrapper<>();
        wrapper.lambda()
                .eq(Organization::getIsExternal, false)
                .eq(Organization::getLevel, level)
                .like(Organization::getName, keyword);
        return organizationMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public List<Organization> batchCreateColleges(Long userId, Long parentOrgId, List<String> names, boolean createAdminAccounts) {
        User admin = checkAdmin(userId);
        List<Long> visibleOrgIds = getVisibleOrgIds(admin);
        if (!visibleOrgIds.contains(parentOrgId)) {
            throw new BusinessException(403, "父单位不在管辖范围内");
        }

        Organization parentOrg = organizationMapper.selectById(parentOrgId);
        OrganizationLevel childLevel = parentOrg != null && parentOrg.getLevel() != null
                ? parentOrg.getLevel().nextDown() : OrganizationLevel.COLLEGE;
        if (childLevel == null) {
            throw new BusinessException("已达最低层级，无法继续创建下级单位");
        }

        List<Organization> created = new ArrayList<>();
        for (String name : names) {
            String trimmed = name.trim();
            if (trimmed.isEmpty()) continue;

            Long exists = organizationMapper.selectCount(
                    new QueryWrapper<Organization>().lambda()
                            .eq(Organization::getParentId, parentOrgId)
                            .eq(Organization::getName, trimmed));
            if (exists > 0) continue;

            Organization org = new Organization();
            org.setName(trimmed);
            org.setDescription(trimmed);
            org.setLevel(childLevel);
            org.setInviteCode(childLevel.name().substring(0, 4) + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            org.setParentId(parentOrgId);
            organizationMapper.insert(org);
            created.add(org);

            if (createAdminAccounts) {
                String username = "admin_" + org.getId();
                if (!userMapper.existsByUsername(username)) {
                    String initPassword = (defaultAdminPassword != null && !defaultAdminPassword.isBlank())
                            ? defaultAdminPassword : SecurityUtils.generateRandomPassword(12);
                    User unitAdmin = new User();
                    unitAdmin.setUsername(username);
                    unitAdmin.setPassword(SecurityUtils.hashPassword(initPassword));
                    unitAdmin.setRealName(trimmed + "管理员");
                    unitAdmin.setRole(UserRole.ADMIN);
                    unitAdmin.setOrganizationId(org.getId());
                    unitAdmin.setCreatedBy(userId);
                    userMapper.insert(unitAdmin);
                }
            }
        }

        return created;
    }

    @Override
    public IPage<Organization> listAllForAdmin(Long userId, Page<Organization> page, String keyword, Long parentId, boolean direct) {
        User admin = checkAdmin(userId);

        List<Long> visibleOrgIds = getVisibleOrgIds(admin);
        if (visibleOrgIds.isEmpty()) {
            return new Page<>(page.getCurrent(), page.getSize());
        }

        var wrapper = new QueryWrapper<Organization>().lambda()
                .eq(Organization::getIsExternal, false);

        if (direct) {
            Long adminOrgId = admin.getOrganizationId();
            boolean isSuper = admin.getRole() == UserRole.SUPER_ADMIN;

            if (isSuper && adminOrgId == null) {
                // Super admin without an org: show top-level orgs (parent_id IS NULL)
                wrapper.isNull(Organization::getParentId);
            } else {
                // Exclude admin's own org from listing
                wrapper.ne(Organization::getId, adminOrgId);
                if (isSuper) {
                    // Super admin: direct children + top-level orgs (parentless)
                    wrapper.and(w -> w.eq(Organization::getParentId, adminOrgId)
                                     .or().isNull(Organization::getParentId));
                } else {
                    // Other admins: only direct children
                    wrapper.eq(Organization::getParentId, adminOrgId);
                }
            }
        } else if (parentId != null) {
            if (!visibleOrgIds.contains(parentId)) {
                throw new BusinessException(403, "无权查看该单位的子单位");
            }
            wrapper.eq(Organization::getParentId, parentId);
        } else {
            wrapper.in(Organization::getId, visibleOrgIds);
        }

        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(Organization::getName, keyword);
        }
        return organizationMapper.selectPage(page, wrapper);
    }

    @Override
    public List<Organization> listChildren(Long userId, Long parentId) {
        User admin = checkAdmin(userId);
        List<Long> visibleOrgIds = getVisibleOrgIds(admin);
        if (!visibleOrgIds.contains(parentId)) {
            throw new BusinessException(403, "无权查看该单位的下级");
        }
        QueryWrapper<Organization> wrapper = new QueryWrapper<>();
        wrapper.lambda()
                .eq(Organization::getIsExternal, false)
                .eq(Organization::getParentId, parentId);
        return organizationMapper.selectList(wrapper);
    }

    @Override
    public Organization getMyOrganization(Long userId) {
        User user = checkAdminExists(userId);
        if (user.getOrganizationId() == null) {
            throw new BusinessException("您尚未分配到单位");
        }
        Organization org = organizationMapper.selectById(user.getOrganizationId());
        if (org == null) throw new BusinessException("单位不存在");
        return org;
    }

    @Override
    @Transactional
    public String regenerateMyInviteCode(Long userId) {
        User user = checkAdminExists(userId);
        if (user.getOrganizationId() == null) {
            throw new BusinessException("您尚未分配到单位");
        }
        Organization org = organizationMapper.selectById(user.getOrganizationId());
        if (org == null) throw new BusinessException("单位不存在");

        String newCode = "ORG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        org.setInviteCode(newCode);
        organizationMapper.updateById(org);
        return newCode;
    }

    private User checkAdmin(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(401, "用户不存在");
        if (user.getRole() != UserRole.ADMIN && user.getRole() != UserRole.SUPER_ADMIN) {
            throw new BusinessException(403, "无权操作");
        }
        return user;
    }

    private User checkAdminExists(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(401, "用户不存在");
        return user;
    }

    private List<Long> getVisibleOrgIds(User admin) {
        return AdminLevelResolver.getVisibleOrgIds(admin, organizationMapper);
    }

    @Override
    public IPage<LoginResponse> listOrgAccounts(Long userId, Long orgId, Page<User> page) {
        User currentUser = userMapper.selectById(userId);
        if (currentUser == null || (currentUser.getRole() != UserRole.ADMIN && currentUser.getRole() != UserRole.SUPER_ADMIN)) {
            throw new BusinessException("无权限");
        }

        OrganizationLevel level = AdminLevelResolver.getLevel(currentUser, organizationMapper);
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("role", UserRole.ADMIN).eq("organization_id", orgId);
        if (level == null || AdminLevelResolver.isSuperAdmin(currentUser)) {
            // SUPER_ADMIN only sees accounts they created
            wrapper.eq("created_by", userId);
        }

        IPage<User> userPage = userMapper.selectPage(page, wrapper);
        List<LoginResponse> list = userPage.getRecords().stream().map(u -> {
            LoginResponse resp = new LoginResponse();
            resp.setUserId(u.getId());
            resp.setUsername(u.getUsername());
            resp.setRealName(u.getRealName());
            resp.setRole(u.getRole().name());
            resp.setPhone(u.getPhone());
            resp.setOrganizationId(u.getOrganizationId());
            if (u.getCreatedAt() != null) {
                resp.setCreatedAt(u.getCreatedAt().toString().replace("T", " "));
            }
            return resp;
        }).collect(Collectors.toList());

        IPage<LoginResponse> result = new Page<>(page.getCurrent(), page.getSize(), userPage.getTotal());
        result.setRecords(list);
        return result;
    }

    private LoginResponse toLoginResponse(User user) {
        LoginResponse resp = new LoginResponse();
        resp.setUserId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setRealName(user.getRealName());
        resp.setRole(user.getRole().name());
        if (user.getOrganizationId() != null) {
            Organization org = organizationMapper.selectById(user.getOrganizationId());
            if (org != null) {
                resp.setOrganizationId(org.getId());
                resp.setOrganizationName(org.getName());
                resp.setUniversityName(organizationMapper.findSchoolOrgName(org.getId()));
            }
        }
        return resp;
    }

}
