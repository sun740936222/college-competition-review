package com.zhk.contest.training.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhk.contest.entity.User;
import com.zhk.contest.exception.BusinessException;
import com.zhk.contest.mapper.OrganizationMapper;
import com.zhk.contest.mapper.UserMapper;
import com.zhk.contest.training.dto.request.SrtpBatchSaveRequest;
import com.zhk.contest.training.entity.SrtpApprovalFlowConfig;
import com.zhk.contest.training.entity.SrtpBatch;
import com.zhk.contest.training.entity.SrtpConclusionReport;
import com.zhk.contest.training.entity.SrtpMidtermReport;
import com.zhk.contest.training.entity.SrtpProject;
import com.zhk.contest.training.entity.SrtpTopic;
import com.zhk.contest.training.enums.SrtpBatchStatus;
import com.zhk.contest.training.mapper.SrtpApprovalFlowConfigMapper;
import com.zhk.contest.training.mapper.SrtpBatchMapper;
import com.zhk.contest.training.mapper.SrtpConclusionReportMapper;
import com.zhk.contest.training.mapper.SrtpMidtermReportMapper;
import com.zhk.contest.training.mapper.SrtpProjectMapper;
import com.zhk.contest.training.mapper.SrtpTopicMapper;
import com.zhk.contest.util.AdminLevelResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 批次管理（管理员）。新建批次时把全局默认审批流模板复制为该批次可独立编辑的副本。 */
@Service
public class SrtpBatchService {

    private final SrtpBatchMapper batchMapper;
    private final SrtpApprovalFlowConfigMapper flowConfigMapper;
    private final SrtpTopicMapper topicMapper;
    private final SrtpProjectMapper projectMapper;
    private final SrtpMidtermReportMapper midtermMapper;
    private final SrtpConclusionReportMapper conclusionMapper;
    private final UserMapper userMapper;
    private final OrganizationMapper organizationMapper;

    public SrtpBatchService(SrtpBatchMapper batchMapper,
                            SrtpApprovalFlowConfigMapper flowConfigMapper,
                            SrtpTopicMapper topicMapper,
                            SrtpProjectMapper projectMapper,
                            SrtpMidtermReportMapper midtermMapper,
                            SrtpConclusionReportMapper conclusionMapper,
                            UserMapper userMapper,
                            OrganizationMapper organizationMapper) {
        this.batchMapper = batchMapper;
        this.flowConfigMapper = flowConfigMapper;
        this.topicMapper = topicMapper;
        this.projectMapper = projectMapper;
        this.midtermMapper = midtermMapper;
        this.conclusionMapper = conclusionMapper;
        this.userMapper = userMapper;
        this.organizationMapper = organizationMapper;
    }

    @Transactional
    public SrtpBatch create(Long adminId, SrtpBatchSaveRequest req) {
        requireAdmin(adminId);
        SrtpBatch b = new SrtpBatch();
        apply(b, req);
        if (b.getStatus() == null) b.setStatus(SrtpBatchStatus.TOPIC_OPEN);
        batchMapper.insert(b);
        copyDefaultFlow(b.getId());
        return b;
    }

    @Transactional
    public SrtpBatch update(Long adminId, Long id, SrtpBatchSaveRequest req) {
        requireAdmin(adminId);
        SrtpBatch b = batchMapper.selectById(id);
        if (b == null) throw new BusinessException("批次不存在");
        apply(b, req);
        batchMapper.updateById(b);
        return b;
    }

    @Transactional
    public void delete(Long adminId, Long id) {
        requireAdmin(adminId);
        // 防呆：批次下若还有任何选题/立项/中期/结题挂着，禁止删除，
        // 免得像项目 4 那种被孤在已删批次里、后续找不到源批次做推荐晋级
        long topics = topicMapper.selectCount(new QueryWrapper<SrtpTopic>().lambda()
                .eq(SrtpTopic::getBatchId, id));
        long projects = projectMapper.selectCount(new QueryWrapper<SrtpProject>().lambda()
                .eq(SrtpProject::getBatchId, id));
        long midterms = midtermMapper.selectCount(new QueryWrapper<SrtpMidtermReport>().lambda()
                .eq(SrtpMidtermReport::getBatchId, id));
        long conclusions = conclusionMapper.selectCount(new QueryWrapper<SrtpConclusionReport>().lambda()
                .eq(SrtpConclusionReport::getBatchId, id));
        long total = topics + projects + midterms + conclusions;
        if (total > 0) {
            StringBuilder sb = new StringBuilder("该批次下仍有 ");
            java.util.List<String> parts = new java.util.ArrayList<>();
            if (topics > 0) parts.add(topics + " 条选题");
            if (projects > 0) parts.add(projects + " 条立项");
            if (midterms > 0) parts.add(midterms + " 条中期报告");
            if (conclusions > 0) parts.add(conclusions + " 条结题报告");
            sb.append(String.join("、", parts));
            sb.append("，请先在对应管理页面删除或迁移这些记录后，再删除该批次");
            throw new BusinessException(sb.toString());
        }
        batchMapper.deleteById(id);
        // 同时清理该批次的审批流副本
        flowConfigMapper.delete(new QueryWrapper<SrtpApprovalFlowConfig>().lambda()
                .eq(SrtpApprovalFlowConfig::getBatchId, id));
    }

    public SrtpBatch get(Long id) {
        return batchMapper.selectById(id);
    }

    public IPage<SrtpBatch> list(Long adminId, Page<SrtpBatch> page, String keyword) {
        User admin = requireAdmin(adminId);
        var wrapper = new QueryWrapper<SrtpBatch>().lambda();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(SrtpBatch::getName, keyword);
        }
        if (!AdminLevelResolver.isSuperAdmin(admin)) {
            List<Long> visible = AdminLevelResolver.getVisibleOrgIds(admin, organizationMapper);
            // 归属可见组织，或未指定组织(全校通用)的批次
            wrapper.and(w -> {
                if (visible.isEmpty()) {
                    w.isNull(SrtpBatch::getOrgId);
                } else {
                    w.in(SrtpBatch::getOrgId, visible).or().isNull(SrtpBatch::getOrgId);
                }
            });
        }
        wrapper.orderByDesc(SrtpBatch::getCreatedAt);
        return batchMapper.selectPage(page, wrapper);
    }

    /** 复制全局默认审批流模板(batch_id=NULL)为该批次副本 */
    private void copyDefaultFlow(Long batchId) {
        List<SrtpApprovalFlowConfig> templates = flowConfigMapper.selectList(
                new QueryWrapper<SrtpApprovalFlowConfig>().lambda()
                        .isNull(SrtpApprovalFlowConfig::getBatchId)
                        .orderByAsc(SrtpApprovalFlowConfig::getStepOrder));
        for (SrtpApprovalFlowConfig t : templates) {
            SrtpApprovalFlowConfig c = new SrtpApprovalFlowConfig();
            c.setBatchId(batchId);
            c.setStage(t.getStage());
            c.setStepOrder(t.getStepOrder());
            c.setStepName(t.getStepName());
            c.setApproverRole(t.getApproverRole());
            c.setNeedAssignExpert(t.getNeedAssignExpert());
            flowConfigMapper.insert(c);
        }
    }

    private void apply(SrtpBatch b, SrtpBatchSaveRequest req) {
        b.setName(req.getName());
        b.setCode(req.getCode());
        b.setYear(req.getYear());
        b.setLevel(req.getLevel());
        if (req.getStatus() != null) b.setStatus(req.getStatus());
        b.setProjectDeadline(req.getProjectDeadline());
        b.setMidtermDeadline(req.getMidtermDeadline());
        b.setConclusionDeadline(req.getConclusionDeadline());
        b.setOrgId(req.getOrgId());
    }

    private User requireAdmin(Long adminId) {
        User u = (adminId == null) ? null : userMapper.selectById(adminId);
        if (u == null) throw new BusinessException(403, "无权操作");
        AdminLevelResolver.requireAtLeast(u, com.zhk.contest.enums.OrganizationLevel.COLLEGE, organizationMapper);
        return u;
    }
}
