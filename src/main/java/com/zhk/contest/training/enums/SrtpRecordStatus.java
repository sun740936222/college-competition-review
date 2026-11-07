package com.zhk.contest.training.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

/** 四阶段记录统一状态机 */
public enum SrtpRecordStatus implements IEnum<String> {
    DRAFT,        // 草稿
    SUBMITTED,    // 已提交(待进入审批)
    IN_REVIEW,    // 审批中
    APPROVED,     // 通过
    RETURNED,     // 已退回申报人
    REJECTED;     // 终态拒绝

    @Override
    public String getValue() {
        return this.name();
    }
}
