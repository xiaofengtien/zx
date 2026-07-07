<template>
  <div class="app-container dashboard">
    <el-row :gutter="20" class="stats-row">
      <el-col :xs="24" :sm="12" :md="6" :lg="6">
        <el-card class="stats-card student-card" @click.native="goToStudentArchive">
          <div class="stats-content">
            <div class="stats-icon">
              <i class="el-icon-user-solid"></i>
            </div>
            <div class="stats-info">
              <div class="stats-label">学员档案</div>
              <div class="stats-value">{{ statistics.studentCount || 0 }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6" :lg="6">
        <el-card class="stats-card paper-card" @click.native="goToPaper">
          <div class="stats-content">
            <div class="stats-icon">
              <i class="el-icon-document"></i>
            </div>
            <div class="stats-info">
              <div class="stats-label">试卷数量</div>
              <div class="stats-value">{{ statistics.paperCount || 0 }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6" :lg="6">
        <el-card class="stats-card question-card" @click.native="goToQuestion">
          <div class="stats-content">
            <div class="stats-icon">
              <i class="el-icon-question"></i>
            </div>
            <div class="stats-info">
              <div class="stats-label">题目数量</div>
              <div class="stats-value">{{ statistics.questionCount || 0 }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6" :lg="6">
        <el-card class="stats-card result-card" @click.native="goToExamResults">
          <div class="stats-content">
            <div class="stats-icon">
              <i class="el-icon-trophy"></i>
            </div>
            <div class="stats-info">
              <div class="stats-label">考试记录</div>
              <div class="stats-value">{{ statistics.examResultCount || 0 }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="chart-row" style="margin-top: 20px;">
      <el-col :xs="24" :sm="24" :md="12" :lg="12">
        <el-card>
          <div slot="header" class="clearfix">
            <span>最近考试记录</span>
            <el-button style="float: right; padding: 3px 0" type="text" @click="goToExamResults">查看全部</el-button>
          </div>
          <el-table :data="recentExamResults" style="width: 100%" v-loading="loading">
            <el-table-column prop="studentAccount" label="学员账号" width="120"></el-table-column>
            <el-table-column prop="paperName" label="试卷名称" :show-overflow-tooltip="true"></el-table-column>
            <el-table-column prop="totalScore" label="总分" width="80" align="center">
              <template slot-scope="scope">
                <span :style="{ color: scope.row.totalScore >= 60 ? '#67C23A' : '#F56C6C' }">
                  {{ scope.row.totalScore || 0 }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="考试时间" width="160">
              <template slot-scope="scope">
                {{ parseTime(scope.row.createTime, '{y}-{m}-{d} {h}:{i}') }}
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="24" :md="12" :lg="12">
        <el-card>
          <div slot="header" class="clearfix">
            <span>成绩分布统计</span>
          </div>
          <div class="score-distribution">
            <div class="score-item">
              <div class="score-label">优秀 (≥90分)</div>
              <div class="score-bar">
                <div class="score-fill excellent" :style="{ width: scoreDistribution.excellent + '%' }"></div>
              </div>
              <div class="score-count">{{ scoreDistribution.excellentCount }}人</div>
            </div>
            <div class="score-item">
              <div class="score-label">良好 (80-89分)</div>
              <div class="score-bar">
                <div class="score-fill good" :style="{ width: scoreDistribution.good + '%' }"></div>
              </div>
              <div class="score-count">{{ scoreDistribution.goodCount }}人</div>
            </div>
            <div class="score-item">
              <div class="score-label">及格 (60-79分)</div>
              <div class="score-bar">
                <div class="score-fill pass" :style="{ width: scoreDistribution.pass + '%' }"></div>
              </div>
              <div class="score-count">{{ scoreDistribution.passCount }}人</div>
            </div>
            <div class="score-item">
              <div class="score-label">不及格 (&lt;60分)</div>
              <div class="score-bar">
                <div class="score-fill fail" :style="{ width: scoreDistribution.fail + '%' }"></div>
              </div>
              <div class="score-count">{{ scoreDistribution.failCount }}人</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { listArchive } from "@/api/student/archive"
import { getPaperList } from "@/api/exam/paper"
import { getQuestionList } from "@/api/exam/question"
import { getPaperResultList } from "@/api/exam/paperResult"
import { parseTime } from "@/utils/zx"

export default {
  name: "Index",
  data() {
    return {
      loading: false,
      statistics: {
        studentCount: 0,
        paperCount: 0,
        questionCount: 0,
        examResultCount: 0
      },
      recentExamResults: [],
      scoreDistribution: {
        excellent: 0,
        excellentCount: 0,
        good: 0,
        goodCount: 0,
        pass: 0,
        passCount: 0,
        fail: 0,
        failCount: 0
      }
    }
  },
  created() {
    this.loadStatistics()
    this.loadRecentExamResults()
  },
  methods: {
    parseTime,
    // 加载统计数据
    async loadStatistics() {
      try {
        // 获取学员档案总数
        const archiveRes = await listArchive({ pageNum: 1, pageSize: 1 })
        if (archiveRes.code === 200) {
          this.statistics.studentCount = archiveRes.total || 0
        }

        // 获取试卷总数
        const paperRes = await getPaperList({ pageNum: 1, pageSize: 1 })
        if (paperRes.code === 200) {
          this.statistics.paperCount = paperRes.total || 0
        }

        // 获取题目总数
        const questionRes = await getQuestionList({ pageNum: 1, pageSize: 1 })
        if (questionRes.code === 200) {
          this.statistics.questionCount = questionRes.total || 0
        }

        // 获取考试记录总数
        const resultRes = await getPaperResultList({ pageNum: 1, pageSize: 1 })
        if (resultRes.code === 200) {
          this.statistics.examResultCount = resultRes.total || 0
        }
      } catch (error) {
        console.error('加载统计数据失败:', error)
      }
    },
    // 加载最近考试记录
    async loadRecentExamResults() {
      this.loading = true
      try {
        const res = await getPaperResultList({ 
          pageNum: 1, 
          pageSize: 10,
          orderByColumn: 'create_time',
          isAsc: 'desc'
        })
        if (res.code === 200) {
          this.recentExamResults = res.rows || []
          this.calculateScoreDistribution(res.rows || [])
        }
      } catch (error) {
        console.error('加载最近考试记录失败:', error)
      } finally {
        this.loading = false
      }
    },
    // 计算成绩分布
    calculateScoreDistribution(results) {
      if (!results || results.length === 0) {
        return
      }

      let excellentCount = 0
      let goodCount = 0
      let passCount = 0
      let failCount = 0

      results.forEach(result => {
        const score = result.totalScore || 0
        if (score >= 90) {
          excellentCount++
        } else if (score >= 80) {
          goodCount++
        } else if (score >= 60) {
          passCount++
        } else {
          failCount++
        }
      })

      const total = results.length
      this.scoreDistribution = {
        excellent: total > 0 ? (excellentCount / total * 100).toFixed(1) : 0,
        excellentCount,
        good: total > 0 ? (goodCount / total * 100).toFixed(1) : 0,
        goodCount,
        pass: total > 0 ? (passCount / total * 100).toFixed(1) : 0,
        passCount,
        fail: total > 0 ? (failCount / total * 100).toFixed(1) : 0,
        failCount
      }
    },
    // 跳转到学员档案页面
    async goToStudentArchive() {
      // 尝试多种可能的菜单名称和路径
      const possiblePaths = [
        this.findMenuPath('学员档案'),
        this.findMenuPath('学员管理'),
        this.findMenuPath('学生档案'),
        '/student/archive',
        '/archive'
      ].filter(p => p) // 过滤掉空值
      
      // 尝试每个路径
      for (const path of possiblePaths) {
        try {
          console.log('尝试跳转到:', path)
          await this.$router.push(path)
          console.log('跳转成功:', path)
          return // 成功则退出
        } catch (err) {
          console.error('跳转失败:', path, err)
          // 继续尝试下一个路径
        }
      }
      
      // 如果所有路径都失败，显示错误提示
      this.$message.warning('无法找到学员档案页面，请检查菜单配置或联系管理员')
    },
    // 跳转到试卷页面
    goToPaper() {
      const path = this.findMenuPath('试卷管理') || this.findMenuPath('试卷') || '/exam/paper'
      this.$router.push(path).catch(err => {
        console.error('跳转失败:', err)
        this.$message.warning('页面不存在或无权限访问')
      })
    },
    // 跳转到题目页面
    goToQuestion() {
      const path = this.findMenuPath('题目管理') || this.findMenuPath('题目') || '/exam/question'
      this.$router.push(path).catch(err => {
        console.error('跳转失败:', err)
        this.$message.warning('页面不存在或无权限访问')
      })
    },
    // 跳转到考试记录页面
    goToExamResults() {
      const path = this.findMenuPath('考试记录') || this.findMenuPath('答题结果') || '/exam/paper-result'
      this.$router.push(path).catch(err => {
        console.error('跳转失败:', err)
        this.$message.warning('页面不存在或无权限访问')
      })
    },
    // 查找菜单路径
    findMenuPath(title) {
      try {
        const routes = this.$store.getters.permission_routes || []
        console.log('查找菜单路径:', title, '可用路由数量:', routes.length)
        
        const findPath = (routes, targetTitle, parentPath = '') => {
          for (const route of routes) {
            // 构建当前路由的完整路径
            let currentPath = route.path
            if (parentPath && route.path && !route.path.startsWith('/')) {
              // 子路由且不是绝对路径，需要拼接父路径
              currentPath = parentPath === '/' ? `/${route.path}` : `${parentPath}/${route.path}`
            } else if (parentPath && route.path && route.path.startsWith('/')) {
              // 子路由是绝对路径，直接使用
              currentPath = route.path
            } else if (!currentPath.startsWith('/')) {
              // 根路由但不是绝对路径，添加 /
              currentPath = `/${currentPath}`
            }
            
            // 检查当前路由
            if (route.meta && route.meta.title === targetTitle) {
              console.log('找到匹配的路由:', currentPath, route.meta.title)
              return currentPath
            }
            
            // 检查子路由
            if (route.children && route.children.length > 0) {
              const childPath = findPath(route.children, targetTitle, currentPath)
              if (childPath) {
                console.log('找到子路由:', childPath)
                return childPath
              }
            }
          }
          return null
        }
        
        const path = findPath(routes, title)
        console.log('最终路径:', path)
        return path
      } catch (error) {
        console.error('查找菜单路径出错:', error)
        return null
      }
    }
  }
}
</script>

<style scoped lang="scss">
.dashboard {
  padding: 20px;
}

.stats-row {
  margin-bottom: 20px;
}

.stats-card {
  cursor: pointer;
  transition: all 0.3s;
  
  &:hover {
    transform: translateY(-5px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  }

  .stats-content {
    display: flex;
    align-items: center;
    padding: 10px 0;

    .stats-icon {
      width: 60px;
      height: 60px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 28px;
      color: #fff;
      margin-right: 20px;
    }

    .stats-info {
      flex: 1;

      .stats-label {
        font-size: 14px;
        color: #909399;
        margin-bottom: 8px;
      }

      .stats-value {
        font-size: 32px;
        font-weight: bold;
        color: #303133;
      }
    }
  }

  &.student-card .stats-icon {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  }

  &.paper-card .stats-icon {
    background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  }

  &.question-card .stats-icon {
    background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  }

  &.result-card .stats-icon {
    background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
  }
}

.chart-row {
  .el-card {
    min-height: 400px;
  }
}

.score-distribution {
  padding: 20px 0;

  .score-item {
    margin-bottom: 25px;
    display: flex;
    align-items: center;

    .score-label {
      width: 120px;
      font-size: 14px;
      color: #606266;
    }

    .score-bar {
      flex: 1;
      height: 24px;
      background: #f0f2f5;
      border-radius: 12px;
      overflow: hidden;
      margin: 0 15px;

      .score-fill {
        height: 100%;
        border-radius: 12px;
        transition: width 0.5s ease;

        &.excellent {
          background: linear-gradient(90deg, #67C23A 0%, #85CE61 100%);
        }

        &.good {
          background: linear-gradient(90deg, #409EFF 0%, #66B1FF 100%);
        }

        &.pass {
          background: linear-gradient(90deg, #E6A23C 0%, #EEBE77 100%);
        }

        &.fail {
          background: linear-gradient(90deg, #F56C6C 0%, #F78989 100%);
        }
      }
    }

    .score-count {
      width: 60px;
      text-align: right;
      font-size: 14px;
      color: #909399;
    }
  }
}
</style>
