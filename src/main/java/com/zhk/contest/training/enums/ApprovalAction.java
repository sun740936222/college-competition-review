package com.zhk.contest.training.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

/** 审批动作 */
public enum ApprovalAction implements IEnum<String> {
    APPROVE, // 通过
    REJECT;  // 驳回(退回申报人)

    @Override
    public String getValue() {
        return this.name();
    }
}
