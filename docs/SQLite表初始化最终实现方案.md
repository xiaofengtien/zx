# SQLite表初始化最终实现方案

## 一、方案确认

### 1.1 已确认的配置

✅ **关键表**（同步初始化）：
- `student_credentials` - 登录必需
- `student_archive` - 学员信息必需
- `dict_data` - 字典数据必需

✅ **非关键表**（异步初始化）：
- `paper_package` - 试卷包
- `paper_index` - 试卷索引
- `app_user_paper_info` - 答题结果
- `app_user_paper_question_result` - 题目结果
- `app_user_paper_question_blank_result` - 完形填空结果

✅ **重试机制**：
- 异步初始化失败时重试
- 失败3次后全量覆盖表结构

✅ **性能目标**：
- 启动时间：<100ms（可接受）
- 最好不超过：500ms

---

## 二、最终实现代码

### 2.1 完整实现（带重试机制）

```javascript
// exam-client/src/database/db.js

const Database = require('better-sqlite3')
const path = require('path')
const fs = require('fs')
const { app } = require('electron')

class DB {
  constructor() {
    // 获取应用数据目录
    const userDataPath = app.getPath('userData')
    
    // 确保目录存在
    if (!fs.existsSync(userDataPath)) {
      fs.mkdirSync(userDataPath, { recursive: true })
    }
    
    // 数据库文件路径
    this.dbPath = path.join(userDataPath, 'exam.db')
    
    // 输出数据库文件路径（用于调试和 DataGrip 连接）
    console.log('=== SQLite 数据库信息 ===')
    console.log('数据库文件路径:', this.dbPath)
    console.log('数据库文件是否存在:', fs.existsSync(this.dbPath))
    
    // 打开数据库
    this.db = new Database(this.dbPath)
    
    // 初始化表结构（优化：快速检查 + 异步初始化）
    this.initTables()
  }

  /**
   * 初始化表结构（主方法）
   * 性能优化：快速检查 + 关键表同步 + 非关键表异步
   */
  initTables() {
    console.log('开始初始化数据库表结构...')
    const startTime = Date.now()
    
    // 1. 快速检查：所有表是否都已存在
    const existingTables = this.getExistingTables()
    const allRequiredTables = [
      // 关键表
      'student_credentials',
      'student_papers',
      'dict_data',
      'student_archive',
      // 试卷相关表（非关键）
      'paper_package',
      'paper_index',
      'app_user_paper_info',
      'app_user_paper_question_result',
      'app_user_paper_question_blank_result'
    ]
    
    const missingTables = allRequiredTables.filter(t => !existingTables.includes(t))
    
    if (missingTables.length === 0) {
      const elapsed = Date.now() - startTime
      console.log(`✓ 所有表已存在，跳过初始化（耗时 ${elapsed}ms）`)
      return // 快速返回，<10ms
    }
    
    console.log(`需要创建 ${missingTables.length} 个表`)
    
    // 2. 同步初始化关键表（必须立即可用）
    this.initCriticalTables(missingTables)
    
    const criticalElapsed = Date.now() - startTime
    console.log(`✓ 关键表初始化完成（耗时 ${criticalElapsed}ms）`)
    
    // 3. 异步初始化非关键表（可以延迟，带重试机制）
    setImmediate(() => {
      this.initPaperTablesAsync(missingTables, 0) // 从第0次重试开始
    })
  }

  /**
   * 获取已存在的表列表
   */
  getExistingTables() {
    try {
      return this.db.prepare(`
        SELECT name FROM sqlite_master 
        WHERE type='table' AND name NOT LIKE 'sqlite_%'
      `).all().map(t => t.name)
    } catch (error) {
      console.error('获取表列表失败:', error)
      return []
    }
  }

  /**
   * 初始化关键表（同步，必须立即可用）
   */
  initCriticalTables(missingTables) {
    const criticalTables = ['student_credentials', 'student_papers', 'dict_data', 'student_archive']
    const toCreate = criticalTables.filter(t => missingTables.includes(t))
    
    if (toCreate.length === 0) {
      return
    }
    
    console.log(`创建关键表: ${toCreate.join(', ')}`)
    
    try {
      // 创建学员凭证表
      if (toCreate.includes('student_credentials')) {
        this.db.exec(`
          CREATE TABLE IF NOT EXISTS student_credentials (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            student_account TEXT UNIQUE NOT NULL,
            password_hash TEXT NOT NULL,
            offline_credential TEXT,
            archive_id INTEGER,
            user_id INTEGER,
            create_time INTEGER,
            expire_time INTEGER,
            last_login_time INTEGER
          )
        `)
      }
      
      // 创建学员考卷类型表
      if (toCreate.includes('student_papers')) {
        this.db.exec(`
          CREATE TABLE IF NOT EXISTS student_papers (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            student_account TEXT NOT NULL,
            paper_types TEXT NOT NULL,
            update_time INTEGER,
            FOREIGN KEY (student_account) REFERENCES student_credentials(student_account)
          )
        `)
      }
      
      // 创建字典数据表
      if (toCreate.includes('dict_data')) {
        this.db.exec(`
          CREATE TABLE IF NOT EXISTS dict_data (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            dict_type TEXT NOT NULL,
            dict_value TEXT NOT NULL,
            dict_label TEXT NOT NULL,
            dict_sort INTEGER DEFAULT 0,
            css_class TEXT,
            list_class TEXT,
            is_default TEXT DEFAULT 'N',
            status TEXT DEFAULT '0',
            create_time INTEGER,
            update_time INTEGER,
            remark TEXT,
            UNIQUE(dict_type, dict_value)
          )
        `)
      }
      
      // 创建学员档案表
      if (toCreate.includes('student_archive')) {
        this.db.exec(`
          CREATE TABLE IF NOT EXISTS student_archive (
            id INTEGER PRIMARY KEY,
            user_id INTEGER,
            student_account TEXT NOT NULL,
            password TEXT,
            phone_number TEXT,
            sex TEXT,
            grade TEXT,
            current_grade TEXT,
            hometown TEXT,
            applicable_papers TEXT DEFAULT '[]',
            status TEXT DEFAULT '0',
            del_flag TEXT DEFAULT '0',
            create_time INTEGER,
            update_time INTEGER,
            remark TEXT,
            UNIQUE(student_account)
          )
        `)
      }
      
      // 创建关键表索引
      this.createCriticalIndexes()
      
      console.log(`✓ 关键表创建成功: ${toCreate.join(', ')}`)
    } catch (error) {
      console.error('创建关键表失败:', error)
      throw error // 关键表创建失败，必须抛出异常
    }
  }

  /**
   * 异步初始化试卷相关表（带重试机制）
   * @param {Array} missingTables - 需要创建的表列表
   * @param {Number} retryCount - 当前重试次数
   */
  initPaperTablesAsync(missingTables, retryCount = 0) {
    const MAX_RETRIES = 3
    const paperTables = [
      'paper_package',
      'paper_index',
      'app_user_paper_info',
      'app_user_paper_question_result',
      'app_user_paper_question_blank_result'
    ]
    
    const toCreate = paperTables.filter(t => missingTables.includes(t))
    
    if (toCreate.length === 0) {
      console.log('✓ 试卷相关表已存在，无需创建')
      return
    }
    
    console.log(`[异步] 创建试卷相关表 (重试 ${retryCount}/${MAX_RETRIES}): ${toCreate.join(', ')}`)
    const startTime = Date.now()
    
    try {
      // 如果重试3次后仍然失败，全量覆盖表结构
      if (retryCount >= MAX_RETRIES) {
        console.warn(`⚠️ 试卷相关表创建失败 ${MAX_RETRIES} 次，执行全量覆盖...`)
        this.recreatePaperTables(toCreate)
        return
      }
      
      // 正常创建表
      this.initPaperTables(toCreate)
      
      // 创建索引
      this.createPaperIndexes()
      
      const elapsed = Date.now() - startTime
      console.log(`✓ [异步] 试卷相关表创建成功（耗时 ${elapsed}ms）`)
      
    } catch (error) {
      console.error(`[异步] 试卷相关表创建失败 (重试 ${retryCount}/${MAX_RETRIES}):`, error.message)
      
      // 重试机制：延迟后重试
      const retryDelay = (retryCount + 1) * 1000 // 1秒、2秒、3秒
      setTimeout(() => {
        this.initPaperTablesAsync(missingTables, retryCount + 1)
      }, retryDelay)
    }
  }

  /**
   * 初始化试卷相关表（具体实现）
   */
  initPaperTables(toCreate) {
    // 试卷包表
    if (toCreate.includes('paper_package')) {
      this.db.exec(`
        CREATE TABLE IF NOT EXISTS paper_package (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          paper_id INTEGER NOT NULL UNIQUE,
          paper_code TEXT NOT NULL UNIQUE,
          package_data BLOB,
          package_path TEXT,
          package_hash TEXT NOT NULL,
          package_size INTEGER NOT NULL,
          storage_type INTEGER DEFAULT 0,
          version INTEGER DEFAULT 1,
          sync_time INTEGER NOT NULL,
          is_active INTEGER DEFAULT 1
        )
      `)
    }
    
    // 试卷索引表
    if (toCreate.includes('paper_index')) {
      this.db.exec(`
        CREATE TABLE IF NOT EXISTS paper_index (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          paper_id INTEGER NOT NULL UNIQUE,
          paper_code TEXT NOT NULL,
          paper_name TEXT NOT NULL,
          total_questions INTEGER DEFAULT 0,
          total_score REAL DEFAULT 0,
          duration INTEGER,
          intro_audio_url TEXT,
          intro_audio_duration INTEGER,
          version INTEGER DEFAULT 1,
          package_hash TEXT,
          sync_time INTEGER,
          is_active INTEGER DEFAULT 1
        )
      `)
    }
    
    // 用户试卷信息表
    if (toCreate.includes('app_user_paper_info')) {
      this.db.exec(`
        CREATE TABLE IF NOT EXISTS app_user_paper_info (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          paper_id INTEGER NOT NULL,
          paper_name TEXT,
          app_user_id INTEGER NOT NULL,
          start_time INTEGER,
          submit_time INTEGER,
          used_time INTEGER,
          total_score REAL DEFAULT 0,
          user_score REAL DEFAULT 0,
          correct_count INTEGER DEFAULT 0,
          wrong_count INTEGER DEFAULT 0,
          is_submit INTEGER DEFAULT 0,
          sync_status INTEGER DEFAULT 0,
          sync_time INTEGER,
          create_time INTEGER,
          update_time INTEGER
        )
      `)
    }
    
    // 用户题目结果表
    if (toCreate.includes('app_user_paper_question_result')) {
      this.db.exec(`
        CREATE TABLE IF NOT EXISTS app_user_paper_question_result (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          paper_id INTEGER NOT NULL,
          app_user_id INTEGER NOT NULL,
          question_id INTEGER NOT NULL,
          answer_ids TEXT,
          user_answer TEXT,
          result INTEGER,
          question_sort INTEGER,
          answer_time INTEGER,
          time_spent INTEGER,
          is_reviewed INTEGER DEFAULT 0,
          sync_status INTEGER DEFAULT 0,
          create_time INTEGER
        )
      `)
    }
    
    // 用户完形填空结果表
    if (toCreate.includes('app_user_paper_question_blank_result')) {
      this.db.exec(`
        CREATE TABLE IF NOT EXISTS app_user_paper_question_blank_result (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          paper_id INTEGER NOT NULL,
          app_user_id INTEGER NOT NULL,
          question_id INTEGER NOT NULL,
          blank_area_id INTEGER,
          blank_index INTEGER,
          answer_ids TEXT,
          result INTEGER,
          answer_time INTEGER,
          time_spent INTEGER,
          sync_status INTEGER DEFAULT 0,
          create_time INTEGER
        )
      `)
    }
  }

  /**
   * 全量覆盖表结构（失败3次后执行）
   * 先删除表，再重新创建
   */
  recreatePaperTables(toCreate) {
    console.log('执行全量覆盖：删除并重新创建试卷相关表...')
    
    try {
      // 1. 删除现有表（如果存在）
      for (const tableName of toCreate) {
        try {
          this.db.exec(`DROP TABLE IF EXISTS ${tableName}`)
          console.log(`  ✓ 删除表: ${tableName}`)
        } catch (error) {
          console.warn(`  删除表 ${tableName} 失败:`, error.message)
        }
      }
      
      // 2. 删除相关索引
      const indexes = [
        'idx_paper_package_code',
        'idx_paper_index_code',
        'idx_paper_info_user',
        'idx_paper_info_sync',
        'idx_paper_result_user',
        'idx_paper_result_sync',
        'idx_paper_blank_sync'
      ]
      
      for (const indexName of indexes) {
        try {
          this.db.exec(`DROP INDEX IF EXISTS ${indexName}`)
        } catch (error) {
          // 索引可能不存在，忽略错误
        }
      }
      
      // 3. 重新创建表
      this.initPaperTables(toCreate)
      
      // 4. 重新创建索引
      this.createPaperIndexes()
      
      console.log('✓ 全量覆盖完成：试卷相关表已重新创建')
      
    } catch (error) {
      console.error('✗ 全量覆盖失败:', error)
      // 即使全量覆盖失败，也不抛出异常（避免影响应用启动）
      // 可以在后续使用时再次尝试创建
    }
  }

  /**
   * 创建关键表索引（同步）
   */
  createCriticalIndexes() {
    this.db.exec(`CREATE INDEX IF NOT EXISTS idx_student_account ON student_credentials(student_account)`)
    this.db.exec(`CREATE INDEX IF NOT EXISTS idx_student_papers_account ON student_papers(student_account)`)
    this.db.exec(`CREATE INDEX IF NOT EXISTS idx_dict_type ON dict_data(dict_type)`)
    this.db.exec(`CREATE INDEX IF NOT EXISTS idx_student_archive_account ON student_archive(student_account)`)
    this.db.exec(`CREATE INDEX IF NOT EXISTS idx_student_archive_user_id ON student_archive(user_id)`)
  }

  /**
   * 创建试卷相关表索引（异步）
   */
  createPaperIndexes() {
    this.db.exec(`CREATE INDEX IF NOT EXISTS idx_paper_package_code ON paper_package(paper_code)`)
    this.db.exec(`CREATE INDEX IF NOT EXISTS idx_paper_index_code ON paper_index(paper_code)`)
    this.db.exec(`CREATE INDEX IF NOT EXISTS idx_paper_info_user ON app_user_paper_info(app_user_id)`)
    this.db.exec(`CREATE INDEX IF NOT EXISTS idx_paper_info_sync ON app_user_paper_info(sync_status)`)
    this.db.exec(`CREATE INDEX IF NOT EXISTS idx_paper_result_user ON app_user_paper_question_result(paper_id, app_user_id)`)
    this.db.exec(`CREATE INDEX IF NOT EXISTS idx_paper_result_sync ON app_user_paper_question_result(sync_status)`)
    this.db.exec(`CREATE INDEX IF NOT EXISTS idx_paper_blank_sync ON app_user_paper_question_blank_result(sync_status)`)
  }

  /**
   * 检查表是否存在（用于首次使用前检查）
   * 如果表不存在，尝试创建
   */
  ensurePaperTablesExist() {
    const paperTables = [
      'paper_package',
      'paper_index',
      'app_user_paper_info',
      'app_user_paper_question_result',
      'app_user_paper_question_blank_result'
    ]
    
    const existingTables = this.getExistingTables()
    const missingTables = paperTables.filter(t => !existingTables.includes(t))
    
    if (missingTables.length > 0) {
      console.log(`检测到缺失的试卷相关表，立即创建: ${missingTables.join(', ')}`)
      try {
        this.initPaperTables(missingTables)
        this.createPaperIndexes()
        console.log('✓ 试卷相关表创建成功')
      } catch (error) {
        console.error('创建试卷相关表失败:', error)
        throw error
      }
    }
  }

  getDB() {
    return this.db
  }

  close() {
    if (this.db) {
      this.db.close()
    }
  }
}

module.exports = DB
```

---

## 三、性能分析

### 3.1 启动时间预估

| 场景 | 操作 | 预估时间 | 说明 |
|------|------|---------|------|
| **后续启动** | 检查表是否存在 | <10ms | 快速返回 |
| **首次启动** | 创建关键表（4个） | 20-50ms | 同步执行 |
| **首次启动** | 创建非关键表（5个） | 30-80ms | 异步执行（不阻塞启动） |
| **总计（首次）** | - | **20-50ms** | 只计算同步部分 |
| **总计（后续）** | - | **<10ms** | 只检查 |

**结论**：启动时间完全满足 <100ms 的要求，甚至远低于目标。

### 3.2 重试机制说明

**重试策略**：
- 第1次失败：延迟1秒后重试
- 第2次失败：延迟2秒后重试
- 第3次失败：延迟3秒后重试
- 第4次失败：执行全量覆盖（删除表后重新创建）

**全量覆盖逻辑**：
1. 删除现有表（如果存在）
2. 删除相关索引
3. 重新创建表
4. 重新创建索引

---

## 四、使用示例

### 4.1 首次使用前检查（可选）

如果担心异步初始化未完成，可以在首次使用试卷功能前检查：

```javascript
// 在首次使用试卷功能前
try {
  db.ensurePaperTablesExist()
} catch (error) {
  console.error('试卷相关表初始化失败:', error)
  // 可以提示用户或重试
}
```

### 4.2 日志输出示例

**首次启动**：
```
开始初始化数据库表结构...
需要创建 9 个表
创建关键表: student_credentials, student_papers, dict_data, student_archive
✓ 关键表创建成功: student_credentials, student_papers, dict_data, student_archive
✓ 关键表初始化完成（耗时 35ms）
[异步] 创建试卷相关表 (重试 0/3): paper_package, paper_index, ...
✓ [异步] 试卷相关表创建成功（耗时 45ms）
```

**后续启动**：
```
开始初始化数据库表结构...
✓ 所有表已存在，跳过初始化（耗时 8ms）
```

**异步初始化失败（重试）**：
```
[异步] 创建试卷相关表 (重试 0/3): paper_package, ...
[异步] 试卷相关表创建失败 (重试 0/3): SQLITE_BUSY: database is locked
[异步] 创建试卷相关表 (重试 1/3): paper_package, ...
[异步] 试卷相关表创建失败 (重试 1/3): SQLITE_BUSY: database is locked
[异步] 创建试卷相关表 (重试 2/3): paper_package, ...
[异步] 试卷相关表创建失败 (重试 2/3): SQLITE_BUSY: database is locked
⚠️ 试卷相关表创建失败 3 次，执行全量覆盖...
执行全量覆盖：删除并重新创建试卷相关表...
  ✓ 删除表: paper_package
  ✓ 删除表: paper_index
  ...
✓ 全量覆盖完成：试卷相关表已重新创建
```

---

## 五、方案确认

### 5.1 已确认的配置

✅ **关键表**：`student_credentials`、`student_archive`、`dict_data`  
✅ **非关键表**：试卷相关表（异步初始化）  
✅ **重试机制**：失败3次后全量覆盖  
✅ **性能目标**：<100ms（可接受），最好不超过500ms  

### 5.2 性能保证

- ✅ **后续启动**：<10ms（只检查表是否存在）
- ✅ **首次启动**：20-50ms（只计算关键表，非关键表异步）
- ✅ **完全满足**：<100ms 的目标要求

### 5.3 容错机制

- ✅ **重试机制**：失败后自动重试（最多3次）
- ✅ **全量覆盖**：3次失败后删除并重新创建表
- ✅ **错误隔离**：非关键表失败不影响应用启动

---

## 六、最终确认

**请确认以下内容**：

1. ✅ 关键表定义：`student_credentials`、`student_archive`、`dict_data`
2. ✅ 非关键表异步初始化：试卷相关表
3. ✅ 重试机制：失败3次后全量覆盖
4. ✅ 性能目标：<100ms（可接受），最好不超过500ms

**如果确认无误，我将开始实现代码！**



