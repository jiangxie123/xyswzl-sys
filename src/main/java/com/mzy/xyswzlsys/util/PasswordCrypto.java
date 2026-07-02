package com.mzy.xyswzlsys.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 密码加解密工具
 * 与前端 encrypt.js 保持一致的对称加密算法
 * 用于：登录/注册时前端提交的加密密码 -> 后端解密为明文 -> BCrypt 存储/比对
 */
public class PasswordCrypto {

    /**
     * 加密密钥（必须与前端 encrypt.js 中的 ENCRYPT_KEY 保持一致）
     */
    private static final String ENCRYPT_KEY = "xyswzl-sys-encryption-key-2026";

    /**
     * 加密数据前缀标识（前端会加 ENC: 前缀）
     */
    private static final String ENC_PREFIX = "ENC:";

    /**
     * 解密前端加密后的密码
     * 若传入的是普通明文（不带 ENC: 前缀），则原样返回（兼容测试场景）
     */
    public static String decryptPassword(String encryptedPassword) {
        if (encryptedPassword == null || encryptedPassword.isEmpty()) {
            return encryptedPassword;
        }

        // 不带加密前缀 → 视为明文，直接返回（兼容旧数据/测试）
        if (!encryptedPassword.startsWith(ENC_PREFIX)) {
            return encryptedPassword;
        }

        // 去掉前缀
        String base64Str = encryptedPassword.substring(ENC_PREFIX.length());

        try {
            // Base64 解码 → 得到异或后的字节
            byte[] xorBytes = Base64.getDecoder().decode(base64Str);

            // 使用异或方式还原为明文字符串
            byte[] keyBytes = ENCRYPT_KEY.getBytes(StandardCharsets.UTF_8);
            char[] result = new char[xorBytes.length];
            for (int i = 0; i < xorBytes.length; i++) {
                result[i] = (char) (xorBytes[i] ^ keyBytes[i % keyBytes.length]);
            }

            return new String(result);
        } catch (Exception e) {
            // 解码失败 → 返回原值（让后续 BCrypt 比对失败，提示账号或密码错误）
            return encryptedPassword;
        }
    }

    /**
     * 仅用于调试：加密一个字符串（与前端算法一致）
     */
    public static String encryptPassword(String plainPassword) {
        if (plainPassword == null) return null;
        byte[] keyBytes = ENCRYPT_KEY.getBytes(StandardCharsets.UTF_8);
        char[] chars = plainPassword.toCharArray();
        byte[] xorBytes = new byte[chars.length];
        for (int i = 0; i < chars.length; i++) {
            xorBytes[i] = (byte) (chars[i] ^ keyBytes[i % keyBytes.length]);
        }
        String base64 = Base64.getEncoder().encodeToString(xorBytes);
        return ENC_PREFIX + base64;
    }
}
