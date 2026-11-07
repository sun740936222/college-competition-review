package com.zhk.contest.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CreateJudgeRequest {
    @NotBlank
    private String username;
    @NotBlank
    private String password;
    @NotBlank(message = "评委姓名不能为空")
    private String realName;
    private String phone;
    @NotBlank
    private String judgeOrganization;

}