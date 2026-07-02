package com.mzy.xyswzlsys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mzy.xyswzlsys.entity.AdminOperationLog;
import com.mzy.xyswzlsys.mapper.AdminOperationLogMapper;
import com.mzy.xyswzlsys.service.AdminOperationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * 管理员操作日志 Service 实现
 */
@Service
public class AdminOperationLogServiceImpl implements AdminOperationLogService {

    @Autowired
    private AdminOperationLogMapper logMapper;

    @Override
    public void log(Long adminId, String adminName, String operationType, String operationModule,
                    String operationDesc, Long targetId, String targetType, int result, String errorMsg) {
        AdminOperationLog log = new AdminOperationLog();
        log.setAdminId(adminId);
        log.setAdminName(adminName);
        log.setOperationType(operationType);
        log.setOperationModule(operationModule);
        log.setOperationDesc(operationDesc);
        log.setTargetId(targetId);
        log.setTargetType(targetType);
        log.setResult(result);
        log.setErrorMsg(errorMsg);
        log.setCreateTime(LocalDateTime.now());
        logMapper.insert(log);
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
                wrapper.le("create_time", end);
            } catch (Exception ignored) {}
        }
        wrapper.orderByDesc("create_time");
        return logMapper.selectPage(page, wrapper);
    }
}
