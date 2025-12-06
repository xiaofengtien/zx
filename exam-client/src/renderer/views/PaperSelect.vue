<template>
  <div class="paper-select-container">
    <!-- 顶部蓝色背景区域 -->
    <div class="header-section">
      <div class="header-content">
        <!-- Logo（第一行） -->
        <div class="logo-container">
          <img :src="logoImage" alt="Logo" class="logo-img" />
        </div>
        <!-- 主标题（第二行） -->
        <h1 class="main-title">{{ mainTitle }}</h1>
        <!-- 副标题（第三行） -->
        <h2 class="sub-title">{{ subTitle }}</h2>
      </div>
    </div>

    <!-- 白色内容区域 -->
    <div class="content-section">
      <h3 class="section-title">试卷列表</h3>
      
      <!-- 骨架屏：数据加载中显示 -->
      <paper-list-skeleton v-if="loading && !dataReady" :count="3" class="skeleton-fade" />
      
      <!-- 方案A：如果配置了applicablePaperIds，显示试卷选择列表 -->
      <div v-else-if="showPaperList && dataReady" class="paper-list-container content-fade-up">
        <div
          v-for="(paper, index) in availablePaperList"
          :key="paper.id"
          class="paper-item"
          :class="{ 'paper-item-selected': selectedPaperId === paper.id }"
          @click="handlePaperClick(paper)"
        >
          <div class="paper-number">{{ index + 1 }}</div>
          <div class="paper-info">
            <div class="paper-name">{{ paper.paper_name || '未命名试卷' }}</div>
            <div class="paper-details">
              <span v-if="paper.enable_start_time && paper.enable_end_time" class="paper-time">
                试卷启用时间: {{ formatDateTime(paper.enable_start_time) }} 至 {{ formatDateTime(paper.enable_end_time) }}
              </span>
              <span v-else-if="paper.enable_start_time" class="paper-time">
                试卷启用时间: {{ formatDateTime(paper.enable_start_time) }} 起
              </span>
              <span v-else-if="paper.enable_end_time" class="paper-time">
                试卷启用时间: 至 {{ formatDateTime(paper.enable_end_time) }}
              </span>
              <span v-else-if="paper.year && paper.month" class="paper-time">
                {{ paper.year }}年{{ paper.month }}月
              </span>
              <div class="paper-practice-info" v-if="paper.practiceLimit !== undefined && paper.practiceLimit > 0">
                <span>限定练习次数: {{ paper.practiceLimit }}次</span>
                <span class="practice-remaining" v-if="paper.practiceInfo && paper.practiceInfo.remaining !== undefined">
                  , 剩余{{ paper.practiceInfo.remaining }}次练习机会
                </span>
              </div>
              <!-- 下载状态显示（始终显示，即使状态为 undefined 也显示默认状态） -->
              <div class="paper-download-status">
                <!-- 下载中状态：显示进度条 -->
                <div v-if="paper.downloadStatus === 'downloading'" class="download-progress-container">
                  <el-progress 
                    :percentage="paper.downloadProgress || 0" 
                    :show-text="false"
                    :stroke-width="6"
                    class="smooth-progress"
                  ></el-progress>
                  <span class="progress-text">下载中 {{ paper.downloadProgress || 0 }}%</span>
                </div>
                
                <!-- 其他状态：显示标签 -->
                <el-tag v-else-if="paper.downloadStatus === 'pending'" size="mini" type="warning">
                  <i class="el-icon-time"></i> 等待下载
                </el-tag>
                <el-tag v-else-if="paper.downloadStatus === 'ready'" size="mini" type="success">
                  <i class="el-icon-success"></i> 已就绪
                </el-tag>
                <el-tag v-else-if="paper.downloadStatus === 'error'" size="mini" type="danger">
                  <i class="el-icon-error"></i> 下载失败
                  <el-button size="mini" type="text" @click.stop="retryDownload(paper)">重试</el-button>
                </el-tag>
                <el-tag v-else-if="paper.downloadStatus === 'checking'" size="mini" type="info">
                  <i class="el-icon-loading"></i> 检查中...
                </el-tag>
                <el-tag v-else size="mini" type="warning">
                  <i class="el-icon-time"></i> 等待下载
                </el-tag>
              </div>
            </div>
          </div>
          <el-button
            type="primary"
            :disabled="!canStartPractice(paper) || !paper.downloadStatus || paper.downloadStatus !== 'ready'"
            @click.stop="startExam(paper)"
            class="start-button"
          >
            {{ getButtonText(paper) }}
          </el-button>
        </div>
        
        <div v-if="availablePaperList.length === 0 && !loading" class="empty-papers">
          <p>暂无可用试卷</p>
        </div>
      </div>
      
      <!-- 方案B：如果没有配置applicablePaperIds，显示试卷类型选择（旧逻辑） -->
      <div v-else-if="!showPaperList && dataReady" class="paper-type-container content-fade-up">
        <el-card class="paper-card">
          <div slot="header">
            <span>请选择要参加的考试类型</span>
          </div>
          <el-radio-group v-model="selectedPaper" @change="handlePaperChange">
            <el-radio 
              v-for="paper in availablePapers" 
              :key="paper.value" 
              :label="paper.value"
              class="paper-radio"
            >
              {{ paper.label }}
            </el-radio>
          </el-radio-group>
        </el-card>
        
        <div class="button-group">
          <el-button 
            type="primary" 
            @click="startExam()" 
            :disabled="!selectedPaper"
          >开始答题</el-button>
          <el-button @click="logout">退出登录</el-button>
        </div>
      </div>
      
    </div>
  </div>
</template>

<script>
const { ipcRenderer } = require('electron')
import PaperListSkeleton from '../components/PaperListSkeleton.vue'

export default {
  name: 'PaperSelect',
  components: {
    PaperListSkeleton
  },
  data() {
    return {
      userInfo: {},
      availablePapers: [], // 试卷类型列表（旧逻辑）
      availablePaperList: [], // 试卷列表（新逻辑，根据applicablePaperIds）
      selectedPaper: '', // 选中的试卷类型（旧逻辑）
      selectedPaperId: null, // 选中的试卷ID（新逻辑）
      showPaperList: false, // 是否显示试卷列表（新逻辑）
      loading: false,
      dataReady: false, // 数据是否准备就绪（用于骨架屏切换）
      pageTitle: '选择试卷类型',
      paperTypeMap: {}, // 从字典数据中动态加载
      // Logo图片 - file-loader返回字符串路径
      logoImage: require('@/assets/images/logo.png'),
      // 标题配置（从字典数据获取）
      mainTitle: '天津市普通高考英语听力计算机化考试', // 默认值
      subTitle: '在线模拟练习系统', // 默认值
      // 下载状态管理
      downloadStatusMap: {}, // { paperId: { status, progress } }
      downloadProgressTimer: null, // 进度轮询定时器
      progressCheckIntervals: {} // 高频进度检查定时器
    }
  },
  activated() {
    // 如果组件被激活（从其他页面返回），重新启动轮询
    if (this.availablePaperList.length > 0 && !this.downloadProgressTimer) {
      this.startDownloadProgressPolling()
    }
  },
  beforeDestroy() {
    // 清理定时器
    if (this.downloadProgressTimer) {
      clearInterval(this.downloadProgressTimer)
      this.downloadProgressTimer = null
    }
    // 清理所有高频进度检查定时器
    Object.values(this.progressCheckIntervals).forEach(intervalId => {
      if (intervalId) {
        clearInterval(intervalId)
      }
    })
    this.progressCheckIntervals = {}
  },
  async mounted() {
    // 检查是否已登录
    const userInfoStr = localStorage.getItem('userInfo')
    if (!userInfoStr) {
      this.$router.push('/login')
      return
    }
    
    this.userInfo = JSON.parse(userInfoStr)
    console.log('从 localStorage 获取的用户信息:', this.userInfo)
    console.log('学员账号 (studentAccount):', this.userInfo.studentAccount)
    console.log('学员账号 (user.userName):', this.userInfo.user?.userName)
    
    // 确定学员账号（优先使用 studentAccount，否则使用 user.userName）
    if (!this.userInfo.studentAccount && this.userInfo.user?.userName) {
      this.userInfo.studentAccount = this.userInfo.user.userName
      console.log('使用 user.userName 作为学员账号:', this.userInfo.studentAccount)
    }
    
    // 开始加载，显示骨架屏
    this.loading = true
    this.dataReady = false
    
    // 加载标题配置
    await this.loadHeaderConfig()
    
    // 加载字典数据
    await this.loadDictData()
    
    // 获取学员适用考卷类型
    await this.loadStudentPapers()
    
    // 优化：启动后台下载（不阻塞）
    this.startBackgroundDownload()
  },
  methods: {
    /**
     * 加载头部标题配置（从字典数据）
     */
    async loadHeaderConfig() {
      try {
        const headerConfig = await ipcRenderer.invoke('dict:getDictData', 'app_header_config')
        console.log('加载头部配置:', headerConfig)
        
        // 构建配置映射
        const configMap = {}
        headerConfig.forEach(item => {
          configMap[item.value] = item.label
        })
        
        // 设置主标题和副标题
        if (configMap.main_title) {
          this.mainTitle = configMap.main_title
        }
        if (configMap.sub_title) {
          this.subTitle = configMap.sub_title
        }
        
        console.log('头部配置加载完成:', { mainTitle: this.mainTitle, subTitle: this.subTitle })
      } catch (error) {
        console.error('加载头部配置失败:', error)
        // 使用默认值
      }
    },
    
    /**
     * 加载字典数据（paper_type）
     */
    async loadDictData() {
      try {
        const dictData = await ipcRenderer.invoke('dict:getDictData', 'paper_type')
        
        // 构建字典映射
        this.paperTypeMap = {}
        dictData.forEach(item => {
          this.paperTypeMap[item.value] = item.label
        })
        
        console.log('加载字典数据:', this.paperTypeMap)
      } catch (error) {
        console.error('加载字典数据失败:', error)
        // 如果加载失败，使用默认映射
        this.paperTypeMap = {
          'pupil': '小学试卷',
          'middle': '中考试卷',
          'high': '高考试卷'
        }
      }
    },
    
    /**
     * 加载学员适用考卷（从本地数据库获取）
     * 优先使用applicablePaperIds（新方案），如果没有则使用applicablePapers（旧方案）
     */
    async loadStudentPapers() {
      try {
        this.loading = true
        
        // 优先通过 user_id 获取，如果没有则通过账号获取
        const userId = this.userInfo.user?.userId
        const studentAccount = this.userInfo.studentAccount || this.userInfo.user?.userName
        
        console.log('查询学员试卷，user_id:', userId, '账号:', studentAccount)
        
        if (!userId && !studentAccount) {
          console.error('无法确定学员 user_id 或账号')
          this.$message.error('无法确定学员信息')
          return
        }
        
        // 获取学员档案信息
        let archive = null
        if (userId) {
          archive = await ipcRenderer.invoke('archive:getByUserId', userId)
        }
        if (!archive && studentAccount) {
          archive = await ipcRenderer.invoke('archive:getByAccount', studentAccount)
        }
        
        if (!archive) {
          console.warn('未找到学员档案信息')
          this.$message.warning('未找到学员档案信息，请联系管理员')
          return
        }
        
        console.log('学员档案信息:', archive)
        
        // 方案A：如果配置了applicablePaperIds，使用新方案
        if (archive.applicablePaperIds && Array.isArray(archive.applicablePaperIds) && archive.applicablePaperIds.length > 0) {
          console.log('使用新方案：根据applicablePaperIds加载试卷列表')
          this.showPaperList = true
          this.pageTitle = '选择试卷'
          
          // 根据试卷ID列表查询试卷
          const papers = await ipcRenderer.invoke('paper:getPapersByIds', archive.applicablePaperIds)
          
          if (papers && papers.length > 0) {
            // 加载每个试卷的练习次数信息
            const papersWithPracticeInfo = await Promise.all(
              papers.map(async (paper) => {
                try {
                  const practiceInfo = await ipcRenderer.invoke(
                    'answer:checkPracticeLimit',
                    paper.id,
                    userId || archive.user_id
                  )
                  // 计算剩余次数
                  const remaining = practiceInfo.practiceLimit > 0 
                    ? Math.max(0, practiceInfo.practiceLimit - practiceInfo.practiceCount)
                    : 999 // 不限制时显示999
                  return {
                    ...paper,
                    practiceLimit: paper.practice_limit || 0,
                    practiceInfo: {
                      ...practiceInfo,
                      remaining: remaining
                    }
                  }
                } catch (error) {
                  console.error(`获取试卷 ${paper.id} 练习次数失败:`, error)
                  const practiceLimit = paper.practice_limit || 0
                  return {
                    ...paper,
                    practiceLimit: practiceLimit,
                    practiceInfo: { 
                      allowed: true, 
                      practiceCount: 0, 
                      practiceLimit: practiceLimit, 
                      remaining: practiceLimit > 0 ? practiceLimit : 999
                    }
                  }
                }
              })
            )
            
            // 先初始化所有试卷的下载状态为"检查中"，避免显示旧数据
            // 确保在设置列表之前，所有 paper 对象都没有 downloadStatus 属性（或设置为 checking）
            papersWithPracticeInfo.forEach(paper => {
              // 删除可能存在的旧状态属性，确保不会显示旧数据
              if (paper.downloadStatus) {
                delete paper.downloadStatus
              }
              if (paper.downloadProgress !== undefined) {
                delete paper.downloadProgress
              }
              // 使用 $set 设置新状态，确保响应式
              this.$set(paper, 'downloadStatus', 'checking')
              this.$set(paper, 'downloadProgress', 0)
            })
            
            // 设置列表，此时显示"检查中"状态
            this.availablePaperList = papersWithPracticeInfo
            console.log(`加载到 ${papers.length} 个试卷，已初始化所有试卷状态为"检查中"`)
            
            // 等待 Vue 渲染完成，然后初始化每个试卷的下载状态（在红色框位置显示）
            await this.$nextTick()
            await this.initDownloadStatusForPapers(papersWithPracticeInfo)
            
            // 如果只有一个试卷，自动选中
            if (papers.length === 1) {
              this.selectedPaperId = papers[0].id
              console.log('您只有一份可用试卷，已自动选中')
            }
          } else {
            this.$message.warning('未找到配置的试卷，请联系管理员')
            console.warn('根据applicablePaperIds未找到试卷:', archive.applicablePaperIds)
          }
        } else {
          // 方案B：如果没有配置applicablePaperIds，使用旧逻辑（按试卷类型）
          console.log('使用旧方案：根据applicablePapers加载试卷类型')
          this.showPaperList = false
          this.pageTitle = '选择试卷类型'
          
          let studentPaperTypes = []
          
          // 优先通过 user_id 查询
          if (userId) {
            try {
              studentPaperTypes = await ipcRenderer.invoke('login:getStudentPapersByUserId', userId)
              console.log('通过 user_id 获取的试卷类型:', studentPaperTypes)
            } catch (error) {
              console.warn('通过 user_id 获取失败，尝试通过账号获取:', error)
            }
          }
          
          // 如果通过 user_id 没获取到，尝试通过账号获取
          if (studentPaperTypes.length === 0 && studentAccount) {
            try {
              studentPaperTypes = await ipcRenderer.invoke('login:getStudentPapers', studentAccount)
              console.log('通过账号获取的试卷类型:', studentPaperTypes)
            } catch (error) {
              console.error('通过账号获取失败:', error)
            }
          }
          
          console.log('最终获取的学员适用试卷类型:', studentPaperTypes)
          console.log('试卷类型数量:', studentPaperTypes.length)
          
          // 获取所有字典数据
          const allDictData = await ipcRenderer.invoke('dict:getDictData', 'paper_type')
          console.log('所有字典数据:', allDictData)
          
          // 根据学员的适用试卷类型，从字典数据中筛选出对应的试卷
          this.availablePapers = allDictData
            .filter(dictItem => studentPaperTypes.includes(dictItem.value))
            .map(dictItem => ({
              value: dictItem.value,  // dictValue，用于查询试卷
              label: dictItem.label,  // dictLabel，用于显示
              sort: dictItem.sort
            }))
            .sort((a, b) => (a.sort || 0) - (b.sort || 0))  // 按排序字段排序
          
          console.log('可用试卷类型列表:', this.availablePapers)
          
          if (this.availablePapers.length === 0) {
            this.$message.warning('您暂无可用试卷，请联系管理员')
          }
        }
      } catch (error) {
        console.error('获取试卷失败:', error)
        this.$message.error('获取试卷失败：' + error.message)
      } finally {
        this.loading = false
        // 延迟设置 dataReady，让骨架屏有平滑过渡
        setTimeout(() => {
          this.dataReady = true
        }, 100)
      }
    },
    
    /**
     * 初始化每个试卷的下载状态（在红色框位置显示）
     * 检查数据库状态和本地文件，设置正确的下载状态
     */
    async initDownloadStatusForPapers(papers) {
      if (!papers || papers.length === 0) return
      
      // 检查网络状态
      let isOnline = true
      try {
        isOnline = await ipcRenderer.invoke('app:getNetworkStatus')
      } catch (error) {
        console.warn('获取网络状态失败，默认使用在线模式:', error)
        isOnline = true
      }
      
      // 关键修复:使用Promise.all并行处理所有试卷,避免顺序执行导致的布局变化
      await Promise.all(papers.map(async (paper) => {
        try {
          // 强制流程：等待下载 → 下载中 → 已就绪
          // 第一步：先设置为"等待下载"（pending）
          this.$set(this.downloadStatusMap, paper.id, {
            status: 'pending',
            progress: 0
          })
          this.$set(paper, 'downloadStatus', 'pending')
          this.$set(paper, 'downloadProgress', 0)
          
          // 等待 Vue 渲染完成，确保UI显示"等待下载"状态
          await this.$nextTick()
          
          // 第二步：根据网络状态决定检查顺序
          let hasQuickStart = false
          let needsUpdate = false
          
          if (isOnline) {
            // 联网状态：先检查OSS版本，再检查本地文件
            // 从服务器获取的paper对象包含最新版本信息
            const remoteVersion = paper.version || 0
            
            // 获取本地版本（从paper_package表或本地文件）
            const localVersion = await ipcRenderer.invoke('paper:getLocalPaperVersion', paper.id)
            
            // 比较版本：如果远程版本 > 本地版本，需要更新
            if (remoteVersion > (localVersion || 0)) {
              console.log(`试卷 ${paper.id} 需要更新：远程版本 v${remoteVersion} > 本地版本 v${localVersion || 0}`)
              needsUpdate = true
              // 保持pending状态，等待下载
            } else {
              // 版本一致或本地版本更新，检查本地文件
              hasQuickStart = await ipcRenderer.invoke('paper:checkQuickStartPackageExists', paper.id)
            }
          } else {
            // 离线状态：优先检查本地文件
            hasQuickStart = await ipcRenderer.invoke('paper:checkQuickStartPackageExists', paper.id)
          }
          
          const downloadStatus = await ipcRenderer.invoke('paper:getDownloadStatus', paper.id)
          
          // 自动下载逻辑：如果在线且(需要更新 或 快速启动包不存在)，且当前未在下载
          if (isOnline && (needsUpdate || !hasQuickStart)) {
             const isDownloading = downloadStatus && downloadStatus.status === 'downloading'
             
             if (!isDownloading) {
                 console.log(`🚀 页面加载时自动触发下载 - 试卷ID: ${paper.id}`)
                 const token = localStorage.getItem('token')
                 
                 if (token) {
                   // 设置初始状态为downloading，进度为0
                   this.$set(this.downloadStatusMap, paper.id, { status: 'downloading', progress: 0 })
                   this.$set(paper, 'downloadStatus', 'downloading')
                   this.$set(paper, 'downloadProgress', 0)
                   
                   // 不再使用前端模拟，直接依赖后端真实进度
                   // 轮询会自动更新进度
                   
                   // 触发快速启动包下载（真实下载，不使用模拟动画）
                   ipcRenderer.invoke('paper:syncQuickStartPackageOnly', { paper, token })
                     .then(result => {
                        if (result && result.success) {
                           console.log(`✅ 快速启动包自动下载完成 - 试卷ID: ${paper.id}`)
                        }
                     })
                     .catch(err => {
                        console.error(`❌ 自动下载失败 - 试卷ID: ${paper.id}:`, err)
                        // 下载失败，更新状态为error
                        this.$set(this.downloadStatusMap, paper.id, { status: 'error', progress: 0 })
                        this.$set(paper, 'downloadStatus', 'error')
                        this.$set(paper, 'downloadProgress', 0)
                     })
                 } else {
                   console.warn(`没有token，无法自动下载 - 试卷ID: ${paper.id}`)
                 }
                   
                 // 已处理下载，跳过后续逻辑
                 return
             }
          }
          
          if (needsUpdate) {
            // 需要更新，检查数据库中的下载状态
            if (downloadStatus) {
              const status = downloadStatus.status || 'pending'
              const progress = downloadStatus.progress || 0
              
              if (status === 'downloading' && progress > 0 && progress < 100) {
                // 数据库显示正在下载且有进度，设置为downloading
                this.$set(this.downloadStatusMap, paper.id, {
                  status: 'downloading',
                  progress: progress
                })
                this.$set(paper, 'downloadStatus', 'downloading')
                this.$set(paper, 'downloadProgress', progress)
              } else {
                // 其他情况保持pending状态（等待下载）
                // 状态已经在上面设置为pending，这里不需要修改
              }
            } else {
              // 没有数据库记录，保持pending状态（等待下载）
              // 状态已经在上面设置为pending，这里不需要修改
            }
          } else if (hasQuickStart) {
            // 快速启动包已存在,直接设置为ready(移除动画,避免布局问题)
            this.$set(this.downloadStatusMap, paper.id, {
              status: 'ready',
              progress: 100
            })
            this.$set(paper, 'downloadStatus', 'ready')
            this.$set(paper, 'downloadProgress', 100)
            
            // 更新数据库状态
            await ipcRenderer.invoke('paper:updateDownloadStatus', {
              paperId: paper.id,
              paperCode: paper.paper_code || paper.paperCode,
              status: 'ready',
              progress: 100
            })
          } else {
            // 快速启动包不存在，检查数据库状态
            if (downloadStatus) {
              const status = downloadStatus.status || 'pending'
              const progress = downloadStatus.progress || 0
              
              if (status === 'downloading' && progress > 0 && progress < 100) {
                // 数据库显示正在下载且有进度，设置为downloading
                this.$set(this.downloadStatusMap, paper.id, {
                  status: 'downloading',
                  progress: progress
                })
                this.$set(paper, 'downloadStatus', 'downloading')
                this.$set(paper, 'downloadProgress', progress)
              } else {
                // 其他情况（pending、error、ready但文件不存在），保持pending状态
                // 如果数据库状态是ready但文件不存在，说明数据库有旧数据，需要清理
                if (status === 'ready' || status === 'completed') {
                  // 清理数据库中的旧状态，更新为pending
                  await ipcRenderer.invoke('paper:updateDownloadStatus', {
                    paperId: paper.id,
                    paperCode: paper.paper_code || paper.paperCode,
                    status: 'pending',
                    progress: 0
                  })
                  console.log(`清理试卷 ${paper.id} 的旧数据库状态（ready但文件不存在），更新为pending`)
                }
                // 保持pending状态（已经在上面设置了）
              }
            } else {
              // 没有数据库记录，保持pending状态（已经在上面设置了）
            }
          }
        } catch (error) {
          console.warn(`初始化试卷 ${paper.id} 的下载状态失败:`, error)
          // 检查失败，默认设置为pending
          this.$set(this.downloadStatusMap, paper.id, {
            status: 'pending',
            progress: 0
          })
          this.$set(paper, 'downloadStatus', 'pending')
          this.$set(paper, 'downloadProgress', 0)
        }
      }))
      
      // 初始化完成后，启动轮询以持续更新进度
      if (!this.downloadProgressTimer) {
        this.startDownloadProgressPolling()
      }
      
      // Vue的响应式系统($set)已经足够,不需要forceUpdate
      // 移除forceUpdate避免整个组件重新渲染导致布局变化
      console.log('✓ 下载状态初始化完成，已更新所有试卷的下载状态')
    },
    
    /**
     * 格式化日期时间
     */
    formatDateTime(timestamp) {
      if (!timestamp) return ''
      const date = new Date(timestamp)
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      const hours = String(date.getHours()).padStart(2, '0')
      const minutes = String(date.getMinutes()).padStart(2, '0')
      const seconds = String(date.getSeconds()).padStart(2, '0')
      return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
    },
    
    /**
     * 检查是否可以开始练习
     */
    canStartPractice(paper) {
      if (!paper.practiceInfo) return true
      // 如果有限制且剩余次数为0，则不允许
      if (paper.practiceLimit > 0 && paper.practiceInfo.remaining <= 0) {
        return false
      }
      return paper.practiceInfo.allowed
    },

    /**
     * 检查快速启动包是否已就绪
     * 快速启动包必须存在才能开始练习
     */
    async isQuickStartPackageReady(paper) {
      // 如果下载状态是ready，说明快速启动包和完整包都已就绪
      if (paper.downloadStatus === 'ready') {
        return true
      }
      // 如果下载状态是downloading，检查进度
      // 快速启动包下载完成后，即使完整包还在下载，也应该允许开始练习
      if (paper.downloadStatus === 'downloading') {
        // 如果进度>=50，说明快速启动包可能已下载完成（快速启动包通常占50%进度）
        // 实际检查快速启动包是否存在
        try {
          const hasQuickStart = await ipcRenderer.invoke('paper:checkQuickStartPackageExists', paper.id)
          if (hasQuickStart) {
            return true
          }
        } catch (e) {
          console.warn('检查快速启动包失败:', e)
        }
        return false
      }
      // 如果下载状态是pending，说明等待下载，快速启动包未就绪
      if (paper.downloadStatus === 'pending') {
        return false
      }
      // 如果下载状态是error，说明下载失败，快速启动包未就绪
      if (paper.downloadStatus === 'error') {
        return false
      }
      // 如果没有下载状态，检查快速启动包是否存在
      try {
        const hasQuickStart = await ipcRenderer.invoke('paper:checkQuickStartPackageExists', paper.id)
        return hasQuickStart
      } catch (e) {
        console.warn('检查快速启动包失败:', e)
        return false
      }
    },
    
    handlePaperChange(value) {
      console.log('选择试卷类型:', value)
    },
    
    handlePaperClick(paper) {
      this.selectedPaperId = paper.id
      console.log('选择试卷:', paper)
    },
    
    async startExam(paper = null) {
      if (this.showPaperList) {
        // 新方案：使用试卷ID
        const targetPaper = paper || this.availablePaperList.find(p => p.id === this.selectedPaperId)
        
        if (!targetPaper) {
          this.$message.warning('请先选择试卷')
          return
        }
        
        // 再次检查练习次数限制（虽然按钮已禁用，但作为双重保障）
        if (!this.canStartPractice(targetPaper)) {
          this.$message.warning(`您已达到该试卷的练习次数限制（${targetPaper.practiceLimit}次）`)
          return
        }
        
        // 再次检查下载状态（虽然按钮已禁用，但作为双重保障）
        if (targetPaper.downloadStatus !== 'ready') {
          this.$message.warning('试卷尚未就绪，请等待下载完成')
          return
        }
        
        console.log('开始练习 - 试卷ID:', targetPaper.id)
        
        // 保存选择的试卷信息
        localStorage.setItem('selectedPaperId', targetPaper.id.toString())
        localStorage.setItem('selectedPaper', targetPaper.paper_type || '')
        localStorage.setItem('selectedPaperLabel', targetPaper.paper_name || '未命名试卷')
        localStorage.setItem('currentPaperId', targetPaper.id.toString())
        localStorage.setItem('currentPaperName', targetPaper.paper_name || '未命名试卷')
        
        // 检查token
        const token = localStorage.getItem('token')
        if (!token) {
          this.$message.error('未找到认证token，请重新登录')
          return
        }

        // 【关键步骤】先从服务器同步最新版本信息，更新本地paper表
        console.log('🔄 从服务器同步最新试卷版本信息...')
        try {
          const versionResult = await ipcRenderer.invoke('paper:refreshPaperVersion', {
            paperId: targetPaper.id,
            token: token
          })
          
          if (versionResult.success) {
            if (versionResult.needsUpdate) {
              console.log(`✓ 检测到新版本: v${versionResult.localVersion} -> v${versionResult.remoteVersion}`)
            } else if (versionResult.isOffline) {
              console.log(`⚠️ 离线状态，使用本地版本: v${versionResult.localVersion}`)
            } else {
              console.log(`✓ 本地版本已是最新: v${versionResult.localVersion}`)
            }
          } else {
            console.warn('版本同步失败:', versionResult.message)
          }
        } catch (error) {
          console.warn('版本同步失败（不影响继续）:', error.message)
        }

        // 检查是否已有完整包，只在没有时才触发下载
        console.log('🔍 检查本地是否已有完整包，paperId:', targetPaper.id)
        try {
          const hasPackage = await ipcRenderer.invoke('paper:checkPackageExists', targetPaper.id)
          console.log('✓ 完整包检查结果:', hasPackage)
          
          if (!hasPackage) {
            console.log('🚀 本地没有完整包，触发后台下载，paperId:', targetPaper.id)
            // 使用 paperId 而不是 paperCode，让后端自己查询 paperCode
            await ipcRenderer.invoke('paper:syncPaperPackageById', {
              paperId: targetPaper.id,
              token: token,
              onlyQuickStart: false // 下载完整包
            })
            console.log('✓ 后台下载已启动')
          } else {
            console.log('✓ 本地已有完整包，跳过下载')
          }
        } catch (error) {
          console.warn('检查/触发后台下载失败（不影响继续）:', error.message)
        }

        // 直接跳转到操作提示页面
        this.$router.push('/operation-tips')
        
        return
      } else {
        // 旧方案：使用试卷类型
        if (!this.selectedPaper) {
          this.$message.warning('请先选择试卷类型')
          return
        }
        localStorage.setItem('selectedPaper', this.selectedPaper)
        const selectedPaperInfo = this.availablePapers.find(p => p.value === this.selectedPaper)
        if (selectedPaperInfo) {
          localStorage.setItem('selectedPaperLabel', selectedPaperInfo.label)
        }
      }
    },
    
    logout() {
      localStorage.clear()
      this.$router.push('/login')
    },
    
    /**
     * 启动后台下载（不阻塞用户操作）
     */
    async startBackgroundDownload() {
      try {
        const token = localStorage.getItem('token')
        if (!token) {
          console.warn('没有token，跳过后台下载')
          return
        }
        
        console.log('试卷列表加载完成，不自动下载完整包，等用户点击"开始练习"时再下载')
        
        if (!this.downloadProgressTimer) {
          this.startDownloadProgressPolling()
        }
        
      } catch (error) {
        console.error('启动后台下载失败:', error)
      }
    },
    
    /**
     * 启动下载进度轮询（检查所有试卷的下载状态）
     */
    startDownloadProgressPolling() {
      // 如果已经有定时器在运行，先清除
      if (this.downloadProgressTimer) {
        clearInterval(this.downloadProgressTimer)
      }
      
      // 每500ms检查一次所有试卷的下载状态
      this.downloadProgressTimer = setInterval(async () => {
        try {
          let hasDownloading = false
          
          // 遍历所有试卷，检查下载状态
          for (const paper of this.availablePaperList) {
            try {
              const currentStatus = this.downloadStatusMap[paper.id]?.status || paper.downloadStatus
              
              // 获取最新的下载状态
              const downloadStatus = await ipcRenderer.invoke('paper:getDownloadStatus', paper.id)
              
              // 只在状态真正改变时才更新
              if (currentStatus === 'downloading' || currentStatus === 'pending' || currentStatus !== 'ready') {
                if (currentStatus === 'downloading' || currentStatus === 'pending') {
                  hasDownloading = true
                }
              
                if (downloadStatus) {
                  const status = downloadStatus.status || 'downloading'
                  const progress = downloadStatus.progress || 0
                  
                  // 优化：只有当状态或进度真正改变时才更新
                  const statusChanged = paper.downloadStatus !== status
                  const progressChanged = paper.downloadProgress !== progress
                  
                  if (statusChanged || progressChanged) {
                    this.$set(this.downloadStatusMap, paper.id, {
                      status: status,
                      progress: progress
                    })
                    
                    if (statusChanged) {
                      this.$set(paper, 'downloadStatus', status)
                    }
                    if (progressChanged) {
                      this.$set(paper, 'downloadProgress', progress)
                    }
                  }
                  
                  // 如果状态是ready，确保hasDownloading为false
                  if (status === 'ready') {
                    hasDownloading = false
                  }
                  
                  // 如果下载完成，检查快速启动包是否存在
                  if (status === 'completed' || status === 'ready' || progress >= 100) {
                    const hasQuickStart = await ipcRenderer.invoke('paper:checkQuickStartPackageExists', paper.id)
                    if (hasQuickStart && paper.downloadStatus !== 'ready') {
                      this.$set(this.downloadStatusMap, paper.id, {
                        status: 'ready',
                        progress: 100
                      })
                      this.$set(paper, 'downloadStatus', 'ready')
                      this.$set(paper, 'downloadProgress', 100)
                    }
                  }
                } else {
                  // 没有下载状态记录，检查快速启动包是否存在
                  const hasQuickStart = await ipcRenderer.invoke('paper:checkQuickStartPackageExists', paper.id)
                  if (hasQuickStart && paper.downloadStatus !== 'ready') {
                    this.$set(this.downloadStatusMap, paper.id, {
                      status: 'ready',
                      progress: 100
                    })
                    this.$set(paper, 'downloadStatus', 'ready')
                    this.$set(paper, 'downloadProgress', 100)
                  }
                }
              }
            } catch (error) {
              console.warn(`检查试卷 ${paper.id} 状态失败:`, error)
            }
          }
          
          // 如果没有正在下载的试卷，可以降低轮询频率或停止（这里选择继续但降低频率? 暂不复杂化）
        } catch (error) {
          console.error('轮询下载状态失败:', error)
        }
      }, 500)
    },
    
    getButtonText(paper) {
      return '开始练习'
    },
    
    /**
     * 模拟下载进度 (0% -> targetProgress)
     * @param {Object} paper 试卷对象
     * @param {Number} startProgress 起始进度
     * @param {Number} targetProgress 目标进度
     * @param {Number} duration 持续时间(ms)
     */
    simulateDownloadToProgress(paper, startProgress, targetProgress, duration) {
      console.log(`🎬 开始模拟下载进度 ${startProgress}% -> ${targetProgress}% - 试卷ID: ${paper.id}`)
      
      // 设置模拟标志,防止轮询中断 (使用 _isSimulatingProgress 统一标志)
      this.$set(paper, '_isSimulatingProgress', true)
      
      // 锁定页面滚动
      const originalOverflow = document.body.style.overflow
      document.body.style.overflow = 'hidden'
      
      let progress = startProgress
      const interval = 100 // 增加到100ms，进一步减少渲染频率
      const step = (targetProgress - startProgress) / (duration / interval)
      
      // 初始状态
      this.$set(this.downloadStatusMap, paper.id, {
        status: 'downloading',
        progress: startProgress
      })
      this.$set(paper, 'downloadStatus', 'downloading')
      this.$set(paper, 'downloadProgress', startProgress)
      
      const timer = setInterval(() => {
        progress += step
        
        // 到达目标进度
        if (progress >= targetProgress) {
          progress = targetProgress
          clearInterval(timer)
          console.log(`✅ 模拟进度到达 ${targetProgress}% - 试卷ID: ${paper.id}，等待真实下载完成`)
          
          // 注意：这里不清除 _isSimulatingProgress，也不恢复滚动
          // 而是等待 startDownloadProgressPolling 检测到 ready 状态后自动清除
          document.body.style.overflow = originalOverflow
        }
        
        const currentProgress = Math.floor(progress)
        
        // 优化：只有当进度真正改变时才更新，减少渲染频率
        if (paper.downloadProgress !== currentProgress) {
          this.$set(this.downloadStatusMap, paper.id, {
            status: 'downloading',
            progress: currentProgress
          })
          
          // 只有当状态不是downloading时才更新状态（防止重复更新导致闪烁）
          if (paper.downloadStatus !== 'downloading') {
            this.$set(paper, 'downloadStatus', 'downloading')
          }
          
          this.$set(paper, 'downloadProgress', currentProgress)
        }
        
      }, interval)
    },
    
    /**
     * 模拟后台下载进度(从50%到95%)
     */
    simulateBackgroundDownloadProgress(paper) {
      this.simulateDownloadToProgress(paper, 50, 95, 3000)
    },
    
    /**
     * 重试下载(只重试快速启动包,不触发完整包下载)
     */
    async retryDownload(paper) {
      try {
        const token = localStorage.getItem('token')
        if (!token) {
          this.$message.error('请先登录')
          return
        }
        
        // 重置状态
        this.$set(this.downloadStatusMap, paper.id, {
          status: 'downloading',
          progress: 0
        })
        this.$set(paper, 'downloadStatus', 'downloading')
        this.$set(paper, 'downloadProgress', 0)
        
        // 重新获取最新的试卷信息
        const papers = await ipcRenderer.invoke('paper:getPapersByIds', [paper.id])
        const latestPaper = papers && papers.length > 0 ? papers[0] : paper
        
        // 只重试快速启动包，不触发完整包下载
        const quickStartResult = await ipcRenderer.invoke('paper:syncQuickStartPackageOnly', {
          paper: latestPaper,
          token: token
        })
        
        if (quickStartResult && quickStartResult.success) {
          // 快速启动包下载成功，但完整包可能还在下载
          // 检查完整包状态，如果完整包也在下载，显示下载进度；否则标记为ready
          const hasFullPackage = await ipcRenderer.invoke('paper:checkPackageExists', paper.id)
          const downloadStatus = await ipcRenderer.invoke('paper:getDownloadStatus', paper.id)
          
          if (hasFullPackage) {
            // 完整包也存在，标记为ready
            this.$set(this.downloadStatusMap, paper.id, {
              status: 'ready',
              progress: 100
            })
            this.$set(paper, 'downloadStatus', 'ready')
            this.$set(paper, 'downloadProgress', 100)
          } else if (downloadStatus && downloadStatus.status === 'downloading') {
            // 完整包正在下载，显示下载进度（从50%开始，因为快速启动包已完成）
            this.$set(this.downloadStatusMap, paper.id, {
              status: 'downloading',
              progress: Math.max(50, downloadStatus.progress || 50)
            })
            this.$set(paper, 'downloadStatus', 'downloading')
            this.$set(paper, 'downloadProgress', Math.max(50, downloadStatus.progress || 50))
          } else {
            // 快速启动包已完成，但完整包未下载，标记为downloading（显示进度条，从50%开始）
            this.$set(this.downloadStatusMap, paper.id, {
              status: 'downloading',
              progress: 50
            })
            this.$set(paper, 'downloadStatus', 'downloading')
            this.$set(paper, 'downloadProgress', 50)
          }
          this.$message.success('快速启动包下载成功')
        } else {
          // 更新状态为error
          this.$set(this.downloadStatusMap, paper.id, {
            status: 'error',
            progress: 0
          })
          this.$set(paper, 'downloadStatus', 'error')
          this.$set(paper, 'downloadProgress', 0)
          this.$message.error('快速启动包下载失败：' + (quickStartResult?.message || '未知错误'))
        }
        
      } catch (error) {
        console.error('重试下载失败:', error)
        this.$message.error('重试下载失败：' + error.message)
        // 更新状态为error
        this.$set(this.downloadStatusMap, paper.id, {
          status: 'error',
          progress: 0
        })
        this.$set(paper, 'downloadStatus', 'error')
        this.$set(paper, 'downloadProgress', 0)
      }
    },
    
    /**
     * 检查下载状态
     */
    async checkDownloadStatus(paperId) {
      try {
        const hasPackage = await ipcRenderer.invoke('paper:checkPackageExists', paperId)
        if (hasPackage) {
          this.$set(this.downloadStatusMap, paperId, {
            status: 'ready',
            progress: 100
          })
          const paper = this.availablePaperList.find(p => p.id === paperId)
          if (paper) {
            this.$set(paper, 'downloadStatus', 'ready')
            this.$set(paper, 'downloadProgress', 100)
            // 更新数据库状态
            await ipcRenderer.invoke('paper:updateDownloadStatus', {
              paperId: paperId,
              paperCode: paper.paper_code || paper.paperCode,
              status: 'ready',
              progress: 100
            })
          }
        }
      } catch (error) {
        console.warn('检查下载状态失败:', error)
      }
    },
    
    /**
     * 发送下载通知（使用Electron Notification API）
     */
    sendDownloadNotification(paperName, type, errorMessage = null) {
      try {
        // 请求通知权限
        if (window.Notification && Notification.permission === 'default') {
          Notification.requestPermission().then(permission => {
            if (permission === 'granted') {
              this.showNotification(paperName, type, errorMessage)
            }
          })
        } else if (window.Notification && Notification.permission === 'granted') {
          this.showNotification(paperName, type, errorMessage)
        } else {
          // 如果不支持Notification API，使用Electron的Notification
          // 通过IPC调用主进程的Notification
          ipcRenderer.invoke('app:showNotification', {
            title: type === 'success' ? '下载完成' : '下载失败',
            body: type === 'success' 
              ? `试卷包 "${paperName}" 下载完成，可以开始答题了`
              : `试卷包 "${paperName}" 下载失败：${errorMessage || '未知错误'}`,
            type: type
          }).catch(error => {
            console.warn('发送通知失败:', error)
          })
        }
      } catch (error) {
        console.warn('发送下载通知失败:', error)
      }
    },
    
    /**
     * 显示通知
     */
    showNotification(paperName, type, errorMessage) {
      if (window.Notification) {
        const notification = new Notification(
          type === 'success' ? '下载完成' : '下载失败',
          {
            body: type === 'success'
              ? `试卷包 "${paperName}" 下载完成，可以开始答题了`
              : `试卷包 "${paperName}" 下载失败：${errorMessage || '未知错误'}`,
            icon: type === 'success' ? '/path/to/success-icon.png' : '/path/to/error-icon.png',
            tag: `download-${paperName}`, // 防止重复通知
            requireInteraction: false // 不要求用户交互
          }
        )
        
        // 点击通知时聚焦窗口
        notification.onclick = () => {
          window.focus()
          notification.close()
        }
        
        // 3秒后自动关闭
        setTimeout(() => {
          notification.close()
        }, 3000)
      }
    }
  }
}
</script>

<style scoped>
.paper-select-container {
  width: 100%;
  min-height: 100vh;
  background: #f5f7fa;
  display: flex;
  flex-direction: column;
  overflow-x: hidden; /* 防止水平滚动导致闪烁 */
}

/* 顶部蓝色背景区域 */
.header-section {
  background: #667eea; /* 使用纯色蓝色背景，与图片一致 */
  padding: 40px 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 300px; /* 确保有足够的高度 */
  height: 300px; /* 固定高度，避免加载时闪烁 */
  flex-shrink: 0; /* 防止收缩 */
}

.header-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  gap: 20px;
  width: 100%;
  max-width: 1200px;
}

.logo-container {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 10px;
}

.logo-img {
  width: 120px;
  height: 120px;
  object-fit: contain;
  filter: drop-shadow(0 2px 6px rgba(0, 0, 0, 0.2));
  background: white;
  border-radius: 12px;
  padding: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.main-title {
  margin: 0;
  font-size: 32px;
  font-weight: 600;
  color: white;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
  line-height: 1.4;
}

.sub-title {
  margin: 0;
  font-size: 20px;
  font-weight: 400;
  color: white;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
  line-height: 1.4;
}

/* 白色内容区域 */
.content-section {
  flex: 1;
  max-width: 1200px;
  width: 100%;
  margin: 0 auto;
  padding: 30px 40px;
  background: white;
  min-height: calc(100vh - 300px); /* 确保填充剩余空间,避免布局变化 */
  transition: none; /* 禁用过渡动画，避免闪烁 */
  position: relative; /* 确保定位上下文 */
  overflow: visible; /* 允许内容溢出，但避免闪烁 */
  will-change: auto; /* 避免GPU加速导致的闪烁 */
}

.section-title {
  margin: 0 0 24px 0;
  font-size: 20px;
  font-weight: 600;
  color: #333;
  border-bottom: 2px solid #409eff;
  padding-bottom: 12px;
}

/* 试卷列表容器 */
.paper-list-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.paper-item {
  display: flex;
  align-items: center;
  padding: 20px;
  background: #f8f9fa;
  border: 2px solid #e4e7ed;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.3s, border-color 0.3s, box-shadow 0.3s;
  gap: 20px;
  min-height: 120px; /* 固定最小高度,防止内容变化时高度改变 */
  will-change: auto; /* 避免GPU加速导致的布局问题 */
}

.paper-item:hover {
  background: #f0f2f5;
  border-color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.15);
}

.paper-item-selected {
  background: #ecf5ff;
  border-color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.2);
}

.paper-number {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  background: #409eff;
  color: white;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 600;
}

.paper-info {
  flex: 1;
  min-width: 0;
}

.paper-name {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
}

.paper-details {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 14px;
  color: #666;
}

.paper-time {
  color: #909399;
}

.paper-practice-info {
  color: #606266;
}

.practice-remaining {
  color: #409eff;
  font-weight: 500;
}

.paper-download-status {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 200px; /* 增加宽度以容纳进度条 */
  height: 24px;
}

/* 下载进度条容器 */
.download-progress-container {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

/* 平滑进度条样式 */
.smooth-progress {
  flex: 1;
  max-width: 150px;
}

/* 关键：为进度条添加平滑过渡动画 */
.smooth-progress >>> .el-progress-bar__inner {
  transition: width 1.5s ease-out !important;
}

.progress-text {
  font-size: 12px;
  color: #409eff;
  white-space: nowrap;
  min-width: 60px;
}

/* 状态标签样式 */
.paper-download-status .el-tag {
  margin-right: 4px;
  width: 110px;
  display: inline-flex;
  justify-content: center;
  align-items: center;
}

.paper-download-status .el-button {
  margin-left: 4px;
  padding: 0 4px;
  font-size: 12px;
}

.start-button {
  flex-shrink: 0;
  min-width: 120px;
}

.empty-papers {
  text-align: center;
  padding: 60px 20px;
  color: #909399;
}

/* 旧逻辑：试卷类型选择 */
.paper-type-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.paper-card {
  margin-bottom: 20px;
}

.paper-radio {
  display: block;
  margin: 15px 0;
  font-size: 16px;
}

.button-group {
  text-align: center;
}

.button-group .el-button {
  margin: 0 10px;
}

.loading-container {
  padding: 60px;
  text-align: center;
}

/* 骨架屏淡出动画 */
.skeleton-fade {
  animation: fadeIn 0.3s ease;
}

/* 内容淡入上移动画 */
.content-fade-up {
  animation: fadeInUp 0.4s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .header-section {
    padding: 30px 15px;
    min-height: 250px;
  }
  
  .logo-img {
    width: 100px;
    height: 100px;
  }
  
  .main-title {
    font-size: 24px;
  }
  
  .sub-title {
    font-size: 18px;
  }
  
  .paper-item {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .start-button {
    width: 100%;
    margin-top: 12px;
  }
}
</style>
