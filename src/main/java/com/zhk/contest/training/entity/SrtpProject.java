package com.zhk.contest.training.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.zhk.contest.training.enums.SrtpRecordStatus;
import com.zhk.contest.training.service.SrtpFlowRecord;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("srtp_project")
public class SrtpProject implements SrtpFlowRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("batch_id")
    private Long batchId;

    @TableField("topic_id")
    private Long topicId;

    @TableField("org_id")
    private Long orgId;

    @TableField("created_by")
    private Long createdBy;

    @TableField("project_type")
    private String projectType;

    @TableField("project_no")
    private String projectNo;

    private String name;

    @TableField("discipline_l1_id")
    private Long disciplineL1Id;

    @TableField("discipline_l2_id")
    private Long disciplineL2Id;

    @TableField("student_count")
    private Integer studentCount;

    private String description;

    private SrtpRecordStatus status = SrtpRecordStatus.DRAFT;

    @TableField("current_step")
    private Integer currentStep = 0;

    @TableField("reject_reason")
    private String rejectReason;

    /** JSON 文本，自定义字段值 */
    @TableField("extra_data")
    private String extraData;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
