<template>
  <div class="task-notification">
    <!-- 任务图标按钮 -->
    <el-badge
      :value="unfinishedCount"
      :hidden="unfinishedCount === 0"
      :max="99"
      class="task-badge"
    >
      <div class="task-icon" @click="toggleDrawer">
        <i class="el-icon-s-order"></i>
      </div>
    </el-badge>

    <!-- 任务抽屉 -->
    <el-drawer
      title="任务中心"
      :visible.sync="drawerVisible"
      direction="rtl"
      size="500px"
      :before-close="handleClose"
      class="task-drawer"
    >
      <div class="drawer-content">
        <el-tabs v-model="activeTab" @tab-click="handleTabClick">
          <!-- 进行中的任务 -->
          <el-tab-pane name="inProgress">
            <span slot="label">
              <i class="el-icon-loading"></i> 进行中
              <el-badge v-if="inProgressTasks.length > 0" :value="inProgressTasks.length" class="tab-badge" />
            </span>
            <div class="task-list">
              <el-empty v-if="inProgressTasks.length === 0" description="暂无进行中的任务" :image-size="80"></el-empty>
              <div v-else>
                <div v-for="task in inProgressTasks" :key="task.id" class="task-item">
                  <div class="task-header">
                    <span class="task-name">{{ task.name }}</span>
                    <el-tag size="mini" type="info">{{ getTaskTypeLabel(task.type) }}</el-tag>
                  </div>
                  <div class="task-progress">
                    <el-progress :percentage="task.progress"></el-progress>
                  </div>
                  <div class="task-step">{{ task.currentStep }}</div>
                  <div class="task-time">创建时间: {{ task.createTime }}</div>
                  <div class="task-actions">
                    <el-button
                      type="warning"
                      size="mini"
                      icon="el-icon-close"
                      :loading="cancellingTaskId === task.id"
                      @click="handleCancel(task)"
                    >
                      取消
                    </el-button>
                  </div>
                </div>
              </div>
            </div>
          </el-tab-pane>

          <!-- 已完成的任务 -->
          <el-tab-pane name="completed">
            <span slot="label">
              <i class="el-icon-success"></i> 已完成
              <el-badge v-if="completedTasks.length > 0" :value="completedTasks.length" class="tab-badge" />
            </span>
            <div class="task-list">
              <el-empty v-if="completedTasks.length === 0" description="暂无已完成的任务" :image-size="80"></el-empty>
              <div v-else>
                <div v-for="task in completedTasks" :key="task.id" class="task-item completed">
                  <div class="task-header">
                    <span class="task-name">{{ task.name }}</span>
                    <el-tag size="mini" type="success">{{ getTaskTypeLabel(task.type) }}</el-tag>
                  </div>
                  <div class="task-info">
                    <div class="task-time">完成时间: {{ task.completeTime }}</div>
                  </div>
                  <div class="task-actions">
                    <el-button
                      v-if="task.type === 'paperGeneration'"
                      type="primary"
                      size="mini"
                      icon="el-icon-download"
                      :loading="downloadingTaskId === task.id"
                      @click="handleDownload(task)"
                    >
                      下载试卷包
                    </el-button>
                    <el-button
                      type="danger"
                      size="mini"
                      icon="el-icon-delete"
                      @click="handleRemove(task.id)"
                    >
                      删除
                    </el-button>
                  </div>
                </div>
              </div>
            </div>
          </el-tab-pane>

          <!-- 失败的任务 -->
          <el-tab-pane name="failed">
            <span slot="label">
              <i class="el-icon-error"></i> 失败
              <el-badge v-if="failedTasks.length > 0" :value="failedTasks.length" class="tab-badge" />
            </span>
            <div class="task-list">
              <div v-if="failedTasks.length > 0" class="clear-all-container">
                <el-button type="danger" size="small" icon="el-icon-delete" @click="handleClearAllFailed">
                  清空全部失败任务
                </el-button>
              </div>
              <el-empty v-if="failedTasks.length === 0" description="暂无失败的任务" :image-size="80"></el-empty>
              <div v-else>
                <div v-for="task in failedTasks" :key="task.id" class="task-item failed">
                  <div class="task-header">
                    <span class="task-name">{{ task.name }}</span>
                    <el-tag size="mini" type="danger">{{ getTaskTypeLabel(task.type) }}</el-tag>
                  </div>
                  <div class="task-error">
                    <i class="el-icon-warning"></i>
                    <span>{{ task.error || '未知错误' }}</span>
                  </div>
                  <div class="task-time">失败时间: {{ task.updateTime }}</div>
                  <div class="task-actions">
                    <el-button
                      type="danger"
                      size="mini"
                      icon="el-icon-delete"
                      @click="handleRemove(task.id)"
                    >
                      清理
                    </el-button>
                  </div>
                </div>
              </div>
            </div>
          </el-tab-pane>

          <!-- 已取消的任务 -->
          <el-tab-pane name="cancelled">
            <span slot="label">
              <i class="el-icon-close"></i> 已取消
              <el-badge v-if="cancelledTasks.length > 0" :value="cancelledTasks.length" class="tab-badge" />
            </span>
            <div class="task-list">
              <el-empty v-if="cancelledTasks.length === 0" description="暂无已取消的任务" :image-size="80"></el-empty>
              <div v-else>
                <div v-for="task in cancelledTasks" :key="task.id" class="task-item cancelled">
                  <div class="task-header">
                    <span class="task-name">{{ task.name }}</span>
                    <el-tag size="mini" type="warning">{{ getTaskTypeLabel(task.type) }}</el-tag>
                  </div>
                  <div class="task-info">
                    <div class="task-time">取消时间: {{ task.updateTime }}</div>
                  </div>
                  <div class="task-actions">
                    <el-button
                      type="danger"
                      size="mini"
                      icon="el-icon-delete"
                      @click="handleRemove(task.id)"
                    >
                      删除
                    </el-button>
                  </div>
                </div>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-drawer>
  </div>
</template>

<script>
import { mapGetters, mapActions, mapState } from 'vuex'
import { downloadPaperPackage, downloadPaperPackageStream, getAllPackageTasks, cancelPackageTask } from '@/api/exam/paper'

export default {
  name: 'TaskNotification',
  data() {
    return {
      downloadingTaskId: null,
      cancellingTaskId: null, // 正在取消的任务ID
      pollingTimer: null, // 轮询定时器
      // 任务类型标签映射
      taskTypeLabels: {
        paperGeneration: '试卷生成',
        fileDownload: '文件下载',
        dataExport: '数据导出',
        dataImport: '数据导入'
      }
    }
  },
  mounted() {
    // 组件挂载时启动定时查询，用于更新任务通知图标上的数字徽章
    this.startPolling()
  },
  beforeDestroy() {
    // 组件销毁前清除定时器（如果存在）
    this.stopPolling()
  },
  watch: {
    // 监听抽屉打开状态，打开时查询任务列表
    drawerVisible: {
      handler(newVal) {
        if (newVal) {
          // 抽屉打开时，查询任务列表
          this.loadAllTasks()
        }
      },
      immediate: false
    }
  },
  computed: {
    ...mapGetters('task', [
      'inProgressTasks',
      'completedTasks',
      'failedTasks',
      'cancelledTasks',
      'unfinishedCount'
    ]),
    drawerVisible: {
      get() {
        return this.$store.state.task.drawerVisible
      },
      set(val) {
        this.$store.dispatch('task/setDrawerVisible', val)
      }
    },
    activeTab: {
      get() {
        return this.$store.state.task.activeTab
      },
      set(val) {
        this.$store.dispatch('task/setActiveTab', val)
      }
    }
  },
  methods: {
    ...mapActions('task', [
      'toggleDrawer',
      'setDrawerVisible',
      'removeTask',
      'clearFailedTasks',
      'loadAllTasks'
    ]),

    /**
     * 开始轮询任务状态
     * 用于更新任务通知图标上的数字徽章（显示进行中的任务数量）
     */
    startPolling() {
      // 如果已经有定时器在运行，先清除
      this.stopPolling()
      
      // 立即查询一次
      this.loadAllTasks()
      
      // 每5秒查询一次任务列表，更新进行中的任务数量
      this.pollingTimer = setInterval(() => {
        this.loadAllTasks()
      }, 5000) // 5秒查询一次
    },

    /**
     * 停止轮询
     */
    stopPolling() {
      if (this.pollingTimer) {
        clearInterval(this.pollingTimer)
        this.pollingTimer = null
      }
    },

    /**
     * 获取任务类型标签
     */
    getTaskTypeLabel(type) {
      return this.taskTypeLabels[type] || '未知类型'
    },

    /**
     * 切换抽屉显示
     */
    toggleDrawer() {
      // 切换抽屉时，如果抽屉即将打开，则查询任务列表
      const willOpen = !this.drawerVisible
      this.$store.dispatch('task/toggleDrawer')
      // 如果抽屉即将打开，查询任务列表
      if (willOpen) {
        this.$nextTick(() => {
          this.loadAllTasks()
        })
      }
    },

    /**
     * 关闭抽屉
     */
    handleClose(done) {
      this.setDrawerVisible(false)
      done()
    },

    /**
     * Tab 切换
     */
    handleTabClick(tab) {
      // Tab 切换逻辑(如果需要)
    },

    /**
     * 下载试卷包（使用流式下载，支持大文件和进度显示）
     */
    async handleDownload(task) {
      if (!task.businessId) {
        this.$message.error('任务数据异常,无法下载')
        return
      }

      this.downloadingTaskId = task.id

      // 显示下载进度提示
      const loading = this.$loading({
        lock: true,
        text: '正在下载...',
        spinner: 'el-icon-loading',
        background: 'rgba(0, 0, 0, 0.7)'
      })

      try {
        // 使用流式下载，带进度回调
        const blob = await downloadPaperPackageStream(task.businessId, (downloaded, total) => {
          const percent = total > 0 ? Math.round((downloaded / total) * 100) : 0
          const downloadedMB = (downloaded / 1024 / 1024).toFixed(2)
          const totalMB = (total / 1024 / 1024).toFixed(2)
          loading.text = `正在下载... ${percent}% (${downloadedMB} MB / ${totalMB} MB)`
        })

        // 创建下载链接
        const url = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.download = `${task.name}_试卷包.zip`
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        window.URL.revokeObjectURL(url)

        loading.close()
        this.$message.success('下载成功')
      } catch (error) {
        console.error('下载失败:', error)
        loading.close()
        this.$message.error('下载失败: ' + (error.message || '未知错误'))
      } finally {
        this.downloadingTaskId = null
      }
    },

    /**
     * 删除任务
     */
    handleRemove(taskId) {
      this.$confirm('确定要删除这个任务吗?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        // 从Vuex中删除（前端删除，不影响后端数据）
        this.removeTask(taskId)
        this.$message.success('删除成功')
        // 重新加载任务列表（确保数据同步）
        this.loadAllTasks()
      }).catch(() => {})
    },

    /**
     * 清空所有失败任务
     */
    handleClearAllFailed() {
      this.$confirm('确定要清空所有失败的任务吗?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        // 从Vuex中清空（前端删除，不影响后端数据）
        this.clearFailedTasks()
        this.$message.success('清空成功')
        // 重新加载任务列表（确保数据同步）
        this.loadAllTasks()
      }).catch(() => {})
    },

    /**
     * 取消任务
     */
    async handleCancel(task) {
      if (!task.businessId) {
        this.$message.error('任务数据异常,无法取消')
        return
      }

      this.$confirm('确定要取消这个任务吗？取消后将中断上传并标记为已取消。', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        this.cancellingTaskId = task.id
        try {
          const response = await cancelPackageTask({ id: task.businessId })
          if (response.code === 200) {
            this.$message.success('任务已取消')
            // 重新加载任务列表（确保状态同步）
            this.loadAllTasks()
          } else {
            this.$message.error(response.msg || '取消失败')
          }
        } catch (error) {
          console.error('取消任务失败:', error)
          this.$message.error('取消失败: ' + (error.message || '未知错误'))
        } finally {
          this.cancellingTaskId = null
        }
      }).catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
.task-notification {
  display: inline-block;
  height: 100%;
  line-height: 50px;
  padding: 0 8px;
  cursor: pointer;
  transition: background 0.3s;
  vertical-align: top;

  &:hover {
    background: rgba(0, 0, 0, 0.025);
  }

  .task-badge {
    position: relative;
    display: inline-block;
    vertical-align: middle;

    ::v-deep .el-badge__content {
      position: absolute;
      top: 8px;
      right: 8px;
      z-index: 1000;
      font-size: 11px;
      padding: 0 4px;
      min-width: 18px;
      height: 18px;
      line-height: 18px;
      border: 1px solid #fff;
      box-shadow: 0 0 0 1px rgba(0, 0, 0, 0.1);
    }
  }

  .task-icon {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    height: 100%;
    width: 24px;
    height: 24px;
    position: relative;
    vertical-align: middle;
    cursor: pointer;

    i {
      display: inline-block;
      width: 18px;
      height: 18px;
      line-height: 18px;
      font-size: 18px;
      color: #5a5e66;
    }
  }
}

.task-drawer {
  ::v-deep .el-drawer__header {
    margin-bottom: 20px;
    padding: 20px;
    border-bottom: 1px solid #ebeef5;
  }

  ::v-deep .el-drawer__body {
    padding: 0;
  }

  .drawer-content {
    height: 100%;
    display: flex;
    flex-direction: column;

    ::v-deep .el-tabs {
      flex: 1;
      display: flex;
      flex-direction: column;

      .el-tabs__header {
        margin: 0;
        padding: 0 20px;
      }

      .el-tabs__content {
        flex: 1;
        overflow-y: auto;
        padding: 20px;
      }

      .tab-badge {
        margin-left: 5px;
      }
    }
  }

  .task-list {
    .clear-all-container {
      margin-bottom: 15px;
      text-align: right;
    }

    .task-item {
      background: #f5f7fa;
      border-radius: 8px;
      padding: 15px;
      margin-bottom: 15px;
      transition: all 0.3s;

      &:hover {
        box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
      }

      &.completed {
        background: #f0f9ff;
        border-left: 4px solid #67c23a;
      }

      &.failed {
        background: #fef0f0;
        border-left: 4px solid #f56c6c;
      }

      &.cancelled {
        background: #fdf6ec;
        border-left: 4px solid #e6a23c;
      }

      .task-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 10px;

        .task-name {
          font-weight: 600;
          font-size: 14px;
          color: #303133;
          flex: 1;
          margin-right: 10px;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
      }

      .task-progress {
        margin-bottom: 10px;
      }

      .task-step {
        font-size: 13px;
        color: #606266;
        margin-bottom: 8px;
      }

      .task-info {
        margin-bottom: 10px;
      }

      .task-time {
        font-size: 12px;
        color: #909399;
        margin-bottom: 5px;
      }

      .task-error {
        display: flex;
        align-items: center;
        color: #f56c6c;
        font-size: 13px;
        margin-bottom: 10px;
        padding: 8px;
        background: #fff;
        border-radius: 4px;

        i {
          margin-right: 5px;
        }

        span {
          flex: 1;
        }
      }

      .task-actions {
        display: flex;
        gap: 10px;
        margin-top: 10px;

        .el-button {
          flex: 1;
        }
      }
    }
  }
}
</style>
