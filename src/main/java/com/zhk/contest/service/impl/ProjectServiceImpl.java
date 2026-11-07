package com.zhk.contest.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhk.contest.dto.request.*;
import com.zhk.contest.dto.response.*;
import com.zhk.contest.entity.*;
import com.zhk.contest.enums.CompetitionLevel;
import com.zhk.contest.enums.MaterialType;
import com.zhk.contest.enums.MemberRole;
import com.zhk.contest.enums.OrganizationLevel;
import com.zhk.contest.enums.ProjectStatus;
import com.zhk.contest.exception.BusinessException;
import com.zhk.contest.mapper.*;
import com.zhk.contest.service.FileStorageService;
import com.zhk.contest.service.ProjectService;
import com.zhk.contest.util.AdminLevelResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProjectServiceImpl implements ProjectService {

    private final ProjectMapper projectMapper;
    private final UserMapper userMapper;
    private final EventMapper eventMapper;
    private final TrackMapper trackMapper;
    private final TeamMemberMapper teamMemberMapper;
    private final AdvisorMapper advisorMapper;
    private final ProjectMaterialMapper materialMapper;
    private final OrganizationMapper organizationMapper;
    private final ReviewLogMapper reviewLogMapper;
    private final FileStorageService fileStorageService;

    private static final long MAX_PDF_SIZE = 50 * 1024 * 1024L;
    private static final long MAX_SUPPORTING_PDF_SIZE = 100 * 1024 * 1024L;
    private static final long MAX_VIDEO_SIZE = 200 * 1024 * 1024L;

    public ProjectServiceImpl(ProjectMapper projectMapper, UserMapper userMapper,
                              EventMapper eventMapper, TrackMapper trackMapper,
                              TeamMemberMapper teamMemberMapper, AdvisorMapper advisorMapper,
                              ProjectMaterialMapper materialMapper, OrganizationMapper organizationMapper,
                              ReviewLogMapper reviewLogMapper,
                              FileStorageService fileStorageService) {
        this.projectMapper = projectMapper;
        this.userMapper = userMapper;
        this.eventMapper = eventMapper;
        this.trackMapper = trackMapper;
        this.teamMemberMapper = teamMemberMapper;
        this.advisorMapper = advisorMapper;
        this.materialMapper = materialMapper;
        this.organizationMapper = organizationMapper;
        this.reviewLogMapper = reviewLogMapper;
        this.fileStorageService = fileStorageService;
    }

    @Override
    @Transactional
    public ProjectDetailResponse createProject(Long userId, ProjectSaveRequest request) {
        userMapper.selectById(userId);
        Event event = eventMapper.selectById(request.getEventId());
        if (event == null) {
            throw new BusinessException("赛事不存在");
        }
        if (event.getRegistrationDeadline() != null && LocalDateTime.now().isAfter(event.getRegistrationDeadline())) {
            throw new BusinessException("报名已截止");
        }
        Track track = trackMapper.selectById(request.getTrackId());
        if (track == null) throw new BusinessException("赛道不存在");

        if (!track.getEventId().equals(request.getEventId())) {
            throw new BusinessException("赛道不属于该赛事");
        }

        Project project = new Project();
        project.setUserId(userId);
        project.setEventId(request.getEventId());
        project.setTrackId(request.getTrackId());
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setStatus(ProjectStatus.DRAFT);
        projectMapper.insert(project);

        // Save team members
        if (request.getMembers() != null) {
            for (int i = 0; i < request.getMembers().size(); i++) {
                TeamMemberRequest m = request.getMembers().get(i);
                TeamMember member = new TeamMember();
                member.setProjectId(project.getId());
                member.setName(m.getName());
                member.setStudentId(m.getStudentId());
                member.setDepartment(m.getDepartment());
                member.setMajor(m.getMajor());
                member.setPhone(m.getPhone());
                member.setMemberRole(m.getMemberRole() != null ? MemberRole.valueOf(m.getMemberRole()) : MemberRole.MEMBER);
                member.setSortOrder(m.getSortOrder() != null ? m.getSortOrder() : i);
                teamMemberMapper.insert(member);
            }
        }

        // Save advisors
        if (request.getAdvisors() != null) {
            for (AdvisorRequest a : request.getAdvisors()) {
                Advisor advisor = new Advisor();
                advisor.setProjectId(project.getId());
                advisor.setName(a.getName());
                advisor.setPhone(a.getPhone());
                advisorMapper.insert(advisor);
            }
        }

        return toDetailResponse(project);
    }

    @Override
    @Transactional
    public ProjectDetailResponse updateProject(Long userId, Long projectId, ProjectSaveRequest request) {
        Project project = getOwnedProject(userId, projectId);

        if (project.getStatus() != ProjectStatus.DRAFT && project.getStatus() != ProjectStatus.REJECTED) {
            throw new BusinessException("只有草稿或已退回状态的项目可以编辑");
        }

        if (!request.getEventId().equals(project.getEventId()) ||
                !request.getTrackId().equals(project.getTrackId())) {
            eventMapper.selectById(request.getEventId());
            Track track = trackMapper.selectById(request.getTrackId());
            if (track == null) throw new BusinessException("赛道不存在");
            if (!track.getEventId().equals(request.getEventId())) {
                throw new BusinessException("赛道不属于该赛事");
            }
            project.setEventId(request.getEventId());
            project.setTrackId(request.getTrackId());
        }

        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setUpdatedAt(LocalDateTime.now());
        projectMapper.updateById(project);
        return toDetailResponse(project);
    }

    @Override
    public ProjectDetailResponse getProject(Long userId, Long projectId) {
        Project project = getOwnedProject(userId, projectId);
        return toDetailResponse(project);
    }

    @Override
    public IPage<ProjectListItemResponse> listProjects(Long userId, int current, int size) {
        Page<Project> page = new Page<>(current, size);
        IPage<Project> projectPage = projectMapper.selectPageByUserId(page, userId);
        return projectPage.convert(this::toListItemResponse);
    }

    @Override
    @Transactional
    public ProjectDetailResponse submitProject(Long userId, Long projectId) {
        Project project = getOwnedProject(userId, projectId);

        if (project.getStatus() != ProjectStatus.DRAFT && project.getStatus() != ProjectStatus.REJECTED) {
            throw new BusinessException("只有草稿或已退回状态的项目可以提交");
        }

        Event event = eventMapper.selectById(project.getEventId());
        if (event.getSubmissionDeadline() != null && LocalDateTime.now().isAfter(event.getSubmissionDeadline())) {
            throw new BusinessException("提交截止时间已过，无法提交");
        }

        List<TeamMember> members = teamMemberMapper.findByProjectIdOrderBySortOrder(projectId);
        boolean hasLeader = members.stream().anyMatch(m -> m.getMemberRole() == MemberRole.LEADER);
        if (!hasLeader) {
            throw new BusinessException("请至少添加一名项目负责人");
        }

        List<ProjectMaterial> materials = materialMapper.findByProjectId(projectId);
        boolean hasPitchPdf = materials.stream().anyMatch(m -> m.getMaterialType() == MaterialType.PITCH_PDF);
        boolean hasPlanPdf = materials.stream().anyMatch(m -> m.getMaterialType() == MaterialType.PROJECT_PLAN_PDF);
        boolean hasVideo = materials.stream().anyMatch(m -> m.getMaterialType() == MaterialType.VIDEO_MP4);

        if (!hasPitchPdf) {
            throw new BusinessException("请上传路演材料PDF");
        }
        if (!hasPlanPdf) {
            throw new BusinessException("请上传项目计划书PDF");
        }
        if (Boolean.TRUE.equals(event.getRequireVideo()) && !hasVideo) {
            throw new BusinessException("请上传项目展示视频");
        }

        project.setStatus(ProjectStatus.PENDING_REVIEW);
        project.setCurrentReviewLevel(OrganizationLevel.COLLEGE);
        project.setRejectReason(null);
        project.setUpdatedAt(LocalDateTime.now());
        projectMapper.updateById(project);
        return toDetailResponse(project);
    }

    @Override
    @Transactional
    public ProjectDetailResponse withdrawProject(Long userId, Long projectId) {
        Project project = getOwnedProject(userId, projectId);

        if (project.getStatus() != ProjectStatus.PENDING_REVIEW) {
            throw new BusinessException("只有待审核状态的项目可以撤回");
        }

        Event event = eventMapper.selectById(project.getEventId());
        if (event.getSubmissionDeadline() != null && LocalDateTime.now().isAfter(event.getSubmissionDeadline())) {
            throw new BusinessException("提交截止时间已过，无法撤回");
        }

        project.setStatus(ProjectStatus.DRAFT);
        project.setCurrentReviewLevel(null);
        project.setUpdatedAt(LocalDateTime.now());
        projectMapper.updateById(project);
        return toDetailResponse(project);
    }

    @Override
    @Transactional
    public void deleteProject(Long userId, Long projectId) {
        Project project = getOwnedProject(userId, projectId);

        if (project.getStatus() != ProjectStatus.DRAFT) {
            throw new BusinessException("只有草稿状态的项目可以删除");
        }

        // Delete uploaded files
        List<ProjectMaterial> materials = materialMapper.findByProjectId(projectId);
        for (ProjectMaterial m : materials) {
            fileStorageService.delete(m.getFilePath());
        }

        // Delete related data
        materialMapper.deleteByProjectId(projectId);
        teamMemberMapper.deleteByProjectId(projectId);
        advisorMapper.deleteByProjectId(projectId);
        projectMapper.deleteById(projectId);
    }

    @Override
    @Transactional
    public MemberResponse addMember(Long userId, Long projectId, TeamMemberRequest request) {
        Project project = getOwnedProject(userId, projectId);
        ensureModifiable(project);

        TeamMember member = new TeamMember();
        member.setProjectId(project.getId());
        member.setName(request.getName());
        member.setStudentId(request.getStudentId());
        member.setDepartment(request.getDepartment());
        member.setMajor(request.getMajor());
        member.setPhone(request.getPhone());
        member.setMemberRole(MemberRole.valueOf(request.getMemberRole()));
        member.setSortOrder(request.getSortOrder());
        teamMemberMapper.insert(member);
        return toMemberResponse(member);
    }

    @Override
    @Transactional
    public MemberResponse updateMember(Long userId, Long projectId, Long memberId, TeamMemberRequest request) {
        getOwnedProject(userId, projectId);
        TeamMember member = teamMemberMapper.selectById(memberId);
        if (member == null) throw new BusinessException("成员不存在");
        if (!member.getProjectId().equals(projectId)) {
            throw new BusinessException("成员不属于该项目");
        }

        member.setName(request.getName());
        member.setStudentId(request.getStudentId());
        member.setDepartment(request.getDepartment());
        member.setMajor(request.getMajor());
        member.setPhone(request.getPhone());
        member.setMemberRole(MemberRole.valueOf(request.getMemberRole()));
        member.setSortOrder(request.getSortOrder());
        teamMemberMapper.updateById(member);
        return toMemberResponse(member);
    }

    @Override
    @Transactional
    public void deleteMember(Long userId, Long projectId, Long memberId) {
        getOwnedProject(userId, projectId);
        TeamMember member = teamMemberMapper.selectById(memberId);
        if (member == null) throw new BusinessException("成员不存在");
        if (!member.getProjectId().equals(projectId)) {
            throw new BusinessException("成员不属于该项目");
        }
        teamMemberMapper.deleteById(member.getId());
    }

    @Override
    @Transactional
    public AdvisorResponse addAdvisor(Long userId, Long projectId, AdvisorRequest request) {
        Project project = getOwnedProject(userId, projectId);
        ensureModifiable(project);

        Advisor advisor = new Advisor();
        advisor.setProjectId(project.getId());
        advisor.setName(request.getName());
        advisor.setPhone(request.getPhone());
        advisorMapper.insert(advisor);
        return toAdvisorResponse(advisor);
    }

    @Override
    @Transactional
    public AdvisorResponse updateAdvisor(Long userId, Long projectId, Long advisorId, AdvisorRequest request) {
        getOwnedProject(userId, projectId);
        Advisor advisor = advisorMapper.selectById(advisorId);
        if (advisor == null) throw new BusinessException("指导老师不存在");
        if (!advisor.getProjectId().equals(projectId)) {
            throw new BusinessException("指导老师不属于该项目");
        }

        advisor.setName(request.getName());
        advisor.setPhone(request.getPhone());
        advisorMapper.updateById(advisor);
        return toAdvisorResponse(advisor);
    }

    @Override
    @Transactional
    public void deleteAdvisor(Long userId, Long projectId, Long advisorId) {
        getOwnedProject(userId, projectId);
        Advisor advisor = advisorMapper.selectById(advisorId);
        if (advisor == null) throw new BusinessException("指导老师不存在");
        if (!advisor.getProjectId().equals(projectId)) {
            throw new BusinessException("指导老师不属于该项目");
        }
        advisorMapper.deleteById(advisor.getId());
    }

    @Override
    @Transactional
    public MaterialResponse uploadMaterial(Long userId, Long projectId, String materialTypeStr, MultipartFile file) {
        Project project = getOwnedProject(userId, projectId);
        ensureModifiable(project);

        MaterialType materialType;
        try {
            materialType = MaterialType.valueOf(materialTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("不支持的材料类型: " + materialTypeStr);
        }

        validateFileType(file, materialType);
        validateFileSize(file, materialType);

        // Replace existing material of same type
        materialMapper.findByProjectIdAndMaterialType(projectId, materialType)
                .ifPresent(existing -> {
                    fileStorageService.delete(existing.getFilePath());
                    materialMapper.deleteById(existing.getId());
                });

        String relativePath = fileStorageService.store(projectId, file);

        ProjectMaterial material = new ProjectMaterial();
        material.setProjectId(project.getId());
        material.setMaterialType(materialType);
        material.setOriginalFilename(file.getOriginalFilename());
        material.setStoredFilename(relativePath.substring(relativePath.lastIndexOf('/') + 1));
        material.setFilePath(relativePath);
        material.setFileSize(file.getSize());
        materialMapper.insert(material);

        return toMaterialResponse(material);
    }

    @Override
    @Transactional
    public void deleteMaterial(Long userId, Long projectId, Long materialId) {
        Project project = getOwnedProject(userId, projectId);
        ensureModifiable(project);

        ProjectMaterial material = materialMapper.selectById(materialId);
        if (material == null) throw new BusinessException("材料不存在");
        if (!material.getProjectId().equals(projectId)) {
            throw new BusinessException("材料不属于该项目");
        }

        fileStorageService.delete(material.getFilePath());
        materialMapper.deleteById(material.getId());
    }

    // ========== Admin operations ==========

    @Override
    public IPage<ProjectAdminListItemResponse> listProjectsForAdmin(Long userId, int current, int size,
                                                                     Long eventId, Long trackId, Long organizationId,
                                                                     String keyword, String status) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(401, "用户不存在");

        Page<Project> page = new Page<>(current, size);
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Project> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();

        // Unified ADMIN role — filter by org tree visibility
        List<Long> visibleOrgIds = AdminLevelResolver.getVisibleOrgIds(user, organizationMapper);
        if (!visibleOrgIds.isEmpty()) {
            List<Long> userIds = userMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
                            .lambda().select(User::getId)
                            .in(User::getOrganizationId, visibleOrgIds))
                    .stream().map(User::getId).toList();
            if (userIds.isEmpty()) {
                return new Page<>(current, size);
            }
            wrapper.lambda().in(Project::getUserId, userIds);
        }

        // Each admin level only sees PENDING_REVIEW projects at their own level
        // SUPER_ADMIN sees all projects at all levels
        boolean isSuperAdmin = AdminLevelResolver.isSuperAdmin(user);
        OrganizationLevel adminLevel = AdminLevelResolver.getLevel(user, organizationMapper);
        if (!isSuperAdmin && adminLevel != null) {
            wrapper.lambda().and(w -> w
                    .ne(Project::getStatus, ProjectStatus.PENDING_REVIEW)
                    .or()
                    .eq(Project::getCurrentReviewLevel, adminLevel)
            );
        }

        // Restrict to events at admin's competition level and one below (same as EventServiceImpl.listAllEvents)
        // Also always include events from admin's visible org tree regardless of competitionLevel,
        // because projects stay in the original (child) event while being reviewed through the chain.
        // SUPER_ADMIN sees events at all levels — skip this filter
        if (!isSuperAdmin && adminLevel != null) {
            CompetitionLevel ownLevel = CompetitionLevel.valueOf(adminLevel.name());
            OrganizationLevel childLevel = adminLevel.nextDown();
            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Event> eventQuery =
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            eventQuery.lambda().and(w -> {
                if (childLevel != null) {
                    CompetitionLevel childCompLevel = CompetitionLevel.valueOf(childLevel.name());
                    w.and(inner -> inner.eq(Event::getCompetitionLevel, ownLevel)
                            .or().eq(Event::getCompetitionLevel, childCompLevel));
                } else {
                    w.and(inner -> inner.eq(Event::getCompetitionLevel, ownLevel));
                }
                if (!visibleOrgIds.isEmpty()) {
                    w.or().in(Event::getOrganizationId, visibleOrgIds);
                }
            });
            List<Long> allowedEventIds = eventMapper.selectList(eventQuery.lambda().select(Event::getId))
                    .stream().map(Event::getId).toList();
            if (!allowedEventIds.isEmpty()) {
                wrapper.lambda().in(Project::getEventId, allowedEventIds);
            }
        }

        if (eventId != null) {
            wrapper.lambda().eq(Project::getEventId, eventId);
        }
        if (trackId != null) {
            wrapper.lambda().eq(Project::getTrackId, trackId);
        }
        if (organizationId != null) {
            List<Long> orgUserIds = userMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
                            .lambda().select(User::getId)
                            .eq(User::getOrganizationId, organizationId))
                    .stream().map(User::getId).toList();
            if (orgUserIds.isEmpty()) {
                return new Page<>(current, size);
            }
            wrapper.lambda().in(Project::getUserId, orgUserIds);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.lambda().like(Project::getName, keyword);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.lambda().eq(Project::getStatus, status);
        }

        wrapper.orderByDesc("created_at");
        IPage<Project> projectPage = projectMapper.selectPage(page, wrapper);
        return projectPage.convert(this::toAdminListItemResponse);
    }

    @Override
    public ProjectDetailResponse adminGetProject(Long userId, Long projectId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(401, "用户不存在");

        Project project = projectMapper.selectById(projectId);
        if (project == null) throw new BusinessException("项目不存在");

        // Allow all admin levels to preview project details and materials.
        // The org-visibility check is removed so that lower-level admins can
        // inspect projects submitted to higher-level events.
        // Actual review (approve/reject) authorization is enforced in reviewProject().

        return toDetailResponse(project);
    }

    @Override
    @Transactional
    public void reviewProject(Long userId, Long projectId, ProjectReviewRequest request) {
        User admin = userMapper.selectById(userId);
        if (admin == null) throw new BusinessException(401, "用户不存在");

        Project project = projectMapper.selectById(projectId);
        if (project == null) throw new BusinessException("项目不存在");

        if (project.getStatus() != ProjectStatus.PENDING_REVIEW) {
            throw new BusinessException("只有待审核状态的项目可以审核");
        }

        // Verify this admin's level matches the current review level
        boolean isSuperAdmin = AdminLevelResolver.isSuperAdmin(admin);
        OrganizationLevel adminLevel = AdminLevelResolver.getLevel(admin, organizationMapper);
        if (adminLevel == null && !isSuperAdmin) throw new BusinessException(403, "无权操作");

        if (project.getCurrentReviewLevel() == null) {
            throw new BusinessException("审核阶段数据异常");
        }
        // SUPER_ADMIN can review at any level; other admins must match the current review level
        if (!isSuperAdmin && !project.getCurrentReviewLevel().equals(adminLevel)) {
            throw new BusinessException("当前审核阶段为" + project.getCurrentReviewLevel().name() + "，您无权审核");
        }

        // For SUPER_ADMIN, the effective level is the project's current review level
        OrganizationLevel effectiveLevel = isSuperAdmin ? project.getCurrentReviewLevel() : adminLevel;

        // Verify project creator is in this admin's org subtree
        User creator = userMapper.selectById(project.getUserId());
        List<Long> visibleOrgIds = AdminLevelResolver.getVisibleOrgIds(admin, organizationMapper);
        if (creator != null && creator.getOrganizationId() != null
                && !visibleOrgIds.isEmpty() && !visibleOrgIds.contains(creator.getOrganizationId())) {
            throw new BusinessException(403, "无权审核该项目");
        }

        Event event = eventMapper.selectById(project.getEventId());
        CompetitionLevel compLevel = event.getCompetitionLevel();
        if (compLevel == null) compLevel = CompetitionLevel.SCHOOL;
        OrganizationLevel finalLevel = compLevel.toOrganizationLevel();

        if ("APPROVE".equals(request.getAction())) {
            if (effectiveLevel.equals(finalLevel)) {
                // Final approval for this competition level
                project.setStatus(ProjectStatus.APPROVED);
                project.setCurrentReviewLevel(null);
                project.setRejectReason(null);
            } else {
                // Advance to next level
                OrganizationLevel nextLevel = effectiveLevel.nextUp();
                if (nextLevel == null) {
                    throw new BusinessException("审核层级异常");
                }
                project.setCurrentReviewLevel(nextLevel);
            }
        } else if ("REJECT".equals(request.getAction())) {
            if (request.getRejectReason() == null || request.getRejectReason().isBlank()) {
                throw new BusinessException("退回时必须填写原因");
            }
            project.setStatus(ProjectStatus.REJECTED);
            project.setCurrentReviewLevel(null);
            project.setRejectReason(request.getRejectReason());
        } else {
            throw new BusinessException("无效的审核操作");
        }

        // Log the review action
        ReviewLog log = new ReviewLog();
        log.setProjectId(projectId);
        log.setReviewerId(userId);
        log.setReviewLevel(adminLevel);
        log.setAction(request.getAction());
        log.setRejectReason(request.getRejectReason());
        log.setReviewedAt(LocalDateTime.now());
        reviewLogMapper.insert(log);

        project.setUpdatedAt(LocalDateTime.now());
        projectMapper.updateById(project);
    }

    @Override
    @Transactional
    public void batchReviewProjects(Long userId, BatchReviewRequest request) {
        if (request.getProjectIds() == null || request.getProjectIds().isEmpty()) {
            throw new BusinessException("请选择要审核的项目");
        }
        if (!"APPROVE".equals(request.getAction())) {
            throw new BusinessException("批量操作仅支持通过");
        }

        ProjectReviewRequest singleRequest = new ProjectReviewRequest();
        singleRequest.setAction("APPROVE");

        for (Long projectId : request.getProjectIds()) {
            reviewProject(userId, projectId, singleRequest);
        }
    }

    private ProjectAdminListItemResponse toAdminListItemResponse(Project project) {
        Event event = eventMapper.selectById(project.getEventId());
        Track track = trackMapper.selectById(project.getTrackId());
        User creator = userMapper.selectById(project.getUserId());

        ProjectAdminListItemResponse resp = new ProjectAdminListItemResponse();
        resp.setId(project.getId());
        resp.setName(project.getName());
        resp.setStatus(project.getStatus().name());
        resp.setCurrentReviewLevel(project.getCurrentReviewLevel() != null
                ? project.getCurrentReviewLevel().name() : null);
        resp.setEventId(project.getEventId());
        resp.setTrackId(project.getTrackId());

        if (event != null) {
            resp.setEventName(event.getName());
            if (event.getCompetitionLevel() != null) {
                resp.setCompetitionLevel(event.getCompetitionLevel().name());
            }
        }
        if (track != null) {
            if (track.getGroupName() != null) {
                resp.setTrackName(track.getName() + "（" + track.getGroupName() + "）");
            } else {
                resp.setTrackName(track.getName());
            }
        }
        if (creator != null) {
            resp.setCreatorName(creator.getRealName());
            if (creator.getOrganizationId() != null) {
                resp.setOrganizationName(organizationMapper.findSchoolOrgName(creator.getOrganizationId()));
            }
        }

        resp.setMemberCount(teamMemberMapper.findByProjectIdOrderBySortOrder(project.getId()).size());
        resp.setMaterialCount(materialMapper.findByProjectId(project.getId()).size());
        resp.setCreatedAt(project.getCreatedAt());
        resp.setUpdatedAt(project.getUpdatedAt());
        return resp;
    }

    private void validateFileType(MultipartFile file, MaterialType type) {
        String originalName = file.getOriginalFilename();
        if (originalName == null) throw new BusinessException("文件名不能为空");

        String lowerName = originalName.toLowerCase();
        switch (type) {
            case PITCH_PDF:
            case PROJECT_PLAN_PDF:
            case SUPPORTING_PDF:
                if (!lowerName.endsWith(".pdf")) throw new BusinessException("仅支持PDF格式");
                break;
            case VIDEO_MP4:
                if (!lowerName.endsWith(".mp4")) throw new BusinessException("仅支持MP4格式");
                break;
        }
    }

    private void validateFileSize(MultipartFile file, MaterialType type) {
        long maxSize = switch (type) {
            case PITCH_PDF, PROJECT_PLAN_PDF -> MAX_PDF_SIZE;
            case SUPPORTING_PDF -> MAX_SUPPORTING_PDF_SIZE;
            case VIDEO_MP4 -> MAX_VIDEO_SIZE;
        };
        if (file.getSize() > maxSize) {
            throw new BusinessException("文件大小超过限制 (" + (maxSize / 1024 / 1024) + "MB)");
        }
    }

    private Project getOwnedProject(Long userId, Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) throw new BusinessException("项目不存在");
        if (!project.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权访问该项目");
        }
        return project;
    }

    private void ensureModifiable(Project project) {
        if (project.getStatus() != ProjectStatus.DRAFT && project.getStatus() != ProjectStatus.REJECTED) {
            throw new BusinessException("只有草稿或已退回状态的项目可以修改");
        }
    }

    private ProjectDetailResponse toDetailResponse(Project project) {
        Event event = eventMapper.selectById(project.getEventId());
        Track track = trackMapper.selectById(project.getTrackId());

        ProjectDetailResponse resp = new ProjectDetailResponse();
        resp.setId(project.getId());
        resp.setEventId(event.getId());
        resp.setEventName(event.getName());
        resp.setTrackId(track.getId());
        if (track.getGroupName() != null) {
            resp.setTrackName(track.getName() + "（" + track.getGroupName() + "）");
        } else {
            resp.setTrackName(track.getName());
        }
        resp.setName(project.getName());
        resp.setDescription(project.getDescription());
        resp.setStatus(project.getStatus().name());
        resp.setCurrentReviewLevel(project.getCurrentReviewLevel() != null
                ? project.getCurrentReviewLevel().name() : null);
        resp.setRejectReason(project.getRejectReason());
        resp.setRequireVideo(event.getRequireVideo());
        resp.setCreatedAt(project.getCreatedAt());
        resp.setUpdatedAt(project.getUpdatedAt());
        resp.setTeamMembers(teamMemberMapper.findByProjectIdOrderBySortOrder(project.getId())
                .stream().map(this::toMemberResponse).toList());
        resp.setAdvisors(advisorMapper.findByProjectId(project.getId())
                .stream().map(this::toAdvisorResponse).toList());
        resp.setMaterials(materialMapper.findByProjectId(project.getId())
                .stream().map(this::toMaterialResponse).toList());
        return resp;
    }

    private ProjectListItemResponse toListItemResponse(Project project) {
        Event event = eventMapper.selectById(project.getEventId());
        Track track = trackMapper.selectById(project.getTrackId());

        ProjectListItemResponse resp = new ProjectListItemResponse();
        resp.setId(project.getId());
        resp.setEventName(event != null ? event.getName() : null);
        if (track != null) {
            if (track.getGroupName() != null) {
                resp.setTrackName(track.getName() + "（" + track.getGroupName() + "）");
            } else {
                resp.setTrackName(track.getName());
            }
        }
        resp.setName(project.getName());
        resp.setStatus(project.getStatus().name());
        resp.setCreatedAt(project.getCreatedAt());
        resp.setUpdatedAt(project.getUpdatedAt());
        return resp;
    }

    private MemberResponse toMemberResponse(TeamMember member) {
        MemberResponse resp = new MemberResponse();
        resp.setId(member.getId());
        resp.setName(member.getName());
        resp.setStudentId(member.getStudentId());
        resp.setDepartment(member.getDepartment());
        resp.setMajor(member.getMajor());
        resp.setPhone(member.getPhone());
        resp.setMemberRole(member.getMemberRole().name());
        resp.setSortOrder(member.getSortOrder());
        return resp;
    }

    private AdvisorResponse toAdvisorResponse(Advisor advisor) {
        AdvisorResponse resp = new AdvisorResponse();
        resp.setId(advisor.getId());
        resp.setName(advisor.getName());
        resp.setPhone(advisor.getPhone());
        return resp;
    }

    private MaterialResponse toMaterialResponse(ProjectMaterial material) {
        MaterialResponse resp = new MaterialResponse();
        resp.setId(material.getId());
        resp.setMaterialType(material.getMaterialType().name());
        resp.setOriginalFilename(material.getOriginalFilename());
        resp.setStoredFilename(material.getStoredFilename());
        resp.setFileSize(material.getFileSize());
        return resp;
    }

    private List<Long> getVisibleOrgIds(User user) {
        return AdminLevelResolver.getVisibleOrgIds(user, organizationMapper);
    }
}
