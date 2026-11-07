package com.zhk.contest.dto.response;

import com.zhk.contest.entity.ScoreCriteria;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
public class ScoreCriteriaResponse {
    private Long id;
    private Long trackId;
    private String name;
    private String description;
    private Integer maxScore;
    private Integer sortOrder;
    private LocalDateTime createdAt;

    public static ScoreCriteriaResponse from(ScoreCriteria sc) {
        ScoreCriteriaResponse r = new ScoreCriteriaResponse();
        r.setId(sc.getId());
        r.setTrackId(sc.getTrackId());
        r.setName(sc.getName());
        r.setDescription(sc.getDescription());
        r.setMaxScore(sc.getMaxScore());
        r.setSortOrder(sc.getSortOrder());
        r.setCreatedAt(sc.getCreatedAt());
        return r;
    }

}
