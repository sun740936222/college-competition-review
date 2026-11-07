package com.zhk.contest.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.zhk.contest.enums.MaterialType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("project_material")
public class ProjectMaterial {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("project_id")
    private Long projectId;

    @TableField("material_type")
    private MaterialType materialType;

    @TableField("original_filename")
    private String originalFilename;

    @TableField("stored_filename")
    private String storedFilename;

    @TableField("file_path")
    private String filePath;

    @TableField("file_size")
    private Long fileSize;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}