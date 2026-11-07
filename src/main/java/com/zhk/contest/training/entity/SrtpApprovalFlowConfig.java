package com.zhk.contest.training.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.zhk.contest.training.enums.ApproverRole;
import com.zhk.contest.training.enums.SrtpStage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("srtp_approval_flow_config")
public class SrtpApprovalFlowConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** NULL=全局默认模板；否则该批次副本 */
    @TableField("batch_id")
    private Long batchId;

    private SrtpStage stage;

    @TableField("step_order")
    private Integer stepOrder;

    @TableField("step_name")
    private String stepName;

    @TableField("approver_role")
    private ApproverRole approverRole;

    @TableField("need_assign_expert")
    private Boolean needAssignExpert = false;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
