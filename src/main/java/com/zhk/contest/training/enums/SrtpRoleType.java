package com.zhk.contest.training.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

/** SRTP 模块身份类型（存于 srtp_member_role，一人多角） */
public enum SrtpRoleType implements IEnum<String> {
    STUDENT,       // 学生
    TEACHER,       // 指导教师
    EXPERT,        // 专家
    COLLEGE_ADMIN, // 学院管理员
    SCHOOL_ADMIN;  // 学校管理员

    @Override
    public String getValue() {
        return this.name();
    }
}
