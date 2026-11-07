package com.zhk.contest.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class AdvisorRequest {

    @NotBlank(message = "姓名不能为空")
    private String name;

    private String phone;

}
