package com.mzy.xyswzlsys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mzy.xyswzlsys.common.PageResult;
import com.mzy.xyswzlsys.dto.request.SysUserRequest;
import com.mzy.xyswzlsys.dto.response.SysUserResponse;
import com.mzy.xyswzlsys.entity.SysUser;
import com.mzy.xyswzlsys.mapper.SysUserMapper;
import com.mzy.xyswzlsys.service.SysUserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户管理 Service 实现
 *
 * 业务权限设计：
 * 1. 学生 (role=0) - 仅能修改自己的基本信息（不能修改角色和状态）
 * 2. 管理员 (role=1) - 可以新增、查询所有用户，但：
 *    - 只能新增学生角色（role=0)
 *    - 只能修改学生用户的信息
 *    - 不能将角色改成管理员/超级管理员
 *    - 不能删除用户
 * 3. 超级管理员 (role=2) - 拥有完整的用户管理权限
 */
@Service
public class SysUserServiceImpl implements SysUserService {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;

    public SysUserServiceImpl(SysUserMapper sysUserMapper, PasswordEncoder passwordEncoder) {
        this.sysUserMapper = sysUserMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public PageResult<SysUserResponse> list(int current, int size, String keyword, Integer role) {
        LambdaQueryWrapper<SysUser> query = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            query.and(wrapper -> wrapper
                    .like(SysUser::getUsername, keyword)
                    .or()
                    .like(SysUser::getRealName, keyword)
                    .or()
                    .like(SysUser::getStudentId, keyword)
            );
        }
        if (role != null) {
            query.eq(SysUser::getRole, role);
        }
        query.orderByDesc(SysUser::getCreateTime);

        Page<SysUser> page = sysUserMapper.selectPage(new Page<>(current, size), query);

        List<SysUserResponse> records = page.getRecords().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return PageResult.of(records, page.getTotal(), page.getSize(), page.getCurrent());
    }

    @Override
    public SysUserResponse getById(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在：ID=" + id);
        }
        return toResponse(user);
    }

    @Override
    public void add(SysUserRequest request, Integer currentRole) {
        // ===== 1. 角色权限校验 =====
        Integer targetRole = request.getRole() != null ? request.getRole() : 0;

        // 管理员只能新增学生用户 (role=0)
        boolean isSuperAdmin = currentRole != null && currentRole == 2;
        if (!isSuperAdmin) {
            // 管理员 role=0
            if (targetRole != 0) {
                throw new SecurityException("无权限：管理员只能新增学生用户");
            }
        }

        // ===== 2. 检查用户名是否已存在 =====
        Long exists = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, request.getUsername())
        );
        if (exists != null && exists > 0) {
            throw new IllegalArgumentException("用户名已存在：" + request.getUsername());
        }

        // ===== 3. 组装 Entity 并插入 =====
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setStudentId(request.getStudentId());
        user.setCollege(request.getCollege());
        user.setRole(targetRole);
        user.setStatus(request.getStatus() != null ? request.getStatus() : 1);

        sysUserMapper.insert(user);
    }

    @Override
    public void update(Long id, SysUserRequest request, Integer currentRole) {
        // ===== 1. 查询目标用户 =====
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在：ID=" + id);
        }

        // ===== 2. 角色权限校验 =====
        boolean isSuperAdmin = currentRole != null && currentRole == 2;

        // 管理员只能修改学生用户
        if (!isSuperAdmin) {
            if (user.getRole() != null && user.getRole() != 0) {
                throw new SecurityException("无权限：管理员不能修改其他管理员或超级管理员的信息");
            }
            // 管理员不能将角色改成管理员或超级管理员
            if (request.getRole() != null && request.getRole() >= 1) {
                throw new SecurityException("无权限：管理员不能将用户角色改成管理员或超级管理员");
            }
        }

        // ===== 3. 只有传递非空字段才更新（支持部分字段更新） =====
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getRealName() != null) {
            user.setRealName(request.getRealName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getStudentId() != null) {
            user.setStudentId(request.getStudentId());
        }
        if (request.getCollege() != null) {
            user.setCollege(request.getCollege());
        }
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }

        sysUserMapper.updateById(user);
    }

    @Override
    public void updateProfile(Long userId, SysUserRequest request) {
        // 1. 检查用户是否存在
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在：ID=" + userId);
        }

        // 2. 只允许修改基本信息（忽略 role 和 status 忽略！
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getRealName() != null) {
            user.setRealName(request.getRealName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getStudentId() != null) {
            user.setStudentId(request.getStudentId());
        }
        if (request.getCollege() != null) {
            user.setCollege(request.getCollege());
        }
        // 注意：故意不更新 role/status！

        sysUserMapper.updateById(user);
    }

    @Override
    public void delete(Long id) {
        int affected = sysUserMapper.deleteById(id);
        if (affected <= 0) {
            throw new IllegalArgumentException("用户不存在：ID=" + id);
        }
    }

    /**
     * Entity -> Response 转换
     */
    private SysUserResponse toResponse(SysUser user) {
        SysUserResponse response = new SysUserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setRealName(user.getRealName());
        response.setPhone(user.getPhone());
        response.setEmail(user.getEmail());
        response.setStudentId(user.getStudentId());
        response.setCollege(user.getCollege());
        response.setRole(user.getRole());
        response.setStatus(user.getStatus());
        response.setCreateTime(user.getCreateTime());
        response.setUpdateTime(user.getUpdateTime());
        return response;
    }
}
