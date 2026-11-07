package com.zhk.contest.training.dto.request;

import com.zhk.contest.training.enums.SrtpBatchStatus;
import lombok.Data;

import java.time.LocalDateTime;

/** 批次保存请求 */
@Data
public class SrtpBatchSaveRequest {
    private String name;
    private String code;
    private Integer year;
    private String level;
    private SrtpBatchStatus status;
    private LocalDateTime projectDeadline;
    private LocalDateTime midtermDeadline;
    private LocalDateTime conclusionDeadline;
    private Long orgId;
}
