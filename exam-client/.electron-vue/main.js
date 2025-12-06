const { app, BrowserWindow, ipcMain, nativeImage } = require('electron')
const path = require('path')
const fs = require('fs')
const { execSync } = require('child_process')
const isDev = process.env.NODE_ENV === 'development'

// 配置模块解析路径，确保能够正确加载 node_modules 中的依赖
// 这对于 Electron 主进程正确解析依赖非常重要
// 只设置 NODE_PATH 环境变量，这是最安全、最兼容的方式
const projectRoot = path.resolve(__dirname, '..')
const nodeModulesPath = path.join(projectRoot, 'node_modules')

// 设置 NODE_PATH 环境变量（最兼容的方式，不会破坏现有功能）
if (!process.env.NODE_PATH) {
  process.env.NODE_PATH = nodeModulesPath
} else if (process.env.NODE_PATH && !process.env.NODE_PATH.includes(nodeModulesPath)) {
  // 根据操作系统使用不同的分隔符
  const pathSeparator = process.platform === 'win32' ? ';' : ':'
  process.env.NODE_PATH = `${nodeModulesPath}${pathSeparator}${process.env.NODE_PATH}`
}

// 设置应用名称（用于用户数据目录路径，使用英文）
// 在开发模式下，Electron 默认使用 "Electron" 作为应用名称
// 设置后，app.getPath('userData') 会返回正确的路径（英文路径）
// 注意：必须在 app.whenReady() 之前调用
// 用户数据目录：~/Library/Application Support/zx-exam-client
app.setName('zx-exam-client')

// 提前设置应用图标（在应用启动的最早阶段，避免显示默认图标）
function setAppIconEarly() {
  let iconPath = null
  let appIcon = null
  
  // 尝试多个可能的图标路径（按优先级）
  const possibleIconPaths = []
  
  if (isDev) {
    // 开发环境：尝试源文件路径
    possibleIconPaths.push(
      path.join(__dirname, '../src/renderer/assets/images/logo.png'),
      path.join(__dirname, '../public/assets/images/logo.png'),
      path.join(__dirname, '../src/renderer/public/assets/images/logo.png')
    )
  } else {
    // 生产环境：尝试打包后的路径
    possibleIconPaths.push(
      path.join(process.resourcesPath, 'app/build/icon.png'),
      path.join(process.resourcesPath, 'app/build/icon.icns'), // macOS
      path.join(process.resourcesPath, 'app/build/icon.ico'), // Windows
      path.join(app.getAppPath(), 'build/icon.png'),
      path.join(__dirname, '../../build/icon.png'),
      path.join(__dirname, '../../build/icon.icns'), // macOS
      path.join(__dirname, '../../build/icon.ico') // Windows
    )
  }
  
  // 查找第一个存在的图标文件
  for (const iconP of possibleIconPaths) {
    if (fs.existsSync(iconP)) {
      iconPath = iconP
      break
    }
  }
  
  // 如果找到图标文件，创建 nativeImage 对象（但不立即设置 Dock）
  // 注意：app.dock 只有在 app.on('ready') 之后才可用
  if (iconPath) {
    try {
      appIcon = nativeImage.createFromPath(iconPath)
      if (appIcon.isEmpty()) {
        console.warn('⚠️ 图标文件为空或无效:', iconPath)
        appIcon = null
      }
    } catch (error) {
      console.error('❌ 加载图标失败:', error)
      appIcon = null
    }
  } else {
    console.warn('⚠️ 未找到图标文件，将使用默认 Electron 图标')
  }
  
  return { iconPath, appIcon }
}

// 提前查找图标路径（在应用启动的最早阶段）
const { iconPath: earlyIconPath, appIcon: earlyAppIcon } = setAppIconEarly()

// 在 macOS 上，在 ready 事件的最开始就设置 Dock 图标（避免闪烁）
// 使用 'ready' 事件，确保在所有其他操作之前设置图标
if (process.platform === 'darwin') {
  // 使用 once 确保只执行一次，并且优先级最高
  app.once('ready', () => {
    // 立即设置 Dock 图标（必须在创建任何窗口之前）
    // 这是避免闪烁的关键：在应用完全启动之前就设置好图标
    if (app.dock) {
      if (earlyAppIcon && !earlyAppIcon.isEmpty()) {
        app.dock.setIcon(earlyAppIcon)
        console.log('✅ macOS Dock 图标已在 ready 事件开始时设置:', earlyIconPath)
      } else if (earlyIconPath) {
        // 如果 nativeImage 创建失败，尝试重新创建
        try {
          const icon = nativeImage.createFromPath(earlyIconPath)
          if (!icon.isEmpty()) {
            app.dock.setIcon(icon)
            console.log('✅ macOS Dock 图标已设置（使用路径）:', earlyIconPath)
          } else {
            console.warn('⚠️ 图标文件为空或无效:', earlyIconPath)
          }
        } catch (error) {
          console.error('❌ 设置 Dock 图标失败:', error)
        }
      }
    }
    // 注意：app.getName() 返回的是用于用户数据目录的名称（英文）
    // 但应用显示名称由 CFBundleDisplayName 控制（中文）
    console.log('应用名称（用户数据目录）:', app.getName())
  })
}

// 添加控制台日志输出（在 app.whenReady() 中输出）
console.log('=== Electron App Starting ===')
console.log('isDev:', isDev)
console.log('__dirname:', __dirname)
console.log('应用名称（用户数据目录）:', app.getName())
console.log('用户数据目录路径:', app.getPath('userData'))

let mainWindow
let splashWindow
let db = null
let isOnline = false // 网络状态

// 初始化数据库
function initDatabase() {
  try {
    const Database = require(path.join(__dirname, '../src/database/db'))
    db = new Database()
    console.log('Database initialized successfully')
  } catch (error) {
    console.error('Database initialization failed:', error)
  }
}

// 检测网络状态
async function checkNetworkStatus() {
  try {
    const API_BASE_URL = process.env.API_BASE_URL || 'http://localhost:8080'
    // 尝试访问公共同步接口，如果成功则说明有网络
    const axios = require('axios')
    const response = await axios.get(`${API_BASE_URL}/student/syncPublicData`, {
      timeout: 5000, // 5秒超时
      headers: {
        'Content-Type': 'application/json'
      }
    })
    return response.status === 200
  } catch (error) {
    console.log('网络检测失败:', error.message)
    return false
  }
}

// 同步所有数据（启动时，无需token）
async function syncDataOnStartup() {
  if (!db) {
    console.error('数据库未初始化，无法同步数据')
    return { success: false, message: '数据库未初始化' }
  }
  
  try {
    const SyncService = require(path.join(__dirname, '../src/database/syncService'))
    const syncService = new SyncService(db)
    const result = await syncService.syncPublicData()
    
    if (result.success) {
      console.log('✓ 启动时数据同步成功（业务数据和字典数据）')
    } else {
      console.warn('启动时数据同步失败:', result.message)
    }
    
    return result
  } catch (error) {
    console.error('启动时数据同步异常:', error)
    return { success: false, message: error.message || '数据同步失败' }
  }
}

// 创建启动画面
function createSplashWindow() {
  splashWindow = new BrowserWindow({
    width: 400,
    height: 300,
    frame: false,
    transparent: true,
    alwaysOnTop: true,
    skipTaskbar: true,
    resizable: false,
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true
    }
  })

  // 启动画面的 HTML 内容
  const splashHTML = `
    <!DOCTYPE html>
    <html>
    <head>
      <meta charset="UTF-8">
      <style>
        * {
          margin: 0;
          padding: 0;
          box-sizing: border-box;
        }
        body {
          width: 100%;
          height: 100vh;
          display: flex;
          justify-content: center;
          align-items: center;
          background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
          font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
          overflow: hidden;
        }
        .splash-content {
          text-align: center;
          color: white;
        }
        .logo {
          font-size: 48px;
          font-weight: bold;
          margin-bottom: 20px;
          text-shadow: 2px 2px 4px rgba(0,0,0,0.3);
        }
        .loading-spinner {
          width: 50px;
          height: 50px;
          border: 4px solid rgba(255,255,255,0.3);
          border-top: 4px solid white;
          border-radius: 50%;
          animation: spin 1s linear infinite;
          margin: 0 auto 20px;
        }
        .loading-text {
          font-size: 16px;
          opacity: 0.9;
        }
        @keyframes spin {
          0% { transform: rotate(0deg); }
          100% { transform: rotate(360deg); }
        }
      </style>
    </head>
    <body>
      <div class="splash-content">
        <div class="logo">择学考试端</div>
        <div class="loading-spinner"></div>
        <div class="loading-text">正在启动...</div>
      </div>
    </body>
    </html>
  `
  
  splashWindow.loadURL(`data:text/html;charset=utf-8,${encodeURIComponent(splashHTML)}`)
  splashWindow.center()
}

function createWindow() {
  // 使用提前加载的图标（如果存在）
  // 如果提前加载失败，则使用默认图标
  const windowIcon = earlyAppIcon || earlyIconPath || null
  
  mainWindow = new BrowserWindow({
    width: 1200,
    height: 800,
    title: '择学考试端', // 窗口标题使用中文显示
    icon: windowIcon, // 使用提前加载的图标
    backgroundColor: '#667eea', // 设置背景色，避免白色闪烁
    show: false, // 先不显示，等加载完成后再显示
    webPreferences: {
      nodeIntegration: true,
      contextIsolation: false,
      enableRemoteModule: true,
      devTools: false // 禁用开发者工具（包括开发环境）
    }
  })

  // 禁用 F5 刷新和 Ctrl+R 刷新
  mainWindow.webContents.on('before-input-event', (event, input) => {
    // 禁用 F5 刷新
    if (input.key === 'F5') {
      event.preventDefault()
    }
    // 禁用 Ctrl+R / Cmd+R 刷新
    if ((input.control || input.meta) && input.key.toLowerCase() === 'r') {
      event.preventDefault()
    }
    // 禁用 Ctrl+Shift+R / Cmd+Shift+R 强制刷新
    if ((input.control || input.meta) && input.shift && input.key.toLowerCase() === 'r') {
      event.preventDefault()
    }
  })

  // 当页面加载完成时，关闭启动画面并显示主窗口
  mainWindow.webContents.once('did-finish-load', () => {
    // 延迟一点时间，确保内容完全渲染
    setTimeout(() => {
      if (splashWindow && !splashWindow.isDestroyed()) {
        splashWindow.close()
        splashWindow = null
      }
      mainWindow.show()
      mainWindow.focus()
    }, 300)
  })

  // 添加错误处理
  mainWindow.webContents.on('did-fail-load', (event, errorCode, errorDescription, validatedURL) => {
    console.error('Failed to load:', errorCode, errorDescription, validatedURL)
    // 关闭启动画面
    if (splashWindow && !splashWindow.isDestroyed()) {
      splashWindow.close()
      splashWindow = null
    }
    mainWindow.show()
    mainWindow.webContents.executeJavaScript(`
      document.body.innerHTML = '<div style="padding: 20px; font-family: Arial;">
        <h2>加载失败</h2>
        <p>错误代码: ${errorCode}</p>
        <p>错误描述: ${errorDescription}</p>
        <p>URL: ${validatedURL}</p>
      </div>'
    `)
  })

  if (isDev) {
    mainWindow.loadURL('http://localhost:9080')
    // 已禁用开发者工具，不再自动打开
    // mainWindow.webContents.openDevTools()
  } else {
    // 生产环境：electron-builder 打包后的路径
    // 在打包后的应用中，文件在 app.asar 中
    const htmlPath = path.join(__dirname, '../dist/web/index.html')
    console.log('Attempting to load HTML from:', htmlPath)
    
    // 检查文件是否存在
    if (fs.existsSync(htmlPath)) {
      console.log('Loading HTML from:', htmlPath)
      mainWindow.loadFile(htmlPath)
    } else {
      // 尝试其他可能的路径
      const alternativePaths = [
        path.join(process.resourcesPath, 'app/dist/web/index.html'),
        path.join(app.getAppPath(), 'dist/web/index.html'),
        path.join(__dirname, '../../dist/web/index.html')
      ]
      
      let loaded = false
      for (const altPath of alternativePaths) {
        try {
          if (fs.existsSync(altPath)) {
            console.log('Loading HTML from alternative path:', altPath)
            mainWindow.loadFile(altPath)
            loaded = true
            break
          }
        } catch (e) {
          console.error('Failed to load from:', altPath, e)
        }
      }
      
      if (!loaded) {
        console.error('Could not find index.html. Tried paths:', [htmlPath, ...alternativePaths])
        // 关闭启动画面
        if (splashWindow && !splashWindow.isDestroyed()) {
          splashWindow.close()
          splashWindow = null
        }
        mainWindow.show()
        mainWindow.webContents.executeJavaScript(`
          document.body.innerHTML = '<div style="padding: 20px; font-family: Arial;">
            <h2>文件未找到</h2>
            <p>无法找到 index.html 文件</p>
            <p>尝试的路径：</p>
            <ul>
              <li>${htmlPath}</li>
              ${alternativePaths.map(p => `<li>${p}</li>`).join('')}
            </ul>
            <p>__dirname: ${__dirname}</p>
            <p>app.getAppPath(): ${app.getAppPath()}</p>
            <p>process.resourcesPath: ${process.resourcesPath || 'undefined'}</p>
          </div>'
        `)
      }
    }
  }

  // 已禁用开发者工具，不再自动打开
  // if (isDev) {
  //   mainWindow.webContents.openDevTools()
  // }

  // 禁用所有可能打开开发者工具的快捷键
  mainWindow.webContents.on('before-input-event', (event, input) => {
    // 禁用 F12
    if (input.key === 'F12') {
      event.preventDefault()
      return
    }
    // 禁用 Ctrl+Shift+I (Windows/Linux) 或 Cmd+Option+I (macOS)
    if (input.key === 'I' && (input.control || input.meta) && input.shift) {
      event.preventDefault()
      return
    }
    // 禁用 Ctrl+Shift+J (Windows/Linux) 或 Cmd+Option+J (macOS)
    if (input.key === 'J' && (input.control || input.meta) && input.shift) {
      event.preventDefault()
      return
    }
    // 禁用 Ctrl+U (Windows/Linux) 或 Cmd+Option+U (macOS) - 查看源代码
    if (input.key === 'U' && (input.control || input.meta) && input.shift) {
      event.preventDefault()
      return
    }
  })

  // 禁用右键菜单中的"检查元素"选项
  // 注意：这会完全禁用右键菜单，包括复制、粘贴等功能
  // 如果需要保留部分右键菜单功能，可以考虑使用自定义菜单
  mainWindow.webContents.on('context-menu', (event, params) => {
    // 阻止默认的右键菜单（包含"检查元素"选项）
    event.preventDefault()
  })
  
  // 额外防护：尝试通过代码打开开发者工具时也会被阻止
  mainWindow.webContents.on('devtools-opened', () => {
    // 如果开发者工具被打开（通过其他方式），立即关闭
    mainWindow.webContents.closeDevTools()
  })

  mainWindow.on('closed', () => {
    mainWindow = null
  })

  // 如果主窗口关闭，也关闭启动画面
  mainWindow.on('close', () => {
    if (splashWindow && !splashWindow.isDestroyed()) {
      splashWindow.close()
      splashWindow = null
    }
  })
}

app.whenReady().then(async () => {
  // 输出应用路径信息
  console.log('app.getAppPath():', app.getAppPath())
  console.log('process.resourcesPath:', process.resourcesPath)
  
  // 在 macOS 上，再次确认 Dock 图标已设置（在创建窗口之前）
  // 这可以确保即使 ready 事件中的设置失败，这里也会设置
  if (process.platform === 'darwin' && app.dock) {
    if (earlyAppIcon && !earlyAppIcon.isEmpty()) {
      app.dock.setIcon(earlyAppIcon)
    } else if (earlyIconPath) {
      try {
        const icon = nativeImage.createFromPath(earlyIconPath)
        if (!icon.isEmpty()) {
          app.dock.setIcon(icon)
        }
      } catch (error) {
        // 静默失败，避免重复日志
      }
    }
  }
  
  // 先创建启动画面
  createSplashWindow()
  
  // 初始化数据库
  initDatabase()
  
  // 检测网络状态
  console.log('检测网络状态...')
  isOnline = await checkNetworkStatus()
  console.log('网络状态:', isOnline ? '在线' : '离线')
  
  // 如果有网络，同步所有业务数据和字典数据
  if (isOnline) {
    console.log('网络可用，开始同步所有业务数据和字典数据...')
    await syncDataOnStartup()
  } else {
    console.log('网络不可用，跳过数据同步')
  }
  
  // 创建主窗口（但不立即显示）
  createWindow()

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      createSplashWindow()
      createWindow()
    }
  })
})

// 已移除启动时同步逻辑，改为登录成功后同步（需要 token）

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit()
  }
})

app.on('before-quit', () => {
  // 关闭数据库连接
  if (db) {
    db.close()
  }
})

// IPC 通信：登录相关
ipcMain.handle('login:online', async (event, { username, password, code, uuid }) => {
  console.log('IPC收到登录请求:', { username, code: code ? '已提供' : '未提供', uuid: uuid ? '已提供' : '未提供' })
  
  if (!db) {
    console.error('数据库未初始化')
    return { success: false, message: '数据库未初始化' }
  }
  try {
    const LoginService = require(path.join(__dirname, '../src/database/loginService'))
    const loginService = new LoginService(db)
    const result = await loginService.onlineLogin(username, password, code, uuid)
    console.log('登录服务返回结果:', result)
    
    // 登录成功后，使用 token 同步所有数据（包括所有学员档案和字典数据）
    // 注意：当前学员的档案信息已经在 getInfo 接口返回并保存，这里同步是为了获取所有学员档案和字典数据
    if (result.success && result.token) {
      console.log('登录成功，开始同步所有数据（所有学员档案和字典数据）...')
      try {
        const SyncService = require(path.join(__dirname, '../src/database/syncService'))
        const syncService = new SyncService(db)
        const syncResult = await syncService.syncAll(result.token)
        
        if (syncResult.success) {
          console.log('✓ 所有数据同步成功（包括所有学员档案和字典数据）')
        } else {
          console.warn('数据同步失败，但登录成功（当前学员数据已从 getInfo 获取）:', syncResult.message)
        }
      } catch (syncError) {
        console.error('数据同步异常，但登录成功（当前学员数据已从 getInfo 获取）:', syncError)
        // 不抛出异常，登录仍然成功，因为当前学员的数据已经从 getInfo 获取
      }
    }
    
    return result
  } catch (error) {
    console.error('Online login error:', error)
    console.error('错误堆栈:', error.stack)
    return { success: false, message: error.message }
  }
})

ipcMain.handle('login:offline', async (event, { username, password, offlineCredential }) => {
  if (!db) {
    return { success: false, message: '数据库未初始化' }
  }
  try {
    const LoginService = require(path.join(__dirname, '../src/database/loginService'))
    const loginService = new LoginService(db)
    return await loginService.offlineLogin(username, password, offlineCredential)
  } catch (error) {
    console.error('Offline login error:', error)
    return { success: false, message: error.message }
  }
})

// IPC 通信：获取学员信息
ipcMain.handle('login:getStudentInfo', async (event, token) => {
  if (!db) {
    console.error('数据库未初始化')
    return null
  }
  try {
    const LoginService = require(path.join(__dirname, '../src/database/loginService'))
    const loginService = new LoginService(db)
    return await loginService.getStudentInfo(token)
  } catch (error) {
    console.error('Get student info error:', error)
    return null
  }
})

ipcMain.handle('login:getStudentPapers', async (event, studentAccount) => {
  if (!db) {
    return []
  }
  try {
    const LoginService = require(path.join(__dirname, '../src/database/loginService'))
    const loginService = new LoginService(db)
    return loginService.getStudentPapers(studentAccount)
  } catch (error) {
    console.error('Get student papers error:', error)
    return []
  }
})

// IPC 通信：根据 user_id 获取学员试卷类型
ipcMain.handle('login:getStudentPapersByUserId', async (event, userId) => {
  if (!db) {
    return []
  }
  try {
    const LoginService = require(path.join(__dirname, '../src/database/loginService'))
    const loginService = new LoginService(db)
    return loginService.getStudentPapersByUserId(userId)
  } catch (error) {
    console.error('Get student papers by user_id error:', error)
    return []
  }
})

// IPC 通信：获取字典数据
ipcMain.handle('dict:getDictData', async (event, dictType) => {
  if (!db) {
    return []
  }
  try {
    const LoginService = require(path.join(__dirname, '../src/database/loginService'))
    const loginService = new LoginService(db)
    return loginService.getDictData(dictType)
  } catch (error) {
    console.error('Get dict data error:', error)
    return []
  }
})

// IPC 通信：获取应用数据目录
ipcMain.handle('app:getUserDataPath', () => {
  return app.getPath('userData')
})

// IPC 通信：获取网络状态
ipcMain.handle('app:getNetworkStatus', () => {
  return isOnline
})

// IPC 通信：手动触发数据同步
ipcMain.handle('sync:syncAll', async (event, token = null) => {
  if (!db) {
    return { success: false, message: '数据库未初始化' }
  }
  try {
    const SyncService = require(path.join(__dirname, '../src/database/syncService'))
    const syncService = new SyncService(db)
    return await syncService.syncAll(token)
  } catch (error) {
    console.error('数据同步失败:', error)
    return { success: false, message: error.message || '数据同步失败' }
  }
})

// IPC 通信：获取学员档案（从本地数据库）
ipcMain.handle('archive:getByAccount', async (event, studentAccount) => {
  if (!db) {
    return null
  }
  try {
    const SyncService = require(path.join(__dirname, '../src/database/syncService'))
    const syncService = new SyncService(db)
    return syncService.getStudentArchiveByAccount(studentAccount)
  } catch (error) {
    console.error('获取学员档案失败:', error)
    return null
  }
})

// IPC 通信：获取学员档案（根据 user_id）
ipcMain.handle('archive:getByUserId', async (event, userId) => {
  if (!db) {
    return null
  }
  try {
    const SyncService = require(path.join(__dirname, '../src/database/syncService'))
    const syncService = new SyncService(db)
    return syncService.getStudentArchiveByUserId(userId)
  } catch (error) {
    console.error('获取学员档案失败:', error)
    return null
  }
})

// IPC 通信：根据试卷类型查询试卷列表
ipcMain.handle('paper:getPapersByType', async (event, paperType) => {
  if (!db) {
    return []
  }
  try {
    // 获取实际的数据库对象
    const database = db.getDB()
    
    // 从SQLite的paper表中查询指定类型的试卷（包含year, month, province字段）
    const papers = database.prepare(`
      SELECT id, paper_name, paper_code, paper_type, paper_desc,
             year, month, province, custom_name,
             total_score, total_questions, duration, status,
             version, package_hash, last_package_time
      FROM paper
      WHERE paper_type = ? AND status = 1
      ORDER BY id DESC
    `).all(paperType)
    
    console.log(`根据试卷类型查询试卷，类型: ${paperType}，找到 ${papers.length} 条记录`)
    return papers
  } catch (error) {
    console.error('根据试卷类型查询试卷失败:', error)
    return []
  }
})

// IPC 通信：根据试卷ID列表查询试卷列表
// 优化：支持离线模式，如果paper表中没有数据，从ZIP包的manifest.json中提取
ipcMain.handle('paper:getPapersByIds', async (event, paperIds) => {
  if (!db) {
    return []
  }
  try {
    if (!paperIds || !Array.isArray(paperIds) || paperIds.length === 0) {
      console.log('试卷ID列表为空，返回空数组')
      return []
    }
    
    // 获取实际的数据库对象
    const database = db.getDB()
    
    // 构建IN查询的占位符
    const placeholders = paperIds.map(() => '?').join(',')
    
    // 1. 优先从SQLite的paper表中查询指定ID的试卷
    let papers = database.prepare(`
      SELECT id, paper_name, paper_code, paper_type, paper_desc,
             total_score, total_questions, duration, status,
             year, month, province, version, package_hash, last_package_time,
             practice_limit, enable_start_time, enable_end_time
      FROM paper
      WHERE id IN (${placeholders}) AND status = 1
      ORDER BY id DESC
    `).all(...paperIds)
    
    console.log(`根据试卷ID列表查询试卷，ID列表: ${JSON.stringify(paperIds)}，从paper表找到 ${papers.length} 条记录`)
    
    // 2. 如果paper表中没有找到所有试卷，尝试从ZIP包的manifest.json中提取（离线降级方案）
    if (papers.length < paperIds.length) {
      console.log(`paper表中只找到 ${papers.length} 条记录，但需要 ${paperIds.length} 条，尝试从ZIP包manifest.json中提取...`)
      
      const PaperService = require(path.join(__dirname, '../src/database/paperService'))
      const paperService = new PaperService(db)
      
      // 找出缺失的试卷ID
      const foundIds = new Set(papers.map(p => p.id))
      const missingIds = paperIds.filter(id => !foundIds.has(id))
      
      console.log(`缺失的试卷ID: ${JSON.stringify(missingIds)}`)
      
      // 尝试从ZIP包中提取试卷信息
      // 方案A：如果paper表中有paper_code记录，直接使用
      // 方案B：如果paper表中没有记录，扫描所有ZIP包，从manifest.json中查找匹配的paperId
      const paperRecordsWithCode = []
      const paperRecordsWithoutCode = []
      
      for (const missingId of missingIds) {
        const paperRecord = database.prepare(`
          SELECT id, paper_code, version FROM paper WHERE id = ? LIMIT 1
        `).get(missingId)
        
        if (paperRecord && paperRecord.paper_code) {
          paperRecordsWithCode.push({ id: missingId, paperCode: paperRecord.paper_code, version: paperRecord.version || 1 })
        } else {
          paperRecordsWithoutCode.push(missingId)
        }
      }
      
      // 处理有paper_code的记录（直接提取）
      for (const record of paperRecordsWithCode) {
        try {
          const { id: missingId, paperCode, version } = record
          console.log(`尝试从ZIP包提取试卷信息: paperId=${missingId}, paperCode=${paperCode}, version=${version}`)
          
          // 尝试从快速启动包或完整包中提取manifest.json
          let manifest = null
          
          // 优先尝试快速启动包
          try {
            const quickStartResult = await paperService.extractQuickStartPackage(paperCode, version)
            if (quickStartResult && quickStartResult.manifest) {
              manifest = quickStartResult.manifest
              console.log(`✓ 从快速启动包提取到manifest: ${paperCode}`)
            }
          } catch (error) {
            console.log(`快速启动包不存在或解压失败: ${error.message}`)
          }
          
          // 如果快速启动包失败，尝试完整包
          if (!manifest) {
            try {
              const extractResult = await paperService.extractPaperPackage(paperCode, version)
              if (extractResult && extractResult.manifest) {
                manifest = extractResult.manifest
                console.log(`✓ 从完整包提取到manifest: ${paperCode}`)
              }
            } catch (error) {
              console.log(`完整包不存在或解压失败: ${error.message}`)
            }
          }
          
          // 如果成功提取到manifest，更新或插入paper表记录
          if (manifest && manifest.paperId === missingId) {
            await updateOrInsertPaperFromManifest(database, manifest, missingId, paperCode, version)
          }
        } catch (error) {
          console.warn(`从ZIP包提取试卷信息失败: paperId=${record.id}，错误: ${error.message}`)
        }
      }
      
      // 处理没有paper_code的记录（扫描所有ZIP包）
      if (paperRecordsWithoutCode.length > 0) {
        console.log(`扫描所有ZIP包，查找试卷ID: ${JSON.stringify(paperRecordsWithoutCode)}`)
        
        try {
          const packageBasePath = path.join(app.getPath('userData'), 'paper_packages')
          if (fs.existsSync(packageBasePath)) {
            const files = fs.readdirSync(packageBasePath)
            const zipFiles = files.filter(file => 
              file.endsWith('.zip') && (file.endsWith('_quick.zip') || !file.includes('_quick'))
            )
            
            console.log(`找到 ${zipFiles.length} 个ZIP文件，开始扫描manifest.json...`)
            
            for (const zipFile of zipFiles) {
              try {
                // 解析文件名：{paperCode}_v{version}.zip 或 {paperCode}_v{version}_quick.zip
                const match = zipFile.match(/^(.+)_v(\d+)(_quick)?\.zip$/)
                if (!match) continue
                
                const paperCode = match[1]
                const version = parseInt(match[2], 10)
                const isQuickStart = !!match[3]
                
                // 尝试提取manifest.json
                let manifest = null
                
                if (isQuickStart) {
                  try {
                    const quickStartResult = await paperService.extractQuickStartPackage(paperCode, version)
                    if (quickStartResult && quickStartResult.manifest) {
                      manifest = quickStartResult.manifest
                    }
                  } catch (error) {
                    // 忽略错误，继续尝试完整包
                  }
                } else {
                  try {
                    const extractResult = await paperService.extractPaperPackage(paperCode, version)
                    if (extractResult && extractResult.manifest) {
                      manifest = extractResult.manifest
                    }
                  } catch (error) {
                    // 忽略错误，继续下一个文件
                  }
                }
                
                // 如果manifest中的paperId在缺失列表中，更新或插入paper表
                if (manifest && manifest.paperId && paperRecordsWithoutCode.includes(manifest.paperId)) {
                  console.log(`✓ 从ZIP包 ${zipFile} 中找到匹配的试卷: paperId=${manifest.paperId}, paperCode=${paperCode}`)
                  await updateOrInsertPaperFromManifest(database, manifest, manifest.paperId, paperCode, version)
                  
                  // 从缺失列表中移除
                  const index = paperRecordsWithoutCode.indexOf(manifest.paperId)
                  if (index > -1) {
                    paperRecordsWithoutCode.splice(index, 1)
                  }
                  
                  // 如果所有缺失的试卷都已找到，提前退出
                  if (paperRecordsWithoutCode.length === 0) {
                    break
                  }
                }
              } catch (error) {
                console.warn(`扫描ZIP包失败: ${zipFile}，错误: ${error.message}`)
              }
            }
          }
        } catch (error) {
          console.warn(`扫描ZIP包目录失败: ${error.message}`)
        }
      }
      
      // 辅助函数：从manifest更新或插入paper表记录
      async function updateOrInsertPaperFromManifest(database, manifest, paperId, paperCode, version) {
        const existingPaper = database.prepare(`
          SELECT id FROM paper WHERE id = ? LIMIT 1
        `).get(paperId)
        
        if (existingPaper) {
          // 更新现有记录（从manifest中获取的信息）
          database.prepare(`
            UPDATE paper 
            SET paper_name = ?, paper_code = ?, paper_type = ?, paper_desc = ?,
                total_score = ?, total_questions = ?, duration = ?,
                practice_limit = ?, trial_listen_enabled = ?, trial_listen_text = ?,
                notes = ?, notes_display_mode = ?,
                version = ?, status = 1, update_time = ?
            WHERE id = ?
          `).run(
            manifest.paperName || paperCode,
            manifest.paperCode || paperCode,
            manifest.paperType || null,
            manifest.paperDesc || null,
            manifest.totalScore || 0,
            manifest.totalQuestions || 0,
            manifest.duration || null,
            manifest.practiceLimit || 0,
            manifest.trialListenEnabled ? 1 : 0,
            manifest.trialListenText || null,
            manifest.notes || null,
            manifest.notesDisplayMode || 'before_exam',
            manifest.version || version,
            Date.now(),
            paperId
          )
          console.log(`✓ 已从manifest.json更新paper表记录: paperId=${paperId}, paperCode=${paperCode}`)
        } else {
          // 插入新记录（注意：manifest.json不包含year, month, province等字段，这些字段为null）
          database.prepare(`
            INSERT INTO paper 
            (id, paper_name, paper_code, paper_type, paper_desc,
             total_score, total_questions, duration,
             practice_limit, trial_listen_enabled, trial_listen_text,
             notes, notes_display_mode,
             version, status, create_time)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
          `).run(
            manifest.paperId || paperId,
            manifest.paperName || paperCode,
            manifest.paperCode || paperCode,
            manifest.paperType || null,
            manifest.paperDesc || null,
            manifest.totalScore || 0,
            manifest.totalQuestions || 0,
            manifest.duration || null,
            manifest.practiceLimit || 0,
            manifest.trialListenEnabled ? 1 : 0,
            manifest.trialListenText || null,
            manifest.notes || null,
            manifest.notesDisplayMode || 'before_exam',
            manifest.version || version,
            1, // status = 1 (启用)
            Date.now()
          )
          console.log(`✓ 已从manifest.json插入paper表新记录: paperId=${paperId}, paperCode=${paperCode}`)
        }
      }
      
      // 3. 重新查询paper表（可能已经更新）
      papers = database.prepare(`
        SELECT id, paper_name, paper_code, paper_type, paper_desc,
               total_score, total_questions, duration, status,
               year, month, province, version, package_hash, last_package_time,
               practice_limit, enable_start_time, enable_end_time
        FROM paper
        WHERE id IN (${placeholders}) AND status = 1
        ORDER BY id DESC
      `).all(...paperIds)
      
      console.log(`从ZIP包提取后，重新查询paper表，找到 ${papers.length} 条记录`)
    }
    
    return papers
  } catch (error) {
    console.error('根据试卷ID列表查询试卷失败:', error)
    return []
  }
})

// IPC 通信：获取试卷包的 manifest 和 questions 数据
ipcMain.handle('paper:getPaperData', async (event, paperId) => {
  if (!db) {
    console.error('paper:getPaperData: 数据库未初始化')
    return null
  }
  try {
    console.log(`paper:getPaperData: 开始获取试卷数据，paperId=${paperId}`)
    
    const PaperService = require(path.join(__dirname, '../src/database/paperService'))
    const paperService = new PaperService(db)
    
    // 获取试卷信息
    const paper = db.getDB().prepare(`
      SELECT id, paper_code, version FROM paper WHERE id = ? LIMIT 1
    `).get(paperId)
    
    if (!paper) {
      console.error(`paper:getPaperData: 试卷不存在，paperId=${paperId}`)
      throw new Error(`试卷不存在（ID: ${paperId}）`)
    }
    
    console.log(`paper:getPaperData: 找到试卷，paper_code=${paper.paper_code}, version=${paper.version || 'null'}`)
    
    // 检查版本号，如果为null则使用默认值1
    const version = paper.version || 1
    console.log(`paper:getPaperData: 使用版本号 ${version}`)
    
    // 检查完整ZIP包是否存在
    const packageInfo = db.getDB().prepare(`
      SELECT paper_code, version, is_active FROM paper_package 
      WHERE paper_code = ? AND is_active = 1
      ORDER BY version DESC
      LIMIT 1
    `).get(paper.paper_code)
    
    if (packageInfo) {
      // 完整包存在，使用完整包
      console.log(`paper:getPaperData: 找到完整ZIP包，version=${packageInfo.version}`)
      
      // 解压完整试卷包并获取 manifest 和 questions
      try {
        const extractResult = await paperService.extractPaperPackage(paper.paper_code, version)
        
        if (!extractResult) {
          console.error(`paper:getPaperData: 解压完整试卷包失败，paper_code=${paper.paper_code}`)
          throw new Error('解压试卷包失败')
        }
        
        if (!extractResult.manifest || !extractResult.questions) {
          console.error(`paper:getPaperData: 解压结果不完整，paper_code=${paper.paper_code}`)
          throw new Error('试卷包数据不完整')
        }
        
        console.log(`paper:getPaperData: 成功从完整包获取试卷数据，paper_code=${paper.paper_code}`)
        
        return {
          manifest: extractResult.manifest,
          questions: extractResult.questions,
          mediaDir: extractResult.mediaDir
        }
      } catch (extractError) {
        // 检测到ZIP损坏，自动清理（但保留版本信息，以便重新下载）
        if (extractError.message && (extractError.message.includes('Invalid or unsupported zip format') || 
            extractError.message.includes('No END header found'))) {
          console.error(`❌ 检测到ZIP包损坏，自动清理: ${paper.paper_code} v${packageInfo.version}`)
          
          try {
            // 清理损坏的包（但保留paper表的版本信息，以便重新下载）
            await paperService.cleanCorruptedPackage(paper.paper_code, packageInfo.version)
            
            // 清除下载状态
            await paperService.updateDownloadStatus(paperId, paper.paper_code, 'error', 0, 0, 0, 'ZIP文件损坏，已自动清理')
            
            console.log(`✓ 已清理损坏的ZIP包，保留版本信息以便重新下载`)
          } catch (deleteError) {
            console.error(`清理损坏数据失败:`, deleteError)
          }
          
          // 抛出友好的错误信息
          throw new Error('试卷包文件已损坏，已自动清理，请返回试卷列表重新下载')
        }
        
        // 其他错误直接抛出
        throw extractError
      }
    }
    
    // 完整包不存在，尝试使用快速启动包
    console.log(`paper:getPaperData: 完整ZIP包不存在，尝试使用快速启动包，paper_code=${paper.paper_code}`)
    
    try {
      // 尝试解压快速启动包（只包含manifest.json和trial_listen/、intro/等）
      const quickStartResult = await paperService.extractQuickStartPackage(paper.paper_code, version)
      
      if (quickStartResult && quickStartResult.manifest) {
        console.log(`paper:getPaperData: 成功从快速启动包获取manifest，paper_code=${paper.paper_code}`)
        
        // 从快速启动包获取manifest，questions为空数组（完整包正在后台下载）
        return {
          manifest: quickStartResult.manifest,
          questions: [], // 快速启动包不包含questions.json，返回空数组
          mediaDir: quickStartResult.mediaDir,
          isQuickStart: true // 标记这是快速启动包
        }
      }
    } catch (error) {
      console.warn(`paper:getPaperData: 快速启动包也不存在或解压失败，paper_code=${paper.paper_code}，错误: ${error.message}`)
    }
    
    // 快速启动包也不存在，抛出错误
    throw new Error(`试卷包未同步，请先同步试卷包（试卷编码: ${paper.paper_code}）`)
  } catch (error) {
    console.error('paper:getPaperData: 获取试卷数据失败:', error.message)
    console.error('错误堆栈:', error.stack)
    // 返回错误信息，而不是null，这样前端可以显示具体的错误
    throw error
  }
})

// IPC 通信：获取试卷的卷别列表
// 优先从本地JSON数据读取，确保中文编码正确
ipcMain.handle('paper:getVolumes', async (event, paperId) => {
  if (!db) {
    return []
  }
  try {
    // 优先从本地JSON文件读取（从manifest中获取volumes）
    try {
      const PaperService = require(path.join(__dirname, '../src/database/paperService'))
      const paperService = new PaperService(db)
      
      const paper = db.getDB().prepare(`
        SELECT id, paper_code, version FROM paper WHERE id = ? LIMIT 1
      `).get(paperId)
      
      if (paper) {
        const version = paper.version || 1
        const paperCode = paper.paper_code
        
        // 检查完整包或快速启动包是否存在
        const packageInfo = db.getDB().prepare(`
          SELECT paper_code, version, is_active FROM paper_package 
          WHERE paper_code = ? AND is_active = 1
          ORDER BY version DESC
          LIMIT 1
        `).get(paperCode)
        
        if (packageInfo) {
          // 从本地JSON文件读取manifest
          const extractResult = await paperService.extractPaperPackage(paperCode, version)
          if (extractResult && extractResult.manifest && extractResult.manifest.volumes) {
            console.log(`paper:getVolumes: 从本地JSON文件读取volumes数据，paper_code=${paperCode}`)
            
            // 从manifest中提取volumes
            const volumes = extractResult.manifest.volumes.map(volume => ({
              id: volume.id || volume.volumeId,
              paper_id: paperId,
              volume_code: String(volume.volumeCode || volume.volume_code || ''),
              volume_name: String(volume.volumeName || volume.volume_name || ''),
              volume_order: volume.volumeOrder || volume.volume_order || 0
            })).sort((a, b) => (a.volume_order || 0) - (b.volume_order || 0))
            
            if (volumes.length > 0) {
              return volumes
            }
          }
        } else {
          // 尝试快速启动包
          try {
            const quickStartResult = await paperService.extractQuickStartPackage(paperCode, version)
            if (quickStartResult && quickStartResult.manifest && quickStartResult.manifest.volumes) {
              console.log(`paper:getVolumes: 从快速启动包读取volumes数据，paper_code=${paperCode}`)
              
              const volumes = quickStartResult.manifest.volumes.map(volume => ({
                id: volume.id || volume.volumeId,
                paper_id: paperId,
                volume_code: String(volume.volumeCode || volume.volume_code || ''),
                volume_name: String(volume.volumeName || volume.volume_name || ''),
                volume_order: volume.volumeOrder || volume.volume_order || 0
              })).sort((a, b) => (a.volume_order || 0) - (b.volume_order || 0))
              
              if (volumes.length > 0) {
                return volumes
              }
            }
          } catch (quickStartError) {
            console.warn(`快速启动包读取失败: ${quickStartError.message}`)
          }
        }
      }
    } catch (jsonError) {
      console.warn(`paper:getVolumes: 从本地JSON文件读取失败，降级到数据库查询: ${jsonError.message}`)
    }
    
    // 降级方案：从数据库读取
    console.log(`paper:getVolumes: 从数据库读取volumes数据（降级方案），paperId=${paperId}`)
    const database = db.getDB()
    const volumes = database.prepare(`
      SELECT id, paper_id, volume_code, volume_name, volume_order
      FROM paper_volume
      WHERE paper_id = ?
      ORDER BY volume_order ASC
    `).all(paperId)
    
    // 确保所有文本字段都是字符串类型，正确处理中文
    return volumes.map(v => ({
      ...v,
      volume_code: String(v.volume_code || ''),
      volume_name: String(v.volume_name || '')
    }))
  } catch (error) {
    console.error('获取卷别列表失败:', error)
    return []
  }
})

// IPC 通信：获取试卷的大题列表
ipcMain.handle('paper:getSections', async (event, paperId, volumeId = null, volumeCode = null) => {
  if (!db) {
    return []
  }
  try {
    const database = db.getDB()
    let query = `
      SELECT id, paper_id, volume_id, volume_code, section_name, section_order,
             question_count, total_score, score_per_question, instruction_text, audio_play_count, answer_time
      FROM paper_section
      WHERE paper_id = ?
    `
    const params = [paperId]
    
    // 优先使用 volumeId，如果没有则使用 volumeCode 查找对应的 volume_id
    if (volumeId) {
      query += ' AND volume_id = ?'
      params.push(volumeId)
    } else if (volumeCode) {
      // 通过 volume_code 查找对应的 volume_id
      const volumeRecord = database.prepare(`
        SELECT id FROM paper_volume WHERE paper_id = ? AND volume_code = ? LIMIT 1
      `).get(paperId, volumeCode)
      
      if (volumeRecord) {
        query += ' AND volume_id = ?'
        params.push(volumeRecord.id)
      } else {
        // 如果找不到对应的 volume_id，返回空数组
        return []
      }
    }
    
    query += ' ORDER BY volume_id ASC, section_order ASC'
    
    const sections = database.prepare(query).all(...params)
    return sections
  } catch (error) {
    console.error('获取大题列表失败:', error)
    return []
  }
})

// IPC 通信：获取试卷的中场配置列表
ipcMain.handle('paper:getIntermissions', async (event, paperId) => {
  if (!db) {
    return []
  }
  try {
    const database = db.getDB()
    const intermissions = database.prepare(`
      SELECT id, paper_id, from_volume, to_volume, intermission_text, can_skip
      FROM paper_intermission
      WHERE paper_id = ?
      ORDER BY from_volume ASC, to_volume ASC
    `).all(paperId)
    return intermissions
  } catch (error) {
    console.error('获取中场配置列表失败:', error)
    return []
  }
})

// IPC 通信：获取媒体文件路径（根据关联ID和媒体类型）
ipcMain.handle('paper:getMediaFiles', async (event, params) => {
  if (!db) {
    return []
  }
  try {
    const database = db.getDB()
    const { paperId, volumeId, sectionId, intermissionId, questionId, mediaType } = params
    
    let query = `
      SELECT id, media_name, media_path, media_url, media_format, media_duration
      FROM question_media
      WHERE 1=1
    `
    const queryParams = []
    
    if (paperId) {
      query += ' AND paper_id = ?'
      queryParams.push(paperId)
    }
    if (volumeId) {
      query += ' AND volume_id = ?'
      queryParams.push(volumeId)
    }
    if (sectionId) {
      query += ' AND section_id = ?'
      queryParams.push(sectionId)
    }
    if (intermissionId) {
      query += ' AND intermission_id = ?'
      queryParams.push(intermissionId)
    }
    if (questionId) {
      query += ' AND question_id = ?'
      queryParams.push(questionId)
    }
    if (mediaType !== undefined && mediaType !== null) {
      query += ' AND media_type = ?'
      queryParams.push(mediaType)
    }
    
    const mediaFiles = database.prepare(query).all(...queryParams)
    return mediaFiles
  } catch (error) {
    console.error('获取媒体文件失败:', error)
    return []
  }
})

// IPC 通信：获取题目列表（根据试卷ID和大题ID）
// 优先从本地JSON文件读取，确保中文编码正确
ipcMain.handle('paper:getQuestions', async (event, paperId, sectionId = null) => {
  if (!db) {
    return []
  }
  try {
    const PaperService = require(path.join(__dirname, '../src/database/paperService'))
    const paperService = new PaperService(db)
    
    // 获取试卷信息
    const paper = db.getDB().prepare(`
      SELECT id, paper_code, version FROM paper WHERE id = ? LIMIT 1
    `).get(paperId)
    
    if (!paper) {
      console.error(`paper:getQuestions: 试卷不存在，paperId=${paperId}`)
      return []
    }
    
    const version = paper.version || 1
    const paperCode = paper.paper_code
    
    // 优先从本地JSON文件读取questions数据
    try {
      // 检查完整包是否存在
      const packageInfo = db.getDB().prepare(`
        SELECT paper_code, version, is_active FROM paper_package 
        WHERE paper_code = ? AND is_active = 1
        ORDER BY version DESC
        LIMIT 1
      `).get(paperCode)
      
      if (packageInfo) {
        // 完整包存在，从本地JSON文件读取
        const extractResult = await paperService.extractPaperPackage(paperCode, version)
        if (extractResult && extractResult.questions && Array.isArray(extractResult.questions)) {
          console.log(`paper:getQuestions: 从本地JSON文件读取题目数据，paper_code=${paperCode}，题目数量=${extractResult.questions.length}`)
          
          // 如果指定了sectionId，需要过滤题目
          if (sectionId) {
            // 获取section的volume_code
            const section = db.getDB().prepare(`
              SELECT volume_code FROM paper_section WHERE id = ? LIMIT 1
            `).get(sectionId)
            
            if (section) {
              // 从questions.json中筛选出属于该section的题目
              // questions.json中的题目可能通过section_id或volume_code关联
              const filteredQuestions = extractResult.questions.filter(q => {
                // 检查题目是否属于该section（通过section_id或volume_code匹配）
                return (q.section_id === sectionId) || 
                       (q.sectionId === sectionId) ||
                       (q.volume_code === section.volume_code) ||
                       (q.volumeCode === section.volume_code)
              })
              
              // 按sort_order或question_sort排序
              filteredQuestions.sort((a, b) => {
                const sortA = a.sort_order || a.question_sort || a.sortOrder || 0
                const sortB = b.sort_order || b.question_sort || b.sortOrder || 0
                return sortA - sortB
              })
              
              // 确保所有文本字段都是字符串类型，正确处理中文
              return filteredQuestions.map(q => ({
                ...q,
                id: q.id || q.question_id || q.questionId,
                question_id: q.question_id || q.questionId || q.id,
                title: String(q.title || ''),
                text: String(q.text || ''),
                answer: String(q.answer || ''),
                analyzes: String(q.analyzes || ''),
                explanation_text: String(q.explanation_text || q.explanationText || ''),
                section_id: sectionId,
                section_order: q.section_order || q.sectionOrder || 0,
                sort_order: q.sort_order || q.question_sort || q.sortOrder || 0,
                score: q.score || 0
              }))
            }
          }
          
          // 如果没有指定sectionId，返回所有题目
          // 确保所有文本字段都是字符串类型，正确处理中文
          return extractResult.questions.map(q => ({
            ...q,
            id: q.id || q.question_id || q.questionId,
            question_id: q.question_id || q.questionId || q.id,
            title: String(q.title || ''),
            text: String(q.text || ''),
            answer: String(q.answer || ''),
            analyzes: String(q.analyzes || ''),
            explanation_text: String(q.explanation_text || q.explanationText || ''),
            section_order: q.section_order || q.sectionOrder || 0,
            sort_order: q.sort_order || q.question_sort || q.sortOrder || 0,
            score: q.score || 0
          })).sort((a, b) => {
            const sortA = a.sort_order || a.question_sort || a.sortOrder || 0
            const sortB = b.sort_order || b.question_sort || b.sortOrder || 0
            return sortA - sortB
          })
        }
      }
    } catch (jsonError) {
      console.warn(`paper:getQuestions: 从本地JSON文件读取失败，降级到数据库查询: ${jsonError.message}`)
    }
    
    // 降级方案：从数据库读取（如果JSON文件不存在或读取失败）
    console.log(`paper:getQuestions: 从数据库读取题目数据（降级方案），paperId=${paperId}, sectionId=${sectionId || 'all'}`)
    const database = db.getDB()
    let query = `
      SELECT pq.question_id, pq.section_id, pq.section_order, pq.sort_order, pq.score,
             q.id, q.title, q.type, q.option_type, q.answer, q.analyzes,
             q.explanation_enabled, q.explanation_text, q.explanation_delay_seconds
      FROM paper_question pq
      INNER JOIN question q ON pq.question_id = q.id
      WHERE pq.paper_id = ?
    `
    const params = [paperId]
    
    if (sectionId) {
      query += ' AND pq.section_id = ?'
      params.push(sectionId)
    }
    
    query += ' ORDER BY pq.section_order ASC, pq.sort_order ASC'
    
    const questions = database.prepare(query).all(...params)
    
    // 确保所有文本字段都是字符串类型，正确处理中文
    return questions.map(q => ({
      ...q,
      title: String(q.title || ''),
      answer: String(q.answer || ''),
      analyzes: String(q.analyzes || ''),
      explanation_text: String(q.explanation_text || '')
    }))
  } catch (error) {
    console.error('获取题目列表失败:', error)
    return []
  }
})

// IPC 通信：获取应用路径
ipcMain.handle('app:getPath', async (event, name) => {
  try {
    return app.getPath(name)
  } catch (error) {
    console.error('获取应用路径失败:', error)
    return null
  }
})

// IPC 通信：控制系统音量
ipcMain.handle('system:setVolume', async (event, volume) => {
  try {
    // volume 范围：0-100
    const clampedVolume = Math.max(0, Math.min(100, volume))
    
    if (process.platform === 'darwin') {
      // macOS: 使用 osascript 设置音量
      // macOS 音量范围是 0-100，但实际输出音量范围是 0-7（或 0-100，取决于系统设置）
      // 这里使用 0-100 的范围，osascript 会自动转换
      const script = `osascript -e "set volume output volume ${clampedVolume}"`
      execSync(script)
      console.log(`✓ 系统音量已设置为: ${clampedVolume}%`)
      return { success: true, volume: clampedVolume }
    } else if (process.platform === 'win32') {
      // Windows: 使用 PowerShell 设置音量
      // Windows 音量范围是 0-100
      const script = `powershell -command "(New-Object -comObject WScript.Shell).SendKeys([char]${Math.floor(clampedVolume / 100 * 50)})"`
      // 更可靠的方法：使用 AudioEndpointVolume COM 对象
      const psScript = `
        $wshShell = New-Object -ComObject WScript.Shell
        $wshShell.SendKeys([char]175)  # 先静音
        $wshShell.SendKeys([char]174)  # 取消静音
        Add-Type -TypeDefinition @"
          using System;
          using System.Runtime.InteropServices;
          public class Audio {
            [DllImport("user32.dll")]
            public static extern void keybd_event(byte bVk, byte bScan, int dwFlags, int dwExtraInfo);
            public static void SetVolume(int volume) {
              int vk = 0xAE; // VK_VOLUME_DOWN
              int vk2 = 0xAF; // VK_VOLUME_UP
              // 简化处理：直接设置到目标音量（需要多次按键）
              // 实际应用中可能需要更复杂的逻辑
            }
          }
"@
        $volume = ${clampedVolume}
        $wshShell.SendKeys([char]175)  # 静音
        Start-Sleep -Milliseconds 100
        $wshShell.SendKeys([char]174)  # 取消静音
      `
      // 使用更简单的方法：通过音量键设置
      // 注意：这种方法不够精确，但可以工作
      console.log(`⚠️ Windows 音量控制需要更复杂的实现，当前仅支持应用内音量`)
      return { success: false, message: 'Windows 系统音量控制需要额外实现' }
    } else {
      // Linux: 使用 amixer 或 pactl
      try {
        // 尝试使用 pactl (PulseAudio)
        const script = `pactl set-sink-volume @DEFAULT_SINK@ ${clampedVolume}%`
        execSync(script)
        console.log(`✓ 系统音量已设置为: ${clampedVolume}%`)
        return { success: true, volume: clampedVolume }
      } catch (error) {
        // 如果 pactl 失败，尝试 amixer (ALSA)
        try {
          const script = `amixer set Master ${clampedVolume}%`
          execSync(script)
          console.log(`✓ 系统音量已设置为: ${clampedVolume}%`)
          return { success: true, volume: clampedVolume }
        } catch (error2) {
          console.warn('⚠️ Linux 音量控制失败:', error2.message)
          return { success: false, message: 'Linux 音量控制需要安装 PulseAudio 或 ALSA' }
        }
      }
    }
  } catch (error) {
    console.error('❌ 设置系统音量失败:', error)
    return { success: false, message: error.message }
  }
})

// IPC 通信：获取系统音量
ipcMain.handle('system:getVolume', async (event) => {
  try {
    if (process.platform === 'darwin') {
      // macOS: 使用 osascript 获取音量
      const script = `osascript -e "output volume of (get volume settings)"`
      const result = execSync(script, { encoding: 'utf8' }).trim()
      const volume = parseInt(result, 10)
      return { success: true, volume: volume }
    } else if (process.platform === 'win32') {
      // Windows: 需要更复杂的实现
      console.log('⚠️ Windows 音量获取需要额外实现')
      return { success: false, message: 'Windows 系统音量获取需要额外实现' }
    } else {
      // Linux: 使用 pactl 获取音量
      try {
        const script = `pactl get-sink-volume @DEFAULT_SINK@ | grep -oP '\\d+%' | head -1 | sed 's/%//'`
        const result = execSync(script, { encoding: 'utf8' }).trim()
        const volume = parseInt(result, 10)
        return { success: true, volume: volume }
      } catch (error) {
        console.warn('⚠️ Linux 音量获取失败:', error.message)
        return { success: false, message: error.message }
      }
    }
  } catch (error) {
    console.error('❌ 获取系统音量失败:', error)
    return { success: false, message: error.message }
  }
})

// IPC 通信：答题相关操作
ipcMain.handle('answer:startExam', async (event, params) => {
  if (!db) {
    return { success: false, message: '数据库未初始化' }
  }
  try {
    const AnswerService = require(path.join(__dirname, '../src/database/answerService'))
    const answerService = new AnswerService(db)
    return await answerService.startExam(params)
  } catch (error) {
    console.error('开始答题失败:', error)
    return { success: false, message: error.message }
  }
})

ipcMain.handle('answer:saveQuestionResult', async (event, params) => {
  if (!db) {
    return { success: false, message: '数据库未初始化' }
  }
  try {
    const AnswerService = require(path.join(__dirname, '../src/database/answerService'))
    const answerService = new AnswerService(db)
    return await answerService.saveQuestionResult(params)
  } catch (error) {
    console.error('保存答题结果失败:', error)
    return { success: false, message: error.message }
  }
})

ipcMain.handle('answer:submitVolume', async (event, params) => {
  if (!db) {
    return { success: false, message: '数据库未初始化' }
  }
  try {
    const AnswerService = require(path.join(__dirname, '../src/database/answerService'))
    const answerService = new AnswerService(db)
    return await answerService.submitVolume(params)
  } catch (error) {
    console.error('提交卷别失败:', error)
    return { success: false, message: error.message }
  }
})

ipcMain.handle('answer:submitExam', async (event, params) => {
  if (!db) {
    return { success: false, message: '数据库未初始化' }
  }
  try {
    const AnswerService = require(path.join(__dirname, '../src/database/answerService'))
    const answerService = new AnswerService(db)
    return await answerService.submitExam(params)
  } catch (error) {
    console.error('提交答题失败:', error)
    return { success: false, message: error.message }
  }
})

ipcMain.handle('answer:updateVolumeStatus', async (event, paperInfoId, volumeCode, status) => {
  if (!db) {
    return { success: false, message: '数据库未初始化' }
  }
  try {
    const AnswerService = require(path.join(__dirname, '../src/database/answerService'))
    const answerService = new AnswerService(db)
    return await answerService.updateVolumeStatus(paperInfoId, volumeCode, status)
  } catch (error) {
    console.error('更新卷别状态失败:', error)
    return { success: false, message: error.message }
  }
})

ipcMain.handle('answer:markIntermissionPlayed', async (event, paperInfoId, fromVolume, toVolume) => {
  if (!db) {
    return { success: false, message: '数据库未初始化' }
  }
  try {
    const AnswerService = require(path.join(__dirname, '../src/database/answerService'))
    const answerService = new AnswerService(db)
    return await answerService.markIntermissionPlayed(paperInfoId, fromVolume, toVolume)
  } catch (error) {
    console.error('标记中场音频已播放失败:', error)
    return { success: false, message: error.message }
  }
})

ipcMain.handle('answer:getQuestionResults', async (event, params) => {
  if (!db) {
    return []
  }
  try {
    const database = db.getDB()
    const { paperInfoId, paperId, appUserId } = params
    
    const results = database.prepare(`
      SELECT question_id, answer_ids, user_answer, result, question_sort
      FROM app_user_paper_question_result
      WHERE paper_id = ? AND app_user_id = ?
      ORDER BY question_sort ASC
    `).all(paperId, appUserId)
    
    return results
  } catch (error) {
    console.error('获取答题结果失败:', error)
    return []
  }
})

ipcMain.handle('answer:getPaperInfoList', async (event, appUserId, paperId = null) => {
  if (!db) {
    return []
  }
  try {
    const AnswerService = require(path.join(__dirname, '../src/database/answerService'))
    const answerService = new AnswerService(db)
    return await answerService.getPaperInfoList(appUserId, paperId)
  } catch (error) {
    console.error('获取答题记录列表失败:', error)
    return []
  }
})

ipcMain.handle('answer:getPaperInfo', async (event, paperInfoId) => {
  if (!db) {
    return null
  }
  try {
    const AnswerService = require(path.join(__dirname, '../src/database/answerService'))
    const answerService = new AnswerService(db)
    return await answerService.getPaperInfo(paperInfoId)
  } catch (error) {
    console.error('获取答题记录详情失败:', error)
    return null
  }
})

ipcMain.handle('answer:clearPaperResult', async (event, { paperInfoId, paperId }) => {
  if (!db) {
    return { success: false, message: '数据库未初始化' }
  }
  try {
    const AnswerService = require(path.join(__dirname, '../src/database/answerService'))
    const answerService = new AnswerService(db)
    return await answerService.clearPaperResult(paperInfoId, paperId)
  } catch (error) {
    console.error('清空答题记录失败:', error)
    return { success: false, message: error.message }
  }
})

ipcMain.handle('answer:checkPracticeLimit', async (event, paperId, appUserId) => {
  if (!db) {
    return { allowed: false, practiceCount: 0, practiceLimit: 0 }
  }
  try {
    const AnswerService = require(path.join(__dirname, '../src/database/answerService'))
    const answerService = new AnswerService(db)
    return await answerService.checkPracticeLimit(paperId, appUserId)
  } catch (error) {
    console.error('检查练习次数限制失败:', error)
    return { allowed: false, practiceCount: 0, practiceLimit: 0 }
  }
})

// IPC 通信：试卷包同步和下载相关
ipcMain.handle('paper:syncPaperPackage', async (event, { paper, token }) => {
  if (!db) {
    return { success: false, message: '数据库未初始化' }
  }
  try {
    const PaperService = require(path.join(__dirname, '../src/database/paperService'))
    const paperService = new PaperService(db)
    const result = await paperService.syncPaperPackage(paper, token)
    return result
  } catch (error) {
    console.error('同步试卷包失败:', error)
    return { success: false, message: error.message || '同步试卷包失败' }
  }
})

// 通过 paperId 同步试卷包（自动查询 paperCode）
ipcMain.handle('paper:syncPaperPackageById', async (event, { paperId, token, onlyQuickStart }) => {
  if (!db) {
    return { success: false, message: '数据库未初始化' }
  }
  try {
    const PaperService = require(path.join(__dirname, '../src/database/paperService'))
    const paperService = new PaperService(db)
    
    // 从数据库查询 paper 信息
    const paper = db.getDB().prepare(`
      SELECT id, paper_code, paper_name, version, package_hash, package_size
      FROM paper
      WHERE id = ?
    `).get(paperId)
    
    if (!paper) {
      return { success: false, message: `未找到试卷，paperId=${paperId}` }
    }
    
    console.log(`[syncPaperPackageById] paperId=${paperId}, paperCode=${paper.paper_code}, version=${paper.version}`)
    
    // 调用 syncPaperPackage
    const result = await paperService.syncPaperPackage(paper, token, null, onlyQuickStart)
    return result
  } catch (error) {
    console.error('同步试卷包失败:', error)
    return { success: false, message: error.message || '同步试卷包失败' }
  }
})

ipcMain.handle('paper:checkPackageExists', async (event, paperId) => {
  if (!db) {
    return false
  }
  try {
    const PaperService = require(path.join(__dirname, '../src/database/paperService'))
    const paperService = new PaperService(db)
    return paperService.checkPackageExists(paperId)
  } catch (error) {
    console.error('检查试卷包是否存在失败:', error)
    return false
  }
})

// 从服务器刷新试卷版本信息并更新本地paper表
ipcMain.handle('paper:refreshPaperVersion', async (event, { paperId, token }) => {
  if (!db) {
    return { success: false, message: '数据库未初始化' }
  }
  try {
    const PaperService = require(path.join(__dirname, '../src/database/paperService'))
    const paperService = new PaperService(db)
    return await paperService.refreshPaperVersionFromServer(paperId, token)
  } catch (error) {
    console.error('刷新试卷版本失败:', error)
    return { success: false, message: error.message || '刷新试卷版本失败' }
  }
})

// 检查是否有任何可用版本的完整包（用于降级使用）
ipcMain.handle('paper:checkAnyPackageExists', async (event, paperId) => {
  if (!db) {
    return null
  }
  try {
    const PaperService = require(path.join(__dirname, '../src/database/paperService'))
    const paperService = new PaperService(db)
    return paperService.checkAnyPackageExists(paperId)
  } catch (error) {
    console.error('检查任意试卷包是否存在失败:', error)
    return null
  }
})

// 更新本地试卷版本（用于降级时更新paper表的版本）
ipcMain.handle('paper:updateLocalVersion', async (event, paperId, version) => {
  if (!db) {
    throw new Error('数据库未初始化')
  }
  try {
    console.log(`[paper:updateLocalVersion] 更新本地版本: paperId=${paperId}, version=${version}`)
    db.prepare(`
      UPDATE paper SET version = ? WHERE id = ?
    `).run(version, paperId)
    console.log(`✓ [paper:updateLocalVersion] 更新成功`)
    return { success: true }
  } catch (error) {
    console.error('更新本地版本失败:', error)
    throw error
  }
})

ipcMain.handle('paper:checkQuickStartPackageExists', async (event, paperId) => {
  if (!db) {
    return false
  }
  try {
    const PaperService = require(path.join(__dirname, '../src/database/paperService'))
    const paperService = new PaperService(db)
    return paperService.checkQuickStartPackageExists(paperId)
  } catch (error) {
    console.error('检查快速启动包是否存在失败:', error)
    return false
  }
})

ipcMain.handle('paper:getLocalPaperVersion', async (event, paperId) => {
  if (!db) {
    return null
  }
  try {
    const PaperService = require(path.join(__dirname, '../src/database/paperService'))
    const paperService = new PaperService(db)
    return paperService.getLocalPaperVersion(paperId)
  } catch (error) {
    console.error('获取本地试卷版本失败:', error)
    return null
  }
})

ipcMain.handle('paper:syncQuickStartPackageOnly', async (event, { paper, token }) => {
  if (!db) {
    return { success: false, message: '数据库未初始化' }
  }
  try {
    const PaperService = require(path.join(__dirname, '../src/database/paperService'))
    const paperService = new PaperService(db)
    const result = await paperService.syncQuickStartPackageOnly(paper, token)
    return result
  } catch (error) {
    console.error('同步快速启动包失败:', error)
    return { success: false, message: error.message || '同步快速启动包失败' }
  }
})

ipcMain.handle('paper:getDownloadStatus', async (event, paperId) => {
  if (!db) {
    return null
  }
  try {
    const PaperService = require(path.join(__dirname, '../src/database/paperService'))
    const paperService = new PaperService(db)
    return paperService.getDownloadStatus(paperId)
  } catch (error) {
    console.error('获取下载状态失败:', error)
    return null
  }
})

// IPC 通信：手动下载完整ZIP包（用于调试或强制下载）
ipcMain.handle('paper:downloadFullPackage', async (event, { paperId, token }) => {
  if (!db) {
    return { success: false, message: '数据库未初始化' }
  }
  try {
    console.log(`[Main Process] 开始手动下载完整包，paperId=${paperId}`)
    
    const PaperService = require(path.join(__dirname, '../src/database/paperService'))
    const paperService = new PaperService(db)
    
    // 获取试卷信息
    const paper = db.getDB().prepare(`
      SELECT id, paper_code, version, package_hash FROM paper WHERE id = ? LIMIT 1
    `).get(paperId)
    
    if (!paper) {
      return { success: false, message: `试卷不存在（ID: ${paperId}）` }
    }
    
    console.log(`[Main Process] 试卷信息: paper_code=${paper.paper_code}, version=${paper.version}`)
    
    // 下载完整包
    const result = await paperService.downloadPaperPackage(
      paper.id,
      paper.paper_code,
      paper.version,
      paper.package_hash || null,
      token,
      (progress) => {
        console.log(`[Main Process] 下载进度: ${progress}%`)
      }
    )
    
    if (result.success) {
      console.log(`[Main Process] ✓ 完整包下载成功: ${paper.paper_code} v${paper.version}`)
      
      // 解压完整包
      await paperService.extractPaperPackage(paper.paper_code, paper.version)
      console.log(`[Main Process] ✓ 完整包解压成功`)
      
      return { success: true, message: '完整包下载并解压成功' }
    } else {
      console.error(`[Main Process] 完整包下载失败: ${result.message}`)
      return { success: false, message: result.message || '下载失败' }
    }
  } catch (error) {
    console.error('[Main Process] 手动下载完整包失败:', error)
    return { success: false, message: error.message || '下载失败' }
  }
})

ipcMain.handle('paper:updateDownloadStatus', async (event, { paperId, paperCode, status, progress, downloadedSize, totalSize, errorMessage }) => {
  if (!db) {
    return { success: false, message: '数据库未初始化' }
  }
  try {
    const PaperService = require(path.join(__dirname, '../src/database/paperService'))
    const paperService = new PaperService(db)
    paperService.updateDownloadStatus(paperId, paperCode, status, progress, downloadedSize, totalSize, errorMessage)
    return { success: true }
  } catch (error) {
    console.error('更新下载状态失败:', error)
    return { success: false, message: error.message || '更新下载状态失败' }
  }
})

ipcMain.handle('paper:clearDownloadStatus', async (event, paperId) => {
  if (!db) {
    return { success: false, message: '数据库未初始化' }
  }
  try {
    const PaperService = require(path.join(__dirname, '../src/database/paperService'))
    const paperService = new PaperService(db)
    paperService.clearDownloadStatus(paperId)
    return { success: true }
  } catch (error) {
    console.error('清除下载状态失败:', error)
    return { success: false, message: error.message || '清除下载状态失败' }
  }
})

// IPC 通信：前端日志转发到主进程控制台
ipcMain.on('app:log', (event, { level, message, ...args }) => {
  const timestamp = new Date().toISOString()
  const prefix = `[${timestamp}] [Renderer]`
  
  switch (level) {
    case 'error':
      console.error(prefix, message, ...Object.values(args))
      break
    case 'warn':
      console.warn(prefix, message, ...Object.values(args))
      break
    case 'info':
      console.log(prefix, message, ...Object.values(args))
      break
    case 'debug':
      console.log(prefix, '[DEBUG]', message, ...Object.values(args))
      break
    default:
      console.log(prefix, message, ...Object.values(args))
  }
})

// IPC 通信：显示通知
ipcMain.handle('app:showNotification', async (event, { title, body, icon }) => {
  const { Notification } = require('electron')
  if (Notification.isSupported()) {
    const notification = new Notification({
      title: title || '通知',
      body: body || '',
      icon: icon || null
    })
    notification.show()
    return { success: true }
  }
  return { success: false, message: '通知功能不支持' }
})

