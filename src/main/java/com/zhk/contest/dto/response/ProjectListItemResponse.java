package com.zhk.contest.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
public class ProjectListItemResponse {
    private Long id;
    private String eventName;
    private String trackName;
    private String name;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
