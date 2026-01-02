const path = require('path')
const fs = require('fs')
const { app } = require('electron')

// 在打包环境中，better-sqlite3 需要从 extraResources 中加载
function getBetterSqlite3() {
  const isDev = process.env.NODE_ENV === 'development'

  if (isDev) {
    // 开发环境：直接从 node_modules 加载
    return require('better-sqlite3')
  }

  // 生产环境：从 extraResources 中加载
  // app.getAppPath() 返回 app.asar 的路径
  // extraResources 放在 app.asar 同级的目录
  const appPath = app.getAppPath()
  const resourcesPath = path.dirname(appPath) // Resources 目录
  const betterSqlite3Path = path.join(resourcesPath, 'node_modules', 'better-sqlite3')

  console.log('=== better-sqlite3 路径调试 ===')
  console.log('isDev:', isDev)
  console.log('appPath:', appPath)
  console.log('resourcesPath:', resourcesPath)
  console.log('betterSqlite3Path:', betterSqlite3Path)
  console.log('路径是否存在:', fs.existsSync(betterSqlite3Path))

  if (fs.existsSync(betterSqlite3Path)) {
    return require(betterSqlite3Path)
  }

  // 如果 extraResources 路径不存在，尝试其他常见路径
  const alternativePaths = [
    path.join(appPath, '..', 'node_modules', 'better-sqlite3'),
    path.join(appPath, 'node_modules', 'better-sqlite3'),
    path.join(process.resourcesPath || '', 'node_modules', 'better-sqlite3')
  ]

  for (const altPath of alternativePaths) {
    if (fs.existsSync(altPath)) {
      console.log('使用备选路径:', altPath)
      return require(altPath)
    }
  }

  // 最后尝试直接 require（可能在某些环境下有效）
  console.log('使用默认 require')
  return require('better-sqlite3')
}

const Database = getBetterSqlite3()

class DB {
  constructor() {
    // 获取应用数据目录
    const userDataPath = app.getPath('userData')

    // 确保目录存在
    if (!fs.existsSync(userDataPath)) {
      fs.mkdirSync(userDataPath, { recursive: true })
    }

    // 数据库文件路径
    this.dbPath = path.join(userDataPath, 'exam.db')

    // 输出数据库文件路径（用于调试和 DataGrip 连接）
    console.log('=== SQLite 数据库信息 ===')
    console.log('[DB] 数据库文件路径:', this.dbPath)
    console.log('[DB] 数据库文件是否存在:', fs.existsSync(this.dbPath))

    // 打开数据库
    this.db = new Database(this.dbPath)

    // 初始化表结构（必须在迁移之前，确保基础表存在）
    this.initTables()

    // 执行数据库迁移（在表结构初始化之后，用于添加新字段等操作）
    this.runMigrations()
  }


  /**
   * 执行数据库迁移
   */
  runMigrations() {
    try {
      const MigrationService = require('./migration')
      const migrationService = new MigrationService(this)
      migrationService.runMigrations()
    } catch (error) {
      console.error('执行数据库迁移失败:', error)
      // 迁移失败不影响应用启动，但会记录错误
    }
  }

  /**
   * 初始化表结构（主方法）
   * 性能优化：快速检查 + 关键表同步 + 非关键表异步
   * 按需初始化：如果没有ZIP包，再同步表（性能优化）
   */
  initTables() {
    console.log('开始初始化数据库表结构...')
    const startTime = Date.now()

    // 1. 快速检查：所有表是否都已存在
    const existingTables = this.getExistingTables()
    const allRequiredTables = [
      // 关键表（必须同步初始化）
      'student_credentials',
      'student_papers',
      'dict_data',
      'student_archive',
      'app_config', // 应用配置表
      // 试卷相关表（非关键，按需初始化）
      'paper_package',
      'paper_index',
      'app_user_paper_info',
      'app_user_paper_question_result',
      'app_user_paper_question_blank_result',
      'question_media_index',
      'download_status', // 下载状态表（用于断点续传和状态持久化）
      // 业务数据表（如果没有ZIP包，需要同步这些数据）
      'paper',
      'paper_section',
      'paper_volume',
      'paper_intermission',
      'paper_question_group',
      'paper_question',
      'question',
      'question_category',
      'question_media',
      'question_answer',
      'question_blank_area'
    ]

    const missingTables = allRequiredTables.filter(t => !existingTables.includes(t))

    if (missingTables.length === 0) {
      const elapsed = Date.now() - startTime
      console.log(`✓ 所有表已存在，跳过初始化（耗时 ${elapsed}ms）`)
      return // 快速返回，<10ms
    }

    console.log(`需要创建 ${missingTables.length} 个表`)

    // 2. 同步初始化关键表（必须立即可用）
    this.initCriticalTables(missingTables)

    const criticalElapsed = Date.now() - startTime
    console.log(`✓ 关键表初始化完成（耗时 ${criticalElapsed}ms）`)

    // 3. 同步初始化试卷相关表（确保在迁移之前完成）
    // 注意：原来使用 setImmediate 异步执行，但会导致迁移脚本找不到表
    // 改为同步执行，确保所有表都在迁移之前创建完成
    this.initPaperTablesSync(missingTables)

    const allElapsed = Date.now() - startTime
    console.log(`✓ 所有表初始化完成（耗时 ${allElapsed}ms）`)
  }

  /**
   * 获取已存在的表列表
   */
  getExistingTables() {
    try {
      return this.db.prepare(`
        SELECT name FROM sqlite_master 
        WHERE type='table' AND name NOT LIKE 'sqlite_%'
      `).all().map(t => t.name)
    } catch (error) {
      console.error('获取表列表失败:', error)
      return []
    }
  }

  /**
   * 初始化关键表（同步，必须立即可用）
   */
  initCriticalTables(missingTables) {
    // 关键表：必须同步初始化，确保立即可用
    // download_status 表用于下载状态持久化，需要同步初始化
    // app_config 表用于存储应用配置信息
    const criticalTables = ['student_credentials', 'student_papers', 'dict_data', 'student_archive', 'download_status', 'app_config']
    const toCreate = criticalTables.filter(t => missingTables.includes(t))

    if (toCreate.length === 0) {
      return
    }

    console.log(`创建关键表: ${toCreate.join(', ')}`)

    try {
      // 创建学员凭证表
      if (toCreate.includes('student_credentials')) {
        this.db.exec(`
          CREATE TABLE IF NOT EXISTS student_credentials (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            student_account TEXT UNIQUE NOT NULL,
            password_hash TEXT NOT NULL,
            offline_credential TEXT,
            archive_id INTEGER,
            user_id INTEGER,
            create_time INTEGER,
            expire_time INTEGER,
            last_login_time INTEGER
          )
        `)
      }

      // 创建学员考卷类型表
      if (toCreate.includes('student_papers')) {
        this.db.exec(`
          CREATE TABLE IF NOT EXISTS student_papers (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            student_account TEXT NOT NULL,
            paper_types TEXT NOT NULL,
            update_time INTEGER,
            FOREIGN KEY (student_account) REFERENCES student_credentials(student_account)
          )
        `)
      }

      // 创建字典数据表
      if (toCreate.includes('dict_data')) {
        this.db.exec(`
          CREATE TABLE IF NOT EXISTS dict_data (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            dict_type TEXT NOT NULL,
            dict_value TEXT NOT NULL,
            dict_label TEXT NOT NULL,
            dict_sort INTEGER DEFAULT 0,
            css_class TEXT,
            list_class TEXT,
            is_default TEXT DEFAULT 'N',
            status TEXT DEFAULT '0',
            create_time INTEGER,
            update_time INTEGER,
            remark TEXT,
            UNIQUE(dict_type, dict_value)
          )
        `)
      }

      // 创建下载状态表（用于断点续传和状态持久化）
      if (toCreate.includes('download_status')) {
        this.db.exec(`
          CREATE TABLE IF NOT EXISTS download_status (
            paper_id INTEGER PRIMARY KEY,
            paper_code TEXT NOT NULL,
            status TEXT NOT NULL,
            progress INTEGER DEFAULT 0,
            downloaded_size INTEGER DEFAULT 0,
            total_size INTEGER DEFAULT 0,
            error_message TEXT,
            start_time INTEGER,
            update_time INTEGER,
            completed_time INTEGER
          )
        `)
        // 创建索引
        this.db.exec(`
          CREATE INDEX IF NOT EXISTS idx_download_status_paper_id ON download_status(paper_id)
        `)
        this.db.exec(`
          CREATE INDEX IF NOT EXISTS idx_download_status_status ON download_status(status)
        `)
      }

      // 创建学员档案表
      if (toCreate.includes('student_archive')) {
        this.db.exec(`
          CREATE TABLE IF NOT EXISTS student_archive (
            id INTEGER PRIMARY KEY,
            user_id INTEGER,
            student_account TEXT NOT NULL,
            password TEXT,
            phone_number TEXT,
            sex TEXT,
            grade TEXT,
            current_grade TEXT,
            hometown TEXT,
            applicable_papers TEXT DEFAULT '[]',
            applicable_paper_ids TEXT DEFAULT '[]',
            status TEXT DEFAULT '0',
            del_flag TEXT DEFAULT '0',
            create_time INTEGER,
            update_time INTEGER,
            remark TEXT,
            UNIQUE(student_account)
          )
        `)
      }

      // 创建应用配置表
      if (toCreate.includes('app_config')) {
        this.db.exec(`
          CREATE TABLE IF NOT EXISTS app_config (
            key TEXT PRIMARY KEY,
            value TEXT
          )
        `)
      }

      // 创建关键表索引
      this.createCriticalIndexes()

      console.log(`✓ 关键表创建成功: ${toCreate.join(', ')}`)
    } catch (error) {
      console.error('创建关键表失败:', error)
      throw error // 关键表创建失败，必须抛出异常
    }
  }

  /**
   * 同步初始化试卷相关表（确保在迁移之前完成）
   * @param {Array} missingTables - 需要创建的表列表
   */
  initPaperTablesSync(missingTables) {
    const paperTables = [
      'paper_package',
      'paper_index',
      'app_user_paper_info',
      'app_user_paper_question_result',
      'app_user_paper_question_blank_result',
      'question_media_index',
      // 业务数据表
      'paper',
      'paper_section',
      'paper_volume',
      'paper_intermission',
      'paper_question_group',
      'paper_question',
      'question',
      'question_category',
      'question_media',
      'question_answer',
      'question_blank_area'
    ]

    const toCreate = paperTables.filter(t => missingTables.includes(t))

    if (toCreate.length === 0) {
      console.log('✓ 试卷相关表已存在，无需创建')
      return
    }

    console.log(`创建试卷相关表: ${toCreate.join(', ')}`)

    try {
      this.initPaperTables(toCreate)
      this.createPaperIndexes()
      console.log(`✓ 试卷相关表创建成功`)
    } catch (error) {
      console.error('创建试卷相关表失败:', error)
      // 不抛出异常，允许应用继续启动
    }
  }

  /**
   * 异步初始化试卷相关表（带重试机制）- 已废弃，保留用于兼容
   * @deprecated 使用 initPaperTablesSync 替代
   * @param {Array} missingTables - 需要创建的表列表
   * @param {Number} retryCount - 当前重试次数
   */
  initPaperTablesAsync(missingTables, retryCount = 0) {
    const MAX_RETRIES = 3
    const paperTables = [
      'paper_package',
      'paper_index',
      'app_user_paper_info',
      'app_user_paper_question_result',
      'app_user_paper_question_blank_result',
      'question_media_index',
      // 注意：download_status 已在 initCriticalTables 中同步创建，这里不再创建
      // 业务数据表（如果没有ZIP包，需要同步这些数据）
      'paper',
      'paper_section',
      'paper_volume',
      'paper_intermission',
      'paper_question_group',
      'paper_question',
      'question',
      'question_category',
      'question_media',
      'question_answer',
      'question_blank_area'
    ]

    const toCreate = paperTables.filter(t => missingTables.includes(t))

    if (toCreate.length === 0) {
      console.log('✓ 试卷相关表已存在，无需创建')
      return
    }

    // 性能优化：检查是否有ZIP包（仅用于日志提示，不阻止创建表）
    const hasZipPackage = !this.shouldDelayPaperTablesInit()
    if (!hasZipPackage) {
      console.log('ℹ️ 未检测到ZIP包，但表结构仍需创建（等待数据同步）')
    }

    console.log(`[异步] 创建试卷相关表 (重试 ${retryCount}/${MAX_RETRIES}): ${toCreate.join(', ')}`)
    const startTime = Date.now()

    try {
      // 如果重试3次后仍然失败，全量覆盖表结构
      if (retryCount >= MAX_RETRIES) {
        console.warn(`⚠️ 试卷相关表创建失败 ${MAX_RETRIES} 次，执行全量覆盖...`)
        this.recreatePaperTables(toCreate)
        return
      }

      // 正常创建表
      this.initPaperTables(toCreate)

      // 创建索引
      this.createPaperIndexes()

      const elapsed = Date.now() - startTime
      console.log(`✓ [异步] 试卷相关表创建成功（耗时 ${elapsed}ms）`)

    } catch (error) {
      console.error(`[异步] 试卷相关表创建失败 (重试 ${retryCount}/${MAX_RETRIES}):`, error.message)

      // 重试机制：延迟后重试
      const retryDelay = (retryCount + 1) * 1000 // 1秒、2秒、3秒
      setTimeout(() => {
        this.initPaperTablesAsync(missingTables, retryCount + 1)
      }, retryDelay)
    }
  }

  /**
   * 检查是否应该延迟初始化试卷相关表
   * 性能优化：如果没有ZIP包，再同步表
   * @returns {Boolean} true-延迟初始化，false-立即初始化
   */
  shouldDelayPaperTablesInit() {
    try {
      // 检查paper_package表是否存在数据
      const existingTables = this.getExistingTables()
      if (!existingTables.includes('paper_package')) {
        // 表不存在，检查是否有ZIP包文件
        const userDataPath = app.getPath('userData')
        const paperPackagesPath = path.join(userDataPath, 'paper_packages')

        if (!fs.existsSync(paperPackagesPath)) {
          return true // 没有ZIP包目录，延迟初始化
        }

        // 检查目录中是否有ZIP文件
        const files = fs.readdirSync(paperPackagesPath)
        const hasZipFiles = files.some(file => file.endsWith('.zip') || file.includes('.zip.part'))

        if (!hasZipFiles) {
          return true // 没有ZIP文件，延迟初始化
        }
      } else {
        // 表存在，检查是否有数据
        const count = this.db.prepare('SELECT COUNT(*) as count FROM paper_package').get()
        if (count && count.count === 0) {
          // 表存在但没有数据，检查是否有ZIP包文件
          const userDataPath = app.getPath('userData')
          const paperPackagesPath = path.join(userDataPath, 'paper_packages')

          if (!fs.existsSync(paperPackagesPath)) {
            return true
          }

          const files = fs.readdirSync(paperPackagesPath)
          const hasZipFiles = files.some(file => file.endsWith('.zip') || file.includes('.zip.part'))

          if (!hasZipFiles) {
            return true
          }
        }
      }

      return false // 有ZIP包，立即初始化
    } catch (error) {
      // 检查失败，默认立即初始化（安全策略）
      console.warn('检查ZIP包状态失败，默认立即初始化表:', error.message)
      return false
    }
  }

  /**
   * 初始化试卷相关表（具体实现）
   */
  initPaperTables(toCreate) {
    // 试卷包表
    if (toCreate.includes('paper_package')) {
      this.db.exec(`
        CREATE TABLE IF NOT EXISTS paper_package (
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
    }

    // 试卷索引表
    if (toCreate.includes('paper_index')) {
      this.db.exec(`
        CREATE TABLE IF NOT EXISTS paper_index (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          paper_id INTEGER NOT NULL UNIQUE,
          paper_code TEXT NOT NULL,
          paper_name TEXT NOT NULL,
          total_questions INTEGER DEFAULT 0,
          total_score REAL DEFAULT 0,
          duration INTEGER,
          intro_audio_url TEXT,
          intro_audio_path TEXT,
          intro_audio_duration INTEGER,
          version INTEGER DEFAULT 1,
          package_hash TEXT,
          sync_time INTEGER,
          is_active INTEGER DEFAULT 1
        )
      `)
    }

    // 用户试卷信息表
    if (toCreate.includes('app_user_paper_info')) {
      this.db.exec(`
        CREATE TABLE IF NOT EXISTS app_user_paper_info (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          paper_id INTEGER NOT NULL,
          paper_name TEXT,
          app_user_id INTEGER NOT NULL,
          start_time INTEGER,
          submit_time INTEGER,
          used_time INTEGER,
          total_score REAL DEFAULT 0,
          user_score REAL DEFAULT 0,
          correct_count INTEGER DEFAULT 0,
          wrong_count INTEGER DEFAULT 0,
          is_submit INTEGER DEFAULT 0,
          sync_status INTEGER DEFAULT 0,
          sync_time INTEGER,
          create_time INTEGER,
          update_time INTEGER
        )
      `)
    }

    // 用户题目结果表
    if (toCreate.includes('app_user_paper_question_result')) {
      this.db.exec(`
        CREATE TABLE IF NOT EXISTS app_user_paper_question_result (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          paper_id INTEGER NOT NULL,
          app_user_id INTEGER NOT NULL,
          question_id INTEGER NOT NULL,
          answer_ids TEXT,
          user_answer TEXT,
          result INTEGER,
          question_sort INTEGER,
          section_id INTEGER,
          volume_id INTEGER,
          volume_code TEXT,
          answer_time INTEGER,
          time_spent INTEGER,
          is_reviewed INTEGER DEFAULT 0,
          sync_status INTEGER DEFAULT 0,
          create_time INTEGER
        )
      `)
    }

    // 用户完形填空结果表
    if (toCreate.includes('app_user_paper_question_blank_result')) {
      this.db.exec(`
        CREATE TABLE IF NOT EXISTS app_user_paper_question_blank_result (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          paper_id INTEGER NOT NULL,
          app_user_id INTEGER NOT NULL,
          question_id INTEGER NOT NULL,
          blank_area_id INTEGER,
          blank_index INTEGER,
          answer_ids TEXT,
          result INTEGER,
          answer_time INTEGER,
          time_spent INTEGER,
          sync_status INTEGER DEFAULT 0,
          create_time INTEGER
        )
      `)
    }

    // 题目媒体文件索引表
    if (toCreate.includes('question_media_index')) {
      this.db.exec(`
        CREATE TABLE IF NOT EXISTS question_media_index (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          paper_id INTEGER NOT NULL,
          question_id INTEGER NOT NULL,
          media_type INTEGER NOT NULL,
          option_id INTEGER,
          blank_area_id INTEGER,
          media_name TEXT NOT NULL,
          media_path TEXT NOT NULL,
          media_format TEXT,
          media_size INTEGER,
          media_duration INTEGER
        )
      `)
    }

    // ========== 下载状态表（用于断点续传和状态持久化） ==========
    if (toCreate.includes('download_status')) {
      this.db.exec(`
        CREATE TABLE IF NOT EXISTS download_status (
          paper_id INTEGER PRIMARY KEY,
          paper_code TEXT NOT NULL,
          status TEXT NOT NULL,
          progress INTEGER DEFAULT 0,
          downloaded_size INTEGER DEFAULT 0,
          total_size INTEGER DEFAULT 0,
          error_message TEXT,
          start_time INTEGER,
          update_time INTEGER,
          completed_time INTEGER
        )
      `)
    }

    // ========== 业务数据表（如果没有ZIP包，需要同步这些数据） ==========

    // 试卷表
    if (toCreate.includes('paper')) {
      this.db.exec(`
        CREATE TABLE IF NOT EXISTS paper (
          id INTEGER PRIMARY KEY,
          paper_name TEXT NOT NULL,
          paper_code TEXT UNIQUE,
          paper_type TEXT,
          paper_desc TEXT,
          business_type INTEGER NOT NULL DEFAULT 5,
          business_id INTEGER,
          total_score REAL DEFAULT 0,
          total_questions INTEGER DEFAULT 0,
          duration INTEGER,
          intro_audio_url TEXT,
          intro_audio_path TEXT,
          intro_audio_duration INTEGER,
          intro_text TEXT,
          auto_next_question INTEGER DEFAULT 1,
          show_answer_immediately INTEGER DEFAULT 0,
          allow_review INTEGER DEFAULT 1,
          question_read_duration INTEGER,
          practice_limit INTEGER DEFAULT 0,
          trial_listen_enabled INTEGER DEFAULT 0,
          trial_listen_audio_url TEXT,
          trial_listen_audio_path TEXT,
          trial_listen_audio_duration INTEGER,
          trial_listen_audio_text TEXT,
          trial_listen_text TEXT,
          trial_intro_audio_url TEXT,
          trial_intro_audio_path TEXT,
          trial_intro_audio_duration INTEGER,
          operate_listen_text TEXT,
          operate_listen_image_url TEXT,
          operate_listen_image_path TEXT,
          notes TEXT,
          notes_display_mode TEXT DEFAULT 'before_exam',
          enable_start_time INTEGER,
          enable_end_time INTEGER,
          version INTEGER DEFAULT 1,
          package_hash TEXT,
          package_size INTEGER,
          last_package_time INTEGER,
          status INTEGER DEFAULT 1,
          create_by TEXT DEFAULT '',
          create_time INTEGER,
          update_by TEXT DEFAULT '',
          update_time INTEGER,
          remark TEXT DEFAULT ''
        )
      `)

      // 向后兼容：确保已存在的旧表拥有所有新字段
      this.addColumnIfNotExists('paper', 'paper_type', 'TEXT')
      this.addColumnIfNotExists('paper', 'practice_limit', 'INTEGER DEFAULT 0')
      this.addColumnIfNotExists('paper', 'trial_listen_enabled', 'INTEGER DEFAULT 0')
      this.addColumnIfNotExists('paper', 'trial_listen_audio_url', 'TEXT')
      this.addColumnIfNotExists('paper', 'trial_listen_audio_path', 'TEXT')
      this.addColumnIfNotExists('paper', 'trial_listen_audio_duration', 'INTEGER')
      this.addColumnIfNotExists('paper', 'trial_listen_audio_text', 'TEXT')
      this.addColumnIfNotExists('paper', 'trial_listen_text', 'TEXT')
      this.addColumnIfNotExists('paper', 'trial_intro_audio_url', 'TEXT')
      this.addColumnIfNotExists('paper', 'trial_intro_audio_path', 'TEXT')
      this.addColumnIfNotExists('paper', 'trial_intro_audio_duration', 'INTEGER')
      this.addColumnIfNotExists('paper', 'operate_listen_text', 'TEXT')
      this.addColumnIfNotExists('paper', 'operate_listen_image_url', 'TEXT')
      this.addColumnIfNotExists('paper', 'operate_listen_image_path', 'TEXT')
      this.addColumnIfNotExists('paper', 'notes', 'TEXT')
      this.addColumnIfNotExists('paper', 'notes_display_mode', 'TEXT DEFAULT "before_exam"')
      this.addColumnIfNotExists('paper', 'enable_start_time', 'INTEGER')
      this.addColumnIfNotExists('paper', 'enable_end_time', 'INTEGER')
    }

    // 试卷大题表
    if (toCreate.includes('paper_section')) {
      this.db.exec(`
        CREATE TABLE IF NOT EXISTS paper_section (
          id INTEGER PRIMARY KEY,
          paper_id INTEGER NOT NULL,
          volume_id INTEGER,
          volume_code TEXT,
          section_name TEXT,
          section_code TEXT,
          section_order INTEGER DEFAULT 0,
          section_type INTEGER,
          section_desc TEXT,
          audio_url TEXT,
          audio_path TEXT,
          audio_duration INTEGER,
          audio_play_count INTEGER DEFAULT 1,
          answer_time INTEGER DEFAULT 5,
          question_count INTEGER DEFAULT 0,
          total_score REAL DEFAULT 0,
          score_per_question REAL DEFAULT 0,
          instruction_text TEXT,
          create_time INTEGER,
          update_time INTEGER
        )
      `)
    }

    // 试卷卷别表
    if (toCreate.includes('paper_volume')) {
      this.db.exec(`
        CREATE TABLE IF NOT EXISTS paper_volume (
          id INTEGER PRIMARY KEY,
          paper_id INTEGER NOT NULL,
          volume_name TEXT,
          volume_code TEXT,
          volume_order INTEGER DEFAULT 0,
          volume_desc TEXT,
          section_count INTEGER DEFAULT 0,
          question_count INTEGER DEFAULT 0,
          total_score REAL DEFAULT 0,
          create_time INTEGER,
          update_time INTEGER
        )
      `)
    }

    // 试卷中场休息表
    if (toCreate.includes('paper_intermission')) {
      this.db.exec(`
        CREATE TABLE IF NOT EXISTS paper_intermission (
          id INTEGER PRIMARY KEY,
          paper_id INTEGER NOT NULL,
          from_volume TEXT,
          to_volume TEXT,
          from_volume_code TEXT,
          to_volume_code TEXT,
          from_volume_id INTEGER,
          to_volume_id INTEGER,
          intermission_duration INTEGER DEFAULT 0,
          intermission_text TEXT,
          intermission_audio_url TEXT,
          intermission_audio_path TEXT,
          intermission_audio_duration INTEGER,
          can_skip INTEGER DEFAULT 0,
          create_time INTEGER,
          update_time INTEGER
        )
      `)
    }

    // 试卷题目组表
    if (toCreate.includes('paper_question_group')) {
      this.db.exec(`
        CREATE TABLE IF NOT EXISTS paper_question_group (
          id INTEGER PRIMARY KEY,
          section_id INTEGER,
          group_name TEXT,
          question_group_id INTEGER,
          group_order INTEGER DEFAULT 0,
          start_question_num INTEGER,
          end_question_num INTEGER,
          audio_url TEXT,
          audio_path TEXT,
          audio_duration INTEGER,
          intro_text TEXT,
          answer_time INTEGER,
          selected_question_ids TEXT,
          create_time INTEGER,
          update_time INTEGER
        )
      `)
    }

    // 试卷-题目关联表
    // 注意：同一道题可能出现在同一试卷的不同大题中，所以唯一键需要包含 section_id
    if (toCreate.includes('paper_question')) {
      this.db.exec(`
        CREATE TABLE IF NOT EXISTS paper_question (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          paper_id INTEGER NOT NULL,
          question_id INTEGER NOT NULL,
          section_id INTEGER,
          section_order INTEGER DEFAULT 0,
          sort_order INTEGER DEFAULT 0,
          score REAL DEFAULT 0,
          create_by TEXT,
          create_time INTEGER,
          update_by TEXT,
          update_time INTEGER,
          UNIQUE(paper_id, question_id, section_id)
        )
      `)
    }


    // 题目表
    if (toCreate.includes('question')) {
      this.db.exec(`
        CREATE TABLE IF NOT EXISTS question (
          id INTEGER PRIMARY KEY,
          question_category_id INTEGER,
          title TEXT,
          media_type INTEGER,
          subject_id INTEGER,
          type INTEGER,
          option_type INTEGER,
          weight INTEGER,
          answer TEXT,
          analyzes TEXT,
          explanation_enabled INTEGER DEFAULT 0,
          explanation_text TEXT,
          explanation_delay_seconds INTEGER DEFAULT 2,
          status INTEGER,
          create_by TEXT DEFAULT '',
          create_time INTEGER,
          update_by TEXT DEFAULT '',
          update_time INTEGER,
          remark TEXT DEFAULT ''
        )
      `)
    }

    // 题目分类表
    if (toCreate.includes('question_category')) {
      this.db.exec(`
        CREATE TABLE IF NOT EXISTS question_category (
          id INTEGER PRIMARY KEY,
          name TEXT,
          father_id INTEGER,
          is_default INTEGER,
          sort_num INTEGER,
          status INTEGER,
          create_by TEXT DEFAULT '',
          create_time INTEGER,
          update_by TEXT DEFAULT '',
          update_time INTEGER,
          remark TEXT DEFAULT ''
        )
      `)
    }

    // 题目媒体文件表
    if (toCreate.includes('question_media')) {
      this.db.exec(`
        CREATE TABLE IF NOT EXISTS question_media (
          id INTEGER PRIMARY KEY,
          question_id INTEGER NOT NULL,
          paper_id INTEGER,
          volume_id INTEGER,
          section_id INTEGER,
          intermission_id INTEGER,
          media_type INTEGER NOT NULL,
          option_id INTEGER,
          blank_area_id INTEGER,
          media_name TEXT NOT NULL,
          media_path TEXT,
          media_url TEXT,
          media_size INTEGER,
          media_format TEXT,
          media_duration INTEGER,
          is_compressed INTEGER DEFAULT 0,
          storage_type INTEGER DEFAULT 0,
          create_time INTEGER
        )
      `)
    }

    // 题目答案表
    if (toCreate.includes('question_answer')) {
      this.db.exec(`
        CREATE TABLE IF NOT EXISTS question_answer (
          id INTEGER PRIMARY KEY,
          question_id INTEGER NOT NULL,
          blank_area_id INTEGER,
          serial_no INTEGER,
          option_name TEXT,
          option_content TEXT,
          is_answer INTEGER,
          status INTEGER,
          create_by TEXT DEFAULT '',
          create_time INTEGER,
          update_by TEXT DEFAULT '',
          update_time INTEGER,
          remark TEXT DEFAULT ''
        )
      `)
    }

    // 完形填空区域表
    if (toCreate.includes('question_blank_area')) {
      this.db.exec(`
        CREATE TABLE IF NOT EXISTS question_blank_area (
          id INTEGER PRIMARY KEY,
          question_id INTEGER NOT NULL,
          blank_index INTEGER,
          answer_ids TEXT,
          status INTEGER,
          create_by TEXT DEFAULT '',
          create_time INTEGER,
          update_by TEXT DEFAULT '',
          update_time INTEGER,
          remark TEXT DEFAULT ''
        )
      `)
    }
  }

  /**
   * 全量覆盖表结构（失败3次后执行）
   * 先删除表，再重新创建
   */
  recreatePaperTables(toCreate) {
    console.log('执行全量覆盖：删除并重新创建试卷相关表...')

    try {
      // 1. 删除现有表（如果存在）
      for (const tableName of toCreate) {
        try {
          this.db.exec(`DROP TABLE IF EXISTS ${tableName}`)
          console.log(`  ✓ 删除表: ${tableName}`)
        } catch (error) {
          console.warn(`  删除表 ${tableName} 失败:`, error.message)
        }
      }

      // 2. 删除相关索引
      const indexes = [
        'idx_paper_package_code',
        'idx_paper_index_code',
        'idx_paper_info_user',
        'idx_paper_info_sync',
        'idx_paper_result_user',
        'idx_paper_result_sync',
        'idx_paper_result_section',
        'idx_paper_result_volume',
        'idx_paper_blank_sync',
        'idx_paper_question',
        'idx_paper_code',
        'idx_paper_business',
        'idx_paper_status',
        'idx_paper_question_paper',
        'idx_paper_question_question',
        'idx_paper_question_sort',
        'idx_question_category',
        'idx_question_category_father',
        'idx_question_media_question',
        'idx_question_media_option',
        'idx_question_media_type',
        'idx_question_answer_question',
        'idx_question_answer_blank',
        'idx_question_blank_question'
      ]

      for (const indexName of indexes) {
        try {
          this.db.exec(`DROP INDEX IF EXISTS ${indexName}`)
        } catch (error) {
          // 索引可能不存在，忽略错误
        }
      }

      // 3. 重新创建表
      this.initPaperTables(toCreate)

      // 4. 重新创建索引
      this.createPaperIndexes()

      console.log('✓ 全量覆盖完成：试卷相关表已重新创建')

    } catch (error) {
      console.error('✗ 全量覆盖失败:', error)
      // 即使全量覆盖失败，也不抛出异常（避免影响应用启动）
      // 可以在后续使用时再次尝试创建
    }
  }

  /**
   * 创建关键表索引（同步）
   */
  createCriticalIndexes() {
    this.db.exec(`CREATE INDEX IF NOT EXISTS idx_student_account ON student_credentials(student_account)`)
    this.db.exec(`CREATE INDEX IF NOT EXISTS idx_student_papers_account ON student_papers(student_account)`)
    this.db.exec(`CREATE INDEX IF NOT EXISTS idx_dict_type ON dict_data(dict_type)`)
    this.db.exec(`CREATE INDEX IF NOT EXISTS idx_student_archive_account ON student_archive(student_account)`)
    this.db.exec(`CREATE INDEX IF NOT EXISTS idx_student_archive_user_id ON student_archive(user_id)`)
  }

  /**
   * 创建试卷相关表索引（异步）
   */
  createPaperIndexes() {
    // 试卷包和索引表索引
    this.db.exec(`CREATE INDEX IF NOT EXISTS idx_paper_package_code ON paper_package(paper_code)`)
    this.db.exec(`CREATE INDEX IF NOT EXISTS idx_paper_index_code ON paper_index(paper_code)`)
    this.db.exec(`CREATE INDEX IF NOT EXISTS idx_paper_group_section ON paper_question_group(section_id)`)

    // 答题结果表索引
    this.db.exec(`CREATE INDEX IF NOT EXISTS idx_paper_info_user ON app_user_paper_info(app_user_id)`)
    this.db.exec(`CREATE INDEX IF NOT EXISTS idx_paper_info_sync ON app_user_paper_info(sync_status)`)
    this.db.exec(`CREATE INDEX IF NOT EXISTS idx_paper_result_user ON app_user_paper_question_result(paper_id, app_user_id)`)
    this.db.exec(`CREATE INDEX IF NOT EXISTS idx_paper_result_sync ON app_user_paper_question_result(sync_status)`)
    this.db.exec(`CREATE INDEX IF NOT EXISTS idx_paper_result_section ON app_user_paper_question_result(paper_id, section_id)`)
    this.db.exec(`CREATE INDEX IF NOT EXISTS idx_paper_result_volume ON app_user_paper_question_result(paper_id, volume_id)`)
    this.db.exec(`CREATE INDEX IF NOT EXISTS idx_paper_blank_sync ON app_user_paper_question_blank_result(sync_status)`)
    this.db.exec(`CREATE INDEX IF NOT EXISTS idx_paper_question ON question_media_index(paper_id, question_id)`)

    // 业务数据表索引
    this.db.exec(`CREATE INDEX IF NOT EXISTS idx_paper_code ON paper(paper_code)`)
    this.db.exec(`CREATE INDEX IF NOT EXISTS idx_paper_business ON paper(business_type, business_id)`)
    this.db.exec(`CREATE INDEX IF NOT EXISTS idx_paper_status ON paper(status)`)
    this.db.exec(`CREATE INDEX IF NOT EXISTS idx_paper_type ON paper(paper_type)`)
    this.db.exec(`CREATE INDEX IF NOT EXISTS idx_paper_type_status ON paper(paper_type, status)`)
    this.db.exec(`CREATE INDEX IF NOT EXISTS idx_paper_question_paper ON paper_question(paper_id)`)
    this.db.exec(`CREATE INDEX IF NOT EXISTS idx_paper_question_question ON paper_question(question_id)`)
    this.db.exec(`CREATE INDEX IF NOT EXISTS idx_paper_question_sort ON paper_question(paper_id, sort_order)`)
    this.db.exec(`CREATE INDEX IF NOT EXISTS idx_question_category ON question(question_category_id)`)
    this.db.exec(`CREATE INDEX IF NOT EXISTS idx_question_category_father ON question_category(father_id)`)
    this.db.exec(`CREATE INDEX IF NOT EXISTS idx_question_media_question ON question_media(question_id)`)
    this.db.exec(`CREATE INDEX IF NOT EXISTS idx_question_media_option ON question_media(option_id)`)
    this.db.exec(`CREATE INDEX IF NOT EXISTS idx_question_media_type ON question_media(question_id, media_type)`)
    this.db.exec(`CREATE INDEX IF NOT EXISTS idx_question_answer_question ON question_answer(question_id)`)
    this.db.exec(`CREATE INDEX IF NOT EXISTS idx_question_answer_blank ON question_answer(blank_area_id)`)
    this.db.exec(`CREATE INDEX IF NOT EXISTS idx_question_blank_question ON question_blank_area(question_id)`)
  }

  /**
   * 检查并添加列（如果不存在）
   * 用于向后兼容，当表已存在但缺少新字段时
   * 
   * @param {string} tableName 表名
   * @param {string} columnName 列名
   * @param {string} columnType 列类型
   */
  addColumnIfNotExists(tableName, columnName, columnType) {
    try {
      // 检查列是否存在
      const columns = this.db.prepare(`
        PRAGMA table_info(${tableName})
      `).all()

      const columnExists = columns.some(col => col.name === columnName)

      if (!columnExists) {
        console.log(`检测到表 ${tableName} 缺少字段 ${columnName}，正在添加...`)
        this.db.exec(`ALTER TABLE ${tableName} ADD COLUMN ${columnName} ${columnType}`)
        console.log(`✓ 字段 ${columnName} 添加成功`)
      }
    } catch (error) {
      // 如果表不存在，忽略错误（表会在initPaperTables中创建）
      if (!error.message.includes('no such table')) {
        console.warn(`添加字段 ${columnName} 失败:`, error.message)
      }
    }
  }

  /**
   * 检查表是否存在（用于首次使用前检查）
   * 如果表不存在，尝试创建
   * 性能优化：如果没有ZIP包，再同步表
   */
  ensurePaperTablesExist() {
    const paperTables = [
      'paper_package',
      'paper_index',
      'app_user_paper_info',
      'app_user_paper_question_result',
      'app_user_paper_question_blank_result',
      'question_media_index',
      // 业务数据表（如果没有ZIP包，需要同步这些数据）
      'paper',
      'paper_section',
      'paper_volume',
      'paper_intermission',
      'paper_question_group',
      'paper_question',
      'question',
      'question_category',
      'question_media',
      'question_answer',
      'question_blank_area'
    ]

    const existingTables = this.getExistingTables()
    const missingTables = paperTables.filter(t => !existingTables.includes(t))

    if (missingTables.length > 0) {
      console.log(`检测到缺失的试卷相关表，立即创建: ${missingTables.join(', ')}`)
      try {
        this.initPaperTables(missingTables)
        this.createPaperIndexes()
        console.log('✓ 试卷相关表创建成功')
      } catch (error) {
        console.error('创建试卷相关表失败:', error)
        throw error
      }
    }

    // 检查并添加paper_type字段（向后兼容）
    if (existingTables.includes('paper')) {
      this.addColumnIfNotExists('paper', 'paper_type', 'TEXT')
    }

    // 检查并添加 section_id 与 volume_code（向后兼容，答题结果增强）
    if (existingTables.includes('app_user_paper_question_result')) {
      this.addColumnIfNotExists('app_user_paper_question_result', 'section_id', 'INTEGER')
      this.addColumnIfNotExists('app_user_paper_question_result', 'volume_code', 'TEXT')
    }

    // 检查 paper_question 表是否需要重建（添加 section_id 列和修改唯一约束）
    if (existingTables.includes('paper_question')) {
      // 检查是否有 section_id 列
      const columns = this.db.prepare(`PRAGMA table_info(paper_question)`).all()
      const hasSectionId = columns.some(col => col.name === 'section_id')

      if (!hasSectionId) {
        console.log('⚠️ paper_question 表缺少 section_id 列，需要重建表...')
        try {
          // SQLite 不支持直接修改唯一约束，需要重建表
          // 1. 备份旧数据
          const oldData = this.db.prepare(`SELECT * FROM paper_question`).all()
          console.log(`备份 ${oldData.length} 条 paper_question 记录`)

          // 2. 删除旧表
          this.db.exec(`DROP TABLE IF EXISTS paper_question`)

          // 3. 创建新表（带 section_id 列和新的唯一约束）
          this.db.exec(`
            CREATE TABLE paper_question (
              id INTEGER PRIMARY KEY AUTOINCREMENT,
              paper_id INTEGER NOT NULL,
              question_id INTEGER NOT NULL,
              section_id INTEGER,
              section_order INTEGER DEFAULT 0,
              sort_order INTEGER DEFAULT 0,
              score REAL DEFAULT 0,
              create_time INTEGER,
              UNIQUE(paper_id, question_id, section_id)
            )
          `)

          // 4. 恢复旧数据（section_id 为 NULL）
          if (oldData.length > 0) {
            const insertStmt = this.db.prepare(`
              INSERT INTO paper_question (paper_id, question_id, section_id, section_order, sort_order, score, create_time)
              VALUES (?, ?, NULL, 0, ?, ?, ?)
            `)
            for (const row of oldData) {
              insertStmt.run(row.paper_id, row.question_id, row.sort_order || 0, row.score || 0, row.create_time || Date.now())
            }
            console.log(`✓ 已恢复 ${oldData.length} 条 paper_question 记录`)
          }

          // 5. 重建索引
          this.db.exec(`CREATE INDEX IF NOT EXISTS idx_paper_question_paper ON paper_question(paper_id)`)
          this.db.exec(`CREATE INDEX IF NOT EXISTS idx_paper_question_question ON paper_question(question_id)`)
          this.db.exec(`CREATE INDEX IF NOT EXISTS idx_paper_question_sort ON paper_question(paper_id, sort_order)`)
          this.db.exec(`CREATE INDEX IF NOT EXISTS idx_paper_question_section ON paper_question(paper_id, section_id)`)

          console.log('✓ paper_question 表重建完成')
          console.log('⚠️ 注意：需要重新同步试卷包以填充 section_id 数据')
        } catch (error) {
          console.error('重建 paper_question 表失败:', error)
        }
      }
    }
  }

  getDB() {
    return this.db
  }

  close() {
    if (this.db) {
      this.db.close()
    }
  }
}

module.exports = DB
