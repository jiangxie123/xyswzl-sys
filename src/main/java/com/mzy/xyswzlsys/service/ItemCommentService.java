package com.mzy.xyswzlsys.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mzy.xyswzlsys.entity.ItemComment;

import java.util.List;

/**
 * 物品留言 Service
 */
public interface ItemCommentService {

    /**
     * 分页查询某物品的留言（状态为正常的）
     */
    IPage<ItemComment> getCommentsByItemId(Long itemId, int current, int size);

    /**
     * 获取单条留言
     */
    ItemComment getCommentById(Long id);

    /**
     * 发布留言
     */
    ItemComment createComment(Long itemId, Long userId, String content, Long parentId);

    /**
     * 删除留言（本人或管理员）
     */
    boolean deleteComment(Long id, Long userId, boolean isAdmin);

    /**
     * 管理员分页查询所有留言
     */
    IPage<ItemComment> getAdminCommentPage(int current, int size, Long itemId, Integer status, String keyword);

    /**
     * 管理员切换留言状态（启用/禁用）
     */
    ItemComment updateCommentStatus(Long id, int status);
}
