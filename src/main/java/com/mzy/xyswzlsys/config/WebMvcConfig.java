package com.mzy.xyswzlsys.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.annotation.PostConstruct;
import java.io.File;

/**
 * 前端资源配置：支持 Vue Router 的 HTML5 History 模式
 * 当用户直接访问 /login、/items 等前端路由时，返回 index.html 让 Vue Router 处理
 * 同时配置文件上传目录的静态资源映射
 *
 * 注：文件上传大小限制（max-file-size / max-request-size）
 *     已在 application.properties 中配置，无需在此类中手动声明 Bean。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 文件上传根目录（与 FileUploadController 保持一致）。
     * 支持 application.properties 中通过 upload.dir=xxx 自定义，
     * 默认使用项目运行目录下的 uploads/（通过绝对路径确保稳定）。
     */
    @Value("${upload.dir:uploads}")
    private String uploadDir;

    /** 解析后的绝对路径，供静态资源映射使用 */
    private String uploadAbsolutePath;

    /**
     * 初始化：将 upload.dir 解析为绝对路径，并确保 uploads/item-images 子目录存在。
     * 避免 Tomcat 运行时当前工作目录（CWD）是临时目录导致路径不稳定。
     */
    @PostConstruct
    public void init() {
        File dir = new File(uploadDir);
        if (!dir.isAbsolute()) {
            // 相对路径 → 以应用运行目录（user.dir）为基准，避免依赖 Tomcat 临时目录
            dir = new File(System.getProperty("user.dir"), uploadDir);
        }
        uploadAbsolutePath = dir.getAbsolutePath() + File.separator;

        // 提前创建上传根目录及 item-images 子目录，避免首次上传时因目录不存在报错
        File itemImagesDir = new File(dir, "item-images");
        if (!itemImagesDir.exists()) {
            boolean created = itemImagesDir.mkdirs();
            if (created) {
                System.out.println("[WebMvcConfig] 已创建上传目录: " + itemImagesDir.getAbsolutePath());
            } else {
                System.err.println("[WebMvcConfig] 警告：无法创建上传目录: " + itemImagesDir.getAbsolutePath());
            }
        }
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // 常见的前端路由 → 转发到 index.html 由 Vue Router 处理
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
        // 1) 前端静态资源（构建产物 js/css，已由 Spring Boot 默认处理，可保留用于显式声明）
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("classpath:/static/assets/");

        // 2) 背景图片等静态资源：
        //    - 放在 frontend/public/images/ 下的文件（开发期使用 Vite 开发服务器直接访问）
        //    - 或放在 src/main/resources/static/images/ 下（构建/打包后由 Spring Boot 提供）
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/", "file:./frontend/public/images/");

        // 3) 上传目录静态资源映射（使用 init() 解析的绝对路径，与 FileUploadController 保持一致）：
        //    URL 形如 /uploads/item-images/xxx.jpg  → 本地 uploads/item-images/xxx.jpg
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadAbsolutePath);
    }
}
