package com.zhk.contest.training.dto.request;

import com.zhk.contest.training.enums.ExpertLevel;
import lombok.Data;

/** 专家保存请求（管理员添加/编辑，或教师自主申请复用部分字段） */
@Data
public class SrtpExpertSaveRequest {
    private Long userId;
    private String name;
    private String phone;
    private String email;
    private Long orgId;
    private String title;
    private String researchField;
    private ExpertLevel expertLevel;
    private String applyComment;
    /** 可选：为专家创建登录账号(留空则不建登录账号)。用户名全库唯一 */
    private String username;
    private String password;
}
