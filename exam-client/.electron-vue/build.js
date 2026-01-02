// 设置生产环境
process.env.NODE_ENV = 'production'
// 生产环境 API 地址（通过 Nginx 代理）
process.env.API_BASE_URL = 'http://47.94.192.7:8818/prod-api'

const webpack = require('webpack')
const webpackMainConfig = require('./webpack.main.config')
const webpackRendererConfig = require('./webpack.renderer.config')
const { execSync } = require('child_process')
const path = require('path')

function build() {
  return new Promise((resolve, reject) => {
    console.log('Building main process...')
    const mainCompiler = webpack(webpackMainConfig)

    mainCompiler.run((err, stats) => {
      if (err) {
        console.error('Main process build error:', err)
        reject(err)
        return
      }

      if (stats.hasErrors()) {
        console.error('Main process build errors:', stats.compilation.errors)
        reject(new Error('Main process build failed'))
        return
      }

      console.log('Main process build completed!')
      console.log('Building renderer process...')
      const rendererCompiler = webpack(webpackRendererConfig)

      rendererCompiler.run((err, stats) => {
        if (err) {
          console.error('Renderer process build error:', err)
          reject(err)
          return
        }

        if (stats.hasErrors()) {
          console.error('Renderer process build errors:', stats.compilation.errors)
          reject(new Error('Renderer process build failed'))
          return
        }

        console.log('Renderer process build completed!')
        console.log('Build completed!')
        resolve()
      })
    })
  })
}

build().catch(console.error)

