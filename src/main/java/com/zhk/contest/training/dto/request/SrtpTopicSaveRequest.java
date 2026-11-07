package com.zhk.contest.training.dto.request;

import com.zhk.contest.training.dto.SrtpAdvisorDTO;
import com.zhk.contest.training.dto.SrtpMemberDTO;
import com.zhk.contest.training.enums.TopicSource;
import lombok.Data;

import java.util.List;
import java.util.Map;

/** 选题保存请求（创建/编辑） */
@Data
public class SrtpTopicSaveRequest {
    private Long batchId;
    private TopicSource source;
    private String projectType;
    private String name;
    private Long disciplineL1Id;
    private Long disciplineL2Id;
    private String researchType;
    private String initiatorContact;
    /** 出题教师 user.id（管理员发布老师题目时指定） */
    private Long teacherId;
    private String info;
    private Map<String, Object> extraData;
    private List<SrtpMemberDTO> members;
    private List<SrtpAdvisorDTO> advisors;
}
