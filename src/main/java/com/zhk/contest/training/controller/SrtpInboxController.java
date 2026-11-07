package com.zhk.contest.training.controller;

import com.zhk.contest.dto.response.ApiResponse;
import com.zhk.contest.training.enums.SrtpStage;
import com.zhk.contest.training.service.SrtpInboxService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 审批工作台：待我审批列表 + 我的大创身份。审批动作复用各阶段既有 approve/reject 接口。 */
@RestController
@RequestMapping("/api/srtp")
public class SrtpInboxController {

    private final SrtpInboxService inboxService;

    public SrtpInboxController(SrtpInboxService inboxService) {
        this.inboxService = inboxService;
    }

    private Long userId(HttpServletRequest req) {
        return (Long) req.getAttribute("userId");
    }

    @GetMapping("/my-identity")
    public ApiResponse<Map<String, Object>> myIdentity(HttpServletRequest req) {
        return ApiResponse.success(inboxService.myIdentity(userId(req)));
    }

    @GetMapping("/my-approvals")
    public ApiResponse<List<Map<String, Object>>> myApprovals(@RequestParam(required = false) SrtpStage stage,
                                                              HttpServletRequest req) {
        return ApiResponse.success(inboxService.myPending(userId(req), stage));
    }
}
