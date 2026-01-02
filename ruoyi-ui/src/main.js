import Vue from 'vue'

import Cookies from 'js-cookie'

import Element from 'element-ui'
import locale from 'element-ui/lib/locale/lang/zh-CN'
import './assets/styles/element-variables.scss'

import '@/assets/styles/index.scss' // global css
import '@/assets/styles/ruoyi.scss' // ruoyi css
import App from './App'
import store from './store'
import router from './router'
import directive from './directive' // directive
import plugins from './plugins' // plugins
import { download } from '@/utils/request'

import './assets/icons' // icon
import './permission' // permission control
import { getDicts } from "@/api/system/dict/data"
import { getConfigKey } from "@/api/system/config"
import { parseTime, resetForm, addDateRange, selectDictLabel, selectDictLabels, handleTree } from "@/utils/ruoyi"
// 分页组件
import Pagination from "@/components/Pagination"
// 自定义表格工具组件
import RightToolbar from "@/components/RightToolbar"
// 富文本组件
import Editor from "@/components/Editor"
// 文件上传组件
import FileUpload from "@/components/FileUpload"
// 图片上传组件
import ImageUpload from "@/components/ImageUpload"
// 图片预览组件
import ImagePreview from "@/components/ImagePreview"
// 字典标签组件
import DictTag from '@/components/DictTag'
// 字典数据组件
import DictData from '@/components/DictData'

// 全局方法挂载
Vue.prototype.getDicts = getDicts
Vue.prototype.getConfigKey = getConfigKey
Vue.prototype.parseTime = parseTime
Vue.prototype.resetForm = resetForm
Vue.prototype.addDateRange = addDateRange
Vue.prototype.selectDictLabel = selectDictLabel
Vue.prototype.selectDictLabels = selectDictLabels
Vue.prototype.download = download
Vue.prototype.handleTree = handleTree

// 全局组件挂载
Vue.component('DictTag', DictTag)
Vue.component('Pagination', Pagination)
Vue.component('RightToolbar', RightToolbar)
Vue.component('Editor', Editor)
Vue.component('FileUpload', FileUpload)
Vue.component('ImageUpload', ImageUpload)
Vue.component('ImagePreview', ImagePreview)

Vue.use(directive)
Vue.use(plugins)
DictData.install()

/**
 * If you don't want to use mock-server
 * you want to use MockJs for mock api
 * you can execute: mockXHR()
 *
 * Currently MockJs will be used in the production environment,
 * please remove it before going online! ! !
 */

Vue.use(Element, {
  size: Cookies.get('size') || 'medium', // set element-ui default size
  locale: locale // 使用中文语言包
})

Vue.config.productionTip = false

// 静默过滤 Chrome 扩展相关错误（仅过滤控制台输出，不影响功能）
if (typeof window !== 'undefined') {
  const shouldFilter = (msg) => {
    if (!msg) return false
    const str = String(msg)
    return str.includes('chrome-extension://') ||
           str.includes('moz-extension://') ||
           str.includes('safari-extension://') ||
           str.includes('edge-extension://') ||
           str.includes('pejdijmoenmkgeppbflobdenhhabjlaj') ||
           str.includes('completion_list') ||
           (str.includes('ERR_FILE_NOT_FOUND') && str.includes('extension'))
  }
  
  // 过滤 console 输出
  if (window.console) {
    const originalError = console.error
    const originalWarn = console.warn
    
    console.error = function(...args) {
      const msg = args.join(' ')
      if (!shouldFilter(msg)) {
        originalError.apply(console, args)
      }
    }
    
    console.warn = function(...args) {
      const msg = args.join(' ')
      if (!shouldFilter(msg)) {
        originalWarn.apply(console, args)
      }
    }
  }
  
  // 过滤 window.onerror
  const originalOnError = window.onerror
  window.onerror = function(message, source, lineno, colno, error) {
    const errorString = String(message || '') + String(source || '')
    if (shouldFilter(errorString)) {
      return true // 返回 true 表示已处理，不显示错误
    }
    if (originalOnError && typeof originalOnError === 'function') {
      return originalOnError.apply(window, arguments)
    }
    return false
  }
  
  // 过滤 error 事件（包括资源加载错误）
  window.addEventListener('error', function(event) {
    const msg = String(event.message || '')
    const src = String(event.filename || event.target?.src || event.target?.href || '')
    if (shouldFilter(msg + src)) {
      event.preventDefault()
      event.stopPropagation()
      event.stopImmediatePropagation()
      return false
    }
  }, true)
  
  // 拦截 fetch 请求中的扩展错误
  if (window.fetch) {
    const originalFetch = window.fetch
    window.fetch = function(...args) {
      const url = args[0]
      if (typeof url === 'string' && shouldFilter(url)) {
        return Promise.resolve(new Response(null, { status: 404 }))
      }
      if (url && typeof url === 'object' && url.url && shouldFilter(url.url)) {
        return Promise.resolve(new Response(null, { status: 404 }))
      }
      return originalFetch.apply(window, args).catch(error => {
        if (shouldFilter(error.message || error.toString())) {
          return new Response(null, { status: 404 })
        }
        throw error
      })
    }
  }
  
  // 拦截 XMLHttpRequest 中的扩展错误
  if (window.XMLHttpRequest) {
    const OriginalXHR = window.XMLHttpRequest
    window.XMLHttpRequest = function() {
      const xhr = new OriginalXHR()
      const originalOpen = xhr.open
      xhr.open = function(method, url, ...rest) {
        if (shouldFilter(url)) {
          xhr._blocked = true
          return
        }
        return originalOpen.apply(xhr, [method, url, ...rest])
      }
      const originalSend = xhr.send
      xhr.send = function(...args) {
        if (xhr._blocked) {
          return
        }
        const originalOnError = xhr.onerror
        xhr.onerror = function(event) {
          const url = xhr.responseURL || ''
          if (shouldFilter(url)) {
            return
          }
          if (originalOnError) {
            return originalOnError.call(xhr, event)
          }
        }
        return originalSend.apply(xhr, args)
      }
      return xhr
    }
  }
}

new Vue({
  el: '#app',
  router,
  store,
  render: h => h(App)
})
