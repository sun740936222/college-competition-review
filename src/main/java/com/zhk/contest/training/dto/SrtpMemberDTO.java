package com.zhk.contest.training.dto;

import com.zhk.contest.training.enums.SrtpMemberKind;
import lombok.Data;

/** 项目成员 DTO（可多个） */
@Data
public class SrtpMemberDTO {
    private Long userId;
    private String name;
    private String studentNo;
    private SrtpMemberKind memberRole;
    private Integer sortOrder;
}
