package com.zhk.contest.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("score_record")
public class ScoreRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("allocation_id")
    private Long allocationId;

    @TableField("criteria_id")
    private Long criteriaId;

    private BigDecimal score;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
