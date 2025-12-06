-- 题目类型和学科字典配置SQL
-- 执行前请确认 sys_dict_type 表中是否已存在对应的字典类型

-- 1. 添加题目类型字典类型（如果不存在）
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '题目类型', 'question_type', '0', 'admin', NOW(), '题目类型：0-单选题,1-多选题,2-判断题,3-填空题,4-排序题,5-完形填空'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_dict_type WHERE dict_type = 'question_type'
);

-- 2. 添加题目类型字典数据
-- 获取字典类型ID
SET @dict_type_id = (SELECT dict_id FROM sys_dict_type WHERE dict_type = 'question_type');

-- 删除已存在的字典数据（如果存在）
DELETE FROM sys_dict_data WHERE dict_type = 'question_type';

-- 插入题目类型字典数据
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
VALUES
(1, '单选题', '0', 'question_type', '', '', 'N', '0', 'admin', NOW(), '单选题'),
(2, '多选题', '1', 'question_type', '', '', 'N', '0', 'admin', NOW(), '多选题'),
(3, '判断题', '2', 'question_type', '', '', 'N', '0', 'admin', NOW(), '判断题'),
(4, '填空题', '3', 'question_type', '', '', 'N', '0', 'admin', NOW(), '填空题'),
(5, '排序题', '4', 'question_type', '', '', 'N', '0', 'admin', NOW(), '排序题'),
(6, '完形填空', '5', 'question_type', '', '', 'N', '0', 'admin', NOW(), '完形填空');

-- 3. 添加学科字典类型（如果不存在）
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '学科', 'subject', '0', 'admin', NOW(), '学科字典'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_dict_type WHERE dict_type = 'subject'
);

-- 4. 添加学科字典数据（示例，请根据实际学科配置）
-- 删除已存在的字典数据（如果存在）
DELETE FROM sys_dict_data WHERE dict_type = 'subject';

-- 插入学科字典数据（示例数据，请根据实际情况修改）
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
VALUES
(1, '语文', '1', 'subject', '', '', 'N', '0', 'admin', NOW(), '语文学科'),
(2, '数学', '2', 'subject', '', '', 'N', '0', 'admin', NOW(), '数学学科'),
(3, '英语', '3', 'subject', '', '', 'N', '0', 'admin', NOW(), '英语学科'),
(4, '物理', '4', 'subject', '', '', 'N', '0', 'admin', NOW(), '物理学科'),
(5, '化学', '5', 'subject', '', '', 'N', '0', 'admin', NOW(), '化学学科'),
(6, '生物', '6', 'subject', '', '', 'N', '0', 'admin', NOW(), '生物学科'),
(7, '历史', '7', 'subject', '', '', 'N', '0', 'admin', NOW(), '历史学科'),
(8, '地理', '8', 'subject', '', '', 'N', '0', 'admin', NOW(), '地理学科'),
(9, '政治', '9', 'subject', '', '', 'N', '0', 'admin', NOW(), '政治学科');

-- 注意：
-- 1. 字典值（dict_value）必须与后端代码中的值匹配
-- 2. 题目类型的值：0,1,2,3,4,5（对应单选题、多选题、判断题、填空题、排序题、完形填空）
-- 3. 学科的值：根据实际学科ID配置（示例中使用1-9，请根据实际情况调整）
-- 4. 执行此SQL后，需要清除Redis缓存或重启应用才能生效



