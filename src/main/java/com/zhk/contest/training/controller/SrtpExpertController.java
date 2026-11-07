package com.zhk.contest.training.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhk.contest.dto.response.ApiResponse;
import com.zhk.contest.training.dto.request.SrtpExpertSaveRequest;
import com.zhk.contest.training.entity.SrtpExpert;
import com.zhk.contest.training.enums.ExpertLevel;
import com.zhk.contest.training.enums.ExpertStatus;
import com.zhk.contest.training.service.SrtpExpertService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

/** 专家库：管理员管理 + 教师自主申请。 */
@RestController
@RequestMapping("/api/srtp")
public class SrtpExpertController {

    private final SrtpExpertService expertService;

    public SrtpExpertController(SrtpExpertService expertService) {
        this.expertService = expertService;
    }

    private Long adminId(HttpServletRequest req) {
        return (Long) req.getAttribute("adminUserId");
    }

    private Long userId(HttpServletRequest req) {
        return (Long) req.getAttribute("userId");
    }

    // ===== 教师自主申请入库 =====

    @PostMapping("/experts/apply")
    public ApiResponse<SrtpExpert> apply(@RequestBody SrtpExpertSaveRequest req, HttpServletRequest request) {
        return ApiResponse.success("申请已提交，待管理员审核", expertService.selfApply(userId(request), req));
    }

    // ===== 管理员管理 =====

    @GetMapping("/admin/experts")
    public ApiResponse<IPage<SrtpExpert>> list(@RequestParam(required = false) String keyword,
                                               @RequestParam(required = false) ExpertLevel level,
                                               @RequestParam(required = false) ExpertStatus status,
                                               @RequestParam(defaultValue = "1") int current,
                                               @RequestParam(defaultValue = "10") int size,
                                               HttpServletRequest req) {
        return ApiResponse.success(expertService.list(adminId(req), new Page<>(current, size), keyword, level, status));
    }

    @PostMapping("/admin/experts")
    public ApiResponse<SrtpExpert> add(@RequestBody SrtpExpertSaveRequest req, HttpServletRequest request) {
        return ApiResponse.success("添加成功", expertService.add(adminId(request), req));
    }

    @PutMapping("/admin/experts/{id}")
    public ApiResponse<SrtpExpert> update(@PathVariable Long id, @RequestBody SrtpExpertSaveRequest req,
                                          HttpServletRequest request) {
        return ApiResponse.success("更新成功", expertService.update(adminId(request), id, req));
    }

    @DeleteMapping("/admin/experts/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        expertService.delete(adminId(request), id);
        return ApiResponse.success("删除成功", null);
    }

    @PostMapping("/admin/experts/{id}/review")
    public ApiResponse<SrtpExpert> review(@PathVariable Long id,
                                          @RequestParam boolean approve,
                                          @RequestParam(required = false) String reason,
                                          HttpServletRequest request) {
        return ApiResponse.success("审核完成", expertService.review(adminId(request), id, approve, reason));
    }
}
