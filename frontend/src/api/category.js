import request from '@/utils/request'

/**
 * 获取所有启用状态的分类列表（公众接口）
 */
export function getAllCategories() {
  return request({
    url: '/categories',
    method: 'get'
  })
}

/**
 * 管理员分页查询分类
 */
export function getCategoryPage(current, size, name) {
  return request({
    url: '/categories/admin/page',
    method: 'get',
    params: { current, size, name }
  })
}

/**
 * 管理员获取单个分类详情
 */
export function getCategoryById(id) {
  return request({
    url: `/categories/admin/${id}`,
    method: 'get'
  })
}

/**
 * 管理员新增分类
 */
export function createCategory(data) {
  return request({
    url: '/categories/admin',
    method: 'post',
    data
  })
}

/**
 * 管理员更新分类
 */
export function updateCategory(id, data) {
  return request({
    url: `/categories/admin/${id}`,
    method: 'put',
    data
  })
}

/**
 * 管理员删除分类
 */
export function deleteCategory(id) {
  return request({
    url: `/categories/admin/${id}`,
    method: 'delete'
  })
}
