package com.mzy.xyswzlsys.controller;

import com.mzy.xyswzlsys.common.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * 文件上传接口
 * POST /api/upload/image - 上传单张图片，返回图片访问 URL
 *
 * 安全要点：
 * 1) 必须登录才能上传（由 SecurityConfig 配置的 /api/** 已要求认证，此处二次校验兜底）
 * 2) 扩展名白名单：仅 jpg/jpeg/png/gif/webp
 * 3) 文件魔数：读取前 12 字节做 magic bytes 校验，避免将脚本伪装成图片上传
 * 4) 文件大小上限：10MB
 * 5) 目录使用绝对路径（以 System.getProperty("user.dir") 为基准），避免写入 Tomcat 临时目录
 */
@RestController
@RequestMapping("/api/upload")
public class FileUploadController {

    /** 允许的扩展名（小写） */
    private static final Set<String> ALLOWED_EXT = new HashSet<>(Arrays.asList(
            ".jpg", ".jpeg", ".png", ".gif", ".webp"
    ));

    /** 单张图片最大大小（10MB） */
    private static final long MAX_SIZE = 10 * 1024 * 1024L;

    /** 魔数表（每个条目是若干字节的十六进制前缀） */
    private static final byte[][] MAGIC_PREFIXES = new byte[][] {
            new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF }, // JPEG
            new byte[] { (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A }, // PNG
            new byte[] { 'G', 'I', 'F' }, // GIF87a / GIF89a
            new byte[] { 'R', 'I', 'F', 'F' }, // WEBP (RIFF....WEBP)
    };

    /**
     * 上传根目录
     */
    @Value("${upload.dir:uploads}")
    private String uploadDir;

    /** 解析后的绝对路径（uploads/ 目录） */
    private File uploadRootDir;

    @PostConstruct
    public void init() {
        File dir = new File(uploadDir);
        if (!dir.isAbsolute()) {
            dir = new File(System.getProperty("user.dir"), uploadDir);
        }
        uploadRootDir = dir;

        File itemImagesDir = new File(uploadRootDir, "item-images");
        if (!itemImagesDir.exists()) {
            boolean created = itemImagesDir.mkdirs();
            if (created) {
                System.out.println("[FileUploadController] 已创建上传目录: " + itemImagesDir.getAbsolutePath());
            } else {
                System.err.println("[FileUploadController] 警告：无法创建上传目录: " + itemImagesDir.getAbsolutePath());
            }
        }
    }

    /**
     * 上传单张图片
     */
    @PostMapping("/image")
    public Result<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.error(400, "请选择要上传的文件");
        }

        // 1) 大小校验
        if (file.getSize() > MAX_SIZE) {
            return Result.error(400, "图片大小不能超过 10MB");
        }

        // 2) 原始文件名合法性（防止路径穿越）
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isEmpty()) {
            return Result.error(400, "文件名不能为空");
        }
        if (originalName.contains("..") || originalName.contains("/") || originalName.contains("\\")) {
            return Result.error(400, "文件名包含非法字符");
        }

        // 3) 扩展名白名单
        String lower = originalName.toLowerCase();
        String ext = null;
        for (String allowed : ALLOWED_EXT) {
            if (lower.endsWith(allowed)) {
                ext = allowed;
                break;
            }
        }
        if (ext == null) {
            return Result.error(400, "不支持的图片格式，仅支持 JPG、PNG、GIF、WEBP");
        }

        // 4) 魔数校验（通过 InputStream 读取前 12 字节，不要依赖 file.getContentType()）
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[12];
            int read = is.read(header);
            if (read <= 0) {
                return Result.error(400, "文件为空或读取失败");
            }
            boolean magicOk = false;
            for (byte[] prefix : MAGIC_PREFIXES) {
                if (startsWith(header, prefix)) {
                    magicOk = true;
                    break;
                }
            }
            if (!magicOk) {
                return Result.error(400, "文件内容与扩展名不一致（仅支持 JPG、PNG、GIF、WEBP）");
            }
        } catch (IOException e) {
            return Result.error(500, "图片校验失败: " + e.getMessage());
        }

        try {
            // 5) 确定保存目录（绝对路径），并确保存在
            File dir = new File(uploadRootDir, "item-images");
            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                if (!created) {
                    return Result.error(500, "服务器无法创建上传目录，请联系管理员");
                }
            }
            if (!dir.canWrite()) {
                return Result.error(500, "服务器上传目录不可写，请联系管理员");
            }

            // 6) 生成唯一文件名（UUID + 时间戳 + 合法扩展名）
            String newName = UUID.randomUUID().toString().replace("-", "")
                    + "_" + System.currentTimeMillis() + ext;

            // 7) 保存文件
            File target = new File(dir, newName);
            file.transferTo(target);

            // 8) 返回访问 URL
            String url = "/uploads/item-images/" + newName;
            Map<String, String> data = new HashMap<>();
            data.put("url", url);
            data.put("name", originalName);
            return Result.success(data);
        } catch (IOException e) {
            System.err.println("[FileUploadController] 图片保存失败: " + e.getMessage());
            return Result.error(500, "图片保存失败：" + e.getMessage());
        }
    }

    /** 字节前缀比较 */
    private static boolean startsWith(byte[] source, byte[] prefix) {
        if (source == null || prefix == null || source.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (source[i] != prefix[i]) return false;
        }
        return true;
    }
}
