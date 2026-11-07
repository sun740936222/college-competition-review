package com.zhk.contest.training.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

/** 专家来源 */
public enum ExpertSource implements IEnum<String> {
    ADMIN_ADD,   // 管理员添加
    SELF_APPLY,  // 教师自主申请
    IMPORT;      // 批量导入

    @Override
    public String getValue() {
        return this.name();
    }
}
