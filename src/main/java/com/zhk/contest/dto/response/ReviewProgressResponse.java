package com.zhk.contest.dto.response;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ReviewProgressResponse {
    private long totalProjects;
    private long approvedProjects;
    private long allocatedProjects;
    private long completedReviews;
    private long totalJudges;
    private long activeJudges;
    private long fullyReviewedProjects;
    private long partialReviewedProjects;
    private long unallocatedProjects;
    private java.util.List<ProjectProgressItem> projects;
}
