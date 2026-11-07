package com.zhk.contest.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Setter
@Getter
public class JudgeProjectResponse {
    private Long id;
    private String name;
    private String eventName;
    private String trackName;
    private String status;
    private LocalDateTime allocatedAt;

    // Populated when COMPLETED
    private BigDecimal totalScore;
    private String overallComment;

}
