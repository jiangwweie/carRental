package com.carrental.common.security;

import com.carrental.common.result.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":4003,\"data\":null,\"message\":\"未登录/Token无效\"}");
            return false;
        }

        String token = authHeader.substring(7);
        if (jwtUtil.isTokenExpired(token)) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":4003,\"data\":null,\"message\":\"Token已过期\"}");
            return false;
        }

        // 将用户信息存入 request attribute 供后续使用
        Long userId = jwtUtil.getUserId(token);
        String role = jwtUtil.getRole(token);
        request.setAttribute("userId", userId);
        request.setAttribute("role", role);

        return true;
    }
}
