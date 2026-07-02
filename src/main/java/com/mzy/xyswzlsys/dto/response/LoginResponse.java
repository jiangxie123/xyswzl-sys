package com.mzy.xyswzlsys.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应 DTO
 * 返回 JWT Token 和当前用户信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    /** JWT Token（前端后续请求需放在 Authorization: Bearer <token> 中） */
    private String token;

    /** 当前用户信息 */
    private SysUserResponse user;
}
