package com.mzy.xyswzlsys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mzy.xyswzlsys.dto.request.ItemInfoRequest;
import com.mzy.xyswzlsys.entity.ItemInfo;
import com.mzy.xyswzlsys.mapper.ItemInfoMapper;
import com.mzy.xyswzlsys.service.ItemInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
    public ItemInfo createItem(ItemInfoRequest request, Long userId) {
        // 基本校验
        if (request.getType() == null) throw new IllegalArgumentException("物品类型不能为空");
        if (request.getType() != 0 && request.getType() != 1) throw new IllegalArgumentException("物品类型无效：0-寻物，1-拾物");
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) throw new IllegalArgumentException("标题不能为空");
        if (request.getDescription() == null || request.getDescription().trim().isEmpty()) throw new IllegalArgumentException("详细描述不能为空");
        if (request.getCategoryId() == null) throw new IllegalArgumentException("请选择物品分类");

        // 组装 entity（关键：默认值由后端设置，不允许前端直接设置）
        ItemInfo item = new ItemInfo();
        item.setType(request.getType());
        item.setCategoryId(request.getCategoryId());
        item.setTitle(request.getTitle().trim());
        item.setDescription(request.getDescription().trim());
        item.setImages(trimToNull(request.getImages(), 2000));
        item.setLocation(trimToNull(request.getLocation(), 100));
        item.setContactPhone(trimToNull(request.getContactPhone(), 20));
        item.setContactWechat(trimToNull(request.getContactWechat(), 30));
        item.setContactQq(trimToNull(request.getContactQq(), 20));

        // 解析丢失/捡到时间（可选）
        if (request.getLostTime() != null && !request.getLostTime().trim().isEmpty()) {
            try {
                item.setLostTime(LocalDateTime.parse(request.getLostTime().trim(),
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            } catch (Exception e) {
                // 前端可能只传 yyyy-MM-dd
                try {
                    item.setLostTime(java.time.LocalDate.parse(request.getLostTime().trim(),
                            DateTimeFormatter.ofPattern("yyyy-MM-dd")).atStartOfDay());
                } catch (Exception ex) {
                    throw new IllegalArgumentException("时间格式不正确，请使用 yyyy-MM-dd HH:mm:ss");
                }
            }
        }

        item.setUserId(userId);
        item.setCreateBy(userId);
        item.setStatus(0);        // 默认：待认领 / 待找回（固定）
        item.setAuditStatus(0);   // 默认：待审核（固定，防止前端绕过）
        item.setAuditRemark(null);
        item.setClaimUserId(null);
        item.setClaimTime(null);

        // 至少有一种联系方式
        boolean hasContact = (item.getContactPhone() != null && !item.getContactPhone().isEmpty())
                || (item.getContactWechat() != null && !item.getContactWechat().isEmpty())
                || (item.getContactQq() != null && !item.getContactQq().isEmpty());
        if (!hasContact) throw new IllegalArgumentException("至少需要填写一种联系方式（电话/微信/QQ）");

        itemInfoMapper.insert(item);
        return item;
    }

    @Override
    public ItemInfo updateItem(Long id, ItemInfoRequest request, Long userId) {
        if (id == null) throw new IllegalArgumentException("物品ID不能为空");

        ItemInfo existing = itemInfoMapper.selectById(id);
        if (existing == null) throw new IllegalArgumentException("物品信息不存在");

        // 权限检查：只有发布者本人才能修改（管理员从审核接口走，不从此处修改）
        boolean isOwner = existing.getUserId().equals(userId);
        if (!isOwner) {
            throw new IllegalArgumentException("无权修改此物品");
        }

        // 只允许修改白名单字段；auditStatus / status / userId 一律不可由用户直接变更
        if (request.getType() != null) existing.setType(request.getType());
        if (request.getCategoryId() != null) existing.setCategoryId(request.getCategoryId());
        if (request.getTitle() != null) existing.setTitle(request.getTitle().trim());
        if (request.getDescription() != null) existing.setDescription(request.getDescription().trim());
        if (request.getImages() != null) existing.setImages(trimToNull(request.getImages(), 2000));
        if (request.getLocation() != null) existing.setLocation(trimToNull(request.getLocation(), 100));
        if (request.getContactPhone() != null) existing.setContactPhone(trimToNull(request.getContactPhone(), 20));
        if (request.getContactWechat() != null) existing.setContactWechat(trimToNull(request.getContactWechat(), 30));
        if (request.getContactQq() != null) existing.setContactQq(trimToNull(request.getContactQq(), 20));

        // 丢失时间（可选）
        if (request.getLostTime() != null && !request.getLostTime().trim().isEmpty()) {
            try {
                existing.setLostTime(LocalDateTime.parse(request.getLostTime().trim(),
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            } catch (Exception e) {
                try {
                    existing.setLostTime(java.time.LocalDate.parse(request.getLostTime().trim(),
                            DateTimeFormatter.ofPattern("yyyy-MM-dd")).atStartOfDay());
                } catch (Exception ex) {
                    throw new IllegalArgumentException("时间格式不正确，请使用 yyyy-MM-dd HH:mm:ss");
                }
            }
        }

        existing.setUpdateBy(userId);

        // 修改后重新提交审核
        existing.setAuditStatus(0);
        existing.setAuditRemark(null);

        itemInfoMapper.updateById(existing);
        return existing;
    }

    /**
     * 截断并将空字符串转为 null，用于避免空串写入数据库
     */
    private String trimToNull(String value, int maxLength) {
        if (value == null) return null;
        String trimmed = value.trim();
        if (trimmed.isEmpty()) return null;
        if (trimmed.length() > maxLength) trimmed = trimmed.substring(0, maxLength);
        return trimmed;
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

        // claimUserId 校验：标记为已认领/找回时必须提供有效认领人 ID
        if (status == 1) {
            if (claimUserId == null || claimUserId <= 0) {
                throw new IllegalArgumentException("标记为已认领时必须提供认领人ID");
            }
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
