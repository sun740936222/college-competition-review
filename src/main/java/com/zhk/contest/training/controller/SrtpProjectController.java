package com.zhk.contest.training.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhk.contest.dto.response.ApiResponse;
import com.zhk.contest.training.dto.request.SrtpApprovalRequest;
import com.zhk.contest.training.dto.request.SrtpProjectSaveRequest;
import com.zhk.contest.training.entity.SrtpProject;
import com.zhk.contest.training.enums.SrtpRecordStatus;
import com.zhk.contest.training.enums.SrtpStage;
import com.zhk.contest.training.service.SrtpApprovalEngine;
import com.zhk.contest.training.service.SrtpParticipantService;
import com.zhk.contest.training.service.SrtpProjectService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/** 立项：申报、提交、审批（通过时生成项目编号）、校级专家分配、查询。 */
@RestController
@RequestMapping("/api/srtp")
public class SrtpProjectController {

    private static final SrtpStage STAGE = SrtpStage.PROJECT;

    private final SrtpProjectService projectService;
    private final SrtpParticipantService participantService;
    private final SrtpApprovalEngine approvalEngine;

    public SrtpProjectController(SrtpProjectService projectService,
                                SrtpParticipantService participantService,
                                SrtpApprovalEngine approvalEngine) {
        this.projectService = projectService;
        this.participantService = participantService;
        this.approvalEngine = approvalEngine;
    }

    private Long userId(HttpServletRequest req) {
        return (Long) req.getAttribute("userId");
    }

    private Long adminId(HttpServletRequest req) {
        return (Long) req.getAttribute("adminUserId");
    }

    @PostMapping("/projects")
    public ApiResponse<SrtpProject> create(@RequestBody SrtpProjectSaveRequest req, HttpServletRequest request) {
        return ApiResponse.success("已保存", projectService.create(userId(request), req));
    }

    @PutMapping("/projects/{id}")
    public ApiResponse<SrtpProject> update(@PathVariable Long id, @RequestBody SrtpProjectSaveRequest req,
                                           HttpServletRequest request) {
        return ApiResponse.success("已保存", projectService.update(userId(request), id, req));
    }

    @DeleteMapping("/projects/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        projectService.delete(userId(request), id);
        return ApiResponse.success("已删除", null);
    }

    @PostMapping("/projects/{id}/submit")
    public ApiResponse<SrtpProject> submit(@PathVariable Long id, HttpServletRequest request) {
        return ApiResponse.success("已提交", projectService.submit(userId(request), id));
    }

    @GetMapping("/projects/mine")
    public ApiResponse<IPage<SrtpProject>> mine(@RequestParam(defaultValue = "1") int current,
                                                @RequestParam(defaultValue = "10") int size,
                                                HttpServletRequest req) {
        return ApiResponse.success(projectService.listMine(userId(req), new Page<>(current, size)));
    }

    /** 学生端：可填报中期/结题的立项（排除省级/国家级推荐副本，附带批次名/级别/状态/该阶段截止时间） */
    @GetMapping("/projects/mine-eligible-for-report")
    public ApiResponse<java.util.List<Map<String, Object>>> mineEligibleForReport(@RequestParam SrtpStage stage,
                                                                                  HttpServletRequest req) {
        return ApiResponse.success(projectService.listMineEligibleForReport(userId(req), stage));
    }

    @GetMapping("/projects/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id, HttpServletRequest req) {
        SrtpProject p = projectService.get(id);
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("project", p);
        detail.put("members", participantService.listMembers(STAGE, id));
        detail.put("advisors", participantService.listAdvisors(STAGE, id));
        detail.put("history", approvalEngine.history(STAGE, id));
        Long aid = (Long) req.getAttribute("adminUserId");
        if (aid != null) {
            try { detail.put("expertAssignments", projectService.listProjectAssignments(aid, id)); }
            catch (Exception ignore) { detail.put("expertAssignments", java.util.List.of()); }
        }
        return ApiResponse.success(detail);
    }

    /** 列出该立项已被指派的评审专家（含分数、评审状态） */
    @GetMapping("/admin/projects/{id}/assignments")
    public ApiResponse<java.util.List<Map<String, Object>>> listProjectAssignments(@PathVariable Long id, HttpServletRequest req) {
        return ApiResponse.success(projectService.listProjectAssignments(adminId(req), id));
    }

    /** 撤销一条立项专家指派（仅当该专家尚未评审时可撤销） */
    @DeleteMapping("/admin/projects/assignments/{assignmentId}")
    public ApiResponse<Void> removeProjectAssignment(@PathVariable Long assignmentId, HttpServletRequest req) {
        projectService.removeProjectAssignment(adminId(req), assignmentId);
        return ApiResponse.success("已撤销指派", null);
    }

    // ===== 审批 =====

    @PostMapping("/projects/{id}/approve")
    public ApiResponse<SrtpProject> approve(@PathVariable Long id, @RequestBody(required = false) SrtpApprovalRequest req,
                                            HttpServletRequest request) {
        String comment = req != null ? req.getComment() : null;
        return ApiResponse.success("已通过", projectService.approve(userId(request), id, comment));
    }

    @PostMapping("/projects/{id}/reject")
    public ApiResponse<SrtpProject> reject(@PathVariable Long id, @RequestBody SrtpApprovalRequest req,
                                           HttpServletRequest request) {
        return ApiResponse.success("已驳回", projectService.reject(userId(request), id, req.getComment(), req.getTargetStep()));
    }

    // ===== 校级专家分配 =====

    @PostMapping("/admin/projects/{id}/assign-expert")
    public ApiResponse<Void> assignExpert(@PathVariable Long id, @RequestParam Long expertId,
                                          HttpServletRequest request) {
        projectService.assignExpert(adminId(request), id, expertId);
        return ApiResponse.success("已分配", null);
    }

    // ===== 管理员列表 =====

    @GetMapping("/admin/projects")
    public ApiResponse<IPage<SrtpProject>> adminList(@RequestParam(required = false) Long batchId,
                                                     @RequestParam(required = false) SrtpRecordStatus status,
                                                     @RequestParam(required = false) String keyword,
                                                     @RequestParam(defaultValue = "1") int current,
                                                     @RequestParam(defaultValue = "10") int size,
                                                     HttpServletRequest req) {
        return ApiResponse.success(projectService.listForAdmin(adminId(req),
                new Page<>(current, size), batchId, status, keyword));
    }
}
