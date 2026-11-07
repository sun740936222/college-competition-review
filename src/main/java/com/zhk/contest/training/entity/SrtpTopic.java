package com.zhk.contest.training.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.zhk.contest.training.enums.ClaimStatus;
import com.zhk.contest.training.enums.SrtpRecordStatus;
import com.zhk.contest.training.enums.TopicSource;
import com.zhk.contest.training.service.SrtpFlowRecord;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("srtp_topic")
public class SrtpTopic implements SrtpFlowRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("batch_id")
    private Long batchId;

    @TableField("org_id")
    private Long orgId;

    private TopicSource source = TopicSource.STUDENT_PROPOSED;

    @TableField("project_type")
    private String projectType;

    private String name;

    @TableField("discipline_l1_id")
    private Long disciplineL1Id;

    @TableField("discipline_l2_id")
    private Long disciplineL2Id;

    @TableField("research_type")
    private String researchType;

    @TableField("initiator_id")
    private Long initiatorId;

    /** 出题教师 user.id（老师出题时绑定；学生选此题则指导教师=该老师） */
    @TableField("teacher_id")
    private Long teacherId;

    @TableField("initiator_contact")
    private String initiatorContact;

    @TableField("claimed_by")
    private Long claimedBy;

    @TableField("claim_status")
    private ClaimStatus claimStatus;

    private String info;

    private SrtpRecordStatus status = SrtpRecordStatus.DRAFT;

    @TableField("current_step")
    private Integer currentStep = 0;

    @TableField("reject_reason")
    private String rejectReason;

    /** JSON 文本，自定义字段值 */
    @TableField("extra_data")
    private String extraData;

    /** 出题教师姓名（非持久化，查询时填充展示） */
    @TableField(exist = false)
    private String teacherName;

    /** 认领学生姓名（非持久化，查询时填充展示） */
    @TableField(exist = false)
    private String claimedByName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
