-- ============================================
-- 考试业务表结构DDL脚本
-- 创建时间：2024-01-XX
-- 说明：用于实现试卷管理、题目关联、媒体文件存储等功能
-- ============================================

-- 1. 创建paper表（试卷主表）
CREATE TABLE IF NOT EXISTS `paper` (
  `id` INT PRIMARY KEY AUTO_INCREMENT COMMENT '试卷ID',
  `paper_name` VARCHAR(255) NOT NULL COMMENT '试卷名称',
  `paper_code` VARCHAR(100) UNIQUE COMMENT '试卷编码（用于同步，格式：PAPER_YYYYMMDD_序号）',
  `paper_desc` TEXT COMMENT '试卷描述',
  `business_type` INT NOT NULL DEFAULT 5 COMMENT '业务类型（5-题库）',
  `business_id` INT COMMENT '业务ID（关联question_category_business_settings.id）',
  `total_score` DECIMAL(10,2) DEFAULT 0 COMMENT '总分',
  `total_questions` INT DEFAULT 0 COMMENT '题目总数',
  `duration` INT COMMENT '考试时长（分钟）',
  `intro_audio_url` VARCHAR(500) COMMENT '开场独白音频URL',
  `intro_audio_path` VARCHAR(500) COMMENT '开场独白音频路径（服务器）',
  `intro_audio_duration` INT COMMENT '开场独白时长（秒）',
  `intro_text` TEXT COMMENT '开场独白文本内容（可选）',
  `auto_next_question` TINYINT DEFAULT 1 COMMENT '是否自动跳转下一题：1-是，0-否',
  `show_answer_immediately` TINYINT DEFAULT 0 COMMENT '是否立即显示答案：1-是，0-否',
  `allow_review` TINYINT DEFAULT 1 COMMENT '是否允许回顾：1-是，0-否',
  `question_read_duration` INT COMMENT '每题读题时长（秒，用于自动跳转）',
  `version` INT DEFAULT 1 COMMENT '版本号（用于增量同步）',
  `package_hash` VARCHAR(64) COMMENT '数据包哈希值（SHA256）',
  `package_size` BIGINT COMMENT '数据包大小（字节）',
  `last_package_time` DATETIME COMMENT '最后打包时间',
  `status` TINYINT DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
  `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` VARCHAR(500) DEFAULT '' COMMENT '备注',
  INDEX `idx_paper_code` (`paper_code`),
  INDEX `idx_business` (`business_type`, `business_id`),
  INDEX `idx_status` (`status`),
  INDEX `idx_version` (`version`)
) COMMENT='试卷表';

-- 2. 创建paper_question表（试卷-题目关联表）
CREATE TABLE IF NOT EXISTS `paper_question` (
  `id` INT PRIMARY KEY AUTO_INCREMENT COMMENT '关联ID',
  `paper_id` INT NOT NULL COMMENT '试卷ID',
  `question_id` INT NOT NULL COMMENT '题目ID',
  `sort_order` INT DEFAULT 0 COMMENT '排序号（题目在试卷中的顺序）',
  `score` DECIMAL(10,2) DEFAULT 0 COMMENT '分值',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE KEY `uk_paper_question` (`paper_id`, `question_id`),
  INDEX `idx_paper_id` (`paper_id`),
  INDEX `idx_question_id` (`question_id`),
  INDEX `idx_sort_order` (`paper_id`, `sort_order`)
) COMMENT='试卷题目关联表';

-- 3. 创建question_media表（题目媒体文件表）
CREATE TABLE IF NOT EXISTS `question_media` (
  `id` INT PRIMARY KEY AUTO_INCREMENT COMMENT '媒体ID',
  `question_id` INT NOT NULL COMMENT '题目ID',
  `media_type` TINYINT NOT NULL COMMENT '媒体类型：1-题目媒体，2-选项媒体，3-辅助识图',
  `option_id` INT COMMENT '选项ID（如果是选项媒体，关联question_answer.id）',
  `blank_area_id` INT COMMENT '完形填空区域ID（如果是完形填空的选项媒体）',
  `media_name` VARCHAR(255) NOT NULL COMMENT '媒体文件名',
  `media_path` VARCHAR(500) COMMENT '媒体路径（服务器路径，用于在线）',
  `media_url` VARCHAR(500) COMMENT '访问URL（CDN/OSS）',
  `media_size` INT COMMENT '文件大小（字节）',
  `media_format` VARCHAR(50) COMMENT '文件格式：jpg/png/mp3/mp4等',
  `media_duration` INT COMMENT '媒体时长（秒，音频/视频）',
  `is_compressed` TINYINT DEFAULT 0 COMMENT '是否压缩：1-是，0-否',
  `storage_type` TINYINT DEFAULT 0 COMMENT '存储类型：0-在线路径，1-SQLite BLOB，2-文件系统',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX `idx_question_id` (`question_id`),
  INDEX `idx_option_id` (`option_id`),
  INDEX `idx_blank_area_id` (`blank_area_id`),
  INDEX `idx_media_type` (`question_id`, `media_type`)
) COMMENT='题目媒体文件表';

-- 4. 调整app_user_paper_info表（添加新字段）
-- 注意：使用 IF NOT EXISTS 语法在某些MySQL版本可能不支持，如果执行报错，请手动检查字段是否存在
ALTER TABLE `app_user_paper_info` 
ADD COLUMN `paper_id` INT COMMENT '试卷ID（关联paper表）' AFTER `business_id`,
ADD COLUMN `start_time` DATETIME COMMENT '开始答题时间',
ADD COLUMN `submit_time` DATETIME COMMENT '提交时间',
ADD COLUMN `used_time` INT COMMENT '用时（秒）',
ADD COLUMN `total_score` DECIMAL(10,2) DEFAULT 0 COMMENT '总分',
ADD COLUMN `user_score` DECIMAL(10,2) DEFAULT 0 COMMENT '得分',
ADD COLUMN `correct_count` INT DEFAULT 0 COMMENT '正确题数',
ADD COLUMN `wrong_count` INT DEFAULT 0 COMMENT '错误题数',
ADD COLUMN `is_submit` TINYINT DEFAULT 0 COMMENT '是否提交：1-是，0-否',
ADD COLUMN `sync_status` TINYINT DEFAULT 0 COMMENT '同步状态：0-未同步，1-已同步，2-同步失败',
ADD COLUMN `sync_time` DATETIME COMMENT '同步时间';

-- 添加索引（如果不存在会报错，可以忽略）
-- 如果索引已存在，执行会报错，可以忽略
CREATE INDEX `idx_paper_id` ON `app_user_paper_info` (`paper_id`);
CREATE INDEX `idx_app_user_id` ON `app_user_paper_info` (`appUserId`);
CREATE INDEX `idx_sync_status` ON `app_user_paper_info` (`sync_status`);

-- 5. 调整app_user_paper_question_result表（添加新字段）
ALTER TABLE `app_user_paper_question_result`
ADD COLUMN `answer_time` DATETIME COMMENT '答题时间',
ADD COLUMN `time_spent` INT COMMENT '用时（秒）',
ADD COLUMN `is_reviewed` TINYINT DEFAULT 0 COMMENT '是否回顾过：1-是，0-否',
ADD COLUMN `sync_status` TINYINT DEFAULT 0 COMMENT '同步状态：0-未同步，1-已同步';

-- 添加索引
CREATE INDEX `idx_paper_user` ON `app_user_paper_question_result` (`paperId`, `appUserId`);
CREATE INDEX `idx_sync_status` ON `app_user_paper_question_result` (`sync_status`);

-- 6. 调整app_user_paper_question_blank_result表（添加新字段）
ALTER TABLE `app_user_paper_question_blank_result`
ADD COLUMN `answer_time` DATETIME COMMENT '答题时间',
ADD COLUMN `time_spent` INT COMMENT '用时（秒）',
ADD COLUMN `sync_status` TINYINT DEFAULT 0 COMMENT '同步状态：0-未同步，1-已同步';

-- 添加索引
CREATE INDEX `idx_sync_status` ON `app_user_paper_question_blank_result` (`sync_status`);

-- 7. 初始化question_category_business_settings（默认类型为5）
-- 为所有启用的题目分类创建默认业务设置
INSERT INTO `question_category_business_settings` 
(`business_id`, `business_type`, `question_method`, `question_num`, `status`, `create_time`)
SELECT 
  qc.id AS business_id,
  5 AS business_type,
  1 AS question_method,
  10 AS question_num,
  1 AS status,
  NOW() AS create_time
FROM `question_category` qc
WHERE qc.status = 0  -- 只初始化启用状态的分类
  AND NOT EXISTS (
    SELECT 1 FROM `question_category_business_settings` qcbs 
    WHERE qcbs.business_id = qc.id AND qcbs.business_type = 5
  );

-- ============================================
-- 脚本执行完成
-- ============================================

