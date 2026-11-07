package com.zhk.contest.training.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zhk.contest.entity.User;
import com.zhk.contest.enums.UserRole;
import com.zhk.contest.exception.BusinessException;
import com.zhk.contest.mapper.OrganizationMapper;
import com.zhk.contest.mapper.UserMapper;
import com.zhk.contest.training.entity.*;
import com.zhk.contest.training.enums.SrtpStage;
import com.zhk.contest.training.mapper.*;
import com.zhk.contest.util.AdminLevelResolver;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** SRTP 数据导出（Apache POI，.xlsx）：选题/立项/中期/结题 + 未提交人员统计。 */
@Service
public class SrtpExportService {

    private final SrtpTopicMapper topicMapper;
    private final SrtpProjectMapper projectMapper;
    private final SrtpMidtermReportMapper midtermMapper;
    private final SrtpConclusionReportMapper conclusionMapper;
    private final SrtpDisciplineMapper disciplineMapper;
    private final UserMapper userMapper;
    private final OrganizationMapper organizationMapper;

    private static final Map<String, String> STATUS = Map.of(
            "DRAFT", "草稿", "SUBMITTED", "已提交", "IN_REVIEW", "审批中",
            "APPROVED", "已通过", "RETURNED", "已退回", "REJECTED", "已拒绝");

    public SrtpExportService(SrtpTopicMapper topicMapper, SrtpProjectMapper projectMapper,
                             SrtpMidtermReportMapper midtermMapper, SrtpConclusionReportMapper conclusionMapper,
                             SrtpDisciplineMapper disciplineMapper, UserMapper userMapper,
                             OrganizationMapper organizationMapper) {
        this.topicMapper = topicMapper;
        this.projectMapper = projectMapper;
        this.midtermMapper = midtermMapper;
        this.conclusionMapper = conclusionMapper;
        this.disciplineMapper = disciplineMapper;
        this.userMapper = userMapper;
        this.organizationMapper = organizationMapper;
    }

    private Map<Long, String> disciplineNames() {
        return disciplineMapper.selectList(null).stream()
                .collect(Collectors.toMap(SrtpDiscipline::getId, SrtpDiscipline::getName, (a, b) -> a));
    }

    private String st(Object status) {
        return status == null ? "" : STATUS.getOrDefault(status.toString(), status.toString());
    }

    public byte[] exportTopics(Long adminId, Long batchId) {
        List<Long> orgs = visibleOrgs(adminId);
        var w = new QueryWrapper<SrtpTopic>().lambda();
        if (batchId != null) w.eq(SrtpTopic::getBatchId, batchId);
        if (orgs != null) w.in(SrtpTopic::getOrgId, orgs);
        w.orderByDesc(SrtpTopic::getCreatedAt);
        List<SrtpTopic> list = topicMapper.selectList(w);
        Map<Long, String> dn = disciplineNames();
        String[] cols = {"选题名称", "来源", "项目类型", "一级学科", "二级学科", "创意研究类型", "联系方式", "状态", "简介"};
        return build("选题列表", cols, list, t -> new String[]{
                nv(t.getName()),
                t.getSource() != null && t.getSource().name().equals("SCHOOL_ASSIGNED") ? "学校给定" : "学生自拟",
                nv(t.getProjectType()), dn.getOrDefault(t.getDisciplineL1Id(), ""), dn.getOrDefault(t.getDisciplineL2Id(), ""),
                nv(t.getResearchType()), nv(t.getInitiatorContact()), st(t.getStatus()), nv(t.getInfo())
        });
    }

    public byte[] exportProjects(Long adminId, Long batchId) {
        List<Long> orgs = visibleOrgs(adminId);
        var w = new QueryWrapper<SrtpProject>().lambda();
        if (batchId != null) w.eq(SrtpProject::getBatchId, batchId);
        if (orgs != null) w.in(SrtpProject::getOrgId, orgs);
        w.orderByDesc(SrtpProject::getCreatedAt);
        List<SrtpProject> list = projectMapper.selectList(w);
        Map<Long, String> dn = disciplineNames();
        String[] cols = {"项目编号", "项目名称", "一级学科", "二级学科", "学生人数", "状态", "简介"};
        return build("立项列表", cols, list, p -> new String[]{
                nv(p.getProjectNo()), nv(p.getName()), dn.getOrDefault(p.getDisciplineL1Id(), ""),
                dn.getOrDefault(p.getDisciplineL2Id(), ""), p.getStudentCount() == null ? "" : p.getStudentCount().toString(),
                st(p.getStatus()), nv(p.getDescription())
        });
    }

    public byte[] exportReports(Long adminId, SrtpStage stage, Long batchId) {
        boolean midterm = stage == SrtpStage.MIDTERM;
        String[] cols = {"项目名称", "项目类型", "学生人数", "状态", "内容"};
        if (midterm) {
            var w = new QueryWrapper<SrtpMidtermReport>().lambda();
            if (batchId != null) w.eq(SrtpMidtermReport::getBatchId, batchId);
            w.orderByDesc(SrtpMidtermReport::getCreatedAt);
            List<SrtpMidtermReport> list = midtermMapper.selectList(w);
            return build("中期报告", cols, list, r -> new String[]{
                    nv(r.getSnapshotName()), nv(r.getProjectType()),
                    r.getStudentCount() == null ? "" : r.getStudentCount().toString(), st(r.getStatus()), nv(r.getDescription())});
        }
        var w = new QueryWrapper<SrtpConclusionReport>().lambda();
        if (batchId != null) w.eq(SrtpConclusionReport::getBatchId, batchId);
        w.orderByDesc(SrtpConclusionReport::getCreatedAt);
        List<SrtpConclusionReport> list = conclusionMapper.selectList(w);
        return build("结题报告", cols, list, r -> new String[]{
                nv(r.getSnapshotName()), nv(r.getProjectType()),
                r.getStudentCount() == null ? "" : r.getStudentCount().toString(), st(r.getStatus()), nv(r.getDescription())});
    }

    /** 结题等级汇总：导出该批次下所有结题记录及对应的项目编号、等级、状态 */
    public byte[] exportConclusionGrades(Long adminId, Long batchId) {
        List<Long> orgs = visibleOrgs(adminId);
        var w = new QueryWrapper<SrtpConclusionReport>().lambda();
        if (batchId != null) w.eq(SrtpConclusionReport::getBatchId, batchId);
        w.orderByDesc(SrtpConclusionReport::getCreatedAt);
        List<SrtpConclusionReport> reports = conclusionMapper.selectList(w);

        // 按可见组织过滤：结题挂在项目上，需要先取关联项目再按 org 收窄
        Map<Long, SrtpProject> projectMap = new HashMap<>();
        for (SrtpConclusionReport r : reports) {
            if (r.getProjectId() != null && !projectMap.containsKey(r.getProjectId())) {
                SrtpProject p = projectMapper.selectById(r.getProjectId());
                if (p != null) projectMap.put(r.getProjectId(), p);
            }
        }
        if (orgs != null) {
            reports = reports.stream().filter(r -> {
                SrtpProject p = projectMap.get(r.getProjectId());
                return p != null && orgs.contains(p.getOrgId());
            }).collect(Collectors.toList());
        }

        Map<String, String> GRADE = Map.of(
                "PASS", "同意结题——合格",
                "GOOD", "同意结题——良好",
                "EXCELLENT", "同意结题——优秀");
        String[] cols = {"项目编号", "项目名称", "项目类型", "学生人数", "审批状态", "结题等级", "驳回意见"};
        return build("结题等级汇总", cols, reports, r -> {
            SrtpProject p = projectMap.get(r.getProjectId());
            String grade;
            if (r.getStatus() != null && r.getStatus().name().equals("APPROVED")) {
                grade = GRADE.getOrDefault(nv(r.getConclusionGrade()), "已通过（未标注等级）");
            } else if (r.getStatus() != null && (r.getStatus().name().equals("REJECTED") || r.getStatus().name().equals("RETURNED"))) {
                grade = "不同意结题";
            } else {
                grade = "尚未结题";
            }
            return new String[]{
                    p != null ? nv(p.getProjectNo()) : "",
                    nv(r.getSnapshotName()),
                    nv(r.getProjectType()),
                    r.getStudentCount() == null ? "" : r.getStudentCount().toString(),
                    st(r.getStatus()),
                    grade,
                    nv(r.getRejectReason())
            };
        });
    }

    /** 未提交选题人员：可见组织内的学生(CONTESTANT) 中，未在该批次发起选题者 */
    public byte[] exportUnsubmitted(Long adminId, Long batchId) {
        if (batchId == null) throw new BusinessException("请先选择批次");
        List<Long> orgs = visibleOrgs(adminId);

        var uw = new QueryWrapper<User>().lambda().eq(User::getRole, UserRole.CONTESTANT);
        if (orgs != null) uw.in(User::getOrganizationId, orgs);
        List<User> students = userMapper.selectList(uw);

        var submitted = topicMapper.selectList(new QueryWrapper<SrtpTopic>().lambda()
                        .eq(SrtpTopic::getBatchId, batchId).isNotNull(SrtpTopic::getInitiatorId))
                .stream().map(SrtpTopic::getInitiatorId).collect(Collectors.toSet());

        Map<Long, String> orgNames = new HashMap<>();
        List<User> unsubmitted = students.stream().filter(u -> !submitted.contains(u.getId())).toList();
        String[] cols = {"姓名", "用户名", "所属单位", "手机号"};
        return build("未提交选题人员", cols, unsubmitted, u -> new String[]{
                nv(u.getRealName()), nv(u.getUsername()),
                orgNames.computeIfAbsent(u.getOrganizationId(), this::orgName), nv(u.getPhone())});
    }

    // ===== 内部 =====

    private interface RowMapper<T> { String[] apply(T item); }

    private <T> byte[] build(String sheetName, String[] cols, List<T> list, RowMapper<T> mapper) {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet(sheetName);
            CellStyle head = headerStyle(wb);
            Row header = sheet.createRow(0);
            for (int i = 0; i < cols.length; i++) {
                Cell c = header.createCell(i); c.setCellValue(cols[i]); c.setCellStyle(head);
            }
            for (int r = 0; r < list.size(); r++) {
                Row row = sheet.createRow(r + 1);
                String[] vals = mapper.apply(list.get(r));
                for (int i = 0; i < vals.length; i++) row.createCell(i).setCellValue(vals[i] == null ? "" : vals[i]);
            }
            for (int i = 0; i < cols.length; i++) sheet.setColumnWidth(i, 20 * 256);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            wb.write(bos);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("导出失败: " + e.getMessage());
        }
    }

    private CellStyle headerStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        s.setFont(f);
        s.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return s;
    }

    /** 可见组织；超级管理员返回 null(不限) */
    private List<Long> visibleOrgs(Long adminId) {
        User u = adminId == null ? null : userMapper.selectById(adminId);
        if (u == null) throw new BusinessException(403, "无权操作");
        if (AdminLevelResolver.isSuperAdmin(u)) return null;
        List<Long> v = AdminLevelResolver.getVisibleOrgIds(u, organizationMapper);
        return v.isEmpty() ? List.of(-1L) : v;
    }

    private String orgName(Long orgId) {
        if (orgId == null) return "";
        var o = organizationMapper.selectById(orgId);
        return o != null ? o.getName() : "";
    }

    private String nv(String s) { return s == null ? "" : s; }
}
