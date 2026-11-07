package com.zhk.contest.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhk.contest.dto.request.*;
import com.zhk.contest.dto.response.*;
import org.springframework.web.multipart.MultipartFile;

public interface ProjectService {
    ProjectDetailResponse createProject(Long userId, ProjectSaveRequest request);
    ProjectDetailResponse updateProject(Long userId, Long projectId, ProjectSaveRequest request);
    ProjectDetailResponse getProject(Long userId, Long projectId);
    IPage<ProjectListItemResponse> listProjects(Long userId, int current, int size);

    ProjectDetailResponse submitProject(Long userId, Long projectId);
    ProjectDetailResponse withdrawProject(Long userId, Long projectId);
    void deleteProject(Long userId, Long projectId);

    MemberResponse addMember(Long userId, Long projectId, TeamMemberRequest request);
    MemberResponse updateMember(Long userId, Long projectId, Long memberId, TeamMemberRequest request);
    void deleteMember(Long userId, Long projectId, Long memberId);

    AdvisorResponse addAdvisor(Long userId, Long projectId, AdvisorRequest request);
    AdvisorResponse updateAdvisor(Long userId, Long projectId, Long advisorId, AdvisorRequest request);
    void deleteAdvisor(Long userId, Long projectId, Long advisorId);

    MaterialResponse uploadMaterial(Long userId, Long projectId, String materialType, MultipartFile file);
    void deleteMaterial(Long userId, Long projectId, Long materialId);

    // Admin operations
    IPage<ProjectAdminListItemResponse> listProjectsForAdmin(Long userId, int current, int size,
                                                             Long eventId, Long trackId, Long organizationId,
                                                             String keyword, String status);
    ProjectDetailResponse adminGetProject(Long userId, Long projectId);
    void reviewProject(Long userId, Long projectId, ProjectReviewRequest request);
    void batchReviewProjects(Long userId, BatchReviewRequest request);
}