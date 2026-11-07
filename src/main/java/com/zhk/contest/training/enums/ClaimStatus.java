package com.zhk.contest.training.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

/** 学校给定选题的认领状态 */
public enum ClaimStatus implements IEnum<String> {
    OPEN,     // 可认领
    CLAIMED;  // 已认领

    @Override
    public String getValue() {
        return this.name();
    }
}
