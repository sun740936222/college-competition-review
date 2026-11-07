package com.zhk.contest.training.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.zhk.contest.training.enums.SrtpRecordStatus;
import com.zhk.contest.training.service.SrtpReportRecord;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("srtp_conclusion_report")
public class SrtpConclusionReport implements SrtpReportRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("project_id")
    private Long projectId;

    @TableField("batch_id")
    private Long batchId;

    @TableField("created_by")
    private Long createdBy;

    @TableField("snapshot_name")
    private String snapshotName;

    @TableField("project_type")
    private String projectType;

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

    /** 结题等级(仅在"校专家结题评审"节点通过后由专家给出): PASS / GOOD / EXCELLENT */
    @TableField("conclusion_grade")
    private String conclusionGrade;

    /** JSON 文本，自定义字段值 */
    @TableField("extra_data")
    private String extraData;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
