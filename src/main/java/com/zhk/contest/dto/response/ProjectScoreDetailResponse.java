package com.zhk.contest.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Setter
@Getter
public class ProjectScoreDetailResponse {
    private Long projectId;
    private String projectName;
    private String trackName;
    private String eventName;
    private List<CriteriaInfo> criteria;
    private List<JudgeScoreItem> judgeScores;

    @Setter
    @Getter
    public static class CriteriaInfo {
        private Long id;
        private String name;
        private String description;
        private Integer maxScore;
        private Integer sortOrder;
    }

    @Setter
    @Getter
    public static class JudgeScoreItem {
        private Long judgeId;
        private String judgeName;
        private String overallComment;
        private BigDecimal totalScore;
        private LocalDateTime submittedAt;
        private List<ScoreItem> scores;
    }

    @Setter
    @Getter
    public static class ScoreItem {
        private Long criteriaId;
        private String criteriaName;
        private BigDecimal score;
        private Integer maxScore;
    }
}