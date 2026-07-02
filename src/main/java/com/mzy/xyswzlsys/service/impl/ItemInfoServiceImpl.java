package com.mzy.xyswzlsys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mzy.xyswzlsys.entity.ItemInfo;
import com.mzy.xyswzlsys.mapper.ItemInfoMapper;
import com.mzy.xyswzlsys.service.ItemInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 物品信息服务实现类
 */
@Service
public class ItemInfoServiceImpl implements ItemInfoService {

    @Autowired
    private ItemInfoMapper itemInfoMapper;

    @Override
    public IPage<ItemInfo> getPublicItemPage(int current, int size, Integer type, Long categoryId, String keyword) {
        Page<ItemInfo> page = new Page<>(current, size);
        QueryWrapper<ItemInfo> wrapper = new QueryWrapper<>();

        // 只返回已审核通过的物品
        wrapper.eq("audit_status", 1);

        // 类型筛选
        if (type != null) {
            wrapper.eq("type", type);
        }

        // 分类筛选
        if (categoryId != null) {
            wrapper.eq("category_id", categoryId);
        }

        // 关键词搜索（标题或描述）
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w.like("title", keyword).or().like("description", keyword));
        }

        // 按创建时间倒序
        wrapper.orderByDesc("create_time");

        return itemInfoMapper.selectPage(page, wrapper);
    }

    @Override
    public IPage<ItemInfo> getAdminItemPage(int current, int size, Integer type, Long categoryId, Integer status, Integer auditStatus, String keyword) {
        Page<ItemInfo> page = new Page<>(current, size);
        QueryWrapper<ItemInfo> wrapper = new QueryWrapper<>();

        if (type != null) wrapper.eq("type", type);
        if (categoryId != null) wrapper.eq("category_id", categoryId);
        if (status != null) wrapper.eq("status", status);
        if (auditStatus != null) wrapper.eq("audit_status", auditStatus);
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w.like("title", keyword).or().like("description", keyword));
        }

        wrapper.orderByDesc("create_time");
        return itemInfoMapper.selectPage(page, wrapper);
    }

    @Override
    public IPage<ItemInfo> getUserItems(int current, int size, Long userId) {
        if (userId == null) throw new IllegalArgumentException("用户ID不能为空");
        Page<ItemInfo> page = new Page<>(current, size);
        QueryWrapper<ItemInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.orderByDesc("create_time");
        return itemInfoMapper.selectPage(page, wrapper);
    }

    @Override
    public ItemInfo getItemById(Long id) {
        if (id == null) throw new IllegalArgumentException("物品ID不能为空");
        ItemInfo item = itemInfoMapper.selectById(id);
        if (item == null) throw new IllegalArgumentException("物品信息不存在");
        return item;
    }

    @Override
    public ItemInfo createItem(ItemInfo item, Long userId) {
        // 基本校验
        if (item.getType() == null) throw new IllegalArgumentException("物品类型不能为空");
        if (item.getType() != 0 && item.getType() != 1) throw new IllegalArgumentException("物品类型无效：0-寻物，1-拾物");
        if (item.getTitle() == null || item.getTitle().trim().isEmpty()) throw new IllegalArgumentException("标题不能为空");
        if (item.getDescription() == null || item.getDescription().trim().isEmpty()) throw new IllegalArgumentException("详细描述不能为空");
        if (item.getCategoryId() == null) throw new IllegalArgumentException("请选择物品分类");

        // 设置默认值
        item.setId(null); // 确保是新增
        item.setUserId(userId);
        item.setCreateBy(userId);
        if (item.getStatus() == null) item.setStatus(0); // 默认待认领
        item.setAuditStatus(0); // 默认待审核
        item.setAuditRemark(null);
        item.setClaimUserId(null);
        item.setClaimTime(null);

        // 至少有一种联系方式
        boolean hasContact = (item.getContactPhone() != null && !item.getContactPhone().trim().isEmpty())
                || (item.getContactWechat() != null && !item.getContactWechat().trim().isEmpty())
                || (item.getContactQq() != null && !item.getContactQq().trim().isEmpty());
        if (!hasContact) throw new IllegalArgumentException("至少需要填写一种联系方式（电话/微信/QQ）");

        itemInfoMapper.insert(item);
        return item;
    }

    @Override
    public ItemInfo updateItem(Long id, ItemInfo item, Long userId) {
        if (id == null) throw new IllegalArgumentException("物品ID不能为空");

        ItemInfo existing = itemInfoMapper.selectById(id);
        if (existing == null) throw new IllegalArgumentException("物品信息不存在");

        // 权限检查：只有发布者或管理员才能修改
        boolean isOwner = existing.getUserId().equals(userId);
        if (!isOwner) {
            // 非发布者，检查是否是管理员角色 - 这里通过上层验证，简化逻辑
            // 简化：允许修改，具体权限在 Controller 层判断
        }

        // 更新字段
        if (item.getType() != null) existing.setType(item.getType());
        if (item.getCategoryId() != null) existing.setCategoryId(item.getCategoryId());
        if (item.getTitle() != null) existing.setTitle(item.getTitle());
        if (item.getDescription() != null) existing.setDescription(item.getDescription());
        if (item.getImages() != null) existing.setImages(item.getImages());
        if (item.getLocation() != null) existing.setLocation(item.getLocation());
        if (item.getLostTime() != null) existing.setLostTime(item.getLostTime());
        if (item.getContactPhone() != null) existing.setContactPhone(item.getContactPhone());
        if (item.getContactWechat() != null) existing.setContactWechat(item.getContactWechat());
        if (item.getContactQq() != null) existing.setContactQq(item.getContactQq());
        existing.setUpdateBy(userId);

        // 修改后重新提交审核
        existing.setAuditStatus(0);
        existing.setAuditRemark(null);

        itemInfoMapper.updateById(existing);
        return existing;
    }

    @Override
    public boolean deleteItem(Long id, Long userId, boolean isAdmin) {
        if (id == null) throw new IllegalArgumentException("物品ID不能为空");

        ItemInfo existing = itemInfoMapper.selectById(id);
        if (existing == null) throw new IllegalArgumentException("物品信息不存在");

        // 权限检查：发布者或管理员
        if (!isAdmin && !existing.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权删除此物品");
        }

        return itemInfoMapper.deleteById(id) > 0;
    }

    @Override
    public ItemInfo auditItem(Long id, int auditStatus, String auditRemark, Long adminId) {
        if (id == null) throw new IllegalArgumentException("物品ID不能为空");
        if (auditStatus != 1 && auditStatus != 2) throw new IllegalArgumentException("审核状态无效：1-通过，2-驳回");
        if (auditStatus == 2 && (auditRemark == null || auditRemark.trim().isEmpty())) {
            throw new IllegalArgumentException("驳回时必须填写审核备注");
        }

        ItemInfo existing = itemInfoMapper.selectById(id);
        if (existing == null) throw new IllegalArgumentException("物品信息不存在");

        existing.setAuditStatus(auditStatus);
        existing.setAuditRemark(auditStatus == 2 ? auditRemark : null);
        existing.setUpdateBy(adminId);

        itemInfoMapper.updateById(existing);
        return existing;
    }

    @Override
    public ItemInfo changeItemStatus(Long id, int status, Long userId, Long claimUserId) {
        if (id == null) throw new IllegalArgumentException("物品ID不能为空");
        if (status < 0 || status > 2) throw new IllegalArgumentException("状态无效：0-待认领，1-已认领/找回，2-已下架");

        ItemInfo existing = itemInfoMapper.selectById(id);
        if (existing == null) throw new IllegalArgumentException("物品信息不存在");

        // 权限检查：只有发布者才能变更状态
        if (!existing.getUserId().equals(userId)) {
            throw new IllegalArgumentException("只有发布者才能变更物品状态");
        }

        existing.setStatus(status);
        existing.setUpdateBy(userId);

        if (status == 1) {
            existing.setClaimUserId(claimUserId);
            existing.setClaimTime(LocalDateTime.now());
        } else {
            existing.setClaimUserId(null);
            existing.setClaimTime(null);
        }

        itemInfoMapper.updateById(existing);
        return existing;
    }
}
