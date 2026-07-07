import request from '@/utils/request'

// 查询分类树
export function getCategoryTree(data) {
  return request({
    url: '/question/category/tree',
    method: 'post',
    data: data
  })
}

// 查询分类列表
export function getCategoryList(data) {
  return request({
    url: '/question/category/list',
    method: 'post',
    data: data
  })
}

// 查询分类详情
export function getCategory(data) {
  return request({
    url: '/question/category/detail',
    method: 'post',
    data: data
  })
}

// 新增分类
export function addCategory(data) {
  return request({
    url: '/question/category/add',
    method: 'post',
    data: data
  })
}

// 修改分类
export function updateCategory(data) {
  return request({
    url: '/question/category/edit',
    method: 'post',
    data: data
  })
}

// 删除分类
export function deleteCategory(data) {
  return request({
    url: '/question/category/remove',
    method: 'post',
    data: data
  })
}

// 更新分类排序
export function updateCategorySort(data) {
  return request({
    url: '/question/category/sort',
    method: 'post',
    data: data
  })
}

// 检查分类名称是否存在
export function checkCategoryName(data) {
  return request({
    url: '/question/category/checkName',
    method: 'post',
    data: data
  })
}



