package com.zhk.contest.dto.response;

import com.zhk.contest.entity.ScoreRecord;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
public class ScoreRecordResponse {
    private Long id;
    private Long criteriaId;
    private String criteriaName;
    private BigDecimal score;
    private Integer maxScore;

    public static ScoreRecordResponse from(ScoreRecord sr) {
        ScoreRecordResponse r = new ScoreRecordResponse();
        r.setId(sr.getId());
        r.setCriteriaId(sr.getCriteriaId());
        r.setScore(sr.getScore());
        return r;
    }

}
