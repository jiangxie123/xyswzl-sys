package com.mzy.xyswzlsys.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mzy.xyswzlsys.entity.AdminOperationLog;

/**
 * 管理员操作日志 Service
 */
public interface AdminOperationLogService {

    /**
     * 记录操作日志
     */
    void log(Long adminId, String adminName, String operationType, String operationModule,
             String operationDesc, Long targetId, String targetType, int result, String errorMsg);

    /**
     * 分页查询操作日志
     */
    IPage<AdminOperationLog> getLogPage(int current, int size, Long adminId, String operationType,
                                         String operationModule, String keyword, String startTime, String endTime);

    /**
     * 清理指定时间范围之前的日志
     * @param daysBefore 清理多少天前的日志（例如 30 表示清理 30 天前的日志）
     * @return 被删除的日志条数
     */
    int cleanOldLogs(int daysBefore);

    /**
     * 定时任务：每天凌晨清理 30 天前的操作日志
     */
    void scheduledCleanOldLogs();
}
