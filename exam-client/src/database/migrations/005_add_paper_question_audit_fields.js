/**
 * 迁移 005：为 paper_question 表添加审计字段（create_by, update_by, update_time）
 * 对应后端 MySQL: ALTER TABLE paper_question ADD COLUMN create_by, update_by, update_time
 */
module.exports = {
  up: (db) => {
    console.log('开始执行迁移 005: 为 paper_question 表添加审计字段...');

    // ============================================
    // 更新 paper_question 表 - 添加审计字段
    // ============================================
    const paperQuestionAuditFields = [
      { name: 'create_by', type: 'TEXT', comment: '创建者' },
      { name: 'update_by', type: 'TEXT', comment: '更新者' },
      { name: 'update_time', type: 'INTEGER', comment: '更新时间（时间戳）' }
    ];

    paperQuestionAuditFields.forEach(field => {
      try {
        db.exec(`ALTER TABLE paper_question ADD COLUMN ${field.name} ${field.type};`);
        console.log(`✓ 已添加 paper_question.${field.name} 字段`);
      } catch (e) {
        if (!e.message.includes('duplicate column name')) {
          throw e;
        }
        console.log(`  paper_question.${field.name} 字段已存在，跳过`);
      }
    });

    console.log('✓ 迁移 005 执行完成');
  },

  down: (db) => {
    console.log('开始回滚迁移 005...');
    console.log('注意：SQLite 不支持删除列，回滚操作需要重建表');
    console.log('为了数据安全，跳过删除字段操作');
    console.log('✓ 迁移 005 回滚完成（字段未删除，避免数据丢失）');
  }
};









