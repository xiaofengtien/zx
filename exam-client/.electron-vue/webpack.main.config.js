const webpack = require('webpack')
const isDev = process.env.NODE_ENV === 'development'
const API_BASE_URL = 'http://47.94.192.7:8818/prod-api' // 强制生产环境地址，排除环境变量干扰

module.exports = {
  mode: isDev ? 'development' : 'production',
  entry: './.electron-vue/main.js',
  target: 'electron-main',
  output: {
    path: require('path').resolve(__dirname, '../dist/electron'),
    filename: 'main.js'
  },
  node: {
    __dirname: false,
    __filename: false
  },
  // 只把原生模块标记为外部（从 extraResources 加载）
  // axios 等纯 JS 模块会被打包进 main.js
  externals: {
    'better-sqlite3': 'commonjs better-sqlite3'
  },
  resolve: {
    extensions: ['.js', '.json']
  },
  module: {
    rules: [
      {
        test: /\.js$/,
        exclude: /node_modules/,
        use: {
          loader: 'babel-loader',
          options: {
            presets: ['@babel/preset-env']
          }
        }
      }
    ]
  },
  plugins: [
    new webpack.DefinePlugin({
      'process.env.API_BASE_URL': JSON.stringify(API_BASE_URL)
    })
  ]
}
