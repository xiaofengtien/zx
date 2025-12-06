<template>
  <div class="app-container">
    <el-row :gutter="20">
      <splitpanes :horizontal="this.$store.getters.device === 'mobile'" class="default-theme">
        <!--分类树-->
        <pane size="16">
          <el-col>
            <div class="head-container">
              <el-input v-model="categoryName" placeholder="请输入分类名称" clearable size="small" prefix-icon="el-icon-search" style="margin-bottom: 20px" />
            </div>
            <div class="head-container">
              <el-tree
                :data="categoryTreeData"
                :props="defaultProps"
                :expand-on-click-node="false"
                :filter-node-method="filterNode"
                ref="categoryTree"
                node-key="id"
                default-expand-all
                highlight-current
                :current-node-key="selectedCategoryId"
                @node-click="handleCategoryNodeClick"
              />
            </div>
          </el-col>
        </pane>
        <!--题目列表-->
        <pane size="84">
          <el-col>
            <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="题目标题" prop="title">
        <el-input
          v-model="queryParams.title"
          placeholder="请输入题目标题"
          clearable
          style="width: 240px"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="题目类型" prop="type">
        <el-select v-model="queryParams.type" placeholder="请选择题目类型" clearable style="width: 240px">
          <el-option
            v-for="dict in (dict.type.question_type || [])"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="学科" prop="subjectId">
        <el-select v-model="queryParams.subjectId" placeholder="请选择学科" clearable style="width: 240px">
          <el-option
            v-for="dict in (dict.type.subject || [])"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['question:question:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['question:question:remove']"
        >删除</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table
      v-loading="loading"
      :data="questionList"
      @selection-change="handleSelectionChange"
      style="width: 100%"
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="题目标题" prop="title" min-width="160" show-overflow-tooltip />
      <el-table-column label="题目分类" prop="questionCategoryId" width="120" align="center" show-overflow-tooltip>
        <template slot-scope="scope">
          {{ getCategoryName(scope.row.questionCategoryId) }}
        </template>
      </el-table-column>
      <el-table-column label="题目类型" prop="type" width="120" align="center">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.question_type || []" :value="scope.row.type" />
        </template>
      </el-table-column>
      <el-table-column label="学科" prop="subjectName" width="120" align="center" show-overflow-tooltip />
      <el-table-column label="权重" prop="weight" width="80" align="center" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="200" fixed="right">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-view"
            @click="handleView(scope.row)"
            v-hasPermi="['question:question:query']"
          >查看</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['question:question:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['question:question:remove']"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />
          </el-col>
        </pane>
      </splitpanes>
    </el-row>

    <!-- 添加或修改题目对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="50%" append-to-body class="question-dialog">
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="题目标题" prop="title">
          <el-input
            v-model="form.title"
            type="textarea"
            :rows="3"
            class="question-input-right-align"
            placeholder="请输入题目标题"
          />
        </el-form-item>
        <!-- 题目类型 - 移到题目标题下面，单独一行 -->
        <el-form-item label="题目类型" prop="mediaType">
          <el-radio-group v-model="form.mediaType" @change="handleMediaTypeChange">
            <el-radio :label="1">文本</el-radio>
            <el-radio :label="3">音频</el-radio>
          </el-radio-group>
          <div v-if="form.mediaType === 3" style="margin-top: 10px;">
            <oss-upload
              ref="mediaUpload"
              v-model="form.mediaUrl"
              :limit="1"
              accept=".mp3,.wav,.ogg,.m4a,.aac"
              :file-size="10"
              tip="只能上传音频文件，且不超过10MB，仅支持一个文件，二次上传将直接替换"
              path-prefix="exam/question"
              list-type="text"
              @change="handleMediaUrlChange"
              @progress="(event, file) => handleUploadProgress('media', event.percent < 100)"
              @preview="handleMediaPreview"
            />
            <div v-if="uploadStatus.media" style="color: #E6A23C; font-size: 12px; margin-top: 5px;">
              请先等待上传完成
            </div>
          </div>
        </el-form-item>
        <el-form-item label="权重" prop="weight">
          <el-input-number v-model="form.weight" controls-position="right" :min="1" :max="100" class="question-input-right-align" style="width: 80px;" />
        </el-form-item>
        <el-row :gutter="20" class="subject-type-row">
          <el-col :span="12">
            <el-form-item label="学科" prop="subjectId">
              <el-select v-model="form.subjectId" placeholder="请选择学科" style="width: 100%">
                <el-option
                  v-for="dict in (dict.type.subject || [])"
                  :key="dict.value"
                  :label="dict.label"
                  :value="parseInt(dict.value)"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="题目类型" prop="type" class="question-type-form-item">
              <el-select v-model="form.type" placeholder="请选择题目类型" style="width: 100%" @change="handleQuestionTypeChange">
                <el-option
                  v-for="dict in (dict.type.question_type || [])"
                  :key="dict.value"
                  :label="dict.label"
                  :value="parseInt(dict.value)"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <!-- 选项类型 - 单独一行 -->
        <el-form-item v-if="showOptionType" label="选项类型" prop="optionType">
          <el-radio-group v-model="form.optionType" @change="handleOptionTypeChange">
            <el-radio :label="1">文本</el-radio>
            <el-radio :label="3">音频</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="答案选项" prop="answers" v-if="showAnswers">
          <div v-for="(answer, index) in form.answers" :key="index" class="answer-option-container question-input-right-align" style="margin-bottom: 15px; padding: 15px 5px 15px 10px; border: 1px solid #e4e7ed; border-radius: 4px; box-sizing: border-box; position: relative;">
            <!-- 第一行：选项名称、选项内容、正确答案、删除 -->
            <div style="display: flex; align-items: center; gap: 10px; position: relative;">
              <div style="flex: 0 0 auto; width: 60px;">
                <el-input v-model="answer.optionName" placeholder="选项" size="small" />
              </div>
              <div style="flex: 1 1 auto; min-width: 0; padding-right: 200px;">
                <el-input v-model="answer.optionContent" placeholder="选项内容" size="small" />
              </div>
              <div style="position: absolute; right: 5px; top: 50%; transform: translateY(-50%); display: flex; align-items: center; gap: 10px; white-space: nowrap;">
                <div style="flex: 0 0 auto;">
                  <el-checkbox v-model="answer.isAnswer" :true-label="1" :false-label="0" @change="handleAnswerChange(answer)" style="margin: 0;">正确答案</el-checkbox>
                </div>
                <div style="flex: 0 0 auto;">
                  <el-button size="small" type="danger" icon="el-icon-delete" circle @click="removeAnswer(index)"></el-button>
                </div>
              </div>
            </div>
            <!-- 第二行：音频上传（显示在选项下方，紧贴选项内容输入框，与题目媒体上传完全一致） -->
            <div v-if="form.optionType === 3" style="margin-top: 10px; padding-left: 70px;">
              <oss-upload
                :ref="`answerUpload${index}`"
                v-model="answer.mediaUrl"
                :limit="1"
                accept=".mp3,.wav,.ogg,.m4a,.aac"
                :file-size="10"
                tip="只能上传音频文件，且不超过10MB，仅支持一个文件，二次上传将直接替换"
                path-prefix="exam/question"
                list-type="text"
                @change="(url) => handleAnswerMediaUrlChange(url, index)"
                @progress="(event, file) => handleUploadProgress(`answer${index}`, event.percent < 100)"
                @preview="(file) => handleAnswerMediaPreview(file, answer)"
              />
              <div v-if="uploadStatus[`answer${index}`]" style="color: #E6A23C; font-size: 12px; margin-top: 5px;">
                请先等待上传完成
              </div>
            </div>
          </div>
          <el-button size="small" type="primary" icon="el-icon-plus" @click="addAnswer">添加选项</el-button>
        </el-form-item>
        <el-form-item label="解析" prop="analyzes">
          <el-input v-model="form.analyzes" type="textarea" :rows="3" class="question-input-right-align" placeholder="请输入题目解析" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer question-dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 媒体文件预览对话框 -->
    <el-dialog
      :title="mediaPreviewTitle"
      :visible.sync="mediaPreviewVisible"
      width="800px"
      append-to-body
      v-if="mediaPreviewType !== 'audio'"
    >
      <div v-if="mediaPreviewType === 'video'" style="text-align: center;">
        <video :src="mediaPreviewUrl" controls style="width: 100%; max-width: 700px; max-height: 500px;"></video>
      </div>
      <div v-else-if="mediaPreviewType === 'image'" style="text-align: center;">
        <img :src="mediaPreviewUrl" style="max-width: 100%; max-height: 500px;" />
      </div>
    </el-dialog>

    <!-- 音频全屏遮盖播放器 -->
    <div v-if="audioPlayerVisible" class="audio-player-overlay" @click.self="closeAudioPlayer">
      <div class="audio-player-container" @click.stop>
        <div class="audio-player-header">
          <span class="audio-player-title">{{ audioPlayerTitle }}</span>
          <el-button
            type="text"
            icon="el-icon-close"
            @click="closeAudioPlayer"
            class="audio-player-close"
          ></el-button>
        </div>
        <div class="audio-player-content">
          <audio
            ref="audioPlayer"
            :src="audioPlayerUrl"
            :type="audioPlayerMimeType"
            @timeupdate="handleAudioTimeUpdate"
            @loadedmetadata="handleAudioLoaded"
            @ended="handleAudioEnded"
            style="width: 100%; display: none;"
            controlslist="nodownload nofullscreen noremoteplayback"
            disablePictureInPicture
          ></audio>
          <div class="audio-player-controls-wrapper">
            <div class="audio-player-controls" :class="{ 'volume-slider-visible': volumeSliderVisible }">
              <button
                @click="toggleAudioPlay"
                class="audio-control-btn play-pause-btn"
                type="button"
              >
                <i :class="audioPlaying ? 'el-icon-video-pause' : 'el-icon-video-play'" class="play-pause-icon"></i>
              </button>
              <div class="audio-time-display">
                <span class="audio-time">{{ formatTime(currentTime) }} / {{ formatTime(duration) }}</span>
              </div>
              <div class="audio-progress-container" :class="{ 'progress-disabled': volumeSliderVisible }">
                <el-slider
                  v-model="audioProgress"
                  :max="100"
                  @change="handleAudioProgressChange"
                  :disabled="volumeSliderVisible"
                  class="audio-progress-slider"
                ></el-slider>
              </div>
              <div class="audio-volume-wrapper">
                <button
                  @click="toggleVolumeSlider"
                  class="audio-volume-btn"
                  :class="{ 'volume-btn-active': volumeSliderVisible || audioMuted }"
                  type="button"
                >
                  <i v-if="audioMuted" class="el-icon-turn-off volume-icon"></i>
                  <i v-else class="el-icon-headset volume-icon"></i>
                </button>
                <div
                  v-if="volumeSliderVisible"
                  class="audio-volume-slider-container"
                  @click.stop
                >
                  <div class="audio-volume-slider-wrapper">
                    <el-slider
                      v-model="audioVolume"
                      :max="100"
                      @change="handleAudioVolumeChange"
                      @input="handleAudioVolumeInput"
                      class="audio-volume-slider"
                    ></el-slider>
                    <div
                      v-if="volumeTooltipVisible"
                      class="audio-volume-tooltip"
                    >
                      {{ audioVolume }}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { getQuestionList, getQuestion, addQuestion, updateQuestion, deleteQuestion, uploadQuestionMedia } from "@/api/exam/question"
import { getCategoryTree } from "@/api/exam/questionCategory"
import Treeselect from "@riophae/vue-treeselect"
import "@riophae/vue-treeselect/dist/vue-treeselect.css"
import ImageUpload from "@/components/ImageUpload"
import OssUpload from "@/components/OssUpload"
import { getToken } from "@/utils/auth"
import { encodeUrlFileName as encodeFileNameHelper } from "@/utils/media"
import { Splitpanes, Pane } from "splitpanes"
import "splitpanes/dist/splitpanes.css"

export default {
  name: "Question",
  dicts: ['question_type', 'subject'],
  components: { Treeselect, ImageUpload, OssUpload, Splitpanes, Pane },
  created() {
    // 确保字典加载完成后再加载数据
    this.$nextTick(() => {
      this.getList()
      this.loadCategoryTree()
    })
    // 监听页面点击事件，当点击非分类树区域时，清空选中状态
    document.addEventListener('click', this.handleDocumentClick)
  },
  mounted() {
    // 监听ESC键关闭音频播放器
    this.handleKeydown = (e) => {
      if (e.key === 'Escape' && this.audioPlayerVisible) {
        this.closeAudioPlayer()
      }
    }
    document.addEventListener('keydown', this.handleKeydown)
  },
  beforeDestroy() {
    // 移除事件监听
    document.removeEventListener('click', this.handleDocumentClick)
    // 关闭音频播放器
    this.closeAudioPlayer()
    // 移除键盘事件监听
    if (this.handleKeydown) {
      document.removeEventListener('keydown', this.handleKeydown)
    }
    // 清理音量滑块外部点击监听
    if (this.volumeSliderVisible) {
      document.removeEventListener('click', this.handleClickOutsideVolumeSlider)
    }
  },
  data() {
    return {
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 题目表格数据
      questionList: [],
      // 分类树选项
      categoryOptions: [],
      // 分类树数据（用于左侧树）
      categoryTreeData: [],
      // 分类名称（搜索）
      categoryName: undefined,
      // 选中的分类ID（用于高亮）
      selectedCategoryId: undefined,
      // 默认树属性
      defaultProps: {
        children: "children",
        label: "name",
        disabled: (data) => {
          // 禁用根节点（isDefault为true的节点），只能展开/收起，不能选中
          return data.isDefault === true || data.isDefault === 1
        }
      },
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 是否显示媒体选项（默认隐藏）
      showMediaOptions: false,
      // 是否显示选项媒体选项（默认隐藏）
      showOptionMediaOptions: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        title: undefined,
        categoryId: undefined,
        type: undefined,
        subjectId: undefined
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        subjectId: [
          { required: true, message: "学科不能为空", trigger: "change" }
        ],
        type: [
          { required: true, message: "题目类型不能为空", trigger: "change" }
        ],
        title: [
          { required: true, message: "题目标题不能为空", trigger: "blur" }
        ],
        weight: [
          { required: true, message: "权重不能为空", trigger: "blur" },
          { type: 'number', message: "权重必须为数字", trigger: "blur" }
        ],
        answers: [
          { validator: this.validateAnswers.bind(this), trigger: "change" }
        ]
      },
      // 上传媒体文件URL
      uploadMediaUrl: process.env.VUE_APP_BASE_API + "/question/media/upload",
      // 上传请求头
      uploadHeaders: {
        Authorization: "Bearer " + getToken()
      },
      // 媒体文件预览
      mediaPreviewVisible: false,
      mediaPreviewUrl: '',
      mediaPreviewType: '', // 'image', 'audio', 'video'
      mediaPreviewTitle: '预览',
      // 音频全屏播放器
      audioPlayerVisible: false,
      audioPlayerUrl: '',
      audioPlayerTitle: '',
      audioPlaying: false,
      currentTime: 0,
      duration: 0,
      audioProgress: 0,
      audioVolume: 100,
      audioMuted: false,
      volumeSliderVisible: false,
      volumeSliderTimer: null,
      volumeTooltipVisible: false,
      volumeTooltipTimer: null,
      handleKeydown: null,
      // 上传状态
      uploadStatus: {
        media: false
      }
    }
  },
  computed: {
    // 是否显示选项类型
    showOptionType() {
      // 修复：当 type 为 0 时，!this.form.type 会返回 true，需要明确判断
      if (this.form.type === undefined || this.form.type === null || this.form.type === '') return false
      // 单选题、多选题、判断题、完形填空需要选项
      return [0, 1, 2, 5].includes(parseInt(this.form.type))
    },
    // 是否显示答案选项
    showAnswers() {
      return this.showOptionType && this.form.answers && this.form.answers.length > 0
    }
  },
  watch: {
    // 监听分类名称搜索
    categoryName(val) {
      this.$refs.categoryTree && this.$refs.categoryTree.filter(val)
    },
    // 监听字典加载完成
    'dict.type.question_type': {
      handler() {
        // 字典加载完成后，确保类型正确
        if (this.queryParams.type !== undefined && typeof this.queryParams.type === 'string') {
          this.queryParams.type = parseInt(this.queryParams.type) || undefined
        }
      },
      immediate: true
    },
    'dict.type.subject': {
      handler() {
        // 字典加载完成后，确保类型正确
        if (this.queryParams.subjectId !== undefined && typeof this.queryParams.subjectId === 'string') {
          this.queryParams.subjectId = parseInt(this.queryParams.subjectId) || undefined
        }
      },
      immediate: true
    }
  },
  methods: {
    /** 查询题目列表 */
    getList() {
      this.loading = true
      getQuestionList(this.queryParams).then(response => {
        this.questionList = response.rows || []
        this.total = response.total || 0
        this.loading = false
      }).catch(() => {
        this.loading = false
        this.questionList = []
        this.total = 0
      })
    },
    /** 加载分类树 */
    loadCategoryTree() {
      getCategoryTree({}).then(response => {
        this.categoryOptions = response.data || []
        this.categoryTreeData = this.convertToTreeData(response.data || [])
        // 默认选中第一个非根节点
        this.$nextTick(() => {
          const firstNonRootNode = this.findFirstNonRootNode(this.categoryTreeData)
          if (firstNonRootNode) {
            this.queryParams.categoryId = firstNonRootNode.id
            this.selectedCategoryId = firstNonRootNode.id
            // 确保只选中非根节点
            if (this.$refs.categoryTree) {
              this.$refs.categoryTree.setCurrentKey(firstNonRootNode.id)
            }
            this.handleQuery()
          } else {
            // 如果没有非根节点，清空选中状态
            this.selectedCategoryId = undefined
            if (this.$refs.categoryTree) {
              this.$refs.categoryTree.setCurrentKey(null)
            }
          }
        })
      }).catch(() => {
        this.categoryOptions = []
        this.categoryTreeData = []
      })
    },
    /** 查找第一个非根节点 */
    findFirstNonRootNode(nodes) {
      if (!nodes || nodes.length === 0) return null
      for (const node of nodes) {
        // 如果不是根节点（isDefault 不为 true 或 1），返回该节点
        if (node.isDefault !== true && node.isDefault !== 1) {
          return node
        }
        // 如果有子节点，递归查找
        if (node.children && node.children.length > 0) {
          const found = this.findFirstNonRootNode(node.children)
          if (found) return found
        }
      }
      return null
    },
    /** 转换分类数据为树形结构 */
    convertToTreeData(categories) {
      if (!categories || categories.length === 0) return []
      return categories.map(cat => ({
        id: cat.id,
        name: cat.name,
        isDefault: cat.isDefault,
        disabled: cat.isDefault === true || cat.isDefault === 1, // 明确设置 disabled 属性
        children: cat.children && cat.children.length > 0 ? this.convertToTreeData(cat.children) : undefined
      }))
    },
    /** 过滤分类树节点 */
    filterNode(value, data) {
      if (!value) return true
      return data.name.indexOf(value) !== -1
    },
    /** 分类树节点点击 */
    handleCategoryNodeClick(data) {
      // 如果点击的是根节点（isDefault为true），不允许选择
      if (data.isDefault === true || data.isDefault === 1) {
        // 清空选中状态，确保根节点不会被选中
        this.selectedCategoryId = undefined
        this.queryParams.categoryId = undefined // 同时清空 queryParams.categoryId
        if (this.$refs.categoryTree) {
          this.$refs.categoryTree.setCurrentKey(null)
        }
        return
      }
      this.queryParams.categoryId = data.id
      this.selectedCategoryId = data.id
      // 设置树节点为选中状态
      this.$nextTick(() => {
        if (this.$refs.categoryTree) {
          this.$refs.categoryTree.setCurrentKey(data.id)
        }
      })
      this.handleQuery()
    },
    /** 清空分类选中状态（当点击页面其他区域时调用） */
    clearCategorySelection() {
      this.selectedCategoryId = undefined
      this.queryParams.categoryId = undefined
      if (this.$refs.categoryTree) {
        this.$refs.categoryTree.setCurrentKey(null)
      }
    },
    /** 处理文档点击事件（检测是否点击了分类树以外的区域） */
    handleDocumentClick(event) {
      // 检查点击的目标是否在分类树区域内
      const categoryTreeEl = this.$refs.categoryTree?.$el
      if (!categoryTreeEl) return

      // 如果点击的不是分类树区域，清空选中状态
      if (!categoryTreeEl.contains(event.target)) {
        // 但是要排除一些特殊情况：
        // 1. 点击的是新增按钮（会在 handleAdd 中处理）
        // 2. 点击的是对话框（不需要清空）
        // 3. 点击的是分类树相关的元素（搜索框等）
        const isAddButton = event.target.closest('.el-button')?.textContent?.includes('新增')
        const isDialog = event.target.closest('.el-dialog')
        const isCategorySearch = event.target.closest('.head-container')

        if (!isAddButton && !isDialog && !isCategorySearch) {
          this.clearCategorySelection()
        }
      }
    },
    /** 转换分类数据结构 */
    normalizer(node) {
      // 确保 children 要么是数组，要么不存在（不能是 undefined 或 null）
      if (node.children && Array.isArray(node.children) && node.children.length > 0) {
        // 递归处理子节点
        const processedChildren = node.children.map(child => this.normalizer(child))
        return {
          id: node.id,
          label: node.name,
          children: processedChildren
        }
      }
      // 如果没有子节点或子节点为空，不返回 children 属性
      return {
        id: node.id,
        label: node.name
      }
    },
    /** 获取分类名称 */
    getCategoryName(categoryId) {
      if (!categoryId) return ''
      const findCategory = (options, id) => {
        for (const option of options) {
          if (option.id === id) {
            return option.label || option.name
          }
          if (option.children && option.children.length > 0) {
            const found = findCategory(option.children, id)
            if (found) return found
          }
        }
        return ''
      }
      return findCategory(this.categoryOptions, categoryId)
    },
    // 取消按钮
    cancel() {
      this.open = false
      this.reset()
    },
    // 表单重置
    reset() {
      // 获取默认的学科ID（英语=3）和题目类型（单选题=0）
      const defaultSubjectId = this.dict.type.subject && this.dict.type.subject.length > 0
        ? parseInt(this.dict.type.subject.find(d => d.label === '英语' || d.value === '3')?.value || '3')
        : 3
      const defaultType = this.dict.type.question_type && this.dict.type.question_type.length > 0
        ? parseInt(this.dict.type.question_type.find(d => d.label === '单选题' || d.value === '0')?.value || '0')
        : 0

      // 获取分类ID（优先使用 queryParams.categoryId，否则使用 selectedCategoryId）
      const categoryId = this.queryParams.categoryId || this.selectedCategoryId

      // 先重置表单验证
      if (this.$refs.form) {
        this.$refs.form.clearValidate()
      }

      // 设置表单数据（使用 Vue.set 或直接赋值确保响应式）
      this.form = {
        id: undefined,
        questionCategoryId: categoryId, // 默认使用左侧选中的分类
        subjectId: defaultSubjectId, // 默认选中英语
        type: defaultType, // 默认选中单选题（0）
        title: undefined,
        mediaType: 1,
        mediaUrl: undefined,
        optionType: 1,
        weight: 1,
        answer: undefined,
        analyzes: undefined,
        answers: [] // 初始化为空数组
      }
      this.showMediaOptions = false
      this.showOptionMediaOptions = false

      // 直接初始化答案选项，不需要 $nextTick（因为 form.type 已经设置了）
      this.initAnswers()
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm")
      this.handleQuery()
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.id)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      // 如果没有选中分类，提示选择（检查 queryParams.categoryId 或 selectedCategoryId）
      if (!this.queryParams.categoryId && !this.selectedCategoryId) {
        this.$modal.msgWarning("请选择题库分类")
        return
      }
      // 确保使用选中的分类ID
      const categoryId = this.queryParams.categoryId || this.selectedCategoryId
      if (!categoryId) {
        this.$modal.msgWarning("请选择题库分类")
        return
      }
      this.reset()
      this.open = true
      this.title = "添加题目"
    },
    /** 查看按钮操作 */
    handleView(row) {
      this.reset()
      const questionIdBO = { id: row.id }
      getQuestion(questionIdBO).then(response => {
        this.form = this.convertDTOToForm(response.data)
        this.open = true
        this.title = "查看题目"
      })
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const questionIdBO = { id: row.id }
      getQuestion(questionIdBO).then(response => {
        this.form = this.convertDTOToForm(response.data)
        this.open = true
        this.title = "修改题目"
      })
    },
    /** 初始化答案选项 */
    initAnswers() {
      // 修复：当 type 为 0 时，!this.form.type 会返回 true，需要明确判断
      if (this.form.type === undefined || this.form.type === null || this.form.type === '') {
        this.$set(this.form, 'answers', [])
        return
      }
      const type = parseInt(this.form.type)
      // 单选题、多选题、判断题、完形填空需要选项
      if ([0, 1, 2, 5].includes(type)) {
        // 单选题、多选题、完形填空默认添加4个选项（A、B、C、D）
        if (type === 0 || type === 1 || type === 5) {
          const answers = [
            { optionName: 'A', optionContent: '', isAnswer: 0, mediaUrl: undefined },
            { optionName: 'B', optionContent: '', isAnswer: 0, mediaUrl: undefined },
            { optionName: 'C', optionContent: '', isAnswer: 0, mediaUrl: undefined },
            { optionName: 'D', optionContent: '', isAnswer: 0, mediaUrl: undefined }
          ]
          this.$set(this.form, 'answers', answers)
        } else if (type === 2) {
          // 判断题只有两个选项（正确、错误）
          const answers = [
            { optionName: 'A', optionContent: '正确', isAnswer: 0, mediaUrl: undefined },
            { optionName: 'B', optionContent: '错误', isAnswer: 0, mediaUrl: undefined }
          ]
          this.$set(this.form, 'answers', answers)
        }
      } else {
        this.$set(this.form, 'answers', [])
      }
    },
    /** 题目类型改变 */
    handleQuestionTypeChange() {
      this.initAnswers()
      // 判断题只有两个选项（正确、错误）
      if (parseInt(this.form.type) === 2) {
        this.form.answers = [
          { optionName: 'A', optionContent: '正确', isAnswer: 0, mediaUrl: undefined },
          { optionName: 'B', optionContent: '错误', isAnswer: 0, mediaUrl: undefined }
        ]
      }
      // 触发答案校验
      this.$nextTick(() => {
        this.$refs.form.validateField('answers')
      })
    },
    /** 媒体类型改变 */
    handleMediaTypeChange() {
      // 如果选择文本，清空媒体文件URL
      if (this.form.mediaType === 1) {
        this.form.mediaUrl = undefined
      }
      // 如果选择音频，需要同时包含文本和音频
      if (this.form.mediaType === 3) {
        // 提示用户：音频必填，文本非必填
        // 这个提示已经在模板中通过 el-alert 显示了
      }
    },
    /** 选项类型改变 */
    handleOptionTypeChange() {
      // 如果选择文本，清空所有选项的媒体文件URL
      if (this.form.optionType === 1) {
        this.form.answers.forEach(answer => {
          answer.mediaUrl = undefined
        })
      }
    },
    /** 触发标题音频上传 */
    triggerMediaUpload() {
      if (this.$refs.mediaUpload && this.$refs.mediaUpload.$refs.upload) {
        const uploadEl = this.$refs.mediaUpload.$refs.upload.$el
        const fileInput = uploadEl.querySelector('input[type="file"]')
        if (fileInput) {
          fileInput.click()
        }
      }
    },
    /** 触发选项音频上传 */
    triggerAnswerUpload(index) {
      const ref = this.$refs[`answerUpload${index}`]
      if (ref) {
        const uploadRef = Array.isArray(ref) ? ref[0] : ref
        if (uploadRef && uploadRef.$refs && uploadRef.$refs.upload) {
          const uploadEl = uploadRef.$refs.upload.$el
          const fileInput = uploadEl.querySelector('input[type="file"]')
          if (fileInput) {
            fileInput.click()
          }
        }
      }
    },
    /** 标题音频URL变化处理（OssUpload组件回调） */
    handleMediaUrlChange(urlOrData) {
      // OssUpload 组件上传成功后，会通过 @change 事件传递文件URL或包含url和duration的对象
      // 提示信息由 OssUpload 组件统一显示，此处不再重复提示
      if (urlOrData) {
        const url = typeof urlOrData === 'object' ? urlOrData.url : urlOrData
        const duration = typeof urlOrData === 'object' ? urlOrData.duration : null
        this.form.mediaUrl = url
        // 题目音频时长存储在 question_media 表中，需要在保存时传递
        if (duration !== null && duration !== undefined) {
          this.form.mediaDuration = duration
        }
      } else {
        // URL 为空表示移除
        this.form.mediaUrl = undefined
        this.form.mediaDuration = undefined
      }
    },
    /** 选项音频URL变化处理（OssUpload组件回调） */
    handleAnswerMediaUrlChange(urlOrData, answerIndex) {
      // 确保 answer 对象存在
      if (!this.form.answers || !this.form.answers[answerIndex]) {
        return
      }
      const answer = this.form.answers[answerIndex]
      // 提示信息由 OssUpload 组件统一显示，此处不再重复提示
      if (urlOrData) {
        const url = typeof urlOrData === 'object' ? urlOrData.url : urlOrData
        const duration = typeof urlOrData === 'object' ? urlOrData.duration : null
        answer.mediaUrl = url
        // 选项音频时长存储在 question_media 表中，需要在保存时传递
        if (duration !== null && duration !== undefined) {
          answer.mediaDuration = duration
        }
      } else {
        // URL 为空表示移除
        answer.mediaUrl = undefined
        answer.mediaDuration = undefined
      }
    },
    /** 答案选项改变 */
    handleAnswerChange(changedAnswer) {
      // 如果是单选题或判断题，确保只有一个正确答案
      const questionType = parseInt(this.form.type)
      if (questionType === 0 || questionType === 2) {
        // 单选题或判断题：如果当前选项被选中为正确答案，取消其他选项的正确答案标记
        const isCurrentAnswer = changedAnswer.isAnswer === 1 || changedAnswer.isAnswer === '1' || changedAnswer.isAnswer === true
        if (isCurrentAnswer) {
          // 如果当前选项被选中为正确答案，取消其他所有选项的正确答案标记
          this.form.answers.forEach(answer => {
            if (answer !== changedAnswer) {
              answer.isAnswer = 0
            }
          })
        }
      }

      // 触发答案校验
      this.$nextTick(() => {
        this.$refs.form.validateField('answers')
      })
    },
    /** 校验答案选项 */
    validateAnswers(rule, value, callback) {
      if (!this.showAnswers) {
        callback()
        return
      }

      const answers = this.form.answers || []

      // 检查是否有选项
      if (answers.length === 0) {
        callback(new Error('至少需要添加一个选项'))
        return
      }

      // 检查正确答案数量（兼容字符串和数字类型）
      const correctAnswers = answers.filter(a => {
        const isAnswer = a.isAnswer
        return isAnswer === 1 || isAnswer === '1' || isAnswer === true
      })
      const questionType = parseInt(this.form.type)

      if (questionType === 0) {
        // 单选题：有且只能有一个正确答案
        if (correctAnswers.length === 0) {
          callback(new Error('单选题必须选择一个正确答案'))
          return
        }
        if (correctAnswers.length > 1) {
          callback(new Error('单选题只能有一个正确答案，请取消其他选项的正确答案标记'))
          return
        }
      } else if (questionType === 1) {
        // 多选题：至少有一个正确答案
        if (correctAnswers.length === 0) {
          callback(new Error('多选题至少需要选择一个正确答案'))
          return
        }
      } else if (questionType === 2) {
        // 判断题：只能有一个正确答案
        if (correctAnswers.length === 0) {
          callback(new Error('判断题必须选择一个正确答案'))
          return
        }
        if (correctAnswers.length > 1) {
          callback(new Error('判断题只能有一个正确答案，请取消其他选项的正确答案标记'))
          return
        }
      } else if (questionType === 5) {
        // 完形填空：至少有一个正确答案
        if (correctAnswers.length === 0) {
          callback(new Error('完形填空至少需要选择一个正确答案'))
          return
        }
      }

      callback()
    },
    /** 添加答案选项 */
    addAnswer() {
      const optionNames = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H']
      const nextName = optionNames[this.form.answers.length] || String.fromCharCode(65 + this.form.answers.length)
      this.form.answers.push({
        optionName: nextName,
        optionContent: '',
        isAnswer: 0,
        mediaUrl: undefined
      })
    },
    /** 删除答案选项 */
    removeAnswer(index) {
      this.form.answers.splice(index, 1)
    },
    /** 媒体文件上传成功 */
    handleMediaUploadSuccess(response, file, fileList) {
      if (response.code === 200) {
        // 直接替换，不追加（仅支持一个文件）
        this.form.mediaUrlList = [{
          name: response.fileName,
          url: response.url
        }]
        this.$modal.msgSuccess('音频上传成功')
      } else {
        this.$modal.msgError(response.msg || "上传失败")
      }
    },
    /** 删除媒体文件 */
    handleMediaRemove(file, fileList) {
      this.form.mediaUrlList = fileList
    },
    /** 媒体文件变化处理（用于处理二次上传替换） */
    handleMediaChange(file, fileList) {
      // 当新文件被选择时（status 为 ready），如果有已上传成功的文件，立即清空旧文件列表
      // 这样新文件可以正常上传，而不是被 limit 阻止
      if (file.status === 'ready') {
        // 检查是否有已上传成功的文件（通过 url 或 status 判断）
        const existingFiles = this.form.mediaUrlList || []
        const uploadedFiles = existingFiles.filter(f => (f.status === 'success' || f.url) && f.uid !== file.uid)

        if (uploadedFiles.length > 0) {
          // 立即清空旧文件列表，只保留当前新文件
          // 使用 $nextTick 确保清空操作完成后再更新
          this.$nextTick(() => {
            // 只保留当前新文件（status 为 ready 的文件）
            const newFileList = fileList.filter(f => f.uid === file.uid || f.status === 'ready')
            this.form.mediaUrlList = newFileList
          })
        }
      }
    },
    /** 音频上传前验证 */
    handleAudioBeforeUpload(file) {
      // 验证文件类型
      const fileName = file.name || ''
      const fileExtension = fileName.split('.').pop()?.toLowerCase() || ''
      // 不支持 FLAC 格式（浏览器兼容性问题）
      const allowedTypes = ['mp3', 'wav', 'ogg', 'm4a', 'aac']
      if (!allowedTypes.includes(fileExtension)) {
        this.$modal.msgError(`文件格式不正确，请上传 ${allowedTypes.join('、')} 格式的音频文件！`)
        return false
      }
      // 明确禁止 FLAC 格式
      if (fileExtension === 'flac') {
        this.$modal.msgError('不支持 FLAC 格式上传，请使用 MP3、WAV 或其他支持的音频格式！')
        return false
      }
      // 验证文件大小（10MB）
      const maxSize = 10 // MB
      const fileSizeMB = file.size / 1024 / 1024
      if (fileSizeMB > maxSize) {
        this.$modal.msgError(`上传文件大小不能超过 ${maxSize} MB！当前文件大小为 ${fileSizeMB.toFixed(2)} MB`)
        return false
      }
      return true
    },
    /** 视频上传前验证 */
    handleVideoBeforeUpload(file) {
      // 验证文件类型
      const fileName = file.name || ''
      const fileExtension = fileName.split('.').pop()?.toLowerCase() || ''
      const allowedTypes = ['mp4', 'avi', 'mov', 'wmv', 'flv', 'webm']
      if (!allowedTypes.includes(fileExtension)) {
        this.$modal.msgError(`文件格式不正确，请上传 ${allowedTypes.join('、')} 格式的视频文件！`)
        return false
      }
      // 验证文件大小（200MB）
      const maxSize = 200 // MB
      const fileSizeMB = file.size / 1024 / 1024
      if (fileSizeMB > maxSize) {
        this.$modal.msgError(`上传文件大小不能超过 ${maxSize} MB！当前文件大小为 ${fileSizeMB.toFixed(2)} MB`)
        return false
      }
      return true
    },
    /** 媒体文件上传错误处理 */
    handleMediaUploadError(err, file, fileList) {
      console.error('上传错误:', err)
      let errorMessage = '上传失败，请重试'

      // 处理后端返回的错误
      if (err && err.response) {
        const response = err.response
        if (response.data) {
          // 检查是否是文件大小超限错误
          if (response.data.msg && (
            response.data.msg.includes('Maximum upload size exceeded') ||
            response.data.msg.includes('exceeds the configured maximum') ||
            response.data.msg.includes('文件大小超过') ||
            response.data.msg.includes('超过限制')
          )) {
            errorMessage = '上传失败：文件大小超过限制，请选择较小的文件！'
          } else if (response.data.msg) {
            errorMessage = `上传失败：${response.data.msg}`
          }
        }
      } else if (err && err.message) {
        if (err.message.includes('Maximum upload size') || err.message.includes('exceeds')) {
          errorMessage = '上传失败：文件大小超过限制（最大 20MB），请选择较小的文件！'
        } else {
          errorMessage = `上传失败：${err.message}`
        }
      }

      this.$modal.msgError(errorMessage)
    },
    /** 从URL中提取文件名 */
    getFileNameFromUrl(url) {
      if (!url) return ''
      const cleanUrl = url.split('?')[0]
      const segments = cleanUrl.split('/')
      return segments[segments.length - 1] || ''
    },
    /** 预览媒体文件 */
    handleMediaPreview(file) {
      // OssUpload 组件传递的 file 对象可能包含 url 字段，或者直接是 URL 字符串
      const url = file?.url || file || this.form.mediaUrl || ''
      if (!url) {
        this.$modal.msgWarning('文件地址不存在')
        return
      }

      // 判断文件类型
      const fileName = file?.name || this.getFileNameFromUrl(url) || ''
      const fileExtension = fileName.split('.').pop()?.toLowerCase() || ''

      if (['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp'].includes(fileExtension)) {
        this.mediaPreviewType = 'image'
        this.mediaPreviewTitle = '图片预览'
        // 处理URL（如果是相对路径，添加baseURL）
        if (url.startsWith('http://') || url.startsWith('https://')) {
          this.mediaPreviewUrl = url
        } else {
          this.mediaPreviewUrl = process.env.VUE_APP_BASE_API + url
        }
        this.mediaPreviewVisible = true
      } else if (['mp3', 'wav', 'ogg', 'm4a', 'aac'].includes(fileExtension)) {
        // 音频使用全屏遮盖播放器
        this.playMediaAudio(url, fileName)
      } else if (['mp4', 'avi', 'mov', 'wmv', 'flv', 'webm'].includes(fileExtension)) {
        this.mediaPreviewType = 'video'
        this.mediaPreviewTitle = '视频预览'
        // 处理URL（如果是相对路径，添加baseURL）
        if (url.startsWith('http://') || url.startsWith('https://')) {
          this.mediaPreviewUrl = url
        } else {
          this.mediaPreviewUrl = process.env.VUE_APP_BASE_API + url
        }
        this.mediaPreviewVisible = true
      } else {
        this.$modal.msgWarning('不支持预览此文件类型')
        return
      }
    },
    /** 答案媒体文件变化处理（用于处理二次上传替换） */
    handleAnswerMediaChange(file, fileList, answerIndex) {
      // 当新文件被选择时（status 为 ready），如果有已上传成功的文件，立即清空旧文件列表
      // 这样新文件可以正常上传，而不是被 limit 阻止
      if (file.status === 'ready' && this.form.answers && this.form.answers[answerIndex]) {
        const answer = this.form.answers[answerIndex]
        const existingFiles = answer.mediaUrlList || []
        const uploadedFiles = existingFiles.filter(f => (f.status === 'success' || f.url) && f.uid !== file.uid)

        if (uploadedFiles.length > 0) {
          // 立即清空旧文件列表，只保留当前新文件
          // 使用 $nextTick 确保清空操作完成后再更新
          this.$nextTick(() => {
            // 只保留当前新文件（status 为 ready 的文件）
            const newFileList = fileList.filter(f => f.uid === file.uid || f.status === 'ready')
            answer.mediaUrlList = newFileList
          })
        }
      }
    },
    /** 答案媒体文件上传成功 */
    handleAnswerMediaSuccess(response, file, fileList, answerIndex) {
      // 确保 answer 对象存在
      if (!this.form.answers || !this.form.answers[answerIndex]) {
        return
      }

      if (response && response.code === 200) {
        const answer = this.form.answers[answerIndex]
        if (!answer.mediaUrlList) {
          answer.mediaUrlList = []
        }
        // 确保只有一个音频文件
        answer.mediaUrlList = [{
          name: response.fileName || file.name,
          url: response.url
        }]
        this.$modal.msgSuccess('音频上传成功')
      } else {
        this.$modal.msgError((response && response.msg) || '上传失败')
      }
    },
    /** 删除答案媒体文件 */
    handleAnswerMediaRemove(file, fileList, answerIndex) {
      // 确保 answer 对象存在
      if (!this.form.answers || !this.form.answers[answerIndex]) {
        return
      }
      const answer = this.form.answers[answerIndex]
      answer.mediaUrlList = fileList
    },
    /** 预览答案媒体文件 */
    handleAnswerMediaPreview(file, answer) {
      // OssUpload 组件传递的 file 对象可能包含 url 字段，或者直接是 URL 字符串
      const url = file?.url || file || answer?.mediaUrl || ''
      if (!url) {
        this.$modal.msgWarning('文件地址不存在')
        return
      }

      // 判断文件类型
      const fileName = file?.name || this.getFileNameFromUrl(url) || ''
      const fileExtension = fileName.split('.').pop()?.toLowerCase() || ''

      if (['mp3', 'wav', 'ogg', 'm4a', 'aac'].includes(fileExtension)) {
        // 音频使用全屏遮盖播放器（与题目媒体预览一致）
        this.playMediaAudio(url, fileName)
      } else {
        this.$modal.msgWarning('不支持预览此文件类型')
      }
    },
    encodeUrlFileName: encodeFileNameHelper,
    /** 播放答案音频（全屏遮盖） */
    playAnswerAudio(mediaFile, answerIndex) {
      const url = mediaFile.url || ''
      if (!url) {
        this.$modal.msgWarning('音频文件地址不存在')
        return
      }

      // OSS返回的URL应该是完整的URL（包含http://或https://），直接使用
      // 如果是相对路径，才需要拼接baseURL
      let audioUrl = url
      if (!url.startsWith('http://') && !url.startsWith('https://')) {
        // 相对路径处理
        if (url.startsWith('/')) {
          audioUrl = process.env.VUE_APP_BASE_API + url
        } else {
          audioUrl = process.env.VUE_APP_BASE_API + '/' + url
        }
      }

      // 对URL中的文件名部分进行编码（处理中文和特殊字符）
      audioUrl = this.encodeUrlFileName(audioUrl)

      // 检查URL是否有效
      if (!audioUrl) {
        this.$modal.msgError('音频文件地址无效')
        return
      }

      console.log('原始URL:', url)
      console.log('转换后URL:', audioUrl) // 调试日志

      // 检查浏览器是否支持该音频格式
      const fileExtension = audioUrl.split('.').pop()?.toLowerCase() || ''
      const audioElement = document.createElement('audio')
      let isFormatSupported = false
      let mimeType = ''

      // 检查常见音频格式支持并设置MIME类型
      if (fileExtension === 'mp3') {
        isFormatSupported = audioElement.canPlayType('audio/mpeg') !== '' ||
                           audioElement.canPlayType('audio/mp3') !== ''
        mimeType = 'audio/mpeg'
      } else if (fileExtension === 'wav') {
        isFormatSupported = audioElement.canPlayType('audio/wav') !== '' ||
                           audioElement.canPlayType('audio/wave') !== ''
        mimeType = 'audio/wav'
      } else if (fileExtension === 'ogg') {
        isFormatSupported = audioElement.canPlayType('audio/ogg') !== ''
        mimeType = 'audio/ogg'
      } else if (fileExtension === 'm4a') {
        isFormatSupported = audioElement.canPlayType('audio/mp4') !== '' ||
                           audioElement.canPlayType('audio/m4a') !== ''
        mimeType = 'audio/mp4'
      } else if (fileExtension === 'aac') {
        isFormatSupported = audioElement.canPlayType('audio/aac') !== '' ||
                           audioElement.canPlayType('audio/mp4') !== ''
        mimeType = 'audio/aac'
      }

      console.log('音频格式支持检测:', {
        format: fileExtension,
        supported: isFormatSupported,
        mimeType: mimeType
      })

      this.audioPlayerUrl = audioUrl
      this.audioPlayerMimeType = mimeType
      this.audioPlayerTitle = this.getAudioFileName(mediaFile)
      this.audioPlayerVisible = true

      // 等待DOM更新后自动播放
      this.$nextTick(() => {
        if (this.$refs.audioPlayer) {
          this.$refs.audioPlayer.volume = this.audioMuted ? 0 : (this.audioVolume / 100)
          // 先加载音频
          this.$refs.audioPlayer.load()
          // 监听加载完成事件，自动播放
          const playAudio = () => {
            this.$refs.audioPlayer.play().then(() => {
              this.audioPlaying = true
            }).catch(err => {
              console.error('播放失败:', err)
              // 如果自动播放失败（可能是浏览器策略限制），不显示错误，用户可以手动点击播放
              if (err.name !== 'NotAllowedError') {
                this.$modal.msgError('音频播放失败：' + (err.message || '未知错误'))
              }
            })
          }
          // 监听 canplay 事件（可以播放时）
          this.$refs.audioPlayer.addEventListener('canplay', playAudio, { once: true })
          // 如果已经可以播放（缓存的情况），立即播放
          if (this.$refs.audioPlayer.readyState >= 2) {
            playAudio()
          }
          // 监听加载错误，提供更详细的错误信息
          this.$refs.audioPlayer.addEventListener('error', (e) => {
            const error = this.$refs.audioPlayer.error
            let errorMsg = '音频加载失败'
            if (error) {
              switch (error.code) {
                case error.MEDIA_ERR_ABORTED:
                  errorMsg = '音频加载被中止'
                  break
                case error.MEDIA_ERR_NETWORK:
                  errorMsg = '网络错误，无法加载音频'
                  break
                case error.MEDIA_ERR_DECODE:
                  errorMsg = '音频解码失败'
                  break
                case error.MEDIA_ERR_SRC_NOT_SUPPORTED:
                  errorMsg = '音频格式不支持或文件不存在'
                  break
                default:
                  errorMsg = `音频加载失败 (错误代码: ${error.code})`
              }
            }
            console.error('音频加载失败:', {
              error: error,
              url: audioUrl,
              mediaFile: mediaFile,
              format: fileExtension,
              formatSupported: isFormatSupported
            })
            console.log(errorMsg + '，请检查文件URL是否正确: ' + audioUrl)
            this.$modal.msgError(errorMsg + '，请检查文件URL是否正确！')
          }, { once: true })
        }
      })
    },
    /** 关闭音频播放器 */
    closeAudioPlayer() {
      if (this.$refs.audioPlayer) {
        this.$refs.audioPlayer.pause()
      }
      this.audioPlayerVisible = false
      this.audioPlaying = false
      this.currentTime = 0
      this.duration = 0
      this.audioProgress = 0
    },
    /** 切换音频播放/暂停 */
    toggleAudioPlay() {
      if (!this.$refs.audioPlayer) return

      if (this.audioPlaying) {
        this.$refs.audioPlayer.pause()
        this.audioPlaying = false
      } else {
        this.$refs.audioPlayer.play().then(() => {
          this.audioPlaying = true
        }).catch(err => {
          console.error('播放失败:', err)
          this.$modal.msgError('音频播放失败')
        })
      }
    },
    /** 停止音频播放 */
    stopAudio() {
      if (!this.$refs.audioPlayer) return
      this.$refs.audioPlayer.pause()
      this.$refs.audioPlayer.currentTime = 0
      this.audioPlaying = false
      this.currentTime = 0
      this.audioProgress = 0
    },
    /** 音频时间更新 */
    handleAudioTimeUpdate() {
      if (this.$refs.audioPlayer) {
        this.currentTime = this.$refs.audioPlayer.currentTime
        if (this.duration > 0) {
          this.audioProgress = (this.currentTime / this.duration) * 100
        }
      }
    },
    /** 音频元数据加载完成 */
    handleAudioLoaded() {
      if (this.$refs.audioPlayer) {
        this.duration = this.$refs.audioPlayer.duration
      }
    },
    /** 音频播放结束 */
    handleAudioEnded() {
      this.audioPlaying = false
      this.currentTime = 0
      this.audioProgress = 0
    },
    /** 音频进度条改变 */
    handleAudioProgressChange(value) {
      if (this.$refs.audioPlayer && this.duration > 0) {
        this.$refs.audioPlayer.currentTime = (value / 100) * this.duration
        this.currentTime = this.$refs.audioPlayer.currentTime
      }
    },
    /** 音频音量改变（拖动时） */
    handleAudioVolumeInput(value) {
      if (this.$refs.audioPlayer) {
        this.$refs.audioPlayer.volume = value / 100
      }
      // 显示tooltip
      this.volumeTooltipVisible = true
      // 清除之前的定时器
      if (this.volumeTooltipTimer) {
        clearTimeout(this.volumeTooltipTimer)
      }
      // 500ms后隐藏tooltip
      this.volumeTooltipTimer = setTimeout(() => {
        this.volumeTooltipVisible = false
      }, 500)
    },
    /** 音频音量改变（拖动结束） */
    handleAudioVolumeChange(value) {
      if (this.$refs.audioPlayer) {
        this.$refs.audioPlayer.volume = value / 100
        // 如果音量大于0，取消静音
        if (value > 0 && this.audioMuted) {
          this.audioMuted = false
        }
        // 如果音量设为0，自动静音
        if (value === 0) {
          this.audioMuted = true
        }
      }
      // 隐藏tooltip
      this.volumeTooltipVisible = false
      if (this.volumeTooltipTimer) {
        clearTimeout(this.volumeTooltipTimer)
      }
    },
    /** 切换音量滑块显示/隐藏 */
    toggleVolumeSlider(event) {
      // 阻止事件冒泡，避免立即触发外部点击监听
      if (event) {
        event.stopPropagation()
      }
      this.volumeSliderVisible = !this.volumeSliderVisible
      if (this.volumeSliderVisible) {
        // 添加点击外部区域隐藏的监听（使用setTimeout确保在下一个事件循环中执行）
        this.$nextTick(() => {
          setTimeout(() => {
            document.addEventListener('click', this.handleClickOutsideVolumeSlider, true)
          }, 0)
        })
      } else {
        // 移除监听
        document.removeEventListener('click', this.handleClickOutsideVolumeSlider, true)
      }
    },
    /** 点击音量滑块外部区域 */
    handleClickOutsideVolumeSlider(event) {
      // 使用refs获取元素，更可靠
      const volumeWrapper = this.$el?.querySelector('.audio-volume-wrapper')
      const volumeContainer = this.$el?.querySelector('.audio-volume-slider-container')
      const volumeBtn = this.$el?.querySelector('.audio-volume-btn')

      if (volumeWrapper) {
        // 检查点击是否在音量控制区域内（包括按钮和滑块容器）
        const isClickInside = volumeWrapper.contains(event.target) ||
                             (volumeContainer && volumeContainer.contains(event.target)) ||
                             (volumeBtn && volumeBtn.contains(event.target))

        if (!isClickInside) {
          this.volumeSliderVisible = false
          document.removeEventListener('click', this.handleClickOutsideVolumeSlider, true)
        }
      }
    },
    /** 切换静音 */
    toggleMute() {
      if (this.$refs.audioPlayer) {
        this.audioMuted = !this.audioMuted
        if (this.audioMuted) {
          this.$refs.audioPlayer.volume = 0
        } else {
          this.$refs.audioPlayer.volume = this.audioMuted ? 0 : (this.audioVolume / 100)
        }
      }
    },
    /** 格式化时间 */
    formatTime(seconds) {
      if (!seconds || isNaN(seconds)) return '0:00'
      const mins = Math.floor(seconds / 60)
      const secs = Math.floor(seconds % 60)
      return `${mins}:${secs.toString().padStart(2, '0')}`
    },
    /** 播放媒体音频（全屏遮盖） */
    playMediaAudio(url, fileName) {
      // OSS返回的URL应该是完整的URL（包含http://或https://），直接使用
      // 如果是相对路径，才需要拼接baseURL
      let audioUrl = url
      if (!url.startsWith('http://') && !url.startsWith('https://')) {
        // 相对路径处理
        if (url.startsWith('/')) {
          audioUrl = process.env.VUE_APP_BASE_API + url
        } else {
          audioUrl = process.env.VUE_APP_BASE_API + '/' + url
        }
      }

      // 对URL中的文件名部分进行编码（处理中文和特殊字符）
      audioUrl = this.encodeUrlFileName(audioUrl)

      // 检查URL是否有效
      if (!audioUrl) {
        this.$modal.msgError('音频文件地址无效')
        return
      }

      console.log('原始URL:', url)
      console.log('转换后URL:', audioUrl) // 调试日志

      // 检查浏览器是否支持该音频格式
      const fileExtension = audioUrl.split('.').pop()?.toLowerCase() || ''
      const audioElement = document.createElement('audio')
      let isFormatSupported = false
      let mimeType = ''

      // 检查常见音频格式支持并设置MIME类型
      if (fileExtension === 'mp3') {
        isFormatSupported = audioElement.canPlayType('audio/mpeg') !== '' ||
                           audioElement.canPlayType('audio/mp3') !== ''
        mimeType = 'audio/mpeg'
      } else if (fileExtension === 'wav') {
        isFormatSupported = audioElement.canPlayType('audio/wav') !== '' ||
                           audioElement.canPlayType('audio/wave') !== ''
        mimeType = 'audio/wav'
      } else if (fileExtension === 'ogg') {
        isFormatSupported = audioElement.canPlayType('audio/ogg') !== ''
        mimeType = 'audio/ogg'
      } else if (fileExtension === 'm4a') {
        isFormatSupported = audioElement.canPlayType('audio/mp4') !== '' ||
                           audioElement.canPlayType('audio/m4a') !== ''
        mimeType = 'audio/mp4'
      } else if (fileExtension === 'aac') {
        isFormatSupported = audioElement.canPlayType('audio/aac') !== '' ||
                           audioElement.canPlayType('audio/mp4') !== ''
        mimeType = 'audio/aac'
      }

      console.log('音频格式支持检测:', {
        format: fileExtension,
        supported: isFormatSupported,
        mimeType: mimeType
      })

      this.audioPlayerUrl = audioUrl
      this.audioPlayerMimeType = mimeType
      this.audioPlayerTitle = fileName || '音频预览'
      this.audioPlayerVisible = true

      // 等待DOM更新后自动播放
      this.$nextTick(() => {
        if (this.$refs.audioPlayer) {
          this.$refs.audioPlayer.volume = this.audioMuted ? 0 : (this.audioVolume / 100)
          // 先加载音频
          this.$refs.audioPlayer.load()
          // 监听加载完成事件，自动播放
          const playAudio = () => {
            this.$refs.audioPlayer.play().then(() => {
              this.audioPlaying = true
            }).catch(err => {
              console.error('播放失败:', err)
              // 如果自动播放失败（可能是浏览器策略限制），不显示错误，用户可以手动点击播放
              if (err.name !== 'NotAllowedError') {
                this.$modal.msgError('音频播放失败：' + (err.message || '未知错误'))
              }
            })
          }
          // 监听 canplay 事件（可以播放时）
          this.$refs.audioPlayer.addEventListener('canplay', playAudio, { once: true })
          // 如果已经可以播放（缓存的情况），立即播放
          if (this.$refs.audioPlayer.readyState >= 2) {
            playAudio()
          }
          // 监听加载错误，提供更详细的错误信息
          this.$refs.audioPlayer.addEventListener('error', (e) => {
            const error = this.$refs.audioPlayer.error
            let errorMsg = '音频加载失败'
            if (error) {
              switch (error.code) {
                case error.MEDIA_ERR_ABORTED:
                  errorMsg = '音频加载被中止'
                  break
                case error.MEDIA_ERR_NETWORK:
                  errorMsg = '网络错误，无法加载音频'
                  break
                case error.MEDIA_ERR_DECODE:
                  errorMsg = '音频解码失败'
                  break
                case error.MEDIA_ERR_SRC_NOT_SUPPORTED:
                  errorMsg = '音频格式不支持或文件不存在'
                  break
                default:
                  errorMsg = `音频加载失败 (错误代码: ${error.code})`
              }
            }
            console.error('音频加载失败:', {
              error: error,
              url: audioUrl,
              fileName: fileName,
              format: fileExtension,
              formatSupported: isFormatSupported
            })
            this.$modal.msgError(errorMsg + '，请检查文件URL是否正确！')
          }, { once: true })
        }
      })
    },
    /** 将DTO转换为表单数据 */
    convertDTOToForm(dto) {
      if (!dto) return {}

      // 转换媒体文件URL（取第一个文件的URL，因为只支持一个文件）
      const mediaUrl = (dto.mediaUrl && dto.mediaUrl.length > 0)
        ? (dto.mediaUrl[0].mediaUrl || dto.mediaUrl[0].mediaPath || '')
        : undefined

      // 转换答案列表
      const answers = (dto.answers || []).map(answer => {
        // 转换选项媒体文件URL（取第一个文件的URL，因为只支持一个文件）
        const answerMediaUrl = (answer.mediaUrl && answer.mediaUrl.length > 0)
          ? (answer.mediaUrl[0].mediaUrl || answer.mediaUrl[0].mediaPath || '')
          : undefined
        // 转换 isAnswer：后端 2 -> 前端 1
        let isAnswer = answer.isAnswer || 0
        if (isAnswer === 2) {
          isAnswer = 1 // 前端使用 1 表示正确答案
        } else {
          isAnswer = 0
        }
        return {
          id: answer.id,
          optionName: answer.optionName || '',
          optionContent: answer.optionContent || '',
          isAnswer: isAnswer,
          mediaUrl: answerMediaUrl
        }
      })

      return {
        id: dto.id,
        questionCategoryId: dto.questionCategoryId,
        subjectId: dto.subjectId,
        type: dto.type,
        title: dto.title,
        mediaType: dto.mediaType || 1,
        mediaUrl: mediaUrl,
        optionType: dto.optionType || 1,
        weight: dto.weight || 1,
        answer: dto.answer,
        analyzes: dto.analyzes,
        answers: answers
      }
    },
    /** 提交按钮 */
    submitForm() {
      // 检查是否有正在上传的文件
      if (this.checkUploading()) {
        this.$modal.msgWarning("请先等待所有文件上传完成")
        return
      }

      this.$refs["form"].validate(valid => {
        if (valid) {
          // 构建提交数据
          const questionBO = this.buildQuestionBO()
          if (this.form.id != undefined) {
            updateQuestion(questionBO).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            }).catch(() => {})
          } else {
            addQuestion(questionBO).then(response => {
              this.$modal.msgSuccess("新增成功")
              this.open = false
              this.getList()
            }).catch(() => {})
          }
        }
      })
    },
    /** 构建题目BO */
    buildQuestionBO() {
      // 转换媒体文件（将 mediaUrl 字符串转换为 mediaUrlList 格式）
      const mediaUrl = this.form.mediaUrl
        ? [{
            mediaName: this.getFileNameFromUrl(this.form.mediaUrl) || '',
            mediaUrl: this.form.mediaUrl || '',
            mediaPath: this.form.mediaUrl || '',
            mediaDuration: this.form.mediaDuration || null, // 添加音频时长
            sortNum: 0
          }]
        : []

      // 转换答案列表
      const answers = (this.form.answers || []).map((answer, index) => {
        // 转换选项媒体文件（将 mediaUrl 字符串转换为 mediaUrlList 格式）
        const answerMedia = answer.mediaUrl
          ? [{
              mediaName: this.getFileNameFromUrl(answer.mediaUrl) || '',
              mediaUrl: answer.mediaUrl || '',
              mediaPath: answer.mediaUrl || '',
              mediaDuration: answer.mediaDuration || null // 添加音频时长
            }]
          : []

        // 转换 isAnswer：前端 1 -> 后端 2（YesOrNoEnum.YES.getCode()）
        let isAnswer = answer.isAnswer || 0
        if (isAnswer === 1 || isAnswer === '1' || isAnswer === true) {
          isAnswer = 2 // YesOrNoEnum.YES.getCode()
        } else {
          isAnswer = 0 // YesOrNoEnum.NO.getCode()
        }

        return {
          id: answer.id,
          serialNo: index + 1,
          optionName: answer.optionName,
          optionContent: answer.optionContent,
          isAnswer: isAnswer,
          mediaUrl: answerMedia
        }
      })

      // 构建答案ID字符串（多个英文逗号隔开）
      const answerIds = this.form.answers
        .filter(answer => answer.isAnswer === 1)
        .map((answer, index) => index + 1)
        .join(',')

      return {
        id: this.form.id,
        questionCategoryId: this.form.questionCategoryId,
        subjectId: this.form.subjectId,
        type: this.form.type,
        title: this.form.title,
        mediaType: this.form.mediaType,
        mediaUrl: mediaUrl,
        optionType: this.form.optionType,
        weight: this.form.weight,
        answer: answerIds,
        analyzes: this.form.analyzes,
        answers: answers
      }
    },
    /** 处理上传进度 */
    handleUploadProgress(type, isUploading) {
      this.$set(this.uploadStatus, type, isUploading)
    },
    /** 检查是否有正在上传的文件 */
    checkUploading() {
      // 检查本页面的上传状态
      const hasUploading = Object.values(this.uploadStatus).some(status => status === true)

      // 检查题目媒体上传组件
      if (this.$refs.mediaUpload && this.$refs.mediaUpload.$refs && this.$refs.mediaUpload.$refs.upload) {
        const mediaFiles = this.$refs.mediaUpload.$refs.upload.uploadFiles || []
        if (mediaFiles.some(file => file.status === 'uploading')) {
          this.handleUploadProgress('media', true)
          return true
        }
      }

      // 检查选项媒体上传组件
      if (this.form.answers && this.form.answers.length > 0) {
        for (let i = 0; i < this.form.answers.length; i++) {
          const answerUploadRef = this.$refs[`answerUpload${i}`]
          if (answerUploadRef && answerUploadRef.$refs && answerUploadRef.$refs.upload) {
            const answerFiles = answerUploadRef.$refs.upload.uploadFiles || []
            if (answerFiles.some(file => file.status === 'uploading')) {
              this.handleUploadProgress(`answer${i}`, true)
              return true
            }
          }
        }
      }

      if (hasUploading) {
        return true
      }

      return false
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const ids = row.id ? [row.id] : this.ids
      const questionTitles = row.title ? [row.title] : this.questionList.filter(item => this.ids.includes(item.id)).map(item => item.title)
      this.$modal.confirm('是否确认删除题目标题为"' + questionTitles.join('、') + '"的数据项？').then(() => {
        return deleteQuestion({ ids: ids })
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
.app-container {
  width: 100%;

  .el-table {
    width: 100%;
  }

  .head-container {
    padding: 10px;
  }
}

::v-deep .splitpanes__splitter {
  background-color: #f0f0f0;
  position: relative;
}

::v-deep .splitpanes__splitter:hover {
  background-color: #e0e0e0;
}

// 分类树选中节点高亮样式（更醒目）
::v-deep .el-tree-node.is-current > .el-tree-node__content {
  background-color: #409EFF !important;
  color: #fff !important;
  font-weight: bold;
}

::v-deep .el-tree-node.is-current > .el-tree-node__content:hover {
  background-color: #66b1ff !important;
}

// 禁用节点样式（根节点）
::v-deep .el-tree-node.is-disabled > .el-tree-node__content {
  color: #c0c4cc !important;
  cursor: not-allowed !important;
  background-color: #f5f7fa !important;
  pointer-events: none !important;
}

::v-deep .el-tree-node.is-disabled > .el-tree-node__content:hover {
  background-color: #f5f7fa !important;
  color: #c0c4cc !important;
}

// 确保禁用的根节点不会被高亮选中
::v-deep .el-tree-node.is-disabled.is-current > .el-tree-node__content {
  background-color: #f5f7fa !important;
  color: #c0c4cc !important;
}

// 答案选项区域：限制上传音频和正确答案组件的宽度，使其更紧凑
::v-deep .el-upload {
  width: 100%;
  display: inline-block;
}

::v-deep .el-upload .el-upload__input {
  display: none;
}

// 限制正确答案复选框的容器宽度
::v-deep .el-checkbox {
  white-space: nowrap;
}

// 答案选项容器：去除右边留白
.el-form-item[label="答案选项"] {
  ::v-deep .el-form-item__content {
    .el-form-item {
      margin-bottom: 0;
    }
  }
}

// 答案选项 div 容器样式优化
.answer-option-container {
  padding-right: 5px !important;
}

// 上传中旋转动画
@keyframes rotating {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

// 音频全屏遮盖播放器样式
.audio-player-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
}

.audio-player-container {
  background: #fff;
  border-radius: 8px;
  width: 90%;
  max-width: 600px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
  overflow: hidden;
}

.audio-player-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 15px 20px;
  border-bottom: 1px solid #e4e7ed;
  background: #f5f7fa;
}

.audio-player-title {
  font-size: 16px;
  font-weight: 500;
  color: #303133;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.audio-player-close {
  margin-left: 10px;
}

.audio-player-content {
  padding: 20px;
}

.audio-player-controls-wrapper {
  margin-top: 15px;
  display: flex;
  justify-content: center;
}

.audio-player-controls {
  display: flex;
  align-items: center;
  gap: 12px;
  background: #f5f7fa;
  border-radius: 20px;
  padding: 8px 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.audio-control-btn {
  background: none;
  border: none;
  padding: 0;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background-color 0.2s ease;
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  border-radius: 50%;

  &:hover {
    background-color: rgba(0, 0, 0, 0.1);
  }

  &:active {
    background-color: rgba(0, 0, 0, 0.15);
  }

  .play-pause-icon {
    font-size: 16px;
    line-height: 1;
    display: inline-block;
    color: #000;
  }
}

.audio-time-display {
  flex-shrink: 0;
  font-size: 13px;
  color: #606266;
  white-space: nowrap;
  min-width: 80px;
  text-align: center;
  position: relative;
  z-index: 1;
  transition: opacity 0.3s ease;
}

.audio-time {
  font-size: 13px;
  color: #606266;
  font-weight: 500;
}

.audio-progress-container {
  flex: 1;
  min-width: 150px;
  max-width: 300px;
  position: relative;
  z-index: 1;
  transition: opacity 0.3s ease;

  &.progress-disabled {
    opacity: 0.3;
    pointer-events: none;

    ::v-deep .el-slider__button {
      cursor: not-allowed;
      opacity: 0.3;
    }

    ::v-deep .el-slider__runway {
      opacity: 0.3;
    }

    ::v-deep .el-slider__bar {
      opacity: 0.3;
    }
  }
}

.audio-progress-slider {
  width: 100%;
}

.audio-volume-wrapper {
  position: relative;
  flex-shrink: 0;
  display: flex;
  align-items: center;
}

.audio-volume-btn {
  background: none;
  border: 2px solid transparent;
  border-radius: 50%;
  padding: 0;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: background-color 0.2s ease;
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  position: relative;
  z-index: 11;
  vertical-align: middle;

  .volume-icon {
    font-size: 18px;
    color: #000;
    vertical-align: middle;
    pointer-events: none; // 确保点击图标时事件能穿透到按钮
  }

  &:hover {
    background-color: rgba(0, 0, 0, 0.1);
  }

  &:active {
    background-color: rgba(0, 0, 0, 0.15);
  }

  &.volume-btn-active {
    border-color: #409EFF;
    background: rgba(64, 158, 255, 0.1);

    .volume-icon {
      color: #409EFF;
    }
  }
}

.audio-volume-slider-container {
  position: absolute;
  right: 100%;
  top: 50%;
  transform: translateY(-50%);
  margin-right: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
  background: #f5f7fa;
  border-radius: 20px;
  padding: 4px 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  animation: slideInLeft 0.3s ease;
  width: 120px;
  flex-shrink: 0;
  z-index: 15;
}

@keyframes slideInLeft {
  from {
    opacity: 0;
    transform: translateY(-50%) translateX(20px);
  }
  to {
    opacity: 1;
    transform: translateY(-50%) translateX(0);
  }
}

.audio-volume-slider-wrapper {
  flex: 1;
  min-width: 0;
  position: relative;
}

.audio-volume-slider {
  width: 100%;

  ::v-deep .el-slider__runway {
    margin: 0;
  }

  ::v-deep .el-slider__bar {
    background-color: #409EFF;
  }

  ::v-deep .el-slider__button {
    border-color: #409EFF;
  }
}

.audio-volume-tooltip {
  position: absolute;
  top: -35px;
  left: 50%;
  transform: translateX(-50%);
  pointer-events: none;
  z-index: 13;
  background-color: rgba(0, 0, 0, 0.75);
  color: #fff;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;

  &::after {
    content: '';
    position: absolute;
    bottom: -4px;
    left: 50%;
    transform: translateX(-50%);
    width: 0;
    height: 0;
    border-left: 4px solid transparent;
    border-right: 4px solid transparent;
    border-top: 4px solid rgba(0, 0, 0, 0.75);
  }
}

// 当音量滑块显示时，降低播放时长等底部元素的透明度
.audio-player-controls.volume-slider-visible {
  .audio-time-display {
    opacity: 0.3;
  }
}

// 自定义上传进度条样式（单图层，动态显示百分比）
.upload-progress-wrapper {
  position: relative;
  width: 100%;
  height: 24px;
  display: flex;
  align-items: center;
}

.upload-progress-bar-bg {
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 100%;
  height: 6px;
  background-color: #e4e7ed;
  border-radius: 3px;
  z-index: 1;
}

.upload-progress-bar-fill {
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  height: 6px;
  background-color: #409EFF;
  border-radius: 3px;
  transition: width 0.3s ease;
  z-index: 2;
}

.upload-progress-text {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  font-size: 11px;
  font-weight: 600;
  color: #409EFF;
  z-index: 3;
  white-space: nowrap;
  text-shadow: 0 0 2px rgba(255, 255, 255, 0.8);
}

/* 题目管理弹窗样式 */
::v-deep .question-dialog {
  .el-dialog__body {
    padding: 20px 20px 10px 20px;
  }

  .el-dialog__footer {
    padding: 10px 20px 20px 20px;
  }

  /* 每一行的宽度右对齐，距离弹窗右边缘20px */
  /* 每一行的宽度 = calc(100% - 20px)，右对齐 */
  .el-form-item {
    width: calc(100% - 20px) !important;
    margin-right: 20px !important;
    box-sizing: border-box;
  }

  /* 学科和题目类型的 el-row 宽度 */
  /* 两个 span="10" 列 + gutter="20" = 83.3333% * (100% - 20px) + 20px */
  /* 使 el-row 宽度正好匹配两个列的总宽度，右对齐 */
  .subject-type-row {
    width: calc(83.3333% * (100% - 20px) + 20px) !important;
    margin-left: 100px !important; /* label宽度 */
    margin-right: 20px !important; /* 距离弹窗右边缘20px */
    box-sizing: border-box;
  }

  /* 题目类型下拉框右边缘距离所在列右边缘10px */
  .question-type-form-item {
    .el-form-item__content {
      padding-right: 10px !important;
    }
  }

  /* 题目标题输入框宽度82%，与题目类型下拉框右对齐 */
  .question-input-right-align {
    width: 82% !important;
    margin-left: auto;
    margin-right: 0;
  }

  /* 答案选项容器右对齐 */
  .answer-option-container.question-input-right-align {
    width: 82% !important;
  }

  /* 权重输入框特殊处理，保持固定宽度，右对齐 */
  .el-input-number.question-input-right-align {
    width: 80px !important;
    margin-left: auto;
    margin-right: calc(82% - 80px);
  }
}

/* Footer按钮右对齐，距离弹窗右边缘20px */
::v-deep .question-dialog {
  .question-dialog-footer {
    text-align: right;
    width: 100% !important;
    padding-right: 20px !important;
    box-sizing: border-box;
  }
  
  /* 修复 el-radio 在 dialog 中的无障碍警告 */
  .el-radio__original {
    /* 确保 radio input 不会被 aria-hidden 影响 */
    position: absolute;
    opacity: 0;
    z-index: -1;
  }
  
  .el-radio__input {
    /* 确保 radio 输入区域可以正常获得焦点 */
    position: relative;
  }
}

/* 上传文件区域样式 - 整个区域可点击，蓝色文字，30%透明度 */
.upload-link-area {
  color: rgba(64, 158, 255, 0.3);
  cursor: pointer;
  font-size: 14px;
  line-height: 1.5;
  user-select: none;
}

.upload-link-area:hover {
  color: rgba(64, 158, 255, 0.9);
}

.upload-link-area .upload-tip {
  margin-left: 8px;
}
</style>

