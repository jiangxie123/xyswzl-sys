package com.mzy.xyswzlsys.service;

import com.mzy.xyswzlsys.common.PageResult;
import com.mzy.xyswzlsys.dto.request.SysUserRequest;
import com.mzy.xyswzlsys.dto.response.SysUserResponse;

/**
 * 用户管理 Service
 *
 * 角色权限说明：
 * - role=0 学生：只能查看和修改自己的基本信息
 * - role=1 管理员：可以查询、新增、修改学生用户（role=0），不能管理其他管理员/超级管理员
 * - role=2 超级管理员：拥有完整权限，可以管理所有用户
 */
public interface SysUserService {

    /**
     * 分页查询用户列表
     * @param current  当前页
     * @param size     每页条数
     * @param keyword  搜索关键字（用户名 / 真实姓名 / 学号）
     * @param role     角色过滤（可选）
     */
    PageResult<SysUserResponse> list(int current, int size, String keyword, Integer role);

    /**
     * 根据 ID 查询单个用户
     */
    SysUserResponse getById(Long id);

    /**
     * 新增用户（带角色校验）
     * @param currentRole 当前登录用户的角色：
     *                    管理员只能新增学生（role=0）
     *                    超级管理员可以新增任何角色
     */
    void add(SysUserRequest request, Integer currentRole);

    /**
     * 修改用户（带角色校验）
     * @param currentRole 当前登录用户的角色：
     *                    管理员只能修改学生用户，且不能把角色改成管理员/超级管理员
     *                    超级管理员可以修改任何用户
     */
    void update(Long id, SysUserRequest request, Integer currentRole);

    /**
     * 修改当前登录用户的个人信息（不含角色和状态）
     * 所有已登录用户都可以使用
     */
    void updateProfile(Long userId, SysUserRequest request);

    /**
     * 删除用户（物理删除，后续可改为逻辑删除）
     * 仅超级管理员可用（URL 级别已限制）
     */
    void delete(Long id);
}
