package com.mzy.xyswzlsys.service.impl;

import com.mzy.xyswzlsys.service.TokenStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Token 存储实现（使用 Redis）
 *
 * 设计：
 * 1. Key = "auth:token:{token}"，Value = "{userId}"
 * 2. TTL = 配置文件指定的 expire-seconds（默认 24 小时）
 * 3. 当 Redis 不可用时，打印警告但不抛出异常 → 后续可在 Filter 中降级为只校验 JWT 本身
 */
@Service
public class RedisTokenStoreServiceImpl implements TokenStoreService {

    private static final Logger log = LoggerFactory.getLogger(RedisTokenStoreServiceImpl.class);

    private static final String TOKEN_KEY_PREFIX = "auth:token:";

    private final StringRedisTemplate redisTemplate;

    /** Token 有效期（秒），从 application.properties 读取 */
    @Value("${auth.token.expire-seconds:86400}")
    private long tokenExpireSeconds;

    /** 用于标记 Redis 是否可用（启动失败或连接异常时置为 false） */
    private volatile boolean redisAvailable = true;

    public RedisTokenStoreServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        // 启动时探测一下 Redis 是否正常
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            log.info("[TokenStore] Redis 连接正常，Token 将存储在 Redis");
        } catch (Exception e) {
            log.warn("[TokenStore] Redis 连接失败：{}，将降级为仅校验 JWT 本身", e.getMessage());
            redisAvailable = false;
        }
    }

    @Override
    public void storeToken(String token, Long userId) {
        if (!redisAvailable) {
            log.debug("[TokenStore] Redis 不可用，跳过 token 存储");
            return;
        }
        try {
            String key = TOKEN_KEY_PREFIX + token;
            redisTemplate.opsForValue().set(key, String.valueOf(userId), tokenExpireSeconds, TimeUnit.SECONDS);
        } catch (RedisConnectionFailureException e) {
            log.warn("[TokenStore] storeToken 失败：Redis 连接异常，降级为仅校验 JWT");
            redisAvailable = false;
        } catch (Exception e) {
            log.warn("[TokenStore] storeToken 失败：{}", e.getMessage());
        }
    }

    @Override
    public boolean isTokenValid(String token) {
        if (!redisAvailable) {
            // Redis 不可用时视为 Token 有效（由 JWT 本身签名保证），这是降级策略
            return true;
        }
        try {
            String key = TOKEN_KEY_PREFIX + token;
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.debug("[TokenStore] isTokenValid 查询异常，降级为有效：{}", e.getMessage());
            return true;
        }
    }

    @Override
    public Long getUserIdByToken(String token) {
        if (!redisAvailable) return null;
        try {
            String key = TOKEN_KEY_PREFIX + token;
            String value = redisTemplate.opsForValue().get(key);
            return value == null ? null : Long.parseLong(value);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void removeToken(String token) {
        if (!redisAvailable) return;
        try {
            String key = TOKEN_KEY_PREFIX + token;
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.debug("[TokenStore] removeToken 失败：{}", e.getMessage());
        }
    }

    @Override
    public boolean isAvailable() {
        return redisAvailable;
    }
}
