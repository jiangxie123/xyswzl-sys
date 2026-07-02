package com.mzy.xyswzlsys.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 物品信息实体类（寻物 / 拾物 统一表）
 * 对应数据库表：item_info
 */
@Data
@TableName("item_info")
public class ItemInfo {

    /** 物品信息ID（主键，自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 类型：0-寻物（丢失物品），1-拾物（捡到物品） */
    private Integer type;

    /** 发布用户ID */
    private Long userId;

    /** 物品分类ID */
    private Long categoryId;

    /** 标题 */
    private String title;

    /** 详细描述 */
    private String description;

    /** 图片URL列表（JSON数组格式） */
    private String images;

    /** 地点 */
    private String location;

    /** 丢失或捡到的时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lostTime;

    /** 联系电话 */
    private String contactPhone;

    /** 微信号 */
    private String contactWechat;

    /** QQ号 */
    private String contactQq;

    /** 状态：0-待找回/待认领，1-已找回/已认领，2-已下架 */
    private Integer status;

    /** 审核状态：0-待审核，1-审核通过，2-审核驳回 */
    private Integer auditStatus;

    /** 审核备注（驳回原因等） */
    private String auditRemark;

    /** 认领人ID（仅在 status=1 时有效） */
    private Long claimUserId;

    /** 认领完成时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime claimTime;

    /** 创建时间 */
    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /** 创建人ID */
    private Long createBy;

    /** 更新人ID */
    private Long updateBy;
}
