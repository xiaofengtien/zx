const fs = require('fs')
const path = require('path')
const { app } = require('electron')

/**
 * 应用配置管理
 * 配置文件位置：{userData}/config.json
 */
class AppConfig {
  constructor() {
    const userDataPath = app.getPath('userData')
    this.configPath = path.join(userDataPath, 'config.json')
    this.config = this.loadConfig()
  }

  /**
   * 加载配置文件
   * 如果文件不存在，使用默认配置
   */
  loadConfig() {
    try {
      if (fs.existsSync(this.configPath)) {
        const content = fs.readFileSync(this.configPath, 'utf-8')
        const config = JSON.parse(content)
        console.log('已加载配置文件:', this.configPath)
        return { ...this.getDefaultConfig(), ...config }
      } else {
        console.log('配置文件不存在，使用默认配置')
        return this.getDefaultConfig()
      }
    } catch (error) {
      console.error('加载配置文件失败，使用默认配置:', error.message)
      return this.getDefaultConfig()
    }
  }

  /**
   * 获取默认配置
   */
  getDefaultConfig() {
    return {
      // 是否启用业务数据表同步（备选方案）
      // false: 只使用ZIP包方案，如果没有ZIP包则提示错误
      // true: 如果没有ZIP包，允许使用业务数据表同步（性能较差，仅用于小数据量场景）
      enableTableSync: false,
      
      // 数据量阈值（如果启用表同步）
      // 如果题目数量超过此阈值，即使启用表同步也会提示使用ZIP包方案
      maxQuestionsForTableSync: 1000,
      
      // 其他配置...
    }
  }

  /**
   * 保存配置
   */
  saveConfig() {
    try {
      const dir = path.dirname(this.configPath)
      if (!fs.existsSync(dir)) {
        fs.mkdirSync(dir, { recursive: true })
      }
      fs.writeFileSync(this.configPath, JSON.stringify(this.config, null, 2), 'utf-8')
      console.log('配置已保存:', this.configPath)
      return true
    } catch (error) {
      console.error('保存配置失败:', error.message)
      return false
    }
  }

  /**
   * 获取配置值
   */
  get(key) {
    return this.config[key]
  }

  /**
   * 设置配置值
   */
  set(key, value) {
    this.config[key] = value
    this.saveConfig()
  }

  /**
   * 检查是否启用业务数据表同步
   */
  isTableSyncEnabled() {
    return this.config.enableTableSync === true
  }

  /**
   * 获取最大题目数量阈值（用于表同步）
   */
  getMaxQuestionsForTableSync() {
    return this.config.maxQuestionsForTableSync || 1000
  }
}

// 单例模式
let configInstance = null

function getConfig() {
  if (!configInstance) {
    configInstance = new AppConfig()
  }
  return configInstance
}

module.exports = { AppConfig, getConfig }


