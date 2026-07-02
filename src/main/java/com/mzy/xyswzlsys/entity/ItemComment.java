package com.mzy.xyswzlsys.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 留言实体类
 * 对应数据库表：item_comment
 */
@Data
@TableName("item_comment")
public class ItemComment {

    /** 留言ID（主键，自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 物品信息ID（关联 item_info.id） */
    private Long itemId;

    /** 留言用户ID（关联 sys_user.id） */
    private Long userId;

    /** 留言内容 */
    private String content;

    /** 父留言ID（0表示顶层留言，其他表示回复某条留言） */
    private Long parentId;

    /** 状态：0-禁用，1-正常 */
    private Integer status;

    /** 创建时间 */
    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
