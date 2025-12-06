<template>
  <div class="broadcast-container">
    <!-- 左侧主内容区：试卷名称 + 卷别名称 -->
    <div class="main-content">
      <div class="paper-info-wrapper">
        <div class="paper-info-text">
          {{ displayText }}
        </div>
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

    <!-- 音频播放器（隐藏，用于播放卷别音频） -->
    <audio
      ref="volumeAudioPlayer"
      :src="volumeAudioPath"
      @ended="onVolumeAudioEnded"
      @error="onVolumeAudioError"
      @loadedmetadata="onVolumeAudioLoaded"
      @canplay="onVolumeAudioCanPlay"
    ></audio>

    <!-- 音频频率动画（页面最底部） -->
    <div class="audio-frequency-bar">
      <canvas ref="frequencyCanvas" class="frequency-canvas"></canvas>
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
  name: 'Broadcast',
  data() {
    return {
      paperId: null,
      paperName: '', // 自定义试卷名称
      volumeName: '', // 卷别名称
      displayText: '', // 显示文本：试卷名称 + 卷别名称
      volumeAudioPath: '',
      volumeAudioDuration: 0,
      navigationTriggered: false, // 防止重复导航
      audioPlayed: false, // 音频是否已播放完成
      audioPlayTimer: null, // 音频播放完成后的定时器
      volume: 50, // 音量（0-100）
      // 学生信息
      studentName: '',
      studentId: '',
      seatNumber: '',
      avatarNumber: null
    }
  },
  async mounted() {
    console.log('🚀 Broadcast.vue mounted 开始')
    
    // 获取试卷ID
    this.paperId = parseInt(localStorage.getItem('currentPaperId')) || null
    console.log('📋 获取到的 paperId:', this.paperId)
    
    if (!this.paperId) {
      console.error('❌ 未找到试卷ID')
      this.$message.error('未找到试卷ID，请返回选择页面')
      setTimeout(() => {
        this.$router.push('/paper-select')
      }, 2000)
      return
    }

    try {
      // 加载学生信息（完全离线可用：从 localStorage 和本地数据库获取）
      console.log('👤 开始加载学生信息')
      await this.loadStudentInfo()
      console.log('✓ 学生信息加载完成')

      // 加载试卷数据
      console.log('📦 开始加载试卷数据')
      await this.loadPaperData()
      console.log('✓ 试卷数据加载完成')
      
      // 等待 Vue 响应式更新完成，确保 audio 元素的 src 已更新
      await this.$nextTick()
      console.log('✓ Vue 响应式更新完成')
      
      // 自动播放卷别音频
      console.log('🎵 准备播放卷别音频')
      this.playVolumeAudio()
    } catch (error) {
      console.error('❌ Broadcast.vue mounted 出错:', error)
      this.$message.error('页面加载失败: ' + error.message)
    }
  },
  methods: {
    async loadStudentInfo() {
      try {
        // 首先从 localStorage 获取 userInfo（完全离线可用）
        const userInfoStr = localStorage.getItem('userInfo')
        let userInfo = null
        if (userInfoStr) {
          userInfo = JSON.parse(userInfoStr)
          // 学生姓名
          this.studentName = userInfo.studentName || userInfo.name || userInfo.user?.nickName || userInfo.studentAccount || '未知'
          // 考生号
          this.studentId = userInfo.studentId || userInfo.studentAccount || userInfo.user?.userName || '未知'
          // 座位号
          this.seatNumber = userInfo.seatNumber || userInfo.seat_number || userInfo.assignedSeatNumber || ''
          // 头像编号（如果有）
          this.avatarNumber = userInfo.avatarNumber || userInfo.avatar_number || null
        }

        // 如果座位号等信息缺失，尝试从本地数据库获取学员档案（完全离线可用）
        if ((!this.seatNumber || !this.studentName || this.studentName === '未知') && userInfo) {
          try {
            const userId = userInfo.user?.userId
            const studentAccount = userInfo.studentAccount || userInfo.user?.userName

            let archive = null
            if (userId) {
              archive = await ipcRenderer.invoke('archive:getByUserId', userId)
            }
            if (!archive && studentAccount) {
              archive = await ipcRenderer.invoke('archive:getByAccount', studentAccount)
            }

            if (archive) {
              // 补充缺失的信息
              if (!this.studentName || this.studentName === '未知') {
                this.studentName = archive.name || archive.student_name || this.studentName
              }
              if (!this.studentId || this.studentId === '未知') {
                this.studentId = archive.student_account || archive.account || this.studentId
              }
              if (!this.seatNumber) {
                this.seatNumber = archive.seat_number || archive.seatNumber || ''
              }
              console.log('✓ 从本地数据库补充学生信息:', { name: this.studentName, id: this.studentId, seat: this.seatNumber })
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

    async loadPaperData() {
      try {
        console.log('📦 [loadPaperData] 开始加载播报页面数据，paperId:', this.paperId)
        
        // 获取试卷包数据
        console.log('📦 [loadPaperData] 调用 paper:getPaperData')
        const paperData = await ipcRenderer.invoke('paper:getPaperData', this.paperId)
        console.log('📦 [loadPaperData] paper:getPaperData 返回:', paperData ? '有数据' : '无数据')
        
        if (!paperData || !paperData.manifest) {
          console.error('❌ [loadPaperData] 无法加载试卷数据，paperData:', paperData)
          throw new Error('无法加载试卷数据')
        }

        const manifest = paperData.manifest
        const mediaDir = paperData.mediaDir || ''
        console.log('📦 [loadPaperData] manifest 存在，mediaDir:', mediaDir)

        // 获取自定义试卷名称
        this.paperName = manifest.paperName || manifest.customName || '试卷'
        console.log('📦 [loadPaperData] 试卷名称:', this.paperName)

        // 获取第一个卷别信息（按 volumeOrder 排序）
        const volumes = manifest.volumes || []
        console.log('📦 [loadPaperData] volumes 数组长度:', volumes.length)
        
        if (volumes.length === 0) {
          console.error('❌ [loadPaperData] 试卷中没有卷别信息')
          throw new Error('试卷中没有卷别信息')
        }

        console.log('📋 [loadPaperData] 所有卷别信息:', JSON.stringify(volumes, null, 2))

        // 按 volumeOrder 排序
        const sortedVolumes = volumes.sort((a, b) => (a.volumeOrder || 0) - (b.volumeOrder || 0))
        
        // 检查 localStorage 中是否已有当前卷别代码（从中场或卷别完成页面跳转过来时会设置）
        const currentVolumeCode = localStorage.getItem('currentVolumeCode')
        let targetVolume = null
        
        if (currentVolumeCode) {
          // 使用 localStorage 中的卷别代码找到对应的卷别
          targetVolume = sortedVolumes.find(v => 
            (v.volumeCode || v.volume_code) === currentVolumeCode
          )
          if (targetVolume) {
            console.log('✓ 使用 localStorage 中的卷别代码:', currentVolumeCode)
          } else {
            console.warn('⚠️ localStorage 中的卷别代码无效，使用第一个卷别')
          }
        }
        
        // 如果没有找到或没有设置，使用第一个卷别
        if (!targetVolume) {
          targetVolume = sortedVolumes[0]
          localStorage.setItem('currentVolumeCode', targetVolume.volumeCode || targetVolume.volume_code)
          console.log('✓ 使用第一个卷别，保存到 localStorage:', targetVolume.volumeCode || targetVolume.volume_code)
        }

        console.log('📋 [loadPaperData] 排序后的卷别列表:', sortedVolumes.map(v => ({
          volumeOrder: v.volumeOrder,
          volumeName: v.volumeName,
          volumeCode: v.volumeCode,
          hasAudio: !!v.volumeAudio
        })))
        console.log('📋 [loadPaperData] 当前卷别信息:', JSON.stringify(targetVolume, null, 2))

        // 获取卷别名称
        this.volumeName = targetVolume.volumeName || targetVolume.volumeCode || targetVolume.volume_code || ''

        // 组合显示文本：自定义试卷名称 + 卷别名称
        this.displayText = `${this.paperName} ${this.volumeName}`

        console.log('✓ [loadPaperData] 试卷名称:', this.paperName)
        console.log('✓ [loadPaperData] 卷别名称:', this.volumeName)
        console.log('✓ [loadPaperData] 显示文本:', this.displayText)

        // 加载卷别音频（完全参考 Notes.vue 的实现方式）
        const volumeAudio = targetVolume.volumeAudio
        // 优先使用 mediaPath（本地路径），如果没有则使用 mediaUrl（远程URL）
        const audioPath = volumeAudio?.mediaPath || volumeAudio?.mediaUrl || ''
        
        if (audioPath) {
          // 检查是否是完整URL
          if (audioPath && (audioPath.startsWith('http://') || audioPath.startsWith('https://'))) {
            // 完整URL，直接使用
            this.volumeAudioPath = audioPath
            console.log('✓ 使用远程音频URL:', audioPath)
          } else {
            // 相对路径，从 volumes/ 目录中查找
            const volumesDir = path.join(mediaDir, 'volumes')
            
            // 优先从 volumes 目录查找
            let audioFilePath = null
            if (audioPath) {
              const fileName = path.basename(audioPath)
              if (fs.existsSync(volumesDir)) {
                const volumesFilePath = path.join(volumesDir, fileName)
                if (fs.existsSync(volumesFilePath)) {
                  audioFilePath = volumesFilePath
                }
              }
            }
            
            // 加载音频文件的辅助函数（完全参考 Notes.vue）
            const loadAudioFile = (filePath) => {
              try {
                const fileBuffer = fs.readFileSync(filePath)
                const ext = path.extname(filePath).toLowerCase()
                let mimeType = 'audio/mpeg'
                if (ext === '.wav') mimeType = 'audio/wav'
                else if (ext === '.m4a' || ext === '.aac') mimeType = 'audio/mp4'
                else if (ext === '.ogg') mimeType = 'audio/ogg'
                
                const base64 = fileBuffer.toString('base64')
                const dataUrl = `data:${mimeType};base64,${base64}`
                this.volumeAudioPath = dataUrl
                return true
              } catch (error) {
                // 降级方案：使用 file:// URL
                this.volumeAudioPath = pathToFileURL(filePath).href
                console.log('✓ 卷别音频加载成功（file:// URL）:', filePath)
                return true
              }
            }
            
            // 扫描目录查找音频文件的辅助函数（完全参考 Notes.vue）
            const scanDirectoryForAudio = () => {
              let foundAudioPath = null
              // 扫描 volumes 目录
              if (fs.existsSync(volumesDir)) {
                const files = fs.readdirSync(volumesDir)
                const audioFile = files.find(file => /\.(mp3|wav|m4a|aac|ogg)$/i.test(file))
                if (audioFile) {
                  foundAudioPath = path.join(volumesDir, audioFile)
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
                console.warn(`卷别音频不存在: ${audioPath}，尝试扫描目录`)
              }
              const foundAudioPath = scanDirectoryForAudio()
              if (foundAudioPath) {
                loadAudioFile(foundAudioPath)
              } else {
                console.warn(`volumes 目录不存在或没有音频文件`)
              }
            }
          }
          
          // 设置音频时长
          if (volumeAudio?.mediaDuration) {
            this.volumeAudioDuration = volumeAudio.mediaDuration
          }
        } else {
          console.warn('⚠️ manifest中没有卷别音频路径')
        }
        
        console.log('🎵 [loadPaperData] 最终设置的音频时长:', this.volumeAudioDuration, '秒')
        console.log('✓ [loadPaperData] loadPaperData 方法执行完成')
      } catch (error) {
        console.error('❌ [loadPaperData] 加载试卷数据失败:', error)
        
        // 友好的错误提示（不显示堆栈信息）
        let errorMessage = '加载试卷数据失败'
        if (error.message) {
          if (error.message.includes('Invalid or unsupported zip format') || 
              error.message.includes('No END header found')) {
            errorMessage = '试卷包文件已损坏，正在自动清理，请稍后重新下载'
          } else if (error.message.includes('未同步')) {
            errorMessage = '试卷包未下载，请返回试卷列表下载'
          } else {
            // 其他错误，只显示简短的错误信息
            errorMessage = '加载试卷数据失败，请重试'
          }
        }
        
        this.$message.error(errorMessage)
        
        // 如果是ZIP损坏，3秒后返回试卷列表
        if (error.message && (error.message.includes('Invalid or unsupported zip format') || 
            error.message.includes('No END header found'))) {
          setTimeout(() => {
            this.$router.push('/paper-select')
          }, 3000)
        }
      }
    },

    playVolumeAudio() {
      // 完全参考 Notes.vue 的实现方式
      if (!this.volumeAudioPath) {
        // 如果没有卷别音频，直接等待3秒后跳转
        console.log('没有卷别音频，等待3秒后跳转')
        // TODO: 测试完成后取消注释
        // this.audioPlayTimer = setTimeout(() => {
        //   this.navigateToNext()
        // }, 3000)
        console.log('✓ 3秒跳转逻辑已注释，等待手动分析')
        return
      }

      const audio = this.$refs.volumeAudioPlayer
      if (!audio) {
        // 音频元素不存在，等待3秒后跳转
        console.log('音频元素不存在，等待3秒后跳转')
        // TODO: 测试完成后取消注释
        // this.audioPlayTimer = setTimeout(() => {
        //   this.navigateToNext()
        // }, 3000)
        console.log('✓ 3秒跳转逻辑已注释，等待手动分析')
        return
      }

      // 等待音频加载完成的回调函数
      const onCanPlay = () => {
        console.log('卷别音频可以播放，开始播放')
        
        // 获取音频时长（优先使用已设置的时长，否则使用音频元素的duration）
        const audioDuration = this.volumeAudioDuration || (audio.duration || 0)
        console.log(`音频时长: ${audioDuration}秒`)
        
        // 清除可能存在的其他定时器
        if (this.audioPlayTimer) {
          clearTimeout(this.audioPlayTimer)
          this.audioPlayTimer = null
        }
        
        // 使用"时长+3秒"算法：在音频开始播放时设置定时器
        // 定时器时间 = 音频时长（秒）* 1000 + 3000（3秒）
        if (audioDuration > 0) {
          const totalWaitTime = (audioDuration * 1000) + 3000
          console.log(`设置定时器: 音频时长(${audioDuration}秒) + 3秒 = ${totalWaitTime}毫秒`)
          this.audioPlayTimer = setTimeout(() => {
            console.log('✓ 定时器触发：音频应该已播放完成并等待3秒，开始跳转')
            this.handleAudioComplete()
          }, totalWaitTime)
        } else {
          // 如果无法获取时长，使用默认值（25秒 + 3秒，参考 manifest 中的 mediaDuration）
          const defaultDuration = 25
          const totalWaitTime = (defaultDuration * 1000) + 3000
          console.warn(`无法获取音频时长，使用默认值${defaultDuration}秒 + 3秒`)
          this.audioPlayTimer = setTimeout(() => {
            console.log('✓ 定时器触发（使用默认时长）：开始跳转')
            this.handleAudioComplete()
          }, totalWaitTime)
        }
        
        // 设置音量
        audio.volume = this.volume / 100
        
        // 开始播放音频
        audio.play().then(() => {
          console.log('✓ 卷别音频开始播放')
          // 启动音频频率可视化
          this.initAudioVisualization(audio)
        }).catch(error => {
          console.error('播放卷别音频失败:', error)
          // 播放失败也等待3秒后跳转
          if (this.audioPlayTimer) {
            clearTimeout(this.audioPlayTimer)
            this.audioPlayTimer = null
          }
          this.audioPlayTimer = setTimeout(() => {
            this.navigateToSectionList()
          }, 3000)
        })
      }

      const onError = () => {
        console.error('卷别音频加载失败')
        // 清除可能存在的其他定时器
        if (this.audioPlayTimer) {
          clearTimeout(this.audioPlayTimer)
          this.audioPlayTimer = null
        }
        // 加载失败也等待3秒后跳转
        this.audioPlayTimer = setTimeout(() => {
          this.navigateToSectionList()
        }, 3000)
      }

      // 检查音频是否已就绪
      if (audio.readyState >= 2) {
        // 音频已就绪，直接播放
        onCanPlay()
      } else {
        // 等待音频加载完成
        audio.addEventListener('canplay', onCanPlay, { once: true })
        audio.addEventListener('error', onError, { once: true })
        
        // 设置超时（10秒），如果音频一直无法加载，则跳转
        setTimeout(() => {
          if (audio.readyState < 2 && !this.audioPlayed) {
            console.error('卷别音频加载超时')
            // 清除可能存在的其他定时器
            if (this.audioPlayTimer) {
              clearTimeout(this.audioPlayTimer)
              this.audioPlayTimer = null
            }
            this.audioPlayTimer = setTimeout(() => {
              this.navigateToSectionList()
            }, 3000)
          }
        }, 10000)
      }
    },

    onVolumeAudioLoaded() {
      const audio = this.$refs.volumeAudioPlayer
      if (audio && audio.duration) {
        // 优先使用音频元素的duration（更准确），如果没有则使用manifest中的时长
        this.volumeAudioDuration = audio.duration || this.volumeAudioDuration
        console.log('卷别音频加载完成，时长:', this.volumeAudioDuration, '秒')
      }
    },

    onVolumeAudioCanPlay() {
      console.log('卷别音频可以播放')
    },

    onVolumeAudioEnded() {
      console.log('卷别音频播放完成 - ended 事件触发')
      // 标记音频已播放完成
      this.audioPlayed = true
      
      // 清除可能存在的其他定时器（时长+3秒的定时器）
      if (this.audioPlayTimer) {
        clearTimeout(this.audioPlayTimer)
        this.audioPlayTimer = null
      }
      
      // 音频播放完成后停留3秒，然后跳转到大题列表页面
      console.log('✓ 卷别音频播放完成，等待3秒后跳转到大题列表页面')
      this.audioPlayTimer = setTimeout(() => {
        console.log('✓ 3秒等待完成，跳转到大题列表页面')
        this.navigateToSectionList()
      }, 3000)
    },
    
    handleAudioComplete() {
      console.log('✓ 定时器触发：音频播放完成并等待3秒，开始跳转')
      this.audioPlayed = true
      
      // 清除可能存在的其他定时器
      if (this.audioPlayTimer) {
        clearTimeout(this.audioPlayTimer)
        this.audioPlayTimer = null
      }
      
      // 跳转到大题列表页面
      this.navigateToSectionList()
    },

    onVolumeAudioError(error) {
      console.error('卷别音频播放错误:', error)
      
      // 清除可能存在的其他定时器
      if (this.audioPlayTimer) {
        clearTimeout(this.audioPlayTimer)
        this.audioPlayTimer = null
      }
      
      // 播放错误也等待3秒后跳转
      this.audioPlayTimer = setTimeout(() => {
        this.navigateToSectionList()
      }, 3000)
    },

    onVolumeChange(value) {
      // 更新音量值
      this.volume = value
      // 更新音频元素的音量
      const audio = this.$refs.volumeAudioPlayer
      if (audio) {
        audio.volume = value / 100
        console.log('🎵 音量已更新:', value, '% (', audio.volume, ')')
      }
    },

    initAudioVisualization(audio) {
      try {
        // 创建 AudioContext
        const AudioContext = window.AudioContext || window.webkitAudioContext
        if (!AudioContext) {
          console.warn('浏览器不支持 Web Audio API')
          return
        }

        this.audioContext = new AudioContext()
        
        // 创建 AnalyserNode
        this.analyser = this.audioContext.createAnalyser()
        this.analyser.fftSize = 256 // 频率分析大小
        this.analyser.smoothingTimeConstant = 0.8 // 平滑系数
        
        // 创建数据数组
        const bufferLength = this.analyser.frequencyBinCount
        this.dataArray = new Uint8Array(bufferLength)
        
        // 连接音频源
        const source = this.audioContext.createMediaElementSource(audio)
        source.connect(this.analyser)
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
      
      // 绘制下波浪线（对称）
      ctx.beginPath()
      for (let i = 0; i < pointCount; i++) {
        // 从频率数据中采样
        const dataIndex = Math.floor((i / pointCount) * this.dataArray.length)
        const amplitude = (this.dataArray[dataIndex] / 255) * (height / 2 - 5) // 振幅（从中心线向下扩展，留出边距）
        
        const x = i
        const y = centerY + amplitude // 从中心线向下偏移
        
        if (i === 0) {
          ctx.moveTo(x, y)
        } else {
          ctx.lineTo(x, y)
        }
      }
      ctx.stroke()

      // 继续动画
      this.animationFrameId = requestAnimationFrame(() => {
        this.drawFrequencyBars()
      })
    },

    stopAudioVisualization() {
      if (this.animationFrameId) {
        cancelAnimationFrame(this.animationFrameId)
        this.animationFrameId = null
      }
      if (this.audioContext) {
        this.audioContext.close().catch(err => {
          console.warn('关闭 AudioContext 失败:', err)
        })
        this.audioContext = null
      }
      this.analyser = null
      this.dataArray = null
    },

    navigateToSectionList() {
      // 跳转到大题列表页面
      // 防止重复导航
      if (this.navigationTriggered) {
        console.log('导航已触发，跳过重复导航')
        return
      }
      
      // 检查当前路由，避免重复导航
      if (this.$route.path === '/section-list') {
        console.log('已在 /section-list 页面，跳过导航')
        return
      }
      
      this.navigationTriggered = true
      console.log('导航到大题列表页面')
      
      // 使用 catch 捕获重复导航错误
      this.$router.push('/section-list').catch(err => {
        // 忽略重复导航错误
        if (err.name !== 'NavigationDuplicated') {
          console.error('导航错误:', err)
        }
      })
    },

    navigateToNext() {
      // 跳转到下一个页面（答题页面）
      // 防止重复导航
      if (this.navigationTriggered) {
        console.log('导航已触发，跳过重复导航')
        return
      }
      
      // 检查当前路由，避免重复导航
      if (this.$route.path === '/exam') {
        console.log('已在 /exam 页面，跳过导航')
        return
      }
      
      this.navigationTriggered = true
      console.log('导航到答题页面')
      
      // 使用 catch 捕获重复导航错误
      this.$router.push('/exam').catch(err => {
        // 忽略重复导航错误
        if (err.name !== 'NavigationDuplicated') {
          console.error('导航错误:', err)
        }
      })
    }
  },
  beforeDestroy() {
    // 停止音频可视化
    this.stopAudioVisualization()
    
    // 清除定时器
    if (this.audioPlayTimer) {
      clearTimeout(this.audioPlayTimer)
      this.audioPlayTimer = null
    }
    
    // 停止音频
    if (this.$refs.volumeAudioPlayer) {
      this.$refs.volumeAudioPlayer.pause()
      this.$refs.volumeAudioPlayer.src = ''
    }
  }
}
</script>

<style scoped>
.broadcast-container {
  width: 100%;
  height: 100vh;
  background: #fff;
  display: flex;
  position: relative;
  padding-bottom: 60px; /* 为底部频率条留出空间 */
}

.main-content {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.paper-info-wrapper {
  width: 100%;
  max-width: 800px;
  background-color: rgba(0, 0, 0, 0.05); /* 灰色背景，跟注意事项背景div一样 */
  padding: 40px;
  border-radius: 8px;
  text-align: center;
  display: inline-block;
  min-width: 400px;
}

.paper-info-text {
  font-size: 32px;
  font-weight: bold;
  color: #409EFF; /* 蓝色高亮 */
  line-height: 1.6;
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

.volume-control {
  position: absolute;
  bottom: 80px;
  left: 50%;
  transform: translateX(-50%);
  width: calc(100% - 40px);
  max-width: 160px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.volume-slider {
  width: 100%;
}

.volume-info {
  display: flex;
  align-items: center;
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




