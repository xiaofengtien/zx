const axios = require('axios')
const fs = require('fs')
const path = require('path')
const crypto = require('crypto')
const { app } = require('electron')
// 后端API地址
const API_BASE_URL = app.isPackaged ? 'http://47.94.192.7:8818/prod-api' : (process.env.API_BASE_URL || 'http://localhost:8080')

// 文件大小阈值（字节）
const BLOB_THRESHOLD = 50 * 1024 * 1024 // 50MB
const FILE_SYSTEM_THRESHOLD = 200 * 1024 * 1024 // 200MB
const FRAGMENT_SIZE = 10 * 1024 * 1024 // 10MB per fragment

// 解压缓存配置
const MAX_CACHE_SIZE = 100 * 1024 * 1024 // 100MB
const CACHE_RETENTION_DAYS = 90 // 90天

/**
 * 试卷包服务
 * 负责试卷包的下载、存储、解压和媒体文件管理
 */
class PaperService {
  constructor(db) {
    this.db = db.getDB()
    const userDataPath = app.getPath('userData')

    // 确保目录存在
    this.packageBasePath = path.join(userDataPath, 'paper_packages')
    this.mediaBasePath = path.join(userDataPath, 'media')
    this.tempBasePath = path.join(userDataPath, 'temp')

    this.ensureDirectories()

    // 解压结果缓存（LRU策略）
    this.extractCache = new Map() // Map<paperCode, { data, timestamp, size }>
    this.cacheSize = 0
  }

  /**
   * 确保必要的目录存在
   */
  ensureDirectories() {
    const dirs = [this.packageBasePath, this.mediaBasePath, this.tempBasePath]
    dirs.forEach(dir => {
      if (!fs.existsSync(dir)) {
        fs.mkdirSync(dir, { recursive: true })
        console.log(`创建目录: ${dir}`)
      }
    })
  }

  /**
   * 同步试卷包（主入口方法）
   * @param {string} paperCode - 试卷编码
   * @param {string} token - 认证token
   * @param {Function} onProgress - 进度回调 (progress: number) => void
   * @returns {Promise<Object>} 同步结果
   */
  /**
   * 同步试卷包
   * @param {string|Object} paperCodeOrPaper - 试卷编码或试卷对象（如果传入对象，则直接使用，不再重新获取）
   * @param {string} token - 认证token
   * @param {Function} onProgress - 进度回调
   * @returns {Promise<Object>} 同步结果
   */
  /**
   * 同步快速启动包（仅下载快速启动包，不解压完整包）
   * @param {string|Object} paperCodeOrPaper - 试卷编码或试卷对象
   * @param {string} token - 认证token
   * @param {Function} onProgress - 进度回调函数
   * @returns {Promise<Object>} 同步结果
   */
  async syncQuickStartPackageOnly(paperCodeOrPaper, token, onProgress = null) {
    try {
      // 判断参数类型：如果是对象，直接使用；如果是字符串，需要获取试卷信息
      let paper
      let paperCode

      if (typeof paperCodeOrPaper === 'object' && paperCodeOrPaper !== null) {
        paper = paperCodeOrPaper
        paperCode = paper.paperCode || paper.paper_code
      } else {
        paperCode = paperCodeOrPaper
        // 获取试卷信息
        paper = await this.getPaperInfo(paperCode, token, true)
        if (!paper) {
          throw new Error(`无法获取试卷信息：${paperCode}`)
        }
      }

      if (!paperCode) {
        paperCode = paper.paperCode || paper.paper_code
        if (!paperCode) {
          throw new Error('无法确定试卷编码')
        }
      }

      // 确保本地paper表中有该试卷记录
      const localPaper = this.db.prepare(`
        SELECT id FROM paper WHERE paper_code = ? LIMIT 1
      `).get(paperCode)

      if (!localPaper && paper && paper.id) {
        try {
          this.db.prepare(`
            INSERT INTO paper 
            (id, paper_name, paper_code, paper_type, paper_desc,
             total_score, total_questions, duration,
             version, package_hash, package_size, last_package_time,
             status, create_time)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
          `).run(
            paper.id,
            paper.paperName || paper.paper_name || paperCode,
            paperCode,
            paper.paperType || paper.paper_type || null,
            paper.paperDesc || paper.paper_desc || null,
            paper.totalScore || paper.total_score || 0,
            paper.totalQuestions || paper.total_questions || 0,
            paper.duration || null,
            paper.version || 1,
            paper.packageHash || paper.package_hash || null,
            paper.packageSize || paper.package_size || null,
            paper.lastPackageTime || paper.last_package_time ? new Date(paper.lastPackageTime || paper.last_package_time).getTime() : null,
            1,
            Date.now()
          )
        } catch (error) {
          console.warn(`插入paper表记录失败: ${error.message}`)
        }
      }

      // 下载快速启动包
      console.log(`开始下载快速启动包: ${paperCode}, 版本: v${paper.version}`)
      let quickStartResult
      try {
        quickStartResult = await this.downloadQuickStartPackage(
          paper.id,
          paperCode,
          paper.version,
          token,
          (progress) => {
            if (onProgress) onProgress(Math.floor(progress))
          }
        )

        if (quickStartResult && quickStartResult.success) {
          // 解压快速启动包
          console.log(`开始解压快速启动包: ${paperCode}, 版本: v${paper.version}`)
          try {
            await this.extractQuickStartPackage(paperCode, paper.version)
            // 解压完成后，立即更新状态为"已就绪"
            this.updateDownloadStatus(paper.id, paperCode, 'ready', 100, 0, 0)
            console.log(`✓ 快速启动包解压完成: ${paperCode}`)
          } catch (extractError) {
            console.warn(`快速启动包解压失败: ${extractError.message}，尝试重新下载...`)
            const quickStartFileName = `${paperCode}_v${paper.version}_quick.zip`
            const quickStartPath = path.join(this.packageBasePath, quickStartFileName)
            if (fs.existsSync(quickStartPath)) {
              fs.unlinkSync(quickStartPath)
            }
            quickStartResult = await this.downloadQuickStartPackage(
              paper.id,
              paperCode,
              paper.version,
              token,
              (progress) => {
                if (onProgress) onProgress(Math.floor(progress))
              }
            )
            if (quickStartResult && quickStartResult.success) {
              await this.extractQuickStartPackage(paperCode, paper.version)
              // 解压完成后，立即更新状态为"已就绪"
              this.updateDownloadStatus(paper.id, paperCode, 'ready', 100, 0, 0)
              console.log(`✓ 重新下载并解压快速启动包完成: ${paperCode}`)
            }
          }
        }
      } catch (error) {
        console.warn(`快速启动包下载失败: ${error.message}`)
        quickStartResult = { success: false, message: error.message }
      }

      return {
        success: quickStartResult?.success || false,
        message: quickStartResult?.message || '快速启动包同步完成',
        paperCode,
        version: paper.version,
        quickStartResult,
        quickStartDownloaded: quickStartResult?.success || false
      }
    } catch (error) {
      console.error(`同步快速启动包失败: ${paperCode}`, error)
      return {
        success: false,
        message: error.message || '同步失败',
        paperCode
      }
    }
  }

  async syncPaperPackage(paperCodeOrPaper, token, onProgress = null) {
    try {
      // 判断参数类型：如果是对象，直接使用；如果是字符串，需要获取试卷信息
      let paper
      let paperCode

      if (typeof paperCodeOrPaper === 'object' && paperCodeOrPaper !== null) {
        // 传入的是试卷对象，直接使用
        paper = paperCodeOrPaper
        paperCode = paper.paperCode || paper.paper_code
        console.log(`✓ 开始同步试卷包: ${paperCode} (使用传入的试卷对象)`)
        console.log(`✓ 试卷对象详情:`, JSON.stringify({
          id: paper.id,
          paperCode: paper.paperCode || paper.paper_code,
          paperName: paper.paperName || paper.paper_name,
          version: paper.version,
          packageHash: paper.packageHash || paper.package_hash,
          packageSize: paper.packageSize || paper.package_size
        }, null, 2))

        // 验证必要字段
        if (!paperCode) {
          throw new Error('传入的试卷对象缺少 paperCode 字段')
        }
        if (!paper.id) {
          throw new Error('传入的试卷对象缺少 id 字段')
        }
        if (paper.version === null || paper.version === undefined) {
          console.warn(`⚠️ 传入的试卷对象 version 为 null/undefined，将使用默认值 1`)
          paper.version = 1
        }
      } else {
        // 传入的是试卷编码，需要获取试卷信息
        paperCode = paperCodeOrPaper
        console.log(`开始同步试卷包: ${paperCode}`)

        // 1. 获取试卷信息（包含package_hash、version等）
        // 策略：优先从服务器获取最新信息，如果失败则降级到本地缓存（离线模式）
        try {
          paper = await this.getPaperInfo(paperCode, token, true) // 强制刷新，获取最新信息
          if (!paper) {
            throw new Error(`无法获取试卷信息：服务器返回的试卷列表中未找到试卷编码 ${paperCode}`)
          }
          console.log(`从服务器获取到的试卷信息: 版本=${paper.version}, hash=${paper.packageHash || '无'}`)
        } catch (error) {
          // 如果服务器请求失败（可能是离线），降级到本地缓存
          if (error.message.includes('网络') || error.message.includes('ECONNREFUSED') ||
            error.message.includes('timeout') || error.message.includes('ENOTFOUND') ||
            error.code === 'ECONNREFUSED' || error.code === 'ENOTFOUND' || error.code === 'ETIMEDOUT') {
            console.warn(`⚠️ 检测到离线状态，降级到使用本地缓存: ${error.message}`)

            // 检查本地是否有可用的完整包
            const localPackage = await this.getLocalPackage(paperCode)
            if (localPackage) {
              console.log(`✓ 离线模式：找到本地完整包 v${localPackage.version}，直接使用`)
              if (onProgress) onProgress(100)
              return {
                success: true,
                message: '离线模式：使用本地试卷包',
                paperCode,
                version: localPackage.version,
                isOfflineMode: true,
                quickStartDownloaded: false
              }
            } else {
              // 本地也没有完整包，尝试获取本地缓存的试卷信息
              paper = await this.getPaperInfo(paperCode, token, false) // 使用本地缓存
              if (paper) {
                console.log(`使用本地缓存的试卷信息: 版本=${paper.version}, hash=${paper.packageHash || '无'}`)
                console.warn(`⚠️ 注意：离线模式下使用的是本地缓存的hash，可能与服务器不一致`)
              } else {
                throw new Error(`离线模式下未找到本地试卷包: ${paperCode}`)
              }
            }
          } else {
            // 其他错误（如认证失败），直接抛出
            throw error
          }
        }

        if (!paper) {
          throw new Error(`试卷不存在: ${paperCode}`)
        }
      }

      // 确保 paperCode 存在
      if (!paperCode) {
        paperCode = paper.paperCode || paper.paper_code
        if (!paperCode) {
          throw new Error('无法确定试卷编码')
        }
      }

      // 2. 确保本地paper表中有该试卷记录（避免paper_package.paper_id为null的错误）
      // 注意：getPaperInfo返回的paper对象可能包含id（从本地查询时）或不包含id（从服务器查询时）
      // 所以需要检查本地paper表，如果没有记录则尝试插入
      const localPaper = this.db.prepare(`
        SELECT id FROM paper WHERE paper_code = ? LIMIT 1
      `).get(paperCode)

      if (!localPaper) {
        // 本地没有该试卷记录，需要插入
        console.log(`本地paper表中没有试卷记录，尝试插入新记录: ${paperCode}`)

        // 如果paper对象有id（从本地查询返回），直接使用
        if (paper && paper.id) {
          try {
            this.db.prepare(`
              INSERT INTO paper 
              (id, paper_name, paper_code, paper_type, paper_desc,
               total_score, total_questions, duration,
               version, package_hash, package_size, last_package_time,
               status, create_time)
              VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            `).run(
              paper.id,
              paper.paperName || paperCode,
              paperCode,
              null, // paper_type
              null, // paper_desc
              0, // total_score
              0, // total_questions
              null, // duration
              paper.version || 1,
              paper.packageHash || null,
              paper.packageSize || null,
              null, // last_package_time
              1, // status = 1
              Date.now()
            )
            console.log(`✓ 已插入paper表记录: ${paperCode} (ID: ${paper.id})`)
          } catch (error) {
            console.warn(`插入paper表记录失败: ${error.message}`)
          }
        } else {
          // 如果paper对象没有id，从服务器获取完整试卷信息（包含id）
          try {
            const response = await axios.post(
              `${API_BASE_URL}/student/sync/paper/list`,
              {
                pageNum: 1,
                pageSize: 100,
                status: 1
              },
              {
                headers: {
                  'Content-Type': 'application/json',
                  'Authorization': `Bearer ${token}`
                }
              }
            )

            if (response.data && response.data.code === 200 && response.data.rows) {
              const fullPaper = response.data.rows.find(p =>
                (p.paperCode || p.paper_code) === paperCode
              )

              if (fullPaper && fullPaper.id) {
                // 插入paper表记录
                this.db.prepare(`
                  INSERT INTO paper 
                  (id, paper_name, paper_code, paper_type, paper_desc,
                   total_score, total_questions, duration,
                   version, package_hash, package_size, last_package_time,
                   status, create_time)
                  VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                `).run(
                  fullPaper.id,
                  fullPaper.paperName || fullPaper.paper_name || paperCode,
                  paperCode,
                  fullPaper.paperType || fullPaper.paper_type || null,
                  fullPaper.paperDesc || fullPaper.paper_desc || null,
                  fullPaper.totalScore || fullPaper.total_score || 0,
                  fullPaper.totalQuestions || fullPaper.total_questions || 0,
                  fullPaper.duration || null,
                  fullPaper.version || 1,
                  fullPaper.packageHash || fullPaper.package_hash || null,
                  fullPaper.packageSize || fullPaper.package_size || null,
                  fullPaper.lastPackageTime || fullPaper.last_package_time ? new Date(fullPaper.lastPackageTime || fullPaper.last_package_time).getTime() : null,
                  1, // status = 1 (启用)
                  Date.now()
                )
                console.log(`✓ 已插入paper表记录: ${paperCode} (ID: ${fullPaper.id})`)
              } else {
                console.warn(`⚠️ 从服务器获取的试卷列表中未找到 ${paperCode}，可能无法插入paper表记录`)
                // 如果找不到，依赖syncZipPackages中已经插入的记录
                // 如果syncZipPackages也没有插入，会在下载时抛出错误
              }
            }
          } catch (error) {
            console.warn(`获取完整试卷信息失败: ${error.message}`)
            // 如果获取失败，依赖syncZipPackages中已经插入的记录
            // 如果syncZipPackages也没有插入，会在下载时抛出错误
          }
        }
      }

      // 3. 检查是否有packageHash
      // 注意：即使packageHash为空，也尝试下载（可能数据库字段未更新，但OSS上已有文件）
      if (!paper.packageHash) {
        console.warn(`试卷 ${paperCode} 没有packageHash，将尝试下载（可能数据库字段未更新）`)
        // 不返回错误，继续尝试下载
      }

      // 4. 先检查快速启动包是否已存在，如果不存在才下载
      // 注意：快速启动包使用当前试卷的版本号（后端已更新为新版本号）
      // 逻辑：联网状态优先下载，然后对比本地，如果版本一致则不更新
      let quickStartResult
      const quickStartExists = await this.checkQuickStartPackageExists(paper.id)

      if (quickStartExists) {
        console.log(`快速启动包已存在，跳过下载: ${paperCode}, 版本: v${paper.version}`)
        // 确保快速启动包已解压
        try {
          await this.extractQuickStartPackage(paperCode, paper.version)
          console.log(`✓ 快速启动包已解压: ${paperCode}`)
          quickStartResult = {
            success: true,
            message: '快速启动包已存在',
            paperCode,
            version: paper.version
          }
        } catch (extractError) {
          console.warn(`快速启动包解压失败: ${extractError.message}，尝试重新下载...`)
          // 如果解压失败，可能是文件损坏，删除并重新下载
          const quickStartFileName = `${paperCode}_v${paper.version}_quick.zip`
          const quickStartPath = path.join(this.packageBasePath, quickStartFileName)
          if (fs.existsSync(quickStartPath)) {
            fs.unlinkSync(quickStartPath)
            console.log(`已删除损坏的快速启动包: ${quickStartFileName}`)
          }
          // 重新下载
          quickStartResult = await this.downloadQuickStartPackage(
            paper.id,
            paperCode,
            paper.version,
            token,
            (progress) => {
              if (onProgress) onProgress(Math.floor(progress * 0.5))
            }
          )
          if (quickStartResult && quickStartResult.success) {
            await this.extractQuickStartPackage(paperCode, paper.version)
            console.log(`✓ 重新下载并解压快速启动包完成: ${paperCode}`)
          } else {
            // 重新下载失败，设置失败状态
            quickStartResult = { success: false, message: extractError.message }
          }
        }
      } else {
        console.log(`开始下载快速启动包: ${paperCode}, 版本: v${paper.version}`)
        try {
          quickStartResult = await this.downloadQuickStartPackage(
            paper.id,
            paperCode,
            paper.version, // 使用服务器返回的最新版本号
            token,
            (progress) => {
              // 快速启动包下载进度（0-50%）
              if (onProgress) onProgress(Math.floor(progress * 0.5))
            }
          )

          if (quickStartResult && quickStartResult.success) {
            // 解压快速启动包（只解压manifest.json和trial_listen/、intro/等）
            // 无论快速启动包是新下载的还是已存在的，都需要确保已解压
            console.log(`开始解压快速启动包: ${paperCode}, 版本: v${paper.version}`)
            try {
              await this.extractQuickStartPackage(paperCode, paper.version)
              // 解压完成后，立即更新状态为"已就绪"
              this.updateDownloadStatus(paper.id, paperCode, 'ready', 100, 0, 0)
              console.log(`✓ 快速启动包解压完成: ${paperCode}`)
            } catch (extractError) {
              console.warn(`快速启动包解压失败: ${extractError.message}，尝试重新下载...`)
              // 如果解压失败，可能是文件损坏，尝试重新下载
              // 删除已存在的文件，重新下载
              const quickStartFileName = `${paperCode}_v${paper.version}_quick.zip`
              const quickStartPath = path.join(this.packageBasePath, quickStartFileName)
              if (fs.existsSync(quickStartPath)) {
                fs.unlinkSync(quickStartPath)
                console.log(`已删除损坏的快速启动包: ${quickStartFileName}`)
              }
              // 重新下载
              quickStartResult = await this.downloadQuickStartPackage(
                paper.id,
                paperCode,
                paper.version,
                token,
                (progress) => {
                  if (onProgress) onProgress(Math.floor(progress * 0.5))
                }
              )
              if (quickStartResult && quickStartResult.success) {
                await this.extractQuickStartPackage(paperCode, paper.version)
                // 解压完成后，立即更新状态为"已就绪"
                this.updateDownloadStatus(paper.id, paperCode, 'ready', 100, 0, 0)
                console.log(`✓ 重新下载并解压快速启动包完成: ${paperCode}`)
              }
            }
          }
        } catch (error) {
          console.warn(`快速启动包下载失败，继续下载完整包: ${error.message}`)
          // 快速启动包下载失败不影响完整包下载，继续执行
          quickStartResult = { success: false, message: error.message }
        }
      }

      // 5. 检查本地是否已有完整ZIP包（智能版本检查）
      const localPackage = await this.getLocalPackage(paperCode)

      if (localPackage) {
        const localVersion = localPackage.version || 0
        const remoteVersion = paper.version || 0
        const localHash = localPackage.package_hash
        const remoteHash = paper.packageHash

        console.log(`[syncPaperPackage] 本地包信息: v${localVersion}, hash=${localHash}`)
        console.log(`[syncPaperPackage] 远程包信息: v${remoteVersion}, hash=${remoteHash}`)

        // 检查文件是否真的存在
        const packageFileName = `${paperCode}_v${localVersion}.zip`
        const packagePath = path.join(this.packageBasePath, packageFileName)
        const fileExists = fs.existsSync(packagePath)

        if (!fileExists) {
          console.warn(`[syncPaperPackage] ⚠️ 数据库有记录但文件不存在，删除记录并重新下载: ${packagePath}`)
          // 删除数据库记录
          this.db.prepare(`DELETE FROM paper_package WHERE paper_code = ? AND version = ?`).run(paperCode, localVersion)
          // 继续下载流程
        } else if (localHash === remoteHash && localVersion === remoteVersion) {
          // 文件存在且hash和版本都一致，跳过下载
          console.log(`[syncPaperPackage] ✓ 完整ZIP包已存在且hash一致，跳过下载: ${paperCode} v${localVersion}`)
          if (onProgress) onProgress(100)
          return {
            success: true,
            message: 'ZIP包已存在且hash一致',
            paperCode,
            version: paper.version,
            quickStartDownloaded: quickStartResult?.success || false
          }
        } else if (localVersion === remoteVersion && localHash !== remoteHash) {
          // 版本号相同但hash不同，说明文件已更新，需要重新下载
          console.warn(`[syncPaperPackage] ⚠️ 版本号相同(v${localVersion})但hash不同，需要重新下载`)
          console.warn(`   本地hash: ${localHash}`)
          console.warn(`   远程hash: ${remoteHash}`)
          // 删除旧文件和记录
          try {
            fs.unlinkSync(packagePath)
            this.db.prepare(`DELETE FROM paper_package WHERE paper_code = ? AND version = ?`).run(paperCode, localVersion)
            console.log(`[syncPaperPackage] 已删除旧文件和记录，准备重新下载`)
          } catch (error) {
            console.error(`删除旧文件失败:`, error)
          }
          // 继续下载流程
        } else if (localVersion > remoteVersion) {
          // 本地版本高于服务器版本，优先使用本地版本（支持线下手动发配试卷）
          console.log(`[syncPaperPackage] ✓ 本地版本(v${localVersion})高于服务器版本(v${remoteVersion})，优先使用本地ZIP包，跳过下载: ${paperCode}`)
          if (onProgress) onProgress(100)
          return {
            success: true,
            message: '本地版本高于服务器版本，使用本地ZIP包',
            paperCode,
            version: localVersion,
            isLocalVersion: true,
            quickStartDownloaded: quickStartResult?.success || false
          }
        } else {
          // 本地版本低于远程版本，需要更新
          console.log(`[syncPaperPackage] 检测到新版本，需要更新: 本地v${localVersion} -> 远程v${remoteVersion}`)

          // 注意：不要在这里删除旧版本，等新版本下载成功后再删除
          // 这样可以确保在新版本下载失败时，旧版本仍然可用
          console.log(`[syncPaperPackage] ⚠️ 暂不删除旧版本v${localVersion}，等v${remoteVersion}下载成功后再删除`)
        }
      } else {
        console.log(`[syncPaperPackage] 本地没有完整包，需要下载: ${paperCode} v${paper.version}`)
      }

      // 6. 后台下载完整ZIP包（带重试机制，处理hash不匹配问题）
      // 注意：如果只需要快速启动包（onlyQuickStart=true），则跳过完整包下载
      console.log(`开始后台下载完整ZIP包: ${paperCode}`)
      let downloadResult
      let retryCount = 0
      const maxRetries = 2 // 最多重试2次

      // 后台下载，不阻塞
      const downloadFullPackage = async () => {
        while (retryCount <= maxRetries) {
          try {
            // 每次重试前，重新获取试卷信息（后端可能已经重新生成了）
            if (retryCount > 0) {
              console.log(`第 ${retryCount} 次重试，重新获取试卷信息...`)
              // 等待3秒，给后端时间重新生成
              await new Promise(resolve => setTimeout(resolve, 3000))

              // 强制从服务器获取最新信息（不使用本地缓存）
              const updatedPaper = await this.getPaperInfo(paperCode, token, true)
              if (updatedPaper) {
                const oldVersion = paper.version
                const oldHash = paper.packageHash
                paper = updatedPaper
                console.log(`重新获取的试卷信息:`)
                console.log(`  版本: ${oldVersion} -> ${paper.version}`)
                console.log(`  hash: ${oldHash || '无'} -> ${paper.packageHash || '无'}`)

                // 如果版本或hash已更新，说明后端已经重新生成
                if (paper.version !== oldVersion || paper.packageHash !== oldHash) {
                  console.log(`✓ 检测到试卷包已更新，使用新的hash进行验证`)
                } else {
                  console.warn(`⚠️ 试卷信息未更新，可能后端还未完成重新生成`)
                }
              } else {
                console.warn(`重新获取试卷信息失败，使用旧的试卷信息重试`)
              }
            }

            downloadResult = await this.downloadPaperPackage(
              paper.id,
              paperCode,
              paper.version,
              paper.packageHash || null, // 如果没有hash，传null（下载后计算）
              token,
              (progress) => {
                // 完整包下载进度（50-100%）
                if (onProgress) onProgress(50 + Math.floor(progress * 0.5))
              }
            )

            if (downloadResult.success) {
              // 解压完整ZIP包
              console.log(`开始解压完整ZIP包: ${paperCode}`)
              await this.extractPaperPackage(paperCode, paper.version)

              // 验证完整包是否已保存到 paper_package 表
              const verifyPackage = this.db.prepare(`
                SELECT paper_code, version, is_active FROM paper_package 
                WHERE paper_code = ? AND is_active = 1
                ORDER BY version DESC
                LIMIT 1
              `).get(paperCode)

              if (verifyPackage) {
                console.log(`✓ 验证：完整包已保存到 paper_package 表，paper_code=${verifyPackage.paper_code}, version=${verifyPackage.version}`)
              } else {
                console.warn(`⚠️ 警告：完整包下载成功但未在 paper_package 表中找到，paper_code=${paperCode}`)
                console.warn(`   这可能是因为保存逻辑出错，请检查 downloadPaperPackage 方法的实现`)
              }

              // 新版本下载成功，删除所有旧版本的完整包
              await this.deleteAllOldVersions(paperCode, paper.version)

              break // 下载成功，退出循环
            }

            // 如果是hash验证失败或文件太小，且还有重试次数，继续重试
            if (downloadResult.message &&
              (downloadResult.message.includes('hash验证失败') || downloadResult.message.includes('文件太小')) &&
              retryCount < maxRetries) {
              console.warn(`下载失败: ${downloadResult.message}`)
              console.warn(`等待后端重新生成后重试（第 ${retryCount + 1} 次）...`)
              retryCount++
              continue
            }

            throw new Error(downloadResult.message || '下载失败')
          } catch (error) {
            // 如果是hash验证失败或文件太小，且还有重试次数，继续重试
            if (error.message &&
              (error.message.includes('hash验证失败') || error.message.includes('文件太小')) &&
              retryCount < maxRetries) {
              console.warn(`下载失败: ${error.message}`)
              console.warn(`等待后端重新生成后重试（第 ${retryCount + 1} 次）...`)
              retryCount++
              continue
            }
            throw error
          }
        }
      }

      // 启动后台下载（不等待完成）
      downloadFullPackage()
        .then(() => {
          console.log(`✓ 完整ZIP包后台下载完成: ${paperCode}`)
        })
        .catch(error => {
          console.error(`后台下载完整ZIP包失败: ${error.message}`)
          // 更新下载状态为失败
          this.updateDownloadStatus(paper.id, paperCode, 'error', 0, 0, 0, error.message)
        })

      return {
        success: true,
        message: '快速启动包已下载，完整包正在后台下载',
        paperCode,
        version: paper.version,
        quickStartResult,
        quickStartDownloaded: quickStartResult?.success || false, // 快速启动包是否已下载（兼容前端判断）
        fullPackageDownloading: true // 标记完整包正在后台下载
      }
    } catch (error) {
      console.error(`同步试卷包失败: ${paperCode}`, error)
      return {
        success: false,
        message: error.message || '同步失败',
        paperCode
      }
    }
  }

  /**
   * 获取试卷信息
   * @param {string} paperCode - 试卷编码
   * @param {string} token - 认证token
   * @returns {Promise<Object>} 试卷信息
   */
  async getPaperInfo(paperCode, token, forceRefresh = false) {
    try {
      // 如果强制刷新，直接从服务器获取（不使用本地缓存）
      if (!forceRefresh) {
        // 先从本地SQLite查询
        const localPaper = this.db.prepare(`
          SELECT id, paper_code, paper_name, version, package_hash, package_size
          FROM paper
          WHERE paper_code = ? AND status = 1
        `).get(paperCode)

        if (localPaper && localPaper.package_hash) {
          return {
            id: localPaper.id,
            paperCode: localPaper.paper_code,
            paperName: localPaper.paper_name,
            version: localPaper.version || 1,
            packageHash: localPaper.package_hash,
            packageSize: localPaper.package_size
          }
        }
      }

      // 从服务器获取试卷列表并查找（总是获取最新信息）
      console.log(`从服务器获取试卷信息: ${paperCode}${forceRefresh ? ' (强制刷新)' : ''}`)

      // 注意：这里应该使用 /student/sync/paper/listByIds 接口，但我们需要先知道试卷ID
      // 由于我们只有 paperCode，需要先通过 /student/sync/paper/list 接口查询
      // 或者，我们可以直接从 syncZipPackages 传入的 papers 列表中查找
      // 但为了保持接口的独立性，这里仍然使用 /student/sync/paper/list 接口
      const response = await axios.post(
        `${API_BASE_URL}/student/sync/paper/list`,
        {
          pageNum: 1,
          pageSize: 100,
          status: 1
        },
        {
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
          }
        }
      )

      console.log(`服务器响应:`, JSON.stringify(response.data, null, 2))

      // 处理不同的响应格式：可能是 rows 或 data
      let paperList = null
      if (response.data && response.data.code === 200) {
        if (response.data.rows) {
          paperList = response.data.rows
        } else if (response.data.data) {
          paperList = Array.isArray(response.data.data) ? response.data.data : [response.data.data]
        } else if (Array.isArray(response.data)) {
          paperList = response.data
        }
      }

      if (paperList && paperList.length > 0) {
        console.log(`服务器返回的试卷列表数量: ${paperList.length}`)
        console.log(`试卷列表中的编码:`, paperList.map(p => p.paperCode || p.paper_code || '无').join(', '))

        // 从试卷列表中查找匹配的paperCode
        const paper = paperList.find(p =>
          (p.paperCode || p.paper_code) === paperCode
        )

        if (paper) {
          const paperInfo = {
            id: paper.id,
            paperCode: paper.paperCode || paper.paper_code,
            paperName: paper.paperName || paper.paper_name,
            version: paper.version || 1,
            packageHash: paper.packageHash || paper.package_hash,
            packageSize: paper.packageSize || paper.package_size
          }

          console.log(`从服务器获取的试卷信息: 版本=${paperInfo.version}, hash=${paperInfo.packageHash || '无'}`)

          // 更新本地数据库（如果本地有记录）
          const localPaper = this.db.prepare(`
            SELECT id FROM paper WHERE paper_code = ? AND status = 1
          `).get(paperCode)

          if (localPaper) {
            this.db.prepare(`
              UPDATE paper 
              SET version = ?, package_hash = ?, package_size = ?
              WHERE paper_code = ?
            `).run(
              paperInfo.version,
              paperInfo.packageHash,
              paperInfo.packageSize,
              paperCode
            )
            console.log(`已更新本地数据库中的试卷信息`)
          }

          return paperInfo
        } else {
          // 未找到匹配的试卷
          console.warn(`⚠️ 服务器返回的试卷列表中未找到试卷编码: ${paperCode}`)
          console.warn(`   服务器返回的试卷列表数量: ${response.data.rows.length}`)
          console.warn(`   试卷列表中的编码:`, response.data.rows.map(p => p.paperCode || p.paper_code).join(', '))
        }
      } else {
        console.warn(`⚠️ 服务器响应异常:`, response.data)
      }

      return null
    } catch (error) {
      // 如果强制刷新失败（可能是网络错误），降级到使用本地信息
      if (forceRefresh) {
        console.warn(`从服务器获取试卷信息失败: ${error.message}`)
        console.warn(`降级到使用本地SQLite中的试卷信息（离线模式）`)

        // 尝试从本地SQLite获取
        const localPaper = this.db.prepare(`
          SELECT id, paper_code, paper_name, version, package_hash, package_size
          FROM paper
          WHERE paper_code = ? AND status = 1
        `).get(paperCode)

        if (localPaper && localPaper.package_hash) {
          console.log(`使用本地试卷信息: 版本=${localPaper.version}, hash=${localPaper.package_hash}`)
          return {
            id: localPaper.id,
            paperCode: localPaper.paper_code,
            paperName: localPaper.paper_name,
            version: localPaper.version || 1,
            packageHash: localPaper.package_hash,
            packageSize: localPaper.package_size
          }
        } else {
          // 本地也没有，抛出错误
          console.error('本地也没有试卷信息，无法继续')
          throw new Error(`无法获取试卷信息（服务器请求失败，且本地无缓存）: ${error.message}`)
        }
      } else {
        // 非强制刷新模式，直接抛出错误
        console.error('获取试卷信息失败:', error.message)
        throw error
      }
    }
  }

  /**
   * 获取本地ZIP包信息
   * @param {string} paperCode - 试卷编码
   * @returns {Promise<Object|null>} 本地ZIP包信息
   */
  async getLocalPackage(paperCode) {
    try {
      const packageInfo = this.db.prepare(`
        SELECT paper_code, package_hash, package_size, storage_type, 
               package_path, version, sync_time, is_active
        FROM paper_package
        WHERE paper_code = ? AND is_active = 1
        ORDER BY version DESC
        LIMIT 1
      `).get(paperCode)

      return packageInfo || null
    } catch (error) {
      console.error('获取本地ZIP包信息失败:', error.message)
      return null
    }
  }

  /**
   * 下载快速启动包（包含manifest.json、trial_listen/、intro/等）
   * @param {number} paperId - 试卷ID
   * @param {string} paperCode - 试卷编码
   * @param {number} version - 版本号
   * @param {string} token - 认证token
   * @param {Function} onProgress - 进度回调函数 (progress) => {}
   * @returns {Promise<Object>} 下载结果
   */
  async downloadQuickStartPackage(paperId, paperCode, version, token, onProgress = null) {
    try {
      const quickStartFileName = `${paperCode}_v${version}_quick.zip`
      const quickStartPath = path.join(this.packageBasePath, quickStartFileName)

      // 检查本地是否已有快速启动包
      if (fs.existsSync(quickStartPath)) {
        const stats = fs.statSync(quickStartPath)
        if (stats.size > 0) {
          console.log(`快速启动包已存在，跳过下载: ${quickStartFileName}`)

          // 确保快速启动包已保存到 paper_package 表
          const quickPaperCode = `${paperCode}_quick`
          const relativePath = path.relative(app.getPath('userData'), quickStartPath)
          const fileBuffer = fs.readFileSync(quickStartPath)
          const quickHash = this.calculateHash(fileBuffer)
          const syncTime = Date.now()

          try {
            // 先禁用旧版本的记录（如果有）
            this.db.prepare(`
              UPDATE paper_package
              SET is_active = 0
              WHERE paper_id = ? AND paper_code = ? AND is_active = 1
            `).run(paperId, quickPaperCode)

            // 检查是否已存在相同版本的记录
            const existingRecord = this.db.prepare(`
              SELECT id FROM paper_package
              WHERE paper_id = ? AND paper_code = ? AND version = ?
            `).get(paperId, quickPaperCode, version)

            if (existingRecord) {
              // 更新现有记录
              this.db.prepare(`
                UPDATE paper_package
                SET package_path = ?, package_hash = ?, package_size = ?,
                    storage_type = 1, sync_time = ?, is_active = 1
                WHERE id = ?
              `).run(
                relativePath,
                quickHash,
                fileBuffer.length,
                syncTime,
                existingRecord.id
              )
              console.log(`✓ 已更新快速启动包到 paper_package 表: paper_id=${paperId}, paper_code=${quickPaperCode}, version=${version}`)
            } else {
              // 插入新记录
              try {
                this.db.prepare(`
                  INSERT INTO paper_package 
                  (paper_id, paper_code, package_path, package_hash, package_size, 
                   storage_type, version, sync_time, is_active)
                  VALUES (?, ?, ?, ?, ?, 1, ?, ?, 1)
                `).run(
                  paperId,
                  quickPaperCode,
                  relativePath,
                  quickHash,
                  fileBuffer.length,
                  version,
                  syncTime
                )
                console.log(`✓ 已插入快速启动包到 paper_package 表: paper_id=${paperId}, paper_code=${quickPaperCode}, version=${version}`)
              } catch (insertError) {
                // 如果插入失败（可能是 UNIQUE 约束），先删除旧记录再插入
                if (insertError.message && insertError.message.includes('UNIQUE')) {
                  console.warn(`插入失败（UNIQUE约束），先删除旧记录: ${insertError.message}`)
                  this.db.prepare(`
                    DELETE FROM paper_package
                    WHERE paper_id = ? AND paper_code = ?
                  `).run(paperId, quickPaperCode)

                  // 重新插入
                  this.db.prepare(`
                    INSERT INTO paper_package 
                    (paper_id, paper_code, package_path, package_hash, package_size, 
                     storage_type, version, sync_time, is_active)
                    VALUES (?, ?, ?, ?, ?, 1, ?, ?, 1)
                  `).run(
                    paperId,
                    quickPaperCode,
                    relativePath,
                    quickHash,
                    fileBuffer.length,
                    version,
                    syncTime
                  )
                  console.log(`✓ 已插入快速启动包到 paper_package 表（删除旧记录后）: paper_id=${paperId}, paper_code=${quickPaperCode}, version=${version}`)
                } else {
                  throw insertError
                }
              }
            }
          } catch (error) {
            console.error(`保存快速启动包到 paper_package 表失败:`, error)
            console.error(`paper_id=${paperId}, paper_code=${quickPaperCode}, version=${version}`)
            // 不抛出错误，因为文件已存在，只是数据库记录失败
          }

          if (onProgress) onProgress(100)
          return {
            success: true,
            message: '快速启动包已存在',
            paperCode,
            version,
            path: quickStartPath
          }
        }
      }

      // 从服务器下载快速启动包（使用 onDownloadProgress 实时更新进度）
      let totalSize = 0
      let downloadedSize = 0
      const response = await axios.post(
        `${API_BASE_URL}/student/sync/paper/package/downloadQuick`,
        { id: paperId },
        {
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
          },
          responseType: 'arraybuffer',
          timeout: 60000, // 1分钟超时（快速启动包通常很小）
          validateStatus: (status) => status < 500,
          onDownloadProgress: (progressEvent) => {
            // 实时更新下载进度
            if (progressEvent.total) {
              totalSize = progressEvent.total
              downloadedSize = progressEvent.loaded
              const progress = Math.floor((downloadedSize / totalSize) * 100)

              // 更新数据库中的下载状态
              this.updateDownloadStatus(paperId, paperCode, 'downloading', progress, downloadedSize, totalSize)

              // 调用进度回调
              if (onProgress) {
                onProgress(progress)
              }

              // 每10%或每1MB更新一次日志
              if (progress % 10 === 0 || downloadedSize % (1024 * 1024) < 100 * 1024) {
                console.log(`快速启动包 ${paperCode} 下载进度: ${progress}% (${(downloadedSize / 1024 / 1024).toFixed(2)}MB / ${(totalSize / 1024 / 1024).toFixed(2)}MB)`)
              }
            }
          }
        }
      )

      if (response.status !== 200) {
        throw new Error(`下载快速启动包失败，状态码: ${response.status}`)
      }

      // 如果 totalSize 为 0，说明 onDownloadProgress 没有触发，使用 response.data 的长度
      if (totalSize === 0) {
        totalSize = response.data.byteLength
      }
      console.log(`快速启动包大小: ${(totalSize / 1024 / 1024).toFixed(2)}MB (${totalSize} 字节)`)

      // 保存到文件系统
      if (!fs.existsSync(this.packageBasePath)) {
        fs.mkdirSync(this.packageBasePath, { recursive: true })
      }

      fs.writeFileSync(quickStartPath, Buffer.from(response.data))
      console.log(`快速启动包已保存: ${quickStartPath}`)

      // 保存快速启动包到 paper_package 表（使用 paperCode + "_quick" 作为 code）
      const quickPaperCode = `${paperCode}_quick`
      const relativePath = path.relative(app.getPath('userData'), quickStartPath)
      const quickHash = this.calculateHash(Buffer.from(response.data))
      const syncTime = Date.now()

      try {
        // 先禁用旧版本的记录（如果有）
        this.db.prepare(`
          UPDATE paper_package
          SET is_active = 0
          WHERE paper_id = ? AND paper_code = ? AND is_active = 1
        `).run(paperId, quickPaperCode)

        // 检查是否已存在相同版本的记录
        const existingRecord = this.db.prepare(`
          SELECT id FROM paper_package
          WHERE paper_id = ? AND paper_code = ? AND version = ?
        `).get(paperId, quickPaperCode, version)

        if (existingRecord) {
          // 更新现有记录
          this.db.prepare(`
            UPDATE paper_package
            SET package_path = ?, package_hash = ?, package_size = ?,
                storage_type = 1, sync_time = ?, is_active = 1
            WHERE id = ?
          `).run(
            relativePath,
            quickHash,
            totalSize,
            syncTime,
            existingRecord.id
          )
          console.log(`✓ 已更新快速启动包到 paper_package 表: paper_id=${paperId}, paper_code=${quickPaperCode}, version=${version}`)
        } else {
          // 插入新记录
          this.db.prepare(`
            INSERT INTO paper_package 
            (paper_id, paper_code, package_path, package_hash, package_size, 
             storage_type, version, sync_time, is_active)
            VALUES (?, ?, ?, ?, ?, 1, ?, ?, 1)
          `).run(
            paperId,
            quickPaperCode,
            relativePath,
            quickHash,
            totalSize,
            version,
            syncTime
          )
          console.log(`✓ 已插入快速启动包到 paper_package 表: paper_id=${paperId}, paper_code=${quickPaperCode}, version=${version}`)
        }

        // 验证记录是否已保存到 paper_package 表
        const verifyRecord = this.db.prepare(`
          SELECT id, paper_code, version, is_active FROM paper_package
          WHERE paper_id = ? AND paper_code = ? AND version = ?
        `).get(paperId, quickPaperCode, version)

        if (verifyRecord) {
          console.log(`✓ 验证：快速启动包已保存到 paper_package 表，id=${verifyRecord.id}, paper_code=${verifyRecord.paper_code}, version=${verifyRecord.version}, is_active=${verifyRecord.is_active}`)
        } else {
          console.error(`❌ 错误：快速启动包保存后验证失败，paper_package 表中未找到记录！`)
          console.error(`   paper_id=${paperId}, paper_code=${quickPaperCode}, version=${version}`)
        }
      } catch (error) {
        console.error(`保存快速启动包到 paper_package 表失败:`, error)
        console.error(`paper_id=${paperId}, paper_code=${quickPaperCode}, version=${version}`)
        console.error(`错误堆栈:`, error.stack)
        // 不抛出错误，因为文件已保存成功，只是数据库记录失败
      }

      // 下载完成，但解压还未完成，保持 downloading 状态（进度90%），等待解压完成后更新为 ready
      // 这样前端可以显示真实的下载进度（0% → 90%），解压完成后会更新为 ready
      this.updateDownloadStatus(paperId, paperCode, 'downloading', 90, totalSize, totalSize)

      if (onProgress) onProgress(90)

      return {
        success: true,
        message: '快速启动包下载成功',
        paperCode,
        version,
        path: quickStartPath,
        size: totalSize
      }
    } catch (error) {
      console.error('下载快速启动包失败:', error.message)
      this.updateDownloadStatus(paperId, paperCode, 'error', 0, 0, 0, error.message)
      throw error
    }
  }

  /**
   * 下载试卷包
   * @param {number} paperId - 试卷ID
   * @param {string} paperCode - 试卷编码
   * @param {number} version - 版本号
   * @param {string} expectedHash - 期望的hash值
   * @param {string} token - 认证token
   * @param {Function} onProgress - 进度回调
   * @returns {Promise<Object>} 下载结果
   */
  async downloadPaperPackage(paperId, paperCode, version, expectedHash, token, onProgress = null) {
    try {
      const packageFileName = `${paperCode}_v${version}.zip`
      const packagePath = path.join(this.packageBasePath, packageFileName)

      // 检查文件大小，决定下载策略
      let fileSize = 0
      try {
        const stats = fs.statSync(packagePath)
        fileSize = stats.size
      } catch (e) {
        // 文件不存在，需要下载
      }

      // 从服务器下载ZIP包
      console.log(`[downloadPaperPackage] 开始下载完整包: paperId=${paperId}, paperCode=${paperCode}, version=${version}`)
      console.log(`[downloadPaperPackage] API URL: ${API_BASE_URL}/student/sync/paper/package/download`)

      let response
      try {
        response = await axios.post(
          `${API_BASE_URL}/student/sync/paper/package/download`,
          { id: paperId },
          {
            headers: {
              'Content-Type': 'application/json',
              'Authorization': `Bearer ${token}`
            },
            responseType: 'stream',
            timeout: 300000, // 5分钟超时
            validateStatus: (status) => status < 500 // 允许4xx状态码，以便检查错误
          }
        )

        // 打印所有响应头
        console.log(`[downloadPaperPackage] 响应状态码: ${response.status}`)
        console.log(`[downloadPaperPackage] 响应头:`, JSON.stringify(response.headers, null, 2))
      } catch (error) {
        console.error(`[downloadPaperPackage] 请求失败:`, error.message)
        // 如果是响应错误，尝试读取错误信息
        if (error.response) {
          console.error(`[downloadPaperPackage] 错误响应状态码:`, error.response.status)
          console.error(`[downloadPaperPackage] 错误响应头:`, JSON.stringify(error.response.headers, null, 2))
          const errorData = await this.readStreamAsString(error.response.data)
          console.error('下载ZIP包失败，服务器返回错误:', errorData)
          throw new Error(`下载失败: ${errorData || error.message}`)
        }
        throw error
      }

      // 检查响应状态码
      if (response.status !== 200) {
        const errorData = await this.readStreamAsString(response.data)
        console.error('下载ZIP包失败，状态码:', response.status, '错误信息:', errorData)
        throw new Error(`下载失败 (状态码: ${response.status}): ${errorData || '未知错误'}`)
      }

      // 检查Content-Type
      const contentType = response.headers['content-type'] || ''
      if (!contentType.includes('application/zip') && !contentType.includes('application/octet-stream')) {
        console.warn('警告：响应Content-Type不是ZIP文件:', contentType)
        // 不立即失败，继续尝试下载（某些服务器可能不设置正确的Content-Type）
      }

      let totalSize = parseInt(response.headers['content-length'] || '0', 10)
      console.log(`ZIP包大小: ${(totalSize / 1024 / 1024).toFixed(2)}MB (${totalSize} 字节)`)
      console.log(`响应Content-Type: ${response.headers['content-type']}`)
      console.log(`响应状态码: ${response.status}`)

      // 如果大小为0，尝试通过HEAD请求获取文件大小，或使用默认策略
      if (totalSize === 0) {
        console.warn('[PaperDownload] ⚠ Content-Length为0，尝试通过HEAD请求获取文件大小')
        try {
          // 尝试HEAD请求获取文件大小（某些服务器在流式响应时不返回Content-Length）
          const headResponse = await axios.head(
            `${API_BASE_URL}/student/sync/paper/package/download?id=${paperId}`,
            {
              headers: {
                'Authorization': `Bearer ${token}`
              },
              timeout: 10000
            }
          )
          totalSize = parseInt(headResponse.headers['content-length'] || '0', 10)
          console.log(`通过HEAD请求获取的文件大小: ${(totalSize / 1024 / 1024).toFixed(2)}MB`)
        } catch (headError) {
          console.warn('HEAD请求失败:', headError.message)
        }

        // 如果仍然是0，使用文件系统下载（会自动处理未知大小）
        if (totalSize === 0) {
          console.warn('[PaperDownload] ⚠ 无法获取文件大小，使用文件系统下载策略（未知大小模式）')
          // 设置一个标记，后续使用文件系统下载策略
          totalSize = -1 // -1 表示未知大小，后续会特殊处理
        }
      }

      // 初始化下载状态
      this.updateDownloadStatus(paperId, paperCode, 'downloading', 0, 0, Math.max(0, totalSize))

      // 根据文件大小选择存储方式和下载策略
      // 优化：50MB以上使用分片下载，提高下载速度
      let result
      try {
        if (totalSize === -1) {
          // 未知大小：使用文件系统下载（流式写入，不需要预知大小）
          console.log('使用文件系统下载策略（未知大小模式）')
          result = await this.downloadToFileSystemUnknownSize(response.data, packagePath, paperCode, version, expectedHash, onProgress, paperId)
        } else if (totalSize < 50 * 1024 * 1024) {
          // 中等文件（50MB以下）：下载到文件系统（串行）
          result = await this.downloadToFileSystem(response.data, packagePath, paperCode, version, expectedHash, totalSize, onProgress)
        } else {
          // 大文件（>=50MB）：使用分片下载（并发，更快，支持断点续传）
          result = await this.downloadWithFragments(paperId, paperCode, version, expectedHash, token, totalSize, onProgress)
        }

        // 下载成功，更新状态
        this.updateDownloadStatus(paperId, paperCode, 'completed', 100, totalSize, totalSize)
        return result
      } catch (error) {
        // 下载失败，更新状态
        this.updateDownloadStatus(paperId, paperCode, 'error', 0, 0, totalSize, error.message)
        throw error
      }
    } catch (error) {
      console.error('下载试卷包失败:', error.message)
      throw error
    }
  }

  /**
   * 下载为BLOB（小文件）
   */
  async downloadAsBlob(stream, paperCode, version, expectedHash, totalSize, onProgress) {
    return new Promise((resolve, reject) => {
      const chunks = []
      let receivedSize = 0

      // 获取paperId用于状态更新
      const paper = this.db.prepare(`
        SELECT id FROM paper WHERE paper_code = ? LIMIT 1
      `).get(paperCode)
      const paperId = paper ? paper.id : null

      stream.on('data', (chunk) => {
        chunks.push(chunk)
        receivedSize += chunk.length
        if (onProgress && totalSize > 0) {
          const progress = Math.floor((receivedSize / totalSize) * 100)
          onProgress(progress)
        }
        // 更新下载状态
        if (paperId) {
          const progress = totalSize > 0 ? Math.floor((receivedSize / totalSize) * 100) : 0
          this.updateDownloadStatus(paperId, paperCode, 'downloading', progress, receivedSize, totalSize)
        }
      })

      stream.on('end', async () => {
        try {
          const buffer = Buffer.concat(chunks)

          console.log(`下载完成，实际接收大小: ${buffer.length} 字节`)

          // 检查文件大小
          if (buffer.length === 0) {
            throw new Error('下载的ZIP包为空')
          }

          // 检查ZIP文件头（PK开头）
          if (buffer.length < 4 || buffer[0] !== 0x50 || buffer[1] !== 0x4B) {
            // 可能是错误响应，尝试解析为文本
            const errorText = buffer.toString('utf8', 0, Math.min(500, buffer.length))
            console.error('下载的内容不是ZIP文件，前500字节:', errorText)
            console.error('下载的buffer长度:', buffer.length)
            console.error('下载的buffer前10字节:', Array.from(buffer.slice(0, 10)).map(b => '0x' + b.toString(16).padStart(2, '0')).join(' '))
            throw new Error(`下载的内容不是ZIP文件，可能是错误信息: ${errorText.substring(0, 200)}`)
          }

          console.log(`ZIP文件头验证通过，文件大小: ${buffer.length} 字节`)

          // 验证hash（如果提供了expectedHash）
          const actualHash = this.calculateHash(buffer)
          console.log(`计算出的hash: ${actualHash}`)
          console.log(`期望的hash: ${expectedHash || '无'}`)

          if (expectedHash && actualHash !== expectedHash) {
            console.error(`ZIP包hash验证失败: 期望 ${expectedHash}, 实际 ${actualHash}`)
            console.error(`ZIP包大小: ${buffer.length} 字节`)

            // 检查是否是错误响应
            const errorText = buffer.toString('utf8', 0, Math.min(500, buffer.length))
            if (errorText.includes('错误') || errorText.includes('失败') || errorText.includes('Exception') || errorText.includes('error')) {
              throw new Error(`服务器返回错误: ${errorText.substring(0, 200)}`)
            }

            // hash验证失败
            // 可能原因：
            // 1) OSS上的文件已更新但数据库未更新（后端会自动重新生成）
            // 2) 文件在传输过程中损坏
            // 3) 后端和客户端的hash算法不一致（需要验证）
            // 4) 后端下载的文件和客户端下载的文件不一致（需要验证）
            console.error(`ZIP包hash验证失败`)
            console.error(`文件大小: ${buffer.length} 字节`)
            console.error(`期望hash (来自数据库): ${expectedHash}`)
            console.error(`实际hash (客户端计算): ${actualHash}`)
            console.error(`hash算法: SHA-256`)
            console.error(`hash长度: 期望=${expectedHash.length}, 实际=${actualHash.length}`)

            // 验证hash算法格式（应该是64个十六进制字符）
            if (actualHash.length !== 64) {
              console.error(`实际hash长度不正确: ${actualHash.length}，应该是64`)
              throw new Error(`hash计算错误: hash长度不正确`)
            }
            if (expectedHash.length !== 64) {
              console.error(`期望hash长度不正确: ${expectedHash.length}，应该是64`)
              throw new Error(`数据库中的hash格式错误: hash长度不正确`)
            }

            // 验证hash字符格式（应该是小写十六进制）
            const hexPattern = /^[0-9a-f]{64}$/
            if (!hexPattern.test(actualHash)) {
              console.error(`实际hash格式不正确: ${actualHash}`)
              throw new Error(`hash计算错误: hash格式不正确`)
            }
            if (!hexPattern.test(expectedHash)) {
              console.error(`期望hash格式不正确: ${expectedHash}`)
              throw new Error(`数据库中的hash格式错误: hash格式不正确`)
            }

            // hash算法和格式都正确，但不匹配
            // 说明：1) 文件内容确实不同 2) 需要后端重新生成
            console.error(`hash算法和格式验证通过，但hash值不匹配`)
            console.error(`可能原因：1) OSS上的文件与数据库记录不一致 2) 文件在传输过程中损坏`)
            throw new Error(`ZIP包hash验证失败: 期望 ${expectedHash}, 实际 ${actualHash}`)
          }

          // 确定最终使用的hash
          // 如果hash匹配，使用期望的hash
          // 如果没有期望hash，使用实际hash
          const finalHash = (expectedHash && actualHash === expectedHash) ? expectedHash : actualHash

          // 获取paper_id（必须存在，否则会报NOT NULL约束错误）
          const paperRecord = this.db.prepare(`
            SELECT id FROM paper WHERE paper_code = ? LIMIT 1
          `).get(paperCode)

          if (!paperRecord || !paperRecord.id) {
            throw new Error(`本地paper表中没有找到试卷记录: ${paperCode}，无法保存paper_package（paper_id不能为null）`)
          }

          // 保存到SQLite
          // 先禁用旧版本的记录（如果有，只禁用完整包的记录，不包括快速包）
          this.db.prepare(`
            UPDATE paper_package
            SET is_active = 0
            WHERE paper_id = ? AND paper_code = ? AND is_active = 1
          `).run(paperRecord.id, paperCode)

          // 检查是否已存在相同版本的记录
          const existingRecord = this.db.prepare(`
            SELECT id FROM paper_package
            WHERE paper_id = ? AND paper_code = ? AND version = ?
          `).get(paperRecord.id, paperCode, version)

          const syncTime = Date.now()

          try {
            if (existingRecord) {
              // 更新现有记录
              this.db.prepare(`
                UPDATE paper_package
                SET package_data = ?, package_hash = ?, package_size = ?,
                    storage_type = 0, sync_time = ?, is_active = 1
                WHERE id = ?
              `).run(
                buffer,
                finalHash,
                buffer.length,
                syncTime,
                existingRecord.id
              )
              console.log(`✓ 已更新完整包到 paper_package 表: paper_id=${paperRecord.id}, paper_code=${paperCode}, version=${version}`)
            } else {
              // 插入新记录
              // 注意：如果表还有 UNIQUE 约束，需要先删除旧记录
              // 但迁移脚本应该已经去掉了 UNIQUE 约束，所以可以直接插入
              try {
                this.db.prepare(`
                  INSERT INTO paper_package 
                  (paper_id, paper_code, package_data, package_hash, package_size, 
                   storage_type, version, sync_time, is_active)
                  VALUES (?, ?, ?, ?, ?, 0, ?, ?, 1)
                `).run(
                  paperRecord.id,
                  paperCode,
                  buffer,
                  finalHash,
                  buffer.length,
                  version,
                  syncTime
                )
                console.log(`✓ 已插入完整包到 paper_package 表: paper_id=${paperRecord.id}, paper_code=${paperCode}, version=${version}`)
              } catch (insertError) {
                // 如果插入失败（可能是 UNIQUE 约束），先删除旧记录再插入
                if (insertError.message && insertError.message.includes('UNIQUE')) {
                  console.warn(`插入失败（UNIQUE约束），先删除旧记录: ${insertError.message}`)
                  this.db.prepare(`
                    DELETE FROM paper_package
                    WHERE paper_id = ? AND paper_code = ?
                  `).run(paperRecord.id, paperCode)

                  // 重新插入
                  this.db.prepare(`
                    INSERT INTO paper_package 
                    (paper_id, paper_code, package_data, package_hash, package_size, 
                     storage_type, version, sync_time, is_active)
                    VALUES (?, ?, ?, ?, ?, 0, ?, ?, 1)
                  `).run(
                    paperRecord.id,
                    paperCode,
                    buffer,
                    finalHash,
                    buffer.length,
                    version,
                    syncTime
                  )
                  console.log(`✓ 已插入完整包到 paper_package 表（删除旧记录后）: paper_id=${paperRecord.id}, paper_code=${paperCode}, version=${version}`)
                } else {
                  throw insertError
                }
              }
            }
          } catch (error) {
            console.error(`保存完整包到 paper_package 表失败:`, error)
            console.error(`paper_id=${paperRecord.id}, paper_code=${paperCode}, version=${version}`)
            console.error(`错误详情:`, error.message)
            throw error
          }

          console.log(`✓ ZIP包已保存为BLOB: ${paperCode}, 大小: ${(buffer.length / 1024 / 1024).toFixed(2)}MB`)

          // 验证记录是否已保存到 paper_package 表
          const verifyRecord = this.db.prepare(`
            SELECT id, paper_code, version, is_active FROM paper_package
            WHERE paper_id = ? AND paper_code = ? AND version = ?
          `).get(paperRecord.id, paperCode, version)

          if (verifyRecord) {
            console.log(`✓ 验证：完整包已保存到 paper_package 表，id=${verifyRecord.id}, paper_code=${verifyRecord.paper_code}, version=${verifyRecord.version}, is_active=${verifyRecord.is_active}`)
          } else {
            console.error(`❌ 错误：完整包保存后验证失败，paper_package 表中未找到记录！`)
            console.error(`   paper_id=${paperRecord.id}, paper_code=${paperCode}, version=${version}`)
          }

          // 更新下载状态（使用已查询的paperRecord）
          if (paperRecord && paperRecord.id) {
            this.updateDownloadStatus(paperRecord.id, paperCode, 'completed', 100, buffer.length, buffer.length)
          }

          resolve({
            success: true,
            storageType: 'blob',
            size: buffer.length,
            hash: finalHash
          })
        } catch (error) {
          reject(error)
        }
      })

      stream.on('error', reject)
    })
  }

  /**
   * 下载到文件系统（中等文件）
   */
  async downloadToFileSystem(stream, filePath, paperCode, version, expectedHash, totalSize, onProgress) {
    return new Promise((resolve, reject) => {
      // 获取paperId用于状态更新
      const paper = this.db.prepare(`
        SELECT id FROM paper WHERE paper_code = ? LIMIT 1
      `).get(paperCode)
      const paperId = paper ? paper.id : null

      const writeStream = fs.createWriteStream(filePath)
      let receivedSize = 0

      stream.on('data', (chunk) => {
        writeStream.write(chunk)
        receivedSize += chunk.length
        if (onProgress && totalSize > 0) {
          const progress = Math.floor((receivedSize / totalSize) * 100)
          onProgress(progress)
        }
        // 更新下载状态
        if (paperId) {
          const progress = totalSize > 0 ? Math.floor((receivedSize / totalSize) * 100) : 0
          this.updateDownloadStatus(paperId, paperCode, 'downloading', progress, receivedSize, totalSize)
        }
      })

      stream.on('end', async () => {
        try {
          writeStream.end()

          // 等待文件写入完成
          await new Promise((resolve) => writeStream.on('close', resolve))

          // 验证hash（如果提供了expectedHash）
          const buffer = fs.readFileSync(filePath)

          // 检查文件大小
          if (buffer.length === 0) {
            fs.unlinkSync(filePath)
            throw new Error('下载的ZIP包为空')
          }

          // 检查ZIP文件头（PK开头）
          if (buffer.length < 4 || buffer[0] !== 0x50 || buffer[1] !== 0x4B) {
            fs.unlinkSync(filePath)
            // 可能是错误响应，尝试解析为文本
            const errorText = buffer.toString('utf8', 0, Math.min(500, buffer.length))
            console.error('下载的内容不是ZIP文件，前500字节:', errorText)
            throw new Error(`下载的内容不是ZIP文件，可能是错误信息: ${errorText.substring(0, 200)}`)
          }

          const actualHash = this.calculateHash(buffer)
          if (expectedHash && actualHash !== expectedHash) {
            fs.unlinkSync(filePath)
            console.error(`ZIP包hash验证失败: 期望 ${expectedHash}, 实际 ${actualHash}`)
            console.error(`ZIP包大小: ${buffer.length} 字节`)
            // 检查是否是错误响应
            const errorText = buffer.toString('utf8', 0, Math.min(500, buffer.length))
            if (errorText.includes('错误') || errorText.includes('失败') || errorText.includes('Exception')) {
              throw new Error(`服务器返回错误: ${errorText.substring(0, 200)}`)
            }
            throw new Error(`ZIP包hash验证失败: 期望 ${expectedHash}, 实际 ${actualHash}`)
          }

          // 如果没有提供expectedHash，使用计算出的hash
          const finalHash = expectedHash || actualHash

          // 获取paper_id（必须存在，否则会报NOT NULL约束错误）
          const paperRecordForFileSystem = this.db.prepare(`
        SELECT id FROM paper WHERE paper_code = ? LIMIT 1
      `).get(paperCode)

          if (!paperRecordForFileSystem || !paperRecordForFileSystem.id) {
            throw new Error(`本地paper表中没有找到试卷记录: ${paperCode}，无法保存paper_package（paper_id不能为null）`)
          }

          // 保存路径到SQLite
          // 先禁用旧版本的记录（如果有，只禁用完整包的记录，不包括快速包）
          this.db.prepare(`
        UPDATE paper_package
        SET is_active = 0
        WHERE paper_id = ? AND paper_code = ? AND is_active = 1
      `).run(paperRecordForFileSystem.id, paperCode)

          // 检查是否已存在相同版本的记录
          const existingRecord = this.db.prepare(`
        SELECT id FROM paper_package
        WHERE paper_id = ? AND paper_code = ? AND version = ?
      `).get(paperRecordForFileSystem.id, paperCode, version)

          const relativePath = path.relative(app.getPath('userData'), filePath)
          const syncTime = Date.now()

          try {
            if (existingRecord) {
              // 更新现有记录
              this.db.prepare(`
            UPDATE paper_package
            SET package_path = ?, package_hash = ?, package_size = ?,
                storage_type = 1, sync_time = ?, is_active = 1
            WHERE id = ?
          `).run(
                relativePath,
                finalHash,
                buffer.length,
                syncTime,
                existingRecord.id
              )
              console.log(`✓ 已更新完整包到 paper_package 表: paper_id=${paperRecordForFileSystem.id}, paper_code=${paperCode}, version=${version}`)
            } else {
              // 插入新记录
              // 注意：如果表还有 UNIQUE 约束，需要先删除旧记录
              // 但迁移脚本应该已经去掉了 UNIQUE 约束，所以可以直接插入
              try {
                this.db.prepare(`
              INSERT INTO paper_package 
              (paper_id, paper_code, package_path, package_hash, package_size, 
               storage_type, version, sync_time, is_active)
              VALUES (?, ?, ?, ?, ?, 1, ?, ?, 1)
            `).run(
                  paperRecordForFileSystem.id,
                  paperCode,
                  relativePath,
                  finalHash,
                  buffer.length,
                  version,
                  syncTime
                )
                console.log(`✓ 已插入完整包到 paper_package 表: paper_id=${paperRecordForFileSystem.id}, paper_code=${paperCode}, version=${version}`)
              } catch (insertError) {
                // 如果插入失败（可能是 UNIQUE 约束），先删除旧记录再插入
                if (insertError.message && insertError.message.includes('UNIQUE')) {
                  console.warn(`插入失败（UNIQUE约束），先删除旧记录: ${insertError.message}`)
                  this.db.prepare(`
                DELETE FROM paper_package
                WHERE paper_id = ? AND paper_code = ?
              `).run(paperRecordForFileSystem.id, paperCode)

                  // 重新插入
                  this.db.prepare(`
                INSERT INTO paper_package 
                (paper_id, paper_code, package_path, package_hash, package_size, 
                 storage_type, version, sync_time, is_active)
                VALUES (?, ?, ?, ?, ?, 1, ?, ?, 1)
              `).run(
                    paperRecordForFileSystem.id,
                    paperCode,
                    relativePath,
                    finalHash,
                    buffer.length,
                    version,
                    syncTime
                  )
                  console.log(`✓ 已插入完整包到 paper_package 表（删除旧记录后）: paper_id=${paperRecordForFileSystem.id}, paper_code=${paperCode}, version=${version}`)
                } else {
                  throw insertError
                }
              }
            }
          } catch (error) {
            console.error(`保存完整包到 paper_package 表失败:`, error)
            console.error(`paper_id=${paperRecordForFileSystem.id}, paper_code=${paperCode}, version=${version}`)
            console.error(`错误详情:`, error.message)
            throw error
          }

          console.log(`✓ ZIP包已保存到文件系统: ${filePath}, 大小: ${(buffer.length / 1024 / 1024).toFixed(2)}MB`)

          // 验证记录是否已保存到 paper_package 表
          const verifyRecord = this.db.prepare(`
        SELECT id, paper_code, version, is_active FROM paper_package
        WHERE paper_id = ? AND paper_code = ? AND version = ?
      `).get(paperRecordForFileSystem.id, paperCode, version)

          if (verifyRecord) {
            console.log(`✓ 验证：完整包已保存到 paper_package 表，id=${verifyRecord.id}, paper_code=${verifyRecord.paper_code}, version=${verifyRecord.version}, is_active=${verifyRecord.is_active}`)
          } else {
            console.error(`❌ 错误：完整包保存后验证失败，paper_package 表中未找到记录！`)
            console.error(`   paper_id=${paperRecordForFileSystem.id}, paper_code=${paperCode}, version=${version}`)
          }

          // 更新下载状态（使用已查询的paperRecordForFileSystem）
          if (paperRecordForFileSystem && paperRecordForFileSystem.id) {
            this.updateDownloadStatus(paperRecordForFileSystem.id, paperCode, 'completed', 100, buffer.length, buffer.length)
          }

          resolve({
            success: true,
            storageType: 'filesystem',
            path: filePath,
            size: buffer.length,
            hash: finalHash
          })
        } catch (error) {
          reject(error)
        }
      })

      stream.on('error', (error) => {
        writeStream.destroy()
        if (fs.existsSync(filePath)) {
          fs.unlinkSync(filePath)
        }
        reject(error)
      })
    })
  }

  /**
   * 未知大小模式下载到文件系统（流式写入）
   * 用于服务器不返回 Content-Length 的情况
   */
  async downloadToFileSystemUnknownSize(stream, filePath, paperCode, version, expectedHash, onProgress, paperId) {
    return new Promise((resolve, reject) => {
      const writeStream = fs.createWriteStream(filePath)
      let receivedSize = 0
      let lastProgressUpdate = 0

      stream.on('data', (chunk) => {
        writeStream.write(chunk)
        receivedSize += chunk.length

        // 每收到 1MB 数据更新一次进度（避免频繁更新）
        if (receivedSize - lastProgressUpdate > 1024 * 1024) {
          lastProgressUpdate = receivedSize
          const sizeMB = (receivedSize / 1024 / 1024).toFixed(2)
          console.log(`下载中... 已接收: ${sizeMB}MB`)

          // 更新下载状态（大小未知，进度无法计算，只更新已下载大小）
          if (paperId) {
            this.updateDownloadStatus(paperId, paperCode, 'downloading', 0, receivedSize, 0)
          }

          // 进度回调（无法计算百分比，只通知已下载大小）
          if (onProgress) {
            onProgress(-1, receivedSize) // -1 表示进度未知
          }
        }
      })

      stream.on('end', async () => {
        try {
          writeStream.end()

          // 等待文件写入完成
          await new Promise((resolve) => writeStream.on('close', resolve))

          console.log(`下载完成，实际接收大小: ${(receivedSize / 1024 / 1024).toFixed(2)}MB (${receivedSize} 字节)`)

          // 验证hash（如果提供了expectedHash）
          const buffer = fs.readFileSync(filePath)

          // 检查文件大小
          if (buffer.length === 0) {
            fs.unlinkSync(filePath)
            reject(new Error('下载的ZIP包为空'))
            return
          }

          // 检查ZIP文件头（PK开头）
          if (buffer.length < 4 || buffer[0] !== 0x50 || buffer[1] !== 0x4B) {
            fs.unlinkSync(filePath)
            const errorText = buffer.toString('utf8', 0, Math.min(500, buffer.length))
            console.error('下载的内容不是ZIP文件，前500字节:', errorText)
            reject(new Error(`下载的内容不是ZIP文件，可能是错误信息: ${errorText.substring(0, 200)}`))
            return
          }

          const actualHash = this.calculateHash(buffer)
          if (expectedHash && actualHash !== expectedHash) {
            fs.unlinkSync(filePath)
            console.error(`ZIP包hash验证失败: 期望 ${expectedHash}, 实际 ${actualHash}`)
            reject(new Error(`ZIP包hash验证失败: 期望 ${expectedHash}, 实际 ${actualHash}`))
            return
          }

          const finalHash = expectedHash || actualHash

          // 获取paper_id
          const paperRecord = this.db.prepare(`
            SELECT id FROM paper WHERE paper_code = ? LIMIT 1
          `).get(paperCode)

          if (!paperRecord || !paperRecord.id) {
            reject(new Error(`本地paper表中没有找到试卷记录: ${paperCode}`))
            return
          }

          // 保存到 paper_package 表
          this.db.prepare(`
            UPDATE paper_package SET is_active = 0
            WHERE paper_id = ? AND paper_code = ? AND is_active = 1
          `).run(paperRecord.id, paperCode)

          const existingRecord = this.db.prepare(`
            SELECT id FROM paper_package
            WHERE paper_id = ? AND paper_code = ? AND version = ?
          `).get(paperRecord.id, paperCode, version)

          const syncTime = Date.now()
          const relativePath = `paper_packages/${path.basename(filePath)}`

          if (existingRecord) {
            this.db.prepare(`
              UPDATE paper_package
              SET package_path = ?, package_hash = ?, package_size = ?,
                  storage_type = 1, sync_time = ?, is_active = 1
              WHERE id = ?
            `).run(relativePath, finalHash, buffer.length, syncTime, existingRecord.id)
          } else {
            this.db.prepare(`
              INSERT INTO paper_package 
              (paper_id, paper_code, package_path, package_hash, package_size, 
               storage_type, version, sync_time, is_active)
              VALUES (?, ?, ?, ?, ?, 1, ?, ?, 1)
            `).run(paperRecord.id, paperCode, relativePath, finalHash, buffer.length, version, syncTime)
          }

          console.log(`✓ ZIP包已保存到文件系统: ${filePath}, 大小: ${(buffer.length / 1024 / 1024).toFixed(2)}MB`)

          // 更新下载状态为完成
          if (paperId) {
            this.updateDownloadStatus(paperId, paperCode, 'completed', 100, buffer.length, buffer.length)
          }

          resolve({
            success: true,
            packagePath: filePath,
            packageSize: buffer.length,
            packageHash: finalHash
          })
        } catch (error) {
          if (fs.existsSync(filePath)) {
            fs.unlinkSync(filePath)
          }
          reject(error)
        }
      })

      stream.on('error', (error) => {
        console.error('下载流错误:', error.message)
        if (fs.existsSync(filePath)) {
          fs.unlinkSync(filePath)
        }
        reject(error)
      })
    })
  }

  /**
   * 分片下载（大文件，支持并发下载和断点续传）
   */
  async downloadWithFragments(paperId, paperCode, version, expectedHash, token, totalSize, onProgress) {
    try {
      const packageFileName = `${paperCode}_v${version}.zip`
      const packagePath = path.join(this.packageBasePath, packageFileName)

      // 动态计算分片大小（优化：使用5MB分片，更快响应）
      // 统一使用5MB分片，无论文件大小，提供更快的响应速度
      const chunkSize = 5 * 1024 * 1024 // 5MB分片

      const totalChunks = Math.ceil(totalSize / chunkSize)
      console.log(`开始分片下载: ${packageFileName}, 大小: ${(totalSize / 1024 / 1024).toFixed(2)}MB, 分片数: ${totalChunks}, 分片大小: ${(chunkSize / 1024 / 1024).toFixed(2)}MB`)

      // 断点续传：检查是否有未完成的下载
      const resumeInfo = this.getResumeInfo(paperId, paperCode, version, totalSize, totalChunks)
      let downloadedBytes = resumeInfo.downloadedBytes
      const downloadedChunks = resumeInfo.downloadedChunks // Set<chunkIndex>

      console.log(`断点续传检查: 已下载 ${downloadedBytes} 字节 (${(downloadedBytes / 1024 / 1024).toFixed(2)}MB), 已完成分片: ${downloadedChunks.size}/${totalChunks}`)

      // 初始化chunks数组（存储每个分片的数据）
      const chunks = new Array(totalChunks)

      // 断点续传：如果文件已存在且需要续传，先读取已下载的分片
      if (resumeInfo.canResume && fs.existsSync(packagePath)) {
        // 断点续传：读取已下载的分片数据到chunks数组
        console.log(`使用断点续传模式，从 ${downloadedBytes} 字节继续下载`)
        const existingFileBuffer = fs.readFileSync(packagePath)
        for (let i = 0; i < totalChunks; i++) {
          if (downloadedChunks.has(i)) {
            const start = i * chunkSize
            const end = Math.min(start + chunkSize - 1, totalSize - 1)
            const chunkLength = end - start + 1
            if (existingFileBuffer.length >= start + chunkLength) {
              chunks[i] = { index: i, data: existingFileBuffer.slice(start, start + chunkLength) }
              console.log(`✓ 分片 ${i + 1}/${totalChunks} 已存在，跳过下载`)
            } else {
              console.warn(`⚠️ 分片 ${i + 1}/${totalChunks} 标记为已下载，但文件不完整，重新下载`)
              downloadedChunks.delete(i)
              downloadedBytes -= chunkLength
            }
          }
        }
      } else {
        // 新下载：清空状态
        downloadedBytes = 0
        downloadedChunks.clear()
        // 更新下载状态
        this.updateDownloadStatus(paperId, paperCode, 'downloading', 0, 0, totalSize)
      }

      // 并发下载所有分片（优化：增加到6个并发，提高下载速度）
      const downloadPromises = []
      const maxConcurrent = 6 // 增加到6个并发
      let currentConcurrent = 0

      for (let i = 0; i < totalChunks; i++) {
        // 跳过已下载的分片（断点续传时，已下载的分片已在上面读取到chunks数组）
        if (downloadedChunks.has(i) && chunks[i] && chunks[i].data) {
          // 分片已下载且已读取，跳过
          continue
        }

        const start = i * chunkSize
        const end = Math.min(start + chunkSize - 1, totalSize - 1)

        // 控制并发数
        while (currentConcurrent >= maxConcurrent) {
          await new Promise(resolve => setTimeout(resolve, 100))
        }

        currentConcurrent++
        const promise = this.downloadChunk(paperId, start, end, token)
          .then(chunkData => {
            chunks[i] = { index: i, data: chunkData }
            downloadedBytes += chunkData.length
            downloadedChunks.add(i)
            currentConcurrent--

            // 更新下载状态到数据库
            const progress = totalSize > 0 ? Math.floor((downloadedBytes / totalSize) * 100) : 0
            this.updateDownloadStatus(paperId, paperCode, 'downloading', progress, downloadedBytes, totalSize)

            // 进度回调
            if (onProgress && totalSize > 0) {
              onProgress(progress)
            }

            console.log(`分片 ${i + 1}/${totalChunks} 下载完成，已下载: ${(downloadedBytes / 1024 / 1024).toFixed(2)}MB / ${(totalSize / 1024 / 1024).toFixed(2)}MB`)
          })
          .catch(error => {
            currentConcurrent--
            // 下载失败，更新状态
            this.updateDownloadStatus(paperId, paperCode, 'error', 0, downloadedBytes, totalSize, error.message)
            throw error
          })

        downloadPromises.push(promise)
      }

      // 等待所有分片下载完成
      await Promise.all(downloadPromises)

      // 验证所有分片都已下载
      for (let i = 0; i < totalChunks; i++) {
        if (!chunks[i] || !chunks[i].data) {
          throw new Error(`分片 ${i + 1}/${totalChunks} 下载失败或数据为空`)
        }
      }

      console.log(`✓ 所有分片下载完成，开始合并写入文件`)

      // 按顺序写入文件（所有分片，包括已下载的和新下载的）
      const writeStream = fs.createWriteStream(packagePath)
      for (let i = 0; i < totalChunks; i++) {
        writeStream.write(chunks[i].data)
      }

      writeStream.end()

      // 等待文件写入完成
      await new Promise((resolve, reject) => {
        writeStream.on('close', resolve)
        writeStream.on('error', reject)
      })

      console.log(`✓ 文件写入完成: ${packagePath}`)

      // 验证hash（如果提供了expectedHash）
      const buffer = fs.readFileSync(packagePath)

      // 检查文件大小
      if (buffer.length === 0) {
        fs.unlinkSync(packagePath)
        throw new Error('下载的ZIP包为空')
      }

      // 检查ZIP文件头（PK开头）
      if (buffer.length < 4 || buffer[0] !== 0x50 || buffer[1] !== 0x4B) {
        fs.unlinkSync(packagePath)
        const errorText = buffer.toString('utf8', 0, Math.min(500, buffer.length))
        console.error('下载的内容不是ZIP文件，前500字节:', errorText)
        throw new Error(`下载的内容不是ZIP文件，可能是错误信息: ${errorText.substring(0, 200)}`)
      }

      const actualHash = this.calculateHash(buffer)

      if (expectedHash && actualHash !== expectedHash) {
        fs.unlinkSync(packagePath)
        console.error(`ZIP包hash验证失败: 期望 ${expectedHash}, 实际 ${actualHash}`)
        throw new Error(`ZIP包hash验证失败: 期望 ${expectedHash}, 实际 ${actualHash}`)
      }

      const finalHash = expectedHash || actualHash

      // 获取paper_id（使用传入的paperId，如果不存在则查询）
      let finalPaperId = paperId
      if (!finalPaperId) {
        const paperRecordForFragments = this.db.prepare(`
          SELECT id FROM paper WHERE paper_code = ? LIMIT 1
        `).get(paperCode)
        if (paperRecordForFragments && paperRecordForFragments.id) {
          finalPaperId = paperRecordForFragments.id
        }
      }

      if (!finalPaperId) {
        throw new Error(`本地paper表中没有找到试卷记录: ${paperCode}，无法保存paper_package`)
      }

      // 保存路径到SQLite
      // 先禁用旧版本的记录（如果有）
      this.db.prepare(`
        UPDATE paper_package
        SET is_active = 0
        WHERE paper_id = ? AND is_active = 1
      `).run(finalPaperId)

      // 检查是否已存在相同版本的记录
      const existingRecord = this.db.prepare(`
        SELECT id FROM paper_package
        WHERE paper_id = ? AND version = ?
      `).get(finalPaperId, version)

      const relativePath = path.relative(app.getPath('userData'), packagePath)
      const syncTime = Date.now()

      try {
        if (existingRecord) {
          // 更新现有记录
          this.db.prepare(`
            UPDATE paper_package
            SET paper_code = ?, package_path = ?, package_hash = ?, package_size = ?,
                storage_type = 1, sync_time = ?, is_active = 1
            WHERE id = ?
          `).run(
            paperCode,
            relativePath,
            finalHash,
            buffer.length,
            syncTime,
            existingRecord.id
          )
          console.log(`✓ 已更新 paper_package 表记录: paper_id=${finalPaperId}, version=${version}`)
        } else {
          // 插入新记录（由于paper_id和paper_code有UNIQUE约束，需要先删除旧记录）
          // 删除所有旧记录（包括is_active=0的），因为UNIQUE约束不允许重复
          this.db.prepare(`
            DELETE FROM paper_package
            WHERE paper_id = ?
          `).run(finalPaperId)

          // 插入新记录
          this.db.prepare(`
            INSERT INTO paper_package 
            (paper_id, paper_code, package_path, package_hash, package_size, 
             storage_type, version, sync_time, is_active)
            VALUES (?, ?, ?, ?, ?, 1, ?, ?, 1)
          `).run(
            finalPaperId,
            paperCode,
            relativePath,
            finalHash,
            buffer.length,
            version,
            syncTime
          )
          console.log(`✓ 已插入 paper_package 表记录: paper_id=${finalPaperId}, version=${version}`)
        }
      } catch (error) {
        console.error(`保存 paper_package 表记录失败:`, error)
        console.error(`paper_id=${finalPaperId}, paper_code=${paperCode}, version=${version}`)
        throw error
      }

      console.log(`✓ ZIP包分片下载完成: ${packagePath}, 大小: ${(buffer.length / 1024 / 1024).toFixed(2)}MB`)

      // 验证记录是否已保存到 paper_package 表
      const verifyRecord = this.db.prepare(`
        SELECT id, paper_code, version, is_active FROM paper_package
        WHERE paper_id = ? AND paper_code = ? AND version = ?
      `).get(finalPaperId, paperCode, version)

      if (verifyRecord) {
        console.log(`✓ 验证：完整包已保存到 paper_package 表，id=${verifyRecord.id}, paper_code=${verifyRecord.paper_code}, version=${verifyRecord.version}, is_active=${verifyRecord.is_active}`)
      } else {
        console.error(`❌ 错误：完整包保存后验证失败，paper_package 表中未找到记录！`)
        console.error(`   paper_id=${finalPaperId}, paper_code=${paperCode}, version=${version}`)
      }

      // 更新下载状态为已完成
      this.updateDownloadStatus(finalPaperId, paperCode, 'completed', 100, buffer.length, totalSize)

      return {
        success: true,
        storageType: 'filesystem',
        path: packagePath,
        size: buffer.length,
        hash: finalHash
      }
    } catch (error) {
      console.error('分片下载失败:', error.message)
      // 更新下载状态为失败
      this.updateDownloadStatus(paperId, paperCode, 'error', 0, 0, totalSize, error.message)
      throw error
    }
  }

  /**
   * 下载单个分片（使用Range请求）
   */
  async downloadChunk(paperId, start, end, token) {
    return new Promise((resolve, reject) => {
      axios({
        method: 'POST',
        url: `${API_BASE_URL}/student/sync/paper/package/download`,
        data: { id: paperId },
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`,
          'Range': `bytes=${start}-${end}`
        },
        responseType: 'arraybuffer',
        timeout: 30000 // 优化：30秒超时，快速失败并重试
      })
        .then(response => {
          if (response.status === 206 || response.status === 200) {
            resolve(Buffer.from(response.data))
          } else {
            reject(new Error(`下载分片失败: 状态码 ${response.status}`))
          }
        })
        .catch(error => {
          reject(new Error(`下载分片失败: ${error.message}`))
        })
    })
  }

  /**
   * 计算文件hash（SHA256）
   */
  calculateHash(buffer) {
    return crypto.createHash('sha256').update(buffer).digest('hex')
  }

  /**
   * 获取断点续传信息
   */
  getResumeInfo(paperId, paperCode, version, totalSize, totalChunks) {
    try {
      // 从数据库查询下载状态
      const status = this.db.prepare(`
        SELECT status, progress, downloaded_size, total_size
        FROM download_status
        WHERE paper_id = ?
      `).get(paperId)

      if (!status || status.status !== 'downloading') {
        return {
          canResume: false,
          downloadedBytes: 0,
          downloadedChunks: new Set()
        }
      }

      // 检查文件是否存在
      const packageFileName = `${paperCode}_v${version}.zip`
      const packagePath = path.join(this.packageBasePath, packageFileName)

      if (!fs.existsSync(packagePath)) {
        // 文件不存在，清除状态
        this.clearDownloadStatus(paperId)
        return {
          canResume: false,
          downloadedBytes: 0,
          downloadedChunks: new Set()
        }
      }

      // 检查文件大小
      const fileStats = fs.statSync(packagePath)
      const fileSize = fileStats.size

      // 如果文件大小与已下载大小不一致，重新下载
      if (fileSize !== status.downloaded_size) {
        console.warn(`文件大小不一致，重新下载: 文件=${fileSize}, 记录=${status.downloaded_size}`)
        this.clearDownloadStatus(paperId)
        return {
          canResume: false,
          downloadedBytes: 0,
          downloadedChunks: new Set()
        }
      }

      // 计算已下载的分片
      const chunkSize = 5 * 1024 * 1024 // 5MB
      const downloadedChunks = new Set()
      let downloadedBytes = 0

      for (let i = 0; i < totalChunks; i++) {
        const start = i * chunkSize
        const end = Math.min(start + chunkSize - 1, totalSize - 1)
        const chunkLength = end - start + 1

        if (fileSize >= start + chunkLength) {
          downloadedChunks.add(i)
          downloadedBytes += chunkLength
        } else if (fileSize > start) {
          // 部分下载，需要重新下载这个分片
          downloadedBytes += fileSize - start
        }
      }

      return {
        canResume: true,
        downloadedBytes: downloadedBytes,
        downloadedChunks: downloadedChunks
      }
    } catch (error) {
      console.warn('获取断点续传信息失败:', error)
      return {
        canResume: false,
        downloadedBytes: 0,
        downloadedChunks: new Set()
      }
    }
  }

  /**
   * 更新下载状态到数据库
   */
  updateDownloadStatus(paperId, paperCode, status, progress, downloadedSize, totalSize, errorMessage = null) {
    try {
      const now = Date.now()

      // 检查是否已存在
      const existing = this.db.prepare(`
        SELECT paper_id FROM download_status WHERE paper_id = ?
      `).get(paperId)

      if (existing) {
        // 更新
        this.db.prepare(`
          UPDATE download_status
          SET status = ?, progress = ?, downloaded_size = ?, total_size = ?,
              error_message = ?, update_time = ?,
              completed_time = CASE WHEN ? = 'completed' OR ? = 'ready' THEN ? ELSE completed_time END
          WHERE paper_id = ?
        `).run(
          status, progress, downloadedSize, totalSize,
          errorMessage, now,
          status, status, now,
          paperId
        )
      } else {
        // 插入
        this.db.prepare(`
          INSERT INTO download_status
          (paper_id, paper_code, status, progress, downloaded_size, total_size,
           error_message, start_time, update_time, completed_time)
          VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        `).run(
          paperId, paperCode, status, progress, downloadedSize, totalSize,
          errorMessage, now, now,
          (status === 'completed' || status === 'ready') ? now : null
        )
      }
    } catch (error) {
      console.warn('更新下载状态失败:', error)
    }
  }

  /**
   * 清除下载状态
   */
  clearDownloadStatus(paperId) {
    try {
      this.db.prepare(`
        DELETE FROM download_status WHERE paper_id = ?
      `).run(paperId)
    } catch (error) {
      console.warn('清除下载状态失败:', error)
    }
  }

  /**
   * 获取下载状态
   */
  getDownloadStatus(paperId) {
    try {
      return this.db.prepare(`
        SELECT paper_id, paper_code, status, progress, downloaded_size, total_size,
               error_message, start_time, update_time, completed_time
        FROM download_status
        WHERE paper_id = ?
      `).get(paperId)
    } catch (error) {
      console.warn('获取下载状态失败:', error)
      return null
    }
  }

  /**
   * 检查ZIP包是否存在
   */
  /**
   * 检查快速启动包是否存在
   * @param {number} paperId - 试卷ID
   * @returns {boolean} 是否存在
   */
  checkQuickStartPackageExists(paperId) {
    try {
      // 从paper表获取试卷编码和版本
      const paper = this.db.prepare(`
        SELECT paper_code, version
        FROM paper
        WHERE id = ?
        LIMIT 1
      `).get(paperId)

      if (paper && paper.paper_code && paper.version) {
        const quickStartFileName = `${paper.paper_code}_v${paper.version}_quick.zip`
        const quickStartPath = path.join(this.packageBasePath, quickStartFileName)
        return fs.existsSync(quickStartPath)
      }

      return false
    } catch (error) {
      console.warn('检查快速启动包是否存在失败:', error)
      return false
    }
  }

  checkPackageExists(paperId) {
    try {
      // 首先获取 paper 表中要求的版本和hash
      const paperInfo = this.db.prepare(`
        SELECT paper_code, version, package_hash, package_size
        FROM paper
        WHERE id = ?
      `).get(paperId)

      if (!paperInfo) {
        console.warn(`[checkPackageExists] 未找到试卷信息，paperId=${paperId}`)
        return false
      }

      const requiredVersion = paperInfo.version
      const paperCode = paperInfo.paper_code
      const expectedHash = paperInfo.package_hash
      const expectedSize = paperInfo.package_size

      console.log(`[checkPackageExists] 检查完整包: paperId=${paperId}, paperCode=${paperCode}, 要求版本=${requiredVersion}`)

      // 检查 paper_package 表中是否有要求版本的完整包（不是 _quick 包）
      const packageInfo = this.db.prepare(`
        SELECT paper_code, version, is_active, package_hash, package_size
        FROM paper_package
        WHERE paper_id = ? AND paper_code = ? AND version = ? AND is_active = 1
      `).get(paperId, paperCode, requiredVersion)

      if (packageInfo) {
        // 检查文件是否存在
        const packageFileName = `${packageInfo.paper_code}_v${packageInfo.version}.zip`
        const packagePath = path.join(this.packageBasePath, packageFileName)
        const exists = fs.existsSync(packagePath)

        if (!exists) {
          // 数据库有记录但文件不存在，删除记录
          console.warn(`[checkPackageExists] ⚠️ 数据库有记录但文件不存在，删除记录: ${packagePath}`)
          this.db.prepare(`DELETE FROM paper_package WHERE paper_id = ? AND version = ?`).run(paperId, requiredVersion)
          return false
        }

        console.log(`[checkPackageExists] ✓ 数据库中有 v${requiredVersion} 记录，文件存在: ${packagePath}`)
        return true
      }

      // 数据库中没有要求版本的记录，检查文件系统
      const packageFileName = `${paperCode}_v${requiredVersion}.zip`
      const packagePath = path.join(this.packageBasePath, packageFileName)
      const existsOnDisk = fs.existsSync(packagePath)

      if (!existsOnDisk) {
        console.log(`[checkPackageExists] 数据库和文件系统都没有 v${requiredVersion}`)
        return false
      }

      console.log(`[checkPackageExists] ⚠️ 文件存在但数据库没有记录，尝试补充记录: ${packagePath}`)

      // 文件存在但数据库没有记录，验证hash并补充记录
      try {
        // 读取文件并计算hash
        const fileBuffer = fs.readFileSync(packagePath)
        const actualHash = crypto.createHash('sha256').update(fileBuffer).digest('hex')
        const actualSize = fileBuffer.length

        console.log(`[checkPackageExists] 文件hash验证: 期望=${expectedHash}, 实际=${actualHash}`)

        // 如果hash不匹配，删除文件
        if (expectedHash && actualHash !== expectedHash) {
          console.error(`[checkPackageExists] ❌ hash不匹配，删除文件: ${packagePath}`)
          fs.unlinkSync(packagePath)
          return false
        }

        // hash匹配或没有期望hash，补充数据库记录
        const relativePath = `paper_packages/${packageFileName}`
        this.db.prepare(`
          INSERT INTO paper_package 
          (paper_id, paper_code, package_path, package_hash, package_size, is_active, version, sync_time, storage_type)
          VALUES (?, ?, ?, ?, ?, 1, ?, ?, 'local')
        `).run(
          paperId,
          paperCode,
          relativePath,
          actualHash,
          actualSize,
          requiredVersion,
          Date.now()
        )

        console.log(`[checkPackageExists] ✓ 已补充数据库记录: paperId=${paperId}, version=${requiredVersion}, hash=${actualHash}`)
        return true
      } catch (error) {
        console.error(`[checkPackageExists] 补充数据库记录失败:`, error)
        return false
      }
    } catch (error) {
      console.warn('检查ZIP包是否存在失败:', error)
      return false
    }
  }

  /**
   * 检查是否有任何可用版本的完整包（用于降级使用）
   * 注意：会同时检查快速包和完整包是否都存在同一版本
   * @param {number} paperId - 试卷ID
   * @returns {Object|null} 返回可用的包信息，或 null
   */
  checkAnyPackageExists(paperId) {
    try {
      const paperInfo = this.db.prepare(`
        SELECT paper_code, version
        FROM paper
        WHERE id = ?
      `).get(paperId)

      if (!paperInfo) {
        console.warn(`[checkAnyPackageExists] 未找到试卷信息，paperId=${paperId}`)
        return null
      }

      const paperCode = paperInfo.paper_code
      const requiredVersion = paperInfo.version

      console.log(`[checkAnyPackageExists] 检查任意可用版本: paperId=${paperId}, paperCode=${paperCode}, 要求版本=${requiredVersion}`)

      // 获取所有可用的完整包版本（从数据库和文件系统）
      const availableVersions = new Set()

      // 1. 从数据库获取完整包版本
      const dbPackages = this.db.prepare(`
        SELECT DISTINCT version
        FROM paper_package
        WHERE paper_id = ? AND paper_code = ? AND is_active = 1
        ORDER BY version DESC
      `).all(paperId, paperCode)

      dbPackages.forEach(p => availableVersions.add(p.version))

      // 2. 从文件系统获取完整包版本
      const fullPackageFiles = fs.readdirSync(this.packageBasePath).filter(f =>
        f.startsWith(`${paperCode}_v`) && f.endsWith('.zip') && !f.includes('_quick')
      )

      fullPackageFiles.forEach(f => {
        const match = f.match(/_v(\d+)\.zip$/)
        if (match) {
          availableVersions.add(parseInt(match[1]))
        }
      })

      // 3. 按版本号从高到低排序
      const sortedVersions = Array.from(availableVersions).sort((a, b) => b - a)

      console.log(`[checkAnyPackageExists] 找到的完整包版本: ${sortedVersions.join(', ') || '无'}`)

      // 4. 检查每个版本，找到同时有快速包和完整包的版本
      for (const version of sortedVersions) {
        const fullPackageFileName = `${paperCode}_v${version}.zip`
        const quickPackageFileName = `${paperCode}_v${version}_quick.zip`
        const fullPackagePath = path.join(this.packageBasePath, fullPackageFileName)
        const quickPackagePath = path.join(this.packageBasePath, quickPackageFileName)

        const hasFullPackage = fs.existsSync(fullPackagePath)
        const hasQuickPackage = fs.existsSync(quickPackagePath)

        console.log(`[checkAnyPackageExists] 检查版本 v${version}: 完整包=${hasFullPackage}, 快速包=${hasQuickPackage}`)

        if (hasFullPackage && hasQuickPackage) {
          // 找到同时有快速包和完整包的版本
          console.log(`[checkAnyPackageExists] ✓ 找到可用版本 v${version}（快速包和完整包都存在）`)
          return {
            exists: true,
            availableVersion: version,
            requiredVersion: requiredVersion,
            isLatest: version === requiredVersion,
            paperCode: paperCode,
            fullPackagePath: fullPackagePath,
            quickPackagePath: quickPackagePath,
            hasQuickPackage: true,
            hasFullPackage: true
          }
        } else if (hasFullPackage) {
          // 只有完整包，没有快速包也可以用（完整包包含所有内容）
          console.log(`[checkAnyPackageExists] ✓ 找到可用版本 v${version}（仅有完整包，但可用）`)
          return {
            exists: true,
            availableVersion: version,
            requiredVersion: requiredVersion,
            isLatest: version === requiredVersion,
            paperCode: paperCode,
            fullPackagePath: fullPackagePath,
            quickPackagePath: null,
            hasQuickPackage: false,
            hasFullPackage: true
          }
        }
      }

      console.log(`[checkAnyPackageExists] 没有找到任何可用版本（快速包或完整包）`)
      return null
    } catch (error) {
      console.warn('检查任意ZIP包是否存在失败:', error)
      return null
    }
  }

  /**
   * 获取本地试卷版本（从paper_package表或本地文件系统）
   * 注意：不从paper表获取，因为paper表的版本可能已被更新为远程版本
   * @param {number} paperId - 试卷ID
   * @returns {number|null} 本地版本号，如果不存在则返回null
   */
  getLocalPaperVersion(paperId) {
    try {
      // 优先从paper_package表获取（最新激活的版本，对应实际存在的文件）
      const packageInfo = this.db.prepare(`
        SELECT paper_code, version, is_active
        FROM paper_package
        WHERE paper_id = ? AND is_active = 1
        ORDER BY version DESC
        LIMIT 1
      `).get(paperId)

      if (packageInfo && packageInfo.version && packageInfo.paper_code) {
        // 验证文件是否存在
        const packageFileName = `${packageInfo.paper_code}_v${packageInfo.version}.zip`
        const packagePath = path.join(this.packageBasePath, packageFileName)
        if (fs.existsSync(packagePath)) {
          return packageInfo.version
        }
      }

      // 如果paper_package表没有或文件不存在，扫描文件系统查找最高版本
      const paper = this.db.prepare(`
        SELECT paper_code
        FROM paper
        WHERE id = ?
        LIMIT 1
      `).get(paperId)

      if (paper && paper.paper_code) {
        // 扫描文件系统，查找该试卷的所有版本
        if (fs.existsSync(this.packageBasePath)) {
          const files = fs.readdirSync(this.packageBasePath)
          let maxVersion = 0

          for (const file of files) {
            // 匹配格式：{paperCode}_v{version}.zip 或 {paperCode}_v{version}_quick.zip
            const match = file.match(new RegExp(`^${paper.paper_code}_v(\\d+)(_quick)?\\.zip$`))
            if (match) {
              const version = parseInt(match[1], 10)
              if (version > maxVersion) {
                maxVersion = version
              }
            }
          }

          if (maxVersion > 0) {
            return maxVersion
          }
        }
      }

      return null
    } catch (error) {
      console.warn('获取本地试卷版本失败:', error)
      return null
    }
  }

  /**
   * 解压快速启动包（只解压manifest.json和trial_listen/、intro/等）
   * @param {string} paperCode - 试卷编码
   * @param {number} version - 版本号
   * @returns {Promise<Object>} 解压结果（包含manifest.json）
   */
  async extractQuickStartPackage(paperCode, version) {
    try {
      const quickStartFileName = `${paperCode}_v${version}_quick.zip`
      const quickStartPath = path.join(this.packageBasePath, quickStartFileName)

      if (!fs.existsSync(quickStartPath)) {
        throw new Error(`快速启动包不存在: ${quickStartPath}`)
      }

      // 解压到临时目录
      const tempDir = path.join(this.tempBasePath, paperCode)
      if (fs.existsSync(tempDir)) {
        // 只删除manifest.json和trial_listen/、trial_intro/、intro/等快速启动包相关文件
        const manifestPath = path.join(tempDir, 'manifest.json')
        const trialListenDir = path.join(tempDir, 'trial_listen')
        const trialIntroDir = path.join(tempDir, 'trial_intro')
        const introDir = path.join(tempDir, 'intro')
        const operateListenDir = path.join(tempDir, 'operate_listen')

        if (fs.existsSync(manifestPath)) fs.unlinkSync(manifestPath)
        if (fs.existsSync(trialListenDir)) fs.rmSync(trialListenDir, { recursive: true, force: true })
        if (fs.existsSync(trialIntroDir)) fs.rmSync(trialIntroDir, { recursive: true, force: true })
        if (fs.existsSync(introDir)) fs.rmSync(introDir, { recursive: true, force: true })
        if (fs.existsSync(operateListenDir)) fs.rmSync(operateListenDir, { recursive: true, force: true })
      } else {
        fs.mkdirSync(tempDir, { recursive: true })
      }

      // 读取快速启动包
      const zipData = fs.readFileSync(quickStartPath)
      await this.extractZip(zipData, tempDir)

      // 读取manifest.json
      const manifestPath = path.join(tempDir, 'manifest.json')
      if (!fs.existsSync(manifestPath)) {
        throw new Error('快速启动包格式不正确：缺少manifest.json')
      }

      // 优先从本地JSON文件读取，确保UTF-8编码正确处理中文
      let manifest
      try {
        // 方法1：直接使用 encoding 选项（推荐）
        manifest = JSON.parse(fs.readFileSync(manifestPath, { encoding: 'utf8' }))
      } catch (parseError) {
        // 方法2：如果方法1失败，使用Buffer方式
        console.warn('使用Buffer方式读取manifest.json:', parseError.message)
        const manifestBuffer = fs.readFileSync(manifestPath)
        // 尝试多种编码方式
        try {
          manifest = JSON.parse(manifestBuffer.toString('utf8'))
        } catch (utf8Error) {
          try {
            manifest = JSON.parse(manifestBuffer.toString('utf16le'))
          } catch (utf16Error) {
            manifest = JSON.parse(manifestBuffer.toString('latin1'))
          }
        }
      }

      // 验证数据是否正确解析
      if (!manifest || typeof manifest !== 'object') {
        throw new Error('manifest.json 解析失败或格式不正确')
      }

      console.log(`✓ manifest.json读取成功: 字段数=${Object.keys(manifest).length}`)

      // 提取快速启动包中的媒体文件到media目录
      const mediaDir = path.join(this.mediaBasePath, paperCode)
      if (!fs.existsSync(mediaDir)) {
        fs.mkdirSync(mediaDir, { recursive: true })
      }

      // 提取trial_intro/目录（试听旁白音频）
      const trialIntroSrc = path.join(tempDir, 'trial_intro')
      if (fs.existsSync(trialIntroSrc)) {
        const trialIntroDest = path.join(mediaDir, 'trial_intro')
        if (fs.existsSync(trialIntroDest)) {
          fs.rmSync(trialIntroDest, { recursive: true, force: true })
        }
        fs.mkdirSync(trialIntroDest, { recursive: true })
        const files = fs.readdirSync(trialIntroSrc)
        for (const file of files) {
          const srcFile = path.join(trialIntroSrc, file)
          const destFile = path.join(trialIntroDest, file)
          if (fs.statSync(srcFile).isFile()) {
            fs.copyFileSync(srcFile, destFile)
          }
        }
        console.log(`  ✓ 已提取 trial_intro/ 目录`)
      }

      // 提取trial_listen/目录（试听音频）
      const trialListenSrc = path.join(tempDir, 'trial_listen')
      if (fs.existsSync(trialListenSrc)) {
        const trialListenDest = path.join(mediaDir, 'trial_listen')
        if (fs.existsSync(trialListenDest)) {
          fs.rmSync(trialListenDest, { recursive: true, force: true })
        }
        fs.mkdirSync(trialListenDest, { recursive: true })
        const files = fs.readdirSync(trialListenSrc)
        for (const file of files) {
          const srcPath = path.join(trialListenSrc, file)
          const destPath = path.join(trialListenDest, file)
          if (fs.statSync(srcPath).isFile()) {
            // 检查源文件大小
            const srcStats = fs.statSync(srcPath)
            if (srcStats.size === 0) {
              console.warn(`⚠️ 警告：试听媒体源文件为空: ${srcPath}`)
            }

            // 复制文件
            fs.copyFileSync(srcPath, destPath)

            // 验证复制后的文件
            if (fs.existsSync(destPath)) {
              const destStats = fs.statSync(destPath)
              if (destStats.size === 0) {
                console.error(`❌ 错误：试听媒体文件复制后为空: ${destPath}`)
              } else if (destStats.size !== srcStats.size) {
                console.error(`❌ 错误：试听媒体文件大小不匹配: 源=${srcStats.size}, 目标=${destStats.size}`)
              } else {
                console.log(`✓ 提取试听媒体: ${destPath}, 大小: ${(destStats.size / 1024).toFixed(2)} KB`)
              }
            } else {
              console.error(`❌ 错误：试听媒体文件复制失败: ${destPath}`)
            }
          }
        }
      }

      // 提取intro/目录
      const introSrc = path.join(tempDir, 'intro')
      if (fs.existsSync(introSrc)) {
        const introDest = path.join(mediaDir, 'intro')
        if (fs.existsSync(introDest)) {
          fs.rmSync(introDest, { recursive: true, force: true })
        }
        fs.mkdirSync(introDest, { recursive: true })
        const files = fs.readdirSync(introSrc)
        for (const file of files) {
          const srcPath = path.join(introSrc, file)
          const destPath = path.join(introDest, file)
          if (fs.statSync(srcPath).isFile()) {
            fs.copyFileSync(srcPath, destPath)
          }
        }
      }

      // 提取operate_listen/目录
      const operateListenSrc = path.join(tempDir, 'operate_listen')
      if (fs.existsSync(operateListenSrc)) {
        const operateListenDest = path.join(mediaDir, 'operate_listen')
        if (fs.existsSync(operateListenDest)) {
          fs.rmSync(operateListenDest, { recursive: true, force: true })
        }
        fs.mkdirSync(operateListenDest, { recursive: true })
        const files = fs.readdirSync(operateListenSrc)
        for (const file of files) {
          const srcPath = path.join(operateListenSrc, file)
          const destPath = path.join(operateListenDest, file)
          if (fs.statSync(srcPath).isFile()) {
            fs.copyFileSync(srcPath, destPath)
          }
        }
      }

      // 提取volumes/目录（卷别名称音频）
      const volumesSrc = path.join(tempDir, 'volumes')
      if (fs.existsSync(volumesSrc)) {
        const volumesDest = path.join(mediaDir, 'volumes')
        if (fs.existsSync(volumesDest)) {
          fs.rmSync(volumesDest, { recursive: true, force: true })
        }
        fs.mkdirSync(volumesDest, { recursive: true })
        const files = fs.readdirSync(volumesSrc)
        for (const file of files) {
          const srcPath = path.join(volumesSrc, file)
          const destPath = path.join(volumesDest, file)
          if (fs.statSync(srcPath).isFile()) {
            fs.copyFileSync(srcPath, destPath)
            console.log(`✓ 提取卷别音频: ${destPath}`)
          }
        }
        console.log(`  ✓ 已提取 volumes/ 目录`)
      }

      // 提取sections/目录（大题说明音频）
      const sectionsSrc = path.join(tempDir, 'sections')
      if (fs.existsSync(sectionsSrc)) {
        console.log(`[Main Process] 📦 发现 sections/ 目录，开始提取大题说明音频`)
        const sectionsDest = path.join(mediaDir, 'sections')
        if (fs.existsSync(sectionsDest)) {
          fs.rmSync(sectionsDest, { recursive: true, force: true })
        }
        fs.mkdirSync(sectionsDest, { recursive: true })
        const files = fs.readdirSync(sectionsSrc)
        console.log(`[Main Process] 📦 sections/ 目录下有 ${files.length} 个文件`)
        for (const file of files) {
          const srcPath = path.join(sectionsSrc, file)
          const destPath = path.join(sectionsDest, file)
          if (fs.statSync(srcPath).isFile()) {
            fs.copyFileSync(srcPath, destPath)
            console.log(`[Main Process] ✓ 提取大题说明音频: ${destPath}`)
          }
        }
        console.log(`[Main Process] ✓ 已提取 sections/ 目录，共 ${files.length} 个文件`)
      } else {
        console.log(`[Main Process] ⚠️ sections/ 目录不存在: ${sectionsSrc}`)
      }

      // 更新本地paper表的基本信息（从manifest中获取）
      try {
        const existingPaper = this.db.prepare(`
          SELECT id FROM paper WHERE paper_code = ? LIMIT 1
        `).get(paperCode)

        if (existingPaper) {
          // 更新paper表记录（使用manifest中的信息）
          const updateStmt = this.db.prepare(`
            UPDATE paper 
            SET paper_name = ?, paper_desc = ?,
                total_score = ?, total_questions = ?, duration = ?,
                practice_limit = ?, trial_listen_enabled = ?, trial_listen_text = ?,
                notes = ?, notes_display_mode = ?,
                update_time = ?
            WHERE paper_code = ?
          `)
          updateStmt.run(
            manifest.paperName || paperCode,
            manifest.paperDesc || null,
            manifest.totalScore || 0,
            manifest.totalQuestions || 0,
            manifest.duration || null,
            manifest.practiceLimit || 0,
            manifest.trialListenEnabled ? 1 : 0,
            manifest.trialListenText || null,
            manifest.notes || null,
            manifest.notesDisplayMode || 'before_exam',
            Date.now(),
            paperCode
          )
          console.log(`✓ 已从快速启动包的manifest.json更新paper表信息: ${paperCode}`)
        }
      } catch (error) {
        console.warn(`更新paper表信息失败（从快速启动包）: ${error.message}`)
      }

      console.log(`✓ 快速启动包解压完成: ${paperCode}`)

      // 处理所有文本字段，确保UTF-8编码正确（避免中文乱码）
      const processTextFields = (obj) => {
        if (obj === null || obj === undefined) return obj
        if (typeof obj === 'string') {
          return String(obj)
        }
        if (Array.isArray(obj)) {
          return obj.map(item => processTextFields(item))
        }
        if (typeof obj === 'object') {
          const processed = {}
          for (const key in obj) {
            if (obj.hasOwnProperty(key)) {
              const value = obj[key]
              if (typeof value === 'string') {
                processed[key] = String(value)
              } else if (Array.isArray(value)) {
                processed[key] = processTextFields(value)
              } else if (typeof value === 'object' && value !== null) {
                processed[key] = processTextFields(value)
              } else {
                processed[key] = value
              }
            }
          }
          return processed
        }
        return obj
      }

      const processedManifest = processTextFields(manifest)

      return {
        manifest: processedManifest,
        mediaDir
      }
    } catch (error) {
      console.error(`解压快速启动包失败: ${error.message}`)
      throw error
    }
  }

  /**
   * 解压试卷包
   * @param {string} paperCode - 试卷编码
   * @param {number} version - 版本号
   * @returns {Promise<Object>} 解压结果
   */
  async extractPaperPackage(paperCode, version) {
    try {
      // 1. 首先确定实际可用的版本（可能降级到较低版本）
      // 查询实际可用的ZIP包版本
      let actualVersion = version
      let packageRecord = this.db.prepare(`
        SELECT id, version, package_path, storage_type, package_data
        FROM paper_package
        WHERE paper_code = ? AND version = ? AND is_active = 1
        LIMIT 1
      `).get(paperCode, version)

      if (!packageRecord) {
        // 指定版本不存在，尝试获取最新可用版本
        packageRecord = this.db.prepare(`
          SELECT id, version, package_path, storage_type, package_data
          FROM paper_package
          WHERE paper_code = ? AND is_active = 1
          ORDER BY version DESC
          LIMIT 1
        `).get(paperCode)

        if (packageRecord) {
          actualVersion = packageRecord.version
          console.log(`⚠️ 指定版本 ${version} 不存在，使用可用版本: ${actualVersion}`)
        } else {
          // 数据库中没有记录，尝试从文件系统查找
          const zipFileName = `${paperCode}_v${version}.zip`
          const zipFilePath = path.join(this.packageBasePath, zipFileName)

          if (fs.existsSync(zipFilePath)) {
            console.log(`✓ 从文件系统找到ZIP包: ${zipFileName}`)
            // 文件存在，继续使用请求的版本
          } else {
            // 尝试查找其他版本的文件
            const files = fs.readdirSync(this.packageBasePath)
            const matchingFiles = files.filter(f => f.startsWith(`${paperCode}_v`) && f.endsWith('.zip') && !f.includes('_quick'))

            if (matchingFiles.length > 0) {
              // 按版本号排序，取最新的
              matchingFiles.sort((a, b) => {
                const vA = parseInt(a.match(/_v(\d+)\.zip$/)?.[1] || '0')
                const vB = parseInt(b.match(/_v(\d+)\.zip$/)?.[1] || '0')
                return vB - vA
              })
              const latestFile = matchingFiles[0]
              actualVersion = parseInt(latestFile.match(/_v(\d+)\.zip$/)?.[1] || version)
              console.log(`⚠️ 从文件系统找到其他版本: ${latestFile}，版本号: ${actualVersion}`)
            } else {
              throw new Error(`ZIP包不存在: ${paperCode} (版本 ${version})，数据库和文件系统中都没有找到`)
            }
          }
        }
      }

      // 检查缓存（使用实际版本号）
      const cacheKey = `${paperCode}_v${actualVersion}`
      if (this.extractCache.has(cacheKey)) {
        const cached = this.extractCache.get(cacheKey)
        console.log(`使用缓存的解压结果: ${paperCode} v${actualVersion}`)
        // 更新缓存时间戳（LRU）
        this.extractCache.delete(cacheKey)
        this.extractCache.set(cacheKey, cached)
        return cached.data
      }

      // 2. 获取ZIP包数据（使用实际版本号）
      const zipData = await this.getPaperPackageData(paperCode, actualVersion)
      if (!zipData) {
        throw new Error(`ZIP包不存在: ${paperCode} (版本 ${actualVersion})`)
      }

      // 记录实际使用的 ZIP 数据来源
      let zipDataSource = null // { filePath, version, fromFileSystem, needsWrite }

      if (!packageRecord || packageRecord.version !== actualVersion) {
        // 重新查询实际版本的记录
        packageRecord = this.db.prepare(`
          SELECT id, version, package_path, storage_type
          FROM paper_package
          WHERE paper_code = ? AND version = ? AND is_active = 1
          LIMIT 1
        `).get(paperCode, actualVersion)
      }

      if (!packageRecord) {
        // paper_package 表中没有记录，需要从文件系统查找并记录文件路径
        const zipFileName = `${paperCode}_v${actualVersion}.zip`
        const zipFilePath = path.join(this.packageBasePath, zipFileName)

        if (fs.existsSync(zipFilePath)) {
          zipDataSource = {
            filePath: zipFilePath,
            version: actualVersion,
            fromFileSystem: true,
            needsWrite: true
          }
          console.log(`✓ 检测到 ZIP 文件来自文件系统，解压后将自动写入 paper_package 表: ${zipFileName}`)
        } else {
          zipDataSource = {
            filePath: null,
            version: actualVersion,
            fromFileSystem: false,
            needsWrite: false
          }
        }
      } else {
        // paper_package 表中已有记录
        zipDataSource = {
          filePath: packageRecord.package_path ? path.join(app.getPath('userData'), packageRecord.package_path) : null,
          version: packageRecord.version,
          fromFileSystem: packageRecord.storage_type === 1,
          needsWrite: false
        }
      }

      // 更新 version 为实际使用的版本
      const version_to_use = actualVersion

      // 2. 解压到临时目录
      const tempDir = path.join(this.tempBasePath, paperCode)
      if (fs.existsSync(tempDir)) {
        fs.rmSync(tempDir, { recursive: true, force: true })
      }
      fs.mkdirSync(tempDir, { recursive: true })

      await this.extractZip(zipData, tempDir)

      // 3. 读取manifest.json和questions.json
      const manifestPath = path.join(tempDir, 'manifest.json')
      const questionsPath = path.join(tempDir, 'questions.json')

      if (!fs.existsSync(manifestPath) || !fs.existsSync(questionsPath)) {
        throw new Error('ZIP包格式不正确：缺少manifest.json或questions.json')
      }

      // 优先从本地JSON文件读取，确保UTF-8编码正确处理中文
      // 使用 readFileSync 的 encoding 选项，确保UTF-8编码
      let manifest, questions
      try {
        // 方法1：直接使用 encoding 选项（推荐）
        manifest = JSON.parse(fs.readFileSync(manifestPath, { encoding: 'utf8' }))
        questions = JSON.parse(fs.readFileSync(questionsPath, { encoding: 'utf8' }))
      } catch (parseError) {
        // 方法2：如果方法1失败，使用Buffer方式
        console.warn('使用Buffer方式读取JSON文件:', parseError.message)
        const manifestBuffer = fs.readFileSync(manifestPath)
        const questionsBuffer = fs.readFileSync(questionsPath)
        // 尝试多种编码方式
        try {
          manifest = JSON.parse(manifestBuffer.toString('utf8'))
          questions = JSON.parse(questionsBuffer.toString('utf8'))
        } catch (utf8Error) {
          // 如果UTF-8失败，尝试其他编码
          try {
            manifest = JSON.parse(manifestBuffer.toString('utf16le'))
            questions = JSON.parse(questionsBuffer.toString('utf16le'))
          } catch (utf16Error) {
            // 最后尝试latin1（单字节编码，不会损坏数据）
            manifest = JSON.parse(manifestBuffer.toString('latin1'))
            questions = JSON.parse(questionsBuffer.toString('latin1'))
          }
        }
      }

      // 验证数据是否正确解析
      if (!manifest || typeof manifest !== 'object') {
        throw new Error('manifest.json 解析失败或格式不正确')
      }
      if (!Array.isArray(questions)) {
        throw new Error('questions.json 解析失败或格式不正确（应为数组）')
      }

      console.log(`✓ JSON文件读取成功: manifest字段数=${Object.keys(manifest).length}, questions数量=${questions.length}`)

      // 4. 更新本地paper表的完整信息（从manifest中获取的信息）
      // 注意：manifest.json不包含year, month, province, custom_name，这些字段需要从服务器同步时获取
      // 但我们可以更新其他字段（paper_name, total_score, total_questions, duration等）
      try {
        const existingPaper = this.db.prepare(`
          SELECT id FROM paper WHERE paper_code = ? LIMIT 1
        `).get(paperCode)

        if (existingPaper) {
          // 更新paper表记录（使用manifest中的信息，包括新字段）
          const updateStmt = this.db.prepare(`
            UPDATE paper 
            SET paper_name = ?, paper_desc = ?,
                total_score = ?, total_questions = ?, duration = ?,
                practice_limit = ?, trial_listen_enabled = ?, trial_listen_text = ?,
                notes = ?, notes_display_mode = ?,
                update_time = ?
            WHERE paper_code = ?
          `)
          updateStmt.run(
            manifest.paperName || paperCode,
            manifest.paperDesc || null,
            manifest.totalScore || 0,
            manifest.totalQuestions || 0,
            manifest.duration || null,
            manifest.practiceLimit || 0,
            manifest.trialListenEnabled ? 1 : 0,
            manifest.trialListenText || null,
            manifest.notes || null,
            manifest.notesDisplayMode || 'before_exam',
            Date.now(),
            paperCode
          )
          console.log(`✓ 已从manifest.json更新paper表信息: ${paperCode}`)
        } else {
          // 如果本地没有记录，至少插入基本信息（year, month, province等字段为null，需要从服务器同步时补充）
          console.log(`本地paper表中没有记录，从manifest.json插入基本信息: ${paperCode}`)
          const insertStmt = this.db.prepare(`
            INSERT INTO paper 
            (id, paper_name, paper_code, paper_desc,
             total_score, total_questions, duration,
             practice_limit, trial_listen_enabled, trial_listen_text,
             notes, notes_display_mode,
             version, status, create_time)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
          `)
          insertStmt.run(
            manifest.paperId || null,
            manifest.paperName || paperCode,
            paperCode,
            manifest.paperDesc || null,
            manifest.totalScore || 0,
            manifest.totalQuestions || 0,
            manifest.duration || null,
            manifest.practiceLimit || 0,
            manifest.trialListenEnabled ? 1 : 0,
            manifest.trialListenText || null,
            manifest.notes || null,
            manifest.notesDisplayMode || 'before_exam',
            manifest.version || 1,
            1, // status = 1 (启用)
            Date.now()
          )
          console.log(`✓ 已从manifest.json插入paper表基本信息: ${paperCode} (注意：year, month, province字段需要从服务器同步时补充)`)
        }
      } catch (error) {
        console.warn(`更新paper表信息失败（从manifest.json）: ${error.message}`)
        // 不抛出错误，继续执行后续步骤
      }

      // 4.1. 保存卷别数据到 paper_volume 表
      if (manifest.volumes && Array.isArray(manifest.volumes)) {
        try {
          const paper = this.db.prepare(`SELECT id FROM paper WHERE paper_code = ? LIMIT 1`).get(paperCode)
          if (paper && paper.id) {
            // 先删除旧的卷别数据
            this.db.prepare(`DELETE FROM paper_volume WHERE paper_id = ?`).run(paper.id)

            // 插入新的卷别数据（如果 manifest 中有 id，使用它；否则让数据库自动生成）
            const volumeStmt = this.db.prepare(`
              INSERT INTO paper_volume 
              (id, paper_id, volume_code, volume_name, volume_order, create_time)
              VALUES (?, ?, ?, ?, ?, ?)
            `)

            for (const volume of manifest.volumes) {
              // 如果 manifest 中有 id，使用它；否则传入 null 让数据库自动生成
              const volumeId = volume.id || null
              volumeStmt.run(
                volumeId,
                paper.id,
                volume.volumeCode,
                volume.volumeName,
                volume.volumeOrder || 1,
                Date.now()
              )
            }
            console.log(`✓ 已保存 ${manifest.volumes.length} 个卷别到 paper_volume 表`)
          }
        } catch (error) {
          console.warn(`保存卷别数据失败: ${error.message}`)
        }
      }

      // 4.2. 保存大题数据到 paper_section 表
      if (manifest.sections && Array.isArray(manifest.sections)) {
        try {
          const paper = this.db.prepare(`SELECT id FROM paper WHERE paper_code = ? LIMIT 1`).get(paperCode)
          if (paper && paper.id) {
            // 先删除旧的大题数据
            this.db.prepare(`DELETE FROM paper_section WHERE paper_id = ?`).run(paper.id)

            // 插入新的大题数据（必须包含 volume_id）
            // 如果 manifest 中有 id，使用它；否则让数据库自动生成
            const sectionStmt = this.db.prepare(`
              INSERT INTO paper_section 
              (id, paper_id, volume_id, volume_code, section_name, section_order, 
               question_count, total_score, score_per_question, instruction_text, audio_play_count, answer_time, create_time)
              VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            `)

            for (const section of manifest.sections) {
              // 优先使用 manifest 中的 volumeId（与 volumes 的 id 匹配）
              let volumeId = section.volumeId || null
              let volumeCode = section.volumeCode || ''

              // 如果 manifest 中没有 volumeId，尝试根据 volumeCode 查找（兼容旧数据）
              if (!volumeId && volumeCode && volumeCode.trim() !== '') {
                const volumeRecord = this.db.prepare(`
                  SELECT id FROM paper_volume WHERE paper_id = ? AND volume_code = ? LIMIT 1
                `).get(paper.id, volumeCode)

                if (volumeRecord) {
                  volumeId = volumeRecord.id
                }
              }

              // 如果仍然没有 volumeId，尝试使用第一个卷别（兼容处理）
              if (!volumeId) {
                const firstVolume = this.db.prepare(`
                  SELECT id, volume_code FROM paper_volume WHERE paper_id = ? ORDER BY volume_order ASC LIMIT 1
                `).get(paper.id)

                if (firstVolume) {
                  volumeId = firstVolume.id
                  volumeCode = firstVolume.volume_code
                  console.warn(`⚠️ 大题 ${section.sectionName} 的 volumeId 和 volumeCode 都无效，使用第一个卷别: id=${volumeId}, code=${volumeCode}`)
                } else {
                  console.warn(`⚠️ 找不到卷别，且试卷没有卷别数据，跳过大题: ${section.sectionName}`)
                  continue
                }
              }

              // 如果 volumeCode 为空，尝试从 paper_volume 表中获取
              if (!volumeCode || volumeCode.trim() === '') {
                const volumeRecord = this.db.prepare(`
                  SELECT volume_code FROM paper_volume WHERE id = ? LIMIT 1
                `).get(volumeId)

                if (volumeRecord) {
                  volumeCode = volumeRecord.volume_code
                }
              }

              // 如果 manifest 中有 id，使用它；否则传入 null 让数据库自动生成
              const sectionId = section.id || section.sectionId || null
              console.log(`[extractPaperPackage] 保存大题: id=${sectionId}, sectionName=${section.sectionName}, volumeId=${volumeId}, volumeCode=${volumeCode}`)

              sectionStmt.run(
                sectionId, // id（使用 manifest 中的原始 section id）
                paper.id,
                volumeId, // volume_id（与 volumes 的 id 匹配）
                volumeCode || '', // volume_code (保留用于显示)
                section.sectionName,
                section.sectionOrder || 1,
                section.questionCount || 0,
                section.totalScore || 0,
                section.scorePerQuestion || 0,
                section.instructionText || null,
                section.audioPlayCount || 1,
                section.answerTime || 5,
                Date.now()
              )
            }
            console.log(`✓ 已保存 ${manifest.sections.length} 个大题到 paper_section 表`)
          }
        } catch (error) {
          console.warn(`保存大题数据失败: ${error.message}`)
        }
      }

      // 4.3. 保存题目组数据到 paper_question_group 表 (New)
      if (manifest.sections && Array.isArray(manifest.sections)) {
        try {
          const paper = this.db.prepare(`SELECT id FROM paper WHERE paper_code = ? LIMIT 1`).get(paperCode)
          if (paper && paper.id) {
            // 清理旧数据：删除该试卷所有大题下的题目组
            // 注意：由于 paper_section 刚刚被重建，如果ID保持一致，这里可以正确关联
            this.db.prepare(`DELETE FROM paper_question_group WHERE section_id IN (SELECT id FROM paper_section WHERE paper_id = ?)`).run(paper.id)

            const groupStmt = this.db.prepare(`
               INSERT INTO paper_question_group
               (id, section_id, group_name, question_group_id, group_order, start_question_num, end_question_num, audio_url, audio_path, audio_duration, intro_text, answer_time, selected_question_ids, create_time)
               VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
             `)

            let groupCount = 0
            for (const section of manifest.sections) {
              if (section.questionGroups && Array.isArray(section.questionGroups)) {
                for (const group of section.questionGroups) {
                  // Java后端生成的JSON中，Group对象可能不包含sectionId（因为是嵌套关系），所以优先使用父级section的ID
                  const sectionId = section.id || section.sectionId || group.sectionId || group.section_id

                  // 处理 selected_question_ids: JSON中是 'questions' 数组
                  let selectedIdsStr = null
                  if (Array.isArray(group.questions)) {
                    selectedIdsStr = JSON.stringify(group.questions)
                  } else if (group.selectedQuestionIds) {
                    selectedIdsStr = group.selectedQuestionIds
                  }

                  groupStmt.run(
                    group.id || null,
                    sectionId,
                    group.groupName || group.group_name,
                    group.questionGroupId || group.question_group_id || null,
                    group.groupOrder || group.group_order || 0,
                    group.startQuestionNum || group.start_question_num || null,
                    group.endQuestionNum || group.end_question_num || null,
                    group.audioUrl || group.audio_url || null,
                    group.audioPath || group.audio_path || null,
                    group.audioDuration || group.audio_duration || 0,
                    group.introText || group.intro_text || null,
                    group.answerTime || group.answer_time || null,
                    selectedIdsStr,
                    Date.now()
                  )
                  groupCount++
                }
              }
            }

            if (groupCount > 0) {
              console.log(`✓ 已保存 ${groupCount} 个题目组到 paper_question_group 表`)
            }
          }
        } catch (error) {
          console.warn(`保存题目组数据失败: ${error.message}`)
        }
      }

      // 4.2.1. 保存题目关联数据到 paper_question 表（从 questions.json 提取）
      if (questions && Array.isArray(questions) && questions.length > 0) {
        try {
          const paper = this.db.prepare(`SELECT id FROM paper WHERE paper_code = ? LIMIT 1`).get(paperCode)
          if (paper && paper.id) {
            // 先删除旧的题目关联数据
            this.db.prepare(`DELETE FROM paper_question WHERE paper_id = ?`).run(paper.id)

            // 插入新的题目关联数据
            const questionStmt = this.db.prepare(`
              INSERT OR REPLACE INTO paper_question 
              (paper_id, question_id, section_id, section_order, sort_order, score, create_time)
              VALUES (?, ?, ?, ?, ?, ?, ?)
            `)

            let savedCount = 0
            for (const question of questions) {
              const questionId = question.id || question.question_id || question.questionId
              if (!questionId) {
                console.warn(`⚠️ 题目缺少ID，跳过: title=${question.title?.substring(0, 30)}...`)
                continue
              }

              // 获取题目所属的大题ID
              const sectionId = question.section_id || question.sectionId || null
              const sectionOrder = question.section_order || question.sectionOrder || 0
              const sortOrder = question.sort_order || question.question_sort || question.sortOrder || 0
              const score = question.score || 0

              questionStmt.run(
                paper.id,
                questionId,
                sectionId,
                sectionOrder,
                sortOrder,
                score,
                Date.now()
              )
              savedCount++
            }
            console.log(`✓ 已保存 ${savedCount} 个题目关联到 paper_question 表`)
          }
        } catch (error) {
          console.warn(`保存题目关联数据失败: ${error.message}`)
        }
      }

      // 4.3. 保存中场配置数据到 paper_intermission 表
      if (manifest.intermissions && Array.isArray(manifest.intermissions)) {
        try {
          const paper = this.db.prepare(`SELECT id FROM paper WHERE paper_code = ? LIMIT 1`).get(paperCode)
          if (paper && paper.id) {
            // 先删除旧的中场配置数据
            this.db.prepare(`DELETE FROM paper_intermission WHERE paper_id = ?`).run(paper.id)

            // 插入新的中场配置数据
            const intermissionStmt = this.db.prepare(`
              INSERT INTO paper_intermission 
              (paper_id, from_volume, to_volume, intermission_text, intermission_audio_url, intermission_audio_path, intermission_audio_duration, can_skip, create_time)
              VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            `)

            for (const intermission of manifest.intermissions) {
              intermissionStmt.run(
                paper.id,
                intermission.fromVolume,
                intermission.toVolume,
                intermission.intermissionText || null,
                intermission.intermissionAudioUrl || null,
                intermission.intermissionAudioPath || null,
                intermission.intermissionAudioDuration || null,
                intermission.canSkip ? 1 : 0,
                Date.now()
              )
            }
            console.log(`✓ 已保存 ${manifest.intermissions.length} 个中场配置到 paper_intermission 表`)
          }
        } catch (error) {
          console.warn(`保存中场配置数据失败: ${error.message}`)
        }
      }

      // 5. 提取媒体文件到media目录
      const mediaDir = path.join(this.mediaBasePath, paperCode)
      await this.extractMediaFiles(tempDir, mediaDir, questions, manifest)

      // 6. 保存媒体文件索引到SQLite
      await this.saveMediaIndex(paperCode, questions, manifest)

      // 7. 清理临时目录
      fs.rmSync(tempDir, { recursive: true, force: true })

      // 8. 处理所有文本字段，确保UTF-8编码正确（避免中文乱码）
      const processTextFields = (obj) => {
        if (obj === null || obj === undefined) return obj
        if (typeof obj === 'string') {
          // 确保字符串是UTF-8编码
          return String(obj)
        }
        if (Array.isArray(obj)) {
          return obj.map(item => processTextFields(item))
        }
        if (typeof obj === 'object') {
          const processed = {}
          for (const key in obj) {
            if (obj.hasOwnProperty(key)) {
              const value = obj[key]
              // 对于文本字段，确保是字符串类型
              if (typeof value === 'string') {
                processed[key] = String(value)
              } else if (Array.isArray(value)) {
                processed[key] = processTextFields(value)
              } else if (typeof value === 'object' && value !== null) {
                processed[key] = processTextFields(value)
              } else {
                processed[key] = value
              }
            }
          }
          return processed
        }
        return obj
      }

      // 处理 manifest 和 questions 中的所有文本字段
      const processedManifest = processTextFields(manifest)
      const processedQuestions = processTextFields(questions)

      const result = {
        success: true,
        manifest: processedManifest,
        questions: processedQuestions,
        mediaDir
      }

      // 8. 缓存解压结果
      this.addToCache(cacheKey, result)

      // 9. 如果 ZIP 文件来自文件系统且 paper_package 表中没有记录，自动写入 paper_package 表
      if (zipDataSource && zipDataSource.needsWrite && zipDataSource.fromFileSystem && zipDataSource.filePath) {
        try {
          console.log(`开始自动写入 paper_package 表: paperCode=${paperCode}, version=${version}`)

          // 获取 paper_id
          const paperRecord = this.db.prepare(`
            SELECT id FROM paper WHERE paper_code = ? LIMIT 1
          `).get(paperCode)

          if (!paperRecord || !paperRecord.id) {
            console.warn(`无法写入 paper_package 表：paper 表中没有找到 paper_code=${paperCode} 的记录`)
          } else {
            const paperId = paperRecord.id
            const actualVersion = zipDataSource.version // 使用实际找到的版本号

            // 读取文件并计算 hash
            const fileStats = fs.statSync(zipDataSource.filePath)
            const fileBuffer = fs.readFileSync(zipDataSource.filePath)
            const packageHash = this.calculateHash(fileBuffer)
            const packageSize = fileStats.size

            // 计算相对路径
            const relativePath = path.relative(app.getPath('userData'), zipDataSource.filePath)
            const syncTime = Date.now()

            // 先禁用旧版本的记录（如果有）
            this.db.prepare(`
              UPDATE paper_package 
              SET is_active = 0 
              WHERE paper_id = ? AND paper_code = ? AND is_active = 1
            `).run(paperId, paperCode)

            // 检查是否已存在相同版本的记录
            const existingRecord = this.db.prepare(`
              SELECT id FROM paper_package
              WHERE paper_id = ? AND paper_code = ? AND version = ?
            `).get(paperId, paperCode, actualVersion)

            if (existingRecord) {
              // 更新现有记录
              this.db.prepare(`
                UPDATE paper_package
                SET package_path = ?, package_hash = ?, package_size = ?,
                    storage_type = 1, sync_time = ?, is_active = 1
                WHERE id = ?
              `).run(
                relativePath,
                packageHash,
                packageSize,
                syncTime,
                existingRecord.id
              )
              console.log(`✓ 已更新完整包到 paper_package 表: paper_id=${paperId}, paper_code=${paperCode}, version=${actualVersion}`)
            } else {
              // 插入新记录
              try {
                this.db.prepare(`
                  INSERT INTO paper_package 
                  (paper_id, paper_code, package_path, package_hash, package_size, 
                   storage_type, version, sync_time, is_active)
                  VALUES (?, ?, ?, ?, ?, 1, ?, ?, 1)
                `).run(
                  paperId,
                  paperCode,
                  relativePath,
                  packageHash,
                  packageSize,
                  actualVersion, // 使用实际找到的版本号
                  syncTime
                )
                console.log(`✓ 已自动插入完整包到 paper_package 表: paper_id=${paperId}, paper_code=${paperCode}, version=${actualVersion}`)
              } catch (insertError) {
                // 如果插入失败（可能是 UNIQUE 约束），先删除旧记录再插入
                if (insertError.message && insertError.message.includes('UNIQUE')) {
                  console.warn(`插入失败（UNIQUE约束），先删除旧记录: ${insertError.message}`)
                  this.db.prepare(`
                    DELETE FROM paper_package
                    WHERE paper_id = ? AND paper_code = ?
                  `).run(paperId, paperCode)

                  // 重新插入
                  this.db.prepare(`
                    INSERT INTO paper_package 
                    (paper_id, paper_code, package_path, package_hash, package_size, 
                     storage_type, version, sync_time, is_active)
                    VALUES (?, ?, ?, ?, ?, 1, ?, ?, 1)
                  `).run(
                    paperId,
                    paperCode,
                    relativePath,
                    packageHash,
                    packageSize,
                    actualVersion, // 使用实际找到的版本号
                    syncTime
                  )
                  console.log(`✓ 已自动插入完整包到 paper_package 表（删除旧记录后）: paper_id=${paperId}, paper_code=${paperCode}, version=${actualVersion}`)
                } else {
                  throw insertError
                }
              }
            }

            // 验证记录是否已保存到 paper_package 表
            const verifyRecord = this.db.prepare(`
              SELECT id, paper_code, version, is_active FROM paper_package
              WHERE paper_id = ? AND paper_code = ? AND version = ?
            `).get(paperId, paperCode, actualVersion)

            if (verifyRecord) {
              console.log(`✓ 验证：完整包已自动保存到 paper_package 表，id=${verifyRecord.id}, paper_code=${verifyRecord.paper_code}, version=${verifyRecord.version}, is_active=${verifyRecord.is_active}`)
            } else {
              console.error(`❌ 错误：完整包自动保存后验证失败，paper_package 表中未找到记录！`)
              console.error(`   paper_id=${paperId}, paper_code=${paperCode}, version=${actualVersion}`)
            }
          }
        } catch (error) {
          console.error(`自动写入 paper_package 表失败:`, error)
          console.error(`paper_code=${paperCode}, version=${version}`)
          console.error(`错误详情:`, error.message)
          // 不抛出错误，因为解压已经成功，只是数据库记录失败
        }
      }

      console.log(`✓ ZIP包解压完成: ${paperCode}`)
      console.log(`✓ 已处理文本字段编码，manifest字段数: ${Object.keys(processedManifest || {}).length}, questions数量: ${processedQuestions?.length || 0}`)
      return result
    } catch (error) {
      console.error(`解压ZIP包失败: ${paperCode}`, error)
      throw error
    }
  }

  /**
   * 获取ZIP包数据（从SQLite或文件系统）
   * @param {string} paperCode - 试卷编码
   * @param {number} version - 指定的版本号（如果提供，优先获取该版本；否则获取最新版本）
   */
  async getPaperPackageData(paperCode, version = null) {
    try {
      let packageInfo

      if (version) {
        // 优先查找指定版本的ZIP包
        packageInfo = this.db.prepare(`
          SELECT storage_type, package_data, package_path, version
          FROM paper_package
          WHERE paper_code = ? AND version = ? AND is_active = 1
          LIMIT 1
        `).get(paperCode, version)

        if (packageInfo) {
          console.log(`✓ 找到指定版本的ZIP包(数据库): paper_code=${paperCode}, version=${version}`)
        } else {
          // 数据库中不存在，尝试从文件系统查找
          const zipFileName = `${paperCode}_v${version}.zip`
          const zipFilePath = path.join(this.packageBasePath, zipFileName)

          if (fs.existsSync(zipFilePath)) {
            console.log(`✓ 找到指定版本的ZIP包(文件系统): ${zipFileName}`)
            // 直接从文件系统读取
            const zipData = fs.readFileSync(zipFilePath)
            console.log(`✓ 从文件系统读取ZIP包成功，大小: ${(zipData.length / 1024 / 1024).toFixed(2)}MB`)

            // 自动将文件系统中的ZIP包写入数据库
            await this.registerFileSystemPackage(paperCode, version, zipFilePath, zipData)

            return zipData
          }

          // 文件系统也不存在指定版本，尝试获取最新版本（向下兼容）
          console.warn(`⚠️ 指定版本 ${version} 的ZIP包不存在（数据库和文件系统都没有），尝试获取最新版本`)
          packageInfo = this.db.prepare(`
            SELECT storage_type, package_data, package_path, version
            FROM paper_package
            WHERE paper_code = ? AND is_active = 1
            ORDER BY version DESC
            LIMIT 1
          `).get(paperCode)

          if (packageInfo) {
            console.warn(`⚠️ 使用最新可用版本: ${packageInfo.version}（请求版本: ${version}）`)
          } else {
            // 数据库中没有任何版本，尝试从文件系统查找最新版本
            const files = fs.readdirSync(this.packageBasePath).filter(f =>
              f.startsWith(`${paperCode}_v`) && f.endsWith('.zip') && !f.includes('_quick')
            )

            if (files.length > 0) {
              // 按版本号排序，取最新的
              files.sort((a, b) => {
                const vA = parseInt(a.match(/_v(\d+)\.zip$/)?.[1] || '0')
                const vB = parseInt(b.match(/_v(\d+)\.zip$/)?.[1] || '0')
                return vB - vA
              })

              const latestFile = files[0]
              const latestVersion = parseInt(latestFile.match(/_v(\d+)\.zip$/)?.[1] || '0')
              const latestFilePath = path.join(this.packageBasePath, latestFile)

              console.warn(`⚠️ 从文件系统找到最新版本: ${latestFile}，版本号: ${latestVersion}`)
              const zipData = fs.readFileSync(latestFilePath)
              console.log(`✓ 从文件系统读取ZIP包成功，大小: ${(zipData.length / 1024 / 1024).toFixed(2)}MB`)

              // 自动将文件系统中的ZIP包写入数据库
              await this.registerFileSystemPackage(paperCode, latestVersion, latestFilePath, zipData)

              return zipData
            }
          }
        }
      } else {
        // 未指定版本，获取最新版本
        packageInfo = this.db.prepare(`
          SELECT storage_type, package_data, package_path, version
          FROM paper_package
          WHERE paper_code = ? AND is_active = 1
          ORDER BY version DESC
          LIMIT 1
        `).get(paperCode)

        if (packageInfo) {
          console.log(`✓ 找到最新版本的ZIP包: paper_code=${paperCode}, version=${packageInfo.version}`)
        } else {
          // 数据库中没有，尝试从文件系统查找
          const files = fs.readdirSync(this.packageBasePath).filter(f =>
            f.startsWith(`${paperCode}_v`) && f.endsWith('.zip') && !f.includes('_quick')
          )

          if (files.length > 0) {
            files.sort((a, b) => {
              const vA = parseInt(a.match(/_v(\d+)\.zip$/)?.[1] || '0')
              const vB = parseInt(b.match(/_v(\d+)\.zip$/)?.[1] || '0')
              return vB - vA
            })

            const latestFile = files[0]
            const latestVersion = parseInt(latestFile.match(/_v(\d+)\.zip$/)?.[1] || '0')
            const latestFilePath = path.join(this.packageBasePath, latestFile)

            console.log(`✓ 从文件系统找到最新版本: ${latestFile}，版本号: ${latestVersion}`)
            const zipData = fs.readFileSync(latestFilePath)
            console.log(`✓ 从文件系统读取ZIP包成功，大小: ${(zipData.length / 1024 / 1024).toFixed(2)}MB`)

            // 自动将文件系统中的ZIP包写入数据库
            await this.registerFileSystemPackage(paperCode, latestVersion, latestFilePath, zipData)

            return zipData
          }
        }
      }

      if (!packageInfo) {
        console.error(`❌ 未找到ZIP包: paper_code=${paperCode}, version=${version || '(最新)'}`)
        return null
      }

      if (packageInfo.storage_type === 0) {
        // BLOB存储
        return packageInfo.package_data
      } else {
        // 文件系统存储
        const absolutePath = path.join(app.getPath('userData'), packageInfo.package_path)
        if (fs.existsSync(absolutePath)) {
          return fs.readFileSync(absolutePath)
        }
        console.error(`❌ ZIP文件不存在: ${absolutePath}`)
        return null
      }
    } catch (error) {
      console.error('获取ZIP包数据失败:', error.message)
      return null
    }
  }

  /**
   * 将文件系统中的ZIP包注册到数据库
   * @param {string} paperCode - 试卷编码
   * @param {number} version - 版本号
   * @param {string} filePath - 文件路径
   * @param {Buffer} zipData - ZIP数据
   */
  async registerFileSystemPackage(paperCode, version, filePath, zipData) {
    try {
      // 获取paper_id
      const paper = this.db.prepare(`
        SELECT id FROM paper WHERE paper_code = ? LIMIT 1
      `).get(paperCode)

      if (!paper) {
        console.warn(`⚠️ 无法注册ZIP包到数据库：paper表中没有找到试卷记录: ${paperCode}`)
        return
      }

      // 计算hash
      const hash = this.calculateHash(zipData)

      // 检查是否已存在
      const existing = this.db.prepare(`
        SELECT id FROM paper_package WHERE paper_code = ? AND version = ?
      `).get(paperCode, version)

      const syncTime = Date.now()
      const relativePath = `paper_packages/${path.basename(filePath)}`

      if (existing) {
        // 更新记录
        this.db.prepare(`
          UPDATE paper_package
          SET package_path = ?, package_hash = ?, package_size = ?,
              storage_type = 1, sync_time = ?, is_active = 1
          WHERE id = ?
        `).run(relativePath, hash, zipData.length, syncTime, existing.id)
        console.log(`✓ 已更新文件系统ZIP包到数据库: paper_code=${paperCode}, version=${version}`)
      } else {
        // 先禁用旧版本
        this.db.prepare(`
          UPDATE paper_package SET is_active = 0 
          WHERE paper_id = ? AND paper_code = ? AND is_active = 1
        `).run(paper.id, paperCode)

        // 插入新记录
        this.db.prepare(`
          INSERT INTO paper_package 
          (paper_id, paper_code, package_path, package_hash, package_size, 
           storage_type, version, sync_time, is_active)
          VALUES (?, ?, ?, ?, ?, 1, ?, ?, 1)
        `).run(paper.id, paperCode, relativePath, hash, zipData.length, version, syncTime)
        console.log(`✓ 已注册文件系统ZIP包到数据库: paper_code=${paperCode}, version=${version}`)
      }
    } catch (error) {
      console.error('注册文件系统ZIP包到数据库失败:', error.message)
      // 不抛出错误，让程序继续运行
    }
  }

  /**
   * 解压ZIP文件（使用Node.js内置zlib和adm-zip或yauzl）
   * 注意：这里需要安装adm-zip或yauzl库
   */
  async extractZip(zipData, targetDir) {
    // 使用adm-zip库（需要安装：npm install adm-zip）
    try {
      // 尝试多种路径来加载 adm-zip
      let AdmZip
      try {
        // 首先尝试直接 require（适用于正常 Node.js 环境）
        AdmZip = require('adm-zip')
      } catch (e1) {
        try {
          // 如果失败，尝试从项目根目录的 node_modules 加载
          // paperService.js 在 src/database/ 目录下，需要向上查找
          const projectRoot = path.resolve(__dirname, '../../')
          const admZipPath = path.join(projectRoot, 'node_modules', 'adm-zip')
          AdmZip = require(admZipPath)
        } catch (e2) {
          try {
            // 如果还是失败，尝试从 Electron 应用路径加载
            const { app } = require('electron')
            const appPath = app.getAppPath()
            const admZipPath = path.join(appPath, 'node_modules', 'adm-zip')
            AdmZip = require(admZipPath)
          } catch (e3) {
            // 最后尝试使用 require.resolve 来查找
            const admZipPath = require.resolve('adm-zip')
            AdmZip = require(admZipPath)
          }
        }
      }

      const zip = new AdmZip(zipData)
      zip.extractAllTo(targetDir, true)
      console.log(`✓ ZIP文件解压完成: ${targetDir}`)
    } catch (error) {
      console.error('adm-zip 解压失败:', error)
      throw error
    }
  }

  /**
   * 提取媒体文件到media目录（支持新的目录结构）
   * 新结构：
   * - trial_listen/ (试听媒体)
   * - volumes/ (卷别名称音频)
   * - sections/ (大题说明音频)
   * - intermission/ (中场音频)
   * - questions/ (题目媒体)
   * - options/ (选项媒体)
   * - explanations/ (讲解媒体)
   */
  async extractMediaFiles(tempDir, mediaDir, questions, manifest) {
    // 主进程日志：直接输出到 Electron 后台控制台
    console.log(`[Main Process] 📦 [extractMediaFiles] 开始提取媒体文件，tempDir=${tempDir}, mediaDir=${mediaDir}`)
    console.log(`[Main Process] 📦 [extractMediaFiles] manifest.sections: ${manifest.sections ? (Array.isArray(manifest.sections) ? `数组，长度=${manifest.sections.length}` : typeof manifest.sections) : '不存在'}`)

    // 确保media目录存在
    if (!fs.existsSync(mediaDir)) {
      fs.mkdirSync(mediaDir, { recursive: true })
    }

    // 提取试卷独白音频（兼容旧格式）
    if (manifest.introAudio) {
      const introAudioSrc = path.join(tempDir, 'media', 'intro', manifest.introAudio)
      if (fs.existsSync(introAudioSrc)) {
        const introDir = path.join(mediaDir, 'intro')
        if (!fs.existsSync(introDir)) {
          fs.mkdirSync(introDir, { recursive: true })
        }
        const introAudioDest = path.join(introDir, manifest.introAudio)
        fs.copyFileSync(introAudioSrc, introAudioDest)
        console.log(`✓ 提取试卷独白音频: ${introAudioDest}`)
      }
    }

    // 提取试听媒体（优先从目录复制，兼容 manifest.trialListenMedia）
    const trialListenSrc = path.join(tempDir, 'trial_listen')
    const trialListenDir = path.join(mediaDir, 'trial_listen')
    if (fs.existsSync(trialListenSrc)) {
      // 直接从 trial_listen/ 目录复制（与 extractQuickStartPackage 保持一致）
      if (fs.existsSync(trialListenDir)) {
        fs.rmSync(trialListenDir, { recursive: true, force: true })
      }
      fs.mkdirSync(trialListenDir, { recursive: true })
      const files = fs.readdirSync(trialListenSrc)
      for (const file of files) {
        const srcPath = path.join(trialListenSrc, file)
        const destPath = path.join(trialListenDir, file)
        if (fs.statSync(srcPath).isFile()) {
          fs.copyFileSync(srcPath, destPath)
          console.log(`✓ 提取试听媒体: ${destPath}`)
        }
      }
    } else if (manifest.trialListenMedia && Array.isArray(manifest.trialListenMedia)) {
      // 降级：从 manifest.trialListenMedia 指定的路径复制
      if (!fs.existsSync(trialListenDir)) {
        fs.mkdirSync(trialListenDir, { recursive: true })
      }
      for (const media of manifest.trialListenMedia) {
        if (media.mediaPath) {
          const srcPath = path.join(tempDir, media.mediaPath)
          if (fs.existsSync(srcPath)) {
            const fileName = path.basename(media.mediaPath)
            const destPath = path.join(trialListenDir, fileName)
            fs.copyFileSync(srcPath, destPath)
            console.log(`✓ 提取试听媒体: ${destPath}`)
          }
        }
      }
    }

    // 提取试听旁白音频（trial_intro/）
    const trialIntroSrc = path.join(tempDir, 'trial_intro')
    const trialIntroDir = path.join(mediaDir, 'trial_intro')
    if (fs.existsSync(trialIntroSrc)) {
      if (fs.existsSync(trialIntroDir)) {
        fs.rmSync(trialIntroDir, { recursive: true, force: true })
      }
      fs.mkdirSync(trialIntroDir, { recursive: true })
      const files = fs.readdirSync(trialIntroSrc)
      for (const file of files) {
        const srcPath = path.join(trialIntroSrc, file)
        const destPath = path.join(trialIntroDir, file)
        if (fs.statSync(srcPath).isFile()) {
          fs.copyFileSync(srcPath, destPath)
          console.log(`✓ 提取试听旁白音频: ${destPath}`)
        }
      }
    }

    // 提取操作提示媒体（operate_listen/）
    const operateListenSrc = path.join(tempDir, 'operate_listen')
    const operateListenDir = path.join(mediaDir, 'operate_listen')
    if (fs.existsSync(operateListenSrc)) {
      if (fs.existsSync(operateListenDir)) {
        fs.rmSync(operateListenDir, { recursive: true, force: true })
      }
      fs.mkdirSync(operateListenDir, { recursive: true })
      const files = fs.readdirSync(operateListenSrc)
      for (const file of files) {
        const srcPath = path.join(operateListenSrc, file)
        const destPath = path.join(operateListenDir, file)
        if (fs.statSync(srcPath).isFile()) {
          fs.copyFileSync(srcPath, destPath)
          console.log(`✓ 提取操作提示媒体: ${destPath}`)
        }
      }
    }

    // 提取注意事项音频（intro/）
    const introSrc = path.join(tempDir, 'intro')
    const introDir = path.join(mediaDir, 'intro')
    if (fs.existsSync(introSrc)) {
      if (fs.existsSync(introDir)) {
        fs.rmSync(introDir, { recursive: true, force: true })
      }
      fs.mkdirSync(introDir, { recursive: true })
      const files = fs.readdirSync(introSrc)
      for (const file of files) {
        const srcPath = path.join(introSrc, file)
        const destPath = path.join(introDir, file)
        if (fs.statSync(srcPath).isFile()) {
          fs.copyFileSync(srcPath, destPath)
          console.log(`✓ 提取注意事项音频: ${destPath}`)
        }
      }
    }

    // 提取卷别名称音频（新格式）
    if (manifest.volumes && Array.isArray(manifest.volumes)) {
      const volumesDir = path.join(mediaDir, 'volumes')
      if (!fs.existsSync(volumesDir)) {
        fs.mkdirSync(volumesDir, { recursive: true })
      }
      for (const volume of manifest.volumes) {
        // 优先处理 volumeAudio 对象（新格式）
        if (volume.volumeAudio && volume.volumeAudio.mediaPath) {
          const srcPath = path.join(tempDir, volume.volumeAudio.mediaPath)
          if (fs.existsSync(srcPath)) {
            const fileName = path.basename(volume.volumeAudio.mediaPath)
            const destPath = path.join(volumesDir, fileName)
            fs.copyFileSync(srcPath, destPath)
            console.log(`✓ 提取卷别音频 (${volume.volumeCode}): ${destPath}`)
          } else {
            console.warn(`⚠️ 卷别音频源文件不存在: ${srcPath}`)
          }
        }
        // 兼容处理 volumeMedia 数组（旧格式）
        if (volume.volumeMedia && Array.isArray(volume.volumeMedia)) {
          for (const media of volume.volumeMedia) {
            if (media.mediaPath) {
              const srcPath = path.join(tempDir, media.mediaPath)
              if (fs.existsSync(srcPath)) {
                const fileName = path.basename(media.mediaPath)
                const destPath = path.join(volumesDir, fileName)
                fs.copyFileSync(srcPath, destPath)
                console.log(`✓ 提取卷别音频 (${volume.volumeCode}): ${destPath}`)
              }
            }
          }
        }
      }
    }

    // 提取大题说明音频（新格式）
    if (manifest.sections && Array.isArray(manifest.sections)) {
      console.log(`[Main Process] 📦 开始提取大题说明音频，共 ${manifest.sections.length} 个大题`)
      const sectionsDir = path.join(mediaDir, 'sections')
      if (!fs.existsSync(sectionsDir)) {
        fs.mkdirSync(sectionsDir, { recursive: true })
      }
      let extractedCount = 0
      for (const section of manifest.sections) {
        const sectionName = section.sectionName || section.section_name || '未知'
        console.log(`[Main Process] 📦 检查大题: ${sectionName}`)

        // 优先处理 instructionAudioPath（单个路径字符串，新格式）
        if (section.instructionAudioPath) {
          const srcPath = path.join(tempDir, section.instructionAudioPath)
          const fileExists = fs.existsSync(srcPath)
          console.log(`[Main Process] 📦 检查大题音频源文件 (instructionAudioPath): ${srcPath}, 存在: ${fileExists}`)
          if (fileExists) {
            const fileName = path.basename(section.instructionAudioPath)
            const destPath = path.join(sectionsDir, fileName)
            fs.copyFileSync(srcPath, destPath)
            console.log(`[Main Process] ✓ 提取大题说明音频 (${sectionName}): ${destPath}`)
            extractedCount++
          } else {
            console.warn(`[Main Process] ⚠️ 大题音频源文件不存在: ${srcPath}`)
          }
        }
        // 兼容处理 instructionMedia 数组（旧格式）
        else if (section.instructionMedia && Array.isArray(section.instructionMedia)) {
          console.log(`[Main Process] 📦 大题 ${sectionName} 有 ${section.instructionMedia.length} 个音频文件 (instructionMedia数组)`)
          for (const media of section.instructionMedia) {
            if (media.mediaPath) {
              const srcPath = path.join(tempDir, media.mediaPath)
              const fileExists = fs.existsSync(srcPath)
              console.log(`[Main Process] 📦 检查大题音频源文件: ${srcPath}, 存在: ${fileExists}`)
              if (fileExists) {
                const fileName = path.basename(media.mediaPath)
                const destPath = path.join(sectionsDir, fileName)
                fs.copyFileSync(srcPath, destPath)
                console.log(`[Main Process] ✓ 提取大题说明音频 (${sectionName}): ${destPath}`)
                extractedCount++
              } else {
                console.warn(`[Main Process] ⚠️ 大题音频源文件不存在: ${srcPath}`)
              }
            } else {
              console.warn(`[Main Process] ⚠️ 大题音频 mediaPath 为空: ${JSON.stringify(media)}`)
            }
          }
        } else {
          console.log(`[Main Process] ⚠️ 大题 ${sectionName} 没有 instructionAudioPath 或 instructionMedia`)
        }
      }
      console.log(`[Main Process] ✓ 大题说明音频提取完成，共提取 ${extractedCount} 个文件`)
    } else {
      console.log(`[Main Process] ⚠️ manifest.sections 不存在或不是数组: ${manifest.sections ? typeof manifest.sections : 'undefined'}`)
    }

    // 提取中场音频（新格式）
    if (manifest.intermissions && Array.isArray(manifest.intermissions)) {
      const intermissionDir = path.join(mediaDir, 'intermission')
      if (!fs.existsSync(intermissionDir)) {
        fs.mkdirSync(intermissionDir, { recursive: true })
      }
      for (const intermission of manifest.intermissions) {
        // 新格式：intermissionMedia 数组
        if (intermission.intermissionMedia && Array.isArray(intermission.intermissionMedia)) {
          for (const media of intermission.intermissionMedia) {
            if (media.mediaPath) {
              const srcPath = path.join(tempDir, media.mediaPath)
              if (fs.existsSync(srcPath)) {
                const fileName = path.basename(media.mediaPath)
                const destPath = path.join(intermissionDir, fileName)
                fs.copyFileSync(srcPath, destPath)
                console.log(`✓ 提取中场音频 (${intermission.fromVolume}->${intermission.toVolume}): ${destPath}`)
              }
            }
          }
        }
        // 兼容旧格式：intermissionAudioPath 字段
        if (intermission.intermissionAudioPath) {
          const audioPath = intermission.intermissionAudioPath
          const fileName = path.basename(audioPath)
          const candidates = [
            path.join(tempDir, audioPath),
            path.join(tempDir, 'media', audioPath),
            path.join(tempDir, 'intermission', fileName)
          ]
          for (const srcPath of candidates) {
            if (srcPath && fs.existsSync(srcPath)) {
              const destPath = path.join(intermissionDir, fileName)
              try {
                fs.copyFileSync(srcPath, destPath)
                console.log(`✓ 提取中场音频(兼容路径) (${intermission.fromVolume}->${intermission.toVolume}): ${destPath}`)
              } catch (err) {
                console.warn(`⚠️ 复制中场音频失败: ${srcPath} -> ${destPath}, error=${err.message}`)
              }
              break
            }
          }
        }
      }
    }

    // 提取题目组音频（新格式）
    if (manifest.questionGroups && Array.isArray(manifest.questionGroups)) {
      console.log(`[Main Process] 📦 开始提取题目组音频，共 ${manifest.questionGroups.length} 个题目组`)
      const groupsDir = path.join(mediaDir, 'questions', 'groups')
      if (!fs.existsSync(groupsDir)) {
        fs.mkdirSync(groupsDir, { recursive: true })
      }

      let extractedCount = 0
      for (const group of manifest.questionGroups) {
        if (group.audioPath) {
          // audioPath 可能是 "questions/groups/xxx.mp3"
          let srcPath = path.join(tempDir, group.audioPath)

          if (!fs.existsSync(srcPath)) {
            // 尝试直接找文件名
            const fileName = path.basename(group.audioPath)
            // 尝试在 tempDir 下查找文件名
            const findFile = (dir, targetFileName) => {
              if (!fs.existsSync(dir)) return null
              const entries = fs.readdirSync(dir, { withFileTypes: true })
              for (const entry of entries) {
                const fullPath = path.join(dir, entry.name)
                if (entry.isFile() && entry.name === targetFileName) {
                  return fullPath
                } else if (entry.isDirectory()) {
                  const found = findFile(fullPath, targetFileName)
                  if (found) return found
                }
              }
              return null
            }
            srcPath = findFile(tempDir, fileName)
          }

          if (srcPath && fs.existsSync(srcPath)) {
            const fileName = path.basename(group.audioPath)
            const destPath = path.join(groupsDir, fileName)

            // 如果源路径包含 questions/groups 前缀，直接复制
            // 如果不包含，可能是从其他地方找到的，复制到 media/questions/groups 目录下
            fs.copyFileSync(srcPath, destPath)
            console.log(`[Main Process] ✓ 提取题目组音频: ${destPath}`)
            extractedCount++
          } else {
            console.warn(`[Main Process] ⚠️ 题目组音频文件不存在: ${group.audioPath}`)
          }
        }
      }
      console.log(`[Main Process] ✓ 题目组音频提取完成，共提取 ${extractedCount} 个文件`)
    }

    // 提取题目媒体文件（支持新格式：media数组）
    for (const question of questions) {
      const questionMediaDir = path.join(mediaDir, 'questions', `q_${question.id}`)
      if (!fs.existsSync(questionMediaDir)) {
        fs.mkdirSync(questionMediaDir, { recursive: true })
      }

      // 新格式：从media数组提取（media_type: 4-题目音频, 5-讲解音频, 6-讲解图片）
      if (question.media && Array.isArray(question.media)) {
        console.log(`[Main Process] 📦 [extractMediaFiles] 题目 ${question.id} 有 ${question.media.length} 个媒体文件`)
        for (const media of question.media) {
          console.log(`[Main Process] 📦 [extractMediaFiles] 处理媒体: mediaType=${media.mediaType}, mediaPath=${media.mediaPath}`)
          if (media.mediaPath) {
            // media.mediaPath 可能是相对路径（如 "questions/q_123/audio.mp3"）或绝对路径
            // 先尝试直接使用 mediaPath（如果 ZIP 包中已经是完整路径）
            let srcPath = path.join(tempDir, media.mediaPath)
            console.log(`[Main Process] 🔍 [extractMediaFiles] 尝试路径1: ${srcPath}`)

            if (!fs.existsSync(srcPath)) {
              // 如果路径不存在，尝试从 ZIP 包的根目录查找（ZIP 包中可能没有 questions/ 前缀）
              const fileName = path.basename(media.mediaPath)
              // 尝试在 tempDir 下查找文件名（可能在任意位置）
              srcPath = null
              try {
                // 递归查找文件
                const findFile = (dir, targetFileName) => {
                  if (!fs.existsSync(dir)) return null
                  const entries = fs.readdirSync(dir, { withFileTypes: true })
                  for (const entry of entries) {
                    const fullPath = path.join(dir, entry.name)
                    if (entry.isFile() && entry.name === targetFileName) {
                      return fullPath
                    } else if (entry.isDirectory()) {
                      const found = findFile(fullPath, targetFileName)
                      if (found) return found
                    }
                  }
                  return null
                }
                srcPath = findFile(tempDir, fileName)
                if (srcPath) {
                  console.log(`[Main Process] ✓ [extractMediaFiles] 递归查找到文件: ${srcPath}`)
                }
              } catch (error) {
                console.error(`[Main Process] ❌ [extractMediaFiles] 递归查找失败:`, error)
              }
            }

            if (srcPath && fs.existsSync(srcPath)) {
              const fileName = path.basename(media.mediaPath)
              // 根据mediaType分类存储
              let subDir = questionMediaDir
              if (media.mediaType === 5 || media.mediaType === 6) {
                // 讲解媒体存储到explanations目录
                const explanationsDir = path.join(mediaDir, 'explanations', `q_${question.id}`)
                if (!fs.existsSync(explanationsDir)) {
                  fs.mkdirSync(explanationsDir, { recursive: true })
                }
                subDir = explanationsDir
              }
              const destPath = path.join(subDir, fileName)
              fs.copyFileSync(srcPath, destPath)
              console.log(`[Main Process] ✓ [extractMediaFiles] 已复制题目媒体: ${srcPath} -> ${destPath}`)
            } else {
              console.warn(`[Main Process] ⚠️ [extractMediaFiles] 题目 ${question.id} 的媒体文件不存在: ${media.mediaPath}`)
            }
          } else {
            console.warn(`[Main Process] ⚠️ [extractMediaFiles] 题目 ${question.id} 的媒体项没有 mediaPath`)
          }
        }
      }

      // 兼容旧格式：titleMedia
      if (question.titleMedia) {
        const srcPath = path.join(tempDir, 'media', `q_${question.questionId || question.id}`, question.titleMedia)
        if (fs.existsSync(srcPath)) {
          const destPath = path.join(questionMediaDir, question.titleMedia)
          fs.copyFileSync(srcPath, destPath)
        }
      }

      // 如果 question.media 为空，尝试从 ZIP 包中扫描 questions/q_{questionId}/ 目录提取题目音频
      // 这是为了兼容后端没有在 questions.json 中填充 media 数组的情况
      if (!question.media || !Array.isArray(question.media) || question.media.length === 0) {
        console.log(`[Main Process] 📦 [extractMediaFiles] 题目 ${question.id} 的 media 数组为空，尝试扫描 ZIP 包中的 questions 目录`)

        // 尝试多个可能的源路径
        const possibleSrcDirs = [
          path.join(tempDir, 'questions', `q_${question.id}`),
          path.join(tempDir, 'media', 'questions', `q_${question.id}`),
          path.join(tempDir, 'questions', `q_${question.questionId || question.id}`),
          path.join(tempDir, 'media', 'questions', `q_${question.questionId || question.id}`)
        ]

        let foundFiles = false
        for (const srcDir of possibleSrcDirs) {
          if (fs.existsSync(srcDir)) {
            console.log(`[Main Process] ✓ [extractMediaFiles] 找到源目录: ${srcDir}`)
            try {
              const files = fs.readdirSync(srcDir)
              const audioFiles = files.filter(f => {
                const ext = path.extname(f).toLowerCase()
                return ext === '.mp3' || ext === '.wav' || ext === '.m4a' || ext === '.aac' || ext === '.ogg'
              })

              if (audioFiles.length > 0) {
                console.log(`[Main Process] 📦 [extractMediaFiles] 在 ${srcDir} 中找到 ${audioFiles.length} 个音频文件`)
                for (const audioFile of audioFiles) {
                  const srcPath = path.join(srcDir, audioFile)
                  const destPath = path.join(questionMediaDir, audioFile)
                  fs.copyFileSync(srcPath, destPath)
                  console.log(`[Main Process] ✓ [extractMediaFiles] 已复制题目音频: ${srcPath} -> ${destPath}`)
                }
                foundFiles = true
                break // 找到文件后就不再尝试其他路径
              }
            } catch (error) {
              console.error(`[Main Process] ❌ [extractMediaFiles] 扫描目录失败: ${srcDir}`, error)
            }
          }
        }

        // 如果以上路径都没找到，尝试递归查找 questions/q_{questionId}/ 目录
        if (!foundFiles) {
          try {
            const findQuestionDir = (dir, questionId) => {
              if (!fs.existsSync(dir)) return null
              const entries = fs.readdirSync(dir, { withFileTypes: true })
              for (const entry of entries) {
                const fullPath = path.join(dir, entry.name)
                if (entry.isDirectory()) {
                  // 检查是否是目标目录（questions/q_{questionId}）
                  if (entry.name === `q_${questionId}` && dir.endsWith('questions')) {
                    return fullPath
                  }
                  // 递归查找
                  const found = findQuestionDir(fullPath, questionId)
                  if (found) return found
                }
              }
              return null
            }

            const foundDir = findQuestionDir(tempDir, question.id)
            if (foundDir && fs.existsSync(foundDir)) {
              console.log(`[Main Process] ✓ [extractMediaFiles] 递归查找到题目目录: ${foundDir}`)
              const files = fs.readdirSync(foundDir)
              const audioFiles = files.filter(f => {
                const ext = path.extname(f).toLowerCase()
                return ext === '.mp3' || ext === '.wav' || ext === '.m4a' || ext === '.aac' || ext === '.ogg'
              })

              if (audioFiles.length > 0) {
                console.log(`[Main Process] 📦 [extractMediaFiles] 在 ${foundDir} 中找到 ${audioFiles.length} 个音频文件`)
                for (const audioFile of audioFiles) {
                  const srcPath = path.join(foundDir, audioFile)
                  const destPath = path.join(questionMediaDir, audioFile)
                  fs.copyFileSync(srcPath, destPath)
                  console.log(`[Main Process] ✓ [extractMediaFiles] 已复制题目音频: ${srcPath} -> ${destPath}`)
                }
              }
            }
          } catch (error) {
            console.error(`[Main Process] ❌ [extractMediaFiles] 递归查找题目目录失败:`, error)
          }
        }
      }

      // 提取选项媒体（支持新格式：answers[].media数组）
      // 注意：保留完整的目录结构，包括 options/q_{questionId}/ 这一层
      if (question.answers && Array.isArray(question.answers)) {
        for (const answer of question.answers) {
          // 新格式：从media数组提取（media_type: 2-选项媒体）
          if (answer.media && Array.isArray(answer.media)) {
            for (const media of answer.media) {
              if (media.mediaPath) {
                const srcPath = path.join(tempDir, media.mediaPath)
                if (fs.existsSync(srcPath)) {
                  // 保留完整的目录结构（例如：options/q_327307266/xxx.mp3）
                  const destPath = path.join(mediaDir, media.mediaPath)
                  const destDir = path.dirname(destPath)

                  // 确保目标目录存在
                  if (!fs.existsSync(destDir)) {
                    fs.mkdirSync(destDir, { recursive: true })
                  }

                  fs.copyFileSync(srcPath, destPath)
                  console.log(`[Main Process] ✓ 提取选项媒体: ${destPath}`)
                } else {
                  console.warn(`[Main Process] ⚠️ 选项媒体源文件不存在: ${srcPath}`)
                }
              }
            }
          }
          // 兼容旧格式：option.media
          if (answer.media && typeof answer.media === 'string') {
            // 旧格式也使用新的路径结构
            const srcPath = path.join(tempDir, 'media', `q_${question.questionId || question.id}`, 'options', answer.media)
            if (fs.existsSync(srcPath)) {
              const optionsDir = path.join(mediaDir, 'options', `q_${question.questionId || question.id}`)
              if (!fs.existsSync(optionsDir)) {
                fs.mkdirSync(optionsDir, { recursive: true })
              }
              const destPath = path.join(optionsDir, answer.media)
              fs.copyFileSync(srcPath, destPath)
              console.log(`[Main Process] ✓ 提取选项媒体(旧格式): ${destPath}`)
            }
          }
        }
      }
    }

    console.log(`[Main Process] ✓ 媒体文件提取完成: ${mediaDir}`)
  }

  /**
   * 保存媒体文件索引到SQLite（支持新的媒体类型和关联字段）
   * 保存到 question_media 表，支持所有 media_type (1-11)
   */
  async saveMediaIndex(paperCode, questions, manifest) {
    try {
      // 获取paper_id
      const paper = this.db.prepare(`
        SELECT id FROM paper WHERE paper_code = ? LIMIT 1
      `).get(paperCode)

      if (!paper) {
        console.warn(`试卷不存在，跳过媒体索引保存: ${paperCode}`)
        return
      }

      const paperId = paper.id

      // 清空该试卷的旧媒体索引（只删除与当前试卷相关的）
      this.db.prepare(`
        DELETE FROM question_media WHERE paper_id = ?
      `).run(paperId)

      // 保存媒体文件的通用方法
      const saveMedia = (mediaData) => {
        // 对于不需要 question_id 的媒体类型（7-卷别名称音频，8-大题说明音频，9-中场音频），使用 0 作为默认值
        // 因为 question_id 字段是 NOT NULL，不能为 null
        const questionId = mediaData.questionId ||
          (mediaData.mediaType === 7 || mediaData.mediaType === 8 || mediaData.mediaType === 9 ? 0 : null)

        // 如果 questionId 仍然为 null，说明是其他类型但没有提供 questionId，使用 0 作为默认值
        const finalQuestionId = questionId !== null ? questionId : 0

        const mediaStmt = this.db.prepare(`
          INSERT INTO question_media 
          (question_id, paper_id, volume_id, section_id, intermission_id,
           media_type, option_id, blank_area_id,
           media_name, media_path, media_url,
           media_size, media_format, media_duration,
           is_compressed, storage_type, create_time)
          VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        `)

        mediaStmt.run(
          finalQuestionId,
          mediaData.paperId || null,
          mediaData.volumeId || null,
          mediaData.sectionId || null,
          mediaData.intermissionId || null,
          mediaData.mediaType,
          mediaData.optionId || null,
          mediaData.blankAreaId || null,
          mediaData.mediaName || '',
          mediaData.mediaPath || null,
          mediaData.mediaUrl || null,
          mediaData.mediaSize || null,
          mediaData.mediaFormat || null,
          mediaData.mediaDuration || null,
          mediaData.isCompressed || 0,
          mediaData.storageType || 0,
          Date.now()
        )
      }

      let mediaCount = 0

      // 1. 保存试听媒体（media_type: 10-试听音频, 11-试听图片）
      if (manifest.trialListenMedia && Array.isArray(manifest.trialListenMedia)) {
        for (const media of manifest.trialListenMedia) {
          const fileName = path.basename(media.mediaPath || '')
          const relativePath = `media/${paperCode}/trial_listen/${fileName}`
          saveMedia({
            paperId,
            mediaType: media.mediaType || (media.mediaFormat === 'mp3' || media.mediaFormat === 'wav' ? 10 : 11),
            mediaName: fileName,
            mediaPath: relativePath,
            mediaFormat: media.mediaFormat,
            mediaSize: media.mediaSize,
            mediaDuration: media.mediaDuration
          })
          mediaCount++
        }
      }

      // 2. 保存卷别名称音频（media_type: 7）
      if (manifest.volumes && Array.isArray(manifest.volumes)) {
        for (const volume of manifest.volumes) {
          // 获取volume_id
          const volumeRecord = this.db.prepare(`
            SELECT id FROM paper_volume WHERE paper_id = ? AND volume_code = ? LIMIT 1
          `).get(paperId, volume.volumeCode)

          if (volumeRecord && volume.volumeMedia && Array.isArray(volume.volumeMedia)) {
            for (const media of volume.volumeMedia) {
              const fileName = path.basename(media.mediaPath || '')
              const relativePath = `media/${paperCode}/volumes/${fileName}`
              saveMedia({
                paperId,
                volumeId: volumeRecord.id,
                mediaType: 7, // 卷别名称音频
                mediaName: fileName,
                mediaPath: relativePath,
                mediaFormat: media.mediaFormat,
                mediaSize: media.mediaSize,
                mediaDuration: media.mediaDuration
              })
              mediaCount++
            }
          }
        }
      }

      // 3. 保存大题说明音频（media_type: 8）
      if (manifest.sections && Array.isArray(manifest.sections)) {
        for (const section of manifest.sections) {
          // 获取section_id（兼容 volumeCode 和 volumeId）
          const sectionRecord = this.db.prepare(`
            SELECT id FROM paper_section WHERE paper_id = ? AND (volume_code = ? OR volume_id = ?) AND section_name = ? LIMIT 1
          `).get(paperId, section.volumeCode || '', section.volumeId || null, section.sectionName || section.section_name)

          if (sectionRecord) {
            // 优先处理 instructionAudioPath（单个路径字符串，新格式）
            if (section.instructionAudioPath) {
              const fileName = path.basename(section.instructionAudioPath)
              const relativePath = `media/${paperCode}/sections/${fileName}`
              // 从 instructionAudioUrl 或 instructionAudioPath 获取格式和时长
              const mediaFormat = section.instructionAudioPath.split('.').pop() || 'mp3'
              const mediaDuration = section.instructionAudioDuration || null
              saveMedia({
                paperId,
                sectionId: sectionRecord.id,
                mediaType: 8, // 大题说明音频
                mediaName: fileName,
                mediaPath: relativePath,
                mediaUrl: section.instructionAudioUrl || null,
                mediaFormat: mediaFormat,
                mediaSize: null, // 如果ZIP包中有文件，可以从文件获取大小
                mediaDuration: mediaDuration
              })
              mediaCount++
              console.log(`[Main Process] ✓ 保存大题说明音频索引: ${section.sectionName || section.section_name}, path=${relativePath}`)
            }
            // 兼容处理 instructionMedia 数组（旧格式）
            else if (section.instructionMedia && Array.isArray(section.instructionMedia)) {
              for (const media of section.instructionMedia) {
                const fileName = path.basename(media.mediaPath || '')
                const relativePath = `media/${paperCode}/sections/${fileName}`
                saveMedia({
                  paperId,
                  sectionId: sectionRecord.id,
                  mediaType: 8, // 大题说明音频
                  mediaName: fileName,
                  mediaPath: relativePath,
                  mediaFormat: media.mediaFormat,
                  mediaSize: media.mediaSize,
                  mediaDuration: media.mediaDuration
                })
                mediaCount++
              }
            }
          } else {
            console.warn(`⚠️ 无法找到大题记录: paperId=${paperId}, volumeCode=${section.volumeCode}, sectionName=${section.sectionName || section.section_name}`)
          }
        }
      }

      // 4. 保存中场音频（media_type: 9）
      if (manifest.intermissions && Array.isArray(manifest.intermissions)) {
        for (const intermission of manifest.intermissions) {
          // 获取intermission_id
          const intermissionRecord = this.db.prepare(`
            SELECT id FROM paper_intermission WHERE paper_id = ? AND from_volume = ? AND to_volume = ? LIMIT 1
          `).get(paperId, intermission.fromVolume, intermission.toVolume)

          if (intermissionRecord && intermission.intermissionMedia && Array.isArray(intermission.intermissionMedia)) {
            for (const media of intermission.intermissionMedia) {
              const fileName = path.basename(media.mediaPath || '')
              const relativePath = `media/${paperCode}/intermission/${fileName}`
              saveMedia({
                paperId,
                intermissionId: intermissionRecord.id,
                mediaType: 9, // 中场音频
                mediaName: fileName,
                mediaPath: relativePath,
                mediaFormat: media.mediaFormat,
                mediaSize: media.mediaSize,
                mediaDuration: media.mediaDuration
              })
              mediaCount++
            }
          }
        }
      }

      // 5. 保存题目媒体（media_type: 1-题目媒体, 4-题目音频, 5-讲解音频, 6-讲解图片）
      for (const question of questions) {
        const questionId = question.id || question.questionId

        // 新格式：从media数组提取
        if (question.media && Array.isArray(question.media)) {
          for (const media of question.media) {
            const fileName = path.basename(media.mediaPath || '')
            let relativePath
            // 根据mediaType确定路径
            if (media.mediaType === 5 || media.mediaType === 6) {
              relativePath = `media/${paperCode}/explanations/q_${questionId}/${fileName}`
            } else {
              relativePath = `media/${paperCode}/questions/q_${questionId}/${fileName}`
            }

            saveMedia({
              questionId,
              paperId,
              mediaType: media.mediaType || 1, // 默认为题目媒体
              mediaName: fileName,
              mediaPath: relativePath,
              mediaFormat: media.mediaFormat,
              mediaSize: media.mediaSize,
              mediaDuration: media.mediaDuration
            })
            mediaCount++
          }
        }

        // 兼容旧格式：titleMedia
        if (question.titleMedia) {
          const relativePath = `media/${paperCode}/questions/q_${questionId}/${question.titleMedia}`
          saveMedia({
            questionId,
            paperId,
            mediaType: 1, // 题目媒体
            mediaName: question.titleMedia,
            mediaPath: relativePath,
            mediaFormat: question.titleMediaFormat,
            mediaSize: question.titleMediaSize,
            mediaDuration: question.titleMediaDuration
          })
          mediaCount++
        }

        // 6. 保存选项媒体（media_type: 2）
        if (question.answers && Array.isArray(question.answers)) {
          for (const answer of question.answers) {
            // 新格式：从media数组提取
            if (answer.media && Array.isArray(answer.media)) {
              for (const media of answer.media) {
                const fileName = path.basename(media.mediaPath || '')
                const relativePath = `media/${paperCode}/options/q_${questionId}/${fileName}`
                saveMedia({
                  questionId,
                  paperId,
                  optionId: answer.id || null,
                  mediaType: 2, // 选项媒体
                  mediaName: fileName,
                  mediaPath: relativePath,
                  mediaFormat: media.mediaFormat,
                  mediaSize: media.mediaSize,
                  mediaDuration: media.mediaDuration
                })
                mediaCount++
              }
            }
            // 兼容旧格式：answer.media (string)
            if (answer.media && typeof answer.media === 'string') {
              const relativePath = `media/${paperCode}/options/q_${questionId}/${answer.media}`
              saveMedia({
                questionId,
                paperId,
                optionId: answer.id || null,
                mediaType: 2, // 选项媒体
                mediaName: answer.media,
                mediaPath: relativePath,
                mediaFormat: answer.mediaFormat,
                mediaSize: answer.mediaSize,
                mediaDuration: answer.mediaDuration
              })
              mediaCount++
            }
          }
        }

        // 7. 保存题目讲解相关字段到question表
        if (question.explanationEnabled !== undefined || question.explanationText || question.explanationDelaySeconds !== undefined) {
          try {
            const updateQuestionStmt = this.db.prepare(`
              UPDATE question 
              SET explanation_enabled = ?, explanation_text = ?, explanation_delay_seconds = ?
              WHERE id = ?
            `)
            updateQuestionStmt.run(
              question.explanationEnabled ? 1 : 0,
              question.explanationText || null,
              question.explanationDelaySeconds || 2,
              questionId
            )
          } catch (error) {
            console.warn(`更新题目讲解字段失败 (questionId=${questionId}): ${error.message}`)
          }
        }
      }

      // 8. 更新paper_question表的section_id和section_order
      if (manifest.sections && Array.isArray(manifest.sections)) {
        for (const section of manifest.sections) {
          // 获取section_id
          const sectionRecord = this.db.prepare(`
            SELECT id FROM paper_section WHERE paper_id = ? AND volume_code = ? AND section_name = ? LIMIT 1
          `).get(paperId, section.volumeCode, section.sectionName)

          if (sectionRecord && section.questions && Array.isArray(section.questions)) {
            // 更新该大题下的所有题目的section_id和section_order
            for (let i = 0; i < section.questions.length; i++) {
              const questionId = section.questions[i]
              try {
                this.db.prepare(`
                  UPDATE paper_question 
                  SET section_id = ?, section_order = ?
                  WHERE paper_id = ? AND question_id = ?
                `).run(sectionRecord.id, i + 1, paperId, questionId)
              } catch (error) {
                console.warn(`更新paper_question的section信息失败 (questionId=${questionId}): ${error.message}`)
              }
            }
          }
        }
      }

      console.log(`✓ 媒体文件索引保存完成: ${paperCode}，共 ${mediaCount} 个媒体文件`)
    } catch (error) {
      console.error('保存媒体文件索引失败:', error.message)
      throw error
    }
  }

  /**
   * 获取媒体文件绝对路径
   * @param {string} relativePath - 相对路径
   * @returns {string} 绝对路径
   */
  getMediaAbsolutePath(relativePath) {
    return path.join(app.getPath('userData'), relativePath)
  }

  /**
   * 检查媒体文件是否存在
   * @param {string} relativePath - 相对路径
   * @returns {boolean} 是否存在
   */
  mediaFileExists(relativePath) {
    const absolutePath = this.getMediaAbsolutePath(relativePath)
    return fs.existsSync(absolutePath)
  }

  /**
   * 添加到缓存（LRU策略）
   */
  addToCache(key, data) {
    const dataSize = JSON.stringify(data).length

    // 如果缓存已满，删除最旧的项
    while (this.cacheSize + dataSize > MAX_CACHE_SIZE && this.extractCache.size > 0) {
      const firstKey = this.extractCache.keys().next().value
      const firstValue = this.extractCache.get(firstKey)
      this.cacheSize -= firstValue.size
      this.extractCache.delete(firstKey)
    }

    this.extractCache.set(key, {
      data,
      timestamp: Date.now(),
      size: dataSize
    })
    this.cacheSize += dataSize
  }

  /**
   * 读取流为字符串（用于读取错误响应）
   */
  async readStreamAsString(stream) {
    return new Promise((resolve, reject) => {
      const chunks = []
      stream.on('data', (chunk) => chunks.push(chunk))
      stream.on('end', () => {
        try {
          const buffer = Buffer.concat(chunks)
          const text = buffer.toString('utf8')
          // 尝试解析为JSON
          try {
            const json = JSON.parse(text)
            resolve(JSON.stringify(json, null, 2))
          } catch (e) {
            resolve(text)
          }
        } catch (error) {
          reject(error)
        }
      })
      stream.on('error', reject)
    })
  }

  /**
   * 扫描本地 paper_packages 目录，导入手动放置的ZIP包
   * 文件名格式：{paperCode}_v{version}.zip
   * 例如：PAPER_20251120_001_v5.zip
   */
  async scanAndImportLocalPackages() {
    try {
      console.log('开始扫描本地 paper_packages 目录，查找手动放置的ZIP包...')

      if (!fs.existsSync(this.packageBasePath)) {
        console.log('paper_packages 目录不存在，跳过扫描')
        return { imported: 0, skipped: 0, errors: 0 }
      }

      const files = fs.readdirSync(this.packageBasePath)
      const zipFiles = files.filter(file => file.endsWith('.zip'))

      if (zipFiles.length === 0) {
        console.log('未找到ZIP文件')
        return { imported: 0, skipped: 0, errors: 0 }
      }

      console.log(`找到 ${zipFiles.length} 个ZIP文件`)

      let imported = 0
      let skipped = 0
      let errors = 0

      for (const zipFile of zipFiles) {
        try {
          // 解析文件名：{paperCode}_v{version}.zip
          const match = zipFile.match(/^(.+)_v(\d+)\.zip$/)
          if (!match) {
            console.warn(`文件名格式不正确，跳过: ${zipFile} (期望格式: {paperCode}_v{version}.zip)`)
            skipped++
            continue
          }

          const paperCode = match[1]
          const version = parseInt(match[2], 10)

          if (!paperCode || isNaN(version)) {
            console.warn(`无法解析文件名，跳过: ${zipFile}`)
            skipped++
            continue
          }

          // 检查本地paper表中是否有该试卷记录
          const paper = this.db.prepare(`
            SELECT id FROM paper WHERE paper_code = ? LIMIT 1
          `).get(paperCode)

          if (!paper || !paper.id) {
            console.warn(`本地paper表中没有找到试卷记录: ${paperCode}，跳过导入: ${zipFile}`)
            console.warn(`  提示：请先确保paper表中有该试卷的记录（可以通过同步试卷列表获得）`)
            skipped++
            continue
          }

          // 检查是否已存在该版本的记录
          const existingPackage = this.db.prepare(`
            SELECT id, version, package_hash FROM paper_package 
            WHERE paper_code = ? AND version = ? AND is_active = 1
            LIMIT 1
          `).get(paperCode, version)

          if (existingPackage) {
            console.log(`试卷包 ${paperCode} v${version} 已存在于数据库中，跳过导入: ${zipFile}`)
            skipped++
            continue
          }

          // 读取ZIP文件并计算hash
          const filePath = path.join(this.packageBasePath, zipFile)
          const fileStats = fs.statSync(filePath)
          const fileBuffer = fs.readFileSync(filePath)
          const packageHash = this.calculateHash(fileBuffer)
          const packageSize = fileStats.size

          // 保存到数据库
          const relativePath = path.relative(app.getPath('userData'), filePath)
          const syncTime = Date.now()

          // 先禁用旧版本的记录（如果有，只禁用完整包的记录，不包括快速包）
          this.db.prepare(`
            UPDATE paper_package 
            SET is_active = 0 
            WHERE paper_id = ? AND paper_code = ? AND is_active = 1
          `).run(paper.id, paperCode)

          // 检查是否已存在相同版本的记录
          const existingRecord = this.db.prepare(`
            SELECT id FROM paper_package
            WHERE paper_id = ? AND paper_code = ? AND version = ?
          `).get(paper.id, paperCode, version)

          try {
            if (existingRecord) {
              // 更新现有记录
              this.db.prepare(`
                UPDATE paper_package
                SET package_path = ?, package_hash = ?, package_size = ?,
                    storage_type = 1, sync_time = ?, is_active = 1
                WHERE id = ?
              `).run(
                relativePath,
                packageHash,
                packageSize,
                syncTime,
                existingRecord.id
              )
              console.log(`✓ 已更新完整包到 paper_package 表: paper_id=${paper.id}, paper_code=${paperCode}, version=${version}`)
            } else {
              // 插入新记录
              try {
                this.db.prepare(`
                  INSERT INTO paper_package 
                  (paper_id, paper_code, package_path, package_hash, package_size, 
                   storage_type, version, sync_time, is_active)
                  VALUES (?, ?, ?, ?, ?, 1, ?, ?, 1)
                `).run(
                  paper.id,
                  paperCode,
                  relativePath,
                  packageHash,
                  packageSize,
                  version,
                  syncTime
                )
                console.log(`✓ 已插入完整包到 paper_package 表: paper_id=${paper.id}, paper_code=${paperCode}, version=${version}`)
              } catch (insertError) {
                // 如果插入失败（可能是 UNIQUE 约束），先删除旧记录再插入
                if (insertError.message && insertError.message.includes('UNIQUE')) {
                  console.warn(`插入失败（UNIQUE约束），先删除旧记录: ${insertError.message}`)
                  this.db.prepare(`
                    DELETE FROM paper_package
                    WHERE paper_id = ? AND paper_code = ?
                  `).run(paper.id, paperCode)

                  // 重新插入
                  this.db.prepare(`
                    INSERT INTO paper_package 
                    (paper_id, paper_code, package_path, package_hash, package_size, 
                     storage_type, version, sync_time, is_active)
                    VALUES (?, ?, ?, ?, ?, 1, ?, ?, 1)
                  `).run(
                    paper.id,
                    paperCode,
                    relativePath,
                    packageHash,
                    packageSize,
                    version,
                    syncTime
                  )
                  console.log(`✓ 已插入完整包到 paper_package 表（删除旧记录后）: paper_id=${paper.id}, paper_code=${paperCode}, version=${version}`)
                } else {
                  throw insertError
                }
              }
            }

            // 验证记录是否已保存到 paper_package 表
            const verifyRecord = this.db.prepare(`
              SELECT id, paper_code, version, is_active FROM paper_package
              WHERE paper_id = ? AND paper_code = ? AND version = ?
            `).get(paper.id, paperCode, version)

            if (verifyRecord) {
              console.log(`✓ 验证：完整包已保存到 paper_package 表，id=${verifyRecord.id}, paper_code=${verifyRecord.paper_code}, version=${verifyRecord.version}, is_active=${verifyRecord.is_active}`)
            } else {
              console.error(`❌ 错误：完整包保存后验证失败，paper_package 表中未找到记录！`)
              console.error(`   paper_id=${paper.id}, paper_code=${paperCode}, version=${version}`)
              throw new Error(`保存到 paper_package 表失败：验证未找到记录`)
            }
          } catch (error) {
            console.error(`保存完整包到 paper_package 表失败:`, error)
            console.error(`paper_id=${paper.id}, paper_code=${paperCode}, version=${version}`)
            console.error(`错误详情:`, error.message)
            throw error
          }

          // 更新paper表的版本信息
          this.db.prepare(`
            UPDATE paper 
            SET version = ?, package_hash = ?, package_size = ?, last_package_time = ?
            WHERE paper_code = ?
          `).run(
            version,
            packageHash,
            packageSize,
            syncTime,
            paperCode
          )

          console.log(`✓ 成功导入手动放置的ZIP包: ${zipFile} (${paperCode} v${version}, ${(packageSize / 1024 / 1024).toFixed(2)}MB)`)
          imported++
        } catch (error) {
          console.error(`导入ZIP包失败: ${zipFile}`, error.message)
          errors++
        }
      }

      console.log(`扫描完成: 导入 ${imported} 个，跳过 ${skipped} 个，错误 ${errors} 个`)
      return { imported, skipped, errors }
    } catch (error) {
      console.error('扫描本地ZIP包失败:', error.message)
      return { imported: 0, skipped: 0, errors: 1 }
    }
  }

  /**
   * 清理旧版本（保留当前版本+最近3个版本，清理90天前的）
   */
  async cleanupOldVersions() {
    try {
      const cutoffTime = Date.now() - (CACHE_RETENTION_DAYS * 24 * 60 * 60 * 1000)

      // 查询需要清理的试卷包
      const oldPackages = this.db.prepare(`
        SELECT paper_code, version, package_path, storage_type
        FROM paper_package
        WHERE sync_time < ? AND is_active = 0
      `).all(cutoffTime)

      // 删除旧版本媒体文件
      for (const pkg of oldPackages) {
        const mediaDir = path.join(this.mediaBasePath, pkg.paper_code)
        if (fs.existsSync(mediaDir)) {
          fs.rmSync(mediaDir, { recursive: true, force: true })
          console.log(`删除旧版本媒体文件: ${mediaDir}`)
        }
      }

      // 删除旧版本ZIP包文件
      for (const pkg of oldPackages) {
        if (pkg.storage_type === 1 && pkg.package_path) {
          const absolutePath = path.join(app.getPath('userData'), pkg.package_path)
          if (fs.existsSync(absolutePath)) {
            fs.unlinkSync(absolutePath)
            console.log(`删除旧版本ZIP包: ${absolutePath}`)
          }
        }
      }

      // 从数据库删除记录
      const deleted = this.db.prepare(`
        DELETE FROM paper_package 
        WHERE sync_time < ? AND is_active = 0
      `).run(cutoffTime).changes

      console.log(`✓ 清理完成，删除了 ${deleted} 条旧版本记录`)
    } catch (error) {
      console.error('清理旧版本失败:', error.message)
    }
  }

  /**
   * 递归删除文件夹
   * @param {string} dirPath - 目录路径
   */
  deleteFolderRecursive(dirPath) {
    if (fs.existsSync(dirPath)) {
      fs.readdirSync(dirPath).forEach((file) => {
        const curPath = path.join(dirPath, file)
        if (fs.lstatSync(curPath).isDirectory()) {
          // 递归删除子目录
          this.deleteFolderRecursive(curPath)
        } else {
          // 删除文件
          fs.unlinkSync(curPath)
        }
      })
      // 删除空目录
      fs.rmdirSync(dirPath)
    }
  }

  /**
   * 从服务器获取最新试卷版本信息并更新本地paper表
   * @param {number} paperId - 试卷ID
   * @param {string} token - 认证token
   * @returns {Object} { success, localVersion, remoteVersion, needsUpdate, isOffline }
   */
  async refreshPaperVersionFromServer(paperId, token) {
    try {
      console.log(`🔄 [refreshPaperVersionFromServer] 开始同步试卷版本信息: paperId=${paperId}`)

      // 1. 获取本地试卷信息
      const localPaper = this.db.prepare(`
        SELECT id, paper_code, paper_name, version, package_hash, package_size
        FROM paper
        WHERE id = ?
      `).get(paperId)

      if (!localPaper) {
        console.warn(`[refreshPaperVersionFromServer] 本地未找到试卷: paperId=${paperId}`)
        return { success: false, message: '本地未找到试卷信息' }
      }

      const paperCode = localPaper.paper_code
      const localVersion = localPaper.version || 0

      console.log(`[refreshPaperVersionFromServer] 本地版本: v${localVersion}, paperCode=${paperCode}`)

      // 2. 从服务器获取最新试卷信息
      try {
        const response = await axios.post(
          `${API_BASE_URL}/student/sync/paper/list`,
          {
            pageNum: 1,
            pageSize: 100,
            status: 1
          },
          {
            headers: {
              'Content-Type': 'application/json',
              'Authorization': `Bearer ${token}`
            },
            timeout: 10000 // 10秒超时
          }
        )

        if (response.data && response.data.code === 200) {
          let paperList = response.data.rows || response.data.data || []

          // 查找目标试卷
          const remotePaper = paperList.find(p =>
            (p.paperCode === paperCode || p.paper_code === paperCode) ||
            (p.id === paperId)
          )

          if (!remotePaper) {
            console.warn(`[refreshPaperVersionFromServer] 服务器未找到试卷: paperCode=${paperCode}`)
            return {
              success: true,
              localVersion,
              remoteVersion: localVersion,
              needsUpdate: false,
              message: '服务器未找到对应试卷，使用本地版本'
            }
          }

          const remoteVersion = remotePaper.version || 0
          const remoteHash = remotePaper.packageHash || remotePaper.package_hash || null
          const remoteSize = remotePaper.packageSize || remotePaper.package_size || null

          console.log(`[refreshPaperVersionFromServer] 服务器版本: v${remoteVersion}, hash=${remoteHash || '无'}`)

          // 3. 判断是否需要更新
          const needsUpdate = remoteVersion > localVersion ||
            (remoteVersion === localVersion && remoteHash && remoteHash !== localPaper.package_hash)

          if (needsUpdate) {
            console.log(`[refreshPaperVersionFromServer] 检测到新版本，更新本地paper表: v${localVersion} -> v${remoteVersion}`)

            // 更新本地paper表
            this.db.prepare(`
              UPDATE paper 
              SET version = ?, package_hash = ?, package_size = ?
              WHERE id = ?
            `).run(remoteVersion, remoteHash, remoteSize, paperId)

            console.log(`✓ [refreshPaperVersionFromServer] 本地paper表已更新`)
          } else {
            console.log(`[refreshPaperVersionFromServer] 本地版本已是最新: v${localVersion}`)
          }

          return {
            success: true,
            localVersion,
            remoteVersion,
            needsUpdate,
            isOffline: false
          }
        } else {
          console.warn(`[refreshPaperVersionFromServer] 服务器返回错误: ${response.data?.msg || '未知错误'}`)
          return {
            success: true,
            localVersion,
            remoteVersion: localVersion,
            needsUpdate: false,
            isOffline: false,
            message: '服务器返回错误，使用本地版本'
          }
        }
      } catch (error) {
        // 网络错误，可能是离线状态
        if (error.message.includes('网络') || error.message.includes('ECONNREFUSED') ||
          error.message.includes('timeout') || error.message.includes('ENOTFOUND') ||
          error.code === 'ECONNREFUSED' || error.code === 'ENOTFOUND' || error.code === 'ETIMEDOUT' ||
          error.message.includes('Network Error')) {
          console.log(`[refreshPaperVersionFromServer] 离线状态，使用本地版本: v${localVersion}`)
          return {
            success: true,
            localVersion,
            remoteVersion: localVersion,
            needsUpdate: false,
            isOffline: true,
            message: '离线状态，使用本地版本'
          }
        }
        throw error
      }
    } catch (error) {
      console.error(`[refreshPaperVersionFromServer] 同步失败:`, error)
      return { success: false, message: error.message || '同步失败' }
    }
  }

  /**
   * 删除所有旧版本（保留当前版本）
   * @param {string} paperCode - 试卷编码
   * @param {number} currentVersion - 当前版本号
   */
  async deleteAllOldVersions(paperCode, currentVersion) {
    try {
      console.log(`🧹 [deleteAllOldVersions] 开始删除所有旧版本: ${paperCode}，保留版本 v${currentVersion}`)

      // 查询所有旧版本
      const oldVersions = this.db.prepare(`
        SELECT DISTINCT version FROM paper_package 
        WHERE paper_code = ? AND version < ?
        ORDER BY version DESC
      `).all(paperCode, currentVersion)

      if (oldVersions.length === 0) {
        console.log(`✓ [deleteAllOldVersions] 没有旧版本需要删除`)
        return
      }

      console.log(`📦 [deleteAllOldVersions] 找到 ${oldVersions.length} 个旧版本: ${oldVersions.map(v => `v${v.version}`).join(', ')}`)

      // 删除每个旧版本
      for (const versionRecord of oldVersions) {
        await this.deleteOldVersionPackage(paperCode, versionRecord.version)
      }

      console.log(`✓ [deleteAllOldVersions] 所有旧版本删除完成`)
    } catch (error) {
      console.error(`❌ [deleteAllOldVersions] 删除所有旧版本失败:`, error)
    }
  }

  /**
   * 清理损坏的试卷包（保留paper表的版本信息，以便重新下载）
   * @param {string} paperCode - 试卷编码
   * @param {number} version - 版本号
   */
  async cleanCorruptedPackage(paperCode, version) {
    try {
      console.log(`🧹 [cleanCorruptedPackage] 清理损坏的试卷包: ${paperCode} v${version}`)

      const app = require('electron').app || require('@electron/remote').app
      const userDataPath = app.getPath('userData')

      // 1. 删除媒体文件目录
      const mediaDir = path.join(userDataPath, 'media', paperCode)
      if (fs.existsSync(mediaDir)) {
        console.log(`🧹 删除媒体文件: ${mediaDir}`)
        try {
          this.deleteFolderRecursive(mediaDir)
          console.log(`✓ 已删除媒体文件`)
        } catch (error) {
          console.warn(`⚠️ 删除媒体文件失败: ${error.message}`)
        }
      }

      // 2. 删除临时文件目录
      const tempDir = path.join(userDataPath, 'temp', paperCode)
      if (fs.existsSync(tempDir)) {
        console.log(`🧹 删除临时文件: ${tempDir}`)
        try {
          this.deleteFolderRecursive(tempDir)
          console.log(`✓ 已删除临时文件`)
        } catch (error) {
          console.warn(`⚠️ 删除临时文件失败: ${error.message}`)
        }
      }

      // 3. 删除快速启动包
      const quickStartFileName = `${paperCode}_v${version}_quick.zip`
      const quickStartPath = path.join(this.packageBasePath, quickStartFileName)
      if (fs.existsSync(quickStartPath)) {
        console.log(`🧹 删除快速启动包: ${quickStartFileName}`)
        try {
          fs.unlinkSync(quickStartPath)
          console.log(`✓ 已删除快速启动包`)
        } catch (error) {
          console.warn(`⚠️ 删除快速启动包失败: ${error.message}`)
        }
      }

      // 4. 删除完整包文件
      const packageFileName = `${paperCode}_v${version}.zip`
      const packagePath = path.join(this.packageBasePath, packageFileName)

      if (fs.existsSync(packagePath)) {
        console.log(`🧹 删除完整包文件: ${packageFileName}`)
        try {
          fs.unlinkSync(packagePath)
          console.log(`✓ 已删除完整包文件`)
        } catch (error) {
          console.warn(`⚠️ 删除完整包文件失败: ${error.message}`)
        }
      }

      // 5. 从数据库中删除 paper_package 表的记录
      try {
        this.db.prepare(`
          DELETE FROM paper_package 
          WHERE paper_code = ? AND version = ?
        `).run(paperCode, version)
        console.log(`✓ 已从 paper_package 表删除记录`)
      } catch (error) {
        console.warn(`⚠️ 删除 paper_package 记录失败: ${error.message}`)
      }

      // 注意：不删除 paper 表的记录，保留版本信息，这样系统会检测到需要重新下载
      console.log(`⚠️ 保留 paper 表的版本信息 (version=${version})，以便重新下载`)

      console.log(`✓ [cleanCorruptedPackage] 损坏的试卷包清理完成`)
    } catch (error) {
      console.error(`❌ [cleanCorruptedPackage] 清理损坏的试卷包失败:`, error)
    }
  }

  /**
   * 删除旧版本的完整包
   * @param {string} paperCode - 试卷编码
   * @param {number} oldVersion - 旧版本号
   */
  async deleteOldVersionPackage(paperCode, oldVersion) {
    try {
      console.log(`🧹 [deleteOldVersionPackage] 删除旧版本数据: ${paperCode} v${oldVersion}`)

      const app = require('electron').app || require('@electron/remote').app
      const userDataPath = app.getPath('userData')

      // 1. 删除旧版本的媒体文件目录
      const mediaDir = path.join(userDataPath, 'media', paperCode)
      if (fs.existsSync(mediaDir)) {
        console.log(`🧹 删除旧版本媒体文件: ${mediaDir}`)
        try {
          this.deleteFolderRecursive(mediaDir)
          console.log(`✓ 已删除旧版本媒体文件`)
        } catch (error) {
          console.warn(`⚠️ 删除媒体文件失败: ${error.message}`)
        }
      }

      // 2. 删除旧版本的临时文件目录
      const tempDir = path.join(userDataPath, 'temp', paperCode)
      if (fs.existsSync(tempDir)) {
        console.log(`🧹 删除旧版本临时文件: ${tempDir}`)
        try {
          this.deleteFolderRecursive(tempDir)
          console.log(`✓ 已删除旧版本临时文件`)
        } catch (error) {
          console.warn(`⚠️ 删除临时文件失败: ${error.message}`)
        }
      }

      // 3. 删除旧版本的快速启动包
      const quickStartFileName = `${paperCode}_v${oldVersion}_quick.zip`
      const quickStartPath = path.join(this.packageBasePath, quickStartFileName)
      if (fs.existsSync(quickStartPath)) {
        console.log(`🧹 删除旧版本快速启动包: ${quickStartFileName}`)
        try {
          fs.unlinkSync(quickStartPath)
          console.log(`✓ 已删除旧版本快速启动包`)
        } catch (error) {
          console.warn(`⚠️ 删除快速启动包失败: ${error.message}`)
        }
      }

      // 4. 删除旧版本的完整包文件
      const oldPackageFileName = `${paperCode}_v${oldVersion}.zip`
      const oldPackagePath = path.join(this.packageBasePath, oldPackageFileName)

      if (fs.existsSync(oldPackagePath)) {
        console.log(`🧹 删除旧版本完整包文件: ${oldPackageFileName}`)
        try {
          fs.unlinkSync(oldPackagePath)
          console.log(`✓ 已删除旧版本完整包文件`)
        } catch (error) {
          console.warn(`⚠️ 删除完整包文件失败: ${error.message}`)
        }
      }

      // 5. 从数据库中删除旧版本的记录
      try {
        this.db.prepare(`
          DELETE FROM paper_package 
          WHERE paper_code = ? AND version = ?
        `).run(paperCode, oldVersion)
        console.log(`✓ 已从数据库删除旧版本记录`)
      } catch (error) {
        console.warn(`⚠️ 删除数据库记录失败: ${error.message}`)
      }

      console.log(`✓ [deleteOldVersionPackage] 旧版本数据删除完成`)
    } catch (error) {
      console.error(`❌ [deleteOldVersionPackage] 删除旧版本数据失败:`, error)
    }
  }
}

module.exports = PaperService

