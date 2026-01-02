const axios = require('axios')

const { app } = require('electron')

const API_BASE_URL = app.isPackaged ? 'http://47.94.192.7:8818/prod-api' : (process.env.API_BASE_URL || 'http://localhost:8080')

/**
 * 表结构同步服务
 * 从后端API获取MySQL表结构，并自动同步到SQLite
 * 用于检测未知的DDL变更
 */
class SchemaSyncService {
  constructor(db) {
    this.db = db.getDB()
  }

  /**
   * 从后端获取表结构（需要后端提供API）
   * @param {string} tableName - 表名
   * @param {string} token - 认证token
   * @returns {Promise<Array>} 字段列表
   */
  async getTableSchemaFromServer(tableName, token) {
    try {
      // 调用后端API获取表结构
      // 注意：需要后端提供这个接口
      const response = await axios.post(
        `${API_BASE_URL}/student/sync/table/schema`,
        { tableName },
        {
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
          }
        }
      )

      if (response.data && response.data.code === 200 && response.data.data) {
        return response.data.data.columns || []
      }

      return []
    } catch (error) {
      console.error(`获取表 ${tableName} 的结构失败:`, error.message)
      return null
    }
  }

  /**
   * 获取SQLite表的字段列表
   * @param {string} tableName - 表名
   * @returns {Array} 字段列表
   */
  getSQLiteTableColumns(tableName) {
    try {
      const columns = this.db.prepare(`PRAGMA table_info(${tableName})`).all()
      return columns.map(col => ({
        name: col.name,
        type: col.type,
        notnull: col.notnull,
        dflt_value: col.dflt_value,
        pk: col.pk
      }))
    } catch (error) {
      if (error.message.includes('no such table')) {
        return []
      }
      throw error
    }
  }

  /**
   * 将MySQL类型转换为SQLite类型
   * @param {string} mysqlType - MySQL类型
   * @returns {string} SQLite类型
   */
  convertMySQLTypeToSQLite(mysqlType) {
    if (!mysqlType) return 'TEXT'

    const type = mysqlType.toUpperCase()

    // 整数类型
    if (type.includes('INT')) {
      if (type.includes('BIGINT')) {
        return 'INTEGER'
      }
      return 'INTEGER'
    }

    // 浮点类型
    if (type.includes('DECIMAL') || type.includes('FLOAT') || type.includes('DOUBLE')) {
      return 'REAL'
    }

    // 文本类型
    if (type.includes('TEXT') || type.includes('VARCHAR') || type.includes('CHAR')) {
      return 'TEXT'
    }

    // 日期时间类型
    if (type.includes('DATE') || type.includes('TIME')) {
      return 'INTEGER' // SQLite使用INTEGER存储时间戳
    }

    // 布尔类型
    if (type.includes('BOOLEAN') || type.includes('TINYINT(1)')) {
      return 'INTEGER'
    }

    // 默认返回TEXT
    return 'TEXT'
  }

  /**
   * 同步表结构（对比MySQL和SQLite，添加缺失字段）
   * @param {string} tableName - 表名
   * @param {string} token - 认证token
   * @returns {Promise<Object>} 同步结果
   */
  async syncTableSchema(tableName, token) {
    try {
      console.log(`开始同步表结构: ${tableName}`)

      // 1. 从后端获取MySQL表结构
      const mysqlColumns = await this.getTableSchemaFromServer(tableName, token)
      if (!mysqlColumns || mysqlColumns.length === 0) {
        console.warn(`无法获取表 ${tableName} 的MySQL结构，跳过同步`)
        return { success: false, message: '无法获取MySQL表结构' }
      }

      // 2. 获取SQLite表结构
      const sqliteColumns = this.getSQLiteTableColumns(tableName)
      if (sqliteColumns.length === 0) {
        console.warn(`表 ${tableName} 在SQLite中不存在，跳过同步（应由迁移脚本创建）`)
        return { success: false, message: '表不存在' }
      }

      // 3. 对比字段，找出缺失的字段
      const sqliteColumnNames = new Set(sqliteColumns.map(col => col.name))
      const missingColumns = mysqlColumns.filter(col => !sqliteColumnNames.has(col.column_name))

      if (missingColumns.length === 0) {
        console.log(`✓ 表 ${tableName} 结构已同步，无缺失字段`)
        return { success: true, addedColumns: 0 }
      }

      // 4. 添加缺失的字段
      console.log(`检测到 ${missingColumns.length} 个缺失字段，开始添加...`)
      let addedCount = 0

      for (const column of missingColumns) {
        try {
          const columnName = column.column_name
          const columnType = this.convertMySQLTypeToSQLite(column.column_type)

          console.log(`添加字段: ${columnName} (${columnType})`)

          this.db.exec(`ALTER TABLE ${tableName} ADD COLUMN ${columnName} ${columnType}`)
          addedCount++

          console.log(`✓ 字段 ${columnName} 添加成功`)
        } catch (error) {
          console.error(`添加字段 ${column.column_name} 失败:`, error.message)
        }
      }

      console.log(`✓ 表 ${tableName} 结构同步完成，添加了 ${addedCount} 个字段`)
      return { success: true, addedColumns: addedCount, columns: missingColumns.map(c => c.column_name) }
    } catch (error) {
      console.error(`同步表 ${tableName} 结构失败:`, error)
      return { success: false, message: error.message }
    }
  }

  /**
   * 同步多个表的结构
   * @param {Array<string>} tableNames - 表名列表
   * @param {string} token - 认证token
   * @returns {Promise<Object>} 同步结果
   */
  async syncMultipleTables(tableNames, token) {
    const results = {}
    let totalAdded = 0

    for (const tableName of tableNames) {
      const result = await this.syncTableSchema(tableName, token)
      results[tableName] = result
      if (result.success && result.addedColumns) {
        totalAdded += result.addedColumns
      }
    }

    return {
      success: true,
      totalAdded,
      results
    }
  }
}

module.exports = SchemaSyncService



