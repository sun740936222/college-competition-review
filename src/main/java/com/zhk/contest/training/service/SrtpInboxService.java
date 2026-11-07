package com.zhk.contest.training.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zhk.contest.training.entity.*;
import com.zhk.contest.training.enums.SrtpRecordStatus;
import com.zhk.contest.training.enums.SrtpStage;
import com.zhk.contest.training.mapper.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 审批工作台"待我审批"聚合：跨四阶段筛出状态=IN_REVIEW、且当前节点角色与登录人身份匹配的记录。
 * 审批动作复用各阶段既有 approve/reject 接口。
 */
@Service
public class SrtpInboxService {

    private final SrtpTopicMapper topicMapper;
    private final SrtpProjectMapper projectMapper;
    private final SrtpMidtermReportMapper midtermMapper;
    private final SrtpConclusionReportMapper conclusionMapper;
    private final SrtpApprovalEngine approvalEngine;
    private final SrtpRoleService roleService;

    public SrtpInboxService(SrtpTopicMapper topicMapper, SrtpProjectMapper projectMapper,
                            SrtpMidtermReportMapper midtermMapper, SrtpConclusionReportMapper conclusionMapper,
                            SrtpApprovalEngine approvalEngine, SrtpRoleService roleService) {
        this.topicMapper = topicMapper;
        this.projectMapper = projectMapper;
        this.midtermMapper = midtermMapper;
        this.conclusionMapper = conclusionMapper;
        this.approvalEngine = approvalEngine;
        this.roleService = roleService;
    }

    /** 当前登录人的大创身份，用于前端决定是否显示"审批工作台" */
    public Map<String, Object> myIdentity(Long userId) {
        Map<String, Object> m = new LinkedHashMap<>();
        boolean teacher = roleService.hasRole(userId, com.zhk.contest.training.enums.SrtpRoleType.TEACHER);
        boolean collegeExpert = roleService.isExpert(userId, com.zhk.contest.training.enums.ExpertLevel.COLLEGE);
        boolean schoolExpert = roleService.isExpert(userId, com.zhk.contest.training.enums.ExpertLevel.SCHOOL);
        m.put("teacher", teacher);
        m.put("collegeExpert", collegeExpert);
        m.put("schoolExpert", schoolExpert);
        m.put("canApprove", teacher || collegeExpert || schoolExpert);
        return m;
    }

    /** 待我审批列表（可按阶段过滤） */
    public List<Map<String, Object>> myPending(Long userId, SrtpStage stageFilter) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (stageFilter == null || stageFilter == SrtpStage.TOPIC) {
            for (SrtpTopic t : inReview(topicMapper, SrtpTopic.class)) {
                addIfMine(result, userId, SrtpStage.TOPIC, t.getId(), t.getName(), t.getBatchId(), t, t.getOrgId());
            }
        }
        if (stageFilter == null || stageFilter == SrtpStage.PROJECT) {
            for (SrtpProject p : inReview(projectMapper, SrtpProject.class)) {
                addIfMine(result, userId, SrtpStage.PROJECT, p.getId(), p.getName(), p.getBatchId(), p, p.getOrgId());
            }
        }
        if (stageFilter == null || stageFilter == SrtpStage.MIDTERM) {
            for (SrtpMidtermReport r : inReview(midtermMapper, SrtpMidtermReport.class)) {
                addIfMine(result, userId, SrtpStage.MIDTERM, r.getId(), r.getSnapshotName(), r.getBatchId(), r, orgOfProject(r.getProjectId()));
            }
        }
        if (stageFilter == null || stageFilter == SrtpStage.CONCLUSION) {
            for (SrtpConclusionReport r : inReview(conclusionMapper, SrtpConclusionReport.class)) {
                addIfMine(result, userId, SrtpStage.CONCLUSION, r.getId(), r.getSnapshotName(), r.getBatchId(), r, orgOfProject(r.getProjectId()));
            }
        }
        return result;
    }

    private <T> List<T> inReview(com.baomidou.mybatisplus.core.mapper.BaseMapper<T> mapper, Class<T> clazz) {
        return mapper.selectList(new QueryWrapper<T>().eq("status", SrtpRecordStatus.IN_REVIEW.name()));
    }

    private void addIfMine(List<Map<String, Object>> result, Long userId, SrtpStage stage,
                           Long id, String name, Long batchId, SrtpFlowRecord record, Long recordOrgId) {
        SrtpApprovalFlowConfig node = approvalEngine.currentNode(stage, record);
        if (node == null) return;
        if (!approvalEngine.canActOnNode(userId, stage, id, node, recordOrgId)) return;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("stage", stage.name());
        m.put("id", id);
        m.put("name", name);
        m.put("batchId", batchId);
        m.put("currentStep", node.getStepOrder());
        m.put("currentStepName", node.getStepName());
        result.add(m);
    }

    private Long orgOfProject(Long projectId) {
        if (projectId == null) return null;
        SrtpProject p = projectMapper.selectById(projectId);
        return p != null ? p.getOrgId() : null;
    }
}
