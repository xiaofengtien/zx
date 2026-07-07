<template>
  <div class="volume-management">
    <div class="header-actions">
      <el-button type="primary" size="small" icon="el-icon-plus" @click="handleAdd">新增卷别</el-button>
      <el-button type="danger" size="small" icon="el-icon-delete" :disabled="selectedIds.length === 0" @click="handleBatchDelete">批量删除</el-button>
    </div>

    <el-table
      :data="volumeList"
      border
      v-loading="loading"
      @selection-change="handleSelectionChange"
      row-key="id"
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="卷别编码" prop="volumeCode" width="120" align="center" />
      <el-table-column label="卷别名称" prop="volumeName" min-width="150" />
      <el-table-column label="排序" prop="volumeOrder" width="80" align="center" />
      <el-table-column label="卷别名称音频" width="200">
        <template slot-scope="scope">
          <el-upload
            :action="uploadMediaUrl"
            :headers="uploadHeaders"
            :before-upload="(file) => handleVolumeAudioBeforeUpload(file, scope.row)"
            :on-success="(response, file) => handleVolumeAudioSuccess(response, file, scope.row)"
            :on-error="handleMediaUploadError"
            :on-remove="() => handleVolumeAudioRemove(scope.row)"
            :limit="1"
            :file-list="getVolumeAudioFileList(scope.row)"
            :show-file-list="false"
          >
            <el-button size="mini" type="text" icon="el-icon-upload2">上传音频</el-button>
          </el-upload>
          <span v-if="scope.row.volumeAudioUrl" style="margin-left: 10px; color: #67C23A;">
            <i class="el-icon-check"></i> 已上传
          </span>
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
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form ref="volumeForm" :model="volumeForm" :rules="volumeRules" label-width="120px">
        <el-form-item label="卷别编码" prop="volumeCode">
          <el-input v-model="volumeForm.volumeCode" placeholder="请输入卷别编码，如：A、B、C" maxlength="10" />
        </el-form-item>
        <el-form-item label="卷别名称" prop="volumeName">
          <el-input v-model="volumeForm.volumeName" placeholder="请输入卷别名称，如：A卷、B卷" maxlength="50" />
        </el-form-item>
        <el-form-item label="排序" prop="volumeOrder">
          <el-input-number v-model="volumeForm.volumeOrder" :min="1" :max="999" controls-position="right" style="width: 200px" />
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
import { getPaperVolumeList, addPaperVolume, updatePaperVolume, deletePaperVolume, batchSavePaperVolume } from "@/api/exam/paper"
import { getQuestionMediaByVolumeId } from "@/api/exam/paper"
import { saveQuestionMedia, removeQuestionMedia } from "@/api/exam/question"
import { getToken } from "@/utils/auth"

export default {
  name: "VolumeManagement",
  props: {
    paperId: {
      type: Number,
      default: undefined
    }
  },
  data() {
    return {
      loading: false,
      loadingVolumeList: false, // 加载卷别列表的锁，防止重复请求
      volumeList: [],
      selectedIds: [],
      dialogVisible: false,
      dialogTitle: "新增卷别",
      volumeForm: {
        id: undefined,
        paperId: undefined,
        volumeCode: undefined,
        volumeName: undefined,
        volumeOrder: 1
      },
      volumeRules: {
        volumeCode: [
          { required: true, message: "卷别编码不能为空", trigger: "blur" },
          { max: 10, message: "卷别编码长度不能超过10个字符", trigger: "blur" }
        ],
        volumeName: [
          { required: true, message: "卷别名称不能为空", trigger: "blur" },
          { max: 50, message: "卷别名称长度不能超过50个字符", trigger: "blur" }
        ],
        volumeOrder: [
          { required: true, message: "排序不能为空", trigger: "blur" }
        ]
      },
      uploadMediaUrl: process.env.VUE_APP_BASE_API + "/question/media/upload",
      uploadHeaders: {
        Authorization: "Bearer " + getToken()
      },
      volumeAudioMap: {} // 存储每个卷别的音频信息 { volumeId: { url, path } }
    }
  },
  watch: {
    paperId: {
      immediate: true, // 组件创建时立即执行一次
      handler(newVal, oldVal) {
        // 只在 paperId 有值时触发（immediate: true 时 oldVal 为 undefined）
        if (newVal) {
          // 延迟执行，确保组件完全初始化，并防止重复请求
          this.$nextTick(() => {
            if (!this.loadingVolumeList && this.paperId === newVal) {
              this.loadVolumeList()
            }
          })
        }
      }
    }
  },
  methods: {
    /** 加载卷别列表 */
    loadVolumeList() {
      if (!this.paperId) return
      // 防止重复请求
      if (this.loadingVolumeList) {
        return
      }
      this.loadingVolumeList = true
      this.loading = true
      getPaperVolumeList({ id: this.paperId }).then(response => {
        if (response.code === 200) {
          this.volumeList = response.data || []
          // 加载每个卷别的音频信息
          this.volumeList.forEach(volume => {
            this.loadVolumeAudio(volume.id)
          })
        } else {
          this.$modal.msgError(response.msg || "加载卷别列表失败")
        }
        this.loading = false
        this.loadingVolumeList = false
      }).catch(() => {
        this.loading = false
        this.loadingVolumeList = false
        this.$modal.msgError("加载卷别列表失败")
      })
    },
    /** 加载卷别音频 */
    loadVolumeAudio(volumeId) {
      getQuestionMediaByVolumeId({ volumeId }).then(response => {
        if (response.code === 200 && response.data && response.data.length > 0) {
          // media_type=7 表示卷别名称音频
          const audioMedia = response.data.find(m => m.mediaType === 7)
          if (audioMedia) {
            this.$set(this.volumeAudioMap, volumeId, {
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
    /** 获取卷别音频文件列表 */
    getVolumeAudioFileList(row) {
      const audio = this.volumeAudioMap[row.id]
      if (audio && audio.url) {
        return [{
          name: audio.url.split('/').pop(),
          url: audio.url
        }]
      }
      return []
    },
    /** 卷别音频上传前校验 */
    handleVolumeAudioBeforeUpload(file, row) {
      const isAudio = ['audio/mpeg', 'audio/wav', 'audio/ogg', 'audio/mp4', 'audio/aac'].includes(file.type)
      const fileSizeMB = file.size / 1024 / 1024
      const maxSize = 10 // MB
      if (!isAudio) {
        this.$modal.msgError('只能上传音频文件！')
        return false
      }
      if (fileSizeMB > maxSize) {
        this.$modal.msgError(`上传音频大小不能超过 ${maxSize} MB！当前文件大小为 ${fileSizeMB.toFixed(2)} MB`)
        return false
      }
      return true
    },
    /** 卷别音频上传成功 */
    handleVolumeAudioSuccess(response, file, row) {
      if (response.code === 200) {
        // 保存音频信息到 question_media 表
        const mediaBO = {
          paperId: this.paperId,
          volumeId: row.id,
          mediaType: 7, // 卷别名称音频
          mediaName: file.name,
          mediaPath: response.data.path || response.data.url,
          mediaUrl: response.data.url,
          mediaSize: file.size,
          mediaFormat: file.name.split('.').pop(),
          storageType: 0 // 在线路径
        }
        saveQuestionMedia(mediaBO).then(saveResponse => {
          if (saveResponse.code === 200) {
            this.$set(this.volumeAudioMap, row.id, {
              url: response.data.url,
              path: response.data.path || response.data.url,
              mediaId: saveResponse.data
            })
            this.$modal.msgSuccess("上传成功")
          } else {
            this.$modal.msgError(saveResponse.msg || "保存媒体记录失败")
          }
        }).catch(() => {
          this.$modal.msgError("保存媒体记录失败")
        })
      } else {
        this.$modal.msgError(response.msg || "上传失败")
      }
    },
    /** 卷别音频移除 */
    handleVolumeAudioRemove(row) {
      const audio = this.volumeAudioMap[row.id]
      if (audio && audio.mediaId) {
        removeQuestionMedia({ id: audio.mediaId }).then(response => {
          if (response.code === 200) {
            this.$set(this.volumeAudioMap, row.id, null)
            this.$modal.msgSuccess("删除成功")
          } else {
            this.$modal.msgError(response.msg || "删除失败")
          }
        }).catch(() => {
          this.$modal.msgError("删除失败")
        })
      } else {
        this.$set(this.volumeAudioMap, row.id, null)
      }
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
      this.dialogTitle = "新增卷别"
      this.volumeForm = {
        id: undefined,
        paperId: this.paperId,
        volumeCode: undefined,
        volumeName: undefined,
        volumeOrder: (this.volumeList.length + 1)
      }
      this.dialogVisible = true
      this.$nextTick(() => {
        this.$refs.volumeForm && this.$refs.volumeForm.clearValidate()
      })
    },
    /** 编辑 */
    handleEdit(row) {
      this.dialogTitle = "编辑卷别"
      this.volumeForm = {
        id: row.id,
        paperId: row.paperId,
        volumeCode: row.volumeCode,
        volumeName: row.volumeName,
        volumeOrder: row.volumeOrder
      }
      this.dialogVisible = true
      this.$nextTick(() => {
        this.$refs.volumeForm && this.$refs.volumeForm.clearValidate()
      })
    },
    /** 删除 */
    handleDelete(row) {
      this.$modal.confirm('确定要删除卷别"' + row.volumeName + '"吗？').then(() => {
        deletePaperVolume({ ids: [row.id] }).then(response => {
          if (response.code === 200) {
            this.$modal.msgSuccess("删除成功")
            this.loadVolumeList()
            this.$emit('refresh')
          } else {
            this.$modal.msgError(response.msg || "删除失败")
          }
        }).catch(() => {
          this.$modal.msgError("删除失败")
        })
      }).catch(() => {})
    },
    /** 批量删除 */
    handleBatchDelete() {
      if (this.selectedIds.length === 0) {
        this.$modal.msgWarning("请选择要删除的卷别")
        return
      }
      this.$modal.confirm('确定要删除选中的 ' + this.selectedIds.length + ' 个卷别吗？').then(() => {
        deletePaperVolume({ ids: this.selectedIds }).then(response => {
          if (response.code === 200) {
            this.$modal.msgSuccess("删除成功")
            this.loadVolumeList()
            this.$emit('refresh')
          } else {
            this.$modal.msgError(response.msg || "删除失败")
          }
        }).catch(() => {
          this.$modal.msgError("删除失败")
        })
      }).catch(() => {})
    },
    /** 提交 */
    handleSubmit() {
      this.$refs.volumeForm.validate(valid => {
        if (valid) {
          if (this.volumeForm.id) {
            // 编辑
            updatePaperVolume(this.volumeForm).then(response => {
              if (response.code === 200) {
                this.$modal.msgSuccess("修改成功")
                this.dialogVisible = false
                this.loadVolumeList()
                this.$emit('refresh')
              } else {
                this.$modal.msgError(response.msg || "修改失败")
              }
            }).catch(() => {
              this.$modal.msgError("修改失败")
            })
          } else {
            // 新增
            addPaperVolume(this.volumeForm).then(response => {
              if (response.code === 200) {
                this.$modal.msgSuccess("新增成功")
                this.dialogVisible = false
                this.loadVolumeList()
                this.$emit('refresh')
              } else {
                this.$modal.msgError(response.msg || "新增失败")
              }
            }).catch(() => {
              this.$modal.msgError("新增失败")
            })
          }
        }
      })
    }
  }
}
</script>

<style scoped>
.volume-management {
  padding: 20px;
}

.header-actions {
  margin-bottom: 20px;
}

.header-actions .el-button {
  margin-right: 10px;
}
</style>

