package com.mzy.xyswzlsys.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * 数据库连接诊断工具
 */
public class DbDiagnostic {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/xyswzl_db?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true";
        String username = "root";
        String password = "adc20050209";

        System.out.println("正在连接数据库...");
        System.out.println("URL: " + url);
        System.out.println("Username: " + username);
        System.out.println();

        try (Connection conn = DriverManager.getConnection(url, username, password);
             Statement stmt = conn.createStatement()) {

            System.out.println("✅ 数据库连接成功！");
            System.out.println("Database: " + conn.getCatalog());
            System.out.println();

            // 查询用户表
            try (ResultSet rs = stmt.executeQuery("SELECT id, username, role, status, LEFT(password, 20) as pw FROM sys_user")) {
                System.out.println("=== sys_user 表数据 ===");
                while (rs.next()) {
                    System.out.println("  id=" + rs.getInt("id")
                            + ", username=" + rs.getString("username")
                            + ", role=" + rs.getInt("role")
                            + ", status=" + rs.getInt("status")
                            + ", pw=" + rs.getString("pw"));
                }
            }

            // 测试表存在性
            String[] tables = {"sys_user", "item_category", "item_info", "item_comment", "admin_operation_log"};
            System.out.println();
            System.out.println("=== 表存在性检查 ===");
            for (String table : tables) {
                try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + table)) {
                    rs.next();
                    System.out.println("  " + table + ": " + rs.getInt(1) + " 行");
                } catch (Exception e) {
                    System.out.println("  " + table + ": ❌ 不存在或无权限");
                }
            }

        } catch (Exception e) {
            System.out.println("❌ 数据库连接失败: " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
