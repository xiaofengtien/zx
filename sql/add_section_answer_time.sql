-- 为大题表添加作答时间字段
ALTER TABLE `paper_section`
ADD COLUMN `answer_time` INT DEFAULT 5 COMMENT '作答时间（单位：秒，默认5秒）' AFTER `instruction_text`;

