-- ============================================
-- 更新 question_media 表，添加辅助识图类型支持
-- ============================================

-- 更新 media_type 字段的注释，添加辅助识图类型（3）
ALTER TABLE `question_media` 
MODIFY COLUMN `media_type` TINYINT NOT NULL COMMENT '媒体类型：1-题目媒体，2-选项媒体，3-辅助识图';



