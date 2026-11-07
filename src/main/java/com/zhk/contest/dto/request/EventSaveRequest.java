package com.zhk.contest.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Setter
@Getter
public class EventSaveRequest {
    @NotBlank
    private String name;
    private String description;
    private LocalDateTime registrationDeadline;
    private LocalDateTime submissionDeadline;
    private LocalDateTime reviewStartTime;
    private LocalDateTime reviewEndTime;
    private Boolean requireVideo;
    private String status;
    private Boolean allowResubmit;
    private Boolean requireComment;
    private String competitionLevel;
    private Long parentEventId;
    private Long organizationId;
    private List<Long> childEventOrgIds;
    private List<TrackSaveRequest> tracks;

}