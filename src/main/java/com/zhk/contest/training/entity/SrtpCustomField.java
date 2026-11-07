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
@TableName("srtp_custom_field")
public class SrtpCustomField {

    @TableId(type = IdType.AUTO)
    private Long id;

    private SrtpStage stage;

    @TableField("batch_id")
    private Long batchId;

    @TableField("field_key")
    private String fieldKey;

    private String label;

    @TableField("field_type")
    private String fieldType = "TEXT";

    /** JSON 文本（SELECT/MULTISELECT 的选项） */
    private String options;

    private Boolean required = false;

    @TableField("sort_order")
    private Integer sortOrder = 0;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
