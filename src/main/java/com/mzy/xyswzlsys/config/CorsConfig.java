package com.mzy.xyswzlsys.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * 跨域配置
 * 允许前端（通常运行在 http://localhost:5173）调用后端（http://localhost:8080）的 API
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        // 允许所有来源（开发环境使用，生产环境建议指定具体域名）
        config.addAllowedOriginPattern("*");
        // 允许携带凭证（Cookie / Token）
        config.setAllowCredentials(true);
        // 允许所有请求方法（GET, POST, PUT, DELETE, OPTIONS）
        config.addAllowedMethod("*");
        // 允许所有请求头（用于接收 Authorization、Content-Type 等）
        config.addAllowedHeader("*");
        // 暴露响应头（让前端能读取 Content-Disposition 等自定义头）
        config.addExposedHeader("*");
        // 预检请求有效期（秒），减少 OPTIONS 请求频率
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 对所有 URL 生效
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
