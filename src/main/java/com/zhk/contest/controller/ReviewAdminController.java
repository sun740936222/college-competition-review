package com.zhk.contest.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhk.contest.config.TokenService;
import com.zhk.contest.dto.request.AllocateJudgesRequest;
import com.zhk.contest.dto.request.BatchAllocateByTrackRequest;
import com.zhk.contest.dto.request.ScoreCriteriaSaveRequest;
import com.zhk.contest.dto.response.*;
import com.zhk.contest.entity.Project;
import com.zhk.contest.service.FileStorageService;
import com.zhk.contest.service.ReviewService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/reviews/admin")
public class ReviewAdminController {

    private final ReviewService reviewService;
    private final FileStorageService fileStorageService;
    private final TokenService tokenService;

    public ReviewAdminController(ReviewService reviewService, FileStorageService fileStorageService, TokenService tokenService) {
        this.reviewService = reviewService;
        this.fileStorageService = fileStorageService;
        this.tokenService = tokenService;
    }

    private Long getAdminUserId(HttpServletRequest req) {
        return (Long) req.getAttribute("adminUserId");
    }

    /**
     * Resolve admin user ID with fallbacks for paths excluded from LoginInterceptor.
     * 1. req attribute "adminUserId" (set by LoginInterceptor for non-excluded paths)
     * 2. ?token= query param (for iframe/video/download scenarios)
     * 3. Authorization header directly (for excluded paths where frontend still sends Bearer token)
     */
    private Long resolveAdminUserId(HttpServletRequest req, String token) {
        Long adminUserId = getAdminUserId(req);
        if (adminUserId == null && token != null && !token.isBlank()) {
            var info = tokenService.getTokenInfo(token);
            if (info != null) adminUserId = info.adminUserId();
        }
        if (adminUserId == null) {
            String authHeader = req.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                var info = tokenService.getTokenInfo(authHeader.substring(7));
                if (info != null) adminUserId = info.adminUserId();
            }
        }
        return adminUserId;
    }

    // ========== Score Criteria ==========

    @GetMapping("/tracks/{trackId}/criteria")
    public ApiResponse<List<ScoreCriteriaResponse>> getCriteria(@PathVariable Long trackId) {
        return ApiResponse.success(reviewService.getCriteria(trackId));
    }

    @PutMapping("/tracks/{trackId}/criteria")
    public ApiResponse<List<ScoreCriteriaResponse>> saveCriteria(
            @PathVariable Long trackId,
            @RequestBody List<@Valid ScoreCriteriaSaveRequest> requests,
            HttpServletRequest req) {
        return ApiResponse.success(reviewService.saveCriteria(getAdminUserId(req), trackId, requests));
    }

    @DeleteMapping("/criteria/{criteriaId}")
    public ApiResponse<Void> deleteCriteria(@PathVariable Long criteriaId, HttpServletRequest req) {
        reviewService.deleteCriteria(getAdminUserId(req), criteriaId);
        return ApiResponse.success(null);
    }

    // ========== Allocation ==========

    @PostMapping("/allocate")
    public ApiResponse<Void> allocateJudges(@RequestBody AllocateJudgesRequest request, HttpServletRequest req) {
        reviewService.allocateJudges(getAdminUserId(req), request);
        return ApiResponse.success(null);
    }

    @PostMapping("/allocate/by-track")
    public ApiResponse<Void> batchAllocateByTrack(@RequestBody BatchAllocateByTrackRequest request, HttpServletRequest req) {
        reviewService.batchAllocateByTrack(getAdminUserId(req), request.getEventId(), request.getTrackId(), request.getJudgeIds());
        return ApiResponse.success(null);
    }

    @GetMapping("/allocations")
    public ApiResponse<IPage<ReviewAllocationResponse>> listAllocations(
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long judgeId,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest req) {
        return ApiResponse.success(reviewService.listAllocations(getAdminUserId(req), eventId, projectId, judgeId,
                new Page<>(current, size)));
    }

    @DeleteMapping("/allocations/{allocationId}")
    public ApiResponse<Void> removeAllocation(@PathVariable Long allocationId, HttpServletRequest req) {
        reviewService.removeAllocation(getAdminUserId(req), allocationId);
        return ApiResponse.success(null);
    }

    // ========== Progress ==========

    @GetMapping("/events/{eventId}/progress")
    public ApiResponse<ReviewProgressResponse> getProgress(@PathVariable Long eventId, HttpServletRequest req) {
        return ApiResponse.success(reviewService.getProgress(getAdminUserId(req), eventId));
    }

    // ========== Scores ==========

    @GetMapping("/events/{eventId}/scores")
    public ApiResponse<IPage<ScoreSummaryItemResponse>> getScoreSummary(
            @PathVariable Long eventId,
            @RequestParam(required = false) Long trackId,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest req) {
        return ApiResponse.success(reviewService.getScoreSummary(
                getAdminUserId(req), eventId, trackId, new Page<>(current, size)));
    }

    // ========== Score Detail ==========

    @GetMapping("/events/{eventId}/projects/{projectId}/scores")
    public ApiResponse<ProjectScoreDetailResponse> getProjectScoreDetail(
            @PathVariable Long eventId,
            @PathVariable Long projectId,
            HttpServletRequest req) {
        return ApiResponse.success(reviewService.getProjectScoreDetail(getAdminUserId(req), eventId, projectId));
    }

    // ========== Materials Cleanup ==========

    // ========== Event Attachment ==========

    @PostMapping("/events/{eventId}/attachment")
    public ApiResponse<Void> uploadEventAttachment(
            @PathVariable Long eventId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String token,
            HttpServletRequest req) {
        Long adminUserId = resolveAdminUserId(req, token);
        if (adminUserId == null) throw new com.zhk.contest.exception.BusinessException(401, "请先登录");
        reviewService.uploadEventAttachment(adminUserId, eventId, file);
        return ApiResponse.success(null);
    }

    @GetMapping("/events/{eventId}/attachment")
    public ResponseEntity<Resource> downloadEventAttachment(
            @PathVariable Long eventId,
            @RequestParam(required = false) String token,
            HttpServletRequest req) {
        Long adminUserId = resolveAdminUserId(req, token);
        if (adminUserId == null) throw new com.zhk.contest.exception.BusinessException(401, "请先登录");
        return reviewService.downloadEventAttachment(eventId);
    }

    @DeleteMapping("/events/{eventId}/attachment")
    public ApiResponse<Void> deleteEventAttachment(
            @PathVariable Long eventId,
            @RequestParam(required = false) String token,
            HttpServletRequest req) {
        Long adminUserId = resolveAdminUserId(req, token);
        if (adminUserId == null) throw new com.zhk.contest.exception.BusinessException(401, "请先登录");
        reviewService.deleteEventAttachment(adminUserId, eventId);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/events/{eventId}/materials")
    public ApiResponse<String> clearMaterials(@PathVariable Long eventId, HttpServletRequest req) {
        reviewService.clearEventMaterials(getAdminUserId(req), eventId);
        return ApiResponse.success("材料已清空");
    }

    // ========== Excel Export ==========

    @GetMapping("/events/{eventId}/export/scores")
    public ResponseEntity<byte[]> exportScores(@PathVariable Long eventId, HttpServletRequest req) {
        byte[] data = reviewService.exportScores(getAdminUserId(req), eventId);
        String filename = "评分明细.xlsx";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + URLEncoder.encode(filename, StandardCharsets.UTF_8))
                .body(data);
    }

    @GetMapping("/events/{eventId}/export/projects")
    public ResponseEntity<byte[]> exportProjects(@PathVariable Long eventId, HttpServletRequest req) {
        byte[] data = reviewService.exportProjects(getAdminUserId(req), eventId);
        String filename = "项目汇总.xlsx";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + URLEncoder.encode(filename, StandardCharsets.UTF_8))
                .body(data);
    }

    @GetMapping("/events/{eventId}/export/allocations")
    public ResponseEntity<byte[]> exportAllocations(@PathVariable Long eventId, HttpServletRequest req) {
        byte[] data = reviewService.exportAllocations(getAdminUserId(req), eventId);
        String filename = "评委分配明细.xlsx";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + URLEncoder.encode(filename, StandardCharsets.UTF_8))
                .body(data);
    }

}
