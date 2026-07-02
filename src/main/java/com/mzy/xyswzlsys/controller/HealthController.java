package com.mzy.xyswzlsys.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mzy.xyswzlsys.common.Result;
import com.mzy.xyswzlsys.entity.SysUser;
import com.mzy.xyswzlsys.mapper.SysUserMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 健康检查接口
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final JdbcTemplate jdbcTemplate;
    private final BCryptPasswordEncoder passwordEncoder;
    private final SysUserMapper sysUserMapper;

    public HealthController(JdbcTemplate jdbcTemplate, SysUserMapper sysUserMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.sysUserMapper = sysUserMapper;
    }

    /**
     * 检查数据库连接
     */
    @GetMapping("/db")
    public Result<Map<String, Object>> checkDb() {
        try {
            List<Map<String, Object>> users = jdbcTemplate.queryForList(
                    "SELECT id, username, role, status FROM sys_user LIMIT 3");
            int count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_user", Integer.class);
            return Result.success(Map.of(
                    "status", "OK",
                    "userCount", count,
                    "sampleUsers", users
            ));
        } catch (Exception e) {
            return Result.error(500, "数据库连接失败: " + e.getMessage());
        }
    }

    /**
     * 登录调试 - 模拟 AuthServiceImpl.login 的完整流程
     */
    @GetMapping("/debug-login")
    public Result<Map<String, Object>> debugLogin() {
        try {
            Map<String, Object> data = new HashMap<>();

            String testUsername = "admin";
            String testPassword = "123456";
            SysUser user = sysUserMapper.selectOne(
                    new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, testUsername)
            );

            data.put("mapperQuery_username", testUsername);
            data.put("mapperQuery_userFound", user != null);

            if (user != null) {
                data.put("mapperQuery_status", user.getStatus());
                data.put("mapperQuery_role", user.getRole());
                data.put("mapperQuery_passwordPrefix",
                        user.getPassword() != null && user.getPassword().length() > 20
                                ? user.getPassword().substring(0, 20) + "..."
                                : user.getPassword());
                data.put("mapperQuery_passwordLength",
                        user.getPassword() == null ? 0 : user.getPassword().length());
                data.put("mapperQuery_isBcryptFormat",
                        user.getPassword() != null && user.getPassword().startsWith("$2"));
                boolean matches = passwordEncoder.matches(testPassword, user.getPassword());
                data.put("mapperQuery_passwordMatches_123456", matches);
                data.put("simulatedLoginResult", matches ? "登录成功 ✓" : "密码不匹配");
            }

            List<Map<String, Object>> allUsers = jdbcTemplate.queryForList(
                    "SELECT id, username, LEFT(password, 20) AS pw_prefix, status, role FROM sys_user");
            data.put("allUsers", allUsers);
            data.put("bcryptOf_123456_now", passwordEncoder.encode("123456"));
            return Result.success(data);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", e.getMessage());
            err.put("errorType", e.getClass().getName());
            StringWriter sw = new StringWriter();
            e.printStackTrace(new java.io.PrintWriter(sw));
            err.put("stacktrace", sw.toString().substring(0, Math.min(800, sw.toString().length())));
            return Result.error(500, "调试失败: " + e.getMessage());
        }
    }

    /**
     * 调试新增用户 - 直接插入并返回详细异常
     */
    @GetMapping("/debug-add-user")
    public Result<Map<String, Object>> debugAddUser() {
        try {
            Map<String, Object> data = new HashMap<>();

            // 直接构造一个用户对象并 insert
            SysUser newUser = new SysUser();
            newUser.setUsername("debuguser");
            newUser.setPassword(passwordEncoder.encode("123456"));
            newUser.setRealName("调试用户");
            newUser.setRole(0);
            newUser.setStatus(1);

            data.put("userBeforeInsert_username", newUser.getUsername());
            data.put("userBeforeInsert_role", newUser.getRole());
            data.put("userBeforeInsert_status", newUser.getStatus());
            data.put("userBeforeInsert_passwordPrefix",
                    newUser.getPassword().substring(0, 20) + "...");

            int rows = sysUserMapper.insert(newUser);
            data.put("insertAffectedRows", rows);
            data.put("insertedUserId", newUser.getId());
            data.put("result", "插入成功 ✓");

            // 验证是否真的插入了
            SysUser found = sysUserMapper.selectById(newUser.getId());
            data.put("verifyFound", found != null);
            if (found != null) {
                data.put("verifyUsername", found.getUsername());
                data.put("verifyCreateTime", found.getCreateTime() != null ? found.getCreateTime().toString() : "null");
            }

            // 清理测试数据
            sysUserMapper.deleteById(newUser.getId());
            data.put("cleanupDone", true);

            return Result.success(data);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", e.getMessage());
            err.put("errorType", e.getClass().getName());
            StringWriter sw = new StringWriter();
            e.printStackTrace(new java.io.PrintWriter(sw));
            err.put("stacktrace", sw.toString().substring(0, Math.min(1200, sw.toString().length())));
            return Result.error(500, "调试失败: " + e.getMessage());
        }
    }
}
