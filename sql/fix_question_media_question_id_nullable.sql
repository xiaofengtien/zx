-- 修复 question_media 表的 question_id 字段，允许为 NULL
-- 因为现在有卷别音频（mediaType=7）、大题音频（mediaType=8）、中场音频（mediaType=9）、试听音频（mediaType=10）、试听图片（mediaType=11）等不需要关联题目的媒体类型

ALTER TABLE `question_media` 
MODIFY COLUMN `question_id` INT NULL COMMENT '题目ID（可为空，用于卷别音频、大题音频等不关联题目的媒体类型）';


