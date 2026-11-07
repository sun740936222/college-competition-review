package com.zhk.contest.dto.response;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class MemberResponse {
    private Long id;
    private String name;
    private String studentId;
    private String department;
    private String major;
    private String phone;
    private String memberRole;
    private Integer sortOrder;

}
