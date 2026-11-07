package com.zhk.contest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhk.contest.entity.Event;
import com.zhk.contest.enums.EventStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface EventMapper extends BaseMapper<Event> {

    @Select("SELECT * FROM event WHERE status = #{status}")
    List<Event> findByStatus(@Param("status") EventStatus status);
}