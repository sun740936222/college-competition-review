package com.zhk.contest.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CreateAdminAccountRequest {
    @NotBlank
    private String username;
    @NotBlank
    private String password;
    private String realName;
    private String phone;

}