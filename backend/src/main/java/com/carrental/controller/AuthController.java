package com.carrental.controller;

import com.carrental.application.auth.AuthService;
import com.carrental.application.auth.WxLoginCommand;
import com.carrental.common.result.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 微信小程序登录
     */
    @PostMapping("/wx-login")
    public ApiResponse<AuthService.LoginResult> wxLogin(@Valid @RequestBody WxLoginCommand command) {
        AuthService.LoginResult result = authService.wxLogin(command);
        return ApiResponse.success(result);
    }

    /**
     * 模拟登录（Sprint 1 开发用，跳过微信流程）
     */
    @PostMapping("/mock-login")
    public ApiResponse<AuthService.LoginResult> mockLogin(@RequestBody MockLoginRequest request) {
        String role = request.getRole() != null ? request.getRole() : "user";
        AuthService.LoginResult result = authService.mockLogin(role);
        return ApiResponse.success(result);
    }

    /**
     * PC 管理端登录
     */
    @PostMapping("/admin-login")
    public ApiResponse<AuthService.LoginResult> adminLogin(@RequestBody Map<String, String> request) {
        String password = request.get("password");
        AuthService.LoginResult result = authService.adminLogin(password);
        return ApiResponse.success(result);
    }

    @lombok.Data
    public static class MockLoginRequest {
        private String role;  // "user" 或 "admin"
    }
}
