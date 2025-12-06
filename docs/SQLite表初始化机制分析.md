# SQLite表初始化机制分析

## 一、现有初始化机制分析

### 1.1 当前流程

```
应用启动 (main.js)
  ↓
app.whenReady()
  ↓
initDatabase()  // 创建 DB 实例
  ↓
new Database()  // db.js 构造函数
  ↓
this.initTables()  // 自动调用，初始化所有表
  ↓
创建表：student_credentials, student_papers, dict_data, student_archive
```

### 1.2 代码位置

**exam-client/src/database/db.js**:
```javascript
class DB {
  constructor() {
    // ... 数据库连接代码 ...
    
    // 初始化表结构（自动调用，无需手动触发）
    this.initTables()  // ← 这里自动调用
  }

  initTables() {
    // 创建学员凭证表
    this.db.exec(`CREATE TABLE IF NOT EXISTS student_credentials (...)`)
    
    // 创建学员考卷类型表
    this.db.exec(`CREATE TABLE IF NOT EXISTS student_papers (...)`)
    
    // 创建字典数据表
    this.db.exec(`CREATE TABLE IF NOT EXISTS dict_data (...)`)
    
    // 创建学员档案表
    this.db.exec(`CREATE TABLE IF NOT EXISTS student_archive (...)`)
    
    // 创建索引...
  }
}
```

**exam-client/.electron-vue/main.js**:
```javascript
// 初始化数据库（应用启动时自动调用）
function initDatabase() {
  const Database = require(path.join(__dirname, '../src/database/db'))
  db = new Database()  // ← 创建实例时自动调用 initTables()
  console.log('Database initialized successfully')
}

app.whenReady().then(() => {
  createSplashWindow()
  initDatabase()  // ← 启动时自动调用
  checkNetworkStatus()
  // ...
})
```

### 1.3 关键特点

✅ **自动初始化**：表创建在 `constructor()` 中自动执行  
✅ **启动时执行**：应用启动时自动调用 `initDatabase()`  
✅ **无需手动触发**：用户无需任何操作  
✅ **使用 IF NOT EXISTS**：如果表已存在，不会报错

---

## 二、试卷相关表的初始化方案

### 2.1 方案对比

#### ❌ 方案A：单独方法，需要手动调用（不推荐）

```javascript
// 问题：需要手动调用，容易遗漏
initPaperTables() {
  // 创建试卷相关表...
}

// 需要在某个地方手动调用
someFunction() {
  db.initPaperTables()  // ← 容易忘记调用
}
```

**缺点**：
- 需要手动触发，容易遗漏
- 不符合现有代码风格
- 维护成本高

#### ✅ 方案B：在 initTables() 中直接添加（推荐）

```javascript
initTables() {
  // 现有的表...
  this.db.exec(`CREATE TABLE IF NOT EXISTS student_credentials (...)`)
  this.db.exec(`CREATE TABLE IF NOT EXISTS student_papers (...)`)
  this.db.exec(`CREATE TABLE IF NOT EXISTS dict_data (...)`)
  this.db.exec(`CREATE TABLE IF NOT EXISTS student_archive (...)`)
  
  // 新增：试卷相关表（和现有表一样，自动初始化）
  this.db.exec(`CREATE TABLE IF NOT EXISTS paper_package (...)`)
  this.db.exec(`CREATE TABLE IF NOT EXISTS paper_index (...)`)
  this.db.exec(`CREATE TABLE IF NOT EXISTS app_user_paper_info (...)`)
  this.db.exec(`CREATE TABLE IF NOT EXISTS app_user_paper_question_result (...)`)
  this.db.exec(`CREATE TABLE IF NOT EXISTS app_user_paper_question_blank_result (...)`)
  
  // 创建索引...
}
```

**优点**：
- ✅ 和现有表一样，启动时自动初始化
- ✅ 无需手动触发
- ✅ 代码风格统一
- ✅ 维护简单

#### ✅ 方案C：单独方法，在 initTables() 中调用（推荐，代码更清晰）

```javascript
initTables() {
  // 现有的表...
  this.initBasicTables()
  
  // 新增：试卷相关表
  this.initPaperTables()
  
  // 创建所有索引
  this.createIndexes()
}

initBasicTables() {
  this.db.exec(`CREATE TABLE IF NOT EXISTS student_credentials (...)`)
  this.db.exec(`CREATE TABLE IF NOT EXISTS student_papers (...)`)
  this.db.exec(`CREATE TABLE IF NOT EXISTS dict_data (...)`)
  this.db.exec(`CREATE TABLE IF NOT EXISTS student_archive (...)`)
}

initPaperTables() {
  // 试卷包表
  this.db.exec(`CREATE TABLE IF NOT EXISTS paper_package (...)`)
  
  // 试卷索引表
  this.db.exec(`CREATE TABLE IF NOT EXISTS paper_index (...)`)
  
  // 用户试卷信息表
  this.db.exec(`CREATE TABLE IF NOT EXISTS app_user_paper_info (...)`)
  
  // 用户题目结果表
  this.db.exec(`CREATE TABLE IF NOT EXISTS app_user_paper_question_result (...)`)
  
  // 用户完形填空结果表
  this.db.exec(`CREATE TABLE IF NOT EXISTS app_user_paper_question_blank_result (...)`)
}

createIndexes() {
  // 所有表的索引...
}
```

**优点**：
- ✅ 代码结构清晰，模块化
- ✅ 启动时自动初始化
- ✅ 易于维护和扩展
- ✅ 符合现有代码风格

---

## 三、推荐方案

### 3.1 最终推荐：方案C（模块化设计）

**理由**：
1. **代码清晰**：将不同类型的表分开管理
2. **易于维护**：后续新增表时，只需在对应方法中添加
3. **自动初始化**：在 `initTables()` 中调用，启动时自动执行
4. **符合现有风格**：和 `initStudentArchiveTable()` 的设计思路一致

### 3.2 实现代码

```javascript
// exam-client/src/database/db.js

class DB {
  constructor() {
    // ... 数据库连接代码 ...
    
    // 初始化表结构（自动调用，无需手动触发）
    this.initTables()  // ← 启动时自动调用
  }

  initTables() {
    console.log('开始初始化数据库表结构...')
    
    // 1. 基础表（学员、字典等）
    this.initBasicTables()
    
    // 2. 试卷相关表（新增）
    this.initPaperTables()
    
    // 3. 创建所有索引
    this.createIndexes()
    
    // 输出表创建信息
    console.log('数据库表结构初始化完成')
    const tables = this.db.prepare(`
      SELECT name FROM sqlite_master 
      WHERE type='table' AND name NOT LIKE 'sqlite_%'
    `).all()
    console.log('已创建的表:', tables.map(t => t.name))
  }

  /**
   * 初始化基础表（学员、字典等）
   */
  initBasicTables() {
    // 创建学员凭证表
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

    // 创建学员考卷类型表
    this.db.exec(`
      CREATE TABLE IF NOT EXISTS student_papers (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        student_account TEXT NOT NULL,
        paper_types TEXT NOT NULL,
        update_time INTEGER,
        FOREIGN KEY (student_account) REFERENCES student_credentials(student_account)
      )
    `)

    // 创建字典数据表
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

    // 创建学员档案表
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

  /**
   * 初始化试卷相关表（新增）
   */
  initPaperTables() {
    // 试卷包表
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
    
    // 试卷索引表
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
    
    // 用户试卷信息表
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
    
    // 用户题目结果表
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
    
    // 用户完形填空结果表
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

  /**
   * 创建所有索引
   */
  createIndexes() {
    // 基础表索引
    this.db.exec(`CREATE INDEX IF NOT EXISTS idx_student_account ON student_credentials(student_account)`)
    this.db.exec(`CREATE INDEX IF NOT EXISTS idx_student_papers_account ON student_papers(student_account)`)
    this.db.exec(`CREATE INDEX IF NOT EXISTS idx_dict_type ON dict_data(dict_type)`)
    this.db.exec(`CREATE INDEX IF NOT EXISTS idx_student_archive_account ON student_archive(student_account)`)
    this.db.exec(`CREATE INDEX IF NOT EXISTS idx_student_archive_user_id ON student_archive(user_id)`)
    
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

## 四、执行流程

### 4.1 完整流程

```
应用启动
  ↓
app.whenReady()
  ↓
initDatabase()
  ↓
new Database()  // 创建 DB 实例
  ↓
constructor()  // 自动执行
  ↓
this.initTables()  // 自动调用
  ↓
├─ initBasicTables()      // 基础表（学员、字典）
├─ initPaperTables()      // 试卷相关表（新增）
└─ createIndexes()        // 所有索引
  ↓
表创建完成（自动完成，无需手动触发）
```

### 4.2 关键点

✅ **自动执行**：应用启动时自动初始化所有表  
✅ **无需手动触发**：用户无需任何操作  
✅ **使用 IF NOT EXISTS**：表已存在时不会报错  
✅ **模块化设计**：代码清晰，易于维护

---

## 五、总结

### 5.1 回答你的问题

**Q: 是否应该在启动时检查并初始化表（就像学员登录一样）？**

**A: 是的！** 应该和现有的表一样，在启动时自动初始化。

**Q: 文档中的 `initPaperTables()` 是否需要手动触发？**

**A: 不需要！** 应该在 `initTables()` 中自动调用，就像现有的表一样。

### 5.2 推荐方案

采用**方案C（模块化设计）**：
- 在 `initTables()` 中调用 `initPaperTables()`
- 启动时自动执行，无需手动触发
- 代码结构清晰，易于维护

### 5.3 与现有机制的一致性

| 特性 | 现有表 | 试卷相关表 | 一致性 |
|------|--------|-----------|--------|
| 启动时自动初始化 | ✅ | ✅ | ✅ |
| 无需手动触发 | ✅ | ✅ | ✅ |
| 使用 IF NOT EXISTS | ✅ | ✅ | ✅ |
| 模块化设计 | ⚠️（都在initTables中） | ✅（单独方法） | 改进 |

---

**结论**：试卷相关表的初始化应该和现有表一样，在应用启动时自动执行，无需手动触发。推荐使用模块化设计，代码更清晰易维护。



