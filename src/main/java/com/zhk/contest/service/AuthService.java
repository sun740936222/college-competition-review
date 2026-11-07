package com.zhk.contest.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhk.contest.dto.request.LoginRequest;
import com.zhk.contest.dto.request.RegisterNationalRequest;
import com.zhk.contest.dto.request.RegisterProvinceRequest;
import com.zhk.contest.dto.request.RegisterRequest;
import com.zhk.contest.dto.request.RegisterSchoolRequest;
import com.zhk.contest.dto.response.LoginResponse;
import com.zhk.contest.entity.User;

import java.util.List;

public interface AuthService {
    LoginResponse register(RegisterRequest request);
    LoginResponse registerNational(RegisterNationalRequest request);
    LoginResponse registerProvince(RegisterProvinceRequest request);
    LoginResponse registerSchool(RegisterSchoolRequest request);
    LoginResponse login(LoginRequest request);
    LoginResponse getMe(Long userId);
    LoginResponse updateProfile(Long userId, Long organizationId, String organizationName);

    // Admin: judge management
    LoginResponse createJudge(Long adminUserId, String username, String password, String realName, String phone, String judgeOrganization);
    IPage<User> listJudges(Long adminUserId, Page<User> page);
    void deleteJudge(Long adminUserId, Long judgeId);

    // Password management
    void changePassword(Long userId, String oldPassword, String newPassword);
}
