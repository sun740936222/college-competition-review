package com.zhk.contest.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;



@Setter
@Getter
public class JudgeProjectDetailResponse {
    private Long id;
    private String name;
    private String description;
    private String eventName;
    private String trackName;
    private Long allocationId;
    private String allocationStatus;
    private LocalDateTime submittedAt;
    private String overallComment;
    private BigDecimal totalScore;
    private String judgeName;
    private String judgePhone;
    private Long eventId;
    private String eventAttachmentName;
    private Boolean allowResubmit;
    private Boolean requireComment;
    private java.time.LocalDateTime reviewStartTime;
    private java.time.LocalDateTime reviewEndTime;
    private List<ScoreCriteriaResponse> criteria;
    private List<ScoreRecordResponse> scores;
    private List<MaterialResponse> materials;

}
