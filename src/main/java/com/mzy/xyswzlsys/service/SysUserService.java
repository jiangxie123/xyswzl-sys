package com.mzy.xyswzlsys.service;

import com.mzy.xyswzlsys.common.PageResult;
import com.mzy.xyswzlsys.dto.request.SysUserRequest;
import com.mzy.xyswzlsys.dto.response.SysUserResponse;

/**
 * 用户管理 Service
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
     * 新增用户
     */
    void add(SysUserRequest request);

    /**
     * 修改用户
     */
    void update(Long id, SysUserRequest request);

    /**
     * 删除用户（物理删除，后续可改为逻辑删除）
     */
    void delete(Long id);
}
