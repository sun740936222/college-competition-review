package com.zhk.contest.training.dto.request;

import com.zhk.contest.training.dto.SrtpAdvisorDTO;
import com.zhk.contest.training.dto.SrtpMemberDTO;
import lombok.Data;

import java.util.List;
import java.util.Map;

/** 立项保存请求（创建/编辑） */
@Data
public class SrtpProjectSaveRequest {
    private Long batchId;
    private Long topicId;
    private String projectType;
    private String name;
    private Long disciplineL1Id;
    private Long disciplineL2Id;
    private Integer studentCount;
    private String description;
    private Map<String, Object> extraData;
    private List<SrtpMemberDTO> members;
    private List<SrtpAdvisorDTO> advisors;
}
