package com.mzy.xyswzlsys.service;

import com.mzy.xyswzlsys.dto.request.LoginRequest;
import com.mzy.xyswzlsys.dto.request.RegisterRequest;
import com.mzy.xyswzlsys.dto.response.LoginResponse;

/**
 * 认证 Service
 */
public interface AuthService {

    /**
     * 用户登录（用户名 + 密码）
     * @return JWT Token + 用户信息
     */
    LoginResponse login(LoginRequest request);

    /**
     * 登出：删除 Redis 中存储的 Token，使该 Token 立即失效
     * @param token 从请求头中提取的 JWT Token
     */
    void logout(String token);

    /**
     * 用户注册（使用强约束 DTO 做参数校验，默认注册为学生角色）
     */
    String register(RegisterRequest request);
}
