package com.zhk.contest.training.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhk.contest.dto.response.ApiResponse;
import com.zhk.contest.training.dto.request.SrtpApprovalRequest;
import com.zhk.contest.training.dto.request.SrtpTopicSaveRequest;
import com.zhk.contest.training.entity.SrtpTopic;
import com.zhk.contest.training.enums.SrtpRecordStatus;
import com.zhk.contest.training.enums.SrtpStage;
import com.zhk.contest.training.service.SrtpApprovalEngine;
import com.zhk.contest.training.service.SrtpParticipantService;
import com.zhk.contest.training.service.SrtpTopicService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 选题：学生自拟申报、学校给定认领、审批、查询。 */
@RestController
@RequestMapping("/api/srtp")
public class SrtpTopicController {

    private static final SrtpStage STAGE = SrtpStage.TOPIC;

    private final SrtpTopicService topicService;
    private final SrtpParticipantService participantService;
    private final SrtpApprovalEngine approvalEngine;

    public SrtpTopicController(SrtpTopicService topicService,
                              SrtpParticipantService participantService,
                              SrtpApprovalEngine approvalEngine) {
        this.topicService = topicService;
        this.participantService = participantService;
        this.approvalEngine = approvalEngine;
    }

    private Long userId(HttpServletRequest req) {
        return (Long) req.getAttribute("userId");
    }

    private Long adminId(HttpServletRequest req) {
        return (Long) req.getAttribute("adminUserId");
    }

    // ===== 学生自拟 =====

    @PostMapping("/topics")
    public ApiResponse<SrtpTopic> create(@RequestBody SrtpTopicSaveRequest req, HttpServletRequest request) {
        return ApiResponse.success("已保存", topicService.create(userId(request), req));
    }

    @PutMapping("/topics/{id}")
    public ApiResponse<SrtpTopic> update(@PathVariable Long id, @RequestBody SrtpTopicSaveRequest req,
                                         HttpServletRequest request) {
        return ApiResponse.success("已保存", topicService.update(userId(request), id, req));
    }

    @DeleteMapping("/topics/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        topicService.delete(userId(request), id);
        return ApiResponse.success("已删除", null);
    }

    @PostMapping("/topics/{id}/submit")
    public ApiResponse<SrtpTopic> submit(@PathVariable Long id, HttpServletRequest request) {
        return ApiResponse.success("已提交", topicService.submit(userId(request), id));
    }

    @GetMapping("/topics/mine")
    public ApiResponse<IPage<SrtpTopic>> mine(@RequestParam(defaultValue = "1") int current,
                                              @RequestParam(defaultValue = "10") int size,
                                              HttpServletRequest req) {
        return ApiResponse.success(topicService.listMine(userId(req), new Page<>(current, size)));
    }

    @GetMapping("/topics/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        return ApiResponse.success(buildDetail(id));
    }

    // ===== 学校给定 + 认领 =====

    @GetMapping("/topics/claimable")
    public ApiResponse<List<SrtpTopic>> claimable(@RequestParam(required = false) Long batchId) {
        return ApiResponse.success(topicService.listClaimable(batchId));
    }

    @GetMapping("/topics/claimed-mine")
    public ApiResponse<List<SrtpTopic>> claimedMine(HttpServletRequest request) {
        return ApiResponse.success(topicService.myClaimedTopics(userId(request)));
    }

    @PostMapping("/topics/{id}/claim")
    public ApiResponse<Void> claim(@PathVariable Long id, HttpServletRequest request) {
        topicService.claim(userId(request), id);
        return ApiResponse.success("认领成功", null);
    }

    @PostMapping("/admin/topics/publish")
    public ApiResponse<SrtpTopic> publish(@RequestBody SrtpTopicSaveRequest req, HttpServletRequest request) {
        return ApiResponse.success("已发布", topicService.publish(adminId(request), req));
    }

    // ===== 审批（审批人：教师/专家/管理员，引擎按节点角色校验）=====

    @PostMapping("/topics/{id}/approve")
    public ApiResponse<SrtpTopic> approve(@PathVariable Long id, @RequestBody(required = false) SrtpApprovalRequest req,
                                          HttpServletRequest request) {
        String comment = req != null ? req.getComment() : null;
        return ApiResponse.success("已通过", topicService.approve(userId(request), id, comment));
    }

    @PostMapping("/topics/{id}/reject")
    public ApiResponse<SrtpTopic> reject(@PathVariable Long id, @RequestBody SrtpApprovalRequest req,
                                         HttpServletRequest request) {
        return ApiResponse.success("已驳回", topicService.reject(userId(request), id, req.getComment(), req.getTargetStep()));
    }

    // ===== 管理员列表 =====

    @GetMapping("/admin/topics")
    public ApiResponse<IPage<SrtpTopic>> adminList(@RequestParam(required = false) Long batchId,
                                                   @RequestParam(required = false) SrtpRecordStatus status,
                                                   @RequestParam(required = false) String keyword,
                                                   @RequestParam(defaultValue = "1") int current,
                                                   @RequestParam(defaultValue = "10") int size,
                                                   HttpServletRequest req) {
        return ApiResponse.success(topicService.listForAdmin(adminId(req),
                new Page<>(current, size), batchId, status, keyword));
    }

    @DeleteMapping("/admin/topics/{id}")
    public ApiResponse<Void> adminDelete(@PathVariable Long id, HttpServletRequest req) {
        topicService.adminDelete(adminId(req), id);
        return ApiResponse.success("已删除", null);
    }

    @DeleteMapping("/admin/topics/batch")
    public ApiResponse<Integer> adminDeleteBatch(@RequestBody List<Long> ids, HttpServletRequest req) {
        int count = topicService.adminDeleteBatch(adminId(req), ids);
        return ApiResponse.success("已删除 " + count + " 条", count);
    }

    private Map<String, Object> buildDetail(Long id) {
        SrtpTopic t = topicService.get(id);
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("topic", t);
        detail.put("members", participantService.listMembers(STAGE, id));
        detail.put("advisors", participantService.listAdvisors(STAGE, id));
        detail.put("history", approvalEngine.history(STAGE, id));
        detail.put("selections", topicService.selectionsForTopic(id));
        return detail;
    }
}
