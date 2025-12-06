<template>
  <div class="operation-tips-container">
    <!-- 顶部蓝色标签 -->
    <div class="page-tag">试听</div>

    <!-- 主内容区 -->
    <div class="content-area">
      <!-- 1. 操作提示 -->
      <div class="section">
        <div class="section-header">
          <div class="section-number">1</div>
          <div class="section-title">操作提示</div>
        </div>
        <div class="tips-card">
          <div class="headset-images">
            <!-- 只有在数据加载完成且图片路径存在时才显示图片 -->
            <img 
              v-if="dataLoaded && operateListenImagePath" 
              :src="operateListenImagePath" 
              alt="操作提示图片" 
              class="headset-img"
              @load="onImageLoaded"
              @error="onImageError"
            />
            <!-- 数据加载中时不显示占位符，避免闪烁 -->
          </div>
          <!-- 只有在数据加载完成后才显示文本 -->
          <div v-if="dataLoaded && operateListenText" class="instruction-text-container">
            <div class="instruction-text" v-html="operateListenText"></div>
          </div>
          <p v-if="dataLoaded && !operateListenText" class="instruction-text">
            请按图示方法佩戴好耳机,并通过右下角音量调节按钮调至合适的音量大小
          </p>
        </div>
      </div>

      <!-- 2. 放音测试 -->
      <div class="section">
        <div class="section-header">
          <div class="section-number">2</div>
          <div class="section-title">放音测试</div>
        </div>
        <div class="audio-test-content">
          <!-- 只有在数据加载完成后才显示提示文本，避免闪烁 -->
          <p class="test-instruction" v-if="dataLoaded">
            如能清晰听到声音,请点击【清晰】;若一直不清晰请联系监考老师调换设备。
          </p>
          <!-- 试听文本：只有在数据加载完成且存在时才显示 -->
          <div class="test-text-box" v-if="dataLoaded && trialListenAudioText">
            <p class="test-text-content">{{ trialListenAudioText }}</p>
          </div>
          <div class="test-buttons">
            <el-button type="success" @click="handleClear">清晰</el-button>
            <el-button type="primary" @click="playTrialAudio" :disabled="audioPlaying">
              {{ audioPlaying ? '播放中...' : '播放试听音频' }}
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 音频播放器（隐藏，用于播放操作提示音频） -->
    <audio
      ref="operationAudioPlayer"
      :src="operationAudioPath"
      @ended="onOperationAudioEnded"
      @error="onOperationAudioError"
      @loadedmetadata="onOperationAudioLoaded"
    ></audio>

    <!-- 试听旁白音频播放器（介绍音频，自动播放） -->
    <audio
      ref="trialIntroAudioPlayer"
      :src="trialIntroAudioPath"
      @ended="onTrialIntroAudioEnded"
      @error="onTrialIntroAudioError"
      @loadedmetadata="onTrialIntroAudioLoaded"
    ></audio>

    <!-- 试听音频播放器（用户点击播放） -->
    <!-- 注意：不在模板中直接绑定 src，而是在 playTrialAudio 中动态设置，避免空 src 触发错误 -->
    <audio
      ref="trialAudioPlayer"
      @ended="onTrialAudioEnded"
      @error="onTrialAudioError"
    ></audio>
  </div>
</template>

<script>
const { ipcRenderer } = require('electron')
const path = require('path')
const fs = require('fs')
const { pathToFileURL } = require('url')

// 日志函数：将日志发送到主进程（IDE控制台）
function logToMain(level, message, ...args) {
  try {
    // 将参数转换为可序列化的对象
    const serializedArgs = args.map(arg => {
      if (typeof arg === 'object' && arg !== null) {
        try {
          return JSON.stringify(arg, null, 2)
        } catch (e) {
          return String(arg)
        }
      }
      return arg
    })
    ipcRenderer.send('app:log', { level, message, args: serializedArgs })
  } catch (error) {
    // 如果IPC失败，降级到console（虽然F12被禁用，但至少不会报错）
    console[level] || console.log(`[${level}]`, message, ...args)
  }
}

// 创建便捷的日志函数
const logger = {
  log: (message, ...args) => logToMain('info', message, ...args),
  error: (message, ...args) => logToMain('error', message, ...args),
  warn: (message, ...args) => logToMain('warn', message, ...args),
  debug: (message, ...args) => logToMain('debug', message, ...args)
}

export default {
  name: 'OperationTips',
  data() {
    return {
      paperId: null,
      operationAudioPath: '',
      trialAudioPath: '',
      operateListenImagePath: '',
      operateListenText: '', // 操作提示文本
      trialListenAudioText: '', // 试听文本内容
      trialIntroAudioPath: '', // 试听旁白音频路径（介绍音频，自动播放）
      audioPlaying: false, // 试听音频播放状态
      trialIntroAudioPlaying: false, // 试听旁白音频播放状态
      operationAudioPlaying: false, // 操作提示音频播放状态
      operationAudioDuration: 0,
      operationAudioPlayed: false,
      navigationTriggered: false, // 防止重复导航
      dataLoaded: false, // 数据加载完成标志，用于避免页面闪烁
      imageLoaded: false // 图片加载完成标志
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

    // 加载试卷数据（如果预加载失败，这里会重新加载）
    await this.loadPaperData()
    
    // 注意：不再自动播放试听音频，只自动播放试听旁白音频（在loadPaperData中处理）
  },
  methods: {
    async loadPaperData() {
      try {
        logger.log('📦 开始加载试卷数据...')
        logger.log('paperId:', this.paperId)
        
        // 获取试卷包数据
        const paperData = await ipcRenderer.invoke('paper:getPaperData', this.paperId)
        logger.log('paperData获取成功:', !!paperData)
        logger.log('paperData.manifest存在:', !!paperData?.manifest)
        logger.log('paperData.mediaDir:', paperData?.mediaDir)
        
        if (!paperData || !paperData.manifest) {
          throw new Error('无法加载试卷数据')
        }

        const manifest = paperData.manifest
        const mediaDir = paperData.mediaDir || ''
        
        logger.log('manifest基本信息:', {
          paperId: manifest.paperId,
          paperCode: manifest.paperCode,
          paperName: manifest.paperName,
          trialListenEnabled: manifest.trialListenEnabled
        })

        // 加载操作提示文本（operate_listen_text字段）
        logger.log('📝 开始加载操作提示文本...')
        if (manifest.operateListenText) {
          this.operateListenText = manifest.operateListenText
          logger.log('✓ 操作提示文本加载成功:', this.operateListenText)
        } else {
          logger.warn('⚠️ manifest中没有operateListenText字段')
        }

        // 加载操作提示图片（从operate_listen目录）
        // 优先使用本地文件，即使manifest中是远程URL
        logger.log('🖼️ 开始加载操作提示图片...')
        logger.log('manifest.operateListenImagePath:', manifest.operateListenImagePath)
        
        const operateListenDir = path.join(mediaDir, 'operate_listen')
        logger.log('operate_listen目录:', operateListenDir)
        logger.log('目录是否存在:', fs.existsSync(operateListenDir))
        
        // 先检查本地文件是否存在
        let localImagePath = null
        if (fs.existsSync(operateListenDir)) {
          const files = fs.readdirSync(operateListenDir)
          logger.log('operate_listen目录中的文件:', files)
          
          // 如果manifest中有路径，尝试匹配文件名
          if (manifest.operateListenImagePath) {
            const fileName = path.basename(manifest.operateListenImagePath)
            logger.log('从manifest提取的文件名:', fileName)
            const imagePath = path.join(operateListenDir, fileName)
            if (fs.existsSync(imagePath)) {
              localImagePath = imagePath
              logger.log('✓ 找到匹配的本地图片文件:', imagePath)
            }
          }
          
          // 如果没找到匹配的文件，尝试查找任何图片文件
          if (!localImagePath) {
            const imageFile = files.find(file => /\.(jpg|jpeg|png|gif|bmp|webp)$/i.test(file))
            if (imageFile) {
              localImagePath = path.join(operateListenDir, imageFile)
              logger.log('✓ 找到本地图片文件（自动匹配）:', localImagePath)
            }
          }
        }
        
        // 优先使用本地文件
        if (localImagePath) {
          const stats = fs.statSync(localImagePath)
          logger.log('本地图片文件统计信息:', {
            path: localImagePath,
            size: stats.size,
            sizeKB: (stats.size / 1024).toFixed(2),
            isFile: stats.isFile()
          })
          
          // 使用 base64 data URL 加载图片（Electron 的 file:// URL 可能无法正确加载）
          try {
            logger.log('尝试使用 base64 data URL 加载图片...')
            const fileBuffer = fs.readFileSync(localImagePath)
            logger.log('图片文件读取成功，大小:', fileBuffer.length, '字节')
            
            const ext = path.extname(localImagePath).toLowerCase()
            let mimeType = 'image/png' // 默认 PNG
            if (ext === '.jpg' || ext === '.jpeg') mimeType = 'image/jpeg'
            else if (ext === '.gif') mimeType = 'image/gif'
            else if (ext === '.bmp') mimeType = 'image/bmp'
            else if (ext === '.webp') mimeType = 'image/webp'
            
            // 转换为 base64
            const base64 = fileBuffer.toString('base64')
            logger.log('Base64 编码完成，长度:', base64.length)
            
            // 创建 data URL
            const dataUrl = `data:${mimeType};base64,${base64}`
            
            // 预加载图片，确保图片可以正常显示后再设置路径
            await this.preloadImage(dataUrl)
            
            this.operateListenImagePath = dataUrl
            this.imageLoaded = true
            logger.log('✓ 操作提示图片加载成功（使用 base64 data URL）')
            logger.log('✓ 提取路径:', localImagePath)
            logger.log('✓ 文件大小:', fileBuffer.length, '字节')
            logger.log('✓ MIME类型:', mimeType)
            logger.log('✓ Data URL 长度:', dataUrl.length, '字符')
            logger.log('✓ this.operateListenImagePath已设置（前100字符）:', this.operateListenImagePath.substring(0, 100) + '...')
          } catch (dataUrlError) {
            logger.error('创建 base64 data URL 失败:', dataUrlError)
            logger.error('错误堆栈:', dataUrlError.stack)
            // 降级方案：使用 file:// URL
            try {
              const fileUrl = pathToFileURL(localImagePath).href
              // 预加载 file:// URL 图片
              await this.preloadImage(fileUrl)
              this.operateListenImagePath = fileUrl
              this.imageLoaded = true
              logger.log('✓ 使用 file:// URL 作为降级方案:', fileUrl)
            } catch (fileUrlError) {
              logger.error('创建 file:// URL 也失败:', fileUrlError)
              this.$message.error('无法加载操作提示图片文件')
              this.imageLoaded = true // 即使失败也标记为已加载
            }
          }
        } else if (manifest.operateListenImagePath) {
          // 本地文件不存在，使用manifest中的路径（可能是远程URL）
          logger.warn('⚠️ 本地图片文件不存在，使用manifest中的路径')
          // 预加载远程图片
          await this.preloadImage(manifest.operateListenImagePath)
          this.operateListenImagePath = manifest.operateListenImagePath
          this.imageLoaded = true
          logger.log('使用路径（远程URL）:', this.operateListenImagePath)
        } else {
          logger.error('❌ 未找到操作提示图片（本地和manifest都没有）')
          // 即使没有图片，也标记为已加载，避免一直等待
          this.imageLoaded = true
        }

        // 加载操作提示音频（如果有）
        // 注意：manifest中可能没有operateListenAudioPath字段，需要从operate_listen目录中查找
        logger.log('🔊 开始加载操作提示音频...')
        if (fs.existsSync(operateListenDir)) {
          const files = fs.readdirSync(operateListenDir)
          logger.log('operate_listen目录中的音频文件:', files.filter(file => /\.(mp3|wav|m4a|aac)$/i.test(file)))
          const audioFile = files.find(file => /\.(mp3|wav|m4a|aac)$/i.test(file))
          if (audioFile) {
            const audioFilePath = path.join(operateListenDir, audioFile)
            // 使用 pathToFileURL 正确编码路径
            this.operationAudioPath = pathToFileURL(audioFilePath).href
            logger.log('✓ 操作提示音频加载成功:', this.operationAudioPath)
            // 尝试获取音频时长
            if (manifest.operateListenAudioDuration) {
              this.operationAudioDuration = manifest.operateListenAudioDuration
            }
          } else {
            logger.log('⚠️ operate_listen目录中未找到音频文件')
          }
        } else {
          logger.warn('⚠️ operate_listen目录不存在，无法加载操作提示音频')
        }

        // 加载试听文本（trial_listen_audio_text字段）
        logger.log('📝 开始加载试听文本...')
        if (manifest.trialListenAudioText) {
          this.trialListenAudioText = manifest.trialListenAudioText
          logger.log('✓ 试听文本加载成功:', this.trialListenAudioText)
        } else {
          logger.warn('⚠️ manifest中没有trialListenAudioText字段')
        }

        // 加载试听旁白音频（trial_intro目录中的音频，自动播放）
        logger.log('🔍 开始加载试听旁白音频（介绍音频）...')
        logger.log('manifest.trialIntroAudioPath:', manifest.trialIntroAudioPath)
        logger.log('manifest.trialIntroAudioUrl:', manifest.trialIntroAudioUrl)
        
        const trialIntroDir = path.join(mediaDir, 'trial_intro')
        logger.log('trial_intro目录:', trialIntroDir)
        logger.log('目录是否存在:', fs.existsSync(trialIntroDir))
        
        // 先检查本地文件是否存在
        let localIntroAudioPath = null
        if (fs.existsSync(trialIntroDir)) {
          const allFiles = fs.readdirSync(trialIntroDir)
          logger.log('trial_intro目录中的文件:', allFiles)
          
          // 如果manifest中有路径，尝试匹配文件名
          if (manifest.trialIntroAudioPath) {
            const fileName = path.basename(manifest.trialIntroAudioPath)
            logger.log('从manifest提取的文件名:', fileName)
            const audioPath = path.join(trialIntroDir, fileName)
            if (fs.existsSync(audioPath)) {
              localIntroAudioPath = audioPath
              logger.log('✓ 找到匹配的本地旁白音频文件:', audioPath)
            }
          }
          
          // 如果没找到匹配的文件，尝试查找任何音频文件
          if (!localIntroAudioPath) {
            const audioFile = allFiles.find(file => /\.(mp3|wav|m4a|aac|ogg)$/i.test(file))
            if (audioFile) {
              localIntroAudioPath = path.join(trialIntroDir, audioFile)
              logger.log('✓ 找到本地旁白音频文件（自动匹配）:', localIntroAudioPath)
            }
          }
        }
        
        // 设置旁白音频路径（使用 base64 data URL，确保可以正常播放）
        if (localIntroAudioPath) {
          const stats = fs.statSync(localIntroAudioPath)
          logger.log('本地旁白音频文件统计信息:', {
            path: localIntroAudioPath,
            size: stats.size,
            sizeKB: (stats.size / 1024).toFixed(2),
            isFile: stats.isFile()
          })
          
          if (stats.size === 0) {
            logger.error(`❌ 试听旁白音频文件为空: ${localIntroAudioPath}`)
            this.$message.warning('试听旁白音频文件为空，请检查文件')
          } else {
            try {
              // 使用 base64 data URL 加载音频（Electron 的安全检查会阻止 Blob URL）
              logger.log('尝试使用 base64 data URL 加载旁白音频...')
              const fileBuffer = fs.readFileSync(localIntroAudioPath)
              logger.log('旁白音频文件读取成功，大小:', fileBuffer.length, '字节')
              
              const ext = path.extname(localIntroAudioPath).toLowerCase()
              let mimeType = 'audio/mpeg' // 默认 MP3
              if (ext === '.wav') mimeType = 'audio/wav'
              else if (ext === '.m4a' || ext === '.aac') mimeType = 'audio/mp4'
              else if (ext === '.ogg') mimeType = 'audio/ogg'
              
              // 转换为 base64
              const base64 = fileBuffer.toString('base64')
              logger.log('Base64 编码完成，长度:', base64.length)
              
              // 创建 data URL
              const dataUrl = `data:${mimeType};base64,${base64}`
              this.trialIntroAudioPath = dataUrl
              logger.log('✓ 试听旁白音频加载成功（使用 base64 data URL）')
              logger.log('✓ 提取路径:', localIntroAudioPath)
              logger.log('✓ 文件大小:', fileBuffer.length, '字节')
              logger.log('✓ MIME类型:', mimeType)
            } catch (dataUrlError) {
              logger.error('创建 base64 data URL 失败:', dataUrlError)
              logger.error('错误堆栈:', dataUrlError.stack)
              // 降级方案：使用 file:// URL
              try {
                const fileUrl = pathToFileURL(localIntroAudioPath).href
                this.trialIntroAudioPath = fileUrl
                logger.log('✓ 使用 file:// URL 作为降级方案:', fileUrl)
              } catch (fileUrlError) {
                logger.error('创建 file:// URL 也失败:', fileUrlError)
                this.$message.error('无法加载试听旁白音频文件')
              }
            }
          }
        } else if (manifest.trialIntroAudioPath) {
          // 本地文件不存在，使用manifest中的路径（可能是远程URL）
          logger.warn('⚠️ 本地旁白音频文件不存在，使用manifest中的路径')
          this.trialIntroAudioPath = manifest.trialIntroAudioPath
          logger.log('使用路径（远程URL）:', this.trialIntroAudioPath)
        } else {
          logger.warn('⚠️ 未找到试听旁白音频（本地和manifest都没有）')
        }

        // 加载试听音频（trial_listen目录中的音频，用户点击播放）
        // 优先使用本地文件，即使manifest中是远程URL
        logger.log('🔍 开始加载试听音频...')
        logger.log('manifest.trialListenAudioPath:', manifest.trialListenAudioPath)
        logger.log('manifest.trialListenAudioUrl:', manifest.trialListenAudioUrl)
        logger.log('mediaDir:', mediaDir)
        
        const trialListenDir = path.join(mediaDir, 'trial_listen')
        logger.log('trial_listen目录:', trialListenDir)
        logger.log('目录是否存在:', fs.existsSync(trialListenDir))
        
        // 先检查本地文件是否存在
        let localAudioPath = null
        if (fs.existsSync(trialListenDir)) {
          const allFiles = fs.readdirSync(trialListenDir)
          logger.log('trial_listen目录中的文件:', allFiles)
          
          // 如果manifest中有路径，尝试匹配文件名
          if (manifest.trialListenAudioPath) {
            const fileName = path.basename(manifest.trialListenAudioPath)
            logger.log('从manifest提取的文件名:', fileName)
            const audioPath = path.join(trialListenDir, fileName)
            if (fs.existsSync(audioPath)) {
              localAudioPath = audioPath
              logger.log('✓ 找到匹配的本地音频文件:', audioPath)
            }
          }
          
          // 如果没找到匹配的文件，尝试查找任何音频文件
          if (!localAudioPath) {
            const audioFile = allFiles.find(file => /\.(mp3|wav|m4a|aac|ogg)$/i.test(file))
            if (audioFile) {
              localAudioPath = path.join(trialListenDir, audioFile)
              logger.log('✓ 找到本地音频文件（自动匹配）:', localAudioPath)
            }
          }
        }
        
        // 优先使用本地文件
        if (localAudioPath) {
          const stats = fs.statSync(localAudioPath)
          logger.log('本地音频文件统计信息:', {
            path: localAudioPath,
            size: stats.size,
            sizeKB: (stats.size / 1024).toFixed(2),
            isFile: stats.isFile(),
            mode: stats.mode.toString(8)
          })
          
          if (stats.size === 0) {
            logger.error(`❌ 试听音频文件为空: ${localAudioPath}`)
            this.$message.warning('试听音频文件为空，请检查文件')
          } else {
            try {
              // 验证文件头（检查文件是否是有效的音频文件）
              const ext = path.extname(localAudioPath).toLowerCase()
              logger.log('检查文件扩展名:', ext)
              
              try {
                const buffer = fs.readFileSync(localAudioPath, { start: 0, end: 11 }) // 读取前12字节
                const isValidAudio = this.validateAudioFileHeader(buffer, ext)
                logger.log('文件头验证结果:', isValidAudio)
                //logger.log('文件头字节（hex）:', buffer.toString('hex'))
                
                if (!isValidAudio) {
                  logger.error(`❌ 试听音频文件头验证失败: ${localAudioPath}`)
                  logger.error('文件头字节:', Array.from(buffer).map(b => '0x' + b.toString(16).padStart(2, '0')).join(' '))
                  logger.error('文件可能已损坏或格式不正确')
                  // 不阻止设置路径，让浏览器尝试解码
                } else {
                  logger.log(`✓ 试听音频文件头验证通过: ${localAudioPath}`)
                }
              } catch (e) {
                logger.warn('读取文件头失败:', e)
                // 继续设置路径，可能只是读取问题
              }
              
              // 使用 base64 data URL 加载音频（Electron 的安全检查会阻止 Blob URL）
              try {
                logger.log('尝试使用 base64 data URL 加载音频...')
                const fileBuffer = fs.readFileSync(localAudioPath)
                logger.log('文件读取成功，大小:', fileBuffer.length, '字节')
                
                // 验证文件头
                const headerBytes = fileBuffer.slice(0, 4)
                //logger.log('文件头字节（hex）:', Buffer.from(headerBytes).toString('hex'))
                logger.log('文件头字节（ascii）:', Buffer.from(headerBytes).toString('ascii', 0, Math.min(4, headerBytes.length)))
                
                const ext = path.extname(localAudioPath).toLowerCase()
                let mimeType = 'audio/mpeg' // 默认 MP3
                if (ext === '.wav') mimeType = 'audio/wav'
                else if (ext === '.m4a' || ext === '.aac') mimeType = 'audio/mp4'
                else if (ext === '.ogg') mimeType = 'audio/ogg'
                
                // 转换为 base64
                const base64 = fileBuffer.toString('base64')
                logger.log('Base64 编码完成，长度:', base64.length)
                
                // 创建 data URL
                const dataUrl = `data:${mimeType};base64,${base64}`
                this.trialAudioPath = dataUrl
                logger.log('✓ 试听音频加载成功（使用 base64 data URL）')
                logger.log('✓ 提取路径:', localAudioPath)
                logger.log('✓ 文件大小:', fileBuffer.length, '字节')
                logger.log('✓ MIME类型:', mimeType)
                logger.log('✓ Data URL 长度:', dataUrl.length, '字符')
                logger.log('✓ this.trialAudioPath已设置（前100字符）:', this.trialAudioPath.substring(0, 100) + '...')
              } catch (dataUrlError) {
                logger.error('创建 base64 data URL 失败:', dataUrlError)
                logger.error('错误堆栈:', dataUrlError.stack)
                // 降级方案：使用 file:// URL
                try {
                  const fileUrl = pathToFileURL(localAudioPath).href
                  this.trialAudioPath = fileUrl
                  logger.log('✓ 使用 file:// URL 作为降级方案:', fileUrl)
                } catch (fileUrlError) {
                  logger.error('创建 file:// URL 也失败:', fileUrlError)
                  this.$message.error('无法加载试听音频文件')
                }
              }
            } catch (error) {
              logger.error('❌ 设置试听音频路径失败:', error)
              logger.error('错误堆栈:', error.stack)
              this.$message.error('设置试听音频路径失败: ' + error.message)
            }
          }
        } else if (manifest.trialListenAudioPath) {
          // 本地文件不存在，使用manifest中的路径（可能是远程URL）
          logger.warn('⚠️ 本地音频文件不存在，使用manifest中的路径')
          this.trialAudioPath = manifest.trialListenAudioPath
          logger.log('使用路径（远程URL）:', this.trialAudioPath)
        } else {
          logger.error('❌ 未找到试听音频（本地和manifest都没有）')
        }
        
        //logger.log('最终试听音频路径:', this.trialAudioPath)
        
        // 标记数据加载完成（在设置所有数据之后，避免页面闪烁）
        this.dataLoaded = true
        
        // 自动播放试听旁白音频（如果存在）
        if (this.trialIntroAudioPath) {
          this.$nextTick(() => {
            this.playTrialIntroAudio().catch(error => {
              logger.error('自动播放试听旁白音频失败:', error)
            })
          })
        }
      } catch (error) {
        logger.error('加载试卷数据失败:', error)
        logger.error('错误堆栈:', error.stack)
        this.$message.error('加载试卷数据失败: ' + error.message)
        // 即使加载失败，也标记为已加载，避免一直显示加载状态
        this.dataLoaded = true
      }
    },

    playOperationAudio() {
      // 操作提示音频（如果有）可以播放，但不自动跳转
      if (!this.operationAudioPath) {
        logger.log('没有操作提示音频')
        return
      }

      const audio = this.$refs.operationAudioPlayer
      if (audio) {
        this.operationAudioPlaying = true
        audio.play().catch(error => {
          logger.error('播放操作提示音频失败:', error)
          this.operationAudioPlaying = false
        })
      }
    },

    onOperationAudioLoaded() {
      const audio = this.$refs.operationAudioPlayer
      if (audio) {
        this.operationAudioDuration = audio.duration || this.operationAudioDuration
      }
    },

    onOperationAudioEnded() {
      logger.log('操作提示音频播放完成')
      this.operationAudioPlayed = true
      this.operationAudioPlaying = false
      // 不再自动跳转，需要用户点击"清晰"按钮并确认
    },

    onOperationAudioError(error) {
      logger.error('操作提示音频播放错误:', error)
      this.operationAudioPlaying = false
      // 不再自动跳转，需要用户点击"清晰"按钮并确认
    },

    async playTrialAudio() {
      // 如果操作提示音频正在播放，先停止它
      if (this.operationAudioPlaying && this.$refs.operationAudioPlayer) {
        logger.log('停止操作提示音频，开始播放试听音频')
        this.$refs.operationAudioPlayer.pause()
        this.$refs.operationAudioPlayer.currentTime = 0
        this.operationAudioPlaying = false
      }
      
      // 如果试听旁白音频正在播放，先停止它
      if (this.trialIntroAudioPlaying && this.$refs.trialIntroAudioPlayer) {
        logger.log('停止试听旁白音频，开始播放试听音频')
        this.$refs.trialIntroAudioPlayer.pause()
        this.$refs.trialIntroAudioPlayer.currentTime = 0
        this.trialIntroAudioPlaying = false
      }
      
      const audio = this.$refs.trialAudioPlayer
      if (!audio) {
        logger.error('音频播放器未找到')
        this.$message.error('音频播放器未初始化')
        return
      }
      
      if (!this.trialAudioPath) {
        logger.error('试听音频路径为空')
        this.$message.error('试听音频文件不存在，请等待加载完成')
        return
      }
      
      logger.log('准备播放试听音频，路径类型:', this.trialAudioPath.startsWith('data:') ? 'data URL' : this.trialAudioPath.startsWith('blob:') ? 'blob URL' : this.trialAudioPath.startsWith('file:') ? 'file URL' : '其他')
      
      // 验证文件是否存在（如果是本地文件）
      if (this.trialAudioPath.startsWith('file://')) {
        try {
          // 从 file:// URL 提取路径
          const filePath = this.trialAudioPath.replace(/^file:\/\//, '')
          // 解码 URL 编码的路径
          const decodedPath = decodeURIComponent(filePath)
          
          if (!fs.existsSync(decodedPath)) {
            console.error('试听音频文件不存在:', decodedPath)
            this.$message.error('试听音频文件不存在，请重新下载试卷包')
            return
          }
          
          const stats = fs.statSync(decodedPath)
          if (stats.size === 0) {
            console.error('试听音频文件为空:', decodedPath)
            this.$message.error('试听音频文件损坏（文件大小为0），请重新下载试卷包')
            return
          }
          
          // 检查文件扩展名
          const ext = path.extname(decodedPath).toLowerCase()
          const supportedFormats = ['.mp3', '.wav', '.m4a', '.aac', '.ogg']
          if (!supportedFormats.includes(ext)) {
            console.warn(`试听音频格式可能不支持: ${ext}，尝试播放`)
          }
          
          // 验证文件头（检查文件是否是有效的音频文件）
          try {
            const buffer = fs.readFileSync(decodedPath, { start: 0, end: 11 }) // 读取前12字节
            const isValidAudio = this.validateAudioFileHeader(buffer, ext)
            if (!isValidAudio) {
              logger.warn(`试听音频文件头验证失败: ${decodedPath}，文件可能已损坏`)
              // 不阻止播放，让浏览器尝试解码
            } else {
              logger.log(`✓ 试听音频文件头验证通过: ${decodedPath}`)
            }
          } catch (e) {
            logger.warn('读取文件头失败:', e)
            // 继续播放，可能只是读取问题
          }
          
          logger.log(`试听音频文件验证通过: ${decodedPath}, 大小: ${(stats.size / 1024).toFixed(2)} KB, 格式: ${ext}`)
        } catch (error) {
          console.error('验证试听音频文件失败:', error)
          // 继续尝试播放，可能只是路径解析问题
        }
      }
      
      logger.log('开始播放试听音频:', this.trialAudioPath.substring(0, 100) + '...')
      logger.log('音频元素当前src:', audio.src || '(空)')
      logger.log('音频元素readyState:', audio.readyState)
      logger.log('音频元素networkState:', audio.networkState)
      
      this.audioPlaying = true
      
      // 重置音频元素（清除之前的错误状态）
      audio.load()
      
      // 设置音频源（必须设置，因为模板中没有绑定）
      logger.log('设置音频src（前100字符）:', this.trialAudioPath.substring(0, 100) + '...')
      audio.src = this.trialAudioPath
      logger.log('音频src已设置，currentSrc:', audio.currentSrc || '(空)')
      
      // 等待一小段时间，确保src设置完成
      await new Promise(resolve => setTimeout(resolve, 50))
      
      logger.log('设置src后的readyState:', audio.readyState)
      logger.log('设置src后的networkState:', audio.networkState)
      //logger.log('设置src后的currentSrc:', audio.currentSrc || '(空)')
      
      // 等待音频加载完成（使用 Promise）
      return new Promise((resolve, reject) => {
        // 添加详细的加载事件监听
        const onLoadStart = () => {
          logger.log('📥 试听音频开始加载')
        }
        
        const onLoadedMetadata = () => {
          logger.log('📊 试听音频元数据加载完成')
          logger.log('音频时长:', audio.duration, '秒')
          logger.log('音频就绪状态:', audio.readyState)
        }
        
        const onCanPlay = () => {
          logger.log('✅ 试听音频可以播放')
          logger.log('音频就绪状态:', audio.readyState)
          // 可以播放后，尝试播放
          audio.play().then(() => {
            logger.log('试听音频播放成功')
            resolve()
          }).catch(error => {
            logger.error('播放试听音频失败:', error)
            logger.error('音频路径:', this.trialAudioPath)
            logger.error('错误详情:', error)
            this.audioPlaying = false
            
            // 提供更详细的错误信息
            let errorMsg = '播放试听音频失败'
            if (error.name === 'NotAllowedError') {
              errorMsg = '浏览器阻止了音频播放，请点击页面后重试'
            } else if (error.name === 'NotSupportedError') {
              errorMsg = '试听音频格式不支持，请检查文件格式'
            } else {
              errorMsg = `播放失败: ${error.message || '未知错误'}`
            }
            this.$message.error(errorMsg)
            reject(error)
          })
        }
        
        const onCanPlayThrough = () => {
          logger.log('✅ 试听音频可以完整播放')
        }
        
        const onError = (e) => {
          logger.error('❌ 试听音频加载错误事件触发')
          logger.error('错误对象:', e)
          logger.error('audio.error:', audio.error)
          this.audioPlaying = false
          // 错误处理在 onTrialAudioError 中
          reject(new Error('音频加载失败'))
        }
        
        const onStalled = () => {
          logger.warn('⚠️ 试听音频加载停滞')
        }
        
        const onSuspend = () => {
          logger.warn('⚠️ 试听音频加载暂停')
        }
        
        // 添加事件监听器
        audio.addEventListener('loadstart', onLoadStart, { once: true })
        audio.addEventListener('loadedmetadata', onLoadedMetadata, { once: true })
        audio.addEventListener('canplay', onCanPlay, { once: true })
        audio.addEventListener('canplaythrough', onCanPlayThrough, { once: true })
        audio.addEventListener('error', onError, { once: true })
        audio.addEventListener('stalled', onStalled, { once: true })
        audio.addEventListener('suspend', onSuspend, { once: true })
        
        // 如果已经可以播放，直接播放
        if (audio.readyState >= 2) { // HAVE_CURRENT_DATA
          logger.log('音频已就绪，直接播放')
          audio.play().then(() => {
            logger.log('试听音频播放成功')
            resolve()
          }).catch(error => {
            logger.error('播放试听音频失败:', error)
            this.audioPlaying = false
            reject(error)
          })
        } else {
          // 等待加载完成
          logger.log('等待音频加载完成...')
          // 设置超时（5秒）
          setTimeout(() => {
            if (audio.readyState < 2) {
              logger.error('音频加载超时')
              this.audioPlaying = false
              reject(new Error('音频加载超时'))
            }
          }, 5000)
        }
      })
    },

    // 播放试听旁白音频（自动播放）
    async playTrialIntroAudio() {
      const audio = this.$refs.trialIntroAudioPlayer
      if (!audio) {
        logger.error('旁白音频播放器未找到')
        return
      }
      
      if (!this.trialIntroAudioPath) {
        logger.warn('没有试听旁白音频')
        return
      }
      
      try {
        logger.log('准备播放试听旁白音频，路径类型:', this.trialIntroAudioPath.startsWith('data:') ? 'data URL' : this.trialIntroAudioPath.startsWith('file:') ? 'file URL' : '其他')
        
        // 确保音频元素已加载
        if (audio.src !== this.trialIntroAudioPath) {
          logger.log('设置旁白音频src')
          audio.src = this.trialIntroAudioPath
        }
        
        // 等待音频加载完成
        return new Promise((resolve, reject) => {
          const onCanPlay = () => {
            logger.log('✅ 试听旁白音频可以播放')
            audio.play().then(() => {
              logger.log('✓ 试听旁白音频播放成功')
              this.trialIntroAudioPlaying = true
              resolve()
            }).catch(error => {
              logger.error('播放试听旁白音频失败:', error)
              this.trialIntroAudioPlaying = false
              // 不显示错误提示，因为这是自动播放，失败不影响用户体验
              reject(error)
            })
          }
          
          const onError = (e) => {
            logger.error('❌ 试听旁白音频加载错误')
            logger.error('错误对象:', e)
            logger.error('audio.error:', audio.error)
            this.trialIntroAudioPlaying = false
            reject(new Error('音频加载失败'))
          }
          
          // 如果已经可以播放，直接播放
          if (audio.readyState >= 2) { // HAVE_CURRENT_DATA
            logger.log('旁白音频已就绪，直接播放')
            audio.play().then(() => {
              logger.log('✓ 试听旁白音频播放成功')
              this.trialIntroAudioPlaying = true
              resolve()
            }).catch(error => {
              logger.error('播放试听旁白音频失败:', error)
              this.trialIntroAudioPlaying = false
              reject(error)
            })
          } else {
            // 等待加载完成
            logger.log('等待旁白音频加载完成...')
            audio.addEventListener('canplay', onCanPlay, { once: true })
            audio.addEventListener('error', onError, { once: true })
            
            // 设置超时（5秒）
            setTimeout(() => {
              if (audio.readyState < 2) {
                logger.error('旁白音频加载超时')
                this.trialIntroAudioPlaying = false
                reject(new Error('音频加载超时'))
              }
            }, 5000)
          }
        })
      } catch (error) {
        logger.error('播放试听旁白音频失败:', error)
        this.trialIntroAudioPlaying = false
        // 不显示错误提示，因为这是自动播放，失败不影响用户体验
      }
    },
    
    onTrialIntroAudioLoaded() {
      logger.log('试听旁白音频加载完成')
    },
    
    onTrialIntroAudioEnded() {
      logger.log('试听旁白音频播放完成')
      this.trialIntroAudioPlaying = false
    },
    
    onTrialIntroAudioError(error) {
      logger.error('试听旁白音频播放错误:', error)
      this.trialIntroAudioPlaying = false
      // 不显示错误提示，因为这是自动播放，失败不影响用户体验
    },

    onTrialAudioEnded() {
      console.log('试听音频播放完成')
      this.audioPlaying = false
    },

    onTrialAudioError(error) {
      logger.error('试听音频播放错误:', error)
      logger.error('音频路径:', this.trialAudioPath)
      logger.error('音频元素:', this.$refs.trialAudioPlayer)
      
      // 获取详细的错误信息
      let errorMessage = '试听音频播放失败'
      let errorCode = null
      let errorDetail = ''
      
      if (error && error.target && error.target.error) {
        const audioError = error.target.error
        errorCode = audioError.code
        
        switch (audioError.code) {
          case 1: // MEDIA_ERR_ABORTED
            errorMessage = '试听音频加载被中止'
            errorDetail = '用户取消了音频加载'
            break
          case 2: // MEDIA_ERR_NETWORK
            errorMessage = '试听音频网络错误'
            errorDetail = '网络连接问题导致无法加载音频'
            break
          case 3: // MEDIA_ERR_DECODE
            errorMessage = '试听音频解码失败'
            errorDetail = '音频文件可能损坏或格式不正确'
            break
          case 4: // MEDIA_ERR_SRC_NOT_SUPPORTED
            errorMessage = '试听音频格式不支持或文件损坏'
            errorDetail = '音频格式不被支持，或文件已损坏。支持的格式：MP3, WAV, M4A, AAC, OGG'
            break
          default:
            errorMessage = `试听音频播放错误 (错误代码: ${audioError.code})`
            errorDetail = audioError.message || '未知错误'
        }
        
        logger.error(`音频错误代码: ${audioError.code}, 消息: ${errorDetail}`)
      } else if (error && error.message) {
        errorMessage = `试听音频错误: ${error.message}`
        errorDetail = error.message
      }
      
      // 如果是文件格式或损坏问题，提供解决建议
      if (errorCode === 3 || errorCode === 4) {
        logger.error('建议：请检查音频文件是否完整，或重新下载试卷包')
        errorMessage += '。建议重新下载试卷包'
      }
      
      // 验证文件是否存在和完整
      if (this.trialAudioPath && this.trialAudioPath.startsWith('file://')) {
        try {
          const filePath = decodeURIComponent(this.trialAudioPath.replace(/^file:\/\//, ''))
          logger.error(`错误时验证文件: 路径=${filePath}`)
          logger.error(`文件是否存在: ${fs.existsSync(filePath)}`)
          
          if (fs.existsSync(filePath)) {
            const stats = fs.statSync(filePath)
            logger.error(`文件信息: 路径=${filePath}, 大小=${stats.size} 字节`)
            
            // 检查文件头
            try {
              const buffer = fs.readFileSync(filePath, { start: 0, end: 11 })
              const ext = path.extname(filePath).toLowerCase()
              const isValidAudio = this.validateAudioFileHeader(buffer, ext)
              logger.error(`文件头验证结果: ${isValidAudio}`)
              //logger.error(`文件头字节（hex）: ${buffer.toString('hex')}`)
              logger.error(`文件头字节（ascii）: ${buffer.toString('ascii', 0, Math.min(12, buffer.length))}`)
              
              if (!isValidAudio) {
                errorMessage = '试听音频文件头验证失败，文件可能已损坏，请重新下载试卷包'
              }
            } catch (e) {
              logger.error('读取文件头失败:', e)
            }
            
            if (stats.size === 0) {
              errorMessage = '试听音频文件为空（文件大小为0），请重新下载试卷包'
            } else if (stats.size < 100) {
              errorMessage = '试听音频文件异常小，可能已损坏，请重新下载试卷包'
            }
          } else {
            errorMessage = '试听音频文件不存在，请重新下载试卷包'
            logger.error(`文件不存在: ${filePath}`)
          }
        } catch (e) {
          logger.error('验证文件时出错:', e)
          logger.error('错误堆栈:', e.stack)
        }
      }
      
      // 记录音频元素的详细信息
      if (this.$refs.trialAudioPlayer) {
        const audio = this.$refs.trialAudioPlayer
        try {
          const audioInfo = {
            src: audio.src,
            currentSrc: audio.currentSrc,
            readyState: audio.readyState,
            networkState: audio.networkState,
            duration: audio.duration,
            paused: audio.paused,
            ended: audio.ended,
            error: null
          }
          
          if (audio.error) {
            audioInfo.error = {
              code: audio.error.code,
              message: audio.error.message
            }
          }
          
          logger.error('音频元素详细信息:', audioInfo)
        } catch (e) {
          logger.error('获取音频元素信息失败:', e)
        }
      }
      
      this.audioPlaying = false
      this.$message.error(errorMessage)
    },

    /**
     * 验证音频文件头（检查文件是否是有效的音频文件）
     * @param {Buffer} buffer - 文件头缓冲区（前12字节）
     * @param {string} ext - 文件扩展名
     * @returns {boolean} 是否是有效的音频文件
     */
    validateAudioFileHeader(buffer, ext) {
      if (!buffer || buffer.length < 4) {
        logger.warn('文件头缓冲区太小或为空')
        return false
      }
      
      // MP3: ID3v2 (49 44 33) 或 MPEG frame sync (FF FB/FA/F3/F2)
      if (ext === '.mp3') {
        const id3 = buffer.toString('ascii', 0, 3) === 'ID3'
        const mpeg = buffer[0] === 0xFF && (buffer[1] & 0xE0) === 0xE0
        
        logger.log('MP3文件头检查:', {
          id3: id3,
          mpeg: mpeg,
          firstByte: '0x' + buffer[0].toString(16).padStart(2, '0'),
          secondByte: '0x' + buffer[1].toString(16).padStart(2, '0'),
          secondByteBits: (buffer[1] & 0xE0).toString(16)
        })
        
        return id3 || mpeg
      }
      
      // WAV: RIFF (52 49 46 46) + WAVE (57 41 56 45)
      if (ext === '.wav') {
        const riff = buffer.toString('ascii', 0, 4) === 'RIFF'
        const wave = buffer.toString('ascii', 8, 12) === 'WAVE'
        return riff && wave
      }
      
      // M4A/AAC: ftyp box (66 74 79 70) 或 ISO base media file format
      if (ext === '.m4a' || ext === '.aac') {
        // M4A 文件通常以 ftyp box 开始
        if (buffer.length >= 8) {
          const ftyp = buffer.toString('ascii', 4, 8) === 'ftyp'
          // 或者检查文件签名
          const signature = buffer.toString('hex', 0, 8)
          return ftyp || signature.startsWith('000000') || signature.includes('66747970')
        }
      }
      
      // OGG: OggS (4F 67 67 53)
      if (ext === '.ogg') {
        return buffer.toString('ascii', 0, 4) === 'OggS'
      }
      
      // 如果无法验证，返回 true（让浏览器尝试解码）
      return true
    },

    handleClear() {
      // 弹出确认框（参考图片样式）
      this.$confirm(
        '<div style="text-align: left; padding: 5px 0;">' +
        '<div style="display: flex; align-items: flex-start;">' +
        '<div style="width: 24px; height: 24px; border-radius: 50%; background-color: #E6A23C; display: flex; align-items: center; justify-content: center; margin-right: 12px; flex-shrink: 0; margin-top: 2px;">' +
        '<span style="color: white; font-size: 16px; font-weight: bold;">!</span>' +
        '</div>' +
        '<div style="flex: 1;">' +
        '<div style="font-size: 16px; color: #303133; margin-bottom: 12px; line-height: 1.5; font-weight: 500;">' +
        '请确定您的放音音量大小是否调至合适位置?' +
        '</div>' +
        '<div style="font-size: 14px; color: #E6A23C; line-height: 1.8;">' +
        '点击<span style="color: #409EFF; font-weight: 500;">【确定】</span>完成试听,等待考试正式开始<br/>' +
        '点击<span style="color: #409EFF; font-weight: 500;">【取消】</span>可返回重新试听' +
        '</div>' +
        '</div>' +
        '</div>' +
        '</div>',
        '提示',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          dangerouslyUseHTMLString: true,
          type: 'warning',
          customClass: 'volume-confirm-dialog',
          confirmButtonClass: 'el-button--primary',
          cancelButtonClass: 'el-button--default',
          showClose: false,
          closeOnClickModal: false,
          closeOnPressEscape: false
        }
      ).then(() => {
        // 用户点击确定，跳转到注意事项页面
        logger.log('用户确认声音清晰，跳转到注意事项页面')
        this.navigateToNotes()
      }).catch(() => {
        // 用户点击取消，不跳转，可以重新试听
        logger.log('用户取消，停留在当前页面')
      })
    },

    navigateToNotes() {
      // 防止重复导航
      if (this.navigationTriggered) {
        console.log('导航已触发，跳过重复导航')
        return
      }
      
      // 检查当前路由，避免重复导航
      if (this.$route.path === '/notes') {
        console.log('已在 /notes 页面，跳过导航')
        return
      }
      
      this.navigationTriggered = true
      console.log('导航到注意事项页面')
      
      // 使用 catch 捕获重复导航错误
      this.$router.push('/notes').catch(err => {
        // 忽略重复导航错误
        if (err.name !== 'NavigationDuplicated') {
          console.error('导航错误:', err)
        }
      })
    },

    /**
     * 预加载图片，确保图片可以正常显示
     * @param {string} imageSrc - 图片路径（data URL 或 URL）
     * @returns {Promise} 图片加载完成的 Promise
     */
    preloadImage(imageSrc) {
      return new Promise((resolve, reject) => {
        if (!imageSrc) {
          resolve()
          return
        }
        
        const img = new Image()
        img.onload = () => {
          logger.log('✓ 图片预加载成功')
          resolve()
        }
        img.onerror = (error) => {
          logger.warn('⚠️ 图片预加载失败，但继续显示:', error)
          // 即使预加载失败，也继续显示（可能是网络问题，但图片路径是正确的）
          resolve()
        }
        img.src = imageSrc
        
        // 设置超时（5秒）
        setTimeout(() => {
          if (!img.complete) {
            logger.warn('⚠️ 图片预加载超时，但继续显示')
            resolve() // 超时也继续，避免阻塞
          }
        }, 5000)
      })
    },

    /**
     * 图片加载成功事件
     */
    onImageLoaded() {
      logger.log('✓ 操作提示图片显示成功')
      this.imageLoaded = true
    },

    /**
     * 图片加载失败事件
     */
    onImageError(error) {
      logger.error('❌ 操作提示图片显示失败:', error)
      this.imageLoaded = true // 即使失败也标记为已加载，避免一直等待
    }
  },
  beforeDestroy() {
    if (this.$refs.operationAudioPlayer) {
      this.$refs.operationAudioPlayer.pause()
      this.$refs.operationAudioPlayer.src = ''
    }
    if (this.$refs.trialAudioPlayer) {
      this.$refs.trialAudioPlayer.pause()
      // 如果是 Blob URL，需要释放资源
      if (this.trialAudioPath && this.trialAudioPath.startsWith('blob:')) {
        try {
          URL.revokeObjectURL(this.trialAudioPath)
          logger.log('✓ 已释放 Blob URL 资源')
        } catch (e) {
          logger.warn('释放 Blob URL 失败:', e)
        }
      }
      // Data URL 不需要释放，但可以清空
      this.$refs.trialAudioPlayer.src = ''
    }
  }
}
</script>

<style scoped>
/* 音量确认对话框样式 */
::v-deep .volume-confirm-dialog {
  min-width: 400px;
}

::v-deep .volume-confirm-dialog .el-message-box__message {
  padding: 0;
}

::v-deep .volume-confirm-dialog .el-message-box__btns {
  padding-top: 20px;
}

::v-deep .volume-confirm-dialog .el-button--default {
  border-color: #409EFF;
  color: #409EFF;
  background-color: #fff;
}

::v-deep .volume-confirm-dialog .el-button--primary {
  background-color: #409EFF;
  border-color: #409EFF;
}
</style>

<style scoped>
.operation-tips-container {
  width: 100%;
  height: 100vh;
  background: #fff;
  position: relative;
  overflow-y: auto;
}

.page-tag {
  position: absolute;
  top: 0;
  left: 0;
  background: #409EFF;
  color: #fff;
  padding: 8px 16px;
  font-size: 14px;
  border-radius: 0 0 8px 0;
  z-index: 10;
}

.content-area {
  padding: 60px 40px 40px;
  max-width: 1200px;
  margin: 0 auto;
}

.section {
  margin-bottom: 40px;
}

.section-header {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}

.section-number {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #909399;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: bold;
  margin-right: 12px;
}

.section-title {
  font-size: 20px;
  font-weight: 500;
  color: #303133;
}

.tips-card {
  background: #E8F5E9;
  border-radius: 8px;
  padding: 30px;
  margin-top: 20px;
  min-height: 300px; /* 固定最小高度，避免图片和文本加载后自适应 */
  display: flex;
  flex-direction: column;
}

.headset-images {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-bottom: 20px;
  min-height: 200px; /* 固定高度，避免加载后自适应 */
  align-items: center; /* 垂直居中 */
}

.headset-img {
  max-width: 200px;
  max-height: 200px; /* 固定最大高度 */
  width: auto;
  height: auto;
  border-radius: 8px;
  object-fit: contain; /* 保持比例，不裁剪 */
}

.headset-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 200px;
  height: 200px;
  background: #f5f5f5;
  border-radius: 8px;
  color: #909399;
}

.headset-placeholder i {
  font-size: 48px;
  margin-bottom: 10px;
}

.instruction-text-container {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 20px;
}

.instruction-text {
  text-align: center;
  font-size: 16px;
  color: #606266;
  line-height: 1.6;
  max-width: 100%;
}

.instruction-text-container .instruction-text {
  width: 100%;
}

.audio-test-content {
  margin-top: 20px;
  position: relative; /* 相对定位，为固定按钮做准备 */
  min-height: 300px; /* 固定最小高度，避免内容加载后自适应 */
}

.test-instruction {
  font-size: 16px;
  color: #606266;
  margin-bottom: 20px;
  line-height: 1.6;
  min-height: 24px; /* 固定高度，避免加载后变化 */
}

.test-text-box {
  background: #fff;
  border: 1px solid #DCDFE6;
  border-radius: 4px;
  padding: 20px;
  margin-bottom: 20px;
  min-height: 80px; /* 固定最小高度，避免加载后自适应 */
  max-height: 200px; /* 限制最大高度，避免过长 */
  overflow-y: auto; /* 如果内容过长，可以滚动 */
}

.test-text-content {
  font-size: 16px;
  color: #303133;
  line-height: 1.8;
  white-space: pre-wrap; /* 保留换行和空格 */
}

.test-text-chinese {
  font-size: 16px;
  color: #303133;
  margin-bottom: 12px;
  line-height: 1.8;
}

.test-text-english {
  font-size: 16px;
  color: #303133;
  line-height: 1.8;
}

.test-buttons {
  display: flex;
  gap: 12px;
  justify-content: center;
  position: relative; /* 相对定位 */
  margin-top: 20px; /* 固定上边距 */
  height: 40px; /* 固定高度，确保按钮区域不会变化 */
  align-items: center; /* 垂直居中 */
}

.test-buttons .el-button {
  min-width: 100px;
  padding: 8px 16px;
  font-size: 14px;
  position: relative; /* 相对定位，确保按钮位置固定 */
}
</style>
