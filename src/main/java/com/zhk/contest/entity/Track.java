package com.zhk.contest.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("track")
public class Track {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("event_id")
    private Long eventId;

    private String name;

    @TableField("group_name")
    private String groupName;

    private String description;

    @TableField("sort_order")
    private Integer sortOrder = 0;
}