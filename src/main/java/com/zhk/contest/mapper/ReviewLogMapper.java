package com.zhk.contest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhk.contest.entity.ReviewLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ReviewLogMapper extends BaseMapper<ReviewLog> {

    @Select("SELECT * FROM review_log WHERE project_id = #{projectId} ORDER BY reviewed_at")
    List<ReviewLog> findByProjectId(Long projectId);
}
