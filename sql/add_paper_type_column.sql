-- ============================================
-- 为paper表添加paper_type字段（试卷类型）
-- 创建时间：2024-XX-XX
-- 说明：用于区分试卷类型（如：中考英语、高考英语），与学员档案的适用试卷类型匹配
-- ============================================

-- 1. 为paper表添加paper_type字段
ALTER TABLE `paper` 
ADD COLUMN `paper_type` VARCHAR(50) COMMENT '试卷类型（字典编码：paper_type，如：junior_high_school_english_listening、high_school_english_listening）' 
AFTER `paper_code`;

-- 2. 添加索引（用于根据试卷类型查询）
ALTER TABLE `paper` 
ADD INDEX `idx_paper_type` (`paper_type`);

-- 3. 添加联合索引（用于根据试卷类型和状态查询）
ALTER TABLE `paper` 
ADD INDEX `idx_paper_type_status` (`paper_type`, `status`);

-- 4. 更新现有数据的paper_type（可选，根据实际情况设置默认值）
-- 注意：如果现有试卷没有设置试卷类型，可以手动更新或保持NULL
-- UPDATE `paper` SET `paper_type` = 'default' WHERE `paper_type` IS NULL;

-- 5. 验证字段是否添加成功
-- SELECT COLUMN_NAME, DATA_TYPE, COLUMN_COMMENT 
-- FROM INFORMATION_SCHEMA.COLUMNS 
-- WHERE TABLE_SCHEMA = DATABASE() 
--   AND TABLE_NAME = 'paper' 
--   AND COLUMN_NAME = 'paper_type';


