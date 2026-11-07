package com.zhk.contest.training.service;

/**
 * 中期/结题报告统一契约（在 {@link SrtpFlowRecord} 基础上补充报告字段），
 * 供 {@link SrtpReportService} 以同一套逻辑驱动两个阶段。
 * SrtpMidtermReport / SrtpConclusionReport 字段一致，Lombok 已生成对应方法，只需 implements。
 */
public interface SrtpReportRecord extends SrtpFlowRecord {

    Long getProjectId();
    void setProjectId(Long projectId);

    void setBatchId(Long batchId);

    Long getCreatedBy();
    void setCreatedBy(Long createdBy);

    String getSnapshotName();
    void setSnapshotName(String snapshotName);

    String getProjectType();
    void setProjectType(String projectType);

    Long getDisciplineL1Id();
    void setDisciplineL1Id(Long disciplineL1Id);

    Long getDisciplineL2Id();
    void setDisciplineL2Id(Long disciplineL2Id);

    Integer getStudentCount();
    void setStudentCount(Integer studentCount);

    String getDescription();
    void setDescription(String description);

    String getExtraData();
    void setExtraData(String extraData);
}
