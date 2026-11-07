package com.zhk.contest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhk.contest.entity.Advisor;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AdvisorMapper extends BaseMapper<Advisor> {

    @Select("SELECT * FROM advisor WHERE project_id = #{projectId}")
    List<Advisor> findByProjectId(Long projectId);

    @Delete("DELETE FROM advisor WHERE project_id = #{projectId}")
    void deleteByProjectId(Long projectId);
}