package com.zhk.contest.service.impl;

import com.zhk.contest.dto.request.ScoreSubmitRequest;
import com.zhk.contest.dto.response.*;
import com.zhk.contest.entity.*;
import com.zhk.contest.enums.UserRole;
import com.zhk.contest.exception.BusinessException;
import com.zhk.contest.mapper.*;
import com.zhk.contest.service.JudgeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class JudgeServiceImpl implements JudgeService {

    private final UserMapper userMapper;
    private final EventMapper eventMapper;
    private final TrackMapper trackMapper;
    private final ProjectMapper projectMapper;
    private final OrganizationMapper organizationMapper;
    private final ScoreCriteriaMapper scoreCriteriaMapper;
    private final ReviewAllocationMapper reviewAllocationMapper;
    private final ScoreRecordMapper scoreRecordMapper;
    private final ProjectMaterialMapper projectMaterialMapper;

    public JudgeServiceImpl(UserMapper userMapper, EventMapper eventMapper,
                            TrackMapper trackMapper, ProjectMapper projectMapper,
                            OrganizationMapper organizationMapper,
                            ScoreCriteriaMapper scoreCriteriaMapper,
                            ReviewAllocationMapper reviewAllocationMapper,
                            ScoreRecordMapper scoreRecordMapper,
                            ProjectMaterialMapper projectMaterialMapper) {
        this.userMapper = userMapper;
        this.eventMapper = eventMapper;
        this.trackMapper = trackMapper;
        this.projectMapper = projectMapper;
        this.organizationMapper = organizationMapper;
        this.scoreCriteriaMapper = scoreCriteriaMapper;
        this.reviewAllocationMapper = reviewAllocationMapper;
        this.scoreRecordMapper = scoreRecordMapper;
        this.projectMaterialMapper = projectMaterialMapper;
    }

    private User checkJudge(Long judgeId) {
        User judge = userMapper.selectById(judgeId);
        if (judge == null || judge.getRole() != UserRole.JUDGE) {
            throw new BusinessException(401, "评委身份验证失败");
        }
        return judge;
    }

    private ReviewAllocation getAllocationOrThrow(Long judgeId, Long projectId) {
        List<ReviewAllocation> allocs = reviewAllocationMapper.findByJudgeId(judgeId);
        return allocs.stream()
                .filter(a -> a.getProjectId().equals(projectId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(403, "该项目未分配给您评审"));
    }

    @Override
    public IPage<JudgeProjectResponse> listMyProjects(Long judgeId, String status, Page<ReviewAllocation> page) {
        checkJudge(judgeId);

        IPage<ReviewAllocation> allocPage;
        if (status != null && !status.isBlank()) {
            allocPage = reviewAllocationMapper.findByJudgeIdAndStatusPaged(page, judgeId, status);
        } else {
            allocPage = reviewAllocationMapper.findByJudgeIdPaged(page, judgeId);
        }

        return allocPage.convert(ra -> {
            JudgeProjectResponse resp = new JudgeProjectResponse();
            resp.setId(ra.getProjectId());
            resp.setStatus(ra.getStatus());
            resp.setTotalScore(ra.getTotalScore());
            resp.setOverallComment(ra.getOverallComment());
            resp.setAllocatedAt(ra.getAllocatedAt());

            Project project = projectMapper.selectById(ra.getProjectId());
            if (project != null) {
                resp.setName(project.getName());
                Event event = eventMapper.selectById(project.getEventId());
                if (event != null) resp.setEventName(event.getName());
                Track track = trackMapper.selectById(project.getTrackId());
                if (track != null) {
                    resp.setTrackName(track.getGroupName() != null
                            ? track.getName() + "（" + track.getGroupName() + "）" : track.getName());
                }
            }

            return resp;
        });
    }

    @Override
    public JudgeProjectDetailResponse getProjectDetail(Long judgeId, Long projectId) {
        User judge = checkJudge(judgeId);
        ReviewAllocation allocation = getAllocationOrThrow(judgeId, projectId);

        Project project = projectMapper.selectById(projectId);
        if (project == null) throw new BusinessException("项目不存在");

        JudgeProjectDetailResponse resp = new JudgeProjectDetailResponse();
        resp.setId(project.getId());
        resp.setName(project.getName());
        resp.setDescription(project.getDescription());
        resp.setAllocationId(allocation.getId());
        resp.setAllocationStatus(allocation.getStatus());
        resp.setSubmittedAt(allocation.getSubmittedAt());
        resp.setOverallComment(allocation.getOverallComment());
        resp.setTotalScore(allocation.getTotalScore());

        // Watermark info from the already-loaded judge entity
        resp.setJudgeName(judge.getRealName() != null ? judge.getRealName() : judge.getUsername());
        resp.setJudgePhone(judge.getPhone());

        Event event = eventMapper.selectById(project.getEventId());
        if (event != null) {
            resp.setEventId(event.getId());
            resp.setEventName(event.getName());
            resp.setAllowResubmit(event.getAllowResubmit());
            resp.setRequireComment(event.getRequireComment());
            resp.setReviewStartTime(event.getReviewStartTime());
            resp.setReviewEndTime(event.getReviewEndTime());
            resp.setEventAttachmentName(event.getAttachmentName());
        }
        Track track = trackMapper.selectById(project.getTrackId());
        if (track != null) {
            resp.setTrackName(track.getGroupName() != null
                    ? track.getName() + "（" + track.getGroupName() + "）" : track.getName());
        }

        // Get scoring criteria for this track
        List<ScoreCriteria> criteria = scoreCriteriaMapper.findByTrackId(project.getTrackId());
        resp.setCriteria(criteria.stream().map(ScoreCriteriaResponse::from).toList());

        // Get existing scores for this allocation — use Map for O(1) criteria lookup
        Map<Long, ScoreCriteria> criteriaMap = criteria.stream()
                .collect(Collectors.toMap(ScoreCriteria::getId, c -> c));
        List<ScoreRecord> records = scoreRecordMapper.findByAllocationId(allocation.getId());
        List<ScoreRecordResponse> scoreResponses = records.stream().map(sr -> {
            ScoreRecordResponse srr = ScoreRecordResponse.from(sr);
            ScoreCriteria sc = criteriaMap.get(sr.getCriteriaId());
            if (sc != null) {
                srr.setCriteriaName(sc.getName());
                srr.setMaxScore(sc.getMaxScore());
            }
            return srr;
        }).toList();
        resp.setScores(scoreResponses);

        // Load materials
        List<ProjectMaterial> materials = projectMaterialMapper.findByProjectId(projectId);
        List<MaterialResponse> materialResponses = materials.stream().map(m -> {
            MaterialResponse mr = new MaterialResponse();
            mr.setId(m.getId());
            mr.setMaterialType(m.getMaterialType().name());
            mr.setOriginalFilename(m.getOriginalFilename());
            mr.setStoredFilename(m.getStoredFilename());
            mr.setFileSize(m.getFileSize());
            return mr;
        }).toList();
        resp.setMaterials(materialResponses);

        return resp;
    }

    @Override
    public List<ScoreCriteriaResponse> getProjectCriteria(Long judgeId, Long projectId) {
        checkJudge(judgeId);
        getAllocationOrThrow(judgeId, projectId);

        Project project = projectMapper.selectById(projectId);
        if (project == null) throw new BusinessException("项目不存在");

        return scoreCriteriaMapper.findByTrackId(project.getTrackId())
                .stream().map(ScoreCriteriaResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public void submitScores(Long judgeId, Long projectId, ScoreSubmitRequest request) {
        checkJudge(judgeId);
        ReviewAllocation allocation = getAllocationOrThrow(judgeId, projectId);

        Project project = projectMapper.selectById(projectId);
        if (project == null) throw new BusinessException("项目不存在");

        Event event = eventMapper.selectById(project.getEventId());

        // Enforce review time window
        if (event != null) {
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            if (event.getReviewStartTime() != null && now.isBefore(event.getReviewStartTime())) {
                throw new BusinessException("评审尚未开始，开始时间：" + event.getReviewStartTime());
            }
            if (event.getReviewEndTime() != null && now.isAfter(event.getReviewEndTime())) {
                throw new BusinessException("评审已结束，结束时间：" + event.getReviewEndTime());
            }
        }

        if ("COMPLETED".equals(allocation.getStatus())) {
            boolean allowResubmit = event != null && Boolean.TRUE.equals(event.getAllowResubmit());
            if (!allowResubmit) {
                throw new BusinessException("该项目已评审提交，无法修改");
            }
        }

        boolean requireComment = event != null && Boolean.TRUE.equals(event.getRequireComment());
        if ("SUBMIT".equals(request.getAction()) && requireComment) {
            if (request.getOverallComment() == null || request.getOverallComment().isBlank()) {
                throw new BusinessException("当前赛事要求提交时必须填写评语");
            }
        }
        List<ScoreCriteria> criteria = scoreCriteriaMapper.findByTrackId(project.getTrackId());
        Map<Long, ScoreCriteria> criteriaMap = criteria.stream()
                .collect(Collectors.toMap(ScoreCriteria::getId, c -> c));

        if (request.getScores() == null || request.getScores().isEmpty()) {
            throw new BusinessException("请填写评分");
        }

        // Delete existing records and re-insert
        List<ScoreRecord> existingRecords = scoreRecordMapper.findByAllocationId(allocation.getId());
        for (ScoreRecord sr : existingRecords) {
            scoreRecordMapper.deleteById(sr.getId());
        }

        BigDecimal totalScore = BigDecimal.ZERO;
        for (ScoreSubmitRequest.ScoreItem item : request.getScores()) {
            ScoreCriteria sc = criteriaMap.get(item.getCriteriaId());
            if (sc == null) throw new BusinessException("评分维度不存在: " + item.getCriteriaId());

            if (item.getScore() == null) {
                throw new BusinessException("请填写所有评分维度的分值");
            }

            if (item.getScore().compareTo(BigDecimal.ZERO) < 0
                    || item.getScore().compareTo(BigDecimal.valueOf(sc.getMaxScore())) > 0) {
                throw new BusinessException(sc.getName() + " 分值范围为0-" + sc.getMaxScore());
            }

            ScoreRecord record = new ScoreRecord();
            record.setAllocationId(allocation.getId());
            record.setCriteriaId(item.getCriteriaId());
            record.setScore(item.getScore());
            scoreRecordMapper.insert(record);

            totalScore = totalScore.add(item.getScore());
        }

        // Update allocation
        allocation.setTotalScore(totalScore);
        allocation.setOverallComment(request.getOverallComment());

        if ("SUBMIT".equals(request.getAction())) {
            allocation.setStatus("COMPLETED");
            allocation.setSubmittedAt(java.time.LocalDateTime.now());
        } else {
            allocation.setStatus("PENDING");
        }

        reviewAllocationMapper.updateById(allocation);
    }
}
