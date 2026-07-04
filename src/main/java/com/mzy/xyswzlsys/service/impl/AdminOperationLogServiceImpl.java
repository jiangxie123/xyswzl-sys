package com.mzy.xyswzlsys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mzy.xyswzlsys.entity.AdminOperationLog;
import com.mzy.xyswzlsys.mapper.AdminOperationLogMapper;
import com.mzy.xyswzlsys.service.AdminOperationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * 管理员操作日志 Service 实现
 *
 * 自动清理机制：
 *   - 每天凌晨 02:00 自动清理 30 天前的操作日志
 *   - 超级管理员可主动通过 API 清理指定天数前的日志
 */
@Service
public class AdminOperationLogServiceImpl implements AdminOperationLogService {

    /** 保留日志天数（默认 30 天） */
    private static final int DEFAULT_RETENTION_DAYS = 30;

    @Autowired
    private AdminOperationLogMapper logMapper;

    @Override
    public void log(Long adminId, String adminName, String operationType, String operationModule,
                    String operationDesc, Long targetId, String targetType, int result, String errorMsg) {
        try {
            AdminOperationLog log = new AdminOperationLog();
            log.setAdminId(adminId);
            log.setAdminName(adminName);
            log.setOperationType(operationType);
            log.setOperationModule(operationModule);
            log.setOperationDesc(truncate(operationDesc, 500));
            log.setTargetId(targetId);
            log.setTargetType(truncate(targetType, 50));
            log.setResult(result);
            log.setErrorMsg(truncate(errorMsg, 500));
            log.setCreateTime(LocalDateTime.now());
            logMapper.insert(log);
        } catch (Exception e) {
            System.err.println("[AdminOperationLog] 写入日志失败: " + e.getMessage());
        }
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    @Override
    public IPage<AdminOperationLog> getLogPage(int current, int size, Long adminId, String operationType,
                                               String operationModule, String keyword, String startTime, String endTime) {
        Page<AdminOperationLog> page = new Page<>(current, size);
        QueryWrapper<AdminOperationLog> wrapper = new QueryWrapper<>();

        if (adminId != null) {
            wrapper.eq("admin_id", adminId);
        }
        if (operationType != null && !operationType.trim().isEmpty()) {
            wrapper.eq("operation_type", operationType);
        }
        if (operationModule != null && !operationModule.trim().isEmpty()) {
            wrapper.eq("operation_module", operationModule);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.like("operation_desc", keyword);
        }
        if (startTime != null && !startTime.isEmpty()) {
            try {
                LocalDateTime start = LocalDate.parse(startTime, DateTimeFormatter.ofPattern("yyyy-MM-dd")).atStartOfDay();
                wrapper.ge("create_time", start);
            } catch (Exception ignored) {}
        }
        if (endTime != null && !endTime.isEmpty()) {
            try {
                LocalDateTime end = LocalDate.parse(endTime, DateTimeFormatter.ofPattern("yyyy-MM-dd")).atTime(LocalTime.MAX);
                wrapper.le("create_time", endTime);
            } catch (Exception ignored) {}
        }
        wrapper.orderByDesc("create_time");
        return logMapper.selectPage(page, wrapper);
    }

    @Override
    public int cleanOldLogs(int daysBefore) {
        if (daysBefore < 0) daysBefore = DEFAULT_RETENTION_DAYS;
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(daysBefore);
        QueryWrapper<AdminOperationLog> wrapper = new QueryWrapper<>();
        wrapper.lt("create_time", cutoffTime);
        return logMapper.delete(wrapper);
    }

    /**
     * 定时清理：每天凌晨 02:00 自动清理 30 天前的操作日志
     * Cron 表达式：秒 分 时 日 月 周
     */
    @Override
    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduledCleanOldLogs() {
        try {
            int deleted = cleanOldLogs(DEFAULT_RETENTION_DAYS);
            System.out.println("[定时任务-日志清理] 已清理 " + DEFAULT_RETENTION_DAYS + " 天前的操作日志 " + deleted + " 条 [" + LocalDateTime.now() + "]");
        } catch (Exception e) {
            System.err.println("[定时任务-日志清理] 执行失败: " + e.getMessage());
        }
    }
}
