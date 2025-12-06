<template>
  <div class="exam-result-container">
    <!-- 骨架屏：数据加载中显示 -->
    <transition name="fade">
      <exam-result-skeleton v-if="loading && !dataReady" />
    </transition>
    
    <!-- 实际内容：数据加载完成后显示 -->
    <transition name="fade-up">
    <div v-if="dataReady" class="result-content">
      <!-- 统计信息卡片 -->
      <el-card class="statistics-card">
        <div slot="header" class="card-header">
          <span class="card-title">答题统计</span>
        </div>
        <div class="statistics-content">
          <div class="stat-item">
            <div class="stat-label">总分</div>
            <div class="stat-value">{{ resultData.totalScore}}</div>
          </div>
          <div class="stat-item">
            <div class="stat-label">得分</div>
            <div class="stat-value score-value" :class="{ 'score-pass': resultData.userScore >= (resultData.totalScore * 0.6) }">
              {{ resultData.userScore || 0 }}
            </div>
          </div>
          <div class="stat-item">
            <div class="stat-label">正确题数</div>
            <div class="stat-value correct-value">{{ resultData.correctCount || 0 }}</div>
          </div>
          <div class="stat-item">
            <div class="stat-label">错误题数</div>
            <div class="stat-value wrong-value">{{ resultData.wrongCount || 0 }}</div>
          </div>
          <div class="stat-item">
            <div class="stat-label">用时</div>
            <div class="stat-value time-value">{{ formatTime(resultData.usedTime || 0) }}</div>
          </div>
        </div>
      </el-card>

      <!-- 答题详情（按卷别和大题分组） -->
      <el-card class="detail-card">
        <div slot="header" class="card-header">
          <span class="card-title">答题详情</span>
          <span class="total-questions">共 {{ totalQuestionCount }} 题</span>
        </div>
        
        <div v-loading="loading" class="detail-content">
          <!-- 按卷别分组 -->
          <div v-for="volume in groupedResults" :key="volume.volumeCode" class="volume-group">
            <div class="volume-header">
              <span class="volume-name">{{ volume.volumeName || `卷别 ${volume.volumeCode}` }}</span>
              <span class="volume-stats">
                正确 <span class="correct-num">{{ volume.correctCount }}</span> / 
                错误 <span class="wrong-num">{{ volume.wrongCount }}</span> / 
                共 {{ volume.totalCount }} 题
              </span>
            </div>
            
            <!-- 按大题分组 -->
            <div v-for="section in volume.sections" :key="section.sectionId" class="section-group">
              <div class="section-header">
                <span class="section-name">{{ section.sectionName || '大题' }}</span>
                <span class="section-stats">
                  正确 {{ section.correctCount }} / 错误 {{ section.wrongCount }} / 共 {{ section.totalCount }} 题
                </span>
              </div>
              
              <!-- 题目列表 -->
              <el-table
                :data="section.questions"
                border
                stripe
                size="small"
                style="width: 100%"
                :row-class-name="getRowClassName"
              >
                <el-table-column label="序号" width="60" align="center">
                  <template slot-scope="scope">
                    {{ scope.row.questionSort || scope.$index + 1 }}
                  </template>
                </el-table-column>
                <el-table-column label="题目标题" prop="title" min-width="200" show-overflow-tooltip>
                  <template slot-scope="scope">
                    <span v-html="formatTitle(scope.row.title)"></span>
                  </template>
                </el-table-column>
                <el-table-column label="题目类型" width="90" align="center">
                  <template slot-scope="scope">
                    <el-tag size="mini" type="info">{{ getQuestionTypeName(scope.row.type) }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="用户答案" width="120" align="center">
                  <template slot-scope="scope">
                    <span :class="{ 'answer-correct': scope.row.result === 1, 'answer-wrong': scope.row.result === 0 }">
                      {{ scope.row.userAnswerText || scope.row.userAnswer || '-' }}
                    </span>
                  </template>
                </el-table-column>
                <el-table-column label="正确答案" width="120" align="center">
                  <template slot-scope="scope">
                    <span class="correct-answer">{{ scope.row.correctAnswerText || '-' }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="结果" width="70" align="center">
                  <template slot-scope="scope">
                    <el-tag :type="scope.row.result === 1 ? 'success' : 'danger'" size="mini">
                      {{ scope.row.result === 1 ? '正确' : '错误' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="用时" width="70" align="center">
                  <template slot-scope="scope">
                    <span>{{ scope.row.timeSpent || 0 }}秒</span>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>
          
          <!-- 无数据提示 -->
          <div v-if="!loading && groupedResults.length === 0" class="no-data">
            <i class="el-icon-document"></i>
            <p>暂无答题记录</p>
          </div>
        </div>
      </el-card>

      <!-- 操作按钮 -->
      <div class="action-buttons">
        <el-button type="primary" @click="handleRetake" :disabled="!canRetake" size="medium">
          {{ canRetake ? '重新考试' : `已达到练习次数限制（${practiceInfo.practiceCount}/${practiceInfo.practiceLimit}）` }}
        </el-button>
        <el-button @click="handleBack" size="medium">返回试卷选择</el-button>
      </div>
    </div>
    </transition>
  </div>
</template>

<script>
const { ipcRenderer } = require('electron')
import ExamResultSkeleton from '../components/ExamResultSkeleton.vue'

export default {
  name: 'ExamResult',
  components: {
    ExamResultSkeleton
  },
  data() {
    return {
      loading: false,
      dataReady: false, // 数据是否准备就绪
      paperInfoId: null,
      paperId: null,
      resultData: {
        totalScore: 100,
        userScore: 0,
        correctCount: 0,
        wrongCount: 0,
        usedTime: 0
      },
      groupedResults: [], // 按卷别和大题分组的结果
      totalQuestionCount: 0,
      practiceInfo: {
        allowed: true,
        practiceCount: 0,
        practiceLimit: 0
      },
      canRetake: true,
      // 缓存数据
      volumesMap: {}, // 卷别信息 { volumeCode: volumeName }
      sectionsMap: {}, // 大题信息 { sectionId: { name, volumeCode, order } }
      questionsMap: {} // 题目信息 { questionId: questionData }
    }
  },
  async mounted() {
    // 从路由参数获取 paperInfoId
    this.paperInfoId = this.$route.query.paperInfoId
    if (!this.paperInfoId) {
      this.$message.error('缺少答题记录ID')
      this.$router.push('/paper-select')
      return
    }

    // 开始加载，显示骨架屏
    this.loading = true
    this.dataReady = false
    
    await this.loadResultData()
    await this.checkPracticeLimit()
    
    // 延迟设置 dataReady，让骨架屏有平滑过渡
    setTimeout(() => {
      this.dataReady = true
    }, 100)
  },
  methods: {
    /**
     * 加载答题结果数据
     */
    async loadResultData() {
      this.loading = true
      try {
        // 获取答题记录详情
        const result = await ipcRenderer.invoke('answer:getPaperInfo', this.paperInfoId)
        
        if (!result) {
          this.$message.error('未找到答题记录')
          this.$router.push('/paper-select')
          return
        }

        this.paperId = result.paper_id
        
        // 获取试卷数据（包含卷别、大题、题目信息）
        await this.loadPaperStructure()
        
        // 处理答题结果
        const questionResults = result.questionResults || []
        
        // 计算统计信息
        let correctCount = 0
        let wrongCount = 0
        let totalTimeSpent = 0
        
        questionResults.forEach(r => {
          if (r.result === 1) correctCount++
          else wrongCount++
          totalTimeSpent += r.time_spent || 0
        })
        
        // 使用数据库中的统计信息，如果没有则使用计算值
        this.resultData = {
          totalScore: result.total_score || 100,
          userScore: result.user_score || 0,
          correctCount: result.correct_count || correctCount,
          wrongCount: result.wrong_count || wrongCount,
          usedTime: result.used_time || totalTimeSpent
        }
        
        this.totalQuestionCount = questionResults.length
        
        // 按卷别和大题分组
        this.groupedResults = this.groupResultsByVolumeAndSection(questionResults)

        // 如果答题结果还未提交，自动提交
        if (!result.is_submit) {
          await this.autoSubmit()
        }
      } catch (error) {
        console.error('加载答题结果失败:', error)
        this.$message.error('加载答题结果失败：' + error.message)
      } finally {
        this.loading = false
      }
    },
    
    /**
     * 加载试卷结构（卷别、大题、题目）
     */
    async loadPaperStructure() {
      try {
        // 获取试卷数据
        const paperData = await ipcRenderer.invoke('paper:getPaperData', this.paperId)
        console.log('[ExamResult] paperData:', paperData ? '已获取' : '为空')
        
        if (!paperData) {
          console.warn('无法获取试卷数据')
          return
        }
        
        // 解析卷别信息
        if (paperData.manifest && paperData.manifest.volumes) {
          paperData.manifest.volumes.forEach(v => {
            const code = v.volumeCode || v.volume_code
            this.volumesMap[code] = v.volumeName || v.volume_name || `卷别 ${code}`
          })
          console.log('[ExamResult] volumesMap:', this.volumesMap)
        }
        
        // 获取大题信息
        const sections = await ipcRenderer.invoke('paper:getSections', this.paperId)
        console.log('[ExamResult] sections:', sections?.length || 0, '个')
        
        if (sections && sections.length > 0) {
          sections.forEach(s => {
            this.sectionsMap[s.id] = {
              name: s.section_name || s.sectionName || '大题',
              volumeCode: s.volume_code || s.volumeCode,
              order: s.section_order || s.sectionOrder || 0
            }
          })
        }
        
        // 获取所有题目信息（用于显示题目标题和正确答案）
        for (const section of sections) {
          const questions = await ipcRenderer.invoke('paper:getQuestions', this.paperId, section.id)
          console.log(`[ExamResult] section ${section.id} 题目:`, questions?.length || 0, '个')
          
          if (questions && questions.length > 0) {
            questions.forEach(q => {
              // 解析选项数据（可能是 JSON 字符串）
              let answers = q.answers || q.options || []
              if (typeof answers === 'string') {
                try {
                  answers = JSON.parse(answers)
                } catch (e) {
                  answers = []
                }
              }
              
              this.questionsMap[q.id] = {
                ...q,
                answers,
                sectionId: section.id,
                volumeCode: section.volume_code || section.volumeCode
              }
              
              console.log(`[ExamResult] 题目 ${q.id}:`, {
                title: q.title?.substring(0, 20),
                answer: q.answer,
                answersCount: answers?.length || 0
              })
            })
          }
        }
        
        console.log('[ExamResult] questionsMap 总数:', Object.keys(this.questionsMap).length)
      } catch (error) {
        console.error('加载试卷结构失败:', error)
      }
    },
    
    /**
     * 按卷别和大题分组答题结果
     */
    groupResultsByVolumeAndSection(questionResults) {
      console.log('[ExamResult] 开始分组，答题结果数量:', questionResults.length)
      const volumeMap = new Map() // volumeCode -> { sections: Map<sectionId, questions[]> }
      
      questionResults.forEach(result => {
        const questionId = result.question_id
        const question = this.questionsMap[questionId] || {}
        const sectionId = question.sectionId || question.section_id || 'unknown'
        const sectionInfo = this.sectionsMap[sectionId] || {}
        const volumeCode = sectionInfo.volumeCode || question.volumeCode || 'default'
        
        console.log(`[ExamResult] 处理题目 ${questionId}:`, {
          found: !!this.questionsMap[questionId],
          sectionId,
          volumeCode,
          userAnswer: result.user_answer,
          answerIds: result.answer_ids
        })
        
        // 获取正确答案文本和用户答案文本
        let correctAnswerText = ''
        let userAnswerText = result.user_answer || ''
        
        // 获取选项列表
        const answers = question.answers || []
        
        if (answers.length > 0) {
          // 从选项的 isAnswer/is_answer 字段判断正确答案
          const correctOptions = answers.filter(opt => 
            opt.isAnswer === 1 || opt.is_answer === 1 || opt.isAnswer === true || opt.is_answer === true
          )
          correctAnswerText = correctOptions.map(opt => 
            opt.optionName || opt.option_name || opt.label || opt.text || ''
          ).join(', ')
          
          // 如果没有找到，尝试从 question.answer 字段解析
          if (!correctAnswerText && question.answer) {
            let correctAnswerIds = []
            if (Array.isArray(question.answer)) {
              correctAnswerIds = question.answer
            } else if (typeof question.answer === 'string' && question.answer) {
              correctAnswerIds = question.answer.split(',').map(id => parseInt(id.trim())).filter(id => !isNaN(id))
            } else if (typeof question.answer === 'number') {
              correctAnswerIds = [question.answer]
            }
            
            if (correctAnswerIds.length > 0) {
              const correctOpts = answers.filter(opt => {
                const optId = opt.id || opt.answerId || opt.answer_id
                return correctAnswerIds.includes(optId)
              })
              correctAnswerText = correctOpts.map(opt => 
                opt.optionName || opt.option_name || opt.label || opt.text || ''
              ).join(', ')
            }
          }
          
          // 解析用户答案
          if (result.answer_ids) {
            const userAnswerIds = String(result.answer_ids).split(',').map(id => parseInt(id.trim())).filter(id => !isNaN(id))
            const userOptions = answers.filter(opt => {
              const optId = opt.id || opt.answerId || opt.answer_id
              return userAnswerIds.includes(optId)
            })
            if (userOptions.length > 0) {
              userAnswerText = userOptions.map(opt => 
                opt.optionName || opt.option_name || opt.label || opt.text || ''
              ).join(', ')
            }
          }
          
          console.log(`[ExamResult] 题目 ${questionId} 答案解析:`, {
            correctAnswerText,
            userAnswerText,
            answersCount: answers.length
          })
        } else {
          console.log(`[ExamResult] 题目 ${questionId} 没有选项数据`)
        }
        
        // 构建题目结果对象
        const questionResult = {
          questionId,
          title: question.title || '',
          type: question.type || 0,
          questionSort: result.question_sort || question.sort_order || question.question_sort || 0,
          userAnswer: result.user_answer || '',
          userAnswerText,
          correctAnswerText,
          result: result.result || 0,
          timeSpent: result.time_spent || 0,
          sectionId,
          volumeCode
        }
        
        // 添加到分组
        if (!volumeMap.has(volumeCode)) {
          volumeMap.set(volumeCode, {
            volumeCode,
            volumeName: this.volumesMap[volumeCode] || `卷别 ${volumeCode}`,
            sections: new Map()
          })
        }
        
        const volumeData = volumeMap.get(volumeCode)
        if (!volumeData.sections.has(sectionId)) {
          volumeData.sections.set(sectionId, {
            sectionId,
            sectionName: sectionInfo.name || '大题',
            sectionOrder: sectionInfo.order || 0,
            questions: []
          })
        }
        
        volumeData.sections.get(sectionId).questions.push(questionResult)
      })
      
      // 转换为数组并排序
      const result = []
      volumeMap.forEach(volumeData => {
        const sections = []
        let volumeCorrect = 0
        let volumeWrong = 0
        
        volumeData.sections.forEach(sectionData => {
          // 按题目序号排序
          sectionData.questions.sort((a, b) => a.questionSort - b.questionSort)
          
          // 计算大题统计
          const sectionCorrect = sectionData.questions.filter(q => q.result === 1).length
          const sectionWrong = sectionData.questions.filter(q => q.result === 0).length
          
          sections.push({
            ...sectionData,
            correctCount: sectionCorrect,
            wrongCount: sectionWrong,
            totalCount: sectionData.questions.length
          })
          
          volumeCorrect += sectionCorrect
          volumeWrong += sectionWrong
        })
        
        // 按大题顺序排序
        sections.sort((a, b) => a.sectionOrder - b.sectionOrder)
        
        result.push({
          volumeCode: volumeData.volumeCode,
          volumeName: volumeData.volumeName,
          sections,
          correctCount: volumeCorrect,
          wrongCount: volumeWrong,
          totalCount: volumeCorrect + volumeWrong
        })
      })
      
      return result
    },

    /**
     * 自动提交答题结果
     */
    async autoSubmit() {
      try {
        const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
        const userId = userInfo.userId || userInfo.user?.userId

        if (!userId) {
          console.warn('无法获取用户ID，跳过自动提交')
          return
        }

        const submitResult = await ipcRenderer.invoke('answer:submitExam', {
          paperInfoId: this.paperInfoId,
          paperId: this.paperId,
          appUserId: userId,
          totalScore: this.resultData.totalScore,
          userScore: this.resultData.userScore,
          correctCount: this.resultData.correctCount,
          wrongCount: this.resultData.wrongCount
        })

        if (submitResult.success) {
          console.log('✓ 答题结果已自动提交')
        }
      } catch (error) {
        console.error('自动提交失败:', error)
      }
    },

    /**
     * 检查练习次数限制
     */
    async checkPracticeLimit() {
      try {
        const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
        const userId = userInfo.userId || userInfo.user?.userId

        if (!userId || !this.paperId) {
          return
        }

        const practiceInfo = await ipcRenderer.invoke('answer:checkPracticeLimit', this.paperId, userId)
        this.practiceInfo = practiceInfo
        this.canRetake = practiceInfo.allowed
      } catch (error) {
        console.error('检查练习次数限制失败:', error)
      }
    },

    /**
     * 重新考试
     */
    async handleRetake() {
      if (!this.canRetake) {
        this.$message.warning(`已达到练习次数限制（${this.practiceInfo.practiceCount}/${this.practiceInfo.practiceLimit}）`)
        return
      }

      this.$confirm('确定要重新考试吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await ipcRenderer.invoke('answer:clearPaperResult', {
            paperInfoId: this.paperInfoId,
            paperId: this.paperId
          })

          this.$message.success('准备重新开始')
          this.$router.push('/paper-select')
        } catch (error) {
          console.error('清空答题记录失败:', error)
          this.$message.error('操作失败：' + error.message)
        }
      }).catch(() => {})
    },

    /**
     * 返回试卷选择页面
     */
    handleBack() {
      this.$router.push('/paper-select')
    },

    /**
     * 格式化时间（秒转时分秒）
     */
    formatTime(seconds) {
      if (!seconds) return '0秒'
      const hours = Math.floor(seconds / 3600)
      const minutes = Math.floor((seconds % 3600) / 60)
      const secs = seconds % 60
      
      if (hours > 0) {
        return `${hours}小时${minutes}分${secs}秒`
      } else if (minutes > 0) {
        return `${minutes}分${secs}秒`
      } else {
        return `${secs}秒`
      }
    },

    /**
     * 获取题目类型名称
     */
    getQuestionTypeName(type) {
      const typeMap = {
        0: '单选题',
        1: '多选题',
        2: '判断题',
        3: '填空题',
        4: '完形填空'
      }
      return typeMap[type] || '单选题'
    },
    
    /**
     * 格式化题目标题（去除HTML标签）
     */
    formatTitle(title) {
      if (!title) return ''
      // 简单处理，保留基本格式
      return title.replace(/<[^>]+>/g, '').substring(0, 100)
    },
    
    /**
     * 获取行样式类名
     */
    getRowClassName({ row }) {
      return row.result === 1 ? 'row-correct' : 'row-wrong'
    }
  }
}
</script>

<style scoped>
.exam-result-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.result-content {
  max-width: 1200px;
  margin: 0 auto;
}

.statistics-card,
.detail-card {
  margin-bottom: 20px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
}

.total-questions {
  font-size: 14px;
  color: #909399;
}

.statistics-content {
  display: flex;
  justify-content: space-around;
  padding: 20px 0;
  flex-wrap: wrap;
}

.stat-item {
  text-align: center;
  min-width: 100px;
  padding: 10px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 10px;
}

.stat-value {
  font-size: 36px;
  font-weight: bold;
  color: #303133;
}

.score-value {
  color: #409EFF;
}

.score-value.score-pass {
  color: #67C23A;
}

.correct-value {
  color: #67C23A;
}

.wrong-value {
  color: #F56C6C;
}

.time-value {
  font-size: 28px;
}

/* 卷别分组样式 */
.volume-group {
  margin-bottom: 24px;
  border: 1px solid #EBEEF5;
  border-radius: 8px;
  overflow: hidden;
}

.volume-header {
  background: linear-gradient(135deg, #409EFF 0%, #66b1ff 100%);
  color: #fff;
  padding: 12px 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.volume-name {
  font-size: 16px;
  font-weight: bold;
}

.volume-stats {
  font-size: 13px;
}

.volume-stats .correct-num {
  color: #95d475;
  font-weight: bold;
}

.volume-stats .wrong-num {
  color: #fab6b6;
  font-weight: bold;
}

/* 大题分组样式 */
.section-group {
  border-top: 1px solid #EBEEF5;
}

.section-header {
  background: #f5f7fa;
  padding: 10px 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #EBEEF5;
}

.section-name {
  font-size: 14px;
  font-weight: 600;
  color: #606266;
}

.section-stats {
  font-size: 12px;
  color: #909399;
}

/* 答案样式 */
.answer-correct {
  color: #67C23A;
  font-weight: 600;
}

.answer-wrong {
  color: #F56C6C;
  font-weight: 600;
}

.correct-answer {
  color: #67C23A;
  font-weight: 500;
}

/* 表格行样式 */
::v-deep .row-correct {
  background-color: #f0f9eb !important;
}

::v-deep .row-wrong {
  background-color: #fef0f0 !important;
}

/* 无数据提示 */
.no-data {
  text-align: center;
  padding: 60px 0;
  color: #909399;
}

.no-data i {
  font-size: 48px;
  margin-bottom: 16px;
}

/* 操作按钮 */
.action-buttons {
  text-align: center;
  margin-top: 30px;
  padding: 20px 0;
}

.action-buttons .el-button {
  margin: 0 10px;
  min-width: 120px;
}

/* 详情内容区域 */
.detail-content {
  min-height: 200px;
}

/* 过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter,
.fade-leave-to {
  opacity: 0;
}

.fade-up-enter-active,
.fade-up-leave-active {
  transition: opacity 0.4s ease, transform 0.4s ease;
}

.fade-up-enter,
.fade-up-leave-to {
  opacity: 0;
  transform: translateY(20px);
}
</style>

