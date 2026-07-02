import request from '@/utils/request'

/**
 * 分页查询物品列表（公开接口，只返回已审核通过的）
 */
export function getItemList(params) {
  return request({
    url: '/items',
    method: 'get',
    params
  })
}

/**
 * 获取物品详情（公开接口）
 */
export function getItemDetail(id) {
  return request({
    url: `/items/${id}`,
    method: 'get'
  })
}

/**
 * 获取当前用户发布的物品（需要登录）
 */
export function getMyItems(current = 1, size = 10) {
  return request({
    url: '/items/my',
    method: 'get',
    params: { current, size }
  })
}

/**
 * 发布物品（需要登录）
 */
export function createItem(data) {
  return request({
    url: '/items',
    method: 'post',
    data
  })
}

/**
 * 更新物品（需要登录）
 */
export function updateItem(id, data) {
  return request({
    url: `/items/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除物品（需要登录）
 */
export function deleteItem(id) {
  return request({
    url: `/items/${id}`,
    method: 'delete'
  })
}

/**
 * 变更物品状态（需要登录，本人操作）
 */
export function changeItemStatus(id, status, claimUserId) {
  return request({
    url: `/items/${id}/status`,
    method: 'post',
    data: { status, claimUserId }
  })
}

/**
 * 管理员分页查询所有物品
 */
export function getAdminItemPage(params) {
  return request({
    url: '/items/admin/page',
    method: 'get',
    params
  })
}

/**
 * 管理员审核物品
 */
export function auditItem(id, auditStatus, auditRemark) {
  return request({
    url: `/items/${id}/audit`,
    method: 'post',
    data: { auditStatus, auditRemark }
  })
}
