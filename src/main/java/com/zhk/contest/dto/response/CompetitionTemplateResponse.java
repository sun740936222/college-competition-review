package com.zhk.contest.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CompetitionTemplateResponse {
    private Long id;
    private String name;
    private String description;
    private List<TrackResponse> tracks;

}
