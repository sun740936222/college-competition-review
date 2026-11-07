package com.zhk.contest.training.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.zhk.contest.training.enums.SrtpStage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("srtp_project_advisor")
public class SrtpProjectAdvisor {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("target_type")
    private SrtpStage targetType;

    @TableField("target_id")
    private Long targetId;

    @TableField("user_id")
    private Long userId;

    private String name;

    private String title;

    private String phone;

    @TableField("research_project")
    private String researchProject;

    @TableField("support_desc")
    private String supportDesc;

    @TableField("sort_order")
    private Integer sortOrder = 0;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
