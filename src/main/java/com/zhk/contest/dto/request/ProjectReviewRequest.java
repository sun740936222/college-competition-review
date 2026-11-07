package com.zhk.contest.dto.request;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ProjectReviewRequest {
    private String action; // APPROVE or REJECT
    private String rejectReason;

}