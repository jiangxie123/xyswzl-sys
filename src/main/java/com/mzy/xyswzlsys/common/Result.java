package com.mzy.xyswzlsys.common;

import lombok.Data;

/**
 * 统一 API 返回结果
 * 所有 Controller 方法的返回值都应使用此类包装
 *
 * JSON 格式示例：
 * {
 *   "code": 200,
 *   "message": "操作成功",
 *   "data": { ... }
 * }
 */
@Data
public class Result<T> {

    /** 状态码，参考 ResultCode */
    private Integer code;

    /** 提示信息 */
    private String message;

    /** 业务数据 */
    private T data;

    private Result() {}

    private Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 成功返回（无数据）
     */
    public static <T> Result<T> success() {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    /**
     * 成功返回（带数据）
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    /**
     * 成功返回（自定义消息）
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), message, data);
    }

    /**
     * 失败返回（使用预定义状态码）
     */
    public static <T> Result<T> error(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    /**
     * 失败返回（自定义状态码和消息）
     */
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    /**
     * 失败返回（使用 500 状态码）
     */
    public static <T> Result<T> error(String message) {
        return new Result<>(ResultCode.INTERNAL_ERROR.getCode(), message, null);
    }

    /**
     * 未认证（401）
     */
    public static <T> Result<T> unauthorized() {
        return error(ResultCode.UNAUTHORIZED);
    }

    /**
     * 无权限（403）
     */
    public static <T> Result<T> forbidden() {
        return error(ResultCode.FORBIDDEN);
    }

    /**
     * 资源不存在（404）
     */
    public static <T> Result<T> notFound() {
        return error(ResultCode.NOT_FOUND);
    }
}
