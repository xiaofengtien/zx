/**
 * 迁移 013：去掉 paper_package 表的 paper_id 和 paper_code 的 UNIQUE 约束
 * 允许同一试卷有多个版本的记录
 */
module.exports = {
  up: (db) => {
    try {
      console.log('开始迁移：去掉 paper_package 表的 UNIQUE 约束...')
      
      // 检查表是否存在
      const tableInfo = db.prepare(`
        SELECT name FROM sqlite_master 
        WHERE type='table' AND name='paper_package'
      `).get()
      
      if (!tableInfo) {
        console.log('  paper_package 表不存在，跳过迁移')
        return
      }
      
      // 1. 创建新表（不带 UNIQUE 约束）
      console.log('  创建新表 paper_package_new...')
      db.exec(`
        CREATE TABLE IF NOT EXISTS paper_package_new (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          paper_id INTEGER NOT NULL,
          paper_code TEXT NOT NULL,
          package_data BLOB,
          package_path TEXT,
          package_hash TEXT NOT NULL,
          package_size INTEGER NOT NULL,
          storage_type INTEGER DEFAULT 0,
          version INTEGER DEFAULT 1,
          sync_time INTEGER NOT NULL,
          is_active INTEGER DEFAULT 1
        )
      `)
      
      // 2. 复制数据
      console.log('  复制数据到新表...')
      const existingData = db.prepare(`
        SELECT paper_id, paper_code, package_data, package_path, 
               package_hash, package_size, storage_type, version, 
               sync_time, is_active
        FROM paper_package
      `).all()
      
      if (existingData.length > 0) {
        const insertStmt = db.prepare(`
          INSERT INTO paper_package_new 
          (paper_id, paper_code, package_data, package_path, 
           package_hash, package_size, storage_type, version, 
           sync_time, is_active)
          VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        `)
        
        const transaction = db.transaction((data) => {
          for (const row of data) {
            insertStmt.run(
              row.paper_id,
              row.paper_code,
              row.package_data,
              row.package_path,
              row.package_hash,
              row.package_size,
              row.storage_type,
              row.version,
              row.sync_time,
              row.is_active
            )
          }
        })
        
        transaction(existingData)
        console.log(`  ✓ 已复制 ${existingData.length} 条记录`)
      } else {
        console.log('  没有现有数据需要复制')
      }
      
      // 3. 删除旧表（会自动删除所有索引，包括自动生成的唯一索引）
      console.log('  删除旧表...')
      db.exec(`DROP TABLE paper_package`)
      
      // 4. 重命名新表
      console.log('  重命名新表...')
      db.exec(`ALTER TABLE paper_package_new RENAME TO paper_package`)
      
      // 5. 删除所有自动生成的唯一索引（如果还存在）
      console.log('  检查并删除自动生成的唯一索引...')
      try {
        const indexes = db.prepare(`
          SELECT name FROM sqlite_master 
          WHERE type='index' AND tbl_name='paper_package' 
          AND name LIKE 'sqlite_autoindex_%'
        `).all()
        
        for (const idx of indexes) {
          try {
            db.exec(`DROP INDEX IF EXISTS ${idx.name}`)
            console.log(`  ✓ 已删除自动生成的唯一索引: ${idx.name}`)
          } catch (e) {
            console.warn(`  删除索引 ${idx.name} 失败: ${e.message}`)
          }
        }
      } catch (e) {
        console.warn(`  检查索引时出错: ${e.message}`)
      }
      
      // 6. 创建非唯一索引（提高查询性能）
      console.log('  创建非唯一索引...')
      try {
        db.exec(`
          CREATE INDEX IF NOT EXISTS idx_paper_package_paper_id 
          ON paper_package(paper_id)
        `)
        db.exec(`
          CREATE INDEX IF NOT EXISTS idx_paper_package_paper_code 
          ON paper_package(paper_code)
        `)
        db.exec(`
          CREATE INDEX IF NOT EXISTS idx_paper_package_active 
          ON paper_package(is_active)
        `)
        console.log('  ✓ 已创建非唯一索引')
      } catch (e) {
        if (!e.message.includes('already exists')) {
          throw e
        }
        console.log('  索引已存在，跳过')
      }
      
      // 7. 验证迁移结果：检查是否还有唯一索引
      console.log('  验证迁移结果...')
      const remainingUniqueIndexes = db.prepare(`
        SELECT name FROM sqlite_master 
        WHERE type='index' AND tbl_name='paper_package' 
        AND sql LIKE '%UNIQUE%'
      `).all()
      
      if (remainingUniqueIndexes.length > 0) {
        console.warn(`  ⚠️ 警告：仍存在唯一索引: ${remainingUniqueIndexes.map(i => i.name).join(', ')}`)
      } else {
        console.log('  ✓ 验证通过：已无唯一索引')
      }
      
      console.log('✓ 迁移完成：已去掉 paper_package 表的 UNIQUE 约束')
    } catch (error) {
      console.error('迁移失败:', error)
      throw error
    }
  },
  
  down: (db) => {
    // 回滚迁移（恢复 UNIQUE 约束）需要重建表
    console.log('回滚迁移：恢复 UNIQUE 约束...')
    // 注意：回滚比较复杂，因为 SQLite 不支持直接添加 UNIQUE 约束
    console.warn('  回滚操作需要手动执行，因为 SQLite 不支持直接添加 UNIQUE 约束')
  }
}









