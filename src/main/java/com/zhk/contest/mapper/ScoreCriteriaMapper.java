package com.zhk.contest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhk.contest.entity.ScoreCriteria;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ScoreCriteriaMapper extends BaseMapper<ScoreCriteria> {

    @Select("SELECT * FROM score_criteria WHERE track_id = #{trackId} ORDER BY sort_order")
    List<ScoreCriteria> findByTrackId(Long trackId);

    @Select("SELECT * FROM score_criteria WHERE track_id = #{trackId} ORDER BY sort_order")
    List<ScoreCriteria> selectByTrackId(Long trackId);
}
