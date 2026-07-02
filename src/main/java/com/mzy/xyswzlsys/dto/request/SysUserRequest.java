package com.mzy.xyswzlsys.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户新增/修改请求 DTO
 */
@Data
public class SysUserRequest {

    /** 用户名（新增时必填，修改时不可改） */
    @NotBlank(message = "用户名不能为空", groups = Create.class)
    private String username;

    /** 密码（新增时必填） */
    @NotBlank(message = "密码不能为空", groups = Create.class)
    private String password;

    /** 真实姓名 */
    private String realName;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 学号 */
    private String studentId;

    /** 学院 */
    private String college;

    /** 角色：0-学生，1-普通管理员，2-超级管理员 */
    private Integer role;

    /** 状态：0-禁用，1-正常 */
    private Integer status;

    /** 分组校验标识：新增时使用 */
    public interface Create {}
}
