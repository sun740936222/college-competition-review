package com.zhk.contest.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhk.contest.dto.request.AllocateJudgesRequest;
import com.zhk.contest.dto.request.ScoreCriteriaSaveRequest;
import com.zhk.contest.dto.response.*;
import com.zhk.contest.entity.Project;
import com.zhk.contest.entity.ReviewAllocation;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ReviewService {

    // Score criteria
    List<ScoreCriteriaResponse> getCriteria(Long trackId);
    List<ScoreCriteriaResponse> saveCriteria(Long adminUserId, Long trackId, List<ScoreCriteriaSaveRequest> requests);
    void deleteCriteria(Long adminUserId, Long criteriaId);
    void uploadEventAttachment(Long adminUserId, Long eventId, MultipartFile file);
    ResponseEntity<Resource> downloadEventAttachment(Long eventId);
    void deleteEventAttachment(Long adminUserId, Long eventId);

    // Allocation
    void allocateJudges(Long adminUserId, AllocateJudgesRequest request);
    IPage<ReviewAllocationResponse> listAllocations(Long adminUserId, Long eventId, Long projectId, Long judgeId, Page<ReviewAllocation> page);
    void removeAllocation(Long adminUserId, Long allocationId);
    void batchAllocateByTrack(Long adminUserId, Long eventId, Long trackId, List<Long> judgeIds);

    // Progress
    ReviewProgressResponse getProgress(Long adminUserId, Long eventId);

    // Scores
    IPage<ScoreSummaryItemResponse> getScoreSummary(Long adminUserId, Long eventId, Long trackId, Page<Project> page);
    ProjectScoreDetailResponse getProjectScoreDetail(Long adminUserId, Long eventId, Long projectId);

    // Materials cleanup
    void clearEventMaterials(Long adminUserId, Long eventId);

    // Excel export
    byte[] exportScores(Long adminUserId, Long eventId);
    byte[] exportProjects(Long adminUserId, Long eventId);
    byte[] exportAllocations(Long adminUserId, Long eventId);
}
