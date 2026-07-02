package com.mzy.xyswzlsys.controller;

import com.mzy.xyswzlsys.common.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * 文件上传接口
 * POST /api/upload/image - 上传单张图片，返回图片访问 URL
 *
 * 设计要点：
 * 1. 上传目录使用绝对路径解析：相对路径 → 以 System.getProperty("user.dir") 为基准
 *    避免 Tomcat 运行时 CWD 指向临时目录导致文件写入不可控位置
 * 2. 目录提前创建：@PostConstruct 初始化时即创建 uploads/item-images
 * 3. 目录创建检查：mkdirs() 返回值必须检查，失败时返回明确的错误信息
 */
@RestController
@RequestMapping("/api/upload")
public class FileUploadController {

    /** 允许的图片类型 */
    private static final Set<String> ALLOWED_TYPES = new HashSet<>(Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    ));

    /** 单张图片最大大小（10MB） */
    private static final long MAX_SIZE = 10 * 1024 * 1024L;

    /**
     * 上传根目录（与 WebMvcConfig 保持一致）。
     * 支持 application.properties 中通过 upload.dir=xxx 自定义，
     * 默认使用项目运行目录下的 uploads/。
     */
    @Value("${upload.dir:uploads}")
    private String uploadDir;

    /** 解析后的绝对路径（uploads/ 目录） */
    private File uploadRootDir;

    /**
     * 初始化：将 upload.dir 解析为绝对路径，并提前创建 uploads/item-images 子目录。
     * 避免首次上传时因目录不存在导致 FileNotFoundException。
     */
    @PostConstruct
    public void init() {
        File dir = new File(uploadDir);
        if (!dir.isAbsolute()) {
            // 相对路径 → 以应用运行目录（user.dir）为基准，避免依赖 Tomcat 临时目录
            dir = new File(System.getProperty("user.dir"), uploadDir);
        }
        uploadRootDir = dir;

        // 提前创建 item-images 子目录
        File itemImagesDir = new File(uploadRootDir, "item-images");
        if (!itemImagesDir.exists()) {
            boolean created = itemImagesDir.mkdirs();
            if (created) {
                System.out.println("[FileUploadController] 已创建上传目录: " + itemImagesDir.getAbsolutePath());
            } else {
                System.err.println("[FileUploadController] 警告：无法创建上传目录: " + itemImagesDir.getAbsolutePath()
                        + "，请检查目录权限或手动创建该目录");
            }
        }
    }

    /**
     * 上传单张图片
     * @param file 上传的文件（表单字段名：file）
     * @return 图片访问 URL（/uploads/item-images/xxx.jpg）
     */
    @PostMapping("/image")
    public Result<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.error(400, "请选择要上传的文件");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            return Result.error(400, "不支持的图片格式，仅支持 JPG、PNG、GIF、WEBP");
        }

        if (file.getSize() > MAX_SIZE) {
            return Result.error(400, "图片大小不能超过 10MB");
        }

        try {
            // 1) 确定保存目录（绝对路径），并确保存在
            File dir = new File(uploadRootDir, "item-images");
            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                if (!created) {
                    return Result.error(500, "服务器无法创建上传目录，请联系管理员（路径: " + dir.getAbsolutePath() + "）");
                }
            }

            // 2) 检查目录是否可写
            if (!dir.canWrite()) {
                return Result.error(500, "服务器上传目录不可写，请联系管理员（路径: " + dir.getAbsolutePath() + "）");
            }

            // 3) 生成唯一文件名（防止文件名冲突）
            String originalName = file.getOriginalFilename();
            String ext = "";
            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf("."));
            }
            String newName = UUID.randomUUID().toString().replace("-", "")
                    + "_" + System.currentTimeMillis() + ext.toLowerCase();

            // 4) 保存文件（使用 Files.copy 避免 transferTo 在某些环境下对相对路径敏感）
            File target = new File(dir, newName);
            file.transferTo(target);

            // 5) 返回访问 URL（通过 WebMvcConfig 的 /uploads/** 静态资源映射访问）
            String url = "/uploads/item-images/" + newName;

            Map<String, String> data = new HashMap<>();
            data.put("url", url);
            data.put("name", originalName);
            return Result.success(data);
        } catch (IOException e) {
            // 打印详细的错误信息，便于定位问题
            System.err.println("[FileUploadController] 图片保存失败: " + e.getMessage());
            return Result.error(500, "图片保存失败：" + e.getMessage());
        }
    }
}
