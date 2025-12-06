-- ============================================
-- 修复 paper_section 表的关系结构
-- 将 volume_code 改为 volume_id（外键关联）
-- 关系：试卷 -> 卷别 -> 大题 -> 题目
-- ============================================

-- 1. 添加 volume_id 字段
ALTER TABLE `paper_section` 
ADD COLUMN `volume_id` INT COMMENT '所属卷别ID（关联paper_volume.id）' AFTER `paper_id`;

-- 2. 更新现有数据：将 volume_code 转换为 volume_id
UPDATE `paper_section` ps
INNER JOIN `paper_volume` pv ON ps.paper_id = pv.paper_id AND ps.volume_code = pv.volume_code
SET ps.volume_id = pv.id;

-- 3. 将 volume_id 设置为 NOT NULL（在数据迁移完成后）
-- ALTER TABLE `paper_section` MODIFY COLUMN `volume_id` INT NOT NULL COMMENT '所属卷别ID（关联paper_volume.id）';

-- 4. 添加外键约束（可选，根据实际情况决定是否添加）
-- ALTER TABLE `paper_section` 
-- ADD CONSTRAINT `fk_paper_section_volume` FOREIGN KEY (`volume_id`) REFERENCES `paper_volume` (`id`) ON DELETE CASCADE;

-- 5. 更新索引：移除旧的索引，添加新的索引
ALTER TABLE `paper_section` 
DROP INDEX IF EXISTS `idx_paper_section`,
ADD INDEX `idx_paper_section` (`paper_id`, `volume_id`, `section_order`),
ADD INDEX `idx_volume_id` (`volume_id`);

-- 6. volume_code 字段保留用于显示和兼容（可选，如果确定不需要可以删除）
-- 如果需要删除 volume_code 字段，执行以下语句：
-- ALTER TABLE `paper_section` DROP COLUMN `volume_code`;

-- 注意：
-- 1. 如果选择保留 volume_code，它仅用于显示和兼容，不再用于关联查询
-- 2. 如果选择删除 volume_code，需要确保所有代码都已更新为使用 volume_id
-- 3. 建议先保留 volume_code，等所有代码更新完成后再删除


