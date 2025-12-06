const path = require('path')
const fs = require('fs')
const { app } = require('electron')

/**
 * 数据库迁移服务
 * 用于检测MySQL的DDL变更并同步到SQLite
 */
class MigrationService {
  constructor(db) {
    this.db = db.getDB()
    this.migrationTableName = 'schema_migrations'
    this.migrationsPath = path.join(__dirname, 'migrations')
    this.initMigrationTable()
  }

  /**
   * 初始化迁移记录表
   */
  initMigrationTable() {
    try {
      this.db.exec(`
        CREATE TABLE IF NOT EXISTS ${this.migrationTableName} (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          version TEXT UNIQUE NOT NULL,
          description TEXT,
          applied_at INTEGER NOT NULL
        )
      `)
      console.log('✓ 迁移记录表已初始化')
    } catch (error) {
      console.error('初始化迁移记录表失败:', error)
    }
  }

  /**
   * 获取已应用的迁移版本列表
   */
  getAppliedMigrations() {
    try {
      const rows = this.db.prepare(`
        SELECT version FROM ${this.migrationTableName} ORDER BY applied_at ASC
      `).all()
      return rows.map(row => row.version)
    } catch (error) {
      console.error('获取已应用的迁移失败:', error)
      return []
    }
  }

  /**
   * 记录迁移已应用
   */
  recordMigration(version, description) {
    try {
      this.db.prepare(`
        INSERT OR IGNORE INTO ${this.migrationTableName} (version, description, applied_at)
        VALUES (?, ?, ?)
      `).run(version, description, Date.now())
      console.log(`✓ 迁移 ${version} 已记录: ${description}`)
    } catch (error) {
      console.error(`记录迁移 ${version} 失败:`, error)
    }
  }

  /**
   * 执行所有未应用的迁移
   */
  async runMigrations() {
    console.log('=== 开始执行数据库迁移 ===')
    
    // 获取已应用的迁移
    const appliedMigrations = this.getAppliedMigrations()
    console.log(`已应用的迁移: ${appliedMigrations.length} 个`)

    // 获取所有迁移文件
    const migrationFiles = this.getMigrationFiles()
    console.log(`找到迁移文件: ${migrationFiles.length} 个`)

    // 执行未应用的迁移
    let appliedCount = 0
    for (const migration of migrationFiles) {
      if (!appliedMigrations.includes(migration.version)) {
        try {
          console.log(`执行迁移: ${migration.version} - ${migration.description}`)
          await this.executeMigration(migration)
          this.recordMigration(migration.version, migration.description)
          appliedCount++
        } catch (error) {
          console.error(`迁移 ${migration.version} 执行失败:`, error)
          throw error // 迁移失败时停止
        }
      } else {
        console.log(`跳过已应用的迁移: ${migration.version}`)
      }
    }

    if (appliedCount > 0) {
      console.log(`✓ 数据库迁移完成，应用了 ${appliedCount} 个新迁移`)
    } else {
      console.log('✓ 数据库已是最新版本，无需迁移')
    }
  }

  /**
   * 获取所有迁移文件
   */
  getMigrationFiles() {
    const migrations = []
    
    // 如果migrations目录不存在，创建它
    if (!fs.existsSync(this.migrationsPath)) {
      fs.mkdirSync(this.migrationsPath, { recursive: true })
      console.log(`创建迁移目录: ${this.migrationsPath}`)
    }

    // 读取迁移文件
    const files = fs.readdirSync(this.migrationsPath)
      .filter(file => file.endsWith('.js'))
      .sort()

    for (const file of files) {
      try {
        const migrationPath = path.join(this.migrationsPath, file)
        const migration = require(migrationPath)
        
        // 从文件名提取版本号（格式：001_add_paper_type.js）
        const match = file.match(/^(\d+)_(.+)\.js$/)
        if (match) {
          migrations.push({
            version: match[1],
            description: match[2].replace(/_/g, ' '),
            file: file,
            path: migrationPath,
            up: migration.up,
            down: migration.down
          })
        }
      } catch (error) {
        console.error(`加载迁移文件 ${file} 失败:`, error)
      }
    }

    return migrations
  }

  /**
   * 执行迁移
   */
  async executeMigration(migration) {
    if (typeof migration.up === 'function') {
      await migration.up(this.db)
    } else {
      throw new Error(`迁移 ${migration.version} 缺少 up 方法`)
    }
  }

  /**
   * 回滚迁移（用于开发调试）
   */
  async rollbackMigration(version) {
    const migrationFiles = this.getMigrationFiles()
    const migration = migrationFiles.find(m => m.version === version)
    
    if (!migration) {
      throw new Error(`未找到迁移 ${version}`)
    }

    if (typeof migration.down === 'function') {
      await migration.down(this.db)
      // 删除迁移记录
      this.db.prepare(`
        DELETE FROM ${this.migrationTableName} WHERE version = ?
      `).run(version)
      console.log(`✓ 迁移 ${version} 已回滚`)
    } else {
      throw new Error(`迁移 ${version} 缺少 down 方法`)
    }
  }
}

module.exports = MigrationService



