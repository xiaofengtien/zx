-- 为 student_archive 表添加 grade 字段（学段：小学、中学、高中）
ALTER TABLE student_archive ADD COLUMN grade VARCHAR(50) COMMENT '学段（字典编码：grade）' AFTER current_grade;

-- 修改 current_grade 字段注释，说明使用字典编码
ALTER TABLE student_archive MODIFY COLUMN current_grade VARCHAR(50) COMMENT '当前年级（字典编码：根据grade联动）';

-- 修改 sex 字段注释，说明使用字典编码
ALTER TABLE student_archive MODIFY COLUMN sex VARCHAR(1) COMMENT '性别（字典编码：sys_user_sex）';

-- 修改 applicable_papers 字段注释，说明使用字典编码
ALTER TABLE student_archive MODIFY COLUMN applicable_papers VARCHAR(500) COMMENT '适用考卷（字典编码，多个用逗号分隔：适用试卷类型）';




