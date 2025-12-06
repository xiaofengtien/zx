const axios = require('axios')
const crypto = require('crypto')

// 后端API地址（需要根据实际情况配置）
// 注意：Electron 应用直接连接后端，不需要 /dev-api 前缀（该前缀仅用于前端 Vue 应用的代理）
const API_BASE_URL = process.env.API_BASE_URL || 'http://localhost:8080'

class LoginService {
  constructor(db) {
    this.db = db.getDB()
  }

  /**
   * 在线登录（简化版：只获取 token 和用户信息，数据从本地数据库获取）
   */
  async onlineLogin(username, password, code, uuid) {
    try {
      console.log('开始在线登录，参数:', { username, code, uuid: uuid ? '已提供' : '未提供' })
      console.log('API地址:', `${API_BASE_URL}/student/onlineLogin`)
      
      // 调用后端在线登录接口
      const response = await axios.post(`${API_BASE_URL}/student/onlineLogin`, {
        username,
        password,
        code,
        uuid
      }, {
        headers: {
          'Content-Type': 'application/json'
        }
      })

      console.log('后端响应:', response.data)

      if (response.data && response.data.code === 200 && response.data.token) {
        const token = response.data.token
        console.log('登录成功，获取到token，开始获取学员信息')
        
        // 获取学员信息（只获取基本信息，不保存数据）
        const userInfo = await this.getStudentInfo(token)
        
        if (userInfo) {
          console.log('=== 获取到学员信息 ===')
          console.log('userInfo 对象:', JSON.stringify(userInfo, null, 2))
          
          // 保存学员账号到 userInfo（用于后续查询）
          if (!userInfo.studentAccount && userInfo.user?.userName) {
            userInfo.studentAccount = userInfo.user.userName
            console.log('设置 studentAccount:', userInfo.studentAccount)
          }
          
          // 如果 getInfo 接口返回了 archiveId 和 applicablePapers，优先保存到本地数据库
          if (userInfo.archiveId && userInfo.applicablePapers) {
            console.log('getInfo 接口返回了学员档案信息，优先保存到本地数据库')
            console.log('archiveId:', userInfo.archiveId)
            console.log('applicablePapers:', userInfo.applicablePapers)
            
            try {
              await this.saveCurrentStudentArchive(userInfo)
              console.log('✓ 当前学员档案已保存到本地数据库')
            } catch (error) {
              console.error('保存当前学员档案失败:', error)
              // 不抛出异常，登录仍然成功
            }
          }
          
          // 保存 token 到 userInfo
          userInfo.token = token
          
          // 关键：每次在线登录成功后，更新本地保存的凭证（包括密码hash）
          // 这样可以防止密码被修改后，离线登录时使用旧密码hash导致失败
          try {
            console.log('开始更新本地凭证（包括密码hash）...')
            await this.saveStudentCredentials(username, password, userInfo, token)
            console.log('✓ 本地凭证已更新（密码hash已更新）')
          } catch (error) {
            console.error('更新本地凭证失败:', error)
            // 不抛出异常，登录仍然成功，但离线登录可能会失败
            console.warn('⚠️ 警告：本地凭证更新失败，离线登录可能会失败')
          }
          
          return {
            success: true,
            token,
            userInfo
          }
        } else {
          console.error('获取学员信息失败')
          return {
            success: false,
            message: '获取学员信息失败'
          }
        }
      }
      
      console.error('登录失败，响应数据:', response.data)
      return {
        success: false,
        message: response.data?.msg || '登录失败'
      }
    } catch (error) {
      console.error('在线登录异常:', error)
      console.error('错误详情:', {
        message: error.message,
        response: error.response?.data,
        status: error.response?.status,
        statusText: error.response?.statusText
      })
      return {
        success: false,
        message: error.response?.data?.msg || error.message || '登录失败'
      }
    }
  }

  /**
   * 离线登录
   */
  async offlineLogin(username, password, offlineCredential) {
    try {
      // 从本地数据库获取凭证
      const credential = this.db.prepare(`
        SELECT * FROM student_credentials 
        WHERE student_account = ?
      `).get(username)

      if (!credential) {
        return {
          success: false,
          message: '未找到本地凭证，请先在线登录'
        }
      }

      // 验证密码
      const passwordHash = this.hashPassword(password)
      if (credential.password_hash !== passwordHash) {
        return {
          success: false,
          message: '密码错误'
        }
      }

      // 如果有离线凭证，验证离线凭证
      if (offlineCredential && credential.offline_credential) {
        // TODO: 验证离线凭证的有效性和过期时间
        // 这里简化处理，实际应该解密和验证离线凭证
      }

      // 检查凭证是否过期
      if (credential.expire_time && credential.expire_time < Date.now()) {
        return {
          success: false,
          message: '离线凭证已过期，请重新在线登录'
        }
      }

      // 更新最后登录时间
      this.db.prepare(`
        UPDATE student_credentials 
        SET last_login_time = ? 
        WHERE student_account = ?
      `).run(Date.now(), username)

      // 获取适用考卷类型
      const papers = this.getStudentPapers(username)

      return {
        success: true,
        userInfo: {
          studentAccount: username,
          archiveId: credential.archive_id,
          userId: credential.user_id
        },
        papers
      }
    } catch (error) {
      console.error('离线登录失败:', error)
      return {
        success: false,
        message: error.message || '离线登录失败'
      }
    }
  }

  /**
   * 获取学员信息
   */
  async getStudentInfo(token) {
    try {
      console.log('获取学员信息，token:', token ? '已提供' : '未提供')
      const response = await axios.get(`${API_BASE_URL}/student/getInfo`, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      })

      console.log('=== 原始响应数据 ===')
      console.log('response 对象:', response)
      console.log('response.data:', response.data)
      console.log('response.data 类型:', typeof response.data)
      console.log('response.data 是否为 null:', response.data === null)
      console.log('response.data 是否为 undefined:', response.data === undefined)
      console.log('response.data 的所有键:', Object.keys(response.data || {}))
      console.log('响应数据结构（完整）:', JSON.stringify(response.data, null, 2))
      
      // 直接检查 applicablePapers
      console.log('直接检查 response.data.applicablePapers:', response.data?.applicablePapers)
      console.log('直接检查 response.data.archiveId:', response.data?.archiveId)

      if (response.data && response.data.code === 200) {
        // 先创建一个基础对象
        let userInfo = {}
        
        // 如果响应中有 data 字段，使用它；否则从根级别获取
        if (response.data.data) {
          userInfo = response.data.data
          console.log('使用 response.data.data 作为 userInfo')
        } else {
          userInfo = {
            user: response.data.user,
            roles: response.data.roles,
            permissions: response.data.permissions
          }
          console.log('从根级别构建 userInfo')
        }
        
        console.log('解析后的用户信息:', userInfo)
        console.log('响应中的 archiveId (根级别):', response.data.archiveId)
        console.log('响应中的 applicablePapers (根级别):', response.data.applicablePapers)
        console.log('响应中的 needForceChangePassword (根级别):', response.data.needForceChangePassword)
        console.log('响应中的 applicablePapers 类型:', typeof response.data.applicablePapers)
        console.log('响应中的 applicablePapers 是否为数组:', Array.isArray(response.data.applicablePapers))
        
        // 从响应中直接获取学员档案信息（后端 getInfo 接口已经返回了）
        // 注意：archiveId、applicablePapers 和 needForceChangePassword 在 response.data 的根级别，不在 data 字段中
        if (response.data.archiveId) {
          userInfo.archiveId = response.data.archiveId
          console.log('设置 archiveId:', userInfo.archiveId)
        }
        
        // 获取学员档案对象（包含 studentName 等字段）
        if (response.data.archive) {
          userInfo.archive = response.data.archive
          userInfo.studentName = response.data.archive.studentName || response.data.archive.student_name
          userInfo.seatNumber = response.data.archive.seatNumber || response.data.archive.seat_number
          console.log('设置 archive:', { studentName: userInfo.studentName, seatNumber: userInfo.seatNumber })
        }
        
        // 获取是否需要强制修改密码标识
        if (response.data.needForceChangePassword !== undefined) {
          userInfo.needForceChangePassword = response.data.needForceChangePassword
          console.log('设置 needForceChangePassword:', userInfo.needForceChangePassword)
        }
        
        // 关键：applicablePapers 在 response.data 的根级别
        const rawApplicablePapers = response.data.applicablePapers
        console.log('=== 解析 applicablePapers ===')
        console.log('原始值 (rawApplicablePapers):', rawApplicablePapers)
        console.log('原始值类型:', typeof rawApplicablePapers)
        console.log('是否为 undefined:', rawApplicablePapers === undefined)
        console.log('是否为 null:', rawApplicablePapers === null)
        console.log('是否为数组:', Array.isArray(rawApplicablePapers))
        
        if (rawApplicablePapers !== undefined && rawApplicablePapers !== null) {
          console.log('找到 applicablePapers，开始处理')
          // 确保是数组
          if (Array.isArray(rawApplicablePapers)) {
            userInfo.applicablePapers = [...rawApplicablePapers] // 使用展开运算符创建新数组
            console.log('✓ 从 getInfo 接口获取到适用试卷类型（数组）:', userInfo.applicablePapers)
            console.log('✓ 复制后的数组长度:', userInfo.applicablePapers.length)
            console.log('✓ 数组内容:', JSON.stringify(userInfo.applicablePapers))
          } else if (typeof rawApplicablePapers === 'string') {
            // 如果是字符串，尝试解析
            userInfo.applicablePapers = rawApplicablePapers.split(',').map(s => s.trim()).filter(s => s)
            console.log('✓ 从 getInfo 接口获取到适用试卷类型（字符串解析）:', userInfo.applicablePapers)
          } else {
            console.error('✗ applicablePapers 类型未知:', typeof rawApplicablePapers, rawApplicablePapers)
            userInfo.applicablePapers = []
          }
        } else {
          console.error('✗ 响应中没有 applicablePapers 字段或为 null/undefined')
          console.error('检查 response.data 的所有键:', Object.keys(response.data))
          console.error('检查 response.data 的完整内容:', JSON.stringify(response.data, null, 2))
          userInfo.applicablePapers = []
        }
        
        console.log('最终 userInfo.applicablePapers:', userInfo.applicablePapers)
        console.log('最终 userInfo.applicablePapers 长度:', userInfo.applicablePapers ? userInfo.applicablePapers.length : 0)
        console.log('最终 userInfo 对象:', JSON.stringify(userInfo, null, 2))
        
        // 如果 getInfo 接口没有返回档案信息，尝试通过其他方式获取
        if (!userInfo.applicablePapers || userInfo.applicablePapers.length === 0) {
          console.log('getInfo 接口未返回档案信息，尝试通过其他方式获取')
          const studentAccount = userInfo.user?.userName
          const userId = userInfo.user?.userId
          
          let archiveInfo = null
          
          // 方法1: 尝试通过 userId 查询（userId 可能就是 archiveId）
          if (userId) {
            console.log('尝试通过 userId 查询档案:', userId)
            archiveInfo = await this.getStudentArchiveById(userId, token)
          }
          
          // 方法2: 如果通过 userId 没找到，尝试从本地数据库获取 archiveId
          if (!archiveInfo && studentAccount) {
            archiveInfo = await this.getStudentArchive(studentAccount, token)
          }
          
        if (archiveInfo) {
          userInfo.applicablePapers = archiveInfo.applicablePapers || []
          userInfo.archiveId = archiveInfo.id
            console.log('通过其他方式获取到学员档案信息:', {
              archiveId: archiveInfo.id,
              applicablePapers: archiveInfo.applicablePapers
            })
          } else {
            console.warn('未能获取学员档案信息，studentAccount:', studentAccount, 'userId:', userId)
          }
        }
        
        return userInfo
      }
      console.error('获取学员信息失败，响应code不是200:', response.data)
      return null
    } catch (error) {
      console.error('获取学员信息异常:', error)
      console.error('错误详情:', {
        message: error.message,
        response: error.response?.data,
        status: error.response?.status
      })
      return null
    }
  }

  /**
   * 通过学员账号查询档案（需要后端提供接口，或者通过列表查询）
   * 目前后端没有直接通过账号查询的接口，所以先尝试通过 userId 查询
   */
  async getStudentArchiveByAccount(studentAccount, token) {
    try {
      if (!studentAccount) {
        return null
      }
      
      console.log('尝试通过学员账号查询档案:', studentAccount)
      
      // 后端没有直接通过账号查询的接口，所以先尝试查询列表然后过滤
      // 或者可以尝试通过 userId 查询（如果 userId 就是 archiveId）
      // 这里先返回 null，让调用方尝试其他方法
      return null
    } catch (error) {
      console.error('通过学员账号查询档案失败:', error)
      return null
    }
  }

  /**
   * 通过 archiveId 获取学员档案信息
   */
  async getStudentArchiveById(archiveId, token) {
    try {
      if (!archiveId) {
        return null
      }
      
      console.log('通过 archiveId 获取学员档案:', archiveId)
      
      const archiveResponse = await axios.post(`${API_BASE_URL}/student/archive/getArchive`, {
        archiveId: archiveId
      }, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      })
      
      console.log('档案查询响应:', archiveResponse.data)
      
      if (archiveResponse.data && archiveResponse.data.code === 200 && archiveResponse.data.data) {
        const archive = archiveResponse.data.data
        console.log('获取到学员档案:', {
          id: archive.id,
          studentAccount: archive.studentAccount,
          applicablePapers: archive.applicablePapers
        })
        return archive
      }
      
      return null
    } catch (error) {
      console.error('通过 archiveId 获取学员档案失败:', error)
      console.error('错误详情:', {
        message: error.message,
        response: error.response?.data,
        status: error.response?.status
      })
      return null
    }
  }

  /**
   * 获取学员档案信息（包含适用考卷类型）
   * 通过学员账号查询档案（从本地数据库获取 archiveId）
   */
  async getStudentArchive(studentAccount, token) {
    try {
      if (!studentAccount) {
        console.warn('学员账号为空，无法获取档案信息')
        return null
      }
      
      console.log('开始获取学员档案，学员账号:', studentAccount)
      
      // 从本地数据库查询之前保存的 archiveId
      const credential = this.db.prepare(`
        SELECT archive_id FROM student_credentials 
        WHERE student_account = ?
      `).get(studentAccount)
      
      if (credential && credential.archive_id) {
        console.log('从本地数据库获取到 archiveId:', credential.archive_id)
        return await this.getStudentArchiveById(credential.archive_id, token)
      }
      
      console.warn('本地数据库中没有找到 archiveId，无法获取学员档案')
      return null
    } catch (error) {
      console.error('获取学员档案失败:', error)
      return null
    }
  }

  /**
   * 保存学员凭证到本地数据库
   * 每次在线登录成功后调用，更新密码hash和离线凭证信息
   * 这样可以防止密码被修改后，离线登录时使用旧密码hash导致失败
   */
  async saveStudentCredentials(username, password, userInfo, token) {
    const passwordHash = this.hashPassword(password)
    const now = Date.now()
    const expireTime = now + (30 * 24 * 60 * 60 * 1000) // 30天

    // 检查是否已存在凭证
    const existingCredential = this.db.prepare(`
      SELECT create_time FROM student_credentials 
      WHERE student_account = ?
    `).get(username)

    // 如果已存在，保留原始的create_time；如果不存在，使用当前时间
    const createTime = existingCredential ? existingCredential.create_time : now

    console.log('保存/更新学员凭证:', {
      student_account: username,
      archive_id: userInfo.archiveId || null,
      user_id: userInfo.user?.userId || null,
      create_time: createTime,
      expire_time: expireTime,
      last_login_time: now
    })

    // 插入或更新凭证（包括密码hash）
    // 注意：使用 INSERT OR REPLACE 会更新所有字段，包括 password_hash
    // 这样即使密码被修改了，本地保存的hash也会更新
    this.db.prepare(`
      INSERT OR REPLACE INTO student_credentials 
      (student_account, password_hash, archive_id, user_id, create_time, expire_time, last_login_time)
      VALUES (?, ?, ?, ?, ?, ?, ?)
    `).run(
      username,
      passwordHash, // 使用当前登录的密码hash（如果密码被修改，这里会更新）
      userInfo.archiveId || null,
      userInfo.user?.userId || null,
      createTime, // 保留原始的创建时间
      expireTime, // 更新过期时间（延长30天）
      now // 更新最后登录时间
    )

    console.log('✓ 学员凭证已保存/更新（密码hash已更新）')
  }

  /**
   * 保存学员适用考卷类型
   */
  async saveStudentPapers(username, userInfo) {
    console.log('=== 开始保存学员试卷类型 ===')
    console.log('学员账号:', username)
    console.log('userInfo 对象:', userInfo)
    console.log('userInfo.applicablePapers:', userInfo.applicablePapers)
    console.log('userInfo.applicablePapers 类型:', typeof userInfo.applicablePapers)
    console.log('userInfo.applicablePapers 是否为数组:', Array.isArray(userInfo.applicablePapers))
    
    // 从用户信息中获取适用考卷类型（dictValue 列表）
    let paperTypes = userInfo.applicablePapers || []
    
    console.log('从用户信息获取的适用试卷类型:', paperTypes)
    console.log('试卷类型数量:', paperTypes.length)
    
    // 如果用户信息中没有，尝试从后端获取学员档案
    if (paperTypes.length === 0 && userInfo.archiveId) {
      try {
        console.log('尝试从后端获取学员档案，archiveId:', userInfo.archiveId)
        const archiveResponse = await axios.post(`${API_BASE_URL}/student/archive/getArchive`, {
          archiveId: userInfo.archiveId
        }, {
          headers: {
            'Authorization': `Bearer ${userInfo.token || ''}`
          }
        })
        
        console.log('学员档案响应:', archiveResponse.data)
        
        if (archiveResponse.data && archiveResponse.data.code === 200 && archiveResponse.data.data) {
          const archive = archiveResponse.data.data
          paperTypes = archive.applicablePapers || []
          console.log('从学员档案获取的适用试卷类型:', paperTypes)
        }
      } catch (error) {
        console.error('获取学员档案失败:', error)
        console.error('错误详情:', {
          message: error.message,
          response: error.response?.data,
          status: error.response?.status
        })
      }
    }
    
    // 确保 paperTypes 是数组
    if (!Array.isArray(paperTypes)) {
      console.warn('paperTypes 不是数组，转换为数组:', paperTypes)
      // 如果是字符串，尝试解析（可能是逗号分隔的字符串）
      if (typeof paperTypes === 'string') {
        paperTypes = paperTypes.split(',').map(s => s.trim()).filter(s => s)
      } else {
        paperTypes = []
      }
    }
    
    console.log('最终保存的试卷类型:', paperTypes)
    console.log('试卷类型数量:', paperTypes.length)
    console.log('试卷类型 JSON:', JSON.stringify(paperTypes))
    
    // 保存到本地数据库
    const paperTypesJson = JSON.stringify(paperTypes)
    const now = Date.now()
    
    console.log('准备保存到数据库，JSON 字符串:', paperTypesJson)
    console.log('当前时间戳:', now)

    // 插入或更新考卷类型
    try {
      const stmt = this.db.prepare(`
      INSERT OR REPLACE INTO student_papers 
      (student_account, paper_types, update_time)
      VALUES (?, ?, ?)
      `)
      
      console.log('执行 SQL 插入/更新操作...')
      const result = stmt.run(username, paperTypesJson, now)
      console.log('SQL 执行结果:', result)
      console.log('影响的行数:', result.changes)
      console.log('最后插入的 ID:', result.lastInsertRowid)
      
      console.log('试卷类型已保存到本地数据库，学员账号:', username)
      
      // 验证保存是否成功
      const verifyResult = this.db.prepare(`
        SELECT paper_types FROM student_papers 
        WHERE student_account = ?
      `).get(username)
      
      console.log('验证查询结果:', verifyResult)
      
      if (verifyResult && verifyResult.paper_types) {
        const savedTypes = JSON.parse(verifyResult.paper_types)
        console.log('验证保存结果，保存的试卷类型:', savedTypes)
        console.log('保存的试卷类型数量:', savedTypes.length)
        
        if (savedTypes.length === 0 && paperTypes.length > 0) {
          console.error('警告：保存的试卷类型为空，但原始数据不为空！')
          console.error('原始数据:', paperTypes)
          console.error('保存的 JSON:', paperTypesJson)
        }
      } else {
        console.error('验证失败：保存后查询不到数据')
        console.error('查询结果:', verifyResult)
      }
    } catch (error) {
      console.error('保存试卷类型到数据库失败:', error)
      console.error('错误堆栈:', error.stack)
      throw error
    }
  }

  /**
   * 获取学员适用考卷类型（返回 dictValue 列表）
   */
  /**
   * 保存当前学员档案到本地数据库（从 getInfo 接口获取的数据）
   */
  async saveCurrentStudentArchive(userInfo) {
    try {
      const archiveId = userInfo.archiveId
      const userId = userInfo.user?.userId
      const studentAccount = userInfo.studentAccount || userInfo.user?.userName
      const applicablePapers = userInfo.applicablePapers || []
      
      if (!archiveId || !studentAccount) {
        console.warn('缺少必要信息，无法保存学员档案:', { archiveId, studentAccount })
        return
      }
      
      // 确保 applicablePapers 是数组
      let applicablePapersJson = '[]'
      if (Array.isArray(applicablePapers)) {
        applicablePapersJson = JSON.stringify(applicablePapers)
      } else if (typeof applicablePapers === 'string') {
        try {
          JSON.parse(applicablePapers)
          applicablePapersJson = applicablePapers
        } catch (e) {
          const arr = applicablePapers.split(',').map(s => s.trim()).filter(s => s)
          applicablePapersJson = JSON.stringify(arr)
        }
      }
      
      console.log('保存当前学员档案到本地数据库:', {
        id: archiveId,
        user_id: userId,
        student_account: studentAccount,
        applicable_papers: applicablePapersJson
      })
      
      // 从后端获取完整的学员档案信息（包含座位号等字段）
      let fullArchive = null
      if (archiveId && userInfo.token) {
        try {
          fullArchive = await this.getStudentArchiveById(archiveId, userInfo.token)
          console.log('获取到完整学员档案:', fullArchive)
        } catch (error) {
          console.warn('获取完整学员档案失败（不影响保存）:', error)
        }
      }
      
      // 插入或更新学员档案（使用 INSERT OR REPLACE，包含座位号和学员姓名字段）
      const stmt = this.db.prepare(`
        INSERT OR REPLACE INTO student_archive 
        (id, user_id, student_account, student_name, applicable_papers, seat_number, status, del_flag, update_time)
        VALUES (?, ?, ?, ?, ?, ?, '0', '0', ?)
      `)
      
      // 从完整档案或userInfo中获取座位号和学员姓名
      const seatNumber = fullArchive?.seatNumber || fullArchive?.seat_number || userInfo.seatNumber || userInfo.seat_number || null
      const studentName = fullArchive?.studentName || fullArchive?.student_name || fullArchive?.name || 
                          userInfo.studentName || userInfo.student_name || userInfo.archive?.studentName || 
                          userInfo.archive?.student_name || userInfo.user?.nickName || userInfo.nickName || null
      
      console.log('学员姓名来源:', { 
        fullArchive: fullArchive?.studentName,
        userInfo: userInfo.studentName,
        archive: userInfo.archive?.studentName,
        nickName: userInfo.user?.nickName,
        final: studentName 
      })
      
      stmt.run(
        archiveId,
        userId || null,
        studentAccount,
        studentName,
        applicablePapersJson,
        seatNumber,
        Date.now()
      )
      
      console.log('✓ 当前学员档案已保存:', { archiveId, studentName, seatNumber })
    } catch (error) {
      console.error('保存当前学员档案失败:', error)
      throw error
    }
  }

  /**
   * 获取学员适用试卷类型（从本地数据库的学员档案表获取）
   */
  getStudentPapers(studentAccount) {
    console.log('查询学员试卷类型，学员账号:', studentAccount)
    
    // 先查询所有数据，用于调试
    const allData = this.db.prepare(`
      SELECT id, user_id, student_account, applicable_papers 
      FROM student_archive 
      WHERE del_flag = '0' AND status = '0'
    `).all()
    console.log('数据库中的所有学员档案（用于调试）:', allData.map(r => ({
      id: r.id,
      user_id: r.user_id,
      student_account: r.student_account,
      applicable_papers: r.applicable_papers
    })))
    
    // 从学员档案表获取（applicable_papers 字段）
    const result = this.db.prepare(`
      SELECT applicable_papers FROM student_archive 
      WHERE student_account = ? AND del_flag = '0' AND status = '0'
      LIMIT 1
    `).get(studentAccount)

    console.log('数据库查询结果:', result)
    console.log('查询条件 student_account:', studentAccount)

    if (result && result.applicable_papers) {
      try {
        const paperTypes = JSON.parse(result.applicable_papers)
        console.log('从数据库获取的试卷类型:', paperTypes)
        // 确保返回的是数组
        if (Array.isArray(paperTypes)) {
          console.log('返回试卷类型数组，长度:', paperTypes.length)
          return paperTypes
        } else {
          console.warn('试卷类型不是数组:', paperTypes)
          return []
        }
      } catch (e) {
        console.error('解析试卷类型失败:', e)
        console.error('原始数据:', result.applicable_papers)
        return []
      }
    }
    console.warn('未找到学员的试卷类型数据，学员账号:', studentAccount)
    return []
  }
  
  /**
   * 根据 user_id 获取学员适用试卷类型（从本地数据库的学员档案表获取）
   */
  getStudentPapersByUserId(userId) {
    console.log('查询学员试卷类型，user_id:', userId)
    console.log('user_id 类型:', typeof userId)
    
    // 确保 user_id 是数字类型（SQLite INTEGER）
    const userIdNum = typeof userId === 'string' ? parseInt(userId, 10) : userId
    console.log('转换后的 user_id:', userIdNum, '类型:', typeof userIdNum)
    
    // 先查询所有数据，用于调试
    const allData = this.db.prepare(`
      SELECT id, user_id, student_account, applicable_papers 
      FROM student_archive 
      WHERE del_flag = '0' AND status = '0'
    `).all()
    console.log('数据库中的所有学员档案（用于调试）:', allData.map(r => ({
      id: r.id,
      user_id: r.user_id,
      user_id_type: typeof r.user_id,
      student_account: r.student_account,
      applicable_papers: r.applicable_papers
    })))
    
    // 从学员档案表获取（applicable_papers 字段）
    // 尝试两种查询方式：精确匹配和类型转换匹配
    let result = this.db.prepare(`
      SELECT applicable_papers FROM student_archive 
      WHERE user_id = ? AND del_flag = '0' AND status = '0'
      LIMIT 1
    `).get(userIdNum)
    
    // 如果没找到，尝试用原始值查询
    if (!result) {
      console.log('使用数字类型查询失败，尝试使用原始值查询')
      result = this.db.prepare(`
        SELECT applicable_papers FROM student_archive 
        WHERE user_id = ? AND del_flag = '0' AND status = '0'
        LIMIT 1
      `).get(userId)
    }

    console.log('数据库查询结果:', result)
    console.log('查询条件 user_id:', userId, '类型:', typeof userId)

    if (result && result.applicable_papers) {
      try {
        const paperTypes = JSON.parse(result.applicable_papers)
        console.log('从数据库获取的试卷类型:', paperTypes)
        // 确保返回的是数组
        if (Array.isArray(paperTypes)) {
          console.log('返回试卷类型数组，长度:', paperTypes.length)
          return paperTypes
        } else {
          console.warn('试卷类型不是数组:', paperTypes)
          return []
        }
      } catch (e) {
        console.error('解析试卷类型失败:', e)
        console.error('原始数据:', result.applicable_papers)
        return []
      }
    }
    console.warn('未找到学员的试卷类型数据，user_id:', userId)
    return []
  }

  /**
   * 保存字典数据到本地数据库
   */
  async saveDictData(token) {
    try {
      console.log('开始获取字典数据')
      
      // 获取 paper_type 字典数据
      const response = await axios.get(`${API_BASE_URL}/system/dict/data/type/paper_type`, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      })

      console.log('字典数据响应:', response.data)

      if (response.data && response.data.code === 200 && response.data.data) {
        const dictDataList = response.data.data
        const now = Date.now()

        // 保存字典数据到本地数据库
        const stmt = this.db.prepare(`
          INSERT OR REPLACE INTO dict_data 
          (dict_type, dict_value, dict_label, dict_sort, css_class, list_class, is_default, status, create_time, update_time, remark)
          VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        `)

        const insertMany = this.db.transaction((items) => {
          for (const item of items) {
            stmt.run(
              'paper_type',
              item.dictValue || item.dict_value,
              item.dictLabel || item.dict_label,
              item.dictSort || item.dict_sort || 0,
              item.cssClass || item.css_class || '',
              item.listClass || item.list_class || '',
              item.isDefault || item.is_default || 'N',
              item.status || '0',
              now,
              now,
              item.remark || ''
            )
          }
        })

        insertMany(dictDataList)
        console.log(`成功保存 ${dictDataList.length} 条字典数据`)
      } else {
        console.error('获取字典数据失败，响应:', response.data)
      }
    } catch (error) {
      console.error('保存字典数据异常:', error)
      console.error('错误详情:', {
        message: error.message,
        response: error.response?.data,
        status: error.response?.status
      })
      // 字典数据获取失败不影响登录流程
    }
  }

  /**
   * 从本地数据库获取字典数据
   */
  getDictData(dictType) {
    try {
      const result = this.db.prepare(`
        SELECT dict_value, dict_label, dict_sort, css_class, list_class, is_default, status
        FROM dict_data 
        WHERE dict_type = ? AND status = '0'
        ORDER BY dict_sort ASC, dict_value ASC
      `).all(dictType)

      return result.map(item => ({
        value: item.dict_value,
        label: item.dict_label,
        sort: item.dict_sort,
        cssClass: item.css_class,
        listClass: item.list_class,
        isDefault: item.is_default,
        status: item.status
      }))
    } catch (error) {
      console.error('获取字典数据失败:', error)
      return []
    }
  }

  /**
   * 密码哈希（简单哈希，实际应该使用更安全的方式）
   */
  hashPassword(password) {
    return crypto.createHash('sha256').update(password).digest('hex')
  }
}

module.exports = LoginService


