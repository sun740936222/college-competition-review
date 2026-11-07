package com.zhk.contest.training.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhk.contest.dto.response.ApiResponse;
import com.zhk.contest.exception.BusinessException;
import com.zhk.contest.training.dto.request.SrtpApprovalRequest;
import com.zhk.contest.training.dto.request.SrtpReportSaveRequest;
import com.zhk.contest.training.enums.SrtpRecordStatus;
import com.zhk.contest.training.enums.SrtpStage;
import com.zhk.contest.training.service.SrtpApprovalEngine;
import com.zhk.contest.training.service.SrtpParticipantService;
import com.zhk.contest.training.service.SrtpReportRecord;
import com.zhk.contest.training.service.SrtpReportService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/** 中期 / 结题报告：{stage} = midterm | conclusion。 */
@RestController
@RequestMapping("/api/srtp")
public class SrtpReportController {

    private final SrtpReportService reportService;
    private final SrtpParticipantService participantService;
    private final SrtpApprovalEngine approvalEngine;

    public SrtpReportController(SrtpReportService reportService,
                               SrtpParticipantService participantService,
                               SrtpApprovalEngine approvalEngine) {
        this.reportService = reportService;
        this.participantService = participantService;
        this.approvalEngine = approvalEngine;
    }

    private Long userId(HttpServletRequest req) { return (Long) req.getAttribute("userId"); }
    private Long adminId(HttpServletRequest req) { return (Long) req.getAttribute("adminUserId"); }

    private SrtpStage parseStage(String stage) {
        if ("midterm".equalsIgnoreCase(stage)) return SrtpStage.MIDTERM;
        if ("conclusion".equalsIgnoreCase(stage)) return SrtpStage.CONCLUSION;
        throw new BusinessException("阶段参数错误，仅支持 midterm / conclusion");
    }

    @PostMapping("/reports/{stage}")
    public ApiResponse<SrtpReportRecord> create(@PathVariable String stage, @RequestBody SrtpReportSaveRequest req,
                                                HttpServletRequest request) {
        return ApiResponse.success("已保存", reportService.create(userId(request), parseStage(stage), req));
    }

    @PutMapping("/reports/{stage}/{id}")
    public ApiResponse<SrtpReportRecord> update(@PathVariable String stage, @PathVariable Long id,
                                                @RequestBody SrtpReportSaveRequest req, HttpServletRequest request) {
        return ApiResponse.success("已保存", reportService.update(userId(request), parseStage(stage), id, req));
    }

    @DeleteMapping("/reports/{stage}/{id}")
    public ApiResponse<Void> delete(@PathVariable String stage, @PathVariable Long id, HttpServletRequest request) {
        reportService.delete(userId(request), parseStage(stage), id);
        return ApiResponse.success("已删除", null);
    }

    @PostMapping("/reports/{stage}/{id}/submit")
    public ApiResponse<SrtpReportRecord> submit(@PathVariable String stage, @PathVariable Long id, HttpServletRequest request) {
        return ApiResponse.success("已提交", reportService.submit(userId(request), parseStage(stage), id));
    }

    @GetMapping("/reports/{stage}/mine")
    public ApiResponse<IPage<? extends SrtpReportRecord>> mine(@PathVariable String stage,
                                                               @RequestParam(defaultValue = "1") int current,
                                                               @RequestParam(defaultValue = "10") int size,
                                                               HttpServletRequest req) {
        return ApiResponse.success(reportService.listMine(userId(req), parseStage(stage), new Page<>(current, size)));
    }

    @GetMapping("/reports/{stage}/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable String stage, @PathVariable Long id, HttpServletRequest req) {
        SrtpStage s = parseStage(stage);
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("report", reportService.get(s, id));
        detail.put("members", participantService.listMembers(s, id));
        detail.put("advisors", participantService.listAdvisors(s, id));
        detail.put("history", approvalEngine.history(s, id));
        // 若调用方是管理员，同时返回已指派的评审专家（学校管理员在结题详情里能看到指派状态）
        Long adminId = (Long) req.getAttribute("adminUserId");
        if (adminId != null) {
            try { detail.put("expertAssignments", reportService.listReportAssignments(adminId, s, id)); }
            catch (Exception ignore) { detail.put("expertAssignments", java.util.List.of()); }
        }
        return ApiResponse.success(detail);
    }

    @PostMapping("/reports/{stage}/{id}/approve")
    public ApiResponse<SrtpReportRecord> approve(@PathVariable String stage, @PathVariable Long id,
                                                 @RequestBody(required = false) SrtpApprovalRequest req, HttpServletRequest request) {
        String comment = req != null ? req.getComment() : null;
        String grade = req != null ? req.getGrade() : null;
        return ApiResponse.success("已通过", reportService.approve(userId(request), parseStage(stage), id, comment, grade));
    }

    @PostMapping("/reports/{stage}/{id}/reject")
    public ApiResponse<SrtpReportRecord> reject(@PathVariable String stage, @PathVariable Long id,
                                                @RequestBody SrtpApprovalRequest req, HttpServletRequest request) {
        return ApiResponse.success("已驳回", reportService.reject(userId(request), parseStage(stage), id, req.getComment(), req.getTargetStep()));
    }

    @GetMapping("/admin/reports/{stage}")
    public ApiResponse<IPage<? extends SrtpReportRecord>> adminList(@PathVariable String stage,
                                                                    @RequestParam(required = false) Long batchId,
                                                                    @RequestParam(required = false) SrtpRecordStatus status,
                                                                    @RequestParam(required = false) String keyword,
                                                                    @RequestParam(defaultValue = "1") int current,
                                                                    @RequestParam(defaultValue = "10") int size,
                                                                    HttpServletRequest req) {
        return ApiResponse.success(reportService.listForAdmin(adminId(req), parseStage(stage),
                new Page<>(current, size), batchId, status, keyword));
    }

    // ===== 校专家结题评审：管理员指派评审专家 =====

    /** 给一份结题/中期报告指派一位专家；只有被指派的专家在其审批工作台里才会看到该记录 */
    @PostMapping("/admin/reports/{stage}/{id}/assign-expert")
    public ApiResponse<Void> assignReportExpert(@PathVariable String stage, @PathVariable Long id,
                                                @RequestParam Long expertId, HttpServletRequest req) {
        reportService.assignReportExpert(adminId(req), parseStage(stage), id, expertId);
        return ApiResponse.success("已指派", null);
    }

    /** 列出该报告已被指派的评审专家（含评审状态），管理员管理指派用 */
    @GetMapping("/admin/reports/{stage}/{id}/assignments")
    public ApiResponse<java.util.List<Map<String, Object>>> listReportAssignments(@PathVariable String stage,
                                                                                  @PathVariable Long id,
                                                                                  HttpServletRequest req) {
        return ApiResponse.success(reportService.listReportAssignments(adminId(req), parseStage(stage), id));
    }

    /** 撤销一条指派（仅当该专家还未评审时可撤销），便于管理员换人 */
    @DeleteMapping("/admin/reports/assignments/{assignmentId}")
    public ApiResponse<Void> removeReportAssignment(@PathVariable Long assignmentId, HttpServletRequest req) {
        reportService.removeReportAssignment(adminId(req), assignmentId);
        return ApiResponse.success("已撤销指派", null);
    }

    /** 学校管理员收尾"校专家结题评审"：按已提交的专家评分平均分推导等级并推进/驳回 */
    @PostMapping("/admin/reports/conclusion/{id}/finalize-expert-review")
    public ApiResponse<String> finalizeConclusionExpertReview(@PathVariable Long id,
                                                              @RequestBody(required = false) SrtpApprovalRequest req,
                                                              HttpServletRequest request) {
        String comment = req != null ? req.getComment() : null;
        String note = reportService.finalizeConclusionExpertReview(adminId(request), id, comment);
        return ApiResponse.success(note, note);
    }
}
