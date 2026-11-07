package com.zhk.contest.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhk.contest.dto.request.*;
import com.zhk.contest.dto.response.*;
import com.zhk.contest.service.ProjectService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    private Long getUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }

    private Long getAdminUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("adminUserId");
    }

    @PostMapping
    public ApiResponse<ProjectDetailResponse> create(@Valid @RequestBody ProjectSaveRequest request, HttpServletRequest req) {
        return ApiResponse.success(projectService.createProject(getUserId(req), request));
    }

    @GetMapping
    public ApiResponse<IPage<ProjectListItemResponse>> list(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "12") int size,
            HttpServletRequest req) {
        return ApiResponse.success(projectService.listProjects(getUserId(req), current, size));
    }

    @GetMapping("/{projectId}")
    public ApiResponse<ProjectDetailResponse> detail(@PathVariable Long projectId, HttpServletRequest req) {
        return ApiResponse.success(projectService.getProject(getUserId(req), projectId));
    }

    @PutMapping("/{projectId}")
    public ApiResponse<ProjectDetailResponse> update(@PathVariable Long projectId,
                                                     @Valid @RequestBody ProjectSaveRequest request,
                                                     HttpServletRequest req) {
        return ApiResponse.success(projectService.updateProject(getUserId(req), projectId, request));
    }

    @DeleteMapping("/{projectId}")
    public ApiResponse<Void> delete(@PathVariable Long projectId, HttpServletRequest req) {
        projectService.deleteProject(getUserId(req), projectId);
        return ApiResponse.success(null);
    }

    @PutMapping("/{projectId}/submit")
    public ApiResponse<ProjectDetailResponse> submit(@PathVariable Long projectId, HttpServletRequest req) {
        return ApiResponse.success(projectService.submitProject(getUserId(req), projectId));
    }

    @PutMapping("/{projectId}/withdraw")
    public ApiResponse<ProjectDetailResponse> withdraw(@PathVariable Long projectId, HttpServletRequest req) {
        return ApiResponse.success(projectService.withdrawProject(getUserId(req), projectId));
    }

    @PostMapping("/{projectId}/members")
    public ApiResponse<MemberResponse> addMember(@PathVariable Long projectId,
                                                 @Valid @RequestBody TeamMemberRequest request,
                                                 HttpServletRequest req) {
        return ApiResponse.success(projectService.addMember(getUserId(req), projectId, request));
    }

    @PutMapping("/{projectId}/members/{memberId}")
    public ApiResponse<MemberResponse> updateMember(@PathVariable Long projectId,
                                                    @PathVariable Long memberId,
                                                    @Valid @RequestBody TeamMemberRequest request,
                                                    HttpServletRequest req) {
        return ApiResponse.success(projectService.updateMember(getUserId(req), projectId, memberId, request));
    }

    @DeleteMapping("/{projectId}/members/{memberId}")
    public ApiResponse<Void> deleteMember(@PathVariable Long projectId,
                                          @PathVariable Long memberId,
                                          HttpServletRequest req) {
        projectService.deleteMember(getUserId(req), projectId, memberId);
        return ApiResponse.success(null);
    }

    @PostMapping("/{projectId}/advisors")
    public ApiResponse<AdvisorResponse> addAdvisor(@PathVariable Long projectId,
                                                   @Valid @RequestBody AdvisorRequest request,
                                                   HttpServletRequest req) {
        return ApiResponse.success(projectService.addAdvisor(getUserId(req), projectId, request));
    }

    @PutMapping("/{projectId}/advisors/{advisorId}")
    public ApiResponse<AdvisorResponse> updateAdvisor(@PathVariable Long projectId,
                                                      @PathVariable Long advisorId,
                                                      @Valid @RequestBody AdvisorRequest request,
                                                      HttpServletRequest req) {
        return ApiResponse.success(projectService.updateAdvisor(getUserId(req), projectId, advisorId, request));
    }

    @DeleteMapping("/{projectId}/advisors/{advisorId}")
    public ApiResponse<Void> deleteAdvisor(@PathVariable Long projectId,
                                           @PathVariable Long advisorId,
                                           HttpServletRequest req) {
        projectService.deleteAdvisor(getUserId(req), projectId, advisorId);
        return ApiResponse.success(null);
    }

    @PostMapping("/{projectId}/materials/{materialType}")
    public ApiResponse<MaterialResponse> uploadMaterial(@PathVariable Long projectId,
                                                        @PathVariable String materialType,
                                                        @RequestParam("file") MultipartFile file,
                                                        HttpServletRequest req) {
        return ApiResponse.success(projectService.uploadMaterial(getUserId(req), projectId, materialType, file));
    }

    @DeleteMapping("/{projectId}/materials/{materialId}")
    public ApiResponse<Void> deleteMaterial(@PathVariable Long projectId,
                                            @PathVariable Long materialId,
                                            HttpServletRequest req) {
        projectService.deleteMaterial(getUserId(req), projectId, materialId);
        return ApiResponse.success(null);
    }

    // ========== Admin endpoints ==========

    @GetMapping("/admin/list")
    public ApiResponse<IPage<ProjectAdminListItemResponse>> adminList(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) Long trackId,
            @RequestParam(required = false) Long organizationId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            HttpServletRequest req) {
        return ApiResponse.success(
                projectService.listProjectsForAdmin(getAdminUserId(req), current, size,
                        eventId, trackId, organizationId, keyword, status));
    }

    @GetMapping("/admin/{projectId}")
    public ApiResponse<ProjectDetailResponse> adminDetail(@PathVariable Long projectId, HttpServletRequest req) {
        return ApiResponse.success(projectService.adminGetProject(getAdminUserId(req), projectId));
    }

    @PutMapping("/admin/{projectId}/review")
    public ApiResponse<Void> review(@PathVariable Long projectId,
                                     @RequestBody ProjectReviewRequest request,
                                     HttpServletRequest req) {
        projectService.reviewProject(getAdminUserId(req), projectId, request);
        return ApiResponse.success(null);
    }

    @PostMapping("/admin/batch-review")
    public ApiResponse<Void> batchReview(@RequestBody BatchReviewRequest request, HttpServletRequest req) {
        projectService.batchReviewProjects(getAdminUserId(req), request);
        return ApiResponse.success(null);
    }
}
