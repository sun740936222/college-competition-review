package com.zhk.contest.training.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.zhk.contest.training.enums.ApprovalAction;
import com.zhk.contest.training.enums.SrtpStage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("srtp_approval_record")
public class SrtpApprovalRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private SrtpStage stage;

    @TableField("target_id")
    private Long targetId;

    @TableField("step_order")
    private Integer stepOrder;

    @TableField("step_name")
    private String stepName;

    @TableField("approver_id")
    private Long approverId;

    private ApprovalAction action;

    private String comment;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
