import request from '@/utils/request'

// 学员在线登录（需要验证码）
export function studentOnlineLogin(username, password, code, uuid) {
  const data = {
    username,
    password,
    code,
    uuid
  }
  return request({
    url: '/student/onlineLogin',
    headers: {
      isToken: false,
      repeatSubmit: false
    },
    method: 'post',
    data: data
  })
}

// 学员离线登录（不需要验证码）
export function studentOfflineLogin(username, password) {
  const data = {
    username,
    password
  }
  return request({
    url: '/student/offlineLogin',
    headers: {
      isToken: false,
      repeatSubmit: false
    },
    method: 'post',
    data: data
  })
}

// 获取学员详细信息
export function getStudentInfo() {
  return request({
    url: '/student/getInfo',
    method: 'get'
  })
}

// 获取学员路由信息
export function getStudentRouters() {
  return request({
    url: '/student/getRouters',
    method: 'get'
  })
}




