-- ============================================================
-- 校园失物招领系统 - 完整数据库初始化脚本
-- 数据库名: xyswzl_db
-- 字符集: utf8mb4
-- 排序规则: utf8mb4_unicode_ci
-- 适用版本: MySQL 8.0+
-- 使用说明: 在 IDEA 数据库面板连接 MySQL 后，直接右键执行此文件
-- ============================================================

-- ------------------------------------------------------------
-- 第 1 步: 创建数据库
-- ------------------------------------------------------------
-- 如果数据库已存在则先删除（谨慎操作，仅在初始化场景使用）
DROP DATABASE IF EXISTS `xyswzl_db`;

-- 创建数据库，指定 utf8mb4 字符集以支持 emoji 和中文
CREATE DATABASE `xyswzl_db`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci
    DEFAULT ENCRYPTION = 'N';

-- 切换到目标数据库
USE `xyswzl_db`;

-- ------------------------------------------------------------
-- 第 2 步: 创建用户表 sys_user
-- 说明: 存储所有用户（学生 + 管理员）的基本信息
-- ------------------------------------------------------------
CREATE TABLE `sys_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID（主键）',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名（登录账号）',
    `password` VARCHAR(200) NOT NULL COMMENT '密码（BCrypt加密存储）',
    `real_name` VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱地址',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像图片URL',
    `student_id` VARCHAR(20) DEFAULT NULL COMMENT '学号',
    `college` VARCHAR(100) DEFAULT NULL COMMENT '所属学院',
    `role` TINYINT NOT NULL DEFAULT 0 COMMENT '角色：0-学生用户，1-普通管理员，2-超级管理员',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '账号状态：0-禁用，1-正常',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建人ID',
    `update_by` BIGINT DEFAULT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`) COMMENT '用户名唯一索引',
    UNIQUE KEY `uk_student_id` (`student_id`) COMMENT '学号唯一索引',
    KEY `idx_role` (`role`) COMMENT '角色索引',
    KEY `idx_status` (`status`) COMMENT '状态索引'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='用户表';

-- ------------------------------------------------------------
-- 第 3 步: 创建物品分类表 item_category
-- 说明: 失物/招领物品的分类字典表
-- ------------------------------------------------------------
CREATE TABLE `item_category` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分类ID（主键）',
    `category_name` VARCHAR(50) NOT NULL COMMENT '分类名称（如：证件类、电子产品、衣物、书籍）',
    `category_code` VARCHAR(50) NOT NULL COMMENT '分类编码（唯一标识，如：ID_CARD、ELECTRONICS）',
    `icon` VARCHAR(100) DEFAULT NULL COMMENT '分类图标URL',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序号（数值越小越靠前）',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建人ID',
    `update_by` BIGINT DEFAULT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_category_code` (`category_code`) COMMENT '分类编码唯一索引',
    UNIQUE KEY `uk_category_name` (`category_name`) COMMENT '分类名称唯一索引',
    KEY `idx_status` (`status`) COMMENT '状态索引',
    KEY `idx_sort_order` (`sort_order`) COMMENT '排序索引'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='物品分类表';

-- ------------------------------------------------------------
-- 第 4 步: 创建寻物/拾物信息表 item_info
-- 说明: 核心业务表，寻物和拾物信息通过 type 字段区分
-- ------------------------------------------------------------
CREATE TABLE `item_info` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '物品信息ID（主键）',
    `type` TINYINT NOT NULL COMMENT '类型：0-寻物（失物招领），1-拾物（捡到物品）',
    `user_id` BIGINT NOT NULL COMMENT '发布用户ID（关联：sys_user.id）',
    `category_id` BIGINT NOT NULL COMMENT '物品分类ID（逻辑关联：item_category.id）',
    `title` VARCHAR(100) NOT NULL COMMENT '标题',
    `description` TEXT DEFAULT NULL COMMENT '详细描述（物品特征、丢失/捡到地点、时间等）',
    `images` VARCHAR(1000) DEFAULT NULL COMMENT '图片URL列表（JSON数组格式，如:["url1","url2"]）',
    `location` VARCHAR(200) DEFAULT NULL COMMENT '地点（如：教学楼A座、图书馆二楼）',
    `lost_time` DATETIME DEFAULT NULL COMMENT '丢失或捡到的时间',
    `contact_phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
    `contact_wechat` VARCHAR(50) DEFAULT NULL COMMENT '微信号',
    `contact_qq` VARCHAR(20) DEFAULT NULL COMMENT 'QQ号',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-待找回/待认领，1-已找回/已认领，2-已下架',
    `audit_status` TINYINT NOT NULL DEFAULT 0 COMMENT '审核状态：0-待审核，1-审核通过，2-审核驳回',
    `audit_remark` VARCHAR(500) DEFAULT NULL COMMENT '审核备注（驳回原因等）',
    `claim_user_id` BIGINT DEFAULT NULL COMMENT '认领人ID（关联：sys_user.id，仅在status=1时有效）',
    `claim_time` DATETIME DEFAULT NULL COMMENT '认领完成时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建人ID',
    `update_by` BIGINT DEFAULT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`) COMMENT '发布用户索引',
    KEY `idx_category_id` (`category_id`) COMMENT '分类索引',
    KEY `idx_type` (`type`) COMMENT '类型索引',
    KEY `idx_status` (`status`) COMMENT '状态索引',
    KEY `idx_audit_status` (`audit_status`) COMMENT '审核状态索引',
    KEY `idx_type_status` (`type`, `status`) COMMENT '类型+状态组合索引',
    KEY `idx_audit_status_type` (`audit_status`, `type`) COMMENT '审核状态+类型组合索引',
    KEY `idx_lost_time` (`lost_time`) COMMENT '丢失/捡到时间索引',
    KEY `idx_claim_user_id` (`claim_user_id`) COMMENT '认领人索引'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='寻物/拾物信息表';

-- ------------------------------------------------------------
-- 第 5 步: 创建留言表 item_comment
-- 说明: 用户在物品详情下的留言交流记录
-- ------------------------------------------------------------
CREATE TABLE `item_comment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '留言ID（主键）',
    `item_id` BIGINT NOT NULL COMMENT '物品信息ID（关联：item_info.id）',
    `user_id` BIGINT NOT NULL COMMENT '留言用户ID（关联：sys_user.id）',
    `content` TEXT NOT NULL COMMENT '留言内容',
    `parent_id` BIGINT DEFAULT 0 COMMENT '父留言ID（0表示顶层留言，大于0表示回复）',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_item_id` (`item_id`) COMMENT '物品索引',
    KEY `idx_user_id` (`user_id`) COMMENT '留言用户索引',
    KEY `idx_parent_id` (`parent_id`) COMMENT '父留言索引（用于回复层级）',
    KEY `idx_status` (`status`) COMMENT '状态索引'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='留言表';

-- ------------------------------------------------------------
-- 第 6 步: 创建管理员操作日志表 admin_operation_log
-- 说明: 记录管理员所有关键操作，便于审计和追溯
-- ------------------------------------------------------------
CREATE TABLE `admin_operation_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID（主键）',
    `admin_id` BIGINT NOT NULL COMMENT '操作管理员ID（关联：sys_user.id）',
    `admin_name` VARCHAR(50) NOT NULL COMMENT '操作管理员姓名（冗余字段，便于直接展示）',
    `operation_type` VARCHAR(50) NOT NULL COMMENT '操作类型（如：CREATE、UPDATE、DELETE、AUDIT、QUERY、LOGIN）',
    `operation_module` VARCHAR(50) NOT NULL COMMENT '操作模块（如：USER、ITEM、CATEGORY、COMMENT、SYSTEM）',
    `operation_desc` VARCHAR(500) DEFAULT NULL COMMENT '操作描述（详细说明操作内容）',
    `target_id` BIGINT DEFAULT NULL COMMENT '操作目标ID（如：审核的物品ID、删除的用户ID）',
    `target_type` VARCHAR(50) DEFAULT NULL COMMENT '操作目标类型（对应操作的表名）',
    `ip_address` VARCHAR(50) DEFAULT NULL COMMENT '操作IP地址',
    `user_agent` VARCHAR(500) DEFAULT NULL COMMENT '用户代理（浏览器信息）',
    `result` TINYINT NOT NULL DEFAULT 1 COMMENT '操作结果：0-失败，1-成功',
    `error_msg` VARCHAR(500) DEFAULT NULL COMMENT '错误信息（操作失败时记录）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY `idx_admin_id` (`admin_id`) COMMENT '管理员索引',
    KEY `idx_operation_type` (`operation_type`) COMMENT '操作类型索引',
    KEY `idx_operation_module` (`operation_module`) COMMENT '操作模块索引',
    KEY `idx_create_time` (`create_time`) COMMENT '操作时间索引',
    KEY `idx_target_id` (`target_id`) COMMENT '操作目标索引'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='管理员操作日志表';

-- ============================================================
-- 第 7 步: 初始化基础数据
-- ============================================================

-- 7.1 初始化物品分类
INSERT INTO `item_category` (`category_name`, `category_code`, `sort_order`, `status`) VALUES
('证件类', 'ID_CARD', 1, 1),
('电子产品', 'ELECTRONICS', 2, 1),
('衣物', 'CLOTHING', 3, 1),
('书籍', 'BOOKS', 4, 1),
('生活用品', 'DAILY_USE', 5, 1),
('钥匙', 'KEYS', 6, 1),
('卡券类', 'CARDS', 7, 1),
('箱包类', 'BAGS', 8, 1),
('其他', 'OTHER', 100, 1);

-- 7.2 初始化超级管理员账号
-- 用户名: admin
-- 密码: admin123（以下为 BCrypt 加密值，请运行项目时使用 BCryptPasswordEncoder 加密后替换）
-- 提示: 生产环境请务必更换默认密码
INSERT INTO `sys_user` (`username`, `password`, `real_name`, `phone`, `role`, `status`, `student_id`, `college`) VALUES
('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjzqAKL9xL5jvMFVdNJHvGCgTq/VEq', '超级管理员', '13800138000', 2, 1, 'admin001', '系统管理部'),
('manager', '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjzqAKL9xL5jvMFVdNJHvGCgTq/VEq', '普通管理员', '13800138001', 1, 1, 'mgr001', '信息学院'),
('student01', '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjzqAKL9xL5jvMFVdNJHvGCgTq/VEq', '张三', '13800138002', 0, 1, '2024001', '信息学院'),
('student02', '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjzqAKL9xL5jvMFVdNJHvGCgTq/VEq', '李四', '13800138003', 0, 1, '2024002', '经济学院');

-- 7.3 初始化测试用物品信息（可选）
INSERT INTO `item_info` (`type`, `user_id`, `category_id`, `title`, `description`, `location`, `lost_time`,
                        `contact_phone`, `status`, `audit_status`) VALUES
(0, 3, 1, '丢失学生证', '棕色学生证，姓名张三，学号2024001', '图书馆一楼', '2024-07-01 08:30:00', '13800138002', 0, 1),
(0, 4, 2, '丢失黑色手机', '黑色小米手机，屏幕有轻微划痕', '教学楼C座301', '2024-07-02 14:00:00', '13800138003', 0, 1),
(1, 3, 3, '捡到一件蓝色外套', '在操场东侧看台捡到，蓝色运动外套，size M', '操场东侧看台', '2024-07-01 17:30:00', '13800138002', 0, 1);

-- 7.4 初始化测试留言（可选）
INSERT INTO `item_comment` (`item_id`, `user_id`, `content`) VALUES
(1, 4, '我也在图书馆，帮你留意一下~'),
(1, 3, '谢谢！'),
(2, 3, '我在附近看到过类似的手机，联系我');

-- ============================================================
-- 执行完成提示
-- ============================================================
-- 数据库初始化完成！
-- 数据库名: xyswzl_db
-- 默认管理员账号: admin / admin123
-- 注意: 生产环境请务必修改默认密码！
