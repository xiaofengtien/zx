# SQLite表初始化性能优化方案

## 一、问题分析

### 1.1 潜在性能问题

**当前方案**：启动时同步初始化所有表

**可能的问题**：
- 如果表结构复杂，CREATE TABLE 语句执行时间较长
- 多个表的创建是串行执行，总时间 = 表1时间 + 表2时间 + ...
- 启动画面可能显示时间过长，影响用户体验

### 1.2 性能测试数据（预估）

| 操作 | 预估时间 | 说明 |
|------|---------|------|
| 创建简单表（无索引） | 1-5ms | 基础表结构 |
| 创建复杂表（有索引） | 5-15ms | 包含多个索引 |
| 创建BLOB字段表 | 10-20ms | 大字段表 |
| **总计（10个表）** | **50-200ms** | 串行执行 |

**结论**：如果表数量不多（<15个），总时间通常在200ms以内，影响不大。但如果表很多或结构复杂，可能需要优化。

---

## 二、优化方案对比

### 方案A：检查表是否存在（最快，推荐）

**思路**：先检查表是否存在，如果存在就跳过创建

**优点**：
- ✅ 首次启动：正常创建表（50-200ms）
- ✅ 后续启动：只检查表是否存在（<10ms）
- ✅ 实现简单，代码改动小
- ✅ 不影响首次启动体验

**缺点**：
- ⚠️ 首次启动仍需创建所有表

**实现**：
```javascript
initTables() {
  // 快速检查：所有表是否都已存在
  const existingTables = this.getExistingTables()
  const requiredTables = ['student_credentials', 'student_papers', 'dict_data', 
                          'student_archive', 'paper_package', 'paper_index', 
                          'app_user_paper_info', 'app_user_paper_question_result',
                          'app_user_paper_question_blank_result']
  
  const missingTables = requiredTables.filter(t => !existingTables.includes(t))
  
  if (missingTables.length === 0) {
    console.log('所有表已存在，跳过初始化（耗时 < 10ms）')
    return // 快速返回
  }
  
  console.log(`需要创建 ${missingTables.length} 个表`)
  // 只创建缺失的表
  this.createMissingTables(missingTables)
}

getExistingTables() {
  return this.db.prepare(`
    SELECT name FROM sqlite_master 
    WHERE type='table' AND name NOT LIKE 'sqlite_%'
  `).all().map(t => t.name)
}
```

**性能**：
- 首次启动：50-200ms（创建所有表）
- 后续启动：<10ms（只检查）

---

### 方案B：异步初始化非关键表

**思路**：关键表同步初始化，非关键表异步初始化

**优点**：
- ✅ 启动速度快（只初始化关键表）
- ✅ 用户体验好（启动画面显示时间短）

**缺点**：
- ⚠️ 代码复杂（需要区分关键表和非关键表）
- ⚠️ 异步初始化可能失败，需要错误处理
- ⚠️ 首次使用非关键表时可能还未初始化完成

**实现**：
```javascript
initTables() {
  // 同步初始化关键表（必须立即可用）
  this.initCriticalTables()  // student_credentials, student_archive, dict_data
  
  // 异步初始化非关键表（可以延迟）
  setImmediate(() => {
    this.initPaperTables()  // 试卷相关表
    this.createIndexes()
  })
}

initCriticalTables() {
  // 只初始化登录、学员档案等关键表
  this.db.exec(`CREATE TABLE IF NOT EXISTS student_credentials (...)`)
  this.db.exec(`CREATE TABLE IF NOT EXISTS student_archive (...)`)
  this.db.exec(`CREATE TABLE IF NOT EXISTS dict_data (...)`)
}

initPaperTables() {
  // 试卷相关表异步初始化
  // ...
}
```

**性能**：
- 启动时间：20-50ms（只初始化关键表）
- 总时间：50-200ms（异步完成）

---

### 方案C：分批初始化 + 进度提示

**思路**：将表分为多批，每批初始化后更新进度

**优点**：
- ✅ 启动画面可以显示进度
- ✅ 用户体验好（有反馈）

**缺点**：
- ⚠️ 代码复杂
- ⚠️ 总时间不变，只是有反馈

**实现**：
```javascript
async initTables(progressCallback) {
  const batches = [
    { name: '基础表', tables: ['student_credentials', 'student_papers'] },
    { name: '业务表', tables: ['student_archive', 'dict_data'] },
    { name: '试卷表', tables: ['paper_package', 'paper_index'] },
    { name: '结果表', tables: ['app_user_paper_info', ...] }
  ]
  
  for (let i = 0; i < batches.length; i++) {
    progressCallback(i + 1, batches.length, batches[i].name)
    await this.initBatch(batches[i].tables)
  }
}
```

---

### 方案D：使用事务批量创建（最快，但风险高）

**思路**：将所有CREATE TABLE语句放在一个事务中

**优点**：
- ✅ 理论上最快（减少IO）

**缺点**：
- ⚠️ SQLite的CREATE TABLE不支持事务回滚
- ⚠️ 如果中间失败，可能导致部分表创建成功
- ⚠️ 不推荐使用

---

## 三、推荐方案

### 3.1 最终推荐：方案A（检查表是否存在）+ 方案B（异步初始化非关键表）

**组合方案**：
1. **启动时**：快速检查所有表是否存在
   - 如果都存在：<10ms，立即返回
   - 如果有缺失：同步创建关键表，异步创建非关键表

2. **关键表**（必须同步初始化）：
   - `student_credentials` - 登录必需
   - `student_archive` - 学员信息必需
   - `dict_data` - 字典数据必需

3. **非关键表**（可以异步初始化）：
   - `paper_package` - 试卷包（首次使用前初始化即可）
   - `paper_index` - 试卷索引
   - `app_user_paper_info` - 答题结果（答题前初始化即可）
   - `app_user_paper_question_result` - 题目结果
   - `app_user_paper_question_blank_result` - 完形填空结果

### 3.2 实现代码

```javascript
// exam-client/src/database/db.js

class DB {
  constructor() {
    // ... 数据库连接代码 ...
    
    // 初始化表结构（优化：快速检查 + 异步初始化）
    this.initTables()
  }

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
      // 试卷相关表
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
    
    // 3. 异步初始化非关键表（可以延迟）
    setImmediate(() => {
      this.initPaperTables(missingTables)
      this.createIndexes()
      const elapsed = Date.now() - startTime
      console.log(`✓ 数据库表结构初始化完成（总耗时 ${elapsed}ms）`)
    })
    
    const criticalElapsed = Date.now() - startTime
    console.log(`✓ 关键表初始化完成（耗时 ${criticalElapsed}ms）`)
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
  }

  /**
   * 初始化试卷相关表（异步，可以延迟）
   */
  initPaperTables(missingTables) {
    const paperTables = [
      'paper_package',
      'paper_index',
      'app_user_paper_info',
      'app_user_paper_question_result',
      'app_user_paper_question_blank_result'
    ]
    
    const toCreate = paperTables.filter(t => missingTables.includes(t))
    
    if (toCreate.length === 0) {
      return
    }
    
    console.log(`异步创建试卷相关表: ${toCreate.join(', ')}`)
    
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
   * 创建所有索引（包括试卷相关表的索引）
   */
  createIndexes() {
    // 关键表索引（已创建，跳过）
    // this.createCriticalIndexes()
    
    // 试卷相关表索引
    this.db.exec(`CREATE INDEX IF NOT EXISTS idx_paper_package_code ON paper_package(paper_code)`)
    this.db.exec(`CREATE INDEX IF NOT EXISTS idx_paper_index_code ON paper_index(paper_code)`)
    this.db.exec(`CREATE INDEX IF NOT EXISTS idx_paper_info_user ON app_user_paper_info(app_user_id)`)
    this.db.exec(`CREATE INDEX IF NOT EXISTS idx_paper_info_sync ON app_user_paper_info(sync_status)`)
    this.db.exec(`CREATE INDEX IF NOT EXISTS idx_paper_result_user ON app_user_paper_question_result(paper_id, app_user_id)`)
    this.db.exec(`CREATE INDEX IF NOT EXISTS idx_paper_result_sync ON app_user_paper_question_result(sync_status)`)
    this.db.exec(`CREATE INDEX IF NOT EXISTS idx_paper_blank_sync ON app_user_paper_question_blank_result(sync_status)`)
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

## 四、性能对比

### 4.1 性能数据（预估）

| 场景 | 原方案 | 优化方案 | 提升 |
|------|--------|---------|------|
| **首次启动** | 50-200ms | 20-50ms（关键表）+ 异步（非关键表） | 启动速度提升 60-75% |
| **后续启动（表已存在）** | 50-200ms | <10ms（只检查） | **提升 95%+** |
| **首次使用试卷功能** | - | 0ms（表已异步创建） | 无影响 |

### 4.2 用户体验

**原方案**：
- 启动画面显示：200-500ms
- 用户感知：可能感觉启动稍慢

**优化方案**：
- 启动画面显示：<50ms（关键表）
- 用户感知：启动很快
- 非关键表在后台异步创建，不影响使用

---

## 五、方案对比总结

| 方案 | 启动时间 | 实现复杂度 | 用户体验 | 推荐度 |
|------|---------|-----------|---------|--------|
| **原方案（全部同步）** | 50-200ms | ⭐ 简单 | ⭐⭐⭐ | ⭐⭐⭐ |
| **方案A（检查表）** | <10ms（后续） | ⭐⭐ 简单 | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| **方案B（异步非关键表）** | 20-50ms | ⭐⭐⭐ 中等 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| **组合方案（A+B）** | <10ms（后续）<br>20-50ms（首次） | ⭐⭐⭐ 中等 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |

---

## 六、推荐方案

### 6.1 最终推荐：组合方案（方案A + 方案B）

**理由**：
1. **启动速度快**：后续启动 <10ms，首次启动 20-50ms
2. **用户体验好**：启动画面显示时间短
3. **实现合理**：关键表同步，非关键表异步
4. **代码清晰**：模块化设计，易于维护

### 6.2 关键表 vs 非关键表

**关键表**（必须同步初始化）：
- `student_credentials` - 登录必需
- `student_archive` - 学员信息必需
- `dict_data` - 字典数据必需
- `student_papers` - 试卷类型（可选，但建议同步）

**非关键表**（可以异步初始化）：
- `paper_package` - 试卷包（首次使用试卷功能前初始化即可）
- `paper_index` - 试卷索引
- `app_user_paper_info` - 答题结果（答题前初始化即可）
- `app_user_paper_question_result` - 题目结果
- `app_user_paper_question_blank_result` - 完形填空结果

---

## 七、需要确认的问题

1. **关键表定义**：是否同意将 `student_credentials`、`student_archive`、`dict_data` 作为关键表？
2. **异步初始化**：是否接受非关键表异步初始化（可能首次使用时表还未创建完成）？
3. **错误处理**：异步初始化失败时，是否需要重试机制？
4. **进度提示**：是否需要显示初始化进度（可选）？

---

**请确认以上方案是否符合你的预期！**

