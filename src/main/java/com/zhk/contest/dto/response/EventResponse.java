package com.zhk.contest.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;


@Setter
@Getter
public class EventResponse {
    private Long id;
    private String name;
    private String description;
    private LocalDateTime registrationDeadline;
    private LocalDateTime submissionDeadline;
    private LocalDateTime reviewStartTime;
    private LocalDateTime reviewEndTime;
    private Boolean requireVideo;
    private Boolean allowResubmit;
    private Boolean requireComment;
    private String attachmentName;
    private String competitionLevel;
    private String status;
    private Long parentEventId;
    private String parentEventName;
    private Integer childEventCount;
    private List<TrackResponse> tracks;
}
