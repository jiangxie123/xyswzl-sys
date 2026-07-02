package com.mzy.xyswzlsys.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mzy.xyswzlsys.common.Result;
import com.mzy.xyswzlsys.entity.ItemCategory;
import com.mzy.xyswzlsys.service.ItemCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 物品分类接口（管理员权限）
 * GET /api/categories - 公众可访问（获取所有启用分类列表）
 * 其他接口需要管理员权限
 */
@RestController
@RequestMapping("/api/categories")
public class ItemCategoryController {

    @Autowired
    private ItemCategoryService categoryService;

    /**
     * 获取所有启用状态的分类列表（公众接口，用于物品筛选）
     */
    @GetMapping
    public Result<List<ItemCategory>> getAllCategories() {
        return Result.success(categoryService.getAllActiveCategories());
    }

    /**
     * 管理员分页查询分类
     */
    @GetMapping("/admin/page")
    public Result<IPage<ItemCategory>> getPage(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String name) {
        return Result.success(categoryService.getCategoryPage(current, size, name));
    }

    /**
     * 管理员获取单个分类详情
     */
    @GetMapping("/admin/{id}")
    public Result<ItemCategory> getById(@PathVariable Long id) {
        return Result.success(categoryService.getCategoryById(id));
    }

    /**
     * 管理员新增分类
     */
    @PostMapping("/admin")
    public Result<ItemCategory> create(@RequestBody ItemCategory category) {
        return Result.success(categoryService.createCategory(category));
    }

    /**
     * 管理员更新分类
     */
    @PutMapping("/admin/{id}")
    public Result<ItemCategory> update(@PathVariable Long id, @RequestBody ItemCategory category) {
        return Result.success(categoryService.updateCategory(id, category));
    }

    /**
     * 管理员删除分类
     */
    @DeleteMapping("/admin/{id}")
    public Result<String> delete(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return Result.success("删除成功");
    }
}
