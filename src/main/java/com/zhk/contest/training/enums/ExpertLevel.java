package com.zhk.contest.training.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

/** 专家级别 */
public enum ExpertLevel implements IEnum<String> {
    COLLEGE, // 院级
    SCHOOL;  // 校级

    @Override
    public String getValue() {
        return this.name();
    }
}
