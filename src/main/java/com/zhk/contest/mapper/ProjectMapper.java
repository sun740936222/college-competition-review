package com.zhk.contest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhk.contest.entity.Project;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ProjectMapper extends BaseMapper<Project> {

    @Select("SELECT * FROM project WHERE user_id = #{userId} ORDER BY created_at DESC")
    IPage<Project> selectPageByUserId(Page<?> page, @Param("userId") Long userId);

    @Select("<script>" +
            "SELECT p.* FROM project p " +
            "JOIN user u ON p.user_id = u.id " +
            "WHERE p.event_id = #{eventId} " +
            "<if test='orgIds != null and orgIds.size() > 0'>" +
            "AND u.organization_id IN " +
            "<foreach collection='orgIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</if>" +
            "ORDER BY p.created_at DESC" +
            "</script>")
    List<Project> findByEventIdAndOrgIds(@Param("eventId") Long eventId, @Param("orgIds") List<Long> orgIds);
}