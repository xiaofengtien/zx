-- 为 student_archive 表添加 seat_number 字段（考试机位）
ALTER TABLE student_archive ADD COLUMN seat_number VARCHAR(50) COMMENT '考试机位号' AFTER hometown;


