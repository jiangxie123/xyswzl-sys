package com.mzy.xyswzlsys.security;

import com.mzy.xyswzlsys.common.Result;
import com.mzy.xyswzlsys.common.ResultCode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Spring Security 安全配置
 * 采用 JWT 无状态认证（不使用 Session）
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 禁用 CSRF（JWT Token 机制天然防护）
                .csrf(AbstractHttpConfigurer::disable)
                // 启用 CORS 支持（使用 CorsConfig 中的配置，允许前端 5173 端口跨域调用）
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // 无状态 Session，Spring Security 不创建 HttpSession
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 配置接口权限
                .authorizeHttpRequests(auth -> auth
                        // 健康检查接口放行（开发调试用）
                        .requestMatchers("/api/health/**").permitAll()
                        // 登录接口放行（无需 Token）
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        // 其他 /api/auth/* 默认放行
                        .requestMatchers("/api/auth/**").permitAll()
                        // 物品列表查询对公众开放（匿名用户也能看发布的失物/拾物信息）
                        .requestMatchers(HttpMethod.GET, "/api/items").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/items/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categories").permitAll()
                        // 分类管理接口只允许管理员访问
                        .requestMatchers("/api/categories/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        // 物品管理和审核接口需要管理员权限
                        .requestMatchers("/api/items/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/items/*/audit").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        // 管理员接口只有管理员角色才能访问
                        .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasRole("SUPER_ADMIN")
                        // 其他所有 API 都需要登录
                        .requestMatchers("/api/**").authenticated()
                        // 其他请求放行
                        .anyRequest().permitAll()
                )
                // 登录认证失败返回统一 JSON
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                            Result<Void> result = Result.error(ResultCode.UNAUTHORIZED.getCode(),
                                    "未登录或 Token 已失效");
                            response.getWriter().write(toJson(result));
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                            Result<Void> result = Result.error(ResultCode.FORBIDDEN.getCode(),
                                    "无权限访问该资源");
                            response.getWriter().write(toJson(result));
                        })
                )
                // 把 JWT 过滤器添加到 UsernamePasswordAuthenticationFilter 之前
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // 禁用默认的 formLogin 和 logout（我们使用自定义的登录接口）
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable);

        return http.build();
    }

    /**
     * CORS 配置源（配置跨域访问规则）
     * 允许前端 5173 端口调用后端 8080 端口
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 允许所有来源（开发环境，生产环境应配置具体域名）
        configuration.addAllowedOriginPattern("*");
        // 允许携带凭证（Token）
        configuration.setAllowCredentials(true);
        // 允许所有请求方法
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // 允许所有请求头
        configuration.addAllowedHeader("*");
        // 暴露所有响应头（供前端读取）
        configuration.addExposedHeader("*");
        // 预检请求有效期 1 小时
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * BCrypt 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 简单的 Result 转 JSON 工具，避免在编译期依赖 Jackson
     */
    private String toJson(Result<Void> result) {
        return "{\"code\":" + result.getCode() + ",\"message\":\"" + result.getMessage() + "\",\"data\":null}";
    }
}
