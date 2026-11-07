package com.zhk.contest.training.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zhk.contest.entity.User;
import com.zhk.contest.exception.BusinessException;
import com.zhk.contest.mapper.UserMapper;
import com.zhk.contest.training.entity.SrtpConclusionReport;
import com.zhk.contest.training.entity.SrtpExpert;
import com.zhk.contest.training.entity.SrtpExpertAssignment;
import com.zhk.contest.training.entity.SrtpMidtermReport;
import com.zhk.contest.training.entity.SrtpProject;
import com.zhk.contest.training.entity.SrtpTopic;
import com.zhk.contest.training.enums.ExpertStatus;
import com.zhk.contest.training.enums.SrtpStage;
import com.zhk.contest.training.mapper.SrtpConclusionReportMapper;
import com.zhk.contest.training.mapper.SrtpExpertAssignmentMapper;
import com.zhk.contest.training.mapper.SrtpExpertMapper;
import com.zhk.contest.training.mapper.SrtpMidtermReportMapper;
import com.zhk.contest.training.mapper.SrtpProjectMapper;
import com.zhk.contest.training.mapper.SrtpTopicMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 校/院级专家的独立评审(顾问性质,不阻塞主审批流)。
 * 学院/学校管理员在"立项管理"里"分配校专"后,被分配的专家可在自己的工作台看到该记录,
 * 提交评审意见(通过 / 驳回 + 评语),结果写回 srtp_expert_assignment 供推荐省级等场景参考。
 */
@Service
public class SrtpExpertReviewService {

    private static final Set<String> ALLOWED_ACTIONS = Set.of("APPROVED", "REJECTED");

    private final SrtpExpertAssignmentMapper assignmentMapper;
    private final SrtpExpertMapper expertMapper;
    private final SrtpProjectMapper projectMapper;
    private final SrtpTopicMapper topicMapper;
    private final SrtpMidtermReportMapper midtermMapper;
    private final SrtpConclusionReportMapper conclusionMapper;
    private final UserMapper userMapper;

    public SrtpExpertReviewService(SrtpExpertAssignmentMapper assignmentMapper,
                                   SrtpExpertMapper expertMapper,
                                   SrtpProjectMapper projectMapper,
                                   SrtpTopicMapper topicMapper,
                                   SrtpMidtermReportMapper midtermMapper,
                                   SrtpConclusionReportMapper conclusionMapper,
                                   UserMapper userMapper) {
        this.assignmentMapper = assignmentMapper;
        this.expertMapper = expertMapper;
        this.projectMapper = projectMapper;
        this.topicMapper = topicMapper;
        this.midtermMapper = midtermMapper;
        this.conclusionMapper = conclusionMapper;
        this.userMapper = userMapper;
    }

    /** 当前登录专家的分配清单（含已完成，用于查看历史评审） */
    public List<Map<String, Object>> myAssignments(Long userId) {
        SrtpExpert expert = findExpert(userId);
        if (expert == null) return List.of();
        List<SrtpExpertAssignment> assignments = assignmentMapper.selectList(
                new QueryWrapper<SrtpExpertAssignment>().lambda()
                        .eq(SrtpExpertAssignment::getExpertId, expert.getId())
                        .orderByDesc(SrtpExpertAssignment::getCreatedAt));
        List<Map<String, Object>> result = new ArrayList<>();
        for (SrtpExpertAssignment a : assignments) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", a.getId());
            row.put("stage", a.getStage() != null ? a.getStage().name() : null);
            row.put("targetId", a.getTargetId());
            row.put("reviewStatus", a.getReviewStatus());
            row.put("score", a.getScore());
            row.put("reviewComment", a.getReviewComment());
            row.put("reviewedAt", a.getReviewedAt());
            row.put("assignedAt", a.getCreatedAt());
            fillTargetInfo(row, a.getStage(), a.getTargetId());
            result.add(row);
        }
        return result;
    }

    /** 专家提交评审意见：必填 action + score(0-100)；不同意时 comment 也必填 */
    @Transactional
    public void submitReview(Long userId, Long assignmentId, String action, Integer score, String comment) {
        if (action == null || !ALLOWED_ACTIONS.contains(action)) {
            throw new BusinessException("action 必须为 APPROVED 或 REJECTED");
        }
        if (score == null || score < 0 || score > 100) {
            throw new BusinessException("评审必须填写 0-100 之间的总分");
        }
        if ("REJECTED".equals(action) && (comment == null || comment.isBlank())) {
            throw new BusinessException("不同意时必须填写评审意见");
        }
        SrtpExpertAssignment a = assignmentMapper.selectById(assignmentId);
        if (a == null) throw new BusinessException("评审分配不存在");
        if (!"PENDING".equals(a.getReviewStatus())) {
            throw new BusinessException("该分配已评审过，不能重复提交");
        }
        SrtpExpert expert = expertMapper.selectById(a.getExpertId());
        if (expert == null || expert.getUserId() == null || !expert.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权评审：该分配不是分配给你的");
        }
        a.setReviewStatus(action);
        a.setScore(score);
        a.setReviewComment(comment);
        a.setReviewedAt(LocalDateTime.now());
        assignmentMapper.updateById(a);
    }

    // ===== 内部 =====

    private SrtpExpert findExpert(Long userId) {
        if (userId == null) return null;
        return expertMapper.selectOne(new QueryWrapper<SrtpExpert>().lambda()
                .eq(SrtpExpert::getUserId, userId)
                .eq(SrtpExpert::getStatus, ExpertStatus.APPROVED)
                .last("LIMIT 1"));
    }

    private void fillTargetInfo(Map<String, Object> row, SrtpStage stage, Long targetId) {
        if (stage == null || targetId == null) return;
        switch (stage) {
            case PROJECT -> {
                SrtpProject p = projectMapper.selectById(targetId);
                if (p != null) {
                    row.put("targetName", p.getName());
                    row.put("projectNo", p.getProjectNo());
                    row.put("targetStatus", p.getStatus() != null ? p.getStatus().name() : null);
                    row.put("batchId", p.getBatchId());
                }
            }
            case TOPIC -> {
                SrtpTopic t = topicMapper.selectById(targetId);
                if (t != null) {
                    row.put("targetName", t.getName());
                    row.put("targetStatus", t.getStatus() != null ? t.getStatus().name() : null);
                    row.put("batchId", t.getBatchId());
                }
            }
            case MIDTERM -> {
                SrtpMidtermReport r = midtermMapper.selectById(targetId);
                if (r != null) {
                    row.put("targetName", r.getSnapshotName());
                    row.put("targetStatus", r.getStatus() != null ? r.getStatus().name() : null);
                    row.put("batchId", r.getBatchId());
                }
            }
            case CONCLUSION -> {
                SrtpConclusionReport r = conclusionMapper.selectById(targetId);
                if (r != null) {
                    row.put("targetName", r.getSnapshotName());
                    row.put("targetStatus", r.getStatus() != null ? r.getStatus().name() : null);
                    row.put("batchId", r.getBatchId());
                }
            }
        }
    }
}
