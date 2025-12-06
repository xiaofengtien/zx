-- 为 paper_intermission 表添加 from_volume_id 和 to_volume_id 字段
-- 因为 volumeCode 会重复，需要通过 volumeId 来唯一标识卷别

ALTER TABLE `paper_intermission`
ADD COLUMN `from_volume_id` INT COMMENT '来源卷别ID（关联paper_volume.id）' AFTER `paper_id`,
ADD COLUMN `to_volume_id` INT COMMENT '目标卷别ID（关联paper_volume.id）' AFTER `from_volume_id`;

-- 添加索引
ALTER TABLE `paper_intermission`
ADD INDEX `idx_from_volume_id` (`from_volume_id`),
ADD INDEX `idx_to_volume_id` (`to_volume_id`),
ADD INDEX `idx_paper_volumes` (`paper_id`, `from_volume_id`, `to_volume_id`);

-- 注意：from_volume 和 to_volume 字段保留，用于显示和兼容性，但不再用于关联查询









