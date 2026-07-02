package com.mzy.xyswzlsys.controller;

import com.mzy.xyswzlsys.common.PageResult;
import com.mzy.xyswzlsys.common.Result;
import com.mzy.xyswzlsys.dto.request.SysUserRequest;
import com.mzy.xyswzlsys.dto.response.SysUserResponse;
import com.mzy.xyswzlsys.security.JwtTokenUtil;
import com.mzy.xyswzlsys.service.SysUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理接口
 *
 * 权限设计说明：
 * 1. 学生（role=0）：只能查看和修改自己的基本信息（/api/users/me）
 * 2. 管理员（role=1）：可以查询所有用户、新增学生、修改学生信息，但不能管理其他管理员/超级管理员
 * 3. 超级管理员（role=2）：拥有完整权限，可以管理所有用户
 *
 * URL级权限由 SecurityConfig 控制，业务级角色权限由 SysUserService 控制
 */
@RestController
@RequestMapping("/api/users")
public class SysUserController {

    private final SysUserService sysUserService;
    private final JwtTokenUtil jwtTokenUtil;

    public SysUserController(SysUserService sysUserService, JwtTokenUtil jwtTokenUtil) {
        this.sysUserService = sysUserService;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    /**
     * 从请求头中解析当前登录用户的 ID 和角色
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return jwtTokenUtil.getUserIdFromToken(authHeader.substring(7));
        }
        return null;
    }

    private Integer getCurrentRole(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return jwtTokenUtil.getRoleFromToken(authHeader.substring(7));
        }
        return null;
    }

    /**
     * 获取当前登录用户信息
     * GET /api/users/me
     * 权限：所有已登录用户
     */
    @GetMapping("/me")
    public Result<SysUserResponse> getCurrentUser(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.error(401, "无法获取当前用户信息");
        }
        return Result.success(sysUserService.getById(userId));
    }

    /**
     * 修改当前登录用户的基本信息（不含角色和状态）
     * PUT /api/users/me
     * 权限：所有已登录用户
     * 注意：学生和管理员可以通过此接口修改自己的联系方式等信息，但不能修改自己的角色和状态
     */
    @PutMapping("/me")
    public Result<Void> updateProfile(@RequestBody SysUserRequest request, HttpServletRequest httpRequest) {
        Long userId = getCurrentUserId(httpRequest);
        if (userId == null) {
            return Result.error(401, "无法获取当前用户信息");
        }
        sysUserService.updateProfile(userId, request);
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
            @RequestParam(required = false) Integer role) {
        return Result.success(sysUserService.list(current, size, keyword, role));
    }

    /**
     * 查询单个用户
     * GET /api/users/{id}
     * 权限：管理员或超级管理员
     */
    @GetMapping("/{id}")
    public Result<SysUserResponse> getById(@PathVariable Long id) {
        return Result.success(sysUserService.getById(id));
    }

    /**
     * 新增用户
     * POST /api/users
     * 权限：管理员或超级管理员
     * 业务限制：管理员只能新增学生（role=0）；超级管理员可以新增任何角色
     */
    @PostMapping
    public Result<Void> add(@Valid @RequestBody SysUserRequest request, HttpServletRequest httpRequest) {
        Integer currentRole = getCurrentRole(httpRequest);
        sysUserService.add(request, currentRole);
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
        Integer currentRole = getCurrentRole(httpRequest);
        sysUserService.update(id, request, currentRole);
        return Result.success();
    }

    /**
     * 删除用户
     * DELETE /api/users/{id}
     * 权限：超级管理员（URL 级别已配置需要 ROLE_SUPER_ADMIN）
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        sysUserService.delete(id);
        return Result.success();
    }
}
