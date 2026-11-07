package com.zhk.contest.dto.request;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TrackSaveRequest {
    private String name;
    private String groupName;
    private String description;
    private Integer sortOrder;

}
