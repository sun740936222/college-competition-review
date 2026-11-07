package com.zhk.contest.training.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

/** 批次状态（推进到哪个阶段） */
public enum SrtpBatchStatus implements IEnum<String> {
    TOPIC_OPEN,       // 选题中
    PROJECT_OPEN,     // 立项中
    MIDTERM_OPEN,     // 中期中
    CONCLUSION_OPEN,  // 结题中
    CLOSED;           // 已结束

    @Override
    public String getValue() {
        return this.name();
    }
}
