const { spawn } = require('child_process')
const webpack = require('webpack')
const webpackMainConfig = require('./webpack.main.config')
const webpackRendererConfig = require('./webpack.renderer.config')
const WebpackDevServer = require('webpack-dev-server')
const path = require('path')

// 设置开发环境
process.env.NODE_ENV = 'development'
// 开发环境默认使用本地后端 (http://localhost:8080)
// 如需测试线上后端，取消下行注释：
// process.env.API_BASE_URL = 'http://47.94.192.7:8818/prod-api'

let electronProcess = null
let webpackMainCompiler = null
let webpackRendererCompiler = null

function startRenderer() {
  return new Promise((resolve, reject) => {
    webpackRendererCompiler = webpack(webpackRendererConfig)

    const PORT = process.env.DEV_PORT ? parseInt(process.env.DEV_PORT, 10) : 9081
    const server = new WebpackDevServer({
      port: PORT,
      hot: true,
      open: false,
      static: {
        directory: path.join(__dirname, '../src/renderer')
      }
    }, webpackRendererCompiler)

    server.startCallback(() => {
      console.log(`Renderer server started on http://localhost:${PORT}`)
      resolve()
    })
  })
}

function startMain() {
  return new Promise((resolve, reject) => {
    webpackMainCompiler = webpack(webpackMainConfig)

    webpackMainCompiler.watch({}, (err, stats) => {
      if (err) {
        console.error('Main process build error:', err)
        return
      }

      if (stats.hasErrors()) {
        console.error('Main process build errors:', stats.compilation.errors)
        return
      }

      // 只在第一次构建时启动 Electron，后续通过热重载
      if (!electronProcess) {
        electronProcess = spawn('npx', ['electron', '.electron-vue/main.js'], {
          stdio: 'inherit',
          env: { ...process.env, NODE_ENV: 'development' },
          cwd: path.join(__dirname, '..')
        })

        electronProcess.on('close', () => {
          electronProcess = null
          process.exit()
        })
      } else {
        // 主进程文件变化时，重启 Electron
        if (electronProcess && electronProcess.kill) {
          electronProcess.kill()
          electronProcess = null

          setTimeout(() => {
            electronProcess = spawn('npx', ['electron', '.electron-vue/main.js'], {
              stdio: 'inherit',
              env: { ...process.env, NODE_ENV: 'development' },
              cwd: path.join(__dirname, '..')
            })

            electronProcess.on('close', () => {
              electronProcess = null
              process.exit()
            })
          }, 1000)
        }
      }

      if (!resolve.called) {
        resolve.called = true
        resolve()
      }
    })
  })
}

async function start() {
  try {
    await startRenderer()
    await startMain()
  } catch (error) {
    console.error('Failed to start:', error)
    process.exit(1)
  }
}

start()
