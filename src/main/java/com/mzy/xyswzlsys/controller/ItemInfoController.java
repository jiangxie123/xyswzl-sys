package com.mzy.xyswzlsys.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mzy.xyswzlsys.common.Result;
import com.mzy.xyswzlsys.entity.ItemInfo;
import com.mzy.xyswzlsys.security.JwtTokenUtil;
import com.mzy.xyswzlsys.service.ItemInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 物品信息接口
 * GET  /api/items - 公开：分页查询列表（已审核通过的）
 * GET  /api/items/{id} - 公开：获取详情
 * POST /api/items - 需要登录：发布物品
 * PUT  /api/items/{id} - 需要登录（本人或管理员）：更新物品
 * DELETE /api/items/{id} - 需要登录（本人或管理员）：删除物品
 * POST /api/items/{id}/audit - 需要管理员：审核物品
 * POST /api/items/{id}/status - 需要登录（本人）：变更状态
 */
@RestController
@RequestMapping("/api/items")
public class ItemInfoController {

    @Autowired
    private ItemInfoService itemInfoService;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    /**
     * 从请求 Token 中解析用户 ID
     */
    private Long getCurrentUserId(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return jwtTokenUtil.getUserIdFromToken(authHeader.substring(7));
        }
        return null;
    }

    /**
     * 从请求 Token 中解析用户角色
     */
    private Integer getCurrentRole(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return jwtTokenUtil.getRoleFromToken(authHeader.substring(7));
        }
        return null;
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
        return Result.success(itemInfoService.getPublicItemPage(current, size, type, categoryId, keyword));
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
     */
    @GetMapping("/my")
    public Result<IPage<ItemInfo>> getMyItems(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader("Authorization") String authHeader) {
        Long userId = getCurrentUserId(authHeader);
        if (userId == null) return Result.error(401, "请先登录");
        return Result.success(itemInfoService.getUserItems(current, size, userId));
    }

    /**
     * 发布物品（需要登录）
     */
    @PostMapping
    public Result<ItemInfo> create(@RequestBody ItemInfo item,
                                    @RequestHeader("Authorization") String authHeader) {
        Long userId = getCurrentUserId(authHeader);
        if (userId == null) return Result.error(401, "请先登录");
        return Result.success(itemInfoService.createItem(item, userId));
    }

    /**
     * 更新物品信息（需要登录：本人或管理员）
     */
    @PutMapping("/{id}")
    public Result<ItemInfo> update(@PathVariable Long id,
                                    @RequestBody ItemInfo item,
                                    @RequestHeader("Authorization") String authHeader) {
        Long userId = getCurrentUserId(authHeader);
        if (userId == null) return Result.error(401, "请先登录");
        return Result.success(itemInfoService.updateItem(id, item, userId));
    }

    /**
     * 删除物品（需要登录：本人或管理员）
     */
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id,
                                  @RequestHeader("Authorization") String authHeader) {
        Long userId = getCurrentUserId(authHeader);
        Integer role = getCurrentRole(authHeader);
        if (userId == null) return Result.error(401, "请先登录");
        boolean deleted = itemInfoService.deleteItem(id, userId, isAdmin(role));
        return Result.success(deleted ? "删除成功" : "删除失败");
    }

    /**
     * 变更物品状态（需要登录：本人）
     */
    @PostMapping("/{id}/status")
    public Result<ItemInfo> changeStatus(@PathVariable Long id,
                                          @RequestBody Map<String, Object> body,
                                          @RequestHeader("Authorization") String authHeader) {
        Long userId = getCurrentUserId(authHeader);
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
        return Result.success(itemInfoService.getAdminItemPage(current, size, type, categoryId, status, auditStatus, keyword));
    }

    /**
     * 管理员审核物品
     */
    @PostMapping("/{id}/audit")
    public Result<ItemInfo> audit(@PathVariable Long id,
                                   @RequestBody Map<String, Object> body,
                                   @RequestHeader("Authorization") String authHeader) {
        Long adminId = getCurrentUserId(authHeader);
        Integer role = getCurrentRole(authHeader);
        if (adminId == null || !isAdmin(role)) return Result.error(403, "无权操作");

        Integer auditStatus = body.get("auditStatus") instanceof Number ? ((Number) body.get("auditStatus")).intValue() : null;
        String auditRemark = body.get("auditRemark") != null ? body.get("auditRemark").toString() : null;

        if (auditStatus == null) return Result.error(400, "审核状态不能为空");
        return Result.success(itemInfoService.auditItem(id, auditStatus, auditRemark, adminId));
    }
}
