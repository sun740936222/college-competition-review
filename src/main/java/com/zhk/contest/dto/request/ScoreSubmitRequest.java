package com.zhk.contest.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Setter
@Getter
public class ScoreSubmitRequest {
    private String action; // "SAVE" or "SUBMIT"
    private String overallComment;
    private List<ScoreItem> scores;

    @Setter
    @Getter
    public static class ScoreItem {
        private Long criteriaId;
        private BigDecimal score;

    }
}
