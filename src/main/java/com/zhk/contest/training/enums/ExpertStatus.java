package com.zhk.contest.training.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

/** 专家入库状态（自主申请需审核） */
public enum ExpertStatus implements IEnum<String> {
    PENDING,   // 待审核
    APPROVED,  // 已通过
    REJECTED;  // 已拒绝

    @Override
    public String getValue() {
        return this.name();
    }
}
