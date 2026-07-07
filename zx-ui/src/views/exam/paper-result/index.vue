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
      <el-form-item label="学号" prop="studentAccount">
        <el-input
          v-model="queryParams.studentAccount"
          placeholder="请输入学号"
          clearable
          style="width: 240px"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="提交状态" prop="submitStatus">
        <el-select v-model="queryParams.submitStatus" placeholder="请选择提交状态" clearable style="width: 240px">
          <el-option label="已提交" value="1" />
          <el-option label="未提交" value="0" />
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
          type="info"
          plain
          icon="el-icon-view"
          size="mini"
          :disabled="single"
          @click="handleView"
        >查看详情</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          :disabled="multiple"
          @click="handleExport"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table
      ref="table"
      v-loading="loading"
      :data="resultList"
      @selection-change="handleSelectionChange"
      @row-click="handleRowClick"
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="试卷名称" prop="paperName" min-width="200" show-overflow-tooltip />
      <el-table-column label="试卷编码" prop="paperCode" width="180" align="center" />
      <el-table-column label="学号" prop="studentAccount" width="120" align="center" />
      <el-table-column label="总分" prop="totalScore" width="100" align="center">
        <template slot-scope="scope">
          <span>{{ scope.row.totalScore || 0 }}</span>
        </template>
      </el-table-column>
      <el-table-column label="得分" prop="score" width="100" align="center">
        <template slot-scope="scope">
          <span>{{ scope.row.score || 0 }}</span>
        </template>
      </el-table-column>
      <el-table-column label="正确率" prop="correctRate" width="100" align="center">
        <template slot-scope="scope">
          <span>{{ scope.row.correctRate || 0 }}%</span>
        </template>
      </el-table-column>
      <el-table-column label="提交状态" prop="submitStatus" width="100" align="center">
        <template slot-scope="scope">
          <el-tag :type="scope.row.submitStatus === 1 ? 'success' : 'info'">
            {{ scope.row.submitStatus === 1 ? '已提交' : '未提交' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="提交时间" prop="submitTime" width="180" align="center">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.submitTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="120" fixed="right">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-view"
            @click.stop="handleView(scope.row)"
          >查看详情</el-button>
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

    <!-- 答题详情对话框 -->
    <el-dialog title="答题详情" :visible.sync="detailVisible" width="1200px" append-to-body>
      <div v-if="detailData">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="试卷名称">{{ detailData.paperName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="试卷编码">{{ detailData.paperCode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="学号">{{ detailData.studentAccount || '-' }}</el-descriptions-item>
          <el-descriptions-item label="总分">{{ detailData.totalScore || 0 }}</el-descriptions-item>
          <el-descriptions-item label="得分">{{ detailData.score || 0 }}</el-descriptions-item>
          <el-descriptions-item label="正确率">{{ detailData.correctRate || 0 }}%</el-descriptions-item>
          <el-descriptions-item label="提交状态">
            <el-tag :type="detailData.submitStatus === 1 ? 'success' : 'info'">
              {{ detailData.submitStatus === 1 ? '已提交' : '未提交' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ detailData.submitTime ? parseTime(detailData.submitTime) : '-' }}</el-descriptions-item>
        </el-descriptions>
        <el-divider />
        <div v-if="detailData.questionResults && detailData.questionResults.length > 0">
          <h4>题目答题情况：</h4>
          <el-table :data="detailData.questionResults" border>
            <el-table-column label="序号" type="index" width="60" align="center" />
            <el-table-column label="题目标题" prop="title" min-width="200" show-overflow-tooltip />
            <el-table-column label="题目类型" prop="type" width="100" align="center">
              <template slot-scope="scope">
                <dict-tag :options="dict.type.question_type" :value="scope.row.type" />
              </template>
            </el-table-column>
            <el-table-column label="用户答案" prop="userAnswer" width="150" align="center" show-overflow-tooltip />
            <el-table-column label="正确答案" prop="correctAnswer" width="150" align="center" show-overflow-tooltip />
            <el-table-column label="结果" prop="result" width="80" align="center">
              <template slot-scope="scope">
                <el-tag :type="scope.row.result === 1 ? 'success' : 'danger'">
                  {{ scope.row.result === 1 ? '正确' : '错误' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="用时" prop="timeSpent" width="80" align="center">
              <template slot-scope="scope">
                <span>{{ scope.row.timeSpent || 0 }}秒</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100" align="center" fixed="right">
              <template slot-scope="scope">
                <el-button
                  size="mini"
                  type="text"
                  @click="handleViewQuestionDetail(scope.row)"
                >查看详情</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <div v-else style="text-align: center; color: #909399; padding: 20px;">
          暂无答题记录
        </div>
      </div>
    </el-dialog>

    <!-- 题目详情对话框 -->
    <el-dialog title="题目详情" :visible.sync="questionDetailVisible" width="800px" append-to-body>
      <div v-if="questionDetailData">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="题目标题">
            <div v-html="questionDetailData.title"></div>
          </el-descriptions-item>
          <el-descriptions-item label="题目类型">
            <dict-tag :options="dict.type.question_type" :value="questionDetailData.type" />
          </el-descriptions-item>
          <el-descriptions-item label="用户答案">
            <span :class="questionDetailData.result === 1 ? 'correct-answer' : 'wrong-answer'">
              {{ questionDetailData.userAnswer || '未作答' }}
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="正确答案">
            <span class="correct-answer">{{ questionDetailData.correctAnswer }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="结果">
            <el-tag :type="questionDetailData.result === 1 ? 'success' : 'danger'">
              {{ questionDetailData.result === 1 ? '正确' : '错误' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="用时">
            <span>{{ questionDetailData.timeSpent || 0 }}秒</span>
          </el-descriptions-item>
          <el-descriptions-item label="解析" v-if="questionDetailData.analyzes">
            <div v-html="questionDetailData.analyzes"></div>
          </el-descriptions-item>
        </el-descriptions>
        <el-divider v-if="questionDetailData.options && questionDetailData.options.length > 0" />
        <div v-if="questionDetailData.options && questionDetailData.options.length > 0">
          <h4>选项列表：</h4>
          <el-table :data="questionDetailData.options" border>
            <el-table-column label="选项" prop="optionName" width="80" align="center" />
            <el-table-column label="选项内容" prop="optionContent" min-width="200" />
            <el-table-column label="是否正确答案" prop="isAnswer" width="120" align="center">
              <template slot-scope="scope">
                <el-tag :type="scope.row.isAnswer === 2 ? 'success' : 'info'">
                  {{ scope.row.isAnswer === 2 ? '是' : '否' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="用户选择" width="100" align="center">
              <template slot-scope="scope">
                <el-tag v-if="isUserSelected(scope.row)" type="warning">已选择</el-tag>
                <span v-else style="color: #909399;">未选择</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getPaperResultList, getPaperResult, exportPaperResult } from "@/api/exam/paperResult"

export default {
  name: "PaperResult",
  dicts: ['question_type'],
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
      // 答题结果表格数据
      resultList: [],
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        paperName: undefined,
        studentAccount: undefined, // 学号（学员账号）
        submitStatus: undefined
      },
      // 详情对话框
      detailVisible: false,
      detailData: null,
      // 题目详情对话框
      questionDetailVisible: false,
      questionDetailData: null,
      // 导出loading
      exportLoading: false
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** 查询答题结果列表 */
    getList() {
      this.loading = true
      console.log('答题结果页面 - 调用 getPaperResultList，参数：', this.queryParams)
      getPaperResultList(this.queryParams).then(response => {
        console.log('答题结果页面 - 接口响应：', response)
        this.resultList = response.rows || []
        this.total = response.total || 0
        this.loading = false
      }).catch(error => {
        console.error('答题结果页面 - 接口错误：', error)
        this.loading = false
        this.resultList = []
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
      this.queryParams = {
        pageNum: 1,
        pageSize: 10,
        paperName: undefined,
        studentAccount: undefined,
        submitStatus: undefined
      }
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
    /** 查看详情按钮操作 */
    handleView(row) {
      const id = row ? row.id : this.ids[0]
      const listRow = row || this.resultList.find(item => item.id === id)
      getPaperResult({ id }).then(response => {
        // 合并答题结果详情和列表数据
        const resultDetail = response.data
        this.detailData = {
          ...listRow,
          questionResults: resultDetail ? resultDetail.questionResults : []
        }
        this.detailVisible = true
      })
    },
    /** 查看题目详情 */
    handleViewQuestionDetail(row) {
      this.questionDetailData = row
      this.questionDetailVisible = true
    },
    /** 判断用户是否选择了该选项 */
    isUserSelected(option) {
      if (!this.questionDetailData || !this.questionDetailData.userAnswerIds) {
        return false
      }
      const userAnswerIds = this.questionDetailData.userAnswerIds.split(',').map(id => parseInt(id))
      return userAnswerIds.includes(option.id)
    },
    /** 导出按钮操作 */
    handleExport() {
      this.$modal.confirm('是否确认导出所有答题结果数据项？').then(() => {
        this.exportLoading = true
        return exportPaperResult(this.queryParams)
      }).then(response => {
        this.$download.excel(response, '答题结果.xlsx')
        this.exportLoading = false
      }).catch(() => {
        this.exportLoading = false
      })
    }
  }
}
</script>

<style scoped>
.correct-answer {
  color: #67c23a;
  font-weight: bold;
}
.wrong-answer {
  color: #f56c6c;
  font-weight: bold;
}
</style>

