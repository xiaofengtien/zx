-- 添加 user_id 字段，关联 sys_user 表
ALTER TABLE student_archive
ADD COLUMN user_id BIGINT(20) DEFAULT NULL COMMENT '关联系统用户ID（sys_user.user_id）';

-- 添加索引
CREATE INDEX idx_user_id ON student_archive(user_id);




