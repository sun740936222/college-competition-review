package com.zhk.contest.training.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.zhk.contest.training.enums.SrtpBatchStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("srtp_batch")
public class SrtpBatch {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String code;

    private Integer year;

    private String level;

    private SrtpBatchStatus status = SrtpBatchStatus.TOPIC_OPEN;

    @TableField("project_deadline")
    private LocalDateTime projectDeadline;

    @TableField("midterm_deadline")
    private LocalDateTime midtermDeadline;

    @TableField("conclusion_deadline")
    private LocalDateTime conclusionDeadline;

    @TableField("org_id")
    private Long orgId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
