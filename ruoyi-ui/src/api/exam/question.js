import request from '@/utils/request'

// 分页查询题目列表
export function getQuestionList(data) {
  return request({
    url: '/question/list',
    method: 'post',
    data: data
  })
}

// 查询题目详情
export function getQuestion(data) {
  return request({
    url: '/question/detail',
    method: 'post',
    data: data
  })
}

// 根据分类ID获取题目列表
export function getQuestionListByCategory(data) {
  return request({
    url: '/question/by/category/list',
    method: 'post',
    data: data
  })
}

// 新增题目
export function addQuestion(data) {
  return request({
    url: '/question/add',
    method: 'post',
    data: data
  })
}

// 修改题目
export function updateQuestion(data) {
  return request({
    url: '/question/edit',
    method: 'post',
    data: data
  })
}

// 删除题目
export function deleteQuestion(data) {
  return request({
    url: '/question/remove',
    method: 'post',
    data: data
  })
}

// 获取题目正确答案
export function getQuestionAnswer(data) {
  return request({
    url: '/question/answer',
    method: 'post',
    data: data
  })
}

// 获取完形填空题内容
export function getQuestionBlankContent(data) {
  return request({
    url: '/question/blank-content',
    method: 'post',
    data: data
  })
}

// 批量复制题目
export function copyQuestion(data) {
  return request({
    url: '/question/copy',
    method: 'post',
    data: data
  })
}

// 批量移动题目
export function moveQuestion(data) {
  return request({
    url: '/question/move',
    method: 'post',
    data: data
  })
}

// 上传题目媒体文件（旧方式，通过后端中转）
export function uploadQuestionMedia(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/question/media/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 获取OSS上传凭证（用于前端直接上传）
export function getOssUploadToken() {
  return request({
    url: '/question/media/getUploadToken',
    method: 'get'
  })
}

// 获取媒体文件的临时下载地址（OSS签名）
export function getMediaDownloadUrl(params) {
  return request({
    url: '/question/media/getDownloadUrl',
    method: 'get',
    params
  })
}

// 删除题目媒体文件
export function deleteQuestionMedia(data) {
  return request({
    url: '/question/media/delete',
    method: 'post',
    data: data
  })
}

// 保存题目媒体文件记录
export function saveQuestionMedia(data) {
  return request({
    url: '/question/media/save',
    method: 'post',
    data: data
  })
}

// 删除题目媒体文件记录（根据ID）
export function removeQuestionMedia(data) {
  return request({
    url: '/question/media/remove',
    method: 'post',
    data: data
  })
}

