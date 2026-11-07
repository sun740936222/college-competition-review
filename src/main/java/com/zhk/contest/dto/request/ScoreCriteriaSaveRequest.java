package com.zhk.contest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ScoreCriteriaSaveRequest {
    @NotBlank
    private String name;
    private String description;
    @NotNull
    private Integer maxScore;
    private Integer sortOrder;

}
