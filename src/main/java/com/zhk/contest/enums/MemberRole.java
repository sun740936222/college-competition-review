package com.zhk.contest.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

public enum MemberRole implements IEnum<String> {
    LEADER,
    MEMBER;

    @Override
    public String getValue() {
        return this.name();
    }
}