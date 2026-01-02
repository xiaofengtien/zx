<template>
  <div class="section-list-container">
    <!-- 中场遮罩 -->
    <div v-if="showIntermission" class="intermission-overlay">
      <div class="intermission-content">
        <div class="intermission-audio-indicator">
          <span class="pulse-dot"></span>
          <span class="pulse-text">{{ intermissionAudioPlaying ? '音频播放中' : '中场休息' }}</span>
        </div>
        <div class="intermission-text" v-html="intermissionText"></div>
        <el-button
          v-if="intermissionCanSkip"
          class="intermission-skip"
          type="primary"
          @click="skipIntermission"
        >
          跳过中场，继续下一卷
        </el-button>
      </div>
      <audio
        ref="intermissionAudioPlayer"
        :src="intermissionAudioPath"
        @ended="onIntermissionAudioEnded"
        @error="onIntermissionAudioError"
      ></audio>
    </div>

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
        <div class="header-top-row">
          <!-- 大题标题 -->
          <div class="header-section-title">{{ currentPlayingSectionName || currentSectionName }}</div>
          
          <!-- 题号信息（仅在显示题目时显示） -->
          <div v-if="showQuestions" class="header-question-info">第 {{ currentQuestionIndex + 1 }} 题 / 共 {{ allQuestions.length }} 题</div>
          
          <!-- 倒计时（仅在显示题目且大题音频未播放时显示） -->
          <div v-if="answerCountdown > 0 && showQuestions && !sectionAudioPlaying" class="header-countdown" :class="{ 'countdown-warning': answerCountdown <= 5 }">
            <span class="countdown-number">{{ answerCountdown }}</span>
            <span class="countdown-text">秒</span>
          </div>
          
          <!-- 音频播放标识 -->
          <div v-if="questionAudioPlaying || sectionAudioPlaying" class="header-audio-indicator">
            <i class="el-icon-headset"></i>
            <span>播放中</span>
          </div>
        </div>
        
        <!-- 大题说明文案 -->
        <!-- 大题说明文案：仅在非题目组模式下显示，避免与题目组说明重复 -->
        <div v-if="currentInstructionText && !isInQuestionGroup" class="header-instruction-text">{{ currentInstructionText }}</div>
      </div>

      <!-- 大题音频播放时显示（空白区域，等待音频播放完成） -->
      <div v-if="!showQuestions" class="section-audio-playing">
        <!-- 空白区域，题目内容在音频播放完成后才显示 -->
      </div>

      <!-- 题目显示区域（音频播放完成后显示） -->
      <!-- 题目显示区域（音频播放完成后显示） -->
      <div v-if="showQuestions && (currentQuestion || isInQuestionGroup)" class="questions-wrapper">
        
        <!-- 题目组说明区域 -->
        <div v-if="isInQuestionGroup && currentQuestionGroup" class="group-intro-container">
<!--           <div class="group-name">{{ currentQuestionGroup.group_name }}</div>-->
           <div class="group-text" v-if="currentQuestionGroup.group_name || currentQuestionGroup.groupName">{{ currentQuestionGroup.group_name || currentQuestionGroup.groupName }}</div>
           <div class="group-text" v-if="currentQuestionGroup.text">{{ currentQuestionGroup.text }}</div>
        </div>

        <!-- 题目内容区域（可滚动，显示多题） -->
        <div class="question-content-wrapper">
          <!-- 显示当前题目及其后续的4题（共5题） -->
          <div 
            v-for="(question, index) in getVisibleQuestions()" 
            :key="question.id || index"
            class="question-container" 
            :class="{ 
              'question-expired': isQuestionExpired(getQuestionGlobalIndex(index)),
              'question-current': !isInQuestionGroup && getQuestionGlobalIndex(index) === currentQuestionIndex
            }"
            :ref="getQuestionGlobalIndex(index) === currentQuestionIndex ? 'currentQuestionElement' : null"
          >
            <div class="question-header-inline">
              <span v-if="!isInQuestionGroup && getQuestionGlobalIndex(index) === currentQuestionIndex" class="current-arrow-indicator">▶</span>
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
              v-for="(item, qIndex) in getQuestionNumbers(section.id)" 
              :key="item.id"
              class="question-number"
              :class="{
                'question-number-current': isCurrentQuestion(section.id, qIndex),
                'question-number-answered': isQuestionAnswered(item.id),
                'question-number-expired': isQuestionExpiredBySection(section.id, qIndex)
              }"
            >
              {{ item.no }}
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

    <!-- 题目组音频播放器 -->
    <audio
      ref="groupAudioPlayer"
      :src="groupAudioPath"
      @ended="onGroupAudioEnded"
      @error="onGroupAudioError"
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
// 使用全局标记防止多次重写（导致日志重复输出）
if (!window.__consoleLogOverridden) {
  window.__consoleLogOverridden = true
  
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

// 单例控制：只允许一个活跃实例控制计时器
let activeInstanceId = null

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
      // 题目组相关
      questionGroups: [], // 当前大题下的题目组
      groupAudioPath: '', // 题目组音频路径
      groupAudioPlaying: false, // 题目组音频是否正在播放
      playedGroupAudios: new Set(), // 已播放过的题目组音频ID
      groupAudioTimer: null, // 题目组音频定时器
      processingTimeUp: false, // 防抖锁：是否正在处理时间结束逻辑
      isPlayingQuestionAudio: false, // 是否正在执行播放题目音频的逻辑（防重复调用）
      // 答题计时
      answerTime: 5, // 答题时间（秒，从大题设置获取）
      answerCountdown: 0, // 答题倒计时（秒）
      answerCountdownTimer: null, // 答题倒计时定时器
      // 题目状态管理
      expiredQuestionIndexes: [], // 已过期的题目索引列表
      answeredQuestions: {}, // 已答题记录 { questionIndex: answerId }
      answeredTimeSpent: {}, // 已答题用时记录 { questionIndex: timeSpent }
      // 卷别完成提示
      showVolumeComplete: false, // 是否显示卷别完成提示
      volumeCompleteText: '', // 完成提示文字
      isAllComplete: false, // 是否所有卷别都完成
      // 中场状态
      showIntermission: false,
      intermissionText: '',
      intermissionAudioPath: '',
      intermissionAudioPlaying: false,
      intermissionCanSkip: false,
      pendingNextVolumeId: null,
      pendingNextVolumeCode: null,
      intermissionAudioEndTimer: null
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
    },

    // 当前大题说明文案
    currentInstructionText() {
       let section = null;
       if (!this.showQuestions) {
           // 播放音频阶段，取 currentSectionIndex 对应的大题
           if (this.currentVolumeSections.length) {
               section = this.currentVolumeSections[this.currentSectionIndex];
           }
       } else {
           // 答题阶段，取当前题目所属的大题
           if (this.currentQuestion && this.currentVolumeSections.length) {
                 const questionSectionId = this.currentQuestion.sectionId || this.currentQuestion.section_id
                 if (questionSectionId) {
                    section = this.currentVolumeSections.find(s => s.id === questionSectionId)
                 }
           }
           // 如果找不到，默认显示第一大题
           if (!section && this.currentVolumeSections.length) {
               section = this.currentVolumeSections[0];
           }
       }
       return section?.instructionText || section?.instruction_text || ''
    },
    
    // 当前题目是否属于题目组
    isInQuestionGroup() {
      if (!this.currentQuestion) return false
      // 检查当前题目是否有关联的 group 信息
      const question = this.allQuestions[this.currentQuestionIndex]
      if (!question) return false
      return !!(question.group || question.groupId || question.group_id)
    },
    
    // 当前题目所属的题目组
    currentQuestionGroup() {
      if (!this.isInQuestionGroup) return null
      const question = this.allQuestions[this.currentQuestionIndex]
      if (!question) return null

      // 如果题目直接包含 group 对象
      if (question.group && typeof question.group === 'object') {
        return question.group
      }

      // 如果只有 groupId，从 questionGroups 中查找
      const groupId = question.groupId || question.group_id
      if (groupId && this.questionGroups.length > 0) {
        return this.questionGroups.find(g => g.id === groupId) || null
      }

      return null
    },

    // 当前题目组的所有题目
    currentGroupQuestions() {
      if (!this.isInQuestionGroup || !this.currentQuestionGroup) {
        return []
      }
      const group = this.currentQuestionGroup
      
      // 1. 优先：严格按照题目组定义的 Selected Question IDs 顺序显示
      // 这能纠正题目本身 sort_order 错乱的问题
      if (group.questionIds && group.questionIds.length > 0) {
          return group.questionIds.map(id => {
              // 宽松匹配 ID
              return this.allQuestions.find(q => (q.id || q.questionId || q.question_id) == id)
          }).filter(q => q)
      }

      const groupId = group.id
      const filterQs = this.allQuestions.filter(q => {
        const qGroupId = q.groupId || q.group_id || (q.group && q.group.id)
        return qGroupId === groupId
      })
      // 按 sort_order 或 id 排序
      return filterQs.sort((a, b) => {
        const orderA = a.sort_order || a.question_sort || a.id
        const orderB = b.sort_order || b.question_sort || b.id
        return orderA - orderB
      })
    },

  },
  beforeDestroy() {
    this.cleanupResources()
  },
  deactivated() {
    this.cleanupResources()
  },
  async mounted() {
    // ========== 调试标记 - 如果你看到这个alert，说明新代码已加载 ==========
    console.log('🔴🔴🔴 [DEBUG] NEW CODE LOADED - MOUNTED 🔴🔴🔴')
    
    // 生成唯一实例ID，设为活跃实例
    this._instanceId = Date.now() + Math.random()
    activeInstanceId = this._instanceId
    console.log(`🚀 [SectionList] mounted, instanceId=${this._instanceId}`)
    
    // 清理可能存在的旧状态
    this.cleanupResources()
    
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
    cleanupResources() {
        console.log('🧹 [SectionList] cleanupResources')
        this.stopAudioVisualization()
        
        // Clear all timers
        const timers = [
          'sectionAudioTimer', 
          'questionAudioTimer', 
          'groupAudioTimer', 
          'answerCountdownTimer', 
          'questionsLoadTimer', 
          'intermissionAudioEndTimer'
        ]
        timers.forEach(t => {
          if (this[t]) {
            clearTimeout(this[t])
            this[t] = null
          }
        })
        
        // Stop Audios
        const audios = ['sectionAudioPlayer', 'questionAudioPlayer', 'groupAudioPlayer', 'intermissionAudioPlayer']
        audios.forEach(refName => {
            const audio = this.$refs[refName]
            if (audio) {
                audio.pause()
                audio.src = ''
            }
        })
    
        this.isPlayingQuestionAudio = false
        this.processingTimeUp = false
        this.sectionAudioPlaying = false
        this.questionAudioPlaying = false
        this.groupAudioPlaying = false
    },
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
          
          // 修复答题时间异常：如果是恢复答题，重新调用 startExam 以重置 start_time 为当前时间
          // 避免出现 "19小时" 这种由于跨天/挂机导致的异常时长
          console.log(`🔄 [mounted] 恢复未提交的答题记录 ${this.paperInfoId}，重置计时器`)
          ipcRenderer.invoke('answer:startExam', {
            paperId: this.paperId,
            appUserId: userId
          }).catch(err => console.warn('重置答题计时器失败:', err))
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
        // 首先从 localStorage 获取 userInfo
        const userInfoStr = localStorage.getItem('userInfo')
        let userInfo = null
        if (userInfoStr) {
          userInfo = JSON.parse(userInfoStr)
        }

        // 优先从本地数据库获取学员档案（student_name 字段是真正的学员姓名）
        if (userInfo) {
          try {
            const userId = userInfo.user?.userId
            const account = userInfo.user?.userName
            let dbArchive = null
            
            if (userId) {
              dbArchive = await ipcRenderer.invoke('archive:getByUserId', userId)
            }
            if (!dbArchive && account) {
              dbArchive = await ipcRenderer.invoke('archive:getByAccount', account)
            }
            
            if (dbArchive) {
              // 从数据库获取学员信息（student_name 是姓名，student_account 是学号）
              this.studentName = String(dbArchive.student_name || dbArchive.studentName || '')
              this.studentId = String(dbArchive.student_account || dbArchive.studentAccount || account || '')
              this.seatNumber = String(dbArchive.seat_number || dbArchive.seatNumber || '')
              this.avatarNumber = dbArchive.avatar_number || dbArchive.avatarNumber || null
              console.log('✓ 从本地数据库获取学生信息:', { name: this.studentName, id: this.studentId, seat: this.seatNumber })
            }
          } catch (error) {
            console.warn('从本地数据库获取学员档案失败:', error)
          }
        }

        // 如果数据库没有数据，使用 localStorage 作为 fallback
        if (!this.studentName && userInfo) {
          const user = userInfo.user || {}
          const archive = userInfo.archive || {}
          this.studentName = String(archive.student_name || archive.studentName || '未知')
          this.studentId = String(archive.student_account || archive.studentAccount || user.userName || '未知')
          this.seatNumber = String(archive.seat_number || archive.seatNumber || '')
          this.avatarNumber = archive.avatar_number || archive.avatarNumber || null
          console.log('✓ 从 localStorage 获取学生信息:', { name: this.studentName, id: this.studentId, seat: this.seatNumber })
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
            answer_time: s.answerTime || s.answer_time || 5,
            instruction_text: s.instructionText || s.instruction_text || ''
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
              .sort((a, b) => (a.id || 0) - (b.id || 0))
              .map((q, index) => ({ no: index + 1, id: q.id })) // 使用对象存储序号和ID
            
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
        console.log(`📦 [loadAndPlaySectionAudio] 开始加载大题音频，sectionId=${section.id}, sectionName=${section.section_name}, Order=${section.section_order}`)
        
        // 获取大题的题目音频播放次数和答题时间设置
        this.questionAudioPlayCount = section.audio_play_count || section.audioPlayCount || 1
        this.answerTime = section.answer_time || section.answerTime || 5
        
        // 获取试卷包数据
        const paperData = await ipcRenderer.invoke('paper:getPaperData', this.paperId)
        if (!paperData || !paperData.manifest) {
          console.warn('⚠️ [loadAndPlaySectionAudio] 无法加载试卷数据')
          this.playNextSectionAudio()
          return
        }
        
        const mediaDir = paperData.mediaDir || ''
        const manifest = paperData.manifest
        
        // 查找对应的大题
        let matchedSection = null
        if (manifest.sections && Array.isArray(manifest.sections)) {
          matchedSection = manifest.sections.find(s => {
            return s.id === section.id || 
                   s.section_id === section.id ||
                   String(s.sectionName || s.section_name || '') === String(section.section_name || section.sectionName || '')
          })
        }
        
        // 获取音频路径（允许为空，为空时尝试智能扫描）
        const audioPath = matchedSection ? (matchedSection.instructionAudioPath || matchedSection.instruction_audio_path || '') : ''
        
        if (!audioPath) {
          console.warn(`⚠️ [loadAndPlaySectionAudio] Manifest中未找到音频路径，将尝试智能扫描本地目录`)
        }
        
        // 1. 尝试通过路径加载 (如果路径存在)
        let audioFilePath = null
        if (audioPath) {
          // 处理 media/ 和 sections/ 前缀
          if (audioPath.startsWith('media/')) {
            const relativePath = audioPath.replace(/^media\//, '')
            audioFilePath = path.join(mediaDir, relativePath)
          } else if (audioPath.startsWith('sections/')) {
            audioFilePath = path.join(mediaDir, 'sections', path.basename(audioPath))
          } else {
             audioFilePath = path.join(mediaDir, 'sections', path.basename(audioPath))
          }
          
          if (audioFilePath && fs.existsSync(audioFilePath)) {
             console.log(`📦 [loadAndPlaySectionAudio] 通过Manifest路径找到文件: ${audioFilePath}`)
          } else {
             console.warn(`⚠️ [loadAndPlaySectionAudio] Manifest路径指向的文件不存在: ${audioFilePath || audioPath}, 转为扫描`)
             audioFilePath = null // 置空以便后续扫描
          }
        }

        // 2. 如果Manifest路径失效或为空，执行智能扫描
        if (!audioFilePath) {
           const sectionInstructionDir = path.join(mediaDir, 'sections')
           if (fs.existsSync(sectionInstructionDir)) {
             const files = fs.readdirSync(sectionInstructionDir)
               .filter(file => /\.(mp3|wav|m4a|aac|ogg)$/i.test(file))
               .sort() // 字母顺序排序: intro_01, intro_02...

             if (files.length > 0) {
                // 策略A: 尝试通过 section_order 匹配文件名 (如 order=1 匹配 "01")
                // 补零处理: 1 -> "01", 1 -> "1"
                const order = section.section_order || (this.currentSectionIndex + 1);
                const orderStr = String(order)
                const orderPad = orderStr.padStart(2, '0')
                
                // 查找文件名中包含 _01_ 或 _1_ 或 01.mp3 的文件
                let match = files.find(f => f.includes(`_${orderPad}_`) || f.includes(`_${orderStr}_`) || f.includes(`intro_${orderPad}`));
                
                if (match) {
                   audioFilePath = path.join(sectionInstructionDir, match)
                   console.log(`📦 [loadAndPlaySectionAudio] 智能匹配(策略A-序号): Order=${order} -> ${match}`)
                } else {
                   // 策略B: 降级为按索引匹配 (当前是第几个大题，就用第几个文件)
                   // 注意：这假设 sortedFiles 和 currentVolumeSections 顺序一致
                   if (this.currentSectionIndex < files.length) {
                      const fileByIndex = files[this.currentSectionIndex]
                      audioFilePath = path.join(sectionInstructionDir, fileByIndex)
                      console.log(`📦 [loadAndPlaySectionAudio] 智能匹配(策略B-索引): Index=${this.currentSectionIndex} -> ${fileByIndex}`)
                   } else {
                      // 策略C: 无论如何取第一个 (保底，但可能错误)
                      audioFilePath = path.join(sectionInstructionDir, files[0])
                      console.warn(`⚠️ [loadAndPlaySectionAudio] 智能匹配(策略C-首个): 无法精确匹配，使用第一个文件 -> ${files[0]}`)
                   }
                }
             }
           }
        }

        // 3. 执行加载
        if (audioFilePath && fs.existsSync(audioFilePath)) {
           // 加载逻辑
           const success = await this.loadAudioFileContent(audioFilePath)
           if (success) {
              this.$nextTick(() => { this.playSectionAudio() })
           } else {
              setTimeout(() => { this.playNextSectionAudio() }, 500)
           }
        } else {
           console.warn(`❌ [loadAndPlaySectionAudio] 最终无法找到任何可用音频，跳过`)
           setTimeout(() => { this.playNextSectionAudio() }, 500)
        }

      } catch (error) {
        console.error('❌ [loadAndPlaySectionAudio] 流程异常:', error)
        setTimeout(() => { this.playNextSectionAudio() }, 500)
      }
    },

    // 提取加载文件内容的通用方法
    async loadAudioFileContent(filePath) {
        try {
            const fileBuffer = fs.readFileSync(filePath)
            const ext = path.extname(filePath).toLowerCase()
            let mimeType = 'audio/mpeg'
            if (ext === '.wav') mimeType = 'audio/wav'
            else if (ext === '.m4a' || ext === '.aac') mimeType = 'audio/mp4'
            else if (ext === '.ogg') mimeType = 'audio/ogg'
            
            const base64 = fileBuffer.toString('base64')
            this.currentSectionAudioPath = `data:${mimeType};base64,${base64}`
            return true
        } catch(e) {
            console.warn(`读取文件失败转用file协议: ${e.message}`)
            this.currentSectionAudioPath = pathToFileURL(filePath).href
            return true
        }
    },

    playSectionAudio() {
      if (!this.currentSectionAudioPath) {
        console.warn('⚠️ [playSectionAudio] 没有大题音频，跳到下一个')
        setTimeout(() => {
          this.playNextSectionAudio()
        }, 500)
        return
      }

      const audio = this.$refs.sectionAudioPlayer
      if (!audio) {
        console.warn('⚠️ [playSectionAudio] 音频元素不存在，跳到下一个')
        setTimeout(() => {
          this.playNextSectionAudio()
        }, 500)
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
          }, 500)
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
            }, 500)
          }
        }, 10000)
      }
    },

    playNextSectionAudio() {
      // 单例检查：只允许活跃实例执行
      if (activeInstanceId !== this._instanceId) {
          console.warn(`⚠️ [playNextSectionAudio] 非活跃实例，跳过`)
          return
      }
      
      // 🔥 调试：打印当前状态
      console.log(`📊 [playNextSectionAudio] 当前状态: showQuestions=${this.showQuestions}, loading=${this.loading}, sectionAudioPlaying=${this.sectionAudioPlaying}, sectionIndex=${this.currentSectionIndex}`)
      
      // 如果题目已加载或正在加载，说明这是当前大题的延续，不需要重新加载
      // 但如果是新大题，showQuestions 应该已经被重置为 false
      if (this.showQuestions || this.loading) {
        console.log('⚠️ [playNextSectionAudio] 题目已加载或正在加载，跳过')
        return
      }
      
      // 清除当前定时器
      if (this.sectionAudioTimer) {
        clearTimeout(this.sectionAudioTimer)
        this.sectionAudioTimer = null
      }
      
      // 停止当前音频
      this.currentSectionAudioPath = ''
      const audio = this.$refs.sectionAudioPlayer
      if (audio) {
        audio.pause()
        audio.src = ''
      }
      this.sectionAudioPlaying = false
      this.stopAudioVisualization()
      
      // 修改交互逻辑：大题导语播放完成后，直接进入该大题的作答（加载题目）
      // 而不是播放下一个大题的导语
      console.log(`✓ [playNextSectionAudio] 大题音频播放完成（或跳过），开始加载题目: sectionIndex=${this.currentSectionIndex}`)
      
      // 使用一次性定时器，防止重复调用
      if (!this.questionsLoadTimer) {
        this.questionsLoadTimer = setTimeout(() => {
          this.questionsLoadTimer = null
          this.loadAllQuestions()
        }, 500) // 缩短等待时间，提升流畅度
      }
    },

    async loadAllQuestions() {
      // 单例检查：只允许活跃实例执行
      if (activeInstanceId !== this._instanceId) {
          console.warn(`⚠️ [loadAllQuestions] 非活跃实例，跳过`)
          return
      }
      
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

          // 加载题目组信息
          const groups = await ipcRenderer.invoke('paper:getQuestionGroups', this.paperId, currentSection.id) || []
          this.questionGroups = groups.map(g => {
            let ids = []
            try {
              if (g.selected_question_ids || g.selectedQuestionIds) {
                const s = g.selected_question_ids || g.selectedQuestionIds
                ids = JSON.parse(typeof s === 'string' ? s : JSON.stringify(s))
                // 可能是 "[1, 2]" 字符串，也可能是数组
                if (typeof ids === 'string') {
                   ids = ids.replace('[', '').replace(']', '').split(',').map(i => Number(i.trim()))
                }
              }
            } catch (e) { console.warn('解析题目组ID失败', e) }
            return {
              ...g,
              id: g.id || g.questionGroupId || g.question_group_id, // 确保ID存在
              questionIds: Array.isArray(ids) ? ids : []
            }
          })
          console.log(`📦 [loadAllQuestions] 加载到 ${this.questionGroups.length} 个题目组`)

          // 将题目与题目组关联
          questions.forEach(q => {
             const qId = q.id || q.question_id || q.questionId
             const group = this.questionGroups.find(g => g.questionIds.includes(qId) || g.questionIds.includes(Number(qId)))
             if (group) {
               q.group = group
             }
          })
          
          // 处理题目数据的文本字段
          const processedQuestions = processTextFields(questions)
          
          // 按 question_sort 或 sort_order 排序
          const sortedQuestions = processedQuestions.sort((a, b) => {
            return (a.id || 0) - (b.id || 0)
          })
          
          // 过滤无效题目
          // 过滤无效题目 (Must have ID)
          const validQuestions = sortedQuestions.filter(q => q && (q.id || q.questionId || q.question_id))
          console.log(`📦 [loadAllQuestions] 有效题目数量: ${validQuestions.length} (原始: ${sortedQuestions.length})`)
          if (validQuestions.length > 0) {
             console.log(`📦 [loadAllQuestions] 第1题示例: ID=${validQuestions[0].id}, Group=${validQuestions[0].group ? 'Yes' : 'No'}`)
          }

          // 添加到列表
          allQuestionsList.push(...validQuestions)
        } catch (error) {
          console.error(`获取大题 ${currentSection.id} 的题目失败:`, error)
        }
        
        console.log(`✓ [loadAllQuestions] 共加载 ${allQuestionsList.length} 个题目`)
        
        if (allQuestionsList.length === 0) {
          console.warn('⚠️ [loadAllQuestions] 没有题目可显示，自动跳到下一个')
          this.loading = false
          this.handleSectionComplete()
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
      
      // 🔥 关键修复：大题音频播放完成后，强制重置状态
      // 这确保即使从上一个大题遗留了 showQuestions=true 的状态，也能正确加载新大题的题目
      console.log(`📊 [onSectionAudioEnded] 重置前状态: showQuestions=${this.showQuestions}, loading=${this.loading}`)
      this.showQuestions = false
      this.loading = false
      console.log(`📊 [onSectionAudioEnded] 重置后状态: showQuestions=${this.showQuestions}, loading=${this.loading}`)
      
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
      }, 500)
    },

    // ==================== 题目音频播放和答题计时相关方法 ====================
    
    /**
     * 播放当前题目的音频
     */
    async playCurrentQuestionAudio() {
      // 单例检查：只允许活跃实例执行
      if (activeInstanceId !== this._instanceId) {
          console.warn(`⚠️ [playCurrentQuestionAudio] 非活跃实例，跳过`)
          return
      }
      
      // 防重复调用：如果正在执行播放逻辑，直接返回
      if (this.isPlayingQuestionAudio) {
        console.warn(`⚠️ [playCurrentQuestionAudio] 正在执行播放逻辑，跳过重复调用，当前题目索引=${this.currentQuestionIndex + 1}`)
        return
      }
      
      const question = this.allQuestions[this.currentQuestionIndex]
      if (!question) {
        console.error(`❌ [playCurrentQuestionAudio] 当前题目不存在(Index=${this.currentQuestionIndex})，停止播放逻辑，不启动倒计时`)
        // 重置状态，避免卡死
        this.isPlayingQuestionAudio = false
        return
      }

      // 1. 检查是否属于题目组
      if (this.isInQuestionGroup) {
        const group = this.currentQuestionGroup
        // 如果该题目组有音频，且尚未播放过
        if ((group.audioUrl || group.audioPath || group.audio_url || group.audio_path) && !this.playedGroupAudios.has(group.id)) {
           console.log(`🎵 [playCurrentQuestionAudio] 检测到题目组音频，优先播放题目组音频: ${group.group_name}`)
           this.playGroupAudio(group)
           return
        }
        // 如果题目组音频已播放，或没有音频，则按照"同时作答"逻辑，直接开始倒计时
        // 注意：题目组模式下，通常不个别播放题目音频，而是播放完组音频后所有题目一起答
        // 除非题目本身有音频（目前假设优先组音频，组音频播完进入答题）
        
        // 重置标志
        this.isPlayingQuestionAudio = false
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
     * 播放题目组音频
     */
    async playGroupAudio(group) {
        try {
            const audioPath = group.audioPath || group.audio_path || group.audioUrl || group.audio_url
            if (!audioPath) {
                console.warn('题目组没有音频路径')
                this.playedGroupAudios.add(group.id)
                this.isPlayingQuestionAudio = false
                this.startAnswerCountdown()
                return
            }

            // 获取试卷包数据以解析路径
            const paperData = await ipcRenderer.invoke('paper:getPaperData', this.paperId)
            const mediaDir = paperData?.mediaDir || ''
            
            let finalPath = ''
            // 简单路径处理逻辑 (类似 getQuestionAudioFilePath)
            if (audioPath.startsWith('http')) {
               finalPath = audioPath
            } else {
               // 尝试本地路径
               const possiblePath = path.join(mediaDir, audioPath)
               if (fs.existsSync(possiblePath)) {
                   finalPath = pathToFileURL(possiblePath).href
               } else {
                   // 尝试拼接
                   const p2 = path.join(mediaDir, 'sections', audioPath) // 假设在 sections 目录下
                   if (fs.existsSync(p2)) finalPath = pathToFileURL(p2).href
               }
            }

            if (!finalPath) {
                 console.warn('无法解析题目组音频路径:', audioPath)
                 this.playedGroupAudios.add(group.id)
                 this.isPlayingQuestionAudio = false
                 this.startAnswerCountdown()
                 return
            }

            this.groupAudioPath = finalPath
            console.log(`🎵 [playGroupAudio] 播放题目组音频: ${finalPath}`)
            
            this.$nextTick(() => {
                const audio = this.$refs.groupAudioPlayer
                if (audio) {
                    audio.volume = this.volume / 100
                    audio.play().then(() => {
                        this.groupAudioPlaying = true
                        this.initAudioVisualization(audio)
                    }).catch(e => {
                        console.error('播放题目组音频失败', e)
                        this.onGroupAudioError()
                    })
                } else {
                    this.onGroupAudioError()
                }
            })

        } catch (e) {
            console.error('准备题目组音频失败', e)
            this.onGroupAudioError()
        }
    },

    onGroupAudioEnded() {
        // 🔥 如果没有设置题目组音频路径，忽略这个事件
        if (!this.groupAudioPath) {
            return
        }
        
        console.log('✓ [onGroupAudioEnded] 题目组音频播放结束')
        this.groupAudioPlaying = false
        if (this.currentQuestionGroup) {
            this.playedGroupAudios.add(this.currentQuestionGroup.id)
        }
        // 音频结束后，开始答题倒计时
        this.isPlayingQuestionAudio = false // 解锁
        
        // 🔥 安全检查：只有在题目已显示且大题音频未播放时才启动倒计时
        if (this.showQuestions && !this.sectionAudioPlaying && this.allQuestions?.length > 0) {
            this.startAnswerCountdown()
        }
    },

    onGroupAudioError() {
        // 🔥 关键修复：如果没有设置题目组音频路径，忽略这个错误事件
        // 因为空的 <audio src=""> 会自动触发 error 事件
        if (!this.groupAudioPath) {
            console.log('⚠️ [onGroupAudioError] 忽略空音频路径的错误事件')
            return
        }
        
        console.warn('⚠️ [onGroupAudioError] 题目组音频播放出错，跳过')
        this.groupAudioPlaying = false
        if (this.currentQuestionGroup) {
            this.playedGroupAudios.add(this.currentQuestionGroup.id)
        }
        this.isPlayingQuestionAudio = false
        
        // 🔥 安全检查：只有在题目已显示且大题音频未播放时才启动倒计时
        if (this.showQuestions && !this.sectionAudioPlaying && this.allQuestions?.length > 0) {
            this.startAnswerCountdown()
        } else {
            console.log('⚠️ [onGroupAudioError] 条件不满足，跳过启动倒计时:', {
                showQuestions: this.showQuestions,
                sectionAudioPlaying: this.sectionAudioPlaying,
                allQuestionsLength: this.allQuestions?.length
            })
        }
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
            const fallbackTimeout = (audio.duration + 0.5) * 1000
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
        }, 500)
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
      // 🔥 调试：打印调用栈，找出是谁在调用这个函数
      console.log('⏱️ [startAnswerCountdown] 被调用，调用栈:', new Error().stack.split('\n').slice(1, 4).join('\n'))
      console.log('⏱️ [startAnswerCountdown] 当前状态:', {
        sectionAudioPlaying: this.sectionAudioPlaying,
        showQuestions: this.showQuestions,
        allQuestionsLength: this.allQuestions?.length,
        currentQuestionIndex: this.currentQuestionIndex,
        instanceId: this._instanceId,
        activeInstanceId: activeInstanceId
      })
      
      // 单例检查：如果不是活跃实例，直接返回
      if (activeInstanceId !== this._instanceId) {
          console.warn(`⚠️ [startAnswerCountdown] 非活跃实例(${this._instanceId})，跳过`)
          return
      }
      
      // 🔥 严防：大题音频播放时绝不启动倒计时
      if (this.sectionAudioPlaying) {
          console.warn('⚠️ [startAnswerCountdown] 大题音频正在播放，跳过题目倒计时')
          return
      }
      
      // 🔥 新增：题目未显示时不启动倒计时
      if (!this.showQuestions) {
          console.warn('⚠️ [startAnswerCountdown] 题目未显示，跳过题目倒计时')
          return
      }
      
      // 🔥 新增：没有题目时不启动倒计时
      if (!this.allQuestions || this.allQuestions.length === 0) {
          console.warn('⚠️ [startAnswerCountdown] 没有题目，跳过题目倒计时')
          return
      }
      
      // 清除之前的定时器
      if (this.answerCountdownTimer) {
        clearInterval(this.answerCountdownTimer)
        this.answerCountdownTimer = null
      }
      
      this.answerCountdown = this.answerTime
      
      // 如果在题目组中，且题目组有独立的答题时间设置，优先使用
      if (this.isInQuestionGroup && this.currentQuestionGroup) {
          if (this.currentQuestionGroup.answerTime || this.currentQuestionGroup.answer_time) {
              const groupTime = this.currentQuestionGroup.answerTime || this.currentQuestionGroup.answer_time
              // 确保时间合理
              if (groupTime > 0) {
                  this.answerCountdown = groupTime
              }
          }
      }

      console.log(`⏱️ [startAnswerCountdown] 开始答题倒计时: ${this.answerCountdown} 秒`)
      
      // 每秒更新倒计时
      this.answerCountdownTimer = setInterval(() => {
        // 每次回调都检查是否仍为活跃实例
        if (activeInstanceId !== this._instanceId) {
            console.warn(`⚠️ [Timer] 非活跃实例，终止定时器`)
            clearInterval(this.answerCountdownTimer)
            this.answerCountdownTimer = null
            return
        }
        
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
      
      // 如果在题目组中，只显示当前题目组的题目（同时显示）
      if (this.isInQuestionGroup) {
          return this.currentGroupQuestions
      }

      // 如果是普通题目，显示所有（或者根据需要分页/切片）
      // 根据用户之前的代码逻辑，似乎是显示所有（列表式）
      return this.allQuestions
    },
    
    /**
     * 获取题目在全局列表中的索引
     */
    getQuestionGlobalIndex(visibleIndex) {
      // calculate the global index based on visibleIndex
      if (this.isInQuestionGroup) {
          // 在题目组模式下，visibleIndex 是相对于 currentGroupQuestions 的索引
          // 需要找到该题目在 allQuestions 中的索引
          // 假设题目ID是唯一的
          const question = this.currentGroupQuestions[visibleIndex]
          if (question) {
              const globalIndex = this.allQuestions.findIndex(q => {
                  const qId = q.id || q.questionId || q.question_id
                  const targetId = question.id || question.questionId || question.question_id
                  return qId === targetId
              })
              return globalIndex !== -1 ? globalIndex : visibleIndex
          }
      }
      // 非分组模式下，显示的是 allQuestions，所以 visibleIndex == globalIndex
      return visibleIndex
    },
    
    /**
     * 滚动到当前题目
     */
    scrollToCurrentQuestion() {
      this.$nextTick(() => {
        const currentElement = this.$refs.currentQuestionElement
        if (currentElement && currentElement[0]) {
          // 使用 scrollIntoView({ block: 'nearest' }) 
          // 仅在元素不可见时才滚动，解决"滑动频率太多"的问题
          // 同时也能确保元素显示在可视区域内，不会被遮挡
          currentElement[0].scrollIntoView({
            behavior: 'smooth',
            block: 'nearest'
          })
        }
      })
    },
    
    /**
     * 答题时间结束，自动跳到下一题
     */
    async onAnswerTimeUp() {
      if (this.processingTimeUp) return
      this.processingTimeUp = true
      
      try {
        console.log(`⏱️ [onAnswerTimeUp] 答题时间结束`)

      // 1. 处理题目组逻辑
      if (this.isInQuestionGroup) {
          const groupQs = this.currentGroupQuestions
          console.log(`⏱️ [onAnswerTimeUp] 题目组作答结束，共 ${groupQs.length} 题`)
          
          for (const q of groupQs) {
             const idx = this.allQuestions.findIndex(aq => aq.id === q.id)
             if (idx > -1) {
                 if (!this.expiredQuestionIndexes.includes(idx)) {
                     this.expiredQuestionIndexes.push(idx)
                 }
                 // 保存未作答结果 (如果已作答，内部会跳过)
                 await this.saveQuestionResultByIndex(idx)
             }
          }

          // 停止音频
          this.isPlayingQuestionAudio = false
          this.groupAudioPlaying = false
          if (this.$refs.groupAudioPlayer) this.$refs.groupAudioPlayer.pause()
          
          // 计算跳转
          const lastQ = groupQs[groupQs.length - 1]
          const lastIdx = this.allQuestions.findIndex(q => q.id === lastQ.id)
          
          if (lastIdx === -1) {
             console.error('无法找到题目组最后一题索引')
             return
          }

          const isLastQuestionOfSection = lastIdx >= this.allQuestions.length - 1 || 
            (this.allQuestions[lastIdx + 1] && (this.allQuestions[lastIdx + 1].sectionId !== this.allQuestions[lastIdx].sectionId))
             
          if (isLastQuestionOfSection) {
             console.log('✓ [onAnswerTimeUp] 题目组完成，进入下一大题')
             this.loadNextSection()
          } else {
             console.log('✓ [onAnswerTimeUp] 题目组完成，进入下一题')
             this.currentQuestionIndex = lastIdx + 1
             this.scrollToCurrentQuestion()
             this.loadQuestion(this.allQuestions[this.currentQuestionIndex])
             setTimeout(() => {
                this.playCurrentQuestionAudio()
             }, 200)
          }
          return
      }

      // 2. 原有单题逻辑
      console.log(`⏱️ [onAnswerTimeUp] 单题 ${this.currentQuestionIndex + 1} 结束`)
      
      // 将当前题目标记为已过期
      if (!this.expiredQuestionIndexes.includes(this.currentQuestionIndex)) {
        this.expiredQuestionIndexes.push(this.currentQuestionIndex)
      }
      
      // 保存结果
      await this.saveQuestionResultByIndex(this.currentQuestionIndex)
      
      // 停止音频
      this.questionAudioPath = ''
      this.questionAudioPlaying = false
      this.isPlayingQuestionAudio = false
      if (this.questionAudioTimer) {
        clearTimeout(this.questionAudioTimer)
        this.questionAudioTimer = null
      }
      const audio = this.$refs.questionAudioPlayer
      if (audio) {
        audio.pause()
        audio.src = ''
      }
      this.stopAudioVisualization()
      
      // 检查是否是当前大题的最后一题
      const currentQuestion = this.allQuestions[this.currentQuestionIndex]
      if (!currentQuestion) {
          console.error('当前题目丢失')
          return
      }
      const currentSectionId = currentQuestion.sectionId || currentQuestion.section_id
      
      const isLastQuestionOfSection = this.currentQuestionIndex >= this.allQuestions.length - 1 ||
        (this.allQuestions[this.currentQuestionIndex + 1] && 
         (this.allQuestions[this.currentQuestionIndex + 1].sectionId || this.allQuestions[this.currentQuestionIndex + 1].section_id) !== currentSectionId)
      
      if (isLastQuestionOfSection) {
        this.loadNextSection()
      } else {
        this.currentQuestionIndex++
        const nextQuestion = this.allQuestions[this.currentQuestionIndex]
        this.scrollToCurrentQuestion()
        this.loadQuestion(nextQuestion)
        console.log(`✓ [onAnswerTimeUp] 跳到下一题 ${this.currentQuestionIndex + 1}`)
        
        setTimeout(() => {
          this.playCurrentQuestionAudio()
        }, 200)
      }
      } finally {
        setTimeout(() => {
            this.processingTimeUp = false
        }, 500)
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
          }, 500)
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
     * 处理大题完成（当没有题目可显示时调用）
     * 不依赖 currentQuestion，直接使用 currentSectionIndex
     */
    handleSectionComplete() {
         console.log(`📦 [handleSectionComplete] 大题 ${this.currentSectionIndex} 无题目或已完成，尝试进入下一个`)
         
         if (this.currentSectionIndex < this.currentVolumeSections.length - 1) {
            // 进入下一个大题
            this.currentSectionIndex++
            console.log(`📦 [handleSectionComplete] 自动进入下一个大题 index: ${this.currentSectionIndex}`)
            
            this.showQuestions = false
            this.loading = true
            this.allQuestions = [] 
            
            const nextSection = this.currentVolumeSections[this.currentSectionIndex]
            setTimeout(() => {
                this.loadAndPlaySectionAudio(nextSection) // 使用这一方法播放音频并触发后续流程
            }, 500)
         } else {
             console.log('✓ [handleSectionComplete] 卷别所有大题完成')
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
      }, 500)
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
            
            // 隐藏完成提示
            this.showVolumeComplete = false

            // 优先使用 manifest 中的中场配置，其次 SQLite（paper_intermission）
            console.log('🔍 [intermission-match] currentVolumeId:', this.currentVolumeId, 'currentVolumeCode:', this.currentVolumeCode)
            console.log('🔍 [intermission-match] nextVolume:', {
              id: nextVolume?.id,
              code: nextVolumeCode,
              name: nextVolume?.volumeName || nextVolume?.volume_name
            })
            const intermission = await this.findIntermissionConfig({ paperData, nextVolume, nextVolumeCode })
            console.log('🔍 [intermission-match] matched intermission:', intermission)

            if (intermission) {
              // 命中中场配置，跳转到 /intermission 路由（保留新匹配逻辑）
              const targetVolumeId = intermission.toVolumeId ?? intermission.to_volume_id ?? nextVolume?.id ?? null
              const targetVolumeCode = intermission.toVolume ?? intermission.to_volume ?? nextVolumeCode
              const fromParam = intermission.fromVolume ?? intermission.from_volume ?? this.currentVolumeCode
              const toParam = intermission.toVolume ?? intermission.to_volume ?? targetVolumeCode
              const fromVolumeIdParam = intermission.fromVolumeId ?? intermission.from_volume_id ?? this.currentVolumeId ?? null
              const toVolumeIdParam = intermission.toVolumeId ?? intermission.to_volume_id ?? nextVolume?.id ?? null

              if (targetVolumeId) {
                localStorage.setItem('currentVolumeId', String(targetVolumeId))
              } else if (nextVolume?.id) {
                localStorage.setItem('currentVolumeId', String(nextVolume.id))
              }
              localStorage.setItem('currentVolumeCode', targetVolumeCode)

              this.$router.push({
                path: '/intermission',
                query: {
                  paperId: this.paperId,
                  paperInfoId: this.paperInfoId,
                  fromVolume: fromParam,
                  toVolume: toParam,
                  fromVolumeId: fromVolumeIdParam ? String(fromVolumeIdParam) : undefined,
                  toVolumeId: toVolumeIdParam ? String(toVolumeIdParam) : undefined
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
     * 解析中场配置（优先 manifest.intermissions，再回退 SQLite）
     */
    async findIntermissionConfig({ paperData, nextVolume, nextVolumeCode }) {
      const manifestIntermissions = paperData?.manifest?.intermissions || []
      const norm = (v) => (v === undefined || v === null) ? '' : String(v).trim()
      const volumes = paperData?.manifest?.volumes || []

      const currentVolumeInfo = volumes.find(v => {
        const vid = v.id || v.volumeId || v.volume_id
        const vcode = norm(v.volumeCode || v.volume_code)
        return (this.currentVolumeId && vid && Number(vid) === Number(this.currentVolumeId)) ||
          (vcode && norm(this.currentVolumeCode) && vcode === norm(this.currentVolumeCode))
      }) || null

      const nextVolumeInfo = volumes.find(v => {
        const vid = v.id || v.volumeId || v.volume_id
        const vcode = norm(v.volumeCode || v.volume_code)
        return (nextVolume?.id && vid && Number(vid) === Number(nextVolume.id)) ||
          (vcode && norm(nextVolumeCode) && vcode === norm(nextVolumeCode))
      }) || null

      const currentCode = norm(currentVolumeInfo?.volumeCode || currentVolumeInfo?.volume_code || this.currentVolumeCode)
      const currentName = norm(currentVolumeInfo?.volumeName || currentVolumeInfo?.volume_name)
      const nextCode = norm(nextVolumeCode || nextVolumeInfo?.volumeCode || nextVolumeInfo?.volume_code)
      const nextVolName = norm(nextVolumeInfo?.volumeName || nextVolumeInfo?.volume_name)

      console.log('🔍 [intermission-match] manifest volumes:', volumes.map(v => ({
        id: v.id || v.volumeId || v.volume_id,
        code: v.volumeCode || v.volume_code,
        name: v.volumeName || v.volume_name
      })))
      console.log('🔍 [intermission-match] manifest intermissions:', manifestIntermissions.map(i => ({
        fromVolumeId: i.fromVolumeId ?? i.from_volume_id,
        toVolumeId: i.toVolumeId ?? i.to_volume_id,
        fromVolume: i.fromVolume ?? i.from_volume,
        toVolume: i.toVolume ?? i.to_volume
      })))
      console.log('🔍 [intermission-match] currentVolume resolved:', {
        id: this.currentVolumeId,
        code: currentCode,
        name: currentName
      })
      console.log('🔍 [intermission-match] nextVolume resolved:', {
        id: nextVolume?.id || nextVolumeInfo?.id || nextVolumeInfo?.volumeId || nextVolumeInfo?.volume_id,
        code: nextCode,
        name: nextVolName
      })

      const tryMatch = (list) => {
        if (!Array.isArray(list)) return null
        return list.find(i => {
          const fromId = i.fromVolumeId ?? i.from_volume_id ?? null
          const toId = i.toVolumeId ?? i.to_volume_id ?? null
          const fromCode = norm(i.fromVolume ?? i.from_volume)
          const toCode = norm(i.toVolume ?? i.to_volume)
          const matchFromId = fromId && this.currentVolumeId && Number(fromId) === Number(this.currentVolumeId)
          const matchToId = toId && nextVolume?.id && Number(toId) === Number(nextVolume.id)
          const matchFromCode = fromCode && currentCode && fromCode === currentCode
          const matchToCode = toCode && nextCode && toCode === nextCode
          const matchFromName = fromCode && currentName && fromCode === currentName
          const matchToName = toCode && nextVolName && toCode === nextVolName

          const matchFrom = matchFromId || matchFromCode || matchFromName
          const matchTo = matchToId || matchToCode || matchToName
          return matchFrom && matchTo
        })
      }

      let intermission = tryMatch(manifestIntermissions)

      if (!intermission) {
        const intermissions = await ipcRenderer.invoke('paper:getIntermissions', this.paperId)
        intermission = tryMatch(intermissions)
      }

      return intermission || null
    },

    /**
     * 显示中场页面并加载音频
     */
    async showIntermissionPage(intermission, nextVolume, paperData) {
      try {
        this.showIntermission = true
        this.showVolumeComplete = false

        // 确定下一卷信息
        const targetVolumeId = intermission.toVolumeId ?? intermission.to_volume_id ?? nextVolume?.id ?? null
        const targetVolumeCode = intermission.toVolume ?? intermission.to_volume ?? (nextVolume?.volumeCode || nextVolume?.volume_code || '')
        this.pendingNextVolumeId = targetVolumeId || null
        this.pendingNextVolumeCode = targetVolumeCode || ''

        // 文本与可跳过标识
        this.intermissionText = intermission.intermissionText || intermission.intermission_text || ''
        this.intermissionCanSkip = Boolean(intermission.canSkip ?? intermission.can_skip)

        // 解析音频路径
        this.intermissionAudioPath = await this.resolveIntermissionAudioPath(intermission, paperData)

        // 自动播放（有音频时），否则直接进入完成逻辑
        if (this.intermissionAudioPath) {
          this.playIntermissionAudio()
        } else {
          this.intermissionAudioPlaying = false
          this.intermissionAudioEndTimer = setTimeout(() => {
            this.finishIntermission()
          }, 500)
        }
      } catch (error) {
        console.error('显示中场页面失败:', error)
        this.finishIntermission()
      }
    },

    /**
     * 解析中场音频路径，优先本地路径（manifest 提取的 intermission 目录），否则使用 URL
     */
    async resolveIntermissionAudioPath(intermission, paperData) {
      try {
        const audioPath = intermission.intermissionAudioPath || intermission.intermission_audio_path || ''
        const audioUrl = intermission.intermissionAudioUrl || intermission.intermission_audio_url || ''
        const mediaDir = paperData?.mediaDir || ''

        if (audioPath) {
          // 相对路径（通常在 mediaDir/intermission/ 下）
          const fullPath = path.join(mediaDir, audioPath.startsWith('intermission/') ? audioPath : path.join('intermission', audioPath))
          if (fs.existsSync(fullPath)) {
            return pathToFileURL(fullPath).href
          }
        }

        if (audioUrl && (audioUrl.startsWith('http://') || audioUrl.startsWith('https://'))) {
          return audioUrl
        }
      } catch (error) {
        console.warn('解析中场音频路径失败:', error)
      }
      return ''
    },

    playIntermissionAudio() {
      try {
        const audioEl = this.$refs.intermissionAudioPlayer
        if (!audioEl) {
          this.intermissionAudioPlaying = false
          this.finishIntermission()
          return
        }
        this.intermissionAudioPlaying = true
        audioEl.currentTime = 0
        const playPromise = audioEl.play()
        if (playPromise?.catch) {
          playPromise.catch(err => {
            console.warn('中场音频播放失败:', err)
            this.intermissionAudioPlaying = false
            this.intermissionAudioEndTimer = setTimeout(() => {
              this.finishIntermission()
            }, 500)
          })
        }
      } catch (error) {
        console.warn('中场音频播放异常:', error)
        this.intermissionAudioPlaying = false
        this.intermissionAudioEndTimer = setTimeout(() => {
          this.finishIntermission()
        }, 500)
      }
    },

    onIntermissionAudioEnded() {
      this.intermissionAudioPlaying = false
      this.intermissionAudioEndTimer = setTimeout(() => {
        this.finishIntermission()
      }, 500)
    },

    onIntermissionAudioError() {
      console.warn('中场音频播放错误，跳过中场')
      this.intermissionAudioPlaying = false
      this.intermissionAudioEndTimer = setTimeout(() => {
        this.finishIntermission()
      }, 500)
    },

    skipIntermission() {
      if (!this.intermissionCanSkip) return
      this.intermissionAudioPlaying = false
      this.finishIntermission()
    },

    finishIntermission() {
      if (this.intermissionAudioEndTimer) {
        clearTimeout(this.intermissionAudioEndTimer)
        this.intermissionAudioEndTimer = null
      }
      this.showIntermission = false

      // 写入下一卷信息
      if (this.pendingNextVolumeId) {
        localStorage.setItem('currentVolumeId', String(this.pendingNextVolumeId))
      }
      if (this.pendingNextVolumeCode) {
        localStorage.setItem('currentVolumeCode', this.pendingNextVolumeCode)
      }

      this.$router.push({
        path: '/broadcast',
        query: {
          fromVolumeComplete: 'true'
        }
      })
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
      }, 500)
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
      const q = this.allQuestions[questionIndex]
      return q && this.answeredQuestions[q.id] === optionId
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
      // 检查是否是当前题目（只有当前题目才能答题）
      // 特殊情况：题目组模式下，组内所有题目都可以作答，不受 currentQuestionIndex 限制
      if (!this.isInQuestionGroup && questionIndex !== this.currentQuestionIndex) {
        console.log(`⚠️ 题目 ${questionIndex + 1} 不是当前题目（当前是第 ${this.currentQuestionIndex + 1} 题），不能选择答案`)
        return
      }
      
      // 记录答案和用时到内存
      // 用时计算：以最后一次选择答案的时间点为准
      const q = this.allQuestions[questionIndex]
      if (q) {
        this.$set(this.answeredQuestions, q.id, optionId)
      }
      const timeSpent = this.answerTime - this.answerCountdown
      this.$set(this.answeredTimeSpent, questionIndex, timeSpent)
      const userAnswerText = option.optionName || option.label || String.fromCharCode(65 + optionId)
      console.log(`✓ 题目 ${questionIndex + 1} 选择了答案:`, userAnswerText, `用时: ${timeSpent}秒（剩余${this.answerCountdown}秒）`)
      
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
    /**
     * 按索引保存题目结果
     */
    async saveQuestionResultByIndex(questionIndex) {
        try {
            const question = this.allQuestions[questionIndex]
            if (!question || !this.paperInfoId) return

            // 检查是否已作答
            const answerId = this.answeredQuestions[question.id || question.questionId || question.question_id]
            if (answerId !== undefined) {
               console.log(`✓ [saveQuestionResultByIndex] 题目 ${questionIndex + 1} 已作答，跳过自动保存`)
               return
            }

            const questionId = question.id || question.question_id || question.questionId
            const userId = (JSON.parse(localStorage.getItem('userInfo') || '{}').user || {}).userId
            if (!questionId || !userId) return

            await ipcRenderer.invoke('answer:saveQuestionResult', {
              paperInfoId: this.paperInfoId,
              paperId: this.paperId,
              appUserId: userId,
              questionId: questionId,
              answerIds: '',
              userAnswer: '',
              result: 0,
              questionSort: questionIndex + 1,
              timeSpent: this.answerTime,
              sectionId: question.section_id || question.sectionId || null,
              volumeId: this.currentVolumeId || null
            })
            console.log(`✓ [saveQuestionResultByIndex] 题目 ${questionIndex + 1} 未作答自动保存完成`)

        } catch (e) {
            console.error(`保存题目 ${questionIndex} 失败`, e)
        }
    },

    async saveCurrentQuestionResult() {
      await this.saveQuestionResultByIndex(this.currentQuestionIndex)
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
          const answerId = this.answeredQuestions[question.id || question.questionId || question.question_id]
          
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
      
      const currentQuestion = this.currentQuestion
      if (!currentQuestion) return false
      
      const currentSectionId = currentQuestion.sectionId || currentQuestion.section_id
      if (currentSectionId !== sectionId) return false
      
      // 如果不在当前大题的 allQuestions 范围，直接返回
      if (!this.allQuestions || !this.allQuestions[qIndex]) return false

      if (this.isInQuestionGroup) {
          // 题目组模式：高亮组内所有题目
          // 只要该 qIndex 对应的题目 ID 在当前题目组中，就高亮
          const targetQ = this.allQuestions[qIndex]
          const groupIds = this.currentGroupQuestions.map(q => q.id)
          return groupIds.includes(targetQ.id)
      }
      
      // 普通模式：只高亮当前题目
      const questionsInSection = this.allQuestions.filter(q => 
        (q.sectionId || q.section_id) === sectionId
      )
      const indexInSection = questionsInSection.findIndex(q => q.id === currentQuestion.id)
      
      return indexInSection === qIndex
    },
    
    /**
     * 检查题目是否已作答（右侧题号列表用）
     */
    isQuestionAnswered(questionId) {
      return this.answeredQuestions[questionId] !== undefined
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
  padding-bottom: 30px; /* 为底部频率条留出空间 */
  overflow: hidden; /* 防止整体溢出 */
  padding-left: 0px;
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
  /* padding-top: 0; header is not fixed anymore */
}

/* 固定悬浮头部 */
.fixed-header {
  position: relative; /* Changed from fixed */
  width: 100%;
  z-index: 100;
  background: #f8f9fa; /* 乳白色背景 */
  color: #333;
  padding: 10px 30px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 10px;
  border-bottom: 1px solid #e0e0e0;
  min-height: 50px;
  height: auto;
  box-sizing: border-box;
  flex-shrink: 0;
}

.header-top-row {
  display: flex;
  align-items: center;
  gap: 30px;
  width: 100%;
}

.header-instruction-text {
  font-size: 14px;
  color: #606266; /* 次要文本颜色 */
  background: transparent; /* 移除背景色 */
  padding: 0; /* 移除内边距 */
  margin-top: 8px; /* 调整顶部间距 */
  border-radius: 0;
  line-height: 1.6;
  width: 100%;
  box-sizing: border-box;
  border: none; /* 移除边框 */
  text-align: left;
  text-indent: 2em; /* 恢复首行缩进 */
  padding-left: 2px; /* 微调左侧对齐 */
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
  padding: 20px 15px;
  display: flex;
  flex-direction: column;
  align-items: center;
  border-left: 1px solid #e0e0e0;
  position: relative;
  height: 100vh;
}

.avatar-section {
  position: relative;
  margin-bottom: 15px;
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
  width: 70px;
  height: 70px;
  border-radius: 50%;
  background: #e0e0e0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  color: #999;
}

.student-details {
  width: 100%;
  margin-bottom: 15px;
}

.info-item {
  display: flex;
  flex-direction: column;
  margin-bottom: 12px;
  text-align: left;
}

.info-label {
  font-size: 14px;
  color: #666;
  margin-bottom: 4px;
}

.info-value {
  font-size: 16px;
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
  line-height: 1.5;
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
  position: relative;
  width: 100%;
  max-width: 160px; /* 恢复原来的宽度限制 */
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-top: 15px;
  border-top: 1px solid #e0e0e0;
  margin-top: auto; /* 保持在底部 */
  flex-shrink: 0;
  padding-bottom: 20px;
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
  height: 30px;
  background-color: #409EFF; /* 蓝色背景 */
  z-index: 1000;
}

.frequency-canvas {
  width: 100%;
  height: 100%;
  display: block;
}

/* 中场遮罩样式 */
.intermission-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
}

.intermission-content {
  background: #ffffff;
  padding: 32px 40px;
  border-radius: 12px;
  width: 520px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
  text-align: center;
}

.intermission-audio-indicator {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  color: #409eff;
  font-weight: 600;
}

.intermission-audio-indicator .pulse-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #409eff;
  position: relative;
  animation: pulse 1.4s infinite;
}

.intermission-audio-indicator .pulse-text {
  font-size: 15px;
}

@keyframes pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(64, 158, 255, 0.4);
  }
  70% {
    box-shadow: 0 0 0 10px rgba(64, 158, 255, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(64, 158, 255, 0);
  }
}

.intermission-text {
  font-size: 16px;
  line-height: 1.7;
  color: #303133;
  margin-bottom: 20px;
  text-align: left;
  word-break: break-all;
}

.intermission-skip {
  margin-top: 4px;
}

.group-intro-container {
  padding: 15px 20px;
  background-color: #f8f9fa;
  border-bottom: 1px solid #ebeef5;
  margin-bottom: 20px;
  border-radius: 4px;
}
.group-name {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 8px;
}
.group-text {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  text-indent: 2em;
}
</style>
