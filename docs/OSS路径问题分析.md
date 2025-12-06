# OSS路径问题分析

## 问题路径

```
paper_packages/high_school_english_listening/2000年1月河北高中英语听力事实上_v17.zip
```

## 问题分析

这个路径使用的是**向后兼容格式**，说明 `groupKey` 为 null。

### 原因

`groupKey` 为 null 的条件（第358行）：
```java
if (paper.getYear() != null && paper.getProvince() != null && paper.getPaperType() != null) {
    // 构建 groupKey
} else {
    // groupKey 为 null，使用向后兼容格式
}
```

**可能的原因**：
1. **数据库中的 `year` 字段为 null**
2. **数据库中的 `province` 字段为 null**
3. **数据库中的 `paper_type` 字段为 null**（不太可能，因为路径中有 `high_school_english_listening`）

### 正确的路径应该是

对于"2000年1月河北高中英语听力"：
- year: 2000
- month: 1
- province: 河北（编码可能是 `hebei`）
- paper_type: `high_school_english_listening`

**正确路径**：
```
paper_packages/200001_hebei_high_school_english_listening/2000年1月河北高中英语听力事实上_v17.zip
```

## 解决方案

### 方案1：更新数据库字段（推荐）

如果这是旧数据，需要更新数据库中的 `year`, `month`, `province` 字段：

```sql
-- 根据 paper_name 或其他信息更新字段
UPDATE paper 
SET year = 2000, 
    month = 1, 
    province = 'hebei'  -- 根据实际情况调整省份编码
WHERE id = ?;  -- 替换为实际的试卷ID
```

### 方案2：重新生成试卷包

更新数据库字段后，重新生成试卷包，会自动使用新格式的路径。

## 检查步骤

1. **检查数据库字段**：
   ```sql
   SELECT id, paper_name, year, month, province, paper_type 
   FROM paper 
   WHERE paper_name LIKE '%2000年1月河北高中英语听力%';
   ```

2. **如果字段为 null**：
   - 更新字段值
   - 重新生成试卷包

3. **验证路径**：
   - 生成后检查日志中的路径
   - 确认路径格式为：`paper_packages/{year}{month}_{province}_{paper_type}/{paper_name}_v{version}.zip`

## 注意事项

- 旧数据可能需要手动更新 `year`, `month`, `province` 字段
- 更新后需要重新生成试卷包才能使用新路径
- 旧路径的文件仍然可以下载（向后兼容），但建议迁移到新格式



