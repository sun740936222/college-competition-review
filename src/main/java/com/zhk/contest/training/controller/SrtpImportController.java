package com.zhk.contest.training.controller;

import com.zhk.contest.dto.response.ApiResponse;
import com.zhk.contest.exception.BusinessException;
import com.zhk.contest.training.service.SrtpImportService;
import com.zhk.contest.training.service.SrtpImportService.ImportResult;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** Excel 批量导入（管理员）：选题/立项 + 模板下载。 */
@RestController
@RequestMapping("/api/srtp/admin/import")
public class SrtpImportController {

    private final SrtpImportService importService;

    public SrtpImportController(SrtpImportService importService) {
        this.importService = importService;
    }

    private Long adminId(HttpServletRequest req) {
        Long id = (Long) req.getAttribute("adminUserId");
        if (id == null) throw new BusinessException(403, "无权操作");
        return id;
    }

    @PostMapping("/topics")
    public ApiResponse<ImportResult> importTopics(@RequestParam Long batchId,
                                                  @RequestParam("file") MultipartFile file,
                                                  HttpServletRequest req) {
        ImportResult r = importService.importTopics(adminId(req), batchId, file);
        return ApiResponse.success("导入完成：成功 " + r.success() + " 条，失败 " + r.fail() + " 条", r);
    }

    @PostMapping("/projects")
    public ApiResponse<ImportResult> importProjects(@RequestParam Long batchId,
                                                    @RequestParam("file") MultipartFile file,
                                                    HttpServletRequest req) {
        ImportResult r = importService.importProjects(adminId(req), batchId, file);
        return ApiResponse.success("导入完成：成功 " + r.success() + " 条，失败 " + r.fail() + " 条", r);
    }

    @GetMapping("/template/topics")
    public ResponseEntity<byte[]> topicTemplate() {
        return xlsx(importService.topicTemplate(), "选题导入模板.xlsx");
    }

    @GetMapping("/template/projects")
    public ResponseEntity<byte[]> projectTemplate() {
        return xlsx(importService.projectTemplate(), "立项导入模板.xlsx");
    }

    private ResponseEntity<byte[]> xlsx(byte[] data, String filename) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + URLEncoder.encode(filename, StandardCharsets.UTF_8))
                .body(data);
    }
}
