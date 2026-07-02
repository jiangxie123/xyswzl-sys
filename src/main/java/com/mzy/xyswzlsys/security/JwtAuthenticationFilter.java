package com.mzy.xyswzlsys.security;

import com.mzy.xyswzlsys.service.TokenStoreService;
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
 * JWT 认证过滤器（完整鉴权逻辑）
 *
 * 鉴权流程（对每个需要认证的请求）：
 *   1. 从请求头 Authorization: Bearer {token} 中提取 Token
 *   2. 检查 JWT 签名是否正确 → 判断是否过期
 *   3. 检查 Redis 中是否存在该 Token → 判断是否已被主动失效（如用户已登出）
 *   4. 从 JWT 中解析出 userId、username、role
 *   5. 将用户信息放入 Spring Security 上下文，后续业务代码可直接读取
 *
 * 降级策略：当 Redis 不可用时，跳过第 3 步，只保留 JWT 本身的校验
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenUtil jwtTokenUtil;
    private final TokenStoreService tokenStoreService;

    public JwtAuthenticationFilter(JwtTokenUtil jwtTokenUtil, TokenStoreService tokenStoreService) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.tokenStoreService = tokenStoreService;
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
                // 步骤 1：校验 JWT 本身（签名 + 有效期）
                if (!jwtTokenUtil.validateToken(token)) {
                    // Token 无效（签名错误 / 已过期）→ 清空上下文，交由 Spring Security 拒绝
                    SecurityContextHolder.clearContext();
                    filterChain.doFilter(request, response);
                    return;
                }

                // 步骤 2：校验 Redis 中是否存在该 Token（防止已登出的 Token 继续使用）
                // 注意：Redis 不可用时 isTokenValid 会返回 true（降级策略）
                if (!tokenStoreService.isTokenValid(token)) {
                    // Redis 中不存在 → 该 Token 已被主动失效（用户登出/管理员强制）
                    SecurityContextHolder.clearContext();
                    filterChain.doFilter(request, response);
                    return;
                }

                // 步骤 3：从 JWT 中解析用户信息
                String username = jwtTokenUtil.getUsernameFromToken(token);
                Integer role = jwtTokenUtil.getRoleFromToken(token);

                // 步骤 4：将角色映射为权限字符串
                String authority = switch (role == null ? 0 : role) {
                    case 1 -> "ROLE_ADMIN";
                    case 2 -> "ROLE_SUPER_ADMIN";
                    default -> "ROLE_STUDENT";
                };

                // 步骤 5：将用户信息放入 Spring Security 上下文
                UserDetails userDetails = new User(
                        username,
                        "",
                        Collections.singletonList(new SimpleGrantedAuthority(authority))
                );

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(token);
                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (Exception e) {
                // 解析过程中出现异常 → 清空上下文，后续 Spring Security 按未认证处理
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
