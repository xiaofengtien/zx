# Hash算法验证说明

## 一、Hash算法一致性验证

### 1. 后端Hash算法（Java）

**位置**：`zx-business/src/main/java/com/zx/student/archive/service/paper/impl/PaperPackageService.java`

```java
public String calculateHash(byte[] data) {
    try {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(data);
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));  // 小写十六进制
        }
        return sb.toString();
    } catch (Exception e) {
        log.error("计算哈希值失败", e);
        return null;
    }
}
```

**特点**：
- 算法：SHA-256
- 输出格式：小写十六进制字符串
- 长度：64个字符（256位 = 32字节 = 64个十六进制字符）

### 2. 客户端Hash算法（Node.js）

**位置**：`exam-client/src/database/paperService.js`

```javascript
calculateHash(buffer) {
  return crypto.createHash('sha256').update(buffer).digest('hex')
}
```

**特点**：
- 算法：SHA-256
- 输出格式：小写十六进制字符串
- 长度：64个字符（256位 = 32字节 = 64个十六进制字符）

### 3. 算法一致性

✅ **算法一致**：两端都使用 SHA-256
✅ **格式一致**：两端都输出小写十六进制字符串
✅ **长度一致**：两端都输出64个字符

## 二、Hash不匹配的可能原因

### 1. 文件内容不同
- **OSS上的文件与数据库记录不一致**：生成试卷包后，OSS上的文件被修改或替换
- **文件在传输过程中损坏**：网络传输错误导致文件内容改变

### 2. 文件大小问题
- **961字节的文件可能是有效的**：如果试卷内容很少（只有少量JSON数据，没有媒体文件），ZIP包确实可能很小
- **不应该仅凭文件大小判断文件是否损坏**：应该通过hash验证来判断

### 3. 版本不一致
- **数据库中的hash是旧版本的**：试卷包已重新生成，但数据库中的hash未更新
- **OSS上的文件是旧版本的**：数据库已更新，但OSS上的文件未更新

## 三、验证流程

### 1. 后端验证（已实现）

```java
// 1. 从OSS下载ZIP包
byte[] zipBytes = ossUtil.downloadFileToBytes(packageFileName);

// 2. 计算hash
String actualHash = paperPackageService.calculateHash(zipBytes);

// 3. 与数据库中的hash对比
if (!actualHash.equals(expectedHash)) {
    // hash不匹配，重新生成试卷包
    generatePaperPackage(paperId);
    // 重新下载并验证
}
```

### 2. 客户端验证（已实现）

```javascript
// 1. 下载ZIP包
const buffer = Buffer.concat(chunks)

// 2. 计算hash
const actualHash = this.calculateHash(buffer)

// 3. 验证hash格式（64个十六进制字符）
if (actualHash.length !== 64) {
  throw new Error('hash长度不正确')
}

// 4. 与期望的hash对比
if (expectedHash && actualHash !== expectedHash) {
  throw new Error('hash不匹配')
}
```

### 3. 对比验证（新增）

**后端日志**：
```
后端下载的ZIP包hash: {hash}, 文件大小: {size} 字节
数据库中的hash: {hash}, 文件大小: {size} 字节
✓ 后端下载的hash与数据库中的hash一致
```

**客户端日志**：
```
期望hash (来自数据库): {hash}
实际hash (客户端计算): {hash}
hash算法: SHA-256
hash长度: 期望=64, 实际=64
```

## 四、问题排查步骤

### 1. 检查后端日志
- 查看"后端下载的ZIP包hash"和"数据库中的hash"是否一致
- 如果不一致，说明OSS上的文件与数据库记录不一致

### 2. 检查客户端日志
- 查看"期望hash"和"实际hash"是否一致
- 查看hash长度和格式是否正确

### 3. 对比后端和客户端的hash
- 如果后端下载的hash与客户端下载的hash不一致，说明：
  - 后端和客户端下载的不是同一个文件
  - 或者文件在传输过程中损坏

### 4. 验证文件大小
- **961字节可能是有效的**：如果试卷内容很少，ZIP包确实可能很小
- **不应该仅凭文件大小判断**：应该通过hash验证来判断

## 五、解决方案

### 1. 后端自动修复（已实现）
- 检测到hash不匹配时，自动重新生成试卷包
- 更新数据库中的hash和文件大小

### 2. 客户端重试机制（已实现）
- hash验证失败时，等待3秒后重新获取试卷信息
- 后端可能已经重新生成，hash已更新
- 最多重试2次

### 3. 详细日志（已实现）
- 记录后端下载的hash
- 记录数据库中的hash
- 记录客户端计算的hash
- 方便对比和排查问题

## 六、测试验证

### 1. 验证hash算法一致性

**测试用例**：使用相同的测试数据，验证后端和客户端计算的hash是否一致

```javascript
// 客户端测试
const testData = Buffer.from('test data')
const clientHash = crypto.createHash('sha256').update(testData).digest('hex')
console.log('客户端hash:', clientHash)
```

```java
// 后端测试
byte[] testData = "test data".getBytes();
String serverHash = paperPackageService.calculateHash(testData);
log.info("后端hash: {}", serverHash);
```

**预期结果**：两端计算的hash应该完全一致

### 2. 验证小文件处理

**测试用例**：创建一个只有961字节的有效ZIP包，验证是否能正常处理

**预期结果**：如果hash匹配，应该能正常处理，不应该因为文件小就拒绝



