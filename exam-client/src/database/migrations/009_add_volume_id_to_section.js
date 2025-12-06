module.exports = {
  up: (db) => {
    console.log('执行 009_add_volume_id_to_section.js 迁移');
    try {
      // 检查 paper_section 表是否存在 volume_id 字段
      const tableInfo = db.prepare(`PRAGMA table_info(paper_section)`).all();
      const hasVolumeId = tableInfo.some(col => col.name === 'volume_id');
      
      if (!hasVolumeId) {
        console.log('检测到 paper_section 表缺少 volume_id 字段，开始添加...');
        
        // 添加 volume_id 字段（允许 NULL，因为可能需要后续更新）
        db.exec(`
          ALTER TABLE paper_section ADD COLUMN volume_id INTEGER;
        `);
        console.log('✓ 已添加 volume_id 字段到 paper_section 表');
        
        // 尝试更新现有数据：根据 volume_code 查找对应的 volume_id
        try {
          db.exec(`
            UPDATE paper_section ps
            SET volume_id = (
              SELECT pv.id 
              FROM paper_volume pv 
              WHERE pv.paper_id = ps.paper_id 
                AND pv.volume_code = ps.volume_code 
              LIMIT 1
            )
            WHERE ps.volume_id IS NULL;
          `);
          console.log('✓ 已更新现有数据的 volume_id');
        } catch (e) {
          console.warn('警告：更新现有数据的 volume_id 失败，可能需要手动处理:', e.message);
        }
        
        // 创建索引
        try {
          db.exec(`CREATE INDEX IF NOT EXISTS idx_paper_section_volume_id ON paper_section(volume_id);`);
          console.log('✓ 已创建 volume_id 索引');
        } catch (e) {
          if (!e.message.includes('already exists')) {
            console.warn('警告：创建索引失败:', e.message);
          }
        }
      } else {
        console.log('字段 volume_id 已存在，跳过添加');
      }
    } catch (e) {
      if (e.message.includes('duplicate column name: volume_id')) {
        console.warn('字段 volume_id 已存在，跳过添加');
      } else {
        console.error('添加 volume_id 字段失败:', e.message);
        throw e;
      }
    }
  },
  down: (db) => {
    console.log('回滚 009_add_volume_id_to_section.js 迁移');
    // SQLite 不支持直接删除列，通常在 down 迁移中会重建表或不执行操作
    // 为了简化，这里不提供 down 操作，实际生产环境需要更复杂的处理
  }
};









