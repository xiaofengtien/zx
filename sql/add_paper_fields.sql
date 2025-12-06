-- 为 paper 表添加年份、月份、省份、自定义名称字段
-- 注意：不添加唯一索引，允许同一 (year, month, province, paper_type) 组合创建多条记录
-- 客户端同步时会选择该组合中版本最新的记录

-- 1. 添加字段
ALTER TABLE `paper` 
ADD COLUMN `year` INT(4) NULL COMMENT '年份（2000-2050）' AFTER `paper_type`,
ADD COLUMN `month` INT(2) NULL COMMENT '月份（1-12）' AFTER `year`,
ADD COLUMN `province` VARCHAR(50) NULL COMMENT '省份编码（字典值：paper_province）' AFTER `month`,
ADD COLUMN `custom_name` VARCHAR(100) NULL COMMENT '自定义名称' AFTER `province`;

-- 2. 添加普通索引（非唯一）：用于查询和排序
-- 注意：不添加唯一索引，允许同一组合创建多条记录（通过 custom_name 区分）
ALTER TABLE `paper` 
ADD INDEX `idx_paper_year_month_province_type` (`year`, `month`, `province`, `paper_type`);

-- 3. 为现有数据迁移（可选，如果有现有数据需要从 paper_code 解析）
-- 注意：此步骤需要根据实际情况调整，如果 paper_code 格式不统一，可能需要手动处理
-- UPDATE paper SET year = SUBSTRING(paper_code, 1, 4) WHERE year IS NULL;
-- UPDATE paper SET month = SUBSTRING(paper_code, 5, 2) WHERE month IS NULL;
-- 等等...

