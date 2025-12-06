module.exports = {
  up: (db) => {
    console.log('执行 010_add_intermission_audio_fields.js 迁移');
    try {
      // 检查 paper_intermission 表是否存在
      const tableInfo = db.prepare(`PRAGMA table_info(paper_intermission)`).all();
      
      if (tableInfo.length === 0) {
        console.warn('paper_intermission 表不存在，跳过添加字段');
        return;
      }
      
      const fields = [
        { name: 'intermission_audio_url', type: 'TEXT' },
        { name: 'intermission_audio_path', type: 'TEXT' },
        { name: 'intermission_audio_duration', type: 'INTEGER' }
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
    } catch (e) {
      console.error('添加 paper_intermission 音频字段失败:', e.message);
      throw e;
    }
  },
  down: (db) => {
    console.log('回滚 010_add_intermission_audio_fields.js 迁移');
    // SQLite 不支持直接删除列，通常在 down 迁移中会重建表或不执行操作
  }
};









