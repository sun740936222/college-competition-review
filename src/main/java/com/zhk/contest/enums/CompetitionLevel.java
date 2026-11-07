package com.zhk.contest.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

public enum CompetitionLevel implements IEnum<String> {
    COLLEGE,
    SCHOOL,
    PROVINCE,
    NATIONAL;

    public OrganizationLevel toOrganizationLevel() {
        return OrganizationLevel.valueOf(this.name());
    }

    @Override
    public String getValue() {
        return this.name();
    }
}
