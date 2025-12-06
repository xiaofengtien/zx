/**
 * 迁移 004：添加考试机位字段到学员档案表
 */
module.exports = {
  up: (db) => {
    console.log('开始执行迁移 004: 添加考试机位字段...');

    try {
      // 检查字段是否已存在
      const columns = db.prepare(`PRAGMA table_info(student_archive)`).all();
      const hasSeatNumber = columns.some(col => col.name === 'seat_number');
      
      if (!hasSeatNumber) {
        db.exec(`ALTER TABLE student_archive ADD COLUMN seat_number TEXT;`);
        console.log('✓ 已添加 student_archive.seat_number 字段');
      } else {
        console.log('  student_archive.seat_number 字段已存在，跳过');
      }
    } catch (e) {
      if (!e.message.includes('duplicate column name')) {
        throw e;
      }
      console.log('  student_archive.seat_number 字段已存在，跳过');
    }

    console.log('✓ 迁移 004 执行完成');
  },

  down: (db) => {
    console.log('开始回滚迁移 004...');
    // 注意：SQLite不支持删除列，回滚操作需要重建表
    // 这里只记录日志，不执行实际删除操作
    console.log('  注意：SQLite不支持删除列，seat_number 字段将保留');
    console.log('✓ 迁移 004 回滚完成（字段未删除）');
  }
};


