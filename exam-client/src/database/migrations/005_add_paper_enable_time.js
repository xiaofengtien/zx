/**
 * 迁移 006：添加试卷启用时间字段
 * 对应后端 SQL: sql/add_paper_enable_time_fields.sql
 */
module.exports = {
  up: (db) => {
    console.log('开始执行迁移 005: 添加试卷启用时间字段...');

    // ============================================
    // 更新 paper 表 - 添加启用时间字段
    // ============================================
    const paperFields = [
      { name: 'enable_start_time', type: 'INTEGER', comment: '试卷启用开始时间（时间戳，为空表示不限制开始时间）' },
      { name: 'enable_end_time', type: 'INTEGER', comment: '试卷启用结束时间（时间戳，为空表示不限制结束时间）' }
    ];

    paperFields.forEach(field => {
      try {
        db.exec(`ALTER TABLE paper ADD COLUMN ${field.name} ${field.type};`);
        console.log(`✓ 已添加 paper.${field.name} 字段`);
      } catch (e) {
        if (!e.message.includes('duplicate column name')) {
          throw e;
        }
        console.log(`  paper.${field.name} 字段已存在，跳过`);
      }
    });

    console.log('✓ 迁移 005 执行完成');
  },

  down: (db) => {
    console.log('开始回滚迁移 005...');
    // 注意：SQLite不支持删除列，回滚操作需要重建表
    // 这里只记录日志，不执行实际删除操作
    console.log('  注意：SQLite不支持删除列，enable_start_time 和 enable_end_time 字段将保留');
    console.log('✓ 迁移 005 回滚完成（字段未删除）');
  }
};

