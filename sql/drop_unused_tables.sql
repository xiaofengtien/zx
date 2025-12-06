-- ============================================
-- 删除题目业务中无用的表（如果确认不再使用）
-- ============================================
-- 注意：执行前请确认这些表是否在其他业务中使用
-- 如果只删除题目业务相关的数据，请使用 DELETE 而不是 DROP TABLE

-- 1. 删除 app_material_business_ref 表中题目业务相关的数据
-- 题目业务类型：10-题目媒体，15-答案媒体，20-辅助识图
DELETE FROM app_material_business_ref 
WHERE business_type IN (10, 15, 20);

-- 2. 如果确认 app_material 和 app_material_business_ref 表完全不再使用，可以删除表
-- 注意：请先确认是否有其他业务使用这些表
-- DROP TABLE IF EXISTS app_material_business_ref;
-- DROP TABLE IF EXISTS app_material;

-- 3. 如果确认 basic_config 表完全不再使用，可以删除表
-- 注意：请先确认是否有其他业务使用此表（如学科配置等）
-- DROP TABLE IF EXISTS basic_config;

-- 4. 删除题目业务相关的业务类型枚举值（已在代码中注释）
-- 业务类型：10-题目媒体，15-答案媒体，20-辅助识图
-- 这些枚举值已在 BusinessTypeEnum 中注释，数据库中的历史数据可以保留或删除

