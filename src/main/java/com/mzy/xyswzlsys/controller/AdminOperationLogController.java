package com.mzy.xyswzlsys.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mzy.xyswzlsys.common.Result;
import com.mzy.xyswzlsys.entity.AdminOperationLog;
import com.mzy.xyswzlsys.service.AdminOperationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员操作日志接口（需要管理员权限）
 */
@RestController
@RequestMapping("/api/admin/logs")
public class AdminOperationLogController {

    @Autowired
    private AdminOperationLogService logService;

    /**
     * 分页查询操作日志
     */
    @GetMapping
    public Result<IPage<AdminOperationLog>> getLogPage(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long adminId,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) String operationModule,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return Result.success(logService.getLogPage(current, size, adminId, operationType,
                operationModule, keyword, startTime, endTime));
    }
}
