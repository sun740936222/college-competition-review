package com.zhk.contest.util;

import com.zhk.contest.entity.Organization;
import com.zhk.contest.entity.User;
import com.zhk.contest.enums.OrganizationLevel;
import com.zhk.contest.enums.UserRole;
import com.zhk.contest.exception.BusinessException;
import com.zhk.contest.mapper.OrganizationMapper;

import java.util.List;
import java.util.stream.Collectors;

public class AdminLevelResolver {

    private AdminLevelResolver() {}

    /**
     * Check if the user is a super admin (no org, full access).
     */
    public static boolean isSuperAdmin(User admin) {
        return admin != null && admin.getRole() == UserRole.SUPER_ADMIN;
    }

    public static OrganizationLevel getLevel(User admin, OrganizationMapper orgMapper) {
        if (admin == null) return null;
        if (isSuperAdmin(admin)) return OrganizationLevel.NATIONAL; // highest level for ordering purposes
        if (admin.getOrganizationId() == null) return null;
        Organization org = orgMapper.selectById(admin.getOrganizationId());
        return org != null ? org.getLevel() : null;
    }

    /** 返回 admin 自身及所有下级组织的 ID（向下查子树）；SUPER_ADMIN 返回所有组织 */
    public static List<Long> getVisibleOrgIds(User admin, OrganizationMapper orgMapper) {
        if (admin == null) return List.of();
        if (isSuperAdmin(admin)) {
            // Super admin sees all organizations
            return orgMapper.selectList(null).stream()
                    .map(Organization::getId)
                    .collect(Collectors.toList());
        }
        if (admin.getOrganizationId() == null) return List.of();
        return orgMapper.findOrgTreeIds(admin.getOrganizationId());
    }

    /** 返回 admin 自身及所有上级祖先组织的 ID（向上查祖先）；SUPER_ADMIN 返回空列表（无祖先概念） */
    public static List<Long> getAncestorOrgIds(User admin, OrganizationMapper orgMapper) {
        if (admin == null) return List.of();
        if (isSuperAdmin(admin)) return List.of();
        if (admin.getOrganizationId() == null) return List.of();
        return orgMapper.findAncestorOrgIds(admin.getOrganizationId());
    }

    public static void requireAtLeast(User admin, OrganizationLevel required, OrganizationMapper orgMapper) {
        if (isSuperAdmin(admin)) return; // SUPER_ADMIN always passes
        OrganizationLevel adminLevel = getLevel(admin, orgMapper);
        if (adminLevel == null || adminLevel.ordinal() < required.ordinal()) {
            throw new BusinessException(403, "无权操作：需要" + required.name() + "及以上权限");
        }
    }

    public static void requireLevel(User admin, OrganizationLevel required, OrganizationMapper orgMapper) {
        if (isSuperAdmin(admin)) return; // SUPER_ADMIN always passes
        OrganizationLevel adminLevel = getLevel(admin, orgMapper);
        if (adminLevel == null || !adminLevel.equals(required)) {
            throw new BusinessException(403, "无权操作：需要" + required.name() + "级别权限");
        }
    }
}
