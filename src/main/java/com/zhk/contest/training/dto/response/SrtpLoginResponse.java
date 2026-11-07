package com.zhk.contest.training.dto.response;

import lombok.Data;

/** 教师/专家登录返回（含首次登录改密标记） */
@Data
public class SrtpLoginResponse {
    private Long userId;
    private String username;
    private String realName;
    private String role;
    private String phone;
    private String email;
    private Long organizationId;
    private String token;
    private Boolean mustChangePassword;
}
