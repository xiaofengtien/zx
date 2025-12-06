-- ============================================
-- 添加试听旁白音频字段
-- 试听旁白音频：进入页面时自动播放的介绍音频
-- 试听音频：用户点击"播放试听音频"按钮时播放的音频
-- ============================================

-- 添加试听旁白音频字段
ALTER TABLE `paper`
ADD COLUMN `trial_intro_audio_url` VARCHAR(500) COMMENT '试听旁白音频URL（介绍音频，自动播放）' AFTER `trial_listen_audio_text`,
ADD COLUMN `trial_intro_audio_path` VARCHAR(500) COMMENT '试听旁白音频本地路径' AFTER `trial_intro_audio_url`,
ADD COLUMN `trial_intro_audio_duration` INT(11) COMMENT '试听旁白音频时长（秒）' AFTER `trial_intro_audio_path`;

-- ============================================
-- 完成字段添加
-- ============================================









