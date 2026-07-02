package com.mzy.xyswzlsys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mzy.xyswzlsys.dto.request.LoginRequest;
import com.mzy.xyswzlsys.dto.response.LoginResponse;
import com.mzy.xyswzlsys.dto.response.SysUserResponse;
import com.mzy.xyswzlsys.entity.SysUser;
import com.mzy.xyswzlsys.mapper.SysUserMapper;
import com.mzy.xyswzlsys.security.JwtTokenUtil;
import com.mzy.xyswzlsys.service.AuthService;
import com.mzy.xyswzlsys.service.TokenStoreService;
import com.mzy.xyswzlsys.util.PasswordCrypto;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 认证 Service 实现
 *
 * 登录流程：
 *   1. 根据用户名查询用户
 *   2. 前端加密后的密码 -> PasswordCrypto.decrypt() 还原为明文
 *   3. BCrypt 比对明文与数据库存储的 BCrypt hash
 *   4. 通过后生成 JWT Token，并将 Token 存入 Redis（TTL = 配置的有效期）
 *   5. 返回用户信息 + Token
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;
    private final TokenStoreService tokenStoreService;

    public AuthServiceImpl(SysUserMapper sysUserMapper,
                         PasswordEncoder passwordEncoder,
                         JwtTokenUtil jwtTokenUtil,
                         TokenStoreService tokenStoreService) {
        this.sysUserMapper = sysUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenUtil = jwtTokenUtil;
        this.tokenStoreService = tokenStoreService;
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

        // 3. 前端提交的密码可能是加密后的（ENC:xxx），先解密为明文
        String plainPassword = PasswordCrypto.decryptPassword(request.getPassword());

        // 4. BCrypt 比对（数据库存储的是 BCrypt hash）
        if (!passwordEncoder.matches(plainPassword, user.getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        // 5. 生成 JWT Token
        String token = jwtTokenUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        // 6. 将 Token 存入 Redis（支持主动失效 / 有效期内重复访问）
        tokenStoreService.storeToken(token, user.getId());

        // 7. 组装响应
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
    public void logout(String token) {
        // 退出登录：删除 Redis 中的 Token，使该 Token 立即失效
        if (token != null && !token.isEmpty()) {
            tokenStoreService.removeToken(token);
        }
    }

    @Override
    public String register(String username, String password, String realName, String phone, String email, String studentId, String college) {
        // 1. 基本参数校验
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }

        // 2. 前端提交的密码可能是加密后的（ENC:xxx），先解密为明文
        String plainPassword = PasswordCrypto.decryptPassword(password);
        if (plainPassword.length() < 6) {
            throw new IllegalArgumentException("密码长度不能少于6位");
        }

        // 3. 检查用户名是否已存在
        SysUser existing = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username.trim())
        );
        if (existing != null) {
            throw new IllegalArgumentException("用户名已存在，请换一个");
        }

        // 4. 创建新用户（BCrypt 加密密码后保存）
        SysUser newUser = new SysUser();
        newUser.setUsername(username.trim());
        newUser.setPassword(passwordEncoder.encode(plainPassword));
        newUser.setRealName(realName != null ? realName.trim() : null);
        newUser.setPhone(phone != null ? phone.trim() : null);
        newUser.setEmail(email != null ? email.trim() : null);
        newUser.setStudentId(studentId != null ? studentId.trim() : null);
        newUser.setCollege(college != null ? college.trim() : null);
        newUser.setRole(0);
        newUser.setStatus(1);

        sysUserMapper.insert(newUser);

        return "注册成功，欢迎使用校园失物招领系统";
    }
}
