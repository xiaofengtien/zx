-- ============================================
-- 学员角色和权限配置DML语句
-- ============================================
-- 说明：此文件用于配置学员角色和答题页面权限
-- 执行顺序：按照文件中的顺序依次执行
-- ============================================

-- ----------------------------
-- 1. 创建学员角色
-- ----------------------------
-- 角色名称：学员
-- 角色标识：student
-- 角色ID：使用自增，建议从100开始（避免与系统角色冲突）
INSERT INTO sys_role (
    role_name, 
    role_key, 
    role_sort, 
    data_scope, 
    menu_check_strictly, 
    dept_check_strictly, 
    status, 
    del_flag, 
    create_by, 
    create_time, 
    remark
) VALUES (
    '学员', 
    'student', 
    3, 
    '1', 
    1, 
    1, 
    '0', 
    '0', 
    'admin', 
    NOW(), 
    '学员角色，只能访问答题页面'
);

-- 获取刚插入的角色ID（假设为100，实际执行时请根据实际情况调整）
-- SET @student_role_id = LAST_INSERT_ID();
-- 或者手动指定角色ID（如果知道的话）
-- SET @student_role_id = 100;

-- ----------------------------
-- 2. 创建答题页面菜单
-- ----------------------------
-- 菜单类型说明：
-- M = 目录
-- C = 菜单
-- F = 按钮

-- 2.1 创建答题模块目录（一级菜单）
INSERT INTO sys_menu (
    menu_name, 
    parent_id, 
    order_num, 
    path, 
    component, 
    is_frame, 
    is_cache, 
    menu_type, 
    visible, 
    status, 
    perms, 
    icon, 
    create_by, 
    create_time, 
    remark
) VALUES (
    '答题中心', 
    0, 
    5, 
    'exam', 
    NULL, 
    1, 
    0, 
    'M', 
    '0', 
    '0', 
    NULL, 
    'education', 
    'admin', 
    NOW(), 
    '答题中心目录'
);

-- 获取答题中心菜单ID（假设为2000，实际执行时请根据实际情况调整）
-- SET @exam_menu_id = LAST_INSERT_ID();
-- 或者手动指定菜单ID
-- SET @exam_menu_id = 2000;

-- 2.2 创建答题页面（二级菜单）
-- TODO: 请根据实际菜单ID调整 parent_id
INSERT INTO sys_menu (
    menu_name, 
    parent_id, 
    order_num, 
    path, 
    component, 
    is_frame, 
    is_cache, 
    menu_type, 
    visible, 
    status, 
    perms, 
    icon, 
    create_by, 
    create_time, 
    remark
) VALUES (
    '答题页面', 
    2000,  -- TODO: 请替换为实际的答题中心菜单ID
    1, 
    'paper', 
    'exam/paper/index', 
    1, 
    0, 
    'C', 
    '0', 
    '0', 
    'exam:paper:view', 
    'edit', 
    'admin', 
    NOW(), 
    '答题页面菜单'
);

-- 获取答题页面菜单ID（假设为2001，实际执行时请根据实际情况调整）
-- SET @paper_menu_id = LAST_INSERT_ID();
-- 或者手动指定菜单ID
-- SET @paper_menu_id = 2001;

-- 2.3 创建答题相关按钮权限
-- 2.3.1 开始答题按钮
INSERT INTO sys_menu (
    menu_name, 
    parent_id, 
    order_num, 
    path, 
    component, 
    is_frame, 
    is_cache, 
    menu_type, 
    visible, 
    status, 
    perms, 
    icon, 
    create_by, 
    create_time, 
    remark
) VALUES (
    '开始答题', 
    2001,  -- TODO: 请替换为实际的答题页面菜单ID
    1, 
    '#', 
    '', 
    1, 
    0, 
    'F', 
    '0', 
    '0', 
    'exam:paper:start', 
    '#', 
    'admin', 
    NOW(), 
    '开始答题按钮'
);

-- 2.3.2 提交答案按钮
INSERT INTO sys_menu (
    menu_name, 
    parent_id, 
    order_num, 
    path, 
    component, 
    is_frame, 
    is_cache, 
    menu_type, 
    visible, 
    status, 
    perms, 
    icon, 
    create_by, 
    create_time, 
    remark
) VALUES (
    '提交答案', 
    2001,  -- TODO: 请替换为实际的答题页面菜单ID
    2, 
    '#', 
    '', 
    1, 
    0, 
    'F', 
    '0', 
    '0', 
    'exam:paper:submit', 
    '#', 
    'admin', 
    NOW(), 
    '提交答案按钮'
);

-- 2.3.3 查看成绩按钮
INSERT INTO sys_menu (
    menu_name, 
    parent_id, 
    order_num, 
    path, 
    component, 
    is_frame, 
    is_cache, 
    menu_type, 
    visible, 
    status, 
    perms, 
    icon, 
    create_by, 
    create_time, 
    remark
) VALUES (
    '查看成绩', 
    2001,  -- TODO: 请替换为实际的答题页面菜单ID
    3, 
    '#', 
    '', 
    1, 
    0, 
    'F', 
    '0', 
    '0', 
    'exam:paper:score', 
    '#', 
    'admin', 
    NOW(), 
    '查看成绩按钮'
);

-- ----------------------------
-- 3. 配置学员角色与菜单的关联
-- ----------------------------
-- 说明：将答题相关菜单权限分配给学员角色
-- TODO: 请根据实际的角色ID和菜单ID调整以下SQL

-- 3.1 分配答题中心目录权限
INSERT INTO sys_role_menu (role_id, menu_id) 
VALUES (100, 2000);  -- TODO: 请替换为实际的角色ID和菜单ID

-- 3.2 分配答题页面权限
INSERT INTO sys_role_menu (role_id, menu_id) 
VALUES (100, 2001);  -- TODO: 请替换为实际的角色ID和菜单ID

-- 3.3 分配答题按钮权限
INSERT INTO sys_role_menu (role_id, menu_id) 
SELECT 100, menu_id  -- TODO: 请替换为实际的角色ID
FROM sys_menu 
WHERE perms IN ('exam:paper:start', 'exam:paper:submit', 'exam:paper:score');

-- ----------------------------
-- 4. 配置学员用户与角色的关联
-- ----------------------------
-- 说明：将学员角色分配给具体的学员用户
-- TODO: 请根据实际的学员用户ID和角色ID执行以下SQL

-- 示例：为学员用户ID为101的用户分配学员角色
-- INSERT INTO sys_user_role (user_id, role_id) 
-- VALUES (101, 100);  -- TODO: 请替换为实际的用户ID和角色ID

-- 批量分配：为所有学员用户分配学员角色（需要先创建学员用户）
-- INSERT INTO sys_user_role (user_id, role_id)
-- SELECT user_id, 100  -- TODO: 请替换为实际的角色ID
-- FROM sys_user
-- WHERE user_type = '01';  -- 学员类型

-- ============================================
-- 权限标识说明
-- ============================================
-- 答题页面查看权限：exam:paper:view
-- 开始答题权限：exam:paper:start
-- 提交答案权限：exam:paper:submit
-- 查看成绩权限：exam:paper:score
-- ============================================
-- 
-- 配置位置说明：
-- 1. 角色配置：sys_role 表
-- 2. 菜单权限配置：sys_menu 表
-- 3. 角色菜单关联：sys_role_menu 表
-- 4. 用户角色关联：sys_user_role 表
-- 
-- 配置步骤：
-- 1. 执行本文件中的SQL语句（注意调整TODO标记的ID）
-- 2. 在系统管理 -> 角色管理 中查看和调整角色权限
-- 3. 在系统管理 -> 菜单管理 中查看和调整菜单权限
-- 4. 在系统管理 -> 用户管理 中为用户分配角色
-- ============================================




