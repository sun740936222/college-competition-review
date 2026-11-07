package com.zhk.contest.training.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zhk.contest.config.TokenService;
import com.zhk.contest.dto.request.LoginRequest;
import com.zhk.contest.dto.response.ApiResponse;
import com.zhk.contest.entity.User;
import com.zhk.contest.exception.BusinessException;
import com.zhk.contest.mapper.UserMapper;
import com.zhk.contest.training.dto.response.SrtpLoginResponse;
import com.zhk.contest.training.entity.SrtpMemberRole;
import com.zhk.contest.training.enums.SrtpRoleType;
import com.zhk.contest.training.mapper.SrtpMemberRoleMapper;
import com.zhk.contest.training.service.SrtpRoleService;
import com.zhk.contest.util.SecurityUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 大创身份认证：教师/专家登录（独立登录身份，不复用"参赛者"）。
 * 教师账号由管理员在"教师管理"中创建/批量导入，不再提供自助注册。
 * 公开路径（拦截器白名单 /api/srtp/auth/**）。
 */
@Controller
@ResponseBody
@RequestMapping("/api/srtp/auth")
public class SrtpAuthController {

    private final UserMapper userMapper;
    private final SrtpMemberRoleMapper memberRoleMapper;
    private final SrtpRoleService roleService;
    private final TokenService tokenService;

    public SrtpAuthController(UserMapper userMapper, SrtpMemberRoleMapper memberRoleMapper,
                             SrtpRoleService roleService, TokenService tokenService) {
        this.userMapper = userMapper;
        this.memberRoleMapper = memberRoleMapper;
        this.roleService = roleService;
        this.tokenService = tokenService;
    }

    /** 教师/专家登录：校验账号密码 + 校验确有教师/专家身份，返回令牌 */
    @PostMapping("/teacher-login")
    public ApiResponse<SrtpLoginResponse> teacherLogin(@RequestBody LoginRequest req) {
        if (req.getUsername() == null || req.getUsername().isBlank()
                || req.getPassword() == null || req.getPassword().isBlank()) {
            throw new BusinessException("用户名和密码不能为空");
        }
        User u = userMapper.selectOne(new QueryWrapper<User>().lambda()
                .eq(User::getUsername, req.getUsername()).last("LIMIT 1"));
        if (u == null || !SecurityUtils.hashPassword(req.getPassword()).equals(u.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        boolean canApprove = roleService.hasRole(u.getId(), SrtpRoleType.TEACHER)
                || roleService.isExpert(u.getId(), com.zhk.contest.training.enums.ExpertLevel.COLLEGE)
                || roleService.isExpert(u.getId(), com.zhk.contest.training.enums.ExpertLevel.SCHOOL);
        if (!canApprove) {
            throw new BusinessException("该账号不是教师/专家身份，无法以教师身份登录");
        }
        String token = tokenService.createToken(u.getId(), "TEACHER");
        SrtpLoginResponse resp = new SrtpLoginResponse();
        resp.setUserId(u.getId());
        resp.setUsername(u.getUsername());
        resp.setRealName(u.getRealName());
        resp.setRole("TEACHER");
        resp.setPhone(u.getPhone());
        resp.setEmail(u.getEmail());
        resp.setOrganizationId(u.getOrganizationId());
        resp.setToken(token);
        resp.setMustChangePassword(hasMustChange(u.getId()));
        return ApiResponse.success("登录成功", resp);
    }

    private boolean hasMustChange(Long userId) {
        return memberRoleMapper.selectCount(new QueryWrapper<SrtpMemberRole>().lambda()
                .eq(SrtpMemberRole::getUserId, userId)
                .eq(SrtpMemberRole::getMustChangePassword, true)) > 0;
    }
}
