package com.zhk.contest.training.controller;

import com.zhk.contest.dto.response.ApiResponse;
import com.zhk.contest.exception.BusinessException;
import com.zhk.contest.training.service.SrtpExpertReviewService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 校/院级专家的顾问性评审入口（不阻塞主审批流；管理员通过"分配校专"下发后由专家在此提交意见） */
@RestController
@RequestMapping("/api/srtp/expert")
public class SrtpExpertReviewController {

    private final SrtpExpertReviewService reviewService;

    public SrtpExpertReviewController(SrtpExpertReviewService reviewService) {
        this.reviewService = reviewService;
    }

    private Long userId(HttpServletRequest req) {
        Object v = req.getAttribute("userId");
        if (v == null) throw new BusinessException(401, "请先登录");
        return (Long) v;
    }

    /** 当前登录专家收到的评审分配（含已完成） */
    @GetMapping("/my-assignments")
    public ApiResponse<List<Map<String, Object>>> myAssignments(HttpServletRequest req) {
        return ApiResponse.success(reviewService.myAssignments(userId(req)));
    }

    /** 提交评审：action=APPROVED / REJECTED，score 0-100，comment 为评审意见（不同意时必填） */
    @PostMapping("/assignments/{id}/review")
    public ApiResponse<Void> submitReview(@PathVariable Long id, @RequestBody ReviewRequest req,
                                          HttpServletRequest servletReq) {
        reviewService.submitReview(userId(servletReq), id, req.getAction(), req.getScore(), req.getComment());
        return ApiResponse.success("评审已提交", null);
    }

    public static class ReviewRequest {
        private String action;
        private Integer score;
        private String comment;
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public Integer getScore() { return score; }
        public void setScore(Integer score) { this.score = score; }
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
    }
}
