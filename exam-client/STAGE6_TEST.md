# 阶段六测试指南

## 一、功能概述

阶段六实现了客户端试卷包同步功能，包括：
1. ✅ ZIP包下载（支持小文件BLOB存储、中等文件文件系统存储）
2. ✅ ZIP包解压和媒体文件提取
3. ✅ 媒体文件索引保存到SQLite
4. ✅ 版本管理和清理策略
5. ✅ 解压结果缓存（LRU策略）

## 二、安装依赖

在 `exam-client` 目录下运行：

```bash
npm install adm-zip
```

## 三、测试步骤

### 1. 准备测试环境

1. **确保后端已启动**，并且有试卷包已生成
2. **确保学员已登录**，并且有权限访问试卷

### 2. 测试ZIP包同步

#### 2.1 启动客户端

```bash
cd exam-client
npm run dev
```

#### 2.2 登录学员账号

使用有权限的学员账号登录。

#### 2.3 观察控制台日志

查看控制台输出，应该看到：

```
步骤 3: 检查数据同步方式...
步骤 3: 检测到ZIP包，使用ZIP包方案（推荐）
步骤 3: 开始同步ZIP包...
开始同步ZIP包...
获取到 X 个试卷，开始同步ZIP包...
同步试卷包: PAPER_XXXXXX
ZIP包大小: XX.XX MB
✓ ZIP包已保存为BLOB: PAPER_XXXXXX, 大小: XX.XX MB
✓ ZIP包解压完成: PAPER_XXXXXX
✓ 媒体文件提取完成: /path/to/media/PAPER_XXXXXX
✓ 媒体文件索引保存完成: PAPER_XXXXXX
✓ 试卷包同步成功: PAPER_XXXXXX
ZIP包同步完成: 成功 X 个，失败 0 个
```

### 3. 验证数据存储

#### 3.1 检查SQLite数据库

使用SQLite工具（如DataGrip、DB Browser）打开数据库文件：
- 路径：`{userData}/exam.db`
- macOS: `~/Library/Application Support/exam-client/exam.db`
- Windows: `%APPDATA%/exam-client/exam.db`
- Linux: `~/.config/exam-client/exam.db`

**检查表：**

1. **paper_package表**：
```sql
SELECT paper_code, package_hash, package_size, storage_type, version, sync_time 
FROM paper_package 
WHERE is_active = 1;
```

2. **question_media_index表**：
```sql
SELECT paper_id, question_id, media_type, media_name, media_path 
FROM question_media_index 
LIMIT 10;
```

3. **paper_index表**：
```sql
SELECT paper_code, intro_audio_path, version 
FROM paper_index;
```

#### 3.2 检查文件系统

**检查ZIP包文件：**
- 路径：`{userData}/paper_packages/`
- 应该看到：`{paper_code}_v{version}.zip` 文件

**检查媒体文件：**
- 路径：`{userData}/media/{paper_code}/`
- 应该看到：
  - `intro/` - 试卷独白音频
  - `q_{question_id}/` - 题目媒体文件
  - `q_{question_id}/options/` - 选项媒体文件

### 4. 测试不同文件大小

#### 4.1 小文件（<50MB）
- 应该存储为BLOB到SQLite
- `storage_type = 0`
- `package_data` 字段有值

#### 4.2 中等文件（50-200MB）
- 应该存储到文件系统
- `storage_type = 1`
- `package_path` 字段有值

#### 4.3 大文件（>200MB）
- ⚠️ 分片下载功能待实现
- 当前会抛出错误

### 5. 测试版本管理

#### 5.1 更新试卷包

1. 在后台管理更新试卷并重新生成试卷包
2. 客户端重新同步
3. 应该检测到版本不一致并重新下载

#### 5.2 清理旧版本

运行清理方法（在代码中调用）：
```javascript
await paperService.cleanupOldVersions()
```

应该删除90天前的旧版本。

### 6. 测试缓存功能

#### 6.1 解压结果缓存

1. 第一次解压：应该看到解压过程
2. 第二次解压：应该看到"使用缓存的解压结果"

#### 6.2 缓存大小限制

缓存大小限制为100MB，超过会自动清理最旧的缓存。

## 四、常见问题

### Q1: 下载失败，提示"ZIP包hash验证失败"

**原因**：下载的文件损坏或hash不匹配

**解决**：
1. 检查网络连接
2. 重新生成试卷包
3. 检查后端OSS配置

### Q2: 解压失败，提示"ZIP解压库未安装"

**原因**：缺少 `adm-zip` 依赖

**解决**：
```bash
cd exam-client
npm install adm-zip
```

### Q3: 媒体文件不存在

**原因**：ZIP包格式不正确或媒体文件路径错误

**解决**：
1. 检查ZIP包内容
2. 确认 `manifest.json` 和 `questions.json` 格式正确
3. 检查媒体文件路径

### Q4: 同步超时

**原因**：文件太大或网络慢

**解决**：
1. 增加超时时间（当前5分钟）
2. 检查网络连接
3. 对于>200MB文件，等待分片下载功能实现

## 五、性能指标

### 预期性能

- **小文件（<50MB）**：下载+解压 < 30秒
- **中等文件（50-200MB）**：下载+解压 < 2分钟
- **大文件（>200MB）**：待实现分片下载

### 存储空间

- **ZIP包**：原始大小
- **媒体文件**：解压后大小（通常与ZIP包大小相近）
- **SQLite数据库**：ZIP包（BLOB）+ 索引数据

## 六、下一步

阶段六完成后，可以开始：
- **阶段七**：客户端答题功能实现
  - 使用本地ZIP包数据
  - 加载媒体文件
  - 实现答题界面

## 七、注意事项

1. ⚠️ **分片下载功能待实现**（>200MB文件）
2. ⚠️ **断点续传功能待完善**（当前使用完整下载）
3. ✅ **版本管理已实现**（保留当前版本+最近3个版本）
4. ✅ **清理策略已实现**（90天清理旧版本）

---

**测试完成后，请反馈测试结果！**


