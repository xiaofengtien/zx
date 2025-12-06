-- 用户试卷练习次数重置表
-- 后台管理系统可以为用户添加重置记录，客户端同步时会清除对应的本地答题记录

DROP TABLE IF EXISTS app_user_paper_reset;

CREATE TABLE app_user_paper_reset (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    paper_id BIGINT COMMENT '试卷ID（为空表示重置该用户所有试卷）',
    reset_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '重置时间',
    reset_by VARCHAR(64) COMMENT '操作人',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_paper_id (paper_id),
    INDEX idx_reset_time (reset_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户试卷练习次数重置表';
