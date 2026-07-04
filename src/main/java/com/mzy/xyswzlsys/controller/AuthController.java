package com.mzy.xyswzlsys.controller;

import com.mzy.xyswzlsys.common.Result;
import com.mzy.xyswzlsys.dto.request.LoginRequest;
import com.mzy.xyswzlsys.dto.request.RegisterRequest;
import com.mzy.xyswzlsys.dto.response.LoginResponse;
import com.mzy.xyswzlsys.service.AdminOperationLogService;
import com.mzy.xyswzlsys.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口
 *
 * 安全机制：
 *   1. 登录接口：密码以加密形式（ENC:xxx）传输，后端解密后做 BCrypt 比对
 *   2. 登录成功：生成 JWT Token 并存储到 Redis（TTL = 配置的有效期）
 *   3. 登出接口：从 Authorization 请求头提取 Token，删除 Redis 中对应记录，使其立即失效
 *   4. 登录失败连续 >=5 次，对该用户名做 1 分钟临时锁定
 *   5. 其他接口：JwtAuthenticationFilter 中校验 JWT 签名 + Redis 中是否存在
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AdminOperationLogService logService;

    @Autowired
    public AuthController(AuthService authService, AdminOperationLogService logService) {
        this.authService = authService;
        this.logService = logService;
    }

    /**
     * 用户登录
     * POST /api/auth/login
     * 请求体：{"username": "admin", "password": "ENC:xxxxxx"}
     * 响应：{"code":200, "data":{"token":"xxx", "user":{...}}}
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.login(request);
            Long userId = response.getUser().getId();
            String username = response.getUser().getUsername();
            Integer role = response.getUser().getRole();
            // 管理员（role=1或2）登录时记录日志
            if (role != null && role >= 1) {
                try {
                    logService.log(userId, username, "LOGIN", "SYSTEM", "管理员登录成功", null, null, 1, null);
                } catch (Exception e) {
                    // 仅记录到日志，不影响登录流程
                    java.lang.System.err.println("[AuthController] 写入操作日志失败: " + e.getMessage());
                }
            }
            return Result.success(response);
        } catch (Exception e) {
            return Result.error(401, e.getMessage());
        }
    }

    /**
     * 登出
     * POST /api/auth/logout
     * 从 Authorization: Bearer {token} 请求头中提取 Token，删除 Redis 中对应记录
     */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        String token = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }
        try {
            authService.logout(token);
        } catch (Exception ignored) {
            // Redis 失败不影响登出流程
        }
        return Result.success();
    }

    /**
     * 用户注册
     * POST /api/auth/register
     * 默认注册为学生角色（role=0），状态为正常（status=1）
     */
    @PostMapping("/register")
    public Result<String> register(@Valid @RequestBody RegisterRequest request) {
        try {
            String msg = authService.register(request);
            return Result.success(msg);
        } catch (Exception e) {
            return Result.error(400, e.getMessage());
        }
    }
}

