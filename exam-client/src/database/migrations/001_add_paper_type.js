/**
 * 迁移：添加 paper_type 字段到 paper 表
 * 版本：001
 * 日期：2024-01-XX
 * 说明：为试卷表添加试卷类型字段，用于区分不同类型的试卷
 */
module.exports = {
  up: async (db) => {
    console.log('执行迁移 001: 添加 paper_type 字段到 paper 表')

    // 首先检查表是否存在（表可能尚未创建，由 initTables 负责创建）
    const tableExists = db.prepare(`
      SELECT name FROM sqlite_master WHERE type='table' AND name='paper'
    `).get()

    if (!tableExists) {
      console.log('表 paper 尚未创建，跳过此迁移（initTables 会创建完整表结构）')
      return
    }

    // 检查字段是否已存在
    const columns = db.prepare(`PRAGMA table_info(paper)`).all()
    const columnExists = columns.some(col => col.name === 'paper_type')

    if (!columnExists) {
      // 添加字段
      db.exec(`ALTER TABLE paper ADD COLUMN paper_type TEXT`)
      console.log('✓ 字段 paper_type 已添加')

      // 添加索引
      db.exec(`CREATE INDEX IF NOT EXISTS idx_paper_type ON paper(paper_type)`)
      db.exec(`CREATE INDEX IF NOT EXISTS idx_paper_type_status ON paper(paper_type, status)`)
      console.log('✓ 索引已创建')
    } else {
      console.log('字段 paper_type 已存在，跳过')
    }
  },

  down: async (db) => {
    console.log('回滚迁移 001: 删除 paper_type 字段')
    // SQLite不支持删除列，这里只删除索引
    try {
      db.exec(`DROP INDEX IF EXISTS idx_paper_type`)
      db.exec(`DROP INDEX IF EXISTS idx_paper_type_status`)
      console.log('✓ 索引已删除')
    } catch (error) {
      console.warn('删除索引失败（可能不存在）:', error.message)
    }
  }
}



