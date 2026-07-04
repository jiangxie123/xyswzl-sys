package com.mzy.xyswzlsys.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mzy.xyswzlsys.common.Result;
import com.mzy.xyswzlsys.entity.ItemCategory;
import com.mzy.xyswzlsys.security.CurrentUserDetails;
import com.mzy.xyswzlsys.security.JwtTokenUtil;
import com.mzy.xyswzlsys.service.AdminOperationLogService;
import com.mzy.xyswzlsys.service.ItemCategoryService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 物品分类接口（管理员权限）
 * GET /api/categories - 公众可访问（获取所有启用分类列表）
 * 其他接口需要管理员权限，已在每个方法开头做角色校验
 */
@RestController
@RequestMapping("/api/categories")
public class ItemCategoryController {

    @Autowired
    private ItemCategoryService categoryService;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private AdminOperationLogService logService;

    /**
     * 从 Spring Security 上下文解析当前登录用户信息
     * 若上下文不可用则退回到解析 JWT token
     */
    private CurrentUserDetails getCurrentUser(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getDetails() instanceof CurrentUserDetails) {
            return (CurrentUserDetails) authentication.getDetails();
        }
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            Long userId = jwtTokenUtil.getUserIdFromToken(token);
            String username = jwtTokenUtil.getUsernameFromToken(token);
            Integer role = jwtTokenUtil.getRoleFromToken(token);
            return new CurrentUserDetails(userId, username, role);
        }
        return null;
    }

    private boolean isAdmin(Integer role) {
        return role != null && (role == 1 || role == 2);
    }

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
            @RequestParam(required = false) String name,
            HttpServletRequest request) {
        CurrentUserDetails user = getCurrentUser(request);
        if (user == null || !isAdmin(user.getRole())) return Result.error(403, "无权操作");
        return Result.success(categoryService.getCategoryPage(current, size, name));
    }

    /**
     * 管理员获取单个分类详情
     */
    @GetMapping("/admin/{id}")
    public Result<ItemCategory> getById(@PathVariable Long id, HttpServletRequest request) {
        CurrentUserDetails user = getCurrentUser(request);
        if (user == null || !isAdmin(user.getRole())) return Result.error(403, "无权操作");
        return Result.success(categoryService.getCategoryById(id));
    }

    /**
     * 管理员新增分类
     */
    @PostMapping("/admin")
    public Result<ItemCategory> create(@RequestBody ItemCategory category, HttpServletRequest httpRequest) {
        CurrentUserDetails user = getCurrentUser(httpRequest);
        if (user == null || !isAdmin(user.getRole())) return Result.error(403, "无权操作");
        if (category.getName() == null || category.getName().trim().isEmpty()) {
            return Result.error(400, "分类名称不能为空");
        }
        if (category.getName().length() > 50) {
            return Result.error(400, "分类名称过长（最多50字符）");
        }
        ItemCategory result = categoryService.createCategory(category);
        logService.log(user.getUserId(), user.getUsername(), "CREATE", "CATEGORY", "新增分类：" + category.getName(), result.getId(), "CATEGORY", 1, null);
        return Result.success(result);
    }

    /**
     * 管理员更新分类
     */
    @PutMapping("/admin/{id}")
    public Result<ItemCategory> update(@PathVariable Long id, @RequestBody ItemCategory category, HttpServletRequest httpRequest) {
        CurrentUserDetails user = getCurrentUser(httpRequest);
        if (user == null || !isAdmin(user.getRole())) return Result.error(403, "无权操作");
        if (category.getName() != null && category.getName().length() > 50) {
            return Result.error(400, "分类名称过长（最多50字符）");
        }
        ItemCategory result = categoryService.updateCategory(id, category);
        logService.log(user.getUserId(), user.getUsername(), "UPDATE", "CATEGORY", "更新分类ID=" + id, id, "CATEGORY", 1, null);
        return Result.success(result);
    }

    /**
     * 管理员删除分类
     */
    @DeleteMapping("/admin/{id}")
    public Result<String> delete(@PathVariable Long id, HttpServletRequest httpRequest) {
        CurrentUserDetails user = getCurrentUser(httpRequest);
        if (user == null || !isAdmin(user.getRole())) return Result.error(403, "无权操作");
        categoryService.deleteCategory(id);
        logService.log(user.getUserId(), user.getUsername(), "DELETE", "CATEGORY", "删除分类ID=" + id, id, "CATEGORY", 1, null);
        return Result.success("删除成功");
    }
}
