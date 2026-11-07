package com.zhk.contest.training.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

/** 审批节点角色（审批流配置中每个节点的审批人角色） */
public enum ApproverRole implements IEnum<String> {
    TEACHER,        // 指导教师
    COLLEGE_EXPERT, // 院级专家
    SCHOOL_EXPERT,  // 校级专家
    COLLEGE_ADMIN,  // 学院管理员
    SCHOOL_ADMIN;   // 学校管理员

    @Override
    public String getValue() {
        return this.name();
    }
}
