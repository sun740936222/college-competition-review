package com.zhk.contest.training.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

/** 选题来源（双通道） */
public enum TopicSource implements IEnum<String> {
    STUDENT_PROPOSED,  // 学生自拟
    SCHOOL_ASSIGNED;   // 学校给定(待认领)

    @Override
    public String getValue() {
        return this.name();
    }
}
