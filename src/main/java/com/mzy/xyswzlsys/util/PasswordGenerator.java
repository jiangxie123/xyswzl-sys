package com.mzy.xyswzlsys.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * BCrypt 密码生成工具（临时使用，用于生成数据库测试用户密码）
 */
public class PasswordGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPassword = args.length > 0 ? args[0] : "123456";
        String encodedPassword = encoder.encode(rawPassword);
        System.out.println("原密码: " + rawPassword);
        System.out.println("BCrypt: " + encodedPassword);
        System.out.println();
        System.out.println("MySQL 更新语句:");
        System.out.println("UPDATE sys_user SET password = '" + encodedPassword + "' WHERE username = 'admin';");
        System.out.println("UPDATE sys_user SET password = '" + encodedPassword + "' WHERE username = 'student';");
        System.out.println("UPDATE sys_user SET password = '" + encodedPassword + "' WHERE username = 'zhangsan';");
        System.out.println("UPDATE sys_user SET password = '" + encodedPassword + "' WHERE username = 'lisi';");
    }
}
