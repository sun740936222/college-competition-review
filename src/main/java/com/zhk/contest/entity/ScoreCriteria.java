package com.zhk.contest.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("score_criteria")
public class ScoreCriteria {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("track_id")
    private Long trackId;

    private String name;

    private String description;

    @TableField("max_score")
    private Integer maxScore = 100;

    @TableField("sort_order")
    private Integer sortOrder = 0;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
