package com.mzy.xyswzlsys.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 留言请求 DTO
 * 限制内容长度 + 校验 itemId 必填，避免：
 * 1) 超长文本 / XSS 注入内容被原样入库
 * 2) itemId 为空造成的无主留言 / 性能陷阱
 */
@Data
public class ItemCommentRequest {

    /** 物品ID（必填） */
    @NotNull(message = "物品ID不能为空")
    private Long itemId;

    /** 留言内容（必填，1-500 字符） */
    @NotBlank(message = "留言内容不能为空")
    @Size(min = 1, max = 500, message = "留言长度需在 1-500 字符之间")
    private String content;

    /** 父留言ID（可选，用于回复，0 或 null 表示一级留言） */
    private Long parentId;
}
