-- ============================================
-- 修复试听功能字段命名
-- 1. 新增试听文本字段
-- 2. 重命名操作提示字段（从 trial_listen_* 改为 operate_listen_*）
-- ============================================

-- 1. 新增试听文本字段
ALTER TABLE `paper`
ADD COLUMN `trial_listen_audio_text` TEXT COMMENT '试听文本内容（描述试听的内容）' AFTER `trial_listen_audio_duration`;

-- 2. 重命名操作提示字段
-- 注意：MySQL 不支持直接重命名列，需要先添加新列，复制数据，删除旧列

-- 2.1 添加新列
ALTER TABLE `paper`
ADD COLUMN `operate_listen_text` TEXT COMMENT '操作提示文本' AFTER `trial_listen_audio_text`,
ADD COLUMN `operate_listen_image_url` VARCHAR(500) COMMENT '操作提示图片URL' AFTER `operate_listen_text`,
ADD COLUMN `operate_listen_image_path` VARCHAR(500) COMMENT '操作提示图片本地路径' AFTER `operate_listen_image_url`;

-- 2.2 复制数据
UPDATE `paper`
SET 
  `operate_listen_text` = `trial_listen_text`,
  `operate_listen_image_url` = `trial_listen_image_url`,
  `operate_listen_image_path` = `trial_listen_image_path`
WHERE `trial_listen_text` IS NOT NULL 
   OR `trial_listen_image_url` IS NOT NULL 
   OR `trial_listen_image_path` IS NOT NULL;

-- 2.3 删除旧列
ALTER TABLE `paper`
DROP COLUMN `trial_listen_text`,
DROP COLUMN `trial_listen_image_url`,
DROP COLUMN `trial_listen_image_path`;

-- ============================================
-- 完成字段修复
-- ============================================









