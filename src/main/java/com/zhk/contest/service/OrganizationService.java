package com.zhk.contest.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhk.contest.dto.request.CreateAdminAccountRequest;
import com.zhk.contest.dto.request.OrgSaveRequest;
import com.zhk.contest.dto.response.LoginResponse;
import com.zhk.contest.entity.Organization;
import com.zhk.contest.entity.User;
import com.zhk.contest.enums.OrganizationLevel;

import java.util.List;

public interface OrganizationService {
    // Public
    List<Organization> listAll();
    List<Organization> searchByName(String keyword);

    // Admin
    Organization createOrg(Long userId, OrgSaveRequest request);
    Organization updateOrg(Long userId, Long orgId, OrgSaveRequest request);
    void deleteOrg(Long userId, Long orgId);
    void batchDeleteOrgs(Long userId, List<Long> orgIds);
    LoginResponse createAdminAccount(Long userId, Long orgId, CreateAdminAccountRequest request);
    IPage<Organization> listAllForAdmin(Long userId, Page<Organization> page, String keyword, Long parentId, boolean direct);
    List<Organization> listChildren(Long userId, Long parentId);
    List<Organization> batchCreateColleges(Long userId, Long parentOrgId, List<String> names, boolean createAdminAccounts);
    List<Organization> searchByLevel(String keyword, OrganizationLevel level);
    IPage<LoginResponse> listOrgAccounts(Long userId, Long orgId, Page<User> page);

    // Invite code (both SUPER_ADMIN and UNIT_ADMIN)
    Organization getMyOrganization(Long userId);
    String regenerateMyInviteCode(Long userId);
}