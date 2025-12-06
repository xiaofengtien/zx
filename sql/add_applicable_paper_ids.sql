-- 为 student_archive 表添加 applicable_paper_ids 字段
-- 用于存储学员适用的具体试卷ID列表（JSON数组格式）

-- MySQL
ALTER TABLE `student_archive` 
ADD COLUMN `applicable_paper_ids` TEXT COMMENT '适用试卷ID列表（JSON数组，如：[1,2,3]）' AFTER `applicable_papers`;



