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
