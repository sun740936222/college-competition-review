package com.zhk.contest.training.controller;

import com.zhk.contest.training.enums.SrtpStage;
import com.zhk.contest.training.service.SrtpExportService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** SRTP 数据导出（管理员）。 */
@RestController
@RequestMapping("/api/srtp/admin/export")
public class SrtpExportController {

    private final SrtpExportService exportService;

    public SrtpExportController(SrtpExportService exportService) {
        this.exportService = exportService;
    }

    private Long adminId(HttpServletRequest req) { return (Long) req.getAttribute("adminUserId"); }

    private ResponseEntity<byte[]> xlsx(byte[] data, String filename) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + URLEncoder.encode(filename, StandardCharsets.UTF_8))
                .body(data);
    }

    @GetMapping("/topics")
    public ResponseEntity<byte[]> topics(@RequestParam(required = false) Long batchId, HttpServletRequest req) {
        return xlsx(exportService.exportTopics(adminId(req), batchId), "选题列表.xlsx");
    }

    @GetMapping("/projects")
    public ResponseEntity<byte[]> projects(@RequestParam(required = false) Long batchId, HttpServletRequest req) {
        return xlsx(exportService.exportProjects(adminId(req), batchId), "立项列表.xlsx");
    }

    @GetMapping("/reports/{stage}")
    public ResponseEntity<byte[]> reports(@PathVariable String stage, @RequestParam(required = false) Long batchId,
                                          HttpServletRequest req) {
        SrtpStage s = "conclusion".equalsIgnoreCase(stage) ? SrtpStage.CONCLUSION : SrtpStage.MIDTERM;
        String name = s == SrtpStage.MIDTERM ? "中期报告.xlsx" : "结题报告.xlsx";
        return xlsx(exportService.exportReports(adminId(req), s, batchId), name);
    }

    @GetMapping("/unsubmitted")
    public ResponseEntity<byte[]> unsubmitted(@RequestParam Long batchId, HttpServletRequest req) {
        return xlsx(exportService.exportUnsubmitted(adminId(req), batchId), "未提交选题人员.xlsx");
    }

    /** 结题等级汇总(项目编号/名称/等级/审批状态) — 学校管理员汇总用 */
    @GetMapping("/conclusion-grades")
    public ResponseEntity<byte[]> conclusionGrades(@RequestParam(required = false) Long batchId, HttpServletRequest req) {
        return xlsx(exportService.exportConclusionGrades(adminId(req), batchId), "结题等级汇总.xlsx");
    }
}
