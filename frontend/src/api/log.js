import request from '@/utils/request'

/**
 * 管理员分页查询操作日志
 */
export function getAdminLogs(params) {
  return request({
    url: '/admin/logs',
    method: 'get',
    params
  })
}

/**
 * 超级管理员清理历史操作日志
 * @param {Object} params
 * @param {number} params.daysBefore - 清理多少天前的日志，默认 30
 */
export function cleanLogs(params) {
  return request({
    url: '/admin/logs/clean',
    method: 'delete',
    params
  })
}
