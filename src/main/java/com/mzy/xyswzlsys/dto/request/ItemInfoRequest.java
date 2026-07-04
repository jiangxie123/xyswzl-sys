package com.mzy.xyswzlsys.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 物品信息新增/修改请求 DTO
 * 限制允许由前端修改的字段，避免：
 * 1) 绕过审核（直接设置 auditStatus=1）
 * 2) 伪造发布者（userId）
 * 3) 超长文本 / 非法枚举值注入
 */
@Data
public class ItemInfoRequest {

    /** 类型：0-寻物，1-拾物（必填） */
    @NotNull(message = "物品类型不能为空")
    private Integer type;

    /** 分类ID（必填） */
    @NotNull(message = "请选择物品分类")
    private Long categoryId;

    /** 标题（必填，2-100 字符） */
    @NotBlank(message = "标题不能为空")
    @Size(min = 2, max = 100, message = "标题长度需在 2-100 字符之间")
    private String title;

    /** 详细描述（必填，最长 2000 字符） */
    @NotBlank(message = "详细描述不能为空")
    @Size(max = 2000, message = "描述内容过长（最多 2000 字符）")
    private String description;

    /** 图片 URL 列表（JSON 字符串，可选，最多 2000 字符） */
    @Size(max = 2000, message = "图片信息过长")
    private String images;

    /** 地点（最长 100 字符） */
    @Size(max = 100, message = "地点描述过长")
    private String location;

    /** 丢失 / 捡到的时间（可选，由前端传字符串；Service 层再校验） */
    private String lostTime;

    /** 联系电话（最长 20 字符） */
    @Size(max = 20, message = "电话过长")
    private String contactPhone;

    /** 微信号（最长 30 字符） */
    @Size(max = 30, message = "微信号过长")
    private String contactWechat;

    /** QQ 号（最长 20 字符） */
    @Size(max = 20, message = "QQ 号过长")
    private String contactQq;
}
