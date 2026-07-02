package com.mzy.xyswzlsys.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求 DTO
 */
@Data
public class LoginRequest {

    /** 用户名（必填） */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /** 密码（必填） */
    @NotBlank(message = "密码不能为空")
    private String password;
}
