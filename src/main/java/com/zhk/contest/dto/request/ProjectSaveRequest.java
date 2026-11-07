package com.zhk.contest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;


@Setter
@Getter
public class ProjectSaveRequest {

    @NotNull(message = "赛事不能为空")
    private Long eventId;

    @NotNull(message = "赛道不能为空")
    private Long trackId;

    @NotBlank(message = "项目名称不能为空")
    private String name;

    private String description;

    private List<TeamMemberRequest> members;
    private List<AdvisorRequest> advisors;

}
