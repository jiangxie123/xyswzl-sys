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
        // 1. 构建查询条件
        LambdaQueryWrapper<SysUser> query = new LambdaQueryWrapper<>();

        // 关键字模糊搜索（用户名、真实姓名、学号）
        if (StringUtils.hasText(keyword)) {
            query.and(wrapper -> wrapper
                    .like(SysUser::getUsername, keyword)
                    .or()
                    .like(SysUser::getRealName, keyword)
                    .or()
                    .like(SysUser::getStudentId, keyword)
            );
        }

        // 角色过滤
        if (role != null) {
            query.eq(SysUser::getRole, role);
        }

        // 按创建时间倒序
        query.orderByDesc(SysUser::getCreateTime);

        // 2. 分页查询
        Page<SysUser> page = sysUserMapper.selectPage(new Page<>(current, size), query);

        // 3. Entity -> Response 转换（密码字段已由 @TableField(select=false) 过滤，安全）
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
    public void add(SysUserRequest request) {
        // 1. 检查用户名是否已存在
        Long exists = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, request.getUsername())
        );
        if (exists != null && exists > 0) {
            throw new IllegalArgumentException("用户名已存在：" + request.getUsername());
        }

        // 2. 组装 Entity（密码使用 BCrypt 加密）
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setStudentId(request.getStudentId());
        user.setCollege(request.getCollege());
        user.setRole(request.getRole() != null ? request.getRole() : 0);
        user.setStatus(request.getStatus() != null ? request.getStatus() : 1);

        sysUserMapper.insert(user);
    }

    @Override
    public void update(Long id, SysUserRequest request) {
        // 1. 检查用户是否存在
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在：ID=" + id);
        }

        // 2. 只有传递非空字段才更新（支持部分字段更新）
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
