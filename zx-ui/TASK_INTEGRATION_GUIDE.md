# 任务管理系统集成指南

本文档说明如何将现有的试卷生成进度对话框迁移到新的任务管理系统。

## 已完成的工作

1. ✅ 创建了 Vuex 任务管理模块 (`/src/store/modules/task.js`)
2. ✅ 创建了任务通知组件 (`/src/components/TaskNotification/index.vue`)
3. ✅ 将任务通知组件集成到导航栏 (`/src/layout/components/Navbar.vue`)
4. ✅ 注册了 task 模块到 Vuex store (`/src/store/index.js`)

## 需要手动集成的页面

由于文件编辑工具的限制,以下页面需要手动修改:

### 1. 试卷列表页 (`/src/views/exam/paper/index.vue`)

#### 修改步骤:

**步骤 1**: 找到 `handleGeneratePackage` 方法(约第 961-986 行),将其替换为:

```javascript
/** 生成试卷包 */
handleGeneratePackage(row) {
  const id = row ? row.id : this.ids[0]
  const paperName = row ? row.paperName : this.paperList.find(p => p.id === id)?.paperName || id
  this.$modal.confirm('是否确认生成试卷"' + paperName + '"的试卷包？生成过程可能需要一些时间，请耐心等待。').then(() => {
    this.generatingPaperId = id
    generatePaperPackage({ id }).then(response => {
      // 添加任务到任务中心
      const taskId = `paper_${id}_${Date.now()}`
      this.$store.dispatch('task/addTask', {
        id: taskId,
        type: 'paperGeneration',
        name: paperName,
        businessId: id,
        metadata: {
          paperName: paperName
        }
      })
      
      // 打开任务抽屉
      this.$store.dispatch('task/setDrawerVisible', true)
      this.$store.dispatch('task/setActiveTab', 'inProgress')
      
      // 开始轮询任务状态
      this.startPolling(id, taskId)
      
      this.$message.success('任务已提交,请在右上角任务中心查看进度')
    }).catch((error) => {
      this.generatingPaperId = null
      if (error && error.msg) {
        this.$modal.msgError(error.msg)
      } else {
        this.$modal.msgError('提交任务失败')
      }
    })
  }).catch(() => {})
},
```

**步骤 2**: 找到 `startPolling` 方法(约第 987-1001 行),修改其参数:

```javascript
/** 开始轮询任务状态 */
startPolling(paperId, taskId) {
  // 清除之前的定时器
  if (this.pollingTimer) {
    clearInterval(this.pollingTimer)
  }
  
  // 每2秒轮询一次
  this.pollingTimer = setInterval(() => {
    this.pollTaskStatus(paperId, taskId)
  }, 2000)
  
  // 立即查询一次
  this.pollTaskStatus(paperId, taskId)
},
```

**步骤 3**: 找到 `pollTaskStatus` 方法(约第 1002-1042 行),将其替换为:

```javascript
/** 轮询查询任务状态 */
pollTaskStatus(paperId, taskId) {
  getPackageTaskStatus(paperId).then(response => {
    if (response.data) {
      const taskInfo = response.data
      const progress = taskInfo.progress || 0
      const currentStep = taskInfo.currentStep || '处理中...'
      
      // 更新任务进度
      this.$store.dispatch('task/updateTaskProgress', {
        id: taskId,
        progress: progress,
        currentStep: currentStep
      })
      
      // 检查任务状态
      if (taskInfo.status === 'SUCCESS') {
        // 任务成功
        this.stopPolling()
        this.generatingPaperId = null
        this.$store.dispatch('task/completeTask', {
          id: taskId,
          result: {
            version: taskInfo.newVersion,
            packageHash: taskInfo.packageHash
          }
        })
        this.$message.success(`试卷包生成成功，版本：v${taskInfo.newVersion || ''}`)
        this.getList() // 刷新列表，更新试卷包信息
      } else if (taskInfo.status === 'FAILED') {
        // 任务失败
        this.stopPolling()
        this.generatingPaperId = null
        this.$store.dispatch('task/failTask', {
          id: taskId,
          error: taskInfo.errorMessage || '未知错误'
        })
        this.$message.error('生成失败：' + (taskInfo.errorMessage || '未知错误'))
      } else if (taskInfo.status === 'CANCELLED') {
        // 任务已取消
        this.stopPolling()
        this.generatingPaperId = null
        this.$store.dispatch('task/failTask', {
          id: taskId,
          error: '任务已取消'
        })
        this.$message.warning('任务已取消')
      }
      // PENDING 和 RUNNING 状态继续轮询
    } else {
      // 任务不存在，可能已完成或已清理
      this.stopPolling()
      this.generatingPaperId = null
    }
  }).catch(error => {
    console.error('查询任务状态失败：', error)
    // 查询失败不停止轮询，继续尝试
  })
},
```

**步骤 4**: (可选) 删除进度对话框相关代码:

- 在模板中删除 `<!-- 生成试卷包进度对话框 -->` 部分(约第 593-628 行)
- 在 data 中删除以下字段:
  - `progressDialogVisible`
  - `progressDialogPaperId`
  - `progressDialogPaperName`
  - `progress`
  - `currentStep`
- 删除 `handleCancelTask` 方法(约第 1050-1066 行)

### 2. 试卷编辑页 (`/src/views/exam/paper/edit.vue`)

在 edit.vue 中找到生成试卷包的代码(约第 776-789 行),进行类似的修改:

```javascript
this.$modal.confirm('是否立即生成试卷包？生成过程可能需要一些时间，请耐心等待。').then(() => {
  // 添加任务到任务中心
  const taskId = `paper_${this.form.id}_${Date.now()}`
  this.$store.dispatch('task/addTask', {
    id: taskId,
    type: 'paperGeneration',
    name: this.form.paperName || this.form.customName,
    businessId: this.form.id,
    metadata: {
      paperName: this.form.paperName || this.form.customName
    }
  })
  
  // 打开任务抽屉
  this.$store.dispatch('task/setDrawerVisible', true)
  this.$store.dispatch('task/setActiveTab', 'inProgress')
  
  import('@/api/exam/paper').then(module => {
    module.generatePaperPackage({ id: this.form.id }).then(packageResponse => {
      this.$message.success('任务已提交,请在右上角任务中心查看进度')
      // 可以添加轮询逻辑,或者直接返回
      this.handleCancel()
    }).catch(() => {
      this.$store.dispatch('task/failTask', {
        id: taskId,
        error: '提交任务失败'
      })
      this.$modal.msgError("试卷包生成失败")
      this.handleCancel()
    })
  })
}).catch(() => {
  this.handleCancel()
})
```

### 3. 学生档案页 (`/src/views/student/archive/index.vue`)

如果该页面也有生成试卷包功能,进行类似的修改。

## 测试步骤

完成集成后,按以下步骤测试:

1. 启动前端开发服务器: `cd zx-ui && npm run dev`
2. 登录系统,进入试卷管理页面
3. 选择一个试卷,点击"生成试卷包"
4. 验证:
   - 右上角出现通知图标,显示徽章(1)
   - 不再显示全屏进度对话框
   - 点击通知图标,打开任务抽屉
   - 在"进行中"Tab 中看到任务,显示进度条
   - 等待任务完成,任务移动到"已完成"Tab
   - 点击"下载"按钮,成功下载试卷包

## 注意事项

1. 任务数据存储在 Vuex 中,页面刷新后会丢失
2. 如需持久化,需要后端支持(创建任务表)
3. 任务 ID 格式: `paper_{paperId}_{timestamp}`
4. 支持扩展其他业务类型的任务

## 问题排查

如果遇到问题:

1. 检查浏览器控制台是否有错误
2. 确认 Vuex store 中 task 模块已正确注册
3. 确认 TaskNotification 组件已正确导入到 Navbar
4. 检查任务状态更新是否正常(使用 Vue Devtools)
