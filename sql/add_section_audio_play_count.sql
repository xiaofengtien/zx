-- 为大题表添加音频播放次数字段
ALTER TABLE `paper_section`
ADD COLUMN `audio_play_count` INT DEFAULT 1 COMMENT '音频播放次数（每道题有音频的情况下播放多少次，默认1次）' AFTER `instruction_audio_duration`;

-- 为字段添加索引（可选，如果需要按播放次数查询）
-- CREATE INDEX `idx_section_audio_play_count` ON `paper_section` (`audio_play_count`);









