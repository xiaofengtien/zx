<template>
  <div class="ready-container">
    <!-- 完整包已加载完成，显示成功提示 -->
    <div v-if="fullPackageReady" class="success-state">
      <div class="success-content">
        <div class="success-message-wrapper">
          <div class="success-line-1">
            <div class="success-icon-wrapper">
              <i class="el-icon-success success-check-icon"></i>
            </div>
            <span class="success-text-1">试卷下载成功，测试准备就绪</span>
          </div>
          <div class="success-line-2">
            <span class="success-text-2">请安静等待测试开始，不要再动耳机、键盘、鼠标等设备</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 完整包未加载完成，显示下载状态 -->
    <div v-else-if="!downloadError" class="loading-state">
      <div class="loading-content">
        <el-icon class="loading-icon">
          <i class="el-icon-loading"></i>
        </el-icon>
        <h3 class="loading-title">正在下载试卷包...</h3>
        <div class="progress-info">
          <el-progress 
            :percentage="downloadProgress" 
            :status="downloadProgress === 100 ? 'success' : null"
            :stroke-width="8"
          ></el-progress>
          <p class="progress-text">{{ downloadStatusMessage }}</p>
        </div>
      </div>
    </div>

    <!-- 下载失败状态 -->
    <div v-else class="error-state">
      <div class="error-content">
        <el-icon class="error-icon">
          <i class="el-icon-error"></i>
        </el-icon>
        <h3 class="error-title">试卷包下载失败</h3>
        <p class="error-message">{{ downloadError }}</p>
        <el-button type="primary" @click="retryDownload">重新下载</el-button>
      </div>
    </div>
  </div>
</template>

<script>
const { ipcRenderer } = require('electron')

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

export default {
  name: 'Ready',
  data() {
    return {
      paperId: null,
      fullPackageReady: false,
      downloadProgress: 0,
      downloadStatusMessage: '正在检查下载状态...',
      downloadError: null,
      checkInterval: null,
      readyDisplayed: false,
      navigationTriggered: false, // 防止重复导航
      downloadStartTime: null, // 下载开始时间
      downloadTimeout: 60000, // 下载超时时间（60秒，如果超时说明下载有问题）
      lastProgressTime: null, // 上次进度更新时间
      lastProgress: 0, // 上次进度值
      progressStuckTimeout: 30000 // 进度卡住超时（30秒，如果卡住说明下载有问题）
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

    // 检查完整包是否已下载完成
    await this.checkFullPackageStatus()
  },
  methods: {
    async checkFullPackageStatus() {
      try {
        console.log('📦 [Ready] 开始检查完整包状态，paperId:', this.paperId)
        
        // 先检查是否有正在进行的下载任务
        const downloadStatus = await ipcRenderer.invoke('paper:getDownloadStatus', this.paperId)
        console.log('📦 [Ready] 下载状态:', downloadStatus)
        
        if (downloadStatus && downloadStatus.status === 'downloading') {
          // 有正在进行的下载任务，等待下载完成，不要立即跳转
          console.log('📦 [Ready] 检测到正在下载新版本，等待下载完成...')
          this.downloadStatusMessage = '正在下载新版本试卷包，请稍候...'
          this.downloadProgress = downloadStatus.progress || 0
          
          // 开始检查下载状态
          this.startDownloadStatusCheck()
          return
        }
        
        // 检查完整包是否存在（必须是完整包，不能是快速启动包）
        const hasFullPackage = await ipcRenderer.invoke('paper:checkPackageExists', this.paperId)
        console.log('📦 [Ready] 完整包检查结果:', hasFullPackage)
        
        if (hasFullPackage) {
          console.log('✓ [Ready] 完整包已存在，显示成功状态')
          this.fullPackageReady = true
          this.downloadProgress = 100
          this.downloadStatusMessage = '试卷包已就绪，准备开始考试'
          
          // 完整包已经下载完成，显示成功提示，3秒后自动跳转到播报页面
          if (!this.readyDisplayed) {
            console.log('⏰ [Ready] 准备3秒后跳转到播报页面')
            this.readyDisplayed = true
            setTimeout(() => {
              console.log('⏰ [Ready] 3秒定时器触发，开始跳转到播报页面')
              this.navigateToBroadcast()
            }, 3000)
          } else {
            console.log('⚠️ [Ready] readyDisplayed 已为 true，跳过重复跳转')
          }
        } else {
          // 完整包不存在，主动触发下载
          console.log('📦 [Ready] 完整包不存在，需要下载完整包才能开始考试')
          this.downloadStatusMessage = '正在准备试卷包，请稍候...'
          
          // 触发完整包下载
          await this.triggerFullPackageDownload()
          
          // 开始检查下载状态
          this.startDownloadStatusCheck()
        }
      } catch (error) {
        console.error('❌ [Ready] 检查完整包状态失败:', error)
        this.downloadError = '检查试卷包状态失败: ' + error.message
      }
    },
    
    async triggerFullPackageDownload() {
      try {
        const token = localStorage.getItem('token')
        if (!token) {
          console.error('❌ [Ready] 未找到认证token')
          this.downloadError = '未找到认证token，请重新登录'
          return
        }
        
        // 记录下载开始时间
        this.downloadStartTime = Date.now()
        this.lastProgressTime = Date.now()
        this.lastProgress = 0
        
        console.log('📦 [Ready] 开始触发完整包下载，paperId:', this.paperId)
        
        // 获取试卷信息
        const papers = await ipcRenderer.invoke('paper:getPapersByIds', [this.paperId])
        if (!papers || papers.length === 0) {
          console.error('❌ [Ready] 未找到试卷信息')
          this.downloadError = '未找到试卷信息'
          return
        }
        
        const paper = papers[0]
        console.log('📦 [Ready] 试卷信息:', { 
          id: paper.id, 
          paperCode: paper.paper_code, 
          version: paper.version 
        })
        
        // 调用同步接口下载完整包
        const result = await ipcRenderer.invoke('paper:syncPaperPackage', {
          paper: paper,
          token: token
        })
        
        if (result.success) {
          console.log('✓ [Ready] 完整包下载请求已发送')
        } else {
          console.warn('⚠️ [Ready] 完整包下载请求失败:', result.message)
          // 不立即报错，继续轮询检查状态
        }
      } catch (error) {
        console.error('❌ [Ready] 触发完整包下载失败:', error)
        // 不立即报错，继续轮询检查状态
      }
    },

    startDownloadStatusCheck() {
      // 每秒检查一次下载状态
      this.checkInterval = setInterval(async () => {
        await this.checkDownloadStatus()
      }, 1000)

      // 立即检查一次
      this.checkDownloadStatus()
    },

    async checkDownloadStatus() {
      try {
        const now = Date.now()
        
        // 检查是否超时（总下载时间超过 120 秒）
        if (this.downloadStartTime && (now - this.downloadStartTime) > this.downloadTimeout) {
          console.log('⚠️ [Ready] 下载超时，尝试使用旧版本')
          await this.tryUseFallbackVersion('下载超时')
          return
        }
        
        // 检查完整包是否存在
        const hasFullPackage = await ipcRenderer.invoke('paper:checkPackageExists', this.paperId)
        
        if (hasFullPackage) {
          // 完整包已下载完成
          console.log('✓ [Ready] 完整包已下载完成，可以开始考试')
          this.fullPackageReady = true
          this.downloadProgress = 100
          this.downloadStatusMessage = '试卷包已就绪，准备开始考试'
          
          // 停止检查
          if (this.checkInterval) {
            clearInterval(this.checkInterval)
            this.checkInterval = null
            console.log('✓ [Ready] 已停止下载状态检查')
          }
          
          // 显示成功提示，3秒后自动跳转到播报页面
          if (!this.readyDisplayed) {
            console.log('⏰ [Ready] 准备3秒后跳转到播报页面（从下载完成触发）')
            this.readyDisplayed = true
            setTimeout(() => {
              console.log('⏰ [Ready] 3秒定时器触发，开始跳转到播报页面（从下载完成触发）')
              this.navigateToBroadcast()
            }, 3000)
          } else {
            console.log('⚠️ [Ready] readyDisplayed 已为 true，跳过重复跳转（从下载完成触发）')
          }
        } else {
          // 获取下载状态
          const downloadStatus = await ipcRenderer.invoke('paper:getDownloadStatus', this.paperId)
          
          if (downloadStatus) {
            const currentProgress = downloadStatus.progress || 0
            this.downloadProgress = currentProgress
            
            // 检查进度是否卡住（30秒内进度没有变化）
            if (currentProgress !== this.lastProgress) {
              this.lastProgressTime = now
              this.lastProgress = currentProgress
            } else if (this.lastProgressTime && (now - this.lastProgressTime) > this.progressStuckTimeout) {
              // 进度卡住超过 30 秒
              console.log('⚠️ [Ready] 下载进度卡住，尝试使用旧版本')
              await this.tryUseFallbackVersion('下载进度停滞')
              return
            }
            
            if (downloadStatus.status === 'downloading') {
              // 只显示进度百分比，不显示具体大小
              this.downloadStatusMessage = `正在下载试卷包: ${this.downloadProgress}%`
            } else if (downloadStatus.status === 'error') {
              // 下载失败，检查是否可以使用旧版本
              console.log('❌ [Ready] 新版本下载失败，检查是否有旧版本可用')
              await this.tryUseFallbackVersion(downloadStatus.errorMessage || '下载失败')
            } else if (downloadStatus.status === 'pending') {
              this.downloadStatusMessage = '等待下载...'
            } else if (downloadStatus.status === 'completed') {
              // 状态显示已完成但检查不到文件，重新检查一次
              console.log('⚠️ [Ready] 状态显示已完成但文件未找到，重新检查')
            }
          } else {
            // 没有下载状态，可能下载未开始
            this.downloadStatusMessage = '等待下载开始...'
          }
        }
      } catch (error) {
        console.error('检查下载状态失败:', error)
        // 不显示错误，继续检查
      }
    },

    async retryDownload() {
      this.downloadError = null
      this.downloadProgress = 0
      this.downloadStatusMessage = '正在重新下载...'
      
      try {
        const token = localStorage.getItem('token')
        if (!token) {
          throw new Error('未找到认证token')
        }

        // 获取试卷信息
        const papers = await ipcRenderer.invoke('paper:getPapersByIds', [this.paperId])
        if (!papers || papers.length === 0) {
          throw new Error('未找到试卷信息')
        }

        const paper = papers[0]

        // 开始下载完整包
        await ipcRenderer.invoke('paper:syncPaperPackage', {
          paper: paper,
          token: token
        })

        // 开始检查下载状态
        this.startDownloadStatusCheck()
      } catch (error) {
        console.error('重新下载失败:', error)
        this.downloadError = '重新下载失败: ' + error.message
      }
    },

    async tryUseFallbackVersion(errorMessage) {
      try {
        // 停止轮询检查
        if (this.checkInterval) {
          clearInterval(this.checkInterval)
          this.checkInterval = null
        }
        
        // 检查是否有任何可用版本
        const fallbackInfo = await ipcRenderer.invoke('paper:checkAnyPackageExists', this.paperId)
        
        if (fallbackInfo && fallbackInfo.exists) {
          // 有旧版本可用
          console.log(`✓ [Ready] 找到可用的旧版本 v${fallbackInfo.availableVersion}，将使用此版本继续`)
          console.log(`✓ [Ready] 快速包: ${fallbackInfo.hasQuickPackage}, 完整包: ${fallbackInfo.hasFullPackage}`)
          
          // 更新本地 paper 表的版本为降级版本，确保后续使用一致
          try {
            await ipcRenderer.invoke('paper:updateLocalVersion', this.paperId, fallbackInfo.availableVersion)
            console.log(`✓ [Ready] 已更新本地版本为 v${fallbackInfo.availableVersion}`)
          } catch (updateError) {
            console.warn(`⚠️ [Ready] 更新本地版本失败: ${updateError.message}，继续使用旧版本`)
          }
          
          // 显示降级提示
          this.downloadStatusMessage = `使用本地缓存版本 v${fallbackInfo.availableVersion} (最新版本 v${fallbackInfo.requiredVersion} 下载失败)`
          
          // 标记为已就绪（使用旧版本）
          this.fullPackageReady = true
          this.downloadProgress = 100
          
          // 跳转到播报页面
          if (!this.readyDisplayed) {
            console.log('⏰ [Ready] 准备3秒后跳转到播报页面（使用旧版本）')
            this.readyDisplayed = true
            setTimeout(() => {
              console.log('⏰ [Ready] 3秒定时器触发，开始跳转到播报页面（使用旧版本）')
              this.navigateToBroadcast()
            }, 3000)
          }
        } else {
          // 没有任何可用版本
          console.log('❌ [Ready] 没有找到任何可用版本，无法继续')
          this.downloadError = errorMessage
        }
      } catch (error) {
        console.error('检查旧版本失败:', error)
        this.downloadError = errorMessage
      }
    },
    
    navigateToBroadcast() {
      console.log('🚀 [Ready] navigateToBroadcast 方法被调用')
      console.log('🚀 [Ready] 当前路由路径:', this.$route.path)
      console.log('🚀 [Ready] navigationTriggered:', this.navigationTriggered)
      
      // 跳转到播报页面
      // 防止重复导航
      if (this.navigationTriggered) {
        console.warn('⚠️ [Ready] 导航已触发，跳过重复导航')
        return
      }
      
      // 检查当前路由，避免重复导航
      if (this.$route.path === '/broadcast') {
        console.warn('⚠️ [Ready] 已在 /broadcast 页面，跳过导航')
        return
      }
      
      this.navigationTriggered = true
      console.log('✅ [Ready] 开始导航到播报页面 /broadcast')
      
      // 使用 catch 捕获重复导航错误
      this.$router.push('/broadcast').then(() => {
        console.log('✅ [Ready] 路由跳转成功，已跳转到 /broadcast')
      }).catch(err => {
        // 忽略重复导航错误
        if (err.name !== 'NavigationDuplicated') {
          console.error('❌ [Ready] 导航错误:', err)
          console.error('❌ [Ready] 错误详情:', err.message, err.stack)
        } else {
          console.log('⚠️ [Ready] 重复导航错误（已忽略）')
        }
      })
    }
  },
  beforeDestroy() {
    // 清除定时器
    if (this.checkInterval) {
      clearInterval(this.checkInterval)
      this.checkInterval = null
    }
  }
}
</script>

<style scoped>
.ready-container {
  width: 100%;
  height: 100vh;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.loading-state,
.error-state,
.success-state {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.loading-content,
.error-content,
.success-content {
  text-align: center;
  max-width: 600px;
  padding: 40px;
}

.success-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.success-message-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 20px;
}

.success-line-1 {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.success-icon-wrapper {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background-color: #67C23A;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.success-check-icon {
  font-size: 16px;
  color: #fff;
  font-weight: bold;
}

.success-text-1 {
  font-size: 20px;
  color: #67C23A;
  font-weight: bold;
}

.success-line-2 {
  display: flex;
  align-items: center;
  justify-content: center;
}

.success-text-2 {
  font-size: 16px;
  color: #606266;
  line-height: 1.6;
}

.loading-icon {
  font-size: 64px;
  color: #409EFF;
  margin-bottom: 20px;
  animation: rotating 2s linear infinite;
}

@keyframes rotating {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

.error-icon {
  font-size: 64px;
  color: #F56C6C;
  margin-bottom: 20px;
}

.loading-title,
.error-title {
  font-size: 24px;
  color: #303133;
  margin-bottom: 20px;
  font-weight: 500;
}

.progress-info {
  margin-top: 30px;
}

.progress-text {
  margin-top: 16px;
  font-size: 16px;
  color: #606266;
}

.error-message {
  font-size: 16px;
  color: #F56C6C;
  margin-bottom: 30px;
  line-height: 1.6;
}
</style>







