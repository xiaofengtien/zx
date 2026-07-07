<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="试卷名称" prop="paperName">
        <el-input
          v-model="queryParams.paperName"
          placeholder="请输入试卷名称"
          clearable
          style="width: 240px"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="试卷类型" prop="paperType">
        <el-select v-model="queryParams.paperType" placeholder="请选择试卷类型" clearable style="width: 240px">
          <el-option
            v-for="dict in dict.type.paper_type"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 240px">
          <el-option label="启用" value="1" />
          <el-option label="禁用" value="0" />
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
          v-hasPermi="['paper:paper:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-edit"
          size="mini"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['paper:paper:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['paper:paper:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="info"
          plain
          icon="el-icon-view"
          size="mini"
          :disabled="single"
          @click="handlePreview"
        >预览</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          :disabled="single"
          @click="handleGeneratePackage"
        >生成试卷包</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-download"
          size="mini"
          :disabled="single"
          :loading="downloadLoading"
          @click="handleDownloadPackage"
        >下载试卷包</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table
      ref="table"
      v-loading="loading"
      :data="paperList"
      @selection-change="handleSelectionChange"
      @row-click="handleRowClick"
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="试卷名称" prop="paperName" min-width="160" show-overflow-tooltip />
      <el-table-column label="试卷编码" prop="paperCode" width="150"  show-overflow-tooltip/>
      <el-table-column label="试卷类型" prop="paperType" width="80" show-overflow-tooltip>
        <template slot-scope="scope">
          <dict-tag :options="dict.type.paper_type" :value="scope.row.paperType" />
        </template>
      </el-table-column>
      <el-table-column label="题目总数" prop="totalQuestions" width="80" align="center" />
      <el-table-column label="总分" prop="totalScore" width="80" align="center">
        <template slot-scope="scope">
          <span>{{ scope.row.totalScore || 0 }}</span>
        </template>
      </el-table-column>
      <el-table-column label="考试时长" prop="duration" width="80" align="center">
        <template slot-scope="scope">
          <span>{{ scope.row.duration || 0 }}分钟</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" prop="status" width="80" align="center">
        <template slot-scope="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'">
            {{ scope.row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" prop="createTime" width="180" align="center">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="280" fixed="right">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click.stop="handleUpdate(scope.row)"
            v-hasPermi="['paper:paper:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-view"
            @click.stop="handlePreview(scope.row)"
          >预览</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-setting"
            @click.stop="handleGeneratePackage(scope.row)"
          >生成试卷包</el-button>
          <el-button
            v-if="scope.row.packageHash || scope.row.lastPackageTime"
            size="mini"
            type="text"
            icon="el-icon-download"
            :loading="scope.row.id === downloadingPaperId"
            @click.stop="handleDownloadPackage(scope.row)"
          >下载试卷包</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click.stop="handleDelete(scope.row)"
            v-hasPermi="['paper:paper:remove']"
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

    <!-- 添加或修改试卷对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="95%" :close-on-click-modal="false" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="试卷名称" prop="paperName">
          <el-input v-model="form.paperName" placeholder="请输入试卷名称" />
        </el-form-item>
        <el-form-item label="试卷描述" prop="paperDesc">
          <el-input v-model="form.paperDesc" type="textarea" :rows="3" placeholder="请输入试卷描述" />
        </el-form-item>
        <el-form-item label="考试时长" prop="duration">
          <el-input-number v-model="form.duration" :min="1" :max="999" controls-position="right" style="width: 200px" />
          <span style="margin-left: 10px; color: #909399;">分钟</span>
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
        <el-form-item label="是否自动跳转">
          <el-radio-group v-model="form.autoNextQuestion">
            <el-radio :label="1">是</el-radio>
            <el-radio :label="0">否</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="是否立即显示答案">
          <el-radio-group v-model="form.showAnswerImmediately">
            <el-radio :label="1">是</el-radio>
            <el-radio :label="0">否</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="是否允许回顾">
          <el-radio-group v-model="form.allowReview">
            <el-radio :label="1">是</el-radio>
            <el-radio :label="0">否</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="每题读题时长" prop="questionReadDuration">
          <el-input-number v-model="form.questionReadDuration" :min="0" :max="300" controls-position="right" style="width: 200px" />
          <span style="margin-left: 10px; color: #909399;">秒（用于自动跳转）</span>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
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
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 题目选择弹窗 -->
    <el-dialog title="选择题库题目" :visible.sync="questionSelectVisible" width="1200px" append-to-body>
      <splitpanes :horizontal="false" class="default-theme">
        <!-- 左侧分类树 -->
        <pane size="20">
          <div class="head-container" style="padding: 10px;">
            <el-input
              v-model="questionSelectCategoryName"
              placeholder="请输入分类名称"
              clearable
              size="small"
              prefix-icon="el-icon-search"
              style="margin-bottom: 10px"
            />
            <el-tree
              :data="questionSelectCategoryTree"
              :props="defaultProps"
              :expand-on-click-node="false"
              :filter-node-method="filterNode"
              ref="questionSelectCategoryTree"
              node-key="id"
              default-expand-all
              highlight-current
              :current-node-key="questionSelectCategoryId"
              @node-click="handleQuestionSelectCategoryClick"
            />
          </div>
        </pane>
        <!-- 右侧题目列表 -->
        <pane size="80">
          <div style="padding: 10px;">
            <el-form :model="questionSelectQueryParams" ref="questionSelectQueryForm" size="small" :inline="true" label-width="80px">
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
            <div style="margin-bottom: 10px;">
              <span>已选择：{{ questionSelectSelectedIds.length }}题</span>
            </div>
            <el-table
              ref="questionSelectTable"
              v-loading="questionSelectLoading"
              :data="questionSelectList"
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

    <!-- 预览对话框 -->
    <el-dialog title="试卷预览" :visible.sync="previewVisible" width="1200px" append-to-body class="paper-preview-dialog">
      <div v-if="previewData" v-loading="previewLoading" class="paper-preview-container">
        <!-- 第一部分：试卷基本信息 -->
        <div class="paper-basic-info">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="试卷名称">{{ previewData.paperName }}</el-descriptions-item>
          <el-descriptions-item label="试卷编码">{{ previewData.paperCode }}</el-descriptions-item>
          <el-descriptions-item label="题目总数">{{ previewData.totalQuestions || 0 }}</el-descriptions-item>
          <el-descriptions-item label="总分">{{ previewData.totalScore || 0 }}</el-descriptions-item>
          <el-descriptions-item label="考试时长">{{ previewData.duration || 0 }}分钟</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="previewData.status === 1 ? 'success' : 'danger'">
              {{ previewData.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="试卷类型">
            <dict-tag :options="dict.type.paper_type" :value="previewData.paperType" />
          </el-descriptions-item>
          <el-descriptions-item label="试卷启用时间" :span="2">
            <span v-if="previewData.enableStartTime && previewData.enableEndTime">
              {{ previewData.enableStartTime }} 至 {{ previewData.enableEndTime }}
            </span>
            <span v-else-if="previewData.enableStartTime">
              {{ previewData.enableStartTime }} 起
            </span>
            <span v-else-if="previewData.enableEndTime">
              至 {{ previewData.enableEndTime }}
            </span>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ previewData.createTime }}</el-descriptions-item>
        </el-descriptions>
        </div>

        <!-- 第二部分：试卷结构（Tab 平铺） -->
        <div class="paper-structure-container">
          <el-tabs v-model="previewActiveTab" type="border-card" v-if="previewVolumeList && previewVolumeList.length > 0">
            <el-tab-pane
              v-for="volume in previewVolumeList"
              :key="volume.id"
              :label="volume.volumeName || volume.volumeCode"
              :name="String(volume.id)"
            >
              <div class="volume-content">
                <div
                  v-for="section in getVolumeSections(volume.id)"
                  :key="section.id"
                  class="section-card"
                >
                  <el-card shadow="hover">
                    <div slot="header" class="section-header">
                      <span class="section-icon">📋</span>
                      <span class="section-title">{{ section.sectionName }}</span>
                      <span class="section-meta">
                        <el-tag size="small" type="info">{{ section.questionCount || 0 }}题</el-tag>
                        <el-tag size="small" type="success" style="margin-left: 8px;">总分：{{ section.totalScore || 0 }}分</el-tag>
                      </span>
                    </div>
                    <div v-if="section.instructionText" class="section-instruction">
                      <i class="el-icon-info"></i>
                      <span>{{ section.instructionText }}</span>
                    </div>
                    <div class="questions-list">
                      <div
                        v-for="(question, index) in getSectionQuestions(section.id)"
                        :key="question.questionId || index"
                        class="question-item"
                      >
                        <div class="question-header">
                          <span class="question-number">题目{{ index + 1 }}：</span>
                          <span class="question-title">{{ question.title || '暂无标题' }}</span>
                        </div>
                        <div class="question-meta">
                          <el-tag size="mini" type="primary" v-if="question.type">
                            <dict-tag :options="dict.type.question_type" :value="question.type" />
                          </el-tag>
                          <span class="question-score">分值：{{ question.score || 0 }}分</span>
                        </div>
                        <!-- 题目选项 -->
                        <div v-if="question.answers && question.answers.length > 0" class="question-options">
                          <div class="options-title">选项：</div>
                          <div class="options-list">
                            <div
                              v-for="(answer, optIndex) in question.answers"
                              :key="answer.id || optIndex"
                              class="option-item"
                            >
                              <span class="option-label">{{ getOptionLabel(optIndex) }}.</span>
                              <span class="option-content">{{ answer.optionContent || answer.optionName || '无内容' }}</span>
                              <el-tag v-if="answer.isAnswer === 1" size="mini" type="success" style="margin-left: 8px;">正确答案</el-tag>
                            </div>
                          </div>
                        </div>
                        <!-- 题目解析 -->
                        <div v-if="question.analyzes" class="question-analyzes">
                          <div class="analyzes-title">解析：</div>
                          <div class="analyzes-content">{{ question.analyzes }}</div>
                        </div>
                      </div>
                      <div v-if="getSectionQuestions(section.id).length === 0" class="empty-questions">
                        <i class="el-icon-warning"></i>
                        <span>该大题下暂无题目</span>
                      </div>
                    </div>
                  </el-card>
                </div>
                <div v-if="getVolumeSections(volume.id).length === 0" class="empty-sections">
                  <i class="el-icon-warning"></i>
                  <span>该卷别下暂无大题</span>
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
          <div v-else class="empty-structure">
            <i class="el-icon-warning"></i>
            <span>该试卷暂无卷别配置</span>
          </div>
        </div>
      </div>
    </el-dialog>


  </div>
</template>

<script>
import { getPaperList, getPaper, addPaper, updatePaper, deletePaper, getPaperQuestionList, generatePaperPackage, downloadPaperPackage, downloadPaperPackageStream, getPackageTaskStatus, cancelPackageTask } from "@/api/exam/paper"
import { getQuestionList } from "@/api/exam/question"
import { getCategoryTree } from "@/api/exam/questionCategory"
import { getToken } from "@/utils/auth"
import { Splitpanes, Pane } from "splitpanes"
import "splitpanes/dist/splitpanes.css"

export default {
  name: "Paper",
  components: { Splitpanes, Pane },
  dicts: ['question_type', 'subject', 'question_type_default_score', 'paper_type'],
  data() {
    return {
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      // 选中单个
      selectedRow: null,
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 试卷表格数据
      paperList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        paperName: undefined,
        paperType: undefined,
        status: undefined
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        paperName: [
          { required: true, message: "试卷名称不能为空", trigger: "blur" }
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
      // 是否自动计算总分
      autoCalculateTotalScore: false,
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
        label: "name",
        disabled: (data) => {
          return data.isDefault === true || data.isDefault === 1
        }
      },
      // 预览
      previewVisible: false,
      previewData: null,
      previewLoading: false,
      previewActiveTab: '',
      previewVolumeList: [], // 嵌套结构：volumes -> sections -> questions
      // 生成/下载试卷包
      generateLoading: false,
      downloadLoading: false,
      generatingPaperId: null,
      downloadingPaperId: null,
      // 轮询定时器
      pollingTimer: null
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
    this.getList()
    this.loadQuestionSelectCategoryTree()
  },
  methods: {
    /** 查询试卷列表 */
    getList() {
      this.loading = true
      getPaperList(this.queryParams).then(response => {
        this.paperList = response.rows || []
        this.total = response.total || 0
        this.loading = false
      }).catch(() => {
        this.loading = false
        this.paperList = []
        this.total = 0
      })
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
      this.selectedRow = selection.length === 1 ? selection[0] : null
    },
    // 行点击事件
    handleRowClick(row) {
      this.$refs.table.toggleRowSelection(row)
    },
    /** 新增按钮操作 */
    handleAdd() {
      // 新开页面进行新增
      this.$router.push({
        path: '/exam/paper/add',
        query: {}
      })
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      const id = row ? row.id : this.ids[0]
      // 新开页面进行修改
      this.$router.push({
        path: '/exam/paper/edit',
        query: { id: id }
      })
    },
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

          if (this.form.id != null) {
            updatePaper(formData).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addPaper(formData).then(response => {
              this.$modal.msgSuccess("新增成功")
              this.open = false
              this.getList()
            })
          }
        }
      })
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const ids = row ? [row.id] : this.ids
      this.$modal.confirm('是否确认删除试卷编号为"' + ids + '"的数据项？').then(() => {
        return deletePaper({ ids })
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    /** 预览按钮操作 */
    async handlePreview(row) {
      const id = row ? row.id : this.ids[0]
      this.previewLoading = true
      this.previewVisible = true
      
      try {
        // 获取试卷完整数据（嵌套结构：volumes -> sections -> questions）
        const response = await getPaper({ id })
        if (response.code === 200 && response.data) {
        this.previewData = response.data
          
          // 直接使用嵌套结构的数据（volumes 已包含 sections，sections 已包含 questions）
          this.previewVolumeList = response.data.volumes || []
          
          // 设置默认激活的 Tab（第一个卷别）
          if (this.previewVolumeList.length > 0) {
            this.previewActiveTab = String(this.previewVolumeList[0].id)
          }
          } else {
          this.$modal.msgError(response.msg || "获取试卷数据失败")
          this.previewVisible = false
        }
      } catch (error) {
        console.error('获取试卷数据失败:', error)
        this.$modal.msgError("获取试卷数据失败")
        this.previewVisible = false
      } finally {
        this.previewLoading = false
      }
    },
    /** 获取指定卷别下的大题列表（从嵌套结构中直接获取） */
    getVolumeSections(volumeId) {
      if (!this.previewVolumeList || volumeId === undefined || volumeId === null) {
        return []
      }
      
      // 从嵌套结构中直接查找对应的 volume，返回其 sections
      const volume = this.previewVolumeList.find(v => {
        const vId = v.id !== undefined && v.id !== null ? String(v.id) : null
        const targetId = volumeId !== undefined && volumeId !== null ? String(volumeId) : null
        return vId === targetId
      })
      
      if (volume && volume.sections) {
        // 已经按 sectionOrder 排序（后端已排序）
        return volume.sections
      }
      
      return []
    },
    /** 获取指定大题下的题目列表（从嵌套结构中直接获取） */
    getSectionQuestions(sectionId) {
      if (!this.previewVolumeList || sectionId === undefined || sectionId === null) {
        return []
      }
      
      // 从嵌套结构中查找对应的 section，返回其 questions
      for (const volume of this.previewVolumeList) {
        if (volume.sections) {
          const section = volume.sections.find(s => {
            const sId = s.id !== undefined && s.id !== null ? String(s.id) : null
            const targetId = sectionId !== undefined && sectionId !== null ? String(sectionId) : null
            return sId === targetId
          })
          
          if (section && section.questions) {
            // 已经按 sectionOrder 排序（后端已排序）
            return section.questions
          }
        }
      }
      
      return []
    },
    /** 获取选项标签（A, B, C, D...） */
    getOptionLabel(index) {
      return String.fromCharCode(65 + index) // A, B, C, D...
    },
    /** 获取选项标签（A、B、C、D...） */
    getOptionLabel(index) {
      return String.fromCharCode(65 + index) // A=65, B=66, C=67, D=68...
    },
    /** 判断是否为正确答案 */
    isCorrectAnswer(question, answerId) {
      if (!question.answer || !answerId) return false
      // answer 可能是逗号分隔的ID字符串，如 "1,2,3"
      const answerIds = question.answer.split(',').map(id => String(id.trim()))
      return answerIds.includes(String(answerId))
    },
    /** 生成试卷包 */
    handleGeneratePackage(row) {
      const id = row ? row.id : this.ids[0]
      const paperName = row ? row.paperName : this.paperList.find(p => p.id === id)?.paperName || id
      this.$modal.confirm('是否确认生成试卷"' + paperName + '"的试卷包？').then(() => {
        // 不设置loading状态，任务提交后立即可以再次点击
        generatePaperPackage({ id }).then(response => {
          // 从后端获取任务（不再手动添加）
          // 立即加载一次任务列表，确保显示最新任务
          this.$store.dispatch('task/loadAllTasks')
          
          // 打开任务抽屉
          this.$store.dispatch('task/setDrawerVisible', true)
          this.$store.dispatch('task/setActiveTab', 'inProgress')
          
          // 开始轮询任务状态（使用paperId作为标识）
          this.startPolling(id)
          
          this.$message.success('任务已提交,请在右上角任务中心查看进度')
        }).catch((error) => {
          // 检查是否是"正在生成"的错误，如果是则显示黄色警告，否则显示错误
          const errorMsg = error && error.msg ? error.msg : '提交任务失败'
          if (errorMsg.includes('该试卷正在生成试卷包')) {
            this.$message.warning('该试卷正在生成试卷包，请等待')
          } else {
            this.$modal.msgError(errorMsg)
          }
        })
      }).catch(() => {})
    },
    /** 开始轮询任务状态（已废弃：使用MySQL数据库后不再需要定时轮询） */
    startPolling(paperId) {
      // 使用MySQL数据库后，不再需要定时轮询
      // 任务状态已持久化到数据库，用户点击任务通知图标时会查询
      // 只在提交任务后立即查询一次，让用户能看到任务已提交
      this.$store.dispatch('task/loadAllTasks')
    },
    /** 轮询查询任务状态（已废弃，改用loadAllTasks） */
    pollTaskStatus(paperId, taskId) {
      getPackageTaskStatus(paperId).then(response => {
        if (response.data) {
          const taskInfo = response.data
          const progress = taskInfo.progress || 0
          const currentStep = taskInfo.currentStep || '处理中...'
          
          // 更新任务进度
          this.$store.dispatch('task/updateTaskProgress', {
            id: taskId,
            progress: progress,
            currentStep: currentStep
          })
          
          // 检查任务状态
          if (taskInfo.status === 'SUCCESS') {
            // 任务成功
            this.stopPolling()
            this.generatingPaperId = null
            this.$store.dispatch('task/completeTask', {
              id: taskId,
              result: {
                version: taskInfo.newVersion,
                packageHash: taskInfo.packageHash
              }
            })
            this.getList() // 刷新列表
          } else if (taskInfo.status === 'FAILED') {
            // 任务失败
            this.stopPolling()
            this.generatingPaperId = null
            this.$store.dispatch('task/failTask', {
              id: taskId,
              error: taskInfo.errorMessage || '未知错误'
            })
          } else if (taskInfo.status === 'CANCELLED') {
            // 任务已取消
            this.stopPolling()
            this.generatingPaperId = null
            this.$store.dispatch('task/failTask', {
              id: taskId,
              error: '任务已取消'
            })
          }
          // PENDING 和 RUNNING 状态继续轮询
        } else {
          // 任务不存在
          this.stopPolling()
          this.generatingPaperId = null
        }
      }).catch(error => {
        console.error('查询任务状态失败：', error)
        // 查询失败不停止轮询，继续尝试
      })
    },
    /** 停止轮询 */
    stopPolling() {
      if (this.pollingTimer) {
        clearInterval(this.pollingTimer)
        this.pollingTimer = null
      }
    },

    /** 下载试卷包（使用流式下载，支持大文件和进度显示） */
    handleDownloadPackage(row) {
      const id = row ? row.id : this.ids[0]
      const paperName = row ? row.paperName : this.paperList.find(p => p.id === id)?.paperName || id
      if (!row.packageHash && !row.lastPackageTime) {
        this.$modal.msgWarning("该试卷尚未生成试卷包，请先生成试卷包")
        return
      }
      this.downloadingPaperId = id
      
      // 显示下载进度提示
      const loading = this.$loading({
        lock: true,
        text: '正在下载...',
        spinner: 'el-icon-loading',
        background: 'rgba(0, 0, 0, 0.7)'
      })
      
      // 使用流式下载
      downloadPaperPackageStream(id, (downloaded, total) => {
        const percent = total > 0 ? Math.round((downloaded / total) * 100) : 0
        const downloadedMB = (downloaded / 1024 / 1024).toFixed(2)
        const totalMB = (total / 1024 / 1024).toFixed(2)
        loading.text = `正在下载... ${percent}% (${downloadedMB} MB / ${totalMB} MB)`
      }).then(blob => {
        // 验证ZIP文件头（ZIP文件以 PK 开头）
        blob.arrayBuffer().then(buffer => {
          const uint8Array = new Uint8Array(buffer)
          
          if (uint8Array.length < 4) {
            console.error('文件太小，不是有效的ZIP文件')
            loading.close()
            this.$modal.msgError('下载的文件太小，不是有效的ZIP格式')
            this.downloadingPaperId = null
            return
          }

          if (uint8Array[0] !== 0x50 || uint8Array[1] !== 0x4B) {
            // 尝试读取前100个字符，看看是否是JSON错误响应
            const textDecoder = new TextDecoder('utf-8')
            const preview = textDecoder.decode(uint8Array.slice(0, Math.min(100, uint8Array.length)))
            console.error('ZIP文件头不正确，文件预览:', preview)
            loading.close()
            this.$modal.msgError('下载的文件不是有效的ZIP格式，可能是错误响应')
            this.downloadingPaperId = null
            return
          }

          // 创建下载链接
          const fileName = `${paperName}_${row.paperCode || id}.zip`
          const url = window.URL.createObjectURL(blob)
          const link = document.createElement('a')
          link.href = url
          link.download = fileName
          document.body.appendChild(link)
          link.click()
          document.body.removeChild(link)
          window.URL.revokeObjectURL(url)
          
          loading.close()
          this.$modal.msgSuccess("下载成功")
          this.downloadingPaperId = null
        }).catch(error => {
          console.error('处理下载文件失败:', error)
          loading.close()
          this.$modal.msgError('处理下载文件失败：' + (error.message || '未知错误'))
          this.downloadingPaperId = null
        })
      }).catch(error => {
        console.error('下载试卷包失败:', error)
        loading.close()
        this.$modal.msgError('下载失败：' + (error.message || '未知错误'))
        this.downloadingPaperId = null
      })
    },
    /** 重置表单 */
    reset() {
      this.form = {
        id: undefined,
        paperName: undefined,
        paperDesc: undefined,
        duration: 60,
        totalScore: undefined,
        introAudioUrl: undefined,
        introAudioPath: undefined,
        introAudioDuration: undefined,
        introText: undefined,
        autoNextQuestion: 0,
        showAnswerImmediately: 0,
        allowReview: 0,
        questionReadDuration: 0,
        status: 1,
        remark: undefined,
        autoCalculateTotalScore: false
      }
      this.introAudioFileList = []
      this.selectedQuestions = []
      this.autoCalculateTotalScore = false
      this.resetForm("form")
    },
    /** 取消按钮 */
    cancel() {
      this.open = false
      this.reset()
    },
    /** 加载已选择的题目 */
    loadSelectedQuestions(paperId) {
      getPaperQuestionList({ paperId }).then(response => {
        if (response.data && response.data.length > 0) {
          // 需要获取题目的完整信息（包括type, subjectId等）
          // 这里先使用已有数据，后续可以通过题目ID批量查询获取完整信息
          this.selectedQuestions = response.data.map(item => ({
            id: item.questionId,
            title: item.title || `题目${item.questionId}`,
            type: item.type,
            subjectId: item.subjectId,
            score: item.score ? parseFloat(item.score) : this.getQuestionTypeDefaultScore(item.type || 0)
          }))
        } else {
          this.selectedQuestions = []
        }
      }).catch(() => {
        this.selectedQuestions = []
      })
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
  },
  beforeDestroy() {
    // 清理轮询定时器
    this.stopPolling()
  }
}
</script>

<style scoped>
/* 预览对话框样式 */
.paper-preview-dialog ::v-deep .el-dialog__body {
  padding: 20px;
  max-height: 80vh;
  overflow-y: auto;
  overflow-x: hidden;
}

.paper-preview-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 试卷基本信息区域 */
.paper-basic-info {
  flex-shrink: 0;
}

/* 试卷结构容器 */
.paper-structure-container {
  flex: 1;
  min-height: 0; /* 允许 flex 子元素收缩 */
  overflow: visible; /* 移除滚动条，让父容器控制滚动 */
}

.paper-structure-container ::v-deep .el-tabs__content {
  overflow: visible; /* 移除滚动条，让父容器控制滚动 */
}

/* 卷别内容区域 */
.volume-content {
  padding: 10px 0;
}

/* 大题卡片 */
.section-card {
  margin-bottom: 16px;
}

.section-card:last-child {
  margin-bottom: 0;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.section-icon {
  font-size: 18px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  flex: 1;
}

.section-meta {
  display: flex;
  align-items: center;
}

/* 大题说明 */
.section-instruction {
  padding: 12px;
  background-color: #f0f9ff;
  border-left: 4px solid #409eff;
  border-radius: 4px;
  margin-bottom: 16px;
  color: #606266;
  font-size: 14px;
  line-height: 1.5;
}

.section-instruction i {
  margin-right: 6px;
  color: #409eff;
}

/* 题目列表 */
.questions-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* 题目项 */
.question-item {
  padding: 12px;
  background-color: #f5f7fa;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
  transition: all 0.3s;
}

.question-item:hover {
  background-color: #ecf5ff;
  border-color: #b3d8ff;
}

.question-header {
  display: flex;
  align-items: flex-start;
  margin-bottom: 8px;
}

.question-number {
  font-weight: 600;
  color: #409eff;
  margin-right: 8px;
  flex-shrink: 0;
}

.question-title {
  flex: 1;
  color: #303133;
  font-size: 14px;
  line-height: 1.5;
  word-break: break-word;
}

.question-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 8px;
}

.question-score {
  color: #67c23a;
  font-size: 13px;
  font-weight: 500;
}

/* 题目选项 */
.question-options {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #e4e7ed;
}

.options-title {
  font-size: 13px;
  font-weight: 600;
  color: #606266;
  margin-bottom: 8px;
}

.options-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.option-item {
  display: flex;
  align-items: flex-start;
  padding: 6px 8px;
  background-color: #fafafa;
  border-radius: 4px;
  font-size: 13px;
  line-height: 1.5;
}

.option-label {
  font-weight: 600;
  color: #409eff;
  margin-right: 8px;
  flex-shrink: 0;
}

.option-content {
  flex: 1;
  color: #303133;
  word-break: break-word;
}

/* 题目答案（无选项时显示） */
.question-answer {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #e4e7ed;
  font-size: 13px;
}

.answer-label {
  font-weight: 600;
  color: #606266;
  margin-right: 8px;
}

.answer-content {
  color: #303133;
  word-break: break-word;
}

/* 题目解析 */
.question-analyzes {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #e4e7ed;
  font-size: 13px;
}

.analyzes-title {
  font-weight: 600;
  color: #606266;
  margin-bottom: 8px;
}

.analyzes-content {
  color: #303133;
  line-height: 1.6;
  word-break: break-word;
  padding: 8px;
  background-color: #f0f9ff;
  border-radius: 4px;
  border-left: 3px solid #409eff;
}

/* 题目选项 */
.question-options {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #e4e7ed;
}

.options-title {
  font-size: 13px;
  font-weight: 600;
  color: #606266;
  margin-bottom: 8px;
}

.options-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.option-item {
  display: flex;
  align-items: flex-start;
  padding: 6px 8px;
  background-color: #fafafa;
  border-radius: 4px;
  font-size: 13px;
  line-height: 1.5;
}

.option-label {
  font-weight: 600;
  color: #409eff;
  margin-right: 8px;
  flex-shrink: 0;
}

.option-content {
  flex: 1;
  color: #303133;
  word-break: break-word;
}

/* 题目解析 */
.question-analyzes {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #e4e7ed;
}

.analyzes-title {
  font-size: 13px;
  font-weight: 600;
  color: #606266;
  margin-bottom: 8px;
}

.analyzes-content {
  padding: 8px 12px;
  background-color: #f0f9ff;
  border-left: 3px solid #409eff;
  border-radius: 4px;
  font-size: 13px;
  line-height: 1.6;
  color: #606266;
  word-break: break-word;
}

/* 空状态 */
.empty-questions,
.empty-sections,
.empty-structure {
  text-align: center;
  padding: 40px 20px;
  color: #909399;
}

.empty-questions i,
.empty-sections i,
.empty-structure i {
  font-size: 48px;
  margin-bottom: 12px;
  display: block;
  color: #c0c4cc;
}

.empty-questions span,
.empty-sections span,
.empty-structure span {
  font-size: 14px;
}

.head-container {
  padding: 10px;
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

</style>

