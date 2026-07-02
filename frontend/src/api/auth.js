import request from '@/utils/request'

// 登录接口
export function login(username, password) {
  return request({
    url: '/auth/login',
    method: 'post',
    data: { username, password }
  })
}

// 注册接口
export function register(data) {
  return request({
    url: '/auth/register',
    method: 'post',
    data
  })
}

// 用户列表
export function getUserList(current, size) {
  return request({
    url: '/users',
    method: 'get',
    params: { current, size }
  })
}

// 获取单个用户
export function getUser(id) {
  return request({
    url: `/users/${id}`,
    method: 'get'
  })
}

// 新增用户
export function addUser(userData) {
  return request({
    url: '/users',
    method: 'post',
    data: userData
  })
}

// 修改用户（管理员用，带角色校验）
export function updateUser(id, userData) {
  return request({
    url: `/users/${id}`,
    method: 'put',
    data: userData
  })
}

// 修改当前登录用户的个人信息（不含角色和状态，所有已登录用户可用）
export function updateProfile(userData) {
  return request({
    url: '/users/me',
    method: 'put',
    data: userData
  })
}

// 删除用户
export function deleteUser(id) {
  return request({
    url: `/users/${id}`,
    method: 'delete'
  })
}
