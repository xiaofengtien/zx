-- 试卷包生成任务表
-- 用于存储所有试卷包生成任务的历史记录，支持任务中心显示多条记录

CREATE TABLE IF NOT EXISTS `paper_package_task` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '任务ID（主键）',
  `paper_id` INT(11) NOT NULL COMMENT '试卷ID',
  `paper_name` VARCHAR(255) DEFAULT NULL COMMENT '试卷名称（用于任务显示）',
  `current_version` INT(11) DEFAULT NULL COMMENT '当前版本号（任务开始时试卷的版本号）',
  `new_version` INT(11) DEFAULT NULL COMMENT '生成的新版本号（成功时返回）',
  `status` VARCHAR(20) NOT NULL COMMENT '任务状态：PENDING-等待中，RUNNING-执行中，SUCCESS-成功，FAILED-失败，CANCELLED-已取消',
  `progress` INT(11) DEFAULT 0 COMMENT '进度（0-100）',
  `current_step` VARCHAR(500) DEFAULT NULL COMMENT '当前步骤描述',
  `error_message` TEXT DEFAULT NULL COMMENT '错误信息',
  `start_time` BIGINT(20) DEFAULT NULL COMMENT '开始时间（时间戳，毫秒）',
  `finish_time` BIGINT(20) DEFAULT NULL COMMENT '完成时间（时间戳，毫秒）',
  `upload_id` VARCHAR(255) DEFAULT NULL COMMENT 'OSS上传ID（用于断点续传）',
  `object_key` VARCHAR(500) DEFAULT NULL COMMENT 'OSS对象键（文件路径，用于断点续传）',
  `uploaded_parts` TEXT DEFAULT NULL COMMENT '已上传的分片列表（JSON字符串，用于断点续传）',
  `file_size` BIGINT(20) DEFAULT NULL COMMENT '文件大小（字节，用于断点续传）',
  `chunk_size` BIGINT(20) DEFAULT NULL COMMENT '分片大小（字节，用于断点续传）',
  `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  `del_flag` CHAR(1) DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_paper_id` (`paper_id`),
  KEY `idx_status` (`status`),
  KEY `idx_start_time` (`start_time`),
  KEY `idx_del_flag` (`del_flag`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试卷包生成任务表';









