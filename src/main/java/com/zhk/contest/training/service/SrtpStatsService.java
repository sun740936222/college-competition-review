package com.zhk.contest.training.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zhk.contest.entity.User;
import com.zhk.contest.enums.OrganizationLevel;
import com.zhk.contest.exception.BusinessException;
import com.zhk.contest.mapper.OrganizationMapper;
import com.zhk.contest.mapper.UserMapper;
import com.zhk.contest.training.entity.*;
import com.zhk.contest.training.enums.SrtpStage;
import com.zhk.contest.training.mapper.*;
import com.zhk.contest.util.AdminLevelResolver;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 按项目类别(A/B/C)统计。TOPIC/PROJECT 按组织范围隔离，报告类按批次统计。 */
@Service
public class SrtpStatsService {

    private final SrtpTopicMapper topicMapper;
    private final SrtpProjectMapper projectMapper;
    private final SrtpMidtermReportMapper midtermMapper;
    private final SrtpConclusionReportMapper conclusionMapper;
    private final UserMapper userMapper;
    private final OrganizationMapper organizationMapper;

    /** 三类项目 */
    public static final String[][] CATEGORIES = {
            {"A", "A类·创新训练项目"},
            {"B", "B类·创业训练项目"},
            {"C", "C类·创业实践项目"}
    };

    public SrtpStatsService(SrtpTopicMapper topicMapper, SrtpProjectMapper projectMapper,
                            SrtpMidtermReportMapper midtermMapper, SrtpConclusionReportMapper conclusionMapper,
                            UserMapper userMapper, OrganizationMapper organizationMapper) {
        this.topicMapper = topicMapper;
        this.projectMapper = projectMapper;
        this.midtermMapper = midtermMapper;
        this.conclusionMapper = conclusionMapper;
        this.userMapper = userMapper;
        this.organizationMapper = organizationMapper;
    }

    public List<Map<String, Object>> byCategory(Long adminId, SrtpStage stage, Long batchId) {
        List<Long> visible = visibleOrgs(adminId);
        List<Map<String, Object>> result = new ArrayList<>();
        long total = 0;
        for (String[] cat : CATEGORIES) {
            long c = count(stage, batchId, cat[0], visible);
            total += c;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("category", cat[0]);
            m.put("label", cat[1]);
            m.put("count", c);
            result.add(m);
        }
        long unclassified = count(stage, batchId, null, visible) ;
        if (unclassified > 0) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("category", "-");
            m.put("label", "未分类");
            m.put("count", unclassified);
            result.add(m);
            total += unclassified;
        }
        Map<String, Object> t = new LinkedHashMap<>();
        t.put("category", "TOTAL");
        t.put("label", "合计");
        t.put("count", total);
        result.add(t);
        return result;
    }

    private long count(SrtpStage stage, Long batchId, String category, List<Long> visible) {
        return switch (stage) {
            case TOPIC -> {
                var w = new QueryWrapper<SrtpTopic>().lambda();
                if (batchId != null) w.eq(SrtpTopic::getBatchId, batchId);
                if (category == null) w.and(q -> q.isNull(SrtpTopic::getProjectType).or().eq(SrtpTopic::getProjectType, ""));
                else w.eq(SrtpTopic::getProjectType, category);
                if (visible != null) w.in(SrtpTopic::getOrgId, visible);
                yield topicMapper.selectCount(w);
            }
            case PROJECT -> {
                var w = new QueryWrapper<SrtpProject>().lambda();
                if (batchId != null) w.eq(SrtpProject::getBatchId, batchId);
                if (category == null) w.and(q -> q.isNull(SrtpProject::getProjectType).or().eq(SrtpProject::getProjectType, ""));
                else w.eq(SrtpProject::getProjectType, category);
                if (visible != null) w.in(SrtpProject::getOrgId, visible);
                yield projectMapper.selectCount(w);
            }
            case MIDTERM -> {
                var w = new QueryWrapper<SrtpMidtermReport>().lambda();
                if (batchId != null) w.eq(SrtpMidtermReport::getBatchId, batchId);
                if (category == null) w.and(q -> q.isNull(SrtpMidtermReport::getProjectType).or().eq(SrtpMidtermReport::getProjectType, ""));
                else w.eq(SrtpMidtermReport::getProjectType, category);
                yield midtermMapper.selectCount(w);
            }
            case CONCLUSION -> {
                var w = new QueryWrapper<SrtpConclusionReport>().lambda();
                if (batchId != null) w.eq(SrtpConclusionReport::getBatchId, batchId);
                if (category == null) w.and(q -> q.isNull(SrtpConclusionReport::getProjectType).or().eq(SrtpConclusionReport::getProjectType, ""));
                else w.eq(SrtpConclusionReport::getProjectType, category);
                yield conclusionMapper.selectCount(w);
            }
        };
    }

    /** 可见组织；超级管理员返回 null(不限) */
    private List<Long> visibleOrgs(Long adminId) {
        User u = adminId == null ? null : userMapper.selectById(adminId);
        if (u == null) throw new BusinessException(403, "无权操作");
        AdminLevelResolver.requireAtLeast(u, OrganizationLevel.COLLEGE, organizationMapper);
        if (AdminLevelResolver.isSuperAdmin(u)) return null;
        List<Long> v = AdminLevelResolver.getVisibleOrgIds(u, organizationMapper);
        return v.isEmpty() ? List.of(-1L) : v;
    }
}
