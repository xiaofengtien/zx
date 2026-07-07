<template>
  <div class="section-management">
    <!-- 卷别筛选 -->
    <div class="filter-bar" style="margin-bottom: 20px;">
      <el-select
        v-model="selectedVolumeId"
        placeholder="请先选择卷别"
        style="width: 200px;"
        @change="handleVolumeFilterChange"
        @focus="handleVolumeSelectFocus"
        clearable
      >
        <el-option
          v-for="volume in volumeList"
          :key="volume.id"
          :label="volume.volumeName"
          :value="volume.id"
        />
      </el-select>
      <span v-if="!selectedVolumeId" style="margin-left: 10px; color: #909399;">
        请先选择卷别，然后管理该卷别下的大题
      </span>
    </div>

    <div class="header-actions" v-if="selectedVolumeId">
      <el-button type="primary" size="small" icon="el-icon-plus" @click="handleAdd">新增大题</el-button>
      <el-button type="danger" size="small" icon="el-icon-delete" :disabled="selectedIds.length === 0" @click="handleBatchDelete">批量删除</el-button>
    </div>

    <el-table
      v-if="selectedVolumeId"
      :data="filteredSectionList"
      border
      v-loading="loading"
      @selection-change="handleSelectionChange"
      row-key="id"
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="卷别" prop="volumeCode" width="100" align="center" />
      <el-table-column label="大题名称" prop="sectionName" min-width="150" />
      <el-table-column label="排序" prop="sectionOrder" width="80" align="center" />
      <el-table-column label="题目数量" prop="questionCount" width="100" align="center" />
      <el-table-column label="总分" prop="totalScore" width="100" align="center">
        <template slot-scope="scope">
          <span>{{ scope.row.totalScore || 0 }}</span>
        </template>
      </el-table-column>
      <el-table-column label="每题分值" prop="scorePerQuestion" width="100" align="center">
        <template slot-scope="scope">
          <span>{{ scope.row.scorePerQuestion || 0 }}</span>
        </template>
      </el-table-column>
      <el-table-column label="大题说明音频" width="200">
        <template slot-scope="scope">
          <el-upload
            :action="uploadMediaUrl"
            :headers="uploadHeaders"
            :before-upload="(file) => handleSectionAudioBeforeUpload(file, scope.row)"
            :on-success="(response, file) => handleSectionAudioSuccess(response, file, scope.row)"
            :on-error="handleMediaUploadError"
            :on-remove="() => handleSectionAudioRemove(scope.row)"
            :limit="1"
            :file-list="getSectionAudioFileList(scope.row)"
            :show-file-list="false"
          >
            <el-button size="mini" type="text" icon="el-icon-upload2">上传音频</el-button>
          </el-upload>
          <span v-if="scope.row.sectionAudioUrl" style="margin-left: 10px; color: #67C23A;">
            <i class="el-icon-check"></i> 已上传
          </span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="250" align="center" fixed="right">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button size="mini" type="text" icon="el-icon-link" @click="handleManageQuestions(scope.row)">关联题目</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" style="color: #f56c6c;" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 空状态提示 -->
    <div v-if="selectedVolumeId && filteredSectionList.length === 0" style="text-align: center; padding: 40px; color: #909399;">
      <i class="el-icon-info" style="font-size: 48px; margin-bottom: 10px;"></i>
      <p>该卷别下暂无大题，请先添加大题</p>
    </div>
    <div v-if="!selectedVolumeId" style="text-align: center; padding: 40px; color: #909399;">
      <i class="el-icon-info" style="font-size: 48px; margin-bottom: 10px;"></i>
      <p>请先选择卷别，然后管理该卷别下的大题</p>
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      :title="dialogTitle"
      :visible.sync="dialogVisible"
      width="700px"
      :close-on-click-modal="false"
    >
      <el-form ref="sectionForm" :model="sectionForm" :rules="sectionRules" label-width="120px">
        <el-form-item label="卷别" prop="volumeCode">
          <el-select v-model="sectionForm.volumeCode" placeholder="请选择卷别" style="width: 100%" @change="handleVolumeCodeChange">
            <el-option
              v-for="volume in volumeList"
              :key="volume.id || volume.volumeCode"
              :label="volume.volumeName"
              :value="volume.volumeCode"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="大题名称" prop="sectionName">
          <el-input v-model="sectionForm.sectionName" placeholder="请输入大题名称，如：第一节、第二节" maxlength="50" />
        </el-form-item>
        <el-form-item label="排序" prop="sectionOrder">
          <el-input-number v-model="sectionForm.sectionOrder" :min="1" :max="999" controls-position="right" style="width: 200px" />
        </el-form-item>
        <el-form-item label="题目数量" prop="questionCount">
          <el-input-number v-model="sectionForm.questionCount" :min="0" :max="999" controls-position="right" style="width: 200px" />
        </el-form-item>
        <el-form-item label="总分" prop="totalScore">
          <el-input-number
            v-model="sectionForm.totalScore"
            :min="0"
            :precision="2"
            :step="0.1"
            controls-position="right"
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item label="每题分值" prop="scorePerQuestion">
          <el-input-number
            v-model="sectionForm.scorePerQuestion"
            :min="0"
            :precision="2"
            :step="0.1"
            controls-position="right"
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item label="大题说明" prop="instructionText">
          <el-input v-model="sectionForm.instructionText" type="textarea" :rows="3" placeholder="请输入大题说明（可选）" maxlength="500" />
        </el-form-item>
        <el-form-item label="作答时间" prop="answerTime">
          <el-input-number
            v-model="sectionForm.answerTime"
            :min="1"
            :max="3600"
            controls-position="right"
            style="width: 200px"
          />
          <span style="margin-left: 10px; color: #909399; font-size: 12px;">
            （秒）
          </span>
        </el-form-item>
        <el-form-item label="音频播放次数" prop="audioPlayCount">
          <el-input-number
            v-model="sectionForm.audioPlayCount"
            :min="1"
            :max="10"
            controls-position="right"
            style="width: 200px"
          />
          <span style="margin-left: 10px; color: #909399; font-size: 12px;">
            （每道题有音频的情况下播放多少次）
          </span>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="handleSubmit">确 定</el-button>
        <el-button @click="dialogVisible = false">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 题目关联对话框 -->
    <el-dialog
      title="关联题目"
      :visible.sync="questionManageVisible"
      width="1200px"
      append-to-body
      :close-on-click-modal="false"
    >
      <div v-if="currentSection">
        <div style="margin-bottom: 15px;">
          <span style="font-weight: bold;">大题：{{ currentSection.sectionName }}</span>
          <span style="margin-left: 20px; color: #909399;">已关联 {{ sectionQuestions.length }} 题</span>
        </div>

        <!-- 题目选择操作栏 -->
        <div style="margin-bottom: 15px;">
          <el-button type="primary" size="small" icon="el-icon-plus" @click="handleSelectQuestions">选择题库题目</el-button>
          <el-button
            v-if="sectionQuestions.length > 0"
            type="danger"
            size="small"
            icon="el-icon-delete"
            @click="handleClearAllSectionQuestions"
          >
            清空全部
          </el-button>
        </div>

        <!-- 已关联题目列表 -->
        <el-table
          v-if="sectionQuestions.length > 0"
          :data="sectionQuestions"
          border
          row-key="questionId"
          max-height="400"
        >
          <el-table-column label="序号" width="80" align="center">
            <template slot-scope="scope">
              <span>{{ scope.row.sectionOrder || (scope.$index + 1) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="题目标题" prop="title" min-width="250" show-overflow-tooltip />
          <el-table-column label="题目类型" prop="type" width="100" align="center">
            <template slot-scope="scope">
              <dict-tag :options="dict.type.question_type" :value="scope.row.type" />
            </template>
          </el-table-column>
          <el-table-column label="学科" prop="subjectId" width="100" align="center">
            <template slot-scope="scope">
              <dict-tag :options="dict.type.subject" :value="scope.row.subjectId" />
            </template>
          </el-table-column>
          <el-table-column label="分值" width="130" align="center">
            <template slot-scope="scope">
              <el-input-number
                v-model="scope.row.score"
                :min="0"
                :precision="2"
                :step="0.1"
                controls-position="right"
                size="small"
                style="width: 100px;"
              />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" align="center">
            <template slot-scope="scope">
              <el-button
                type="text"
                icon="el-icon-delete"
                size="mini"
                @click="handleRemoveSectionQuestion(scope.$index)"
              >删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-else description="暂未关联题目" :image-size="100" />
      </div>
      <div slot="footer" class="dialog-footer">
        <el-button @click="questionManageVisible = false">取 消</el-button>
        <el-button type="primary" @click="handleSaveSectionQuestions">保 存</el-button>
      </div>
    </el-dialog>

    <!-- 题目选择弹窗（复用edit.vue中的逻辑） -->
    <el-dialog
      title="选择题库题目"
      :visible.sync="questionSelectVisible"
      width="1200px"
      append-to-body
    >
      <splitpanes :horizontal="false" class="default-theme">
        <pane size="20">
          <div class="category-tree-container">
            <el-input
              v-model="questionSelectCategoryName"
              placeholder="筛选分类"
              size="small"
              prefix-icon="el-icon-search"
              style="margin-bottom: 10px;"
            />
            <el-tree
              ref="questionSelectCategoryTree"
              :data="questionSelectCategoryTree"
              :props="defaultProps"
              node-key="id"
              :default-expand-all="true"
              :filter-node-method="filterNode"
              highlight-current
              @node-click="handleQuestionSelectCategoryClick"
            >
              <span class="custom-tree-node" slot-scope="{ node, data }">
                <span>{{ node.label }}</span>
              </span>
            </el-tree>
          </div>
        </pane>
        <pane size="80">
          <div class="question-list-container">
            <el-form :model="questionSelectQueryParams" ref="questionSelectQueryForm" size="small" :inline="true">
              <el-form-item label="题目标题">
                <el-input
                  v-model="questionSelectQueryParams.title"
                  placeholder="请输入题目标题"
                  clearable
                  style="width: 240px"
                  @keyup.enter.native="handleQuestionSelectQuery"
                />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuestionSelectQuery">搜索</el-button>
                <el-button icon="el-icon-refresh" size="mini" @click="resetQuestionSelectQuery">重置</el-button>
              </el-form-item>
            </el-form>
            <el-table
              ref="questionSelectTable"
              v-loading="questionSelectLoading"
              :data="questionSelectList"
              row-key="id"
              @selection-change="handleQuestionSelectSelectionChange"
              max-height="400"
            >
              <el-table-column type="selection" width="55" align="center" :reserve-selection="true" />
              <el-table-column label="题目标题" prop="title" min-width="200" show-overflow-tooltip />
              <el-table-column label="题目类型" prop="type" width="100" align="center">
                <template slot-scope="scope">
                  <dict-tag :options="dict.type.question_type" :value="scope.row.type" />
                </template>
              </el-table-column>
              <el-table-column label="学科" prop="subjectId" width="100" align="center">
                <template slot-scope="scope">
                  <dict-tag :options="dict.type.subject" :value="scope.row.subjectId" />
                </template>
              </el-table-column>
            </el-table>
            <pagination
              v-show="questionSelectTotal > 0"
              :total="questionSelectTotal"
              :page.sync="questionSelectQueryParams.pageNum"
              :limit.sync="questionSelectQueryParams.pageSize"
              @pagination="getQuestionSelectList"
            />
          </div>
        </pane>
      </splitpanes>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="handleQuestionSelectConfirm">确 定</el-button>
        <el-button @click="questionSelectVisible = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getPaperSectionList, addPaperSection, updatePaperSection, deletePaperSection, getPaperQuestionList, batchSavePaperQuestion } from "@/api/exam/paper"
import { getPaperVolumeList } from "@/api/exam/paper"
import { getQuestionMediaBySectionId, saveQuestionMedia, removeQuestionMedia } from "@/api/exam/paper"
import { getQuestionList } from "@/api/exam/question"
import { getCategoryTree } from "@/api/exam/questionCategory"
import { getToken } from "@/utils/auth"
import { Splitpanes, Pane } from "splitpanes"
import "splitpanes/dist/splitpanes.css"

export default {
  name: "SectionManagement",
  components: { Splitpanes, Pane },
  dicts: ['question_type', 'subject'],
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
      sectionList: [],
      volumeList: [],
      selectedVolumeId: undefined, // 当前选中的卷别ID
      selectedIds: [],
      dialogVisible: false,
      dialogTitle: "新增大题",
      // 题目关联相关
      questionManageVisible: false,
      currentSection: null,
      sectionQuestions: [],
      // 题目选择相关
      questionSelectVisible: false,
      questionSelectCategoryTree: [],
      questionSelectCategoryName: undefined,
      questionSelectCategoryId: undefined,
      questionSelectList: [],
      questionSelectLoading: false,
      questionSelectTotal: 0,
      questionSelectQueryParams: {
        pageNum: 1,
        pageSize: 10,
        title: undefined,
        categoryId: undefined
      },
      questionSelectSelectedIds: [],
      questionSelectSelectedRows: [],
      defaultProps: {
        children: "children",
        label: "name"
      },
      sectionForm: {
        id: undefined,
        paperId: undefined,
        volumeId: undefined,
        volumeCode: undefined,
        sectionName: undefined,
        sectionOrder: 1,
        questionCount: 0,
        totalScore: 0,
        scorePerQuestion: 0,
        instructionText: undefined,
        answerTime: 5,
        audioPlayCount: 1
      },
      sectionRules: {
        volumeCode: [
          { required: true, message: "卷别不能为空", trigger: "change" }
        ],
        sectionName: [
          { required: true, message: "大题名称不能为空", trigger: "blur" },
          { max: 50, message: "大题名称长度不能超过50个字符", trigger: "blur" }
        ],
        sectionOrder: [
          { required: true, message: "排序不能为空", trigger: "blur" }
        ]
      },
      uploadMediaUrl: process.env.VUE_APP_BASE_API + "/question/media/upload",
      uploadHeaders: {
        Authorization: "Bearer " + getToken()
      },
      sectionAudioMap: {} // 存储每个大题的音频信息 { sectionId: { url, path, mediaId } }
    }
  },
  computed: {
    /** 根据选中的卷别筛选大题列表 */
    filteredSectionList() {
      if (!this.selectedVolumeId) {
        return []
      }
      return this.sectionList.filter(section => section.volumeId === this.selectedVolumeId)
    }
  },
  watch: {
    paperId: {
      immediate: true, // 组件创建时立即执行一次
      handler(newVal, oldVal) {
        // 只在 paperId 有值时触发（immediate: true 时 oldVal 为 undefined）
        if (newVal) {
          // 延迟执行，确保组件完全初始化
          this.$nextTick(() => {
            // 不在这里加载卷别列表，改为按需加载（用户点击下拉框时）
            // 只加载大题列表
            this.loadSectionList()
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
      getPaperVolumeList({ id: this.paperId }).then(response => {
        if (response.code === 200) {
          this.volumeList = response.data || []
        }
        this.loadingVolumeList = false
      }).catch(() => {
        this.loadingVolumeList = false
        // 忽略错误
      })
    },
    /** 加载大题列表 */
    loadSectionList() {
      if (!this.paperId) return
      this.loading = true
      getPaperSectionList({ id: this.paperId }).then(response => {
        if (response.code === 200) {
          this.sectionList = response.data || []
          // 加载每个大题的音频信息
          this.sectionList.forEach(section => {
            this.loadSectionAudio(section.id)
          })
        } else {
          this.$modal.msgError(response.msg || "加载大题列表失败")
        }
        this.loading = false
      }).catch(() => {
        this.loading = false
        this.$modal.msgError("加载大题列表失败")
      })
    },
    /** 加载大题音频 */
    loadSectionAudio(sectionId) {
      getQuestionMediaBySectionId({ sectionId }).then(response => {
        if (response.code === 200 && response.data && response.data.length > 0) {
          // media_type=8 表示大题说明音频
          const audioMedia = response.data.find(m => m.mediaType === 8)
          if (audioMedia) {
            this.$set(this.sectionAudioMap, sectionId, {
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
    /** 获取大题音频文件列表 */
    getSectionAudioFileList(row) {
      const audio = this.sectionAudioMap[row.id]
      if (audio && audio.url) {
        return [{
          name: audio.url.split('/').pop(),
          url: audio.url
        }]
      }
      return []
    },
    /** 大题音频上传前校验 */
    handleSectionAudioBeforeUpload(file, row) {
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
    /** 大题音频上传成功 */
    handleSectionAudioSuccess(response, file, row) {
      if (response.code === 200) {
        const mediaBO = {
          paperId: this.paperId,
          sectionId: row.id,
          mediaType: 8, // 大题说明音频
          mediaName: file.name,
          mediaPath: response.data.path || response.data.url,
          mediaUrl: response.data.url,
          mediaSize: file.size,
          mediaFormat: file.name.split('.').pop(),
          storageType: 0 // 在线路径
        }
        saveQuestionMedia(mediaBO).then(saveResponse => {
          if (saveResponse.code === 200) {
            this.$set(this.sectionAudioMap, row.id, {
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
    /** 大题音频移除 */
    handleSectionAudioRemove(row) {
      const audio = this.sectionAudioMap[row.id]
      if (audio && audio.mediaId) {
        removeQuestionMedia({ id: audio.mediaId }).then(response => {
          if (response.code === 200) {
            this.$set(this.sectionAudioMap, row.id, null)
            this.$modal.msgSuccess("删除成功")
          } else {
            this.$modal.msgError(response.msg || "删除失败")
          }
        }).catch(() => {
          this.$modal.msgError("删除失败")
        })
      } else {
        this.$set(this.sectionAudioMap, row.id, null)
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
    /** 卷别代码变化时，同步设置 volumeId */
    handleVolumeCodeChange(volumeCode) {
      if (volumeCode && this.volumeList.length > 0) {
        const volume = this.volumeList.find(v => v.volumeCode === volumeCode)
        if (volume) {
          this.sectionForm.volumeId = volume.id
        }
      }
    },
    /** 卷别下拉框获得焦点时，按需加载卷别列表 */
    handleVolumeSelectFocus() {
      // 如果卷别列表为空且未在加载中，则加载
      if (this.volumeList.length === 0 && !this.loadingVolumeList && this.paperId) {
        this.loadVolumeList()
      }
    },
    /** 卷别筛选变化 */
    handleVolumeFilterChange(volumeId) {
      // 大题列表已经通过computed过滤，无需重新加载
    },
    /** 新增 */
    handleAdd() {
      if (!this.selectedVolumeId) {
        this.$modal.msgWarning("请先选择卷别")
        return
      }
      this.dialogTitle = "新增大题"
      const selectedVolume = this.volumeList.find(v => v.id === this.selectedVolumeId)
      this.sectionForm = {
        id: undefined,
        paperId: this.paperId,
        volumeId: this.selectedVolumeId,
        volumeCode: selectedVolume ? selectedVolume.volumeCode : undefined,
        sectionName: undefined,
        sectionOrder: (this.filteredSectionList.length + 1),
        questionCount: 0,
        totalScore: 0,
        scorePerQuestion: 0,
        instructionText: undefined,
        answerTime: 5,
        audioPlayCount: 1
      }
      this.dialogVisible = true
      this.$nextTick(() => {
        this.$refs.sectionForm && this.$refs.sectionForm.clearValidate()
      })
    },
    /** 编辑 */
    handleEdit(row) {
      this.dialogTitle = "编辑大题"
      this.sectionForm = {
        id: row.id,
        paperId: row.paperId,
        volumeId: row.volumeId,
        volumeCode: row.volumeCode,
        sectionName: row.sectionName,
        sectionOrder: row.sectionOrder,
        questionCount: row.questionCount || 0,
        totalScore: row.totalScore || 0,
        scorePerQuestion: row.scorePerQuestion || 0,
        instructionText: row.instructionText,
        answerTime: row.answerTime || 5,
        audioPlayCount: row.audioPlayCount || 1
      }
      this.dialogVisible = true
      this.$nextTick(() => {
        this.$refs.sectionForm && this.$refs.sectionForm.clearValidate()
      })
    },
    /** 删除 */
    handleDelete(row) {
      this.$modal.confirm('确定要删除大题"' + row.sectionName + '"吗？').then(() => {
        deletePaperSection({ ids: [row.id] }).then(response => {
          if (response.code === 200) {
            this.$modal.msgSuccess("删除成功")
            this.loadSectionList()
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
        this.$modal.msgWarning("请选择要删除的大题")
        return
      }
      this.$modal.confirm('确定要删除选中的 ' + this.selectedIds.length + ' 个大题吗？').then(() => {
        deletePaperSection({ ids: this.selectedIds }).then(response => {
          if (response.code === 200) {
            this.$modal.msgSuccess("删除成功")
            this.loadSectionList()
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
      this.$refs.sectionForm.validate(valid => {
        if (valid) {
          if (this.sectionForm.id) {
            // 编辑
            updatePaperSection(this.sectionForm).then(response => {
              if (response.code === 200) {
                this.$modal.msgSuccess("修改成功")
                this.dialogVisible = false
                this.loadSectionList()
                this.$emit('refresh')
              } else {
                this.$modal.msgError(response.msg || "修改失败")
              }
            }).catch(() => {
              this.$modal.msgError("修改失败")
            })
          } else {
            // 新增
            addPaperSection(this.sectionForm).then(response => {
              if (response.code === 200) {
                this.$modal.msgSuccess("新增成功")
                this.dialogVisible = false
                this.loadSectionList()
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
    },
    /** 管理大题题目 */
    handleManageQuestions(row) {
      this.currentSection = row
      this.loadSectionQuestions(row.id)
      this.questionManageVisible = true
    },
    /** 加载大题已关联的题目 */
    loadSectionQuestions(sectionId) {
      if (!this.paperId || !sectionId) return
      getPaperQuestionList({ paperId: this.paperId }).then(response => {
        if (response.code === 200 && response.data) {
          // 筛选出属于该大题的题目
          this.sectionQuestions = response.data
            .filter(pq => pq.sectionId === sectionId)
            .map((pq, index) => ({
              questionId: pq.questionId,
              id: pq.questionId,
              title: pq.title || '暂无标题',
              type: pq.type,
              subjectId: pq.subjectId,
              score: parseFloat(pq.score) || 0,
              sectionOrder: pq.sectionOrder || (index + 1)
            }))
            .sort((a, b) => (a.sectionOrder || 0) - (b.sectionOrder || 0))
        } else {
          this.sectionQuestions = []
        }
      }).catch(() => {
        this.sectionQuestions = []
      })
    },
    /** 选择题库题目 */
    handleSelectQuestions() {
      this.questionSelectVisible = true
      this.questionSelectSelectedIds = this.sectionQuestions.map(q => q.questionId)
      if (!this.questionSelectCategoryTree.length) {
        this.loadQuestionSelectCategoryTree()
      }
      this.getQuestionSelectList()
    },
    /** 加载题目选择分类树 */
    loadQuestionSelectCategoryTree() {
      getCategoryTree({}).then(response => {
        this.questionSelectCategoryTree = this.convertToTreeData(response.data || [])
        this.$nextTick(() => {
          const firstNonRootNode = this.findFirstNonRootNode(this.questionSelectCategoryTree)
          if (firstNonRootNode) {
            this.questionSelectCategoryId = firstNonRootNode.id
            this.questionSelectQueryParams.categoryId = firstNonRootNode.id
            if (this.$refs.questionSelectCategoryTree) {
              this.$refs.questionSelectCategoryTree.setCurrentKey(firstNonRootNode.id)
            }
          }
        })
      })
    },
    /** 题目选择分类树点击 */
    handleQuestionSelectCategoryClick(data) {
      if (data.isDefault === true || data.isDefault === 1) {
        this.questionSelectCategoryId = undefined
        this.questionSelectQueryParams.categoryId = undefined
        return
      }
      this.questionSelectCategoryId = data.id
      this.questionSelectQueryParams.categoryId = data.id
      this.handleQuestionSelectQuery()
    },
    /** 题目选择搜索 */
    handleQuestionSelectQuery() {
      this.questionSelectQueryParams.pageNum = 1
      this.getQuestionSelectList()
    },
    /** 重置题目选择搜索 */
    resetQuestionSelectQuery() {
      this.resetForm("questionSelectQueryForm")
      this.handleQuestionSelectQuery()
    },
    /** 获取题目选择列表 */
    getQuestionSelectList() {
      this.questionSelectLoading = true
      getQuestionList(this.questionSelectQueryParams).then(response => {
        this.questionSelectList = response.rows || []
        this.questionSelectTotal = response.total || 0
        this.questionSelectLoading = false
        // 恢复已选择的项
        this.$nextTick(() => {
          this.questionSelectList.forEach(row => {
            if (this.questionSelectSelectedIds.includes(row.id)) {
              this.$refs.questionSelectTable.toggleRowSelection(row, true)
            }
          })
        })
      }).catch(() => {
        this.questionSelectLoading = false
        this.questionSelectList = []
        this.questionSelectTotal = 0
      })
    },
    /** 题目选择表格选中变化 */
    handleQuestionSelectSelectionChange(selection) {
      this.questionSelectSelectedRows = selection
      this.questionSelectSelectedIds = selection.map(item => item.id)
    },
    /** 题目选择确认 */
    handleQuestionSelectConfirm() {
      // 合并新选择的题目到已选列表（避免重复）
      const existingIds = this.sectionQuestions.map(q => q.questionId)
      const newQuestions = this.questionSelectSelectedRows
        .filter(row => !existingIds.includes(row.id))
        .map((row, index) => ({
          questionId: row.id,
          id: row.id,
          title: row.title,
          type: row.type,
          subjectId: row.subjectId,
          score: this.calculateDefaultScore(row),
          sectionOrder: this.sectionQuestions.length + index + 1
        }))

      // 更新已存在的题目信息
      this.questionSelectSelectedRows.forEach(row => {
        const existingIndex = this.sectionQuestions.findIndex(q => q.questionId === row.id)
        if (existingIndex >= 0) {
          this.$set(this.sectionQuestions[existingIndex], 'title', row.title)
          this.$set(this.sectionQuestions[existingIndex], 'type', row.type)
          this.$set(this.sectionQuestions[existingIndex], 'subjectId', row.subjectId)
        }
      })

      // 添加新题目
      this.sectionQuestions.push(...newQuestions)

      this.questionSelectVisible = false
    },
    /** 计算默认分值 */
    calculateDefaultScore(question) {
      // 从字典表读取题目类型默认分值
      const dictData = this.dict.type.question_type_default_score || []
      const item = dictData.find(d => {
        const [type] = d.value.split(',')
        return parseInt(type) === question.type
      })
      if (item) {
        const [, score] = item.value.split(',')
        return parseFloat(score) || 1.0
      }
      const defaultScoreMap = {
        0: 2.0, 1: 5.0, 2: 1.0, 3: 3.0, 4: 5.0, 5: 10.0
      }
      return defaultScoreMap[question.type] || 1.0
    },
    /** 移除大题题目 */
    handleRemoveSectionQuestion(index) {
      this.sectionQuestions.splice(index, 1)
      // 重新排序
      this.sectionQuestions.forEach((q, i) => {
        q.sectionOrder = i + 1
      })
    },
    /** 清空全部大题题目 */
    handleClearAllSectionQuestions() {
      this.$modal.confirm('是否确认清空该大题下的所有题目？').then(() => {
        this.sectionQuestions = []
      }).catch(() => {})
    },
    /** 保存大题题目关联 */
    handleSaveSectionQuestions() {
      if (!this.currentSection || !this.paperId) return

      // 构建题目关联数据
      const questionList = this.sectionQuestions.map((q, index) => ({
        paperId: this.paperId,
        questionId: q.questionId,
        sectionId: this.currentSection.id,
        sectionOrder: index + 1,
        score: q.score
      }))

      // 调用批量保存接口
      batchSavePaperQuestion({
        paperId: this.paperId,
        sectionId: this.currentSection.id,
        questionList: questionList
      }).then(response => {
        if (response.code === 200) {
          this.$modal.msgSuccess("保存成功")
          this.questionManageVisible = false
          // 刷新大题列表（更新题目数量）
          this.loadSectionList()
          this.$emit('refresh')
        } else {
          this.$modal.msgError(response.msg || "保存失败")
        }
      }).catch(() => {
        this.$modal.msgError("保存失败")
      })
    },
    /** 转换为树形数据 */
    convertToTreeData(data) {
      return data.map(item => ({
        ...item,
        children: item.children && item.children.length > 0 ? this.convertToTreeData(item.children) : undefined
      }))
    },
    /** 查找第一个非根节点 */
    findFirstNonRootNode(treeData) {
      for (const node of treeData) {
        if (node.isDefault !== true && node.isDefault !== 1) {
          return node
        }
        if (node.children && node.children.length > 0) {
          const found = this.findFirstNonRootNode(node.children)
          if (found) return found
        }
      }
      return null
    },
    /** 过滤节点 */
    filterNode(value, data) {
      if (!value) return true
      return data.name.indexOf(value) !== -1
    }
  },
  watch: {
    questionSelectCategoryName(val) {
      this.$refs.questionSelectCategoryTree && this.$refs.questionSelectCategoryTree.filter(val)
    }
  }
}
</script>

<style scoped>
.section-management {
  padding: 20px;
}

.filter-bar {
  margin-bottom: 20px;
}

.header-actions {
  margin-bottom: 20px;
}

.header-actions .el-button {
  margin-right: 10px;
}

.category-tree-container {
  padding: 10px;
  height: 100%;
  overflow-y: auto;
}

.question-list-container {
  padding: 10px;
}
</style>

