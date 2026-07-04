package com.mzy.xyswzlsys.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mzy.xyswzlsys.common.Result;
import com.mzy.xyswzlsys.entity.AdminOperationLog;
import com.mzy.xyswzlsys.security.CurrentUserDetails;
import com.mzy.xyswzlsys.security.JwtTokenUtil;
import com.mzy.xyswzlsys.service.AdminOperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员操作日志接口（需要管理员权限）
 *
 * 权限说明：
 *   - 仅管理员或超级管理员可以访问
 *   - 学生访问 → 返回 403 无权操作
 *   - 未登录 → 返回 401
 */
@RestController
@RequestMapping("/api/admin/logs")
public class AdminOperationLogController {

    @Autowired
    private AdminOperationLogService logService;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    /**
     * 从 Spring Security 上下文获取当前用户；若上下文不可用，则回退到 JWT 解析
     */
    private CurrentUserDetails getCurrentUserDetails(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getDetails() instanceof CurrentUserDetails) {
            return (CurrentUserDetails) authentication.getDetails();
        }
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                Long userId = jwtTokenUtil.getUserIdFromToken(token);
                String username = jwtTokenUtil.getUsernameFromToken(token);
                Integer role = jwtTokenUtil.getRoleFromToken(token);
                return new CurrentUserDetails(userId, username, role);
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }

    private boolean isAdmin(Integer role) {
        return role != null && (role == 1 || role == 2);
    }

    /**
     * 是否为超级管理员（role=2）
     */
    private boolean isSuperAdmin(Integer role) {
        return role != null && role == 2;
    }

    /**
     * 分页查询操作日志
     */
    @GetMapping
    public Result<IPage<AdminOperationLog>> getLogPage(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long adminId,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) String operationModule,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        CurrentUserDetails user = getCurrentUserDetails(request);
        if (user == null || user.getUserId() == null) {
            return Result.error(401, "请先登录");
        }
        if (!isAdmin(user.getRole())) {
            return Result.error(403, "无权操作");
        }
        if (current < 1) current = 1;
        if (size < 1 || size > 100) size = 10;
        return Result.success(logService.getLogPage(current, size, adminId, operationType,
                operationModule, keyword, startTime, endTime));
    }

    /**
     * 主动清理历史操作日志
     * - 仅超级管理员（role=2）可操作
     * - daysBefore: 清理多少天前的日志，默认 30 天
     */
    @DeleteMapping("/clean")
    public Result<java.util.Map<String, Object>> cleanLogs(
            HttpServletRequest request,
            @RequestParam(defaultValue = "30") int daysBefore) {
        CurrentUserDetails user = getCurrentUserDetails(request);
        if (user == null || user.getUserId() == null) {
            return Result.error(401, "请先登录");
        }
        if (!isSuperAdmin(user.getRole())) {
            return Result.error(403, "仅超级管理员可清理日志");
        }
        int deleted = logService.cleanOldLogs(daysBefore);
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("deleted", deleted);
        data.put("daysBefore", daysBefore);
        logService.log(user.getUserId(), user.getUsername(), "CLEAN",
                "SYSTEM", "清理 " + daysBefore + " 天前的操作日志，共 " + deleted + " 条",
                null, null, 1, null);
        return Result.success(data);
    }
}
