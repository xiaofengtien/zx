/**
 * 迁移 003：试卷功能增强 - 添加新表和新字段
 * 对应后端 SQL: sql/paper_enhancement_phase1.sql
 */
module.exports = {
  up: (db) => {
    console.log('开始执行迁移 003: 试卷功能增强...');

    // ============================================
    // 1. 更新 paper 表 - 添加新字段
    // ============================================
    const paperFields = [
      { name: 'practice_limit', type: 'INTEGER DEFAULT 0', comment: '练习次数限制（0表示不限制）' },
      { name: 'trial_listen_enabled', type: 'INTEGER DEFAULT 0', comment: '是否启用试听（0-否，1-是）' },
      { name: 'trial_listen_text', type: 'TEXT', comment: '试听提示文案' },
      { name: 'notes', type: 'TEXT', comment: '注意事项' },
      { name: 'notes_display_mode', type: 'TEXT DEFAULT "before_exam"', comment: '注意事项显示时机：before_exam-考试前显示一次，before_section-每大题前显示' }
    ];

    paperFields.forEach(field => {
      try {
        db.exec(`ALTER TABLE paper ADD COLUMN ${field.name} ${field.type};`);
        console.log(`✓ 已添加 paper.${field.name} 字段`);
      } catch (e) {
        if (!e.message.includes('duplicate column name')) {
          throw e;
        }
        console.log(`  paper.${field.name} 字段已存在，跳过`);
      }
    });

    // ============================================
    // 2. 创建 paper_volume 表（卷别表）
    // ============================================
    try {
      db.exec(`
        CREATE TABLE IF NOT EXISTS paper_volume (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          paper_id INTEGER NOT NULL,
          volume_code TEXT NOT NULL,
          volume_name TEXT NOT NULL,
          volume_order INTEGER DEFAULT 1,
          create_time INTEGER,
          update_time INTEGER,
          UNIQUE(paper_id, volume_code)
        );
      `);
      console.log('✓ 已创建 paper_volume 表');

      // 创建索引
      try {
        db.exec(`CREATE INDEX IF NOT EXISTS idx_paper_volume_paper_id ON paper_volume(paper_id);`);
        db.exec(`CREATE INDEX IF NOT EXISTS idx_paper_volume_code ON paper_volume(paper_id, volume_code);`);
        console.log('✓ 已创建 paper_volume 表索引');
      } catch (e) {
        if (!e.message.includes('already exists')) {
          throw e;
        }
      }
    } catch (e) {
      if (!e.message.includes('already exists')) {
        throw e;
      }
      console.log('  paper_volume 表已存在，跳过');
    }

    // ============================================
    // 3. 创建 paper_section 表（大题表）
    // ============================================
    try {
      db.exec(`
        CREATE TABLE IF NOT EXISTS paper_section (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          paper_id INTEGER NOT NULL,
          volume_id INTEGER NOT NULL,
          volume_code TEXT,
          section_name TEXT NOT NULL,
          section_order INTEGER DEFAULT 1,
          question_count INTEGER DEFAULT 0,
          total_score REAL DEFAULT 0,
          score_per_question REAL DEFAULT 0,
          instruction_text TEXT,
          create_time INTEGER,
          update_time INTEGER
        );
      `);
      console.log('✓ 已创建 paper_section 表');

      // 创建索引
      try {
        db.exec(`CREATE INDEX IF NOT EXISTS idx_paper_section_paper_id ON paper_section(paper_id);`);
        db.exec(`CREATE INDEX IF NOT EXISTS idx_paper_section_volume_id ON paper_section(volume_id);`);
        db.exec(`CREATE INDEX IF NOT EXISTS idx_paper_section_volume ON paper_section(paper_id, volume_id, section_order);`);
        console.log('✓ 已创建 paper_section 表索引');
      } catch (e) {
        if (!e.message.includes('already exists')) {
          throw e;
        }
      }
    } catch (e) {
      if (!e.message.includes('already exists')) {
        throw e;
      }
      console.log('  paper_section 表已存在，检查是否需要添加 volume_id 字段');
      
      // 检查是否需要添加 volume_id 字段（用于升级已存在的表）
      try {
        const tableInfo = db.exec(`PRAGMA table_info(paper_section);`);
        const hasVolumeId = tableInfo[0]?.values?.some(col => col[1] === 'volume_id');
        
        if (!hasVolumeId) {
          console.log('  检测到 paper_section 表缺少 volume_id 字段，开始升级...');
          
          // 添加 volume_id 字段
          db.exec(`ALTER TABLE paper_section ADD COLUMN volume_id INTEGER;`);
          console.log('  ✓ 已添加 volume_id 字段');
          
          // 更新现有数据：根据 volume_code 查找对应的 volume_id
          // 注意：这需要 paper_volume 表已经存在且有数据
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
            console.log('  ✓ 已更新现有数据的 volume_id');
          } catch (e) {
            console.log('  警告：更新现有数据的 volume_id 失败，可能需要手动处理:', e.message);
          }
          
          // 创建新索引
          try {
            db.exec(`CREATE INDEX IF NOT EXISTS idx_paper_section_volume_id ON paper_section(volume_id);`);
            db.exec(`CREATE INDEX IF NOT EXISTS idx_paper_section_volume_new ON paper_section(paper_id, volume_id, section_order);`);
            console.log('  ✓ 已创建新索引');
          } catch (e) {
            if (!e.message.includes('already exists')) {
              console.log('  警告：创建索引失败:', e.message);
            }
          }
        } else {
          console.log('  paper_section 表已包含 volume_id 字段，跳过升级');
        }
      } catch (e) {
        console.log('  检查 volume_id 字段时出错:', e.message);
      }
    }

    // ============================================
    // 4. 创建 paper_intermission 表（中场配置表）
    // ============================================
    try {
      db.exec(`
        CREATE TABLE IF NOT EXISTS paper_intermission (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          paper_id INTEGER NOT NULL,
          from_volume TEXT NOT NULL,
          to_volume TEXT NOT NULL,
          intermission_text TEXT,
          intermission_audio_url TEXT,
          intermission_audio_path TEXT,
          intermission_audio_duration INTEGER,
          can_skip INTEGER DEFAULT 0,
          create_time INTEGER,
          update_time INTEGER,
          UNIQUE(paper_id, from_volume, to_volume)
        );
      `);
      console.log('✓ 已创建 paper_intermission 表');

      // 如果表已存在，检查并添加音频字段（向后兼容）
      try {
        const tableInfo = db.prepare(`PRAGMA table_info(paper_intermission)`).all();
        const hasAudioUrl = tableInfo.some(col => col.name === 'intermission_audio_url');
        const hasAudioPath = tableInfo.some(col => col.name === 'intermission_audio_path');
        const hasAudioDuration = tableInfo.some(col => col.name === 'intermission_audio_duration');
        
        if (!hasAudioUrl) {
          db.exec(`ALTER TABLE paper_intermission ADD COLUMN intermission_audio_url TEXT;`);
          console.log('  ✓ 已添加 intermission_audio_url 字段');
        }
        if (!hasAudioPath) {
          db.exec(`ALTER TABLE paper_intermission ADD COLUMN intermission_audio_path TEXT;`);
          console.log('  ✓ 已添加 intermission_audio_path 字段');
        }
        if (!hasAudioDuration) {
          db.exec(`ALTER TABLE paper_intermission ADD COLUMN intermission_audio_duration INTEGER;`);
          console.log('  ✓ 已添加 intermission_audio_duration 字段');
        }
      } catch (e) {
        if (!e.message.includes('duplicate column name')) {
          console.log('  检查/添加音频字段时出错:', e.message);
        }
      }

      // 创建索引
      try {
        db.exec(`CREATE INDEX IF NOT EXISTS idx_paper_intermission_paper_id ON paper_intermission(paper_id);`);
        console.log('✓ 已创建 paper_intermission 表索引');
      } catch (e) {
        if (!e.message.includes('already exists')) {
          throw e;
        }
      }
    } catch (e) {
      if (!e.message.includes('already exists')) {
        throw e;
      }
      console.log('  paper_intermission 表已存在，跳过');
    }

    // ============================================
    // 5. 更新 paper_question 表 - 添加 section_id 和 section_order
    // ============================================
    const paperQuestionFields = [
      { name: 'section_id', type: 'INTEGER', comment: '大题ID（关联paper_section.id）' },
      { name: 'section_order', type: 'INTEGER DEFAULT 0', comment: '题目在大题内的排序' }
    ];

    paperQuestionFields.forEach(field => {
      try {
        db.exec(`ALTER TABLE paper_question ADD COLUMN ${field.name} ${field.type};`);
        console.log(`✓ 已添加 paper_question.${field.name} 字段`);
      } catch (e) {
        if (!e.message.includes('duplicate column name')) {
          throw e;
        }
        console.log(`  paper_question.${field.name} 字段已存在，跳过`);
      }
    });

    // ============================================
    // 6. 更新 question 表 - 添加讲解相关字段
    // ============================================
    const questionFields = [
      { name: 'explanation_enabled', type: 'INTEGER DEFAULT 0', comment: '是否有答题讲解（0-否，1-是）' },
      { name: 'explanation_text', type: 'TEXT', comment: '讲解文字' },
      { name: 'explanation_delay_seconds', type: 'INTEGER DEFAULT 2', comment: '讲解显示延迟（秒）' }
    ];

    questionFields.forEach(field => {
      try {
        db.exec(`ALTER TABLE question ADD COLUMN ${field.name} ${field.type};`);
        console.log(`✓ 已添加 question.${field.name} 字段`);
      } catch (e) {
        if (!e.message.includes('duplicate column name')) {
          throw e;
        }
        console.log(`  question.${field.name} 字段已存在，跳过`);
      }
    });

    // ============================================
    // 7. 更新 question_media 表 - 添加关联字段
    // ============================================
    const questionMediaFields = [
      { name: 'paper_id', type: 'INTEGER', comment: '试卷ID（关联paper.id）' },
      { name: 'volume_id', type: 'INTEGER', comment: '卷别ID（关联paper_volume.id）' },
      { name: 'section_id', type: 'INTEGER', comment: '大题ID（关联paper_section.id）' },
      { name: 'intermission_id', type: 'INTEGER', comment: '中场配置ID（关联paper_intermission.id）' }
    ];

    questionMediaFields.forEach(field => {
      try {
        db.exec(`ALTER TABLE question_media ADD COLUMN ${field.name} ${field.type};`);
        console.log(`✓ 已添加 question_media.${field.name} 字段`);
      } catch (e) {
        if (!e.message.includes('duplicate column name')) {
          throw e;
        }
        console.log(`  question_media.${field.name} 字段已存在，跳过`);
      }
    });

    // 创建索引
    try {
      db.exec(`CREATE INDEX IF NOT EXISTS idx_question_media_paper_id ON question_media(paper_id);`);
      db.exec(`CREATE INDEX IF NOT EXISTS idx_question_media_volume_id ON question_media(volume_id);`);
      db.exec(`CREATE INDEX IF NOT EXISTS idx_question_media_section_id ON question_media(section_id);`);
      db.exec(`CREATE INDEX IF NOT EXISTS idx_question_media_intermission_id ON question_media(intermission_id);`);
      console.log('✓ 已创建 question_media 表索引');
    } catch (e) {
      if (!e.message.includes('already exists')) {
        throw e;
      }
    }

    // ============================================
    // 8. 更新 app_user_paper_info 表 - 添加新字段
    // ============================================
    const appUserPaperInfoFields = [
      { name: 'practice_count', type: 'INTEGER DEFAULT 0', comment: '已练习次数' },
      { name: 'last_practice_time', type: 'INTEGER', comment: '最后练习时间' },
      { name: 'volume_status', type: 'TEXT', comment: '各卷状态（JSON格式）' },
      { name: 'volume_submit_time', type: 'TEXT', comment: '各卷提交时间（JSON格式）' },
      { name: 'intermission_played', type: 'TEXT', comment: '中场音频播放状态（JSON格式）' },
      { name: 'assigned_seat_number', type: 'TEXT', comment: '分配的机位号' },
      { name: 'actual_seat_number', type: 'TEXT', comment: '实际坐的机位号' }
    ];

    appUserPaperInfoFields.forEach(field => {
      try {
        db.exec(`ALTER TABLE app_user_paper_info ADD COLUMN ${field.name} ${field.type};`);
        console.log(`✓ 已添加 app_user_paper_info.${field.name} 字段`);
      } catch (e) {
        if (!e.message.includes('duplicate column name')) {
          throw e;
        }
        console.log(`  app_user_paper_info.${field.name} 字段已存在，跳过`);
      }
    });

    console.log('✓ 迁移 003 执行完成');
  },

  down: (db) => {
    console.log('开始回滚迁移 003...');

    // 注意：回滚操作需要谨慎，因为可能已有数据
    // 这里只删除表，不删除字段（避免数据丢失）

    try {
      db.exec(`DROP TABLE IF EXISTS paper_intermission;`);
      console.log('✓ 已删除 paper_intermission 表');
    } catch (e) {
      console.log('  删除 paper_intermission 表失败:', e.message);
    }

    try {
      db.exec(`DROP TABLE IF EXISTS paper_section;`);
      console.log('✓ 已删除 paper_section 表');
    } catch (e) {
      console.log('  删除 paper_section 表失败:', e.message);
    }

    try {
      db.exec(`DROP TABLE IF EXISTS paper_volume;`);
      console.log('✓ 已删除 paper_volume 表');
    } catch (e) {
      console.log('  删除 paper_volume 表失败:', e.message);
    }

    console.log('✓ 迁移 003 回滚完成（注意：字段未删除，避免数据丢失）');
  }
};


