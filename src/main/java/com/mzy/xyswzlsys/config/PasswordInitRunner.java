package com.mzy.xyswzlsys.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 数据库密码初始化器
 * 在应用启动时，将数据库中所有用户的密码更新为 BCrypt 加密
 */
@Component
@Order(1)
public class PasswordInitRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public PasswordInitRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            // 1. 查询所有用户
            List<Map<String, Object>> users = jdbcTemplate.queryForList(
                    "SELECT id, username, password FROM sys_user");

            if (users.isEmpty()) {
                System.out.println("[密码初始化] 没有用户，跳过");
                return;
            }

            System.out.println("========================================");
            System.out.println("[密码初始化] 发现 " + users.size() + " 个用户，正在更新密码...");
            System.out.println("----------------------------------------");

            // 2. 生成 BCrypt 加密的 "123456"
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            String bcryptPassword = encoder.encode("123456");

            System.out.println("明文密码: 123456");
            System.out.println("BCrypt 格式: " + bcryptPassword);
            System.out.println("----------------------------------------");

            // 3. 更新所有用户的密码
            for (Map<String, Object> user : users) {
                jdbcTemplate.update("UPDATE sys_user SET password = ? WHERE id = ?",
                        bcryptPassword, user.get("id"));
                System.out.println("  ✅ 用户 " + user.get("username") + " (id=" + user.get("id") + ") 密码已更新");
            }

            // 4. 验证
            System.out.println("----------------------------------------");
            System.out.println("[密码初始化] 验证结果：");
            List<Map<String, Object>> afterUpdate = jdbcTemplate.queryForList(
                    "SELECT id, username, LEFT(password, 20) as pw_prefix FROM sys_user");
            for (Map<String, Object> user : afterUpdate) {
                System.out.println("  id=" + user.get("id")
                        + ", username=" + user.get("username")
                        + ", password=" + user.get("pw_prefix") + "...");
            }
            System.out.println("========================================");

        } catch (Exception e) {
            System.out.println("[密码初始化] 错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
