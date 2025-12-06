# ZIP包存储位置说明

## 一、数据流说明

### 完整流程

```
┌─────────────────────────────────────────────────────────────┐
│  步骤1：后台管理 - 生成试卷包                                  │
├─────────────────────────────────────────────────────────────┤
│  位置：后端服务器                                             │
│  操作：生成ZIP包 → 上传到OSS                                  │
│  OSS路径：paper_packages/{paper_code}_v{version}.zip        │
│  存储位置：OSS对象存储（服务器端）                             │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│  步骤2：客户端 - 下载试卷包                                    │
├─────────────────────────────────────────────────────────────┤
│  位置：客户端本地                                             │
│  操作：从OSS下载ZIP包 → 存储到本地                             │
│  本地路径：{userData}/paper_packages/{paper_code}_v{version}.zip│
│  存储位置：客户端本地文件系统                                  │
└─────────────────────────────────────────────────────────────┘
```

---

## 二、存储位置对比

### 1. 服务器端（OSS）

**路径**：`paper_packages/{paper_code}_v{version}.zip`

**说明**：
- 这是OSS对象存储中的路径
- 后端生成ZIP包后上传到这里
- 客户端通过HTTP请求从这里下载

**示例**：
```
OSS存储：
paper_packages/junior_high_school_english_listening_v1.zip
paper_packages/high_school_english_listening_v2.zip
```

### 2. 客户端本地

**路径**：`{userData}/paper_packages/{paper_code}_v{version}.zip`

**说明**：
- 这是客户端本地文件系统的路径
- 客户端从OSS下载ZIP包后存储到这里
- 用于离线使用

**示例（macOS）**：
```
/Users/Stephen/Library/Application Support/exam-client/paper_packages/
├── junior_high_school_english_listening_v1.zip
└── high_school_english_listening_v2.zip
```

**示例（Windows）**：
```
C:\Users\{用户名}\AppData\Roaming\exam-client\paper_packages\
├── junior_high_school_english_listening_v1.zip
└── high_school_english_listening_v2.zip
```

**示例（Linux）**：
```
~/.config/exam-client/paper_packages/
├── junior_high_school_english_listening_v1.zip
└── high_school_english_listening_v2.zip
```

---

## 三、关键区别

### ❌ 不是自动的

**重要说明**：
- 后端生成ZIP包后上传到OSS，**不会自动**存放到客户端本地
- 客户端需要**主动下载**ZIP包到本地
- 这是两个不同的存储位置

### ✅ 正确的流程

1. **后端**：生成ZIP包 → 上传到OSS
2. **客户端**：检测到需要ZIP包 → 从OSS下载 → 存储到本地

---

## 四、客户端下载逻辑（待实现）

### 需要实现的功能

```javascript
// exam-client/src/database/paperService.js

class PaperService {
  /**
   * 同步试卷包
   * 1. 从OSS下载ZIP包
   * 2. 存储到本地 {userData}/paper_packages/{paper_code}_v{version}.zip
   * 3. 根据文件大小选择存储方式（BLOB或文件系统）
   */
  async syncPaperPackage(paperCode) {
    // 1. 获取试卷信息（包含package_hash、version等）
    const paper = await this.getPaperByCode(paperCode)
    
    // 2. 检查本地是否已有ZIP包
    const localPath = path.join(
      this.userDataPath, 
      'paper_packages', 
      `${paperCode}_v${paper.version}.zip`
    )
    
    if (fs.existsSync(localPath)) {
      // 检查hash是否一致
      const localHash = await this.calculateFileHash(localPath)
      if (localHash === paper.packageHash) {
        console.log('ZIP包已存在且版本一致，跳过下载')
        return
      }
    }
    
    // 3. 从OSS下载ZIP包
    const zipData = await this.downloadFromOSS(paperCode, paper.version)
    
    // 4. 根据文件大小选择存储方式
    if (zipData.length < 50 * 1024 * 1024) {
      // <50MB：存储为BLOB
      await this.saveToSQLite(paperCode, zipData)
    } else {
      // >=50MB：存储到文件系统
      await this.saveToFileSystem(paperCode, zipData, localPath)
    }
  }
}
```

---

## 五、总结

### ✅ 确认

1. **后端生成ZIP包** → 上传到OSS（服务器端）
2. **客户端下载ZIP包** → 存储到本地 `{userData}/paper_packages/`（客户端本地）
3. **这是两个不同的位置**，需要客户端主动下载

### ⚠️ 当前状态

- ✅ 后端已实现：生成ZIP包并上传到OSS
- ❌ 客户端未实现：从OSS下载ZIP包到本地（阶段六的任务）

### 📝 需要实现

客户端需要实现：
1. 从OSS下载ZIP包的功能
2. 存储到本地 `{userData}/paper_packages/` 目录
3. 根据文件大小选择存储方式（BLOB或文件系统）

---

## 六、路径说明

| 位置 | 路径 | 说明 |
|------|------|------|
| **OSS（服务器端）** | `paper_packages/{paper_code}_v{version}.zip` | 后端生成后上传到这里 |
| **客户端本地（macOS）** | `~/Library/Application Support/exam-client/paper_packages/{paper_code}_v{version}.zip` | 客户端下载后存储到这里 |
| **客户端本地（Windows）** | `C:\Users\{用户名}\AppData\Roaming\exam-client\paper_packages\{paper_code}_v{version}.zip` | 客户端下载后存储到这里 |
| **客户端本地（Linux）** | `~/.config/exam-client/paper_packages/{paper_code}_v{version}.zip` | 客户端下载后存储到这里 |

**关键点**：OSS路径和客户端本地路径是**不同的**，需要客户端主动下载！


