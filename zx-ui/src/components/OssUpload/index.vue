<template>
  <div class="oss-upload">
    <!-- 当 listType 为 picture-card 时，使用自定义缩略图卡片 -->
    <template v-if="listType === 'picture-card'">
      <div class="custom-picture-card-list">
        <!-- 已上传的图片缩略图 -->
        <div
          v-for="file in fileList"
          :key="`file-${file.uid}-${file.url || ''}`"
          class="custom-picture-card-item"
        >
          <el-image
            :key="`image-${file.uid}-${file.url || ''}`"
            :src="file.url"
            fit="cover"
            class="custom-picture-card-image"
            :preview-src-list="[file.url]"
            @error="handleImageError"
          >
            <div slot="error" class="image-slot">
              <i class="el-icon-picture-outline"></i>
            </div>
          </el-image>
          <div class="custom-picture-card-actions">
            <i
              class="el-icon-view"
              @click="handlePreview(file)"
              title="预览"
            ></i>
            <i
              class="el-icon-delete"
              @click="handleCustomRemove(file)"
              title="删除"
              :class="{ 'is-disabled': disabled }"
            ></i>
          </div>
        </div>
        <!-- 上传按钮 -->
        <el-upload
          v-if="fileList.length < limit"
          ref="upload"
          :action="uploadAction"
          :headers="uploadHeaders"
          :data="uploadData"
          :before-upload="handleBeforeUpload"
          :on-success="handleSuccess"
          :on-error="handleError"
          :on-progress="handleProgress"
          :on-change="handleFileChange"
          :on-exceed="handleExceed"
          :show-file-list="false"
          :limit="limit"
          :accept="accept"
          :disabled="disabled"
          :auto-upload="autoUpload"
          :list-type="listType"
          class="custom-picture-card-upload"
        >
          <div class="custom-picture-card-upload-btn">
            <i class="el-icon-plus"></i>
          </div>
        </el-upload>
      </div>
      <div v-if="tip" class="el-upload__tip">{{ tip }}</div>
    </template>
    <!-- 当 listType 为 text 或其他时，使用默认的 el-upload -->
    <el-upload
      v-else
      ref="upload"
      :action="uploadAction"
      :headers="uploadHeaders"
      :data="uploadData"
      :before-upload="handleBeforeUpload"
      :on-success="handleSuccess"
      :on-error="handleError"
      :on-progress="handleProgress"
      :on-remove="handleRemove"
      :on-change="handleFileChange"
      :on-exceed="handleExceed"
      :on-preview="handlePreview"
      :show-file-list="listType === 'text' && !showFileList ? true : showFileList"
      :file-list="fileList"
      :limit="limit"
      :accept="accept"
      :disabled="disabled"
      :auto-upload="autoUpload"
      :drag="drag"
      :list-type="listType"
      v-bind="$attrs"
      v-on="$listeners"
      :class="{ 'oss-upload-no-button': listType === 'text' && !showFileList }"
    >
      <!-- 当 listType 为 text 且 showFileList 为 false 时，显示按钮样式（隐藏上传区域） -->
      <template v-if="listType === 'text' && !showFileList">
        <el-button size="small" type="primary" :disabled="uploading" @click.stop.prevent="triggerUpload">点击上传</el-button>
      </template>
      <slot v-else>
        <el-button size="small" type="primary" :disabled="uploading">点击上传</el-button>
      </slot>
      <div slot="tip" class="el-upload__tip">
        <slot name="tip">
          <span v-if="tip">{{ tip }}</span>
        </slot>
      </div>
      <!-- 自定义文件列表显示（当 listType 为 text 时） -->
      <template v-if="listType === 'text'" slot="file" slot-scope="{ file }">
        <slot name="file" :file="file">
          <span
            :class="['upload-file-name', { 'is-audio': isAudioFile(file) }]"
            :title="(file.url || value) ? '点击预览' : ''"
            @click.stop.prevent="handleFileClick(file)"
            style="cursor: pointer;"
          >
            <i :class="getFileIcon(file)" style="margin-right: 4px;"></i>{{ file.name }}
          </span>
          <i
            class="el-icon-close upload-file-remove"
            @click.stop.prevent="handleFileRemove(file)"
            style="cursor: pointer;"
          ></i>
        </slot>
      </template>
    </el-upload>
  </div>
</template>

<script>
import { getOssUploadToken, getMediaDownloadUrl } from '@/api/exam/question'
import { getToken } from '@/utils/auth'

export default {
  name: 'OssUpload',
  props: {
    // 文件列表
    value: {
      type: [String, Array],
      default: null
    },
    // 数量限制
    limit: {
      type: Number,
      default: 1
    },
    // 文件类型限制
    accept: {
      type: String,
      default: ''
    },
    // 大小限制（MB），默认10MB（图片等媒体文件限制）
    fileSize: {
      type: Number,
      default: 10
    },
    // 是否显示文件列表
    showFileList: {
      type: Boolean,
      default: true
    },
    // 是否禁用
    disabled: {
      type: Boolean,
      default: false
    },
    // 是否自动上传
    autoUpload: {
      type: Boolean,
      default: true
    },
    // 是否支持拖拽
    drag: {
      type: Boolean,
      default: false
    },
    // 提示文字
    tip: {
      type: String,
      default: ''
    },
    // 文件路径前缀（用于生成文件路径）
    pathPrefix: {
      type: String,
      default: 'exam/question'
    },
    // 文件列表显示类型
    listType: {
      type: String,
      default: 'text'
    }
  },
  data() {
    return {
      uploadAction: 'http://up-as0.qiniup.com', // 上传地址（OSS直传地址，默认七牛云）
      uploadHeaders: {}, // 上传请求头
      uploadData: {}, // 上传额外参数
      fileList: [], // 文件列表
      uploading: false, // 是否正在上传
      ossConfig: null, // OSS配置（token, domain, ossType）
    }
  },
  watch: {
    value: {
      handler(val) {
        this.updateFileList(val)
      },
      immediate: true
    },
    fileList: {
      handler(newVal, oldVal) {
        console.log('[OssUpload] watch fileList 变化')
        console.log('[OssUpload] watch fileList oldVal:', oldVal)
        console.log('[OssUpload] watch fileList newVal:', newVal)
        console.log('[OssUpload] watch fileList listType:', this.listType)
        if (this.listType === 'picture-card' && newVal && newVal.length > 0) {
          newVal.forEach((file, index) => {
            console.log(`[OssUpload] watch fileList[${index}]:`, {
              uid: file.uid,
              url: file.url,
              name: file.name,
              status: file.status
            })
          })
        }
      },
      deep: true,
      immediate: true
    }
  },
  mounted() {
    console.log('[OssUpload] mounted')
    console.log('[OssUpload] mounted listType:', this.listType)
    console.log('[OssUpload] mounted fileList:', this.fileList)
    console.log('[OssUpload] mounted value:', this.value)
  },
  updated() {
    console.log('[OssUpload] updated')
    console.log('[OssUpload] updated listType:', this.listType)
    console.log('[OssUpload] updated fileList:', this.fileList)
    if (this.listType === 'picture-card' && this.fileList && this.fileList.length > 0) {
      this.fileList.forEach((file, index) => {
        console.log(`[OssUpload] updated fileList[${index}]:`, {
          uid: file.uid,
          url: file.url,
          name: file.name,
          status: file.status
        })
      })
    }
  },
  methods: {
    /**
     * 更新文件列表
     */
    async updateFileList(val) {
      console.log('[OssUpload] updateFileList 开始')
      console.log('[OssUpload] updateFileList 输入 val:', val)
      console.log('[OssUpload] updateFileList listType:', this.listType)
      console.log('[OssUpload] updateFileList 当前 fileList:', this.fileList)
      
      if (!val) {
        console.log('[OssUpload] updateFileList val 为空，清空 fileList')
        this.$set(this, 'fileList', [])
        return
      }
      
      let newFileList = []
      
      // 处理对象格式（包含url和duration）
      let actualUrl = val
      if (typeof val === 'object' && val !== null && !Array.isArray(val) && val.url) {
        actualUrl = val.url
        console.log('[OssUpload] updateFileList 对象格式，提取URL:', actualUrl)
      }
      
      if (typeof actualUrl === 'string') {
        // 单个文件URL - 确保是完整的HTTP/HTTPS URL
        console.log('[OssUpload] updateFileList 单个文件URL')
        const imageUrl = this.normalizeImageUrl(actualUrl)
        // 获取带签名的下载URL
        let displayUrl = imageUrl
        try {
          console.log('[OssUpload] updateFileList 开始获取带签名的下载URL')
          const downloadResponse = await getMediaDownloadUrl({ url: imageUrl })
          console.log('[OssUpload] updateFileList getMediaDownloadUrl 响应:', downloadResponse)
          const signedUrl = downloadResponse?.downloadUrl || downloadResponse?.data?.downloadUrl
          if (downloadResponse?.code === 200 && signedUrl) {
            displayUrl = signedUrl
            console.log('[OssUpload] updateFileList 使用带签名的URL:', displayUrl)
          } else {
            console.log('[OssUpload] updateFileList 未获取到签名URL，使用原始URL')
          }
        } catch (error) {
          console.error('[OssUpload] updateFileList 获取签名URL失败:', error)
          console.log('[OssUpload] updateFileList 使用原始URL作为fallback')
        }
        newFileList = [{
          name: this.getFileNameFromUrl(actualUrl),
          url: displayUrl, // 使用带签名的URL（如果有），否则使用原始URL
          status: 'success',
          uid: Date.now() // 为已有文件生成uid，用于区分新旧文件
        }]
        console.log('[OssUpload] updateFileList 单个文件 newFileList:', newFileList)
      } else if (Array.isArray(val)) {
        // 文件URL数组
        console.log('[OssUpload] updateFileList 文件URL数组，长度:', val.length)
        // 使用 Promise.all 并行获取所有文件的签名URL
        const filePromises = val.map(async (url, index) => {
          const imageUrl = this.normalizeImageUrl(url)
          let displayUrl = imageUrl
          try {
            const downloadResponse = await getMediaDownloadUrl({ url: imageUrl })
            const signedUrl = downloadResponse?.downloadUrl || downloadResponse?.data?.downloadUrl
            if (downloadResponse?.code === 200 && signedUrl) {
              displayUrl = signedUrl
            }
          } catch (error) {
            console.error('[OssUpload] updateFileList 获取签名URL失败:', error)
          }
          return {
            name: this.getFileNameFromUrl(url),
            url: displayUrl,
            status: 'success',
            uid: Date.now() + index
          }
        })
        newFileList = await Promise.all(filePromises)
        console.log('[OssUpload] updateFileList 数组 newFileList:', newFileList)
      }
      
      // 使用 Vue.set 确保响应式更新
      this.$set(this, 'fileList', newFileList)
      console.log('[OssUpload] updateFileList 更新后的 fileList:', this.fileList)
      console.log('[OssUpload] updateFileList 完成')
      
      // 对于 picture-card 类型，确保 el-upload 组件能正确显示缩略图
      if (this.listType === 'picture-card' && newFileList.length > 0) {
        this.$nextTick(() => {
          if (this.$refs.upload) {
            const uploadComponent = this.$refs.upload
            // 同步更新 el-upload 的内部 fileList
            if (uploadComponent.fileList && Array.isArray(uploadComponent.fileList)) {
              uploadComponent.fileList.splice(0, uploadComponent.fileList.length, ...newFileList)
            } else {
              this.$set(uploadComponent, 'fileList', [...newFileList])
            }
            uploadComponent.$forceUpdate()
          }
        })
      }
    },

    /**
     * 规范化图片URL，确保是完整的HTTP URL（将HTTPS转换为HTTP）
     */
    normalizeImageUrl(url) {
      console.log('[OssUpload] normalizeImageUrl 输入:', url)
      if (!url) {
        console.log('[OssUpload] normalizeImageUrl 输出: null (输入为空)')
        return url
      }
      
      let normalizedUrl = url
      
      // 如果已经是完整的HTTP/HTTPS URL，转换为HTTP
      if (url.startsWith('https://')) {
        normalizedUrl = url.replace('https://', 'http://')
        console.log('[OssUpload] normalizeImageUrl HTTPS转HTTP:', normalizedUrl)
      } else if (url.startsWith('http://')) {
        console.log('[OssUpload] normalizeImageUrl 输出:', normalizedUrl, '(已经是HTTP)')
        return normalizedUrl
      } else if (url.startsWith('/')) {
        // 如果是相对路径，添加baseURL
        normalizedUrl = process.env.VUE_APP_BASE_API + url
        console.log('[OssUpload] normalizeImageUrl 输出:', normalizedUrl, '(添加了baseURL)')
        return normalizedUrl
      } else {
        // 其他情况，直接返回（可能是OSS的完整URL，但需要检查协议）
        console.log('[OssUpload] normalizeImageUrl 输出:', normalizedUrl, '(其他情况，直接返回)')
        return normalizedUrl
      }
      
      console.log('[OssUpload] normalizeImageUrl 最终输出:', normalizedUrl)
      return normalizedUrl
    },

    /**
     * 从URL中提取文件名
     */
    getFileNameFromUrl(url) {
      if (!url) return ''
      try {
        // Strip query string (for signed URLs)
        let cleanUrl = url.split('?')[0]
        // Decode URI component (handle %2F etc)
        cleanUrl = decodeURIComponent(cleanUrl)
        const parts = cleanUrl.split('/')
        return parts[parts.length - 1] || 'audio.mp3'
      } catch (e) {
        console.warn('[OssUpload] 解析文件名失败:', e)
        return 'audio.mp3'
      }
    },

    /**
     * 上传前处理：获取OSS token并设置上传参数
     */
    async handleBeforeUpload(file) {
      // 文件大小校验
      const isLtSize = file.size / 1024 / 1024 < this.fileSize
      if (!isLtSize) {
        this.$modal.msgError(`上传文件大小不能超过 ${this.fileSize}MB!`)
        return false
      }

      // 文件类型校验
      if (this.accept) {
        const acceptTypes = this.accept.split(',').map(t => t.trim())
        const fileExtension = file.name.split('.').pop().toLowerCase()
        if (!acceptTypes.some(type => fileExtension === type.replace('.', ''))) {
          this.$modal.msgError(`只能上传 ${this.accept} 格式的文件!`)
          return false
        }
      }

      // 获取OSS上传凭证
      try {
        this.uploading = true
        const response = await getOssUploadToken()
        
        if (response.code !== 200) {
          this.$modal.msgError(response.msg || '获取上传凭证失败')
          return false
        }

        // 注意：request.js 的响应拦截器已经返回了 res.data，所以数据直接在 response 中
        // 后端返回格式：{ code: 200, msg: "操作成功", token: "...", domain: "...", ossType: "...", uploadUrl: "..." }
        if (!response.token) {
          console.error('获取上传凭证响应数据:', response)
          this.$modal.msgError('获取上传凭证失败：响应中缺少 token 字段')
          return false
        }

        this.ossConfig = {
          token: response.token,
          domain: response.domain,
          ossType: response.ossType || 'qiniu', // 默认七牛云
          uploadUrl: response.uploadUrl // 上传地址（七牛云需要，阿里云为null）
        }

        // 根据OSS类型设置上传参数
        if (this.ossConfig.ossType === 'qiniu') {
          // 七牛云上传
          await this.setupQiniuUpload(file)
        } else if (this.ossConfig.ossType === 'aliyun') {
          // 阿里云上传
          await this.setupAliyunUpload(file)
        } else {
          this.$modal.msgError('不支持的OSS类型: ' + this.ossConfig.ossType)
          return false
        }

        return true
      } catch (error) {
        console.error('获取上传凭证失败:', error)
        this.$modal.msgError('获取上传凭证失败: ' + (error.message || '未知错误'))
        return false
      } finally {
        this.uploading = false
      }
    },

    /**
     * 设置七牛云上传参数
     */
    async setupQiniuUpload(file) {
      // 生成文件路径
      const filePath = this.generateFilePath(file.name)
      
      // 七牛云上传地址（使用后端返回的上传地址，确保区域正确）
      // 如果后端没有返回 uploadUrl，则使用 HTTP 上传域名（避免 SSL 问题）
      this.uploadAction = this.ossConfig.uploadUrl || 'http://up-as0.qiniup.com'
      
      // 七牛云上传参数
      this.uploadData = {
        token: this.ossConfig.token,
        key: filePath // 文件路径
      }
      
      this.uploadHeaders = {}
    },

    /**
     * 设置阿里云上传参数
     */
    async setupAliyunUpload(file) {
      try {
        // 解析阿里云token（JSON格式）
        const tokenData = JSON.parse(this.ossConfig.token)
        
        // 生成文件路径（注意：后端 token.dir 已包含基础路径，这里只生成日期+文件名）
        const filePath = this.generateFilePathWithoutPrefix(file.name)
        const fullKey = (tokenData.dir || '') + filePath
        
        // 阿里云PostObject上传地址
        this.uploadAction = tokenData.host
        
        // 阿里云PostObject上传参数
        this.uploadData = {
          key: fullKey,
          policy: tokenData.policy,
          OSSAccessKeyId: tokenData.accessid,
          signature: tokenData.signature,
          'x-oss-content-type': file.type || 'application/octet-stream'
        }
        
        this.uploadHeaders = {}
      } catch (error) {
        console.error('解析阿里云token失败:', error)
            throw new Error('解析上传凭证失败')
      }
    },

    /**
     * 生成唯一的文件路径
     * 格式：exam/question/2025/11/20/filename_timestamp_uuid.ext
     */
    generateFilePath(fileName) {
      if (!fileName) {
        fileName = this.generateUUID() + '.tmp'
      }

      // 如果文件名以 paper_packages/ 开头，说明是试卷包文件，直接使用原始路径
      if (fileName.startsWith('paper_packages/')) {
        return fileName
      }

      // 提取文件扩展名
      let extension = ''
      let baseName = fileName
      const lastDotIndex = fileName.lastIndexOf('.')
      if (lastDotIndex > 0) {
        extension = fileName.substring(lastDotIndex)
        baseName = fileName.substring(0, lastDotIndex)
      }

      // 检查文件名是否包含非 ASCII 字符（中文等）
      // 如果包含，使用随机字符串代替，避免七牛云签名 URL 中文编码问题
      const hasNonAscii = /[^\x00-\x7F]/.test(baseName)
      if (hasNonAscii) {
        // 使用 "audio" 或 "file" 前缀 + UUID，保持文件名简洁
        const prefix = extension.toLowerCase().match(/\.(mp3|wav|m4a|ogg|flac)$/) ? 'audio' : 'file'
        baseName = `${prefix}_${this.generateUUID().substring(0, 16)}`
      }

      // 生成唯一文件名：原文件名_时间戳_UUID前8位.扩展名
      const timestamp = Date.now()
      const uuid = this.generateUUID().substring(0, 8)
      const uniqueFileName = `${baseName}_${timestamp}_${uuid}${extension}`

      // 按日期分目录存储：yyyy/MM/dd
      const date = new Date()
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      const datePath = `${year}/${month}/${day}`

      // 构建完整路径
      const pathPrefix = this.pathPrefix || 'exam/question'
      return `${pathPrefix}/${datePath}/${uniqueFileName}`
    },

    /**
     * 生成文件路径（不含前缀）
     * 用于阿里云上传，因为后端 token.dir 已包含前缀
     * 格式：2025/11/20/filename_timestamp_uuid.ext
     */
    generateFilePathWithoutPrefix(fileName) {
      if (!fileName) {
        fileName = this.generateUUID() + '.tmp'
      }

      // 提取文件扩展名
      let extension = ''
      let baseName = fileName
      const lastDotIndex = fileName.lastIndexOf('.')
      if (lastDotIndex > 0) {
        extension = fileName.substring(lastDotIndex)
        baseName = fileName.substring(0, lastDotIndex)
      }

      // 检查文件名是否包含非 ASCII 字符（中文等）
      const hasNonAscii = /[^\x00-\x7F]/.test(baseName)
      if (hasNonAscii) {
        const prefix = extension.toLowerCase().match(/\.(mp3|wav|m4a|ogg|flac)$/) ? 'audio' : 'file'
        baseName = `${prefix}_${this.generateUUID().substring(0, 16)}`
      }

      // 生成唯一文件名
      const timestamp = Date.now()
      const uuid = this.generateUUID().substring(0, 8)
      const uniqueFileName = `${baseName}_${timestamp}_${uuid}${extension}`

      // 按日期分目录存储：yyyy/MM/dd
      const date = new Date()
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      const datePath = `${year}/${month}/${day}`

      // 只返回日期路径 + 文件名（不含前缀）
      return `${datePath}/${uniqueFileName}`
    },

    /**
     * 生成UUID（简化版）
     */
    generateUUID() {
      // 生成类似Java UUID的格式：xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
      return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
        const r = Math.random() * 16 | 0
        const v = c === 'x' ? r : (r & 0x3 | 0x8)
        return v.toString(16)
      })
    },

    /**
     * 获取音频时长（秒）
     */
    getAudioDuration(file) {
      return new Promise((resolve) => {
        if (!this.isAudioFile(file)) {
          resolve(null)
          return
        }
        
        const audio = new Audio()
        const objectUrl = URL.createObjectURL(file.raw || file)
        
        audio.addEventListener('loadedmetadata', () => {
          const duration = Math.round(audio.duration) // 四舍五入到秒
          URL.revokeObjectURL(objectUrl)
          console.log('[OssUpload] 获取音频时长:', duration, '秒')
          resolve(duration)
        })
        
        audio.addEventListener('error', () => {
          console.warn('[OssUpload] 获取音频时长失败:', file.name)
          URL.revokeObjectURL(objectUrl)
          resolve(null)
        })
        
        audio.src = objectUrl
      })
    },

    /**
     * 上传成功回调
     */
    async handleSuccess(response, file) {
      console.log('[OssUpload] handleSuccess 开始')
      console.log('[OssUpload] handleSuccess response:', response)
      console.log('[OssUpload] handleSuccess file:', file)
      console.log('[OssUpload] handleSuccess ossConfig:', this.ossConfig)
      console.log('[OssUpload] handleSuccess listType:', this.listType)
      console.log('[OssUpload] handleSuccess 当前 fileList:', this.fileList)
      
      this.uploading = false
      
      let fileUrl = ''
      
      if (this.ossConfig.ossType === 'qiniu') {
        // 七牛云：从响应中获取key，拼接domain
        // 七牛云响应格式：{ hash: "...", key: "..." }
        const key = response.key || this.uploadData.key
        console.log('[OssUpload] 七牛云上传 - key:', key)
        // 确保domain不以/结尾，key不以/开头
        const domain = this.ossConfig.domain.replace(/\/$/, '')
        const cleanKey = key.replace(/^\//, '')
        fileUrl = domain + '/' + cleanKey
        console.log('[OssUpload] 七牛云上传 - domain:', domain)
        console.log('[OssUpload] 七牛云上传 - fileUrl:', fileUrl)
      } else if (this.ossConfig.ossType === 'aliyun') {
        // 阿里云：PostObject成功时，文件URL = domain + key
        // 注意：阿里云PostObject成功时返回状态码204，没有响应体
        // 需要从上传参数中获取key
        const key = this.uploadData.key
        console.log('[OssUpload] 阿里云上传 - key:', key)
        // 确保domain不以/结尾，key不以/开头
        const domain = this.ossConfig.domain.replace(/\/$/, '')
        const cleanKey = key.replace(/^\//, '')
        fileUrl = domain + '/' + cleanKey
        console.log('[OssUpload] 阿里云上传 - domain:', domain)
        console.log('[OssUpload] 阿里云上传 - fileUrl:', fileUrl)
      }
      
      console.log('[OssUpload] handleSuccess 计算得到的 fileUrl:', fileUrl)
      
      // 如果是音频文件，获取时长
      let audioDuration = null
      if (this.isAudioFile(file)) {
        audioDuration = await this.getAudioDuration(file)
        console.log('[OssUpload] handleSuccess 音频时长:', audioDuration, '秒')
      }
      
      // 更新文件对象（确保 file.url 正确设置，Element UI 的 picture-card 会直接使用 file.url）
      file.url = fileUrl
      file.response = response
      file.status = 'success'
      if (audioDuration !== null) {
        file.duration = audioDuration
      }
      
      // 更新 fileList，确保显示正确的文件信息（特别是 picture-card 类型需要正确的 url）
      // 注意：Element UI 的 el-upload 在 picture-card 模式下会直接使用 file.url 显示缩略图
      // 规范化图片URL，确保是完整的HTTP/HTTPS URL
      const normalizedUrl = this.normalizeImageUrl(fileUrl)
      console.log('[OssUpload] handleSuccess 规范化后的 normalizedUrl:', normalizedUrl)
      
      // 获取带签名的下载URL（用于私有bucket）
      let displayUrl = normalizedUrl
      try {
        console.log('[OssUpload] handleSuccess 开始获取带签名的下载URL')
        const downloadResponse = await getMediaDownloadUrl({ url: normalizedUrl })
        console.log('[OssUpload] handleSuccess getMediaDownloadUrl 响应:', downloadResponse)
        const signedUrl = downloadResponse?.downloadUrl || downloadResponse?.data?.downloadUrl
        if (downloadResponse?.code === 200 && signedUrl) {
          displayUrl = signedUrl
          console.log('[OssUpload] handleSuccess 使用带签名的URL:', displayUrl)
        } else {
          console.log('[OssUpload] handleSuccess 未获取到签名URL，使用原始URL')
        }
      } catch (error) {
        console.error('[OssUpload] handleSuccess 获取签名URL失败:', error)
        console.log('[OssUpload] handleSuccess 使用原始URL作为fallback')
      }
      
      const newFileItem = {
        name: file.name,
        url: displayUrl, // 使用带签名的URL（如果有），否则使用原始URL
        status: 'success',
        uid: file.uid, // 保持原有的 uid，确保 Element UI 能正确识别文件
        response: response
      }
      
      // 如果是音频文件，添加时长信息
      if (audioDuration !== null) {
        newFileItem.duration = audioDuration
      }
      
      console.log('[OssUpload] handleSuccess 创建的 newFileItem:', newFileItem)
      
      // 同时更新 file.url，确保 Element UI 能正确使用
      file.url = displayUrl
      
      // 更新 fileList（对于 picture-card 类型，使用自定义缩略图卡片）
      // 如果 limit 为 1，替换现有文件；否则追加
      if (this.limit === 1) {
        // 对于 picture-card 类型，需要确保响应式更新
        if (this.listType === 'picture-card') {
          this.fileList = []
          this.$nextTick(() => {
            this.$set(this, 'fileList', [newFileItem])
            this.$nextTick(() => {
              this.$forceUpdate()
            })
          })
        } else {
          this.$set(this, 'fileList', [newFileItem])
        }
      } else {
        // 追加新文件
        const currentList = [...this.fileList]
        currentList.push(newFileItem)
        this.$set(this, 'fileList', currentList)
        if (this.listType === 'picture-card') {
          this.$nextTick(() => {
            this.$forceUpdate()
          })
        }
      }
      
      // 触发change事件，传递URL和时长信息
      this.$nextTick(() => {
        console.log('[OssUpload] handleSuccess 触发 change 事件，fileUrl:', fileUrl, 'duration:', audioDuration)
        // 如果change事件需要传递对象，传递包含URL和duration的对象
        // 否则只传递URL（保持向后兼容）
        const changeData = audioDuration !== null ? { url: fileUrl, duration: audioDuration } : fileUrl
        this.emitChange(changeData)
      })
      
      // 触发progress事件，通知父组件上传完成（false表示上传结束）
      this.$emit('progress', { percent: 100 }, file)
      
      // 触发文件上传成功事件，传递文件信息（包含时长）
      this.$emit('success', fileUrl, file, audioDuration)
      
      console.log('[OssUpload] handleSuccess 完成')
      // 根据文件类型显示不同的成功提示
      const isAudio = this.isAudioFile(file)
      this.$modal.msgSuccess(isAudio ? '音频上传成功' : '上传成功')
    },

    /**
     * 上传失败回调
     */
    handleError(err, file) {
      this.uploading = false
      console.error('上传失败:', err, file)
      
      // 触发progress事件，通知父组件上传结束（false表示上传结束）
      this.$emit('progress', { percent: 0 }, file)
      
      this.$modal.msgError('上传失败: ' + (err.message || '未知错误'))
    },

    /**
     * 上传进度回调
     */
    handleProgress(event, file) {
      // 可以在这里处理上传进度
      this.$emit('progress', event, file)
    },

    /**
     * 触发change事件
     * @param {string|object} urlOrData - URL字符串或包含url和duration的对象
     */
    emitChange(urlOrData) {
      // 处理对象格式（包含url和duration）
      let url = null
      if (typeof urlOrData === 'object' && urlOrData !== null) {
        url = urlOrData.url || urlOrData
      } else {
        url = urlOrData
      }
      
      if (this.limit === 1) {
        // 单个文件：
        // 1. 对于 v-model（input事件），传递URL字符串，确保组件能正确回显
        // 2. 对于 change事件，传递对象（包含url和duration）以便父组件获取时长信息
        this.$emit('input', url) // v-model 使用 URL 字符串
        this.$emit('change', typeof urlOrData === 'object' && urlOrData !== null ? urlOrData : url) // change 事件传递完整对象
      } else {
        // 多个文件
        const urls = this.fileList.map(f => f.url).filter(Boolean)
        if (url) {
          urls.push(url)
        }
        this.$emit('input', urls)
        this.$emit('change', urls)
      }
    },

    /**
     * 清空文件列表
     */
    clearFiles() {
      this.$refs.upload && this.$refs.upload.clearFiles()
      this.fileList = []
      this.emitChange(null)
    },

    /**
     * 手动上传
     */
    submit() {
      this.$refs.upload && this.$refs.upload.submit()
    },

    /**
     * 触发文件选择（用于按钮点击）
     */
    triggerUpload(event) {
      // 阻止事件冒泡和默认行为，避免触发 el-upload 的默认行为
      if (event) {
        event.stopPropagation()
        event.preventDefault()
      }
      
      // 直接查找并触发文件输入框，使用 setTimeout 确保在事件处理完成后触发
      setTimeout(() => {
        if (this.$refs.upload && this.$refs.upload.$el) {
          const uploadEl = this.$refs.upload.$el
          const fileInput = uploadEl.querySelector('input[type="file"]')
          if (fileInput) {
            fileInput.click()
          }
        }
      }, 10)
    },

    /**
     * 文件移除回调
     */
    handleRemove(file, fileList) {
      // 更新内部文件列表
      this.fileList = fileList
      // 触发change事件，传递null表示移除
      this.emitChange(null)
      // 触发remove事件
      this.$emit('remove', file, fileList)
    },

    /**
     * 判断是否为音频文件
     */
    isAudioFile(file) {
      const audioExtensions = ['.mp3', '.wav', '.ogg', '.m4a', '.aac', '.flac', '.wma']
      const fileName = file.name || ''
      const extension = fileName.substring(fileName.lastIndexOf('.')).toLowerCase()
      return audioExtensions.includes(extension)
    },

    /**
     * 获取文件图标
     */
    getFileIcon(file) {
      if (this.isAudioFile(file)) {
        return 'el-icon-headset'
      }
      // 可以根据文件类型返回不同图标
      const imageExtensions = ['.jpg', '.jpeg', '.png', '.gif', '.bmp', '.webp']
      const fileName = file.name || ''
      const extension = fileName.substring(fileName.lastIndexOf('.')).toLowerCase()
      if (imageExtensions.includes(extension)) {
        return 'el-icon-picture'
      }
      return 'el-icon-document'
    },

    /**
     * Element UI 的预览处理（用于 picture-card 类型）
     * 当 list-type="picture-card" 时，Element UI 会自动调用此方法
     */
    handlePreview(file) {
      // 优先使用 file.url，如果没有则从 value 中获取
      let fileUrl = file?.url
      if (!fileUrl) {
        // 尝试从 fileList 中查找
        const fileInList = this.fileList.find(f => f.uid === file.uid || f.name === file.name)
        if (fileInList && fileInList.url) {
          fileUrl = fileInList.url
        } else {
          // 最后尝试从 value 中获取
          fileUrl = this.value
        }
      }
      
      if (!fileUrl) {
        if (this.$message) {
          this.$message.warning('文件地址不存在')
        }
        return
      }
      
      // 构建预览文件对象
      const previewFile = {
        ...file,
        url: fileUrl,
        name: file?.name || this.getFileNameFromUrl(fileUrl)
      }
      
      // 触发preview事件，让父组件处理预览逻辑
      this.$emit('preview', previewFile)
    },

    /**
     * 文件点击处理（预览，用于 text 类型）
     */
    handleFileClick(file) {
      // 对于 text 类型，直接调用 handlePreview
      this.handlePreview(file)
    },

    /**
     * 文件移除处理
     */
    handleFileRemove(file) {
      // 调用 el-upload 的 handleRemove 方法
      if (this.$refs.upload) {
        this.$refs.upload.handleRemove(file)
      }
    },

    /**
     * 自定义缩略图卡片删除处理
     */
    handleCustomRemove(file) {
      if (this.disabled) {
        return
      }
      // 从 fileList 中移除
      const index = this.fileList.findIndex(f => f.uid === file.uid)
      if (index > -1) {
        this.fileList.splice(index, 1)
        // 触发change事件
        this.emitChange(null)
        // 触发remove事件
        this.$emit('remove', file, this.fileList)
      }
    },

    /**
     * 图片加载错误处理
     */
    handleImageError() {
      // 可以在这里处理图片加载错误
      console.warn('图片加载失败')
    },


    /**
     * 文件变化处理（用于处理二次上传覆盖）
     */
    handleFileChange(file, fileList) {
      // 当新文件被选择时（status 为 ready），如果有已上传成功的文件，立即清空旧文件列表
      // 这样新文件可以正常上传，而不是被 limit 阻止
      if (file.status === 'ready') {
        // 检查是否有已上传成功的文件（通过 url 或 status 判断）
        const existingFiles = this.fileList || []
        // 如果旧文件没有uid(数据库加载)或者uid不同(手动上传的其他文件),都视为需要替换的旧文件
        const uploadedFiles = existingFiles.filter(f => {
          const isUploaded = f.status === 'success' || f.url
          const isDifferentFile = !f.uid || f.uid !== file.uid
          return isUploaded && isDifferentFile
        })

        if (uploadedFiles.length > 0) {
          // 立即清空旧文件列表，只保留当前新文件
          // 使用 $nextTick 确保清空操作完成后再更新
          this.$nextTick(() => {
            // 只保留当前新文件（status 为 ready 的文件）
            const newFileList = fileList.filter(f => f.uid === file.uid || f.status === 'ready')
            this.fileList = newFileList
            // 清空 v-model 值，让新文件可以上传
            this.$emit('input', null)
            this.$emit('change', null)
          })
        }
      }
      
      // 触发 change 事件，让父组件可以监听文件变化
      this.$emit('file-change', file, fileList)
    },

    /**
     * 文件超出限制处理（用于二次上传替换）
     */
    handleExceed(files, fileList) {
      // 当用户尝试上传新文件但已达到limit时，清空旧文件列表并允许新文件上传
      if (this.limit === 1 && files && files.length > 0) {
        this.$modal.msgWarning('将替换现有文件')
        // 清空旧文件列表
        this.fileList = []
        // 清空 v-model 值
        this.$emit('input', null)
        this.$emit('change', null)
        // 等待DOM更新后，手动触发文件选择
        this.$nextTick(() => {
          // 手动添加新文件到上传列表
          const file = files[0]
          if (this.$refs.upload) {
            this.$refs.upload.handleStart(file)
            this.$refs.upload.submit()
          }
        })
      } else {
        this.$modal.msgError(`上传文件数量不能超过 ${this.limit} 个!`)
      }
    }
  }
}
</script>

<style scoped>
.oss-upload {
  width: 100%;
}

/* 自定义缩略图卡片列表 */
.custom-picture-card-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

/* 自定义缩略图卡片项 */
.custom-picture-card-item {
  position: relative;
  width: 148px;
  height: 148px;
  border: 1px solid #c0ccda;
  border-radius: 6px;
  overflow: hidden;
  background-color: #fafafa;
  transition: all 0.3s;
}

.custom-picture-card-item:hover {
  border-color: #409EFF;
}

.custom-picture-card-image {
  width: 100%;
  height: 100%;
}

.custom-picture-card-image ::v-deep .el-image__inner {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.custom-picture-card-actions {
  position: absolute;
  top: 0;
  right: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 20px;
  opacity: 0;
  transition: opacity 0.3s;
}

.custom-picture-card-item:hover .custom-picture-card-actions {
  opacity: 1;
}

.custom-picture-card-actions i {
  color: #fff;
  font-size: 20px;
  cursor: pointer;
  transition: color 0.3s;
}

.custom-picture-card-actions i:hover {
  color: #409EFF;
}

.custom-picture-card-actions i.is-disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

/* 自定义上传按钮 */
.custom-picture-card-upload {
  width: 148px;
  height: 148px;
  border: 1px dashed #c0ccda;
  border-radius: 6px;
  background-color: #fafafa;
  transition: all 0.3s;
  cursor: pointer;
}

.custom-picture-card-upload:hover {
  border-color: #409EFF;
}

.custom-picture-card-upload-btn {
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  color: #8c939d;
  font-size: 28px;
}

.custom-picture-card-upload ::v-deep .el-upload {
  width: 100%;
  height: 100%;
  display: block;
}

/* 图片加载错误占位符 */
.image-slot {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 100%;
  color: #909399;
  font-size: 30px;
  background-color: #f5f7fa;
}

/* 文件名称样式（超链接样式） */
.oss-upload ::v-deep .upload-file-name {
  color: #409EFF;
  cursor: pointer;
  text-decoration: none;
  margin-right: 8px;
  display: inline-block;
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  user-select: none;
  pointer-events: auto;
}

.oss-upload ::v-deep .el-upload-list__item-name {
  cursor: pointer;
  pointer-events: auto;
}

.upload-file-name:hover {
  color: #66b1ff;
  text-decoration: underline;
}

.upload-file-name.is-audio {
  color: #67C23A;
}

.upload-file-name.is-audio:hover {
  color: #85ce61;
}

/* 移除按钮样式 */
.upload-file-remove {
  color: #909399;
  cursor: pointer;
  margin-left: 8px;
  font-size: 16px;
}

.upload-file-remove:hover {
  color: #f56c6c;
}

/* 隐藏上传区域（当 listType 为 text 且 showFileList 为 false 时，改用按钮样式） */
.oss-upload-no-button ::v-deep .el-upload {
  border: none;
  background: transparent;
  /* 禁用 el-upload 的默认点击行为，避免重复触发文件选择 */
  pointer-events: none;
}
/* 确保提示文字和文件列表仍然可以交互 */
.oss-upload-no-button ::v-deep .el-upload__tip,
.oss-upload-no-button ::v-deep .el-upload-list {
  pointer-events: auto;
}
.oss-upload-no-button ::v-deep .el-upload__input {
  display: none !important;
  pointer-events: none !important;
}
.oss-upload-no-button ::v-deep .el-upload__input-wrapper {
  display: none !important;
  pointer-events: none !important;
}
/* 隐藏拖拽区域（如果有） */
.oss-upload-no-button ::v-deep .el-upload-dragger {
  display: none !important;
}
/* 确保提示文字和文件列表仍然显示 */
.oss-upload-no-button ::v-deep .el-upload__tip {
  display: block;
  margin-top: 7px;
}
.oss-upload-no-button ::v-deep .el-upload-list {
  display: block;
}
</style>

