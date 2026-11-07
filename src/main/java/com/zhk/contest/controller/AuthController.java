package com.zhk.contest.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhk.contest.config.TokenService;
import com.zhk.contest.dto.request.ChangePasswordRequest;
import com.zhk.contest.dto.request.CreateJudgeRequest;
import com.zhk.contest.dto.request.LoginRequest;
import com.zhk.contest.dto.request.ProfileUpdateRequest;
import com.zhk.contest.dto.request.RegisterNationalRequest;
import com.zhk.contest.dto.request.RegisterProvinceRequest;
import com.zhk.contest.dto.request.RegisterRequest;
import com.zhk.contest.dto.request.RegisterSchoolRequest;
import com.zhk.contest.dto.response.ApiResponse;
import com.zhk.contest.dto.response.LoginResponse;
import com.zhk.contest.entity.User;
import com.zhk.contest.service.AuthService;

import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final TokenService tokenService;

    public AuthController(AuthService authService, TokenService tokenService) {
        this.authService = authService;
        this.tokenService = tokenService;
    }

    @PostMapping("/register")
    public ApiResponse<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        LoginResponse resp = authService.register(request);
        resp.setToken(tokenService.createToken(resp.getUserId(), resp.getRole()));
        return ApiResponse.success(resp);
    }

    @PostMapping("/register/national")
    public ApiResponse<LoginResponse> registerNational(@Valid @RequestBody RegisterNationalRequest request) {
        LoginResponse resp = authService.registerNational(request);
        resp.setToken(tokenService.createToken(resp.getUserId(), resp.getRole()));
        return ApiResponse.success(resp);
    }

    @PostMapping("/register/province")
    public ApiResponse<LoginResponse> registerProvince(@Valid @RequestBody RegisterProvinceRequest request) {
        LoginResponse resp = authService.registerProvince(request);
        resp.setToken(tokenService.createToken(resp.getUserId(), resp.getRole()));
        return ApiResponse.success(resp);
    }

    @PostMapping("/register/school")
    public ApiResponse<LoginResponse> registerSchool(@Valid @RequestBody RegisterSchoolRequest request) {
        LoginResponse resp = authService.registerSchool(request);
        resp.setToken(tokenService.createToken(resp.getUserId(), resp.getRole()));
        return ApiResponse.success(resp);
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse resp = authService.login(request);
        resp.setToken(tokenService.createToken(resp.getUserId(), resp.getRole()));
        return ApiResponse.success(resp);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            tokenService.removeToken(authHeader.substring(7));
        }
        return ApiResponse.success(null);
    }

    @GetMapping("/me")
    public ApiResponse<LoginResponse> me(@RequestParam(required = false) String role, HttpServletRequest request) {
        Long userId = "admin".equals(role)
                ? (Long) request.getAttribute("adminUserId")
                : (Long) request.getAttribute("userId");
        if (userId == null) return ApiResponse.error(401, "未登录");
        return ApiResponse.success(authService.getMe(userId));
    }

    @PutMapping("/profile")
    public ApiResponse<LoginResponse> updateProfile(@RequestBody ProfileUpdateRequest request, HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("userId");
        return ApiResponse.success(authService.updateProfile(userId, request.getOrganizationId(), request.getOrganizationName()));
    }

    // ========== Password Management ==========

    @PutMapping("/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request, HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("userId");
        authService.changePassword(userId, request.getOldPassword(), request.getNewPassword());
        return ApiResponse.success(null);
    }

    // ========== Judge Management (Admin) ==========

    @PostMapping("/judges")
    public ApiResponse<LoginResponse> createJudge(@RequestBody CreateJudgeRequest request, HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("adminUserId");
        return ApiResponse.success(authService.createJudge(userId, request.getUsername(), request.getPassword(),
                request.getRealName(), request.getPhone(), request.getJudgeOrganization()));
    }

    @GetMapping("/judges")
    public ApiResponse<IPage<User>> listJudges(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("adminUserId");
        return ApiResponse.success(authService.listJudges(userId, new Page<>(current, size)));
    }

    @DeleteMapping("/judges/{judgeId}")
    public ApiResponse<Void> deleteJudge(@PathVariable Long judgeId, HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("adminUserId");
        authService.deleteJudge(userId, judgeId);
        return ApiResponse.success(null);
    }
}
