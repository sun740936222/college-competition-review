package com.zhk.contest.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class BatchReviewRequest {
    private List<Long> projectIds;
    private String action;
}