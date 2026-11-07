package com.zhk.contest.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhk.contest.config.TokenService;
import com.zhk.contest.config.TokenService.TokenInfo;
import com.zhk.contest.dto.request.ScoreSubmitRequest;
import com.zhk.contest.dto.response.*;
import com.zhk.contest.entity.Event;
import com.zhk.contest.entity.ProjectMaterial;
import com.zhk.contest.entity.ReviewAllocation;
import com.zhk.contest.exception.BusinessException;
import com.zhk.contest.mapper.EventMapper;
import com.zhk.contest.mapper.ProjectMaterialMapper;
import com.zhk.contest.mapper.ReviewAllocationMapper;
import com.zhk.contest.service.FileStorageService;
import com.zhk.contest.service.JudgeService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/judge")
public class JudgeController {

    private final JudgeService judgeService;
    private final ReviewAllocationMapper reviewAllocationMapper;
    private final ProjectMaterialMapper materialMapper;
    private final FileStorageService fileStorageService;
    private final TokenService tokenService;
    private final EventMapper eventMapper;

    public JudgeController(JudgeService judgeService,
                           ReviewAllocationMapper reviewAllocationMapper,
                           ProjectMaterialMapper materialMapper,
                           FileStorageService fileStorageService,
                           TokenService tokenService,
                           EventMapper eventMapper) {
        this.judgeService = judgeService;
        this.reviewAllocationMapper = reviewAllocationMapper;
        this.materialMapper = materialMapper;
        this.fileStorageService = fileStorageService;
        this.tokenService = tokenService;
        this.eventMapper = eventMapper;
    }

    private Long getJudgeId(HttpServletRequest req) {
        return (Long) req.getAttribute("userId");
    }

    @GetMapping("/projects")
    public ApiResponse<IPage<JudgeProjectResponse>> listProjects(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "12") int size,
            HttpServletRequest req) {
        return ApiResponse.success(judgeService.listMyProjects(getJudgeId(req), status,
                new Page<>(current, size)));
    }

    @GetMapping("/projects/{projectId}")
    public ApiResponse<JudgeProjectDetailResponse> projectDetail(
            @PathVariable Long projectId,
            HttpServletRequest req) {
        return ApiResponse.success(judgeService.getProjectDetail(getJudgeId(req), projectId));
    }

    @GetMapping("/projects/{projectId}/criteria")
    public ApiResponse<List<ScoreCriteriaResponse>> getCriteria(
            @PathVariable Long projectId,
            HttpServletRequest req) {
        return ApiResponse.success(judgeService.getProjectCriteria(getJudgeId(req), projectId));
    }

    @PutMapping("/projects/{projectId}/scores")
    public ApiResponse<Void> submitScores(
            @PathVariable Long projectId,
            @RequestBody ScoreSubmitRequest request,
            HttpServletRequest req) {
        judgeService.submitScores(getJudgeId(req), projectId, request);
        return ApiResponse.success(null);
    }

    /** Download event scoring attachment. Judge auth via token query param. */
    @GetMapping("/events/{eventId}/attachment")
    public ResponseEntity<Resource> downloadEventAttachment(@PathVariable Long eventId,
                                                             @RequestParam(required = false) String token,
                                                             HttpServletRequest req) {
        Long judgeId = getJudgeId(req);
        if (judgeId == null && token != null && !token.isBlank()) {
            TokenInfo info = tokenService.getTokenInfo(token);
            if (info != null) judgeId = info.userId();
        }
        if (judgeId == null) throw new BusinessException(401, "请先登录");

        Event event = eventMapper.selectById(eventId);
        if (event == null) throw new BusinessException("赛事不存在");
        if (event.getAttachmentPath() == null) throw new BusinessException("该赛事未上传评分附件");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + java.net.URLEncoder.encode(
                                event.getAttachmentName(), java.nio.charset.StandardCharsets.UTF_8))
                .body(fileStorageService.loadAsResource(event.getAttachmentPath()));
    }

    /** Judge file access with authorization check. Supports iframe/video/axios via ?token= query parameter. */
    @GetMapping("/files/{materialId}")
    public ResponseEntity<Resource> serveFile(@PathVariable Long materialId,
                                               @RequestParam(required = false) String token,
                                               HttpServletRequest req) {
        Long judgeId = getJudgeId(req);
        // Fallback: iframe/video/axios with responseType don't send Authorization header,
        // so use ?token= query param
        if (judgeId == null && token != null && !token.isBlank()) {
            TokenInfo info = tokenService.getTokenInfo(token);
            if (info != null) judgeId = info.userId();
        }
        if (judgeId == null) throw new BusinessException(401, "请先登录");

        ProjectMaterial material = materialMapper.selectById(materialId);
        if (material == null) throw new BusinessException("文件不存在");

        // Verify judge is allocated to this project
        List<ReviewAllocation> allocs = reviewAllocationMapper.findByJudgeId(judgeId);
        boolean authorized = allocs.stream().anyMatch(a -> a.getProjectId().equals(material.getProjectId()));
        if (!authorized) {
            throw new BusinessException(403, "您无权访问该项目文件");
        }

        try {
            Resource resource = fileStorageService.loadAsResource(material.getFilePath());
            String filename = material.getStoredFilename().toLowerCase();

            MediaType mediaType;
            if (filename.endsWith(".pdf")) {
                mediaType = MediaType.APPLICATION_PDF;
            } else if (filename.endsWith(".mp4")) {
                mediaType = MediaType.valueOf("video/mp4");
            } else {
                mediaType = MediaType.APPLICATION_OCTET_STREAM;
            }

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.inline().filename(material.getOriginalFilename(), StandardCharsets.UTF_8).build().toString())
                    .body(resource);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("文件读取失败: " + e.getMessage());
        }
    }
}
