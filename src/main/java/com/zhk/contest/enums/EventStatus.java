package com.zhk.contest.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

public enum EventStatus implements IEnum<String> {
    DRAFT,
    OPEN,
    CLOSED;

    @Override
    public String getValue() {
        return this.name();
    }
}