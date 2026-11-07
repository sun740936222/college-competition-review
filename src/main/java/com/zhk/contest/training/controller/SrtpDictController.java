package com.zhk.contest.training.controller;

import com.zhk.contest.dto.response.ApiResponse;
import com.zhk.contest.training.entity.SrtpApprovalFlowConfig;
import com.zhk.contest.training.entity.SrtpBatch;
import com.zhk.contest.training.entity.SrtpCustomField;
import com.zhk.contest.training.entity.SrtpDiscipline;
import com.zhk.contest.training.enums.SrtpStage;
import com.zhk.contest.training.mapper.SrtpBatchMapper;
import com.zhk.contest.training.mapper.SrtpDisciplineMapper;
import com.zhk.contest.training.service.SrtpApprovalEngine;
import com.zhk.contest.training.service.SrtpCustomFieldService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** SRTP 字典/配置查询：学科、批次、审批流、自定义字段（供前端动态渲染，所有登录用户可读）。 */
@RestController
@RequestMapping("/api/srtp/dict")
public class SrtpDictController {

    private final SrtpDisciplineMapper disciplineMapper;
    private final SrtpBatchMapper batchMapper;
    private final SrtpApprovalEngine approvalEngine;
    private final SrtpCustomFieldService customFieldService;
    private final com.zhk.contest.training.service.SrtpTeacherService teacherService;

    public SrtpDictController(SrtpDisciplineMapper disciplineMapper,
                             SrtpBatchMapper batchMapper,
                             SrtpApprovalEngine approvalEngine,
                             SrtpCustomFieldService customFieldService,
                             com.zhk.contest.training.service.SrtpTeacherService teacherService) {
        this.disciplineMapper = disciplineMapper;
        this.batchMapper = batchMapper;
        this.approvalEngine = approvalEngine;
        this.customFieldService = customFieldService;
        this.teacherService = teacherService;
    }

    /** 指导教师检索（学生选指导教师用，返回 userId/姓名/工号） */
    @GetMapping("/teachers")
    public ApiResponse<java.util.List<java.util.Map<String, Object>>> teachers(
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(teacherService.searchForPicker(keyword));
    }

    @GetMapping("/disciplines")
    public ApiResponse<List<SrtpDiscipline>> disciplines() {
        return ApiResponse.success(disciplineMapper.selectList(
                new QueryWrapper<SrtpDiscipline>().lambda()
                        .orderByAsc(SrtpDiscipline::getParentId)
                        .orderByAsc(SrtpDiscipline::getSortOrder)));
    }

    /** 批次下拉（所有登录用户，用于学生选题/立项时选择批次） */
    @GetMapping("/batches")
    public ApiResponse<List<SrtpBatch>> batches() {
        return ApiResponse.success(batchMapper.selectList(
                new QueryWrapper<SrtpBatch>().lambda()
                        .orderByDesc(SrtpBatch::getCreatedAt)));
    }

    @GetMapping("/flow")
    public ApiResponse<List<SrtpApprovalFlowConfig>> flow(@RequestParam SrtpStage stage,
                                                          @RequestParam(required = false) Long batchId) {
        return ApiResponse.success(approvalEngine.resolveFlow(stage, batchId));
    }

    @GetMapping("/custom-fields")
    public ApiResponse<List<SrtpCustomField>> customFields(@RequestParam SrtpStage stage,
                                                          @RequestParam(required = false) Long batchId) {
        return ApiResponse.success(customFieldService.definitions(stage, batchId));
    }
}
