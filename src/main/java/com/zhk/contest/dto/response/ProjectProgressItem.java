package com.zhk.contest.dto.response;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ProjectProgressItem {
    private Long projectId;
    private String projectName;
    private String trackName;
    private String groupName;
    private String orgName;
    private String projectStatus;
    private int totalJudges;
    private int completedJudges;
    private int completionPercent;
}
