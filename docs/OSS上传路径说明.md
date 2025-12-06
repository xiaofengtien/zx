# OSS ZIP包上传路径说明

## 当前实现

### 路径格式

**新格式（推荐）**：
```
paper_packages/{year}{month}_{province}_{paper_type}/{paper_name}_v{version}.zip
```

**如果 month 为 null**：
```
paper_packages/{year}_{province}_{paper_type}/{paper_name}_v{version}.zip
```

**向后兼容格式**（如果无法构建 groupKey）：
```
paper_packages/{paper_type}/{paper_name}_v{version}.zip
```
或
```
paper_packages/{paper_name}_v{version}.zip
```

### 示例

1. **有月份的情况**：
   - 试卷：2000年1月河北高中英语听力（模拟一）
   - 路径：`paper_packages/200001_hebei_high_school_english_listening/2000年1月河北高中英语听力模拟一_v1.zip`

2. **无月份的情况**：
   - 试卷：2000年河北高中英语听力（模拟一）
   - 路径：`paper_packages/2000_hebei_high_school_english_listening/2000年河北高中英语听力模拟一_v1.zip`

3. **同一组合多条记录**：
   - 2000年1月河北高中英语听力（模拟一）- version: 1
   - 2000年1月河北高中英语听力（模拟二）- version: 2
   - 2000年1月河北高中英语听力（模拟三）- version: 3
   - 路径：
     - `paper_packages/200001_hebei_high_school_english_listening/2000年1月河北高中英语听力模拟一_v1.zip`
     - `paper_packages/200001_hebei_high_school_english_listening/2000年1月河北高中英语听力模拟二_v2.zip`
     - `paper_packages/200001_hebei_high_school_english_listening/2000年1月河北高中英语听力模拟三_v3.zip`

## 实现细节

### 1. 分组键（groupKey）构建

```java
String groupKey = null;
if (paper.getYear() != null && paper.getProvince() != null && paper.getPaperType() != null) {
    if (paper.getMonth() != null) {
        groupKey = String.format("%d%02d_%s_%s", paper.getYear(), paper.getMonth(), paper.getProvince(), paper.getPaperType());
    } else {
        groupKey = String.format("%d_%s_%s", paper.getYear(), paper.getProvince(), paper.getPaperType());
    }
}
```

### 2. 文件名清理

```java
String safeFileName = paperName != null ? paperName.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9_\\-]", "_") : "paper";
```

**清理规则**：
- 保留：中文、英文字母、数字、下划线、横线
- 替换为下划线：其他所有特殊字符（如空格、括号、冒号等）

### 3. 路径构建

```java
if (groupKey != null) {
    return String.format("paper_packages/%s/%s_v%d.zip", groupKey, safeFileName, version);
} else {
    // 向后兼容格式
    ...
}
```

## 客户端下载

客户端通过 `/student/sync/paper/package/download` 接口下载，后端使用相同的 `buildPackageFileName` 方法构建路径，确保路径一致。

## 注意事项

1. **文件名唯一性**：由于 `paperName` 包含 `customName`，同一组合的不同记录会有不同的文件名，不会冲突。

2. **特殊字符处理**：`paperName` 中的特殊字符会被替换为下划线，确保文件名合法。

3. **版本管理**：同一试卷的不同版本使用 `_v{version}` 后缀区分。

4. **路径一致性**：上传和下载使用相同的路径构建逻辑，确保一致性。

## 是否需要调整？

**结论：不需要调整**

当前实现已经：
- ✅ 使用 `paperName` 作为文件名（符合要求）
- ✅ 使用 `(year, month, province, paper_type)` 作为分组目录（符合要求）
- ✅ 处理了 `month` 为 null 的情况
- ✅ 清理了特殊字符，确保文件名合法
- ✅ 上传和下载使用相同的路径构建逻辑

**唯一需要注意的点**：
- 如果 `paperName` 中包含大量特殊字符，可能会产生较长的文件名
- 建议在生成 `paperName` 时避免使用过多特殊字符



