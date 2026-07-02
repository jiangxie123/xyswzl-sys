package com.mzy.xyswzlsys.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 前端资源配置：支持 Vue Router 的 HTML5 History 模式
 * 当用户直接访问 /login、/items 等前端路由时，返回 index.html 让 Vue Router 处理
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // 所有非 /api 的路径都转发到 index.html，由 Vue Router 处理
        // 常见的前端路由
        String[] frontRoutes = {
            "/login",
            "/items",
            "/items/{id}",
            "/publish",
            "/profile",
            "/admin/users",
            "/admin/categories",
            "/admin/audit",
            "/admin/logs"
        };
        for (String route : frontRoutes) {
            registry.addViewController(route).setViewName("forward:/index.html");
        }
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 确保静态资源（js/css/images）能正确加载
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("classpath:/static/assets/");
    }
}
