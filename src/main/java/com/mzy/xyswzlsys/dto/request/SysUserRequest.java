package com.mzy.xyswzlsys.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户新增/修改请求 DTO
 *
 * 统一做输入校验，避免：
 *   1) 超长文本注入（超过数据库字段长度）
 *   2) 特殊字符（如脚本）作为 username/phone 等字段
 *   3) 非法 role/status 值（例如 role=99）
 */
@Data
public class SysUserRequest {

    /** 用户名（字母、数字、下划线，3-30 字符；新增时必填，修改时不可改） */
    @NotBlank(message = "用户名不能为空", groups = Create.class)
    @Size(min = 3, max = 30, message = "用户名长度需在 3-30 字符之间")
    @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "用户名仅支持字母、数字和下划线")
    private String username;

    /** 密码（前端加密后传送，后端会解密；新增时必填） */
    @NotBlank(message = "密码不能为空", groups = Create.class)
    @Size(min = 6, max = 200, message = "密码长度需在 6-64 字符之间")
    private String password;

    /** 真实姓名（最长 50 字符） */
    @Size(max = 50, message = "真实姓名过长")
    private String realName;

    /** 手机号（仅允许数字，最长 20） */
    @Size(max = 20, message = "手机号过长")
    @Pattern(regexp = "^$|^[0-9\\-+]+$", message = "手机号格式不正确")
    private String phone;

    /** 邮箱（格式校验） */
    @Size(max = 100, message = "邮箱过长")
    @Email(message = "邮箱格式不正确")
    private String email;

    /** 学号（最长 30，字母数字） */
    @Size(max = 30, message = "学号过长")
    @Pattern(regexp = "^$|^[A-Za-z0-9\\-]+$", message = "学号仅支持字母数字和横杠")
    private String studentId;

    /** 学院（最长 100 字符） */
    @Size(max = 100, message = "学院名称过长")
    private String college;

    /** 角色：0-学生，1-普通管理员，2-超级管理员（仅管理员和超级管理员可设置） */
    @Min(value = 0, message = "角色值无效")
    @Max(value = 2, message = "角色值无效")
    private Integer role;

    /** 状态：0-禁用，1-正常 */
    @Min(value = 0, message = "状态值无效")
    @Max(value = 1, message = "状态值无效")
    private Integer status;

    /** 分组校验标识：新增时使用 */
    public interface Create {}
}
