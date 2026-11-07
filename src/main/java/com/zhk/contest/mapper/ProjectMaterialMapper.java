package com.zhk.contest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhk.contest.entity.ProjectMaterial;
import com.zhk.contest.enums.MaterialType;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ProjectMaterialMapper extends BaseMapper<ProjectMaterial> {

    @Select("SELECT * FROM project_material WHERE project_id = #{projectId}")
    List<ProjectMaterial> findByProjectId(Long projectId);

    @Select("SELECT * FROM project_material WHERE project_id = #{projectId} AND material_type = #{materialType}")
    Optional<ProjectMaterial> findByProjectIdAndMaterialType(@Param("projectId") Long projectId, @Param("materialType") MaterialType materialType);

    @Select("SELECT * FROM project_material WHERE stored_filename = #{storedFilename}")
    Optional<ProjectMaterial> findByStoredFilename(String storedFilename);

    @Delete("DELETE FROM project_material WHERE project_id = #{projectId}")
    void deleteByProjectId(Long projectId);
}