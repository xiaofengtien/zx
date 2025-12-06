-- 为 student_archive 表添加 pwd_update_date 字段（密码最后更新时间）
-- 用于实现首次登录强制修改密码功能

ALTER TABLE student_archive ADD COLUMN pwd_update_date DATETIME COMMENT '密码最后更新时间' AFTER del_flag;

-- 为已有数据设置默认值（如果密码已存在，设置为创建时间；如果密码为空，设置为NULL）
-- 注意：这里假设已有学员的密码都是默认密码，需要首次登录时修改
UPDATE student_archive SET pwd_update_date = NULL WHERE pwd_update_date IS NULL;



