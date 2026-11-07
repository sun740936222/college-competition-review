package com.zhk.contest.training.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.zhk.contest.training.enums.SrtpMemberKind;
import com.zhk.contest.training.enums.SrtpStage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("srtp_project_member")
public class SrtpProjectMember {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("target_type")
    private SrtpStage targetType;

    @TableField("target_id")
    private Long targetId;

    @TableField("user_id")
    private Long userId;

    private String name;

    @TableField("student_no")
    private String studentNo;

    @TableField("member_role")
    private SrtpMemberKind memberRole = SrtpMemberKind.MEMBER;

    @TableField("sort_order")
    private Integer sortOrder = 0;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
