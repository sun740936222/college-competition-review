package com.zhk.contest.training.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.zhk.contest.dto.response.ApiResponse;
import com.zhk.contest.entity.User;
import com.zhk.contest.exception.BusinessException;
import com.zhk.contest.mapper.UserMapper;
import com.zhk.contest.training.entity.SrtpMemberRole;
import com.zhk.contest.training.mapper.SrtpMemberRoleMapper;
import com.zhk.contest.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 大创账号自助操作（已登录）：修改密码（同时清除首次登录改密标记）。
 * 路径不在 /api/srtp/auth/** 白名单内，故需登录。
 */
@RestController
@RequestMapping("/api/srtp/account")
public class SrtpAccountController {

    private final UserMapper userMapper;
    private final SrtpMemberRoleMapper memberRoleMapper;

    public SrtpAccountController(UserMapper userMapper, SrtpMemberRoleMapper memberRoleMapper) {
        this.userMapper = userMapper;
        this.memberRoleMapper = memberRoleMapper;
    }

    @PostMapping("/change-password")
    @Transactional
    public ApiResponse<Void> changePassword(@RequestBody Map<String, String> body, HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("userId");
        if (userId == null) throw new BusinessException(401, "请先登录");
        String oldPwd = body.get("oldPassword");
        String newPwd = body.get("newPassword");
        if (newPwd == null || newPwd.length() < 6) throw new BusinessException("新密码至少6位");

        User u = userMapper.selectById(userId);
        if (u == null) throw new BusinessException("用户不存在");
        if (oldPwd == null || !SecurityUtils.hashPassword(oldPwd).equals(u.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        u.setPassword(SecurityUtils.hashPassword(newPwd));
        userMapper.updateById(u);
        // 清除首次登录改密标记
        memberRoleMapper.update(null, new UpdateWrapper<SrtpMemberRole>().lambda()
                .eq(SrtpMemberRole::getUserId, userId)
                .set(SrtpMemberRole::getMustChangePassword, false));
        return ApiResponse.success("密码修改成功", null);
    }
}
