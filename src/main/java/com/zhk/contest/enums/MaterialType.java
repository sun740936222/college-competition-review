package com.zhk.contest.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

public enum MaterialType implements IEnum<String> {
    PITCH_PDF,
    PROJECT_PLAN_PDF,
    SUPPORTING_PDF,
    VIDEO_MP4;

    @Override
    public String getValue() {
        return this.name();
    }
}