package com.zhk.contest.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.zhk.contest.enums.OrganizationLevel;
import com.zhk.contest.enums.ProjectStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("project")
public class Project {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("event_id")
    private Long eventId;

    @TableField("track_id")
    private Long trackId;

    private String name;

    private String description;

    private ProjectStatus status = ProjectStatus.DRAFT;

    @TableField("current_review_level")
    private OrganizationLevel currentReviewLevel;

    @TableField("reject_reason")
    private String rejectReason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}