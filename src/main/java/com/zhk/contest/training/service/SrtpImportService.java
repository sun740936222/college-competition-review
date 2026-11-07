package com.zhk.contest.training.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zhk.contest.entity.User;
import com.zhk.contest.exception.BusinessException;
import com.zhk.contest.mapper.UserMapper;
import com.zhk.contest.training.dto.SrtpAdvisorDTO;
import com.zhk.contest.training.dto.SrtpMemberDTO;
import com.zhk.contest.training.entity.SrtpDiscipline;
import com.zhk.contest.training.entity.SrtpMemberRole;
import com.zhk.contest.training.entity.SrtpProject;
import com.zhk.contest.training.entity.SrtpTopic;
import com.zhk.contest.training.enums.ClaimStatus;
import com.zhk.contest.training.enums.SrtpMemberKind;
import com.zhk.contest.training.enums.SrtpRecordStatus;
import com.zhk.contest.training.enums.SrtpRoleType;
import com.zhk.contest.training.enums.SrtpStage;
import com.zhk.contest.training.enums.TopicSource;
import com.zhk.contest.training.mapper.SrtpDisciplineMapper;
import com.zhk.contest.training.mapper.SrtpMemberRoleMapper;
import com.zhk.contest.training.mapper.SrtpProjectMapper;
import com.zhk.contest.training.mapper.SrtpTopicMapper;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Excel 批量导入（选题=老师出题 / 立项）+ 模板下载。逐行校验，返回成功/失败统计与错误明细。 */
@Service
public class SrtpImportService {

    private final SrtpTopicMapper topicMapper;
    private final SrtpProjectMapper projectMapper;
    private final SrtpDisciplineMapper disciplineMapper;
    private final SrtpMemberRoleMapper memberRoleMapper;
    private final SrtpParticipantService participantService;
    private final UserMapper userMapper;

    private static final String[] TOPIC_COLS = {"选题名称", "出题教师姓名", "出题教师工号", "项目类别(A/B/C)", "一级学科", "二级学科", "创意研究类型", "简介"};
    private static final String[] PROJECT_COLS = {"项目名称", "项目类别(A/B/C)", "指导教师姓名", "指导教师工号", "负责人姓名", "负责人学号", "一级学科", "二级学科", "学生人数", "简介"};

    public SrtpImportService(SrtpTopicMapper topicMapper, SrtpProjectMapper projectMapper,
                             SrtpDisciplineMapper disciplineMapper, SrtpMemberRoleMapper memberRoleMapper,
                             SrtpParticipantService participantService, UserMapper userMapper) {
        this.topicMapper = topicMapper;
        this.projectMapper = projectMapper;
        this.disciplineMapper = disciplineMapper;
        this.memberRoleMapper = memberRoleMapper;
        this.participantService = participantService;
        this.userMapper = userMapper;
    }

    /** 导入结果 */
    public record ImportResult(int success, int fail, List<String> errors) {}

    /**
     * 选题导入（老师出题）：列= 选题名称 | 出题教师工号 | 项目类别(A/B/C) | 一级学科 | 二级学科 | 创意研究类型 | 简介。
     * 按工号解析出题教师并绑定；生成"可选题库"(SCHOOL_ASSIGNED, 开放认领)。
     */
    @Transactional
    public ImportResult importTopics(Long adminId, Long batchId, MultipartFile file) {
        if (batchId == null) throw new BusinessException("请先选择批次");
        Long adminOrg = orgOf(adminId);
        Map<String, Long> dn = disciplineNameToId();
        int success = 0, fail = 0;
        List<String> errors = new ArrayList<>();

        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (isEmptyRow(row)) continue;
                String name = cell(row, 0);
                String teacherName = cell(row, 1);
                String workId = cell(row, 2);
                if (name.isBlank()) { fail++; errors.add("第" + (r + 1) + "行：选题名称为空，已跳过"); continue; }
                if (workId.isBlank()) { fail++; errors.add("第" + (r + 1) + "行：出题教师工号为空，已跳过"); continue; }
                User teacher = findTeacher(workId);
                if (teacher == null) { fail++; errors.add("第" + (r + 1) + "行：工号 " + workId + " 不是有效的教师账号，已跳过"); continue; }
                // 姓名与工号一致性校验（防止填错工号）
                if (!teacherName.isBlank() && teacher.getRealName() != null
                        && !teacherName.equals(teacher.getRealName())) {
                    fail++;
                    errors.add("第" + (r + 1) + "行：工号 " + workId + " 对应教师为「" + teacher.getRealName()
                            + "」，与填写的姓名「" + teacherName + "」不一致，已跳过");
                    continue;
                }

                SrtpTopic t = new SrtpTopic();
                t.setBatchId(batchId);
                t.setOrgId(teacher.getOrganizationId() != null ? teacher.getOrganizationId() : adminOrg);
                t.setSource(TopicSource.SCHOOL_ASSIGNED);
                t.setTeacherId(teacher.getId());
                t.setInitiatorId(teacher.getId());
                t.setName(name);
                t.setProjectType(nullIfBlank(cell(row, 3)));
                t.setDisciplineL1Id(dn.get(cell(row, 4)));
                t.setDisciplineL2Id(dn.get(cell(row, 5)));
                t.setResearchType(nullIfBlank(cell(row, 6)));
                t.setInfo(nullIfBlank(cell(row, 7)));
                t.setStatus(SrtpRecordStatus.APPROVED);
                t.setCurrentStep(0);
                t.setClaimStatus(ClaimStatus.OPEN);
                topicMapper.insert(t);
                success++;
            }
        } catch (IOException e) {
            throw new BusinessException("文件解析失败: " + e.getMessage());
        }
        return new ImportResult(success, fail, errors);
    }

    /** 按工号查找教师账号（须具备 TEACHER 身份） */
    private User findTeacher(String workId) {
        User u = userMapper.selectOne(new QueryWrapper<User>().lambda()
                .eq(User::getUsername, workId).last("LIMIT 1"));
        if (u == null) return null;
        long isTeacher = memberRoleMapper.selectCount(new QueryWrapper<SrtpMemberRole>().lambda()
                .eq(SrtpMemberRole::getUserId, u.getId())
                .eq(SrtpMemberRole::getRoleType, SrtpRoleType.TEACHER));
        return isTeacher > 0 ? u : null;
    }

    /** 软解析学生账号：按学号(=用户名)找到则返回 user.id，否则 null（学生不强制有账号） */
    private Long resolveStudentId(String studentNo) {
        if (studentNo == null || studentNo.isBlank()) return null;
        User u = userMapper.selectOne(new QueryWrapper<User>().lambda()
                .eq(User::getUsername, studentNo).last("LIMIT 1"));
        return u != null ? u.getId() : null;
    }

    @Transactional
    public ImportResult importProjects(Long adminId, Long batchId, MultipartFile file) {
        if (batchId == null) throw new BusinessException("请先选择批次");
        Long orgId = orgOf(adminId);
        Map<String, Long> dn = disciplineNameToId();
        int success = 0, fail = 0;
        List<String> errors = new ArrayList<>();

        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (isEmptyRow(row)) continue;
                String name = cell(row, 0);
                if (name.isBlank()) { fail++; errors.add("第" + (r + 1) + "行：项目名称为空，已跳过"); continue; }

                // 指导教师（可选；填了则校验工号+姓名一致，并绑定为指导教师）
                String teacherName = cell(row, 2);
                String teacherWorkId = cell(row, 3);
                User teacher = null;
                if (!teacherWorkId.isBlank()) {
                    teacher = findTeacher(teacherWorkId);
                    if (teacher == null) {
                        fail++; errors.add("第" + (r + 1) + "行：指导教师工号 " + teacherWorkId + " 不是有效教师账号，已跳过"); continue;
                    }
                    if (!teacherName.isBlank() && teacher.getRealName() != null && !teacherName.equals(teacher.getRealName())) {
                        fail++; errors.add("第" + (r + 1) + "行：工号 " + teacherWorkId + " 对应教师为「" + teacher.getRealName()
                                + "」，与填写姓名「" + teacherName + "」不一致，已跳过"); continue;
                    }
                }

                SrtpProject p = new SrtpProject();
                p.setBatchId(batchId);
                p.setOrgId(orgId);
                p.setCreatedBy(adminId);
                p.setName(name);
                p.setProjectType(nullIfBlank(cell(row, 1)));
                p.setDisciplineL1Id(dn.get(cell(row, 6)));
                p.setDisciplineL2Id(dn.get(cell(row, 7)));
                p.setStudentCount(parseInt(cell(row, 8)));
                p.setDescription(nullIfBlank(cell(row, 9)));
                p.setStatus(SrtpRecordStatus.DRAFT);
                p.setCurrentStep(0);
                projectMapper.insert(p);

                // 负责人（学生）
                String leaderName = cell(row, 4);
                String leaderNo = cell(row, 5);
                if (!leaderName.isBlank() || !leaderNo.isBlank()) {
                    SrtpMemberDTO leader = new SrtpMemberDTO();
                    leader.setName(nullIfBlank(leaderName));
                    leader.setStudentNo(nullIfBlank(leaderNo));
                    leader.setMemberRole(SrtpMemberKind.LEADER);
                    leader.setUserId(resolveStudentId(leaderNo));
                    participantService.replaceMembers(SrtpStage.PROJECT, p.getId(), List.of(leader));
                }
                // 指导教师
                if (teacher != null) {
                    SrtpAdvisorDTO adv = new SrtpAdvisorDTO();
                    adv.setUserId(teacher.getId());
                    adv.setName(teacher.getRealName());
                    participantService.replaceAdvisors(SrtpStage.PROJECT, p.getId(), List.of(adv));
                }
                success++;
            }
        } catch (IOException e) {
            throw new BusinessException("文件解析失败: " + e.getMessage());
        }
        return new ImportResult(success, fail, errors);
    }

    public byte[] topicTemplate() { return template("选题导入模板", TOPIC_COLS); }
    public byte[] projectTemplate() { return template("立项导入模板", PROJECT_COLS); }

    // ===== 内部 =====

    private byte[] template(String sheetName, String[] cols) {
        try (Workbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            Sheet sheet = wb.createSheet(sheetName);
            Row header = sheet.createRow(0);
            CellStyle style = wb.createCellStyle();
            Font f = wb.createFont(); f.setBold(true); style.setFont(f);
            for (int i = 0; i < cols.length; i++) {
                Cell c = header.createCell(i);
                c.setCellValue(cols[i]);
                c.setCellStyle(style);
                sheet.setColumnWidth(i, 20 * 256);
            }
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            wb.write(bos);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("生成模板失败: " + e.getMessage());
        }
    }

    private Map<String, Long> disciplineNameToId() {
        return disciplineMapper.selectList(null).stream()
                .collect(Collectors.toMap(SrtpDiscipline::getName, SrtpDiscipline::getId, (a, b) -> a));
    }

    private String cell(Row row, int idx) {
        if (row == null) return "";
        Cell c = row.getCell(idx);
        if (c == null) return "";
        return switch (c.getCellType()) {
            case STRING -> c.getStringCellValue().trim();
            case NUMERIC -> {
                double d = c.getNumericCellValue();
                yield (d == Math.floor(d)) ? String.valueOf((long) d) : String.valueOf(d);
            }
            case BOOLEAN -> String.valueOf(c.getBooleanCellValue());
            default -> "";
        };
    }

    private boolean isEmptyRow(Row row) {
        if (row == null) return true;
        for (int i = 0; i < 8; i++) if (!cell(row, i).isBlank()) return false;
        return true;
    }

    private String nullIfBlank(String s) { return (s == null || s.isBlank()) ? null : s; }

    private Integer parseInt(String s) {
        try { return s == null || s.isBlank() ? null : (int) Double.parseDouble(s); }
        catch (NumberFormatException e) { return null; }
    }

    private Long orgOf(Long userId) {
        User u = userId == null ? null : userMapper.selectById(userId);
        return u != null ? u.getOrganizationId() : null;
    }
}
