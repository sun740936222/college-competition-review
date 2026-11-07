package com.zhk.contest.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
public class ScoreSummaryItemResponse {
    private Long projectId;
    private String projectName;
    private String organizationName;
    private String trackName;
    private String eventName;
    private int judgeCount;
    private BigDecimal averageScore;
    private BigDecimal highestScore;
    private BigDecimal lowestScore;
    private String status;

}
