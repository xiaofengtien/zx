# 客户端功能实现 TODO 清单

## 概述

本文档列出了客户端（Electron应用）需要实现的所有功能，按照优先级和依赖关系组织。

---

## Phase 1: 数据同步和存储（基础功能）

### 1.1 试卷包同步增强
- [ ] **更新试卷包解压逻辑**
  - 支持新的 `manifest.json` 结构（包含 `volumes`, `sections`, `intermissions`, `trialListenMedia`, `notes` 等）
  - 支持新的 `questions.json` 结构（包含 `sectionId`, `sectionOrder`, `explanationEnabled`, `explanationText`, `media` 数组等）
  - 支持新的媒体文件目录结构（`trial_listen/`, `volumes/`, `sections/`, `intermission/`, `questions/`, `options/`, `explanations/`）

- [ ] **更新本地数据库表结构**
  - 确保 `paper` 表包含新字段（`practice_limit`, `trial_listen_enabled`, `trial_listen_text`, `notes`, `notes_display_mode`）
  - 创建/更新 `paper_volume` 表（卷别表）
  - 创建/更新 `paper_section` 表（大题表）
  - 创建/更新 `paper_intermission` 表（中场配置表）
  - 更新 `paper_question` 表（添加 `section_id`, `section_order`）
  - 更新 `question` 表（添加 `explanation_enabled`, `explanation_text`, `explanation_delay_seconds`）
  - 更新 `question_media` 表（添加 `paper_id`, `volume_id`, `section_id`, `intermission_id`，支持新的 `media_type` 4-11）

- [ ] **更新同步服务（syncService.js）**
  - 同步 `paper_volume` 表数据
  - 同步 `paper_section` 表数据
  - 同步 `paper_intermission` 表数据
  - 同步新的 `question_media` 数据（支持所有 media_type）

### 1.2 答题数据存储增强
- [ ] **更新 `app_user_paper_info` 表**
  - 添加 `practice_count`（已练习次数）
  - 添加 `last_practice_time`（最后练习时间）
  - 添加 `volume_status`（各卷状态，JSON格式：`{"A":"completed","B":"in_progress"}`）
  - 添加 `volume_submit_time`（各卷提交时间，JSON格式：`{"A":"2024-01-01 10:00:00","B":null}`）
  - 添加 `intermission_played`（中场音频播放状态，JSON格式：`{"A->B":true}`）
  - 添加 `assigned_seat_number`（分配的机位号）
  - 添加 `actual_seat_number`（实际坐的机位号）

- [ ] **更新 `answerService.js`**
  - 支持多卷答题状态管理
  - 支持练习次数统计和限制检查
  - 支持中场音频播放状态记录
  - 支持按卷提交答题结果

---

## Phase 2: 试卷选择页面增强（PaperSelect.vue）

### 2.1 试卷列表显示
- [x] 支持根据 `applicable_paper_ids` 显示试卷列表（已完成）
- [ ] **显示试卷详细信息**
  - 显示试卷名称（`paper_name`）
  - 显示试卷类型（`paper_type`）
  - 显示练习次数限制（`practice_limit`，0表示不限制）
  - 显示已练习次数（从 `app_user_paper_info` 查询）
  - 显示试卷状态（是否可用）

- [ ] **练习次数限制检查**
  - 如果 `practice_limit > 0` 且 `practice_count >= practice_limit`，禁用"开始答题"按钮
  - 显示提示信息："已达到练习次数限制（X/X）"

- [ ] **试卷详情查看**
  - 点击试卷可查看详细信息（弹窗或抽屉）
  - 显示试卷描述、考试时长、题目总数、总分等

---

## Phase 3: 试听和注意事项页面（新增）

### 3.1 试听页面（TrialListen.vue）
- [ ] **创建试听页面组件**
  - 路径：`exam-client/src/renderer/views/TrialListen.vue`
  - 路由：`/trial-listen`

- [ ] **试听内容显示**
  - 显示试听提示文案（`trial_listen_text`）
  - 显示试听图片示例（从 `question_media` 查询 `media_type=11`）
  - 播放试听音频示例（从 `question_media` 查询 `media_type=10`）
  - 提供"跳过试听"按钮（如果 `trial_listen_enabled=0` 则直接跳过）

- [ ] **试听音频播放控制**
  - 音频播放/暂停控制
  - 显示音频时长
  - 播放完成后自动进入下一步

- [ ] **页面导航**
  - "开始答题"按钮（试听完成后或跳过）
  - 跳转到注意事项页面或直接进入答题页面

### 3.2 注意事项页面（Notes.vue）
- [ ] **创建注意事项页面组件**
  - 路径：`exam-client/src/renderer/views/Notes.vue`
  - 路由：`/notes`

- [ ] **注意事项显示**
  - 显示注意事项内容（`notes`，支持富文本）
  - 根据 `notes_display_mode` 决定显示时机：
    - `before_exam`：考试前显示一次
    - `before_section`：每大题前显示

- [ ] **页面导航**
  - "我已了解，开始答题"按钮
  - 跳转到答题页面

---

## Phase 4: 答题页面核心功能（ExamPaper.vue）

### 4.1 多卷支持
- [ ] **卷别管理**
  - 从 `manifest.json` 读取 `volumes` 数组
  - 显示当前卷别（A卷、B卷、C卷等）
  - 支持卷别切换（根据 `volume_status` 判断是否可切换）
  - 播放卷别名称音频（从 `question_media` 查询 `media_type=7`，关联到 `paper_volume`）

- [ ] **卷别状态管理**
  - 初始化 `volume_status`（JSON格式：`{"A":"in_progress","B":"pending"}`）
  - 当前卷别提交后，更新状态为 `"completed"`
  - 检查下一卷是否可开始（上一卷必须 `"completed"`）

- [ ] **中场音频播放**
  - A卷提交成功后，检查是否有中场配置（`intermissions` 数组，`fromVolume="A"`, `toVolume="B"`）
  - 播放中场音频（从 `question_media` 查询 `media_type=9`，关联到 `paper_intermission`）
  - 显示中场提示文字（`intermission_text`）
  - 中场音频不可跳过（`canSkip=false`）
  - 记录中场播放状态（`intermission_played`：`{"A->B":true}`）
  - 中场音频播放完成后，自动进入B卷

### 4.2 大题（Section）支持
- [ ] **大题显示**
  - 从 `manifest.json` 读取 `sections` 数组
  - 按 `section_order` 排序显示大题
  - 显示大题名称（如"第一节"、"第二节"）
  - 显示大题说明（`instruction_text`）
  - 播放大题说明音频（从 `question_media` 查询 `media_type=8`，关联到 `paper_section`）
  - 显示大题题目数量和总分（`question_count`, `total_score`）

- [ ] **题目分组**
  - 根据 `section_id` 将题目分组到对应大题
  - 按 `section_order` 和 `section_order`（题目在大题内的顺序）排序

- [ ] **注意事项显示（每大题前）**
  - 如果 `notes_display_mode="before_section"`，每大题开始前显示注意事项
  - 显示后自动进入该大题的第一题

### 4.3 题目显示增强
- [ ] **题目音频支持**
  - 从 `questions.json` 的 `media` 数组读取题目音频（`media_type=4`）
  - 播放题目音频（听力题等）
  - 音频播放控制（播放/暂停/重播）

- [ ] **题目媒体显示**
  - 显示题目图片（`media_type=1`）
  - 显示题目视频（`media_type=1`，格式为视频）

- [ ] **选项音频支持**
  - 从 `questions.json` 的 `answers[].media` 数组读取选项音频（`media_type=2`）
  - 播放选项音频（点击选项时播放）

### 4.4 答题讲解功能
- [ ] **讲解显示逻辑**
  - 检查题目是否有讲解（`explanationEnabled=true`）
  - 播放完试卷名称音频后，等待 `explanationDelaySeconds` 秒（默认2秒）
  - 自动显示讲解文字（`explanationText`）
  - 播放讲解音频（从 `questions.json` 的 `media` 数组读取 `media_type=5`）
  - 显示讲解图片（从 `questions.json` 的 `media` 数组读取 `media_type=6`，如果有）

- [ ] **讲解显示时机**
  - 在题目显示后自动显示（不等待用户答题）
  - 提供"隐藏讲解"按钮（可选）

### 4.5 答题流程控制
- [ ] **光标锁定**
  - 当前题目高亮显示（图标闪烁动画）
  - 锁定到当前题目，不允许跳转到其他题目（除非允许回顾）

- [ ] **自动跳转**
  - 根据 `autoNextQuestion` 配置决定是否自动跳转
  - 如果启用，完成当前题后等待 `questionReadDuration` 秒，自动跳转到下一题

- [ ] **答题进度显示**
  - 显示当前进度（第X题/共Y题）
  - 显示当前大题进度（第X大题/共Y大题）
  - 显示当前卷别进度（A卷/B卷）

- [ ] **计时功能**
  - 显示已用时间（从 `start_time` 开始计时）
  - 显示剩余时间（如果有 `duration` 限制）
  - 时间到自动提交（如果配置了时间限制）

### 4.6 右侧信息栏
- [ ] **学员信息显示**
  - 显示学员姓名
  - 显示分配的机位号（`assigned_seat_number`）
  - 显示实际坐的机位号（`actual_seat_number`，暂时不处理变更）

- [ ] **音量调节**
  - 提供音量滑块
  - 保存音量设置到 `localStorage`

- [ ] **答题进度总览**
  - 显示各卷完成状态
  - 显示各大题完成状态

---

## Phase 5: 答题结果页面（ExamResult.vue）

### 5.1 结果统计
- [ ] **创建答题结果页面组件**
  - 路径：`exam-client/src/renderer/views/ExamResult.vue`
  - 路由：`/exam-result`

- [ ] **整体统计**
  - 显示总分和得分
  - 显示正确题数和错误题数
  - 显示用时
  - 显示各卷得分情况（如果有多卷）

- [ ] **练习次数更新**
  - 提交答题后，`practice_count` + 1
  - 更新 `last_practice_time`
  - 检查是否达到练习次数限制

### 5.2 错题查看
- [ ] **错题列表**
  - 显示所有答错的题目
  - 显示正确答案和用户答案
  - 显示题目解析

- [ ] **错题详情**
  - 点击错题查看详情
  - 显示题目内容、选项、正确答案、用户答案、解析

### 5.3 答题详情
- [ ] **答题记录查看**
  - 显示所有题目的答题记录
  - 显示用户选择的答案
  - 显示是否正确

### 5.4 结果提交
- [ ] **在线提交**
  - 如果在线，提交答题结果到服务端
  - 同步 `app_user_paper_info` 表数据
  - 同步 `app_user_paper_question_result` 表数据
  - 同步 `app_user_paper_question_blank_result` 表数据（如果有完形填空）

- [ ] **离线缓存**
  - 如果离线，保存答题结果到本地 SQLite
  - 标记为待同步状态
  - 下次在线时自动同步

---

## Phase 6: 试卷下载成功提示（PaperDownload.vue）

### 6.1 下载成功提示页面
- [ ] **创建下载成功提示页面组件**
  - 路径：`exam-client/src/renderer/views/PaperDownload.vue`
  - 路由：`/paper-download`

- [ ] **提示信息显示**
  - 显示"试卷下载成功"提示
  - 显示下载的试卷名称
  - 显示下载时间

- [ ] **页面导航**
  - "开始答题"按钮（跳转到试听页面或注意事项页面）
  - "返回"按钮（返回试卷选择页面）

---

## Phase 7: 路由和导航

### 7.1 路由配置
- [ ] **更新路由配置（router/index.js）**
  - 添加试听页面路由：`/trial-listen`
  - 添加注意事项页面路由：`/notes`
  - 添加答题结果页面路由：`/exam-result`
  - 添加试卷下载成功页面路由：`/paper-download`

### 7.2 页面流程
- [ ] **完整答题流程**
  1. 试卷选择页面（`/paper-select`）
  2. 试卷下载成功页面（`/paper-download`）- 如果刚下载
  3. 试听页面（`/trial-listen`）- 如果 `trial_listen_enabled=1`
  4. 注意事项页面（`/notes`）- 如果 `notes_display_mode="before_exam"` 且有 `notes`
  5. 答题页面（`/exam`）
     - 如果 `notes_display_mode="before_section"`，每大题前显示注意事项
     - A卷完成后播放中场音频（如果有）
     - B卷完成后进入结果页面
  6. 答题结果页面（`/exam-result`）

---

## Phase 8: IPC 通信增强

### 8.1 新增 IPC 处理器
- [ ] **卷别相关**
  - `paper:getVolumes` - 根据试卷ID查询卷别列表
  - `paper:getVolumeMedia` - 根据卷别ID查询媒体文件

- [ ] **大题相关**
  - `paper:getSections` - 根据试卷ID查询大题列表
  - `paper:getSectionMedia` - 根据大题ID查询媒体文件
  - `paper:getSectionQuestions` - 根据大题ID查询题目列表

- [ ] **中场相关**
  - `paper:getIntermissions` - 根据试卷ID查询中场配置列表
  - `paper:getIntermissionMedia` - 根据中场配置ID查询媒体文件

- [ ] **答题相关**
  - `answer:startExam` - 开始答题（创建 `app_user_paper_info` 记录）
  - `answer:submitVolume` - 提交卷别答题结果
  - `answer:submitExam` - 提交整个试卷答题结果（所有卷都完成后）
  - `answer:getPracticeCount` - 查询练习次数
  - `answer:checkPracticeLimit` - 检查练习次数限制

- [ ] **媒体文件相关**
  - `media:getByPaperId` - 根据试卷ID查询媒体文件（支持新的 media_type）
  - `media:getByVolumeId` - 根据卷别ID查询媒体文件
  - `media:getBySectionId` - 根据大题ID查询媒体文件
  - `media:getByIntermissionId` - 根据中场配置ID查询媒体文件
  - `media:getByQuestionIdAndType` - 根据题目ID和媒体类型查询（支持 media_type 4-6）

---

## Phase 9: 数据流和状态管理

### 9.1 答题状态管理
- [ ] **状态数据结构**
  ```javascript
  {
    paperId: 1,
    currentVolume: 'A', // 当前卷别
    volumeStatus: { A: 'completed', B: 'in_progress' }, // 各卷状态
    currentSection: 1, // 当前大题ID
    currentQuestion: 1, // 当前题目ID
    answers: {}, // 答题记录 { questionId: { answerId: 1, blankAnswers: {} } }
    startTime: Date, // 开始时间
    intermissionPlayed: { 'A->B': true } // 中场播放状态
  }
  ```

- [ ] **状态持久化**
  - 保存答题状态到 `localStorage`（临时）
  - 保存答题记录到 SQLite（持久化）
  - 支持断点续答（刷新页面后恢复状态）

### 9.2 媒体文件管理
- [ ] **媒体文件路径解析**
  - 从 ZIP 包中提取的媒体文件路径
  - 转换为本地文件系统路径
  - 支持在线和离线两种模式

- [ ] **媒体文件缓存**
  - 缓存已播放的音频文件
  - 预加载下一题的媒体文件

---

## Phase 10: 用户体验优化

### 10.1 加载和错误处理
- [ ] **加载状态**
  - 试卷加载中显示加载动画
  - 媒体文件加载中显示加载提示

- [ ] **错误处理**
  - 试卷加载失败提示
  - 媒体文件加载失败提示
  - 网络错误提示（在线模式）

### 10.2 交互优化
- [ ] **键盘快捷键**
  - 支持键盘操作（方向键切换题目等）

- [ ] **触摸支持**
  - 支持触摸操作（移动端适配）

- [ ] **无障碍支持**
  - 支持屏幕阅读器
  - 支持键盘导航

---

## 优先级说明

### 高优先级（必须实现）
1. Phase 1: 数据同步和存储增强
2. Phase 4: 答题页面核心功能（多卷、大题、题目显示）
3. Phase 5: 答题结果页面
4. Phase 8: IPC 通信增强

### 中优先级（重要功能）
1. Phase 2: 试卷选择页面增强
2. Phase 3: 试听和注意事项页面
3. Phase 6: 试卷下载成功提示
4. Phase 7: 路由和导航

### 低优先级（优化功能）
1. Phase 9: 数据流和状态管理（部分功能）
2. Phase 10: 用户体验优化

---

## 技术要点

### 1. 媒体文件路径处理
- ZIP 包中的相对路径需要转换为本地绝对路径
- 支持在线模式（从 OSS 加载）和离线模式（从本地文件系统加载）

### 2. JSON 数据解析
- `manifest.json` 和 `questions.json` 使用新的结构
- 需要兼容旧格式（向后兼容）

### 3. 状态管理
- 使用 Vuex 或简单的状态管理
- 答题状态需要持久化到 SQLite

### 4. 音频播放
- 使用 HTML5 Audio API
- 支持多个音频同时播放（如题目音频和选项音频）
- 音频播放完成后触发回调

### 5. 定时器管理
- 答题计时器
- 自动跳转定时器
- 讲解显示延迟定时器
- 需要正确清理定时器（避免内存泄漏）

---

## 测试要点

### 1. 功能测试
- [ ] 多卷答题流程（A卷 -> 中场 -> B卷）
- [ ] 大题显示和切换
- [ ] 题目音频播放
- [ ] 选项音频播放
- [ ] 答题讲解显示
- [ ] 练习次数限制
- [ ] 试听功能
- [ ] 注意事项显示

### 2. 边界测试
- [ ] 单卷试卷（没有多卷）
- [ ] 没有大题的试卷
- [ ] 没有中场的多卷试卷
- [ ] 练习次数为0（不限制）
- [ ] 离线模式答题
- [ ] 网络中断恢复

### 3. 性能测试
- [ ] 大量题目加载性能
- [ ] 媒体文件加载性能
- [ ] 答题状态保存性能

---

## 注意事项

1. **向后兼容**：需要兼容旧格式的试卷包（没有 volumes、sections 等）
2. **数据迁移**：现有答题记录需要迁移到新结构
3. **错误处理**：所有异步操作都需要错误处理
4. **用户体验**：加载过程要有明确的提示，避免用户等待
5. **数据安全**：答题结果需要加密存储（如果需要）

---

## 预计工作量

- **Phase 1**: 2-3天
- **Phase 2**: 0.5天
- **Phase 3**: 1-2天
- **Phase 4**: 5-7天（最复杂）
- **Phase 5**: 2-3天
- **Phase 6**: 0.5天
- **Phase 7**: 0.5天
- **Phase 8**: 1-2天
- **Phase 9**: 1-2天
- **Phase 10**: 2-3天

**总计**: 15-25天

---

## 更新日志

- 2024-XX-XX: 初始版本创建



