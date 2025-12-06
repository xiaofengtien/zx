import request from '@/utils/request'

// 查询学员档案列表 - 使用POST方法
export function listArchive(query) {
  return request({
    url: '/student/archive/listArchive',
    method: 'post',
    data: query
  })
}

// 查询学员档案详细（单个）- 使用POST方法
export function getArchive(archiveId) {
  return request({
    url: '/student/archive/getArchive',
    method: 'post',
    data: { archiveId: archiveId }
  })
}

// 批量查询学员档案详细 - 使用POST方法
export function getArchiveBatch(ids) {
  return request({
    url: '/student/archive/getArchiveBatch',
    method: 'post',
    data: { ids: ids }
  })
}

// 新增学员档案 - 使用POST方法
export function addArchive(data) {
  return request({
    url: '/student/archive/addArchive',
    method: 'post',
    data: data
  })
}

// 修改学员档案 - 使用POST方法
export function updateArchive(data) {
  return request({
    url: '/student/archive/updateArchive',
    method: 'post',
    data: data
  })
}

// 删除学员档案（单个）- 使用POST方法
export function delArchive(archiveId) {
  return request({
    url: '/student/archive/delArchive',
    method: 'post',
    data: { archiveId: archiveId }
  })
}

// 批量删除学员档案 - 使用POST方法
export function delArchiveBatch(ids) {
  return request({
    url: '/student/archive/delArchiveBatch',
    method: 'post',
    data: { ids: ids }
  })
}

// 重置学员密码 - 使用POST方法
export function resetArchivePwd(archiveId, newPassword) {
  return request({
    url: '/student/archive/resetPwd',
    method: 'post',
    data: { 
      archiveId: archiveId,
      newPassword: newPassword
    }
  })
}

// 修改学员密码 - 使用POST方法
export function changeArchivePwd(archiveId, oldPassword, newPassword) {
  return request({
    url: '/student/archive/changePwd',
    method: 'post',
    data: { 
      archiveId: archiveId,
      oldPassword: oldPassword,
      newPassword: newPassword
    }
  })
}

// 忘记密码（通过手机号+验证码重置密码）- 使用POST方法
export function forgotPassword(data) {
  return request({
    url: '/student/archive/forgotPassword',
    method: 'post',
    data: data
  })
}

// 重置学员练习次数 - 使用POST方法
export function resetPractice(data) {
  return request({
    url: '/student/paper/reset',
    method: 'post',
    data: data
  })
}
