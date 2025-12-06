module.exports = {
  up: (db) => {
    console.log('执行 011_fix_trial_listen_fields.js 迁移');
    try {
      // 1. 添加 trial_listen_audio_text 字段
      try {
        db.exec(`
          ALTER TABLE paper ADD COLUMN trial_listen_audio_text TEXT;
        `);
        console.log('  ✓ 已添加 trial_listen_audio_text 字段到 paper 表');
      } catch (e) {
        if (e.message.includes('duplicate column name')) {
          console.warn('  字段 trial_listen_audio_text 已存在，跳过添加');
        } else {
          console.error('  添加 trial_listen_audio_text 字段失败:', e.message);
          throw e;
        }
      }

      // 2. 添加 operate_listen_text 字段
      try {
        db.exec(`
          ALTER TABLE paper ADD COLUMN operate_listen_text TEXT;
        `);
        console.log('  ✓ 已添加 operate_listen_text 字段到 paper 表');
      } catch (e) {
        if (e.message.includes('duplicate column name')) {
          console.warn('  字段 operate_listen_text 已存在，跳过添加');
        } else {
          console.error('  添加 operate_listen_text 字段失败:', e.message);
          throw e;
        }
      }

      // 3. 添加 operate_listen_image_url 字段
      try {
        db.exec(`
          ALTER TABLE paper ADD COLUMN operate_listen_image_url TEXT;
        `);
        console.log('  ✓ 已添加 operate_listen_image_url 字段到 paper 表');
      } catch (e) {
        if (e.message.includes('duplicate column name')) {
          console.warn('  字段 operate_listen_image_url 已存在，跳过添加');
        } else {
          console.error('  添加 operate_listen_image_url 字段失败:', e.message);
          throw e;
        }
      }

      // 4. 添加 operate_listen_image_path 字段
      try {
        db.exec(`
          ALTER TABLE paper ADD COLUMN operate_listen_image_path TEXT;
        `);
        console.log('  ✓ 已添加 operate_listen_image_path 字段到 paper 表');
      } catch (e) {
        if (e.message.includes('duplicate column name')) {
          console.warn('  字段 operate_listen_image_path 已存在，跳过添加');
        } else {
          console.error('  添加 operate_listen_image_path 字段失败:', e.message);
          throw e;
        }
      }

      // 5. 迁移数据：将 trial_listen_text 复制到 operate_listen_text
      try {
        db.exec(`
          UPDATE paper
          SET operate_listen_text = trial_listen_text
          WHERE operate_listen_text IS NULL AND trial_listen_text IS NOT NULL;
        `);
        console.log('  ✓ 已迁移 trial_listen_text 数据到 operate_listen_text');
      } catch (e) {
        if (e.message.includes('no such column: trial_listen_text')) {
          console.warn('  字段 trial_listen_text 不存在，跳过数据迁移');
        } else {
          console.warn('  迁移 trial_listen_text 数据失败:', e.message);
        }
      }

      // 6. 迁移数据：将 trial_listen_image_url 复制到 operate_listen_image_url
      try {
        db.exec(`
          UPDATE paper
          SET operate_listen_image_url = trial_listen_image_url
          WHERE operate_listen_image_url IS NULL AND trial_listen_image_url IS NOT NULL;
        `);
        console.log('  ✓ 已迁移 trial_listen_image_url 数据到 operate_listen_image_url');
      } catch (e) {
        if (e.message.includes('no such column: trial_listen_image_url')) {
          console.warn('  字段 trial_listen_image_url 不存在，跳过数据迁移');
        } else {
          console.warn('  迁移 trial_listen_image_url 数据失败:', e.message);
        }
      }

      // 7. 迁移数据：将 trial_listen_image_path 复制到 operate_listen_image_path
      try {
        db.exec(`
          UPDATE paper
          SET operate_listen_image_path = trial_listen_image_path
          WHERE operate_listen_image_path IS NULL AND trial_listen_image_path IS NOT NULL;
        `);
        console.log('  ✓ 已迁移 trial_listen_image_path 数据到 operate_listen_image_path');
      } catch (e) {
        if (e.message.includes('no such column: trial_listen_image_path')) {
          console.warn('  字段 trial_listen_image_path 不存在，跳过数据迁移');
        } else {
          console.warn('  迁移 trial_listen_image_path 数据失败:', e.message);
        }
      }

      // 注意：SQLite 不支持直接删除列，旧字段保留但不使用
      // 如果需要删除旧字段，需要重建表（这里不执行，避免数据丢失）

      console.log('✓ 迁移 011 执行完成');
    } catch (e) {
      console.error('执行 011_fix_trial_listen_fields.js 迁移失败:', e.message);
      throw e;
    }
  },
  down: (db) => {
    console.log('回滚 011_fix_trial_listen_fields.js 迁移');
    // SQLite 不支持直接删除列，通常在 down 迁移中会重建表或不执行操作
    // 为了简化，这里不提供 down 操作，实际生产环境需要更复杂的处理
  }
};









