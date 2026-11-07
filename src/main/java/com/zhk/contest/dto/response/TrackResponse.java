package com.zhk.contest.dto.response;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TrackResponse {
    private Long id;
    private String name;
    private String groupName;
    private String description;
}
