# DataGrip 连接 SQLite 数据库配置指南

## 1. 找到数据库文件路径

数据库文件位置取决于 Electron 应用的 `userData` 路径：

### macOS 开发环境
```
~/Library/Application Support/exam-client/exam.db
```

### macOS 生产环境（打包后）
```
~/Library/Application Support/[应用名称]/exam.db
```

### 查看实际路径
启动应用后，在控制台日志中查找：
```
=== SQLite 数据库信息 ===
数据库文件路径: /Users/你的用户名/Library/Application Support/exam-client/exam.db
```

## 2. 在 DataGrip 中配置 SQLite 数据源

### 步骤 1：添加数据源
1. 打开 DataGrip
2. 点击左上角的 `+` 按钮，选择 `Data Source` → `SQLite`
3. 或者：`File` → `New` → `Data Source` → `SQLite`

### 步骤 2：配置连接
1. **Database file**: 点击 `...` 按钮，选择数据库文件路径
   - 例如：`/Users/你的用户名/Library/Application Support/exam-client/exam.db`
2. **Driver**: 选择 `SQLite`（通常会自动选择）
3. 点击 `Test Connection` 测试连接
4. 如果提示需要下载驱动，点击 `Download` 下载 SQLite 驱动
5. 连接成功后，点击 `OK` 保存

### 步骤 3：查看数据
1. 在左侧数据源列表中，展开你的 SQLite 数据源
2. 展开 `exam.db` → `Tables`
3. 可以看到以下表：
   - `student_archive` - 学员档案表
   - `student_credentials` - 学员凭证表
   - `student_papers` - 学员试卷类型表（已废弃，数据在 student_archive 中）
   - `dict_data` - 字典数据表

## 3. 常用查询

### 查看所有学员档案
```sql
SELECT 
    id, 
    user_id, 
    student_account, 
    applicable_papers,
    status,
    del_flag
FROM student_archive
WHERE del_flag = '0' AND status = '0';
```

### 查看特定学员的试卷类型
```sql
SELECT 
    id,
    user_id,
    student_account,
    applicable_papers
FROM student_archive
WHERE user_id = 100 
  AND del_flag = '0' 
  AND status = '0';
```

### 查看字典数据
```sql
SELECT * FROM dict_data WHERE dict_type = 'paper_type';
```

## 4. 注意事项

1. **文件权限**：确保 DataGrip 有读取数据库文件的权限
2. **文件锁定**：如果应用正在运行，数据库文件可能被锁定。可以：
   - 关闭应用后再连接
   - 或者使用只读模式连接（DataGrip 支持）
3. **路径中的空格**：如果路径包含空格，确保正确转义

## 5. 如果找不到数据库文件

1. 检查应用是否已启动过（数据库文件在首次启动时创建）
2. 检查控制台日志中的数据库路径
3. 在 Finder 中按 `Cmd + Shift + G`，输入路径查看



