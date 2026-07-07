import request from '@/utils/request'

// 分页查询答题结果列表
export function getPaperResultList(data) {
  return request({
    url: '/paper/result/list',
    method: 'post',
    data: data
  })
}

// 查询答题结果详情
export function getPaperResult(data) {
  return request({
    url: '/paper/result/detail',
    method: 'post',
    data: data
  })
}

// 导出答题结果
export function exportPaperResult(data) {
  return request({
    url: '/paper/result/export',
    method: 'post',
    data: data,
    responseType: 'blob'
  })
}



