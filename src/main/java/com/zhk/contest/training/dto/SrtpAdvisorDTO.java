package com.zhk.contest.training.dto;

import lombok.Data;

/** 指导教师 DTO（可多个） */
@Data
public class SrtpAdvisorDTO {
    private Long userId;
    private String name;
    private String title;
    private String phone;
    private String researchProject;
    private String supportDesc;
    private Integer sortOrder;
}
