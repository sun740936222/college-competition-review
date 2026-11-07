package com.zhk.contest.training.dto.request;

import com.zhk.contest.training.dto.SrtpAdvisorDTO;
import com.zhk.contest.training.dto.SrtpMemberDTO;
import lombok.Data;

import java.util.List;
import java.util.Map;

/** 中期/结题报告保存请求 */
@Data
public class SrtpReportSaveRequest {
    private Long projectId;
    private String projectType;
    private Integer studentCount;
    private String description;
    private Map<String, Object> extraData;
    private List<SrtpMemberDTO> members;
    private List<SrtpAdvisorDTO> advisors;
}
