package com.zhk.contest.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class AllocateJudgesRequest {
    private List<AllocateItem> allocations;

    @Setter
    @Getter
    public static class AllocateItem {
        private Long projectId;
        private List<Long> judgeIds;

    }
}
