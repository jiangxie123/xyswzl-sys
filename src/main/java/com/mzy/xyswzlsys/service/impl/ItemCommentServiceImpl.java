package com.mzy.xyswzlsys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mzy.xyswzlsys.entity.ItemComment;
import com.mzy.xyswzlsys.mapper.ItemCommentMapper;
import com.mzy.xyswzlsys.service.ItemCommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 物品留言 Service 实现
 */
@Service
public class ItemCommentServiceImpl implements ItemCommentService {

    @Autowired
    private ItemCommentMapper commentMapper;

    @Override
    public IPage<ItemComment> getCommentsByItemId(Long itemId, int current, int size) {
        if (itemId == null) throw new IllegalArgumentException("物品ID不能为空");
        Page<ItemComment> page = new Page<>(current, size);
        QueryWrapper<ItemComment> wrapper = new QueryWrapper<>();
        wrapper.eq("item_id", itemId);
        wrapper.eq("status", 1);  // 只显示正常状态的留言
        wrapper.orderByDesc("create_time");
        return commentMapper.selectPage(page, wrapper);
    }

    @Override
    public ItemComment getCommentById(Long id) {
        if (id == null) throw new IllegalArgumentException("留言ID不能为空");
        ItemComment comment = commentMapper.selectById(id);
        if (comment == null) throw new IllegalArgumentException("留言不存在");
        return comment;
    }

    @Override
    public ItemComment createComment(Long itemId, Long userId, String content, Long parentId) {
        if (itemId == null) throw new IllegalArgumentException("物品ID不能为空");
        if (userId == null) throw new IllegalArgumentException("用户ID不能为空");
        if (content == null || content.trim().isEmpty()) throw new IllegalArgumentException("留言内容不能为空");

        ItemComment comment = new ItemComment();
        comment.setItemId(itemId);
        comment.setUserId(userId);
        comment.setContent(content.trim());
        comment.setParentId(parentId != null ? parentId : 0L);
        comment.setStatus(1);  // 默认正常
        commentMapper.insert(comment);
        return comment;
    }

    @Override
    public boolean deleteComment(Long id, Long userId, boolean isAdmin) {
        if (id == null) throw new IllegalArgumentException("留言ID不能为空");
        ItemComment existing = commentMapper.selectById(id);
        if (existing == null) throw new IllegalArgumentException("留言不存在");

        // 权限检查：本人或管理员才能删除
        if (!isAdmin && !existing.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权删除此留言");
        }
        return commentMapper.deleteById(id) > 0;
    }

    @Override
    public IPage<ItemComment> getAdminCommentPage(int current, int size, Long itemId, Integer status, String keyword) {
        Page<ItemComment> page = new Page<>(current, size);
        QueryWrapper<ItemComment> wrapper = new QueryWrapper<>();
        if (itemId != null) wrapper.eq("item_id", itemId);
        if (status != null) wrapper.eq("status", status);
        if (keyword != null && !keyword.trim().isEmpty()) wrapper.like("content", keyword);
        wrapper.orderByDesc("create_time");
        return commentMapper.selectPage(page, wrapper);
    }

    @Override
    public ItemComment updateCommentStatus(Long id, int status) {
        if (id == null) throw new IllegalArgumentException("留言ID不能为空");
        if (status != 0 && status != 1) throw new IllegalArgumentException("状态无效：0-禁用，1-正常");
        ItemComment existing = commentMapper.selectById(id);
        if (existing == null) throw new IllegalArgumentException("留言不存在");
        existing.setStatus(status);
        commentMapper.updateById(existing);
        return existing;
    }
}
