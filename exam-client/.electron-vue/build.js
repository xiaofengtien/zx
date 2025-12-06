const webpack = require('webpack')
const webpackMainConfig = require('./webpack.main.config')
const webpackRendererConfig = require('./webpack.renderer.config')
const { execSync } = require('child_process')
const path = require('path')

// 设置生产环境
process.env.NODE_ENV = 'production'

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

