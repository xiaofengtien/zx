const axios = require('axios')
const { app } = require('electron')
const path = require('path')

// 后端API地址
const API_BASE_URL = process.env.API_BASE_URL || 'http://localhost:8080'

/**
 * 答题服务
 * 负责答题数据的存储、查询和同步
 */
class AnswerService {
  constructor(db) {
    this.db = db.getDB()
  }

  /**
   * 开始答题（创建答题记录）
   * @param {Object} params - 参数对象
   * @param {number} params.paperId - 试卷ID
   * @param {string} params.paperName - 试卷名称
   * @param {number} params.appUserId - 用户ID
   * @param {Array<string>} params.volumeCodes - 卷别代码列表（如 ["A", "B"]）
   * @param {string} params.assignedSeatNumber - 分配的机位号（可选）
   * @returns {Promise<Object>} 答题记录信息
   */
  async startExam({ paperId, paperName, appUserId, volumeCodes = [], assignedSeatNumber = null }) {
    try {
      const now = Date.now()
      
      // 检查是否已有未提交的答题记录
      const existing = this.db.prepare(`
        SELECT id, start_time, is_submit, volume_status
        FROM app_user_paper_info
        WHERE paper_id = ? AND app_user_id = ? AND is_submit = 0
        ORDER BY start_time DESC
        LIMIT 1
      `).get(paperId, appUserId)

      if (existing) {
        console.log('发现未提交的答题记录，继续答题')
        return {
          success: true,
          paperInfoId: existing.id,
          startTime: existing.start_time,
          volumeStatus: existing.volume_status ? JSON.parse(existing.volume_status) : {},
          message: '继续未完成的答题'
        }
      }

      // 初始化卷别状态（如果提供了卷别代码）
      let volumeStatus = {}
      if (volumeCodes && volumeCodes.length > 0) {
        volumeCodes.forEach((code, index) => {
          volumeStatus[code] = index === 0 ? 'in_progress' : 'pending'
        })
      }

      // 创建新的答题记录
      // 注意：字段数量和参数数量必须匹配
      const result = this.db.prepare(`
        INSERT INTO app_user_paper_info (
          paper_id, paper_name, app_user_id, start_time, 
          practice_count, last_practice_time,
          volume_status, volume_submit_time, intermission_played,
          assigned_seat_number, actual_seat_number,
          is_submit, sync_status, create_time, update_time
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      `).run(
        paperId, 
        paperName, 
        appUserId, 
        now,
        0, // practice_count
        null, // last_practice_time
        JSON.stringify(volumeStatus), // volume_status
        JSON.stringify({}), // volume_submit_time
        JSON.stringify({}), // intermission_played
        assignedSeatNumber, // assigned_seat_number
        null, // actual_seat_number
        0, // is_submit
        0, // sync_status
        now, // create_time
        now // update_time
      )

      console.log(`✓ 创建答题记录成功，paperInfoId: ${result.lastInsertRowid}`)

      return {
        success: true,
        paperInfoId: result.lastInsertRowid,
        startTime: now,
        volumeStatus,
        message: '开始答题'
      }
    } catch (error) {
      console.error('开始答题失败:', error)
      throw error
    }
  }

  /**
   * 保存单个题目的答题结果
   * @param {Object} params - 参数对象
   * @param {number} params.paperInfoId - 答题记录ID
   * @param {number} params.paperId - 试卷ID
   * @param {number} params.appUserId - 用户ID
   * @param {number} params.questionId - 题目ID
   * @param {string} params.answerIds - 答案ID（多个用逗号分隔）
   * @param {string} params.userAnswer - 用户答案文本
   * @param {number} params.result - 结果（0-错误，1-正确）
   * @param {number} params.questionSort - 题目序号
   * @param {number} params.timeSpent - 用时（秒）
   * @returns {Promise<Object>} 保存结果
   */
  async saveQuestionResult({
    paperInfoId,
    paperId,
    appUserId,
    questionId,
    answerIds,
    userAnswer,
    result,
    questionSort,
    timeSpent
  }) {
    try {
      const now = Date.now()

      // 检查是否已存在该题目的答题结果
      const existing = this.db.prepare(`
        SELECT id FROM app_user_paper_question_result
        WHERE paper_id = ? AND app_user_id = ? AND question_id = ?
      `).get(paperId, appUserId, questionId)

      if (existing) {
        // 更新现有记录
        this.db.prepare(`
          UPDATE app_user_paper_question_result
          SET answer_ids = ?, user_answer = ?, result = ?, 
              question_sort = ?, answer_time = ?, time_spent = ?,
              sync_status = 0
          WHERE id = ?
        `).run(answerIds, userAnswer, result, questionSort, now, timeSpent, existing.id)
      } else {
        // 插入新记录
        this.db.prepare(`
          INSERT INTO app_user_paper_question_result (
            paper_id, app_user_id, question_id, answer_ids, user_answer,
            result, question_sort, answer_time, time_spent, sync_status, create_time
          ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?)
        `).run(
          paperId, appUserId, questionId, answerIds, userAnswer,
          result, questionSort, now, timeSpent, now
        )
      }

      return { success: true }
    } catch (error) {
      console.error('保存题目答题结果失败:', error)
      throw error
    }
  }

  /**
   * 保存完形填空答题结果
   * @param {Object} params - 参数对象
   * @param {number} params.paperInfoId - 答题记录ID
   * @param {number} params.paperId - 试卷ID
   * @param {number} params.appUserId - 用户ID
   * @param {number} params.questionId - 题目ID
   * @param {number} params.blankAreaId - 完形填空区域ID
   * @param {number} params.blankIndex - 填空序号
   * @param {string} params.answerIds - 答案ID（多个用逗号分隔）
   * @param {number} params.result - 结果（0-错误，1-正确）
   * @param {number} params.timeSpent - 用时（秒）
   * @returns {Promise<Object>} 保存结果
   */
  async saveBlankResult({
    paperInfoId,
    paperId,
    appUserId,
    questionId,
    blankAreaId,
    blankIndex,
    answerIds,
    result,
    timeSpent
  }) {
    try {
      const now = Date.now()

      // 检查是否已存在该完形填空的答题结果
      const existing = this.db.prepare(`
        SELECT id FROM app_user_paper_question_blank_result
        WHERE paper_id = ? AND app_user_id = ? AND question_id = ? 
          AND blank_area_id = ? AND blank_index = ?
      `).get(paperId, appUserId, questionId, blankAreaId, blankIndex)

      if (existing) {
        // 更新现有记录
        this.db.prepare(`
          UPDATE app_user_paper_question_blank_result
          SET answer_ids = ?, result = ?, answer_time = ?, time_spent = ?,
              sync_status = 0
          WHERE id = ?
        `).run(answerIds, result, now, timeSpent, existing.id)
      } else {
        // 插入新记录
        this.db.prepare(`
          INSERT INTO app_user_paper_question_blank_result (
            paper_id, app_user_id, question_id, blank_area_id, blank_index,
            answer_ids, result, answer_time, time_spent, sync_status, create_time
          ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?)
        `).run(
          paperId, appUserId, questionId, blankAreaId, blankIndex,
          answerIds, result, now, timeSpent, now
        )
      }

      return { success: true }
    } catch (error) {
      console.error('保存完形填空答题结果失败:', error)
      throw error
    }
  }

  /**
   * 检查练习次数限制
   * @param {number} paperId - 试卷ID
   * @param {number} appUserId - 用户ID
   * @returns {Promise<Object>} 检查结果 { allowed: boolean, practiceCount: number, practiceLimit: number }
   */
  async checkPracticeLimit(paperId, appUserId) {
    try {
      // 获取试卷的练习次数限制
      const paper = this.db.prepare(`
        SELECT practice_limit FROM paper WHERE id = ?
      `).get(paperId)

      if (!paper) {
        throw new Error('试卷不存在')
      }

      const practiceLimit = paper.practice_limit || 0

      // 如果限制为0，表示不限制
      if (practiceLimit === 0) {
        return {
          allowed: true,
          practiceCount: 0,
          practiceLimit: 0
        }
      }

      // 统计已提交的练习次数
      const result = this.db.prepare(`
        SELECT COUNT(*) as count FROM app_user_paper_info
        WHERE paper_id = ? AND app_user_id = ? AND is_submit = 1
      `).get(paperId, appUserId)

      const practiceCount = result.count || 0
      const allowed = practiceCount < practiceLimit

      return {
        allowed,
        practiceCount,
        practiceLimit
      }
    } catch (error) {
      console.error('检查练习次数限制失败:', error)
      throw error
    }
  }

  /**
   * 更新卷别状态
   * @param {number} paperInfoId - 答题记录ID
   * @param {string} volumeCode - 卷别代码（如 "A", "B"）
   * @param {string} status - 状态（"pending", "in_progress", "completed"）
   * @returns {Promise<Object>} 更新结果
   */
  async updateVolumeStatus(paperInfoId, volumeCode, status) {
    try {
      const paperInfo = this.db.prepare(`
        SELECT volume_status FROM app_user_paper_info WHERE id = ?
      `).get(paperInfoId)

      if (!paperInfo) {
        throw new Error('答题记录不存在')
      }

      const volumeStatus = paperInfo.volume_status ? JSON.parse(paperInfo.volume_status) : {}
      volumeStatus[volumeCode] = status

      this.db.prepare(`
        UPDATE app_user_paper_info
        SET volume_status = ?, update_time = ?
        WHERE id = ?
      `).run(JSON.stringify(volumeStatus), Date.now(), paperInfoId)

      return {
        success: true,
        volumeStatus
      }
    } catch (error) {
      console.error('更新卷别状态失败:', error)
      throw error
    }
  }

  /**
   * 按卷提交答题结果
   * @param {Object} params - 参数对象
   * @param {number} params.paperInfoId - 答题记录ID
   * @param {string} params.volumeCode - 卷别代码（如 "A", "B"）
   * @returns {Promise<Object>} 提交结果
   */
  async submitVolume({ paperInfoId, volumeCode }) {
    try {
      const now = Date.now()

      const paperInfo = this.db.prepare(`
        SELECT volume_status, volume_submit_time FROM app_user_paper_info WHERE id = ?
      `).get(paperInfoId)

      if (!paperInfo) {
        throw new Error('答题记录不存在')
      }

      // 更新卷别状态为已完成
      const volumeStatus = paperInfo.volume_status ? JSON.parse(paperInfo.volume_status) : {}
      volumeStatus[volumeCode] = 'completed'

      // 更新卷别提交时间
      const volumeSubmitTime = paperInfo.volume_submit_time ? JSON.parse(paperInfo.volume_submit_time) : {}
      volumeSubmitTime[volumeCode] = new Date(now).toISOString()

      this.db.prepare(`
        UPDATE app_user_paper_info
        SET volume_status = ?, volume_submit_time = ?, update_time = ?
        WHERE id = ?
      `).run(JSON.stringify(volumeStatus), JSON.stringify(volumeSubmitTime), now, paperInfoId)

      console.log(`✓ 卷别 ${volumeCode} 提交成功，paperInfoId: ${paperInfoId}`)

      return {
        success: true,
        volumeCode,
        volumeStatus,
        submitTime: volumeSubmitTime[volumeCode]
      }
    } catch (error) {
      console.error('按卷提交答题结果失败:', error)
      throw error
    }
  }

  /**
   * 标记中场音频已播放
   * @param {number} paperInfoId - 答题记录ID
   * @param {string} fromVolume - 来源卷别（如 "A"）
   * @param {string} toVolume - 目标卷别（如 "B"）
   * @returns {Promise<Object>} 更新结果
   */
  async markIntermissionPlayed(paperInfoId, fromVolume, toVolume) {
    try {
      const paperInfo = this.db.prepare(`
        SELECT intermission_played FROM app_user_paper_info WHERE id = ?
      `).get(paperInfoId)

      if (!paperInfo) {
        throw new Error('答题记录不存在')
      }

      const intermissionPlayed = paperInfo.intermission_played ? JSON.parse(paperInfo.intermission_played) : {}
      const key = `${fromVolume}->${toVolume}`
      intermissionPlayed[key] = true

      this.db.prepare(`
        UPDATE app_user_paper_info
        SET intermission_played = ?, update_time = ?
        WHERE id = ?
      `).run(JSON.stringify(intermissionPlayed), Date.now(), paperInfoId)

      return {
        success: true,
        intermissionPlayed
      }
    } catch (error) {
      console.error('标记中场音频已播放失败:', error)
      throw error
    }
  }

  /**
   * 更新机位号
   * @param {number} paperInfoId - 答题记录ID
   * @param {string} actualSeatNumber - 实际坐的机位号
   * @returns {Promise<Object>} 更新结果
   */
  async updateSeatNumber(paperInfoId, actualSeatNumber) {
    try {
      this.db.prepare(`
        UPDATE app_user_paper_info
        SET actual_seat_number = ?, update_time = ?
        WHERE id = ?
      `).run(actualSeatNumber, Date.now(), paperInfoId)

      return {
        success: true,
        actualSeatNumber
      }
    } catch (error) {
      console.error('更新机位号失败:', error)
      throw error
    }
  }

  /**
   * 提交答题结果（更新答题记录状态，支持练习次数统计）
   * @param {Object} params - 参数对象
   * @param {number} params.paperInfoId - 答题记录ID
   * @param {number} params.paperId - 试卷ID
   * @param {number} params.appUserId - 用户ID
   * @param {number} params.totalScore - 总分
   * @param {number} params.userScore - 得分
   * @param {number} params.correctCount - 正确数
   * @param {number} params.wrongCount - 错误数
   * @returns {Promise<Object>} 提交结果
   */
  async submitExam({
    paperInfoId,
    paperId,
    appUserId,
    totalScore,
    userScore,
    correctCount,
    wrongCount
  }) {
    try {
      const now = Date.now()

      // 计算用时（从开始时间到现在）
      const paperInfo = this.db.prepare(`
        SELECT start_time, practice_count FROM app_user_paper_info WHERE id = ?
      `).get(paperInfoId)

      if (!paperInfo) {
        throw new Error('答题记录不存在')
      }

      const usedTime = Math.floor((now - paperInfo.start_time) / 1000) // 转换为秒

      // 更新答题记录（包括练习次数）
      this.db.prepare(`
        UPDATE app_user_paper_info
        SET submit_time = ?, used_time = ?, total_score = ?, user_score = ?,
            correct_count = ?, wrong_count = ?, is_submit = 1,
            practice_count = practice_count + 1, last_practice_time = ?,
            sync_status = 0, update_time = ?
        WHERE id = ?
      `).run(now, usedTime, totalScore, userScore, correctCount, wrongCount, now, now, paperInfoId)

      console.log(`✓ 提交答题结果成功，paperInfoId: ${paperInfoId}`)

      return {
        success: true,
        paperInfoId,
        usedTime,
        practiceCount: (paperInfo.practice_count || 0) + 1,
        message: '答题结果已保存'
      }
    } catch (error) {
      console.error('提交答题结果失败:', error)
      throw error
    }
  }

  /**
   * 获取答题记录详情
   * @param {number} paperInfoId - 答题记录ID
   * @returns {Promise<Object>} 答题记录详情
   */
  async getPaperInfo(paperInfoId) {
    try {
      const paperInfo = this.db.prepare(`
        SELECT * FROM app_user_paper_info WHERE id = ?
      `).get(paperInfoId)

      if (!paperInfo) {
        return null
      }

      // 获取题目答题结果
      const questionResults = this.db.prepare(`
        SELECT * FROM app_user_paper_question_result
        WHERE paper_id = ? AND app_user_id = ?
        ORDER BY question_sort ASC
      `).all(paperInfo.paper_id, paperInfo.app_user_id)

      // 获取完形填空答题结果
      const blankResults = this.db.prepare(`
        SELECT * FROM app_user_paper_question_blank_result
        WHERE paper_id = ? AND app_user_id = ?
        ORDER BY question_id ASC, blank_index ASC
      `).all(paperInfo.paper_id, paperInfo.app_user_id)

      return {
        ...paperInfo,
        questionResults,
        blankResults
      }
    } catch (error) {
      console.error('获取答题记录详情失败:', error)
      throw error
    }
  }

  /**
   * 获取用户的答题记录列表
   * @param {number} appUserId - 用户ID
   * @param {number} paperId - 试卷ID（可选）
   * @returns {Promise<Array>} 答题记录列表
   */
  async getPaperInfoList(appUserId, paperId = null) {
    try {
      let query = `
        SELECT * FROM app_user_paper_info
        WHERE app_user_id = ?
      `
      const params = [appUserId]

      if (paperId) {
        query += ' AND paper_id = ?'
        params.push(paperId)
      }

      query += ' ORDER BY start_time DESC'

      const results = this.db.prepare(query).all(...params)
      return results
    } catch (error) {
      console.error('获取答题记录列表失败:', error)
      throw error
    }
  }

  /**
   * 计算答题统计信息
   * @param {number} paperInfoId - 答题记录ID
   * @returns {Promise<Object>} 统计信息
   */
  async calculateStatistics(paperInfoId) {
    try {
      const paperInfo = this.db.prepare(`
        SELECT paper_id, app_user_id FROM app_user_paper_info WHERE id = ?
      `).get(paperInfoId)

      if (!paperInfo) {
        throw new Error('答题记录不存在')
      }

      // 统计题目答题结果
      const questionStats = this.db.prepare(`
        SELECT 
          COUNT(*) as total,
          SUM(CASE WHEN result = 1 THEN 1 ELSE 0 END) as correct,
          SUM(CASE WHEN result = 0 THEN 1 ELSE 0 END) as wrong
        FROM app_user_paper_question_result
        WHERE paper_id = ? AND app_user_id = ?
      `).get(paperInfo.paper_id, paperInfo.app_user_id)

      // 统计完形填空答题结果
      const blankStats = this.db.prepare(`
        SELECT 
          COUNT(*) as total,
          SUM(CASE WHEN result = 1 THEN 1 ELSE 0 END) as correct,
          SUM(CASE WHEN result = 0 THEN 1 ELSE 0 END) as wrong
        FROM app_user_paper_question_blank_result
        WHERE paper_id = ? AND app_user_id = ?
      `).get(paperInfo.paper_id, paperInfo.app_user_id)

      return {
        questionTotal: questionStats.total || 0,
        questionCorrect: questionStats.correct || 0,
        questionWrong: questionStats.wrong || 0,
        blankTotal: blankStats.total || 0,
        blankCorrect: blankStats.correct || 0,
        blankWrong: blankStats.wrong || 0,
        total: (questionStats.total || 0) + (blankStats.total || 0),
        correct: (questionStats.correct || 0) + (blankStats.correct || 0),
        wrong: (questionStats.wrong || 0) + (blankStats.wrong || 0)
      }
    } catch (error) {
      console.error('计算答题统计信息失败:', error)
      throw error
    }
  }

  /**
   * 同步答题结果到服务端
   * @param {number} paperInfoId - 答题记录ID
   * @param {string} token - 用户token
   * @returns {Promise<Object>} 同步结果
   */
  async syncToServer(paperInfoId, token) {
    try {
      // 获取答题记录详情
      const paperInfo = await this.getPaperInfo(paperInfoId)

      if (!paperInfo) {
        throw new Error('答题记录不存在')
      }

      if (paperInfo.sync_status === 1) {
        console.log('答题记录已同步，跳过')
        return { success: true, message: '已同步', skipped: true }
      }

      // 构建提交数据
      const submitData = {
        businessType: 5, // 考试业务类型
        businessId: paperInfo.paper_id,
        appUserId: paperInfo.app_user_id,
        questionResults: paperInfo.questionResults.map(r => ({
          questionId: r.question_id,
          answerIds: r.answer_ids || '',
          userAnswer: r.user_answer || '',
          result: r.result
        })),
        blankResults: paperInfo.blankResults.map(r => ({
          questionId: r.question_id,
          blankAreaId: r.blank_area_id,
          blankIndex: r.blank_index,
          answerIds: r.answer_ids || '',
          result: r.result
        }))
      }

      // 调用服务端接口
      const headers = {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      }

      console.log('提交答题结果到服务端:', {
        paperInfoId,
        paperId: paperInfo.paper_id,
        questionCount: submitData.questionResults.length,
        blankCount: submitData.blankResults.length
      })

      const response = await axios.post(
        `${API_BASE_URL}/student/paper/submit`,
        submitData,
        { headers }
      )

      if (response.data && response.data.code === 200) {
        // 更新同步状态
        const now = Date.now()
        this.db.prepare(`
          UPDATE app_user_paper_info
          SET sync_status = 1, sync_time = ?, update_time = ?
          WHERE id = ?
        `).run(now, now, paperInfoId)

        // 更新题目答题结果的同步状态
        this.db.prepare(`
          UPDATE app_user_paper_question_result
          SET sync_status = 1
          WHERE paper_id = ? AND app_user_id = ?
        `).run(paperInfo.paper_id, paperInfo.app_user_id)

        // 更新完形填空答题结果的同步状态
        this.db.prepare(`
          UPDATE app_user_paper_question_blank_result
          SET sync_status = 1
          WHERE paper_id = ? AND app_user_id = ?
        `).run(paperInfo.paper_id, paperInfo.app_user_id)

        console.log(`✓ 答题结果同步成功，paperInfoId: ${paperInfoId}`)

        return {
          success: true,
          message: '同步成功',
          data: response.data.data
        }
      } else {
        throw new Error(response.data?.msg || '同步失败')
      }
    } catch (error) {
      console.error('同步答题结果到服务端失败:', error)
      
      // 如果是网络错误，返回失败但不抛出异常（允许离线缓存）
      if (error.code === 'ECONNREFUSED' || error.code === 'ETIMEDOUT' || !error.response) {
        return {
          success: false,
          message: '网络错误，答题结果已保存到本地，将在网络恢复后自动同步',
          offline: true
        }
      }

      throw error
    }
  }

  /**
   * 同步所有未同步的答题结果
   * @param {string} token - 用户token
   * @returns {Promise<Object>} 同步结果
   */
  async syncAllUnsynced(token) {
    try {
      // 获取所有未同步的答题记录
      const unsynced = this.db.prepare(`
        SELECT id FROM app_user_paper_info
        WHERE sync_status = 0 AND is_submit = 1
        ORDER BY submit_time ASC
      `).all()

      console.log(`发现 ${unsynced.length} 条未同步的答题记录`)

      const results = []
      for (const record of unsynced) {
        try {
          const result = await this.syncToServer(record.id, token)
          results.push({ paperInfoId: record.id, ...result })
        } catch (error) {
          console.error(`同步答题记录失败，paperInfoId: ${record.id}`, error)
          results.push({
            paperInfoId: record.id,
            success: false,
            message: error.message
          })
        }
      }

      const successCount = results.filter(r => r.success).length
      const failCount = results.length - successCount

      return {
        success: true,
        total: unsynced.length,
        successCount,
        failCount,
        results
      }
    } catch (error) {
      console.error('同步所有未同步的答题结果失败:', error)
      throw error
    }
  }

  /**
   * 清空答题记录（用于重新考试）
   * @param {number} paperInfoId - 答题记录ID
   * @param {number} paperId - 试卷ID
   * @returns {Promise<Object>} 清空结果
   */
  async clearPaperResult(paperInfoId, paperId) {
    try {
      // 获取答题记录信息
      const paperInfo = this.db.prepare(`
        SELECT app_user_id FROM app_user_paper_info WHERE id = ?
      `).get(paperInfoId)

      if (!paperInfo) {
        throw new Error('答题记录不存在')
      }

      const appUserId = paperInfo.app_user_id

      // 删除题目答题结果
      this.db.prepare(`
        DELETE FROM app_user_paper_question_result
        WHERE paper_id = ? AND app_user_id = ?
      `).run(paperId, appUserId)

      // 删除完形填空答题结果
      this.db.prepare(`
        DELETE FROM app_user_paper_question_blank_result
        WHERE paper_id = ? AND app_user_id = ?
      `).run(paperId, appUserId)

      // 删除答题记录（注意：这里删除的是本次答题记录，不是所有记录）
      this.db.prepare(`
        DELETE FROM app_user_paper_info
        WHERE id = ?
      `).run(paperInfoId)

      console.log(`✓ 已清空答题记录，paperInfoId: ${paperInfoId}, paperId: ${paperId}`)

      return {
        success: true,
        message: '答题记录已清空'
      }
    } catch (error) {
      console.error('清空答题记录失败:', error)
      throw error
    }
  }
}

module.exports = AnswerService

