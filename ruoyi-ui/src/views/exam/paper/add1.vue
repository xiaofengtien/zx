<template>
  <div class="app-container">
    <el-card>
      <div slot="header" class="clearfix">
        <span class="card-title">添加试卷</span>
        <el-button style="float: right; padding: 3px 0" type="text" @click="handleCancel">返回</el-button>
      </div>

      <el-form ref="form" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="试卷名称" prop="paperName">
          <el-row :gutter="8" style="flex-wrap: nowrap;">
            <el-col :span="3" style="padding-left: 0; flex: 0 0 auto;">
              <el-form-item prop="year" required style="margin-bottom: 0;">
                <el-select v-model="form.year" placeholder="选择年" style="width: 100%">
                  <el-option
                    v-for="year in yearOptions"
                    :key="year"
                    :label="year + '年'"
                    :value="year"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="3" style="flex: 0 0 auto;">
              <el-form-item prop="month" required style="margin-bottom: 0">
                <el-select v-model="form.month" placeholder="选择月" style="width: 100%">
                  <el-option
                    v-for="dict in dict.type.paper_month"
                    :key="dict.value"
                    :label="dict.label"
                    :value="parseInt(dict.value)"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="3" style="flex: 0 0 auto;">
              <el-form-item prop="province" required style="margin-bottom: 0">
                <el-select v-model="form.province" placeholder="选择省" style="width: 100%">
                  <el-option
                    v-for="dict in dict.type.paper_province"
                    :key="dict.value"
                    :label="dict.label"
                    :value="dict.value"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="4" style="flex: 0 0 auto;">
              <el-form-item prop="paperType" required style="margin-bottom: 0">
                <el-select v-model="form.paperType" placeholder="选择类型" style="width: 100%">
                  <el-option
                    v-for="dict in dict.type.paper_type"
                    :key="dict.value"
                    :label="dict.label"
                    :value="dict.value"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="10" style="flex: 1;">
              <el-form-item prop="customName" style="margin-bottom: 0;">
                <el-input
                  v-model="form.customName"
                  placeholder="自定义名称（可选）"
                  style="text-align: right;"
                  clearable
                />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="考试时长" prop="duration" required>
              <el-input-number v-model="form.duration" :min="1" :max="999" controls-position="right" style="width: 200px" />
              <span style="margin-left: 10px; color: #909399;">分钟</span>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="试卷描述" prop="paperDesc">
          <el-input v-model="form.paperDesc" type="textarea" :rows="3" placeholder="请输入试卷描述" />
        </el-form-item>

        <el-form-item label="总分" prop="totalScore">
          <el-input-number
            v-model="form.totalScore"
            :min="0"
            :precision="2"
            :step="0.1"
            controls-position="right"
            style="width: 200px"
            @change="handleTotalScoreChange"
          />
          <span style="margin-left: 10px; color: #909399;">分</span>
          <el-checkbox
            v-model="form.autoCalculateTotalScore"
            style="margin-left: 20px;"
            @change="handleAutoCalculateChange"
          >
            自动计算总分
          </el-checkbox>
          <span v-if="form.autoCalculateTotalScore" style="margin-left: 10px; color: #67C23A;">
            当前总分：{{ calculatedTotalScore.toFixed(2) }} 分
          </span>
        </el-form-item>

        <el-form-item label="开场独白音频">
          <el-upload
            :action="uploadMediaUrl"
            :headers="uploadHeaders"
            :before-upload="handleIntroAudioBeforeUpload"
            :on-success="handleIntroAudioSuccess"
            :on-error="handleMediaUploadError"
            :on-remove="handleIntroAudioRemove"
            :limit="1"
            :file-list="introAudioFileList"
          >
            <el-button size="small" type="primary">上传音频</el-button>
            <div slot="tip" class="el-upload__tip">只能上传音频文件，且不超过10MB，仅支持一个文件，二次上传将直接替换</div>
          </el-upload>
        </el-form-item>

        <el-form-item label="开场独白文本">
          <el-input v-model="form.introText" type="textarea" :rows="3" placeholder="请输入开场独白文本（可选）" />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="7">
            <el-form-item label="是否自动跳转">
              <el-radio-group v-model="form.autoNextQuestion">
                <el-radio :label="1">是</el-radio>
                <el-radio :label="0">否</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="7">
            <el-form-item label="是否显示答案">
              <el-radio-group v-model="form.showAnswerImmediately">
                <el-radio :label="1">是</el-radio>
                <el-radio :label="0">否</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="7">
            <el-form-item label="是否允许回顾">
              <el-radio-group v-model="form.allowReview">
                <el-radio :label="1">是</el-radio>
                <el-radio :label="0">否</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="每题读题时长" prop="questionReadDuration">
          <el-input-number v-model="form.questionReadDuration" :min="0" :max="300" controls-position="right" style="width: 200px" />
          <span style="margin-left: 10px; color: #909399;">秒（用于自动跳转）</span>
        </el-form-item>

        <el-form-item label="练习次数限制" prop="practiceLimit">
          <el-input-number v-model="form.practiceLimit" :min="0" :max="999" controls-position="right" style="width: 200px" />
          <span style="margin-left: 10px; color: #909399;">次（0表示不限制）</span>
        </el-form-item>

        <el-form-item label="试听功能">
          <el-row :gutter="20">
            <el-col :span="6">
              <el-switch
                v-model="form.trialListenEnabled"
                :active-value="1"
                :inactive-value="0"
                active-text="启用"
                inactive-text="禁用"
              />
            </el-col>
            <el-col :span="18" v-if="form.trialListenEnabled === 1">
              <el-input
                v-model="form.trialListenText"
                type="textarea"
                :rows="2"
                placeholder="请输入试听提示文案"
                style="width: 100%"
              />
            </el-col>
          </el-row>
        </el-form-item>

        <el-form-item label="注意事项" prop="notes">
          <el-input
            v-model="form.notes"
            type="textarea"
            :rows="4"
            placeholder="请输入注意事项（支持富文本）"
          />
          <div style="margin-top: 10px;">
            <span style="color: #909399; margin-right: 20px;">显示时机：</span>
            <el-radio-group v-model="form.notesDisplayMode">
              <el-radio label="before_exam">考试前显示一次</el-radio>
              <el-radio label="before_section">每大题前显示</el-radio>
            </el-radio-group>
          </div>
        </el-form-item>

        <el-form-item label="状态" prop="status" required>
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="请输入备注" />
        </el-form-item>

        <el-form-item label="题目列表">
          <div class="selected-questions-container">
            <!-- 操作栏 -->
            <div class="selected-questions-header">
              <span class="question-count">
                已选择 <strong>{{ selectedQuestions.length }}</strong> 题
                <span v-if="form.totalScore && form.totalScore > 0" style="margin-left: 10px; color: #909399;">
                  （剩余可分配：{{ remainingScore.toFixed(2) }} 分）
                </span>
              </span>
              <div class="header-actions">
                <el-button type="primary" size="small" icon="el-icon-plus" @click="handleSelectQuestions">
                  选择题库题目
                </el-button>
                <el-button
                  v-if="selectedQuestions.length > 0"
                  type="danger"
                  size="small"
                  icon="el-icon-delete"
                  @click="handleClearAllQuestions"
                >
                  清空全部
                </el-button>
                <el-button
                  v-if="selectedQuestions.length > 0"
                  type="warning"
                  size="small"
                  icon="el-icon-setting"
                  @click="handleBatchSetScore"
                >
                  批量设置分值
                </el-button>
                <el-button
                  v-if="selectedQuestions.length > 0 && form.totalScore && form.totalScore > 0"
                  type="info"
                  size="small"
                  icon="el-icon-sort"
                  @click="handleAverageScore"
                >
                  平均分配
                </el-button>
              </div>
            </div>

            <!-- 题目表格 -->
            <el-table
              v-if="selectedQuestions.length > 0"
              :data="selectedQuestions"
              border
              max-height="400"
              row-key="id"
              class="selected-questions-table"
              :show-overflow-tooltip="true"
            >
              <el-table-column label="序号" width="60" align="center" fixed="left">
                <template slot-scope="scope">
                  <span>{{ scope.$index + 1 }}</span>
                </template>
              </el-table-column>
              <el-table-column label="题目标题" prop="title" min-width="250" show-overflow-tooltip />
              <el-table-column label="题目类型" prop="type" width="100" align="center">
                <template slot-scope="scope">
                  <dict-tag :options="dict.type.question_type" :value="scope.row.type" />
                </template>
              </el-table-column>
              <el-table-column label="学科" prop="subjectId" width="80" align="center">
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
                    :controls="true"
                    controls-position="right"
                    size="small"
                    style="width: 100px;"
                    @change="handleQuestionScoreChange(scope.row, scope.$index)"
                  />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="80" align="center" fixed="right">
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

            <!-- 空状态 -->
            <el-empty
              v-else
              description="暂未选择题目"
              :image-size="100"
            />
          </div>
        </el-form-item>
      </el-form>

      <div class="form-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="handleCancel">取 消</el-button>
      </div>
    </el-card>

    <!-- 题目选择弹窗 -->
    <el-dialog title="选择题库题目" :visible.sync="questionSelectVisible" width="1200px" append-to-body>
      <splitpanes :horizontal="false" class="default-theme">
        <!-- 左侧分类树 -->
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
        <!-- 右侧题目列表 -->
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
import { addPaper, generatePaperPackage } from "@/api/exam/paper"
import { getQuestionList } from "@/api/exam/question"
import { getCategoryTree } from "@/api/exam/questionCategory"
import { getToken } from "@/utils/auth"
import { Splitpanes, Pane } from "splitpanes"
import "splitpanes/dist/splitpanes.css"

export default {
  name: "PaperAdd",
  components: { Splitpanes, Pane },
  dicts: ['question_type', 'subject', 'question_type_default_score', 'paper_type', 'paper_month', 'paper_province'],
  data() {
    return {
      // 年份选项（2000-2050）
      yearOptions: (() => {
        const years = []
        for (let i = 2000; i <= 2050; i++) {
          years.push(i)
        }
        return years
      })(),
      // 表单参数
      form: {
        paperName: undefined, // 试卷名称
        year: undefined, // 不设置默认值，让用户选择
        month: undefined,
        province: undefined,
        paperType: undefined,
        customName: undefined,
        paperDesc: undefined,
        duration: 60,
        totalScore: undefined,
        introAudioUrl: undefined,
        introAudioPath: undefined,
        introAudioDuration: undefined,
        introText: undefined,
        autoNextQuestion: 1,
        showAnswerImmediately: 0,
        allowReview: 0,
        questionReadDuration: 0,
        practiceLimit: 0,
        trialListenEnabled: 0,
        trialListenText: undefined,
        notes: undefined,
        notesDisplayMode: 'before_exam',
        status: 1,
        remark: undefined,
        autoCalculateTotalScore: false
      },
      // 表单校验
      rules: {
        paperName: [
          { required: true, message: "试卷名称不能为空", trigger: "blur" }
        ],
        year: [
          { required: true, message: "年份不能为空", trigger: "blur" },
          { type: 'number', min: 2000, max: 2050, message: "年份必须在2000-2050之间", trigger: "blur" }
        ],
        month: [
          { required: true, message: "月份不能为空", trigger: "change" }
        ],
        province: [
          { required: true, message: "省份不能为空", trigger: "change" }
        ],
        paperType: [
          { required: true, message: "试卷类型不能为空", trigger: "change" }
        ],
        duration: [
          { required: true, message: "考试时长不能为空", trigger: "blur" }
        ],
        status: [
          { required: true, message: "状态不能为空", trigger: "change" }
        ]
      },
      // 上传媒体文件URL
      uploadMediaUrl: process.env.VUE_APP_BASE_API + "/question/media/upload",
      // 上传请求头
      uploadHeaders: {
        Authorization: "Bearer " + getToken()
      },
      // 开场独白音频文件列表
      introAudioFileList: [],
      // 已选择的题目列表（包含 id, title, type, subjectId, score 等字段）
      selectedQuestions: [],
      // 题目选择弹窗
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
      // 默认树属性
      defaultProps: {
        children: "children",
        label: "name"
      }
    }
  },
  computed: {
    /** 计算属性：当前计算出的总分 */
    calculatedTotalScore() {
      return this.selectedQuestions.reduce((sum, q) => {
        return sum + (parseFloat(q.score) || 0)
      }, 0)
    },
    /** 计算属性：剩余可分配分值 */
    remainingScore() {
      if (!this.form.totalScore || this.form.totalScore <= 0) {
        return 0
      }
      return this.calculateRemainingScore()
    }
  },
  created() {
    this.loadQuestionSelectCategoryTree()
  },
  methods: {
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          // 构建题目ID列表和分值列表
          const questionIds = this.selectedQuestions.map(q => q.id)
          const scores = this.selectedQuestions.map(q => parseFloat(q.score) || 0)

          if (questionIds.length === 0) {
            this.$modal.msgWarning("请至少选择一个题目")
            return
          }

          // 验证分值
          if (scores.some(score => isNaN(score) || score < 0)) {
            this.$modal.msgWarning("请为所有题目设置有效的分值")
            return
          }

          // 如果开启了自动计算总分，使用计算出的总分
          if (this.form.autoCalculateTotalScore) {
            this.form.totalScore = this.calculatedTotalScore
          }

          const formData = {
            ...this.form,
            questionIds: questionIds,
            scores: scores
          }

          // paperName作为试卷名称，需要提交给后端
          // 注意：后端会根据year、month、province、paperType、customName自动生成paperName
          // 但这里保留paperName字段，如果用户填写了试卷名称，可以作为参考

          // 调试日志
          console.log('提交的表单数据:', formData)
          console.log('paperName:', formData.paperName)
          console.log('year:', formData.year, 'month:', formData.month, 'province:', formData.province)
          console.log('paperType:', formData.paperType, 'customName:', formData.customName)

          addPaper(formData).then(response => {
            if (response.code === 200) {
              const paperId = response.data || response.id
              this.$modal.msgSuccess("新增成功")
              // 提示是否生成试卷包
              this.$modal.confirm('是否立即生成试卷包？生成过程可能需要一些时间，请耐心等待。').then(() => {
                generatePaperPackage({ id: paperId }).then(packageResponse => {
                  this.$modal.msgSuccess("试卷包生成成功")
                  this.handleCancel()
                }).catch(() => {
                  this.$modal.msgError("试卷包生成失败")
                  this.handleCancel()
                })
              }).catch(() => {
                this.handleCancel()
              })
            } else {
              this.$modal.msgError(response.msg || "新增失败")
            }
          }).catch(error => {
            console.error('新增试卷失败:', error)
            this.$modal.msgError("新增失败：" + (error.message || "未知错误"))
          })
        }
      })
    },
    /** 取消按钮 */
    handleCancel() {
      this.$router.back()
    },
    /** 选择题库题目 */
    handleSelectQuestions() {
      this.questionSelectVisible = true
      this.questionSelectSelectedIds = this.selectedQuestions.map(q => q.id)
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
      const existingIds = this.selectedQuestions.map(q => q.id)
      const newQuestions = this.questionSelectSelectedRows
        .filter(row => !existingIds.includes(row.id))
        .map(row => ({
          id: row.id,
          title: row.title,
          type: row.type,
          subjectId: row.subjectId,
          score: this.calculateDefaultScore(row) // 计算默认分值
        }))

      // 更新已存在的题目信息（如果题目信息有变化）
      this.questionSelectSelectedRows.forEach(row => {
        const existingIndex = this.selectedQuestions.findIndex(q => q.id === row.id)
        if (existingIndex >= 0) {
          // 更新题目信息，但保留已有的分值
          this.$set(this.selectedQuestions[existingIndex], 'title', row.title)
          this.$set(this.selectedQuestions[existingIndex], 'type', row.type)
          this.$set(this.selectedQuestions[existingIndex], 'subjectId', row.subjectId)
        }
      })

      // 添加新题目
      this.selectedQuestions.push(...newQuestions)

      // 如果开启了自动计算总分，更新总分
      if (this.form.autoCalculateTotalScore) {
        this.updateTotalScore()
      }

      this.questionSelectVisible = false
    },
    /** 移除题目 */
    handleRemoveQuestion(index) {
      this.selectedQuestions.splice(index, 1)
      // 如果开启了自动计算总分，更新总分
      if (this.form.autoCalculateTotalScore) {
        this.updateTotalScore()
      }
    },
    /** 清空全部题目 */
    handleClearAllQuestions() {
      this.$modal.confirm('是否确认清空所有已选题目？').then(() => {
        this.selectedQuestions = []
        if (this.form.autoCalculateTotalScore) {
          this.form.totalScore = 0
        }
      }).catch(() => {})
    },
    /** 计算默认分值（方案4：智能推荐法） */
    calculateDefaultScore(question) {
      // 1. 如果试卷设置了总分，使用平均分配
      if (this.form.totalScore && this.form.totalScore > 0) {
        const remainingScore = this.calculateRemainingScore()
        const remainingCount = this.selectedQuestions.length + 1 // 包括当前题目
        if (remainingCount > 0) {
          return parseFloat((remainingScore / remainingCount).toFixed(2))
        }
      }

      // 2. 如果没有设置总分，从字典表读取题目类型默认分值
      const defaultScore = this.getQuestionTypeDefaultScore(question.type)
      return defaultScore
    },
    /** 从字典表获取题目类型默认分值 */
    getQuestionTypeDefaultScore(questionType) {
      // 从字典表 question_type_default_score 读取
      const dictData = this.dict.type.question_type_default_score || []
      const item = dictData.find(d => {
        // dict_value 格式为 "类型值,默认分值"，如 "0,2.0"
        const [type, score] = d.value.split(',')
        return parseInt(type) === questionType
      })

      if (item) {
        const [, score] = item.value.split(',')
        return parseFloat(score) || 1.0
      }

      // 如果字典表中没有配置，使用默认值
      const defaultScoreMap = {
        0: 2.0,  // 单选题
        1: 5.0,  // 多选题
        2: 1.0,  // 判断题
        3: 3.0,  // 填空题
        4: 5.0,  // 排序题
        5: 10.0  // 完形填空
      }
      return defaultScoreMap[questionType] || 1.0
    },
    /** 计算剩余可分配分值 */
    calculateRemainingScore() {
      const totalScore = parseFloat(this.form.totalScore) || 0
      const usedScore = this.selectedQuestions.reduce((sum, q) => {
        return sum + (parseFloat(q.score) || 0)
      }, 0)
      return Math.max(0, totalScore - usedScore)
    },
    /** 更新总分（自动计算） */
    updateTotalScore() {
      if (this.form.autoCalculateTotalScore) {
        const total = this.selectedQuestions.reduce((sum, q) => {
          return sum + (parseFloat(q.score) || 0)
        }, 0)
        this.form.totalScore = parseFloat(total.toFixed(2))
      }
    },
    /** 题目分值变化 */
    handleQuestionScoreChange(question, index) {
      // 如果开启了自动计算总分，更新总分
      if (this.form.autoCalculateTotalScore) {
        this.updateTotalScore()
      }
    },
    /** 总分变化 */
    handleTotalScoreChange() {
      // 如果总分变化且开启了自动计算，重新平均分配
      if (this.form.autoCalculateTotalScore && this.selectedQuestions.length > 0) {
        this.handleAverageScore()
      }
    },
    /** 自动计算总分开关变化 */
    handleAutoCalculateChange() {
      if (this.form.autoCalculateTotalScore) {
        this.updateTotalScore()
      }
    },
    /** 平均分配分值 */
    handleAverageScore() {
      if (!this.form.totalScore || this.form.totalScore <= 0) {
        this.$modal.msgWarning('请先设置试卷总分')
        return
      }

      if (this.selectedQuestions.length === 0) {
        this.$modal.msgWarning('请先选择题目')
        return
      }

      const averageScore = parseFloat((this.form.totalScore / this.selectedQuestions.length).toFixed(2))
      this.selectedQuestions.forEach(question => {
        this.$set(question, 'score', averageScore)
      })

      this.$modal.msgSuccess(`已平均分配，每题 ${averageScore} 分`)
    },
    /** 批量设置分值 */
    handleBatchSetScore() {
      if (this.selectedQuestions.length === 0) {
        this.$modal.msgWarning('请先选择题目')
        return
      }

      this.$prompt('请输入分值（所有题目将设置为相同分值）', '批量设置分值', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputPattern: /^\d+(\.\d{1,2})?$/,
        inputErrorMessage: '请输入有效的分值（支持小数点后2位）',
        inputPlaceholder: '请输入分值，如：5.0'
      }).then(({ value }) => {
        const score = parseFloat(value)
        if (isNaN(score) || score < 0) {
          this.$modal.msgError('请输入有效的分值')
          return
        }

        this.selectedQuestions.forEach(question => {
          this.$set(question, 'score', score)
        })

        // 如果开启了自动计算总分，更新总分
        if (this.form.autoCalculateTotalScore) {
          this.updateTotalScore()
        }

        this.$modal.msgSuccess(`已批量设置，每题 ${score} 分`)
      }).catch(() => {})
    },
    /** 开场独白音频上传前校验 */
    handleIntroAudioBeforeUpload(file) {
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
    /** 开场独白音频上传成功 */
    handleIntroAudioSuccess(response, file) {
      if (response.code === 200) {
        this.form.introAudioUrl = response.data.url
        this.form.introAudioPath = response.data.path
        this.$modal.msgSuccess("上传成功")
      } else {
        this.$modal.msgError(response.msg || "上传失败")
      }
    },
    /** 开场独白音频移除 */
    handleIntroAudioRemove() {
      this.form.introAudioUrl = undefined
      this.form.introAudioPath = undefined
    },
    /** 媒体文件上传错误 */
    handleMediaUploadError(err) {
      this.$modal.msgError("上传失败：" + (err.message || "未知错误"))
    },
    /** 过滤节点 */
    filterNode(value, data) {
      if (!value) return true
      return data.name.indexOf(value) !== -1
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
.card-title {
  font-size: 18px;
  font-weight: bold;
}

.form-footer {
  text-align: center;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #e4e7ed;
}

/* 已选题目容器样式 */
.selected-questions-container {
  width: 100%;
}

.selected-questions-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  padding: 10px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.question-count {
  font-size: 14px;
  color: #606266;
}

.question-count strong {
  color: #409EFF;
  font-size: 16px;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.selected-questions-table {
  margin-top: 10px;
  width: 100%;
}

/* 去除表格横向滚动条，使用自适应列宽 */
.selected-questions-table ::v-deep .el-table__body-wrapper {
  overflow-x: hidden !important;
}

.selected-questions-table ::v-deep .el-table {
  width: 100% !important;
}

/* 空状态样式 */
::v-deep .el-empty {
  padding: 40px 0;
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

