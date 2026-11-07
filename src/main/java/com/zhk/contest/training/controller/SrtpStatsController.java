package com.zhk.contest.training.controller;

import com.zhk.contest.dto.response.ApiResponse;
import com.zhk.contest.training.enums.SrtpStage;
import com.zhk.contest.training.service.SrtpStatsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 大创统计（管理员）：按项目类别统计。 */
@RestController
@RequestMapping("/api/srtp/admin/stats")
public class SrtpStatsController {

    private final SrtpStatsService statsService;

    public SrtpStatsController(SrtpStatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/by-category")
    public ApiResponse<List<Map<String, Object>>> byCategory(@RequestParam(defaultValue = "PROJECT") SrtpStage stage,
                                                            @RequestParam(required = false) Long batchId,
                                                            HttpServletRequest req) {
        Long adminId = (Long) req.getAttribute("adminUserId");
        return ApiResponse.success(statsService.byCategory(adminId, stage, batchId));
    }
}
