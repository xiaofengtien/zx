/**
 * 迁移 012: 为 paper_intermission 表添加 from_volume_id 和 to_volume_id 字段
 * 因为 volumeCode 会重复，需要通过 volumeId 来唯一标识卷别
 */
module.exports = {
  up: (db) => {
    console.log('执行 012_add_intermission_volume_id_fields.js 迁移');
    try {
      // 检查 paper_intermission 表是否存在
      const tableInfo = db.prepare(`PRAGMA table_info(paper_intermission)`).all();
      
      if (tableInfo.length === 0) {
        console.warn('paper_intermission 表不存在，跳过添加字段');
        return;
      }
      
      const fields = [
        { name: 'from_volume_id', type: 'INTEGER' },
        { name: 'to_volume_id', type: 'INTEGER' }
      ];
      
      for (const field of fields) {
        const hasField = tableInfo.some(col => col.name === field.name);
        
        if (!hasField) {
          try {
            db.exec(`
              ALTER TABLE paper_intermission ADD COLUMN ${field.name} ${field.type};
            `);
            console.log(`✓ 已添加 ${field.name} 字段到 paper_intermission 表`);
          } catch (e) {
            if (e.message.includes('duplicate column name')) {
              console.warn(`字段 ${field.name} 已存在，跳过添加`);
            } else {
              console.error(`添加 ${field.name} 字段失败:`, e.message);
              throw e;
            }
          }
        } else {
          console.log(`字段 ${field.name} 已存在，跳过添加`);
        }
      }

      // 创建索引
      try {
        db.exec(`CREATE INDEX IF NOT EXISTS idx_paper_intermission_from_volume_id ON paper_intermission(from_volume_id);`);
        db.exec(`CREATE INDEX IF NOT EXISTS idx_paper_intermission_to_volume_id ON paper_intermission(to_volume_id);`);
        db.exec(`CREATE INDEX IF NOT EXISTS idx_paper_intermission_volumes ON paper_intermission(paper_id, from_volume_id, to_volume_id);`);
        console.log('✓ 已创建 paper_intermission 表新索引');
      } catch (e) {
        if (!e.message.includes('already exists')) {
          console.warn('警告：创建 paper_intermission 表索引失败:', e.message);
        }
      }
    } catch (e) {
      console.error('执行 012_add_intermission_volume_id_fields.js 迁移失败:', e.message);
      throw e;
    }
    console.log('✓ 迁移 012 执行完成');
  },
  down: (db) => {
    console.log('回滚 012_add_intermission_volume_id_fields.js 迁移');
    // SQLite 不支持直接删除列，通常在 down 迁移中会重建表或不执行操作
  }
};









