-- ============================================
-- 添加试卷启用时间字段
-- 创建时间：2025-01-XX
-- 说明：为paper表添加enable_start_time和enable_end_time字段，用于控制试卷的启用时间段
-- ============================================

-- 添加试卷启用开始时间字段
ALTER TABLE `paper` 
ADD COLUMN `enable_start_time` DATETIME COMMENT '试卷启用开始时间（为空表示不限制开始时间）' AFTER `notes_display_mode`;

-- 添加试卷启用结束时间字段
ALTER TABLE `paper` 
ADD COLUMN `enable_end_time` DATETIME COMMENT '试卷启用结束时间（为空表示不限制结束时间）' AFTER `enable_start_time`;

-- 添加索引（可选，用于查询优化）
CREATE INDEX `idx_paper_enable_time` ON `paper` (`enable_start_time`, `enable_end_time`);

-- ============================================
-- 完成：试卷启用时间字段添加
-- ============================================









