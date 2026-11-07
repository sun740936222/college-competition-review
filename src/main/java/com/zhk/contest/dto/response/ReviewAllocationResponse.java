package com.zhk.contest.dto.response;

import com.zhk.contest.entity.ReviewAllocation;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Setter
@Getter
public class ReviewAllocationResponse {
    private Long id;
    private Long projectId;
    private String projectName;
    private String eventName;
    private String trackName;
    private Long judgeId;
    private String judgeName;
    private String status;
    private String overallComment;
    private BigDecimal totalScore;
    private LocalDateTime submittedAt;
    private LocalDateTime allocatedAt;

    public static ReviewAllocationResponse from(ReviewAllocation ra) {
        ReviewAllocationResponse r = new ReviewAllocationResponse();
        r.setId(ra.getId());
        r.setProjectId(ra.getProjectId());
        r.setJudgeId(ra.getJudgeId());
        r.setStatus(ra.getStatus());
        r.setOverallComment(ra.getOverallComment());
        r.setTotalScore(ra.getTotalScore());
        r.setSubmittedAt(ra.getSubmittedAt());
        r.setAllocatedAt(ra.getAllocatedAt());
        return r;
    }

}
