package com.mzy.xyswzlsys.service;

/**
 * Token 存储服务
 * 负责：将登录后的 Token 存入 Redis，并提供 Token 是否有效、主动失效等能力
 */
public interface TokenStoreService {

    /**
     * 保存 Token（Redis：key = token，value = userId，TTL = 有效期）
     */
    void storeToken(String token, Long userId);

    /**
     * 校验 Token 是否存在（未被主动失效且未过期）
     * @return true 表示有效，false 表示已失效/不存在
     */
    boolean isTokenValid(String token);

    /**
     * 从 Token 获取关联的用户 ID
     */
    Long getUserIdByToken(String token);

    /**
     * 主动删除 Token（用户退出登录时调用）
     */
    void removeToken(String token);

    /**
     * 判断 Token 存储是否可用（用于启动时的容错判断）
     */
    boolean isAvailable();
}
