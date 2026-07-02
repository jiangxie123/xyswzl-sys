package com.mzy.xyswzlsys.controller;

import com.mzy.xyswzlsys.common.PageResult;
import com.mzy.xyswzlsys.common.Result;
import com.mzy.xyswzlsys.dto.request.SysUserRequest;
import com.mzy.xyswzlsys.dto.response.SysUserResponse;
import com.mzy.xyswzlsys.service.SysUserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理接口
 */
@RestController
@RequestMapping("/api/users")
public class SysUserController {

    private final SysUserService sysUserService;

    public SysUserController(SysUserService sysUserService) {
        this.sysUserService = sysUserService;
    }

    /**
     * 分页查询用户列表
     * GET /api/users?current=1&size=10&keyword=xxx&role=0
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
     */
    @GetMapping("/{id}")
    public Result<SysUserResponse> getById(@PathVariable Long id) {
        return Result.success(sysUserService.getById(id));
    }

    /**
     * 新增用户
     * POST /api/users
     */
    @PostMapping
    public Result<Void> add(@Valid @RequestBody SysUserRequest request) {
        sysUserService.add(request);
        return Result.success();
    }

    /**
     * 修改用户
     * PUT /api/users/{id}
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody SysUserRequest request) {
        sysUserService.update(id, request);
        return Result.success();
    }

    /**
     * 删除用户
     * DELETE /api/users/{id}
     * 注意：该接口已在 SecurityConfig 中配置为需要 ROLE_SUPER_ADMIN 权限
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        sysUserService.delete(id);
        return Result.success();
    }
}
