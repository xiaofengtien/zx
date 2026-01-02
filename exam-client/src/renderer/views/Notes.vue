<template>
  <div class="notes-container">
    <!-- 居中显示注意事项文本（不显示label，只显示文本内容） -->
    <div class="content-area">
      <div class="notes-text-wrapper">
        <div v-if="notesText" class="notes-text" v-html="notesText"></div>
      </div>
    </div>

    <!-- 音频播放器（隐藏，用于播放注意事项音频） -->
    <audio
      ref="notesAudioPlayer"
      :src="notesAudioPath"
      @ended="onNotesAudioEnded"
      @error="onNotesAudioError"
      @loadedmetadata="onNotesAudioLoaded"
      @canplay="onNotesAudioCanPlay"
    ></audio>
  </div>
</template>

<script>
const { ipcRenderer } = require('electron')
const path = require('path')
const fs = require('fs')
const { pathToFileURL } = require('url')

export default {
  name: 'Notes',
  data() {
    return {
      paperId: null,
      notesAudioPath: '',
      notesText: '', // 注意事项文本
      notesAudioDuration: 0,
      navigationTriggered: false, // 防止重复导航
      audioPlayed: false, // 音频是否已播放完成
      audioPlayTimer: null // 音频播放完成后的定时器
    }
  },
  async mounted() {
    // 获取试卷ID
    this.paperId = parseInt(localStorage.getItem('currentPaperId')) || null
    if (!this.paperId) {
      this.$message.error('未找到试卷ID，请返回选择页面')
      setTimeout(() => {
        this.$router.push('/paper-select')
      }, 2000)
      return
    }

    // 加载试卷数据
    await this.loadPaperData()
    
    // 自动播放注意事项音频
    this.playNotesAudio()
  },
  methods: {
    async loadPaperData() {
      try {
        console.log('📦 开始加载注意事项页面数据，paperId:', this.paperId)
        
        // 获取试卷包数据
        const paperData = await ipcRenderer.invoke('paper:getPaperData', this.paperId)
        if (!paperData || !paperData.manifest) {
          throw new Error('无法加载试卷数据')
        }

        const manifest = paperData.manifest
        const mediaDir = paperData.mediaDir || ''

        // 加载注意事项文本
        if (manifest.notes) {
          this.notesText = manifest.notes
          console.log('✓ 注意事项文本加载成功')
        } else {
          console.warn('⚠️ manifest中没有注意事项文本')
        }

        // 加载注意事项音频
        // 优先使用 introAudioPath/introAudioUrl（注意事项音频）
        // 如果没有，则使用 trialIntroAudioPath/trialIntroAudioUrl（试听旁白音频，兼容旧数据）
        const audioPath = manifest.introAudioPath || manifest.introAudioUrl || manifest.trialIntroAudioPath || manifest.trialIntroAudioUrl
        
        if (audioPath) {
          // 检查是否是完整URL
          if (audioPath && (audioPath.startsWith('http://') || audioPath.startsWith('https://'))) {
            // 完整URL，直接使用
            this.notesAudioPath = audioPath
            console.log('✓ 使用远程音频URL:', audioPath)
          } else {
            // 相对路径，从trial_intro/目录中查找（因为用户数据中音频在trial_intro目录）
            const trialIntroDir = path.join(mediaDir, 'trial_intro')
            const introDir = path.join(mediaDir, 'intro')
            
            // 优先从 trial_intro 目录查找
            let audioFilePath = null
            if (audioPath) {
              const fileName = path.basename(audioPath)
              if (fs.existsSync(trialIntroDir)) {
                const trialIntroFilePath = path.join(trialIntroDir, fileName)
                if (fs.existsSync(trialIntroFilePath)) {
                  audioFilePath = trialIntroFilePath
                }
              }
              if (!audioFilePath && fs.existsSync(introDir)) {
                const introFilePath = path.join(introDir, fileName)
                if (fs.existsSync(introFilePath)) {
                  audioFilePath = introFilePath
                }
              }
            }
            
            // 加载音频文件的辅助函数
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
                this.notesAudioPath = dataUrl
                console.log('✓ 注意事项音频加载成功（base64 data URL）:', filePath)
                return true
              } catch (error) {
                // 降级方案：使用 file:// URL
                this.notesAudioPath = pathToFileURL(filePath).href
                console.log('✓ 注意事项音频加载成功（file:// URL）:', filePath)
                return true
              }
            }
            
            // 扫描目录查找音频文件的辅助函数
            const scanDirectoryForAudio = () => {
              let foundAudioPath = null
              // 优先扫描 trial_intro 目录
              if (fs.existsSync(trialIntroDir)) {
                const files = fs.readdirSync(trialIntroDir)
                const audioFile = files.find(file => /\.(mp3|wav|m4a|aac|ogg)$/i.test(file))
                if (audioFile) {
                  foundAudioPath = path.join(trialIntroDir, audioFile)
                }
              }
              // 如果 trial_intro 没找到，尝试 intro 目录
              if (!foundAudioPath && fs.existsSync(introDir)) {
                const files = fs.readdirSync(introDir)
                const audioFile = files.find(file => /\.(mp3|wav|m4a|aac|ogg)$/i.test(file))
                if (audioFile) {
                  foundAudioPath = path.join(introDir, audioFile)
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
                console.warn(`注意事项音频不存在: ${audioPath}，尝试扫描目录`)
              }
              const foundAudioPath = scanDirectoryForAudio()
              if (foundAudioPath) {
                loadAudioFile(foundAudioPath)
              } else {
                console.warn(`trial_intro 和 intro 目录都不存在或没有音频文件`)
              }
            }
          }
          
          // 设置音频时长（优先使用 introAudioDuration，如果没有则使用 trialIntroAudioDuration）
          if (manifest.introAudioDuration) {
            this.notesAudioDuration = manifest.introAudioDuration
          } else if (manifest.trialIntroAudioDuration) {
            this.notesAudioDuration = manifest.trialIntroAudioDuration
          }
        } else {
          console.warn('⚠️ manifest中没有注意事项音频路径')
        }
      } catch (error) {
        console.error('加载试卷数据失败:', error)
        this.$message.error('加载试卷数据失败: ' + error.message)
      }
    },

    playNotesAudio() {
      if (!this.notesAudioPath) {
        // 如果没有注意事项音频，直接等待3秒后跳转
        console.log('没有注意事项音频，等待3秒后跳转')
        // TODO: 暂时注释掉，用于逐步分析
        // this.audioPlayTimer = setTimeout(() => {
        //   this.navigateToReady()
        // }, 3000)
        console.log('✓ 3秒跳转逻辑已注释，等待手动分析')
        return
      }

      const audio = this.$refs.notesAudioPlayer
      if (!audio) {
        // 音频元素不存在，等待3秒后跳转
        console.log('音频元素不存在，等待3秒后跳转')
        // TODO: 暂时注释掉，用于逐步分析
        // this.audioPlayTimer = setTimeout(() => {
        //   this.navigateToReady()
        // }, 3000)
        console.log('✓ 3秒跳转逻辑已注释，等待手动分析')
        return
      }

      // 等待音频加载完成的回调函数
      const onCanPlay = () => {
        console.log('注意事项音频可以播放，开始播放')
        
        // 获取音频时长（优先使用已设置的时长，否则使用音频元素的duration）
        const audioDuration = this.notesAudioDuration || (audio.duration || 0)
        console.log(`音频时长: ${audioDuration}秒`)
        
        // 清除可能存在的其他定时器
        if (this.audioPlayTimer) {
          clearTimeout(this.audioPlayTimer)
          this.audioPlayTimer = null
        }
        
        // 使用"时长+3秒"算法：在音频开始播放时设置定时器
        // 定时器时间 = 音频时长（秒）* 1000 + 3000（3秒）
        if (audioDuration > 0) {
          const totalWaitTime = (audioDuration * 1000) + 500
          console.log(`设置定时器: 音频时长(${audioDuration}秒) + 0.5秒 = ${totalWaitTime}毫秒`)
          this.audioPlayTimer = setTimeout(() => {
            console.log('✓ 定时器触发：音频应该已播放完成并等待3秒，开始跳转')
            this.handleAudioComplete()
          }, totalWaitTime)
        } else {
          // 如果无法获取时长，使用默认值（251秒 + 3秒）
          const defaultDuration = 251
          const totalWaitTime = (defaultDuration * 1000) + 500
          console.warn(`无法获取音频时长，使用默认值${defaultDuration}秒 + 0.5秒`)
          this.audioPlayTimer = setTimeout(() => {
            console.log('✓ 定时器触发（使用默认时长）：开始跳转')
            this.handleAudioComplete()
          }, totalWaitTime)
        }
        
        // 开始播放音频
        audio.play().then(() => {
          console.log('✓ 注意事项音频开始播放')
        }).catch(error => {
          console.error('播放注意事项音频失败:', error)
          // 播放失败也等待3秒后跳转
          if (this.audioPlayTimer) {
            clearTimeout(this.audioPlayTimer)
            this.audioPlayTimer = null
          }
          this.audioPlayTimer = setTimeout(() => {
            this.navigateToReady()
          }, 3000)
        })
      }

      const onError = () => {
        console.error('注意事项音频加载失败')
        // 清除可能存在的其他定时器
        if (this.audioPlayTimer) {
          clearTimeout(this.audioPlayTimer)
          this.audioPlayTimer = null
        }
        // 加载失败也等待3秒后跳转
        this.audioPlayTimer = setTimeout(() => {
          this.navigateToReady()
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
            console.error('注意事项音频加载超时')
            // 清除可能存在的其他定时器
            if (this.audioPlayTimer) {
              clearTimeout(this.audioPlayTimer)
              this.audioPlayTimer = null
            }
            this.audioPlayTimer = setTimeout(() => {
              this.navigateToReady()
            }, 3000)
          }
        }, 10000)
      }
    },

    onNotesAudioLoaded() {
      const audio = this.$refs.notesAudioPlayer
      if (audio && audio.duration) {
        // 优先使用音频元素的duration（更准确），如果没有则使用manifest中的时长
        this.notesAudioDuration = audio.duration || this.notesAudioDuration
        console.log('注意事项音频加载完成，时长:', this.notesAudioDuration, '秒')
        console.log('音频元素duration:', audio.duration, '秒')
        console.log('manifest中的时长:', this.notesAudioDuration, '秒')
      } else {
        console.log('注意事项音频加载完成，但无法获取时长，使用manifest中的时长:', this.notesAudioDuration, '秒')
      }
    },

    onNotesAudioCanPlay() {
      console.log('注意事项音频可以播放')
    },

    onNotesAudioEnded() {
      console.log('注意事项音频播放完成 - ended 事件触发')
      // 注意：不再在这里处理跳转，因为已经在播放开始时设置了"时长+3秒"的定时器
      // 这里只做验证和日志记录
      const audio = this.$refs.notesAudioPlayer
      
      if (audio) {
        const currentTime = audio.currentTime || 0
        const duration = audio.duration || this.notesAudioDuration || 0
        
        console.log(`音频播放完成检查: currentTime=${currentTime.toFixed(2)}秒, duration=${duration.toFixed(2)}秒, notesAudioDuration=${this.notesAudioDuration}秒`)
        
        // 验证音频是否真的播放完成
        if (duration > 0 && currentTime < duration - 2) {
          console.warn(`⚠️ 音频可能未完整播放: currentTime=${currentTime.toFixed(2)}秒, duration=${duration.toFixed(2)}秒`)
          // 如果提前结束，尝试继续播放
          setTimeout(() => {
            const newCurrentTime = audio.currentTime || 0
            if (newCurrentTime < duration - 1) {
              console.warn('音频确实未完整播放，尝试继续播放...')
              audio.currentTime = duration - 0.5
              audio.play().catch(e => {
                console.error('继续播放失败:', e)
              })
            }
          }, 500)
        } else {
          console.log('✓ 音频播放完成验证通过')
        }
      }
      
      // 标记音频已播放完成（但不立即跳转，等待定时器）
      this.audioPlayed = true
    },
    
    handleAudioComplete() {
      console.log('✓ 定时器触发：音频播放完成并等待3秒，开始跳转')
      this.audioPlayed = true
      
      // 清除可能存在的其他定时器（虽然理论上不应该有）
      if (this.audioPlayTimer) {
        clearTimeout(this.audioPlayTimer)
        this.audioPlayTimer = null
      }
      
      // 直接跳转（因为定时器已经包含了3秒等待）
      this.navigateToReady()
    },

    onNotesAudioError(error) {
      console.error('注意事项音频播放错误:', error)
      
      // 清除可能存在的其他定时器
      if (this.audioPlayTimer) {
        clearTimeout(this.audioPlayTimer)
        this.audioPlayTimer = null
      }
      
      // 播放错误也等待3秒后跳转
      this.audioPlayTimer = setTimeout(() => {
        this.navigateToReady()
      }, 3000)
    },

    navigateToReady() {
      // 跳转到准备页面
      // 防止重复导航
      if (this.navigationTriggered) {
        console.log('导航已触发，跳过重复导航')
        return
      }
      
      // 检查当前路由，避免重复导航
      if (this.$route.path === '/ready') {
        console.log('已在 /ready 页面，跳过导航')
        return
      }
      
      this.navigationTriggered = true
      console.log('导航到准备页面')
      
      // 使用 catch 捕获重复导航错误
      this.$router.push('/ready').catch(err => {
        // 忽略重复导航错误
        if (err.name !== 'NavigationDuplicated') {
          console.error('导航错误:', err)
        }
      })
    }
  },
  beforeDestroy() {
    // 清除定时器
    if (this.audioPlayTimer) {
      clearTimeout(this.audioPlayTimer)
      this.audioPlayTimer = null
    }
    
    // 停止音频
    if (this.$refs.notesAudioPlayer) {
      this.$refs.notesAudioPlayer.pause()
      this.$refs.notesAudioPlayer.src = ''
    }
  }
}
</script>

<style scoped>
.notes-container {
  width: 100%;
  height: 100vh;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.content-area {
  max-width: 800px;
  padding: 40px;
  text-align: left;
}

.notes-text-wrapper {
  background-color: rgba(0, 0, 0, 0.05);
  border-radius: 8px;
  padding: 30px 40px;
  display: inline-block;
  min-width: 400px;
  max-width: 100%;
}

.notes-text {
  font-size: 20px;
  color: #303133;
  line-height: 2;
  text-align: left;
  white-space: pre-wrap;
  word-wrap: break-word;
}

/* 富文本样式支持 */
.notes-text p {
  margin: 0 0 15px 0;
  text-align: left;
}
</style>









