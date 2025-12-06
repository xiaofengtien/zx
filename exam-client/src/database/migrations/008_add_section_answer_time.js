/**
 * 迁移 008：为 paper_section 表添加作答时间字段
 */
module.exports = {
  up: (db) => {
    console.log('开始执行迁移 008: 为 paper_section 表添加作答时间字段...');

    try {
      // 检查字段是否已存在
      const columns = db.prepare(`PRAGMA table_info(paper_section)`).all();
      const hasField = columns.some(col => col.name === 'answer_time');

      if (!hasField) {
        db.exec(`ALTER TABLE paper_section ADD COLUMN answer_time INTEGER DEFAULT 5;`);
        console.log(`✓ 已添加 paper_section.answer_time 字段`);
      } else {
        console.log(`  paper_section.answer_time 字段已存在，跳过`);
      }
    } catch (e) {
      if (!e.message.includes('duplicate column name')) {
        throw e;
      }
      console.log(`  paper_section.answer_time 字段已存在，跳过`);
    }

    console.log('✓ 迁移 008 执行完成');
  },

  down: (db) => {
    console.log('开始回滚迁移 008...');
    console.log('注意：SQLite 不支持删除列，回滚操作需要重建表');
    console.log('为了数据安全，跳过删除字段操作');
    console.log('✓ 迁移 008 回滚完成（字段未删除，避免数据丢失）');
  }
};









