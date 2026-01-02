<template>
  <div class="intermission-container">
    <!-- 居中显示中场文本（完全复制注意事项页面的样式） -->
    <div class="content-area">
      <div class="intermission-text-wrapper">
        <div class="intermission-audio-indicator" v-if="intermissionAudioPath">
          <span class="pulse-dot"></span>
          <span>{{ intermissionAudioPlaying ? '中场音频播放中' : '中场结束' }}</span>
        </div>
        <div v-if="intermissionText" class="intermission-text" v-html="intermissionText"></div>
      </div>
    </div>

    <!-- 音频播放器（隐藏，用于播放中场音频） -->
    <audio
      ref="intermissionAudioPlayer"
      :src="intermissionAudioPath"
      @ended="onIntermissionAudioEnded"
      @error="onIntermissionAudioError"
      @loadedmetadata="onIntermissionAudioLoaded"
      @canplay="onIntermissionAudioCanPlay"
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

export default {
  name: 'Intermission',
  data() {
    return {
      paperId: null,
      paperInfoId: null,
      fromVolume: '',
      toVolume: '',
      toVolumeId: null,
      intermissionText: '',
      intermissionAudioPath: '',
      intermissionAudioDuration: 0,
      navigationTriggered: false,
      audioPlayed: false,
      audioPlayTimer: null,
      volume: 50,
      intermissionAudioPlaying: false,
      // 音频频率可视化
      audioContext: null,
      analyser: null,
      dataArray: null,
      animationFrameId: null,
      _audioSources: {}
    }
  },
  async mounted() {
    // 获取路由参数
    this.paperId = parseInt(this.$route.query.paperId) || parseInt(localStorage.getItem('currentPaperId')) || null
    this.paperInfoId = parseInt(this.$route.query.paperInfoId) || null
    this.fromVolume = this.$route.query.fromVolume || localStorage.getItem('completedVolumeCode') || ''
    this.toVolume = this.$route.query.toVolume || ''

    if (!this.paperId) {
      this.$message.error('未找到试卷ID')
      setTimeout(() => {
        this.$router.push('/paper-select')
      }, 2000)
      return
    }

    // 加载音量设置
    const savedVolume = localStorage.getItem('volume')
    if (savedVolume) {
      this.volume = parseInt(savedVolume)
    }

    // 加载中场数据
    await this.loadIntermissionData()
    
    // 自动播放中场音频
    this.playIntermissionAudio()
  },
  beforeDestroy() {
    if (this.audioPlayTimer) {
      clearTimeout(this.audioPlayTimer)
    }
    this.destroyAudioVisualization()
  },
  methods: {
    async loadIntermissionData() {
      try {
        
        // 拉取 paperData 获取卷信息与 mediaDir
        const paperData = await ipcRenderer.invoke('paper:getPaperData', this.paperId)
        const mediaDir = paperData?.mediaDir || ''
        const volumes = paperData?.manifest?.volumes || []

        // 解析 from/to 卷的 ID/code/name
        const norm = (v) => (v === undefined || v === null) ? '' : String(v).trim()
        const fromVol = volumes.find(v => {
          const vid = v.id || v.volumeId || v.volume_id
          const vcode = norm(v.volumeCode || v.volume_code)
          const vname = norm(v.volumeName || v.volume_name)
          return (this.$route.query.fromVolumeId && vid && Number(vid) === Number(this.$route.query.fromVolumeId)) ||
                 (vcode && norm(this.fromVolume) && vcode === norm(this.fromVolume)) ||
                 (vname && norm(this.fromVolume) && vname === norm(this.fromVolume))
        }) || null

        const toVol = volumes.find(v => {
          const vid = v.id || v.volumeId || v.volume_id
          const vcode = norm(v.volumeCode || v.volume_code)
          const vname = norm(v.volumeName || v.volume_name)
          return (this.$route.query.toVolumeId && vid && Number(vid) === Number(this.$route.query.toVolumeId)) ||
                 (vcode && norm(this.toVolume) && vcode === norm(this.toVolume)) ||
                 (vname && norm(this.toVolume) && vname === norm(this.toVolume))
        }) || null

        const fromIdResolved = fromVol?.id || fromVol?.volumeId || fromVol?.volume_id
        const toIdResolved = toVol?.id || toVol?.volumeId || toVol?.volume_id
        const fromCodeResolved = norm(fromVol?.volumeCode || fromVol?.volume_code || this.fromVolume)
        const toCodeResolved = norm(toVol?.volumeCode || toVol?.volume_code || this.toVolume)
        const fromNameResolved = norm(fromVol?.volumeName || fromVol?.volume_name || this.fromVolume)
        const toNameResolved = norm(toVol?.volumeName || toVol?.volume_name || this.toVolume)


        // 获取中场配置
        const intermissions = await ipcRenderer.invoke('paper:getIntermissions', this.paperId)
        const intermission = intermissions.find(i => {
          const fid = i.from_volume_id || i.fromVolumeId || null
          const tid = i.to_volume_id || i.toVolumeId || null
          const fcode = norm(i.from_volume)
          const tcode = norm(i.to_volume)
          const fname = norm(i.from_volume)
          const tname = norm(i.to_volume)

          const matchFrom = (fid && fromIdResolved && Number(fid) === Number(fromIdResolved)) ||
                            (fid && this.$route.query.fromVolumeId && Number(fid) === Number(this.$route.query.fromVolumeId)) ||
                            (fcode && fromCodeResolved && fcode === fromCodeResolved) ||
                            (fname && fromNameResolved && fname === fromNameResolved)

          const matchTo = (tid && toIdResolved && Number(tid) === Number(toIdResolved)) ||
                          (tid && this.$route.query.toVolumeId && Number(tid) === Number(this.$route.query.toVolumeId)) ||
                          (tcode && toCodeResolved && tcode === toCodeResolved) ||
                          (tname && toNameResolved && tname === toNameResolved)

          return matchFrom && matchTo
        })

        if (!intermission) {
          console.warn('⚠️ 未找到中场配置，直接跳转到下一卷别')
          setTimeout(() => {
            this.navigateToBroadcast()
          }, 1000)
          return
        }

        // 记录下一卷别的ID（如果可用）
        this.toVolumeId = intermission.to_volume_id || intermission.toVolumeId || toIdResolved || null

        // 加载中场文本
        if (intermission.intermission_text) {
          this.intermissionText = intermission.intermission_text
          console.log('✓ 中场文本加载成功')
        }

        // 加载中场音频，优先从 manifest 获取（驼峰字段），再 fallback SQLite（下划线字段）
        // 先尝试从 manifest.intermissions 获取完整的中场配置（包含音频路径）
        const manifestIntermissions = paperData?.manifest?.intermissions || []
        const manifestIntermission = manifestIntermissions.find(mi => {
          const miFid = mi.fromVolumeId || mi.from_volume_id
          const miTid = mi.toVolumeId || mi.to_volume_id
          return (miFid && fromIdResolved && Number(miFid) === Number(fromIdResolved)) ||
                 (miTid && toIdResolved && Number(miTid) === Number(toIdResolved)) ||
                 (mi.fromVolume === fromNameResolved && mi.toVolume === toNameResolved)
        }) || null

        // 合并 manifest 和 SQLite 的字段（manifest 优先）
        const audioPath = manifestIntermission?.intermissionAudioPath ||
                          intermission.intermissionAudioPath ||
                          intermission.intermission_audio_path || ''
        const audioUrl = manifestIntermission?.intermissionAudioUrl ||
                         intermission.intermissionAudioUrl ||
                         intermission.intermission_audio_url || ''
        const audioDuration = manifestIntermission?.intermissionAudioDuration ||
                              intermission.intermissionAudioDuration ||
                              intermission.intermission_audio_duration || 0

        const resolvedAudioPath = await this.resolveAudioPathDirect(audioPath, audioUrl, mediaDir)
        
        if (resolvedAudioPath) {
          this.intermissionAudioPath = resolvedAudioPath
        }
        
        if (audioDuration) {
          this.intermissionAudioDuration = audioDuration
        }

        // 标记中场音频已播放
        if (this.paperInfoId) {
          await ipcRenderer.invoke('answer:markIntermissionPlayed', 
            this.paperInfoId,
            this.fromVolume,
            this.toVolume
          )
        }
      } catch (error) {
        console.error('加载中场数据失败:', error)
      }
    },

    playIntermissionAudio() {
      if (this.navigationTriggered) {
        return
      }

      const audio = this.$refs.intermissionAudioPlayer
      if (!audio || !this.intermissionAudioPath) {
        console.log('中场音频不存在，等待3秒后跳转')
        this.audioPlayTimer = setTimeout(() => {
          this.navigateToBroadcast()
        }, 3000)
        return
      }

      // 确保重新加载音频源
      try {
        audio.load()
      } catch (e) {}

      // 设置音量
      audio.volume = this.volume / 100

      const onCanPlay = () => {
        console.log('中场音频可以播放，开始播放')
        
        const audioDuration = this.intermissionAudioDuration || (audio.duration || 0)
        console.log(`中场音频时长: ${audioDuration}秒`)

        if (this.audioPlayTimer) {
          clearTimeout(this.audioPlayTimer)
          this.audioPlayTimer = null
        }

        // 音频播放完成后等待3秒再跳转
        if (audioDuration > 0) {
          const totalWaitTime = (audioDuration * 1000) + 500
          console.log(`设置定时器: 音频时长(${audioDuration}秒) +0.5秒 = ${totalWaitTime}毫秒`)
          this.audioPlayTimer = setTimeout(() => {
            console.log('✓ 中场音频播放完成并等待3秒，开始跳转')
            this.navigateToBroadcast()
          }, totalWaitTime)
        } else {
          // 如果无法获取时长，使用默认值
          const defaultDuration = 60
          const totalWaitTime = (defaultDuration * 1000) + 500
          console.warn(`无法获取音频时长，使用默认值${defaultDuration}秒 + 3秒`)
          this.audioPlayTimer = setTimeout(() => {
            this.navigateToBroadcast()
          }, totalWaitTime)
        }
        
        // 开始播放音频
        audio.play().then(() => {
          console.log('✓ 中场音频开始播放')
          this.intermissionAudioPlaying = true
          // 初始化音频可视化
          this.initAudioVisualization(audio)
        }).catch(error => {
          console.error('播放中场音频失败:', error)
          if (this.audioPlayTimer) {
            clearTimeout(this.audioPlayTimer)
            this.audioPlayTimer = null
          }
          this.audioPlayTimer = setTimeout(() => {
            this.navigateToBroadcast()
          }, 3000)
        })
      }

      const onError = () => {
        console.error('中场音频加载失败')
        this.intermissionAudioPlaying = false
        if (this.audioPlayTimer) {
          clearTimeout(this.audioPlayTimer)
          this.audioPlayTimer = null
        }
        this.audioPlayTimer = setTimeout(() => {
          this.navigateToBroadcast()
        }, 3000)
      }

      if (audio.readyState >= 2) {
        onCanPlay()
      } else {
        audio.addEventListener('canplay', onCanPlay, { once: true })
        audio.addEventListener('error', onError, { once: true })
        
        setTimeout(() => {
          if (audio.readyState < 2 && !this.audioPlayed) {
            console.error('中场音频加载超时')
            if (this.audioPlayTimer) {
              clearTimeout(this.audioPlayTimer)
              this.audioPlayTimer = null
            }
            this.audioPlayTimer = setTimeout(() => {
              this.navigateToBroadcast()
            }, 3000)
          }
        }, 10000)
      }
    },

    onIntermissionAudioLoaded() {
      const audio = this.$refs.intermissionAudioPlayer
      if (audio && audio.duration) {
        this.intermissionAudioDuration = audio.duration || this.intermissionAudioDuration
        console.log('中场音频加载完成，时长:', this.intermissionAudioDuration, '秒')
      }
    },

    onIntermissionAudioCanPlay() {
      console.log('中场音频可以播放')
    },

    onIntermissionAudioEnded() {
      console.log('中场音频播放完成 - ended 事件触发')
      this.audioPlayed = true
      this.intermissionAudioPlaying = false
    },

    onIntermissionAudioError(error) {
      console.error('中场音频播放错误:', error)
      this.intermissionAudioPlaying = false
      if (this.audioPlayTimer) {
        clearTimeout(this.audioPlayTimer)
        this.audioPlayTimer = null
      }
      this.audioPlayTimer = setTimeout(() => {
        this.navigateToBroadcast()
      }, 3000)
    },

    /**
     * 直接解析音频路径（传入已提取的 audioPath 和 audioUrl）
     * 返回 base64 data URL（参考 SectionList.vue 的实现）
     */
    async resolveAudioPathDirect(audioPath, audioUrl, mediaDir) {
      try {
        if (audioPath && mediaDir) {
          const candidates = []
          // 候选1：如果已带 intermission/ 前缀则直接用，否则加上
          const rel = audioPath.startsWith('intermission/') ? audioPath : path.join('intermission', audioPath)
          candidates.push(path.join(mediaDir, rel))
          // 候选2：直接拼接
          candidates.push(path.join(mediaDir, audioPath))
          // 候选3：去掉 media/<code>/ 前缀
          candidates.push(path.join(mediaDir, audioPath.replace(/^media\/[^/]+\//, '')))
          // 候选4：只取文件名
          const fileName = path.basename(audioPath)
          candidates.push(path.join(mediaDir, 'intermission', fileName))

          for (const full of candidates) {
            if (full && fs.existsSync(full)) {
              // 读取文件并转成 base64 data URL（参考 SectionList.vue）
              try {
                const fileBuffer = fs.readFileSync(full)
                const ext = path.extname(full).toLowerCase()
                let mimeType = 'audio/mpeg'
                if (ext === '.wav') mimeType = 'audio/wav'
                else if (ext === '.m4a' || ext === '.aac') mimeType = 'audio/mp4'
                else if (ext === '.ogg') mimeType = 'audio/ogg'
                const base64 = fileBuffer.toString('base64')
                const dataUrl = `data:${mimeType};base64,${base64}`
                return dataUrl
              } catch (readErr) {
                // 降级：使用 file:// URL
                return pathToFileURL(full).href
              }
            }
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

    navigateToBroadcast() {
      if (this.navigationTriggered) {
        return
      }
      this.navigationTriggered = true

      // 在跳转前统一写入当前卷别信息：ID优先、CODE回退
      try {
        // 先写入 code（方便回退场景）
        localStorage.setItem('currentVolumeCode', this.toVolume || '')

        if (this.toVolumeId) {
          localStorage.setItem('currentVolumeId', String(this.toVolumeId))
        } else {
          // 如果没有ID，尝试从 manifest 中根据 code 解析出 ID
          if (this.paperId && this.toVolume) {
            ipcRenderer.invoke('paper:getPaperData', this.paperId).then(paperData => {
              const volumes = paperData?.manifest?.volumes || []
              const match = volumes.find(v => (v.volumeCode || v.volume_code || '') === this.toVolume)
              if (match && match.id) {
                localStorage.setItem('currentVolumeId', String(match.id))
              } else {
                console.warn('⚠️ [Intermission] 未能通过 manifest 解析到下一卷别ID，保留现有ID')
              }
            }).catch(err => {
              console.warn('⚠️ [Intermission] 解析下一卷别ID失败:', err)
            })
          }
        }
      } catch (e) {
        console.warn('⚠️ [Intermission] 写入当前卷别信息失败（不影响跳转）:', e)
      }

      console.log('跳转到卷别播报页面')
      this.$router.push({
        path: '/broadcast',
        query: {
          fromIntermission: 'true'
        }
      }).catch(err => {
        if (err.name !== 'NavigationDuplicated') {
          console.error('导航错误:', err)
        }
      })
    },

    // 初始化音频可视化
    initAudioVisualization(audio) {
      try {
        if (!audio) return

        const audioId = 'intermission-audio'

        // 如果已经创建过 MediaElementSource，直接复用
        if (this._audioSources && this._audioSources[audioId]) {
          if (this.audioContext && this.audioContext.state === 'suspended') {
            this.audioContext.resume()
          }
          if (!this.analyser) {
            this.analyser = this.audioContext.createAnalyser()
            this.analyser.fftSize = 256
            this.analyser.smoothingTimeConstant = 0.8
            this.dataArray = new Uint8Array(this.analyser.frequencyBinCount)
          }
          try {
            this._audioSources[audioId].disconnect()
          } catch (e) { /* 忽略断开错误 */ }
          this._audioSources[audioId].connect(this.analyser)
          this.analyser.connect(this.audioContext.destination)
          this.drawFrequencyBars()
          return
        }

        if (!this._audioSources) {
          this._audioSources = {}
        }

        if (!this.audioContext) {
          this.audioContext = new AudioContext()
        }

        if (!this.analyser) {
          this.analyser = this.audioContext.createAnalyser()
          this.analyser.fftSize = 256
          this.analyser.smoothingTimeConstant = 0.8
          this.dataArray = new Uint8Array(this.analyser.frequencyBinCount)
        }

        this._audioSources[audioId] = this.audioContext.createMediaElementSource(audio)
        this._audioSources[audioId].connect(this.analyser)
        this.analyser.connect(this.audioContext.destination)

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

      if (canvas.width !== canvas.offsetWidth) {
        canvas.width = canvas.offsetWidth
      }
      if (canvas.height !== canvas.offsetHeight) {
        canvas.height = canvas.offsetHeight
      }

      const width = canvas.width
      const height = canvas.height

      this.analyser.getByteFrequencyData(this.dataArray)

      ctx.fillStyle = '#409EFF'
      ctx.fillRect(0, 0, width, height)

      const centerY = height / 2
      const pointCount = width

      ctx.strokeStyle = '#FFFFFF'
      ctx.lineWidth = 1.5
      ctx.lineCap = 'round'
      ctx.lineJoin = 'round'

      // 绘制上波浪线
      ctx.beginPath()
      for (let i = 0; i < pointCount; i++) {
        const dataIndex = Math.floor((i / pointCount) * this.dataArray.length)
        const amplitude = (this.dataArray[dataIndex] / 255) * (height / 2 - 5)
        const x = i
        const y = centerY - amplitude
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
        const y = centerY + amplitude
        if (i === 0) {
          ctx.moveTo(x, y)
        } else {
          ctx.lineTo(x, y)
        }
      }
      ctx.stroke()

      this.animationFrameId = requestAnimationFrame(() => {
        this.drawFrequencyBars()
      })
    },

    stopAudioVisualization() {
      if (this.animationFrameId) {
        cancelAnimationFrame(this.animationFrameId)
        this.animationFrameId = null
      }
      this.analyser = null
      this.dataArray = null
    },

    destroyAudioVisualization() {
      this.stopAudioVisualization()
      if (this.audioContext) {
        this.audioContext.close().catch(err => {
          console.warn('关闭 AudioContext 失败:', err)
        })
        this.audioContext = null
      }
      this._audioSources = {}
    }
  }
}
</script>

<style scoped>
.intermission-container {
  min-height: 100vh;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
}

.content-area {
  width: 100%;
  max-width: 800px;
  padding: 40px;
}

.intermission-text-wrapper {
  background: white;
  border-radius: 8px;
  padding: 40px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.intermission-text {
  font-size: 18px;
  line-height: 1.8;
  color: #303133;
  text-align: left;
}

.intermission-audio-indicator {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #409eff;
  font-weight: 600;
  margin-bottom: 12px;
}

.pulse-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #409eff;
  position: relative;
  animation: pulse 1.4s infinite;
}

@keyframes pulse {
  0% { box-shadow: 0 0 0 0 rgba(64,158,255,0.4); }
  70% { box-shadow: 0 0 0 10px rgba(64,158,255,0); }
  100% { box-shadow: 0 0 0 0 rgba(64,158,255,0); }
}

.intermission-text >>> p {
  margin-bottom: 16px;
}

.intermission-text >>> p:last-child {
  margin-bottom: 0;
}

/* 音频频率动画（页面最底部） */
.audio-frequency-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  width: 100%;
  height: 30px;
  background-color: #409EFF;
  z-index: 1000;
}

.frequency-canvas {
  width: 100%;
  height: 100%;
  display: block;
}
</style>

