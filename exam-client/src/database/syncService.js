const axios = require('axios')
const { getConfig } = require('./config')
const PaperService = require('./paperService')

// 后端API地址
const API_BASE_URL = process.env.API_BASE_URL || 'http://localhost:8080'

/**
 * 数据同步服务
 * 负责在应用启动时从后端同步数据到本地 SQLite
 */
class SyncService {
  constructor(db) {
    this.db = db.getDB()
    this.config = getConfig()
    this.paperService = new PaperService(db)
  }

  /**
   * 同步所有数据（覆盖同步）
   * @param {string} token - 学员 token（必需）
   */
  async syncAll(token = null) {
    console.log('=== 开始同步所有数据到 SQLite ===')
    console.log('Token 是否提供:', token ? '是' : '否')
    
    if (!token) {
      console.error('同步数据需要 token')
      return { success: false, message: '同步数据需要 token' }
    }
    
    try {
      // 使用新的统一同步接口（学员专用）
      console.log('调用学员同步接口: /student/syncData')
      const headers = {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      }
      
      const response = await axios.get(`${API_BASE_URL}/student/syncData`, {
        headers
      })
      
      console.log('同步接口响应状态:', response.status)
      console.log('同步接口响应数据:', JSON.stringify(response.data, null, 2))
      
      if (response.data && response.data.code === 200) {
        // 1. 同步学员档案数据
        if (response.data.archives) {
          console.log('步骤 1: 同步学员档案数据...')
          await this.syncStudentArchivesFromResponse(response.data.archives)
          console.log('步骤 1: 学员档案数据同步完成')
        }
        
        // 2. 同步字典数据
        if (response.data.dictData) {
          console.log('步骤 2: 同步字典数据...')
          await this.syncDictDataFromResponse(response.data.dictData)
          console.log('步骤 2: 字典数据同步完成')
        }
        
        // 3. 同步试卷包或业务数据
        // 优先尝试ZIP包方案，失败则降级到业务数据表同步
        if (token) {
          console.log('步骤 3: 开始同步试卷数据...')
          
          // 3.1 先扫描本地手动放置的ZIP包（支持线下手动发配）
          try {
            // 使用已创建的 paperService 实例，无需重新创建
            const scanResult = await this.paperService.scanAndImportLocalPackages()
            if (scanResult.imported > 0) {
              console.log(`步骤 3.1: 扫描并导入了 ${scanResult.imported} 个本地手动放置的ZIP包`)
            }
          } catch (error) {
            console.warn('步骤 3.1: 扫描本地ZIP包失败:', error.message)
            console.error('扫描本地ZIP包详细错误:', error)
            // 继续执行后续同步
          }
          
          // 3.2 优先尝试ZIP包方案（从服务器下载）
          // 优化：不阻塞，在后台静默下载
          let zipSyncSuccess = false
          try {
            console.log('步骤 3.2: 尝试使用ZIP包方案（推荐，后台下载）...')
            // 注意：syncZipPackages 会从本地数据库获取 applicablePaperIds
            // 所以需要确保步骤1（同步学员档案）已经完成
            
            // 优化：不等待下载完成，立即返回，让用户先进入系统
            // ZIP包在后台下载，通过进度回调更新状态
            this.syncZipPackages(token).then(zipSyncResult => {
              if (zipSyncResult && zipSyncResult.success && zipSyncResult.successCount > 0) {
                console.log('步骤 3.2: ZIP包后台下载成功')
              } else {
                console.warn('步骤 3.2: ZIP包后台下载未成功，可能服务器上没有ZIP包')
                if (zipSyncResult && zipSyncResult.message) {
                  console.warn('步骤 3.2: 下载失败原因:', zipSyncResult.message)
                }
              }
            }).catch(error => {
              console.warn('步骤 3.2: ZIP包后台下载失败:', error.message)
              console.error('步骤 3.2: 错误详情:', error)
            })
            
            // 检查本地是否已有ZIP包（如果有，认为同步成功，允许用户使用）
            const hasLocalZip = await this.checkZipPackageExists()
            if (hasLocalZip) {
              console.log('步骤 3.2: 检测到本地已有ZIP包，允许用户使用')
              zipSyncSuccess = true
            } else {
              // 本地没有ZIP包，但后台正在下载，也认为可以继续（不阻塞）
              console.log('步骤 3.2: 本地没有ZIP包，后台正在下载，不阻塞用户操作')
              zipSyncSuccess = true // 允许用户先进入系统
            }
          } catch (error) {
            console.warn('步骤 3.2: ZIP包同步检查失败:', error.message)
            // 即使检查失败，也允许用户进入系统（后台会继续下载）
            zipSyncSuccess = true
          }
          
          // 如果ZIP包同步失败，检查是否需要降级到业务数据表同步
          if (!zipSyncSuccess) {
            // 检查本地是否已有ZIP包（可能是之前下载的）
            const hasLocalZip = await this.checkZipPackageExists()
            
            if (hasLocalZip) {
              console.log('步骤 3: 检测到本地已有ZIP包，跳过业务数据同步')
            } else {
              // 没有ZIP包，检查配置是否允许表同步
              const tableSyncEnabled = this.config.isTableSyncEnabled()
              
              if (!tableSyncEnabled) {
                // 配置关闭，不允许表同步
                const errorMsg = '未检测到ZIP包，且业务数据表同步已禁用。\n\n' +
                  '解决方案：\n' +
                  '1. 请在后台管理-试卷管理中生成试卷包并上传到OSS\n' +
                  '2. 或者联系开发管理者开启业务数据表同步功能（仅适用于小数据量场景）'
                
                console.error('❌ 数据同步失败:', errorMsg)
                return { 
                  success: false, 
                  message: errorMsg,
                  errorCode: 'NO_ZIP_PACKAGE_AND_TABLE_SYNC_DISABLED'
                }
              } else {
                // 配置开启，允许表同步（但会警告）
                console.warn('⚠️ 警告：未检测到ZIP包，使用业务数据表同步（备选方案）')
                console.warn('⚠️ 业务数据表同步不适合大数据量场景（>1000条题目）')
                console.warn('⚠️ 强烈建议使用ZIP包方案以获得更好的性能')
                console.log('步骤 3: 开始同步业务数据（试卷、题目、分类等）...')
                
                try {
                  await this.syncBusinessData(token)
                  console.log('步骤 3: 业务数据同步完成')
                } catch (error) {
                  console.error('步骤 3: 业务数据同步失败:', error.message)
                  // 检查是否是数据量过大导致的超时
                  if (error.message.includes('timeout') || error.message.includes('超时')) {
                    const errorMsg = '业务数据同步超时，可能是数据量过大。\n\n' +
                      '解决方案：\n' +
                      '1. 请在后台管理-试卷管理中生成试卷包（推荐）\n' +
                      '2. 或者联系开发管理者调整配置'
                    return { 
                      success: false, 
                      message: errorMsg,
                      errorCode: 'TABLE_SYNC_TIMEOUT'
                    }
                  }
                  throw error
                }
              }
            }
          }
        } else {
          console.log('步骤 3: 无token，跳过业务数据同步（需要登录后同步）')
        }
        
        // 步骤 4: 同步练习次数重置记录
        console.log('步骤 4: 同步练习次数重置记录...')
        await this.syncPaperResets(token)
        console.log('步骤 4: 练习次数重置记录同步完成')
        
        console.log('=== 数据同步完成 ===')
        console.log(`学员档案数量: ${response.data.archiveCount || 0}`)
        console.log(`字典类型数量: ${response.data.dictTypeCount || 0}`)
        return { success: true, message: '数据同步成功' }
      } else {
        console.error('同步接口返回错误:', response.data)
        return { success: false, message: response.data?.msg || '数据同步失败' }
      }
    } catch (error) {
      console.error('数据同步失败:', error)
      console.error('错误类型:', error.constructor.name)
      console.error('错误消息:', error.message)
      console.error('错误堆栈:', error.stack)
      if (error.response) {
        console.error('HTTP 响应状态:', error.response.status)
        console.error('HTTP 响应数据:', error.response.data)
      }
      return { success: false, message: error.message || '数据同步失败' }
    }
  }

  /**
   * 公共同步所有数据（无需token，用于启动时同步）
   * 使用公共同步接口 /student/syncPublicData
   * 同步业务数据（学员档案）和字典数据
   */
  async syncPublicData() {
    console.log('开始公共同步所有数据（无需token）...')
    
    try {
      // 调用公共同步接口
      const response = await axios.get(`${API_BASE_URL}/student/syncPublicData`, {
        headers: {
          'Content-Type': 'application/json'
        }
      })
      
      console.log('公共同步接口响应状态:', response.status)
      console.log('公共同步接口响应数据:', JSON.stringify(response.data, null, 2))
      
      if (response.data && response.data.code === 200) {
        // 1. 同步学员档案数据（业务数据）
        if (response.data.archives) {
          console.log('步骤 1: 同步学员档案数据...')
          await this.syncStudentArchivesFromResponse(response.data.archives)
          console.log('步骤 1: 学员档案数据同步完成')
        }
        
        // 2. 同步字典数据
        if (response.data.dictData) {
          console.log('步骤 2: 同步字典数据...')
          await this.syncDictDataFromResponse(response.data.dictData)
          console.log('步骤 2: 字典数据同步完成')
        }
        
        // 3. 公共同步不包含业务数据（需要token）
        // 业务数据（试卷、题目、分类）需要权限验证，只能在登录后同步
        // 检查ZIP包状态（用于提示）
        const hasZipPackage = await this.checkZipPackageExists()
        if (!hasZipPackage) {
          console.warn('⚠️ 警告：未检测到ZIP包')
          console.warn('⚠️ 请在后台管理-试卷管理中生成试卷包，或登录后同步业务数据')
        } else {
          console.log('步骤 3: 检测到ZIP包，使用ZIP包方案（推荐）')
        }
        
        console.log('=== 公共同步完成 ===')
        console.log(`学员档案数量: ${response.data.archiveCount || 0}`)
        console.log(`字典类型数量: ${response.data.dictTypeCount || 0}`)
        return { success: true, message: '数据同步成功' }
      } else {
        console.error('公共同步接口返回错误:', response.data)
        return { success: false, message: response.data?.msg || '数据同步失败' }
      }
    } catch (error) {
      console.error('公共同步数据失败:', error)
      console.error('错误类型:', error.constructor.name)
      console.error('错误消息:', error.message)
      if (error.response) {
        console.error('HTTP 响应状态:', error.response.status)
        console.error('HTTP 响应数据:', error.response.data)
      }
      return { success: false, message: error.message || '数据同步失败' }
    }
  }

  /**
   * 同步字典数据
   */
  async syncDictData(token = null) {
    console.log('开始同步字典数据...')
    
    try {
      // 清空现有字典数据（覆盖同步）
      this.db.exec('DELETE FROM dict_data')
      console.log('已清空现有字典数据')
      
      // 需要同步的字典类型列表
      const dictTypes = [
        'paper_type',      // 试卷类型
        'sys_user_sex',    // 性别
        'grade',           // 学段
        'pupil',           // 小学年级
        'middle',          // 中学年级
        'high',            // 高中年级
        'app_header_config' // 客户端头部配置（主标题、副标题）
      ]
      
      const headers = {}
      if (token) {
        headers['Authorization'] = `Bearer ${token}`
      }
      
      // 遍历每个字典类型，从后端获取数据
      for (const dictType of dictTypes) {
        try {
          console.log(`获取字典类型: ${dictType}`)
          const response = await axios.get(`${API_BASE_URL}/system/dict/data/type/${dictType}`, {
            headers: {
              'Content-Type': 'application/json',
              ...headers
            }
          })
          
          if (response.data && response.data.code === 200 && response.data.data) {
            const dictDataList = response.data.data
            console.log(`获取到 ${dictType} 字典数据 ${dictDataList.length} 条`)
            
            // 插入到本地数据库
            const stmt = this.db.prepare(`
              INSERT INTO dict_data 
              (dict_type, dict_value, dict_label, dict_sort, css_class, list_class, 
               is_default, status, create_time, update_time, remark)
              VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            `)
            
            const insertMany = this.db.transaction((items) => {
              for (const item of items) {
                stmt.run(
                  dictType,
                  item.dictValue || item.dict_value,
                  item.dictLabel || item.dict_label,
                  item.dictSort || item.dict_sort || 0,
                  item.cssClass || item.css_class || null,
                  item.listClass || item.list_class || null,
                  item.isDefault || item.is_default || 'N',
                  item.status || '0',
                  item.createTime ? new Date(item.createTime).getTime() : Date.now(),
                  item.updateTime ? new Date(item.updateTime).getTime() : null,
                  item.remark || null
                )
              }
            })
            
            insertMany(dictDataList)
            console.log(`✓ ${dictType} 字典数据同步完成`)
          } else {
            console.warn(`字典类型 ${dictType} 无数据或获取失败`)
          }
        } catch (error) {
          console.error(`获取字典类型 ${dictType} 失败:`, error.message)
          // 继续同步其他字典类型
        }
      }
      
      console.log('字典数据同步完成')
    } catch (error) {
      console.error('同步字典数据失败:', error)
      throw error
    }
  }

  /**
   * 从响应数据同步学员档案（新方法）
   */
  async syncStudentArchivesFromResponse(archives) {
    console.log('开始同步学员档案数据（从响应数据）...')
    
    try {
      // 检查是否需要创建学员档案表
      this.initStudentArchiveTable()
      
      // 检查表是否存在
      const tableExists = this.db.prepare(`
        SELECT name FROM sqlite_master 
        WHERE type='table' AND name='student_archive'
      `).get()
      
      if (!tableExists) {
        console.error('错误：student_archive 表不存在！')
        throw new Error('student_archive 表不存在')
      }
      console.log('✓ student_archive 表已确认存在')
      
      // 清空现有学员档案数据（覆盖同步）
      this.db.exec('DELETE FROM student_archive')
      console.log('已清空现有学员档案数据')
      
      if (!archives || archives.length === 0) {
        console.warn('警告：响应中没有学员档案数据')
        return
      }
      
      console.log(`获取到学员档案 ${archives.length} 条`)
      
      // 插入到本地数据库
      const stmt = this.db.prepare(`
        INSERT INTO student_archive 
        (id, user_id, student_account, student_name, password, phone_number, sex, grade, 
         current_grade, hometown, applicable_papers, applicable_paper_ids, seat_number, status, del_flag, 
         create_time, update_time, remark)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      `)
      
      const insertMany = this.db.transaction((items) => {
        for (const item of items) {
          // 处理 applicablePapers（List<String> 转 JSON 字符串）
          let applicablePapersJson = '[]'
          if (item.applicablePapers) {
            if (Array.isArray(item.applicablePapers)) {
              applicablePapersJson = JSON.stringify(item.applicablePapers)
              console.log(`学员 ${item.studentAccount || item.student_account} (ID: ${item.id}, user_id: ${item.userId || item.user_id}) 的适用试卷类型:`, item.applicablePapers)
            } else if (typeof item.applicablePapers === 'string') {
              // 如果已经是字符串，尝试解析
              try {
                JSON.parse(item.applicablePapers)
                applicablePapersJson = item.applicablePapers
              } catch (e) {
                // 如果不是 JSON，当作逗号分隔的字符串处理
                const arr = item.applicablePapers.split(',').map(s => s.trim()).filter(s => s)
                applicablePapersJson = JSON.stringify(arr)
              }
            }
          }
          
          // 处理 applicablePaperIds（List<Integer> 转 JSON 字符串）
          let applicablePaperIdsJson = '[]'
          if (item.applicablePaperIds) {
            if (Array.isArray(item.applicablePaperIds)) {
              applicablePaperIdsJson = JSON.stringify(item.applicablePaperIds)
              console.log(`学员 ${item.studentAccount || item.student_account} 的适用试卷ID列表:`, item.applicablePaperIds)
            } else if (typeof item.applicablePaperIds === 'string') {
              // 如果已经是字符串，尝试解析
              try {
                JSON.parse(item.applicablePaperIds)
                applicablePaperIdsJson = item.applicablePaperIds
              } catch (e) {
                // 如果不是 JSON，当作逗号分隔的字符串处理
                const arr = item.applicablePaperIds.split(',').map(s => parseInt(s.trim())).filter(s => !isNaN(s))
                applicablePaperIdsJson = JSON.stringify(arr)
              }
            }
          }
          
          const userId = item.userId || item.user_id || null
          const studentAccount = item.studentAccount || item.student_account || ''
          
          console.log(`保存学员档案: ID=${item.id}, user_id=${userId}, student_account=${studentAccount}, applicable_papers=${applicablePapersJson}, applicable_paper_ids=${applicablePaperIdsJson}`)
          
          stmt.run(
            item.id,
            userId,
            studentAccount,
            item.studentName || item.student_name || item.name || null,
            item.password || '',
            item.phoneNumber || item.phone_number || null,
            item.sex || null,
            item.grade || null,
            item.currentGrade || item.current_grade || null,
            item.hometown || null,
            applicablePapersJson,
            applicablePaperIdsJson,
            item.seatNumber || item.seat_number || null,
            item.status || '0',
            item.delFlag || item.del_flag || '0',
            item.createTime ? new Date(item.createTime).getTime() : Date.now(),
            item.updateTime ? new Date(item.updateTime).getTime() : null,
            item.remark || null
          )
        }
      })
      
      insertMany(archives)
      console.log(`✓ 学员档案数据同步完成，共 ${archives.length} 条`)
      
      // 验证保存的数据
      const verifyStmt = this.db.prepare(`
        SELECT id, user_id, student_account, applicable_papers 
        FROM student_archive 
        WHERE del_flag = '0' AND status = '0'
        LIMIT 10
      `)
      const verifyResults = verifyStmt.all()
      console.log('验证保存的数据（前10条）:', verifyResults.map(r => ({
        id: r.id,
        user_id: r.user_id,
        student_account: r.student_account,
        applicable_papers: r.applicable_papers
      })))
    } catch (error) {
      console.error('同步学员档案数据失败:', error)
      throw error
    }
  }

  /**
   * 从响应数据同步字典数据（新方法）
   */
  async syncDictDataFromResponse(dictDataMap) {
    console.log('开始同步字典数据（从响应数据）...')
    
    try {
      // 清空现有字典数据（覆盖同步）
      this.db.exec('DELETE FROM dict_data')
      console.log('已清空现有字典数据')
      
      if (!dictDataMap || Object.keys(dictDataMap).length === 0) {
        console.warn('警告：响应中没有字典数据')
        return
      }
      
      const stmt = this.db.prepare(`
        INSERT INTO dict_data 
        (dict_type, dict_value, dict_label, dict_sort, css_class, list_class, 
         is_default, status, create_time, update_time, remark)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      `)
      
      const insertMany = this.db.transaction((items) => {
        for (const item of items) {
          stmt.run(
            item.dictType || item.dict_type,
            item.dictValue || item.dict_value,
            item.dictLabel || item.dict_label,
            item.dictSort || item.dict_sort || 0,
            item.cssClass || item.css_class || null,
            item.listClass || item.list_class || null,
            item.isDefault || item.is_default || 'N',
            item.status || '0',
            item.createTime ? new Date(item.createTime).getTime() : Date.now(),
            item.updateTime ? new Date(item.updateTime).getTime() : null,
            item.remark || null
          )
        }
      })
      
      // 遍历所有字典类型
      let totalCount = 0
      for (const [dictType, dictDataList] of Object.entries(dictDataMap)) {
        if (dictDataList && dictDataList.length > 0) {
          insertMany(dictDataList)
          totalCount += dictDataList.length
          console.log(`✓ ${dictType} 字典数据同步完成，共 ${dictDataList.length} 条`)
        }
      }
      
      console.log(`字典数据同步完成，共 ${totalCount} 条`)
    } catch (error) {
      console.error('同步字典数据失败:', error)
      throw error
    }
  }

  /**
   * 同步学员档案数据（旧方法，保留用于兼容）
   */
  async syncStudentArchives(token = null) {
    console.log('开始同步学员档案数据...')
    
    try {
      // 检查是否需要创建学员档案表（表应该在 db.js 中已创建，这里只是确保）
      this.initStudentArchiveTable()
      
      // 检查表是否存在
      const tableExists = this.db.prepare(`
        SELECT name FROM sqlite_master 
        WHERE type='table' AND name='student_archive'
      `).get()
      
      if (!tableExists) {
        console.error('错误：student_archive 表不存在！')
        throw new Error('student_archive 表不存在')
      }
      console.log('✓ student_archive 表已确认存在')
      
      // 清空现有学员档案数据（覆盖同步）
      const deleteResult = this.db.exec('DELETE FROM student_archive')
      console.log('已清空现有学员档案数据')
      
      const headers = {
        'Content-Type': 'application/json'
      }
      if (token) {
        headers['Authorization'] = `Bearer ${token}`
      }
      
      // 从后端获取所有学员档案（使用 POST 方法，接口是 /student/archive/listArchive）
      console.log('从后端获取学员档案列表...')
      console.log('请求 URL:', `${API_BASE_URL}/student/archive/listArchive`)
      console.log('请求头:', headers)
      
      const response = await axios.post(`${API_BASE_URL}/student/archive/listArchive`, {
        pageNum: 1,
        pageSize: 10000 // 获取所有数据
      }, {
        headers
      })
      
      console.log('后端响应状态:', response.status)
      console.log('后端响应数据:', JSON.stringify(response.data, null, 2))
      
      if (response.data && response.data.code === 200 && response.data.rows) {
        const archives = response.data.rows
        console.log(`获取到学员档案 ${archives.length} 条`)
        
        if (archives.length === 0) {
          console.warn('警告：后端返回的学员档案列表为空')
        }
        
        // 插入到本地数据库
        const stmt = this.db.prepare(`
          INSERT INTO student_archive 
          (id, user_id, student_account, student_name, password, phone_number, sex, grade, 
           current_grade, hometown, applicable_papers, applicable_paper_ids, seat_number, status, del_flag, 
           create_time, update_time, remark)
          VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        `)
        
        const insertMany = this.db.transaction((items) => {
          for (const item of items) {
            // 处理 applicablePapers（List<String> 转 JSON 字符串）
            let applicablePapersJson = '[]'
            if (item.applicablePapers) {
              if (Array.isArray(item.applicablePapers)) {
                applicablePapersJson = JSON.stringify(item.applicablePapers)
                console.log(`学员 ${item.studentAccount || item.student_account} (ID: ${item.id}, user_id: ${item.userId || item.user_id}) 的适用试卷类型:`, item.applicablePapers)
              } else if (typeof item.applicablePapers === 'string') {
                // 如果已经是字符串，尝试解析
                try {
                  JSON.parse(item.applicablePapers)
                  applicablePapersJson = item.applicablePapers
                } catch (e) {
                  // 如果不是 JSON，当作逗号分隔的字符串处理
                  const arr = item.applicablePapers.split(',').map(s => s.trim()).filter(s => s)
                  applicablePapersJson = JSON.stringify(arr)
                }
              }
            }
            
            // 处理 applicablePaperIds（List<Integer> 转 JSON 字符串）
            let applicablePaperIdsJson = '[]'
            if (item.applicablePaperIds) {
              if (Array.isArray(item.applicablePaperIds)) {
                applicablePaperIdsJson = JSON.stringify(item.applicablePaperIds)
                console.log(`学员 ${item.studentAccount || item.student_account} 的适用试卷ID列表:`, item.applicablePaperIds)
              } else if (typeof item.applicablePaperIds === 'string') {
                // 如果已经是字符串，尝试解析
                try {
                  JSON.parse(item.applicablePaperIds)
                  applicablePaperIdsJson = item.applicablePaperIds
                } catch (e) {
                  // 如果不是 JSON，当作逗号分隔的字符串处理
                  const arr = item.applicablePaperIds.split(',').map(s => parseInt(s.trim())).filter(s => !isNaN(s))
                  applicablePaperIdsJson = JSON.stringify(arr)
                }
              }
            }
            
            const userId = item.userId || item.user_id || null
            const studentAccount = item.studentAccount || item.student_account || ''
            
            console.log(`保存学员档案: ID=${item.id}, user_id=${userId}, student_account=${studentAccount}, applicable_papers=${applicablePapersJson}, applicable_paper_ids=${applicablePaperIdsJson}`)
            
            stmt.run(
              item.id,
              userId,
              studentAccount,
              item.studentName || item.student_name || item.name || null,
              item.password || '',
              item.phoneNumber || item.phone_number || null,
              item.sex || null,
              item.grade || null,
              item.currentGrade || item.current_grade || null,
              item.hometown || null,
              applicablePapersJson,
              applicablePaperIdsJson,
              item.seatNumber || item.seat_number || null,
              item.status || '0',
              item.delFlag || item.del_flag || '0',
              item.createTime ? new Date(item.createTime).getTime() : Date.now(),
              item.updateTime ? new Date(item.updateTime).getTime() : null,
              item.remark || null
            )
          }
        })
        
        insertMany(archives)
        console.log(`✓ 学员档案数据同步完成，共 ${archives.length} 条`)
        
        // 验证保存的数据
        const verifyStmt = this.db.prepare(`
          SELECT id, user_id, student_account, applicable_papers 
          FROM student_archive 
          WHERE del_flag = '0' AND status = '0'
          LIMIT 10
        `)
        const verifyResults = verifyStmt.all()
        console.log('验证保存的数据（前10条）:', verifyResults.map(r => ({
          id: r.id,
          user_id: r.user_id,
          student_account: r.student_account,
          applicable_papers: r.applicable_papers
        })))
      } else {
        console.warn('获取学员档案失败或无数据')
        console.warn('响应数据:', response.data)
        if (response.data && response.data.code !== 200) {
          console.warn('响应错误码:', response.data.code)
          console.warn('响应错误消息:', response.data.msg)
        }
      }
    } catch (error) {
      console.error('同步学员档案数据失败:', error)
      console.error('错误类型:', error.constructor.name)
      console.error('错误消息:', error.message)
      if (error.response) {
        console.error('HTTP 响应状态:', error.response.status)
        console.error('HTTP 响应数据:', error.response.data)
      }
      // 如果是因为认证失败，记录但不抛出异常（允许离线使用）
      if (error.response && error.response.status === 401) {
        console.warn('同步学员档案需要认证，跳过（允许离线使用）')
        // 不抛出异常，允许继续运行
      } else if (error.code === 'ECONNREFUSED') {
        console.warn('无法连接到后端服务，跳过同步（允许离线使用）')
        // 不抛出异常，允许继续运行
      } else {
        // 其他错误也允许继续运行，但记录错误
        console.error('同步失败，但应用将继续运行（允许离线使用）')
      }
    }
  }

  /**
   * 初始化学员档案表（表已在 db.js 中创建，这里只是确保存在）
   */
  initStudentArchiveTable() {
    // 表已经在 db.js 的 initTables() 中创建，这里只是确保存在
    // 如果表不存在，创建它（作为备用）
    this.db.exec(`
      CREATE TABLE IF NOT EXISTS student_archive (
        id INTEGER PRIMARY KEY,
        user_id INTEGER,
        student_account TEXT NOT NULL,
        student_name TEXT,
        password TEXT,
        phone_number TEXT,
        sex TEXT,
        grade TEXT,
        current_grade TEXT,
        hometown TEXT,
        seat_number TEXT,
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
    
    // 如果表已存在，检查是否需要添加新字段
    try {
      const columns = this.db.prepare(`PRAGMA table_info(student_archive)`).all()
      const hasApplicablePaperIds = columns.some(col => col.name === 'applicable_paper_ids')
      if (!hasApplicablePaperIds) {
        this.db.exec(`ALTER TABLE student_archive ADD COLUMN applicable_paper_ids TEXT DEFAULT '[]'`)
        console.log('已添加 applicable_paper_ids 字段到 student_archive 表')
      }
      const hasSeatNumber = columns.some(col => col.name === 'seat_number')
      if (!hasSeatNumber) {
        this.db.exec(`ALTER TABLE student_archive ADD COLUMN seat_number TEXT`)
        console.log('已添加 seat_number 字段到 student_archive 表')
      }
      const hasStudentName = columns.some(col => col.name === 'student_name')
      if (!hasStudentName) {
        this.db.exec(`ALTER TABLE student_archive ADD COLUMN student_name TEXT`)
        console.log('已添加 student_name 字段到 student_archive 表')
      }
    } catch (error) {
      console.warn('检查/添加字段失败:', error.message)
    }
    
    // 创建索引
    this.db.exec(`
      CREATE INDEX IF NOT EXISTS idx_student_archive_account ON student_archive(student_account)
    `)
    this.db.exec(`
      CREATE INDEX IF NOT EXISTS idx_student_archive_user_id ON student_archive(user_id)
    `)
    
    console.log('学员档案表已确认存在')
  }

  /**
   * 从本地数据库获取学员档案（根据账号）
   */
  getStudentArchiveByAccount(studentAccount) {
    const result = this.db.prepare(`
      SELECT * FROM student_archive 
      WHERE student_account = ? AND del_flag = '0' AND status = '0'
      LIMIT 1
    `).get(studentAccount)
    
    if (result) {
      // 解析 applicablePapers
      try {
        result.applicablePapers = JSON.parse(result.applicable_papers || '[]')
      } catch (e) {
        result.applicablePapers = []
      }
      delete result.applicable_papers
      
      // 解析 applicablePaperIds
      try {
        result.applicablePaperIds = JSON.parse(result.applicable_paper_ids || '[]')
      } catch (e) {
        result.applicablePaperIds = []
      }
      delete result.applicable_paper_ids
    }
    
    return result
  }

  /**
   * 从本地数据库获取学员档案（根据 user_id）
   */
  getStudentArchiveByUserId(userId) {
    const result = this.db.prepare(`
      SELECT * FROM student_archive 
      WHERE user_id = ? AND del_flag = '0' AND status = '0'
      LIMIT 1
    `).get(userId)
    
    if (result) {
      // 解析 applicablePapers
      try {
        result.applicablePapers = JSON.parse(result.applicable_papers || '[]')
      } catch (e) {
        result.applicablePapers = []
      }
      delete result.applicable_papers
      
      // 解析 applicablePaperIds
      try {
        result.applicablePaperIds = JSON.parse(result.applicable_paper_ids || '[]')
      } catch (e) {
        result.applicablePaperIds = []
      }
      delete result.applicable_paper_ids
    }
    
    return result
  }

  /**
   * 同步ZIP包（阶段六实现，支持按applicable_paper_ids同步）
   * @param {string} token - 认证token
   * @returns {Promise<Object>} 同步结果
   */
  async syncZipPackages(token) {
    try {
      console.log('开始同步ZIP包...')
      
      // 1. 获取当前登录学员的档案信息
      let currentArchive = null
      let applicablePaperIds = null
      
      try {
        // 尝试从后端获取当前用户信息
        const headers = {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        }
        
        const userInfoResponse = await axios.get(
          `${API_BASE_URL}/student/getInfo`,
          { headers }
        )
        
        console.log('getInfo 接口响应:', JSON.stringify(userInfoResponse.data, null, 2))
        
        if (userInfoResponse.data && userInfoResponse.data.code === 200) {
          // 注意：getInfo 接口返回的数据结构是：
          // { code: 200, user: {...}, roles: [...], permissions: [...], archiveId: ..., applicablePapers: [...] }
          // user 字段直接在 response.data 下，而不是 response.data.data 下
          let userInfo = null
          if (userInfoResponse.data.data) {
            // 如果存在 data 字段，使用 data
            userInfo = userInfoResponse.data.data
          } else {
            // 否则，直接从 response.data 中获取 user 等信息
            userInfo = {
              user: userInfoResponse.data.user,
              roles: userInfoResponse.data.roles,
              permissions: userInfoResponse.data.permissions,
              archiveId: userInfoResponse.data.archiveId,
              applicablePapers: userInfoResponse.data.applicablePapers
            }
          }
          
          const userId = userInfo?.user?.userId
          
          console.log('解析后的 userInfo:', JSON.stringify(userInfo, null, 2))
          console.log('获取到的 userId:', userId)
          
          if (userId) {
            // 从本地数据库获取学员档案
            currentArchive = this.getStudentArchiveByUserId(userId)
            console.log('从本地数据库获取的学员档案:', currentArchive ? {
              id: currentArchive.id,
              studentAccount: currentArchive.studentAccount,
              applicablePaperIds: currentArchive.applicablePaperIds,
              applicablePaperIdsType: typeof currentArchive.applicablePaperIds,
              applicablePaperIdsLength: currentArchive.applicablePaperIds ? currentArchive.applicablePaperIds.length : 0
            } : 'null')
            
            if (currentArchive) {
              // 确保 applicablePaperIds 是数组
              if (currentArchive.applicablePaperIds) {
                if (Array.isArray(currentArchive.applicablePaperIds)) {
                  applicablePaperIds = currentArchive.applicablePaperIds
                } else if (typeof currentArchive.applicablePaperIds === 'string') {
                  // 如果是字符串，尝试解析
                  try {
                    applicablePaperIds = JSON.parse(currentArchive.applicablePaperIds)
                  } catch (e) {
                    console.warn('解析 applicablePaperIds 失败:', e.message)
                    applicablePaperIds = []
                  }
                }
              }
              
              if (applicablePaperIds && applicablePaperIds.length > 0) {
                console.log(`✓ 检测到学员配置了适用试卷ID列表: ${JSON.stringify(applicablePaperIds)}`)
              } else {
                console.warn('⚠️ 学员档案中没有配置适用试卷ID列表，或列表为空')
                console.warn('   applicablePaperIds 值:', currentArchive.applicablePaperIds)
                console.warn('   请确保学员档案中已配置 applicablePaperIds 字段（JSON数组格式，如：[4]）')
              }
            } else {
              console.warn('⚠️ 未找到学员档案，userId:', userId)
            }
          } else {
            console.warn('⚠️ 无法获取 userId')
          }
        }
      } catch (error) {
        console.error('❌ 获取当前学员信息失败:', error.message)
        console.error('   错误详情:', error)
      }
      
      // 2. 获取试卷列表
      const headers = {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      }
      
      let papers = []
      
      if (applicablePaperIds && applicablePaperIds.length > 0) {
        // 方案A：如果配置了applicable_paper_ids，只同步指定的试卷
        console.log(`✓ 使用新方案：只同步指定的 ${applicablePaperIds.length} 个试卷`)
        console.log(`✓ 试卷ID列表: ${JSON.stringify(applicablePaperIds)}`)
        console.log(`✓ 准备调用接口: POST ${API_BASE_URL}/student/sync/paper/listByIds`)
        
        // 调用学员专用的根据ID列表查询试卷的接口
        const response = await axios.post(
          `${API_BASE_URL}/student/sync/paper/listByIds`,
          { ids: applicablePaperIds },
          { headers }
        )
        
        console.log(`✓ 接口调用成功，响应状态: ${response.status}`)
        console.log(`✓ 响应数据:`, JSON.stringify(response.data, null, 2))
        
        if (response.data && response.data.code === 200 && response.data.data) {
          papers = response.data.data
          console.log(`获取到 ${papers.length} 个指定试卷（已过滤未授权的试卷）`)
          
          if (papers.length === 0) {
            console.warn('根据ID列表查询试卷返回空列表，可能所有试卷ID都未授权或不存在')
            return {
              success: true,
              successCount: 0,
              skipCount: 0,
              errorCount: 0,
              message: '未找到可同步的试卷包（所有试卷ID都未授权或不存在）'
            }
          }
        } else {
          console.error('❌ 根据ID列表查询试卷失败:', response.data?.msg || '未知错误')
          console.error('   响应数据:', JSON.stringify(response.data, null, 2))
          throw new Error(`根据ID列表查询试卷失败: ${response.data?.msg || '未知错误'}`)
        }
      } else {
        // 如果没有配置applicable_paper_ids，返回错误提示
        console.error('❌ 学员未配置适用试卷ID列表，无法同步试卷包')
        console.error('   当前 applicablePaperIds 值:', applicablePaperIds)
        console.error('   当前学员档案:', currentArchive ? {
          id: currentArchive.id,
          studentAccount: currentArchive.studentAccount,
          userId: currentArchive.userId,
          applicablePaperIds: currentArchive.applicablePaperIds,
          applicablePaperIdsType: typeof currentArchive.applicablePaperIds
        } : 'null')
        console.error('   请确保：')
        console.error('   1. 学员档案中已配置 applicablePaperIds 字段（JSON数组格式，如：[4]）')
        console.error('   2. 已执行步骤1（同步学员档案数据）')
        console.error('   3. 本地数据库中的 applicable_paper_ids 字段不为空')
        return {
          success: false,
          successCount: 0,
          skipCount: 0,
          errorCount: 0,
          message: '学员未配置适用试卷ID列表，请联系管理员配置'
        }
      }
      
      // 3. 过滤已存在的试卷包（只同步新增的，空间换时间）
      const papersToSync = []
      for (const paper of papers) {
        const paperCode = paper.paperCode || paper.paper_code
        if (!paperCode) {
          console.warn(`试卷ID ${paper.id} 缺少paperCode，跳过`)
          continue
        }
        
        // 检查本地是否已有该试卷包
        const existingPackage = this.db.prepare(`
          SELECT paper_id, package_hash, version 
          FROM paper_package 
          WHERE paper_code = ? AND is_active = 1
          ORDER BY version DESC
          LIMIT 1
        `).get(paperCode)
        
        const remoteHash = paper.packageHash || paper.package_hash
        const remoteVersion = paper.version || 0
        
        if (existingPackage) {
          // 本地已有，检查是否需要更新
          const localHash = existingPackage.package_hash
          const localVersion = existingPackage.version || 0
          
          if (remoteHash && remoteHash === localHash && remoteVersion === localVersion) {
            // 版本和hash一致，跳过
            console.log(`试卷包 ${paperCode} 已存在且版本一致，跳过同步`)
            continue
          } else if (localVersion > remoteVersion) {
            // 本地版本高于服务器版本，优先使用本地版本（支持线下手动发配试卷）
            console.log(`✓ 本地版本(v${localVersion})高于服务器版本(v${remoteVersion})，优先使用本地ZIP包: ${paperCode}`)
            continue
          } else {
            // 版本或hash不一致，需要更新
            console.log(`试卷包 ${paperCode} 需要更新: 本地v${localVersion}(${localHash?.substring(0, 8)}...) -> 远程v${remoteVersion}(${remoteHash?.substring(0, 8)}...)`)
          }
        } else {
          // 本地没有，需要同步
          console.log(`试卷包 ${paperCode} 本地不存在，需要同步`)
        }
        
        papersToSync.push(paper)
      }
      
      console.log(`需要同步的试卷包数量: ${papersToSync.length} (共 ${papers.length} 个，已过滤 ${papers.length - papersToSync.length} 个已存在且版本一致的)`)

      const results = []
      for (const paper of papersToSync) {
        try {
          const paperCode = paper.paperCode || paper.paper_code
          if (!paperCode) {
            console.warn(`试卷ID ${paper.id} 缺少paperCode，跳过`)
            continue
          }

          // 在同步ZIP包之前，确保本地paper表中有该试卷记录（避免paper_package.paper_id为null的错误）
          // 同时更新所有字段（包括year, month, province, custom_name）
          console.log(`准备同步试卷包: ${paperCode}，服务器返回的试卷信息:`, {
            id: paper.id,
            paperName: paper.paperName || paper.paper_name,
            year: paper.year,
            month: paper.month,
            province: paper.province,
            customName: paper.customName || paper.custom_name,
            version: paper.version
          })
          
          // 查询本地paper表中是否已有该试卷记录
          const localPaper = this.db.prepare(`
            SELECT id, year, month, province FROM paper WHERE paper_code = ? LIMIT 1
          `).get(paperCode)

          if (!localPaper) {
            console.log(`本地paper表中没有试卷记录，先插入记录: ${paperCode}`)
            // 插入paper表记录（包含year, month, province, custom_name字段）
            const insertStmt = this.db.prepare(`
              INSERT INTO paper 
              (id, paper_name, paper_code, paper_type, paper_desc,
               year, month, province, custom_name,
               total_score, total_questions, duration,
               version, package_hash, package_size, last_package_time,
               status, create_time)
              VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            `)
            insertStmt.run(
              paper.id,
              paper.paperName || paper.paper_name || paperCode,
              paperCode,
              paper.paperType || paper.paper_type || null,
              paper.paperDesc || paper.paper_desc || null,
              paper.year || null,
              paper.month || null,
              paper.province || null,
              paper.customName || paper.custom_name || null,
              paper.totalScore || paper.total_score || 0,
              paper.totalQuestions || paper.total_questions || 0,
              paper.duration || null,
              paper.version || 1,
              paper.packageHash || paper.package_hash || null,
              paper.packageSize || paper.package_size || null,
              paper.lastPackageTime || paper.last_package_time ? new Date(paper.lastPackageTime || paper.last_package_time).getTime() : null,
              1, // status = 1 (启用)
              Date.now()
            )
            console.log(`✓ 已插入paper表记录: ${paperCode} (ID: ${paper.id}, 年月: ${paper.year || '无'}年${paper.month ? paper.month + '月' : ''}, 省份: ${paper.province || '无'})`)
          } else {
            // 如果记录已存在，更新所有字段（包括year, month, province, custom_name）
            console.log(`本地paper表中已有记录，更新字段: ${paperCode}，当前年月: ${localPaper.year || '无'}年${localPaper.month ? localPaper.month + '月' : ''}，省份: ${localPaper.province || '无'}`)
            const updateStmt = this.db.prepare(`
              UPDATE paper 
              SET paper_name = ?, paper_type = ?, paper_desc = ?,
                  year = ?, month = ?, province = ?, custom_name = ?,
                  total_score = ?, total_questions = ?, duration = ?,
                  version = ?, package_hash = ?, package_size = ?, last_package_time = ?,
                  update_time = ?
              WHERE paper_code = ?
            `)
            const updateResult = updateStmt.run(
              paper.paperName || paper.paper_name || paperCode,
              paper.paperType || paper.paper_type || null,
              paper.paperDesc || paper.paper_desc || null,
              paper.year || null,
              paper.month || null,
              paper.province || null,
              paper.customName || paper.custom_name || null,
              paper.totalScore || paper.total_score || 0,
              paper.totalQuestions || paper.total_questions || 0,
              paper.duration || null,
              paper.version || 1,
              paper.packageHash || paper.package_hash || null,
              paper.packageSize || paper.package_size || null,
              paper.lastPackageTime || paper.last_package_time ? new Date(paper.lastPackageTime || paper.last_package_time).getTime() : null,
              Date.now(),
              paperCode
            )
            if (updateResult.changes > 0) {
              console.log(`✓ 已更新paper表记录: ${paperCode} (年月: ${paper.year || '无'}年${paper.month ? paper.month + '月' : ''}, 省份: ${paper.province || '无'})`)
            } else {
              console.warn(`⚠️ 更新paper表记录失败或没有变化: ${paperCode}`)
            }
          }

          const paperName = paper.paperName || paper.paper_name || paperCode
          console.log(`同步试卷包: ${paperCode} (${paperName})`)
          console.log(`传入 syncPaperPackage 的试卷对象:`, JSON.stringify({
            id: paper.id,
            paperCode: paper.paperCode || paper.paper_code,
            paperName: paper.paperName || paper.paper_name,
            version: paper.version,
            packageHash: paper.packageHash || paper.package_hash,
            packageSize: paper.packageSize || paper.package_size
          }, null, 2))
          
          // 优化：试卷列表加载时，只下载快速启动包，不下载完整包
          // 完整包将在用户点击"开始练习"时后台下载
          const result = await this.paperService.syncQuickStartPackageOnly(
            paper, // 传入完整的试卷对象，而不是 paperCode
            token,
            (progress) => {
              console.log(`快速启动包 ${paperCode} 下载进度: ${progress}%`)
            }
          )

          results.push({
            paperCode,
            success: result.success,
            message: result.message
          })

          if (result.success) {
            console.log(`✓ 试卷包同步成功: ${paperCode}`)
            
            // 验证逻辑：如果快速启动包已下载，验证快速启动包；如果完整包已下载，验证完整包
            if (result.quickStartDownloaded) {
              console.log(`✓ 快速启动包已下载: ${paperCode}`)
              
              // 快速启动包已下载完成，但完整包可能还在下载
              // 检查完整包是否已存在
              try {
                const paperId = paper.id
                if (paperId) {
                  // 检查完整包是否已存在
                  const hasFullPackage = this.paperService.checkPackageExists(paperId)
                  
                  if (hasFullPackage) {
                    // 完整包也存在，更新为ready状态
                    this.paperService.updateDownloadStatus(
                      paperId,
                      paperCode,
                      'ready', // 快速启动包和完整包都已完成
                      100, // 进度100%
                      0, // 完整包大小未知
                      0, // 完整包已下载大小未知
                      null // 无错误信息
                    )
                    console.log(`✓ 已更新下载状态为ready（快速启动包和完整包都已完成）: ${paperCode}`)
                  } else {
                    // 完整包还在下载，更新为downloading状态，进度从50%开始
                    this.paperService.updateDownloadStatus(
                      paperId,
                      paperCode,
                      'downloading', // 完整包正在下载
                      50, // 快速启动包已完成，进度从50%开始
                      0, // 完整包大小未知
                      0, // 完整包已下载大小未知
                      null // 无错误信息
                    )
                    console.log(`✓ 已更新下载状态为downloading（快速启动包已完成，完整包正在下载）: ${paperCode}`)
                  }
                }
              } catch (error) {
                console.warn(`更新快速启动包下载状态失败: ${error.message}`)
              }
            }
            
            if (result.fullPackageDownloading) {
              console.log(`✓ 完整包正在后台下载: ${paperCode}`)
              // 完整包正在后台下载，不立即验证（等下载完成后再验证）
            } else {
              // 完整包已下载完成，验证是否保存到了 paper_package 表
              const verifyPackage = this.db.prepare(`
                SELECT paper_code, version, is_active FROM paper_package 
                WHERE paper_code = ? AND is_active = 1
                ORDER BY version DESC
                LIMIT 1
              `).get(paperCode)
              
              if (verifyPackage) {
                console.log(`✓ 验证：试卷包已保存到 paper_package 表，paper_code=${verifyPackage.paper_code}, version=${verifyPackage.version}`)
              } else {
                console.warn(`⚠️ 注意：完整包可能正在后台下载中，paper_package 表中暂未找到记录，paper_code=${paperCode}`)
              }
            }
            
            // 更新本地paper表的version和package_hash字段（确保显示最新数据）
            // 如果记录不存在，则插入新记录；如果存在，则更新
            const remoteHash = paper.packageHash || paper.package_hash
            const remoteVersion = paper.version || 0
            if (remoteHash && remoteVersion) {
              // 检查记录是否存在
              const existingPaper = this.db.prepare(`
                SELECT id FROM paper WHERE paper_code = ? LIMIT 1
              `).get(paperCode)
              
              if (existingPaper) {
                // 更新现有记录（包括year, month, province, custom_name等所有字段）
                const updateStmt = this.db.prepare(`
                  UPDATE paper 
                  SET paper_name = ?, paper_type = ?, paper_desc = ?,
                      year = ?, month = ?, province = ?, custom_name = ?,
                      total_score = ?, total_questions = ?, duration = ?,
                      version = ?, package_hash = ?, package_size = ?, last_package_time = ?,
                      update_time = ?
                  WHERE paper_code = ?
                `)
                const updateResult = updateStmt.run(
                  paper.paperName || paper.paper_name || paperCode,
                  paper.paperType || paper.paper_type || null,
                  paper.paperDesc || paper.paper_desc || null,
                  paper.year || null,
                  paper.month || null,
                  paper.province || null,
                  paper.customName || paper.custom_name || null,
                  paper.totalScore || paper.total_score || 0,
                  paper.totalQuestions || paper.total_questions || 0,
                  paper.duration || null,
                  remoteVersion,
                  remoteHash,
                  paper.packageSize || paper.package_size || null,
                  paper.lastPackageTime || paper.last_package_time ? new Date(paper.lastPackageTime || paper.last_package_time).getTime() : null,
                  Date.now(),
                  paperCode
                )
                if (updateResult.changes > 0) {
                  console.log(`✓ 已更新本地paper表的完整信息: ${paperCode} -> v${remoteVersion} (年月: ${paper.year || '无'}年${paper.month ? paper.month + '月' : ''}, 省份: ${paper.province || '无'})`)
                }
              } else {
                // 插入新记录（如果本地没有该试卷的基本信息，包含year, month, province, custom_name字段）
                const insertStmt = this.db.prepare(`
                  INSERT INTO paper 
                  (id, paper_name, paper_code, paper_type, paper_desc,
                   year, month, province, custom_name,
                   total_score, total_questions, duration, 
                   version, package_hash, package_size, last_package_time, 
                   status, create_time)
                  VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                `)
                insertStmt.run(
                  paper.id,
                  paper.paperName || paper.paper_name || paperCode,
                  paperCode,
                  paper.paperType || paper.paper_type || null,
                  paper.paperDesc || paper.paper_desc || null,
                  paper.year || null,
                  paper.month || null,
                  paper.province || null,
                  paper.customName || paper.custom_name || null,
                  paper.totalScore || paper.total_score || 0,
                  paper.totalQuestions || paper.total_questions || 0,
                  paper.duration || null,
                  remoteVersion,
                  remoteHash,
                  paper.packageSize || paper.package_size || null,
                  paper.lastPackageTime || paper.last_package_time ? new Date(paper.lastPackageTime || paper.last_package_time).getTime() : null,
                  1, // status = 1 (启用)
                  Date.now()
                )
                console.log(`✓ 已插入本地paper表的新记录: ${paperCode} -> v${remoteVersion} (年月: ${paper.year || '无'}年${paper.month ? paper.month + '月' : ''}, 省份: ${paper.province || '无'})`)
              }
            }
          } else {
            console.warn(`⚠️ 试卷包同步失败: ${paperCode} - ${result.message}`)
          }
        } catch (error) {
          console.error(`同步试卷包失败: ${paper.paperCode || paper.id}`, error.message)
          results.push({
            paperCode: paper.paperCode || paper.paper_code,
            success: false,
            message: error.message
          })
        }
      }

      const successCount = results.filter(r => r.success).length
      const failCount = results.length - successCount

      console.log(`ZIP包同步完成: 成功 ${successCount} 个，失败 ${failCount} 个`)

      // 清理旧版本
      await this.paperService.cleanupOldVersions()

      return {
        success: true,
        total: results.length,
        successCount,
        failCount,
        results
      }
    } catch (error) {
      console.error('同步ZIP包失败:', error.message)
      throw error
    }
  }

  /**
   * 检查ZIP包是否存在
   * @returns {Promise<Boolean>} true-存在ZIP包，false-不存在ZIP包
   */
  async checkZipPackageExists() {
    try {
      const path = require('path')
      const fs = require('fs')
      const { app } = require('electron')
      
      // 检查paper_package表是否有数据
      const tableExists = this.db.prepare(`
        SELECT name FROM sqlite_master 
        WHERE type='table' AND name='paper_package'
      `).get()
      
      if (tableExists) {
        const count = this.db.prepare('SELECT COUNT(*) as count FROM paper_package WHERE is_active = 1').get()
        if (count && count.count > 0) {
          return true // 有ZIP包数据
        }
      }
      
      // 检查是否有ZIP包文件
      const userDataPath = app.getPath('userData')
      const paperPackagesPath = path.join(userDataPath, 'paper_packages')
      
      if (fs.existsSync(paperPackagesPath)) {
        const files = fs.readdirSync(paperPackagesPath)
        const hasZipFiles = files.some(file => file.endsWith('.zip') || file.includes('.zip.part'))
        if (hasZipFiles) {
          return true // 有ZIP文件
        }
      }
      
      return false // 没有ZIP包
    } catch (error) {
      console.warn('检查ZIP包状态失败:', error.message)
      return false // 检查失败，假设没有ZIP包
    }
  }

  /**
   * 检查是否需要同步业务数据（已废弃，改用checkZipPackageExists）
   * @deprecated 使用 checkZipPackageExists() 和配置检查替代
   * @returns {Promise<Boolean>} true-需要同步，false-不需要同步
   */
  async shouldSyncBusinessData() {
    try {
      const path = require('path')
      const fs = require('fs')
      const { app } = require('electron')
      
      console.log('  [检查] 开始检查是否需要同步业务数据...')
      
      // 检查paper_package表是否有数据
      const tableExists = this.db.prepare(`
        SELECT name FROM sqlite_master 
        WHERE type='table' AND name='paper_package'
      `).get()
      
      if (tableExists) {
        const count = this.db.prepare('SELECT COUNT(*) as count FROM paper_package WHERE is_active = 1').get()
        console.log(`  [检查] paper_package表中有 ${count ? count.count : 0} 条激活的ZIP包数据`)
        if (count && count.count > 0) {
          // 有ZIP包数据，不需要同步业务数据
          console.log('  [检查] 检测到ZIP包数据，不需要同步业务数据')
          return false
        }
      } else {
        console.log('  [检查] paper_package表不存在')
      }
      
      // 检查是否有ZIP包文件
      const userDataPath = app.getPath('userData')
      const paperPackagesPath = path.join(userDataPath, 'paper_packages')
      console.log(`  [检查] ZIP包目录: ${paperPackagesPath}`)
      
      if (!fs.existsSync(paperPackagesPath)) {
        console.log('  [检查] ZIP包目录不存在，需要同步业务数据')
        return true // 没有ZIP包目录，需要同步业务数据
      }
      
      // 检查目录中是否有ZIP文件
      const files = fs.readdirSync(paperPackagesPath)
      console.log(`  [检查] ZIP包目录中的文件: ${files.join(', ')}`)
      const hasZipFiles = files.some(file => file.endsWith('.zip') || file.includes('.zip.part'))
      
      if (hasZipFiles) {
        console.log('  [检查] 检测到ZIP文件，不需要同步业务数据')
        return false
      } else {
        console.log('  [检查] 没有ZIP文件，需要同步业务数据')
        return true // 没有ZIP文件，需要同步业务数据
      }
    } catch (error) {
      console.warn('  [检查] 检查ZIP包状态失败，默认需要同步业务数据:', error.message)
      console.error('  [检查] 错误堆栈:', error.stack)
      return true // 检查失败，默认需要同步（安全策略）
    }
  }

  /**
   * 同步业务数据（试卷、题目、分类等）
   * 如果没有ZIP包，需要同步这些数据以便离线使用
   * 
   * ⚠️ 性能警告：
   * - 此方法适合小数据量场景（<1000条题目）
   * - 大数据量场景（>10000条题目）可能需要数小时
   * - 强烈建议使用ZIP包方案
   * 
   * @param {string} token - 学员 token（可选，公共同步时不需要）
   */
  async syncBusinessData(token = null) {
    console.log('开始同步业务数据（试卷、题目、分类等）...')
    console.log('Token 是否提供:', token ? '是' : '否')
    console.warn('⚠️ 性能警告：业务数据表同步不适合大数据量场景')
    console.warn('⚠️ 如果题目数量 > 10,000 条，同步可能需要数小时')
    console.warn('⚠️ 强烈建议使用ZIP包方案以获得更好的性能')
    
    try {
      const headers = {
        'Content-Type': 'application/json'
      }
      if (token) {
        headers['Authorization'] = `Bearer ${token}`
      }
      
      // 1. 同步题目分类
      console.log('  1.1 同步题目分类...')
      try {
        await this.syncQuestionCategories(headers)
      } catch (error) {
        console.error('  1.1 同步题目分类失败:', error.message)
        // 继续执行其他同步
      }
      
      // 2. 同步题目（分页获取所有题目）
      console.log('  1.2 同步题目...')
      try {
        await this.syncQuestions(headers)
      } catch (error) {
        console.error('  1.2 同步题目失败:', error.message)
        // 继续执行其他同步
      }
      
      // 3. 同步试卷
      console.log('  1.3 同步试卷...')
      try {
        await this.syncPapers(headers)
      } catch (error) {
        console.error('  1.3 同步试卷失败:', error.message)
        // 继续执行其他同步
      }
      
      // 4. 同步试卷-题目关联
      console.log('  1.4 同步试卷-题目关联...')
      try {
        await this.syncPaperQuestions(headers)
      } catch (error) {
        console.error('  1.4 同步试卷-题目关联失败:', error.message)
        // 继续执行其他同步
      }
      
      // 5. 同步题目媒体文件
      console.log('  1.5 同步题目媒体文件...')
      try {
        await this.syncQuestionMedia(headers)
      } catch (error) {
        console.error('  1.5 同步题目媒体文件失败:', error.message)
        // 继续执行其他同步
      }
      
      console.log('✓ 业务数据同步完成')
    } catch (error) {
      console.error('同步业务数据失败:', error)
      console.error('错误堆栈:', error.stack)
      // 不抛出异常，允许其他同步继续
    }
  }

  /**
   * 同步题目分类
   */
  async syncQuestionCategories(headers) {
    try {
      // 清空现有数据
      this.db.exec('DELETE FROM question_category')
      
      // 获取分类树（使用客户端专用接口）
      const response = await axios.post(`${API_BASE_URL}/student/sync/category/tree`, {
        status: 0 // 只获取启用状态的分类
      }, { headers })
      
      if (response.data && response.data.code === 200 && response.data.data) {
        const categories = this.flattenCategoryTree(response.data.data)
        console.log(`  获取到 ${categories.length} 个分类`)
        
        const stmt = this.db.prepare(`
          INSERT INTO question_category 
          (id, name, father_id, is_default, sort_num, status, create_by, create_time, update_by, update_time, remark)
          VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        `)
        
        const insertMany = this.db.transaction((items) => {
          for (const item of items) {
            stmt.run(
              item.id,
              item.name,
              item.fatherId || item.father_id || null,
              item.isDefault || item.is_default ? 1 : 0,
              item.sortNum || item.sort_num || 0,
              item.status || 0,
              item.createBy || item.create_by || '',
              item.createTime ? new Date(item.createTime).getTime() : Date.now(),
              item.updateBy || item.update_by || '',
              item.updateTime ? new Date(item.updateTime).getTime() : null,
              item.remark || ''
            )
          }
        })
        
        insertMany(categories)
        console.log(`  ✓ 题目分类同步完成，共 ${categories.length} 条`)
      }
    } catch (error) {
      console.error('同步题目分类失败:', error.message)
      throw error
    }
  }

  /**
   * 扁平化分类树（递归）
   */
  flattenCategoryTree(tree, result = []) {
    for (const node of tree) {
      result.push(node)
      if (node.children && node.children.length > 0) {
        this.flattenCategoryTree(node.children, result)
      }
    }
    return result
  }

  /**
   * 同步题目（分页获取所有题目）
   * 先获取题目列表，然后逐个获取详情（包含答案、完形填空区域）
   */
  async syncQuestions(headers) {
    try {
      // 清空现有数据
      this.db.exec('DELETE FROM question')
      this.db.exec('DELETE FROM question_answer')
      this.db.exec('DELETE FROM question_blank_area')
      
      // 第一步：获取所有题目ID列表
      let pageNum = 1
      const pageSize = 100
      const questionIds = []
      
      while (true) {
        const response = await axios.post(`${API_BASE_URL}/student/sync/question/list`, {
          pageNum,
          pageSize,
          status: 1 // 只获取启用状态的题目
        }, { headers })
        
        if (response.data && response.data.code === 200 && response.data.rows) {
          const questions = response.data.rows
          if (questions.length === 0) {
            break
          }
          
          // 先插入题目基本信息
          const questionStmt = this.db.prepare(`
            INSERT INTO question 
            (id, question_category_id, title, media_type, subject_id, type, option_type, weight, answer, analyzes, status, create_by, create_time, update_by, update_time, remark)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
          `)
          
          const insertMany = this.db.transaction((items) => {
            for (const item of items) {
              questionStmt.run(
                item.id,
                item.questionCategoryId || item.question_category_id,
                item.title,
                item.mediaType || item.media_type,
                item.subjectId || item.subject_id,
                item.type,
                item.optionType || item.option_type,
                item.weight,
                item.answer,
                item.analyzes || item.analysis,
                item.status,
                item.createBy || item.create_by || '',
                item.createTime ? new Date(item.createTime).getTime() : Date.now(),
                item.updateBy || item.update_by || '',
                item.updateTime ? new Date(item.updateTime).getTime() : null,
                item.remark || ''
              )
              questionIds.push(item.id)
            }
          })
          
          insertMany(questions)
          
          // 检查是否还有更多数据
          if (questions.length < pageSize) {
            break
          }
          
          pageNum++
        } else {
          break
        }
      }
      
      console.log(`  已获取 ${questionIds.length} 条题目基本信息，开始获取详情...`)
      
      // 第二步：逐个获取题目详情（包含答案、完形填空区域）
      const answerStmt = this.db.prepare(`
        INSERT INTO question_answer 
        (id, question_id, blank_area_id, serial_no, option_name, option_content, is_answer, status, create_by, create_time, update_by, update_time, remark)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      `)
      
      const blankAreaStmt = this.db.prepare(`
        INSERT INTO question_blank_area 
        (id, question_id, blank_index, answer_ids, status, create_by, create_time, update_by, update_time, remark)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      `)
      
      let detailCount = 0
      for (const questionId of questionIds) {
        try {
          // 获取题目详情（使用客户端专用接口）
          const detailResponse = await axios.post(`${API_BASE_URL}/student/sync/question/detail`, {
            id: questionId
          }, { headers })
          
          if (detailResponse.data && detailResponse.data.code === 200 && detailResponse.data.data) {
            const question = detailResponse.data.data
            
            // 插入答案
            if (question.answers && Array.isArray(question.answers)) {
              for (const answer of question.answers) {
                answerStmt.run(
                  answer.id,
                  questionId,
                  answer.blankAreaId || answer.blank_area_id || null,
                  answer.serialNo || answer.serial_no,
                  answer.optionName || answer.option_name,
                  answer.optionContent || answer.option_content,
                  answer.isAnswer || answer.is_answer ? 1 : 0,
                  answer.status || 1,
                  answer.createBy || answer.create_by || '',
                  answer.createTime ? new Date(answer.createTime).getTime() : Date.now(),
                  answer.updateBy || answer.update_by || '',
                  answer.updateTime ? new Date(answer.updateTime).getTime() : null,
                  answer.remark || ''
                )
              }
            }
            
            // 插入完形填空区域
            if (question.blankAreas && Array.isArray(question.blankAreas)) {
              for (const area of question.blankAreas) {
                blankAreaStmt.run(
                  area.id,
                  questionId,
                  area.blankIndex || area.blank_index,
                  area.answerIds || area.answer_ids,
                  area.status || 1,
                  area.createBy || area.create_by || '',
                  area.createTime ? new Date(area.createTime).getTime() : Date.now(),
                  area.updateBy || area.update_by || '',
                  area.updateTime ? new Date(area.updateTime).getTime() : null,
                  area.remark || ''
                )
              }
            }
            
            detailCount++
            if (detailCount % 10 === 0) {
              console.log(`  已获取 ${detailCount}/${questionIds.length} 条题目详情...`)
            }
          }
        } catch (error) {
          console.warn(`  获取题目 ${questionId} 详情失败:`, error.message)
          // 继续处理下一个题目
        }
      }
      
      console.log(`  ✓ 题目同步完成，共 ${questionIds.length} 条（包含答案和完形填空区域）`)
    } catch (error) {
      console.error('同步题目失败:', error.message)
      throw error
    }
  }

  /**
   * 同步试卷
   */
  async syncPapers(headers) {
    try {
      // 清空现有数据
      this.db.exec('DELETE FROM paper')
      
      let pageNum = 1
      const pageSize = 100
      let totalCount = 0
      
      while (true) {
        const response = await axios.post(`${API_BASE_URL}/student/sync/paper/list`, {
          pageNum,
          pageSize,
          status: 1 // 只获取启用状态的试卷
        }, { headers })
        
        if (response.data && response.data.code === 200 && response.data.rows) {
          const papers = response.data.rows
          if (papers.length === 0) {
            break
          }
          
          const stmt = this.db.prepare(`
            INSERT INTO paper 
            (id, paper_name, paper_code, paper_type, paper_desc, 
             year, month, province, custom_name,
             business_type, business_id, total_score, total_questions, duration, 
             practice_limit, trial_listen_enabled, trial_listen_text, notes, notes_display_mode,
             enable_start_time, enable_end_time,
             intro_audio_url, intro_audio_path, intro_audio_duration, intro_text, auto_next_question, show_answer_immediately, 
             allow_review, question_read_duration, version, package_hash, package_size, last_package_time, status, 
             create_by, create_time, update_by, update_time, remark)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
          `)
          
          const insertMany = this.db.transaction((items) => {
            for (const item of items) {
              stmt.run(
                item.id,
                item.paperName || item.paper_name,
                item.paperCode || item.paper_code,
                item.paperType || item.paper_type || null,
                item.paperDesc || item.paper_desc,
                item.year || null,
                item.month || null,
                item.province || null,
                item.customName || item.custom_name || null,
                item.businessType || item.business_type || 5,
                item.businessId || item.business_id,
                item.totalScore || item.total_score || 0,
                item.totalQuestions || item.total_questions || 0,
                item.duration,
                item.practiceLimit || item.practice_limit || 0,
                item.trialListenEnabled || item.trial_listen_enabled ? 1 : 0,
                item.trialListenText || item.trial_listen_text || null,
                item.notes || null,
                item.notesDisplayMode || item.notes_display_mode || 'before_exam',
                item.enableStartTime ? new Date(item.enableStartTime).getTime() : (item.enable_start_time ? new Date(item.enable_start_time).getTime() : null),
                item.enableEndTime ? new Date(item.enableEndTime).getTime() : (item.enable_end_time ? new Date(item.enable_end_time).getTime() : null),
                item.introAudioUrl || item.intro_audio_url,
                item.introAudioPath || item.intro_audio_path,
                item.introAudioDuration || item.intro_audio_duration,
                item.introText || item.intro_text,
                item.autoNextQuestion || item.auto_next_question !== undefined ? (item.autoNextQuestion || item.auto_next_question ? 1 : 0) : 1,
                item.showAnswerImmediately || item.show_answer_immediately ? 1 : 0,
                item.allowReview || item.allow_review !== undefined ? (item.allowReview || item.allow_review ? 1 : 0) : 1,
                item.questionReadDuration || item.question_read_duration,
                item.version || 1,
                item.packageHash || item.package_hash,
                item.packageSize || item.package_size,
                item.lastPackageTime ? new Date(item.lastPackageTime).getTime() : null,
                item.status || 1,
                item.createBy || item.create_by || '',
                item.createTime ? new Date(item.createTime).getTime() : Date.now(),
                item.updateBy || item.update_by || '',
                item.updateTime ? new Date(item.updateTime).getTime() : null,
                item.remark || ''
              )
            }
          })
          
          insertMany(papers)
          totalCount += papers.length
          
          console.log(`  已同步 ${totalCount} 条试卷...`)
          
          // 检查是否还有更多数据
          if (papers.length < pageSize) {
            break
          }
          
          pageNum++
        } else {
          break
        }
      }
      
      console.log(`  ✓ 试卷同步完成，共 ${totalCount} 条`)
    } catch (error) {
      console.error('同步试卷失败:', error.message)
      throw error
    }
  }

  /**
   * 同步试卷-题目关联
   */
  async syncPaperQuestions(headers) {
    try {
      // 清空现有数据
      this.db.exec('DELETE FROM paper_question')
      
      // 获取所有试卷ID
      const papers = this.db.prepare('SELECT id FROM paper WHERE status = 1').all()
      
      if (papers.length === 0) {
        console.log('  没有试卷，跳过试卷-题目关联同步')
        return
      }
      
      let totalCount = 0
      const stmt = this.db.prepare(`
        INSERT INTO paper_question 
        (id, paper_id, question_id, section_id, section_order, sort_order, score, create_by, create_time, update_by, update_time)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      `)
      
      // 遍历每个试卷，获取其题目列表
      for (const paper of papers) {
        try {
          // 获取试卷详情（使用客户端专用接口）
          const response = await axios.post(`${API_BASE_URL}/student/sync/paper/detail`, {
            id: paper.id
          }, { headers })
          
          if (response.data && response.data.code === 200 && response.data.data) {
            const paperData = response.data.data
            const questions = paperData.questions || []
            
            if (questions.length > 0) {
              const insertMany = this.db.transaction((items) => {
                let sortOrder = 0
                for (const question of items) {
                  // PaperQuestionDTO中questionId是题目ID
                  const questionId = question.questionId || question.question_id || question.id
                  if (questionId) {
                    stmt.run(
                      null, // id自增
                      paper.id,
                      questionId,
                      question.sectionId || question.section_id || null,
                      question.sectionOrder || question.section_order || null,
                      question.sortOrder || question.sort_order || sortOrder++,
                      question.score || 0,
                      question.createBy || question.create_by || null, // create_by
                      question.createTime || question.create_time ? new Date(question.createTime || question.create_time).getTime() : Date.now(), // create_time
                      question.updateBy || question.update_by || null, // update_by
                      question.updateTime || question.update_time ? new Date(question.updateTime || question.update_time).getTime() : null // update_time
                    )
                    totalCount++
                  }
                }
              })
              
              insertMany(questions)
            }
          }
        } catch (error) {
          console.warn(`  获取试卷 ${paper.id} 的题目失败:`, error.message)
          // 继续处理下一个试卷
        }
      }
      
      console.log(`  ✓ 试卷-题目关联同步完成，共 ${totalCount} 条`)
    } catch (error) {
      console.error('同步试卷-题目关联失败:', error.message)
      throw error
    }
  }

  /**
   * 同步题目媒体文件
   * 从题目详情中提取媒体文件信息
   */
  async syncQuestionMedia(headers) {
    try {
      // 清空现有数据
      this.db.exec('DELETE FROM question_media')
      
      // 获取所有题目ID
      const questions = this.db.prepare('SELECT id FROM question WHERE status = 1').all()
      
      if (questions.length === 0) {
        console.log('  没有题目，跳过题目媒体文件同步')
        return
      }
      
      let totalCount = 0
      const stmt = this.db.prepare(`
        INSERT INTO question_media 
        (id, question_id, paper_id, volume_id, section_id, intermission_id,
         media_type, option_id, blank_area_id, media_name, media_path, media_url, 
         media_size, media_format, media_duration, is_compressed, storage_type, create_time)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      `)
      
      // 遍历每个题目，获取其媒体文件
      let processedCount = 0
      for (const question of questions) {
        try {
          // 获取题目详情（使用客户端专用接口）
          const response = await axios.post(`${API_BASE_URL}/student/sync/question/detail`, {
            id: question.id
          }, { headers })
          
          if (response.data && response.data.code === 200 && response.data.data) {
            const questionData = response.data.data
            
            // 题目媒体文件（mediaType=1, 4-题目音频, 5-讲解音频, 6-讲解图片）
            const questionMediaList = questionData.mediaUrl || []
            for (const media of questionMediaList) {
              stmt.run(
                media.id || media.materialId || null,
                question.id,
                media.paperId || media.paper_id || null,
                media.volumeId || media.volume_id || null,
                media.sectionId || media.section_id || null,
                media.intermissionId || media.intermission_id || null,
                media.mediaType || media.media_type || 1, // 默认为题目媒体
                null,
                null,
                media.mediaName || media.media_name || '',
                media.mediaPath || media.media_path,
                media.mediaUrl || media.media_url,
                media.mediaSize || media.media_size,
                media.mediaFormat || media.media_format,
                media.mediaDuration || media.media_duration,
                media.isCompressed || media.is_compressed ? 1 : 0,
                media.storageType || media.storage_type || 0,
                media.createTime ? new Date(media.createTime).getTime() : Date.now()
              )
              totalCount++
            }
            
            // 选项媒体文件（从answers中提取）
            if (questionData.answers && Array.isArray(questionData.answers)) {
              for (const answer of questionData.answers) {
                if (answer.mediaUrl && Array.isArray(answer.mediaUrl)) {
                  for (const media of answer.mediaUrl) {
                    stmt.run(
                      media.id || media.materialId || null,
                      question.id,
                      media.paperId || media.paper_id || null,
                      media.volumeId || media.volume_id || null,
                      media.sectionId || media.section_id || null,
                      media.intermissionId || media.intermission_id || null,
                      2, // 选项媒体
                      answer.id,
                      null,
                      media.mediaName || media.media_name || '',
                      media.mediaPath || media.media_path,
                      media.mediaUrl || media.media_url,
                      media.mediaSize || media.media_size,
                      media.mediaFormat || media.media_format,
                      media.mediaDuration || media.media_duration,
                      media.isCompressed || media.is_compressed ? 1 : 0,
                      media.storageType || media.storage_type || 0,
                      media.createTime ? new Date(media.createTime).getTime() : Date.now()
                    )
                    totalCount++
                  }
                }
              }
            }
            
            // 辅助识图（mediaType=3）
            const recognitionList = questionData.aidedRecognitionUrl || []
            for (const media of recognitionList) {
              stmt.run(
                media.id || media.materialId || null,
                question.id,
                media.paperId || media.paper_id || null,
                media.volumeId || media.volume_id || null,
                media.sectionId || media.section_id || null,
                media.intermissionId || media.intermission_id || null,
                3, // 辅助识图
                null,
                null,
                media.mediaName || media.media_name || '',
                media.mediaPath || media.media_path,
                media.mediaUrl || media.media_url,
                media.mediaSize || media.media_size,
                media.mediaFormat || media.media_format,
                media.mediaDuration || media.media_duration,
                media.isCompressed || media.is_compressed ? 1 : 0,
                media.storageType || media.storage_type || 0,
                media.createTime ? new Date(media.createTime).getTime() : Date.now()
              )
              totalCount++
            }
          }
          
          processedCount++
          if (processedCount % 10 === 0) {
            console.log(`  已处理 ${processedCount}/${questions.length} 条题目的媒体文件...`)
          }
        } catch (error) {
          console.warn(`  获取题目 ${question.id} 的媒体文件失败:`, error.message)
          // 继续处理下一个题目
        }
      }
      
      console.log(`  ✓ 题目媒体文件同步完成，共 ${totalCount} 条`)
    } catch (error) {
      console.error('同步题目媒体文件失败:', error.message)
      throw error
    }
  }

  /**
   * 同步练习次数重置记录
   * 从服务端获取重置记录，清除本地对应的答题记录
   */
  async syncPaperResets(token) {
    try {
      if (!token) {
        console.log('无token，跳过重置记录同步')
        return
      }

      // 获取上次同步时间
      let lastSyncTime = null
      try {
        const result = this.db.prepare(`
          SELECT value FROM app_config WHERE key = 'last_reset_sync_time'
        `).get()
        if (result) {
          lastSyncTime = parseInt(result.value)
        }
      } catch (e) {
        // 表可能不存在，忽略
      }

      // 从服务端获取重置记录
      const url = lastSyncTime 
        ? `${API_BASE_URL}/student/paper/reset/sync?sinceTime=${lastSyncTime}`
        : `${API_BASE_URL}/student/paper/reset/sync`
      
      const response = await axios.get(url, {
        headers: { 'Authorization': `Bearer ${token}` }
      })

      if (response.data?.code !== 200) {
        console.warn('获取重置记录失败:', response.data?.msg)
        return
      }

      const resets = response.data.data || []
      if (resets.length === 0) {
        console.log('无新的重置记录')
        return
      }

      console.log(`获取到 ${resets.length} 条重置记录，开始清除本地答题记录...`)

      // 处理每条重置记录
      for (const reset of resets) {
        const userId = reset.userId
        const paperId = reset.paperId
        const resetTime = new Date(reset.resetTime).getTime()

        if (paperId) {
          // 重置指定试卷
          const deleted = this.db.prepare(`
            DELETE FROM app_user_paper_info 
            WHERE app_user_id = ? AND paper_id = ? AND start_time < ?
          `).run(userId, paperId, resetTime)
          console.log(`  清除用户 ${userId} 试卷 ${paperId} 的答题记录: ${deleted.changes} 条`)
        } else {
          // 重置所有试卷
          const deleted = this.db.prepare(`
            DELETE FROM app_user_paper_info 
            WHERE app_user_id = ? AND start_time < ?
          `).run(userId, resetTime)
          console.log(`  清除用户 ${userId} 所有答题记录: ${deleted.changes} 条`)
        }
      }

      // 保存同步时间
      this.db.exec(`CREATE TABLE IF NOT EXISTS app_config (key TEXT PRIMARY KEY, value TEXT)`)
      this.db.prepare(`INSERT OR REPLACE INTO app_config (key, value) VALUES ('last_reset_sync_time', ?)`)
        .run(Date.now().toString())

      console.log('✓ 练习次数重置记录同步完成')
    } catch (error) {
      console.error('同步重置记录失败:', error.message)
      // 不抛出异常，不影响其他同步
    }
  }
}

module.exports = SyncService

