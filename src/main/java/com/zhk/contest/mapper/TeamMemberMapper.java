package com.zhk.contest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhk.contest.entity.TeamMember;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TeamMemberMapper extends BaseMapper<TeamMember> {

    @Select("SELECT * FROM team_member WHERE project_id = #{projectId} ORDER BY sort_order ASC")
    List<TeamMember> findByProjectIdOrderBySortOrder(Long projectId);

    @Delete("DELETE FROM team_member WHERE project_id = #{projectId}")
    void deleteByProjectId(Long projectId);
}