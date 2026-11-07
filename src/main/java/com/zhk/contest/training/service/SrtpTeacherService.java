package com.zhk.contest.training.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zhk.contest.entity.User;
import com.zhk.contest.enums.OrganizationLevel;
import com.zhk.contest.enums.UserRole;
import com.zhk.contest.exception.BusinessException;
import com.zhk.contest.mapper.OrganizationMapper;
import com.zhk.contest.mapper.UserMapper;
import com.zhk.contest.training.entity.SrtpMemberRole;
import com.zhk.contest.training.entity.SrtpProjectAdvisor;
import com.zhk.contest.training.entity.SrtpTopic;
import com.zhk.contest.training.enums.SrtpRoleType;
import com.zhk.contest.training.mapper.SrtpMemberRoleMapper;
import com.zhk.contest.training.mapper.SrtpProjectAdvisorMapper;
import com.zhk.contest.training.mapper.SrtpTopicMapper;
import com.zhk.contest.util.AdminLevelResolver;
import com.zhk.contest.util.SecurityUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 教师管理（管理员）：单个新建 / Excel 批量导入（第一列姓名、第二列工号）/ 列表。
 * 登录用户名=工号（全库唯一，解决同名同姓）；初始密码=工号；创建即置 must_change_password=1（首次登录提示改密）。
 */
@Service
public class SrtpTeacherService {

    private final UserMapper userMapper;
    private final SrtpMemberRoleMapper memberRoleMapper;
    private final SrtpTopicMapper topicMapper;
    private final SrtpProjectAdvisorMapper advisorMapper;
    private final OrganizationMapper organizationMapper;

    public SrtpTeacherService(UserMapper userMapper, SrtpMemberRoleMapper memberRoleMapper,
                              SrtpTopicMapper topicMapper, SrtpProjectAdvisorMapper advisorMapper,
                              OrganizationMapper organizationMapper) {
        this.userMapper = userMapper;
        this.memberRoleMapper = memberRoleMapper;
        this.topicMapper = topicMapper;
        this.advisorMapper = advisorMapper;
        this.organizationMapper = organizationMapper;
    }

    public record ImportResult(int success, int skipped, int fail, List<String> errors) {}

    /** 单个新建教师；返回初始密码（=工号）供告知 */
    @Transactional
    public String createTeacher(Long adminId, String name, String workId) {
        User admin = requireAdmin(adminId);
        if (name == null || name.isBlank() || workId == null || workId.isBlank()) {
            throw new BusinessException("姓名和工号不能为空");
        }
        if (userMapper.existsByUsername(workId.trim())) {
            throw new BusinessException("工号已存在：" + workId);
        }
        provision(name.trim(), workId.trim(), admin.getOrganizationId());
        return workId.trim();
    }

    /** Excel 批量导入：第一列姓名、第二列工号（首行为表头，跳过） */
    @Transactional
    public ImportResult importTeachers(Long adminId, MultipartFile file) {
        User admin = requireAdmin(adminId);
        int success = 0, skipped = 0, fail = 0;
        List<String> errors = new ArrayList<>();
        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                String name = cell(row, 0);
                String workId = cell(row, 1);
                if (name.isBlank() && workId.isBlank()) continue;
                if (name.isBlank() || workId.isBlank()) {
                    fail++; errors.add("第" + (r + 1) + "行：姓名或工号为空"); continue;
                }
                if (userMapper.existsByUsername(workId)) {
                    skipped++; errors.add("第" + (r + 1) + "行：工号 " + workId + " 已存在，跳过"); continue;
                }
                provision(name, workId, admin.getOrganizationId());
                success++;
            }
        } catch (IOException e) {
            throw new BusinessException("文件解析失败: " + e.getMessage());
        }
        return new ImportResult(success, skipped, fail, errors);
    }

    /** 教师列表（管理员可见组织范围内） */
    public List<Map<String, Object>> listTeachers(Long adminId, String keyword) {
        User admin = requireAdmin(adminId);
        var w = new QueryWrapper<SrtpMemberRole>().lambda()
                .eq(SrtpMemberRole::getRoleType, SrtpRoleType.TEACHER);
        if (!AdminLevelResolver.isSuperAdmin(admin)) {
            List<Long> visible = AdminLevelResolver.getVisibleOrgIds(admin, organizationMapper);
            if (!visible.isEmpty()) w.in(SrtpMemberRole::getOrgId, visible);
        }
        List<SrtpMemberRole> roles = memberRoleMapper.selectList(w);
        List<Map<String, Object>> result = new ArrayList<>();
        for (SrtpMemberRole role : roles) {
            User u = userMapper.selectById(role.getUserId());
            if (u == null) continue;
            if (keyword != null && !keyword.isBlank()
                    && !(safe(u.getRealName()).contains(keyword) || safe(u.getUsername()).contains(keyword))) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("userId", u.getId());
            m.put("workId", u.getUsername());
            m.put("name", u.getRealName());
            m.put("phone", u.getPhone());
            m.put("mustChangePassword", Boolean.TRUE.equals(role.getMustChangePassword()));
            result.add(m);
        }
        return result;
    }

    /** 删除教师账号（连同其大创教师身份）；名下有出的题目、或已被绑定为某立项/中期/结题指导教师时禁止删除 */
    @Transactional
    public void deleteTeacher(Long adminId, Long userId) {
        requireAdmin(adminId);
        long topics = topicMapper.selectCount(new QueryWrapper<SrtpTopic>().lambda()
                .eq(SrtpTopic::getTeacherId, userId));
        if (topics > 0) {
            throw new BusinessException("该教师名下还有 " + topics + " 个出的题目，请先在“选题管理”中删除这些题目，再删除该教师");
        }
        long advised = advisorMapper.selectCount(new QueryWrapper<SrtpProjectAdvisor>().lambda()
                .eq(SrtpProjectAdvisor::getUserId, userId));
        if (advised > 0) {
            throw new BusinessException("该教师已被 " + advised + " 个立项/中期/结题记录选为指导教师，删除会导致这些记录的“指导教师审核”环节无人可审，请先在对应记录中更换指导教师后再删除该教师");
        }
        memberRoleMapper.delete(new QueryWrapper<SrtpMemberRole>().lambda()
                .eq(SrtpMemberRole::getUserId, userId));
        userMapper.deleteById(userId);
    }

    /** 供学生选择指导教师的检索（任意登录用户可用，按姓名/工号模糊匹配，最多50条） */
    public List<Map<String, Object>> searchForPicker(String keyword) {
        List<SrtpMemberRole> roles = memberRoleMapper.selectList(
                new QueryWrapper<SrtpMemberRole>().lambda()
                        .eq(SrtpMemberRole::getRoleType, SrtpRoleType.TEACHER)
                        .eq(SrtpMemberRole::getStatus, "ACTIVE"));
        List<Map<String, Object>> result = new ArrayList<>();
        for (SrtpMemberRole role : roles) {
            User u = userMapper.selectById(role.getUserId());
            if (u == null) continue;
            if (keyword != null && !keyword.isBlank()
                    && !(safe(u.getRealName()).contains(keyword) || safe(u.getUsername()).contains(keyword))) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("userId", u.getId());
            m.put("name", u.getRealName());
            m.put("workId", u.getUsername());
            result.add(m);
            if (result.size() >= 50) break;
        }
        return result;
    }

    public byte[] template() {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("教师导入模板");
            Row header = sheet.createRow(0);
            CellStyle style = wb.createCellStyle();
            Font f = wb.createFont(); f.setBold(true); style.setFont(f);
            String[] cols = {"姓名", "工号"};
            for (int i = 0; i < cols.length; i++) {
                Cell c = header.createCell(i); c.setCellValue(cols[i]); c.setCellStyle(style);
                sheet.setColumnWidth(i, 20 * 256);
            }
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            wb.write(bos);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("生成模板失败: " + e.getMessage());
        }
    }

    // ===== 内部 =====

    /** 创建 user(用户名=工号,初始密码=工号) + TEACHER 身份(须改密) */
    private void provision(String name, String workId, Long orgId) {
        User u = new User();
        u.setUsername(workId);
        u.setPassword(SecurityUtils.hashPassword(workId)); // 初始密码=工号
        u.setRealName(name);
        u.setRole(UserRole.CONTESTANT);
        u.setOrganizationId(orgId);
        userMapper.insert(u);

        SrtpMemberRole role = new SrtpMemberRole();
        role.setUserId(u.getId());
        role.setRoleType(SrtpRoleType.TEACHER);
        role.setOrgId(orgId);
        role.setStatus("ACTIVE");
        role.setMustChangePassword(true);
        memberRoleMapper.insert(role);
    }

    private String cell(Row row, int idx) {
        Cell c = row.getCell(idx);
        if (c == null) return "";
        return switch (c.getCellType()) {
            case STRING -> c.getStringCellValue().trim();
            case NUMERIC -> {
                double d = c.getNumericCellValue();
                yield (d == Math.floor(d)) ? String.valueOf((long) d) : String.valueOf(d);
            }
            default -> "";
        };
    }

    private String safe(String s) { return s == null ? "" : s; }

    private User requireAdmin(Long adminId) {
        User u = (adminId == null) ? null : userMapper.selectById(adminId);
        if (u == null) throw new BusinessException(403, "无权操作");
        AdminLevelResolver.requireAtLeast(u, OrganizationLevel.COLLEGE, organizationMapper);
        return u;
    }
}
