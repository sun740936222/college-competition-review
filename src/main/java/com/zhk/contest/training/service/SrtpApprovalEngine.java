package com.zhk.contest.training.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zhk.contest.exception.BusinessException;
import com.zhk.contest.training.entity.SrtpApprovalFlowConfig;
import com.zhk.contest.training.entity.SrtpApprovalRecord;
import com.zhk.contest.training.entity.SrtpExpert;
import com.zhk.contest.training.entity.SrtpExpertAssignment;
import com.zhk.contest.training.entity.SrtpProjectAdvisor;
import com.zhk.contest.training.enums.ApprovalAction;
import com.zhk.contest.training.enums.ApproverRole;
import com.zhk.contest.training.enums.SrtpRecordStatus;
import com.zhk.contest.training.enums.SrtpStage;
import com.zhk.contest.training.mapper.SrtpApprovalFlowConfigMapper;
import com.zhk.contest.training.mapper.SrtpApprovalRecordMapper;
import com.zhk.contest.training.mapper.SrtpExpertAssignmentMapper;
import com.zhk.contest.training.mapper.SrtpExpertMapper;
import com.zhk.contest.training.mapper.SrtpProjectAdvisorMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * SRTP 四阶段通用审批引擎。
 * 流程解析：优先取该批次配置的节点；无则回退全局默认模板（batch_id=NULL）。
 * 驱动 {@link SrtpFlowRecord} 状态机（决策 + 变更记录状态），记录持久化由各业务 Service 负责；
 * 审批日志由本引擎写入。驳回统一"退回申报人"（见 PRD 决策4）。
 * 「指导教师审核」节点仅本记录的指导教师本人可审（按 user_id 绑定，解决同名同姓与串审）。
 */
@Service
public class SrtpApprovalEngine {

    private final SrtpApprovalFlowConfigMapper flowConfigMapper;
    private final SrtpApprovalRecordMapper approvalRecordMapper;
    private final SrtpProjectAdvisorMapper advisorMapper;
    private final SrtpExpertMapper expertMapper;
    private final SrtpExpertAssignmentMapper assignmentMapper;
    private final SrtpRoleService roleService;

    public SrtpApprovalEngine(SrtpApprovalFlowConfigMapper flowConfigMapper,
                              SrtpApprovalRecordMapper approvalRecordMapper,
                              SrtpProjectAdvisorMapper advisorMapper,
                              SrtpExpertMapper expertMapper,
                              SrtpExpertAssignmentMapper assignmentMapper,
                              SrtpRoleService roleService) {
        this.flowConfigMapper = flowConfigMapper;
        this.approvalRecordMapper = approvalRecordMapper;
        this.advisorMapper = advisorMapper;
        this.expertMapper = expertMapper;
        this.assignmentMapper = assignmentMapper;
        this.roleService = roleService;
    }

    /** 解析某阶段生效的审批流：批次专属优先，否则全局模板；按 step_order 升序 */
    public List<SrtpApprovalFlowConfig> resolveFlow(SrtpStage stage, Long batchId) {
        if (batchId != null) {
            List<SrtpApprovalFlowConfig> batchFlow = flowConfigMapper.selectList(
                    new QueryWrapper<SrtpApprovalFlowConfig>().lambda()
                            .eq(SrtpApprovalFlowConfig::getBatchId, batchId)
                            .eq(SrtpApprovalFlowConfig::getStage, stage)
                            .orderByAsc(SrtpApprovalFlowConfig::getStepOrder));
            if (!batchFlow.isEmpty()) return batchFlow;
        }
        return flowConfigMapper.selectList(
                new QueryWrapper<SrtpApprovalFlowConfig>().lambda()
                        .isNull(SrtpApprovalFlowConfig::getBatchId)
                        .eq(SrtpApprovalFlowConfig::getStage, stage)
                        .orderByAsc(SrtpApprovalFlowConfig::getStepOrder));
    }

    /** 当前所处审批节点；无（已终态/无流程）则返回 null */
    public SrtpApprovalFlowConfig currentNode(SrtpStage stage, SrtpFlowRecord record) {
        if (record.getCurrentStep() == null || record.getCurrentStep() <= 0) return null;
        return resolveFlow(stage, record.getBatchId()).stream()
                .filter(n -> n.getStepOrder().equals(record.getCurrentStep()))
                .findFirst().orElse(null);
    }

    /**
     * 提交：DRAFT / RETURNED → 进入审批流第一节点；若该阶段无审批节点，直接通过。
     * 只变更 record 状态，不落库（由调用方 Service 保存）。
     */
    public void submit(SrtpStage stage, SrtpFlowRecord record) {
        if (record.getStatus() == SrtpRecordStatus.IN_REVIEW
                || record.getStatus() == SrtpRecordStatus.APPROVED) {
            throw new BusinessException("当前状态不可提交");
        }
        List<SrtpApprovalFlowConfig> flow = resolveFlow(stage, record.getBatchId());
        record.setRejectReason(null);
        if (flow.isEmpty()) {
            record.setStatus(SrtpRecordStatus.APPROVED);
            record.setCurrentStep(0);
        } else {
            record.setStatus(SrtpRecordStatus.IN_REVIEW);
            record.setCurrentStep(flow.get(0).getStepOrder());
        }
    }

    /**
     * 通过：校验审批人权限 → 记录审批日志 → 推进到下一节点或终态 APPROVED。
     * recordOrgId 为记录所属学院（管理员范围校验用）。
     */
    public void approve(SrtpStage stage, SrtpFlowRecord record, Long approverId,
                        String comment, Long recordOrgId) {
        SrtpApprovalFlowConfig node = requireActiveNode(stage, record);
        requireCanApprove(approverId, stage, record.getId(), node, recordOrgId);
        approveInternal(stage, record, approverId, comment, node);
    }

    /** 与 approve 相同，但跳过权限校验：仅供上层聚合逻辑（如"学校管理员汇总校专家评审后收尾"）调用 */
    public void approveBypassPermission(SrtpStage stage, SrtpFlowRecord record, Long actorId, String comment) {
        SrtpApprovalFlowConfig node = requireActiveNode(stage, record);
        approveInternal(stage, record, actorId, comment, node);
    }

    private void approveInternal(SrtpStage stage, SrtpFlowRecord record, Long actorId, String comment,
                                  SrtpApprovalFlowConfig node) {
        writeRecord(stage, record.getId(), node, actorId, ApprovalAction.APPROVE, comment);
        List<SrtpApprovalFlowConfig> flow = resolveFlow(stage, record.getBatchId());
        SrtpApprovalFlowConfig next = flow.stream()
                .filter(n -> n.getStepOrder() > node.getStepOrder())
                .min((a, b) -> Integer.compare(a.getStepOrder(), b.getStepOrder()))
                .orElse(null);
        if (next == null) {
            record.setStatus(SrtpRecordStatus.APPROVED);
        } else {
            record.setStatus(SrtpRecordStatus.IN_REVIEW);
            record.setCurrentStep(next.getStepOrder());
        }
    }

    /** 与 rejectTo 相同，但跳过权限校验：仅供聚合收尾逻辑调用 */
    public void rejectToBypassPermission(SrtpStage stage, SrtpFlowRecord record, Long actorId,
                                          String comment, Integer targetStep) {
        SrtpApprovalFlowConfig node = requireActiveNode(stage, record);
        if (comment == null || comment.isBlank()) throw new BusinessException("驳回必须填写意见");
        writeRecord(stage, record.getId(), node, actorId, ApprovalAction.REJECT, comment);
        if (targetStep == null || targetStep <= 0) {
            record.setStatus(SrtpRecordStatus.RETURNED);
            record.setCurrentStep(0);
            record.setRejectReason(comment);
            return;
        }
        boolean valid = resolveFlow(stage, record.getBatchId()).stream()
                .anyMatch(n -> n.getStepOrder().equals(targetStep) && n.getStepOrder() < node.getStepOrder());
        if (!valid) throw new BusinessException("退回目标节点无效");
        record.setStatus(SrtpRecordStatus.IN_REVIEW);
        record.setCurrentStep(targetStep);
        record.setRejectReason(comment);
    }

    /** 驳回：退回申报人（状态 RETURNED），记录意见；重新提交后从头再走 */
    public void reject(SrtpStage stage, SrtpFlowRecord record, Long approverId, String comment, Long recordOrgId) {
        rejectTo(stage, record, approverId, comment, null, recordOrgId);
    }

    /**
     * 驳回到指定节点：
     * targetStep 为空或 &lt;=0 → 退回申报人（RETURNED，从头再走）；
     * targetStep 为某个更靠前的审批节点 → 退回该节点重审（状态仍 IN_REVIEW，currentStep=targetStep）。
     */
    public void rejectTo(SrtpStage stage, SrtpFlowRecord record, Long approverId, String comment,
                         Integer targetStep, Long recordOrgId) {
        SrtpApprovalFlowConfig node = requireActiveNode(stage, record);
        requireCanApprove(approverId, stage, record.getId(), node, recordOrgId);
        if (comment == null || comment.isBlank()) {
            throw new BusinessException("驳回必须填写意见");
        }
        writeRecord(stage, record.getId(), node, approverId, ApprovalAction.REJECT, comment);

        if (targetStep == null || targetStep <= 0) {
            record.setStatus(SrtpRecordStatus.RETURNED);
            record.setCurrentStep(0);
            record.setRejectReason(comment);
            return;
        }
        // 校验目标节点为当前节点之前的有效审批节点
        boolean valid = resolveFlow(stage, record.getBatchId()).stream()
                .anyMatch(n -> n.getStepOrder().equals(targetStep) && n.getStepOrder() < node.getStepOrder());
        if (!valid) {
            throw new BusinessException("退回目标节点无效");
        }
        record.setStatus(SrtpRecordStatus.IN_REVIEW);
        record.setCurrentStep(targetStep);
        record.setRejectReason(comment);
    }

    /** 某记录的审批日志（时间线） */
    public List<SrtpApprovalRecord> history(SrtpStage stage, Long targetId) {
        return approvalRecordMapper.selectList(
                new QueryWrapper<SrtpApprovalRecord>().lambda()
                        .eq(SrtpApprovalRecord::getStage, stage)
                        .eq(SrtpApprovalRecord::getTargetId, targetId)
                        .orderByAsc(SrtpApprovalRecord::getCreatedAt));
    }

    private SrtpApprovalFlowConfig requireActiveNode(SrtpStage stage, SrtpFlowRecord record) {
        if (record.getStatus() != SrtpRecordStatus.IN_REVIEW) {
            throw new BusinessException("当前记录不在审批中，无法操作");
        }
        SrtpApprovalFlowConfig node = currentNode(stage, record);
        if (node == null) {
            throw new BusinessException("未找到当前审批节点");
        }
        return node;
    }

    private void requireCanApprove(Long approverId, SrtpStage stage, Long targetId,
                                   SrtpApprovalFlowConfig node, Long recordOrgId) {
        if (!canActOnNode(approverId, stage, targetId, node, recordOrgId)) {
            throw new BusinessException(403, "无权审批当前节点：" + node.getStepName());
        }
    }

    /**
     * 判断某人能否操作当前节点：
     * 「指导教师」节点 → 必须是本记录的指导教师本人（按 srtp_project_advisor.user_id 绑定）；
     * 「校/院级专家」节点 → 除了角色匹配，还必须在 srtp_expert_assignment 里被明确指派到本记录，
     *                      以支持"学校管理员选定某几位专家为特定项目评审"的场景；
     * 其它节点 → 按角色/组织范围判定（{@link SrtpRoleService#matchesApproverRole}）。
     */
    public boolean canActOnNode(Long approverId, SrtpStage stage, Long targetId,
                                SrtpApprovalFlowConfig node, Long recordOrgId) {
        if (node == null || approverId == null) return false;
        if (node.getApproverRole() == ApproverRole.TEACHER) {
            return isAdvisorOf(approverId, stage, targetId);
        }
        if (node.getApproverRole() == ApproverRole.COLLEGE_EXPERT
                || node.getApproverRole() == ApproverRole.SCHOOL_EXPERT) {
            if (!roleService.matchesApproverRole(approverId, node.getApproverRole(), recordOrgId)) return false;
            return isAssignedExpert(approverId, stage, targetId);
        }
        return roleService.matchesApproverRole(approverId, node.getApproverRole(), recordOrgId);
    }

    /** 该用户是否被明确指派为本记录的评审专家（按 srtp_expert.user_id → srtp_expert_assignment 关联） */
    private boolean isAssignedExpert(Long userId, SrtpStage stage, Long targetId) {
        SrtpExpert expert = expertMapper.selectOne(new QueryWrapper<SrtpExpert>().lambda()
                .eq(SrtpExpert::getUserId, userId)
                .last("LIMIT 1"));
        if (expert == null) return false;
        return assignmentMapper.selectCount(new QueryWrapper<SrtpExpertAssignment>().lambda()
                .eq(SrtpExpertAssignment::getExpertId, expert.getId())
                .eq(SrtpExpertAssignment::getStage, stage)
                .eq(SrtpExpertAssignment::getTargetId, targetId)) > 0;
    }

    /** 该用户是否为本记录的指导教师 */
    private boolean isAdvisorOf(Long userId, SrtpStage stage, Long targetId) {
        return advisorMapper.selectCount(new QueryWrapper<SrtpProjectAdvisor>().lambda()
                .eq(SrtpProjectAdvisor::getTargetType, stage)
                .eq(SrtpProjectAdvisor::getTargetId, targetId)
                .eq(SrtpProjectAdvisor::getUserId, userId)) > 0;
    }

    private void writeRecord(SrtpStage stage, Long targetId, SrtpApprovalFlowConfig node,
                             Long approverId, ApprovalAction action, String comment) {
        SrtpApprovalRecord r = new SrtpApprovalRecord();
        r.setStage(stage);
        r.setTargetId(targetId);
        r.setStepOrder(node.getStepOrder());
        r.setStepName(node.getStepName());
        r.setApproverId(approverId);
        r.setAction(action);
        r.setComment(comment);
        approvalRecordMapper.insert(r);
    }
}
