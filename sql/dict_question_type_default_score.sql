-- 题目类型默认分值字典配置
-- 字典类型：question_type_default_score

-- 1. 插入字典类型
INSERT INTO `sys_dict_type` (`dict_name`, `dict_type`, `status`, `create_by`, `create_time`, `remark`) 
VALUES ('题目类型默认分值', 'question_type_default_score', '0', 'admin', NOW(), '题目类型对应的默认分值配置，用于试卷设计时自动分配分值');

-- 2. 插入字典数据
-- 单选题（type=0）默认2分
INSERT INTO `sys_dict_data` (`dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `remark`) 
VALUES (1, '单选题', '0', 'question_type_default_score', '', '', 'N', '0', 'admin', NOW(), '单选题默认分值：2分');

-- 多选题（type=1）默认5分
INSERT INTO `sys_dict_data` (`dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `remark`) 
VALUES (2, '多选题', '1', 'question_type_default_score', '', '', 'N', '0', 'admin', NOW(), '多选题默认分值：5分');

-- 判断题（type=2）默认1分
INSERT INTO `sys_dict_data` (`dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `remark`) 
VALUES (3, '判断题', '2', 'question_type_default_score', '', '', 'N', '0', 'admin', NOW(), '判断题默认分值：1分');

-- 填空题（type=3）默认3分
INSERT INTO `sys_dict_data` (`dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `remark`) 
VALUES (4, '填空题', '3', 'question_type_default_score', '', '', 'N', '0', 'admin', NOW(), '填空题默认分值：3分');

-- 排序题（type=4）默认5分
INSERT INTO `sys_dict_data` (`dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `remark`) 
VALUES (5, '排序题', '4', 'question_type_default_score', '', '', 'N', '0', 'admin', NOW(), '排序题默认分值：5分');

-- 完形填空（type=5）默认10分
INSERT INTO `sys_dict_data` (`dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `remark`) 
VALUES (6, '完形填空', '5', 'question_type_default_score', '', '', 'N', '0', 'admin', NOW(), '完形填空默认分值：10分');

-- 注意：字典数据的 dict_value 存储的是题目类型值（0-5）
-- 实际的分值需要存储在 remark 字段中，或者使用 dict_label 存储分值
-- 这里建议使用 dict_label 存储 "题目类型名称:分值" 的格式，或者使用 remark 存储分值
-- 为了更好的扩展性，建议使用以下格式：

-- 重新设计：使用 dict_label 存储 "题目类型名称"，dict_value 存储 "类型值"，remark 存储 "默认分值"
-- 或者更简单的方式：dict_label 存储 "题目类型名称"，dict_value 存储 "类型值,默认分值"（如 "0,2.0"）

-- 推荐方案：使用 dict_value 存储 "类型值,默认分值" 格式
-- 更新上面的插入语句，使用 "类型值,默认分值" 格式

DELETE FROM `sys_dict_data` WHERE `dict_type` = 'question_type_default_score';
DELETE FROM `sys_dict_type` WHERE `dict_type` = 'question_type_default_score';

-- 重新插入字典类型
INSERT INTO `sys_dict_type` (`dict_name`, `dict_type`, `status`, `create_by`, `create_time`, `remark`) 
VALUES ('题目类型默认分值', 'question_type_default_score', '0', 'admin', NOW(), '题目类型对应的默认分值配置，格式：dict_value为"类型值,默认分值"，如"0,2.0"表示单选题默认2分');

-- 重新插入字典数据（使用 "类型值,默认分值" 格式）
INSERT INTO `sys_dict_data` (`dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `remark`) 
VALUES 
(1, '单选题', '0,2.0', 'question_type_default_score', '', '', 'N', '0', 'admin', NOW(), '单选题默认分值：2分'),
(2, '多选题', '1,5.0', 'question_type_default_score', '', '', 'N', '0', 'admin', NOW(), '多选题默认分值：5分'),
(3, '判断题', '2,1.0', 'question_type_default_score', '', '', 'N', '0', 'admin', NOW(), '判断题默认分值：1分'),
(4, '填空题', '3,3.0', 'question_type_default_score', '', '', 'N', '0', 'admin', NOW(), '填空题默认分值：3分'),
(5, '排序题', '4,5.0', 'question_type_default_score', '', '', 'N', '0', 'admin', NOW(), '排序题默认分值：5分'),
(6, '完形填空', '5,10.0', 'question_type_default_score', '', '', 'N', '0', 'admin', NOW(), '完形填空默认分值：10分');



