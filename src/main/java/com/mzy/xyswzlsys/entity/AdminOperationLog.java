package com.mzy.xyswzlsys.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理员操作日志实体类
 * 对应数据库表：admin_operation_log
 */
@Data
@TableName("admin_operation_log")
public class AdminOperationLog {

    /** 日志ID（主键，自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作管理员ID */
    private Long adminId;

    /** 操作管理员姓名 */
    private String adminName;

    /** 操作类型（如：CREATE, UPDATE, DELETE, AUDIT, LOGIN） */
    private String operationType;

    /** 操作模块（如：USER, ITEM, CATEGORY, COMMENT, SYSTEM） */
    private String operationModule;

    /** 操作描述（详细说明操作内容） */
    private String operationDesc;

    /** 操作目标ID（如：审核的物品ID、删除的用户ID） */
    private Long targetId;

    /** 操作目标类型 */
    private String targetType;

    /** 操作IP地址 */
    private String ipAddress;

    /** 用户代理（浏览器信息） */
    private String userAgent;

    /** 操作结果：0-失败，1-成功 */
    private Integer result;

    /** 错误信息（操作失败时记录） */
    private String errorMsg;

    /** 操作时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
