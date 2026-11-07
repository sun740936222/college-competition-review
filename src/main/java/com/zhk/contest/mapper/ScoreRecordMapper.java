package com.zhk.contest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhk.contest.entity.ScoreRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ScoreRecordMapper extends BaseMapper<ScoreRecord> {

    @Select("SELECT * FROM score_record WHERE allocation_id = #{allocationId} ORDER BY criteria_id")
    List<ScoreRecord> findByAllocationId(Long allocationId);

    @Select("SELECT sr.* FROM score_record sr " +
            "JOIN review_allocation ra ON sr.allocation_id = ra.id " +
            "WHERE ra.project_id = #{projectId}")
    List<ScoreRecord> findByProjectId(Long projectId);
}
