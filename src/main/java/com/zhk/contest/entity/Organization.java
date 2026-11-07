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
@TableName("organization")
public class Organization {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    @TableField("invite_code")
    private String inviteCode;

    private String description;

    @TableField("parent_id")
    private Long parentId;

    private OrganizationLevel level = OrganizationLevel.SCHOOL;

    @TableField("is_external")
    private Boolean isExternal;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
