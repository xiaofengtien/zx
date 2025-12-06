<template>
  <div class="paper-structure-table">
    <div class="table-header">
      <div class="header-left">
        <span class="table-title"></span>
      </div>
      <div class="header-right">
        <el-button type="primary" size="small" icon="el-icon-plus" @click="handleAddVolume">新增卷别</el-button>
        <el-button type="text" icon="el-icon-refresh" size="small" @click="loadAllData" title="刷新">刷新</el-button>
      </div>
    </div>

    <el-table
      :data="displayTableData"
      border
      row-key="id"
      v-loading="loading"
      style="width: 100%"
    >
      <!-- 名称列 -->
      <el-table-column label="名称" min-width="300">
        <template slot-scope="scope">
          <span :style="{ paddingLeft: (scope.row.level * 20) + 'px' }">
            <!-- 卷别 -->
            <template v-if="scope.row.type === 'volume'">
              <i
                :class="isVolumeExpanded(scope.row.volumeId) ? 'el-icon-folder-opened' : 'el-icon-folder'"
                style="color: #409EFF; margin-right: 5px; cursor: pointer;"
                @click.stop="toggleVolumeExpand(scope.row.volumeId)"
                :title="isVolumeExpanded(scope.row.volumeId) ? '点击折叠' : '点击展开'"
              ></i>
              <strong>{{ scope.row.volumeName }}</strong>
              <span style="margin-left: 10px; color: #909399; font-size: 12px;">
                ({{ scope.row.sectionCount || 0 }}个大题，{{ scope.row.questionCount || 0 }}题)
              </span>
            </template>
            <!-- 大题 -->
            <template v-else-if="scope.row.type === 'section'">
              <i class="el-icon-tickets" style="color: #67C23A; margin-right: 5px;"></i>
              <span>{{ scope.row.sectionName }}</span>
              <span style="margin-left: 10px; color: #909399; font-size: 12px;">
                ({{ scope.row.questionCount || 0 }}题)
              </span>
            </template>
            <!-- 题目 -->
            <template v-else-if="scope.row.type === 'question'">
              <i class="el-icon-question" style="color: #E6A23C; margin-right: 5px;"></i>
              <span>{{ scope.row.title }}</span>
              <span style="margin-left: 10px; color: #67C23A; font-size: 12px;">
                ({{ scope.row.score || 0 }}分)
              </span>
            </template>
          </span>
        </template>
      </el-table-column>

      <!-- 排序列 -->
      <el-table-column label="排序" width="80" align="center">
        <template slot-scope="scope">
          <span v-if="scope.row.type === 'volume'">{{ scope.row.volumeOrder }}</span>
          <span v-else-if="scope.row.type === 'section'">{{ scope.row.sectionOrder }}</span>
          <span v-else-if="scope.row.type === 'question'">{{ scope.row.sectionOrder }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>

      <!-- 题目数量列 -->
      <el-table-column label="题目数量" width="100" align="center">
        <template slot-scope="scope">
          <span v-if="scope.row.type === 'volume' || scope.row.type === 'section'">
            {{ scope.row.questionCount || 0 }}
          </span>
          <span v-else>-</span>
        </template>
      </el-table-column>

      <!-- 操作列 -->
      <el-table-column label="操作" width="220" align="center" fixed="right">
        <template slot-scope="scope">
          <!-- 卷别操作 -->
          <template v-if="scope.row.type === 'volume'">
            <el-button
              type="text"
              icon="el-icon-plus"
              size="mini"
              @click="handleAddSection(scope.row)"
              title="新增大题"
            >
              新增大题
            </el-button>
            <el-button
              type="text"
              icon="el-icon-edit"
              size="mini"
              @click="handleEditVolume(scope.row)"
              title="编辑卷别"
            >
              编辑
            </el-button>
            <el-button
              type="text"
              icon="el-icon-delete"
              size="mini"
              style="color: #f56c6c;"
              @click="handleDeleteVolume(scope.row)"
              title="删除卷别"
            >
              删除
            </el-button>
          </template>
          <!-- 大题操作 -->
          <template v-else-if="scope.row.type === 'section'">
            <el-button
              type="text"
              icon="el-icon-plus"
              size="mini"
              @click="handleManageQuestions(scope.row)"
              title="管理题目"
            >
              管理题目
            </el-button>
            <el-button
              type="text"
              icon="el-icon-edit"
              size="mini"
              @click="handleEditSection(scope.row)"
              title="编辑大题"
            >
              编辑
            </el-button>
            <el-button
              type="text"
              icon="el-icon-delete"
              size="mini"
              style="color: #f56c6c;"
              @click="handleDeleteSection(scope.row)"
              title="删除大题"
            >
              删除
            </el-button>
          </template>
          <!-- 题目操作 -->
          <template v-else-if="scope.row.type === 'question'">
            <el-button
              type="text"
              icon="el-icon-delete"
              size="mini"
              style="color: #f56c6c;"
              @click="handleDeleteQuestion(scope.row)"
              title="删除题目"
            >
              删除
            </el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>

    <!-- 卷别编辑对话框 -->
    <el-dialog
      :title="volumeDialogTitle"
      :visible.sync="volumeDialogVisible"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form ref="volumeForm" :model="volumeForm" :rules="volumeRules" label-width="120px">
        <el-form-item label="卷别名称" prop="volumeName">
          <el-input v-model="volumeForm.volumeName" placeholder="如：A卷、B卷" maxlength="50" />
        </el-form-item>
        <el-form-item label="排序" prop="volumeOrder">
          <el-input-number v-model="volumeForm.volumeOrder" :min="1" :max="999" controls-position="right" style="width: 200px" />
        </el-form-item>
        <el-form-item label="卷别名称音频">
          <oss-upload
            ref="volumeAudioUpload"
            v-model="volumeAudioUrl"
            :limit="1"
            accept=".mp3,.wav,.ogg,.m4a,.aac"
            :file-size="10"
            tip="只能上传音频文件，且不超过10MB，仅支持一个文件，二次上传将直接替换"
            path-prefix="exam/question"
            list-type="text"
            @change="handleVolumeAudioChange"
            @progress="(event, file) => handleUploadProgress('volumeAudio', event.percent < 100)"
            @preview="handleVolumeAudioPreview"
          />
          <div v-if="uploadStatus.volumeAudio" style="color: #E6A23C; font-size: 12px; margin-top: 5px;">
            请先等待上传完成
          </div>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="handleVolumeSubmit">确 定</el-button>
        <el-button @click="volumeDialogVisible = false">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 大题编辑对话框 -->
    <el-dialog
      :title="sectionDialogTitle"
      :visible.sync="sectionDialogVisible"
      width="700px"
      :close-on-click-modal="false"
    >
      <el-form ref="sectionForm" :model="sectionForm" :rules="sectionRules" label-width="120px">
        <el-form-item label="大题标题" prop="sectionName">
          <el-input v-model="sectionForm.sectionName" placeholder="如：第一节、第二节" maxlength="50" />
        </el-form-item>
        <el-form-item label="大题说明" prop="instructionText">
          <Editor v-model="sectionForm.instructionText" :min-height="120" />
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
            （每道题有音频的情况下播放多少次，如：每段材料读两遍设置为2，每段对话仅读一遍设置为1）
          </span>
        </el-form-item>
        <el-form-item label="大题说明音频">
          <oss-upload
            ref="sectionAudioUpload"
            v-model="sectionAudioUrl"
            :limit="1"
            accept=".mp3,.wav,.ogg,.m4a,.aac"
            :file-size="10"
            tip="只能上传音频文件，且不超过10MB，仅支持一个文件，二次上传将直接替换"
            path-prefix="exam/question"
            list-type="text"
            @change="handleSectionAudioChange"
            @progress="(event, file) => handleUploadProgress('sectionAudio', event.percent < 100)"
            @preview="handleSectionAudioPreview"
          />
          <div v-if="uploadStatus.sectionAudio" style="color: #E6A23C; font-size: 12px; margin-top: 5px;">
            请先等待上传完成
          </div>
        </el-form-item>
        <el-form-item label="排序" prop="sectionOrder">
          <el-input-number v-model="sectionForm.sectionOrder" :min="1" :max="999" controls-position="right" style="width: 200px" />
        </el-form-item>
        <el-form-item label="题目数量">
          <el-input-number
            v-model="sectionForm.questionCount"
            :min="0"
            :max="999"
            controls-position="right"
            style="width: 200px"
            :disabled="true"
          />
          <span style="margin-left: 10px; color: #909399; font-size: 12px;">
            （自动计算：当前已关联 {{ sectionFormQuestionCount }} 题）
          </span>
        </el-form-item>
        <el-form-item label="每题分值" prop="scorePerQuestion">
          <el-input-number
            v-model="sectionForm.scorePerQuestion"
            :min="0"
            :precision="2"
            :step="0.1"
            controls-position="right"
            style="width: 200px"
            @change="handleScorePerQuestionChange"
          />
          <span style="margin-left: 10px; color: #909399; font-size: 12px;">
            （设置后，该大题下所有题目将使用此分值）
          </span>
        </el-form-item>
        <el-form-item label="大题总分" prop="totalScore">
          <el-input-number
            v-model="sectionForm.totalScore"
            :min="0"
            :precision="2"
            :step="0.1"
            controls-position="right"
            style="width: 200px"
            :disabled="true"
          />
          <span style="margin-left: 10px; color: #909399; font-size: 12px;">
            （每题分值 × 题目数量，自动计算）
          </span>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="sectionDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="handleSectionSubmit">确 定</el-button>
      </div>
    </el-dialog>

    <!-- 题目管理抽屉（只展示已选题目列表） -->
    <el-drawer
      :title="questionManageTitle"
      :visible.sync="questionManageVisible"
      size="60%"
      direction="rtl"
      :close-on-click-modal="false"
      :wrapperClosable="true"
    >
      <div class="question-manage-container">
        <div class="question-list-header">
          <span style="font-size: 14px; padding-left: 10px; color: #606266;">
            <strong>{{ questionManageSectionName }}</strong>
            <span style="margin-left: 10px; color: #909399;">
              (已选 {{ sectionQuestionsTotal }} 题)
            </span>
          </span>
          <div>
            <el-button type="primary" size="small" icon="el-icon-plus" @click="handleSelectQuestions">选择题目</el-button>
            <!-- 暂时隐藏批量设置分值 -->
            <!-- <el-button
              v-if="sectionQuestions.length > 0"
              type="warning"
              size="small"
              icon="el-icon-edit"
              @click="handleBatchSetScore"
            >
              批量设置分值
            </el-button> -->
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
          @selection-change="handleSectionQuestionSelectionChange"
        >
          <el-table-column type="selection" width="55" align="center" />
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
          <el-table-column label="每题分值" width="130" align="center">
            <template slot-scope="scope">
              <span>{{ scope.row.scorePerQuestion || scope.row.score || 0 }}</span>
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
        <el-empty v-else description="暂未关联题目，请点击「新增题目」按钮选择题库题目" :image-size="100" />
        <!-- 分页 -->
        <pagination
          v-show="sectionQuestionsTotal > 0"
          :total="sectionQuestionsTotal"
          :page.sync="sectionQuestionsQueryParams.pageNum"
          :limit.sync="sectionQuestionsQueryParams.pageSize"
          @pagination="loadSectionQuestions"
        />
      </div>
      <div slot="footer" class="drawer-footer">
        <el-button type="primary" @click="handleSaveQuestions">保 存</el-button>
        <el-button @click="questionManageVisible = false">取 消</el-button>
      </div>
    </el-drawer>

    <!-- 题目选择弹窗（独立弹窗） -->
    <el-dialog
      title="选择题库题目"
      :visible.sync="questionSelectVisible"
      width="1200px"
      append-to-body
      :close-on-click-modal="true"
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

    <!-- 批量设置分值弹窗 -->
    <el-dialog
      title="批量设置分值"
      :visible.sync="batchSetScoreVisible"
      width="400px"
      append-to-body
    >
      <el-form label-width="100px">
        <el-form-item label="分值">
          <el-input-number
            v-model="batchScore"
            :min="0"
            :precision="2"
            :step="0.1"
            controls-position="right"
            style="width: 200px;"
          />
        </el-form-item>
        <el-form-item>
          <span style="color: #909399; font-size: 12px;">
            将为选中的 {{ batchSetScoreSelectedCount }} 题设置统一分值
          </span>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="handleBatchSetScoreConfirm">确 定</el-button>
        <el-button @click="batchSetScoreVisible = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  getPaperVolumeList, addPaperVolume, updatePaperVolume, deletePaperVolume,
  getPaperSectionList, addPaperSection, updatePaperSection, deletePaperSection,
  getPaperQuestionList, batchSavePaperQuestion,
  updatePaper, getPaper
} from "@/api/exam/paper"
import { getQuestionMediaByVolumeId, getQuestionMediaBySectionId } from "@/api/exam/paper"
import { getQuestionList } from "@/api/exam/question"
import { getCategoryTree } from "@/api/exam/questionCategory"
import { getToken } from "@/utils/auth"
import { Splitpanes, Pane } from "splitpanes"
import "splitpanes/dist/splitpanes.css"
import OssUpload from "@/components/OssUpload"
import Editor from "@/components/Editor"

export default {
  name: "PaperStructureTable",
  components: { Splitpanes, Pane, OssUpload, Editor },
  dicts: ['question_type', 'subject'],
  props: {
    paperId: {
      type: Number,
      default: undefined
    },
    paperName: {
      type: String,
      default: ''
    },
    autoCalculateTotalScore: {
      type: Boolean,
      default: false
    }
  },
  emits: ['update-total-score'],
  data() {
    return {
      loading: false,

      // 表格数据
      volumeList: [],
      flatTableData: [], // 扁平化的表格数据
      expandedVolumes: [], // 展开的卷别ID列表

      // 卷别编辑
      volumeDialogVisible: false,
      volumeDialogTitle: '新增卷别',
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
      volumeAudioUrl: null, // 卷别音频URL（用于 OssUpload 的 v-model）
      volumeAudioMap: {},
      // 上传状态跟踪
      uploadStatus: {
        volumeAudio: false,
        sectionAudio: false
      },

      // 大题编辑
      sectionDialogVisible: false,
      sectionDialogTitle: '新增大题',
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
          { required: true, message: "大题标题不能为空", trigger: "blur" },
          { max: 50, message: "大题标题长度不能超过50个字符", trigger: "blur" }
        ],
        instructionText: [
          { required: true, message: "大题说明不能为空", trigger: "blur" }
        ]
      },
      sectionEditTab: 'info',
      sectionQuestions: [],
      sectionAudioFileList: [],
      sectionAudioUrl: null, // 大题音频URL（用于 OssUpload 的 v-model）
      sectionAudioMap: {},
      currentSectionId: null, // 当前编辑的大题ID

      // 题目管理抽屉（独立）
      questionManageVisible: false,
      questionManageTitle: '',
      questionManageSectionId: null,
      questionManageSectionName: '',
      sectionQuestionsTotal: 0, // 题目总数（用于分页）
      sectionQuestionsQueryParams: {
        pageNum: 1,
        pageSize: 10
      },
      sectionQuestionSelectedRows: [], // 已选中的题目行（用于批量操作）

      // 批量设置分值
      batchSetScoreVisible: false,
      batchScore: 0,

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
      // 已使用 OssUpload 组件，不再需要 uploadMediaUrl 和 uploadHeaders

      // 数据缓存
      sectionList: [],
      questionList: []
    }
  },
  created() {
    // 组件创建时，如果是新增模式（paperId 不存在），清空所有数据
    if (!this.paperId) {
      this.resetData()
    }
  },
  mounted() {
    // 新增模式下不需要加载数据，编辑模式下才加载
    if (this.paperId) {
      this.loadAllData()
    } else {
      // 新增模式：确保清空所有缓存数据
      this.resetData()
    }
  },
  beforeDestroy() {
    // 组件销毁前，清空所有数据，避免缓存
    this.resetData()
  },
  watch: {
    paperId: {
      handler(newVal, oldVal) {
        // 如果 paperId 变为 undefined/null（从编辑模式切换到新增模式），清空数据
        if (!newVal && oldVal) {
          this.resetData()
        } else if (newVal && newVal !== oldVal) {
          // 只在编辑模式下加载数据（paperId 存在且变化）
          this.loadAllData()
        } else if (!newVal) {
          // 新增模式：确保清空所有缓存数据
          this.resetData()
        }
      }
    },
    sectionQuestions: {
      handler(newVal) {
        // 只有在编辑大题对话框打开时，才更新题目数量
        // 避免在题目管理抽屉中操作时影响编辑对话框的显示
        if (this.sectionDialogVisible && this.sectionForm && this.sectionForm.id) {
          // 从完整的 questionList 中获取题目数量，而不是从分页的 sectionQuestions
          const sectionQuestions = this.questionList.filter(q => {
            const qSectionId = q.sectionId !== undefined && q.sectionId !== null ? q.sectionId : null
            return qSectionId === this.sectionForm.id
          })
          this.sectionForm.questionCount = sectionQuestions.length
        }
      },
      deep: true
    },
    questionSelectCategoryName(val) {
      this.$refs.questionSelectCategoryTree && this.$refs.questionSelectCategoryTree.filter(val)
    }
  },
  computed: {
    /** 是否有可展开的行 */
    hasExpandableRows() {
      return this.volumeList.length > 0
    },
    /** 计算显示的表格数据（根据折叠状态） */
    displayTableData() {
      if (this.expandedVolumes.length === 0 && this.volumeList.length > 0) {
        // 如果没有任何展开的卷别，只显示卷别行
        return this.flatTableData.filter(row => row.type === 'volume')
      }

      const result = []
      let currentVolumeId = null
      let isExpanded = false

      for (const row of this.flatTableData) {
        if (row.type === 'volume') {
          currentVolumeId = row.volumeId
          isExpanded = this.isVolumeExpanded(currentVolumeId)
          result.push(row)
        } else if (row.type === 'section' || row.type === 'question') {
          if (isExpanded && row.volumeId === currentVolumeId) {
            result.push(row)
          }
        }
      }

      return result
    },
    /** 批量设置分值选中的题目数量 */
    batchSetScoreSelectedCount() {
      return this.sectionQuestionSelectedRows.length
    },
    /** 编辑大题对话框中显示的题目数量（从完整的 questionList 中计算） */
    sectionFormQuestionCount() {
      if (!this.sectionForm || !this.sectionForm.id) {
        return 0
      }
      // 从完整的 questionList 中获取题目数量，确保包括已选但未保存的题目
      return this.questionList.filter(q => {
        const qSectionId = q.sectionId !== undefined && q.sectionId !== null ? q.sectionId : null
        return qSectionId === this.sectionForm.id
      }).length
    }
  },
  methods: {
    /** 重置所有数据（用于新增模式） */
    resetData() {
      // 清空表格数据
      this.volumeList = []
      this.flatTableData = []
      this.expandedVolumes = []
      this.sectionList = []
      this.questionList = []

      // 清空音频缓存
      this.volumeAudioMap = {}
      this.sectionAudioMap = {}
      this.volumeAudioFileList = []
      this.sectionAudioFileList = []

      // 清空表单数据
      this.volumeForm = {
        id: undefined,
        paperId: undefined,
        volumeName: undefined,
        volumeOrder: 1
      }
      this.sectionForm = {
        id: undefined,
        paperId: undefined,
        volumeId: undefined,
        volumeCode: undefined,
        sectionName: undefined,
        sectionOrder: 1,
        questionCount: 0,
        totalScore: 0,
        scorePerQuestion: 0,
        instructionText: undefined
      }

      // 清空题目相关数据
      this.sectionQuestions = []
      this.currentSectionId = null
      this.questionSelectSelectedIds = []
      this.questionSelectSelectedRows = []
      this.sectionQuestionSelectedRows = []

      // 关闭所有对话框
      this.volumeDialogVisible = false
      this.sectionDialogVisible = false
      this.questionManageVisible = false
      this.questionSelectVisible = false
      this.batchSetScoreVisible = false
    },
    getFileNameFromUrl(url) {
      if (!url) return ''
      const cleanUrl = url.split('?')[0]
      const segments = cleanUrl.split('/')
      return segments[segments.length - 1] || ''
    },
    async resolveMediaPreviewUrl(rawUrl) {
      if (!rawUrl) return ''
      try {
        const { getMediaDownloadUrl } = await import('@/api/exam/question')
        const { normalizeMediaUrl } = await import('@/utils/media')
        const response = await getMediaDownloadUrl({ url: rawUrl })
        const signedUrl = response?.downloadUrl || response?.data?.downloadUrl
        if (response?.code === 200 && signedUrl) {
          return signedUrl
        }
        return normalizeMediaUrl(rawUrl)
      } catch (error) {
        console.error('获取媒体下载链接失败:', error)
        const { normalizeMediaUrl } = await import('@/utils/media')
        return normalizeMediaUrl(rawUrl)
      }
    },
    async updateVolumeAudioFileList(rawUrl, fileName) {
      const previewUrl = await this.resolveMediaPreviewUrl(rawUrl)
      this.volumeAudioFileList = [{
        name: fileName || this.getFileNameFromUrl(rawUrl),
        url: previewUrl,
        status: 'success'
      }]
    },
    async updateSectionAudioFileList(rawUrl, fileName) {
      const previewUrl = await this.resolveMediaPreviewUrl(rawUrl)
      this.sectionAudioFileList = [{
        name: fileName || this.getFileNameFromUrl(rawUrl),
        url: previewUrl,
        status: 'success'
      }]
    },
    handleVolumeAudioPreview(file) {
      console.log('handleVolumeAudioPreview called:', {
        file,
        volumeAudioUrl: this.volumeAudioUrl,
        volumeFormId: this.volumeForm.id,
        volumeAudioMap: this.volumeAudioMap[this.volumeForm.id]
      })

      // 优先使用 file.url，如果没有则从缓存中获取
      const audioUrl = file?.url || this.volumeAudioUrl || this.volumeAudioMap[this.volumeForm.id]?.url
      if (!audioUrl) {
        console.warn('音频地址不存在:', { file, volumeAudioUrl: this.volumeAudioUrl, volumeFormId: this.volumeForm.id })
        this.$message.warning('音频地址不存在')
        return
      }
      const previewFile = {
        ...file,
        url: audioUrl,
        name: file?.name || this.getFileNameFromUrl(audioUrl)
      }
      console.log('触发 preview-audio 事件:', previewFile)
      this.$emit('preview-audio', previewFile)
    },
    handleVolumeAudioSlotRemove(file) {
      if (this.$refs.volumeAudioUpload && file) {
        this.$refs.volumeAudioUpload.handleRemove(file)
      }
    },
    handleSectionAudioPreview(file) {
      console.log('handleSectionAudioPreview called:', {
        file,
        sectionAudioUrl: this.sectionAudioUrl,
        sectionFormId: this.sectionForm.id,
        sectionAudioMap: this.sectionAudioMap[this.sectionForm.id]
      })

      // 优先使用 file.url，如果没有则从缓存中获取
      const audioUrl = file?.url || this.sectionAudioUrl || this.sectionAudioMap[this.sectionForm.id]?.url
      if (!audioUrl) {
        console.warn('音频地址不存在:', { file, sectionAudioUrl: this.sectionAudioUrl, sectionFormId: this.sectionForm.id })
        this.$message.warning('音频地址不存在')
        return
      }
      const previewFile = {
        ...file,
        url: audioUrl,
        name: file?.name || this.getFileNameFromUrl(audioUrl)
      }
      console.log('触发 preview-audio 事件:', previewFile)
      this.$emit('preview-audio', previewFile)
    },
    /** 加载所有数据 */
    async loadAllData() {
      if (!this.paperId) return

      this.loading = true
      try {
        const [volumeRes, sectionRes, questionRes] = await Promise.all([
          getPaperVolumeList({ id: this.paperId }),
          getPaperSectionList({ id: this.paperId }),
          getPaperQuestionList({ paperId: this.paperId })
        ])

        const volumes = volumeRes.code === 200 ? (volumeRes.data || []) : []
        this.sectionList = sectionRes.code === 200 ? (sectionRes.data || []) : []
        this.questionList = questionRes.code === 200 ? (questionRes.data || []) : []

        // 调试日志：检查数据是否正确加载
        console.log('编辑页面 - 加载数据完成:', {
          paperId: this.paperId,
          volumesCount: volumes.length,
          sectionsCount: this.sectionList.length,
          questionsCount: this.questionList.length,
          volumes: volumes.map(v => ({ id: v.id, volumeName: v.volumeName, volumeCode: v.volumeCode })),
          sections: this.sectionList.map(s => ({
            id: s.id,
            sectionName: s.sectionName,
            volumeId: s.volumeId,
            volumeIdType: typeof s.volumeId,
            volumeCode: s.volumeCode
          }))
        })

        // 构建表格数据（卷别 -> 大题 -> 题目）
        this.buildTableData(volumes)

        // 加载音频信息
        this.loadAudioInfo(volumes)

        // 如果开启了自动计算总分，实时计算所有大题的总分之和
        if (this.autoCalculateTotalScore) {
          this.calculateAndEmitTotalScore()
        }
      } catch (error) {
        console.error('加载数据失败:', error)
      } finally {
        this.loading = false
      }
    },

    /** 构建表格数据 */
    buildTableData(volumes) {
      // 如果没有传入 volumes，使用 this.volumeList
      const volumesToUse = volumes || this.volumeList || []
      const sortedVolumes = [...volumesToUse].sort((a, b) => (a.volumeOrder || 0) - (b.volumeOrder || 0))

      // 确保 sectionList 和 questionList 是数组（直接使用 this.sectionList，确保使用最新数据）
      const sectionList = this.sectionList || []
      const questionList = this.questionList || []

      // 保存原始层级数据（用于查找）
      this.volumeList = sortedVolumes.map(volume => {
        // 使用 this.sectionList 而不是局部变量 sectionList，确保使用最新数据
        // 优先使用 volumeId 匹配，如果不存在则使用 volumeCode 匹配
        const targetVolumeId = volume.id !== undefined && volume.id !== null ? String(volume.id) : null
        const targetVolumeCode = volume.volumeCode || ''

        const volumeSections = this.sectionList
          .filter(section => {
            // 优先使用 volumeId 匹配
            const sectionVolumeId = section.volumeId !== undefined && section.volumeId !== null ? String(section.volumeId) : null
            if (sectionVolumeId && sectionVolumeId === targetVolumeId) {
              return true
            }

            // 如果 volumeId 不匹配或不存在，使用 volumeCode 匹配
            if (targetVolumeCode) {
              const sectionVolumeCode = (section.volumeCode || '').trim()
              const targetCode = targetVolumeCode.trim()
              if (sectionVolumeCode && targetCode && sectionVolumeCode === targetCode) {
                return true
              }
            }

            return false
          })
          .sort((a, b) => (a.sectionOrder || 0) - (b.sectionOrder || 0))

        let volumeQuestionCount = 0
        const sections = volumeSections.map(section => {
          const sectionQuestions = questionList
            .filter(q => q.sectionId === section.id)
            .sort((a, b) => (a.sectionOrder || 0) - (b.sectionOrder || 0))

          volumeQuestionCount += sectionQuestions.length

          return {
            ...section,
            questionCount: sectionQuestions.length,
            questions: sectionQuestions
          }
        })

        return {
          ...volume,
          sectionCount: sections.length,
          questionCount: volumeQuestionCount,
          sections: sections
        }
      })

      // 构建扁平化的表格数据（用于显示）
      this.flatTableData = []
      // 保留已有的展开状态，同时添加新卷别（新卷别默认展开）
      const existingExpanded = this.expandedVolumes || []
      const allVolumeIds = sortedVolumes.map(v => v.id)
      // 如果之前没有任何展开状态，默认展开所有卷别
      if (existingExpanded.length === 0 && allVolumeIds.length > 0) {
        // 首次加载，默认展开所有
        this.expandedVolumes = [...allVolumeIds]
      } else {
        // 保留已有状态，同时添加新卷别（新卷别默认展开）
        const validExpanded = existingExpanded.filter(id => allVolumeIds.includes(id))
        // 找出新卷别（在 allVolumeIds 中但不在 validExpanded 中的）
        const newVolumeIds = allVolumeIds.filter(id => !validExpanded.includes(id))
        // 合并已有状态和新卷别
        this.expandedVolumes = [...validExpanded, ...newVolumeIds]
      }

      sortedVolumes.forEach(volume => {
        // 使用严格匹配，确保类型一致（都转为字符串比较）
        // 优先使用 volumeId 匹配，如果不存在则使用 volumeCode 匹配
        const targetVolumeId = volume.id !== undefined && volume.id !== null ? String(volume.id) : null
        const targetVolumeCode = volume.volumeCode || ''

        const volumeSections = this.sectionList
          .filter(section => {
            // 优先使用 volumeId 匹配
            const sectionVolumeId = section.volumeId !== undefined && section.volumeId !== null ? String(section.volumeId) : null
            if (sectionVolumeId && sectionVolumeId === targetVolumeId) {
              return true
            }

            // 如果 volumeId 不匹配或不存在，使用 volumeCode 匹配
            if (targetVolumeCode) {
              const sectionVolumeCode = (section.volumeCode || '').trim()
              const targetCode = targetVolumeCode.trim()
              if (sectionVolumeCode && targetCode && sectionVolumeCode === targetCode) {
                return true
              }
            }

            return false
          })
          .sort((a, b) => (a.sectionOrder || 0) - (b.sectionOrder || 0))

        let volumeQuestionCount = 0
        volumeSections.forEach(section => {
          // 确保正确过滤：题目数据中的 sectionId 应该与 section.id 匹配
          const sectionQuestions = this.questionList.filter(q => {
            const qSectionId = q.sectionId !== undefined && q.sectionId !== null ? q.sectionId : null
            return qSectionId === section.id
          })
          volumeQuestionCount += sectionQuestions.length
        })

        // 添加卷别行
        this.flatTableData.push({
          id: `volume-${volume.id}`,
          type: 'volume',
          level: 0,
          volumeId: volume.id,
          volumeName: volume.volumeName,
          volumeCode: volume.volumeCode,
          volumeOrder: volume.volumeOrder,
          sectionCount: volumeSections.length,
          questionCount: volumeQuestionCount
        })

        // 添加大题行
        volumeSections.forEach(section => {
          // 确保正确过滤：题目数据中的 sectionId 应该与 section.id 匹配
          const sectionQuestions = this.questionList
            .filter(q => {
              const qSectionId = q.sectionId !== undefined && q.sectionId !== null ? q.sectionId : null
              return qSectionId === section.id
            })
            .sort((a, b) => (a.sectionOrder || 0) - (b.sectionOrder || 0))

          this.flatTableData.push({
            id: `section-${section.id}`,
            type: 'section',
            level: 1,
            sectionId: section.id,
            sectionName: section.sectionName,
            sectionOrder: section.sectionOrder,
            questionCount: sectionQuestions.length,
            totalScore: section.totalScore,
            scorePerQuestion: section.scorePerQuestion,
            instructionText: section.instructionText,
            volumeId: section.volumeId,
            volumeCode: section.volumeCode,
            volumeName: volume.volumeName
          })

          // 添加题目行
          sectionQuestions.forEach((q, index) => {
            this.flatTableData.push({
              id: `question-${q.questionId}-${section.id}`,
              type: 'question',
              level: 2,
              questionId: q.questionId,
              title: q.title || `题目${index + 1}`,
              type: q.type,
              subjectId: q.subjectId,
              score: parseFloat(q.score) || 0,
              scorePerQuestion: section.scorePerQuestion || parseFloat(q.score) || 0, // 每题分值（从大题获取）
              sectionOrder: q.sectionOrder || (index + 1),
              sectionId: q.sectionId || section.id, // 优先使用题目数据中的 sectionId
              sectionName: section.sectionName,
              volumeId: volume.id,
              volumeName: volume.volumeName
            })
          })
        })
      })
    },

    /** 加载音频信息 - 从 paper_volume 和 paper_section 表加载，而不是 question_media */
    async loadAudioInfo(volumes) {
      // 加载卷别音频 - 直接从 volume 对象中获取（后端已返回）
      for (const volume of volumes) {
        if (volume.volumeAudioUrl) {
          this.$set(this.volumeAudioMap, volume.id, {
            url: volume.volumeAudioUrl,
            path: volume.volumeAudioPath || volume.volumeAudioUrl,
            duration: volume.volumeAudioDuration || null
          })
        }
      }

      // 加载大题音频 - 直接从 section 对象中获取（后端已返回）
      for (const section of this.sectionList) {
        if (section.instructionAudioUrl) {
          this.$set(this.sectionAudioMap, section.id, {
            url: section.instructionAudioUrl,
            path: section.instructionAudioPath || section.instructionAudioUrl,
            duration: section.instructionAudioDuration || null
          })
        }
      }
    },

    /** 新增卷别 */
    handleAddVolume() {
      this.volumeDialogTitle = '新增卷别'
      // 计算最大排序号
      const maxOrder = this.volumeList.length > 0
        ? Math.max(...this.volumeList.map(v => v.volumeOrder || 0))
        : 0
      this.volumeForm = {
        id: undefined,
        paperId: this.paperId,
        volumeName: undefined,
        volumeOrder: maxOrder + 1
      }
      this.volumeAudioFileList = []
      this.volumeAudioUrl = null // 清空音频URL
      this.volumeDialogVisible = true
    },

    /** 编辑卷别 */
    handleEditVolume(row) {
      this.volumeDialogTitle = '编辑卷别'
      this.volumeForm = {
        id: row.volumeId,
        paperId: this.paperId,
        volumeName: row.volumeName,
        volumeOrder: row.volumeOrder
      }

      // 加载音频文件列表 - 使用辅助方法获取预览URL
      const audio = this.volumeAudioMap[row.volumeId]
      if (audio && audio.url) {
        this.volumeAudioUrl = audio.url // 设置 OssUpload 的 v-model
        this.updateVolumeAudioFileList(audio.url)
      } else {
        this.volumeAudioFileList = []
        this.volumeAudioUrl = null
      }

      this.volumeDialogVisible = true
    },

    /** 删除卷别（仅更新本地数据，不调用后端） */
    handleDeleteVolume(row) {
      this.$modal.confirm(`确定要删除卷别"${row.volumeName}"吗？删除后该卷别下的所有大题和题目也会被删除！`).then(() => {
        // 从本地数据中删除卷别
        const volumeIndex = this.volumeList.findIndex(v => v.id === row.volumeId)
        if (volumeIndex >= 0) {
          this.volumeList.splice(volumeIndex, 1)
        }
        // 删除该卷别下的所有大题
        const sectionIds = this.sectionList.filter(s => s.volumeId === row.volumeId).map(s => s.id)
        this.sectionList = this.sectionList.filter(s => s.volumeId !== row.volumeId)
        // 删除该卷别下的所有题目
        this.questionList = this.questionList.filter(q => {
          const qSectionId = q.sectionId !== undefined && q.sectionId !== null ? q.sectionId : null
          return !sectionIds.includes(qSectionId)
        })
        // 更新表格数据
        this.buildTableData()
        // 更新题目数量
        this.updateSectionQuestionCountFromList()
        // 如果开启了自动计算总分，实时计算所有大题的总分之和
        if (this.autoCalculateTotalScore) {
          this.calculateAndEmitTotalScore()
        }
        this.$modal.msgSuccess("删除成功（待保存）")
      }).catch(() => {})
    },

    /** 卷别表单提交（仅更新本地数据，不调用后端） */
    handleVolumeSubmit() {
      this.$refs.volumeForm.validate(valid => {
        if (valid) {
          if (this.volumeForm.id) {
            // 编辑：更新本地数据
            const volume = this.volumeList.find(v => v.id === this.volumeForm.id)
            if (volume) {
              volume.volumeName = this.volumeForm.volumeName
              volume.volumeOrder = this.volumeForm.volumeOrder
            }
            this.$modal.msgSuccess("修改成功（待保存）")
          } else {
            // 新增：添加到本地数据（生成临时ID）
            const tempVolumeId = `temp_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
            const newVolume = {
              id: tempVolumeId,
              paperId: this.paperId,
              volumeName: this.volumeForm.volumeName,
              volumeOrder: this.volumeForm.volumeOrder,
              volumeCode: '', // 后端会自动生成
              questionCount: 0,
              sectionCount: 0
            }
            this.volumeList.push(newVolume)
            // 如果有上传的音频，需要将音频信息从 volumeForm.id 迁移到新的临时ID
            if (this.volumeAudioUrl) {
              const oldVolumeId = this.volumeForm.id || 'temp_' + Date.now()
              const audioInfo = this.volumeAudioMap[oldVolumeId]
              if (audioInfo) {
                // 迁移音频信息到新的临时ID
                this.$set(this.volumeAudioMap, tempVolumeId, audioInfo)
                // 删除旧的音频信息（如果存在）
                if (oldVolumeId !== tempVolumeId) {
                  this.$delete(this.volumeAudioMap, oldVolumeId)
                }
              } else if (this.volumeAudioUrl) {
                // 如果 audioMap 中没有，但 volumeAudioUrl 有值，创建新的音频信息
                this.$set(this.volumeAudioMap, tempVolumeId, {
                  url: this.volumeAudioUrl,
                  path: this.volumeAudioUrl,
                  duration: null
                })
              }
            }
            // 确保新卷别自动展开
            const newVolumeId = newVolume.id
            // 更新表格数据
            this.buildTableData()
            // 在 buildTableData 之后，确保新卷别是展开的
            this.$nextTick(() => {
              if (newVolumeId && !this.expandedVolumes.includes(newVolumeId)) {
                // 重新赋值整个数组，确保 Vue 响应式系统能检测到变化
                this.expandedVolumes = [...this.expandedVolumes, newVolumeId]
              }
            })
            this.$modal.msgSuccess("新增成功（待保存）")
          }
          this.volumeDialogVisible = false
        }
      })
    },

    /** 新增大题 */
    handleAddSection(volumeRow) {
      this.sectionDialogTitle = '新增大题'
      // 计算该卷别下大题的最大排序号
      const volumeSections = this.sectionList.filter(s => {
        // 确保类型匹配
        const sVolumeId = s.volumeId !== undefined && s.volumeId !== null ? String(s.volumeId) : null
        const rowVolumeId = volumeRow.volumeId !== undefined && volumeRow.volumeId !== null ? String(volumeRow.volumeId) : null
        return sVolumeId === rowVolumeId
      })
      const maxOrder = volumeSections.length > 0
        ? Math.max(...volumeSections.map(s => s.sectionOrder || 0))
        : 0

      // ✅ 立即生成临时ID,确保音频上传时能正确缓存
      const tempId = `temp_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`

      // 确保 volumeId 类型正确（与 volume.id 保持一致）
      const volumeId = volumeRow.volumeId

      this.sectionForm = {
        id: tempId,  // ✅ 使用临时ID而不是undefined
        answerTime: 5,
        audioPlayCount: 1,
        paperId: this.paperId,
        volumeId: volumeId,  // 直接使用 volumeRow.volumeId
        volumeCode: volumeRow.volumeCode,
        sectionName: undefined,
        sectionOrder: maxOrder + 1,
        questionCount: 0,
        totalScore: 0,
        scorePerQuestion: 0,
        instructionText: undefined
      }
      this.sectionQuestions = []
      this.sectionAudioFileList = []
      this.sectionAudioUrl = null // 清空音频URL
      this.currentSectionId = null
      this.sectionEditTab = 'info'
      this.sectionDialogVisible = true
    },

    /** 编辑大题 */
    handleEditSection(row) {
      this.sectionDialogTitle = '编辑大题'
      const section = this.sectionList.find(s => s.id === row.sectionId)
      if (section) {
        // 先设置 currentSectionId，确保后续操作能正确获取题目（包括已选但未保存的题目）
        this.currentSectionId = section.id

        // 自动计算题目数量（包括已选但未保存的题目）
        const sectionQuestions = this.questionList.filter(q => {
          const qSectionId = q.sectionId !== undefined && q.sectionId !== null ? q.sectionId : null
          return qSectionId === section.id
        })
        const questionCount = sectionQuestions.length

        // 每题分值优先使用大题设置的，如果没有则使用第一个题目的分值
        let scorePerQuestion = section.scorePerQuestion || 0
        if (scorePerQuestion === 0 && sectionQuestions.length > 0) {
          scorePerQuestion = sectionQuestions[0].score || 0
        }
        // 大题总分 = 每题分值 × 题目数量
        const totalScore = parseFloat((scorePerQuestion * questionCount).toFixed(2))

        this.sectionForm = {
          id: section.id,
          paperId: this.paperId,
          volumeId: section.volumeId,
          volumeCode: section.volumeCode,
          sectionName: section.sectionName,
          sectionOrder: section.sectionOrder,
          questionCount: questionCount,
          totalScore: totalScore,
          scorePerQuestion: scorePerQuestion,
          instructionText: section.instructionText,
          answerTime: section.answerTime || 5,
          audioPlayCount: section.audioPlayCount || 1
        }

        // 加载题目列表（确保显示所有已选题目，包括未保存的）
        this.loadSectionQuestions(section.id)

        // 加载音频文件列表 - 使用辅助方法获取预览URL
        const audio = this.sectionAudioMap[section.id]
        if (audio && audio.url) {
          this.sectionAudioUrl = audio.url // 设置 OssUpload 的 v-model
          this.updateSectionAudioFileList(audio.url)
        } else {
          this.sectionAudioFileList = []
          this.sectionAudioUrl = null
        }
      }
      this.sectionEditTab = 'info'
      this.sectionDialogVisible = true
    },

    /** 更新大题表单中的每题分值和总分 */
    updateSectionFormScore() {
      if (!this.currentSectionId) return

      // 先获取当前大题下的所有题目（包括已选但未保存的题目），确保题目不会被清空
      const sectionQuestions = this.questionList.filter(q => {
        const qSectionId = q.sectionId !== undefined && q.sectionId !== null ? q.sectionId : null
        return qSectionId === this.currentSectionId
      })

      // 大题总分 = 每题分值 × 题目数量（不清空题目，只重新计算）
      const scorePerQuestion = this.sectionForm.scorePerQuestion || 0
      const totalScore = parseFloat((scorePerQuestion * sectionQuestions.length).toFixed(2))

      this.sectionForm.totalScore = totalScore
      this.sectionForm.questionCount = sectionQuestions.length

      // 如果设置了每题分值，更新所有题目的分值（不清空题目）
      if (scorePerQuestion > 0 && sectionQuestions.length > 0) {
        // 更新完整列表中的题目分值
        sectionQuestions.forEach(q => {
          q.score = scorePerQuestion
        })
        // 重新加载当前页显示的题目列表，确保显示最新的题目和分值
        this.loadSectionQuestions(this.currentSectionId)
      }

      // 如果开启了自动计算总分，实时计算所有大题的总分之和
      if (this.autoCalculateTotalScore) {
        this.calculateAndEmitTotalScore()
      }
    },

    /** 每题分值变化时，更新大题总分和所有题目的分值 */
    handleScorePerQuestionChange(value) {
      if (!this.currentSectionId) return

      // 先获取当前大题下的所有题目（包括已选但未保存的题目），确保题目不会被清空
      const sectionQuestions = this.questionList.filter(q => {
        const qSectionId = q.sectionId !== undefined && q.sectionId !== null ? q.sectionId : null
        return qSectionId === this.currentSectionId
      })

      // 更新大题总分（不清空题目，只重新计算）
      const questionCount = sectionQuestions.length
      this.sectionForm.questionCount = questionCount
      this.sectionForm.totalScore = parseFloat((value * questionCount).toFixed(2))

      // 如果是在编辑大题时，更新所有题目的分值（不清空题目）
      if (value > 0 && sectionQuestions.length > 0) {
        // 更新完整列表中的题目分值
        sectionQuestions.forEach(q => {
          q.score = value
        })
        // 重新加载当前页显示的题目列表，确保显示最新的题目和分值
        this.loadSectionQuestions(this.currentSectionId)
      }

      // 如果开启了自动计算总分，实时计算所有大题的总分之和
      if (this.autoCalculateTotalScore) {
        this.calculateAndEmitTotalScore()
      }
    },

    /** 计算所有大题的总分之和并通知父组件 */
    calculateAndEmitTotalScore() {
      // 计算所有大题的总分之和
      const totalScore = this.sectionList.reduce((sum, section) => {
        return sum + (parseFloat(section.totalScore) || 0)
      }, 0)

      // 通知父组件更新总分
      this.$emit('update-total-score', parseFloat(totalScore.toFixed(2)))
    },

    /** 删除大题（仅更新本地数据，不调用后端） */
    handleDeleteSection(row) {
      this.$modal.confirm(`确定要删除大题"${row.sectionName}"吗？删除后该大题下的所有题目也会被删除！`).then(() => {
        // 从本地数据中删除大题
        const sectionIndex = this.sectionList.findIndex(s => s.id === row.sectionId)
        if (sectionIndex >= 0) {
          this.sectionList.splice(sectionIndex, 1)
        }
        // 删除该大题下的所有题目
        this.questionList = this.questionList.filter(q => {
          const qSectionId = q.sectionId !== undefined && q.sectionId !== null ? q.sectionId : null
          return qSectionId !== row.sectionId
        })
        // 更新表格数据
        this.buildTableData()
        // 更新题目数量
        this.updateSectionQuestionCountFromList()
        // 如果开启了自动计算总分，实时计算所有大题的总分之和
        if (this.autoCalculateTotalScore) {
          this.calculateAndEmitTotalScore()
        }
        this.$modal.msgSuccess("删除成功（待保存）")
      }).catch(() => {})
    },

    /** 大题表单提交（仅更新本地数据，不调用后端） */
    handleSectionSubmit() {
      this.$refs.sectionForm.validate(valid => {
        if (valid) {
          // 从完整的 questionList 中获取题目数量
          const sectionQuestions = this.questionList.filter(q => {
            const qSectionId = q.sectionId !== undefined && q.sectionId !== null ? q.sectionId : null
            return qSectionId === this.sectionForm.id
          })
          const questionCount = sectionQuestions.length

          // 大题总分 = 每题分值 × 题目数量
          const scorePerQuestion = this.sectionForm.scorePerQuestion || 0
          const totalScore = parseFloat((scorePerQuestion * questionCount).toFixed(2))

          // 如果大题下有题目，更新所有题目的分值为大题设置的每题分值
          if (scorePerQuestion > 0 && sectionQuestions.length > 0) {
            // 更新当前大题下所有题目的分值
            sectionQuestions.forEach(q => {
              q.score = scorePerQuestion
            })
          }

          // 判断是编辑还是新增：如果 sectionForm.id 是临时ID（以 temp_ 开头）或者 sectionList 中不存在，则是新增
          const isTempId = this.sectionForm.id && String(this.sectionForm.id).startsWith('temp_')
          const existingSection = this.sectionList.find(s => s.id === this.sectionForm.id)
          const isEdit = !isTempId && existingSection

          if (isEdit) {
            // 编辑：更新本地数据
            if (existingSection) {
              existingSection.sectionName = this.sectionForm.sectionName
              existingSection.sectionOrder = this.sectionForm.sectionOrder
              existingSection.instructionText = this.sectionForm.instructionText
              existingSection.answerTime = this.sectionForm.answerTime || 5
              existingSection.questionCount = questionCount
          existingSection.totalScore = totalScore
          existingSection.scorePerQuestion = scorePerQuestion
          existingSection.audioPlayCount = this.sectionForm.audioPlayCount || 1
            }
            this.$modal.msgSuccess("修改成功（待保存）")
          } else {
            // 新增：添加到本地数据（使用已有的临时ID）
            const newSection = {
              id: this.sectionForm.id,  // ✅ 使用已有的临时ID,确保音频缓存能正确关联
              paperId: this.paperId,
              volumeId: this.sectionForm.volumeId,
              volumeCode: this.sectionForm.volumeCode,
              sectionName: this.sectionForm.sectionName,
              sectionOrder: this.sectionForm.sectionOrder,
              instructionText: this.sectionForm.instructionText,
              answerTime: this.sectionForm.answerTime || 5,
              audioPlayCount: this.sectionForm.audioPlayCount || 1,
              questionCount: questionCount,
              totalScore: totalScore,
              scorePerQuestion: scorePerQuestion
            }
            // 直接添加到 sectionList，不使用临时ID方案
            this.sectionList.push(newSection)

            // 保存需要展开的卷别ID
            const volumeId = this.sectionForm.volumeId

            // 立即更新表格数据
            this.buildTableData()

            // 确保对应的卷别是展开的
            if (volumeId) {
              // 确保类型匹配
              const volumeIdStr = String(volumeId)
              const isExpanded = this.expandedVolumes.some(id => String(id) === volumeIdStr)
              if (!isExpanded) {
                this.expandedVolumes = [...this.expandedVolumes, volumeId]
              }
            }

            this.$modal.msgSuccess("新增成功（待保存）")
          }

          // 更新表格显示
          this.updateTableDataQuestionCount()
          // 如果开启了自动计算总分，实时计算所有大题的总分之和
          if (this.autoCalculateTotalScore) {
            this.calculateAndEmitTotalScore()
          }
          this.sectionDialogVisible = false
        }
      })
    },

    /** 加载大题题目列表（带分页） */
    loadSectionQuestions(sectionId) {
      if (!sectionId) {
        sectionId = this.currentSectionId
      }
      if (!sectionId) return

      // 确保使用正确的 sectionId 过滤（必须严格匹配，不能使用默认值）
      const allQuestions = this.questionList
        .filter(q => {
          // 严格匹配：题目必须有 sectionId，且必须等于传入的 sectionId
          const qSectionId = q.sectionId !== undefined && q.sectionId !== null ? q.sectionId : null
          return qSectionId === sectionId
        })
        .map((q, index) => ({
          questionId: q.questionId,
          id: q.questionId,
          title: q.title || '暂无标题',
          type: q.type,
          subjectId: q.subjectId,
          score: parseFloat(q.score) || 0,
          sectionOrder: q.sectionOrder || (index + 1),
          sectionId: q.sectionId || sectionId
        }))
        .sort((a, b) => (a.sectionOrder || 0) - (b.sectionOrder || 0))

      // 更新总数
      this.sectionQuestionsTotal = allQuestions.length

      // 分页处理
      const { pageNum, pageSize } = this.sectionQuestionsQueryParams
      const start = (pageNum - 1) * pageSize
      const end = start + pageSize
      this.sectionQuestions = allQuestions.slice(start, end)
    },

    /** 管理题目（独立弹窗） */
    handleManageQuestions(sectionRow) {
      this.questionManageSectionId = sectionRow.sectionId
      this.questionManageSectionName = sectionRow.sectionName
      //this.questionManageTitle = `管理题目 - ${sectionRow.sectionName}`
      this.currentSectionId = sectionRow.sectionId
      this.loadSectionQuestions(sectionRow.sectionId)
      this.questionManageVisible = true
    },

    /** 删除题目 */
    handleDeleteQuestion(row) {
      // 从大题中删除题目
      const section = this.sectionList.find(s => {
        const sectionQuestions = this.questionList.filter(q => q.sectionId === s.id)
        return sectionQuestions.some(q => q.questionId === row.questionId)
      })

      if (section) {
        this.currentSectionId = section.id
        this.loadSectionQuestions(section.id)
        const index = this.sectionQuestions.findIndex(q => q.questionId === row.questionId)
        if (index >= 0) {
          this.handleRemoveQuestion(index)
          this.handleSaveQuestions()
        }
      }
    },

    /** 选择题库题目（独立弹窗） */
    handleSelectQuestions() {
      this.questionSelectVisible = true
      // 恢复已选择的题目（从所有题目中恢复，不仅仅是当前页）
      const allSectionQuestions = this.questionList
        .filter(q => {
          const qSectionId = q.sectionId !== undefined && q.sectionId !== null ? q.sectionId : null
          return qSectionId === this.currentSectionId
        })
      this.questionSelectSelectedIds = allSectionQuestions.map(q => q.questionId)
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
      if (!this.currentSectionId) return

      // 获取所有已选题目（包括所有页的）
      const allSectionQuestions = this.questionList
        .filter(q => {
          const qSectionId = q.sectionId !== undefined && q.sectionId !== null ? q.sectionId : null
          return qSectionId === this.currentSectionId
        })
      const existingIds = allSectionQuestions.map(q => q.questionId)

      // 获取大题设置的每题分值（如果是在编辑大题时，使用表单中的分值；否则使用已选题目中的分值或大题设置的分值）
      let defaultScore = 0
      if (this.sectionDialogVisible && this.sectionForm.scorePerQuestion > 0) {
        // 使用大题表单中设置的每题分值
        defaultScore = this.sectionForm.scorePerQuestion
      } else if (allSectionQuestions.length > 0) {
        // 使用第一个已选题目的分值
        defaultScore = allSectionQuestions[0].score || 0
      } else {
        // 如果没有已选题目，使用大题设置的每题分值（如果有）
        const section = this.sectionList.find(s => s.id === this.currentSectionId)
        defaultScore = section?.scorePerQuestion || 0
      }

      // 合并新选择的题目到已选列表（避免重复）
      const newQuestions = this.questionSelectSelectedRows
        .filter(row => !existingIds.includes(row.id))
        .map((row, index) => ({
          questionId: row.id,
          id: row.id,
          title: row.title,
          type: row.type,
          subjectId: row.subjectId,
          score: defaultScore, // 使用大题设置的每题分值
          sectionOrder: allSectionQuestions.length + index + 1,
          sectionId: this.currentSectionId
        }))

      // 更新已存在的题目信息（更新完整列表）
      this.questionSelectSelectedRows.forEach(row => {
        const existingIndex = this.questionList.findIndex(q => q.questionId === row.id && q.sectionId === this.currentSectionId)
        if (existingIndex >= 0) {
          this.$set(this.questionList[existingIndex], 'title', row.title)
          this.$set(this.questionList[existingIndex], 'type', row.type)
          this.$set(this.questionList[existingIndex], 'subjectId', row.subjectId)
        }
      })

      // 添加新题目到完整列表
      this.questionList.push(...newQuestions)

      // 重新加载当前页的题目列表
      this.loadSectionQuestions(this.currentSectionId)

      // 更新大题和试卷的题目数量
      this.updateSectionQuestionCountFromList()
      this.updatePaperTotalQuestions()

      // 如果是在编辑大题时添加题目，使用大题设置的每题分值更新新题目的分值，并更新大题总分
      if (this.sectionDialogVisible && this.sectionForm.id) {
        const scorePerQuestion = this.sectionForm.scorePerQuestion || 0
        if (scorePerQuestion > 0) {
          // 更新新添加题目的分值
          newQuestions.forEach(q => {
            q.score = scorePerQuestion
          })
        }
        this.updateSectionFormScore()
      } else if (this.autoCalculateTotalScore) {
        // 如果开启了自动计算总分，实时计算所有大题的总分之和
        this.calculateAndEmitTotalScore()
      }

      // 关闭弹窗
      this.questionSelectVisible = false

      // 清空选择状态
      this.$refs.questionSelectTable && this.$refs.questionSelectTable.clearSelection()
      this.questionSelectSelectedRows = []
      this.questionSelectSelectedIds = []
    },

    /** 计算默认分值 */
    calculateDefaultScore(question) {
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
      // 从当前页移除
      const removedQuestion = this.sectionQuestions[index]
      this.sectionQuestions.splice(index, 1)

      // 从完整列表中移除
      const allIndex = this.questionList.findIndex(q => q.questionId === removedQuestion.questionId && q.sectionId === this.currentSectionId)
      if (allIndex >= 0) {
        this.questionList.splice(allIndex, 1)
      }

      // 重新加载当前页（因为总数变了）
      this.loadSectionQuestions(this.currentSectionId)

      // 更新大题和试卷的题目数量
      this.updateSectionQuestionCountFromList()
      this.updatePaperTotalQuestions()

      // 如果是在编辑大题时删除题目，更新大题表单中的每题分值和总分
      if (this.sectionDialogVisible && this.sectionForm.id) {
        this.updateSectionFormScore()
      } else if (this.autoCalculateTotalScore) {
        // 如果开启了自动计算总分，实时计算所有大题的总分之和
        this.calculateAndEmitTotalScore()
      }
    },

    /** 题目列表选择变化 */
    handleSectionQuestionSelectionChange(selection) {
      this.sectionQuestionSelectedRows = selection
    },

    /** 批量设置分值 */
    handleBatchSetScore() {
      if (this.sectionQuestionSelectedRows.length === 0) {
        this.$modal.msgWarning("请先选择要设置分值的题目")
        return
      }
      this.batchScore = 0
      this.batchSetScoreVisible = true
    },

    /** 批量设置分值确认 */
    handleBatchSetScoreConfirm() {
      if (this.batchScore <= 0) {
        this.$modal.msgWarning("分值必须大于0")
        return
      }

      // 更新选中题目的分值（包括所有页的）
      const selectedQuestionIds = this.sectionQuestionSelectedRows.map(row => row.questionId)
      this.questionList.forEach(q => {
        if (selectedQuestionIds.includes(q.questionId) && q.sectionId === this.currentSectionId) {
          q.score = this.batchScore
        }
      })

      // 更新当前页显示
      this.sectionQuestions.forEach(q => {
        if (selectedQuestionIds.includes(q.questionId)) {
          q.score = this.batchScore
        }
      })

      this.batchSetScoreVisible = false
      this.$modal.msgSuccess(`已为 ${selectedQuestionIds.length} 题设置分值为 ${this.batchScore} 分`)
    },

    /** 清空全部题目 */
    handleClearAllQuestions() {
      this.$modal.confirm('是否确认清空该大题下的所有题目？').then(() => {
        // 从完整列表中移除该大题下的所有题目
        this.questionList = this.questionList.filter(q => {
          const qSectionId = q.sectionId !== undefined && q.sectionId !== null ? q.sectionId : null
          return qSectionId !== this.currentSectionId
        })
        // 重新加载当前页
        this.loadSectionQuestions(this.currentSectionId)

        // 更新大题和试卷的题目数量
        this.updateSectionQuestionCountFromList()
        this.updatePaperTotalQuestions()

        // 如果开启了自动计算总分，实时计算所有大题的总分之和
        if (this.autoCalculateTotalScore) {
          this.calculateAndEmitTotalScore()
        }
      }).catch(() => {})
    },

    /** 保存题目（仅更新本地数据，不调用后端） */
    handleSaveQuestions() {
      if (!this.currentSectionId) return

      // 获取所有题目（不仅仅是当前页）
      const allSectionQuestions = this.questionList
        .filter(q => {
          const qSectionId = q.sectionId !== undefined && q.sectionId !== null ? q.sectionId : null
          return qSectionId === this.currentSectionId
        })
        .sort((a, b) => (a.sectionOrder || 0) - (b.sectionOrder || 0))

      // 更新题目的 sectionOrder
      allSectionQuestions.forEach((q, index) => {
        q.sectionOrder = index + 1
      })

      // 更新大题的题目数量
      this.updateSectionQuestionCountFromList()
      // 更新表格显示
      this.updateTableDataQuestionCount()
      // 如果开启了自动计算总分，实时计算所有大题的总分之和
      if (this.autoCalculateTotalScore) {
        this.calculateAndEmitTotalScore()
      }

      this.$modal.msgSuccess("保存成功（待保存）")
      this.questionManageVisible = false
    },

    /** 更新大题的题目数量（已废弃，使用 updateSectionQuestionCountFromList） */
    updateSectionQuestionCount(sectionId, questionCount) {
      // 此方法已废弃，不再调用后端API
      // 直接更新本地数据
      const section = this.sectionList.find(s => s.id === sectionId)
      if (section) {
        section.questionCount = questionCount
        this.updateTableDataQuestionCount()
      }
    },

    /** 从题目列表更新大题的题目数量（不调用后端，仅更新本地显示） */
    updateSectionQuestionCountFromList() {
      // 更新所有大题的题目数量
      this.sectionList.forEach(section => {
        const count = this.questionList.filter(q => {
          const qSectionId = q.sectionId !== undefined && q.sectionId !== null ? q.sectionId : null
          return qSectionId === section.id
        }).length
        section.questionCount = count
      })
      // 更新表格显示
      this.updateTableDataQuestionCount()
    },

    /** 更新表格数据中的题目数量显示 */
    updateTableDataQuestionCount() {
      // 更新 flatTableData 中的题目数量
      this.flatTableData.forEach(row => {
        if (row.type === 'section') {
          const section = this.sectionList.find(s => s.id === row.sectionId)
          if (section) {
            row.questionCount = section.questionCount || 0
          }
        } else if (row.type === 'volume') {
          // 重新计算卷别的题目数量
          const volumeSections = this.sectionList.filter(s => s.volumeId === row.volumeId)
          const volumeQuestionCount = volumeSections.reduce((sum, s) => sum + (s.questionCount || 0), 0)
          row.questionCount = volumeQuestionCount
          row.sectionCount = volumeSections.length
        }
      })

      // 如果开启了自动计算总分，实时计算所有大题的总分之和
      if (this.autoCalculateTotalScore) {
        this.calculateAndEmitTotalScore()
      }
    },

    /** 更新试卷的题目总数（仅更新本地数据，不调用后端） */
    updatePaperTotalQuestions() {
      // 此方法不再调用后端API，仅用于计算
      // 实际的更新将在父组件保存时统一提交
      const totalQuestions = this.questionList.length
      // 通过事件通知父组件更新 totalQuestions
      this.$emit('update-total-questions', totalQuestions)
    },

    /** 获取所有待保存的数据（供父组件调用） */
    getStructureData() {
      // 分离新增和编辑的卷别
      const volumesToAdd = []
      const volumesToUpdate = []
      this.volumeList.forEach(volume => {
        // 获取缓存的音频URL（如果存在）
        const audioInfo = this.volumeAudioMap[volume.id]
        const volumeData = {
          paperId: this.paperId,
          volumeName: volume.volumeName,
          volumeOrder: volume.volumeOrder
        }
        // 如果有缓存的音频URL，添加到数据中
        if (audioInfo && audioInfo.url) {
          volumeData.volumeAudioUrl = audioInfo.url
          volumeData.volumeAudioPath = audioInfo.path || audioInfo.url
          // 如果有音频时长，也添加到数据中
          if (audioInfo.duration !== null && audioInfo.duration !== undefined) {
            volumeData.volumeAudioDuration = audioInfo.duration
          }
        }

        if (volume.id && volume.id.toString().startsWith('temp_')) {
          // 新增的卷别（临时ID）
          volumeData.tempId = volume.id  // ✅ 添加 tempId
          volumesToAdd.push(volumeData)
        } else {
          // 编辑的卷别
          volumeData.id = volume.id
          volumesToUpdate.push(volumeData)
        }
      })

      // 分离新增和编辑的大题
      const sectionsToAdd = []
      const sectionsToUpdate = []
      this.sectionList.forEach(section => {
        // 获取缓存的音频URL（如果存在）
        const audioInfo = this.sectionAudioMap[section.id]
        const sectionData = {
          paperId: this.paperId,
          volumeId: section.volumeId,
          volumeCode: section.volumeCode,
          sectionName: section.sectionName,
          sectionOrder: section.sectionOrder,
          instructionText: section.instructionText,
          questionCount: section.questionCount,
          totalScore: section.totalScore,
          scorePerQuestion: section.scorePerQuestion,
          answerTime: section.answerTime || 5,
          audioPlayCount: section.audioPlayCount || 1
        }
        // 如果有缓存的音频URL，添加到数据中
        if (audioInfo && audioInfo.url) {
          sectionData.instructionAudioUrl = audioInfo.url
          sectionData.instructionAudioPath = audioInfo.path || audioInfo.url
          // 如果有音频时长，也添加到数据中
          if (audioInfo.duration !== null && audioInfo.duration !== undefined) {
            sectionData.instructionAudioDuration = audioInfo.duration
          }
        }

        if (section.id && section.id.toString().startsWith('temp_')) {
          // 新增的大题（临时ID）
          sectionData.tempId = section.id  // ✅ 添加 tempId
          // 如果 volumeId 也是临时ID，添加 volumeTempId
          if (section.volumeId && section.volumeId.toString().startsWith('temp_')) {
            sectionData.volumeTempId = section.volumeId
            delete sectionData.volumeId  // 删除临时的 volumeId
          }
          sectionsToAdd.push(sectionData)
        } else {
          // 编辑的大题
          sectionData.id = section.id
          // ✅ 编辑模式下,如果 volumeId 是临时ID,也需要添加 volumeTempId
          if (section.volumeId && section.volumeId.toString().startsWith('temp_')) {
            sectionData.volumeTempId = section.volumeId
            delete sectionData.volumeId  // 删除临时的 volumeId
          }
          sectionsToUpdate.push(sectionData)
        }
      })

      // 构建嵌套结构：volumes -> sections -> questions
      // 1. 按大题分组题目（使用字符串 key 确保类型一致）
      const questionsBySection = {}
      this.questionList.forEach(q => {
        const sectionId = q.sectionId !== undefined && q.sectionId !== null ? q.sectionId : null
        if (sectionId) {
          // 统一转换为字符串作为 key，确保匹配
          const sectionKey = String(sectionId)
          if (!questionsBySection[sectionKey]) {
            questionsBySection[sectionKey] = []
          }
          const questionData = {
            paperId: this.paperId,
            questionId: q.questionId,
            sectionOrder: q.sectionOrder || 0,
            score: q.score || 0
          }
          questionsBySection[sectionKey].push(questionData)
        }
      })

      // 2. 合并所有 volumes 和 sections
      const allVolumes = [...volumesToAdd, ...volumesToUpdate]
      const allSections = [...sectionsToAdd, ...sectionsToUpdate]

      // 3. 按 volumeId 或 volumeTempId 分组 sections
      const sectionsByVolume = {}
      allSections.forEach(section => {
        // 确定 volume 的 key（优先使用 volumeId，否则使用 volumeTempId）
        const volumeKey = section.volumeId || section.volumeTempId
        if (volumeKey) {
          // 统一转换为字符串作为 key
          const volumeKeyStr = String(volumeKey)
          if (!sectionsByVolume[volumeKeyStr]) {
            sectionsByVolume[volumeKeyStr] = []
          }
          // 为每个 section 添加其 questions（使用字符串 key 匹配）
          const sectionId = section.id || section.tempId
          const sectionKey = sectionId ? String(sectionId) : null
          section.questions = sectionKey ? (questionsBySection[sectionKey] || []) : []
          sectionsByVolume[volumeKeyStr].push(section)
        }
      })

      // 4. 将 sections 嵌套到对应的 volumes 中
      allVolumes.forEach(volume => {
        // 确定 volume 的 key（优先使用 id，否则使用 tempId），统一转换为字符串
        const volumeKey = volume.id || volume.tempId
        const volumeKeyStr = volumeKey ? String(volumeKey) : null
        volume.sections = volumeKeyStr ? (sectionsByVolume[volumeKeyStr] || []) : []
      })

      // 调试日志：检查嵌套结构
      console.log('getStructureData - 嵌套结构:', {
        volumesCount: allVolumes.length,
        volumes: allVolumes.map(v => ({
          id: v.id,
          tempId: v.tempId,
          volumeName: v.volumeName,
          sectionsCount: v.sections ? v.sections.length : 0,
          sections: v.sections ? v.sections.map(s => ({
            id: s.id,
            tempId: s.tempId,
            sectionName: s.sectionName,
            questionsCount: s.questions ? s.questions.length : 0
          })) : []
        })),
        totalQuestions: this.questionList.length
      })

      return {
        volumes: allVolumes, // 嵌套结构：volumes -> sections -> questions
        totalQuestions: this.questionList.length
      }
    },

    /** 卷别音频文件变化处理（OssUpload组件回调） */
    handleVolumeAudioChange(urlOrData) {
      // OssUpload 组件上传成功后，会通过 @change 事件传递文件URL或包含url和duration的对象
      if (urlOrData) {
        const url = typeof urlOrData === 'object' ? urlOrData.url : urlOrData
        const duration = typeof urlOrData === 'object' ? urlOrData.duration : null
        const volumeId = this.volumeForm.id || 'temp_' + Date.now()
        // 更新 volumeAudioUrl，确保 OssUpload 组件能正确回显
        this.volumeAudioUrl = url
        // 缓存音频URL、路径和时长（用于后续保存）
        this.$set(this.volumeAudioMap, volumeId, {
          url: url,
          path: url, // OssUpload 返回的是完整URL，path 也使用 URL
          duration: duration // 音频时长（秒）
        })
        // 更新文件列表用于显示
        this.updateVolumeAudioFileList(url)
      } else {
        // URL 为空表示移除
        this.handleVolumeAudioRemove()
      }
    },

    /** 卷别音频移除（OssUpload组件回调） */
    handleVolumeAudioRemove() {
      this.handleUploadProgress('volumeAudio', false)
      this.volumeAudioUrl = null
      const volumeId = this.volumeForm.id || 'temp_' + Date.now()
      this.$set(this.volumeAudioMap, volumeId, null)
      this.volumeAudioFileList = []
    },

    /** 卷别音频超出文件限制处理（用于二次上传替换） */
    handleVolumeAudioExceed(files, fileList) {
      // 当用户尝试上传新文件但已达到limit时,清空旧文件列表并手动添加新文件
      this.$modal.msgWarning('将替换现有音频文件')
      // 清空旧文件列表
      this.volumeAudioFileList = []
      // 等待DOM更新后,手动触发文件选择
      this.$nextTick(() => {
        // 手动添加新文件到上传列表
        if (files && files.length > 0) {
          const file = files[0]
          this.$refs.volumeAudioUpload.handleStart(file)
          this.$refs.volumeAudioUpload.submit()
        }
      })
    },

    /** 大题音频超出文件限制处理（用于二次上传替换） */
    handleSectionAudioExceed(files, fileList) {
      // 当用户尝试上传新文件但已达到limit时,清空旧文件列表并手动添加新文件
      this.$modal.msgWarning('将替换现有音频文件')
      // 清空旧文件列表
      this.sectionAudioFileList = []
      // 等待DOM更新后,手动触发文件选择
      this.$nextTick(() => {
        // 手动添加新文件到上传列表
        if (files && files.length > 0) {
          const file = files[0]
          this.$refs.sectionAudioUpload.handleStart(file)
          this.$refs.sectionAudioUpload.submit()
        }
      })
    },

    /** 大题音频文件变化处理（OssUpload组件回调） */
    handleSectionAudioChange(urlOrData) {
      // OssUpload 组件上传成功后，会通过 @change 事件传递文件URL或包含url和duration的对象
      if (urlOrData) {
        const url = typeof urlOrData === 'object' ? urlOrData.url : urlOrData
        const duration = typeof urlOrData === 'object' ? urlOrData.duration : null
        const sectionId = this.sectionForm.id || 'temp_' + Date.now()
        // 更新 sectionAudioUrl，确保 OssUpload 组件能正确回显
        this.sectionAudioUrl = url
        // 缓存音频URL、路径和时长（用于后续保存）
        this.$set(this.sectionAudioMap, sectionId, {
          url: url,
          path: url, // OssUpload 返回的是完整URL，path 也使用 URL
          duration: duration // 音频时长（秒）
        })
        // 更新文件列表用于显示
        this.updateSectionAudioFileList(url)
      } else {
        // URL 为空表示移除
        this.handleSectionAudioRemove()
      }
    },

    /** 大题音频移除（OssUpload组件回调） */
    handleSectionAudioRemove() {
      this.handleUploadProgress('sectionAudio', false)
      this.sectionAudioUrl = null
      const sectionId = this.sectionForm.id || 'temp_' + Date.now()
      this.$set(this.sectionAudioMap, sectionId, null)
      this.sectionAudioFileList = []
    },
    /** 处理上传进度 */
    handleUploadProgress(type, isUploading) {
      this.$set(this.uploadStatus, type, isUploading)
    },
    /** 检查是否有正在上传的文件 */
    checkUploading() {
      // 检查本组件的上传状态
      const hasUploading = Object.values(this.uploadStatus).some(status => status === true)

      // 检查 Element UI 上传组件的内部状态
      if (this.$refs.volumeAudioUpload) {
        const volumeAudioFiles = this.$refs.volumeAudioUpload.uploadFiles || []
        if (volumeAudioFiles.some(file => file.status === 'uploading')) {
          this.handleUploadProgress('volumeAudio', true)
          return true
        }
      }

      if (this.$refs.sectionAudioUpload) {
        const sectionAudioFiles = this.$refs.sectionAudioUpload.uploadFiles || []
        if (sectionAudioFiles.some(file => file.status === 'uploading')) {
          this.handleUploadProgress('sectionAudio', true)
          return true
        }
      }

      if (hasUploading) {
        return true
      }

      return false
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
    },

    /** 切换卷别展开/折叠 */
    toggleVolumeExpand(volumeId) {
      const index = this.expandedVolumes.indexOf(volumeId)
      if (index >= 0) {
        this.expandedVolumes.splice(index, 1)
      } else {
        this.expandedVolumes.push(volumeId)
      }
    },

    /** 判断卷别是否展开 */
    isVolumeExpanded(volumeId) {
      return this.expandedVolumes.includes(volumeId)
    }
  }
}
</script>

<style scoped>
.paper-structure-table {
  padding: 20px;
  background: #fff;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 1px solid #e4e7ed;
}

.audio-player-footer {
  padding: 10px 20px 20px;
  text-align: right;
  border-top: 1px solid #e4e7ed;
}

.upload-file-name {
  color: #409EFF;
  text-decoration: underline;
  cursor: pointer;
  margin-right: 6px;
}

.upload-file-remove {
  margin-left: 6px;
  cursor: pointer;
  color: #c0c4cc;
}

.upload-file-remove:hover {
  color: #f56c6c;
}

.table-title {
  font-weight: bold;
  font-size: 16px;
}

.header-right {
  display: flex;
  gap: 10px;
}

.question-list-panel {
  min-height: 300px;
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

.question-manage-container {
  min-height: 300px;
}

.question-manage-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 1px solid #e4e7ed;
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

