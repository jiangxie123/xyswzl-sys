package com.mzy.xyswzlsys.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mzy.xyswzlsys.common.Result;
import com.mzy.xyswzlsys.dto.request.ItemCommentRequest;
import com.mzy.xyswzlsys.entity.ItemComment;
import com.mzy.xyswzlsys.service.ItemCommentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 物品留言接口
 * GET  /api/comments/item/{itemId} - 公开：分页查询物品留言
 * POST /api/comments - 需要登录：发布留言
 * DELETE /api/comments/{id} - 需要登录：删除本人留言
 * GET  /api/comments/admin/page - 管理员：查询所有留言
 * POST /api/comments/{id}/status - 管理员：切换留言状态
 */
@RestController
@RequestMapping("/api/comments")
public class ItemCommentController {

    @Autowired
    private ItemCommentService commentService;

    /**
     * 从 Token 解析出的 userId，简化为：通过请求头 Authorization 解析
     * （在实际场景中，可通过 Spring Security 上下文直接获取）
     */
    @Autowired
    private com.mzy.xyswzlsys.security.JwtTokenUtil jwtTokenUtil;

    private Long parseUserIdFromToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return jwtTokenUtil.getUserIdFromToken(authHeader.substring(7));
        }
        return null;
    }

    private Integer parseRoleFromToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return jwtTokenUtil.getRoleFromToken(authHeader.substring(7));
        }
        return null;
    }

    private boolean isAdmin(String authHeader) {
        Integer role = parseRoleFromToken(authHeader);
        return role != null && (role == 1 || role == 2);
    }

    /**
     * 分页查询某物品的留言（公开，只显示正常状态的）
     */
    @GetMapping("/item/{itemId}")
    public Result<IPage<ItemComment>> getCommentsByItem(
            @PathVariable Long itemId,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(commentService.getCommentsByItemId(itemId, current, size));
    }

    /**
     * 发布留言（需要登录）
     */
    @PostMapping
    public Result<ItemComment> create(@Valid @RequestBody ItemCommentRequest request,
                                       @RequestHeader("Authorization") String authHeader) {
        Long userId = parseUserIdFromToken(authHeader);
        if (userId == null) return Result.error(401, "请先登录");
        return Result.success(commentService.createComment(request.getItemId(), userId,
                request.getContent(), request.getParentId()));
    }

    /**
     * 删除留言（需要登录：本人或管理员）
     */
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id,
                                  @RequestHeader("Authorization") String authHeader) {
        Long userId = parseUserIdFromToken(authHeader);
        boolean admin = isAdmin(authHeader);
        commentService.deleteComment(id, userId, admin);
        return Result.success("删除成功");
    }

    /**
     * 管理员分页查询所有留言
     */
    @GetMapping("/admin/page")
    public Result<IPage<ItemComment>> getAdminPage(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long itemId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        return Result.success(commentService.getAdminCommentPage(current, size, itemId, status, keyword));
    }

    /**
     * 管理员切换留言状态（0-禁用，1-正常）
     * 注：只有管理员才能调用（前端页面在管理员面板），此处补充角色检查
     */
    @PostMapping("/{id}/status")
    public Result<ItemComment> updateStatus(@PathVariable Long id,
                                             @RequestBody Map<String, Integer> body,
                                             @RequestHeader("Authorization") String authHeader) {
        if (!isAdmin(authHeader)) return Result.error(403, "无权操作");
        Integer status = body.get("status");
        if (status == null) throw new IllegalArgumentException("状态不能为空");
        return Result.success(commentService.updateCommentStatus(id, status));
    }
}
