<template>
  <div class="exam-paper-container">
    <!-- 顶部导航栏 -->
    <div class="exam-header">
      <div class="header-left">
        <h2>{{ paperName || '答题页面' }}</h2>
      </div>
      <div class="header-right">
        <div class="timer">
          <span class="timer-label">已用时间：</span>
          <span class="timer-value" :class="{ 'timer-over-limit': isTimeOverLimit }">{{ formatTime(usedTime) }}</span>
          <span v-if="remainingTime >= 0" class="timer-remaining">
            / 剩余：{{ formatTime(remainingTime) }}
          </span>
        </div>
        <el-button size="small" @click="backToSelect">返回</el-button>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="exam-content-wrapper">
      <!-- 左侧答题区域 -->
      <div class="exam-content">
        <!-- 加载状态 -->
        <div v-if="loading" class="loading-container">
          <el-loading text="正在加载试卷数据..." />
        </div>

        <!-- 错误提示 -->
        <el-alert
          v-if="error"
          :title="error"
          type="error"
          :closable="false"
          show-icon
          style="margin: 20px;"
        />

        <!-- 大题标题显示（页面最顶部） -->
        <div v-if="currentSection && !showIntermission && !loading" class="section-title-container">
          <div class="section-title-header">
            <h2 class="section-title-text">{{ currentSection.section_name }}</h2>
            <div class="section-title-info">
              <span>共 {{ currentSection.question_count }} 题</span>
              <span v-if="currentSection.total_score">总分：{{ currentSection.total_score }} 分</span>
            </div>
          </div>
          <div v-if="currentSection.instruction_text" class="section-instruction-text">
            <p>{{ currentSection.instruction_text }}</p>
          </div>
          <!-- 大题音频播放器（隐藏，自动播放） -->
          <audio
            ref="sectionInstructionAudioPlayer"
            :src="sectionInstructionAudioPath"
            @ended="onSectionInstructionAudioEnded"
            @error="onSectionInstructionAudioError"
            @loadedmetadata="onSectionInstructionAudioLoaded"
            @canplay="onSectionInstructionAudioCanPlay"
          ></audio>
        </div>

        <!-- 卷别切换（如果有多卷） -->
        <div v-if="volumes.length > 1 && !loading && currentSection" class="volume-switcher">
          <el-tabs v-model="currentVolumeCode" @tab-click="handleVolumeSwitch">
            <el-tab-pane
              v-for="volume in volumes"
              :key="volume.volume_code"
              :label="volume.volume_name"
              :name="volume.volume_code"
              :disabled="!canSwitchToVolume(volume.volume_code)"
            >
            </el-tab-pane>
          </el-tabs>
        </div>

        <!-- 中场音频播放（A卷提交后） -->
        <div v-if="showIntermission" class="intermission-container">
          <el-card>
            <div slot="header">
              <h3>中场休息</h3>
            </div>
            <div class="intermission-content">
              <p v-if="currentIntermission.intermissionText" class="intermission-text">
                {{ currentIntermission.intermissionText }}
              </p>
              <div class="audio-player" v-if="intermissionAudio">
                <audio
                  ref="intermissionAudioPlayer"
                  :src="intermissionAudioPath"
                  @ended="onIntermissionAudioEnded"
                  @error="onIntermissionAudioError"
                ></audio>
                <!-- 卷别名称音频播放器（隐藏） -->
                <audio
                  v-if="volumeNameAudio"
                  ref="volumeNameAudioPlayer"
                  :src="volumeNameAudioPath"
                  @ended="onVolumeNameAudioEnded"
                ></audio>
                <el-button
                  type="primary"
                  @click="playIntermissionAudio"
                  :disabled="intermissionAudioPlaying"
                >
                  {{ intermissionAudioPlaying ? '播放中...' : '播放中场音频' }}
                </el-button>
              </div>
            </div>
          </el-card>
        </div>

        <!-- 题目显示区域 -->
        <div v-if="currentQuestion && !showIntermission && !loading" class="question-container">
          <el-card class="question-card">
            <!-- 题目头部 -->
            <div class="question-header">
              <div class="question-number">
                <span class="number-badge" :class="{ 'question-active': true }">
                  第 {{ currentQuestionIndex + 1 }} 题
                </span>
                <span class="progress-text">
                  （{{ currentQuestionIndex + 1 }} / {{ totalQuestions }}）
                </span>
              </div>
              <div class="question-score" v-if="currentQuestion.score">
                分值：{{ currentQuestion.score }} 分
              </div>
            </div>

            <!-- 题目内容 -->
            <div class="question-content">
              <!-- 题目音频（media_type=4） -->
              <div v-if="questionAudio" class="question-audio">
                <audio
                  ref="questionAudioPlayer"
                  :src="questionAudioPath"
                  @ended="onQuestionAudioEnded"
                ></audio>
                <el-button
                  type="primary"
                  @click="playQuestionAudio"
                  :disabled="questionAudioPlaying"
                >
                  <i class="el-icon-video-play"></i>
                  {{ questionAudioPlaying ? '播放中...' : '播放题目音频' }}
                </el-button>
                <el-button
                  size="small"
                  @click="replayQuestionAudio"
                  :disabled="questionAudioPlaying"
                >
                  重播
                </el-button>
              </div>

              <!-- 题目文本 -->
              <div class="question-title" v-html="formatQuestionTitle(currentQuestion.title)"></div>

              <!-- 题目媒体（图片/视频，media_type=1） -->
              <div v-if="questionMedia.length > 0" class="question-media">
                <div
                  v-for="(media, index) in questionMedia"
                  :key="index"
                  class="media-item"
                >
                  <img
                    v-if="isImage(media.media_format)"
                    :src="getMediaAbsolutePath(media.media_path)"
                    alt="题目图片"
                    class="question-image"
                  />
                  <video
                    v-else-if="isVideo(media.media_format)"
                    :src="getMediaAbsolutePath(media.media_path)"
                    controls
                    class="question-video"
                  ></video>
                </div>
              </div>

              <!-- 答题讲解（自动显示） -->
              <div
                v-if="showExplanation && currentQuestion.explanation_text"
                class="question-explanation"
              >
                <el-alert
                  :title="currentQuestion.explanation_text"
                  type="info"
                  :closable="true"
                  @close="showExplanation = false"
                  show-icon
                >
                  <div v-if="explanationAudio" class="explanation-audio">
                    <audio
                      ref="explanationAudioPlayer"
                      :src="explanationAudioPath"
                      @ended="onExplanationAudioEnded"
                    ></audio>
                    <el-button
                      size="small"
                      @click="playExplanationAudio"
                      :disabled="explanationAudioPlaying"
                    >
                      {{ explanationAudioPlaying ? '播放中...' : '播放讲解音频' }}
                    </el-button>
                  </div>
                  <div v-if="explanationImages.length > 0" class="explanation-images">
                    <img
                      v-for="(img, index) in explanationImages"
                      :key="index"
                      :src="getMediaAbsolutePath(img.media_path)"
                      alt="讲解图片"
                      class="explanation-image"
                    />
                  </div>
                </el-alert>
              </div>

              <!-- 选项列表 -->
              <div class="question-options">
                <div
                  v-for="(option, index) in currentQuestionOptions"
                  :key="option.id || index"
                  class="option-item"
                  :class="{
                    'option-selected': isOptionSelected(option.id),
                    'option-correct': showAnswer && option.is_correct,
                    'option-wrong': showAnswer && isOptionSelected(option.id) && !option.is_correct
                  }"
                  @click="handleOptionClick(option)"
                >
                  <div class="option-label">
                    <span class="option-letter">{{ getOptionLetter(index) }}</span>
                    <span class="option-text" v-html="formatOptionText(option.text)"></span>
                  </div>
                  <!-- 选项音频（media_type=2） -->
                  <div v-if="option.media && option.media.length > 0" class="option-audio">
                  <audio
                    :ref="`optionAudioPlayer_${option.id}`"
                    :src="optionAudioPaths[option.id] || ''"
                    @ended="onOptionAudioEnded(option.id)"
                  ></audio>
                    <el-button
                      size="mini"
                      @click="playOptionAudio(option)"
                      :disabled="playingOptionAudioId === option.id"
                    >
                      <i class="el-icon-video-play"></i>
                      {{ playingOptionAudioId === option.id ? '播放中...' : '播放' }}
                    </el-button>
                  </div>
                </div>
              </div>
            </div>
          </el-card>

          <!-- 题目导航 -->
          <div class="question-navigation">
            <el-button
              :disabled="currentQuestionIndex === 0 || !allowReview"
              @click="goToPreviousQuestion"
            >
              上一题
            </el-button>
            <el-button
              type="primary"
              :disabled="currentQuestionIndex >= totalQuestions - 1"
              @click="goToNextQuestion"
            >
              下一题
            </el-button>
            <el-button
              v-if="currentQuestionIndex >= totalQuestions - 1"
              type="success"
              @click="submitCurrentVolume"
            >
              提交{{ currentVolume ? currentVolume.volume_name : '试卷' }}
            </el-button>
          </div>
        </div>

        <!-- 注意事项显示（before_section模式） -->
        <div v-if="showNotes && !loading" class="notes-container">
          <el-card>
            <div slot="header">
              <h3>注意事项</h3>
            </div>
            <div class="notes-content" v-html="notesContent"></div>
            <div class="notes-actions">
              <el-button type="primary" @click="confirmNotesAndContinue">
                我已了解，开始答题
              </el-button>
            </div>
          </el-card>
        </div>

        <!-- 下载状态检查页面（注意事项播放完毕后） -->
        <div v-if="showDownloadStatusCheck && !loading" class="download-status-check-container">
          <el-card class="download-status-card">
            <!-- 下载中：显示进度 -->
            <div v-if="!fullPackageReady" class="download-progress-container">
              <div class="download-progress-header">
                <i class="el-icon-loading"></i>
                <h3>正在下载试卷包...</h3>
              </div>
              <div class="download-progress-content">
                <el-progress
                  :percentage="downloadProgress"
                  :status="downloadProgress === 100 ? 'success' : ''"
                  :stroke-width="20"
                ></el-progress>
                <p class="download-progress-text">
                  下载进度：{{ downloadProgress }}%
                </p>
                <p v-if="downloadStatusMessage" class="download-status-message">
                  {{ downloadStatusMessage }}
                </p>
              </div>
            </div>
            
            <!-- 下载完成：显示准备就绪提示 -->
            <div v-else class="download-ready-container">
              <div class="download-ready-content">
                <i class="el-icon-success" style="font-size: 48px; color: #67C23A; margin-bottom: 20px;"></i>
                <h3 style="margin-bottom: 20px; text-align: center;">试卷已经下载成功</h3>
                <p class="ready-message" style="text-align: center; line-height: 1.8; font-size: 16px;">
                  测试准备就绪<br>
                  请安静等待测试开始，不要再动耳机、键盘、鼠标等设备
                </p>
              </div>
            </div>
          </el-card>
        </div>

        <!-- 无题目提示 -->
        <div v-if="!loading && !error && !currentQuestion && !showIntermission && !showNotes && !showDownloadStatusCheck" class="no-question">
          <el-alert
            title="暂无题目"
            type="info"
            :closable="false"
            show-icon
          >
            <p>当前卷别或大题没有题目</p>
          </el-alert>
        </div>
      </div>

      <!-- 右侧信息栏 -->
      <div class="exam-sidebar">
        <el-card class="sidebar-card">
          <div slot="header">
            <h4>答题信息</h4>
          </div>
          <!-- 学员信息 -->
          <div class="sidebar-section">
            <h5>学员信息</h5>
            <p>姓名：{{ userInfo.studentAccount || '未知' }}</p>
            <p v-if="paperInfo && paperInfo.assigned_seat_number">
              机位号：{{ paperInfo.assigned_seat_number }}
            </p>
          </div>

          <!-- 音量调节 -->
          <div class="sidebar-section">
            <h5>音量调节</h5>
            <el-slider
              v-model="volume"
              :min="0"
              :max="100"
              @change="onVolumeChange"
            ></el-slider>
            <span class="volume-value">{{ volume }}%</span>
          </div>

          <!-- 答题进度 -->
          <div class="sidebar-section">
            <h5>答题进度</h5>
            <div class="progress-info">
              <p>当前卷别：{{ currentVolume ? currentVolume.volume_name : '-' }}</p>
              <p>当前大题：{{ currentSection ? currentSection.section_name : '-' }}</p>
              <p>已完成：{{ answeredCount }} / {{ totalQuestions }}</p>
              <el-progress
                :percentage="progressPercentage"
                :status="progressPercentage === 100 ? 'success' : ''"
              ></el-progress>
            </div>
          </div>

          <!-- 各卷完成状态 -->
          <div v-if="volumes.length > 1" class="sidebar-section">
            <h5>各卷状态</h5>
            <div
              v-for="volume in volumes"
              :key="volume.volume_code"
              class="volume-status-item"
            >
              <span>{{ volume.volume_name }}：</span>
              <el-tag
                :type="getVolumeStatusType(volume.volume_code)"
                size="small"
              >
                {{ getVolumeStatusText(volume.volume_code) }}
              </el-tag>
            </div>
          </div>
        </el-card>
      </div>
    </div>

    <!-- 底部操作栏 -->
    <div class="exam-footer">
      <div class="footer-left">
        <span class="progress-text">
          进度：{{ currentQuestionIndex + 1 }} / {{ totalQuestions }}
        </span>
      </div>
      <div class="footer-right">
        <el-button @click="backToSelect">返回选择</el-button>
        <el-button
          type="primary"
          :disabled="!canSubmitAll"
          @click="submitAllVolumes"
        >
          提交全部
        </el-button>
      </div>
    </div>
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

export default {
  name: 'ExamPaper',
  data() {
    return {
      // 基础数据
      paperId: null,
      paperInfo: null,
      paperName: '',
      loading: true,
      error: null,
      
      // 试卷数据
      manifest: null,
      questions: [],
      volumes: [],
      sections: [],
      intermissions: [],
      
      // 当前状态
      currentVolumeCode: null,
      currentVolume: null,
      currentSection: null,
      currentQuestionIndex: 0,
      currentQuestion: null,
      currentQuestionOptions: [],
      
      // 答题记录
      paperInfoId: null,
      volumeStatus: {}, // { "A": "in_progress", "B": "pending" }
      userAnswers: {}, // { questionId: { answerIds: [], userAnswer: "" } }
      answeredCount: 0,
      
      // 音频播放
      questionAudio: null,
      questionAudioPath: '',
      questionAudioPlaying: false,
      sectionInstructionAudio: null,
      sectionInstructionAudioPath: '',
      sectionInstructionAudioPlaying: false,
      intermissionAudio: null,
      intermissionAudioPath: '',
      intermissionAudioPlaying: false,
      explanationAudio: null,
      explanationAudioPath: '',
      explanationAudioPlaying: false,
      volumeNameAudio: null, // 卷别名称音频
      volumeNameAudioPath: '',
      volumeNameAudioPlaying: false,
      playingOptionAudioId: null,
      optionAudioPaths: {}, // { optionId: audioPath }
      volume: 50, // 音量 0-100
      
      // 媒体文件
      questionMedia: [],
      explanationImages: [],
      
      // 显示控制
      showIntermission: false,
      currentIntermission: null,
      showExplanation: false,
      showAnswer: false, // 是否显示答案（用于结果查看）
      showNotes: false, // 是否显示注意事项（before_section模式）
      notesContent: '', // 注意事项内容
      showDownloadStatusCheck: false, // 是否显示下载状态检查页面
      fullPackageReady: false, // 完整包是否已下载完成
      downloadProgress: 0, // 下载进度（0-100）
      downloadStatusMessage: '', // 下载状态消息
      downloadStatusCheckInterval: null, // 下载状态检查定时器
      allowReview: false, // 是否允许回顾（根据配置）
      autoNextQuestion: false, // 是否自动跳转下一题
      questionReadDuration: 3, // 题目阅读时长（秒）
      currentQuestionStartTime: null, // 当前题目开始时间
      
      // 计时（从00:00开始计时，不累加之前的时间）
      timerStartTime: null, // 本次答题开始的时间戳（用于计算已用时间）
      usedTime: 0, // 已用时间（秒），从00:00开始
      remainingTime: -1, // 剩余时间（秒），-1表示无限制
      timerInterval: null,
      
      // 用户信息
      userInfo: {},
      
      // 计算属性
      totalQuestions: 0,
      mediaBasePath: ''
    }
  },
  computed: {
    progressPercentage() {
      if (this.totalQuestions === 0) return 0
      return Math.floor((this.answeredCount / this.totalQuestions) * 100)
    },
    canSubmitAll() {
      // 检查是否所有卷别都已完成
      if (this.volumes.length === 0) return false
      return this.volumes.every(volume => {
        const status = this.volumeStatus[volume.volume_code]
        return status === 'completed'
      })
    },
    // 检查是否超过考试时间限制
    isTimeOverLimit() {
      if (!this.manifest || !this.manifest.duration) {
        return false // 没有时间限制，不显示红色
      }
      const totalSeconds = this.manifest.duration * 60 // 考试时长（秒）
      return this.usedTime > totalSeconds
    }
  },
  async mounted() {
    // 加载音量设置
    const savedVolume = localStorage.getItem('examVolume')
    if (savedVolume) {
      this.volume = parseInt(savedVolume)
    }
    
    // 获取用户信息
    const userInfoStr = localStorage.getItem('userInfo')
    if (userInfoStr) {
      this.userInfo = JSON.parse(userInfoStr)
    }
    
    // 获取试卷ID
    const selectedPaperId = localStorage.getItem('selectedPaperId')
    if (!selectedPaperId) {
      this.$message.warning('未选择试卷，返回选择页面')
      this.$router.push('/paper-select')
      return
    }
    
    this.paperId = parseInt(selectedPaperId)
    
    // 加载试卷数据
    await this.loadPaperData()
  },
  beforeDestroy() {
    // 清理定时器
    if (this.timerInterval) {
      clearInterval(this.timerInterval)
    }
    
    // 清理下载状态检查定时器
    if (this.downloadStatusCheckInterval) {
      clearInterval(this.downloadStatusCheckInterval)
      this.downloadStatusCheckInterval = null
    }
    
    // 停止所有音频播放
    this.stopAllAudio()
  },
  methods: {
    /**
     * 加载试卷数据
     */
    async loadPaperData() {
      try {
        this.loading = true
        this.error = null
        
        // 1. 获取试卷基本信息
        const papers = await ipcRenderer.invoke('paper:getPapersByIds', [this.paperId])
        if (!papers || papers.length === 0) {
          throw new Error('未找到试卷信息')
        }
        
        this.paperInfo = papers[0]
        this.paperName = this.paperInfo.paper_name || '未命名试卷'
        
        // 2. 获取试卷包数据（manifest 和 questions）
        // 优化：优先从快速启动包加载manifest，如果完整包不存在
        let paperData = null
        try {
          paperData = await ipcRenderer.invoke('paper:getPaperData', this.paperId)
          if (!paperData) {
            throw new Error('无法加载试卷包数据：返回数据为空')
          }
        } catch (error) {
          console.error('加载试卷包数据失败:', error)
          // 如果错误信息包含"未同步"，提示用户先同步
          if (error.message && error.message.includes('未同步')) {
            throw new Error(`试卷包未同步，请返回选择页面重新同步试卷数据。错误：${error.message}`)
          } else {
            throw new Error(`无法加载试卷包数据：${error.message || '未知错误'}`)
          }
        }
        
        // 优先使用本地JSON数据，确保所有文本字段编码正确
        const processTextFields = (obj) => {
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
        
        // 处理 manifest 和 questions，确保所有文本字段编码正确
        this.manifest = processTextFields(paperData.manifest) || {}
        this.questions = processTextFields(paperData.questions || [])
        this.mediaBasePath = paperData.mediaDir || ''
        
        // 如果是从快速启动包加载的（questions为空），说明完整包正在后台下载
        if (paperData.isQuickStart && (!this.questions || this.questions.length === 0)) {
          console.log('从快速启动包加载，完整包正在后台下载，questions为空')
          // 可以显示提示信息，但允许用户继续（操作提示页面只需要manifest）
        }
        
        // 3. 获取卷别列表
        this.volumes = await ipcRenderer.invoke('paper:getVolumes', this.paperId)
        // 确保所有文本字段编码正确
        this.volumes = this.volumes.map(v => ({
          ...v,
          volume_code: String(v.volume_code || ''),
          volume_name: String(v.volume_name || '')
        }))

        // 4. 获取大题列表
        this.sections = await ipcRenderer.invoke('paper:getSections', this.paperId)
        // 确保所有文本字段编码正确
        this.sections = this.sections.map(s => ({
          ...s,
          section_name: String(s.section_name || ''),
          instruction_text: String(s.instruction_text || ''),
          volume_code: String(s.volume_code || '')
        }))
        
        // 5. 获取中场配置列表
        this.intermissions = await ipcRenderer.invoke('paper:getIntermissions', this.paperId)
        
        // 6. 初始化卷别状态
        if (this.volumes.length > 0) {
          const volumeCodes = this.volumes.map(v => v.volume_code)
          this.currentVolumeCode = volumeCodes[0]
          this.currentVolume = this.volumes[0]
          
          // 检查是否有未完成的答题记录
          await this.checkExistingExam()
        }
        
        // 7. 加载当前卷别的题目
        await this.loadCurrentVolumeQuestions()
        
        // 8. 开始计时
        this.startTimer()
        
        this.loading = false
      } catch (error) {
        console.error('加载试卷数据失败:', error)
        this.error = error.message || '加载试卷数据失败'
        this.loading = false
      }
    },
    
    /**
     * 检查是否有未完成的答题记录
     */
    async checkExistingExam() {
      try {
        const userId = this.userInfo.userId || this.userInfo.user?.userId
        if (!userId) {
          // 如果没有用户ID，创建新的答题记录
          await this.startNewExam()
          return
        }
        
        // 检查是否有未提交的答题记录
        const existing = await ipcRenderer.invoke('answer:getPaperInfoList', userId, this.paperId)
        const unfinished = existing.find(e => !e.is_submit)
        
        if (unfinished) {
          // 恢复未完成的答题
          this.paperInfoId = unfinished.id
          // 注意：startTime 用于保存到数据库，但计时从00:00开始
          this.startTime = unfinished.start_time
          this.volumeStatus = unfinished.volume_status ? JSON.parse(unfinished.volume_status) : {}
          
          // 恢复用户答案
          await this.restoreAnswers()
        } else {
          // 创建新的答题记录
          await this.startNewExam()
        }
      } catch (error) {
        console.error('检查答题记录失败:', error)
        // 如果检查失败，创建新记录
        await this.startNewExam()
      }
    },
    
    /**
     * 开始新的答题
     */
    async startNewExam() {
      try {
        const userId = this.userInfo.userId || this.userInfo.user?.userId
        if (!userId) {
          throw new Error('用户未登录')
        }
        
        const volumeCodes = this.volumes.map(v => v.volume_code)
        
        const result = await ipcRenderer.invoke('answer:startExam', {
          paperId: this.paperId,
          paperName: this.paperName,
          appUserId: userId,
          volumeCodes: volumeCodes,
          assignedSeatNumber: null // TODO: 从用户信息获取
        })
        
        if (result.success) {
          this.paperInfoId = result.paperInfoId
          // 注意：startTime 用于保存到数据库，但计时从00:00开始
          this.startTime = result.startTime
          this.volumeStatus = result.volumeStatus || {}
        } else {
          throw new Error(result.message || '开始答题失败')
        }
      } catch (error) {
        console.error('开始答题失败:', error)
        this.$message.error('开始答题失败：' + error.message)
      }
    },
    
    /**
     * 加载当前卷别的题目
     */
    async loadCurrentVolumeQuestions() {
      if (!this.currentVolume) return
      
      // 播放卷别名称音频
      await this.loadVolumeNameAudio()
      
      // 获取当前卷别的大题
      const currentSections = this.sections.filter(s => s.volume_code === this.currentVolumeCode)
      if (currentSections.length === 0) {
        this.currentSection = null
        this.currentQuestion = null
        this.totalQuestions = 0
        return
      }
      
      // 使用第一个大题（后续可以支持大题切换）
      this.currentSection = currentSections[0]
      
      // 检查是否需要显示注意事项（before_section模式）
      if (this.manifest && this.manifest.notesDisplayMode === 'before_section' && this.manifest.notes) {
        this.showNotes = true
        this.notesContent = this.manifest.notes
        // 等待用户确认后继续
        return
      }
      
      // 加载大题题目
      await this.loadSectionQuestions()
    },
    
    /**
     * 加载大题题目
     */
    async loadSectionQuestions() {
      if (!this.currentSection) return
      
      console.log(`📦 [loadSectionQuestions] 开始加载大题题目，sectionId=${this.currentSection.id}, sectionName=${this.currentSection.section_name}`)
      
      // 加载大题音频（先加载，然后播放）
      await this.loadSectionInstructionAudio()
      
      // 自动播放大题音频（严格按照 Notes.vue 的逻辑）
      this.playSectionInstructionAudio()
      
      // 获取该大题的题目
      const questions = await ipcRenderer.invoke('paper:getQuestions', this.paperId, this.currentSection.id)
      this.totalQuestions = questions.length
      
      console.log(`📦 [loadSectionQuestions] 大题题目数量: ${this.totalQuestions}`)
      
      if (questions.length > 0) {
        this.currentQuestionIndex = 0
        await this.loadQuestion(questions[0])
      } else {
        this.currentQuestion = null
      }
    },
    
    /**
     * 加载卷别名称音频
     */
    async loadVolumeNameAudio() {
      if (!this.currentVolume) return
      
      try {
        const mediaFiles = await ipcRenderer.invoke('paper:getMediaFiles', {
          volumeId: this.currentVolume.id,
          mediaType: 7 // 卷别名称音频
        })
        
        if (mediaFiles.length > 0) {
          this.volumeNameAudio = mediaFiles[0]
          this.volumeNameAudioPath = await this.getMediaAbsolutePath(mediaFiles[0].media_path)
          // 自动播放卷别名称音频
          this.$nextTick(() => {
            this.playVolumeNameAudio()
          })
        }
      } catch (error) {
        console.error('加载卷别名称音频失败:', error)
      }
    },
    
    /**
     * 播放卷别名称音频
     */
    playVolumeNameAudio() {
      if (!this.$refs.volumeNameAudioPlayer) return
      const audio = this.$refs.volumeNameAudioPlayer
      audio.volume = this.volume / 100
      audio.play()
      this.volumeNameAudioPlaying = true
    },
    
    /**
     * 卷别名称音频播放结束
     */
    onVolumeNameAudioEnded() {
      this.volumeNameAudioPlaying = false
    },
    
    /**
     * 确认注意事项并继续
     * 在跳转前检查完整包下载状态
     */
    async confirmNotesAndContinue() {
      this.showNotes = false
      
      // 检查完整包是否已下载完成
      const hasFullPackage = await ipcRenderer.invoke('paper:checkPackageExists', this.paperId)
      
      if (!hasFullPackage) {
        // 完整包未下载完成，显示下载状态检查页面
        console.log('完整包未下载完成，显示下载状态检查页面')
        this.showDownloadStatusCheck = true
        this.fullPackageReady = false
        this.downloadProgress = 0
        this.downloadStatusMessage = '正在检查下载状态...'
        
        // 开始轮询下载状态
        this.startDownloadStatusCheck()
      } else {
        // 完整包已下载完成，直接加载题目
        console.log('完整包已下载完成，直接加载题目')
        await this.loadSectionQuestions()
      }
    },
    
    /**
     * 开始下载状态检查（轮询）
     */
    startDownloadStatusCheck() {
      // 立即检查一次
      this.checkDownloadStatus()
      
      // 设置定时器，每秒检查一次
      this.downloadStatusCheckInterval = setInterval(() => {
        this.checkDownloadStatus()
      }, 1000)
    },
    
    /**
     * 检查下载状态
     */
    async checkDownloadStatus() {
      try {
        // 1. 检查完整包是否存在
        const hasFullPackage = await ipcRenderer.invoke('paper:checkPackageExists', this.paperId)
        
        if (hasFullPackage) {
          // 完整包已下载完成
          console.log('完整包已下载完成')
          this.fullPackageReady = true
          this.downloadProgress = 100
          this.downloadStatusMessage = '下载完成'
          
          // 停止轮询
          if (this.downloadStatusCheckInterval) {
            clearInterval(this.downloadStatusCheckInterval)
            this.downloadStatusCheckInterval = null
          }
          
          // 延迟2秒后自动加载题目（给用户时间看到准备就绪提示）
          setTimeout(async () => {
            this.showDownloadStatusCheck = false
            await this.loadSectionQuestions()
          }, 2000)
          
          return
        }
        
        // 2. 获取下载状态
        const downloadStatus = await ipcRenderer.invoke('paper:getDownloadStatus', this.paperId)
        
        if (downloadStatus) {
          const status = downloadStatus.status || 'downloading'
          const progress = downloadStatus.progress || 0
          
          this.downloadProgress = progress
          
          // 根据状态设置消息
          if (status === 'downloading') {
            this.downloadStatusMessage = `正在下载... ${progress}%`
          } else if (status === 'pending') {
            this.downloadStatusMessage = '等待下载...'
          } else if (status === 'error') {
            this.downloadStatusMessage = `下载失败：${downloadStatus.error_message || '未知错误'}`
            // 下载失败时，停止轮询，但继续显示页面（用户可以重试）
            if (this.downloadStatusCheckInterval) {
              clearInterval(this.downloadStatusCheckInterval)
              this.downloadStatusCheckInterval = null
            }
          } else if (status === 'completed' || status === 'ready') {
            // 状态显示已完成，但文件可能还没完全写入，再检查一次文件
            setTimeout(async () => {
              const hasPackage = await ipcRenderer.invoke('paper:checkPackageExists', this.paperId)
              if (hasPackage) {
                this.fullPackageReady = true
                this.downloadProgress = 100
                this.downloadStatusMessage = '下载完成'
                
                if (this.downloadStatusCheckInterval) {
                  clearInterval(this.downloadStatusCheckInterval)
                  this.downloadStatusCheckInterval = null
                }
                
                setTimeout(async () => {
                  this.showDownloadStatusCheck = false
                  await this.loadSectionQuestions()
                }, 2000)
              }
            }, 500)
          }
        } else {
          // 没有下载状态记录，可能还没开始下载
          this.downloadStatusMessage = '等待开始下载...'
          this.downloadProgress = 0
        }
      } catch (error) {
        console.error('检查下载状态失败:', error)
        this.downloadStatusMessage = `检查下载状态失败：${error.message}`
      }
    },
    
    /**
     * 恢复用户答案
     */
    async restoreAnswers() {
      if (!this.paperInfoId) return
      
      try {
        const userId = this.userInfo.userId || this.userInfo.user?.userId
        if (!userId) return
        
        // 获取所有已保存的答题结果
        const questionResults = await ipcRenderer.invoke('answer:getQuestionResults', {
          paperInfoId: this.paperInfoId,
          paperId: this.paperId,
          appUserId: userId
        })
        
        // 恢复答案到 userAnswers
        this.userAnswers = {}
        for (const result of questionResults) {
          this.userAnswers[result.question_id] = {
            answerIds: result.answer_ids ? result.answer_ids.split(',').map(id => parseInt(id)) : [],
            userAnswer: result.user_answer || ''
          }
        }
        
        // 更新已回答数量
        this.updateAnsweredCount()
      } catch (error) {
        console.error('恢复答案失败:', error)
      }
    },
    
    /**
     * 加载题目详情
     */
    async loadQuestion(questionData) {
      // 记录当前题目开始时间
      this.currentQuestionStartTime = Date.now()
      
      // 优先从本地 questions.json 中查找题目详情（确保中文编码正确）
      const question = this.questions.find(q => {
        const qId = q.id || q.question_id || q.questionId
        const dataId = questionData.question_id || questionData.id
        return qId === dataId
      })
      if (!question) {
        this.currentQuestion = questionData
        this.currentQuestionOptions = []
        return
      }
      
      // 确保题目文本正确编码（优先使用本地JSON数据）
      const questionTitle = question.title || questionData.title || ''
      const questionText = question.text || questionData.text || ''
      
      // 递归处理所有文本字段，确保UTF-8编码正确
      const processTextFields = (obj) => {
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
      
      // 处理整个question对象，确保所有文本字段编码正确
      const processedQuestion = processTextFields({
        ...questionData,
        ...question
      })
      
      this.currentQuestion = {
        ...processedQuestion,
        // 确保 title 和 text 字段是字符串类型
        title: String(questionTitle),
        text: String(questionText)
      }
      
      // 加载题目选项
      if (question.answers && Array.isArray(question.answers)) {
        // 确保选项文本正确编码
        this.currentQuestionOptions = question.answers.map(option => {
          const processedOption = processTextFields(option)
          return {
            ...processedOption,
            text: String(processedOption.text || ''),
            label: String(processedOption.label || '')
          }
        })
        // 预加载选项音频路径
        for (const option of this.currentQuestionOptions) {
          if (option.media && Array.isArray(option.media)) {
            const audioMedia = option.media.find(m => m.mediaType === 2)
            if (audioMedia) {
              this.optionAudioPaths[option.id] = await this.getMediaAbsolutePath(audioMedia.mediaPath)
            }
          }
        }
      } else {
        this.currentQuestionOptions = []
      }
      
      // 加载题目媒体
      await this.loadQuestionMedia(question)
      
      // 加载配置
      if (this.manifest) {
        this.autoNextQuestion = this.manifest.autoNextQuestion || false
        this.questionReadDuration = this.manifest.questionReadDuration || 3
        this.allowReview = this.manifest.allowReview || false
      }
      
      // 加载讲解（如果有）
      if (question.explanationEnabled) {
        // 延迟显示讲解（在卷别名称音频播放完成后）
        const delaySeconds = question.explanationDelaySeconds || 2
        setTimeout(() => {
          this.showExplanation = true
          // 自动播放讲解音频
          this.$nextTick(() => {
            if (this.explanationAudio) {
              this.playExplanationAudio()
            }
          })
        }, delaySeconds * 1000)
      } else {
        this.showExplanation = false
      }
    },
    
    /**
     * 加载题目媒体文件
     */
    async loadQuestionMedia(question) {
      this.questionMedia = []
      this.explanationImages = []
      this.questionAudio = null
      this.explanationAudio = null
      
      if (!question.media || !Array.isArray(question.media)) return
      
      for (const media of question.media) {
        if (media.mediaType === 1) {
          // 题目媒体（图片/视频）
          this.questionMedia.push(media)
        } else if (media.mediaType === 4) {
          // 题目音频
          this.questionAudio = media
          this.questionAudioPath = await this.getMediaAbsolutePath(media.mediaPath)
        } else if (media.mediaType === 5) {
          // 讲解音频
          this.explanationAudio = media
          this.explanationAudioPath = await this.getMediaAbsolutePath(media.mediaPath)
        } else if (media.mediaType === 6) {
          // 讲解图片
          this.explanationImages.push(media)
        }
      }
      
      // 加载大题说明音频（在loadQuestionMedia中不再加载，改为在loadSectionInstructionAudio中加载）
    },
    
    /**
     * 获取媒体文件绝对路径（返回 file:// URL）
     */
    async getMediaAbsolutePath(relativePath) {
      if (!relativePath) return ''
      if (relativePath.startsWith('http://') || relativePath.startsWith('https://')) {
        return relativePath
      }
      // 如果已经是 file:// URL，直接返回
      if (relativePath.startsWith('file://')) {
        return relativePath
      }
      // 通过 IPC 获取 userData 路径
      try {
        const userDataPath = await ipcRenderer.invoke('app:getPath', 'userData')
        const absolutePath = path.join(userDataPath, relativePath)
        
        // 检查文件是否存在
        const fs = require('fs')
        if (fs.existsSync(absolutePath)) {
          // 使用 pathToFileURL 正确编码路径（特别是 macOS 上的空格和特殊字符）
          return pathToFileURL(absolutePath).href
        } else {
          console.warn(`媒体文件不存在: ${absolutePath}`)
          // 即使文件不存在，也返回编码后的路径，让浏览器显示错误
          return pathToFileURL(absolutePath).href
        }
      } catch (error) {
        console.error('获取 userData 路径失败:', error)
        // 降级方案：尝试使用相对路径构建绝对路径
        try {
          const absolutePath = path.resolve(relativePath)
          return pathToFileURL(absolutePath).href
        } catch (e) {
          console.error('构建绝对路径失败:', e)
          return relativePath
        }
      }
    },
    
    /**
     * 获取选项音频路径（异步）
     */
    async getOptionAudioPath(option) {
      if (!option.media || !Array.isArray(option.media)) return ''
      const audioMedia = option.media.find(m => m.mediaType === 2)
      if (!audioMedia) return ''
      return await this.getMediaAbsolutePath(audioMedia.mediaPath)
    },
    
    /**
     * 判断是否为图片
     */
    isImage(format) {
      return ['jpg', 'jpeg', 'png', 'gif', 'webp'].includes(format?.toLowerCase())
    },
    
    /**
     * 判断是否为视频
     */
    isVideo(format) {
      return ['mp4', 'webm', 'ogg'].includes(format?.toLowerCase())
    },
    
    /**
     * 格式化题目标题（支持HTML）
     */
    formatQuestionTitle(title) {
      if (!title) return ''
      // 确保文本是字符串类型
      let text = String(title)
      // 简单的HTML转义和格式化
      // 将换行符转换为 <br>，同时保留中文字符
      return text.replace(/\n/g, '<br>')
    },
    
    /**
     * 格式化选项文本（支持HTML）
     */
    formatOptionText(text) {
      if (!text) return ''
      // 确保文本是字符串类型
      let optionText = String(text)
      // 将换行符转换为 <br>，同时保留中文字符
      return optionText.replace(/\n/g, '<br>')
    },
    
    /**
     * 获取选项字母（A, B, C, D...）
     */
    getOptionLetter(index) {
      return String.fromCharCode(65 + index) // A=65
    },
    
    /**
     * 检查选项是否被选中
     */
    isOptionSelected(optionId) {
      if (!this.currentQuestion) return false
      const answer = this.userAnswers[this.currentQuestion.id]
      if (!answer) return false
      return answer.answerIds && answer.answerIds.includes(optionId)
    },
    
    /**
     * 处理选项点击
     */
    handleOptionClick(option) {
      if (!this.currentQuestion) return
      if (this.showAnswer) return // 如果已显示答案，不允许修改
      
      const questionId = this.currentQuestion.id
      if (!this.userAnswers[questionId]) {
        this.userAnswers[questionId] = {
          answerIds: [],
          userAnswer: ''
        }
      }
      
      const answer = this.userAnswers[questionId]
      
      // 根据题目类型处理（单选/多选）
      if (this.currentQuestion.type === 'single_choice') {
        // 单选题：替换答案
        answer.answerIds = [option.id]
        answer.userAnswer = option.text
      } else if (this.currentQuestion.type === 'multiple_choice') {
        // 多选题：切换答案
        const index = answer.answerIds.indexOf(option.id)
        if (index > -1) {
          answer.answerIds.splice(index, 1)
        } else {
          answer.answerIds.push(option.id)
        }
        answer.userAnswer = answer.answerIds.map(id => {
          const opt = this.currentQuestionOptions.find(o => o.id === id)
          return opt ? opt.text : ''
        }).filter(t => t).join(', ')
      }
      
      // 更新已回答数量
      this.updateAnsweredCount()
      
      // 保存答案到数据库
      this.saveAnswer()
      
      // 如果启用自动跳转，等待指定时间后自动跳转下一题
      if (this.autoNextQuestion && this.currentQuestionIndex < this.totalQuestions - 1) {
        setTimeout(() => {
          this.goToNextQuestion()
        }, this.questionReadDuration * 1000)
      }
    },
    
    /**
     * 更新已回答数量
     */
    updateAnsweredCount() {
      this.answeredCount = Object.keys(this.userAnswers).filter(qId => {
        const answer = this.userAnswers[qId]
        return answer && answer.answerIds && answer.answerIds.length > 0
      }).length
    },
    
    /**
     * 保存答案到数据库
     */
    async saveAnswer() {
      if (!this.currentQuestion || !this.paperInfoId) return
      
      try {
        const userId = this.userInfo.userId || this.userInfo.user?.userId
        if (!userId) return
        
        const answer = this.userAnswers[this.currentQuestion.id]
        if (!answer) return
        
        // 计算用时
        const timeSpent = this.currentQuestionStartTime 
          ? Math.floor((Date.now() - this.currentQuestionStartTime) / 1000) 
          : 0
        
        // 计算是否正确（简单判断，后续可以优化）
        let result = 0
        if (this.currentQuestion.answer) {
          const correctAnswerIds = Array.isArray(this.currentQuestion.answer) 
            ? this.currentQuestion.answer 
            : this.currentQuestion.answer.split(',').map(id => parseInt(id))
          const userAnswerIds = answer.answerIds.map(id => parseInt(id))
          
          if (correctAnswerIds.length === userAnswerIds.length) {
            const isCorrect = correctAnswerIds.every(id => userAnswerIds.includes(id)) &&
                            userAnswerIds.every(id => correctAnswerIds.includes(id))
            result = isCorrect ? 1 : 0
          }
        }
        
        await ipcRenderer.invoke('answer:saveQuestionResult', {
          paperInfoId: this.paperInfoId,
          paperId: this.paperId,
          appUserId: userId,
          questionId: this.currentQuestion.id,
          answerIds: answer.answerIds.join(','),
          userAnswer: answer.userAnswer,
          result: result,
          questionSort: this.currentQuestionIndex + 1,
          timeSpent: timeSpent
        })
      } catch (error) {
        console.error('保存答案失败:', error)
      }
    },
    
    /**
     * 播放题目音频
     */
    playQuestionAudio() {
      if (!this.$refs.questionAudioPlayer) return
      const audio = this.$refs.questionAudioPlayer
      audio.volume = this.volume / 100
      audio.play()
      this.questionAudioPlaying = true
    },
    
    /**
     * 重播题目音频
     */
    replayQuestionAudio() {
      if (!this.$refs.questionAudioPlayer) return
      const audio = this.$refs.questionAudioPlayer
      audio.currentTime = 0
      audio.play()
      this.questionAudioPlaying = true
    },
    
    /**
     * 题目音频播放结束
     */
    onQuestionAudioEnded() {
      this.questionAudioPlaying = false
    },
    
    /**
     * 播放选项音频
     */
    playOptionAudio(option) {
      const ref = this.$refs[`optionAudioPlayer_${option.id}`]
      if (!ref || !ref[0]) return
      const audio = ref[0]
      audio.volume = this.volume / 100
      audio.play()
      this.playingOptionAudioId = option.id
    },
    
    /**
     * 选项音频播放结束
     */
    onOptionAudioEnded(optionId) {
      if (this.playingOptionAudioId === optionId) {
        this.playingOptionAudioId = null
      }
    },
    
    /**
     * 加载大题说明音频（参考Notes.vue的实现）
     */
    async loadSectionInstructionAudio() {
      if (!this.currentSection) {
        console.warn('⚠️ [loadSectionInstructionAudio] 当前大题不存在，无法加载音频')
        return
      }
      
      try {
        console.log(`📦 [loadSectionInstructionAudio] 开始加载大题音频，sectionId=${this.currentSection.id}`)
        
        // 获取试卷包数据
        const paperData = await ipcRenderer.invoke('paper:getPaperData', this.paperId)
        if (!paperData || !paperData.manifest) {
          console.warn('⚠️ [loadSectionInstructionAudio] 无法加载试卷数据')
          return
        }
        
        const mediaDir = paperData.mediaDir || ''
        
        // 获取大题音频（mediaType=8）
        const sectionMedia = await ipcRenderer.invoke('paper:getMediaFiles', {
          sectionId: this.currentSection.id,
          mediaType: 8
        })
        
        if (sectionMedia.length === 0) {
          console.warn(`⚠️ [loadSectionInstructionAudio] 大题 ${this.currentSection.id} 没有音频文件`)
          return
        }
        
        this.sectionInstructionAudio = sectionMedia[0]
        // 只使用本地路径，不使用远程URL
        const audioPath = sectionMedia[0].media_path
        
        console.log(`📦 [loadSectionInstructionAudio] 找到音频文件路径: ${audioPath}`)
        console.log(`📦 [loadSectionInstructionAudio] mediaDir: ${mediaDir}`)
        
        if (!audioPath) {
          console.warn('⚠️ [loadSectionInstructionAudio] 音频路径为空')
          return
        }
        
        // 只处理本地路径，忽略远程URL
        let audioFilePath = null
        
        // 如果路径以 media/ 开头，说明是完整的相对路径（如 media/${paperCode}/sections/${fileName}）
        if (audioPath.startsWith('media/')) {
          // 使用 getMediaAbsolutePath 方法获取绝对路径
          const fileUrl = await this.getMediaAbsolutePath(audioPath)
          if (fileUrl && fileUrl.startsWith('file://')) {
            // 将 file:// URL 转换为本地文件路径
            try {
              const urlObj = new URL(fileUrl)
              audioFilePath = decodeURIComponent(urlObj.pathname)
              // Windows 路径需要去掉开头的 /
              if (process.platform === 'win32' && audioFilePath.startsWith('/')) {
                audioFilePath = audioFilePath.substring(1)
              }
              console.log(`📦 [loadSectionInstructionAudio] 从 media/ 路径解析得到本地文件: ${audioFilePath}`)
            } catch (error) {
              console.warn(`⚠️ [loadSectionInstructionAudio] 解析 file:// URL 失败: ${error.message}`)
            }
          }
        } else {
          // 如果只是文件名，从 sections/ 目录查找
          const sectionInstructionDir = path.join(mediaDir, 'sections')
          const fileName = path.basename(audioPath)
          console.log(`📦 [loadSectionInstructionAudio] 尝试从 sections 目录查找: ${sectionInstructionDir}/${fileName}`)
          if (fs.existsSync(sectionInstructionDir)) {
            const sectionInstructionFilePath = path.join(sectionInstructionDir, fileName)
            if (fs.existsSync(sectionInstructionFilePath)) {
              audioFilePath = sectionInstructionFilePath
              console.log(`✓ [loadSectionInstructionAudio] 找到本地音频文件: ${audioFilePath}`)
            } else {
              console.warn(`⚠️ [loadSectionInstructionAudio] 音频文件不存在: ${sectionInstructionFilePath}`)
            }
          } else {
            console.warn(`⚠️ [loadSectionInstructionAudio] sections 目录不存在: ${sectionInstructionDir}`)
          }
        }
        
        // 加载音频文件的辅助函数
        const loadAudioFile = (filePath) => {
          try {
            if (!fs.existsSync(filePath)) {
              console.warn(`⚠️ [loadSectionInstructionAudio] 音频文件不存在: ${filePath}`)
              return false
            }
            
            console.log(`📦 [loadSectionInstructionAudio] 开始读取音频文件: ${filePath}`)
            const fileBuffer = fs.readFileSync(filePath)
            const ext = path.extname(filePath).toLowerCase()
            let mimeType = 'audio/mpeg'
            if (ext === '.wav') mimeType = 'audio/wav'
            else if (ext === '.m4a' || ext === '.aac') mimeType = 'audio/mp4'
            else if (ext === '.ogg') mimeType = 'audio/ogg'
            
            const base64 = fileBuffer.toString('base64')
            const dataUrl = `data:${mimeType};base64,${base64}`
            this.sectionInstructionAudioPath = dataUrl
            console.log(`✓ [loadSectionInstructionAudio] 大题音频加载成功（base64 data URL）: ${filePath}`)
            return true
          } catch (error) {
            console.error(`❌ [loadSectionInstructionAudio] 读取音频文件失败: ${error.message}`)
            // 降级方案：使用 file:// URL
            try {
              this.sectionInstructionAudioPath = pathToFileURL(filePath).href
              console.log(`✓ [loadSectionInstructionAudio] 大题音频加载成功（file:// URL）: ${filePath}`)
              return true
            } catch (urlError) {
              console.error(`❌ [loadSectionInstructionAudio] 生成 file:// URL 失败: ${urlError.message}`)
              return false
            }
          }
        }
        
        // 扫描目录查找音频文件的辅助函数
        const scanDirectoryForAudio = () => {
          let foundAudioPath = null
          const sectionInstructionDir = path.join(mediaDir, 'sections')
          if (fs.existsSync(sectionInstructionDir)) {
            const files = fs.readdirSync(sectionInstructionDir)
            console.log(`📦 [loadSectionInstructionAudio] sections 目录下的文件: ${files.join(', ')}`)
            const audioFile = files.find(file => /\.(mp3|wav|m4a|aac|ogg)$/i.test(file))
            if (audioFile) {
              foundAudioPath = path.join(sectionInstructionDir, audioFile)
              console.log(`✓ [loadSectionInstructionAudio] 扫描目录找到音频文件: ${foundAudioPath}`)
            }
          }
          return foundAudioPath
        }
        
        // 如果找到了音频文件路径，直接加载
        if (audioFilePath && fs.existsSync(audioFilePath)) {
          loadAudioFile(audioFilePath)
        } else {
          // 文件不存在或路径不存在，尝试扫描目录
          if (audioPath) {
            console.warn(`⚠️ [loadSectionInstructionAudio] 大题音频不存在: ${audioPath}，尝试扫描目录`)
          }
          const foundAudioPath = scanDirectoryForAudio()
          if (foundAudioPath) {
            loadAudioFile(foundAudioPath)
          } else {
            console.warn(`⚠️ [loadSectionInstructionAudio] sections 目录不存在或没有音频文件，mediaDir=${mediaDir}`)
          }
        }
      } catch (error) {
        console.error('❌ [loadSectionInstructionAudio] 加载大题音频失败:', error)
      }
    },
    
    /**
     * 播放大题说明音频（自动播放，严格按照 Notes.vue 的逻辑）
     */
    playSectionInstructionAudio() {
      if (!this.sectionInstructionAudioPath) {
        // 如果没有大题音频，直接返回
        console.log('⚠️ [playSectionInstructionAudio] 没有大题音频')
        return
      }

      const audio = this.$refs.sectionInstructionAudioPlayer
      if (!audio) {
        // 音频元素不存在
        console.log('⚠️ [playSectionInstructionAudio] 音频元素不存在')
        return
      }

      console.log(`🎵 [playSectionInstructionAudio] 开始播放大题音频，sectionId=${this.currentSection?.id}, sectionName=${this.currentSection?.section_name}`)

      // 等待音频加载完成的回调函数（严格按照 Notes.vue 的逻辑）
      const onCanPlay = () => {
        console.log('🎵 [playSectionInstructionAudio] 大题音频可以播放，开始播放')
        
        audio.volume = this.volume / 100

        // 开始播放音频
        audio.play().then(() => {
          console.log('✓ [playSectionInstructionAudio] 大题音频开始播放')
          this.sectionInstructionAudioPlaying = true
        }).catch(error => {
          console.error('❌ [playSectionInstructionAudio] 播放大题音频失败:', error)
          this.sectionInstructionAudioPlaying = false
        })
      }

      const onError = () => {
        console.error('❌ [playSectionInstructionAudio] 大题音频加载失败')
        this.sectionInstructionAudioPlaying = false
      }

      // 检查音频是否已就绪（严格按照 Notes.vue 的逻辑）
      if (audio.readyState >= 2) {
        // 音频已就绪，直接播放
        onCanPlay()
      } else {
        // 等待音频加载完成
        audio.addEventListener('canplay', onCanPlay, { once: true })
        audio.addEventListener('error', onError, { once: true })
        
        // 设置超时（10秒），如果音频一直无法加载，则记录警告
        setTimeout(() => {
          if (audio.readyState < 2 && !this.sectionInstructionAudioPlaying) {
            console.warn('⚠️ [playSectionInstructionAudio] 大题音频加载超时')
          }
        }, 10000)
      }
    },
    
    /**
     * 大题说明音频播放结束
     */
    onSectionInstructionAudioEnded() {
      console.log('✓ [onSectionInstructionAudioEnded] 大题音频播放完成')
      this.sectionInstructionAudioPlaying = false
    },
    
    /**
     * 大题说明音频加载完成
     */
    onSectionInstructionAudioLoaded() {
      const audio = this.$refs.sectionInstructionAudioPlayer
      if (audio && audio.duration) {
        console.log(`📦 [onSectionInstructionAudioLoaded] 大题音频加载完成，时长: ${audio.duration.toFixed(2)} 秒`)
      }
    },
    
    /**
     * 大题说明音频可以播放
     */
    onSectionInstructionAudioCanPlay() {
      console.log('📦 [onSectionInstructionAudioCanPlay] 大题音频可以播放')
    },
    
    /**
     * 大题说明音频播放错误
     */
    onSectionInstructionAudioError(error) {
      console.error('❌ [onSectionInstructionAudioError] 大题音频播放错误:', error)
      this.sectionInstructionAudioPlaying = false
    },
    
    /**
     * 播放讲解音频
     */
    playExplanationAudio() {
      if (!this.$refs.explanationAudioPlayer) return
      const audio = this.$refs.explanationAudioPlayer
      audio.volume = this.volume / 100
      audio.play()
      this.explanationAudioPlaying = true
    },
    
    /**
     * 讲解音频播放结束
     */
    onExplanationAudioEnded() {
      this.explanationAudioPlaying = false
    },
    
    /**
     * 播放中场音频
     */
    playIntermissionAudio() {
      if (!this.$refs.intermissionAudioPlayer) return
      const audio = this.$refs.intermissionAudioPlayer
      audio.volume = this.volume / 100
      audio.play()
      this.intermissionAudioPlaying = true
    },
    
    /**
     * 中场音频播放结束
     */
    onIntermissionAudioEnded() {
      this.intermissionAudioPlaying = false
      // 自动进入下一卷
      this.enterNextVolume()
    },
    
    /**
     * 中场音频播放错误
     */
    onIntermissionAudioError() {
      this.intermissionAudioPlaying = false
      this.$message.warning('中场音频播放失败，将直接进入下一卷')
      // 直接进入下一卷
      this.enterNextVolume()
    },
    
    /**
     * 进入下一卷
     */
    async enterNextVolume() {
      const currentIndex = this.volumes.findIndex(v => v.volume_code === this.currentVolumeCode)
      if (currentIndex < 0 || currentIndex >= this.volumes.length - 1) {
        // 已经是最后一卷
        return
      }
      
      const nextVolume = this.volumes[currentIndex + 1]
      this.currentVolumeCode = nextVolume.volume_code
      this.currentVolume = nextVolume
      this.showIntermission = false
      this.currentIntermission = null
      
      // 更新卷别状态
      await this.updateVolumeStatus(nextVolume.volume_code, 'in_progress')
      
      // 加载下一卷的题目
      await this.loadCurrentVolumeQuestions()
    },
    
    /**
     * 切换到指定卷别
     */
    async handleVolumeSwitch(tab) {
      const volumeCode = tab.name
      if (!this.canSwitchToVolume(volumeCode)) {
        this.$message.warning('请先完成上一卷')
        return
      }
      
      this.currentVolumeCode = volumeCode
      this.currentVolume = this.volumes.find(v => v.volume_code === volumeCode)
      await this.loadCurrentVolumeQuestions()
    },
    
    /**
     * 检查是否可以切换到指定卷别
     */
    canSwitchToVolume(volumeCode) {
      const currentIndex = this.volumes.findIndex(v => v.volume_code === this.currentVolumeCode)
      const targetIndex = this.volumes.findIndex(v => v.volume_code === volumeCode)
      
      if (targetIndex <= currentIndex) {
        // 可以切换到当前卷或之前的卷
        return true
      }
      
      // 检查上一卷是否已完成
      if (currentIndex >= 0 && currentIndex < this.volumes.length - 1) {
        const prevVolume = this.volumes[currentIndex]
        const status = this.volumeStatus[prevVolume.volume_code]
        return status === 'completed'
      }
      
      return false
    },
    
    /**
     * 更新卷别状态
     */
    async updateVolumeStatus(volumeCode, status) {
      if (!this.paperInfoId) return
      
      try {
        const result = await ipcRenderer.invoke('answer:updateVolumeStatus', this.paperInfoId, volumeCode, status)
        if (result.success) {
          this.volumeStatus = result.volumeStatus
        }
      } catch (error) {
        console.error('更新卷别状态失败:', error)
      }
    },
    
    /**
     * 提交当前卷别
     */
    async submitCurrentVolume() {
      if (!this.currentVolume) return
      
      try {
        // 提交当前卷
        const result = await ipcRenderer.invoke('answer:submitVolume', {
          paperInfoId: this.paperInfoId,
          volumeCode: this.currentVolumeCode
        })
        
        if (!result.success) {
          throw new Error(result.message || '提交失败')
        }
        
        // 更新本地状态
        this.volumeStatus[this.currentVolumeCode] = 'completed'
        
        // 检查是否有中场配置
        const intermission = this.intermissions.find(
          i => i.from_volume === this.currentVolumeCode
        )
        
        if (intermission) {
          // 显示中场音频
          this.showIntermission = true
          this.currentIntermission = intermission
          
          // 加载中场音频
          const mediaFiles = await ipcRenderer.invoke('paper:getMediaFiles', {
            intermissionId: intermission.id,
            mediaType: 9
          })
          
          if (mediaFiles.length > 0) {
            this.intermissionAudio = mediaFiles[0]
            this.intermissionAudioPath = await this.getMediaAbsolutePath(mediaFiles[0].media_path)
          }
          
          // 标记中场音频已播放
          await ipcRenderer.invoke('answer:markIntermissionPlayed',
            this.paperInfoId,
            intermission.from_volume,
            intermission.to_volume
          )
          
          // 如果不可跳过，自动播放
          if (!intermission.can_skip && this.intermissionAudioPath) {
            this.playIntermissionAudio()
          }
        } else {
          // 没有中场配置，直接进入下一卷或完成
          const currentIndex = this.volumes.findIndex(v => v.volume_code === this.currentVolumeCode)
          if (currentIndex < this.volumes.length - 1) {
            // 还有下一卷
            await this.enterNextVolume()
          } else {
            // 所有卷都完成了
            await this.submitAllVolumes()
          }
        }
        
        this.$message.success(`${this.currentVolume.volume_name} 提交成功`)
      } catch (error) {
        console.error('提交卷别失败:', error)
        this.$message.error('提交失败：' + error.message)
      }
    },
    
    /**
     * 提交所有卷别
     */
    async submitAllVolumes() {
      if (!this.canSubmitAll) {
        this.$message.warning('请先完成所有卷别')
        return
      }
      
      try {
        // 计算总分和得分
        // TODO: 实现分数计算逻辑
        
        const result = await ipcRenderer.invoke('answer:submitExam', {
          paperInfoId: this.paperInfoId,
          paperId: this.paperId,
          appUserId: this.userInfo.userId || this.userInfo.user?.userId,
          totalScore: this.manifest?.totalScore || 0,
          userScore: 0, // TODO: 计算得分
          correctCount: 0, // TODO: 计算正确数
          wrongCount: 0 // TODO: 计算错误数
        })
        
        if (!result.success) {
          throw new Error(result.message || '提交失败')
        }
        
        this.$message.success('答题提交成功')
        
        // 跳转到结果页面
        this.$router.push({
          path: '/exam-result',
          query: {
            paperInfoId: this.paperInfoId
          }
        })
      } catch (error) {
        console.error('提交答题失败:', error)
        this.$message.error('提交失败：' + error.message)
      }
    },
    
    /**
     * 上一题
     */
    async goToPreviousQuestion() {
      if (this.currentQuestionIndex <= 0) return
      if (!this.allowReview) {
        this.$message.warning('当前不允许回顾题目')
        return
      }
      
      // 保存当前题目答案
      await this.saveAnswer()
      
      this.currentQuestionIndex--
      const questions = await ipcRenderer.invoke('paper:getQuestions', this.paperId, this.currentSection.id)
      if (questions[this.currentQuestionIndex]) {
        await this.loadQuestion(questions[this.currentQuestionIndex])
      }
    },
    
    /**
     * 下一题
     */
    async goToNextQuestion() {
      if (this.currentQuestionIndex >= this.totalQuestions - 1) return
      
      // 保存当前题目答案
      await this.saveAnswer()
      
      this.currentQuestionIndex++
      const questions = await ipcRenderer.invoke('paper:getQuestions', this.paperId, this.currentSection.id)
      if (questions[this.currentQuestionIndex]) {
        await this.loadQuestion(questions[this.currentQuestionIndex])
      }
    },
    
    /**
     * 开始计时
     */
    startTimer() {
      // 从00:00开始计时，不累加之前的时间
      if (!this.timerStartTime) {
        this.timerStartTime = Date.now()
      }
      
      this.timerInterval = setInterval(() => {
        const now = Date.now()
        // 已用时间从00:00开始计算（本次答题的时长）
        this.usedTime = Math.floor((now - this.timerStartTime) / 1000)
        
        // 如果有时间限制，计算剩余时间
        if (this.manifest && this.manifest.duration) {
          const totalSeconds = this.manifest.duration * 60
          this.remainingTime = Math.max(0, totalSeconds - this.usedTime)
          
          // 时间到自动提交
          if (this.remainingTime === 0) {
            this.submitAllVolumes()
          }
        }
      }, 1000)
    },
    
    /**
     * 格式化时间（秒转 MM:SS 或 HH:MM:SS）
     * 超过1小时显示 HH:MM:SS，否则显示 MM:SS
     */
    formatTime(seconds) {
      if (seconds < 0) seconds = 0
      
      const hours = Math.floor(seconds / 3600)
      const mins = Math.floor((seconds % 3600) / 60)
      const secs = seconds % 60
      
      // 超过1小时，显示 HH:MM:SS
      if (hours > 0) {
        return `${String(hours).padStart(2, '0')}:${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`
      }
      // 否则显示 MM:SS
      return `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`
    },
    
    /**
     * 音量改变
     */
    onVolumeChange(value) {
      localStorage.setItem('examVolume', value.toString())
      // 更新所有音频播放器的音量
      this.updateAllAudioVolume()
    },
    
    /**
     * 更新所有音频播放器的音量
     */
    updateAllAudioVolume() {
      const volume = this.volume / 100
      const audioRefs = [
        this.$refs.questionAudioPlayer,
        this.$refs.sectionInstructionAudioPlayer,
        this.$refs.intermissionAudioPlayer,
        this.$refs.explanationAudioPlayer
      ]
      
      audioRefs.forEach(audio => {
        if (audio) {
          audio.volume = volume
        }
      })
    },
    
    /**
     * 停止所有音频播放
     */
    stopAllAudio() {
      const audioRefs = [
        this.$refs.questionAudioPlayer,
        this.$refs.sectionInstructionAudioPlayer,
        this.$refs.intermissionAudioPlayer,
        this.$refs.explanationAudioPlayer
      ]
      
      audioRefs.forEach(audio => {
        if (audio) {
          audio.pause()
          audio.currentTime = 0
        }
      })
    },
    
    /**
     * 获取卷别状态类型
     */
    getVolumeStatusType(volumeCode) {
      const status = this.volumeStatus[volumeCode]
      if (status === 'completed') return 'success'
      if (status === 'in_progress') return 'warning'
      return 'info'
    },
    
    /**
     * 获取卷别状态文本
     */
    getVolumeStatusText(volumeCode) {
      const status = this.volumeStatus[volumeCode]
      if (status === 'completed') return '已完成'
      if (status === 'in_progress') return '进行中'
      return '未开始'
    },
    
    /**
     * 返回选择页面
     */
    backToSelect() {
      this.$confirm('确定要返回吗？未保存的答案将丢失。', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.$router.push('/paper-select')
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.exam-paper-container {
  width: 100%;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

.exam-header {
  padding: 15px 20px;
  background: white;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 15px;
}

.header-left h2 {
  margin: 0;
  color: #333;
  font-size: 20px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.timer {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 14px;
  color: #666;
}

.timer-value {
  font-weight: bold;
  color: #409eff;
}

.timer-value.timer-over-limit {
  color: #f56c6c; /* 超过考试时间显示红色 */
}

.timer-remaining {
  color: #f56c6c;
}

.exam-content-wrapper {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.exam-content {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  background: #f5f7fa;
}

.exam-sidebar {
  width: 280px;
  padding: 20px;
  background: white;
  border-left: 1px solid #e4e7ed;
  overflow-y: auto;
}

.sidebar-card {
  margin-bottom: 20px;
}

.sidebar-section {
  margin-bottom: 20px;
}

.sidebar-section h5 {
  margin: 0 0 10px 0;
  color: #333;
  font-size: 14px;
}

.volume-status-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-size: 13px;
}

.loading-container {
  padding: 100px;
  text-align: center;
}

.volume-switcher {
  margin-bottom: 20px;
  background: white;
  padding: 10px;
  border-radius: 4px;
}

.intermission-container {
  margin-bottom: 20px;
}

.intermission-content {
  text-align: center;
  padding: 20px;
}

.intermission-text {
  font-size: 16px;
  color: #666;
  margin-bottom: 20px;
}

/* 大题标题容器（页面顶部） */
.section-title-container {
  margin-bottom: 30px;
  padding: 20px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.section-title-header {
  margin-bottom: 15px;
}

.section-title-text {
  margin: 0 0 10px 0;
  font-size: 24px;
  font-weight: bold;
  color: #409EFF;
  line-height: 1.6;
}

.section-title-info {
  display: flex;
  gap: 20px;
  font-size: 14px;
  color: #666;
}

.section-instruction-text {
  padding: 15px;
  background: rgba(0, 0, 0, 0.05);
  border-radius: 4px;
  line-height: 1.8;
  color: #333;
}

.section-instruction-text p {
  margin: 0;
  font-size: 16px;
}

.question-container {
  margin-bottom: 20px;
}

.question-card {
  margin-bottom: 20px;
}

.question-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #e4e7ed;
}

.question-number {
  display: flex;
  align-items: center;
  gap: 10px;
}

.number-badge {
  padding: 6px 12px;
  background: #409eff;
  color: white;
  border-radius: 4px;
  font-weight: bold;
  transition: all 0.3s;
}

.number-badge.question-active {
  animation: questionPulse 1.5s ease-in-out infinite;
}

@keyframes questionPulse {
  0%, 100% {
    box-shadow: 0 0 0 0 rgba(64, 158, 255, 0.7);
  }
  50% {
    box-shadow: 0 0 0 8px rgba(64, 158, 255, 0);
  }
}

.progress-text {
  color: #666;
  font-size: 14px;
}

.question-score {
  color: #f56c6c;
  font-weight: bold;
}

.question-content {
  margin-bottom: 20px;
}

.question-audio {
  margin-bottom: 15px;
  padding: 15px;
  background: #f5f7fa;
  border-radius: 4px;
}

.question-title {
  font-size: 16px;
  line-height: 1.6;
  color: #333;
  margin-bottom: 20px;
}

.question-media {
  margin-bottom: 20px;
}

.media-item {
  margin-bottom: 10px;
}

.question-image {
  max-width: 100%;
  border-radius: 4px;
}

.question-video {
  max-width: 100%;
  border-radius: 4px;
}

.question-explanation {
  margin-bottom: 20px;
}

.explanation-audio {
  margin-top: 10px;
}

.explanation-images {
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.explanation-image {
  max-width: 200px;
  border-radius: 4px;
}

.question-options {
  margin-top: 20px;
}

.option-item {
  padding: 15px;
  margin-bottom: 10px;
  border: 2px solid #e4e7ed;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
}

.option-item:hover {
  border-color: #409eff;
  background: #ecf5ff;
}

.option-selected {
  border-color: #409eff;
  background: #ecf5ff;
}

.option-correct {
  border-color: #67c23a;
  background: #f0f9ff;
}

.option-wrong {
  border-color: #f56c6c;
  background: #fef0f0;
}

.option-label {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.option-letter {
  width: 24px;
  height: 24px;
  line-height: 24px;
  text-align: center;
  background: #409eff;
  color: white;
  border-radius: 50%;
  font-weight: bold;
  flex-shrink: 0;
}

.option-text {
  flex: 1;
  line-height: 1.6;
  color: #333;
}

.option-audio {
  margin-top: 10px;
  margin-left: 34px;
}

.question-navigation {
  display: flex;
  justify-content: center;
  gap: 10px;
  padding: 20px;
  background: white;
  border-radius: 4px;
}

.exam-footer {
  padding: 15px 20px;
  background: white;
  border-top: 1px solid #e4e7ed;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.footer-left {
  color: #666;
  font-size: 14px;
}

.footer-right {
  display: flex;
  gap: 10px;
}

.audio-player {
  margin-top: 10px;
}

.no-question {
  padding: 40px;
  text-align: center;
}

.progress-info {
  font-size: 13px;
  color: #666;
}

.progress-info p {
  margin: 5px 0;
}

.volume-value {
  display: block;
  text-align: center;
  margin-top: 5px;
  font-size: 12px;
  color: #666;
}

.notes-container {
  margin-bottom: 20px;
}

.notes-content {
  padding: 20px;
  line-height: 1.8;
  color: #333;
}

.notes-actions {
  text-align: center;
  padding: 20px;
  border-top: 1px solid #e4e7ed;
}

/* 下载状态检查页面样式 */
.download-status-check-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
  padding: 40px;
}

.download-status-card {
  width: 100%;
  max-width: 600px;
  margin: 0 auto;
}

.download-progress-container {
  padding: 40px 20px;
  text-align: center;
}

.download-progress-header {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin-bottom: 30px;
}

.download-progress-header i {
  font-size: 24px;
  color: #409EFF;
  animation: rotating 2s linear infinite;
}

.download-progress-header h3 {
  margin: 0;
  font-size: 20px;
  color: #333;
}

.download-progress-content {
  margin-top: 30px;
}

.download-progress-text {
  margin-top: 20px;
  font-size: 16px;
  color: #666;
}

.download-status-message {
  margin-top: 15px;
  font-size: 14px;
  color: #909399;
}

.download-ready-container {
  padding: 60px 40px;
  text-align: center;
}

.download-ready-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.download-ready-content h3 {
  margin: 0;
  font-size: 24px;
  color: #333;
  font-weight: 500;
}

.ready-message {
  margin: 0;
  color: #666;
  line-height: 1.8;
}

@keyframes rotating {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>
