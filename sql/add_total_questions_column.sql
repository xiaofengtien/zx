-- ============================================
-- 添加 total_questions 字段到 paper 表
-- 如果字段已存在，此脚本不会报错
-- ============================================

-- 检查并添加 total_questions 字段
ALTER TABLE `paper` 
ADD COLUMN IF NOT EXISTS `total_questions` INT DEFAULT 0 COMMENT '题目总数' AFTER `total_score`;

-- 更新现有数据的 total_questions（根据 paper_question 表统计）
UPDATE `paper` p
SET `total_questions` = (
    SELECT COUNT(*) 
    FROM `paper_question` pq 
    WHERE pq.`paper_id` = p.`id`
);



