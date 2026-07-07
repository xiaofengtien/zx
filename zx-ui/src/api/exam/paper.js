import request from '@/utils/request'
import { getToken } from '@/utils/auth'

// 分页查询试卷列表
export function getPaperList(data) {
  return request({
    url: '/paper/list',
    method: 'post',
    data: data
  })
}

// 查询试卷详情
export function getPaper(data) {
  return request({
    url: '/paper/detail',
    method: 'post',
    data: data
  })
}

// 根据试卷编码获取试卷详情
export function getPaperByCode(data) {
  return request({
    url: '/paper/detailByCode',
    method: 'post',
    data: data
  })
}

// 新增试卷
export function addPaper(data) {
  return request({
    url: '/paper/add',
    method: 'post',
    data: data
  })
}

// 修改试卷
export function updatePaper(data) {
  return request({
    url: '/paper/edit',
    method: 'post',
    data: data
  })
}

// 删除试卷
export function deletePaper(data) {
  return request({
    url: '/paper/remove',
    method: 'post',
    data: data
  })
}

// 获取试卷题目列表
export function getPaperQuestionList(data) {
  return request({
    url: '/paper/question/list',
    method: 'post',
    data: data
  })
}

// 添加题目到试卷
export function addPaperQuestion(data) {
  return request({
    url: '/paper/question/add',
    method: 'post',
    data: data
  })
}

// 从试卷移除题目
export function removePaperQuestion(data) {
  return request({
    url: '/paper/question/remove',
    method: 'post',
    data: data
  })
}

// 批量保存试卷题目关联（用于大题题目关联）
export function batchSavePaperQuestion(data) {
  return request({
    url: '/paper/question/batchSave',
    method: 'post',
    data: data
  })
}

// 生成试卷包（异步）
export function generatePaperPackage(data) {
  return request({
    url: '/paper/generatePackage',
    method: 'post',
    data: data
  })
}

// 查询试卷包生成任务状态
export function getPackageTaskStatus(paperId) {
  return request({
    url: '/paper/getPackageTaskStatus',
    method: 'get',
    params: { paperId }
  })
}

// 获取所有任务列表（用于任务中心显示）
export function getAllPackageTasks() {
  return request({
    url: '/paper/getAllPackageTasks',
    method: 'get'
  })
}

// 取消试卷包生成任务
export function cancelPackageTask(data) {
  return request({
    url: '/paper/cancelPackageTask',
    method: 'post',
    data: data
  })
}

// 下载试卷包（兼容旧接口，保留）
export function downloadPaperPackage(data) {
  return request({
    url: '/paper/downloadPackage',
    method: 'post',
    data: data,
    responseType: 'blob'
  })
}

// 流式下载试卷包（支持大文件，分片下载，带进度回调）
export function downloadPaperPackageStream(paperId, onProgress) {
  return new Promise((resolve, reject) => {
    const token = getToken()
    const baseURL = process.env.VUE_APP_BASE_API

    // 首先获取文件大小（通过HEAD请求或第一次请求）
    const chunkSize = 10 * 1024 * 1024 // 10MB分片

    // 构建请求头
    const headers = {
      'Content-Type': 'application/json'
    }
    if (token) {
      headers['Authorization'] = 'Bearer ' + token
    }

    // 先发送一个HEAD请求获取文件大小（如果HEAD不支持，则使用GET请求的第一个字节范围）
    fetch(`${baseURL}/paper/downloadPackageStream?paperId=${paperId}`, {
      method: 'HEAD',
      headers: headers
    }).then(headResponse => {
      let fileSize = null

      // 尝试从Content-Length获取文件大小
      const contentLength = headResponse.headers.get('Content-Length')
      if (contentLength) {
        fileSize = parseInt(contentLength, 10)
      }

      // 如果HEAD请求返回200但没有Content-Length，尝试从Content-Range获取
      if (!fileSize && headResponse.status === 200) {
        const contentRange = headResponse.headers.get('Content-Range')
        if (contentRange) {
          // Content-Range格式: bytes 0-999/10000
          const match = contentRange.match(/\/(\d+)/)
          if (match) {
            fileSize = parseInt(match[1], 10)
          }
        }
      }

      if (!fileSize) {
        // 如果无法获取文件大小，回退到普通下载
        console.warn('无法获取文件大小，使用普通下载')
        return downloadPaperPackage({ id: paperId }).then(resolve).catch(reject)
      }

      // 分片下载
      const chunks = []
      let downloadedBytes = 0
      const totalChunks = Math.ceil(fileSize / chunkSize)

      // 下载所有分片
      const downloadChunks = []
      for (let i = 0; i < totalChunks; i++) {
        const start = i * chunkSize
        const end = Math.min(start + chunkSize - 1, fileSize - 1)

        downloadChunks.push(
          fetch(`${baseURL}/paper/downloadPackageStream?paperId=${paperId}`, {
            method: 'GET',
            headers: {
              ...headers,
              'Range': `bytes=${start}-${end}`
            }
          }).then(response => {
            if (!response.ok && response.status !== 206) {
              throw new Error(`下载分片失败: ${response.status} ${response.statusText}`)
            }
            return response.arrayBuffer()
          }).then(buffer => {
            chunks[i] = buffer
            downloadedBytes += buffer.byteLength

            // 进度回调
            if (onProgress) {
              onProgress(downloadedBytes, fileSize)
            }

            return buffer
          })
        )
      }

      // 等待所有分片下载完成
      Promise.all(downloadChunks).then(() => {
        // 合并所有分片
        const totalLength = chunks.reduce((sum, chunk) => sum + chunk.byteLength, 0)
        const mergedArray = new Uint8Array(totalLength)
        let offset = 0
        for (let i = 0; i < chunks.length; i++) {
          mergedArray.set(new Uint8Array(chunks[i]), offset)
          offset += chunks[i].byteLength
        }

        // 创建Blob
        const blob = new Blob([mergedArray], { type: 'application/zip' })
        resolve(blob)
      }).catch(reject)
    }).catch(error => {
      // HEAD请求失败，回退到普通下载
      console.warn('HEAD请求失败，使用普通下载:', error)
      downloadPaperPackage({ id: paperId }).then(resolve).catch(reject)
    })
  })
}

// 根据试卷ID列表查询试卷列表
export function getPaperListByIds(data) {
  return request({
    url: '/paper/listByIds',
    method: 'post',
    data: data
  })
}

// ========== 卷别管理 API ==========
// 查询卷别列表
export function getPaperVolumeList(data) {
  return request({
    url: '/paper/volume/list',
    method: 'post',
    data: data
  })
}

// 新增卷别
export function addPaperVolume(data) {
  return request({
    url: '/paper/volume/add',
    method: 'post',
    data: data
  })
}

// 修改卷别
export function updatePaperVolume(data) {
  return request({
    url: '/paper/volume/edit',
    method: 'post',
    data: data
  })
}

// 批量保存卷别
export function batchSavePaperVolume(data) {
  return request({
    url: '/paper/volume/batchSave',
    method: 'post',
    data: data
  })
}

// 删除卷别
export function deletePaperVolume(data) {
  return request({
    url: '/paper/volume/remove',
    method: 'post',
    data: data
  })
}

// ========== 大题管理 API ==========
// 查询大题列表
export function getPaperSectionList(data) {
  return request({
    url: '/paper/section/list',
    method: 'post',
    data: data
  })
}

// 新增大题
export function addPaperSection(data) {
  return request({
    url: '/paper/section/add',
    method: 'post',
    data: data
  })
}

// 修改大题
export function updatePaperSection(data) {
  return request({
    url: '/paper/section/edit',
    method: 'post',
    data: data
  })
}

// 批量保存大题
export function batchSavePaperSection(data) {
  return request({
    url: '/paper/section/batchSave',
    method: 'post',
    data: data
  })
}

// 删除大题
export function deletePaperSection(data) {
  return request({
    url: '/paper/section/remove',
    method: 'post',
    data: data
  })
}

// ========== 中场配置管理 API ==========
// 查询中场配置列表
export function getPaperIntermissionList(data) {
  return request({
    url: '/paper/intermission/list',
    method: 'post',
    data: data
  })
}

// 新增中场配置
export function addPaperIntermission(data) {
  return request({
    url: '/paper/intermission/add',
    method: 'post',
    data: data
  })
}

// 修改中场配置
export function updatePaperIntermission(data) {
  return request({
    url: '/paper/intermission/edit',
    method: 'post',
    data: data
  })
}

// 批量保存中场配置
export function batchSavePaperIntermission(data) {
  return request({
    url: '/paper/intermission/batchSave',
    method: 'post',
    data: data
  })
}

// 删除中场配置
export function deletePaperIntermission(data) {
  return request({
    url: '/paper/intermission/remove',
    method: 'post',
    data: data
  })
}

// ========== 题目媒体管理 API ==========
// 根据试卷ID查询媒体文件列表
export function getQuestionMediaByPaperId(data) {
  return request({
    url: '/question/media/listByPaperId',
    method: 'post',
    data: data
  })
}

// 根据卷别ID查询媒体文件列表
export function getQuestionMediaByVolumeId(data) {
  return request({
    url: '/question/media/listByVolumeId',
    method: 'post',
    data: data
  })
}

// 根据大题ID查询媒体文件列表
export function getQuestionMediaBySectionId(data) {
  return request({
    url: '/question/media/listBySectionId',
    method: 'post',
    data: data
  })
}

// 根据中场配置ID查询媒体文件列表
export function getQuestionMediaByIntermissionId(data) {
  return request({
    url: '/question/media/listByIntermissionId',
    method: 'post',
    data: data
  })
}

// ========== 智能导入 API ==========
// 检查解析服务状态
export function getParseServiceStatus() {
  return request({
    url: '/paper/import/status',
    method: 'get'
  })
}

// 解析 Word 文件（上传文件并获取预览数据）
export function parseImportFiles(wordFile, audioFile) {
  const formData = new FormData()
  formData.append('wordFile', wordFile)
  if (audioFile) {
    formData.append('audioFile', audioFile)
  }
  return request({
    url: '/paper/import/parse',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    timeout: 300000 // 5分钟超时（解析大文件+音频切片可能较慢）
  })
}

// 确认导入试卷
export function confirmImport(data) {
  return request({
    url: '/paper/import/confirm',
    method: 'post',
    data: data
  })
}

// ========== 异步解析 API ==========
// 提交解析任务（异步）
export function submitParseTask(wordFile, audioFile, audioOssUrl) {
  const formData = new FormData()
  formData.append('wordFile', wordFile)
  if (audioFile) {
    formData.append('audioFile', audioFile)
  }
  if (audioOssUrl) {
    formData.append('audioOssUrl', audioOssUrl)
  }
  return request({
    url: '/paper/import/submitTask',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    timeout: 60000 // 1分钟超时（仅上传文件）
  })
}

// 获取解析任务状态
export function getParseTaskStatus(taskId) {
  return request({
    url: `/paper/import/task/${taskId}`,
    method: 'get'
  })
}

// 取消解析任务
export function cancelParseTask(taskId) {
  return request({
    url: `/paper/import/task/${taskId}/cancel`,
    method: 'post'
  })
}

// 批量创建题目（将解析结果保存到题库）
export function createQuestionsFromParse(parseResult, categoryId, subjectId, defaultQuestionType, listeningOnly) {
  return request({
    url: '/paper/import/createQuestions',
    method: 'post',
    data: {
      parseResult,
      categoryId,
      subjectId,
      defaultQuestionType,
      listeningOnly
    }
  })
}

// ========== 多卷别顺序导入（会话管理）API ==========

// 开始导入会话
export function startImportSession() {
  return request({
    url: '/paper/import/session/start',
    method: 'post'
  })
}

// 添加卷别到导入会话
export function addVolumeToSession(data) {
  return request({
    url: '/paper/import/session/addVolume',
    method: 'post',
    data: data
  })
}

// 获取导入会话状态
export function getImportSession(sessionKey) {
  return request({
    url: `/paper/import/session/${sessionKey}`,
    method: 'get'
  })
}

// 完成导入会话（创建试卷和题目）
export function finalizeImportSession(data) {
  return request({
    url: '/paper/import/session/finalize',
    method: 'post',
    data: data
  })
}

// ========== 题目组 API ==========

// 根据大题ID查询题目组列表
export function listQuestionGroupsBySectionId(sectionId) {
  return request({
    url: `/paper/questionGroup/list/${sectionId}`,
    method: 'get'
  })
}

// 根据试卷ID查询所有题目组
export function listQuestionGroupsByPaperId(paperId) {
  return request({
    url: `/paper/questionGroup/listByPaper/${paperId}`,
    method: 'get'
  })
}

// 新增题目组
export function addQuestionGroup(data) {
  return request({
    url: '/paper/questionGroup/add',
    method: 'post',
    data: data
  })
}

// 修改题目组
export function updateQuestionGroup(data) {
  return request({
    url: '/paper/questionGroup/edit',
    method: 'post',
    data: data
  })
}

// 删除题目组
export function deleteQuestionGroup(id) {
  return request({
    url: `/paper/questionGroup/${id}`,
    method: 'delete'
  })
}

// 批量保存题目组
export function batchSaveQuestionGroups(data) {
  return request({
    url: '/paper/questionGroup/batchSave',
    method: 'post',
    data: data
  })
}

// 生成 TTS
export function generateTTS(data) {
  return request({
    url: '/paper/tool/tts',
    method: 'post',
    data: data
  })
}
