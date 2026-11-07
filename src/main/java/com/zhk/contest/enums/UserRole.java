package com.zhk.contest.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

public enum UserRole implements IEnum<String> {
    ADMIN,
    SUPER_ADMIN,
    CONTESTANT,
    JUDGE;

    @Override
    public String getValue() {
        return this.name();
    }
}