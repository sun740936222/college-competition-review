package com.zhk.contest.training.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.zhk.contest.training.enums.ExpertLevel;
import com.zhk.contest.training.enums.ExpertSource;
import com.zhk.contest.training.enums.ExpertStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("srtp_expert")
public class SrtpExpert {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    private String name;

    private String phone;

    private String email;

    @TableField("org_id")
    private Long orgId;

    private String title;

    @TableField("research_field")
    private String researchField;

    @TableField("expert_level")
    private ExpertLevel expertLevel = ExpertLevel.COLLEGE;

    private ExpertSource source = ExpertSource.ADMIN_ADD;

    private ExpertStatus status = ExpertStatus.APPROVED;

    @TableField("apply_comment")
    private String applyComment;

    @TableField("reject_reason")
    private String rejectReason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
