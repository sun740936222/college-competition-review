package com.zhk.contest.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

public enum OrganizationLevel implements IEnum<String> {
    COLLEGE,
    SCHOOL,
    PROVINCE,
    NATIONAL;

    public boolean isAtLeast(OrganizationLevel other) {
        return this.ordinal() >= other.ordinal();
    }

    public OrganizationLevel nextUp() {
        int next = this.ordinal() + 1;
        if (next >= values().length) return null;
        return values()[next];
    }

    public OrganizationLevel nextDown() {
        int prev = this.ordinal() - 1;
        if (prev < 0) return null;
        return values()[prev];
    }

    @Override
    public String getValue() {
        return this.name();
    }
}
