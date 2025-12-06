-- ============================================
-- 试卷月份和省份字典数据
-- 创建时间：2024-01-XX
-- 说明：用于试卷管理的年月、地区字段
-- ============================================

-- 1. 插入月份字典类型
INSERT INTO `sys_dict_type` (`dict_name`, `dict_type`, `status`, `create_by`, `create_time`, `remark`) 
VALUES ('月份', 'paper_month', '0', 'admin', NOW(), '试卷月份字典（1-12月）')
ON DUPLICATE KEY UPDATE `dict_name`='月份', `remark`='试卷月份字典（1-12月）';

-- 2. 插入月份字典数据（1-12月）
INSERT INTO `sys_dict_data` (`dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `remark`) VALUES
(1, '1月', '1', 'paper_month', '', '', 'N', '0', 'admin', NOW(), ''),
(2, '2月', '2', 'paper_month', '', '', 'N', '0', 'admin', NOW(), ''),
(3, '3月', '3', 'paper_month', '', '', 'N', '0', 'admin', NOW(), ''),
(4, '4月', '4', 'paper_month', '', '', 'N', '0', 'admin', NOW(), ''),
(5, '5月', '5', 'paper_month', '', '', 'N', '0', 'admin', NOW(), ''),
(6, '6月', '6', 'paper_month', '', '', 'N', '0', 'admin', NOW(), ''),
(7, '7月', '7', 'paper_month', '', '', 'N', '0', 'admin', NOW(), ''),
(8, '8月', '8', 'paper_month', '', '', 'N', '0', 'admin', NOW(), ''),
(9, '9月', '9', 'paper_month', '', '', 'N', '0', 'admin', NOW(), ''),
(10, '10月', '10', 'paper_month', '', '', 'N', '0', 'admin', NOW(), ''),
(11, '11月', '11', 'paper_month', '', '', 'N', '0', 'admin', NOW(), ''),
(12, '12月', '12', 'paper_month', '', '', 'N', '0', 'admin', NOW(), '')
ON DUPLICATE KEY UPDATE `dict_label`=VALUES(`dict_label`), `dict_sort`=VALUES(`dict_sort`);

-- 3. 插入省份字典类型
INSERT INTO `sys_dict_type` (`dict_name`, `dict_type`, `status`, `create_by`, `create_time`, `remark`) 
VALUES ('省份', 'paper_province', '0', 'admin', NOW(), '试卷省份字典（中国省份简称）')
ON DUPLICATE KEY UPDATE `dict_name`='省份', `remark`='试卷省份字典（中国省份简称）';

-- 4. 插入省份字典数据（34个省级行政区）
-- 注意：dict_value使用拼音，dict_label显示简称
INSERT INTO `sys_dict_data` (`dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `remark`) VALUES
(1, '北京', 'beijing', 'paper_province', '', '', 'N', '0', 'admin', NOW(), ''),
(2, '天津', 'tianjin', 'paper_province', '', '', 'N', '0', 'admin', NOW(), ''),
(3, '河北', 'hebei', 'paper_province', '', '', 'N', '0', 'admin', NOW(), ''),
(4, '山西', 'shanxi', 'paper_province', '', '', 'N', '0', 'admin', NOW(), ''),
(5, '内蒙古', 'neimenggu', 'paper_province', '', '', 'N', '0', 'admin', NOW(), ''),
(6, '辽宁', 'liaoning', 'paper_province', '', '', 'N', '0', 'admin', NOW(), ''),
(7, '吉林', 'jilin', 'paper_province', '', '', 'N', '0', 'admin', NOW(), ''),
(8, '黑龙江', 'heilongjiang', 'paper_province', '', '', 'N', '0', 'admin', NOW(), ''),
(9, '上海', 'shanghai', 'paper_province', '', '', 'N', '0', 'admin', NOW(), ''),
(10, '江苏', 'jiangsu', 'paper_province', '', '', 'N', '0', 'admin', NOW(), ''),
(11, '浙江', 'zhejiang', 'paper_province', '', '', 'N', '0', 'admin', NOW(), ''),
(12, '安徽', 'anhui', 'paper_province', '', '', 'N', '0', 'admin', NOW(), ''),
(13, '福建', 'fujian', 'paper_province', '', '', 'N', '0', 'admin', NOW(), ''),
(14, '江西', 'jiangxi', 'paper_province', '', '', 'N', '0', 'admin', NOW(), ''),
(15, '山东', 'shandong', 'paper_province', '', '', 'N', '0', 'admin', NOW(), ''),
(16, '河南', 'henan', 'paper_province', '', '', 'N', '0', 'admin', NOW(), ''),
(17, '湖北', 'hubei', 'paper_province', '', '', 'N', '0', 'admin', NOW(), ''),
(18, '湖南', 'hunan', 'paper_province', '', '', 'N', '0', 'admin', NOW(), ''),
(19, '广东', 'guangdong', 'paper_province', '', '', 'N', '0', 'admin', NOW(), ''),
(20, '广西', 'guangxi', 'paper_province', '', '', 'N', '0', 'admin', NOW(), ''),
(21, '海南', 'hainan', 'paper_province', '', '', 'N', '0', 'admin', NOW(), ''),
(22, '重庆', 'chongqing', 'paper_province', '', '', 'N', '0', 'admin', NOW(), ''),
(23, '四川', 'sichuan', 'paper_province', '', '', 'N', '0', 'admin', NOW(), ''),
(24, '贵州', 'guizhou', 'paper_province', '', '', 'N', '0', 'admin', NOW(), ''),
(25, '云南', 'yunnan', 'paper_province', '', '', 'N', '0', 'admin', NOW(), ''),
(26, '西藏', 'xizang', 'paper_province', '', '', 'N', '0', 'admin', NOW(), ''),
(27, '陕西', 'shaanxi', 'paper_province', '', '', 'N', '0', 'admin', NOW(), ''),
(28, '甘肃', 'gansu', 'paper_province', '', '', 'N', '0', 'admin', NOW(), ''),
(29, '青海', 'qinghai', 'paper_province', '', '', 'N', '0', 'admin', NOW(), ''),
(30, '宁夏', 'ningxia', 'paper_province', '', '', 'N', '0', 'admin', NOW(), ''),
(31, '新疆', 'xinjiang', 'paper_province', '', '', 'N', '0', 'admin', NOW(), ''),
(32, '香港', 'xianggang', 'paper_province', '', '', 'N', '0', 'admin', NOW(), ''),
(33, '澳门', 'aomen', 'paper_province', '', '', 'N', '0', 'admin', NOW(), ''),
(34, '台湾', 'taiwan', 'paper_province', '', '', 'N', '0', 'admin', NOW(), '')
ON DUPLICATE KEY UPDATE `dict_label`=VALUES(`dict_label`), `dict_sort`=VALUES(`dict_sort`);



