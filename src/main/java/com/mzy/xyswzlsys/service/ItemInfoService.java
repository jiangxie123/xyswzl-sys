package com.mzy.xyswzlsys.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mzy.xyswzlsys.entity.ItemInfo;

/**
 * 物品信息服务接口
 */
public interface ItemInfoService {

    /**
     * 分页查询物品列表（公开查询，只返回已审核通过的）
     * @param current 当前页
     * @param size 每页条数
     * @param type 类型：0-寻物，1-拾物，null-全部
     * @param categoryId 分类ID，null-全部
     * @param keyword 搜索关键词（搜索标题和描述），null-不搜索
     * @return 分页结果
     */
    IPage<ItemInfo> getPublicItemPage(int current, int size, Integer type, Long categoryId, String keyword);

    /**
     * 分页查询所有物品（管理员用，含待审核和已驳回）
     */
    IPage<ItemInfo> getAdminItemPage(int current, int size, Integer type, Long categoryId, Integer status, Integer auditStatus, String keyword);

    /**
     * 查询某用户发布的物品
     */
    IPage<ItemInfo> getUserItems(int current, int size, Long userId);

    /**
     * 获取物品详情
     */
    ItemInfo getItemById(Long id);

    /**
     * 发布物品
     */
    ItemInfo createItem(ItemInfo item, Long userId);

    /**
     * 更新物品信息
     */
    ItemInfo updateItem(Long id, ItemInfo item, Long userId);

    /**
     * 删除物品
     */
    boolean deleteItem(Long id, Long userId, boolean isAdmin);

    /**
     * 审核物品
     * @param id 物品ID
     * @param auditStatus 审核状态：1-通过，2-驳回
     * @param auditRemark 审核备注（驳回时必填）
     * @param adminId 审核人ID
     */
    ItemInfo auditItem(Long id, int auditStatus, String auditRemark, Long adminId);

    /**
     * 变更物品状态（认领/找回/下架）
     * @param id 物品ID
     * @param status 新状态：0-待认领，1-已认领/找回，2-已下架
     * @param userId 当前用户ID
     * @param claimUserId 认领人ID（status=1 时需要）
     */
    ItemInfo changeItemStatus(Long id, int status, Long userId, Long claimUserId);
}
