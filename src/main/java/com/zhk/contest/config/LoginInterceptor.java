package com.zhk.contest.config;

import com.zhk.contest.config.TokenService.TokenInfo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    private final TokenService tokenService;

    public LoginInterceptor(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        boolean isAdminPath = request.getRequestURI().contains("/admin/");
        String key = isAdminPath ? "adminUserId" : "userId";

        // Try token-based auth first
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            TokenInfo info = tokenService.getTokenInfo(token);
            if (info != null) {
                Long id = isAdminPath ? info.adminUserId() : info.userId();
                if (id != null) {
                    request.setAttribute("userId", info.userId());
                    request.setAttribute("adminUserId", info.adminUserId());
                    return true;
                }
            }
        }

        // Fall back to session-based auth
        var session = request.getSession(false);
        if (session != null && session.getAttribute(key) != null) {
            request.setAttribute("userId", session.getAttribute("userId"));
            request.setAttribute("adminUserId", session.getAttribute("adminUserId"));
            return true;
        }

        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"请先登录\",\"data\":null}");
        return false;
    }
}
