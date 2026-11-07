package com.zhk.contest.training.service;

import com.zhk.contest.training.enums.SrtpRecordStatus;

/**
 * 四阶段业务记录（选题/立项/中期/结题）统一契约，供 {@link SrtpApprovalEngine} 通用驱动状态机。
 * 四个实体均已具备这些字段（Lombok @Data 生成对应方法），只需 implements 即可。
 */
public interface SrtpFlowRecord {

    Long getId();

    Long getBatchId();

    SrtpRecordStatus getStatus();

    void setStatus(SrtpRecordStatus status);

    Integer getCurrentStep();

    void setCurrentStep(Integer currentStep);

    String getRejectReason();

    void setRejectReason(String rejectReason);
}
