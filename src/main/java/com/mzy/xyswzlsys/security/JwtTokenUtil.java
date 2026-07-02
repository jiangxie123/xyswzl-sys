package com.mzy.xyswzlsys.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT Token 工具类
 * 负责：生成 Token、解析 Token、校验 Token
 */
@Component
public class JwtTokenUtil {

    /** Token 密钥（生产环境请修改为更复杂的字符串，建议从配置文件读取） */
    private static final String SECRET = "xyswzl-sys-secret-key-please-change-this-in-production-2026";

    /** Token 有效期：24 小时（单位毫秒） */
    private static final long EXPIRATION_MS = 24 * 60 * 60 * 1000L;

    private final SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    /**
     * 根据用户信息生成 Token
     */
    public String generateToken(Long userId, String username, Integer role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", role);

        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(key)
                .compact();
    }

    /**
     * 解析 Token 获取声明信息（过期 / 签名不对都会抛出异常）
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 从 Token 中获取用户名
     */
    public String getUsernameFromToken(String token) {
        return parseToken(token).getSubject();
    }

    /**
     * 从 Token 中获取用户 ID
     */
    public Long getUserIdFromToken(String token) {
        return parseToken(token).get("userId", Long.class);
    }

    /**
     * 从 Token 中获取角色
     */
    public Integer getRoleFromToken(String token) {
        Number role = parseToken(token).get("role", Number.class);
        return role != null ? role.intValue() : null;
    }

    /**
     * 校验 Token 是否有效（未过期 + 未被篡改）
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }
}
