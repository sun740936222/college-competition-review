package com.zhk.contest.training.controller;

import com.zhk.contest.dto.response.ApiResponse;
import com.zhk.contest.exception.BusinessException;
import com.zhk.contest.training.entity.SrtpCustomField;
import com.zhk.contest.training.enums.SrtpStage;
import com.zhk.contest.training.service.SrtpCustomFieldService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 自定义字段管理（管理员）。 */
@RestController
@RequestMapping("/api/srtp/admin/custom-fields")
public class SrtpCustomFieldController {

    private final SrtpCustomFieldService customFieldService;

    public SrtpCustomFieldController(SrtpCustomFieldService customFieldService) {
        this.customFieldService = customFieldService;
    }

    private void requireAdmin(HttpServletRequest req) {
        if (req.getAttribute("adminUserId") == null) throw new BusinessException(403, "无权操作");
    }

    @GetMapping
    public ApiResponse<List<SrtpCustomField>> list(@RequestParam SrtpStage stage,
                                                   @RequestParam(required = false) Long batchId,
                                                   HttpServletRequest req) {
        requireAdmin(req);
        return ApiResponse.success(customFieldService.listForAdmin(stage, batchId));
    }

    @PostMapping
    public ApiResponse<SrtpCustomField> save(@RequestBody SrtpCustomField field, HttpServletRequest req) {
        requireAdmin(req);
        return ApiResponse.success("已保存", customFieldService.save(field));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, HttpServletRequest req) {
        requireAdmin(req);
        customFieldService.delete(id);
        return ApiResponse.success("已删除", null);
    }
}
