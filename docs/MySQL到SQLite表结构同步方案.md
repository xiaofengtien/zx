# MySQL到SQLite表结构同步方案

## 一、问题背景

当MySQL数据库表结构发生变化（如添加新字段）时，需要同步到SQLite中。例如：
- 添加 `paper_type` 字段到 `paper` 表
- 添加 `package_hash` 字段到 `paper` 表
- 其他后续需求新增的字段

## 二、解决方案

采用**双重机制**：版本化迁移 + 自动检测同步

### 1. 版本化迁移（推荐，用于已知变更）

**优点**：
- ✅ 可靠、可控
- ✅ 可以记录变更历史
- ✅ 可以回滚
- ✅ 适合已知的、计划内的变更

**实现**：
- 使用 `migration.js` 管理迁移
- 迁移文件存放在 `migrations/` 目录
- 格式：`001_add_paper_type.js`, `002_add_new_field.js`

**使用流程**：
1. MySQL执行DDL变更
2. 创建迁移文件（如 `002_add_new_field.js`）
3. 客户端启动时自动执行未应用的迁移

### 2. 自动检测同步（备用，用于未知变更）

**优点**：
- ✅ 自动检测未知变更
- ✅ 无需手动创建迁移文件
- ✅ 适合紧急修复或遗漏的变更

**缺点**：
- ⚠️ 需要后端提供API
- ⚠️ 需要网络连接
- ⚠️ 可能无法处理复杂变更（如删除字段、修改类型）

**实现**：
- 使用 `schemaSync.js` 从后端API获取表结构
- 对比MySQL和SQLite的字段差异
- 自动添加缺失字段

## 三、实现细节

### 1. 迁移系统

#### 迁移文件格式

```javascript
// migrations/001_add_paper_type.js
module.exports = {
  up: async (db) => {
    // 添加字段
    db.exec(`ALTER TABLE paper ADD COLUMN paper_type TEXT`)
    // 添加索引
    db.exec(`CREATE INDEX IF NOT EXISTS idx_paper_type ON paper(paper_type)`)
  },
  
  down: async (db) => {
    // 回滚操作（可选）
    db.exec(`DROP INDEX IF EXISTS idx_paper_type`)
  }
}
```

#### 迁移执行流程

```
应用启动
  ↓
检查迁移记录表
  ↓
获取所有迁移文件
  ↓
对比已应用的迁移
  ↓
执行未应用的迁移
  ↓
记录迁移版本
```

### 2. 自动同步系统

#### 后端API（需要实现）

```java
@PostMapping("/student/sync/table/schema")
public AjaxResult getTableSchema(@RequestBody Map<String, String> params) {
    String tableName = params.get("tableName");
    
    // 查询MySQL表结构
    List<ColumnInfo> columns = getTableColumnsFromMySQL(tableName);
    
    return AjaxResult.success(columns);
}
```

#### 客户端同步流程

```
在线登录后
  ↓
调用同步API获取表结构
  ↓
对比SQLite表结构
  ↓
添加缺失字段
  ↓
记录同步结果
```

## 四、使用指南

### 1. 添加新字段（推荐方式：迁移）

**步骤1**：在MySQL中执行DDL
```sql
ALTER TABLE `paper` ADD COLUMN `new_field` VARCHAR(100) COMMENT '新字段';
```

**步骤2**：创建迁移文件
```bash
# 创建文件：exam-client/src/database/migrations/002_add_new_field.js
```

**步骤3**：编写迁移代码
```javascript
module.exports = {
  up: async (db) => {
    db.exec(`ALTER TABLE paper ADD COLUMN new_field TEXT`)
  },
  down: async (db) => {
    // SQLite不支持删除列，这里留空
  }
}
```

**步骤4**：客户端启动时自动执行

### 2. 自动检测同步（备用方式）

**步骤1**：确保后端提供了 `/student/sync/table/schema` API

**步骤2**：在客户端代码中调用
```javascript
const SchemaSyncService = require('./schemaSync')
const schemaSync = new SchemaSyncService(db)

// 同步单个表
await schemaSync.syncTableSchema('paper', token)

// 同步多个表
await schemaSync.syncMultipleTables(['paper', 'question'], token)
```

## 五、类型映射

MySQL类型 → SQLite类型：

| MySQL类型 | SQLite类型 | 说明 |
|-----------|-----------|------|
| INT, INTEGER, BIGINT | INTEGER | 整数 |
| DECIMAL, FLOAT, DOUBLE | REAL | 浮点数 |
| VARCHAR, TEXT, CHAR | TEXT | 文本 |
| DATETIME, TIMESTAMP | INTEGER | 时间戳（存储为毫秒） |
| TINYINT(1), BOOLEAN | INTEGER | 布尔值（0/1） |

## 六、注意事项

### 1. SQLite限制

- ❌ **不支持删除列**：只能添加，不能删除
- ❌ **不支持修改列类型**：只能添加新列
- ❌ **不支持重命名列**：需要重建表

### 2. 迁移文件命名

- 格式：`{序号}_{描述}.js`
- 序号：3位数字，递增（001, 002, 003...）
- 描述：使用下划线分隔（`add_paper_type`, `add_new_field`）

### 3. 迁移执行顺序

- 按文件名排序执行
- 已应用的迁移不会重复执行
- 迁移失败会停止后续迁移

### 4. 自动同步的限制

- 只能添加字段，不能删除或修改
- 需要网络连接和后端API支持
- 复杂变更（如修改类型）需要手动处理

## 七、最佳实践

### 1. 优先使用迁移

- 对于计划内的变更，使用迁移文件
- 迁移文件可以版本控制
- 迁移可以回滚（如果实现了down方法）

### 2. 自动同步作为补充

- 用于检测遗漏的变更
- 用于紧急修复
- 用于开发阶段的快速同步

### 3. 定期检查

- 定期检查是否有未同步的字段
- 将自动检测到的变更转换为迁移文件
- 保持迁移文件的完整性

## 八、示例

### 示例1：添加 paper_type 字段（已完成）

**迁移文件**：`migrations/001_add_paper_type.js`

```javascript
module.exports = {
  up: async (db) => {
    const columns = db.prepare(`PRAGMA table_info(paper)`).all()
    const columnExists = columns.some(col => col.name === 'paper_type')
    
    if (!columnExists) {
      db.exec(`ALTER TABLE paper ADD COLUMN paper_type TEXT`)
      db.exec(`CREATE INDEX IF NOT EXISTS idx_paper_type ON paper(paper_type)`)
    }
  },
  down: async (db) => {
    db.exec(`DROP INDEX IF EXISTS idx_paper_type`)
  }
}
```

### 示例2：添加新字段 new_field

**迁移文件**：`migrations/002_add_new_field.js`

```javascript
module.exports = {
  up: async (db) => {
    const columns = db.prepare(`PRAGMA table_info(paper)`).all()
    if (!columns.some(col => col.name === 'new_field')) {
      db.exec(`ALTER TABLE paper ADD COLUMN new_field TEXT`)
    }
  },
  down: async (db) => {
    // SQLite不支持删除列
  }
}
```

## 九、总结

- ✅ **迁移系统**：用于已知的、计划内的变更（推荐）
- ✅ **自动同步**：用于未知的、遗漏的变更（备用）
- ✅ **双重保障**：确保表结构始终同步
- ✅ **向后兼容**：不影响现有数据



