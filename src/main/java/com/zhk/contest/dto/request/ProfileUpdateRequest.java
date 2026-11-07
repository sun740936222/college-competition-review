package com.zhk.contest.dto.request;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ProfileUpdateRequest {

    private Long organizationId;
    private String organizationName;

}
