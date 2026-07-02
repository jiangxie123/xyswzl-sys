package com.mzy.xyswzlsys.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户响应 DTO（返回给前端的用户信息，不包含密码）
 */
@Data
public class SysUserResponse {

    private Long id;
    private String username;
    private String realName;
    private String phone;
    private String email;
    private String studentId;
    private String college;

    /** 角色：0-学生，1-普通管理员，2-超级管理员 */
    private Integer role;

    /** 状态：0-禁用，1-正常 */
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
