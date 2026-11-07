package com.zhk.contest.training.controller;

import com.zhk.contest.dto.response.ApiResponse;
import com.zhk.contest.training.service.SrtpPromotionService;
import com.zhk.contest.training.service.SrtpPromotionService.PromoteResult;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 项目推荐/晋级（学校管理员）：校级→省级→国家级。 */
@RestController
@RequestMapping("/api/srtp/admin/promotion")
public class SrtpPromotionController {

    private final SrtpPromotionService promotionService;

    public SrtpPromotionController(SrtpPromotionService promotionService) {
        this.promotionService = promotionService;
    }

    private Long adminId(HttpServletRequest req) {
        return (Long) req.getAttribute("adminUserId");
    }

    @PostMapping
    public ApiResponse<PromoteResult> promote(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        Long toBatchId = body.get("toBatchId") == null ? null : Long.valueOf(body.get("toBatchId").toString());
        @SuppressWarnings("unchecked")
        List<Object> raw = (List<Object>) body.get("projectIds");
        List<Long> ids = raw == null ? List.of() : raw.stream().map(o -> Long.valueOf(o.toString())).toList();
        PromoteResult r = promotionService.promote(adminId(req), toBatchId, ids);
        return ApiResponse.success("推荐完成：成功 " + r.promoted() + "，跳过 " + r.skipped(), r);
    }

    /** 某源批次中已推荐过的项目ID（前端标记用） */
    @GetMapping("/promoted-ids")
    public ApiResponse<List<Long>> promotedIds(@RequestParam Long fromBatchId, HttpServletRequest req) {
        return ApiResponse.success(promotionService.promotedFromIds(adminId(req), fromBatchId));
    }
}
