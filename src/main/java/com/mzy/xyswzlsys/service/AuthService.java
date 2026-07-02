package com.mzy.xyswzlsys.service;

import com.mzy.xyswzlsys.dto.request.LoginRequest;
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
     * 登出（当前 JWT 无状态方案下，客户端直接丢弃 Token 即可）
     * 这里仅预留方法，方便未来扩展黑名单等机制
     */
    void logout();

    /**
     * 用户注册（默认注册为学生角色）
     */
    String register(String username, String password, String realName, String phone, String email, String studentId, String college);
}
