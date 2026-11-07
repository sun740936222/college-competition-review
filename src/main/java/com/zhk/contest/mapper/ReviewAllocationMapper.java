package com.zhk.contest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhk.contest.entity.ReviewAllocation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ReviewAllocationMapper extends BaseMapper<ReviewAllocation> {

    @Select("SELECT * FROM review_allocation WHERE judge_id = #{judgeId} ORDER BY allocated_at DESC")
    List<ReviewAllocation> findByJudgeId(Long judgeId);

    @Select("SELECT * FROM review_allocation WHERE judge_id = #{judgeId} ORDER BY allocated_at DESC")
    IPage<ReviewAllocation> findByJudgeIdPaged(Page<ReviewAllocation> page, Long judgeId);

    @Select("SELECT * FROM review_allocation WHERE project_id = #{projectId}")
    List<ReviewAllocation> findByProjectId(Long projectId);

    @Select("SELECT * FROM review_allocation WHERE judge_id = #{judgeId} AND status = #{status} ORDER BY allocated_at DESC")
    List<ReviewAllocation> findByJudgeIdAndStatus(@Param("judgeId") Long judgeId, @Param("status") String status);

    @Select("SELECT * FROM review_allocation WHERE judge_id = #{judgeId} AND status = #{status} ORDER BY allocated_at DESC")
    IPage<ReviewAllocation> findByJudgeIdAndStatusPaged(Page<ReviewAllocation> page, @Param("judgeId") Long judgeId, @Param("status") String status);

    @Select("SELECT COUNT(*) FROM review_allocation WHERE judge_id = #{judgeId} AND status = #{status}")
    long countByJudgeIdAndStatus(@Param("judgeId") Long judgeId, @Param("status") String status);

    @Select("SELECT COUNT(*) FROM review_allocation WHERE project_id = #{projectId} AND status = #{status}")
    long countByProjectIdAndStatus(@Param("projectId") Long projectId, @Param("status") String status);

    @Select("SELECT ra.* FROM review_allocation ra " +
            "JOIN project p ON ra.project_id = p.id " +
            "WHERE p.event_id = #{eventId}")
    List<ReviewAllocation> findByEventId(Long eventId);
}
