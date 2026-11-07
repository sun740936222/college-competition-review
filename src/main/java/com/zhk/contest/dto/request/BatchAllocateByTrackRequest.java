package com.zhk.contest.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class BatchAllocateByTrackRequest {
    private Long eventId;
    private Long trackId;
    private List<Long> judgeIds;
}
