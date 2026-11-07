package com.zhk.contest.training.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhk.contest.dto.response.ApiResponse;
import com.zhk.contest.training.service.SrtpTrackingService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 项目全流程追踪看板（管理员）。 */
@RestController
@RequestMapping("/api/srtp/admin/tracking")
public class SrtpTrackingController {

    private final SrtpTrackingService trackingService;

    public SrtpTrackingController(SrtpTrackingService trackingService) {
        this.trackingService = trackingService;
    }

    @GetMapping
    public ApiResponse<IPage<Map<String, Object>>> track(@RequestParam(required = false) Long batchId,
                                                         @RequestParam(required = false) String keyword,
                                                         @RequestParam(defaultValue = "1") int current,
                                                         @RequestParam(defaultValue = "10") int size,
                                                         HttpServletRequest req) {
        Long adminId = (Long) req.getAttribute("adminUserId");
        return ApiResponse.success(trackingService.track(adminId, new Page<>(current, size), batchId, keyword));
    }
}
