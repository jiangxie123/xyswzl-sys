package com.mzy.xyswzlsys.controller;

import com.mzy.xyswzlsys.common.PageResult;
import com.mzy.xyswzlsys.common.Result;
import com.mzy.xyswzlsys.dto.request.SysUserRequest;
import com.mzy.xyswzlsys.dto.response.SysUserResponse;
import com.mzy.xyswzlsys.security.CurrentUserDetails;
import com.mzy.xyswzlsys.security.JwtTokenUtil;
import com.mzy.xyswzlsys.service.AdminOperationLogService;
import com.mzy.xyswzlsys.service.SysUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理接口
 *
 * 权限设计说明：
 *   1. 学生（role=0）：只能查看和修改自己的基本信息（/api/users/me）
 *   2. 管理员（role=1）：可以查询所有用户、新增学生、修改学生信息，但不能管理其他管理员/超级管理员
 *   3. 超级管理员（role=2）：拥有完整权限，可以管理所有用户
 *
 * 每个管理接口的开头都做了角色判断，防止学生越权。
 */
@RestController
@RequestMapping("/api/users")
public class SysUserController {

    private final SysUserService sysUserService;
    private final JwtTokenUtil jwtTokenUtil;
    private final AdminOperationLogService logService;

    @Autowired
    public SysUserController(SysUserService sysUserService, JwtTokenUtil jwtTokenUtil, AdminOperationLogService logService) {
        this.sysUserService = sysUserService;
        this.jwtTokenUtil = jwtTokenUtil;
        this.logService = logService;
    }

    /**
     * 从 Spring Security 上下文获取当前登录用户；若上下文不可用，则退回到 JWT 解析
     */
    private CurrentUserDetails getCurrentUserDetails(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getDetails() instanceof CurrentUserDetails) {
            return (CurrentUserDetails) authentication.getDetails();
        }
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            Long userId = jwtTokenUtil.getUserIdFromToken(token);
            String username = jwtTokenUtil.getUsernameFromToken(token);
            Integer role = jwtTokenUtil.getRoleFromToken(token);
            return new CurrentUserDetails(userId, username, role);
        }
        return null;
    }

    private boolean isAdmin(Integer role) {
        return role != null && (role == 1 || role == 2);
    }

    private boolean isSuperAdmin(Integer role) {
        return role != null && role == 2;
    }

    /**
     * 获取当前登录用户信息
     * GET /api/users/me
     * 权限：所有已登录用户
     */
    @GetMapping("/me")
    public Result<SysUserResponse> getCurrentUser(HttpServletRequest request) {
        CurrentUserDetails user = getCurrentUserDetails(request);
        if (user == null || user.getUserId() == null) {
            return Result.error(401, "无法获取当前用户信息");
        }
        return Result.success(sysUserService.getById(user.getUserId()));
    }

    /**
     * 修改当前登录用户的基本信息（不含角色和状态）
     * PUT /api/users/me
     * 权限：所有已登录用户
     */
    @PutMapping("/me")
    public Result<Void> updateProfile(@RequestBody SysUserRequest request, HttpServletRequest httpRequest) {
        CurrentUserDetails user = getCurrentUserDetails(httpRequest);
        if (user == null || user.getUserId() == null) {
            return Result.error(401, "无法获取当前用户信息");
        }
        sysUserService.updateProfile(user.getUserId(), request);
        return Result.success();
    }

    /**
     * 分页查询用户列表
     * GET /api/users?current=1&size=10&keyword=xxx&role=0
     * 权限：管理员或超级管理员
     */
    @GetMapping
    public Result<PageResult<SysUserResponse>> list(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer role,
            HttpServletRequest httpRequest) {
        CurrentUserDetails user = getCurrentUserDetails(httpRequest);
        if (user == null || !isAdmin(user.getRole())) return Result.error(403, "无权操作");
        // 分页参数保护
        if (current < 1) current = 1;
        if (size < 1 || size > 100) size = 10;
        return Result.success(sysUserService.list(current, size, keyword, role));
    }

    /**
     * 查询单个用户
     * GET /api/users/{id}
     * 权限：管理员或超级管理员
     */
    @GetMapping("/{id}")
    public Result<SysUserResponse> getById(@PathVariable Long id, HttpServletRequest httpRequest) {
        CurrentUserDetails user = getCurrentUserDetails(httpRequest);
        if (user == null || !isAdmin(user.getRole())) return Result.error(403, "无权操作");
        return Result.success(sysUserService.getById(id));
    }

    /**
     * 新增用户
     * POST /api/users
     * 权限：管理员或超级管理员
     * 业务限制：管理员只能新增学生（role=0 或不传角色）；超级管理员可以新增任何角色
     */
    @PostMapping
    public Result<Void> add(@Valid @RequestBody SysUserRequest request, HttpServletRequest httpRequest) {
        CurrentUserDetails user = getCurrentUserDetails(httpRequest);
        if (user == null || !isAdmin(user.getRole())) return Result.error(403, "无权操作");
        sysUserService.add(request, user.getRole());
        logService.log(user.getUserId(), user.getUsername(), "CREATE", "USER", "新增用户：" + request.getUsername(), null, "USER", 1, null);
        return Result.success();
    }

    /**
     * 修改用户
     * PUT /api/users/{id}
     * 权限：管理员或超级管理员
     * 业务限制：管理员只能修改学生用户的信息；超级管理员可以修改任何用户
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody SysUserRequest request, HttpServletRequest httpRequest) {
        CurrentUserDetails user = getCurrentUserDetails(httpRequest);
        if (user == null || !isAdmin(user.getRole())) return Result.error(403, "无权操作");
        sysUserService.update(id, request, user.getRole());
        logService.log(user.getUserId(), user.getUsername(), "UPDATE", "USER", "修改用户ID=" + id, id, "USER", 1, null);
        return Result.success();
    }

    /**
     * 删除用户
     * DELETE /api/users/{id}
     * 权限：超级管理员（防止普通管理员删除其他管理员账号）
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest httpRequest) {
        CurrentUserDetails user = getCurrentUserDetails(httpRequest);
        // 删除操作限制为超级管理员
        if (user == null || !isSuperAdmin(user.getRole())) return Result.error(403, "仅超级管理员可删除用户");
        sysUserService.delete(id);
        logService.log(user.getUserId(), user.getUsername(), "DELETE", "USER", "删除用户ID=" + id, id, "USER", 1, null);
        return Result.success();
    }
}
