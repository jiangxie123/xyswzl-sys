import request from '@/utils/request'

/**
 * 分页查询某物品的留言
 */
export function getItemComments(itemId, current = 1, size = 10) {
  return request({
    url: `/comments/item/${itemId}`,
    method: 'get',
    params: { current, size }
  })
}

/**
 * 发布留言
 */
export function createComment(itemId, content, parentId) {
  return request({
    url: '/comments',
    method: 'post',
    data: { itemId, content, parentId }
  })
}

/**
 * 删除留言
 */
export function deleteComment(id) {
  return request({
    url: `/comments/${id}`,
    method: 'delete'
  })
}

/**
 * 管理员分页查询所有留言
 */
export function getAdminCommentPage(params) {
  return request({
    url: '/comments/admin/page',
    method: 'get',
    params
  })
}

/**
 * 管理员切换留言状态
 */
export function updateCommentStatus(id, status) {
  return request({
    url: `/comments/${id}/status`,
    method: 'post',
    data: { status }
  })
}
