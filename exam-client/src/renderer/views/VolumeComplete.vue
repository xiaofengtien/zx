<template>
  <div class="volume-complete-container">
    <div class="content-area">
      <div class="complete-message">
        <div class="message-icon">
          <i class="el-icon-success"></i>
        </div>
        <div class="message-text">
          {{ isAllComplete ? '作答完成' : volumeName + '作答完成' }}
        </div>
      </div>
    </div>
  </div>
</template>

<script>
const { ipcRenderer } = require('electron')

export default {
  name: 'VolumeComplete',
  data() {
    return {
      paperId: null,
      paperInfoId: null,
      currentVolumeCode: '',
      volumeName: '',
      isAllComplete: false, // 是否所有卷别都完成
      timer: null
    }
  },
  async mounted() {
    // 获取参数
    this.paperId = parseInt(this.$route.query.paperId) || parseInt(localStorage.getItem('currentPaperId')) || null
    this.paperInfoId = parseInt(this.$route.query.paperInfoId) || parseInt(localStorage.getItem('currentPaperInfoId')) || null
    this.currentVolumeCode = localStorage.getItem('currentVolumeCode') || localStorage.getItem('completedVolumeCode') || ''
    
    if (!this.paperId) {
      this.$message.error('未找到试卷ID')
      setTimeout(() => {
        this.$router.push('/paper-select')
      }, 2000)
      return
    }

    // 如果没有 paperInfoId，尝试从数据库获取
    if (!this.paperInfoId) {
      try {
        const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
        const userId = userInfo.userId || userInfo.user?.userId
        
        if (userId) {
          const paperInfoList = await ipcRenderer.invoke('answer:getPaperInfoList', userId, this.paperId)
          const unsubmittedPaperInfo = paperInfoList.find(p => p.is_submit === 0)
          if (unsubmittedPaperInfo) {
            this.paperInfoId = unsubmittedPaperInfo.id
            localStorage.setItem('currentPaperInfoId', this.paperInfoId.toString())
          }
        }
      } catch (error) {
        console.error('获取答题记录失败:', error)
      }
    }

    // 获取当前卷别名称
    if (this.paperId) {
      try {
        const paperData = await ipcRenderer.invoke('paper:getPaperData', this.paperId)
        if (paperData && paperData.manifest && paperData.manifest.volumes) {
          const volumes = paperData.manifest.volumes
          const currentVolume = volumes.find(v => 
            (v.volumeCode || v.volume_code) === this.currentVolumeCode
          )
          if (currentVolume) {
            this.volumeName = currentVolume.volumeName || currentVolume.volume_name || this.currentVolumeCode
          } else {
            this.volumeName = this.currentVolumeCode
          }
        } else {
          this.volumeName = this.currentVolumeCode
        }
      } catch (error) {
        console.error('获取卷别信息失败:', error)
        this.volumeName = this.currentVolumeCode
      }
    } else {
      this.volumeName = this.currentVolumeCode
    }

    // 先检查是否有下一卷别
    await this.checkAndProceed()
  },
  beforeDestroy() {
    if (this.timer) {
      clearTimeout(this.timer)
    }
  },
  methods: {
    /**
     * 检查并决定下一步操作
     * 顺序：先显示当前卷别完成 -> 3秒后检查下一卷别 -> 如果没有则显示"作答完成" -> 3秒后跳转结果页
     */
    async checkAndProceed() {
      try {
        const paperData = await ipcRenderer.invoke('paper:getPaperData', this.paperId)
        if (!paperData || !paperData.manifest || !paperData.manifest.volumes) {
          // 没有卷别信息，显示作答完成后跳转
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
        let nextValidVolume = null
        let nextVolumeCode = null
        for (let i = currentIndex + 1; i < sortedVolumes.length; i++) {
          const nextVolume = sortedVolumes[i]
          const hasValidContent = await this.checkVolumeHasQuestions(nextVolume, paperData)
          if (hasValidContent) {
            nextValidVolume = nextVolume
            nextVolumeCode = nextVolume.volumeCode || nextVolume.volume_code
            break
          }
        }
        
        if (nextValidVolume) {
          // 有下一卷别，3秒后跳转
          console.log(`✓ [VolumeComplete] 找到下一个有效卷别: ${nextVolumeCode}`)
          this.timer = setTimeout(async () => {
            await this.goToNextVolume(nextValidVolume, nextVolumeCode)
          }, 3000)
        } else {
          // 没有更多卷别，3秒后显示"作答完成"，再3秒后跳转结果页
          this.timer = setTimeout(() => {
            this.showAllCompleteAndSubmit()
          }, 3000)
        }
      } catch (error) {
        console.error('检查下一卷别失败:', error)
        this.showAllCompleteAndSubmit()
      }
    },
    
    /**
     * 跳转到下一卷别
     */
    async goToNextVolume(nextVolume, nextVolumeCode) {
      try {
        // 检查是否有中场设置
        const intermissions = await ipcRenderer.invoke('paper:getIntermissions', this.paperId)
        const intermission = intermissions.find(i => 
          i.from_volume === this.currentVolumeCode && 
          i.to_volume === nextVolumeCode
        )
        
        if (intermission) {
          // 有中场设置，跳转到中场页面
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
          // 没有中场设置，直接跳转到下一卷别的广播页面
          localStorage.setItem('currentVolumeCode', nextVolumeCode)
          this.$router.push('/broadcast')
        }
      } catch (error) {
        console.error('跳转下一卷别失败:', error)
        this.showAllCompleteAndSubmit()
      }
    },
    
    /**
     * 显示"作答完成"，3秒后提交并跳转结果页
     */
    showAllCompleteAndSubmit() {
      this.isAllComplete = true
      this.timer = setTimeout(async () => {
        await this.submitAllAndComplete()
      }, 3000)
    },
    
    /**
     * 检查卷别是否有有效内容（大题和题目）
     */
    async checkVolumeHasQuestions(volume, paperData) {
      try {
        if (!paperData || !paperData.manifest) return false
        
        const manifest = paperData.manifest
        const sections = manifest.sections || []
        const volumeId = volume.id
        
        // 找到属于这个卷别的大题
        const volumeSections = sections.filter(s => s.volumeId === volumeId)
        if (volumeSections.length === 0) {
          console.log(`⚠️ [checkVolumeHasQuestions] 卷别 ${volume.volumeCode || volume.volume_code} 没有大题`)
          return false
        }
        
        // 检查这些大题是否有题目
        for (const section of volumeSections) {
          const questions = await ipcRenderer.invoke('paper:getQuestions', this.paperId, section.id)
          if (questions && questions.length > 0) {
            console.log(`✓ [checkVolumeHasQuestions] 卷别 ${volume.volumeCode || volume.volume_code} 有题目`)
            return true
          }
        }
        
        console.log(`⚠️ [checkVolumeHasQuestions] 卷别 ${volume.volumeCode || volume.volume_code} 的大题都没有题目`)
        return false
      } catch (error) {
        console.error('检查卷别内容失败:', error)
        return false
      }
    },
    
    /**
     * 检查所有卷别是否完成，然后整体提交
     */
    async submitAllAndComplete() {
      try {
        const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
        const userId = userInfo.userId || userInfo.user?.userId
        
        if (!userId || !this.paperId) {
          console.error('缺少必要信息')
          this.$router.push('/paper-select')
          return
        }

        // 如果没有 paperInfoId，尝试获取
        if (!this.paperInfoId) {
          const paperInfoList = await ipcRenderer.invoke('answer:getPaperInfoList', userId, this.paperId)
          const unsubmittedPaperInfo = paperInfoList.find(p => p.is_submit === 0)
          if (unsubmittedPaperInfo) {
            this.paperInfoId = unsubmittedPaperInfo.id
          }
        }

        if (!this.paperInfoId) {
          console.error('未找到答题记录')
          this.$router.push('/paper-select')
          return
        }

        // 获取答题记录，检查所有卷别状态
        const paperInfo = await ipcRenderer.invoke('answer:getPaperInfo', this.paperInfoId)
        if (!paperInfo) {
          console.error('未找到答题记录')
          this.$router.push('/paper-select')
          return
        }

        const volumeStatus = paperInfo.volume_status ? JSON.parse(paperInfo.volume_status) : {}
        
        // 获取试卷数据
        const paperData = await ipcRenderer.invoke('paper:getPaperData', this.paperId)
        const volumes = paperData?.manifest?.volumes || []
        
        // 检查所有卷别是否都已完成
        let allVolumesCompleted = true
        for (const volume of volumes) {
          const volumeCode = volume.volumeCode || volume.volume_code
          if (volumeStatus[volumeCode] !== 'completed') {
            allVolumesCompleted = false
            break
          }
        }

        if (!allVolumesCompleted) {
          console.warn('⚠️ 并非所有卷别都已完成，但已没有更多卷别，强制提交')
        }

        // 计算所有题目的统计信息
        const questionResults = paperInfo.questionResults || []
        const correctCount = questionResults.filter(r => r.result === 1).length
        const wrongCount = questionResults.filter(r => r.result === 0).length

        // 计算总分和得分
        const totalScore = paperData?.manifest?.totalScore
        const scorePerQuestion = totalScore / questionResults.length
        const userScore = Math.round(correctCount * scorePerQuestion)

        // 提交整体试卷
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
          // 跳转到答题完成页面
          this.$router.push({
            path: '/exam-result',
            query: {
              paperInfoId: this.paperInfoId
            }
          })
        } else {
          throw new Error(submitResult.message || '提交答题结果失败')
        }
      } catch (error) {
        console.error('提交整体试卷失败:', error)
        this.$message.error('提交答题结果失败：' + error.message)
        this.$router.push('/paper-select')
      }
    }
  }
}
</script>

<style scoped>
.volume-complete-container {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  width: 100vw;
  height: 100vh;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.content-area {
  text-align: center;
  padding: 40px;
  margin-top: -100px; /* 向上偏移80px，使内容视觉居中 */
}

.complete-message {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 20px;
}

.message-icon {
  font-size: 80px;
  color: #67C23A;
}

.message-icon i {
  font-size: 80px;
}

.message-text {
  font-size: 32px;
  font-weight: bold;
  color: #303133;
  text-align: center;
}
</style>


