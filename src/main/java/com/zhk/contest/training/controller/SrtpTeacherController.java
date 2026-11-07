package com.zhk.contest.training.controller;

import com.zhk.contest.dto.response.ApiResponse;
import com.zhk.contest.training.service.SrtpTeacherService;
import com.zhk.contest.training.service.SrtpTeacherService.ImportResult;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/** 教师管理（管理员后台）。 */
@RestController
@RequestMapping("/api/srtp/admin/teachers")
public class SrtpTeacherController {

    private final SrtpTeacherService teacherService;

    public SrtpTeacherController(SrtpTeacherService teacherService) {
        this.teacherService = teacherService;
    }

    private Long adminId(HttpServletRequest req) {
        return (Long) req.getAttribute("adminUserId");
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list(@RequestParam(required = false) String keyword,
                                                       HttpServletRequest req) {
        return ApiResponse.success(teacherService.listTeachers(adminId(req), keyword));
    }

    @PostMapping
    public ApiResponse<String> create(@RequestBody Map<String, String> body, HttpServletRequest req) {
        String pwd = teacherService.createTeacher(adminId(req), body.get("name"), body.get("workId"));
        return ApiResponse.success("创建成功，初始密码为工号：" + pwd, pwd);
    }

    @PostMapping("/import")
    public ApiResponse<ImportResult> importTeachers(@RequestParam("file") MultipartFile file, HttpServletRequest req) {
        ImportResult r = teacherService.importTeachers(adminId(req), file);
        return ApiResponse.success("导入完成：成功 " + r.success() + "，跳过(工号已存在) " + r.skipped() + "，失败 " + r.fail(), r);
    }

    @DeleteMapping("/{userId}")
    public ApiResponse<Void> delete(@PathVariable Long userId, HttpServletRequest req) {
        teacherService.deleteTeacher(adminId(req), userId);
        return ApiResponse.success("已删除", null);
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> template() {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + URLEncoder.encode("教师导入模板.xlsx", StandardCharsets.UTF_8))
                .body(teacherService.template());
    }
}
