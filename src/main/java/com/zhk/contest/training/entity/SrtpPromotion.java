package com.zhk.contest.training.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("srtp_promotion")
public class SrtpPromotion {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("from_batch_id")
    private Long fromBatchId;

    @TableField("from_project_id")
    private Long fromProjectId;

    @TableField("to_batch_id")
    private Long toBatchId;

    @TableField("to_project_id")
    private Long toProjectId;

    @TableField("promoted_by")
    private Long promotedBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
