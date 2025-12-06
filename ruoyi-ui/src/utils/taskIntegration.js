/**
 * 任务管理集成辅助工具
 * 用于将现有的进度对话框逻辑迁移到任务管理系统
 */

/**
 * 生成试卷包 - 集成任务管理系统
 * 替换原有的 handleGeneratePackage 方法
 */
export function handleGeneratePackageWithTask(vm, row) {
    const id = row ? row.id : vm.ids[0]
    const paperName = row ? row.paperName : vm.paperList.find(p => p.id === id)?.paperName || id

    vm.$modal.confirm('是否确认生成试卷"' + paperName + '"的试卷包？生成过程可能需要一些时间，请耐心等待。').then(() => {
        vm.generatingPaperId = id
        vm.$api.generatePaperPackage({ id }).then(response => {
            // 添加任务到任务中心
            const taskId = `paper_${id}_${Date.now()}`
            vm.$store.dispatch('task/addTask', {
                id: taskId,
                type: 'paperGeneration',
                name: paperName,
                businessId: id,
                metadata: {
                    paperName: paperName
                }
            })

            // 打开任务抽屉
            vm.$store.dispatch('task/setDrawerVisible', true)
            vm.$store.dispatch('task/setActiveTab', 'inProgress')

            // 开始轮询任务状态
            startPollingWithTask(vm, id, taskId)

            vm.$message.success('任务已提交,请在右上角任务中心查看进度')
        }).catch((error) => {
            vm.generatingPaperId = null
            if (error && error.msg) {
                vm.$modal.msgError(error.msg)
            } else {
                vm.$modal.msgError('提交任务失败')
            }
        })
    }).catch(() => { })
}

/**
 * 开始轮询任务状态 - 集成任务管理系统
 */
export function startPollingWithTask(vm, paperId, taskId) {
    // 清除之前的定时器
    if (vm.pollingTimer) {
        clearInterval(vm.pollingTimer)
    }

    // 每2秒轮询一次
    vm.pollingTimer = setInterval(() => {
        pollTaskStatusWithTask(vm, paperId, taskId)
    }, 2000)

    // 立即查询一次
    pollTaskStatusWithTask(vm, paperId, taskId)
}

/**
 * 轮询查询任务状态 - 集成任务管理系统
 */
export function pollTaskStatusWithTask(vm, paperId, taskId) {
    vm.$api.getPackageTaskStatus(paperId).then(response => {
        if (response.data) {
            const taskInfo = response.data
            const progress = taskInfo.progress || 0
            const currentStep = taskInfo.currentStep || '处理中...'

            // 更新任务进度
            vm.$store.dispatch('task/updateTaskProgress', {
                id: taskId,
                progress: progress,
                currentStep: currentStep
            })

            // 检查任务状态
            if (taskInfo.status === 'SUCCESS') {
                // 任务成功
                stopPolling(vm)
                vm.generatingPaperId = null
                vm.$store.dispatch('task/completeTask', {
                    id: taskId,
                    result: {
                        version: taskInfo.newVersion,
                        packageHash: taskInfo.packageHash
                    }
                })
                vm.$message.success(`试卷包生成成功，版本：v${taskInfo.newVersion || ''}`)
                vm.getList() // 刷新列表，更新试卷包信息
            } else if (taskInfo.status === 'FAILED') {
                // 任务失败
                stopPolling(vm)
                vm.generatingPaperId = null
                vm.$store.dispatch('task/failTask', {
                    id: taskId,
                    error: taskInfo.errorMessage || '未知错误'
                })
                vm.$message.error('生成失败：' + (taskInfo.errorMessage || '未知错误'))
            } else if (taskInfo.status === 'CANCELLED') {
                // 任务已取消
                stopPolling(vm)
                vm.generatingPaperId = null
                vm.$store.dispatch('task/failTask', {
                    id: taskId,
                    error: '任务已取消'
                })
                vm.$message.warning('任务已取消')
            }
            // PENDING 和 RUNNING 状态继续轮询
        } else {
            // 任务不存在，可能已完成或已清理
            stopPolling(vm)
            vm.generatingPaperId = null
        }
    }).catch(error => {
        console.error('查询任务状态失败：', error)
        // 查询失败不停止轮询，继续尝试
    })
}

/**
 * 停止轮询
 */
export function stopPolling(vm) {
    if (vm.pollingTimer) {
        clearInterval(vm.pollingTimer)
        vm.pollingTimer = null
    }
}
