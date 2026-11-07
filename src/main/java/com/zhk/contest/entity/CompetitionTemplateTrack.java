package com.zhk.contest.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("competition_template_track")
public class CompetitionTemplateTrack {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("template_id")
    private Long templateId;

    private String name;

    @TableField("group_name")
    private String groupName;

    private String description;

    @TableField("sort_order")
    private Integer sortOrder = 0;
}
