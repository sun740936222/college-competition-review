package com.zhk.contest.dto.response;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class LoginResponse {
    private Long userId;
    private String username;
    private String realName;
    private String role;
    private String phone;
    private String email;
    private Long organizationId;
    private String organizationName;
    private String universityName;
    private String orgLevel;
    private String token;
    private String createdAt;

}
