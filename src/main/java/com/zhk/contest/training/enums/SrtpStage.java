package com.zhk.contest.training.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

/** 业务阶段 */
public enum SrtpStage implements IEnum<String> {
    TOPIC,       // 选题
    PROJECT,     // 立项
    MIDTERM,     // 中期
    CONCLUSION;  // 结题

    @Override
    public String getValue() {
        return this.name();
    }
}
