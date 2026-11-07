package com.zhk.contest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhk.contest.entity.Track;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TrackMapper extends BaseMapper<Track> {

    @Select("SELECT * FROM track WHERE event_id = #{eventId} ORDER BY sort_order ASC")
    List<Track> findByEventIdOrderBySortOrder(Long eventId);
}