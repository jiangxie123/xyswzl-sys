package com.mzy.xyswzlsys.controller;

import com.mzy.xyswzlsys.common.Result;
import com.mzy.xyswzlsys.dto.request.LoginRequest;
import com.mzy.xyswzlsys.dto.response.LoginResponse;
import com.mzy.xyswzlsys.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 用户登录
     * POST /api/auth/login
     * 请求体：{"username": "admin", "password": "123456"}
     * 响应：{"code":200, "data":{"token":"xxx", "user":{...}}}
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    /**
     * 登出
     * POST /api/auth/logout
     * JWT 无状态方案下：客户端丢弃 Token 即可
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.success();
    }

    /**
     * 用户注册
     * POST /api/auth/register
     * 请求体：{"username": "xxx", "password": "xxx", "realName": "xxx", "phone": "xxx", "email": "xxx", "studentId": "xxx", "college": "xxx"}
     * 默认注册为学生角色（role=0），状态为正常（status=1）
     */
    @PostMapping("/register")
    public Result<String> register(@RequestBody java.util.Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");
        String realName = request.get("realName");
        String phone = request.get("phone");
        String email = request.get("email");
        String studentId = request.get("studentId");
        String college = request.get("college");
        return Result.success(authService.register(username, password, realName, phone, email, studentId, college));
    }
}
