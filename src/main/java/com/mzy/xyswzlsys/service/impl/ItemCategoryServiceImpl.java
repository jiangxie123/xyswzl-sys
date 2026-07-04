package com.mzy.xyswzlsys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mzy.xyswzlsys.entity.ItemCategory;
import com.mzy.xyswzlsys.mapper.ItemCategoryMapper;
import com.mzy.xyswzlsys.service.ItemCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 物品分类服务实现类
 */
@Service
public class ItemCategoryServiceImpl implements ItemCategoryService {

    @Autowired
    private ItemCategoryMapper categoryMapper;

    @Override
    public List<ItemCategory> getAllActiveCategories() {
        QueryWrapper<ItemCategory> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1).orderByAsc("sort_order", "id");
        return categoryMapper.selectList(wrapper);
    }

    @Override
    public IPage<ItemCategory> getCategoryPage(int current, int size, String name) {
        Page<ItemCategory> page = new Page<>(current, size);
        QueryWrapper<ItemCategory> wrapper = new QueryWrapper<>();
        if (name != null && !name.isEmpty()) {
            wrapper.like("category_name", name);
        }
        wrapper.orderByAsc("sort_order", "id");
        return categoryMapper.selectPage(page, wrapper);
    }

    @Override
    public ItemCategory getCategoryById(Long id) {
        if (id == null) throw new IllegalArgumentException("分类 ID 不能为空");
        ItemCategory category = categoryMapper.selectById(id);
        if (category == null) throw new IllegalArgumentException("分类不存在");
        return category;
    }

    @Override
    public ItemCategory createCategory(ItemCategory category) {
        if (category.getName() == null || category.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("分类名称不能为空");
        }
        if (category.getSortOrder() == null) category.setSortOrder(0);
        if (category.getStatus() == null) category.setStatus(1);
        if (category.getCategoryCode() == null || category.getCategoryCode().trim().isEmpty()) {
            category.setCategoryCode("CAT_" + System.currentTimeMillis());
        }
        categoryMapper.insert(category);
        return category;
    }

    @Override
    public ItemCategory updateCategory(Long id, ItemCategory category) {
        if (id == null) throw new IllegalArgumentException("分类 ID 不能为空");
        ItemCategory existing = categoryMapper.selectById(id);
        if (existing == null) throw new IllegalArgumentException("分类不存在");
        if (category.getName() != null) existing.setName(category.getName());
        if (category.getCategoryCode() != null) existing.setCategoryCode(category.getCategoryCode());
        if (category.getIcon() != null) existing.setIcon(category.getIcon());
        if (category.getSortOrder() != null) existing.setSortOrder(category.getSortOrder());
        if (category.getStatus() != null) existing.setStatus(category.getStatus());
        categoryMapper.updateById(existing);
        return existing;
    }

    @Override
    public boolean deleteCategory(Long id) {
        if (id == null) throw new IllegalArgumentException("分类 ID 不能为空");
        ItemCategory existing = categoryMapper.selectById(id);
        if (existing == null) throw new IllegalArgumentException("分类不存在");
        return categoryMapper.deleteById(id) > 0;
    }
}
