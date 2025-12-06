/**
 * 迁移 002：为 paper 表添加 year, month, province, custom_name 字段
 * 并添加唯一索引
 */
module.exports = {
  up: (db) => {
    // 1. 添加字段（如果不存在）
    try {
      db.exec(`
        ALTER TABLE paper 
        ADD COLUMN year INTEGER;
      `);
      console.log('✓ 已添加 year 字段');
    } catch (e) {
      if (!e.message.includes('duplicate column name')) {
        throw e;
      }
      console.log('  year 字段已存在，跳过');
    }

    try {
      db.exec(`
        ALTER TABLE paper 
        ADD COLUMN month INTEGER;
      `);
      console.log('✓ 已添加 month 字段');
    } catch (e) {
      if (!e.message.includes('duplicate column name')) {
        throw e;
      }
      console.log('  month 字段已存在，跳过');
    }

    try {
      db.exec(`
        ALTER TABLE paper 
        ADD COLUMN province TEXT;
      `);
      console.log('✓ 已添加 province 字段');
    } catch (e) {
      if (!e.message.includes('duplicate column name')) {
        throw e;
      }
      console.log('  province 字段已存在，跳过');
    }

    try {
      db.exec(`
        ALTER TABLE paper 
        ADD COLUMN custom_name TEXT;
      `);
      console.log('✓ 已添加 custom_name 字段');
    } catch (e) {
      if (!e.message.includes('duplicate column name')) {
        throw e;
      }
      console.log('  custom_name 字段已存在，跳过');
    }

    // 2. 创建普通索引（非唯一）：用于查询和排序
    // 注意：不创建唯一索引，允许同一组合创建多条记录（通过 custom_name 区分）
    // 客户端同步时会选择该组合中版本最新的记录
    try {
      db.exec(`
        CREATE INDEX IF NOT EXISTS idx_paper_year_month_province_type 
        ON paper(year, month, province, paper_type);
      `);
      console.log('✓ 已创建索引 idx_paper_year_month_province_type（非唯一）');
    } catch (e) {
      // SQLite 的 IF NOT EXISTS 应该能处理重复创建，但为了安全还是捕获异常
      if (!e.message.includes('already exists') && !e.message.includes('duplicate')) {
        throw e;
      }
      console.log('  索引已存在，跳过');
    }
  },

  down: (db) => {
    // 回滚：删除索引和字段
    try {
      db.exec(`DROP INDEX IF EXISTS idx_paper_year_month_province_type;`);
      console.log('✓ 已删除唯一索引');
    } catch (e) {
      console.log('  索引不存在，跳过');
    }

    try {
      db.exec(`ALTER TABLE paper DROP COLUMN custom_name;`);
      console.log('✓ 已删除 custom_name 字段');
    } catch (e) {
      console.log('  custom_name 字段不存在，跳过');
    }

    try {
      db.exec(`ALTER TABLE paper DROP COLUMN province;`);
      console.log('✓ 已删除 province 字段');
    } catch (e) {
      console.log('  province 字段不存在，跳过');
    }

    try {
      db.exec(`ALTER TABLE paper DROP COLUMN month;`);
      console.log('✓ 已删除 month 字段');
    } catch (e) {
      console.log('  month 字段不存在，跳过');
    }

    try {
      db.exec(`ALTER TABLE paper DROP COLUMN year;`);
      console.log('✓ 已删除 year 字段');
    } catch (e) {
      console.log('  year 字段不存在，跳过');
    }
  }
};

