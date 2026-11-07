package com.zhk.contest.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhk.contest.dto.request.ScoreSubmitRequest;
import com.zhk.contest.dto.response.JudgeProjectDetailResponse;
import com.zhk.contest.dto.response.JudgeProjectResponse;
import com.zhk.contest.dto.response.ScoreCriteriaResponse;
import com.zhk.contest.entity.ReviewAllocation;

import java.util.List;

public interface JudgeService {
    IPage<JudgeProjectResponse> listMyProjects(Long judgeId, String status, Page<ReviewAllocation> page);
    JudgeProjectDetailResponse getProjectDetail(Long judgeId, Long projectId);
    List<ScoreCriteriaResponse> getProjectCriteria(Long judgeId, Long projectId);
    void submitScores(Long judgeId, Long projectId, ScoreSubmitRequest request);
}
