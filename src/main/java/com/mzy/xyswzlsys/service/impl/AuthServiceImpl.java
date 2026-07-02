package com.mzy.xyswzlsys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mzy.xyswzlsys.dto.request.LoginRequest;
import com.mzy.xyswzlsys.dto.response.LoginResponse;
import com.mzy.xyswzlsys.dto.response.SysUserResponse;
import com.mzy.xyswzlsys.entity.SysUser;
import com.mzy.xyswzlsys.mapper.SysUserMapper;
import com.mzy.xyswzlsys.security.JwtTokenUtil;
import com.mzy.xyswzlsys.service.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 认证 Service 实现
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;

    public AuthServiceImpl(SysUserMapper sysUserMapper, PasswordEncoder passwordEncoder, JwtTokenUtil jwtTokenUtil) {
        this.sysUserMapper = sysUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        // 1. 根据用户名查询用户
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, request.getUsername())
        );
        if (user == null) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        // 2. 检查账号状态
        if (user.getStatus() == null || user.getStatus() == 0) {
            throw new IllegalStateException("账号已被禁用，请联系管理员");
        }

        // 3. 校验密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        // 4. 生成 JWT Token
        String token = jwtTokenUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        // 5. 组装响应（转换 Entity -> Response，不返回密码字段）
        SysUserResponse userResponse = new SysUserResponse();
        userResponse.setId(user.getId());
        userResponse.setUsername(user.getUsername());
        userResponse.setRealName(user.getRealName());
        userResponse.setPhone(user.getPhone());
        userResponse.setEmail(user.getEmail());
        userResponse.setStudentId(user.getStudentId());
        userResponse.setCollege(user.getCollege());
        userResponse.setRole(user.getRole());
        userResponse.setStatus(user.getStatus());
        userResponse.setCreateTime(user.getCreateTime());
        userResponse.setUpdateTime(user.getUpdateTime());

        return new LoginResponse(token, userResponse);
    }

    @Override
    public void logout() {
        // JWT 无状态方案：客户端丢弃 Token 即可
        // 若后续需要 Token 黑名单机制，可在这里实现
    }

    @Override
    public String register(String username, String password, String realName, String phone, String email, String studentId, String college) {
        // 1. 基本参数校验
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("密码长度不能少于6位");
        }

        // 2. 检查用户名是否已存在
        SysUser existing = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username.trim())
        );
        if (existing != null) {
            throw new IllegalArgumentException("用户名已存在，请换一个");
        }

        // 3. 创建新用户
        SysUser newUser = new SysUser();
        newUser.setUsername(username.trim());
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setRealName(realName != null ? realName.trim() : null);
        newUser.setPhone(phone != null ? phone.trim() : null);
        newUser.setEmail(email != null ? email.trim() : null);
        newUser.setStudentId(studentId != null ? studentId.trim() : null);
        newUser.setCollege(college != null ? college.trim() : null);
        newUser.setRole(0);  // 默认学生角色
        newUser.setStatus(1);  // 默认启用

        sysUserMapper.insert(newUser);

        return "注册成功，欢迎使用校园失物招领系统";
    }
}
