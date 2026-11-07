package com.zhk.contest.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class OrgSaveRequest {
    @NotBlank
    private String name;
    private String description;
    private Long parentId;
    /** 指定组织层级（仅在无上级单位时使用，值为 NATIONAL/PROVINCE/SCHOOL/COLLEGE） */
    private String level;

}