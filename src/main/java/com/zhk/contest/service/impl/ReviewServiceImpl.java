package com.zhk.contest.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhk.contest.dto.request.AllocateJudgesRequest;
import com.zhk.contest.dto.request.ScoreCriteriaSaveRequest;
import com.zhk.contest.dto.response.*;
import com.zhk.contest.entity.*;
import com.zhk.contest.enums.OrganizationLevel;
import com.zhk.contest.enums.ProjectStatus;
import com.zhk.contest.enums.UserRole;
import com.zhk.contest.exception.BusinessException;
import com.zhk.contest.util.AdminLevelResolver;
import com.zhk.contest.mapper.*;
import com.zhk.contest.service.FileStorageService;
import com.zhk.contest.service.ReviewService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReviewServiceImpl implements ReviewService {

    private final UserMapper userMapper;
    private final EventMapper eventMapper;
    private final TrackMapper trackMapper;
    private final ProjectMapper projectMapper;
    private final OrganizationMapper organizationMapper;
    private final ScoreCriteriaMapper scoreCriteriaMapper;
    private final ReviewAllocationMapper reviewAllocationMapper;
    private final ScoreRecordMapper scoreRecordMapper;
    private final ProjectMaterialMapper projectMaterialMapper;
    private final FileStorageService fileStorageService;

    public ReviewServiceImpl(UserMapper userMapper, EventMapper eventMapper,
                             TrackMapper trackMapper, ProjectMapper projectMapper,
                             OrganizationMapper organizationMapper,
                             ScoreCriteriaMapper scoreCriteriaMapper,
                             ReviewAllocationMapper reviewAllocationMapper,
                             ScoreRecordMapper scoreRecordMapper,
                             ProjectMaterialMapper projectMaterialMapper,
                             FileStorageService fileStorageService) {
        this.userMapper = userMapper;
        this.eventMapper = eventMapper;
        this.trackMapper = trackMapper;
        this.projectMapper = projectMapper;
        this.organizationMapper = organizationMapper;
        this.scoreCriteriaMapper = scoreCriteriaMapper;
        this.reviewAllocationMapper = reviewAllocationMapper;
        this.scoreRecordMapper = scoreRecordMapper;
        this.projectMaterialMapper = projectMaterialMapper;
        this.fileStorageService = fileStorageService;
    }

    private User checkAdmin(Long adminUserId) {
        User admin = userMapper.selectById(adminUserId);
        if (admin == null) throw new BusinessException(401, "用户不存在");
        return admin;
    }

    private User checkCompetitionLevelAdmin(Long adminUserId, Long eventId) {
        User admin = userMapper.selectById(adminUserId);
        if (admin == null) throw new BusinessException(401, "用户不存在");
        Event event = eventMapper.selectById(eventId);
        if (event == null) throw new BusinessException("赛事不存在");
        OrganizationLevel adminLevel = AdminLevelResolver.getLevel(admin, organizationMapper);
        OrganizationLevel requiredLevel = event.getCompetitionLevel() != null
                ? event.getCompetitionLevel().toOrganizationLevel() : OrganizationLevel.SCHOOL;
        if (adminLevel == null || !adminLevel.equals(requiredLevel)) {
            throw new BusinessException(403, "只有" + requiredLevel.name() + "级别管理员可执行此操作");
        }
        return admin;
    }

    private List<Long> getVisibleOrgIds(User admin) {
        return AdminLevelResolver.getVisibleOrgIds(admin, organizationMapper);
    }

    // ========== Score Criteria ==========

    @Override
    public List<ScoreCriteriaResponse> getCriteria(Long trackId) {
        return scoreCriteriaMapper.findByTrackId(trackId).stream()
                .map(ScoreCriteriaResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public List<ScoreCriteriaResponse> saveCriteria(Long adminUserId, Long trackId, List<ScoreCriteriaSaveRequest> requests) {
        checkAdmin(adminUserId);

        Track track = trackMapper.selectById(trackId);
        if (track == null) throw new BusinessException("赛道不存在");

        // Delete existing criteria and replace
        List<ScoreCriteria> existing = scoreCriteriaMapper.findByTrackId(trackId);
        for (ScoreCriteria sc : existing) {
            scoreCriteriaMapper.deleteById(sc.getId());
        }

        List<ScoreCriteria> saved = new ArrayList<>();
        for (int i = 0; i < requests.size(); i++) {
            ScoreCriteriaSaveRequest req = requests.get(i);
            ScoreCriteria sc = new ScoreCriteria();
            sc.setTrackId(trackId);
            sc.setName(req.getName());
            sc.setDescription(req.getDescription());
            sc.setMaxScore(req.getMaxScore() != null ? req.getMaxScore() : 100);
            sc.setSortOrder(req.getSortOrder() != null ? req.getSortOrder() : i);
            scoreCriteriaMapper.insert(sc);
            saved.add(sc);
        }

        return saved.stream().map(ScoreCriteriaResponse::from).toList();
    }

    @Override
    @Transactional
    public void deleteCriteria(Long adminUserId, Long criteriaId) {
        checkAdmin(adminUserId);

        ScoreCriteria sc = scoreCriteriaMapper.selectById(criteriaId);
        if (sc == null) throw new BusinessException("评分维度不存在");

        scoreCriteriaMapper.deleteById(criteriaId);
    }

    @Override
    @Transactional
    public void uploadEventAttachment(Long adminUserId, Long eventId, MultipartFile file) {
        checkCompetitionLevelAdmin(adminUserId, eventId);

        Event event = eventMapper.selectById(eventId);
        if (event == null) throw new BusinessException("赛事不存在");

        // Delete old attachment if exists
        if (event.getAttachmentPath() != null) {
            fileStorageService.delete(event.getAttachmentPath());
        }

        String path = fileStorageService.store("events", file);
        event.setAttachmentPath(path);
        event.setAttachmentName(file.getOriginalFilename());
        eventMapper.updateById(event);
    }

    @Override
    public ResponseEntity<Resource> downloadEventAttachment(Long eventId) {
        Event event = eventMapper.selectById(eventId);
        if (event == null) throw new BusinessException("赛事不存在");
        if (event.getAttachmentPath() == null) throw new BusinessException("该赛事未上传评分附件");

        Resource resource = fileStorageService.loadAsResource(event.getAttachmentPath());
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + java.net.URLEncoder.encode(
                                event.getAttachmentName(), java.nio.charset.StandardCharsets.UTF_8))
                .body(resource);
    }

    @Override
    @Transactional
    public void deleteEventAttachment(Long adminUserId, Long eventId) {
        checkCompetitionLevelAdmin(adminUserId, eventId);

        Event event = eventMapper.selectById(eventId);
        if (event == null) throw new BusinessException("赛事不存在");

        if (event.getAttachmentPath() != null) {
            fileStorageService.delete(event.getAttachmentPath());
        }
        event.setAttachmentPath(null);
        event.setAttachmentName(null);
        eventMapper.updateById(event);
    }

    // ========== Allocation ==========

    @Override
    @Transactional
    public void allocateJudges(Long adminUserId, AllocateJudgesRequest request) {
        User admin = checkAdmin(adminUserId);
        List<Long> visibleOrgIds = getVisibleOrgIds(admin);

        if (request.getAllocations() == null) return;

        for (AllocateJudgesRequest.AllocateItem item : request.getAllocations()) {
            Project project = projectMapper.selectById(item.getProjectId());
            if (project == null) throw new BusinessException("项目不存在: " + item.getProjectId());

            // Verify project is approved
            if (project.getStatus() != ProjectStatus.APPROVED) {
                throw new BusinessException("只能分配已审核通过的项目: " + project.getName());
            }

            // Verify project is within admin's org tree
            User creator = userMapper.selectById(project.getUserId());
            if (creator != null && creator.getOrganizationId() != null
                    && !visibleOrgIds.isEmpty() && !visibleOrgIds.contains(creator.getOrganizationId())) {
                throw new BusinessException(403, "项目不在您的管辖范围内: " + project.getName());
            }

            for (Long judgeId : item.getJudgeIds()) {
                User judge = userMapper.selectById(judgeId);
                if (judge == null || judge.getRole() != UserRole.JUDGE) {
                    throw new BusinessException("评委不存在或不是评委角色: " + judgeId);
                }

                // Check for duplicate
                ReviewAllocation existing = reviewAllocationMapper.selectOne(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ReviewAllocation>()
                                .eq(ReviewAllocation::getProjectId, item.getProjectId())
                                .eq(ReviewAllocation::getJudgeId, judgeId));
                if (existing != null) continue; // skip duplicate

                ReviewAllocation ra = new ReviewAllocation();
                ra.setProjectId(item.getProjectId());
                ra.setJudgeId(judgeId);
                ra.setStatus("PENDING");
                reviewAllocationMapper.insert(ra);
            }
        }
    }

    @Override
    public IPage<ReviewAllocationResponse> listAllocations(Long adminUserId, Long eventId, Long projectId, Long judgeId, Page<ReviewAllocation> page) {
        User admin = checkAdmin(adminUserId);
        List<Long> visibleOrgIds = getVisibleOrgIds(admin);

        List<ReviewAllocation> allocations;
        if (eventId != null) {
            allocations = reviewAllocationMapper.findByEventId(eventId);
        } else if (projectId != null) {
            allocations = reviewAllocationMapper.findByProjectId(projectId);
        } else if (judgeId != null) {
            allocations = reviewAllocationMapper.findByJudgeId(judgeId);
        } else {
            allocations = reviewAllocationMapper.selectList(null);
        }

        List<ReviewAllocationResponse> allItems = allocations.stream()
                .filter(ra -> {
                    Project p = projectMapper.selectById(ra.getProjectId());
                    if (p == null) return false;
                    User creator = userMapper.selectById(p.getUserId());
                    if (creator == null || creator.getOrganizationId() == null) return true;
                    return visibleOrgIds.isEmpty() || visibleOrgIds.contains(creator.getOrganizationId());
                })
                .map(ra -> {
                    ReviewAllocationResponse resp = ReviewAllocationResponse.from(ra);
                    Project p = projectMapper.selectById(ra.getProjectId());
                    if (p != null) {
                        resp.setProjectName(p.getName());
                        Event e = eventMapper.selectById(p.getEventId());
                        if (e != null) resp.setEventName(e.getName());
                        Track t = trackMapper.selectById(p.getTrackId());
                        if (t != null) {
                            resp.setTrackName(t.getGroupName() != null
                                    ? t.getName() + "（" + t.getGroupName() + "）" : t.getName());
                        }
                    }
                    User j = userMapper.selectById(ra.getJudgeId());
                    if (j != null) {
                        String name = j.getRealName() != null ? j.getRealName() : j.getUsername();
                        resp.setJudgeName(name);
                    }
                    return resp;
                })
                .toList();

        // Manual pagination after org-tree filtering
        int total = allItems.size();
        int from = (int) Math.min((page.getCurrent() - 1) * page.getSize(), total);
        int to = (int) Math.min(from + page.getSize(), total);
        List<ReviewAllocationResponse> pageItems = allItems.subList(from, to);

        IPage<ReviewAllocationResponse> result = new Page<>(page.getCurrent(), page.getSize(), total);
        result.setRecords(pageItems);
        return result;
    }

    @Override
    @Transactional
    public void removeAllocation(Long adminUserId, Long allocationId) {
        checkAdmin(adminUserId);

        ReviewAllocation ra = reviewAllocationMapper.selectById(allocationId);
        if (ra == null) throw new BusinessException("分配记录不存在");

        // Delete scores first, then allocation
        List<ScoreRecord> records = scoreRecordMapper.findByAllocationId(allocationId);
        for (ScoreRecord sr : records) {
            scoreRecordMapper.deleteById(sr.getId());
        }
        reviewAllocationMapper.deleteById(allocationId);
    }

    // ========== Progress ==========

    @Override
    public ReviewProgressResponse getProgress(Long adminUserId, Long eventId) {
        User admin = checkAdmin(adminUserId);
        Event event = eventMapper.selectById(eventId);
        if (event == null) throw new BusinessException("赛事不存在");

        ReviewProgressResponse resp = new ReviewProgressResponse();

        // Scope projects by admin's visible org tree
        List<Long> visibleOrgIds = getVisibleOrgIds(admin);
        List<Project> scopedProjects;
        if (visibleOrgIds.isEmpty()) {
            scopedProjects = projectMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Project>()
                            .eq(Project::getEventId, eventId));
        } else {
            scopedProjects = projectMapper.findByEventIdAndOrgIds(eventId, visibleOrgIds);
        }

        // Exclude DRAFT from "submitted" count
        List<Project> submittedProjects = scopedProjects.stream()
                .filter(p -> p.getStatus() != ProjectStatus.DRAFT)
                .toList();
        resp.setTotalProjects(submittedProjects.size());

        long approvedCount = submittedProjects.stream()
                .filter(p -> p.getStatus() == ProjectStatus.APPROVED)
                .count();
        resp.setApprovedProjects(approvedCount);

        // Allocations for this event
        List<ReviewAllocation> allocations = reviewAllocationMapper.findByEventId(eventId);

        // Scope allocations to visible projects
        Set<Long> scopedProjectIds = scopedProjects.stream()
                .map(Project::getId)
                .collect(Collectors.toSet());
        List<ReviewAllocation> scopedAllocations = allocations.stream()
                .filter(ra -> scopedProjectIds.contains(ra.getProjectId()))
                .toList();

        long allocatedProjects = scopedAllocations.stream()
                .map(ReviewAllocation::getProjectId)
                .distinct()
                .count();
        resp.setAllocatedProjects(allocatedProjects);

        // Aggregate per-project allocation status
        Map<Long, List<ReviewAllocation>> allocByProject = scopedAllocations.stream()
                .collect(Collectors.groupingBy(ReviewAllocation::getProjectId));

        long fullyReviewed = 0;
        long partialReviewed = 0;
        long unallocated = 0;
        long completedReviews = 0;

        for (Project p : submittedProjects) {
            List<ReviewAllocation> projectAllocs = allocByProject.getOrDefault(p.getId(), Collections.emptyList());
            if (projectAllocs.isEmpty()) {
                unallocated++;
            } else {
                long done = projectAllocs.stream().filter(ra -> "COMPLETED".equals(ra.getStatus())).count();
                completedReviews += done;
                if (done == projectAllocs.size()) {
                    fullyReviewed++;
                } else {
                    partialReviewed++;
                }
            }
        }

        resp.setCompletedReviews(completedReviews);
        resp.setFullyReviewedProjects(fullyReviewed);
        resp.setPartialReviewedProjects(partialReviewed);
        resp.setUnallocatedProjects(unallocated);

        // Judges: count only those with allocations in this event's scoped projects
        long activeJudges = scopedAllocations.stream()
                .map(ReviewAllocation::getJudgeId)
                .distinct()
                .count();
        resp.setActiveJudges(activeJudges);

        // Total judges: those in admin's visible orgs (or all if super admin)
        List<User> scopedJudges;
        if (visibleOrgIds.isEmpty()) {
            scopedJudges = userMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                            .eq(User::getRole, UserRole.JUDGE));
        } else {
            scopedJudges = userMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                            .eq(User::getRole, UserRole.JUDGE)
                            .in(User::getOrganizationId, visibleOrgIds));
        }
        resp.setTotalJudges(scopedJudges.size());

        // Build per-project progress list
        Map<String, String> trackNameMap = new HashMap<>();
        List<ProjectProgressItem> items = new ArrayList<>();
        for (Project p : submittedProjects) {
            ProjectProgressItem item = new ProjectProgressItem();
            item.setProjectId(p.getId());
            item.setProjectName(p.getName());
            item.setProjectStatus(p.getStatus().name());

            List<ReviewAllocation> projectAllocs = allocByProject.getOrDefault(p.getId(), Collections.emptyList());
            item.setTotalJudges(projectAllocs.size());
            long done = projectAllocs.stream().filter(ra -> "COMPLETED".equals(ra.getStatus())).count();
            item.setCompletedJudges((int) done);
            item.setCompletionPercent(projectAllocs.isEmpty() ? 0
                    : (int) Math.round(done * 100.0 / projectAllocs.size()));

            if (p.getTrackId() != null) {
                String trackKey = p.getTrackId().toString();
                if (!trackNameMap.containsKey(trackKey)) {
                    Track track = trackMapper.selectById(p.getTrackId());
                    if (track != null) {
                        trackNameMap.put(trackKey, track.getName());
                        trackNameMap.put(trackKey + "_group", track.getGroupName());
                    }
                }
                item.setTrackName(trackNameMap.get(trackKey));
                item.setGroupName(trackNameMap.get(trackKey + "_group"));
            }

            items.add(item);
        }

        // Load org names in batch and assign to items
        Map<Long, ProjectProgressItem> itemByProjectId = new HashMap<>();
        for (ProjectProgressItem item : items) {
            itemByProjectId.put(item.getProjectId(), item);
        }
        Map<Long, String> orgNameMap = new HashMap<>();
        for (Project p : submittedProjects) {
            if (p.getUserId() != null && !orgNameMap.containsKey(p.getUserId())) {
                User u = userMapper.selectById(p.getUserId());
                if (u != null && u.getOrganizationId() != null) {
                    Organization org = organizationMapper.selectById(u.getOrganizationId());
                    if (org != null) {
                        orgNameMap.put(p.getUserId(), org.getName());
                    }
                }
            }
            ProjectProgressItem item = itemByProjectId.get(p.getId());
            if (item != null) {
                item.setOrgName(orgNameMap.getOrDefault(p.getUserId(), ""));
            }
        }

        resp.setProjects(items);
        return resp;
    }

    // ========== Score Summary ==========

    @Override
    public IPage<ScoreSummaryItemResponse> getScoreSummary(Long adminUserId, Long eventId, Long trackId, Page<Project> page) {
        User admin = checkAdmin(adminUserId);
        Event event = eventMapper.selectById(eventId);
        if (event == null) throw new BusinessException("赛事不存在");

        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Project>()
                .eq(Project::getEventId, eventId);
        if (trackId != null) {
            wrapper.eq(Project::getTrackId, trackId);
        }

        IPage<Project> projectPage = projectMapper.selectPage(page, wrapper);

        return projectPage.convert(p -> {
            ScoreSummaryItemResponse item = new ScoreSummaryItemResponse();
            item.setProjectId(p.getId());
            item.setProjectName(p.getName());
            item.setStatus(p.getStatus().name());

            Track t = trackMapper.selectById(p.getTrackId());
            if (t != null) {
                item.setTrackName(t.getGroupName() != null
                        ? t.getName() + "（" + t.getGroupName() + "）" : t.getName());
            }
            item.setEventName(event.getName());

            User creator = userMapper.selectById(p.getUserId());
            if (creator != null && creator.getOrganizationId() != null) {
                String rootName = organizationMapper.findSchoolOrgName(creator.getOrganizationId());
                if (rootName != null) item.setOrganizationName(rootName);
            }

            List<ReviewAllocation> allocs = reviewAllocationMapper.findByProjectId(p.getId())
                    .stream().filter(a -> "COMPLETED".equals(a.getStatus()))
                    .toList();

            item.setJudgeCount(allocs.size());

            if (!allocs.isEmpty()) {
                List<BigDecimal> totals = allocs.stream()
                        .map(a -> a.getTotalScore() != null ? a.getTotalScore() : BigDecimal.ZERO)
                        .toList();
                BigDecimal sum = totals.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
                item.setAverageScore(sum.divide(BigDecimal.valueOf(totals.size()), 1, RoundingMode.HALF_UP));
                item.setHighestScore(totals.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO));
                item.setLowestScore(totals.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO));
            }

            return item;
        });
    }

    // ========== Score Detail ==========

    @Override
    public ProjectScoreDetailResponse getProjectScoreDetail(Long adminUserId, Long eventId, Long projectId) {
        User admin = checkAdmin(adminUserId);
        Event event = eventMapper.selectById(eventId);
        if (event == null) throw new BusinessException("赛事不存在");

        Project project = projectMapper.selectById(projectId);
        if (project == null || !project.getEventId().equals(eventId)) {
            throw new BusinessException("项目不存在");
        }

        ProjectScoreDetailResponse resp = new ProjectScoreDetailResponse();
        resp.setProjectId(project.getId());
        resp.setProjectName(project.getName());
        resp.setEventName(event.getName());

        Track track = trackMapper.selectById(project.getTrackId());
        resp.setTrackName(track != null ? (track.getGroupName() != null
                ? track.getName() + "（" + track.getGroupName() + "）" : track.getName()) : "");

        // Criteria definition for this track
        List<ScoreCriteria> criteriaList = scoreCriteriaMapper.findByTrackId(project.getTrackId());
        List<ProjectScoreDetailResponse.CriteriaInfo> criteriaInfos = criteriaList.stream().map(sc -> {
            ProjectScoreDetailResponse.CriteriaInfo ci = new ProjectScoreDetailResponse.CriteriaInfo();
            ci.setId(sc.getId());
            ci.setName(sc.getName());
            ci.setDescription(sc.getDescription());
            ci.setMaxScore(sc.getMaxScore());
            ci.setSortOrder(sc.getSortOrder());
            return ci;
        }).toList();
        resp.setCriteria(criteriaInfos);

        // Completed allocations for this project
        List<ReviewAllocation> allocs = reviewAllocationMapper.findByProjectId(projectId).stream()
                .filter(a -> "COMPLETED".equals(a.getStatus()))
                .toList();

        List<ProjectScoreDetailResponse.JudgeScoreItem> judgeItems = allocs.stream().map(alloc -> {
            ProjectScoreDetailResponse.JudgeScoreItem item = new ProjectScoreDetailResponse.JudgeScoreItem();
            item.setJudgeId(alloc.getJudgeId());
            User judge = userMapper.selectById(alloc.getJudgeId());
            item.setJudgeName(judge != null ? (judge.getRealName() != null ? judge.getRealName() : judge.getUsername()) : "");
            item.setOverallComment(alloc.getOverallComment());
            item.setTotalScore(alloc.getTotalScore());
            item.setSubmittedAt(alloc.getSubmittedAt());

            // Per-criteria scores
            List<ScoreRecord> records = scoreRecordMapper.findByAllocationId(alloc.getId());
            List<ProjectScoreDetailResponse.ScoreItem> scoreItems = records.stream().map(sr -> {
                ProjectScoreDetailResponse.ScoreItem si = new ProjectScoreDetailResponse.ScoreItem();
                si.setCriteriaId(sr.getCriteriaId());
                si.setScore(sr.getScore());
                // Find criteria name and maxScore
                criteriaList.stream()
                        .filter(c -> c.getId().equals(sr.getCriteriaId()))
                        .findFirst()
                        .ifPresent(c -> {
                            si.setCriteriaName(c.getName());
                            si.setMaxScore(c.getMaxScore());
                        });
                return si;
            }).toList();
            item.setScores(scoreItems);

            return item;
        }).toList();
        resp.setJudgeScores(judgeItems);

        return resp;
    }

    // ========== Materials Cleanup ==========

    @Override
    @Transactional
    public void clearEventMaterials(Long adminUserId, Long eventId) {
        checkCompetitionLevelAdmin(adminUserId, eventId);

        Event event = eventMapper.selectById(eventId);
        if (event == null) throw new BusinessException("赛事不存在");

        List<Project> projects = projectMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Project>()
                        .eq(Project::getEventId, eventId));

        int deleted = 0;
        for (Project p : projects) {
            List<ProjectMaterial> materials = projectMaterialMapper.findByProjectId(p.getId());
            for (ProjectMaterial m : materials) {
                fileStorageService.delete(m.getFilePath());
            }
            if (!materials.isEmpty()) {
                projectMaterialMapper.deleteByProjectId(p.getId());
                deleted += materials.size();
            }
        }

        if (deleted == 0) throw new BusinessException("该赛事下没有材料可清空");
    }

    // ========== Excel Export ==========

    @Override
    public byte[] exportScores(Long adminUserId, Long eventId) {
        checkCompetitionLevelAdmin(adminUserId, eventId);
        Event event = eventMapper.selectById(eventId);
        if (event == null) throw new BusinessException("赛事不存在");

        List<Project> projects = projectMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Project>()
                        .eq(Project::getEventId, eventId));

        // Collect all unique criteria across all tracks in this event, ordered by track then sort_order
        Set<Long> trackIds = projects.stream().map(Project::getTrackId).collect(Collectors.toSet());
        List<ScoreCriteria> allCriteria = new ArrayList<>();
        for (Long trackId : trackIds) {
            allCriteria.addAll(scoreCriteriaMapper.findByTrackId(trackId));
        }
        allCriteria.sort(Comparator.comparing(ScoreCriteria::getTrackId)
                .thenComparing(ScoreCriteria::getSortOrder));
        // Deduplicate by id
        Set<Long> seen = new HashSet<>();
        List<ScoreCriteria> uniqueCriteria = new ArrayList<>();
        for (ScoreCriteria sc : allCriteria) {
            if (seen.add(sc.getId())) uniqueCriteria.add(sc);
        }

        // Pre-load track and org info
        Map<Long, Track> trackMap = new HashMap<>();
        for (Long tid : trackIds) {
            Track t = trackMapper.selectById(tid);
            if (t != null) trackMap.put(tid, t);
        }

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("评分明细");
            CellStyle headerStyle = headerStyle(wb);

            // Build header row: fixed columns + per-criteria columns + tail columns
            int col = 0;
            Row headerRow = sheet.createRow(0);
            Cell h0 = headerRow.createCell(col++); h0.setCellValue("项目名称"); h0.setCellStyle(headerStyle);
            Cell h1 = headerRow.createCell(col++); h1.setCellValue("所属高校"); h1.setCellStyle(headerStyle);
            Cell h2 = headerRow.createCell(col++); h2.setCellValue("赛道"); h2.setCellStyle(headerStyle);
            Cell h3 = headerRow.createCell(col++); h3.setCellValue("评委姓名"); h3.setCellStyle(headerStyle);

            int criteriaStartCol = col;
            for (ScoreCriteria sc : uniqueCriteria) {
                Cell c = headerRow.createCell(col++);
                c.setCellValue(sc.getName() + "(满分" + sc.getMaxScore() + ")");
                c.setCellStyle(headerStyle);
            }
            int criteriaEndCol = col;

            Cell ht = headerRow.createCell(col++); ht.setCellValue("总分"); ht.setCellStyle(headerStyle);
            Cell hc = headerRow.createCell(col++); hc.setCellValue("评语"); hc.setCellStyle(headerStyle);
            Cell hs = headerRow.createCell(col++); hs.setCellValue("提交时间"); hs.setCellStyle(headerStyle);

            int rowNum = 1;
            for (Project p : projects) {
                // Only include projects with completed reviews
                List<ReviewAllocation> allocs = reviewAllocationMapper.findByProjectId(p.getId()).stream()
                        .filter(a -> "COMPLETED".equals(a.getStatus()))
                        .toList();
                if (allocs.isEmpty()) continue;

                Track track = trackMap.get(p.getTrackId());
                String orgName = getOrgName(p.getUserId());

                for (ReviewAllocation alloc : allocs) {
                    Row row = sheet.createRow(rowNum++);
                    int c = 0;
                    row.createCell(c++).setCellValue(p.getName() != null ? p.getName() : "");
                    row.createCell(c++).setCellValue(orgName);
                    row.createCell(c++).setCellValue(track != null
                            ? (track.getGroupName() != null ? track.getName() + "（" + track.getGroupName() + "）" : track.getName())
                            : "");

                    User judge = userMapper.selectById(alloc.getJudgeId());
                    row.createCell(c++).setCellValue(judge != null
                            ? (judge.getRealName() != null ? judge.getRealName() : judge.getUsername()) : "");

                    // Per-criteria scores (use absolute column indices)
                    List<ScoreRecord> records = scoreRecordMapper.findByAllocationId(alloc.getId());
                    Map<Long, BigDecimal> scoreByCriteria = records.stream()
                            .collect(Collectors.toMap(ScoreRecord::getCriteriaId, ScoreRecord::getScore));
                    for (int ci = criteriaStartCol; ci < criteriaEndCol; ci++) {
                        Long criteriaId = uniqueCriteria.get(ci - criteriaStartCol).getId();
                        BigDecimal score = scoreByCriteria.get(criteriaId);
                        row.createCell(ci).setCellValue(score != null ? score.doubleValue() : 0);
                    }

                    // Tail columns: resume after the criteria columns
                    c = criteriaEndCol;
                    if (alloc.getTotalScore() != null)
                        row.createCell(c++).setCellValue(alloc.getTotalScore().doubleValue());
                    else c++;
                    row.createCell(c++).setCellValue(alloc.getOverallComment() != null ? alloc.getOverallComment() : "");
                    row.createCell(c++).setCellValue(alloc.getSubmittedAt() != null ? alloc.getSubmittedAt().toString() : "");
                }
            }

            // Auto-size columns
            for (int i = 0; i < col; i++) {
                sheet.setColumnWidth(i, Math.min(18 * 256, sheet.getColumnWidth(i)));
                if (i >= criteriaStartCol && i < criteriaEndCol) sheet.setColumnWidth(i, 14 * 256);
            }
            sheet.setColumnWidth(0, 22 * 256);
            sheet.setColumnWidth(col - 3, 20 * 256);
            sheet.setColumnWidth(col - 2, 20 * 256);

            // Sheet 2: per-project score summary
            Sheet summarySheet = wb.createSheet("成绩汇总");
            Row sHeader = summarySheet.createRow(0);
            String[] sCols = {"项目名称", "所属高校", "赛道", "评委数", "平均分", "最高分", "最低分"};
            for (int i = 0; i < sCols.length; i++) {
                Cell c = sHeader.createCell(i); c.setCellValue(sCols[i]); c.setCellStyle(headerStyle);
            }
            // Build project score summary
            Map<Long, List<ReviewAllocation>> allocsByProject = new LinkedHashMap<>();
            for (Project proj : projects) {
                List<ReviewAllocation> projAllocs = reviewAllocationMapper.findByProjectId(proj.getId()).stream()
                        .filter(a -> "COMPLETED".equals(a.getStatus()))
                        .toList();
                if (!projAllocs.isEmpty()) {
                    allocsByProject.put(proj.getId(), projAllocs);
                }
            }
            int sRow = 1;
            for (Project proj : projects) {
                List<ReviewAllocation> projAllocs = allocsByProject.get(proj.getId());
                if (projAllocs == null || projAllocs.isEmpty()) continue;
                Row row = summarySheet.createRow(sRow++);
                row.createCell(0).setCellValue(proj.getName() != null ? proj.getName() : "");
                row.createCell(1).setCellValue(getOrgName(proj.getUserId()));
                Track t = trackMap.get(proj.getTrackId());
                row.createCell(2).setCellValue(t != null ? (t.getGroupName() != null ? t.getName() + "（" + t.getGroupName() + "）" : t.getName()) : "");
                row.createCell(3).setCellValue(projAllocs.size());
                double avg = projAllocs.stream().mapToDouble(a -> a.getTotalScore() != null ? a.getTotalScore().doubleValue() : 0).average().orElse(0);
                double max = projAllocs.stream().mapToDouble(a -> a.getTotalScore() != null ? a.getTotalScore().doubleValue() : 0).max().orElse(0);
                double min = projAllocs.stream().mapToDouble(a -> a.getTotalScore() != null ? a.getTotalScore().doubleValue() : 0).min().orElse(0);
                row.createCell(4).setCellValue(Math.round(avg * 10.0) / 10.0);
                row.createCell(5).setCellValue(Math.round(max * 10.0) / 10.0);
                row.createCell(6).setCellValue(Math.round(min * 10.0) / 10.0);
            }
            for (int i = 0; i < sCols.length; i++) summarySheet.setColumnWidth(i, 18 * 256);
            summarySheet.setColumnWidth(0, 22 * 256);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            wb.write(bos);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("导出失败: " + e.getMessage());
        }
    }

    @Override
    public byte[] exportProjects(Long adminUserId, Long eventId) {
        checkCompetitionLevelAdmin(adminUserId, eventId);
        Event event = eventMapper.selectById(eventId);
        if (event == null) throw new BusinessException("赛事不存在");

        List<Project> projects = projectMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Project>()
                        .eq(Project::getEventId, eventId));

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("项目汇总");
            Row header = sheet.createRow(0);
            String[] cols = {"项目名称", "所属高校", "赛道", "状态", "简介"};
            CellStyle headerStyle = headerStyle(wb);
            for (int i = 0; i < cols.length; i++) {
                Cell c = header.createCell(i); c.setCellValue(cols[i]); c.setCellStyle(headerStyle);
            }
            for (int r = 0; r < projects.size(); r++) {
                Row row = sheet.createRow(r + 1);
                Project p = projects.get(r);
                row.createCell(0).setCellValue(p.getName() != null ? p.getName() : "");
                row.createCell(1).setCellValue(getOrgName(p.getUserId()));
                Track t = trackMapper.selectById(p.getTrackId());
                row.createCell(2).setCellValue(t != null ? (t.getGroupName() != null ? t.getName() + "（" + t.getGroupName() + "）" : t.getName()) : "");
                row.createCell(3).setCellValue(p.getStatus() != null ? p.getStatus().name() : "");
                row.createCell(4).setCellValue(p.getDescription() != null ? p.getDescription() : "");
            }
            autoSize(sheet, cols.length);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            wb.write(bos);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("导出失败: " + e.getMessage());
        }
    }

    @Override
    public byte[] exportAllocations(Long adminUserId, Long eventId) {
        checkCompetitionLevelAdmin(adminUserId, eventId);
        List<ReviewAllocation> allocations = reviewAllocationMapper.findByEventId(eventId);

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("评委分配明细");
            Row header = sheet.createRow(0);
            String[] cols = {"项目名称", "赛道", "评委姓名", "状态", "总分", "评语", "提交时间"};
            CellStyle headerStyle = headerStyle(wb);
            for (int i = 0; i < cols.length; i++) {
                Cell c = header.createCell(i); c.setCellValue(cols[i]); c.setCellStyle(headerStyle);
            }
            for (int r = 0; r < allocations.size(); r++) {
                Row row = sheet.createRow(r + 1);
                ReviewAllocation ra = allocations.get(r);
                Project p = projectMapper.selectById(ra.getProjectId());
                row.createCell(0).setCellValue(p != null ? p.getName() : "");
                if (p != null) {
                    Track t = trackMapper.selectById(p.getTrackId());
                    row.createCell(1).setCellValue(t != null ? (t.getGroupName() != null ? t.getName() + "（" + t.getGroupName() + "）" : t.getName()) : "");
                }
                User judge = userMapper.selectById(ra.getJudgeId());
                row.createCell(2).setCellValue(judge != null ? (judge.getRealName() != null ? judge.getRealName() : judge.getUsername()) : "");
                row.createCell(3).setCellValue("COMPLETED".equals(ra.getStatus()) ? "已完成" : "待评审");
                if (ra.getTotalScore() != null) row.createCell(4).setCellValue(ra.getTotalScore().doubleValue());
                row.createCell(5).setCellValue(ra.getOverallComment() != null ? ra.getOverallComment() : "");
                if (ra.getSubmittedAt() != null) row.createCell(6).setCellValue(ra.getSubmittedAt().toString());
            }
            autoSize(sheet, cols.length);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            wb.write(bos);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("导出失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void batchAllocateByTrack(Long adminUserId, Long eventId, Long trackId, List<Long> judgeIds) {
        User admin = userMapper.selectById(adminUserId);
        if (admin == null) throw new BusinessException("管理员不存在");

        List<Long> visibleOrgIds = AdminLevelResolver.getVisibleOrgIds(admin, organizationMapper);
        List<Project> projects = projectMapper.selectList(
                new QueryWrapper<Project>().lambda()
                        .eq(Project::getEventId, eventId)
                        .eq(Project::getTrackId, trackId)
                        .eq(Project::getStatus, ProjectStatus.APPROVED));

        int allocated = 0;
        for (Project project : projects) {
            User creator = userMapper.selectById(project.getUserId());
            if (creator == null || creator.getOrganizationId() == null ||
                !visibleOrgIds.contains(creator.getOrganizationId())) continue;

            for (Long judgeId : judgeIds) {
                User judge = userMapper.selectById(judgeId);
                if (judge == null || judge.getRole() != UserRole.JUDGE) continue;

                Long existing = reviewAllocationMapper.selectCount(
                        new QueryWrapper<ReviewAllocation>().lambda()
                                .eq(ReviewAllocation::getProjectId, project.getId())
                                .eq(ReviewAllocation::getJudgeId, judgeId));
                if (existing > 0) continue;

                ReviewAllocation allocation = new ReviewAllocation();
                allocation.setProjectId(project.getId());
                allocation.setJudgeId(judgeId);
                allocation.setStatus("PENDING");
                reviewAllocationMapper.insert(allocation);
                allocated++;
            }
        }
    }

    private String getOrgName(Long userId) {
        if (userId == null) return "";
        User u = userMapper.selectById(userId);
        if (u != null && u.getOrganizationId() != null) {
            String name = organizationMapper.findSchoolOrgName(u.getOrganizationId());
            return name != null ? name : "";
        }
        return "";
    }

    private CellStyle headerStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private void autoSize(Sheet sheet, int colCount) {
        for (int i = 0; i < colCount; i++) {
            sheet.setColumnWidth(i, 16 * 256);
        }
    }
}
