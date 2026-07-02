package com.mzy.xyswzlsys.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 物品分类实体类
 * 用于管理失物/拾物信息的分类标签
 */
@Data
@TableName("item_category")
public class ItemCategory {

    /**
     * 分类 ID（主键，自增）
     */
    private Long id;

    /**
     * 分类名称（如：电子产品）
     */
    @TableField("category_name")
    private String name;

    /**
     * 分类编码（如：ELECTRONICS）
     */
    @TableField("category_code")
    private String categoryCode;

    /**
     * 图标 URL
     */
    private String icon;

    /**
     * 排序权重（数值越小越靠前）
     */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 状态（1=正常启用，0=禁用）
     */
    private Integer status;

    /**
     * 创建时间（由 MyBatis-Plus MetaObjectHandler 自动填充）
     */
    @TableField(value = "create_time", fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间（由 MyBatis-Plus MetaObjectHandler 自动填充）
     */
    @TableField(value = "update_time", fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
