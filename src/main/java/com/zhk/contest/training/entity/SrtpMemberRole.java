package com.zhk.contest.training.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.zhk.contest.training.enums.ExpertLevel;
import com.zhk.contest.training.enums.SrtpRoleType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("srtp_member_role")
public class SrtpMemberRole {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("role_type")
    private SrtpRoleType roleType;

    @TableField("org_id")
    private Long orgId;

    @TableField("expert_level")
    private ExpertLevel expertLevel;

    private String status = "ACTIVE";

    @TableField("must_change_password")
    private Boolean mustChangePassword = false;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
