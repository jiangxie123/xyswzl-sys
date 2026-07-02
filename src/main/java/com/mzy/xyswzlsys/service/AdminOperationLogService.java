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
}
