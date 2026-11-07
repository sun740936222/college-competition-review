package com.zhk.contest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TeamMemberRequest {

    @NotBlank(message = "姓名不能为空")
    private String name;

    private String studentId;
    private String department;
    private String major;

    @NotBlank(message = "联系电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入有效的手机号码")
    private String phone;

    private String memberRole = "MEMBER";
    private Integer sortOrder = 0;

}
