<template>
  <div class="section-list-container">
    <!-- 卷别完成提示遮罩 -->
    <div v-if="showVolumeComplete" class="volume-complete-overlay">
      <div class="complete-message">
        <div class="message-icon">
          <i class="el-icon-success"></i>
        </div>
        <div class="message-text">{{ volumeCompleteText }}</div>
      </div>
    </div>

    <!-- 左侧主内容区：显示当前卷别下的大题（按顺序显示）或题目 -->
    <div class="main-content">
      <!-- 固定悬浮头部（始终显示） -->
      <div class="fixed-header">
        <!-- 大题标题 -->
        <div class="header-section-title">{{ currentPlayingSectionName || currentSectionName }}</div>
        
        <!-- 题号信息（仅在显示题目时显示） -->
        <div v-if="showQuestions" class="header-question-info">第 {{ currentQuestionIndex + 1 }} 题 / 共 {{ allQuestions.length }} 题</div>
        
        <!-- 倒计时 -->
        <div v-if="answerCountdown > 0" class="header-countdown" :class="{ 'countdown-warning': answerCountdown <= 5 }">
          <span class="countdown-number">{{ answerCountdown }}</span>
          <span class="countdown-text">秒</span>
        </div>
        
        <!-- 音频播放标识 -->
        <div v-if="questionAudioPlaying || sectionAudioPlaying" class="header-audio-indicator">
          <i class="el-icon-headset"></i>
          <span>播放中</span>
        </div>
      </div>

      <!-- 大题音频播放时显示（空白区域，等待音频播放完成） -->
      <div v-if="!showQuestions" class="section-audio-playing">
        <!-- 空白区域，题目内容在音频播放完成后才显示 -->
      </div>

      <!-- 题目显示区域（音频播放完成后显示） -->
      <div v-if="showQuestions && currentQuestion" class="questions-wrapper">

        <!-- 题目内容区域（可滚动，显示多题） -->
        <div class="question-content-wrapper">
          <!-- 显示当前题目及其后续的4题（共5题） -->
          <div 
            v-for="(question, index) in getVisibleQuestions()" 
            :key="question.id || index"
            class="question-container" 
            :class="{ 
              'question-expired': isQuestionExpired(getQuestionGlobalIndex(index)),
              'question-current': getQuestionGlobalIndex(index) === currentQuestionIndex
            }"
            :ref="getQuestionGlobalIndex(index) === currentQuestionIndex ? 'currentQuestionElement' : null"
          >
            <div class="question-header-inline">
              <span v-if="getQuestionGlobalIndex(index) === currentQuestionIndex" class="current-arrow-indicator">▶</span>
              <span class="question-number-badge">第 {{ getQuestionGlobalIndex(index) + 1 }} 题</span>
            </div>
            
            <div class="question-content">
              <div v-if="question.title" class="question-title" v-html="formatQuestionTitle(question.title)"></div>
              
              <div v-if="question.text" class="question-text" v-html="formatQuestionText(question.text)"></div>

              <!-- 选项（两列布局） -->
              <div v-if="question.answers && question.answers.length > 0" class="question-options-grid">
                <div 
                  v-for="(option, optIndex) in question.answers" 
                  :key="optIndex"
                  class="option-item-compact"
                  :class="{ 
                    'option-disabled': isQuestionExpired(getQuestionGlobalIndex(index)),
                    'option-selected': isOptionSelected(getQuestionGlobalIndex(index), option.id || optIndex)
                  }"
                  @click="selectOption(getQuestionGlobalIndex(index), option.id || optIndex, option)"
                >
                  <span class="option-label">{{ option.label || option.optionName || String.fromCharCode(65 + optIndex) }}</span>
                  <span class="option-text" v-html="formatOptionText(option.text)"></span>
                </div>
              </div>
              <div v-else class="no-options">
                <p>本题无选项</p>
              </div>
            </div>
          </div>
        </div>

        <!-- 分页按钮（注释掉，保留代码但不显示） -->
        <!-- <div class="pagination-controls">
          <el-button 
            v-if="currentQuestionIndex > 0"
            type="primary"
            @click="goToPreviousQuestion"
            :disabled="loading"
            size="medium"
          >
            <i class="el-icon-arrow-left"></i> 上一题
          </el-button>
          <div v-else style="width: 120px;"></div>
          <el-button 
            v-if="currentQuestionIndex < allQuestions.length - 1"
            type="primary"
            @click="goToNextQuestion"
            :disabled="loading"
            size="medium"
          >
            下一题 <i class="el-icon-arrow-right"></i>
          </el-button>
          <div v-else style="width: 120px;"></div>
        </div> -->
      </div>
      
      <!-- 加载中提示 -->
      <div v-if="loading && !showQuestions" class="loading-wrapper">
        <el-loading :loading="loading" text="正在加载题目..."></el-loading>
      </div>
    </div>

    <!-- 右侧学生信息区 -->
    <div class="student-info-panel">
      <!-- 头像 -->
      <div class="avatar-section">
        <div class="avatar-number" v-if="avatarNumber">{{ avatarNumber }}</div>
        <div class="avatar-placeholder">
          <i class="el-icon-user-solid"></i>
        </div>
      </div>
      
      <!-- 学生信息 -->
      <div class="student-details">
        <div class="info-item">
          <span class="info-label">姓名</span>
          <span class="info-value">{{ studentName || '未知' }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">考生号</span>
          <span class="info-value">{{ studentId || '未知' }}</span>
        </div>
        <div class="info-item" v-if="seatNumber">
          <span class="info-label">座位号</span>
          <span class="info-value">{{ seatNumber }}</span>
        </div>
      </div>

      <!-- 题目序号列表（中间部分） -->
      <div class="question-numbers-section">
        <div 
          v-for="section in currentVolumeSections" 
          :key="section.id"
          class="question-group"
        >
          <div class="section-title">
            {{ section.section_name || section.sectionName || '大题' }}
          </div>
          <div class="question-numbers">
            <span 
              v-for="(questionNum, qIndex) in getQuestionNumbers(section.id)" 
              :key="questionNum"
              class="question-number"
              :class="{
                'question-number-current': isCurrentQuestion(section.id, qIndex),
                'question-number-answered': isQuestionAnswered(section.id, qIndex),
                'question-number-expired': isQuestionExpiredBySection(section.id, qIndex)
              }"
            >
              {{ questionNum }}
            </span>
          </div>
        </div>
      </div>

      <!-- 音量调节（在学生信息区域最底部） -->
      <div class="volume-control">
        <el-slider
          v-model="volume"
          :min="0"
          :max="100"
          :step="1"
          @change="onVolumeChange"
          class="volume-slider"
        ></el-slider>
        <div class="volume-info">
          <i class="el-icon-microphone"></i>
          <span>音量: {{ volume }}%</span>
        </div>
      </div>
    </div>

    <!-- 音频频率动画（页面最底部） -->
    <div class="audio-frequency-bar">
      <canvas ref="frequencyCanvas" class="frequency-canvas"></canvas>
    </div>

    <!-- 音频播放器（隐藏，用于播放大题音频） -->
    <audio
      ref="sectionAudioPlayer"
      :src="currentSectionAudioPath"
      @ended="onSectionAudioEnded"
      @error="onSectionAudioError"
      @loadedmetadata="onSectionAudioLoaded"
      @canplay="onSectionAudioCanPlay"
    ></audio>
    
    <!-- 题目音频播放器（隐藏） -->
    <audio
      ref="questionAudioPlayer"
      :src="questionAudioPath"
      @ended="onQuestionAudioEnded"
      @error="onQuestionAudioError"
    ></audio>
  </div>
</template>

<script>
const { ipcRenderer } = require('electron')
const path = require('path')
const fs = require('fs')
const { pathToFileURL } = require('url')

// 日志工具函数：将日志发送到主进程控制台
function logToMain(level, ...args) {
  try {
    ipcRenderer.send('app:log', { level, message: args.join(' '), args: args })
  } catch (error) {
    // 如果 IPC 失败，静默失败（避免循环错误）
  }
}

// 重写 console.log, console.warn, console.error 以同时输出到主进程
const originalLog = console.log
const originalWarn = console.warn
const originalError = console.error

console.log = function(...args) {
  originalLog.apply(console, args)
  logToMain('info', ...args)
}

console.warn = function(...args) {
  originalWarn.apply(console, args)
  logToMain('warn', ...args)
}

console.error = function(...args) {
  originalError.apply(console, args)
  logToMain('error', ...args)
}

// 递归处理所有文本字段，确保UTF-8编码正确
function processTextFields(obj) {
  if (obj === null || obj === undefined) return obj
  if (typeof obj === 'string') {
    return String(obj)
  }
  if (Array.isArray(obj)) {
    return obj.map(item => processTextFields(item))
  }
  if (typeof obj === 'object') {
    const processed = {}
    for (const key in obj) {
      if (obj.hasOwnProperty(key)) {
        const value = obj[key]
        if (typeof value === 'string') {
          processed[key] = String(value)
        } else if (Array.isArray(value)) {
          processed[key] = processTextFields(value)
        } else if (typeof value === 'object' && value !== null) {
          processed[key] = processTextFields(value)
        } else {
          processed[key] = value
        }
      }
    }
    return processed
  }
  return obj
}

export default {
  name: 'SectionList',
  data() {
    return {
      paperId: null,
      paperInfoId: null, // 答题记录ID（用于多卷别复用）
      currentVolumeCode: null, // 当前卷别代码
      currentVolumeId: null, // 当前卷别ID（优先使用，因为volumeCode可能为空）
      currentVolumeSections: [], // 当前卷别下的大题列表
      sectionQuestionsMap: {}, // { sectionId: [questionNumbers] }
      volume: 50, // 音量（0-100）
      // 学生信息
      studentName: '',
      studentId: '',
      seatNumber: '',
      avatarNumber: null,
      // 音频频率可视化
      audioContext: null,
      analyser: null,
      dataArray: null,
      animationFrameId: null,
      // 大题音频播放
      currentSectionAudioPath: '',
      currentSectionIndex: 0, // 当前正在播放的大题索引
      sectionAudioPlaying: false,
      sectionAudioTimer: null, // 音频播放完成后的定时器
      // 题目显示
      showQuestions: false, // 是否显示题目
      allQuestions: [], // 所有题目列表
      currentQuestionIndex: 0, // 当前题目索引
      currentQuestion: null, // 当前题目数据
      currentQuestionOptions: [], // 当前题目选项
      loading: false, // 加载状态
      questionsLoadTimer: null, // 题目加载定时器
      // 题目音频播放
      questionAudioPath: '', // 当前题目音频路径
      questionAudioPlaying: false, // 题目音频是否正在播放
      questionAudioPlayCount: 1, // 题目音频需要播放的次数（从大题设置获取）
      questionAudioPlayedCount: 0, // 题目音频已播放的次数
      questionAudioTimer: null, // 题目音频相关定时器
      isPlayingQuestionAudio: false, // 是否正在执行播放题目音频的逻辑（防重复调用）
      // 答题计时
      answerTime: 5, // 答题时间（秒，从大题设置获取）
      answerCountdown: 0, // 答题倒计时（秒）
      answerCountdownTimer: null, // 答题倒计时定时器
      // 题目状态管理
      expiredQuestionIndexes: [], // 已过期的题目索引列表
      answeredQuestions: {}, // 已答题记录 { questionIndex: answerId }
      answeredTimeSpent: {}, // 已答题用时记录 { questionIndex: timeSpent }
      autoNextTimer: null, // 选择答案后自动跳转下一题的定时器
      // 卷别完成提示
      showVolumeComplete: false, // 是否显示卷别完成提示
      volumeCompleteText: '', // 完成提示文字
      isAllComplete: false // 是否所有卷别都完成
    }
  },
  computed: {
    // 当前卷别标题（大题标题）
    currentVolumeTitle() {
      if (!this.currentVolume) {
        return '试卷'
      }
      return this.currentVolume.volume_name || this.currentVolume.volumeName || '试卷'
    },
    
    // 当前正在播放音频的大题名称
    currentPlayingSectionName() {
      if (!this.currentVolumeSections.length || this.showQuestions) return ''
      const section = this.currentVolumeSections[this.currentSectionIndex]
      return section?.section_name || section?.sectionName || ''
    },
    
    // 当前大题名称
    currentSectionName() {
      // 根据当前题目找到对应的大题名称
      if (!this.currentQuestion || !this.currentVolumeSections.length) {
        // 如果没有当前题目，返回第一个大题的名称或空字符串
        return this.currentVolumeSections[0]?.section_name || this.currentVolumeSections[0]?.sectionName || ''
      }
      
      // 尝试从题目数据中获取 sectionId 或 section_id
      const questionSectionId = this.currentQuestion.sectionId || this.currentQuestion.section_id
      if (questionSectionId) {
        const section = this.currentVolumeSections.find(s => s.id === questionSectionId)
        if (section) {
          return section.section_name || section.sectionName || ''
        }
      }
      
      // 如果找不到，返回第一个大题的名称
      return this.currentVolumeSections[0]?.section_name || this.currentVolumeSections[0]?.sectionName || ''
    }
  },
  async mounted() {
    console.log('🚀 SectionList.vue mounted 开始')
    
    // 获取试卷ID和当前卷别信息
    this.paperId = parseInt(localStorage.getItem('currentPaperId')) || null
    this.currentVolumeCode = localStorage.getItem('currentVolumeCode') || ''
    // 优先使用 volumeId（因为 volumeCode 可能为空）
    const storedVolumeId = localStorage.getItem('currentVolumeId')
    this.currentVolumeId = storedVolumeId ? parseInt(storedVolumeId) : null
    
    console.log('🚀 [SectionList] 获取卷别信息:', { 
      volumeId: this.currentVolumeId, 
      volumeCode: this.currentVolumeCode 
    })
    
    if (!this.paperId) {
      console.error('❌ 无法获取试卷ID')
      return
    }

    // 初始化或复用答题记录
    await this.initOrReusePaperInfo()

    // 加载学生信息
    await this.loadStudentInfo()
    
    // 加载大题和题目数据
    await this.loadSectionsAndQuestions()
  },

  methods: {
    /**
     * 初始化或复用答题记录
     */
    async initOrReusePaperInfo() {
      try {
        const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
        const userId = userInfo.userId || userInfo.user?.userId
        
        if (!userId || !this.paperId) {
          return
        }

        // 检查是否有未提交的答题记录
        const paperInfoList = await ipcRenderer.invoke('answer:getPaperInfoList', userId, this.paperId)
        const unsubmittedPaperInfo = paperInfoList.find(p => p.is_submit === 0)
        
        if (unsubmittedPaperInfo) {
          // 复用现有答题记录
          this.paperInfoId = unsubmittedPaperInfo.id
          localStorage.setItem('currentPaperInfoId', this.paperInfoId.toString())
          console.log('✓ 复用现有答题记录，paperInfoId:', this.paperInfoId)
        } else {
          // 创建新的答题记录
          const paperData = await ipcRenderer.invoke('paper:getPaperData', this.paperId)
          const paperName = paperData?.manifest?.paperName || '未命名试卷'
          const volumes = paperData?.manifest?.volumes || []
          const volumeCodes = volumes.map(v => v.volumeCode || v.volume_code).filter(Boolean)
          
          const startResult = await ipcRenderer.invoke('answer:startExam', {
            paperId: this.paperId,
            paperName: paperName,
            appUserId: userId,
            volumeCodes: volumeCodes,
            assignedSeatNumber: null
          })
          
          if (startResult.success) {
            this.paperInfoId = startResult.paperInfoId
            localStorage.setItem('currentPaperInfoId', this.paperInfoId.toString())
            console.log('✓ 创建新答题记录，paperInfoId:', this.paperInfoId)
          }
        }
      } catch (error) {
        console.error('初始化答题记录失败:', error)
      }
    },

    async loadStudentInfo() {
      try {
        // 首先从 localStorage 获取 userInfo（完全离线可用）
        const userInfoStr = localStorage.getItem('userInfo')
        let userInfo = null
        if (userInfoStr) {
          userInfo = JSON.parse(userInfoStr)
          const user = userInfo.user || {}
          const archive = userInfo.archive || {}
          
          // 优先使用新字段 student_name（学员姓名）和 student_account（学号）
          // 注意：student_name 是姓名，student_account 是学号
          this.studentName = String(archive.student_name || archive.studentName || archive.name || user.nickName || '未知')
          this.studentId = String(archive.student_account || archive.studentAccount || archive.account || user.userName || '未知')
          this.seatNumber = String(archive.seat_number || archive.seatNumber || '')
          this.avatarNumber = archive.avatar_number || archive.avatarNumber || null
          
          console.log('✓ 从 localStorage 获取学生信息:', { 
            name: this.studentName, 
            id: this.studentId, 
            seat: this.seatNumber 
          })
        }

        // 如果座位号等信息缺失，尝试从本地数据库获取学员档案（完全离线可用）
        if ((!this.seatNumber || !this.studentName || this.studentName === '未知') && userInfo) {
          try {
            const userId = userInfo.user?.userId
            const account = userInfo.user?.userName
            
            if (userId) {
              const archive = await ipcRenderer.invoke('archive:getByUserId', userId)
              if (archive) {
                // 补充缺失的信息（优先使用 student_name 字段）
                if (!this.studentName || this.studentName === '未知') {
                  this.studentName = String(archive.student_name || archive.studentName || archive.name || this.studentName)
                }
                if (!this.studentId || this.studentId === '未知') {
                  this.studentId = String(archive.student_account || archive.studentAccount || archive.account || this.studentId)
                }
                if (!this.seatNumber) {
                  this.seatNumber = String(archive.seat_number || archive.seatNumber || '')
                }
                console.log('✓ 从本地数据库补充学生信息:', { name: this.studentName, id: this.studentId, seat: this.seatNumber })
              }
            } else if (account) {
              const archive = await ipcRenderer.invoke('archive:getByAccount', account)
              if (archive) {
                // 补充缺失的信息（优先使用 student_name 字段）
                if (!this.studentName || this.studentName === '未知') {
                  this.studentName = String(archive.student_name || archive.studentName || archive.name || this.studentName)
                }
                if (!this.studentId || this.studentId === '未知') {
                  this.studentId = String(archive.student_account || archive.studentAccount || archive.account || this.studentId)
                }
                if (!this.seatNumber) {
                  this.seatNumber = String(archive.seat_number || archive.seatNumber || '')
                }
                console.log('✓ 从本地数据库补充学生信息:', { name: this.studentName, id: this.studentId, seat: this.seatNumber })
              }
            }
          } catch (error) {
            console.warn('从本地数据库获取学员档案失败（不影响离线使用）:', error)
            // 不影响主流程，继续使用 localStorage 中的信息
          }
        }
      } catch (error) {
        console.error('加载学生信息失败:', error)
      }
    },

    async loadSectionsAndQuestions() {
      try {
        console.log('📦 [loadSectionsAndQuestions] 开始加载大题和题目数据')
        console.log('📦 [loadSectionsAndQuestions] paperId:', this.paperId)
        console.log('📦 [loadSectionsAndQuestions] currentVolumeCode:', this.currentVolumeCode, '类型:', typeof this.currentVolumeCode)
        
        // 先获取当前卷别的 volume_id（用于筛选大题）
        let targetVolumeId = null
        const targetVolumeCode = this.currentVolumeCode || ''
        
        // 优先使用 currentVolumeId（从 localStorage 获取）
        // 如果没有，则从 manifest 中根据 volumeCode 查找
        if (this.currentVolumeId) {
          targetVolumeId = this.currentVolumeId
          console.log('📦 [loadSectionsAndQuestions] 使用 localStorage 中的 volumeId:', targetVolumeId)
        } else {
          // 从 manifest 中获取 volume_id
          const paperData = await ipcRenderer.invoke('paper:getPaperData', this.paperId)
          if (paperData && paperData.manifest && paperData.manifest.volumes) {
            console.log('📦 [loadSectionsAndQuestions] manifest.volumes:', JSON.stringify(paperData.manifest.volumes.map(v => ({
              id: v.id,
              volumeCode: v.volumeCode,
              volumeName: v.volumeName
            }))))
            
            // 如果 volumeCode 为空，使用第一个卷别
            let targetVolume = null
            if (targetVolumeCode) {
              targetVolume = paperData.manifest.volumes.find(v => 
                (v.volumeCode || v.volume_code || '') === targetVolumeCode
              )
            }
            
            // 如果没找到，使用第一个卷别
            if (!targetVolume) {
              const sortedVolumes = [...paperData.manifest.volumes].sort((a, b) => 
                (a.volumeOrder || 0) - (b.volumeOrder || 0)
              )
              targetVolume = sortedVolumes[0]
              console.log('📦 [loadSectionsAndQuestions] volumeCode 匹配失败，使用第一个卷别')
            }
            
            if (targetVolume) {
              targetVolumeId = targetVolume.id
              // 保存到 localStorage 以便后续使用
              localStorage.setItem('currentVolumeId', String(targetVolumeId))
              console.log('📦 [loadSectionsAndQuestions] 找到目标卷别:', {
                volumeCode: targetVolumeCode,
                volumeId: targetVolumeId,
                volumeName: targetVolume.volumeName
              })
            } else {
              console.warn('⚠️ [loadSectionsAndQuestions] 未找到任何卷别')
            }
          }
        }
        
        // 优先从 manifest 获取大题信息（确保数据完整）
        const paperData = await ipcRenderer.invoke('paper:getPaperData', this.paperId)
        let allSections = []
        
        if (paperData && paperData.manifest && paperData.manifest.sections && paperData.manifest.sections.length > 0) {
          // 从 manifest 获取大题
          allSections = paperData.manifest.sections.map(s => ({
            id: s.id || s.section_id || s.sectionId,
            section_name: s.sectionName || s.section_name || '大题',
            volume_id: s.volumeId || s.volume_id,
            volume_code: s.volumeCode || s.volume_code || '',
            section_order: s.sectionOrder || s.section_order || 0,
            audio_play_count: s.audioPlayCount || s.audio_play_count || 1,
            answer_time: s.answerTime || s.answer_time || 5
          }))
          console.log('📦 [loadSectionsAndQuestions] 从 manifest 获取大题:', allSections.length, '个')
        } else {
          // 降级：从数据库获取大题
          allSections = await ipcRenderer.invoke('paper:getSections', this.paperId)
          console.log('📦 [loadSectionsAndQuestions] 从数据库获取大题:', allSections.length, '个')
        }
        
        // 打印每个大题的 volume_code 和 volume_id，用于调试
        allSections.forEach((s, i) => {
          console.log(`📦 [loadSectionsAndQuestions] 大题 ${i}: id=${s.id}, volume_id=${s.volume_id}, volume_code="${s.volume_code}", section_name="${s.section_name}"`)
        })
        
        // 处理所有文本字段，确保编码正确
        const processedSections = processTextFields(allSections)
        
        // 筛选当前卷别下的大题（按顺序排序）
        // 必须使用 volume_id 匹配，因为 volume_code 可能为空
        console.log('📦 [loadSectionsAndQuestions] 目标卷别代码:', `"${targetVolumeCode}"`, '目标卷别ID:', targetVolumeId)
        
        if (!targetVolumeId) {
          console.error('❌ [loadSectionsAndQuestions] 无法确定目标卷别ID，无法筛选大题')
          this.currentVolumeSections = []
        } else {
          this.currentVolumeSections = processedSections
            .filter(section => {
              // 使用 volume_id 匹配（这是唯一可靠的方式）
              const match = section.volume_id === targetVolumeId
              console.log(`📦 [loadSectionsAndQuestions] 筛选大题: section_name="${section.section_name}", volume_id=${section.volume_id}, 目标=${targetVolumeId}, 匹配=${match}`)
              return match
            })
            .sort((a, b) => (a.section_order || 0) - (b.section_order || 0))
        }
        
        console.log('📦 [loadSectionsAndQuestions] 当前卷别下的大题:', this.currentVolumeSections.length)
        
        // 获取每个大题下的题目序号
        for (const section of this.currentVolumeSections) {
          try {
            const questions = await ipcRenderer.invoke('paper:getQuestions', this.paperId, section.id)
            console.log(`📦 [loadSectionsAndQuestions] 大题 ${section.id} 下的题目:`, questions.length, '个')
            
            // 处理题目数据的文本字段
            const processedQuestions = processTextFields(questions)
            
            // 生成题目序号（1, 2, 3, ...），而不是使用数据库中的 question_sort
            // 右侧显示的应该是连续的序号，表示这个大题有多少道题
            const questionNumbers = processedQuestions
              .sort((a, b) => (a.question_sort || a.sort_order || 0) - (b.question_sort || b.sort_order || 0))
              .map((q, index) => index + 1) // 使用连续序号 1, 2, 3, ...
            
            this.$set(this.sectionQuestionsMap, section.id, questionNumbers)
            console.log(`📦 [loadSectionsAndQuestions] 大题 ${section.id} 题号列表:`, questionNumbers)
          } catch (error) {
            console.error(`获取大题 ${section.id} 的题目失败:`, error)
            this.$set(this.sectionQuestionsMap, section.id, [])
          }
        }
        
        console.log('✓ [loadSectionsAndQuestions] 大题和题目数据加载完成')
        
        // 加载完成后，开始自动播放第一个大题的音频
        if (this.currentVolumeSections.length > 0) {
          this.currentSectionIndex = 0
          this.loadAndPlaySectionAudio(this.currentVolumeSections[0])
        }
      } catch (error) {
        console.error('❌ [loadSectionsAndQuestions] 加载大题和题目数据失败:', error)
      }
    },

    async loadAndPlaySectionAudio(section) {
      try {
        console.log(`📦 [loadAndPlaySectionAudio] 开始加载大题音频，sectionId=${section.id}, sectionName=${section.section_name}`)
        
        // 获取大题的题目音频播放次数和答题时间设置（用于后续题目播放）
        this.questionAudioPlayCount = section.audio_play_count || section.audioPlayCount || 1
        this.answerTime = section.answer_time || section.answerTime || 5
        console.log(`📦 [loadAndPlaySectionAudio] 题目音频播放次数: ${this.questionAudioPlayCount}, 答题时间: ${this.answerTime}秒`)
        
        // 获取试卷包数据
        const paperData = await ipcRenderer.invoke('paper:getPaperData', this.paperId)
        if (!paperData || !paperData.manifest) {
          console.warn('⚠️ [loadAndPlaySectionAudio] 无法加载试卷数据')
          // 如果没有音频，直接跳到下一个
          this.playNextSectionAudio()
          return
        }
        
        const mediaDir = paperData.mediaDir || ''
        const manifest = paperData.manifest
        
        // 从 manifest.sections 中查找对应的大题
        // 优先使用 section.id 匹配，如果匹配不到，尝试使用 section_name 匹配
        let matchedSection = null
        if (manifest.sections && Array.isArray(manifest.sections)) {
          matchedSection = manifest.sections.find(s => {
            // 尝试多种匹配方式
            return s.id === section.id || 
                   s.section_id === section.id ||
                   String(s.sectionName || s.section_name || '') === String(section.section_name || section.sectionName || '')
          })
        }
        
        if (!matchedSection) {
          console.warn(`⚠️ [loadAndPlaySectionAudio] 在 manifest 中找不到对应的大题，sectionId=${section.id}, sectionName=${section.section_name}`)
          // 如果没有匹配的大题，等待1.5秒后跳到下一个
          setTimeout(() => {
            this.playNextSectionAudio()
          }, 1500)
          return
        }
        
        // 从 manifest 中获取大题音频路径
        // 优先使用 instructionAudioPath，如果没有则使用 instructionAudioUrl
        const audioPath = matchedSection.instructionAudioPath || matchedSection.instruction_audio_path || ''
        
        if (!audioPath) {
          console.warn(`⚠️ [loadAndPlaySectionAudio] 大题 ${section.id} 没有音频文件路径，跳到下一个`)
          // 如果没有音频，等待1.5秒后跳到下一个
          setTimeout(() => {
            this.playNextSectionAudio()
          }, 1500)
          return
        }
        
        console.log(`📦 [loadAndPlaySectionAudio] 开始加载大题音频`)
        
        // 只处理本地路径，忽略远程URL
        let audioFilePath = null
        
        // 如果路径以 media/ 开头，说明是完整的相对路径
        if (audioPath.startsWith('media/')) {
          // 尝试从 mediaDir 解析
          const relativePath = audioPath.replace(/^media\//, '')
          audioFilePath = path.join(mediaDir, relativePath)
          if (!fs.existsSync(audioFilePath)) {
            // 如果不存在，尝试从 sections 目录查找
            const fileName = path.basename(audioPath)
            const sectionInstructionDir = path.join(mediaDir, 'sections')
            if (fs.existsSync(sectionInstructionDir)) {
              const sectionInstructionFilePath = path.join(sectionInstructionDir, fileName)
              if (fs.existsSync(sectionInstructionFilePath)) {
                audioFilePath = sectionInstructionFilePath
                console.log(`📦 [loadAndPlaySectionAudio] 从 sections 目录找到音频文件`)
              }
            }
          } else {
            console.log(`📦 [loadAndPlaySectionAudio] 从 media/ 路径找到音频文件`)
          }
        } else if (audioPath.startsWith('sections/')) {
          // 如果路径以 sections/ 开头，直接从 sections 目录查找
          const fileName = path.basename(audioPath)
          const sectionInstructionDir = path.join(mediaDir, 'sections')
          if (fs.existsSync(sectionInstructionDir)) {
            const sectionInstructionFilePath = path.join(sectionInstructionDir, fileName)
            if (fs.existsSync(sectionInstructionFilePath)) {
              audioFilePath = sectionInstructionFilePath
              console.log(`📦 [loadAndPlaySectionAudio] 从 sections/ 路径找到音频文件`)
            }
          }
        } else {
          // 如果只是文件名，从 sections/ 目录查找
          const sectionInstructionDir = path.join(mediaDir, 'sections')
          const fileName = path.basename(audioPath)
          if (fs.existsSync(sectionInstructionDir)) {
            const sectionInstructionFilePath = path.join(sectionInstructionDir, fileName)
            if (fs.existsSync(sectionInstructionFilePath)) {
              audioFilePath = sectionInstructionFilePath
              console.log(`📦 [loadAndPlaySectionAudio] 从文件名找到音频文件`)
            }
          }
        }
        
        // 加载音频文件的辅助函数（完全参考 Broadcast.vue 和 Notes.vue 的实现）
        const loadAudioFile = (filePath) => {
          try {
            if (!fs.existsSync(filePath)) {
              console.warn(`⚠️ [loadAndPlaySectionAudio] 音频文件不存在: ${filePath}`)
              return false
            }
            
            // 获取文件大小用于日志
            const stats = fs.statSync(filePath)
            const fileSizeMB = (stats.size / 1024 / 1024).toFixed(2)
            console.log(`📦 [loadAndPlaySectionAudio] 音频文件大小: ${fileSizeMB}MB`)
            
            // 先尝试读取文件并转成 base64 data URL
            try {
              const fileBuffer = fs.readFileSync(filePath)
              const ext = path.extname(filePath).toLowerCase()
              let mimeType = 'audio/mpeg'
              if (ext === '.wav') mimeType = 'audio/wav'
              else if (ext === '.m4a' || ext === '.aac') mimeType = 'audio/mp4'
              else if (ext === '.ogg') mimeType = 'audio/ogg'
              
              const base64 = fileBuffer.toString('base64')
              const dataUrl = `data:${mimeType};base64,${base64}`
              this.currentSectionAudioPath = dataUrl
              return true
            } catch (error) {
              console.warn(`⚠️ [loadAndPlaySectionAudio] 读取文件失败，尝试 file:// URL: ${error.message}`)
              // 降级方案：使用 file:// URL
              this.currentSectionAudioPath = pathToFileURL(filePath).href
              console.log(`✓ [loadAndPlaySectionAudio] 大题音频加载成功（file:// URL）`)
              return true
            }
          } catch (error) {
            console.error(`❌ [loadAndPlaySectionAudio] 加载音频文件失败: ${error.message}`)
            return false
          }
        }
        
        // 扫描目录查找音频文件的辅助函数
        const scanDirectoryForAudio = () => {
          let foundAudioPath = null
          const sectionInstructionDir = path.join(mediaDir, 'sections')
          if (fs.existsSync(sectionInstructionDir)) {
            const files = fs.readdirSync(sectionInstructionDir)
            const audioFile = files.find(file => /\.(mp3|wav|m4a|aac|ogg)$/i.test(file))
            if (audioFile) {
              foundAudioPath = path.join(sectionInstructionDir, audioFile)
            }
          }
          return foundAudioPath
        }
        
        // 如果找到了音频文件路径，直接加载
        if (audioFilePath && fs.existsSync(audioFilePath)) {
          if (loadAudioFile(audioFilePath)) {
            // 加载成功后，等待音频就绪后播放
            this.$nextTick(() => {
              this.playSectionAudio()
            })
          } else {
            // 加载失败，跳到下一个
            setTimeout(() => {
              this.playNextSectionAudio()
            }, 1500)
          }
        } else {
          // 文件不存在或路径不存在，尝试扫描目录
          const foundAudioPath = scanDirectoryForAudio()
          if (foundAudioPath) {
            if (loadAudioFile(foundAudioPath)) {
              this.$nextTick(() => {
                this.playSectionAudio()
              })
            } else {
              setTimeout(() => {
                this.playNextSectionAudio()
              }, 1500)
            }
          } else {
            console.warn(`⚠️ [loadAndPlaySectionAudio] 大题音频不存在，跳到下一个`)
            setTimeout(() => {
              this.playNextSectionAudio()
            }, 1500)
          }
        }
      } catch (error) {
        console.error('❌ [loadAndPlaySectionAudio] 加载大题音频失败:', error)
        // 出错后也跳到下一个
        setTimeout(() => {
          this.playNextSectionAudio()
        }, 1500)
      }
    },

    playSectionAudio() {
      if (!this.currentSectionAudioPath) {
        console.warn('⚠️ [playSectionAudio] 没有大题音频，跳到下一个')
        setTimeout(() => {
          this.playNextSectionAudio()
        }, 1500)
        return
      }

      const audio = this.$refs.sectionAudioPlayer
      if (!audio) {
        console.warn('⚠️ [playSectionAudio] 音频元素不存在，跳到下一个')
        setTimeout(() => {
          this.playNextSectionAudio()
        }, 1500)
        return
      }

      // 等待音频加载完成的回调函数
      const onCanPlay = () => {
        console.log(`🎵 [playSectionAudio] 大题音频可以播放，开始播放，sectionIndex=${this.currentSectionIndex}`)

        // 获取音频时长（仅用于日志和超时备用）
        const audioDuration = audio.duration || 0
        console.log(`📊 [playSectionAudio] 音频时长: ${audioDuration}秒`)
        
        // 清除可能存在的其他定时器
        if (this.sectionAudioTimer) {
          clearTimeout(this.sectionAudioTimer)
          this.sectionAudioTimer = null
        }
        
        // 设置超时备用定时器：如果音频播放时间超过预期时长+60秒还没结束，强制跳到下一个
        // 正常情况下，onSectionAudioEnded 会处理跳转，这里只是备用机制
        const timeoutDuration = audioDuration > 0 ? (audioDuration * 1000) + 60000 : 120000 // 默认2分钟超时
        console.log(`📊 [playSectionAudio] 设置超时备用定时器: ${(timeoutDuration / 1000).toFixed(2)}秒后强制跳转`)
        this.sectionAudioTimer = setTimeout(() => {
          // 检查是否已经跳转（避免重复调用）
          if (this.showQuestions || this.loading) {
            console.log('⚠️ [超时备用] 题目已加载，跳过')
            return
          }
          console.warn('⚠️ [超时备用] 音频播放超时，强制跳到下一个')
          this.playNextSectionAudio()
        }, timeoutDuration)
        
        // 开始播放音频
        audio.volume = this.volume / 100
        console.log(`📊 [playSectionAudio] 调用 audio.play()，volume=${audio.volume}`)
        audio.play().then(() => {
          console.log(`✓ [playSectionAudio] 大题音频开始播放成功，sectionIndex=${this.currentSectionIndex}`)
          this.sectionAudioPlaying = true
          // 初始化音频可视化
          this.initAudioVisualization(audio)
        }).catch(error => {
          console.error('❌ [playSectionAudio] 播放大题音频失败:', error)
          this.sectionAudioPlaying = false
          // 播放失败也等待1.5秒后跳到下一个
          if (this.sectionAudioTimer) {
            clearTimeout(this.sectionAudioTimer)
            this.sectionAudioTimer = null
          }
          this.sectionAudioTimer = setTimeout(() => {
            this.playNextSectionAudio()
          }, 1500)
        })
      }

      // 检查音频是否已就绪
      if (audio.readyState >= 2) {
        // 音频已就绪，直接播放
        console.log(`✓ [playSectionAudio] 音频已就绪，直接播放`)
        onCanPlay()
      } else {
        // 等待音频加载完成
        console.log(`⏳ [playSectionAudio] 音频未就绪，等待 canplay 事件`)
        // 注意：error 事件已在模板中通过 @error="onSectionAudioError" 处理，不需要重复监听
        audio.addEventListener('canplay', onCanPlay, { once: true })
        
        // 设置超时（10秒），如果音频一直无法加载，则跳到下一个
        setTimeout(() => {
          if (audio.readyState < 2 && !this.sectionAudioPlaying) {
            console.error('❌ [playSectionAudio] 大题音频加载超时（10秒），readyState=' + audio.readyState)
            // 清除可能存在的其他定时器
            if (this.sectionAudioTimer) {
              clearTimeout(this.sectionAudioTimer)
              this.sectionAudioTimer = null
            }
            this.sectionAudioTimer = setTimeout(() => {
              this.playNextSectionAudio()
            }, 1500)
          }
        }, 10000)
      }
    },

    playNextSectionAudio() {
      // 防止重复调用
      if (this.showQuestions || this.loading) {
        console.log('⚠️ [playNextSectionAudio] 题目已加载或正在加载，跳过')
        return
      }
      
      // 清除当前定时器
      if (this.sectionAudioTimer) {
        clearTimeout(this.sectionAudioTimer)
        this.sectionAudioTimer = null
      }
      
      // 停止当前音频（先清空路径，再清空 src，避免触发错误事件）
      this.currentSectionAudioPath = '' // 先清空路径，这样 error 事件会被忽略
      const audio = this.$refs.sectionAudioPlayer
      if (audio) {
        audio.pause()
        audio.src = ''
      }
      this.sectionAudioPlaying = false
      // 停止音频可视化
      this.stopAudioVisualization()
      
      // 移动到下一个大题
      this.currentSectionIndex++
      
      if (this.currentSectionIndex < this.currentVolumeSections.length) {
        // 还有下一个大题，加载并播放
        const nextSection = this.currentVolumeSections[this.currentSectionIndex]
        console.log(`📦 [playNextSectionAudio] 播放下一个大题音频，sectionIndex=${this.currentSectionIndex}, sectionId=${nextSection.id}`)
        this.loadAndPlaySectionAudio(nextSection)
      } else {
        // 所有大题音频播放完成，等待1.5秒后加载题目
        console.log('✓ [playNextSectionAudio] 所有大题音频播放完成，等待1.5秒后加载题目')
        
        // 重置 currentSectionIndex 为 0，准备加载第一个大题的题目
        this.currentSectionIndex = 0
        console.log(`📦 [playNextSectionAudio] 重置 currentSectionIndex 为 0，准备加载第一个大题的题目`)
        
        // 使用一次性定时器，防止重复调用
        if (!this.questionsLoadTimer) {
          this.questionsLoadTimer = setTimeout(() => {
            this.questionsLoadTimer = null
            this.loadAllQuestions()
          }, 1500)
        }
      }
    },

    async loadAllQuestions() {
      // 防止重复调用
      if (this.showQuestions || this.loading) {
        console.log('⚠️ [loadAllQuestions] 题目已加载或正在加载，跳过重复调用')
        return
      }
      
      try {
        console.log('📦 [loadAllQuestions] 开始加载当前大题的题目')
        this.loading = true
        
        // 只加载当前大题的题目（不是所有大题）
        const currentSection = this.currentVolumeSections[this.currentSectionIndex]
        if (!currentSection) {
          console.error('❌ [loadAllQuestions] 未找到当前大题')
          this.loading = false
          return
        }
        
        console.log(`📦 [loadAllQuestions] 当前大题: ${currentSection.section_name || currentSection.sectionName} (索引: ${this.currentSectionIndex})`)
        
        const allQuestionsList = []
        
        try {
          const questions = await ipcRenderer.invoke('paper:getQuestions', this.paperId, currentSection.id)
          console.log(`📦 [loadAllQuestions] 大题 ${currentSection.id} 的题目:`, questions.length, '个')
          
          // 处理题目数据的文本字段
          const processedQuestions = processTextFields(questions)
          
          // 按 question_sort 或 sort_order 排序
          const sortedQuestions = processedQuestions.sort((a, b) => {
            return (a.question_sort || a.sort_order || 0) - (b.question_sort || b.sort_order || 0)
          })
          
          // 添加到列表
          allQuestionsList.push(...sortedQuestions)
        } catch (error) {
          console.error(`获取大题 ${currentSection.id} 的题目失败:`, error)
        }
        
        console.log(`✓ [loadAllQuestions] 共加载 ${allQuestionsList.length} 个题目`)
        
        if (allQuestionsList.length === 0) {
          console.warn('⚠️ [loadAllQuestions] 没有题目可显示')
          this.loading = false
          return
        }
        
        // 处理所有题目，确保文本字段正确
        this.allQuestions = allQuestionsList.map(q => this.processQuestion(q))
        
        console.log(`✓ [loadAllQuestions] 题目处理完成，准备显示`)
        
        // 重置题目状态
        this.expiredQuestionIndexes = []
        this.answeredQuestions = {}
        
        // 加载第一题
        if (this.allQuestions.length > 0) {
          this.currentQuestionIndex = 0
          this.loadQuestion(this.allQuestions[0])
          console.log(`✓ [loadAllQuestions] 第一题加载完成，currentSectionName:`, this.currentSectionName)
        }
        
        // 使用 $nextTick 确保 DOM 更新后再显示
        this.$nextTick(() => {
          this.showQuestions = true
          this.loading = false
          console.log('✓ [loadAllQuestions] 题目显示完成，showQuestions=true, currentQuestion:', this.currentQuestion ? '已设置' : '未设置')
          
          // 显示题目后，开始播放第一题的音频
          this.$nextTick(() => {
            this.playCurrentQuestionAudio()
          })
        })
      } catch (error) {
        console.error('❌ [loadAllQuestions] 加载题目失败:', error)
        this.loading = false
      }
    },

    processQuestion(question) {
      // 处理题目数据，确保所有文本字段都是字符串
      const processed = {
        ...question,
        title: String(question.title || ''),
        text: String(question.text || ''),
        answer: String(question.answer || ''),
        analyzes: String(question.analyzes || ''),
        explanation_text: String(question.explanation_text || '')
      }
      
      // 处理选项
      if (question.answers && Array.isArray(question.answers)) {
        processed.answers = question.answers.map((opt, index) => ({
          ...opt,
          label: String(opt.label || String.fromCharCode(65 + index)),
          text: String(opt.text || '')
        }))
      } else if (question.options && Array.isArray(question.options)) {
        processed.answers = question.options.map((opt, index) => ({
          ...opt,
          label: String(opt.label || String.fromCharCode(65 + index)),
          text: String(opt.text || '')
        }))
      }
      
      return processed
    },

    loadQuestion(questionData) {
      try {
        if (!questionData) {
          console.error('❌ [loadQuestion] 题目数据为空')
          this.currentQuestion = null
          this.currentQuestionOptions = []
          return
        }
        
        console.log(`📦 [loadQuestion] 开始加载题目 ${this.currentQuestionIndex + 1}，原始数据:`, questionData)
        
        // 处理题目标题和文本
        const questionTitle = String(questionData.title || '')
        const questionText = String(questionData.text || '')
        
        this.currentQuestion = {
          ...questionData,
          title: questionTitle,
          text: questionText
        }
        
        // 处理选项
        this.currentQuestionOptions = []
        if (questionData.answers && Array.isArray(questionData.answers) && questionData.answers.length > 0) {
          this.currentQuestionOptions = questionData.answers.map((opt, index) => ({
            ...opt,
            label: String(opt.label || String.fromCharCode(65 + index)),
            text: String(opt.text || opt.content || opt.option_text || '')
          }))
        } else if (questionData.options && Array.isArray(questionData.options) && questionData.options.length > 0) {
          this.currentQuestionOptions = questionData.options.map((opt, index) => ({
            ...opt,
            label: String(opt.label || String.fromCharCode(65 + index)),
            text: String(opt.text || opt.content || opt.option_text || '')
          }))
        }
        
        console.log(`✓ [loadQuestion] 题目加载完成:`, {
          title: this.currentQuestion.title,
          text: this.currentQuestion.text,
          optionsCount: this.currentQuestionOptions.length,
          hasQuestion: !!this.currentQuestion
        })
      } catch (error) {
        console.error('❌ [loadQuestion] 加载题目失败:', error)
        this.currentQuestion = null
        this.currentQuestionOptions = []
      }
    },

    formatQuestionTitle(title) {
      const text = String(title || '')
      return text.replace(/\n/g, '<br>')
    },

    formatQuestionText(text) {
      const textStr = String(text || '')
      return textStr.replace(/\n/g, '<br>')
    },

    formatOptionText(text) {
      const optionText = String(text || '')
      return optionText.replace(/\n/g, '<br>')
    },

    goToPreviousQuestion() {
      if (this.currentQuestionIndex > 0) {
        this.currentQuestionIndex--
        this.loadQuestion(this.allQuestions[this.currentQuestionIndex])
        // 滚动到顶部
        this.$nextTick(() => {
          const contentWrapper = document.querySelector('.question-content-wrapper')
          if (contentWrapper) {
            contentWrapper.scrollTop = 0
          }
        })
      }
    },

    goToNextQuestion() {
      if (this.currentQuestionIndex < this.allQuestions.length - 1) {
        this.currentQuestionIndex++
        this.loadQuestion(this.allQuestions[this.currentQuestionIndex])
        // 滚动到顶部
        this.$nextTick(() => {
          const contentWrapper = document.querySelector('.question-content-wrapper')
          if (contentWrapper) {
            contentWrapper.scrollTop = 0
          }
        })
      }
    },

    getCurrentSectionName() {
      // 根据当前题目找到对应的大题名称
      if (!this.currentQuestion || !this.currentVolumeSections.length) {
        return '题目'
      }
      
      // 尝试从题目数据中获取 sectionId 或 section_id
      const questionSectionId = this.currentQuestion.sectionId || this.currentQuestion.section_id
      if (questionSectionId) {
        const section = this.currentVolumeSections.find(s => s.id === questionSectionId)
        if (section) {
          return section.section_name || section.sectionName || '题目'
        }
      }
      
      // 如果找不到，返回第一个大题的名称
      return this.currentVolumeSections[0]?.section_name || this.currentVolumeSections[0]?.sectionName || '题目'
    },

    onSectionAudioEnded() {
      // 忽略因为清空 src 导致的 ended 事件
      if (!this.currentSectionAudioPath) {
        console.log('⚠️ [onSectionAudioEnded] 忽略空音频路径的结束事件')
        return
      }
      
      // 获取音频实际播放信息
      const audio = this.$refs.sectionAudioPlayer
      const currentTime = audio ? audio.currentTime : 0
      const duration = audio ? audio.duration : 0
      
      console.log(`✓ [onSectionAudioEnded] 大题音频播放完成，sectionIndex=${this.currentSectionIndex}`)
      console.log(`📊 [onSectionAudioEnded] 实际播放时间: ${currentTime.toFixed(2)}秒, 总时长: ${duration.toFixed(2)}秒`)
      
      // 检查是否提前结束（实际播放时间远小于总时长）
      if (duration > 0 && currentTime < duration * 0.9) {
        console.warn(`⚠️ [onSectionAudioEnded] 警告：音频可能提前结束！实际=${currentTime.toFixed(2)}秒, 预期=${duration.toFixed(2)}秒`)
      }
      
      this.sectionAudioPlaying = false
      
      // 清除之前设置的定时器
      if (this.sectionAudioTimer) {
        clearTimeout(this.sectionAudioTimer)
        this.sectionAudioTimer = null
        console.log('✓ [onSectionAudioEnded] 已清除定时器')
      }
      
      // 大题音频只播放1次，播放完成后立即跳到下一个大题或加载题目
      console.log('✓ [onSectionAudioEnded] 大题音频播放完成，立即继续...')
      this.playNextSectionAudio()
    },

    onSectionAudioLoaded() {
      const audio = this.$refs.sectionAudioPlayer
      if (audio && audio.duration) {
        console.log(`📦 [onSectionAudioLoaded] 大题音频加载完成，时长: ${audio.duration.toFixed(2)} 秒`)
      }
    },

    onSectionAudioCanPlay() {
      console.log('📦 [onSectionAudioCanPlay] 大题音频可以播放')
    },

    onSectionAudioError(error) {
      // 忽略因为清空 src 导致的 error 事件
      if (!this.currentSectionAudioPath) {
        console.log('⚠️ [onSectionAudioError] 忽略空音频路径的错误事件')
        return
      }
      
      // 如果题目已经显示或正在加载，忽略错误
      if (this.showQuestions || this.loading || this.questionsLoadTimer) {
        console.log('⚠️ [onSectionAudioError] 题目已加载或正在加载，忽略错误事件')
        return
      }
      
      console.error('❌ [onSectionAudioError] 大题音频播放错误:', error)
      this.sectionAudioPlaying = false
      // 清除可能存在的其他定时器
      if (this.sectionAudioTimer) {
        clearTimeout(this.sectionAudioTimer)
        this.sectionAudioTimer = null
      }
      // 播放错误也等待1.5秒后跳到下一个
      this.sectionAudioTimer = setTimeout(() => {
        this.playNextSectionAudio()
      }, 1500)
    },

    // ==================== 题目音频播放和答题计时相关方法 ====================
    
    /**
     * 播放当前题目的音频
     */
    async playCurrentQuestionAudio() {
      // 防重复调用：如果正在执行播放逻辑，直接返回
      if (this.isPlayingQuestionAudio) {
        console.warn(`⚠️ [playCurrentQuestionAudio] 正在执行播放逻辑，跳过重复调用，当前题目索引=${this.currentQuestionIndex + 1}`)
        return
      }
      
      const question = this.allQuestions[this.currentQuestionIndex]
      if (!question) {
        console.warn(`⚠️ [playCurrentQuestionAudio] 当前题目不存在，索引=${this.currentQuestionIndex}`)
        this.startAnswerCountdown()
        return
      }
      
      // 设置标志，防止重复调用
      this.isPlayingQuestionAudio = true
      
      console.log(`🎵 [playCurrentQuestionAudio] 开始处理题目 ${this.currentQuestionIndex + 1}, questionId=${question.id}`)
      console.log(`📦 [playCurrentQuestionAudio] 题目数据:`, {
        id: question.id,
        title: question.title,
        hasMedia: !!question.media,
        mediaCount: question.media ? question.media.length : 0,
        mediaTypes: question.media ? question.media.map(m => m.mediaType || m.media_type) : []
      })
      
      // 先停止并清理之前的音频（确保切换题目时使用正确的音频）
      const audio = this.$refs.questionAudioPlayer
      if (audio) {
        audio.pause()
        audio.src = ''
      }
      this.questionAudioPath = ''
      this.questionAudioPlaying = false
      // 停止之前的音频可视化
      this.stopAudioVisualization()
      
      // 重置播放计数
      this.questionAudioPlayedCount = 0
      
      // 获取题目音频文件路径（根据当前题目的ID）
      const audioFilePath = await this.getQuestionAudioFilePath(question)
      
      if (!audioFilePath) {
        console.warn(`⚠️ [playCurrentQuestionAudio] 题目 ${this.currentQuestionIndex + 1} (ID: ${question.id}) 没有找到音频文件，直接开始答题计时`)
        // 重置标志
        this.isPlayingQuestionAudio = false
        // 没有音频，直接开始答题计时
        this.startAnswerCountdown()
        return
      }
      
      console.log(`🎵 [playCurrentQuestionAudio] 找到音频文件: ${audioFilePath}`)
      
      // 加载音频文件（转成 base64 或 file:// URL）
      const loaded = this.loadQuestionAudioFile(audioFilePath)
      if (!loaded) {
        console.warn(`⚠️ [playCurrentQuestionAudio] 音频文件加载失败，直接开始答题计时`)
        // 重置标志
        this.isPlayingQuestionAudio = false
        this.startAnswerCountdown()
        return
      }
      
      console.log(`✓ [playCurrentQuestionAudio] 音频加载成功，questionAudioPath=${this.questionAudioPath ? '已设置' : '未设置'}`)
      
      // 确保 questionAudioPath 已设置后再播放
      if (!this.questionAudioPath) {
        console.error(`❌ [playCurrentQuestionAudio] questionAudioPath 未设置，无法播放`)
        // 重置标志
        this.isPlayingQuestionAudio = false
        this.startAnswerCountdown()
        return
      }
      
      // 直接调用播放（不使用 $nextTick，因为 loadQuestionAudioFile 是同步的）
      // 使用 setTimeout 确保 DOM 更新完成
      setTimeout(() => {
        try {
          console.log(`✓ [playCurrentQuestionAudio] setTimeout 执行，准备调用 playQuestionAudio，questionAudioPath=${this.questionAudioPath ? '已设置' : '未设置'}`)
          if (!this.questionAudioPath) {
            console.error(`❌ [playCurrentQuestionAudio] setTimeout 中 questionAudioPath 为空，无法播放`)
            // 重置标志
            this.isPlayingQuestionAudio = false
            this.startAnswerCountdown()
            return
          }
          console.log(`✓ [playCurrentQuestionAudio] 即将调用 this.playQuestionAudio()`)
          this.playQuestionAudio()
          console.log(`✓ [playCurrentQuestionAudio] this.playQuestionAudio() 调用完成`)
        } catch (error) {
          console.error(`❌ [playCurrentQuestionAudio] setTimeout 中发生错误:`, error)
          this.isPlayingQuestionAudio = false
          this.startAnswerCountdown()
        }
      }, 100)
    },
    
    /**
     * 加载题目音频文件（完全参考 Broadcast.vue 的实现）
     */
    loadQuestionAudioFile(filePath) {
      try {
        if (!fs.existsSync(filePath)) {
          console.warn(`⚠️ [loadQuestionAudioFile] 音频文件不存在: ${filePath}`)
          return false
        }
        
        // 获取文件大小
        const stats = fs.statSync(filePath)
        const fileSizeMB = (stats.size / 1024 / 1024).toFixed(2)
        console.log(`📦 [loadQuestionAudioFile] 音频文件大小: ${fileSizeMB}MB`)
        
        // 先尝试读取文件并转成 base64 data URL
        try {
          const fileBuffer = fs.readFileSync(filePath)
          const ext = path.extname(filePath).toLowerCase()
          let mimeType = 'audio/mpeg'
          if (ext === '.wav') mimeType = 'audio/wav'
          else if (ext === '.m4a' || ext === '.aac') mimeType = 'audio/mp4'
          else if (ext === '.ogg') mimeType = 'audio/ogg'
          
          const base64 = fileBuffer.toString('base64')
          const dataUrl = `data:${mimeType};base64,${base64}`
          this.questionAudioPath = dataUrl
          console.log(`✓ [loadQuestionAudioFile] 题目音频路径已设置，长度=${dataUrl.length}，前100字符=${dataUrl.substring(0, 100)}...`)
          return true
        } catch (error) {
          console.warn(`⚠️ [loadQuestionAudioFile] 读取文件失败，尝试 file:// URL: ${error.message}`)
          // 降级方案：使用 file:// URL
          this.questionAudioPath = pathToFileURL(filePath).href
          console.log(`✓ [loadQuestionAudioFile] 题目音频加载成功（file:// URL），路径=${this.questionAudioPath}`)
          return true
        }
      } catch (error) {
        console.error(`❌ [loadQuestionAudioFile] 加载音频文件失败: ${error.message}`)
        return false
      }
    },
    
    /**
     * 获取题目音频文件路径（返回本地文件系统路径，不是 URL）
     * 题目音频来源优先级：
     * 1. question.media 字段（题目自身的音频）
     * 2. question.answers[0].media 字段（第一个选项的音频，用作题目音频）
     * 音频路径格式如 "options/xxx.mp3"，实际文件在 mediaDir/options/ 目录
     */
    async getQuestionAudioFilePath(question) {
      try {
        const questionId = question.id || question.question_id
        
        // 快速检查：如果 question.media 为空或没有 mediaType=4 的音频，直接返回 null
        if (!question.media || !Array.isArray(question.media) || question.media.length === 0) {
          console.log(`⚠️ [getQuestionAudioFilePath] 题目 ${questionId} 没有 media 数据，跳过`)
          return null
        }
        
        const audioMedia = question.media.find(m => (m.mediaType || m.media_type) === 4)
        if (!audioMedia) {
          console.log(`⚠️ [getQuestionAudioFilePath] 题目 ${questionId} 没有 mediaType=4 的音频，跳过`)
          return null
        }
        
        // 获取试卷包数据
        const paperData = await ipcRenderer.invoke('paper:getPaperData', this.paperId)
        if (!paperData || !paperData.mediaDir) {
          console.log('⚠️ [getQuestionAudioFilePath] 无法获取媒体目录')
          return null
        }
        
        const mediaDir = paperData.mediaDir
        console.log(`📦 [getQuestionAudioFilePath] 检查题目音频, questionId=${questionId}`)
        
        // 辅助函数：根据路径查找并返回本地文件路径
        const findAudioFile = (audioPath) => {
          if (!audioPath) {
            console.log(`⚠️ [findAudioFile] 音频路径为空`)
            return null
          }
          
          // 构建完整路径
          let fullPath = path.join(mediaDir, audioPath)
          console.log(`🔍 [findAudioFile] 尝试路径1: ${fullPath}`)
          if (fs.existsSync(fullPath)) {
            console.log(`✓ [findAudioFile] 找到音频文件: ${fullPath}`)
            return fullPath
          }
          
          // 如果路径不存在，尝试只用文件名在 questions/q_{questionId}/ 目录下查找
          const fileName = path.basename(audioPath)
          const questionMediaDir = path.join(mediaDir, 'questions', `q_${questionId}`)
          console.log(`🔍 [findAudioFile] 尝试路径2: ${path.join(questionMediaDir, fileName)}`)
          if (fs.existsSync(questionMediaDir)) {
            const fullPath2 = path.join(questionMediaDir, fileName)
            if (fs.existsSync(fullPath2)) {
              console.log(`✓ [findAudioFile] 在 questions 目录找到音频: ${fullPath2}`)
              return fullPath2
            }
          }
          
          // 如果路径不存在，尝试只用文件名在 options 目录下查找（兼容旧数据）
          const optionsDir = path.join(mediaDir, 'options')
          console.log(`🔍 [findAudioFile] 尝试路径3: ${path.join(optionsDir, fileName)}`)
          if (fs.existsSync(optionsDir)) {
            const fullPath3 = path.join(optionsDir, fileName)
            if (fs.existsSync(fullPath3)) {
              console.log(`✓ [findAudioFile] 在 options 目录找到音频: ${fullPath3}`)
              return fullPath3
            }
          }
          
          console.log(`❌ [findAudioFile] 所有路径都未找到音频文件`)
          return null
        }
        
        // 从 question.media 获取音频路径（已在开头验证 audioMedia 存在）
        const audioPath = audioMedia.mediaPath || audioMedia.media_path
        console.log(`📦 [getQuestionAudioFilePath] 题目 ${questionId} 音频路径: ${audioPath}`)
        const filePath = findAudioFile(audioPath)
        if (filePath) {
          console.log(`✓ [getQuestionAudioFilePath] 找到题目音频: ${filePath}`)
          return filePath
        }
        
        // 如果 findAudioFile 没找到，尝试扫描 questions/q_{questionId}/ 目录
        const questionMediaDir = path.join(mediaDir, 'questions', `q_${questionId}`)
        console.log(`🔍 [getQuestionAudioFilePath] 扫描目录1: ${questionMediaDir}`)
        if (fs.existsSync(questionMediaDir)) {
          try {
            const files = fs.readdirSync(questionMediaDir)
            console.log(`📦 [getQuestionAudioFilePath] questions 目录中的文件:`, files)
            const audioFile = files.find(f => f.endsWith('.mp3') || f.endsWith('.wav') || f.endsWith('.m4a') || f.endsWith('.aac'))
            if (audioFile) {
              const filePath = path.join(questionMediaDir, audioFile)
              console.log(`✓ [getQuestionAudioFilePath] 从 questions 目录扫描找到音频: ${filePath}`)
              return filePath
            } else {
              console.warn(`⚠️ [getQuestionAudioFilePath] questions 目录中没有找到音频文件`)
            }
          } catch (error) {
            console.warn(`⚠️ [getQuestionAudioFilePath] 扫描 questions 目录失败:`, error)
          }
        } else {
          console.warn(`⚠️ [getQuestionAudioFilePath] questions 目录不存在: ${questionMediaDir}`)
        }
        
        // 3. 尝试扫描 options/q_{questionId}/ 目录查找音频文件（兼容：题目音频可能在 options 目录）
        const questionOptionsDir = path.join(mediaDir, 'options', `q_${questionId}`)
        console.log(`🔍 [getQuestionAudioFilePath] 扫描目录2: ${questionOptionsDir}`)
        if (fs.existsSync(questionOptionsDir)) {
          try {
            const files = fs.readdirSync(questionOptionsDir)
            console.log(`📦 [getQuestionAudioFilePath] options 目录中的文件:`, files)
            // 查找第一个音频文件作为题目音频（通常题目音频是第一个）
            const audioFiles = files.filter(f => f.endsWith('.mp3') || f.endsWith('.wav') || f.endsWith('.m4a') || f.endsWith('.aac'))
            if (audioFiles.length > 0) {
              // 优先选择文件名中包含 "title" 或 "question" 的，否则选择第一个
              let audioFile = audioFiles.find(f => f.toLowerCase().includes('title') || f.toLowerCase().includes('question'))
              if (!audioFile) {
                audioFile = audioFiles[0] // 如果没有找到，使用第一个
              }
              const filePath = path.join(questionOptionsDir, audioFile)
              console.log(`✓ [getQuestionAudioFilePath] 从 options 目录扫描找到题目音频: ${filePath}`)
              return filePath
            } else {
              console.warn(`⚠️ [getQuestionAudioFilePath] options 目录中没有找到音频文件`)
            }
          } catch (error) {
            console.warn(`⚠️ [getQuestionAudioFilePath] 扫描 options 目录失败:`, error)
          }
        } else {
          console.warn(`⚠️ [getQuestionAudioFilePath] options 目录不存在: ${questionOptionsDir}`)
        }
        
        console.log(`❌ [getQuestionAudioFilePath] 题目 ${questionId} 没有找到音频`)
        return null
      } catch (error) {
        console.error('获取题目音频文件路径失败:', error)
        return null
      }
    },
    
    /**
     * 播放题目音频（完全参考 Broadcast.vue 的实现）
     */
    playQuestionAudio() {
      try {
        console.log(`🎵 [playQuestionAudio] 方法被调用`)
      } catch (e) {
        console.error(`❌ [playQuestionAudio] console.log 失败:`, e)
      }
      console.log(`🎵 [playQuestionAudio] 开始播放题目音频，questionAudioPath=${this.questionAudioPath ? '已设置' : '未设置'}`)
      const audio = this.$refs.questionAudioPlayer
      if (!audio) {
        console.warn('⚠️ [playQuestionAudio] 音频元素不存在')
        // 重置标志
        this.isPlayingQuestionAudio = false
        this.startAnswerCountdown()
        return
      }
      if (!this.questionAudioPath) {
        console.warn('⚠️ [playQuestionAudio] 音频路径不存在，questionAudioPath为空')
        // 重置标志
        this.isPlayingQuestionAudio = false
        this.startAnswerCountdown()
        return
      }
      
      // 等待音频加载完成的回调函数
      const onCanPlay = () => {
        console.log(`🎵 [playQuestionAudio] 题目音频可以播放，开始播放`)
        
        // 设置音量
        audio.volume = this.volume / 100
        console.log(`🎵 [playQuestionAudio] 音量设置: this.volume=${this.volume}, audio.volume=${audio.volume}, audio.muted=${audio.muted}`)
        
        // 开始播放音频
        audio.play().then(() => {
          console.log(`✓ [playQuestionAudio] 题目音频开始播放，第 ${this.questionAudioPlayedCount + 1}/${this.questionAudioPlayCount} 遍`)
          console.log(`✓ [playQuestionAudio] 音频时长: ${audio.duration} 秒, currentTime=${audio.currentTime}, paused=${audio.paused}`)
          this.questionAudioPlaying = true
          // 重置标志（播放成功后）
          this.isPlayingQuestionAudio = false
          // 初始化音频可视化
          this.initAudioVisualization(audio)
          
          // 备用机制：设置定时器，确保即使 ended 事件不触发也能继续
          // 时长 + 2秒缓冲
          if (audio.duration && audio.duration > 0) {
            const fallbackTimeout = (audio.duration + 2) * 1000
            console.log(`✓ [playQuestionAudio] 设置备用定时器: ${fallbackTimeout}ms`)
            if (this.questionAudioTimer) {
              clearTimeout(this.questionAudioTimer)
            }
            this.questionAudioTimer = setTimeout(() => {
              // 检查音频是否还在播放状态但 ended 事件没触发
              if (this.questionAudioPlaying && !audio.paused && audio.currentTime >= audio.duration - 0.5) {
                console.warn('⚠️ [playQuestionAudio] 备用定时器触发，手动调用 onQuestionAudioEnded')
                this.onQuestionAudioEnded()
              } else if (this.questionAudioPlaying) {
                console.warn('⚠️ [playQuestionAudio] 备用定时器触发，但音频状态异常，强制开始答题计时')
                console.warn(`  - paused: ${audio.paused}, currentTime: ${audio.currentTime}, duration: ${audio.duration}`)
                this.questionAudioPlaying = false
                this.isPlayingQuestionAudio = false
                this.startAnswerCountdown()
              }
            }, fallbackTimeout)
          }
        }).catch(error => {
          console.error('❌ [playQuestionAudio] 播放题目音频失败:', error)
          this.questionAudioPlaying = false
          // 重置标志
          this.isPlayingQuestionAudio = false
          // 播放失败，直接开始答题计时
          this.startAnswerCountdown()
        })
      }
      
      // 强制重新加载音频（确保使用新的音频源）
      // 因为 Vue 响应式绑定可能导致 readyState 状态不准确
      audio.load()
      console.log(`✓ [playQuestionAudio] 调用 audio.load() 强制重新加载，readyState=${audio.readyState}`)
      
      // 等待音频加载完成
      console.log(`⏳ [playQuestionAudio] 等待 canplaythrough 事件`)
      audio.addEventListener('canplaythrough', onCanPlay, { once: true })
      
      // 设置超时（10秒），如果音频一直无法加载，则开始答题计时
      setTimeout(() => {
        if (audio.readyState < 3 && !this.questionAudioPlaying) {
          console.error('❌ [playQuestionAudio] 题目音频加载超时（10秒），readyState=' + audio.readyState)
          // 重置标志
          this.isPlayingQuestionAudio = false
          this.startAnswerCountdown()
        }
      }, 10000)
    },
    
    /**
     * 重新播放题目音频（用于重复播放）
     */
    replayQuestionAudio() {
      const audio = this.$refs.questionAudioPlayer
      if (!audio || !this.questionAudioPath) {
        console.warn('⚠️ [replayQuestionAudio] 无法重新播放')
        this.startAnswerCountdown()
        return
      }
      
      audio.currentTime = 0
      audio.volume = this.volume / 100
      
      audio.play().then(() => {
        console.log(`✓ [replayQuestionAudio] 题目音频开始播放，第 ${this.questionAudioPlayedCount + 1}/${this.questionAudioPlayCount} 遍`)
        this.questionAudioPlaying = true
        // 初始化音频可视化
        this.initAudioVisualization(audio)
      }).catch(error => {
        console.error('重新播放题目音频失败:', error)
        this.questionAudioPlaying = false
        this.startAnswerCountdown()
      })
    },
    
    /**
     * 题目音频播放结束事件
     */
    onQuestionAudioEnded() {
      // 忽略因为清空 src 导致的 ended 事件
      if (!this.questionAudioPath) {
        return
      }
      
      this.questionAudioPlayedCount++
      console.log(`✓ [onQuestionAudioEnded] 题目音频播放完成，已播放 ${this.questionAudioPlayedCount}/${this.questionAudioPlayCount} 次`)
      this.questionAudioPlaying = false
      
      // 检查是否需要重复播放
      if (this.questionAudioPlayedCount < this.questionAudioPlayCount) {
        // 还需要继续播放，等待1秒后重新播放
        console.log(`✓ [onQuestionAudioEnded] 需要重复播放，等待1秒后播放第 ${this.questionAudioPlayedCount + 1} 遍`)
        this.questionAudioTimer = setTimeout(() => {
          this.replayQuestionAudio()
        }, 1000)
      } else {
        // 播放次数已满，重置标志并开始答题计时
        console.log('✓ [onQuestionAudioEnded] 音频播放完成，开始答题计时')
        this.isPlayingQuestionAudio = false
        this.startAnswerCountdown()
      }
    },
    
    /**
     * 题目音频播放错误事件
     */
    onQuestionAudioError(error) {
      if (!this.questionAudioPath) {
        return
      }
      
      console.error('❌ [onQuestionAudioError] 题目音频播放错误:', error)
      this.questionAudioPlaying = false
      // 重置标志
      this.isPlayingQuestionAudio = false
      // 播放错误，直接开始答题计时
      this.startAnswerCountdown()
    },
    
    /**
     * 开始答题倒计时
     */
    startAnswerCountdown() {
      // 清除之前的定时器
      if (this.answerCountdownTimer) {
        clearInterval(this.answerCountdownTimer)
        this.answerCountdownTimer = null
      }
      
      this.answerCountdown = this.answerTime
      console.log(`⏱️ [startAnswerCountdown] 开始答题倒计时: ${this.answerCountdown} 秒`)
      
      // 每秒更新倒计时
      this.answerCountdownTimer = setInterval(() => {
        this.answerCountdown--
        
        if (this.answerCountdown <= 0) {
          // 倒计时结束
          clearInterval(this.answerCountdownTimer)
          this.answerCountdownTimer = null
          console.log('⏱️ [startAnswerCountdown] 答题时间结束')
          this.onAnswerTimeUp()
        }
      }, 1000)
    },
    
    /**
     * 停止答题倒计时
     */
    stopAnswerCountdown() {
      if (this.answerCountdownTimer) {
        clearInterval(this.answerCountdownTimer)
        this.answerCountdownTimer = null
      }
      this.answerCountdown = 0
    },
    
    /**
     * 获取可见的题目列表（显示所有题目，包括已过期的）
     */
    getVisibleQuestions() {
      if (!this.allQuestions || this.allQuestions.length === 0) {
        return []
      }
      
      // 显示所有题目，包括已过期的题目
      return this.allQuestions
    },
    
    /**
     * 获取题目在全局列表中的索引
     */
    getQuestionGlobalIndex(visibleIndex) {
      // 现在显示所有题目，所以 visibleIndex 就是全局索引
      return visibleIndex
    },
    
    /**
     * 滚动到当前题目
     */
    scrollToCurrentQuestion() {
      this.$nextTick(() => {
        const currentElement = this.$refs.currentQuestionElement
        if (currentElement && currentElement[0]) {
          // 使用 block: 'nearest' 避免过度滚动
          // 或者手动计算滚动位置，留出头部空间
          const container = document.querySelector('.question-content-wrapper')
          if (container) {
            const elementTop = currentElement[0].offsetTop
            // 滚动到元素位置，但留出一点顶部空间（不要紧贴顶部）
            container.scrollTo({
              top: Math.max(0, elementTop - 10),
              behavior: 'smooth'
            })
          }
        }
      })
    },
    
    /**
     * 答题时间结束，自动跳到下一题
     */
    async onAnswerTimeUp() {
      console.log(`⏱️ [onAnswerTimeUp] 题目 ${this.currentQuestionIndex + 1} 答题时间结束`)
      
      // 将当前题目标记为已过期
      if (!this.expiredQuestionIndexes.includes(this.currentQuestionIndex)) {
        this.expiredQuestionIndexes.push(this.currentQuestionIndex)
        console.log(`⏱️ [onAnswerTimeUp] 题目 ${this.currentQuestionIndex + 1} 已标记为过期`)
      }
      
      // 自动保存当前题目的答题记录（无论是否作答）
      await this.saveCurrentQuestionResult()
      
      // 停止当前题目音频和所有相关定时器
      this.questionAudioPath = ''
      this.questionAudioPlaying = false
      this.isPlayingQuestionAudio = false // 重置播放标志
      // 清除题目音频定时器
      if (this.questionAudioTimer) {
        clearTimeout(this.questionAudioTimer)
        this.questionAudioTimer = null
      }
      const audio = this.$refs.questionAudioPlayer
      if (audio) {
        audio.pause()
        audio.src = ''
      }
      // 停止音频可视化
      this.stopAudioVisualization()
      
      // 检查是否是当前大题的最后一题
      const currentQuestion = this.allQuestions[this.currentQuestionIndex]
      const currentSectionId = currentQuestion.sectionId || currentQuestion.section_id
      
      // 检查下一题是否属于同一个大题
      const isLastQuestionOfSection = this.currentQuestionIndex >= this.allQuestions.length - 1 ||
        (this.allQuestions[this.currentQuestionIndex + 1] && 
         (this.allQuestions[this.currentQuestionIndex + 1].sectionId || this.allQuestions[this.currentQuestionIndex + 1].section_id) !== currentSectionId)
      
      console.log(`📦 [onAnswerTimeUp] 判断是否是最后一题: isLastQuestionOfSection=${isLastQuestionOfSection}, currentIndex=${this.currentQuestionIndex}, totalQuestions=${this.allQuestions.length}`)
      console.log(`📦 [onAnswerTimeUp] 当前题目ID: ${currentQuestion.id}, sectionId: ${currentSectionId}`)
      
      if (isLastQuestionOfSection) {
        // 当前大题的最后一题答题结束，检查是否有下一个大题
        console.log('✓ [onAnswerTimeUp] 当前大题答题完成，检查下一个大题')
        this.loadNextSection()
      } else {
        // 跳到下一题（同一个大题内）
        this.currentQuestionIndex++
        const nextQuestion = this.allQuestions[this.currentQuestionIndex]
        
        // 滚动到当前题目
        this.scrollToCurrentQuestion()
        this.loadQuestion(nextQuestion)
        console.log(`✓ [onAnswerTimeUp] 跳到题目 ${this.currentQuestionIndex + 1}, questionId=${nextQuestion?.id}`)
        
        // 播放下一题音频（使用 setTimeout 确保状态已清理）
        setTimeout(() => {
          console.log(`✓ [onAnswerTimeUp] setTimeout 触发，准备播放题目 ${this.currentQuestionIndex + 1} 的音频`)
          this.playCurrentQuestionAudio()
        }, 200)
      }
    },
    
    /**
     * 加载下一个大题的题目列表
     */
    async loadNextSection() {
      try {
        // 找到当前大题在 currentVolumeSections 中的索引
        const currentQuestion = this.allQuestions[this.currentQuestionIndex]
        const currentSectionId = currentQuestion.sectionId || currentQuestion.section_id
        const currentSectionIndex = this.currentVolumeSections.findIndex(s => s.id === currentSectionId)
        
        console.log(`📦 [loadNextSection] 当前大题索引: ${currentSectionIndex}, 大题ID: ${currentSectionId}`)
        
        // 检查是否有下一个大题
        if (currentSectionIndex >= 0 && currentSectionIndex < this.currentVolumeSections.length - 1) {
          const nextSection = this.currentVolumeSections[currentSectionIndex + 1]
          console.log(`📦 [loadNextSection] 找到下一个大题: ${nextSection.section_name || nextSection.sectionName}`)
          
          // 重置状态
          this.showQuestions = false
          this.loading = true
          this.currentSectionIndex = currentSectionIndex + 1
          
          // 等待1.5秒后播放下一个大题音频
          setTimeout(() => {
            this.loadAndPlaySectionAudio(nextSection)
          }, 1500)
        } else {
          // 没有更多大题，当前卷别答题完成
          console.log('✓ [loadNextSection] 当前卷别所有大题答题完成')
          this.goToVolumeComplete()
        }
      } catch (error) {
        console.error('❌ [loadNextSection] 加载下一个大题失败:', error)
        this.goToVolumeComplete()
      }
    },
    
    /**
     * 当前卷别答题完成，显示提示并检查下一步
     */
    async goToVolumeComplete() {
      // 停止所有定时器
      this.stopAnswerCountdown()
      if (this.questionAudioTimer) {
        clearTimeout(this.questionAudioTimer)
        this.questionAudioTimer = null
      }
      
      // 保存当前卷别代码
      localStorage.setItem('completedVolumeCode', this.currentVolumeCode)
      
      console.log(`📦 [goToVolumeComplete] 当前卷别 ${this.currentVolumeCode} 答题完成`)
      
      // 1. 保存当前卷别的答案并提交当前卷别
      await this.saveCurrentVolumeAnswers()
      
      // 2. 获取当前卷别名称并显示完成提示
      let volumeName = this.currentVolumeCode
      try {
        const paperData = await ipcRenderer.invoke('paper:getPaperData', this.paperId)
        if (paperData?.manifest?.volumes) {
          const currentVolume = paperData.manifest.volumes.find(v => 
            (v.volumeCode || v.volume_code) === this.currentVolumeCode
          )
          if (currentVolume) {
            volumeName = currentVolume.volumeName || currentVolume.volume_name || this.currentVolumeCode
          }
        }
      } catch (error) {
        console.warn('获取卷别名称失败:', error)
      }
      
      // 显示卷别完成提示
      this.volumeCompleteText = `${volumeName}作答完成`
      this.showVolumeComplete = true
      
      // 1.5秒后检查下一步
      setTimeout(() => {
        this.checkNextVolumeOrComplete()
      }, 1500)
    },
    
    /**
     * 检查是否有下一卷别或全部完成
     */
    async checkNextVolumeOrComplete() {
      try {
        const paperData = await ipcRenderer.invoke('paper:getPaperData', this.paperId)
        if (!paperData?.manifest?.volumes) {
          this.showAllCompleteAndSubmit()
          return
        }

        const volumes = paperData.manifest.volumes
        const sortedVolumes = [...volumes].sort((a, b) => (a.volumeOrder || 0) - (b.volumeOrder || 0))
        
        // 找到当前卷别的索引
        const currentIndex = sortedVolumes.findIndex(v => 
          (v.volumeCode || v.volume_code) === this.currentVolumeCode
        )
        
        // 检查后续卷别是否有有效的（有题目的）
        for (let i = currentIndex + 1; i < sortedVolumes.length; i++) {
          const nextVolume = sortedVolumes[i]
          const hasValidContent = await this.checkVolumeHasQuestions(nextVolume, paperData)
          if (hasValidContent) {
            const nextVolumeCode = nextVolume.volumeCode || nextVolume.volume_code
            console.log(`✓ 找到下一个有效卷别: ${nextVolumeCode}`)
            
            // 隐藏完成提示，跳转到下一卷别
            this.showVolumeComplete = false
            
            // 检查是否有中场设置
            const intermissions = await ipcRenderer.invoke('paper:getIntermissions', this.paperId)
            const intermission = intermissions?.find(i => 
              i.from_volume === this.currentVolumeCode && 
              i.to_volume === nextVolumeCode
            )
            
            if (intermission) {
              // 同步保存下一卷的 ID 和 Code，避免后续页面读取不一致
              if (nextVolume?.id) {
                localStorage.setItem('currentVolumeId', String(nextVolume.id))
              }
              localStorage.setItem('currentVolumeCode', nextVolumeCode)
              this.$router.push({
                path: '/intermission',
                query: {
                  paperId: this.paperId,
                  paperInfoId: this.paperInfoId,
                  fromVolume: this.currentVolumeCode,
                  toVolume: nextVolumeCode
                }
              })
            } else {
              if (nextVolume?.id) {
                localStorage.setItem('currentVolumeId', String(nextVolume.id))
              }
              localStorage.setItem('currentVolumeCode', nextVolumeCode)
              this.$router.push({
                path: '/broadcast',
                query: {
                  fromVolumeComplete: 'true'
                }
              })
            }
            return
          }
        }
        
        // 没有更多卷别，显示"作答完成"
        this.showAllCompleteAndSubmit()
      } catch (error) {
        console.error('检查下一卷别失败:', error)
        this.showAllCompleteAndSubmit()
      }
    },
    
    /**
     * 检查卷别是否有有效内容
     */
    async checkVolumeHasQuestions(volume, paperData) {
      try {
        if (!paperData?.manifest) return false
        
        const sections = paperData.manifest.sections || []
        const volumeId = volume.id
        const volumeSections = sections.filter(s => s.volumeId === volumeId)
        
        if (volumeSections.length === 0) return false
        
        for (const section of volumeSections) {
          const questions = await ipcRenderer.invoke('paper:getQuestions', this.paperId, section.id)
          if (questions?.length > 0) return true
        }
        return false
      } catch (error) {
        return false
      }
    },
    
    /**
     * 显示"作答完成"，1.5秒后提交并跳转结果页
     */
    async showAllCompleteAndSubmit() {
      this.isAllComplete = true
      this.volumeCompleteText = '作答完成'
      this.showVolumeComplete = true
      
      setTimeout(async () => {
        await this.submitAllAndComplete()
      }, 1500)
    },
    
    /**
     * 提交所有答案并跳转结果页
     */
    async submitAllAndComplete() {
      try {
        const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
        const userId = userInfo.userId || userInfo.user?.userId
        
        if (!userId || !this.paperId || !this.paperInfoId) {
          this.$router.push('/paper-select')
          return
        }

        const paperInfo = await ipcRenderer.invoke('answer:getPaperInfo', this.paperInfoId)
        const paperData = await ipcRenderer.invoke('paper:getPaperData', this.paperId)
        
        const questionResults = paperInfo?.questionResults || []
        const correctCount = questionResults.filter(r => r.result === 1).length
        const wrongCount = questionResults.filter(r => r.result === 0).length
        const totalScore = paperData?.manifest?.totalScore || 100
        const scorePerQuestion = questionResults.length > 0 ? totalScore / questionResults.length : 0
        const userScore = Math.round(correctCount * scorePerQuestion)

        const submitResult = await ipcRenderer.invoke('answer:submitExam', {
          paperInfoId: this.paperInfoId,
          paperId: this.paperId,
          appUserId: userId,
          totalScore: totalScore,
          userScore: userScore,
          correctCount: correctCount,
          wrongCount: wrongCount
        })

        if (submitResult.success) {
          this.$router.push({
            path: '/exam-result',
            query: { paperInfoId: this.paperInfoId }
          })
        } else {
          throw new Error(submitResult.message || '提交失败')
        }
      } catch (error) {
        console.error('提交失败:', error)
        this.$message.error('提交答题结果失败')
        this.$router.push('/paper-select')
      }
    },
    
    /**
     * 提交当前卷别（答案已在答题过程中实时保存，这里只提交卷别状态）
     * 注意：不再重新保存答案，因为：
     * 1. selectOption 方法在用户选择答案时已实时保存
     * 2. saveCurrentQuestionResult 方法在答题时间结束时已保存未作答的题目
     * 3. allQuestions 只包含当前大题的题目，重新保存会导致之前大题的答案被遗漏或覆盖
     */
    async saveCurrentVolumeAnswers() {
      try {
        // 确保有答题记录
        if (!this.paperInfoId) {
          await this.initOrReusePaperInfo()
        }

        if (!this.paperInfoId) {
          console.error('无法创建或获取答题记录')
          return
        }

        // 只提交当前卷别状态，不重新保存答案
        await ipcRenderer.invoke('answer:submitVolume', {
          paperInfoId: this.paperInfoId,
          volumeCode: this.currentVolumeCode
        })

        console.log(`✓ 当前卷别 ${this.currentVolumeCode} 已提交`)
      } catch (error) {
        console.error('提交当前卷别失败:', error)
      }
    },
    
    
    /**
     * 检查卷别是否有有效内容（大题和题目）
     */
    async checkVolumeHasQuestions(volume, paperData) {
      try {
        if (!paperData || !paperData.manifest) return false
        
        const manifest = paperData.manifest
        const sections = manifest.sections || []
        
        // 找到属于这个卷别的大题
        const volumeSections = sections.filter(s => s.volumeId === volume.id)
        if (volumeSections.length === 0) {
          console.log(`⚠️ [checkVolumeHasQuestions] 卷别 ${volume.volumeCode} 没有大题`)
          return false
        }
        
        // 检查这些大题是否有题目
        for (const section of volumeSections) {
          const questions = await ipcRenderer.invoke('paper:getQuestions', this.paperId, section.id)
          if (questions && questions.length > 0) {
            console.log(`✓ [checkVolumeHasQuestions] 卷别 ${volume.volumeCode} 有题目`)
            return true
          }
        }
        
        console.log(`⚠️ [checkVolumeHasQuestions] 卷别 ${volume.volumeCode} 的大题都没有题目`)
        return false
      } catch (error) {
        console.error('检查卷别内容失败:', error)
        return false
      }
    },
    
    /**
     * 检查题目是否已过期
     */
    isQuestionExpired(index) {
      return this.expiredQuestionIndexes.includes(index)
    },
    
    /**
     * 检查题目是否可以答题
     */
    canAnswerQuestion(index) {
      // 只有当前题目可以答题，已过期的题目不能答题
      return index === this.currentQuestionIndex && !this.isQuestionExpired(index)
    },
    
    /**
     * 检查选项是否被选中
     */
    isOptionSelected(questionIndex, optionId) {
      return this.answeredQuestions[questionIndex] === optionId
    },
    
    /**
     * 选择答案（实时保存到数据库）
     */
    async selectOption(questionIndex, optionId, option) {
      // 检查是否已过期
      if (this.isQuestionExpired(questionIndex)) {
        console.log(`⚠️ 题目 ${questionIndex + 1} 已过期，不能选择答案`)
        return
      }
      
      // 检查是否是当前题目（只有当前题目才能答题）
      if (questionIndex !== this.currentQuestionIndex) {
        console.log(`⚠️ 题目 ${questionIndex + 1} 不是当前题目（当前是第 ${this.currentQuestionIndex + 1} 题），不能选择答案`)
        return
      }
      
      // 清除之前的自动跳转定时器（用户更换了答案）
      if (this.autoNextTimer) {
        clearTimeout(this.autoNextTimer)
        this.autoNextTimer = null
      }
      
      // 记录答案和用时到内存（多次选择使用最后一次的时间）
      this.$set(this.answeredQuestions, questionIndex, optionId)
      const timeSpent = this.answerTime - this.answerCountdown
      this.$set(this.answeredTimeSpent, questionIndex, timeSpent)
      const userAnswerText = option.optionName || option.label || String.fromCharCode(65 + optionId)
      console.log(`✓ 题目 ${questionIndex + 1} 选择了答案:`, userAnswerText, `用时: ${timeSpent}秒`)
      
      // 设置2秒后自动跳转下一题（如果用户在2秒内未更换答案）
      this.autoNextTimer = setTimeout(() => {
        this.autoNextTimer = null
        // 检查是否还在当前题目且倒计时还在进行
        if (this.currentQuestionIndex === questionIndex && this.answerCountdown > 0) {
          console.log(`⏱️ 用户2秒内未更换答案，自动跳转下一题`)
          // 停止当前倒计时
          this.stopAnswerCountdown()
          // 触发答题时间结束逻辑（会自动标记过期并跳转）
          this.onAnswerTimeUp()
        }
      }, 2000)
      
      // 实时保存到数据库
      try {
        const question = this.allQuestions[questionIndex]
        if (!question || !this.paperInfoId) {
          console.error(`❌ [selectOption] 题目 ${questionIndex + 1} 数据无效，question=${!!question}, paperInfoId=${this.paperInfoId}`)
          return
        }
        
        // 获取题目ID（兼容多种字段名）
        const questionId = question.id || question.question_id || question.questionId
        if (!questionId) {
          console.error(`❌ [selectOption] 题目 ${questionIndex + 1} 没有有效的ID，无法保存答案，question:`, JSON.stringify({
            id: question.id,
            question_id: question.question_id,
            questionId: question.questionId,
            title: question.title?.substring(0, 50)
          }))
          return
        }
        console.log(`📦 [selectOption] 题目 ${questionIndex + 1} 的ID: ${questionId}, sectionId: ${question.section_id || question.sectionId}`)
        
        const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
        const userId = userInfo.userId || userInfo.user?.userId
        if (!userId) return
        
        // 判断是否正确：从选项的 isAnswer/is_answer 字段判断
        const selectedOption = question.answers?.find(opt => (opt.id || opt.answerId) === optionId)
        let isCorrect = 0
        if (selectedOption) {
          isCorrect = (selectedOption.isAnswer === 1 || selectedOption.is_answer === 1 || 
                       selectedOption.isAnswer === true || selectedOption.is_answer === true) ? 1 : 0
        }
        // 如果选项没有 isAnswer 字段，降级使用 question.answer
        if (!selectedOption || (selectedOption.isAnswer === undefined && selectedOption.is_answer === undefined)) {
          const correctAnswerIds = question.answer ? 
            (Array.isArray(question.answer) ? question.answer : String(question.answer).split(',').map(id => parseInt(id))) : []
          isCorrect = correctAnswerIds.includes(optionId) ? 1 : 0
        }
        
        await ipcRenderer.invoke('answer:saveQuestionResult', {
          paperInfoId: this.paperInfoId,
          paperId: this.paperId,
          appUserId: userId,
          questionId: questionId,
          answerIds: String(optionId),
          userAnswer: userAnswerText,
          result: isCorrect,
          questionSort: questionIndex + 1,
          timeSpent: timeSpent, // 已用时间
          sectionId: question.section_id || question.sectionId || null,
          volumeId: this.currentVolumeId || null
        })
        console.log(`✓ 题目 ${questionIndex + 1} (ID: ${questionId}) 答案已保存到数据库，结果: ${isCorrect ? '正确' : '错误'}，用时: ${timeSpent}秒`)
      } catch (error) {
        console.error(`保存题目 ${questionIndex + 1} 答案失败:`, error)
      }
    },

    /**
     * 保存当前题目的答题记录（答题时间结束时自动调用）
     * 注意：如果用户已作答，selectOption 已保存记录，这里不再重复保存
     */
    async saveCurrentQuestionResult() {
      try {
        const question = this.allQuestions[this.currentQuestionIndex]
        if (!question || !this.paperInfoId) {
          console.error(`❌ [saveCurrentQuestionResult] 题目 ${this.currentQuestionIndex + 1} 数据无效，question=${!!question}, paperInfoId=${this.paperInfoId}`)
          return
        }
        
        const answerId = this.answeredQuestions[this.currentQuestionIndex]
        
        // 如果用户已作答，selectOption 已经保存了正确的用时，不再重复保存
        if (answerId !== undefined) {
          console.log(`✓ [saveCurrentQuestionResult] 题目 ${this.currentQuestionIndex + 1} 已在选择时保存，跳过`)
          return
        }
        
        // 获取题目ID（兼容多种字段名）
        const questionId = question.id || question.question_id || question.questionId
        if (!questionId) {
          console.error(`❌ [saveCurrentQuestionResult] 题目 ${this.currentQuestionIndex + 1} 没有有效的ID，无法保存答案，question:`, JSON.stringify({
            id: question.id,
            question_id: question.question_id,
            questionId: question.questionId,
            title: question.title?.substring(0, 50)
          }))
          return
        }
        
        const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
        const userId = userInfo.userId || userInfo.user?.userId
        if (!userId) {
          console.error(`❌ [saveCurrentQuestionResult] 无法获取用户ID`)
          return
        }
        
        console.log(`📦 [saveCurrentQuestionResult] 保存未作答题目 ${this.currentQuestionIndex + 1}，questionId: ${questionId}`)
        
        // 未作答的题目：answerIds 为空，isCorrect 为 0，用时为完整答题时间
        await ipcRenderer.invoke('answer:saveQuestionResult', {
          paperInfoId: this.paperInfoId,
          paperId: this.paperId,
          appUserId: userId,
          questionId: questionId,
          answerIds: '',
          userAnswer: '',
          result: 0,
          questionSort: this.currentQuestionIndex + 1,
          timeSpent: this.answerTime, // 未作答用完整答题时间
          sectionId: question.section_id || question.sectionId || null,
          volumeId: this.currentVolumeId || null
        })
        console.log(`✓ [saveCurrentQuestionResult] 题目 ${this.currentQuestionIndex + 1} (ID: ${questionId}) 未作答，已保存，用时: ${this.answerTime}秒`)
      } catch (error) {
        console.error(`[saveCurrentQuestionResult] 保存题目 ${this.currentQuestionIndex + 1} 失败:`, error)
      }
    },

    // ==================== 结束：题目音频播放和答题计时相关方法 ====================

    /**
     * 保存所有答案并创建/获取答题记录，然后跳转到完成页面
     */
    async saveAllAnswersAndCreatePaperInfo() {
      try {
        const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
        const userId = userInfo.userId || userInfo.user?.userId
        
        if (!userId || !this.paperId) {
          console.error('缺少用户ID或试卷ID')
          this.$router.push('/paper-select')
          return
        }

        // 获取试卷信息
        const paperData = await ipcRenderer.invoke('paper:getPaperData', this.paperId)
        const paperName = paperData?.manifest?.paperName || '未命名试卷'

        // 创建或获取答题记录
        const startResult = await ipcRenderer.invoke('answer:startExam', {
          paperId: this.paperId,
          paperName: paperName,
          appUserId: userId,
          volumeCodes: [],
          assignedSeatNumber: null
        })

        if (!startResult.success) {
          throw new Error(startResult.message || '创建答题记录失败')
        }

        const paperInfoId = startResult.paperInfoId

        // 保存所有答案
        let correctCount = 0
        let wrongCount = 0

        for (let i = 0; i < this.allQuestions.length; i++) {
          const question = this.allQuestions[i]
          const answerId = this.answeredQuestions[i]
          
          if (answerId === undefined) {
            // 未答题，标记为错误
            wrongCount++
            continue
          }

          // 获取选项信息
          const option = question.answers?.find(opt => (opt.id || opt.answerId) === answerId)
          const userAnswerText = option ? (option.optionName || option.option_name || option.text || '') : ''

          // 判断是否正确：从选项的 isAnswer/is_answer 字段判断
          let isCorrect = 0
          if (option) {
            isCorrect = (option.isAnswer === 1 || option.is_answer === 1 || 
                         option.isAnswer === true || option.is_answer === true) ? 1 : 0
          }

          if (isCorrect) {
            correctCount++
          } else {
            wrongCount++
          }

          // 保存答案（时间暂时设为0，因为自动答题模式没有单独计时）
          await ipcRenderer.invoke('answer:saveQuestionResult', {
            paperInfoId: paperInfoId,
            paperId: this.paperId,
            appUserId: userId,
            questionId: question.id,
            answerIds: String(answerId),
            userAnswer: userAnswerText,
            result: isCorrect,
            questionSort: i + 1,
            timeSpent: 0,
            sectionId: question.section_id || question.sectionId || null,
            volumeId: this.currentVolumeId || null
          })
        }

        // 计算总分和得分（需要从试卷配置获取每题分数，这里暂时简化）
        const totalScore = paperData?.manifest?.totalScore
        const scorePerQuestion = totalScore / this.allQuestions.length
        const userScore = Math.round(correctCount * scorePerQuestion)

        // 提交答题结果
        const submitResult = await ipcRenderer.invoke('answer:submitExam', {
          paperInfoId: paperInfoId,
          paperId: this.paperId,
          appUserId: userId,
          totalScore: totalScore,
          userScore: userScore,
          correctCount: correctCount,
          wrongCount: wrongCount
        })

        if (submitResult.success) {
          // 跳转到答题完成页面
          this.$router.push({
            path: '/exam-result',
            query: {
              paperInfoId: paperInfoId
            }
          })
        } else {
          throw new Error(submitResult.message || '提交答题结果失败')
        }
      } catch (error) {
        console.error('保存答案并创建答题记录失败:', error)
        this.$message.error('保存答题结果失败：' + error.message)
        this.$router.push('/paper-select')
      }
    },

    getQuestionNumbers(sectionId) {
      return this.sectionQuestionsMap[sectionId] || []
    },
    
    /**
     * 检查是否是当前题目（右侧题号列表用）
     */
    isCurrentQuestion(sectionId, qIndex) {
      const questions = this.sectionQuestionsMap[sectionId] || []
      if (questions.length === 0) return false
      
      // 找到当前题目在哪个大题中
      const currentQuestion = this.allQuestions[this.currentQuestionIndex]
      if (!currentQuestion) return false
      
      const currentSectionId = currentQuestion.sectionId || currentQuestion.section_id
      if (currentSectionId !== sectionId) return false
      
      // 计算当前题目在这个大题中的索引
      const questionsInSection = this.allQuestions.filter(q => 
        (q.sectionId || q.section_id) === sectionId
      )
      const indexInSection = questionsInSection.findIndex(q => q.id === currentQuestion.id)
      
      return indexInSection === qIndex
    },
    
    /**
     * 检查题目是否已作答（右侧题号列表用）
     */
    isQuestionAnswered(sectionId, qIndex) {
      // 计算全局索引
      const globalIndex = this.getGlobalIndexBySection(sectionId, qIndex)
      return this.answeredQuestions[globalIndex] !== undefined
    },
    
    /**
     * 检查题目是否已过期（右侧题号列表用）
     */
    isQuestionExpiredBySection(sectionId, qIndex) {
      const globalIndex = this.getGlobalIndexBySection(sectionId, qIndex)
      return this.expiredQuestionIndexes.includes(globalIndex)
    },
    
    /**
     * 根据大题ID和题目在大题中的索引，获取全局索引
     * 使用 sectionQuestionsMap 来计算，因为 allQuestions 只包含当前大题的题目
     */
    getGlobalIndexBySection(sectionId, qIndex) {
      let globalIndex = 0
      for (const section of this.currentVolumeSections) {
        // 使用 sectionQuestionsMap 获取每个大题的题目数量
        const questionNumbers = this.sectionQuestionsMap[section.id] || []
        if (section.id === sectionId) {
          return globalIndex + qIndex
        }
        globalIndex += questionNumbers.length
      }
      return -1
    },

    async onVolumeChange(value) {
      // 更新音量值
      this.volume = value
      console.log('🎵 音量已更新:', value, '%')
      
      // 控制系统音量（通过 IPC）
      try {
        const result = await ipcRenderer.invoke('system:setVolume', value)
        if (result.success) {
          console.log('✓ 系统音量已设置为:', value, '%')
        } else {
          console.warn('⚠️ 系统音量设置失败，使用应用内音量:', result.message)
          // 降级：使用应用内音量
          const audio = this.$refs.sectionAudioPlayer
          if (audio) {
            audio.volume = value / 100
          }
          const questionAudio = this.$refs.questionAudioPlayer
          if (questionAudio) {
            questionAudio.volume = value / 100
          }
        }
      } catch (error) {
        console.error('❌ 设置系统音量失败:', error)
        // 降级：使用应用内音量
        const audio = this.$refs.sectionAudioPlayer
        if (audio) {
          audio.volume = value / 100
        }
        const questionAudio = this.$refs.questionAudioPlayer
        if (questionAudio) {
          questionAudio.volume = value / 100
        }
      }
    },

    initAudioVisualization(audio) {
      try {
        // 停止之前的动画
        if (this.animationFrameId) {
          cancelAnimationFrame(this.animationFrameId)
          this.animationFrameId = null
        }
        
        // 检查是否已经为这个音频元素创建过 MediaElementSource
        // 每个音频元素只能创建一次 MediaElementSource
        const audioId = audio === this.$refs.sectionAudioPlayer ? 'section' : 'question'
        
        // 如果已经有这个音频元素的 source，直接复用
        if (this._audioSources && this._audioSources[audioId]) {
          // 恢复 AudioContext（如果被暂停）
          if (this.audioContext && this.audioContext.state === 'suspended') {
            this.audioContext.resume()
          }
          // 确保 analyser 存在
          if (!this.analyser) {
            this.analyser = this.audioContext.createAnalyser()
            this.analyser.fftSize = 256
            this.analyser.smoothingTimeConstant = 0.8
            this.dataArray = new Uint8Array(this.analyser.frequencyBinCount)
          }
          // 重新连接（断开旧连接，建立新连接）
          try {
            this._audioSources[audioId].disconnect()
          } catch (e) { /* 忽略断开错误 */ }
          this._audioSources[audioId].connect(this.analyser)
          this.analyser.connect(this.audioContext.destination)
          // 开始绘制动画
          this.drawFrequencyBars()
          return
        }
        
        // 创建 AudioContext（如果不存在）
        const AudioContext = window.AudioContext || window.webkitAudioContext
        if (!AudioContext) {
          console.warn('浏览器不支持 Web Audio API')
          return
        }

        if (!this.audioContext) {
          this.audioContext = new AudioContext()
        }
        
        // 初始化 sources 存储对象
        if (!this._audioSources) {
          this._audioSources = {}
        }
        
        // 创建 AnalyserNode
        if (!this.analyser) {
          this.analyser = this.audioContext.createAnalyser()
          this.analyser.fftSize = 256
          this.analyser.smoothingTimeConstant = 0.8
          this.dataArray = new Uint8Array(this.analyser.frequencyBinCount)
        }
        
        // 为这个音频元素创建 MediaElementSource（只创建一次）
        this._audioSources[audioId] = this.audioContext.createMediaElementSource(audio)
        this._audioSources[audioId].connect(this.analyser)
        this.analyser.connect(this.audioContext.destination)
        
        // 开始绘制动画
        this.drawFrequencyBars()
      } catch (error) {
        console.error('初始化音频可视化失败:', error)
      }
    },

    drawFrequencyBars() {
      if (!this.analyser || !this.$refs.frequencyCanvas) {
        return
      }

      const canvas = this.$refs.frequencyCanvas
      const ctx = canvas.getContext('2d')
      
      // 设置画布尺寸
      if (canvas.width !== canvas.offsetWidth) {
        canvas.width = canvas.offsetWidth
      }
      if (canvas.height !== canvas.offsetHeight) {
        canvas.height = canvas.offsetHeight
      }

      const width = canvas.width
      const height = canvas.height

      // 获取频率数据
      this.analyser.getByteFrequencyData(this.dataArray)

      // 清空画布
      ctx.fillStyle = '#409EFF' // 蓝色背景
      ctx.fillRect(0, 0, width, height)

      // 绘制波浪线
      const centerY = height / 2 // 中心线位置
      const pointCount = width // 点的数量，铺满整个宽度
      
      ctx.strokeStyle = '#FFFFFF' // 白色线条
      ctx.lineWidth = 1.5 // 细线条
      ctx.lineCap = 'round'
      ctx.lineJoin = 'round'
      
      // 绘制上波浪线
      ctx.beginPath()
      for (let i = 0; i < pointCount; i++) {
        // 从频率数据中采样
        const dataIndex = Math.floor((i / pointCount) * this.dataArray.length)
        const amplitude = (this.dataArray[dataIndex] / 255) * (height / 2 - 5) // 振幅（从中心线向上扩展，留出边距）
        
        const x = i
        const y = centerY - amplitude // 从中心线向上偏移
        
        if (i === 0) {
          ctx.moveTo(x, y)
        } else {
          ctx.lineTo(x, y)
        }
      }
      ctx.stroke()
      
      // 绘制下波浪线（镜像）
      ctx.beginPath()
      for (let i = 0; i < pointCount; i++) {
        const dataIndex = Math.floor((i / pointCount) * this.dataArray.length)
        const amplitude = (this.dataArray[dataIndex] / 255) * (height / 2 - 5)
        
        const x = i
        const y = centerY + amplitude // 从中心线向下偏移
        
        if (i === 0) {
          ctx.moveTo(x, y)
        } else {
          ctx.lineTo(x, y)
        }
      }
      ctx.stroke()

      // 继续动画循环
      this.animationFrameId = requestAnimationFrame(() => {
        this.drawFrequencyBars()
      })
    },

    stopAudioVisualization() {
      // 只停止动画，不关闭 AudioContext（因为 MediaElementSource 只能创建一次）
      if (this.animationFrameId) {
        cancelAnimationFrame(this.animationFrameId)
        this.animationFrameId = null
      }
      // 不再关闭 AudioContext，保持音频连接
      // this.audioContext 和 this._mediaSource 保持不变
      this.analyser = null
      this.dataArray = null
    },
    
    // 完全清理音频可视化资源（仅在组件销毁时调用）
    destroyAudioVisualization() {
      this.stopAudioVisualization()
      if (this.audioContext) {
        this.audioContext.close().catch(err => {
          console.warn('关闭 AudioContext 失败:', err)
        })
        this.audioContext = null
      }
      this._mediaSource = null
      this._lastAudioElement = null
    }
  },

  beforeDestroy() {
    // 完全清理音频可视化资源
    this.destroyAudioVisualization()
    
    // 清除音频播放定时器
    if (this.sectionAudioTimer) {
      clearTimeout(this.sectionAudioTimer)
      this.sectionAudioTimer = null
    }
    
    // 清除题目加载定时器
    if (this.questionsLoadTimer) {
      clearTimeout(this.questionsLoadTimer)
      this.questionsLoadTimer = null
    }
    
    // 清除题目音频定时器
    if (this.questionAudioTimer) {
      clearTimeout(this.questionAudioTimer)
      this.questionAudioTimer = null
    }
    
    // 清除答题倒计时定时器
    if (this.answerCountdownTimer) {
      clearInterval(this.answerCountdownTimer)
      this.answerCountdownTimer = null
    }
    
    // 清除自动跳转下一题定时器
    if (this.autoNextTimer) {
      clearTimeout(this.autoNextTimer)
      this.autoNextTimer = null
    }
    
    // 停止大题音频播放
    if (this.$refs.sectionAudioPlayer) {
      this.$refs.sectionAudioPlayer.pause()
      this.$refs.sectionAudioPlayer.src = ''
    }
    
    // 停止题目音频播放
    if (this.$refs.questionAudioPlayer) {
      this.$refs.questionAudioPlayer.pause()
      this.$refs.questionAudioPlayer.src = ''
    }
  }
}
</script>

<style scoped>
/* 卷别完成提示遮罩 */
.volume-complete-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(245, 247, 250, 0.98);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}

.volume-complete-overlay .complete-message {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 20px;
  margin-top: -80px; /* 向上偏移80px，使内容视觉居中 */
}

.volume-complete-overlay .message-icon {
  font-size: 80px;
  color: #67C23A;
}

.volume-complete-overlay .message-icon i {
  font-size: 80px;
}

.volume-complete-overlay .message-text {
  font-size: 32px;
  font-weight: bold;
  color: #303133;
  text-align: center;
}

.section-list-container {
  width: 100%;
  height: 100vh;
  background: #fff;
  display: flex;
  position: relative;
  padding-bottom: 60px; /* 为底部频率条留出空间 */
  overflow: hidden; /* 防止整体溢出 */
  padding-top: 10px;
  padding-left: 20px;
  margin-left: 0px;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: flex-start; /* 顶部对齐 */
  justify-content: flex-start; /* 左对齐 */
  padding: 0;
  margin: 0;
  margin-left: 0 !important;
  overflow: hidden;
  height: 100%;
}

/* 大题音频播放时（空白区域，等待音频播放完成） */
.section-audio-playing {
  width: 100%;
  height: 100%;
  min-height: calc(100vh - 100px);
}

/* 题目显示区域 */
.questions-wrapper {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 0;
  margin: 0;
  margin-left: 0 !important;
  padding-top: 50px; /* 为固定头部留出空间 */
}

/* 固定悬浮头部 */
.fixed-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 300px; /* 为右侧学生信息区留出空间 */
  z-index: 1000;
  background: #f8f9fa; /* 乳白色背景 */
  color: #333;
  padding: 10px 30px;
  display: flex;
  align-items: center;
  gap: 30px;
  border-bottom: 1px solid #e0e0e0;
  height: 50px;
  box-sizing: border-box;
}

/* 大题标题 */
.header-section-title {
  font-size: 16px;
  font-weight: bold;
  color: #409EFF;
  flex-shrink: 0;
}

/* 题号信息 */
.header-question-info {
  font-size: 14px;
  color: #606266;
  background: #fff;
  padding: 6px 16px;
  border-radius: 15px;
  border: 1px solid #e0e0e0;
}

/* 倒计时 */
.header-countdown {
  display: flex;
  align-items: center;
  gap: 4px;
  background: #E6A23C;
  color: #fff;
  padding: 6px 14px;
  border-radius: 15px;
  font-weight: bold;
  margin-left: auto;
}

.header-countdown.countdown-warning {
  background: #F56C6C;
  animation: shake 0.5s infinite;
}

.countdown-number {
  font-size: 18px;
}

.countdown-text {
  font-size: 12px;
}

/* 音频播放标识 */
.header-audio-indicator {
  display: flex;
  align-items: center;
  gap: 5px;
  color: #67C23A;
  font-size: 14px;
}

.header-audio-indicator i {
  font-size: 16px;
}

/* 大题标题（固定在顶部） */
.section-title-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: #fff;
  border-bottom: 2px solid #409EFF;
  padding: 15px 30px;
  padding-left: 60px !important; /* 强制左边距60px */
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  flex-shrink: 0;
  width: 100%;
  box-sizing: border-box;
  margin: 0;
}

.section-title-text {
  font-size: 18px;
  font-weight: bold;
  color: #409EFF;
  min-height: 24px; /* 确保有高度，即使内容为空 */
  line-height: 1.5;
  margin: 0;
  padding: 0;
  margin-left: 0; /* 确保没有负边距 */
}

.question-counter {
  font-size: 14px;
  color: #606266;
  background: #f0f2f5;
  padding: 6px 12px;
  border-radius: 4px;
}

/* 答题倒计时样式 */
.answer-countdown {
  display: flex;
  align-items: center;
  gap: 5px;
  background: #409EFF;
  color: #fff;
  padding: 8px 16px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
  animation: pulse 1s infinite;
}

.answer-countdown.countdown-warning {
  background: #F56C6C;
  animation: pulse-warning 0.5s infinite;
}

.countdown-number {
  font-size: 20px;
  font-weight: bold;
  min-width: 30px;
  text-align: center;
}

.countdown-text {
  font-size: 12px;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.05); }
}

@keyframes pulse-warning {
  0%, 100% { transform: scale(1); background: #F56C6C; }
  50% { transform: scale(1.1); background: #E6A23C; }
}

/* 音频播放状态指示器 */
.audio-playing-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #67C23A;
  font-size: 14px;
  padding: 6px 12px;
  background: #f0f9eb;
  border-radius: 4px;
  animation: audio-playing 1s infinite;
}

.audio-playing-indicator i {
  font-size: 16px;
}

@keyframes audio-playing {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}

/* 已过期题目样式 */
.question-expired {
  opacity: 0.5;
  pointer-events: none;
  position: relative;
  background: #fafafa !important;
  border-color: #e0e0e0 !important;
}

/* 隐藏"已过期"标签，只保留灰色禁用效果 */
.question-expired::after {
  display: none;
}

.question-expired .question-number-badge {
  background: #e0e0e0 !important;
  color: #909399 !important;
}

.question-expired .current-indicator {
  display: none;
}

.question-expired .option-item-compact {
  background: #f5f5f5 !important;
  cursor: not-allowed !important;
  border-color: #e0e0e0 !important;
}

.question-expired .option-item-compact:hover {
  background: #f5f5f5 !important;
  border-color: #e0e0e0 !important;
  box-shadow: none !important;
}

.question-expired .option-label {
  background: #c0c4cc !important;
}

/* 兼容旧版样式 */
.question-expired .option-item {
  background: #f5f5f5 !important;
  cursor: not-allowed;
}

.question-expired .option-item:hover {
  background: #f5f5f5 !important;
  border-color: #dcdfe6 !important;
}

/* 题目内容区域（可滚动） */
.question-content-wrapper {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 20px 30px 20px 60px; /* 左边间隔60px，与标题对齐 */
}

.question-container {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 15px;
  border: 2px solid transparent;
  transition: all 0.3s;
}

/* 当前题目高亮 */
.question-current {
  border-color: #409EFF;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
  animation: pulse-border 2s ease-in-out infinite;
}

@keyframes pulse-border {
  0%, 100% {
    border-color: #409EFF;
    box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
  }
  50% {
    border-color: #66b1ff;
    box-shadow: 0 6px 16px rgba(64, 158, 255, 0.5);
  }
}

/* 题目头部信息 */
.question-header-inline {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 1px solid #ebeef5;
}

.question-number-badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 12px;
  background: #ecf5ff;
  color: #409EFF;
  font-size: 14px;
  font-weight: 600;
  border-radius: 4px;
}

/* 当前题目箭头指示器（黑色箭头闪烁） */
.current-arrow-indicator {
  display: inline-block;
  color: #000;
  font-size: 16px;
  margin-right: 8px;
  animation: arrow-blink 0.6s ease-in-out infinite;
}

@keyframes arrow-blink {
  0%, 100% { 
    opacity: 1;
  }
  50% { 
    opacity: 0.2;
  }
}

.question-content {
  margin-bottom: 10px;
}

.question-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  line-height: 1.6;
  margin-bottom: 12px;
}

.question-text {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  margin-bottom: 12px;
}

/* 选项网格布局（两列） */
.question-options-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
  margin-top: 15px;
}

.option-item-compact {
  display: flex;
  align-items: flex-start;
  padding: 10px 12px;
  background: #f5f7fa;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
  transition: all 0.3s;
  cursor: pointer;
}

.option-item-compact:hover {
  background: #ecf5ff;
  border-color: #409EFF;
  box-shadow: 0 2px 6px rgba(64, 158, 255, 0.15);
}

.option-disabled {
  cursor: not-allowed !important;
  opacity: 0.7;
}

/* 未选中的禁用选项 */
.option-disabled:not(.option-selected) {
  background: #f5f5f5 !important;
  border-color: #e4e7ed !important;
}

/* 选中的选项样式 */
.option-selected {
  background: #ecf5ff !important;
  border-color: #409EFF !important;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.25) !important;
}

.option-selected .option-label {
  background: #409EFF !important;
  color: #fff !important;
}

/* 已选中且禁用的选项 - 保持选中样式 */
.option-disabled.option-selected {
  background: #ecf5ff !important;
  border-color: #409EFF !important;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.25) !important;
}

.option-disabled.option-selected .option-label {
  background: #409EFF !important;
  color: #fff !important;
}

.option-disabled:hover {
  box-shadow: none !important;
}

.option-disabled:not(.option-selected):hover {
  background: #f5f5f5 !important;
  border-color: #e4e7ed !important;
}

/* 旧版样式保留（兼容） */
.question-options {
  margin-top: 20px;
}

.option-item {
  display: flex;
  align-items: flex-start;
  margin-bottom: 12px;
  padding: 15px;
  background: #f5f7fa;
  border-radius: 6px;
  border: 1px solid #e4e7ed;
  transition: all 0.3s;
  cursor: pointer;
}

.option-item:hover {
  background: #ecf5ff;
  border-color: #409EFF;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.15);
}

.option-item-compact .option-label {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 26px;
  height: 26px;
  line-height: 1;
  text-align: center;
  background: #409EFF;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  border-radius: 4px;
  margin-right: 8px;
  flex-shrink: 0;
}

.option-item-compact .option-text {
  flex: 1;
  color: #606266;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
}

.option-label {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 32px;
  height: 32px;
  line-height: 1;
  text-align: center;
  background: #409EFF;
  color: #fff;
  border-radius: 50%;
  font-weight: bold;
  font-size: 14px;
  margin-right: 15px;
  flex-shrink: 0;
}

.option-text {
  flex: 1;
  font-size: 16px;
  color: #303133;
  line-height: 1.8;
  padding-top: 4px;
}

.pagination-controls {
  position: sticky;
  bottom: 0;
  z-index: 100;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 30px 15px 60px; /* 左边间隔60px，与标题对齐 */
  background: #fff;
  border-top: 1px solid #e0e0e0;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.05);
  flex-shrink: 0;
}

.pagination-controls .el-button {
  min-width: 120px;
  height: 40px;
  font-size: 14px;
}

.loading-wrapper {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 400px;
}

.no-options {
  padding: 20px;
  text-align: center;
  color: #909399;
  font-size: 14px;
}

.student-info-panel {
  width: 300px;
  background: #f9f9f9;
  padding: 30px 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  border-left: 1px solid #e0e0e0;
  position: relative;
  height: 100vh;
}

.avatar-section {
  position: relative;
  margin-bottom: 30px;
}

.avatar-number {
  position: absolute;
  top: -10px;
  right: -10px;
  background: #409EFF;
  color: #fff;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: bold;
  z-index: 1;
}

.avatar-placeholder {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  background: #e0e0e0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48px;
  color: #999;
}

.student-details {
  width: 100%;
  margin-bottom: 30px;
}

.info-item {
  display: flex;
  flex-direction: column;
  margin-bottom: 20px;
  text-align: left;
}

.info-label {
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
}

.info-value {
  font-size: 18px;
  color: #409EFF; /* 蓝色高亮 */
  font-weight: bold; /* 加粗 */
}

.question-numbers-section {
  width: 100%;
  flex: 1;
  overflow-y: auto;
  margin-bottom: 20px;
}

.question-group {
  margin-bottom: 25px;
}

.section-title {
  font-size: 16px;
  font-weight: bold;
  color: #333;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #e0e0e0;
}

.question-numbers {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.question-number {
  display: inline-block;
  width: 36px;
  height: 36px;
  line-height: 36px;
  text-align: center;
  background: #f0f0f0;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  color: #333;
  transition: all 0.3s;
}

/* 当前题目 - 蓝色边框 */
.question-number-current {
  background: #ecf5ff;
  border-color: #409EFF;
  color: #409EFF;
  font-weight: bold;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}

/* 已作答 - 绿色背景（不禁用，保持正常显示） */
.question-number-answered {
  background: #67C23A;
  border-color: #67C23A;
  color: #fff;
}

/* 已过期且已作答 - 保持绿色背景 */
.question-number-expired.question-number-answered {
  background: #67C23A;
  border-color: #67C23A;
  color: #fff;
}

/* 已过期未作答 - 灰色 */
.question-number-expired:not(.question-number-answered) {
  background: #f5f5f5;
  border-color: #ddd;
  color: #c0c4cc;
}

.volume-control {
  position: absolute;
  bottom: 80px;
  left: 50%;
  transform: translateX(-50%);
  width: calc(100% - 40px);
  max-width: 160px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-top: 10px;
  border-top: 1px solid #e0e0e0;
}

.volume-slider {
  width: 100%;
}

.volume-info {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 14px;
  color: #666;
}

.volume-info i {
  font-size: 18px;
}

/* 音频频率动画（页面最底部） */
.audio-frequency-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  width: 100%;
  height: 60px;
  background-color: #409EFF; /* 蓝色背景 */
  z-index: 1000;
}

.frequency-canvas {
  width: 100%;
  height: 100%;
  display: block;
}
</style>
