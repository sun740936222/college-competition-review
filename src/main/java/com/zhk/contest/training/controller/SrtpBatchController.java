package com.zhk.contest.training.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhk.contest.dto.response.ApiResponse;
import com.zhk.contest.training.dto.request.SrtpBatchSaveRequest;
import com.zhk.contest.training.entity.SrtpBatch;
import com.zhk.contest.training.service.SrtpBatchService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

/** 批次管理（管理员）。 */
@RestController
@RequestMapping("/api/srtp/admin/batches")
public class SrtpBatchController {

    private final SrtpBatchService batchService;

    public SrtpBatchController(SrtpBatchService batchService) {
        this.batchService = batchService;
    }

    private Long adminId(HttpServletRequest req) {
        return (Long) req.getAttribute("adminUserId");
    }

    @GetMapping
    public ApiResponse<IPage<SrtpBatch>> list(@RequestParam(required = false) String keyword,
                                              @RequestParam(defaultValue = "1") int current,
                                              @RequestParam(defaultValue = "10") int size,
                                              HttpServletRequest req) {
        return ApiResponse.success(batchService.list(adminId(req), new Page<>(current, size), keyword));
    }

    @GetMapping("/{id}")
    public ApiResponse<SrtpBatch> get(@PathVariable Long id) {
        return ApiResponse.success(batchService.get(id));
    }

    @PostMapping
    public ApiResponse<SrtpBatch> create(@RequestBody SrtpBatchSaveRequest req, HttpServletRequest request) {
        return ApiResponse.success("创建成功", batchService.create(adminId(request), req));
    }

    @PutMapping("/{id}")
    public ApiResponse<SrtpBatch> update(@PathVariable Long id, @RequestBody SrtpBatchSaveRequest req,
                                         HttpServletRequest request) {
        return ApiResponse.success("更新成功", batchService.update(adminId(request), id, req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        batchService.delete(adminId(request), id);
        return ApiResponse.success("删除成功", null);
    }
}
