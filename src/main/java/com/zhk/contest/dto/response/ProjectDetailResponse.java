package com.zhk.contest.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Setter
@Getter
public class ProjectDetailResponse {
    private Long id;
    private Long eventId;
    private String eventName;
    private Long trackId;
    private String trackName;
    private String name;
    private String description;
    private String status;
    private String currentReviewLevel;
    private String rejectReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean requireVideo;
    private List<MemberResponse> teamMembers;
    private List<AdvisorResponse> advisors;
    private List<MaterialResponse> materials;

}
