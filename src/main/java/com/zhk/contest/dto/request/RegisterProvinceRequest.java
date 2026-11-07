package com.zhk.contest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterProvinceRequest {
    @NotBlank(message = "请输入上级邀请码")
    private String parentInviteCode;

    @NotBlank(message = "请输入省级组委会名称")
    @Size(min = 2, max = 100)
    private String provinceName;

    private String description;

    @NotBlank(message = "请输入用户名")
    @Size(min = 3, max = 50)
    private String username;

    @NotBlank(message = "请输入密码")
    @Size(min = 6, max = 100)
    private String password;

    @NotBlank(message = "请输入姓名")
    private String realName;

    private String phone;
    private String email;
}
