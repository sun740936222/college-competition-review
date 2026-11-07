package com.zhk.contest.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
public class ProjectAdminListItemResponse {
    private Long id;
    private String name;
    private String status;
    private String currentReviewLevel;
    private String competitionLevel;
    private String eventName;
    private Long eventId;
    private String trackName;
    private Long trackId;
    private String creatorName;
    private String organizationName;
    private Integer memberCount;
    private Integer materialCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}