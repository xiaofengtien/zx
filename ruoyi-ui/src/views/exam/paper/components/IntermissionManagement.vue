<template>
  <div class="intermission-management">
    <div class="header-actions">
      <el-button type="primary" size="small" icon="el-icon-plus" @click="handleAdd">新增中场配置</el-button>
      <el-button type="danger" size="small" icon="el-icon-delete" :disabled="selectedIds.length === 0" @click="handleBatchDelete">批量删除</el-button>
    </div>

    <el-table
      :data="intermissionList"
      border
      v-loading="loading"
      @selection-change="handleSelectionChange"
      row-key="id"
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="从卷别" prop="fromVolume" width="100" align="center" />
      <el-table-column label="到卷别" prop="toVolume" width="100" align="center" />
      <el-table-column label="中场提示文字" prop="intermissionText" min-width="200" show-overflow-tooltip />
      <el-table-column label="是否可跳过" prop="canSkip" width="120" align="center">
        <template slot-scope="scope">
          <el-tag :type="scope.row.canSkip ? 'info' : 'danger'">
            {{ scope.row.canSkip ? '可跳过' : '不可跳过' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" align="center" fixed="right">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" style="color: #f56c6c;" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      :title="dialogTitle"
      :visible.sync="dialogVisible"
      width="700px"
      :close-on-click-modal="false"
    >
      <el-form ref="intermissionForm" :model="intermissionForm" :rules="intermissionRules" label-width="120px">
        <el-form-item label="从卷别" prop="fromVolumeId">
          <el-select
            v-model="fromVolumeSelectValue"
            placeholder="请选择从卷别"
            style="width: 100%"
            @visible-change="handleVolumeSelectVisible"
            filterable
          >
            <el-option
              v-for="volume in volumeList"
              :key="volume.id || volume.tempId || `volume_${volume.volumeCode}`"
              :label="`${volume.volumeName}${volume.tempId ? '' : ''}`"
              :value="volume.tempId || volume.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="到卷别" prop="toVolumeId">
          <el-select
            v-model="toVolumeSelectValue"
            placeholder="请选择到卷别"
            style="width: 100%"
            @visible-change="handleVolumeSelectVisible"
            filterable
          >
            <el-option
              v-for="volume in volumeList"
              :key="volume.id || volume.tempId || `volume_${volume.volumeCode}`"
              :label="`${volume.volumeName}${volume.tempId ? '' : ''}`"
              :value="volume.tempId || volume.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="中场提示文字" prop="intermissionText">
          <el-input v-model="intermissionForm.intermissionText" type="textarea" :rows="3" placeholder="请输入中场提示文字（可选）" maxlength="500" />
        </el-form-item>
        <el-form-item label="中场音频">
          <oss-upload
            ref="intermissionAudioUpload"
            v-model="intermissionAudioUrl"
            :limit="1"
            accept=".mp3,.wav,.ogg,.m4a,.aac"
            :file-size="10"
            tip="只能上传音频文件，且不超过10MB，仅支持一个文件，二次上传将直接替换"
            path-prefix="exam/question"
            list-type="text"
            @change="handleIntermissionAudioChange"
            @progress="(event, file) => handleUploadProgress('intermissionAudio', event.percent < 100)"
            @preview="handleIntermissionAudioPreview"
          />
          <div v-if="uploadStatus.intermissionAudio" style="color: #E6A23C; font-size: 12px; margin-top: 5px;">
            请先等待上传完成
          </div>
        </el-form-item>
        <el-form-item label="是否可跳过" prop="canSkip">
          <el-switch
            v-model="intermissionForm.canSkip"
            :active-value="true"
            :inactive-value="false"
            active-text="可跳过"
            inactive-text="不可跳过"
          />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="handleSubmit">确 定</el-button>
        <el-button @click="dialogVisible = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getPaperIntermissionList, addPaperIntermission, updatePaperIntermission, deletePaperIntermission } from "@/api/exam/paper"
import { getPaperVolumeList } from "@/api/exam/paper"
import { getQuestionMediaByIntermissionId } from "@/api/exam/paper"
import { saveQuestionMedia, removeQuestionMedia } from "@/api/exam/question"
import { getToken } from "@/utils/auth"
import OssUpload from "@/components/OssUpload"

export default {
  name: "IntermissionManagement",
  components: { OssUpload },
  props: {
    paperId: {
      type: Number,
      default: undefined
    }
  },
  computed: {
    /** 从卷别选择器的值（统一处理真实ID和临时ID） */
    fromVolumeSelectValue: {
      get() {
        // 优先返回临时ID，如果没有则返回真实ID
        return this.intermissionForm.fromVolumeTempId || this.intermissionForm.fromVolumeId || null
      },
      set(value) {
        // 设置时通过 handleFromVolumeChange 处理
        this.handleFromVolumeChange(value)
      }
    },
    /** 到卷别选择器的值（统一处理真实ID和临时ID） */
    toVolumeSelectValue: {
      get() {
        // 优先返回临时ID，如果没有则返回真实ID
        return this.intermissionForm.toVolumeTempId || this.intermissionForm.toVolumeId || null
      },
      set(value) {
        // 设置时通过 handleToVolumeChange 处理
        this.handleToVolumeChange(value)
      }
    }
  },
  data() {
    return {
      loading: false,
      loadingVolumeList: false, // 加载卷别列表的锁，防止重复请求
      intermissionList: [],
      volumeList: [],
      selectedIds: [],
      dialogVisible: false,
      dialogTitle: "新增中场配置",
      initialIntermissionIds: [], // 记录编辑模式下初始加载的中场配置ID（用于计算需要删除的项）
      intermissionForm: {
        id: undefined,
        paperId: undefined,
        fromVolumeId: undefined,
        toVolumeId: undefined,
        fromVolumeTempId: undefined,
        toVolumeTempId: undefined,
        fromVolume: undefined, // 保留用于显示
        toVolume: undefined, // 保留用于显示
        intermissionText: undefined,
        canSkip: false
      },
      intermissionRules: {
        fromVolumeId: [
          {
            validator: (rule, value, callback) => {
              // 检查真实ID或临时ID是否至少有一个有值
              const hasRealId = this.intermissionForm.fromVolumeId !== null && this.intermissionForm.fromVolumeId !== undefined
              const hasTempId = this.intermissionForm.fromVolumeTempId !== null && this.intermissionForm.fromVolumeTempId !== undefined && this.intermissionForm.fromVolumeTempId !== ''
              if (!hasRealId && !hasTempId) {
                callback(new Error("从卷别不能为空"))
              } else {
                callback()
              }
            },
            trigger: "change"
          }
        ],
        toVolumeId: [
          {
            validator: (rule, value, callback) => {
              // 检查真实ID或临时ID是否至少有一个有值
              const hasRealId = this.intermissionForm.toVolumeId !== null && this.intermissionForm.toVolumeId !== undefined
              const hasTempId = this.intermissionForm.toVolumeTempId !== null && this.intermissionForm.toVolumeTempId !== undefined && this.intermissionForm.toVolumeTempId !== ''
              if (!hasRealId && !hasTempId) {
                callback(new Error("到卷别不能为空"))
              } else {
                callback()
              }
            },
            trigger: "change"
          }
        ]
      },
      uploadMediaUrl: process.env.VUE_APP_BASE_API + "/question/media/upload",
      uploadHeaders: {
        Authorization: "Bearer " + getToken()
      },
      intermissionAudioMap: {}, // 存储每个中场配置的音频信息 { intermissionId: { url, path, mediaId } }
      intermissionAudioFileList: [], // 当前编辑的中场配置的音频文件列表
      intermissionAudioUrl: null, // 中场音频URL（用于 OssUpload 的 v-model）
      uploadStatus: {
        intermissionAudio: false
      }
    }
  },
  watch: {
    paperId: {
      immediate: true, // 组件创建时立即执行一次
      handler(newVal, oldVal) {
        // 如果 paperId 变为 undefined/null（从编辑模式切换到新增模式），清空数据
        if (!newVal && oldVal) {
          this.intermissionList = []
          this.intermissionAudioMap = {}
          this.volumeList = []
          return
        }
        // 只在 paperId 有值时触发（编辑模式）
        if (newVal) {
          // 延迟执行，确保组件完全初始化
          this.$nextTick(() => {
            // 不在这里加载卷别列表，改为按需加载（用户点击下拉框时）
            // 只加载中场配置列表
            this.loadIntermissionList()
          })
        }
        // 新增模式（paperId 为 undefined）时，不加载数据，使用本地数据管理
      }
    }
  },
  methods: {
    /** 卷别下拉框显示/隐藏时触发（使用 visible-change 替代 focus，避免重复触发） */
    handleVolumeSelectVisible(visible) {
      console.log('[IntermissionManagement] handleVolumeSelectVisible 触发', {
        visible: visible,
        paperId: this.paperId,
        volumeListLength: this.volumeList.length,
        loadingVolumeList: this.loadingVolumeList
      })

      // 只在打开下拉框时加载（visible === true）
      if (!visible) {
        return
      }

      // 完全从试卷结构组件获取，不调用后端接口
      if (!this.loadingVolumeList) {
          this.loadVolumeListFromStructure()
      } else {
        console.log('[IntermissionManagement] 正在加载中，跳过刷新')
      }
    },
    /** 从试卷结构组件获取卷别列表（完全从页面获取，不调用后端） */
    loadVolumeListFromStructure() {
      console.log('[IntermissionManagement] loadVolumeListFromStructure 开始')

      // 尝试多种方式获取试卷结构组件
      let paperStructureTable = null

      // 方式1: 通过 $parent.$parent.$parent 获取（因为 IntermissionManagement 在 el-tab-pane 下，el-tab-pane 在 el-tabs 下，el-tabs 在父组件下）
      // IntermissionManagement -> el-tab-pane -> el-tabs -> add.vue/edit.vue
      if (this.$parent && this.$parent.$parent && this.$parent.$parent.$parent && this.$parent.$parent.$parent.$refs) {
        paperStructureTable = this.$parent.$parent.$parent.$refs.paperStructureTable
        console.log('[IntermissionManagement] 通过 $parent.$parent.$parent 获取:', {
          exists: !!paperStructureTable,
          hasRefs: !!this.$parent.$parent.$parent.$refs,
          refsKeys: this.$parent.$parent.$parent.$refs ? Object.keys(this.$parent.$parent.$parent.$refs) : []
        })
      }

      // 方式2: 如果方式1失败，尝试通过 $parent.$parent（el-tabs 层级）
      if (!paperStructureTable && this.$parent && this.$parent.$parent && this.$parent.$parent.$refs) {
        paperStructureTable = this.$parent.$parent.$refs.paperStructureTable
        console.log('[IntermissionManagement] 通过 $parent.$parent 获取:', {
          exists: !!paperStructureTable,
          hasRefs: !!this.$parent.$parent.$refs,
          refsKeys: this.$parent.$parent.$refs ? Object.keys(this.$parent.$parent.$refs) : []
        })
      }

      // 方式3: 通过 $root 查找（如果组件在同一个根下）
      if (!paperStructureTable && this.$root && this.$root.$refs) {
        paperStructureTable = this.$root.$refs.paperStructureTable
        console.log('[IntermissionManagement] 通过 $root 获取:', {
          exists: !!paperStructureTable,
          refsKeys: Object.keys(this.$root.$refs)
        })
      }

      // 方式4: 通过 $parent 向上查找所有层级
      if (!paperStructureTable) {
        let current = this.$parent
        let level = 0
        while (current && level < 10) {
          if (current.$refs && current.$refs.paperStructureTable) {
            paperStructureTable = current.$refs.paperStructureTable
            console.log('[IntermissionManagement] 通过向上查找第', level, '层获取:', {
              exists: !!paperStructureTable,
              componentName: current.$options.name || current.$options._componentTag || 'Unknown'
            })
            break
          }
          current = current.$parent
          level++
        }
      }

      console.log('[IntermissionManagement] paperStructureTable 最终结果:', {
        exists: !!paperStructureTable,
        volumeList: paperStructureTable ? paperStructureTable.volumeList : null,
        volumeListLength: paperStructureTable && paperStructureTable.volumeList ? paperStructureTable.volumeList.length : 0,
        volumeListType: paperStructureTable && paperStructureTable.volumeList ? typeof paperStructureTable.volumeList : 'N/A',
        isArray: paperStructureTable && paperStructureTable.volumeList ? Array.isArray(paperStructureTable.volumeList) : false
      })

      if (paperStructureTable && paperStructureTable.volumeList && Array.isArray(paperStructureTable.volumeList)) {
        // 转换为卷别选择器需要的格式，包含 id 或 tempId
        const rawVolumes = paperStructureTable.volumeList
        console.log('[IntermissionManagement] 原始卷别列表:', rawVolumes.map(v => ({
          id: v.id,
          idType: typeof v.id,
          idString: String(v.id),
          isTemp: v.id && String(v.id).startsWith('temp_'),
          volumeName: v.volumeName,
          volumeCode: v.volumeCode,
          volumeOrder: v.volumeOrder
        })))

        this.volumeList = rawVolumes.map(volume => {
          const volumeId = volume.id
          // 判断是否为临时ID：如果 id 是字符串且以 temp_ 开头
          const isTempId = volumeId && String(volumeId).startsWith('temp_')
          return {
            id: isTempId ? undefined : volumeId, // 如果是临时ID，id 设为 undefined
            tempId: isTempId ? volumeId : undefined, // 如果是临时ID，tempId 设为 volumeId
          volumeCode: volume.volumeCode || volume.volumeName,
            volumeName: volume.volumeName,
            volumeOrder: volume.volumeOrder
          }
        }).sort((a, b) => {
          const orderA = a.volumeOrder || 0
          const orderB = b.volumeOrder || 0
          return orderA - orderB
        })

        console.log('[IntermissionManagement] 转换后的卷别列表:', this.volumeList.map(v => ({
          id: v.id,
          tempId: v.tempId,
          volumeName: v.volumeName,
          value: v.id || v.tempId
        })))
      } else {
        console.warn('[IntermissionManagement] 无法获取试卷结构组件的卷别列表', {
          paperStructureTable: !!paperStructureTable,
          hasVolumeList: paperStructureTable && !!paperStructureTable.volumeList,
          isArray: paperStructureTable && paperStructureTable.volumeList ? Array.isArray(paperStructureTable.volumeList) : false,
          parentChain: this.getParentChain()
        })
        this.volumeList = []
      }
    },

    /** 获取父组件链（用于调试） */
    getParentChain() {
      const chain = []
      let current = this.$parent
      let level = 0
      while (current && level < 10) {
        chain.push({
          level: level,
          componentName: current.$options.name || current.$options._componentTag || 'Unknown',
          hasRefs: !!current.$refs,
          refsKeys: current.$refs ? Object.keys(current.$refs) : []
        })
        current = current.$parent
        level++
      }
      return chain
    },
    /** 处理从卷别变化 */
    handleFromVolumeChange(value) {
      console.log('[IntermissionManagement] handleFromVolumeChange 触发', {
        value: value,
        valueType: typeof value,
        volumeListLength: this.volumeList.length
      })

      if (!value) {
        // 清空选择
        this.intermissionForm.fromVolumeId = null
        this.intermissionForm.fromVolumeTempId = null
        this.intermissionForm.fromVolume = null
        return
      }

      // 查找匹配的卷别（支持真实ID和临时ID）
      const volume = this.volumeList.find(v => {
        const volumeValue = v.id || v.tempId
        return volumeValue === value || String(volumeValue) === String(value)
      })

      console.log('[IntermissionManagement] 找到的卷别:', {
        volume: volume,
        volumeId: volume ? volume.id : null,
        volumeTempId: volume ? volume.tempId : null
      })

      if (volume) {
        this.$set(this.intermissionForm, 'fromVolume', volume.volumeName) // 保留用于显示
        // 判断是临时ID还是真实ID
        if (volume.tempId) {
          // 临时ID
          this.$set(this.intermissionForm, 'fromVolumeTempId', volume.tempId)
          this.$set(this.intermissionForm, 'fromVolumeId', null)
          console.log('[IntermissionManagement] 设置临时ID:', volume.tempId)
        } else if (volume.id) {
          // 真实ID
          this.$set(this.intermissionForm, 'fromVolumeId', typeof volume.id === 'number' ? volume.id : parseInt(volume.id, 10))
          this.$set(this.intermissionForm, 'fromVolumeTempId', null)
          console.log('[IntermissionManagement] 设置真实ID:', this.intermissionForm.fromVolumeId)
        }

        // 触发表单验证（因为验证规则检查的是 fromVolumeId，但实际值可能在 fromVolumeTempId 中）
        this.$nextTick(() => {
          if (this.$refs.intermissionForm) {
            this.$refs.intermissionForm.validateField('fromVolumeId')
          }
        })
      } else {
        console.warn('[IntermissionManagement] 未找到匹配的卷别:', value)
      }
    },
    /** 处理到卷别变化 */
    handleToVolumeChange(value) {
      console.log('[IntermissionManagement] handleToVolumeChange 触发', {
        value: value,
        valueType: typeof value,
        volumeListLength: this.volumeList.length,
        currentToVolumeId: this.intermissionForm.toVolumeId,
        currentToVolumeTempId: this.intermissionForm.toVolumeTempId
      })

      if (value === null || value === undefined || value === '') {
        // 清空选择
        this.$set(this.intermissionForm, 'toVolumeId', null)
        this.$set(this.intermissionForm, 'toVolumeTempId', null)
        this.$set(this.intermissionForm, 'toVolume', null)
        return
      }

      // 查找匹配的卷别（优先匹配 tempId，然后匹配 id）
      const volume = this.volumeList.find(v => {
        const tempIdMatch = v.tempId && (v.tempId === value || String(v.tempId) === String(value))
        const idMatch = v.id && (v.id === value || String(v.id) === String(value) || (typeof v.id === 'number' && v.id === parseInt(value, 10)))
        return tempIdMatch || idMatch
      })

      console.log('[IntermissionManagement] 找到的卷别:', {
        volume: volume,
        volumeId: volume ? volume.id : null,
        volumeTempId: volume ? volume.tempId : null,
        value: value
      })

      if (volume) {
        this.$set(this.intermissionForm, 'toVolume', volume.volumeName) // 保留用于显示
        // 判断是临时ID还是真实ID
        if (volume.tempId) {
          // 临时ID
          this.$set(this.intermissionForm, 'toVolumeTempId', volume.tempId)
          this.$set(this.intermissionForm, 'toVolumeId', null)
          console.log('[IntermissionManagement] 设置临时ID:', volume.tempId)
        } else if (volume.id) {
          // 真实ID
          this.$set(this.intermissionForm, 'toVolumeId', typeof volume.id === 'number' ? volume.id : parseInt(volume.id, 10))
          this.$set(this.intermissionForm, 'toVolumeTempId', null)
          console.log('[IntermissionManagement] 设置真实ID:', this.intermissionForm.toVolumeId)
        }

        // 触发表单验证（因为验证规则检查的是 toVolumeId，但实际值可能在 toVolumeTempId 中）
        this.$nextTick(() => {
          if (this.$refs.intermissionForm) {
            this.$refs.intermissionForm.validateField('toVolumeId')
          }
        })

        // 触发表单验证（因为验证规则检查的是 toVolumeId，但实际值可能在 toVolumeTempId 中）
        this.$nextTick(() => {
          if (this.$refs.intermissionForm) {
            this.$refs.intermissionForm.validateField('toVolumeId')
          }
        })
      } else {
        console.warn('[IntermissionManagement] 未找到匹配的卷别:', value, 'volumeList:', this.volumeList)
      }
    },
    /** 加载卷别列表（完全从页面获取，不调用后端接口） */
    loadVolumeList() {
      console.log('[IntermissionManagement] loadVolumeList 开始（完全从页面获取）', {
        paperId: this.paperId,
        loadingVolumeList: this.loadingVolumeList
      })

      // 完全从试卷结构组件获取，不调用后端接口
      this.loadVolumeListFromStructure()
    },
    /** 加载中场配置列表（仅在编辑模式下，从后端加载已有数据） */
    loadIntermissionList() {
      // 只在编辑模式（paperId 存在）且需要加载已有数据时调用
      // 新增模式下不加载，使用本地数据管理
      if (!this.paperId) {
        // 新增模式，初始化空列表
        this.intermissionList = []
        this.initialIntermissionIds = []
        return
      }

      // 编辑模式：从后端加载已有数据（仅在首次加载时）
      // 注意：如果已经有本地数据（用户已操作），则不重新加载，避免覆盖用户的操作
      if (this.intermissionList.length > 0) {
        // 已有本地数据，不重新加载
        return
      }

      this.loading = true
      getPaperIntermissionList({ id: this.paperId }).then(response => {
        if (response.code === 200) {
          this.intermissionList = response.data || []
          // 记录初始加载的ID列表（用于后续计算需要删除的项）
          this.initialIntermissionIds = this.intermissionList
            .filter(i => i.id && !String(i.id).startsWith('temp_'))
            .map(i => i.id)
          // 加载每个中场配置的音频信息
          this.intermissionList.forEach(intermission => {
            this.loadIntermissionAudio(intermission.id)
          })
        } else {
          this.$modal.msgError(response.msg || "加载中场配置列表失败")
        }
        this.loading = false
      }).catch(() => {
        this.loading = false
        this.$modal.msgError("加载中场配置列表失败")
      })
    },
    /** 加载中场音频 */
    loadIntermissionAudio(intermissionId) {
      getQuestionMediaByIntermissionId({ intermissionId }).then(response => {
        if (response.code === 200 && response.data && response.data.length > 0) {
          // media_type=9 表示中场音频
          const audioMedia = response.data.find(m => m.mediaType === 9)
          if (audioMedia) {
            this.$set(this.intermissionAudioMap, intermissionId, {
              url: audioMedia.mediaUrl,
              path: audioMedia.mediaPath,
              mediaId: audioMedia.id
            })
          }
        }
      }).catch(() => {
        // 忽略错误
      })
    },
    /** 获取中场音频文件列表 */
    getIntermissionAudioFileList(row) {
      const audio = this.intermissionAudioMap[row.id]
      if (audio && audio.url) {
        return [{
          name: audio.url.split('/').pop(),
          url: audio.url
        }]
      }
      return []
    },
    getFileNameFromUrl(url) {
      if (!url) return ''
      const cleanUrl = url.split('?')[0]
      const segments = cleanUrl.split('/')
      return segments[segments.length - 1] || ''
    },
    async resolveMediaPreviewUrl(rawUrl) {
      if (!rawUrl || rawUrl.trim() === '') {
        return ''
      }
      try {
        const { getMediaDownloadUrl } = await import('@/api/exam/question')
        const { normalizeMediaUrl } = await import('@/utils/media')
        // 确保 url 参数存在且有效
        if (rawUrl && rawUrl.trim() !== '') {
        const response = await getMediaDownloadUrl({ url: rawUrl })
        const signedUrl = response?.downloadUrl || response?.data?.downloadUrl
        if (response?.code === 200 && signedUrl) {
          return signedUrl
          }
          return normalizeMediaUrl(rawUrl)
        }
        return normalizeMediaUrl(rawUrl)
      } catch (error) {
        console.error('获取媒体下载链接失败:', error)
        const { normalizeMediaUrl } = await import('@/utils/media')
        return normalizeMediaUrl(rawUrl)
      }
    },
    async updateIntermissionAudioFileList(rawUrl, fileName) {
      if (!rawUrl || rawUrl.trim() === '') {
        this.intermissionAudioFileList = []
        return
      }
      const previewUrl = await this.resolveMediaPreviewUrl(rawUrl)
      this.intermissionAudioFileList = [{
        name: fileName || this.getFileNameFromUrl(rawUrl),
        url: previewUrl || rawUrl, // 如果获取预览URL失败，使用原始URL
        status: 'success'
      }]
    },
    /** 中场音频文件变化处理（OssUpload组件回调） */
    handleIntermissionAudioChange(urlOrData) {
      // OssUpload 组件上传成功后，会通过 @change 事件传递文件URL或包含url和duration的对象
      if (urlOrData) {
        const url = typeof urlOrData === 'object' ? urlOrData.url : urlOrData
        const duration = typeof urlOrData === 'object' ? urlOrData.duration : null

        if (!url || url.trim() === '') {
          this.handleIntermissionAudioRemove()
          return
        }

        const intermissionId = this.intermissionForm.id || 'temp_' + Date.now()
        // 更新 intermissionAudioUrl，确保 OssUpload 组件能正确回显
        this.intermissionAudioUrl = url
        // 缓存音频URL和路径（用于后续保存）
        this.$set(this.intermissionAudioMap, intermissionId, {
          url: url,
          path: url, // OssUpload 返回的是完整URL，path 也使用 URL
          duration: duration // 音频时长（秒）
        })
        // 更新文件列表用于显示
        this.updateIntermissionAudioFileList(url)
      } else {
        // URL 为空表示移除
        this.handleIntermissionAudioRemove()
      }
    },

    /** 中场音频移除（OssUpload组件回调） */
    handleIntermissionAudioRemove() {
      this.handleUploadProgress('intermissionAudio', false)
      this.intermissionAudioUrl = null
      const intermissionId = this.intermissionForm.id || 'temp_' + Date.now()
      this.$set(this.intermissionAudioMap, intermissionId, null)
      this.intermissionAudioFileList = []
    },
    /** 中场音频预览 */
    handleIntermissionAudioPreview(file) {
      const previewFile = {
        ...file,
        url: file?.url || file?.response?.url || this.intermissionAudioMap[this.intermissionForm.id]?.url
      }
      if (!previewFile.url) {
        this.$message.warning('音频地址不存在')
        return
      }
      this.$emit('preview-audio', previewFile)
    },
    /** 中场音频文件移除（自定义模板中的移除按钮） */
    handleIntermissionAudioSlotRemove(file) {
      if (this.$refs.intermissionAudioUpload && file) {
        this.$refs.intermissionAudioUpload.handleRemove(file)
      }
    },
    /** 处理上传进度 */
    handleUploadProgress(type, isUploading) {
      this.$set(this.uploadStatus, type, isUploading)
    },
    /** 媒体文件上传错误 */
    handleMediaUploadError(err) {
      this.$modal.msgError("上传失败：" + (err.message || "未知错误"))
    },
    /** 选择变化 */
    handleSelectionChange(selection) {
      this.selectedIds = selection.map(item => item.id)
    },
    /** 新增 */
    handleAdd() {
      // 完全从页面获取卷别列表，不调用后端
      // 每次都刷新以获取最新的卷别（包括新增的）
      if (!this.loadingVolumeList) {
          this.loadVolumeListFromStructure()
      }
      this.dialogTitle = "新增中场配置"
      // 生成临时ID
      const tempId = `temp_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
      this.intermissionForm = {
        id: tempId, // 使用临时ID
        paperId: this.paperId,
        fromVolume: this.volumeList.length > 0 ? this.volumeList[0].volumeCode : undefined,
        toVolume: this.volumeList.length > 1 ? this.volumeList[1].volumeCode : undefined,
        intermissionText: undefined,
        canSkip: false
      }
      // 清空音频文件列表和URL
      this.intermissionAudioFileList = []
      this.intermissionAudioUrl = null
      this.dialogVisible = true
      this.$nextTick(() => {
        this.$refs.intermissionForm && this.$refs.intermissionForm.clearValidate()
      })
    },
    /** 编辑 */
    async handleEdit(row) {
      this.dialogTitle = "编辑中场配置"
      this.intermissionForm = {
        id: row.id,
        paperId: row.paperId,
        fromVolumeId: row.fromVolumeId || null,
        toVolumeId: row.toVolumeId || null,
        fromVolumeTempId: row.fromVolumeTempId || null,
        toVolumeTempId: row.toVolumeTempId || null,
        fromVolume: row.fromVolume,
        toVolume: row.toVolume,
        intermissionText: row.intermissionText,
        canSkip: row.canSkip !== undefined ? row.canSkip : false
      }

      // 如果编辑时已有卷别ID，需要确保下拉框能正确显示
      // 通过触发一次加载来确保 volumeList 已加载
      if (this.volumeList.length === 0) {
        this.loadVolumeListFromStructure()
      }

      // 加载音频文件列表
      const audio = this.intermissionAudioMap[row.id]
      if (audio && audio.url) {
        this.intermissionAudioUrl = audio.url // 设置 OssUpload 的 v-model
        await this.updateIntermissionAudioFileList(audio.url)
      } else {
        this.intermissionAudioFileList = []
        this.intermissionAudioUrl = null
      }
      this.dialogVisible = true
      this.$nextTick(() => {
        this.$refs.intermissionForm && this.$refs.intermissionForm.clearValidate()
      })
    },
    /** 删除 */
    handleDelete(row) {
      this.$modal.confirm('确定要删除中场配置"' + row.fromVolume + ' -> ' + row.toVolume + '"吗？').then(() => {
        // 所有操作都在前端完成，等待统一保存
        const index = this.intermissionList.findIndex(i => i.id === row.id)
        if (index !== -1) {
          this.intermissionList.splice(index, 1)
          // 删除音频缓存
          if (this.intermissionAudioMap[row.id]) {
            this.$delete(this.intermissionAudioMap, row.id)
          }
          this.$modal.msgSuccess("删除成功（待保存）")
        }
      }).catch(() => {})
    },
    /** 批量删除 */
    handleBatchDelete() {
      if (this.selectedIds.length === 0) {
        this.$modal.msgWarning("请选择要删除的中场配置")
        return
      }
      this.$modal.confirm('确定要删除选中的 ' + this.selectedIds.length + ' 个中场配置吗？').then(() => {
        // 所有操作都在前端完成，等待统一保存
        this.selectedIds.forEach(id => {
          const index = this.intermissionList.findIndex(i => i.id === id)
          if (index !== -1) {
            this.intermissionList.splice(index, 1)
            // 删除音频缓存
            if (this.intermissionAudioMap[id]) {
              this.$delete(this.intermissionAudioMap, id)
            }
          }
        })
        this.selectedIds = []
        this.$modal.msgSuccess("删除成功（待保存）")
      }).catch(() => {})
    },
    /** 提交 */
    handleSubmit() {
      this.$refs.intermissionForm.validate(valid => {
        if (valid) {
          if (this.intermissionForm.fromVolume === this.intermissionForm.toVolume) {
            this.$modal.msgError("从卷别和到卷别不能相同")
            return
          }

          // 判断是新增还是编辑（通过检查ID是否为临时ID）
          const isTempId = this.intermissionForm.id && String(this.intermissionForm.id).startsWith('temp_')
          const existingIntermission = this.intermissionList.find(i => i.id === this.intermissionForm.id)
          const isEdit = !isTempId && existingIntermission

          if (isEdit) {
            // 编辑：更新本地数据
            if (existingIntermission) {
              existingIntermission.fromVolumeId = this.intermissionForm.fromVolumeId
              existingIntermission.toVolumeId = this.intermissionForm.toVolumeId
              existingIntermission.fromVolumeTempId = this.intermissionForm.fromVolumeTempId
              existingIntermission.toVolumeTempId = this.intermissionForm.toVolumeTempId
              existingIntermission.fromVolume = this.intermissionForm.fromVolume
              existingIntermission.toVolume = this.intermissionForm.toVolume
              existingIntermission.intermissionText = this.intermissionForm.intermissionText
              existingIntermission.canSkip = this.intermissionForm.canSkip
            }
            // 如果有音频缓存，更新到对应的ID
            const audioKey = this.intermissionForm.tempAudioId || this.intermissionForm.id
            const audio = this.intermissionAudioMap[audioKey]
            if (audio) {
              this.$set(this.intermissionAudioMap, this.intermissionForm.id, audio)
              if (this.intermissionForm.tempAudioId && this.intermissionForm.tempAudioId !== this.intermissionForm.id) {
                this.$delete(this.intermissionAudioMap, this.intermissionForm.tempAudioId)
              }
            }

            // 所有操作都在前端完成，等待统一保存
            this.$modal.msgSuccess("修改成功（待保存）")
            this.dialogVisible = false
          } else {
            // 新增：添加到本地数据
            const newIntermission = {
              id: this.intermissionForm.id, // 使用临时ID
              paperId: this.paperId,
              fromVolumeId: this.intermissionForm.fromVolumeId,
              toVolumeId: this.intermissionForm.toVolumeId,
              fromVolumeTempId: this.intermissionForm.fromVolumeTempId,
              toVolumeTempId: this.intermissionForm.toVolumeTempId,
              fromVolume: this.intermissionForm.fromVolume,
              toVolume: this.intermissionForm.toVolume,
              intermissionText: this.intermissionForm.intermissionText,
              canSkip: this.intermissionForm.canSkip
            }
            this.intermissionList.push(newIntermission)

            // 如果有音频缓存，迁移到新的ID
            const audioKey = this.intermissionForm.tempAudioId || this.intermissionForm.id
            const audio = this.intermissionAudioMap[audioKey]
            if (audio) {
              this.$set(this.intermissionAudioMap, this.intermissionForm.id, audio)
              if (this.intermissionForm.tempAudioId && this.intermissionForm.tempAudioId !== this.intermissionForm.id) {
                this.$delete(this.intermissionAudioMap, this.intermissionForm.tempAudioId)
              }
            }

            // 所有操作都在前端完成，等待统一保存
            this.$modal.msgSuccess("新增成功（待保存）")
            this.dialogVisible = false
          }
        }
      })
    },
    /** 获取所有待保存的中场配置数据（供父组件调用）
     * 采用"删除后新增"策略，返回统一列表，后端会先删除再全部新增
     */
    getIntermissionData() {
      const intermissions = []

      this.intermissionList.forEach(intermission => {
        // 获取缓存的音频URL（如果存在）
        const audioInfo = this.intermissionAudioMap[intermission.id]
        const intermissionData = {
          // 不包含 id 字段，后端会先删除再全部新增
          // paperId 由后端设置
          fromVolumeId: intermission.fromVolumeId || null,
          toVolumeId: intermission.toVolumeId || null,
          fromVolumeTempId: intermission.fromVolumeTempId || null,
          toVolumeTempId: intermission.toVolumeTempId || null,
          fromVolume: intermission.fromVolume,
          toVolume: intermission.toVolume,
          intermissionText: intermission.intermissionText,
          canSkip: intermission.canSkip !== undefined ? (intermission.canSkip ? 1 : 0) : 0
        }

        // 如果有缓存的音频URL，添加到数据中
        if (audioInfo && audioInfo.url) {
          intermissionData.intermissionAudioUrl = audioInfo.url
          intermissionData.intermissionAudioPath = audioInfo.path || audioInfo.url
          intermissionData.intermissionAudioDuration = audioInfo.duration || null
        }

        // 如果是临时ID，保留 tempId 用于前端标识（后端不使用）
        if (intermission.id && String(intermission.id).startsWith('temp_')) {
          intermissionData.tempId = intermission.id
        }

        intermissions.push(intermissionData)
      })

      // 返回统一列表，后端会先删除再全部新增
      return intermissions
    }
  }
}
</script>

<style scoped>
.intermission-management {
  padding: 20px;
}

.header-actions {
  margin-bottom: 20px;
}

.header-actions .el-button {
  margin-right: 10px;
}

/* 音频上传组件样式 */
.upload-file-name {
  cursor: pointer;
  color: #409EFF;
  margin-right: 8px;
}

.upload-file-name:hover {
  text-decoration: underline;
}

.upload-file-remove {
  cursor: pointer;
  color: #909399;
  margin-left: 8px;
}

.upload-file-remove:hover {
  color: #F56C6C;
}
</style>

