package com.mzy.xyswzlsys.common;

/**
 * 统一状态码枚举
 * 所有 API 返回的 code 字段都应使用此枚举中的值
 */
public enum ResultCode {

    /** 成功 */
    SUCCESS(200, "操作成功"),

    /** 请求参数错误 */
    BAD_REQUEST(400, "请求参数错误"),

    /** 未认证（Token 缺失或无效） */
    UNAUTHORIZED(401, "未认证或认证已过期"),

    /** 无权限 */
    FORBIDDEN(403, "无操作权限"),

    /** 资源不存在 */
    NOT_FOUND(404, "资源不存在"),

    /** 业务冲突（如用户名已存在） */
    CONFLICT(409, "数据已存在"),

    /** 服务器内部错误 */
    INTERNAL_ERROR(500, "服务器内部错误");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
