package com.mzy.xyswzlsys.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT 认证过滤器
 * 从请求 Header 中读取 Authorization: Bearer <token>
 * 解析 Token 后将用户信息放入 Spring Security 上下文
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenUtil jwtTokenUtil;

    public JwtAuthenticationFilter(JwtTokenUtil jwtTokenUtil) {
        this.jwtTokenUtil = jwtTokenUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // 有 Token 且以 Bearer 开头才进行认证
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                if (jwtTokenUtil.validateToken(token)) {
                    String username = jwtTokenUtil.getUsernameFromToken(token);
                    Integer role = jwtTokenUtil.getRoleFromToken(token);

                    // 将角色映射为权限字符串：ROLE_STUDENT / ROLE_ADMIN / ROLE_SUPER_ADMIN
                    String authority = switch (role == null ? 0 : role) {
                        case 1 -> "ROLE_ADMIN";
                        case 2 -> "ROLE_SUPER_ADMIN";
                        default -> "ROLE_STUDENT";
                    };

                    UserDetails userDetails = new User(
                            username,
                            "", // 密码已在登录阶段校验，此处不需要
                            Collections.singletonList(new SimpleGrantedAuthority(authority))
                    );

                    // 将认证信息放入 Security 上下文，后续接口可通过注解做权限控制
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(token); // 将原始 Token 放入详情方便业务层使用
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception e) {
                // Token 解析失败，Security 上下文为空，后续接口会被当作未认证
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
