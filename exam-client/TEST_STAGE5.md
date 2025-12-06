# 阶段五：客户端SQLite初始化优化 - 测试指南

## 一、测试环境准备

### 1.1 启动开发环境

```bash
cd exam-client
npm install  # 如果还没安装依赖
npm run dev  # 启动开发模式
```

### 1.2 查看日志输出

启动后，在终端中会看到以下日志：

```
=== Electron App Starting ===
isDev: true
=== SQLite 数据库信息 ===
数据库文件路径: /Users/Stephen/Library/Application Support/exam-client/exam.db
数据库文件是否存在: false
开始初始化数据库表结构...
```

---

## 二、测试场景

### 场景1：首次启动（所有表都不存在）

**测试步骤**：
1. 删除数据库文件（如果存在）
2. 启动应用
3. 观察日志输出

**预期结果**：

```
开始初始化数据库表结构...
需要创建 10 个表
创建关键表: student_credentials, student_papers, dict_data, student_archive
✓ 关键表创建成功: student_credentials, student_papers, dict_data, student_archive
✓ 关键表初始化完成（耗时 XXms）
[异步] 创建试卷相关表 (重试 0/3): paper_package, paper_index, app_user_paper_info, app_user_paper_question_result, app_user_paper_question_blank_result, question_media_index
✓ [异步] 试卷相关表创建成功（耗时 XXms）
```

**验证方法**：
- 检查日志中是否显示"关键表初始化完成"
- 检查日志中是否显示"试卷相关表创建成功"
- 检查耗时是否 < 100ms（关键表部分）

---

### 场景2：后续启动（所有表已存在）

**测试步骤**：
1. 首次启动后，关闭应用
2. 再次启动应用
3. 观察日志输出

**预期结果**：

```
开始初始化数据库表结构...
✓ 所有表已存在，跳过初始化（耗时 <10ms）
```

**验证方法**：
- 检查日志中是否显示"所有表已存在，跳过初始化"
- 检查耗时是否 < 10ms

---

### 场景3：按需初始化（没有ZIP包时延迟创建）

**测试步骤**：
1. 删除数据库文件
2. 确保 `paper_packages` 目录不存在或为空
3. 启动应用
4. 观察日志输出

**预期结果**：

```
开始初始化数据库表结构...
需要创建 10 个表
创建关键表: student_credentials, student_papers, dict_data, student_archive
✓ 关键表初始化完成（耗时 XXms）
⚠️ 未检测到ZIP包，延迟初始化试卷相关表（按需创建）
```

**验证方法**：
- 检查日志中是否显示"未检测到ZIP包，延迟初始化试卷相关表"
- 检查试卷相关表是否未创建（通过DataGrip查看）

---

### 场景4：按需初始化（有ZIP包时立即创建）

**测试步骤**：
1. 删除数据库文件
2. 创建 `paper_packages` 目录并放入一个ZIP文件
3. 启动应用
4. 观察日志输出

**预期结果**：

```
开始初始化数据库表结构...
需要创建 10 个表
创建关键表: student_credentials, student_papers, dict_data, student_archive
✓ 关键表初始化完成（耗时 XXms）
[异步] 创建试卷相关表 (重试 0/3): paper_package, paper_index, ...
✓ [异步] 试卷相关表创建成功（耗时 XXms）
```

**验证方法**：
- 检查日志中是否显示"试卷相关表创建成功"
- 检查所有表是否都已创建

---

### 场景5：重试机制（模拟失败场景）

**注意**：这个场景需要手动修改代码来模拟失败，或者等待真实的失败场景。

**测试步骤**：
1. 在 `initPaperTables()` 方法中临时添加 `throw new Error('模拟失败')`
2. 启动应用
3. 观察日志输出

**预期结果**：

```
[异步] 创建试卷相关表 (重试 0/3): paper_package, ...
[异步] 试卷相关表创建失败 (重试 0/3): 模拟失败
[异步] 创建试卷相关表 (重试 1/3): paper_package, ...
[异步] 试卷相关表创建失败 (重试 1/3): 模拟失败
[异步] 创建试卷相关表 (重试 2/3): paper_package, ...
[异步] 试卷相关表创建失败 (重试 2/3): 模拟失败
⚠️ 试卷相关表创建失败 3 次，执行全量覆盖...
执行全量覆盖：删除并重新创建试卷相关表...
  ✓ 删除表: paper_package
  ...
✓ 全量覆盖完成：试卷相关表已重新创建
```

**验证方法**：
- 检查日志中是否显示重试过程
- 检查是否在第3次失败后执行全量覆盖

---

## 三、数据库验证

### 3.1 使用 DataGrip 连接数据库

**数据库路径**：
- **macOS**: `~/Library/Application Support/exam-client/exam.db`
- **Windows**: `C:\Users\{用户名}\AppData\Roaming\exam-client\exam.db`
- **Linux**: `~/.config/exam-client/exam.db`

**连接步骤**：
1. 打开 DataGrip
2. 添加数据源 → SQLite
3. 选择数据库文件路径
4. 测试连接

### 3.2 验证表结构

**关键表（必须存在）**：
```sql
-- 检查关键表
SELECT name FROM sqlite_master 
WHERE type='table' AND name IN (
  'student_credentials',
  'student_papers',
  'dict_data',
  'student_archive'
);
```

**试卷相关表（按需创建）**：
```sql
-- 检查试卷相关表
SELECT name FROM sqlite_master 
WHERE type='table' AND name IN (
  'paper_package',
  'paper_index',
  'app_user_paper_info',
  'app_user_paper_question_result',
  'app_user_paper_question_blank_result',
  'question_media_index'
);
```

### 3.3 验证索引

```sql
-- 检查所有索引
SELECT name, tbl_name FROM sqlite_master 
WHERE type='index' AND name LIKE 'idx_%'
ORDER BY tbl_name, name;
```

**预期索引**：
- `idx_student_account` (student_credentials)
- `idx_student_papers_account` (student_papers)
- `idx_dict_type` (dict_data)
- `idx_student_archive_account` (student_archive)
- `idx_student_archive_user_id` (student_archive)
- `idx_paper_package_code` (paper_package)
- `idx_paper_index_code` (paper_index)
- `idx_paper_info_user` (app_user_paper_info)
- `idx_paper_info_sync` (app_user_paper_info)
- `idx_paper_result_user` (app_user_paper_question_result)
- `idx_paper_result_sync` (app_user_paper_question_result)
- `idx_paper_blank_sync` (app_user_paper_question_blank_result)
- `idx_paper_question` (question_media_index)

### 3.4 验证表结构字段

**验证 paper_package 表**：
```sql
PRAGMA table_info(paper_package);
```

**预期字段**：
- `id`, `paper_id`, `paper_code`, `package_data`, `package_path`, `package_hash`, `package_size`, `storage_type`, `version`, `sync_time`, `is_active`

**验证 app_user_paper_info 表（新字段）**：
```sql
PRAGMA table_info(app_user_paper_info);
```

**预期新字段**：
- `start_time`, `submit_time`, `used_time`, `total_score`, `user_score`, `correct_count`, `wrong_count`, `is_submit`, `sync_status`, `sync_time`

---

## 四、性能测试

### 4.1 测试启动时间

**测试方法**：
1. 使用 `console.time()` 和 `console.timeEnd()` 记录时间
2. 或者查看日志中的耗时信息

**性能目标**：
- **首次启动**：关键表初始化 < 100ms
- **后续启动**：< 10ms（只检查表是否存在）

### 4.2 测试异步初始化

**测试方法**：
1. 启动应用后，立即检查试卷相关表是否存在
2. 等待几秒后再次检查

**预期结果**：
- 启动时可能表还未创建（异步）
- 几秒后表应该已创建完成

---

## 五、测试 ensurePaperTablesExist 方法

### 5.1 测试按需创建表

**测试步骤**：
1. 删除试卷相关表（保留关键表）
2. 在代码中调用 `db.ensurePaperTablesExist()`
3. 检查表是否创建成功

**测试代码**（可以在开发者工具中执行）：
```javascript
// 在主进程中
const db = require('./src/database/db')
const database = new db()
database.ensurePaperTablesExist()
```

**预期结果**：
```
检测到缺失的试卷相关表，立即创建: paper_package, paper_index, ...
✓ 试卷相关表创建成功
```

---

## 六、常见问题排查

### 6.1 表未创建

**可能原因**：
- 异步初始化还未完成
- 按需初始化被延迟（没有ZIP包）
- 初始化失败但未抛出异常

**解决方法**：
1. 检查日志中是否有错误信息
2. 等待几秒后再次检查
3. 手动调用 `ensurePaperTablesExist()` 方法

### 6.2 性能不达标

**可能原因**：
- 表数量过多
- 表结构复杂
- 磁盘IO慢

**解决方法**：
1. 检查日志中的耗时信息
2. 确认是否使用了快速检查机制
3. 确认关键表是否同步创建，非关键表是否异步创建

### 6.3 重试机制未触发

**可能原因**：
- 初始化成功，无需重试
- 错误被捕获但未记录

**解决方法**：
1. 手动模拟失败场景
2. 检查日志中是否有重试记录

---

## 七、测试检查清单

- [ ] 首次启动：所有表创建成功
- [ ] 首次启动：关键表同步创建，耗时 < 100ms
- [ ] 首次启动：非关键表异步创建
- [ ] 后续启动：快速检查，耗时 < 10ms
- [ ] 按需初始化：没有ZIP包时延迟创建
- [ ] 按需初始化：有ZIP包时立即创建
- [ ] 重试机制：失败后自动重试（最多3次）
- [ ] 全量覆盖：3次失败后自动修复
- [ ] 所有索引创建成功
- [ ] 表结构字段正确（特别是新字段）
- [ ] `ensurePaperTablesExist()` 方法正常工作

---

## 八、测试报告模板

```
测试日期：YYYY-MM-DD
测试人员：XXX
测试环境：macOS/Windows/Linux

测试结果：
1. 首次启动：✓ 通过 / ✗ 失败
   - 关键表创建：耗时 XXms
   - 非关键表创建：耗时 XXms
   
2. 后续启动：✓ 通过 / ✗ 失败
   - 检查耗时：XXms
   
3. 按需初始化：✓ 通过 / ✗ 失败
   - 无ZIP包场景：✓ / ✗
   - 有ZIP包场景：✓ / ✗
   
4. 数据库验证：✓ 通过 / ✗ 失败
   - 表数量：XX 个
   - 索引数量：XX 个
   
5. 性能测试：✓ 通过 / ✗ 失败
   - 首次启动：XXms
   - 后续启动：XXms
```

---

## 九、快速测试命令

### 9.1 查看数据库文件位置

```bash
# macOS
ls -la ~/Library/Application\ Support/exam-client/exam.db

# Windows (PowerShell)
Test-Path "$env:APPDATA\exam-client\exam.db"

# Linux
ls -la ~/.config/exam-client/exam.db
```

### 9.2 删除数据库文件（重新测试）

```bash
# macOS
rm ~/Library/Application\ Support/exam-client/exam.db

# Windows (PowerShell)
Remove-Item "$env:APPDATA\exam-client\exam.db"

# Linux
rm ~/.config/exam-client/exam.db
```

### 9.3 使用 SQLite 命令行工具验证

```bash
# macOS (需要安装 sqlite3)
sqlite3 ~/Library/Application\ Support/exam-client/exam.db ".tables"

# 查看表结构
sqlite3 ~/Library/Application\ Support/exam-client/exam.db ".schema paper_package"
```

---

## 十、下一步

测试通过后，可以继续：
1. 阶段六：客户端试卷包同步功能
2. 阶段七：客户端答题功能实现


