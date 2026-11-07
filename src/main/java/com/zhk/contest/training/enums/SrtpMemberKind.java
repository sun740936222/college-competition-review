package com.zhk.contest.training.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

/** 团队成员类型（子表 srtp_project_member.member_role） */
public enum SrtpMemberKind implements IEnum<String> {
    LEADER, // 项目负责人
    MEMBER; // 普通成员

    @Override
    public String getValue() {
        return this.name();
    }
}
