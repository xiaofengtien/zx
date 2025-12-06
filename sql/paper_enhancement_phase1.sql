-- ============================================
-- 试卷功能增强 - Phase 1: 数据库表结构调整
-- ============================================

-- ============================================
-- 1. Question表（题目表）字段调整
-- ============================================
-- 移除音频相关字段（改为通过question_media表关联）
-- 添加答题讲解相关字段

ALTER TABLE `question` 
ADD COLUMN `explanation_enabled` TINYINT(1) DEFAULT 0 COMMENT '是否有答题讲解（0-否，1-是）' AFTER `analysis`,
ADD COLUMN `explanation_text` TEXT COMMENT '讲解文字' AFTER `explanation_enabled`,
ADD COLUMN `explanation_delay_seconds` INT DEFAULT 2 COMMENT '讲解显示延迟（秒，播放完试卷名称音频后等待时间，默认2秒）' AFTER `explanation_text`;

-- 注意：如果Question表中存在以下字段，需要先移除（根据实际情况调整）
-- ALTER TABLE `question` DROP COLUMN `question_audio_url`;
-- ALTER TABLE `question` DROP COLUMN `question_audio_path`;
-- ALTER TABLE `question` DROP COLUMN `question_audio_duration`;
-- ALTER TABLE `question` DROP COLUMN `explanation_audio_url`;
-- ALTER TABLE `question` DROP COLUMN `explanation_audio_path`;
-- ALTER TABLE `question` DROP COLUMN `explanation_audio_duration`;

-- ============================================
-- 2. QuestionMedia表（题目媒体表）字段扩展
-- ============================================
-- 添加关联字段，支持关联到paper、paper_volume、paper_section、paper_intermission

ALTER TABLE `question_media` 
ADD COLUMN `paper_id` INT COMMENT '试卷ID（关联paper.id，用于试听媒体等）' AFTER `question_id`,
ADD COLUMN `volume_id` INT COMMENT '卷别ID（关联paper_volume.id，用于卷别名称音频）' AFTER `paper_id`,
ADD COLUMN `section_id` INT COMMENT '大题ID（关联paper_section.id，用于大题说明音频）' AFTER `volume_id`,
ADD COLUMN `intermission_id` INT COMMENT '中场配置ID（关联paper_intermission.id，用于中场音频）' AFTER `section_id`,
ADD INDEX `idx_paper_id` (`paper_id`),
ADD INDEX `idx_volume_id` (`volume_id`),
ADD INDEX `idx_section_id` (`section_id`),
ADD INDEX `idx_intermission_id` (`intermission_id`);

-- media_type 类型说明（现有类型保持不变，新增类型如下）：
-- 1-题目媒体（题目内容相关的图片、视频等）
-- 2-选项媒体（选项相关的音频、图片等）
-- 3-辅助识图
-- 4-题目音频（题目本身的音频，如听力题的题目音频）
-- 5-讲解音频（答题讲解的音频）
-- 6-讲解图片（答题讲解的图片，如果需要）
-- 7-卷别名称音频（试卷A、试卷B等的名称音频，关联到paper_volume）
-- 8-大题说明音频（第一节、第二节等的说明音频，关联到paper_section）
-- 9-中场音频（卷间中场音频，关联到paper_intermission）
-- 10-试听音频（试听示例音频，关联到paper）
-- 11-试听图片（试听示例图片，关联到paper）

-- ============================================
-- 3. PaperVolume表（试卷卷别表）- 新建
-- ============================================

CREATE TABLE IF NOT EXISTS `paper_volume` (
  `id` INT PRIMARY KEY AUTO_INCREMENT COMMENT '卷别ID',
  `paper_id` INT NOT NULL COMMENT '试卷ID',
  `volume_code` VARCHAR(10) NOT NULL COMMENT '卷别代码（A、B、C等）',
  `volume_name` VARCHAR(100) NOT NULL COMMENT '卷别名称（如：试卷A、试卷B）',
  `volume_order` INT NOT NULL COMMENT '卷别顺序（1,2,3...）',
  `volume_audio_url` VARCHAR(500) COMMENT '卷别名称音频URL（播放试卷名称）',
  `volume_audio_path` VARCHAR(500) COMMENT '卷别名称音频本地路径',
  `volume_audio_duration` INT COMMENT '卷别名称音频时长（秒）',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX `idx_paper_volume` (`paper_id`, `volume_order`),
  UNIQUE KEY `uk_paper_volume` (`paper_id`, `volume_code`)
) COMMENT='试卷卷别表';

-- ============================================
-- 4. PaperSection表（大题表）- 新建
-- ============================================

CREATE TABLE IF NOT EXISTS `paper_section` (
  `id` INT PRIMARY KEY AUTO_INCREMENT COMMENT '大题ID',
  `paper_id` INT NOT NULL COMMENT '试卷ID',
  `volume_id` INT NOT NULL COMMENT '所属卷别ID（关联paper_volume.id）',
  `volume_code` VARCHAR(10) COMMENT '卷别代码（A、B、C等，保留用于显示和兼容）',
  `section_name` VARCHAR(100) NOT NULL COMMENT '大题名称（如：第一节、第二节）',
  `section_order` INT NOT NULL COMMENT '大题顺序（1,2,3...，在同一卷内排序）',
  `question_count` INT NOT NULL DEFAULT 0 COMMENT '题目数量（自动计算）',
  `total_score` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '总分（自动计算）',
  `score_per_question` DECIMAL(10,2) COMMENT '每题分数（如果统一）',
  `instruction_text` TEXT COMMENT '大题说明文字',
  `instruction_audio_url` VARCHAR(500) COMMENT '大题说明音频URL',
  `instruction_audio_path` VARCHAR(500) COMMENT '大题说明音频本地路径',
  `instruction_audio_duration` INT COMMENT '大题说明音频时长（秒）',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX `idx_paper_section` (`paper_id`, `volume_id`, `section_order`),
  INDEX `idx_volume_id` (`volume_id`)
) COMMENT='试卷大题表';

-- ============================================
-- 5. PaperIntermission表（卷间中场配置表）- 新建
-- ============================================

CREATE TABLE IF NOT EXISTS `paper_intermission` (
  `id` INT PRIMARY KEY AUTO_INCREMENT COMMENT '中场配置ID',
  `paper_id` INT NOT NULL COMMENT '试卷ID',
  `from_volume` VARCHAR(10) NOT NULL COMMENT '来源卷别（如：A）',
  `to_volume` VARCHAR(10) NOT NULL COMMENT '目标卷别（如：B）',
  `intermission_text` TEXT COMMENT '中场提示文案',
  `intermission_audio_url` VARCHAR(500) COMMENT '中场音频URL',
  `intermission_audio_path` VARCHAR(500) COMMENT '中场音频本地路径',
  `intermission_audio_duration` INT COMMENT '中场音频时长（秒）',
  `can_skip` TINYINT(1) DEFAULT 0 COMMENT '是否可以跳过（0-否，1-是，当前统一为0）',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX `idx_paper_intermission` (`paper_id`, `from_volume`, `to_volume`),
  UNIQUE KEY `uk_paper_intermission` (`paper_id`, `from_volume`, `to_volume`)
) COMMENT='卷间中场配置表';

-- ============================================
-- 6. PaperQuestion表（试卷-题目关联表）字段调整
-- ============================================
-- 添加大题归属字段

ALTER TABLE `paper_question` 
ADD COLUMN `section_id` INT COMMENT '所属大题ID（关联paper_section.id）' AFTER `question_id`,
ADD COLUMN `section_order` INT COMMENT '在大题中的顺序（可拖拽调整，后台配置时确定）' AFTER `section_id`,
ADD INDEX `idx_section_id` (`section_id`),
ADD INDEX `idx_section_order` (`section_id`, `section_order`);

-- ============================================
-- 7. AppUserPaperInfo表（用户答题记录表）字段调整
-- ============================================
-- 添加练习次数统计、多卷状态、机位信息等字段

ALTER TABLE `app_user_paper_info` 
ADD COLUMN `practice_count` INT DEFAULT 0 COMMENT '已练习次数' AFTER `user_score`,
ADD COLUMN `last_practice_time` DATETIME COMMENT '最后练习时间' AFTER `practice_count`,
ADD COLUMN `volume_status` TEXT COMMENT '各卷状态（JSON对象，如：{"A":"completed","B":"in_progress"}）' AFTER `last_practice_time`,
ADD COLUMN `volume_submit_time` TEXT COMMENT '各卷提交时间（JSON对象，如：{"A":"2024-01-01 10:00:00","B":null}）' AFTER `volume_status`,
ADD COLUMN `intermission_played` TEXT COMMENT '中场音频播放状态（JSON对象，如：{"A->B":true}）' AFTER `volume_submit_time`,
ADD COLUMN `assigned_seat_number` VARCHAR(50) COMMENT '分配的机位号（后台配置）' AFTER `intermission_played`,
ADD COLUMN `actual_seat_number` VARCHAR(50) COMMENT '实际坐的机位号（可能发生变化，暂时不处理）' AFTER `assigned_seat_number`;

-- ============================================
-- 8. Paper表（试卷表）字段调整
-- ============================================
-- 添加练习次数限制、试听功能、注意事项等字段
-- 注意：year, month, province 字段保留但隐藏（不再必填）

ALTER TABLE `paper` 
ADD COLUMN `practice_limit` INT DEFAULT 0 COMMENT '限制练习次数（0表示不限制，按试卷提交次数计算）' AFTER `allow_review`,
ADD COLUMN `trial_listen_enabled` TINYINT(1) DEFAULT 1 COMMENT '是否启用试听（0-否，1-是）' AFTER `practice_limit`,
ADD COLUMN `trial_listen_text` TEXT COMMENT '试听提示文案' AFTER `trial_listen_enabled`,
ADD COLUMN `trial_listen_image_url` VARCHAR(500) COMMENT '试听图片示例URL' AFTER `trial_listen_text`,
ADD COLUMN `trial_listen_image_path` VARCHAR(500) COMMENT '试听图片示例本地路径' AFTER `trial_listen_image_url`,
ADD COLUMN `trial_listen_audio_url` VARCHAR(500) COMMENT '试听音频示例URL' AFTER `trial_listen_image_path`,
ADD COLUMN `trial_listen_audio_path` VARCHAR(500) COMMENT '试听音频示例本地路径' AFTER `trial_listen_audio_url`,
ADD COLUMN `trial_listen_audio_duration` INT COMMENT '试听音频时长（秒）' AFTER `trial_listen_audio_path`,
ADD COLUMN `notes` TEXT COMMENT '注意事项（支持富文本）' AFTER `trial_listen_audio_duration`,
ADD COLUMN `notes_display_mode` VARCHAR(20) DEFAULT 'before_exam' COMMENT '注意事项显示时机（before_exam-考试前，before_section-每大题前）' AFTER `notes`;

-- ============================================
-- 完成 Phase 1: 数据库表结构调整
-- ============================================


