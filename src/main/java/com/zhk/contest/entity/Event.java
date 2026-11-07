package com.zhk.contest.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.zhk.contest.enums.CompetitionLevel;
import com.zhk.contest.enums.EventStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("event")
public class Event {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String description;

    @TableField("registration_deadline")
    private LocalDateTime registrationDeadline;

    @TableField("submission_deadline")
    private LocalDateTime submissionDeadline;

    @TableField("review_start_time")
    private LocalDateTime reviewStartTime;

    @TableField("review_end_time")
    private LocalDateTime reviewEndTime;

    private EventStatus status;

    @TableField("competition_level")
    private CompetitionLevel competitionLevel = CompetitionLevel.SCHOOL;

    @TableField("organization_id")
    private Long organizationId;

    @TableField("parent_event_id")
    private Long parentEventId;

    @TableField("require_video")
    private Boolean requireVideo = false;

    @TableField("allow_resubmit")
    private Boolean allowResubmit = false;

    @TableField("require_comment")
    private Boolean requireComment = false;

    @TableField("attachment_name")
    private String attachmentName;

    @TableField("attachment_path")
    private String attachmentPath;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
