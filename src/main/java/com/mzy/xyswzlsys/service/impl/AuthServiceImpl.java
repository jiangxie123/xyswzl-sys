package com.mzy.xyswzlsys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mzy.xyswzlsys.dto.request.LoginRequest;
import com.mzy.xyswzlsys.dto.request.RegisterRequest;
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 认证 Service 实现
 *
 * 登录流程：
 *   1. 根据用户名查询用户
 *   2. 前端加密后的密码 -> PasswordCrypto.decrypt() 还原为明文
 *   3. BCrypt 比对明文与数据库存储的 BCrypt hash
 *   4. 通过后生成 JWT Token，并将 Token 存入 Redis（TTL = 配置的有效期）
 *   5. 返回用户信息 + Token
 *   额外：同一用户名连续失败 >=5 次，则 1 分钟内拒绝登录（简单防爆力破解）
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;
    private final TokenStoreService tokenStoreService;

    /** 用户名 -> [失败次数, 最后失败时间 ms] */
    private static final Map<String, long[]> FAILURE_CACHE = new ConcurrentHashMap<>();
    private static final int MAX_FAILURE = 5;
    private static final long LOCK_DURATION_MS = TimeUnit.MINUTES.toMillis(1);

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
        String username = request.getUsername() == null ? "" : request.getUsername().trim();

        // 1) 暴力破解防护：按用户名统计失败次数
        long[] cached = FAILURE_CACHE.get(username);
        if (cached != null) {
            long failCount = cached[0];
            long lastFailTs = cached[1];
            if (failCount >= MAX_FAILURE) {
                long elapsed = System.currentTimeMillis() - lastFailTs;
                if (elapsed < LOCK_DURATION_MS) {
                    long remainSec = (LOCK_DURATION_MS - elapsed) / 1000 + 1;
                    throw new IllegalStateException("登录失败次数过多，请稍后再试（约 " + remainSec + " 秒）");
                } else {
                    // 超过锁定窗口，重置计数
                    FAILURE_CACHE.remove(username);
                }
            }
        }

        // 2) 根据用户名查询用户
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)
        );
        if (user == null) {
            recordFailure(username);
            throw new IllegalArgumentException("用户名或密码错误");
        }

        // 3) 检查账号状态
        if (user.getStatus() == null || user.getStatus() == 0) {
            recordFailure(username);
            throw new IllegalStateException("账号已被禁用，请联系管理员");
        }

        // 4) 前端提交的密码可能是加密后的（ENC:xxx），先解密为明文
        String plainPassword = PasswordCrypto.decryptPassword(request.getPassword());

        // 5) BCrypt 比对（数据库存储的是 BCrypt hash）
        if (!passwordEncoder.matches(plainPassword, user.getPassword())) {
            recordFailure(username);
            throw new IllegalArgumentException("用户名或密码错误");
        }

        // 6) 登录成功：重置失败计数
        FAILURE_CACHE.remove(username);

        // 7) 生成 JWT Token
        String token = jwtTokenUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        // 8) 将 Token 存入 Redis
        tokenStoreService.storeToken(token, user.getId());

        // 9) 组装响应
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

    /** 记录登录失败 */
    private void recordFailure(String username) {
        long[] cached = FAILURE_CACHE.get(username);
        if (cached == null) {
            FAILURE_CACHE.put(username, new long[] { 1, System.currentTimeMillis() });
        } else {
            cached[0] = cached[0] + 1;
            cached[1] = System.currentTimeMillis();
        }
    }

    @Override
    public void logout(String token) {
        if (token != null && !token.isEmpty()) {
            tokenStoreService.removeToken(token);
        }
    }

    @Override
    public String register(RegisterRequest request) {
        String username = request.getUsername() == null ? "" : request.getUsername().trim();
        if (username.isEmpty()) throw new IllegalArgumentException("用户名不能为空");

        // 1) 密码解密 + 长度校验
        String plainPassword = PasswordCrypto.decryptPassword(request.getPassword());
        if (plainPassword == null || plainPassword.length() < 6) {
            throw new IllegalArgumentException("密码长度不能少于 6 位");
        }

        // 2) 检查用户名是否已存在
        SysUser existing = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)
        );
        if (existing != null) {
            throw new IllegalArgumentException("用户名已存在，请换一个");
        }

        // 3) 学号唯一性（可选，但需要非空时做校验）
        String safeStudentId = (request.getStudentId() != null && !request.getStudentId().trim().isEmpty())
                ? request.getStudentId().trim() : null;
        if (safeStudentId != null) {
            SysUser existingStudent = sysUserMapper.selectOne(
                    new LambdaQueryWrapper<SysUser>().eq(SysUser::getStudentId, safeStudentId)
            );
            if (existingStudent != null) {
                throw new IllegalArgumentException("该学号已被注册，请更换或不填");
            }
        }

        // 4) 组装用户
        SysUser newUser = new SysUser();
        newUser.setUsername(username);
        newUser.setPassword(passwordEncoder.encode(plainPassword));
        newUser.setRealName((request.getRealName() != null && !request.getRealName().trim().isEmpty())
                ? request.getRealName().trim() : null);
        newUser.setPhone((request.getPhone() != null && !request.getPhone().trim().isEmpty())
                ? request.getPhone().trim() : null);
        newUser.setEmail((request.getEmail() != null && !request.getEmail().trim().isEmpty())
                ? request.getEmail().trim() : null);
        newUser.setStudentId(safeStudentId);
        newUser.setCollege((request.getCollege() != null && !request.getCollege().trim().isEmpty())
                ? request.getCollege().trim() : null);
        newUser.setRole(0);
        newUser.setStatus(1);

        sysUserMapper.insert(newUser);

        return "注册成功，欢迎使用校园失物招领系统";
    }
}

