package com.zhk.contest.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

public enum ProjectStatus implements IEnum<String> {
    DRAFT,
    PENDING_REVIEW,
    APPROVED,
    REJECTED;

    @Override
    public String getValue() {
        return this.name();
    }
}