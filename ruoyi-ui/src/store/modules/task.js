/**
 * 任务管理 Vuex 模块
 * 用于管理异步任务(如试卷生成、文件下载等)的状态
 */

const state = {
    // 任务列表
    tasks: [],
    // 抽屉显示状态
    drawerVisible: false,
    // 当前激活的 Tab
    activeTab: 'inProgress'
}

const getters = {
    // 按创建时间倒序排序（最新的在前面）
    sortedTasks: state => {
        return [...state.tasks].sort((a, b) => {
            // 解析时间字符串进行比较
            const timeA = a.createTime ? new Date(a.createTime).getTime() : 0
            const timeB = b.createTime ? new Date(b.createTime).getTime() : 0
            return timeB - timeA // 倒序
        })
    },
    // 进行中的任务（已排序）
    inProgressTasks: (state, getters) => {
        return getters.sortedTasks.filter(task => task.status === 'inProgress')
    },
    // 已完成的任务（已排序）
    completedTasks: (state, getters) => {
        return getters.sortedTasks.filter(task => task.status === 'completed')
    },
    // 失败的任务（已排序）
    failedTasks: (state, getters) => {
        return getters.sortedTasks.filter(task => task.status === 'failed')
    },
    // 已取消的任务（已排序）
    cancelledTasks: (state, getters) => {
        return getters.sortedTasks.filter(task => task.status === 'cancelled')
    },
    // 未完成任务数量(用于徽章显示)
    unfinishedCount: state => {
        return state.tasks.filter(task => task.status === 'inProgress').length
    },
    // 根据 ID 获取任务
    getTaskById: state => id => {
        return state.tasks.find(task => task.id === id)
    }
}

const mutations = {
    // 添加新任务
    ADD_TASK(state, task) {
        // 检查是否已存在相同 ID 的任务
        const existingIndex = state.tasks.findIndex(t => t.id === task.id)
        if (existingIndex !== -1) {
            // 如果存在,更新任务
            state.tasks.splice(existingIndex, 1, task)
        } else {
            // 否则添加新任务
            state.tasks.unshift(task)
        }
    },

    // 更新任务
    UPDATE_TASK(state, { id, updates }) {
        const task = state.tasks.find(t => t.id === id)
        if (task) {
            Object.assign(task, updates)
        }
    },

    // 删除任务
    REMOVE_TASK(state, id) {
        const index = state.tasks.findIndex(t => t.id === id)
        if (index !== -1) {
            state.tasks.splice(index, 1)
        }
    },

    // 清空失败任务
    CLEAR_FAILED_TASKS(state) {
        state.tasks = state.tasks.filter(task => task.status !== 'failed')
    },

    // 设置抽屉显示状态
    SET_DRAWER_VISIBLE(state, visible) {
        state.drawerVisible = visible
    },

    // 设置激活的 Tab
    SET_ACTIVE_TAB(state, tab) {
        state.activeTab = tab
    },

    // 设置所有任务（用于从后端加载）
    SET_ALL_TASKS(state, tasks) {
        state.tasks = tasks
    }
}

/**
 * 将后端任务格式转换为前端任务格式
 */
function convertBackendTaskToFrontend(backendTask) {
    // 状态映射：SUCCESS -> completed, FAILED -> failed, CANCELLED -> cancelled, PENDING/RUNNING -> inProgress
    let status = 'inProgress'
    if (backendTask.status === 'SUCCESS') {
        status = 'completed'
    } else if (backendTask.status === 'FAILED') {
        status = 'failed'
    } else if (backendTask.status === 'CANCELLED') {
        status = 'cancelled'
    }

    // 生成任务ID（使用paperId作为唯一标识）
    const taskId = `paper_${backendTask.paperId}_${backendTask.startTime || Date.now()}`

    // 格式化时间
    const formatTime = (timestamp) => {
        if (!timestamp) return ''
        return new Date(timestamp).toLocaleString('zh-CN')
    }

    // 生成任务名称，格式：试卷包生成 - ${paperName}V${version}
    // 如果后端提供了paperName，使用paperName；否则使用默认格式
    // 显示逻辑：
    // 1. 如果任务已完成（有newVersion），显示新版本号（newVersion）
    // 2. 如果任务进行中（没有newVersion但有currentVersion），显示"正在生成V{currentVersion+1}"
    // 3. 如果都没有，只显示试卷名称
    let taskName
    let version = null
    let versionSuffix = ''

    if (backendTask.newVersion != null) {
        // 任务已完成，显示新版本号
        version = backendTask.newVersion
        versionSuffix = `V${version}`
    } else if (backendTask.currentVersion != null) {
        // 任务进行中，显示即将生成的新版本号（currentVersion + 1）
        version = backendTask.currentVersion + 1
        versionSuffix = `V${version}`
    }

    if (backendTask.paperName) {
        // 使用后端返回的paperName，格式：试卷包生成 - ${paperName}V${version}
        if (versionSuffix) {
            taskName = `试卷包生成 - ${backendTask.paperName}${versionSuffix}`
        } else {
            // 如果完全没有版本号，只显示paperName
            taskName = `试卷包生成 - ${backendTask.paperName}`
        }
    } else {
        // 如果没有paperName，使用默认格式
        taskName = `试卷包生成 - 试卷ID: ${backendTask.paperId}`
        if (versionSuffix) {
            taskName += versionSuffix
        }
    }

    return {
        id: taskId,
        type: 'paperGeneration',
        name: taskName,
        businessId: backendTask.paperId,
        status: status,
        progress: backendTask.progress || 0,
        currentStep: backendTask.currentStep || '处理中...',
        createTime: formatTime(backendTask.startTime),
        updateTime: formatTime(backendTask.finishTime || backendTask.startTime),
        completeTime: backendTask.status === 'SUCCESS' ? formatTime(backendTask.finishTime) : null,
        error: backendTask.errorMessage || null,
        metadata: {
            paperId: backendTask.paperId,
            newVersion: backendTask.newVersion
        }
    }
}

const actions = {
    /**
     * 从后端加载所有任务
     */
    async loadAllTasks({ commit, state }) {
        try {
            const { getAllPackageTasks } = require('@/api/exam/paper')
            const response = await getAllPackageTasks()
            if (response.code === 200 && response.data) {
                const backendTasks = response.data
                const frontendTasks = backendTasks.map(backendTask => {
                    return convertBackendTaskToFrontend(backendTask)
                })

                // 合并策略：保留前端新添加的任务（可能还未同步到后端），合并后端任务
                const existingTaskIds = new Set(state.tasks.map(t => t.id))
                const newTasks = frontendTasks.filter(t => !existingTaskIds.has(t.id))
                const mergedTasks = [...state.tasks, ...newTasks]

                // 更新已存在的任务（从后端获取最新状态）
                const updatedTasks = mergedTasks.map(frontendTask => {
                    const backendTask = backendTasks.find(bt => {
                        const backendTaskId = `paper_${bt.paperId}_${bt.startTime || 0}`
                        return backendTaskId === frontendTask.id
                    })
                    if (backendTask) {
                        // 如果后端有对应任务，使用后端数据
                        return convertBackendTaskToFrontend(backendTask)
                    }
                    // 否则保留前端任务（可能是刚添加的，还未同步到后端）
                    return frontendTask
                })

                // 清空现有任务，替换为合并后的任务
                commit('SET_ALL_TASKS', updatedTasks)
            }
        } catch (error) {
            console.error('加载任务列表失败:', error)
        }
    },

    /**
     * 添加任务
     * @param {Object} taskData - 任务数据
     * @param {String} taskData.id - 任务唯一标识
     * @param {String} taskData.type - 任务类型(paperGeneration, fileDownload 等)
     * @param {String} taskData.name - 任务名称
     * @param {String} taskData.businessId - 业务 ID(如试卷 ID)
     * @param {Object} taskData.metadata - 任务元数据
     */
    addTask({ commit }, taskData) {
        const task = {
            id: taskData.id,
            type: taskData.type || 'paperGeneration',
            name: taskData.name,
            businessId: taskData.businessId,
            status: 'inProgress',
            progress: 0,
            currentStep: '任务已提交，等待处理...',
            createTime: new Date().toLocaleString('zh-CN'),
            updateTime: new Date().toLocaleString('zh-CN'),
            metadata: taskData.metadata || {},
            error: null
        }
        commit('ADD_TASK', task)
    },

    /**
     * 更新任务进度
     */
    updateTaskProgress({ commit }, { id, progress, currentStep }) {
        commit('UPDATE_TASK', {
            id,
            updates: {
                progress,
                currentStep,
                updateTime: new Date().toLocaleString('zh-CN')
            }
        })
    },

    /**
     * 标记任务完成
     */
    completeTask({ commit }, { id, result }) {
        commit('UPDATE_TASK', {
            id,
            updates: {
                status: 'completed',
                progress: 100,
                currentStep: '任务已完成',
                completeTime: new Date().toLocaleString('zh-CN'),
                updateTime: new Date().toLocaleString('zh-CN'),
                result: result || {}
            }
        })
    },

    /**
     * 标记任务失败
     */
    failTask({ commit }, { id, error }) {
        commit('UPDATE_TASK', {
            id,
            updates: {
                status: 'failed',
                currentStep: '任务失败',
                error: error || '未知错误',
                updateTime: new Date().toLocaleString('zh-CN')
            }
        })
    },

    /**
     * 删除任务
     */
    removeTask({ commit }, id) {
        commit('REMOVE_TASK', id)
    },

    /**
     * 清空失败任务
     */
    clearFailedTasks({ commit }) {
        commit('CLEAR_FAILED_TASKS')
    },

    /**
     * 切换抽屉显示状态
     */
    toggleDrawer({ commit, state }) {
        commit('SET_DRAWER_VISIBLE', !state.drawerVisible)
    },

    /**
     * 设置抽屉显示状态
     */
    setDrawerVisible({ commit }, visible) {
        commit('SET_DRAWER_VISIBLE', visible)
    },

    /**
     * 设置激活的 Tab
     */
    setActiveTab({ commit }, tab) {
        commit('SET_ACTIVE_TAB', tab)
    }
}

export default {
    namespaced: true,
    state,
    getters,
    mutations,
    actions
}
