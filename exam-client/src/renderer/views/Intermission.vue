<template>
  <div class="intermission-container">
    <!-- 居中显示中场文本（完全复制注意事项页面的样式） -->
    <div class="content-area">
      <div class="intermission-text-wrapper">
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
      intermissionText: '',
      intermissionAudioPath: '',
      intermissionAudioDuration: 0,
      navigationTriggered: false,
      audioPlayed: false,
      audioPlayTimer: null,
      volume: 50
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
  },
  methods: {
    async loadIntermissionData() {
      try {
        console.log('📦 [Intermission] 开始加载中场数据，paperId:', this.paperId, 'fromVolume:', this.fromVolume, 'toVolume:', this.toVolume)
        
        // 获取中场配置
        const intermissions = await ipcRenderer.invoke('paper:getIntermissions', this.paperId)
        const intermission = intermissions.find(i => 
          i.from_volume === this.fromVolume && i.to_volume === this.toVolume
        )

        if (!intermission) {
          console.warn('⚠️ 未找到中场配置，直接跳转到下一卷别')
          setTimeout(() => {
            this.navigateToBroadcast()
          }, 1000)
          return
        }

        // 加载中场文本
        if (intermission.intermission_text) {
          this.intermissionText = intermission.intermission_text
          console.log('✓ 中场文本加载成功')
        }

        // 加载中场音频
        const mediaFiles = await ipcRenderer.invoke('paper:getMediaFiles', {
          paperId: this.paperId,
          intermissionId: intermission.id,
          mediaType: 9 // 中场音频类型
        })

        if (mediaFiles.length > 0) {
          const mediaFile = mediaFiles[0]
          const mediaPath = mediaFile.media_path || mediaFile.media_url || ''
          
          if (mediaPath) {
            if (mediaPath.startsWith('http://') || mediaPath.startsWith('https://')) {
              this.intermissionAudioPath = mediaPath
            } else {
              // 从本地媒体目录加载
              const paperData = await ipcRenderer.invoke('paper:getPaperData', this.paperId)
              const mediaDir = paperData?.mediaDir || ''
              const audioFilePath = path.join(mediaDir, mediaPath)
              
              if (fs.existsSync(audioFilePath)) {
                this.intermissionAudioPath = pathToFileURL(audioFilePath).href
              } else {
                console.warn('⚠️ 中场音频文件不存在:', audioFilePath)
              }
            }
          }

          // 获取音频时长
          if (mediaFile.media_duration) {
            this.intermissionAudioDuration = mediaFile.media_duration
          }
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
          const totalWaitTime = (audioDuration * 1000) + 3000
          console.log(`设置定时器: 音频时长(${audioDuration}秒) + 3秒 = ${totalWaitTime}毫秒`)
          this.audioPlayTimer = setTimeout(() => {
            console.log('✓ 中场音频播放完成并等待3秒，开始跳转')
            this.navigateToBroadcast()
          }, totalWaitTime)
        } else {
          // 如果无法获取时长，使用默认值
          const defaultDuration = 60
          const totalWaitTime = (defaultDuration * 1000) + 3000
          console.warn(`无法获取音频时长，使用默认值${defaultDuration}秒 + 3秒`)
          this.audioPlayTimer = setTimeout(() => {
            this.navigateToBroadcast()
          }, totalWaitTime)
        }
        
        // 开始播放音频
        audio.play().then(() => {
          console.log('✓ 中场音频开始播放')
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
    },

    onIntermissionAudioError(error) {
      console.error('中场音频播放错误:', error)
      if (this.audioPlayTimer) {
        clearTimeout(this.audioPlayTimer)
        this.audioPlayTimer = null
      }
      this.audioPlayTimer = setTimeout(() => {
        this.navigateToBroadcast()
      }, 3000)
    },

    navigateToBroadcast() {
      if (this.navigationTriggered) {
        return
      }
      this.navigationTriggered = true

      console.log('跳转到卷别播报页面')
      this.$router.push('/broadcast').catch(err => {
        if (err.name !== 'NavigationDuplicated') {
          console.error('导航错误:', err)
        }
      })
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

.intermission-text >>> p {
  margin-bottom: 16px;
}

.intermission-text >>> p:last-child {
  margin-bottom: 0;
}
</style>


