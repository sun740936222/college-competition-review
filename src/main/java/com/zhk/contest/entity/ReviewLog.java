package com.zhk.contest.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.zhk.contest.enums.OrganizationLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("review_log")
public class ReviewLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("project_id")
    private Long projectId;

    @TableField("reviewer_id")
    private Long reviewerId;

    @TableField("review_level")
    private OrganizationLevel reviewLevel;

    private String action;

    @TableField("reject_reason")
    private String rejectReason;

    @TableField("reviewed_at")
    private LocalDateTime reviewedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
