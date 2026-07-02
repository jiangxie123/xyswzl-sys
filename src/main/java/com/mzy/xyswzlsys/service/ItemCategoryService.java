package com.mzy.xyswzlsys.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mzy.xyswzlsys.entity.ItemCategory;

import java.util.List;

/**
 * 物品分类服务接口
 * 提供分类的增删改查及状态管理
 */
public interface ItemCategoryService {

    /**
     * 获取所有启用状态的分类列表（按 sortOrder 升序）
     * 用于前端物品列表页的筛选条件
     */
    List<ItemCategory> getAllActiveCategories();

    /**
     * 分页获取所有分类（管理员使用）
     * @param current 当前页码
     * @param size 每页条数
     * @param name 搜索关键词（可选，按名称模糊搜索）
     */
    IPage<ItemCategory> getCategoryPage(int current, int size, String name);

    /**
     * 根据 ID 获取分类详情
     */
    ItemCategory getCategoryById(Long id);

    /**
     * 新增分类
     */
    ItemCategory createCategory(ItemCategory category);

    /**
     * 更新分类
     */
    ItemCategory updateCategory(Long id, ItemCategory category);

    /**
     * 删除分类
     */
    boolean deleteCategory(Long id);
}
