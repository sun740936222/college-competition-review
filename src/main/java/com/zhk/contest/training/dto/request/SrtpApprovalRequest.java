package com.zhk.contest.training.dto.request;

import lombok.Data;

/** 审批动作请求（通过/驳回） */
@Data
public class SrtpApprovalRequest {
    private String comment;
    /** 驳回专用：退回目标节点 step_order；空或 &lt;=0 表示退回申报人 */
    private Integer targetStep;
    /** 结题"校专家结题评审"节点通过时必填：PASS / GOOD / EXCELLENT */
    private String grade;
}
