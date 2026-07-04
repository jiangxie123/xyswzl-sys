package com.mzy.xyswzlsys.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mzy.xyswzlsys.common.Result;
import com.mzy.xyswzlsys.dto.request.ItemInfoRequest;
import com.mzy.xyswzlsys.entity.ItemInfo;
import com.mzy.xyswzlsys.security.CurrentUserDetails;
import com.mzy.xyswzlsys.security.JwtTokenUtil;
import com.mzy.xyswzlsys.service.AdminOperationLogService;
import com.mzy.xyswzlsys.service.ItemInfoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 物品信息接口
 *
 * 统一的身份获取方式：当前登录用户的 userId / username / role 来自 JwtAuthenticationFilter 注入到
 * Spring Security Context 的 CurrentUserDetails。这样发布、更新、状态变更、"我的发布"
 * 查询使用的 userId 都是**同一个来源**，避免 Token 中值与 userInfo.id 不一致时出现"我发布的物品看不到"。
 */
@RestController
@RequestMapping("/api/items")
public class ItemInfoController {

    @Autowired
    private ItemInfoService itemInfoService;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private AdminOperationLogService logService;

    /**
     * 从 Spring Security 上下文解析当前用户信息（JwtAuthenticationFilter 已注入 CurrentUserDetails）。
     * 同时提供回退路径：如果 SecurityContext 中没有 CurrentUserDetails，
     * 则直接从请求的 Authorization header 中使用 JwtTokenUtil 解析，
     * 确保"我的发布"页面能正确读到发布者的 id。
     */
    private CurrentUserDetails getCurrentUser() {
        // 方式 1：从 Spring Security 上下文读取（优先级更高）
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getDetails() instanceof CurrentUserDetails) {
            return (CurrentUserDetails) authentication.getDetails();
        }
        // 方式 2：从当前请求里读 Authorization header
        HttpServletRequest request = getCurrentRequest();
        if (request == null) return null;
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                Long userId = jwtTokenUtil.getUserIdFromToken(token);
                String username = jwtTokenUtil.getUsernameFromToken(token);
                Integer role = jwtTokenUtil.getRoleFromToken(token);
                return new CurrentUserDetails(userId, username, role);
            } catch (Exception ignored) {
                // 解析失败就返回 null，上层会判定为未登录
            }
        }
        return null;
    }

    private Long getCurrentUserId() {
        CurrentUserDetails user = getCurrentUser();
        return user == null ? null : user.getUserId();
    }

    private Integer getCurrentRole() {
        CurrentUserDetails user = getCurrentUser();
        return user == null ? 0 : user.getRole();
    }

    private String getCurrentUsername() {
        CurrentUserDetails user = getCurrentUser();
        return user == null ? null : user.getUsername();
    }

    /**
     * 通过 RequestContextHolder 取当前请求（避免每个 Controller 方法都传 HttpServletRequest）。
     */
    private HttpServletRequest getCurrentRequest() {
        try {
            Object attr = org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes();
            if (attr instanceof org.springframework.web.context.request.ServletRequestAttributes) {
                return ((org.springframework.web.context.request.ServletRequestAttributes) attr).getRequest();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }


    private boolean isAdmin(Integer role) {
        return role != null && (role == 1 || role == 2);
    }

    // ==================== 公开接口 ====================

    /**
     * 分页查询物品列表（公开，只返回已审核通过的）
     */
    @GetMapping
    public Result<IPage<ItemInfo>> getPublicList(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword) {
        // 简单分页保护：current 最小 1，size 最大 100
        if (current < 1) current = 1;
        if (size < 1 || size > 100) size = 10;

        IPage<ItemInfo> page = itemInfoService.getPublicItemPage(current, size, type, categoryId, keyword);
        return Result.success(page);
    }

    /**
     * 获取物品详情（公开）
     */
    @GetMapping("/{id}")
    public Result<ItemInfo> getDetail(@PathVariable Long id) {
        return Result.success(itemInfoService.getItemById(id));
    }

    // ==================== 登录用户接口 ====================

    /**
     * 获取当前用户发布的物品列表（需要登录）
     * 与发布时用的 userId 解析逻辑一致，确保"我的发布"能显示自己发布的物品。
     */
    @GetMapping("/my")
    public Result<IPage<ItemInfo>> getMyItems(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size) {
        if (current < 1) current = 1;
        if (size < 1 || size > 100) size = 10;

        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        IPage<ItemInfo> page = itemInfoService.getUserItems(current, size, userId);
        return Result.success(page);
    }

    /**
     * 发布物品（需要登录，使用受约束的 DTO）
     */
    @PostMapping
    public Result<ItemInfo> create(@Valid @RequestBody ItemInfoRequest request) {
        Long userId = getCurrentUserId();
        if (userId == null) return Result.error(401, "请先登录");
        return Result.success(itemInfoService.createItem(request, userId));
    }

    /**
     * 更新物品信息（需要登录：本人）
     */
    @PutMapping("/{id}")
    public Result<ItemInfo> update(@PathVariable Long id,
                                    @Valid @RequestBody ItemInfoRequest request) {
        Long userId = getCurrentUserId();
        if (userId == null) return Result.error(401, "请先登录");
        return Result.success(itemInfoService.updateItem(id, request, userId));
    }

    /**
     * 删除物品（需要登录：本人或管理员）
     */
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        Integer role = getCurrentRole();
        if (userId == null) return Result.error(401, "请先登录");
        boolean deleted = itemInfoService.deleteItem(id, userId, isAdmin(role));
        return Result.success(deleted ? "删除成功" : "删除失败");
    }

    /**
     * 变更物品状态（需要登录：本人）
     */
    @PostMapping("/{id}/status")
    public Result<ItemInfo> changeStatus(@PathVariable Long id,
                                          @RequestBody Map<String, Object> body) {
        Long userId = getCurrentUserId();
        if (userId == null) return Result.error(401, "请先登录");

        Integer status = body.get("status") instanceof Number ? ((Number) body.get("status")).intValue() : null;
        Long claimUserId = body.get("claimUserId") instanceof Number ? ((Number) body.get("claimUserId")).longValue() : null;

        if (status == null) return Result.error(400, "状态不能为空");
        return Result.success(itemInfoService.changeItemStatus(id, status, userId, claimUserId));
    }

    // ==================== 管理员接口 ====================

    /**
     * 管理员分页查询所有物品（含待审核、已驳回）
     */
    @GetMapping("/admin/page")
    public Result<IPage<ItemInfo>> getAdminList(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer auditStatus,
            @RequestParam(required = false) String keyword) {
        if (!isAdmin(getCurrentRole())) return Result.error(403, "无权操作");
        if (current < 1) current = 1;
        if (size < 1 || size > 100) size = 10;
        return Result.success(itemInfoService.getAdminItemPage(current, size, type, categoryId, status, auditStatus, keyword));
    }

    /**
     * 管理员审核物品
     */
    @PostMapping("/{id}/audit")
    public Result<ItemInfo> audit(@PathVariable Long id,
                                   @RequestBody Map<String, Object> body,
                                   @RequestHeader("Authorization") String authHeader) {
        Long adminId = getCurrentUserId();
        Integer role = getCurrentRole();
        if (!isAdmin(role)) return Result.error(403, "无权操作");

        Integer auditStatus = body.get("auditStatus") instanceof Number ? ((Number) body.get("auditStatus")).intValue() : null;
        String auditRemark = body.get("auditRemark") != null ? body.get("auditRemark").toString() : null;

        if (auditStatus == null) return Result.error(400, "审核状态不能为空");
        ItemInfo item = itemInfoService.auditItem(id, auditStatus, auditRemark, adminId);
        String operationDesc = (auditStatus == 1 ? "审核通过" : "审核驳回") + "物品ID=" + id;

        String operatorName = getCurrentUsername();
        try {
            // Fallback：如果 Token 头不为空也可以用它解析 username
            if (operatorName == null && authHeader != null && authHeader.startsWith("Bearer ")) {
                operatorName = jwtTokenUtil.getUsernameFromToken(authHeader.substring(7));
            }
        } catch (Exception ignored) {}
        logService.log(adminId, operatorName, "AUDIT", "ITEM", operationDesc, id, "ITEM", 1, null);
        return Result.success(item);
    }
}
