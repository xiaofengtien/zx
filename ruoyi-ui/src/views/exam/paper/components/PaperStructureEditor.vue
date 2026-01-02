<template>
  <div class="paper-structure-editor">
    <splitpanes :horizontal="false" class="default-theme">
      <!-- 左侧：树形结构 -->
      <pane :size="30" :min-size="20" :max-size="50">
        <div class="tree-container">
          <div class="tree-header">
            <span class="tree-title">试卷结构</span>
            <el-button
              type="text"
              icon="el-icon-refresh"
              size="small"
              @click="refreshTree"
              title="刷新"
            />
          </div>
          <el-tree
            ref="structureTree"
            :data="treeData"
            :props="treeProps"
            node-key="id"
            :default-expand-all="true"
            :highlight-current="true"
            :expand-on-click-node="false"
            @node-click="handleNodeClick"
            class="structure-tree"
          >
            <span class="custom-tree-node" slot-scope="{ node, data }">
              <span class="tree-node-label">
                <i :class="getNodeIcon(data.type)" class="node-icon"></i>
                <span>{{ node.label }}</span>
                <span v-if="data.type === 'section'" class="question-count">
                  ({{ data.questionCount || 0 }}题)
                </span>
                <span v-if="data.type === 'question'" class="question-score">
                  {{ data.score ? `(${data.score}分)` : '' }}
                </span>
              </span>
              <span class="tree-node-actions">
                <el-tooltip
                  v-if="data.type === 'paper'"
                  content="新增卷别（如：A卷、B卷）"
                  placement="top"
                >
                  <el-button
                    type="text"
                    icon="el-icon-plus"
                    size="mini"
                    @click.stop="handleAddVolume"
                  />
                </el-tooltip>
                <el-tooltip
                  v-if="data.type === 'volume'"
                  content="在该卷别下新增大题（如：第一节、第二节）"
                  placement="top"
                >
                  <el-button
                    type="text"
                    icon="el-icon-plus"
                    size="mini"
                    @click.stop="handleAddSection(data)"
                  />
                </el-tooltip>
                <el-tooltip
                  v-if="data.type === 'section'"
                  content="为大题添加题目"
                  placement="top"
                >
                  <el-button
                    type="text"
                    icon="el-icon-plus"
                    size="mini"
                    @click.stop="handleAddQuestion(data)"
                  />
                </el-tooltip>
                <el-tooltip
                  v-if="data.type === 'volume' || data.type === 'section'"
                  :content="data.type === 'volume' ? '编辑卷别信息' : '编辑大题信息'"
                  placement="top"
                >
                  <el-button
                    type="text"
                    icon="el-icon-edit"
                    size="mini"
                    @click.stop="handleEdit(data)"
                  />
                </el-tooltip>
                <el-tooltip
                  v-if="data.type === 'volume' || data.type === 'section'"
                  :content="data.type === 'volume' ? '删除卷别（将同时删除该卷别下的所有大题和题目）' : '删除大题（将同时删除该大题下的所有题目）'"
                  placement="top"
                >
                  <el-button
                    type="text"
                    icon="el-icon-delete"
                    size="mini"
                    style="color: #f56c6c;"
                    @click.stop="handleDelete(data)"
                  />
                </el-tooltip>
              </span>
            </span>
          </el-tree>
        </div>
      </pane>

      <!-- 右侧：编辑区域 -->
      <pane :size="70" :min-size="50">
        <div class="edit-container">
          <!-- 卷别编辑 -->
          <div v-if="currentNode && currentNode.type === 'volume'" class="edit-panel">
            <div class="edit-header">
              <h3>编辑卷别：{{ currentNode.label }}</h3>
            </div>
            <el-form ref="volumeForm" :model="volumeForm" :rules="volumeRules" label-width="120px">
              <el-form-item label="卷别名称" prop="volumeName">
                <el-input v-model="volumeForm.volumeName" placeholder="如：A卷、B卷" maxlength="50" />
              </el-form-item>
              <el-form-item label="排序" prop="volumeOrder">
                <el-input-number v-model="volumeForm.volumeOrder" :min="1" :max="999" controls-position="right" style="width: 200px" />
              </el-form-item>
              <el-form-item label="卷别名称音频">
                <el-upload
                  :action="uploadMediaUrl"
                  :headers="uploadHeaders"
                  :before-upload="handleVolumeAudioBeforeUpload"
                  :on-success="handleVolumeAudioSuccess"
                  :on-error="handleMediaUploadError"
                  :on-remove="handleVolumeAudioRemove"
                  :limit="1"
                  :file-list="volumeAudioFileList"
                >
                  <el-button size="small" type="primary">上传音频</el-button>
                  <div slot="tip" class="el-upload__tip">只能上传音频文件，且不超过10MB，仅支持一个文件，二次上传将直接替换</div>
                </el-upload>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="handleVolumeSubmit">保存</el-button>
                <el-button @click="handleCancel">取消</el-button>
              </el-form-item>
            </el-form>
          </div>

          <!-- 大题编辑 + 题目列表 -->
          <div v-else-if="currentNode && currentNode.type === 'section'" class="edit-panel">
            <div class="edit-header">
              <h3>编辑大题：{{ currentNode.label }}</h3>
            </div>
            <el-tabs v-model="sectionEditTab" type="border-card">
              <el-tab-pane label="大题信息" name="info">
                <el-form ref="sectionForm" :model="sectionForm" :rules="sectionRules" label-width="120px">
                  <el-form-item label="大题名称" prop="sectionName">
                    <el-input v-model="sectionForm.sectionName" placeholder="如：第一节、第二节" maxlength="50" />
                  </el-form-item>
                  <el-form-item label="排序" prop="sectionOrder">
                    <el-input-number v-model="sectionForm.sectionOrder" :min="1" :max="999" controls-position="right" style="width: 200px" />
                  </el-form-item>
                  <el-form-item label="题目数量" prop="questionCount">
                    <el-input-number
                      v-model="sectionForm.questionCount"
                      :min="0"
                      :max="999"
                      controls-position="right"
                      style="width: 200px"
                      :disabled="true"
                    />
                    <span style="margin-left: 10px; color: #909399; font-size: 12px;">
                      （自动计算：当前已关联 {{ sectionQuestions.length }} 题）
                    </span>
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
                  <el-form-item label="大题说明音频">
                    <el-upload
                      :action="uploadMediaUrl"
                      :headers="uploadHeaders"
                      :before-upload="handleSectionAudioBeforeUpload"
                      :on-success="handleSectionAudioSuccess"
                      :on-error="handleMediaUploadError"
                      :on-remove="handleSectionAudioRemove"
                      :limit="1"
                      :file-list="sectionAudioFileList"
                    >
                      <el-button size="small" type="primary">上传音频</el-button>
                      <div slot="tip" class="el-upload__tip">只能上传音频文件，且不超过10MB，仅支持一个文件，二次上传将直接替换</div>
                    </el-upload>
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
                  <el-form-item>
                    <el-button type="primary" @click="handleSectionSubmit">保存</el-button>
                    <el-button @click="handleCancel">取消</el-button>
                  </el-form-item>
                </el-form>
              </el-tab-pane>
              <el-tab-pane label="题目列表" name="questions">
                <div class="question-list-panel">
                  <div class="question-list-header">
                    <span>已关联 {{ sectionQuestions.length }} 题</span>
                    <div>
                      <el-button type="primary" size="small" icon="el-icon-plus" @click="handleSelectQuestions">选择题库题目</el-button>
                      <el-button
                        v-if="sectionQuestions.length > 0"
                        type="danger"
                        size="small"
                        icon="el-icon-delete"
                        @click="handleClearAllQuestions"
                      >
                        清空全部
                      </el-button>
                    </div>
                  </div>
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
                          @click="handleRemoveQuestion(scope.$index)"
                        >删除</el-button>
                      </template>
                    </el-table-column>
                  </el-table>
                  <el-empty v-else description="暂未关联题目" :image-size="100" />
                  <div v-if="sectionQuestions.length > 0" class="question-list-footer">
                    <el-button type="primary" @click="handleSaveQuestions">保存题目</el-button>
                  </div>
                </div>
              </el-tab-pane>
            </el-tabs>
          </div>

          <!-- 空状态 -->
          <div v-else class="empty-state">
            <i class="el-icon-info" style="font-size: 48px; color: #909399;"></i>
            <p>请从左侧选择卷别或大题进行编辑</p>
          </div>
        </div>
      </pane>
    </splitpanes>

    <!-- 题目选择弹窗 -->
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
import {
  getPaperVolumeList, addPaperVolume, updatePaperVolume, deletePaperVolume,
  getPaperSectionList, addPaperSection, updatePaperSection, deletePaperSection,
  getPaperQuestionList, batchSavePaperQuestion
} from "@/api/exam/paper"
import { getQuestionMediaByVolumeId, getQuestionMediaBySectionId, saveQuestionMedia, removeQuestionMedia } from "@/api/exam/paper"
import { getQuestionList } from "@/api/exam/question"
import { getCategoryTree } from "@/api/exam/questionCategory"
import { getToken } from "@/utils/auth"
import { Splitpanes, Pane } from "splitpanes"
import "splitpanes/dist/splitpanes.css"

export default {
  name: "PaperStructureEditor",
  components: { Splitpanes, Pane },
  dicts: ['question_type', 'subject'],
  props: {
    paperId: {
      type: Number,
      required: true
    },
    paperName: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      // 树形结构数据
      treeData: [],
      treeProps: {
        children: 'children',
        label: 'label'
      },
      currentNode: null, // 当前选中的节点

      // 卷别编辑表单
      volumeForm: {
        id: undefined,
        paperId: undefined,
        volumeName: undefined,
        volumeOrder: 1
      },
      volumeRules: {
        volumeName: [
          { required: true, message: "卷别名称不能为空", trigger: "blur" },
          { max: 50, message: "卷别名称长度不能超过50个字符", trigger: "blur" }
        ]
      },
      volumeAudioFileList: [],
      volumeAudioMap: {},

      // 大题编辑表单
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
        sectionName: [
          { required: true, message: "大题名称不能为空", trigger: "blur" },
          { max: 50, message: "大题名称长度不能超过50个字符", trigger: "blur" }
        ]
      },
      sectionEditTab: 'info',
      sectionQuestions: [],
      sectionAudioFileList: [],
      sectionAudioMap: {},

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

      // 上传相关
      uploadMediaUrl: process.env.VUE_APP_BASE_API + "/question/media/upload",
      uploadHeaders: {
        Authorization: "Bearer " + getToken()
      },

      // 数据缓存
      volumeList: [],
      sectionList: [],
      questionList: [],

      // 加载状态
      loadingData: false
    }
  },
  mounted() {
    // 组件挂载时，如果 paperId 已有值，立即加载数据
    if (this.paperId) {
      this.loadAllData()
    }
  },
  watch: {
    paperId: {
      handler(newVal, oldVal) {
        // 只在值变化且不为空时加载（mounted 会处理初始加载）
        if (newVal && newVal !== oldVal) {
          this.loadAllData()
        }
      }
    },
    // 监听题目列表变化，自动更新题目数量
    sectionQuestions: {
      handler(newVal) {
        if (this.sectionForm && this.sectionForm.id) {
          this.sectionForm.questionCount = newVal.length
        }
      },
      deep: true
    }
  },
  methods: {
    /** 加载所有数据并构建树 */
    async loadAllData() {
      if (!this.paperId) return

      // 防止重复加载
      if (this.loadingData) return
      this.loadingData = true

      try {
        // 并行加载卷别、大题、题目列表
        const [volumeRes, sectionRes, questionRes] = await Promise.all([
          getPaperVolumeList({ id: this.paperId }),
          getPaperSectionList({ id: this.paperId }),
          getPaperQuestionList({ paperId: this.paperId })
        ])

        this.volumeList = volumeRes.code === 200 ? (volumeRes.data || []) : []
        this.sectionList = sectionRes.code === 200 ? (sectionRes.data || []) : []
        this.questionList = questionRes.code === 200 ? (questionRes.data || []) : []

        // 构建树形结构
        this.buildTree()
      } catch (error) {
        console.error('加载数据失败:', error)
      } finally {
        this.loadingData = false
      }
    },

    /** 构建树形结构 */
    buildTree() {
      const tree = [{
        id: `paper-${this.paperId}`,
        type: 'paper',
        label: '试卷结构',
        children: []
      }]

      // 按 volumeOrder 排序卷别
      const sortedVolumes = [...this.volumeList].sort((a, b) => (a.volumeOrder || 0) - (b.volumeOrder || 0))

      sortedVolumes.forEach(volume => {
        const volumeNode = {
          id: `volume-${volume.id}`,
          type: 'volume',
          label: volume.volumeName || (volume.volumeCode ? `${volume.volumeCode} - ${volume.volumeName}` : '未命名卷别'),
          volumeId: volume.id,
          volumeCode: volume.volumeCode,
          volumeName: volume.volumeName,
          volumeOrder: volume.volumeOrder,
          children: []
        }

        // 获取该卷别下的大题（按 sectionOrder 排序）
        const volumeSections = this.sectionList
          .filter(section => section.volumeId === volume.id)
          .sort((a, b) => (a.sectionOrder || 0) - (b.sectionOrder || 0))

        volumeSections.forEach(section => {
          const sectionNode = {
            id: `section-${section.id}`,
            type: 'section',
            label: section.sectionName,
            sectionId: section.id,
            sectionName: section.sectionName,
            sectionOrder: section.sectionOrder,
            questionCount: section.questionCount || 0,
            totalScore: section.totalScore,
            scorePerQuestion: section.scorePerQuestion,
            instructionText: section.instructionText,
            volumeId: section.volumeId,
            volumeCode: section.volumeCode,
            children: []
          }

          // 获取该大题下的题目（按 sectionOrder 排序）
          const sectionQuestions = this.questionList
            .filter(q => q.sectionId === section.id)
            .sort((a, b) => (a.sectionOrder || 0) - (b.sectionOrder || 0))

          sectionQuestions.forEach((question, index) => {
            sectionNode.children.push({
              id: `question-${question.questionId}`,
              type: 'question',
              label: question.title || `题目${index + 1}`,
              questionId: question.questionId,
              title: question.title,
              type: question.type,
              subjectId: question.subjectId,
              score: question.score,
              sectionOrder: question.sectionOrder || (index + 1)
            })
          })

          volumeNode.children.push(sectionNode)
        })

        tree[0].children.push(volumeNode)
      })

      this.treeData = tree

      // 加载音频信息
      this.loadAudioInfo()
    },

    async loadAudioInfo() {
      console.log('PaperStructureEditor: loadAudioInfo called')
      // 加载卷别音频
      for (const volume of this.volumeList) {
        let hasAudio = false
        // 1. 优先从 volume 对象中获取
        if (volume.volumeAudioUrl) {
           console.log(`Editor: Volume ${volume.id} has audio in entity:`, volume.volumeAudioUrl)
           this.$set(this.volumeAudioMap, volume.id, {
             url: volume.volumeAudioUrl,
             path: volume.volumeAudioPath || volume.volumeAudioUrl,
             duration: volume.volumeAudioDuration || null
           })
           hasAudio = true
        }

        // 2. 兜底：从媒体表获取
        if (!hasAudio) {
           try {
             const res = await getQuestionMediaByVolumeId({ volumeId: volume.id })
             if (res.code === 200 && res.data && res.data.length > 0) {
               const audioMedia = res.data.find(m => m.mediaType === 7)
               if (audioMedia) {
                 console.log(`Editor: Volume ${volume.id} found audio in media table:`, audioMedia.mediaUrl)
                 this.$set(this.volumeAudioMap, volume.id, {
                   url: audioMedia.mediaUrl,
                   path: audioMedia.mediaPath,
                   mediaId: audioMedia.id
                 })
               }
             }
           } catch (e) {
             console.error(`Editor: Failed to fetch media for volume ${volume.id}`, e)
           }
        }
      }

      // 加载大题音频
      for (const section of this.sectionList) {
        if (section.instructionAudioUrl) {
             this.$set(this.sectionAudioMap, section.id, {
                url: section.instructionAudioUrl,
                path: section.instructionAudioPath,
                duration: section.instructionAudioDuration || null
             })
        } else {
             try {
                const res = await getQuestionMediaBySectionId({ sectionId: section.id })
                if (res.code === 200 && res.data && res.data.length > 0) {
                   const audioMedia = res.data.find(m => m.mediaType === 8)
                   if (audioMedia) {
                      this.$set(this.sectionAudioMap, section.id, {
                        url: audioMedia.mediaUrl,
                        path: audioMedia.mediaPath,
                        mediaId: audioMedia.id
                      })
                   }
                }
             } catch (e) {}
        }
      }
    },

    /** 获取节点图标 */
    getNodeIcon(type) {
      const iconMap = {
        'paper': 'el-icon-document',
        'volume': 'el-icon-folder-opened',
        'section': 'el-icon-tickets',
        'question': 'el-icon-question'
      }
      return iconMap[type] || 'el-icon-document'
    },

    /** 节点点击 */
    handleNodeClick(data) {
      this.currentNode = data

      if (data.type === 'volume') {
        this.loadVolumeForm(data)
      } else if (data.type === 'section') {
        this.loadSectionForm(data)
        this.loadSectionQuestions(data.sectionId)
      }
    },

    /** 加载卷别表单 */
    loadVolumeForm(data) {
      const volume = this.volumeList.find(v => v.id === data.volumeId)
      if (volume) {
        this.volumeForm = {
          id: volume.id,
          paperId: this.paperId,
          volumeName: volume.volumeName,
          volumeOrder: volume.volumeOrder
        }

        // 加载音频文件列表
        const audio = this.volumeAudioMap[volume.id]
        if (audio && audio.url) {
          this.volumeAudioFileList = [{
            name: audio.url.split('/').pop(),
            url: audio.url
          }]
        } else {
          this.volumeAudioFileList = []
        }
      }
    },

    /** 加载大题表单 */
    loadSectionForm(data) {
      const section = this.sectionList.find(s => s.id === data.sectionId)
      if (section) {
        // 自动计算题目数量（从已关联的题目列表获取）
        const sectionQuestions = this.questionList.filter(q => q.sectionId === section.id)
        const questionCount = sectionQuestions.length

        this.sectionForm = {
          id: section.id,
          paperId: this.paperId,
          volumeId: section.volumeId,
          volumeCode: section.volumeCode,
          sectionName: section.sectionName,
          sectionOrder: section.sectionOrder,
          questionCount: questionCount,
          totalScore: section.totalScore || 0,
          scorePerQuestion: section.scorePerQuestion || 0,
          instructionText: section.instructionText,
          answerTime: section.answerTime || 5,
          audioPlayCount: section.audioPlayCount || 1
        }

        // 加载音频文件列表
        const audio = this.sectionAudioMap[section.id]
        if (audio && audio.url) {
          this.sectionAudioFileList = [{
            name: audio.url.split('/').pop(),
            url: audio.url
          }]
        } else {
          this.sectionAudioFileList = []
        }
      }
    },

    /** 加载大题题目列表 */
    loadSectionQuestions(sectionId) {
      this.sectionQuestions = this.questionList
        .filter(q => q.sectionId === sectionId)
        .map((q, index) => ({
          questionId: q.questionId,
          id: q.questionId,
          title: q.title || '暂无标题',
          type: q.type,
          subjectId: q.subjectId,
          score: parseFloat(q.score) || 0,
          sectionOrder: q.sectionOrder || (index + 1)
        }))
        .sort((a, b) => (a.sectionOrder || 0) - (b.sectionOrder || 0))
    },

    /** 新增卷别 */
    handleAddVolume() {
      this.$prompt('请输入卷别名称', '新增卷别', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputPattern: /.+/,
        inputErrorMessage: '卷别名称不能为空'
      }).then(({ value }) => {
        const newVolume = {
          paperId: this.paperId,
          volumeName: value.trim(),
          volumeOrder: (this.volumeList.length + 1)
        }

        addPaperVolume(newVolume).then(response => {
          if (response.code === 200) {
            this.$modal.msgSuccess("新增成功")
            this.loadAllData()
            // 选中新创建的卷别
            this.$nextTick(() => {
              const newVolumeId = response.data
              const nodeId = `volume-${newVolumeId}`
              this.$refs.structureTree.setCurrentKey(nodeId)
              const node = this.$refs.structureTree.getNode(nodeId)
              if (node) {
                this.handleNodeClick(node.data)
              }
            })
          } else {
            this.$modal.msgError(response.msg || "新增失败")
          }
        }).catch(() => {
          this.$modal.msgError("新增失败")
        })
      }).catch(() => {})
    },

    /** 新增大题 */
    handleAddSection(volumeNode) {
      if (!volumeNode.volumeId) return

      this.$prompt('请输入大题名称', '新增大题', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputPattern: /.+/,
        inputErrorMessage: '大题名称不能为空'
      }).then(({ value }) => {
        const newSection = {
          paperId: this.paperId,
          volumeId: volumeNode.volumeId,
          volumeCode: volumeNode.volumeCode,
          sectionName: value,
          sectionOrder: (this.sectionList.filter(s => s.volumeId === volumeNode.volumeId).length + 1),
          questionCount: 0,
          totalScore: 0,
          scorePerQuestion: 0,
          instructionText: undefined,
          answerTime: 5,
          audioPlayCount: 1
        }

        addPaperSection(newSection).then(response => {
          if (response.code === 200) {
            this.$modal.msgSuccess("新增成功")
            this.loadAllData()
            // 选中新创建的大题
            this.$nextTick(() => {
              const newSectionId = response.data
              const nodeId = `section-${newSectionId}`
              this.$refs.structureTree.setCurrentKey(nodeId)
              const node = this.$refs.structureTree.getNode(nodeId)
              if (node) {
                this.handleNodeClick(node.data)
              }
            })
          } else {
            this.$modal.msgError(response.msg || "新增失败")
          }
        }).catch(() => {
          this.$modal.msgError("新增失败")
        })
      }).catch(() => {})
    },

    /** 添加题目 */
    handleAddQuestion(sectionNode) {
      if (!sectionNode.sectionId) return
      this.currentNode = sectionNode
      this.sectionEditTab = 'questions'
      this.handleSelectQuestions()
    },

    /** 编辑节点 */
    handleEdit(data) {
      this.handleNodeClick(data)
    },

    /** 删除节点 */
    handleDelete(data) {
      let confirmText = ''
      let deleteApi = null
      let deleteParams = {}

      if (data.type === 'volume') {
        confirmText = `确定要删除卷别"${data.label}"吗？删除后该卷别下的所有大题和题目也会被删除！`
        deleteApi = deletePaperVolume
        deleteParams = { ids: [data.volumeId] }
      } else if (data.type === 'section') {
        confirmText = `确定要删除大题"${data.label}"吗？删除后该大题下的所有题目也会被删除！`
        deleteApi = deletePaperSection
        deleteParams = { ids: [data.sectionId] }
      } else {
        // 题目删除在题目列表中处理
        return
      }

      this.$modal.confirm(confirmText).then(() => {
        deleteApi(deleteParams).then(response => {
          if (response.code === 200) {
            this.$modal.msgSuccess("删除成功")
            this.currentNode = null
            // 更新试卷的题目总数
            this.updatePaperTotalQuestions()
            this.loadAllData()
          } else {
            this.$modal.msgError(response.msg || "删除失败")
          }
        }).catch(() => {
          this.$modal.msgError("删除失败")
        })
      }).catch(() => {})
    },

    /** 刷新树 */
    refreshTree() {
      this.loadAllData()
    },

    /** 卷别表单提交 */
    handleVolumeSubmit() {
      this.$refs.volumeForm.validate(valid => {
        if (valid) {
          if (this.volumeForm.id) {
            updatePaperVolume(this.volumeForm).then(response => {
              if (response.code === 200) {
                this.$modal.msgSuccess("修改成功")
                this.loadAllData()
              } else {
                this.$modal.msgError(response.msg || "修改失败")
              }
            }).catch(() => {
              this.$modal.msgError("修改失败")
            })
          }
        }
      })
    },

    /** 大题表单提交 */
    handleSectionSubmit() {
      this.$refs.sectionForm.validate(valid => {
        if (valid) {
          if (this.sectionForm.id) {
            // 自动计算题目数量（从已关联的题目列表获取）
            const sectionQuestions = this.questionList.filter(q => q.sectionId === this.sectionForm.id)
            this.sectionForm.questionCount = sectionQuestions.length

            updatePaperSection(this.sectionForm).then(response => {
              if (response.code === 200) {
                this.$modal.msgSuccess("修改成功")
                // 更新试卷的题目总数
                this.updatePaperTotalQuestions()
                this.loadAllData()
              } else {
                this.$modal.msgError(response.msg || "修改失败")
              }
            }).catch(() => {
              this.$modal.msgError("修改失败")
            })
          }
        }
      })
    },

    /** 取消编辑 */
    handleCancel() {
      this.currentNode = null
      this.$refs.structureTree && this.$refs.structureTree.setCurrentKey(null)
    },

    /** 选择题库题目 */
    handleSelectQuestions() {
      if (!this.currentNode || this.currentNode.type !== 'section') {
        this.$modal.msgWarning("请先选择大题")
        return
      }

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
      if (this.$refs.questionSelectQueryForm) {
        this.$refs.questionSelectQueryForm.resetFields()
      }
      this.questionSelectQueryParams.title = undefined
      this.questionSelectQueryParams.categoryId = this.questionSelectCategoryId
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
      if (!this.currentNode || this.currentNode.type !== 'section') return

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
      // 根据题目类型返回默认分值
      const defaultScoreMap = {
        0: 2.0,  // 单选题
        1: 5.0,  // 多选题
        2: 1.0,  // 判断题
        3: 3.0,  // 填空题
        4: 5.0,  // 简答题
        5: 10.0  // 论述题
      }
      return defaultScoreMap[question.type] || 1.0
    },

    /** 移除题目 */
    handleRemoveQuestion(index) {
      this.sectionQuestions.splice(index, 1)
      // 重新排序
      this.sectionQuestions.forEach((q, i) => {
        q.sectionOrder = i + 1
      })
    },

    /** 清空全部题目 */
    handleClearAllQuestions() {
      this.$modal.confirm('是否确认清空该大题下的所有题目？').then(() => {
        this.sectionQuestions = []
      }).catch(() => {})
    },

    /** 保存题目 */
    handleSaveQuestions() {
      if (!this.currentNode || this.currentNode.type !== 'section') return

      const questionList = this.sectionQuestions.map((q, index) => ({
        paperId: this.paperId,
        questionId: q.questionId,
        sectionId: this.currentNode.sectionId,
        sectionOrder: index + 1,
        score: q.score
      }))

      batchSavePaperQuestion({
        paperId: this.paperId,
        sectionId: this.currentNode.sectionId,
        questionList: questionList
      }).then(response => {
        if (response.code === 200) {
          // 自动更新大题的题目数量
          this.updateSectionQuestionCount(this.currentNode.sectionId, questionList.length)
          this.$modal.msgSuccess("保存成功")
          this.loadAllData()
        } else {
          this.$modal.msgError(response.msg || "保存失败")
        }
      }).catch(() => {
        this.$modal.msgError("保存失败")
      })
    },

    /** 更新大题的题目数量 */
    updateSectionQuestionCount(sectionId, questionCount) {
      const section = this.sectionList.find(s => s.id === sectionId)
      if (section) {
        updatePaperSection({
          id: sectionId,
          paperId: this.paperId,
          volumeId: section.volumeId,
          volumeCode: section.volumeCode,
          sectionName: section.sectionName,
          sectionOrder: section.sectionOrder,
          questionCount: questionCount,
          totalScore: section.totalScore,
          scorePerQuestion: section.scorePerQuestion,
          instructionText: section.instructionText
        }).then(() => {
          // 更新试卷的题目总数
          this.updatePaperTotalQuestions()
        }).catch(() => {
          // 忽略错误，不影响主流程
        })
      }
    },

    /** 更新试卷的题目总数 */
    updatePaperTotalQuestions() {
      // 统计所有大题下的题目总数
      const totalQuestions = this.questionList.length

      // 调用更新试卷接口
      updatePaper({
        id: this.paperId,
        totalQuestions: totalQuestions
      }).catch(() => {
        // 忽略错误，不影响主流程
      })
    },

    /** 卷别音频上传前校验 */
    handleVolumeAudioBeforeUpload(file) {
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
    handleVolumeAudioSuccess(response, file) {
      if (response.code === 200) {
        const mediaBO = {
          paperId: this.paperId,
          volumeId: this.volumeForm.id,
          mediaType: 7,
          mediaName: file.name,
          mediaPath: response.data.path || response.data.url,
          mediaUrl: response.data.url,
          mediaSize: file.size,
          mediaFormat: file.name.split('.').pop(),
          storageType: 0
        }
        saveQuestionMedia(mediaBO).then(saveResponse => {
          if (saveResponse.code === 200) {
            this.$set(this.volumeAudioMap, this.volumeForm.id, {
              url: response.data.url,
              path: response.data.path || response.data.url,
              mediaId: saveResponse.data
            })
            this.volumeAudioFileList = [{
              name: file.name,
              url: response.data.url
            }]
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
    handleVolumeAudioRemove() {
      const audio = this.volumeAudioMap[this.volumeForm.id]
      if (audio && audio.mediaId) {
        removeQuestionMedia({ id: audio.mediaId }).then(response => {
          if (response.code === 200) {
            this.$set(this.volumeAudioMap, this.volumeForm.id, null)
            this.volumeAudioFileList = []
            this.$modal.msgSuccess("删除成功")
          } else {
            this.$modal.msgError(response.msg || "删除失败")
          }
        }).catch(() => {
          this.$modal.msgError("删除失败")
        })
      } else {
        this.volumeAudioFileList = []
      }
    },

    /** 大题音频上传前校验 */
    handleSectionAudioBeforeUpload(file) {
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
    handleSectionAudioSuccess(response, file) {
      if (response.code === 200) {
        const mediaBO = {
          paperId: this.paperId,
          sectionId: this.sectionForm.id,
          mediaType: 8,
          mediaName: file.name,
          mediaPath: response.data.path || response.data.url,
          mediaUrl: response.data.url,
          mediaSize: file.size,
          mediaFormat: file.name.split('.').pop(),
          storageType: 0
        }
        saveQuestionMedia(mediaBO).then(saveResponse => {
          if (saveResponse.code === 200) {
            this.$set(this.sectionAudioMap, this.sectionForm.id, {
              url: response.data.url,
              path: response.data.path || response.data.url,
              mediaId: saveResponse.data
            })
            this.sectionAudioFileList = [{
              name: file.name,
              url: response.data.url
            }]
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
    handleSectionAudioRemove() {
      const audio = this.sectionAudioMap[this.sectionForm.id]
      if (audio && audio.mediaId) {
        removeQuestionMedia({ id: audio.mediaId }).then(response => {
          if (response.code === 200) {
            this.$set(this.sectionAudioMap, this.sectionForm.id, null)
            this.sectionAudioFileList = []
            this.$modal.msgSuccess("删除成功")
          } else {
            this.$modal.msgError(response.msg || "删除失败")
          }
        }).catch(() => {
          this.$modal.msgError("删除失败")
        })
      } else {
        this.sectionAudioFileList = []
      }
    },

    /** 媒体文件上传错误 */
    handleMediaUploadError(err) {
      this.$modal.msgError("上传失败：" + (err.message || "未知错误"))
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
.paper-structure-editor {
  height: 600px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
}

.tree-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
}

.tree-header {
  padding: 10px 15px;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.tree-title {
  font-weight: bold;
  font-size: 14px;
}

.structure-tree {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
}

.custom-tree-node {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 14px;
  padding-right: 8px;
}

.tree-node-label {
  display: flex;
  align-items: center;
  flex: 1;
}

.node-icon {
  margin-right: 5px;
  color: #409EFF;
}

.question-count {
  margin-left: 8px;
  color: #909399;
  font-size: 12px;
}

.question-score {
  margin-left: 8px;
  color: #67C23A;
  font-size: 12px;
}

.tree-node-actions {
  display: none;
}

.custom-tree-node:hover .tree-node-actions {
  display: block;
}

.tree-node-actions .el-button {
  padding: 0 5px;
}

.edit-container {
  height: 100%;
  overflow-y: auto;
  background: #fff;
  padding: 20px;
}

.edit-panel {
  height: 100%;
}

.edit-header {
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 1px solid #e4e7ed;
}

.edit-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: bold;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #909399;
}

.empty-state p {
  margin-top: 10px;
}

.question-list-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.question-list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 1px solid #e4e7ed;
}

.question-list-footer {
  margin-top: 15px;
  padding-top: 15px;
  border-top: 1px solid #e4e7ed;
  text-align: right;
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

