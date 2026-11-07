package com.zhk.contest.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhk.contest.dto.request.BatchCreateCollegesRequest;
import com.zhk.contest.dto.request.CreateAdminAccountRequest;
import com.zhk.contest.dto.request.OrgSaveRequest;
import com.zhk.contest.dto.response.ApiResponse;
import com.zhk.contest.dto.response.LoginResponse;
import com.zhk.contest.entity.Organization;
import com.zhk.contest.service.OrganizationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/organizations")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    /** Public: list all organizations */
    @GetMapping
    public ApiResponse<List<Organization>> list() {
        return ApiResponse.success(organizationService.listAll());
    }

    /** Public: search organizations */
    @GetMapping("/search")
    public ApiResponse<List<Organization>> search(@RequestParam String keyword) {
        return ApiResponse.success(organizationService.searchByName(keyword));
    }

    // ========== Admin endpoints ==========

    /** Admin: list all organizations (with stats, paginated) */
    @GetMapping("/admin/all")
    public ApiResponse<IPage<Organization>> listAll(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long parentId,
            @RequestParam(required = false, defaultValue = "false") boolean direct,
            HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("adminUserId");
        return ApiResponse.success(organizationService.listAllForAdmin(userId, new Page<>(current, size), keyword, parentId, direct));
    }

    /** Admin: create organization */
    @PostMapping("/admin")
    public ApiResponse<Organization> create(@Valid @RequestBody OrgSaveRequest request, HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("adminUserId");
        return ApiResponse.success(organizationService.createOrg(userId, request));
    }

    /** Admin: update organization */
    @PutMapping("/admin/{orgId}")
    public ApiResponse<Organization> update(@PathVariable Long orgId,
                                             @Valid @RequestBody OrgSaveRequest request,
                                             HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("adminUserId");
        return ApiResponse.success(organizationService.updateOrg(userId, orgId, request));
    }

    /** Admin: delete organization */
    @DeleteMapping("/admin/{orgId}")
    public ApiResponse<Void> delete(@PathVariable Long orgId, HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("adminUserId");
        organizationService.deleteOrg(userId, orgId);
        return ApiResponse.success(null);
    }

    /** Admin: batch delete organizations (with all subordinate accounts) */
    @PostMapping("/admin/batch-delete")
    public ApiResponse<Void> batchDelete(@RequestBody List<Long> orgIds, HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("adminUserId");
        organizationService.batchDeleteOrgs(userId, orgIds);
        return ApiResponse.success(null);
    }

    /** Admin: create UNIT_ADMIN account for an organization */
    @PostMapping("/admin/{orgId}/accounts")
    public ApiResponse<LoginResponse> createAdminAccount(@PathVariable Long orgId,
                                                          @Valid @RequestBody CreateAdminAccountRequest request,
                                                          HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("adminUserId");
        return ApiResponse.success(organizationService.createAdminAccount(userId, orgId, request));
    }

    /** Admin: list admin accounts for an organization */
    @GetMapping("/admin/{orgId}/accounts")
    public ApiResponse<IPage<LoginResponse>> listAccounts(@PathVariable Long orgId,
                                                          @RequestParam(defaultValue = "1") int current,
                                                          @RequestParam(defaultValue = "20") int size,
                                                          HttpServletRequest req) {
        Long adminUserId = (Long) req.getAttribute("adminUserId");
        return ApiResponse.success(organizationService.listOrgAccounts(adminUserId, orgId,
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(current, size)));
    }

    /** Admin: list child organizations under a parent */
    @GetMapping("/admin/{parentId}/children")
    public ApiResponse<List<Organization>> listChildren(@PathVariable Long parentId, HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("adminUserId");
        return ApiResponse.success(organizationService.listChildren(userId, parentId));
    }

    /** Admin: lookup provinces (hardcoded list) */
    @GetMapping("/admin/province-lookup")
    public ApiResponse<List<String>> lookupProvinces(HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("adminUserId");
        // 中国 34 个省级行政区
        List<String> provinces = List.of(
                "北京市", "天津市", "上海市", "重庆市",
                "河北省", "山西省", "辽宁省", "吉林省", "黑龙江省",
                "江苏省", "浙江省", "安徽省", "福建省", "江西省", "山东省",
                "河南省", "湖北省", "湖南省", "广东省", "海南省",
                "四川省", "贵州省", "云南省", "陕西省", "甘肃省", "青海省", "台湾省",
                "内蒙古自治区", "广西壮族自治区", "西藏自治区", "宁夏回族自治区", "新疆维吾尔自治区",
                "香港特别行政区", "澳门特别行政区");
        return ApiResponse.success(provinces);
    }

    /** Admin: batch create child organizations (colleges) under a parent */
    @PostMapping("/admin/batch-create")
    public ApiResponse<List<Organization>> batchCreate(@Valid @RequestBody BatchCreateCollegesRequest request,
                                                        HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("adminUserId");
        return ApiResponse.success(organizationService.batchCreateColleges(
                userId, request.getParentOrgId(), request.getCollegeNames(), request.isCreateAdminAccounts()));
    }

    /** Admin (both SUPER_ADMIN and UNIT_ADMIN): get my own organization info (with invite code) */
    @GetMapping("/admin/my-org")
    public ApiResponse<Organization> getMyOrg(HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("adminUserId");
        return ApiResponse.success(organizationService.getMyOrganization(userId));
    }

    /** Admin (both SUPER_ADMIN and UNIT_ADMIN): regenerate invite code for my organization */
    @PutMapping("/admin/my-invite-code/regenerate")
    public ApiResponse<String> regenerateMyInviteCode(HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("adminUserId");
        return ApiResponse.success(organizationService.regenerateMyInviteCode(userId));
    }
}
