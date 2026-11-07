package com.zhk.contest.training.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.zhk.contest.training.enums.SrtpStage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("srtp_expert_assignment")
public class SrtpExpertAssignment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private SrtpStage stage;

    @TableField("target_id")
    private Long targetId;

    @TableField("expert_id")
    private Long expertId;

    @TableField("assigned_by")
    private Long assignedBy;

    @TableField("review_status")
    private String reviewStatus = "PENDING"; // PENDING / APPROVED / REJECTED

    @TableField("review_comment")
    private String reviewComment;

    /** 专家打分 0-100（评审时必填） */
    private Integer score;

    @TableField("reviewed_at")
    private LocalDateTime reviewedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
