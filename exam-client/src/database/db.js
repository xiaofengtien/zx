const Database = require('better-sqlite3')
const path = require('path')
const fs = require('fs')
const { app } = require('electron')

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
    console.log('数据库文件路径:', this.dbPath)
    console.log('数据库文件是否存在:', fs.existsSync(this.dbPath))
    
    // 打开数据库
    this.db = new Database(this.dbPath)
    
    // 执行数据库迁移（在初始化表结构之前）
    this.runMigrations()
    
    // 初始化表结构（优化：快速检查 + 异步初始化）
    this.initTables()
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
    
    // 3. 异步初始化非关键表（可以延迟，带重试机制）
    // 性能优化：如果没有ZIP包，再同步表
    setImmediate(() => {
      this.initPaperTablesAsync(missingTables, 0) // 从第0次重试开始
    })
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
    const criticalTables = ['student_credentials', 'student_papers', 'dict_data', 'student_archive', 'download_status']
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
      
      // 创建关键表索引
      this.createCriticalIndexes()
      
      console.log(`✓ 关键表创建成功: ${toCreate.join(', ')}`)
    } catch (error) {
      console.error('创建关键表失败:', error)
      throw error // 关键表创建失败，必须抛出异常
    }
  }

  /**
   * 异步初始化试卷相关表（带重试机制）
   * 性能优化：如果没有ZIP包，再同步表
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
      
      // 如果表已存在，检查是否需要添加字段（向后兼容）
      this.addColumnIfNotExists('paper', 'paper_type', 'TEXT')
      this.addColumnIfNotExists('paper', 'practice_limit', 'INTEGER DEFAULT 0')
      this.addColumnIfNotExists('paper', 'trial_listen_enabled', 'INTEGER DEFAULT 0')
      this.addColumnIfNotExists('paper', 'trial_listen_audio_url', 'TEXT')
      this.addColumnIfNotExists('paper', 'trial_listen_audio_path', 'TEXT')
      this.addColumnIfNotExists('paper', 'trial_listen_audio_duration', 'INTEGER')
      this.addColumnIfNotExists('paper', 'trial_listen_audio_text', 'TEXT')
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
      
      // 确保 paper_section 表有 volume_id 字段（如果表已存在但缺少该字段）
      this.addColumnIfNotExists('paper_section', 'volume_id', 'INTEGER')
      // 确保 paper_section 表有 audio_play_count 字段
      this.addColumnIfNotExists('paper_section', 'audio_play_count', 'INTEGER DEFAULT 1')
      // 确保 paper_section 表有 answer_time 字段
      this.addColumnIfNotExists('paper_section', 'answer_time', 'INTEGER DEFAULT 5')
      
      // 确保 paper_intermission 表有音频相关字段
      this.addColumnIfNotExists('paper_intermission', 'intermission_audio_url', 'TEXT')
      this.addColumnIfNotExists('paper_intermission', 'intermission_audio_path', 'TEXT')
      this.addColumnIfNotExists('paper_intermission', 'intermission_audio_duration', 'INTEGER')
      // 添加 volumeId 字段（因为 volumeCode 会重复，需要通过 volumeId 来唯一标识卷别）
      this.addColumnIfNotExists('paper_intermission', 'from_volume_id', 'INTEGER')
      this.addColumnIfNotExists('paper_intermission', 'to_volume_id', 'INTEGER')
    }
    
    // 试卷-题目关联表
    if (toCreate.includes('paper_question')) {
      this.db.exec(`
        CREATE TABLE IF NOT EXISTS paper_question (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          paper_id INTEGER NOT NULL,
          question_id INTEGER NOT NULL,
          sort_order INTEGER DEFAULT 0,
          score REAL DEFAULT 0,
          create_time INTEGER,
          UNIQUE(paper_id, question_id)
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
    
    // 答题结果表索引
    this.db.exec(`CREATE INDEX IF NOT EXISTS idx_paper_info_user ON app_user_paper_info(app_user_id)`)
    this.db.exec(`CREATE INDEX IF NOT EXISTS idx_paper_info_sync ON app_user_paper_info(sync_status)`)
    this.db.exec(`CREATE INDEX IF NOT EXISTS idx_paper_result_user ON app_user_paper_question_result(paper_id, app_user_id)`)
    this.db.exec(`CREATE INDEX IF NOT EXISTS idx_paper_result_sync ON app_user_paper_question_result(sync_status)`)
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
