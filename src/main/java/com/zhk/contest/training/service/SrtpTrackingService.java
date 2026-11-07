package com.zhk.contest.training.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhk.contest.entity.User;
import com.zhk.contest.exception.BusinessException;
import com.zhk.contest.mapper.OrganizationMapper;
import com.zhk.contest.mapper.UserMapper;
import com.zhk.contest.training.entity.SrtpBatch;
import com.zhk.contest.training.entity.SrtpConclusionReport;
import com.zhk.contest.training.entity.SrtpMidtermReport;
import com.zhk.contest.training.entity.SrtpProject;
import com.zhk.contest.training.entity.SrtpTopic;
import com.zhk.contest.training.mapper.SrtpBatchMapper;
import com.zhk.contest.training.mapper.SrtpConclusionReportMapper;
import com.zhk.contest.training.mapper.SrtpMidtermReportMapper;
import com.zhk.contest.training.mapper.SrtpProjectMapper;
import com.zhk.contest.training.mapper.SrtpTopicMapper;
import com.zhk.contest.util.AdminLevelResolver;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 项目全流程追踪：按项目聚合 选题→立项→中期→结题 状态。 */
@Service
public class SrtpTrackingService {

    private final SrtpProjectMapper projectMapper;
    private final SrtpTopicMapper topicMapper;
    private final SrtpMidtermReportMapper midtermMapper;
    private final SrtpConclusionReportMapper conclusionMapper;
    private final SrtpBatchMapper batchMapper;
    private final UserMapper userMapper;
    private final OrganizationMapper organizationMapper;

    public SrtpTrackingService(SrtpProjectMapper projectMapper, SrtpTopicMapper topicMapper,
                               SrtpMidtermReportMapper midtermMapper, SrtpConclusionReportMapper conclusionMapper,
                               SrtpBatchMapper batchMapper,
                               UserMapper userMapper, OrganizationMapper organizationMapper) {
        this.projectMapper = projectMapper;
        this.topicMapper = topicMapper;
        this.midtermMapper = midtermMapper;
        this.conclusionMapper = conclusionMapper;
        this.batchMapper = batchMapper;
        this.userMapper = userMapper;
        this.organizationMapper = organizationMapper;
    }

    public IPage<Map<String, Object>> track(Long adminId, Page<SrtpProject> page, Long batchId, String keyword) {
        User admin = adminId == null ? null : userMapper.selectById(adminId);
        if (admin == null) throw new BusinessException(403, "无权操作");

        var w = new QueryWrapper<SrtpProject>().lambda();
        if (batchId != null) w.eq(SrtpProject::getBatchId, batchId);
        if (keyword != null && !keyword.isBlank()) w.like(SrtpProject::getName, keyword);
        if (!AdminLevelResolver.isSuperAdmin(admin)) {
            List<Long> visible = AdminLevelResolver.getVisibleOrgIds(admin, organizationMapper);
            if (visible.isEmpty()) return emptyPage(page);
            w.in(SrtpProject::getOrgId, visible);
        }
        w.orderByDesc(SrtpProject::getCreatedAt);

        IPage<SrtpProject> projects = projectMapper.selectPage(page, w);
        // 预加载本页涉及的批次信息，便于展示"批次(级别)"以区分同一项目在不同批次里的副本（如推荐晋级产生的省级副本）
        Map<Long, SrtpBatch> batchMap = new HashMap<>();
        for (SrtpProject p : projects.getRecords()) {
            if (p.getBatchId() != null && !batchMap.containsKey(p.getBatchId())) {
                SrtpBatch b = batchMapper.selectById(p.getBatchId());
                if (b != null) batchMap.put(p.getBatchId(), b);
            }
        }
        IPage<Map<String, Object>> result = emptyPage(page);
        result.setTotal(projects.getTotal());
        result.setRecords(projects.getRecords().stream().map(p -> toRow(p, batchMap)).toList());
        return result;
    }

    private Map<String, Object> toRow(SrtpProject p, Map<Long, SrtpBatch> batchMap) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("projectId", p.getId());
        m.put("projectNo", p.getProjectNo());
        m.put("name", p.getName());
        m.put("batchId", p.getBatchId());
        SrtpBatch b = p.getBatchId() != null ? batchMap.get(p.getBatchId()) : null;
        m.put("batchName", b != null ? b.getName() : null);
        m.put("batchLevel", b != null ? b.getLevel() : null);
        m.put("topicStatus", p.getTopicId() != null ? statusOf(topicMapper.selectById(p.getTopicId())) : "—");
        m.put("projectStatus", p.getStatus() != null ? p.getStatus().name() : "—");
        m.put("midtermStatus", latestMidtermStatus(p.getId()));
        m.put("conclusionStatus", latestConclusionStatus(p.getId()));
        return m;
    }

    private String statusOf(SrtpTopic t) { return t != null && t.getStatus() != null ? t.getStatus().name() : "—"; }

    private String latestMidtermStatus(Long projectId) {
        List<SrtpMidtermReport> l = midtermMapper.selectList(new QueryWrapper<SrtpMidtermReport>().lambda()
                .eq(SrtpMidtermReport::getProjectId, projectId)
                .orderByDesc(SrtpMidtermReport::getCreatedAt).last("LIMIT 1"));
        return l.isEmpty() || l.get(0).getStatus() == null ? "未填报" : l.get(0).getStatus().name();
    }

    private String latestConclusionStatus(Long projectId) {
        List<SrtpConclusionReport> l = conclusionMapper.selectList(new QueryWrapper<SrtpConclusionReport>().lambda()
                .eq(SrtpConclusionReport::getProjectId, projectId)
                .orderByDesc(SrtpConclusionReport::getCreatedAt).last("LIMIT 1"));
        return l.isEmpty() || l.get(0).getStatus() == null ? "未填报" : l.get(0).getStatus().name();
    }

    private IPage<Map<String, Object>> emptyPage(Page<SrtpProject> page) {
        return new Page<>(page.getCurrent(), page.getSize());
    }
}
