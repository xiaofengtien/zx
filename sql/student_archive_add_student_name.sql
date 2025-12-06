-- ============================================
-- 学员档案表增加姓名字段
-- ============================================

-- 增加 student_name 字段（姓名）
ALTER TABLE `student_archive` 
ADD COLUMN `student_name` VARCHAR(100) COMMENT '学员姓名' AFTER `name`;

-- 说明：
-- 1. name 字段：原有字段，可能存储学号或姓名（历史遗留）
-- 2. student_name 字段：新增字段，明确存储学员姓名
-- 3. student_account 字段：存储学号（考生号）
-- 4. 建议后续统一使用 student_name 存储姓名，student_account 存储学号


