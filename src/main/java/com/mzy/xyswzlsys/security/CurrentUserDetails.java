package com.mzy.xyswzlsys.security;

/**
 * 在 JwtAuthenticationFilter 中解析的当前登录用户的简要信息，
 * 作为 UsernamePasswordAuthenticationToken 的 details 字段放入 Spring Security 上下文。
 *
 * 业务代码可以通过 SecurityContextHolder.getContext().getAuthentication().getDetails()
 * 或调用本类的静态方法 CurrentUserDetails.current() 获取。
 */
public class CurrentUserDetails {

    private final Long userId;
    private final String username;
    private final Integer role;

    public CurrentUserDetails(Long userId, String username, Integer role) {
        this.userId = userId;
        this.username = username;
        this.role = role;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public Integer getRole() {
        return role;
    }
}
