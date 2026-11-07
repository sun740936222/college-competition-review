package com.zhk.contest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class BatchCreateCollegesRequest {
    @NotNull
    private Long parentOrgId;
    @NotEmpty
    private List<@NotBlank String> collegeNames;
    private boolean createAdminAccounts = true;

}
