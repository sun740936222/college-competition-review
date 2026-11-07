package com.zhk.contest.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("review_allocation")
public class ReviewAllocation {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("project_id")
    private Long projectId;

    @TableField("judge_id")
    private Long judgeId;

    private String status = "PENDING";

    @TableField("overall_comment")
    private String overallComment;

    @TableField("total_score")
    private BigDecimal totalScore;

    @TableField("submitted_at")
    private LocalDateTime submittedAt;

    @TableField("allocated_at")
    private LocalDateTime allocatedAt;
}
