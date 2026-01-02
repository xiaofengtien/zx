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
            <!-- 题目组 -->
            <template v-else-if="scope.row.type === 'question_group'">
              <i
                :class="isGroupExpanded(scope.row.groupId) ? 'el-icon-folder-opened' : 'el-icon-folder'"
                style="color: #9b59b6; margin-right: 5px; cursor: pointer;"
                @click.stop="toggleGroupExpand(scope.row.groupId)"
                :title="isGroupExpanded(scope.row.groupId) ? '点击折叠' : '点击展开'"
              ></i>
              <span style="color: #9b59b6;">{{ scope.row.title || '题目组' }}</span>
              <span style="margin-left: 10px; color: #909399; font-size: 12px;">
                ({{ scope.row.questionCount || 0 }}题)
              </span>
              <el-tag v-if="scope.row.audioUrl" size="mini" type="success" style="margin-left: 8px; cursor: pointer;" @click.stop="previewQuestionGroupAudio(scope.row)" title="点击预览音频">有音频</el-tag>
            </template>
            <!-- 题目 -->
            <template v-else-if="scope.row.type === 'question'">
              <i class="el-icon-question" style="color: #E6A23C; margin-right: 5px;"></i>
              <el-tag v-if="scope.row.groupId" size="mini" type="warning" style="margin-right: 5px;">组内</el-tag>
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
              icon="el-icon-folder-add"
              size="mini"
              style="color: #9b59b6;"
              @click="handleAddGroup(scope.row)"
              title="新增题目组"
            >
              新增题组
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
          <!-- 题目组操作 -->
          <template v-else-if="scope.row.type === 'question_group'">
            <el-button
              type="text"
              icon="el-icon-edit"
              size="mini"
              @click="handleEditGroup(scope.row)"
              title="编辑题目组"
            >
              编辑
            </el-button>
            <el-button
              type="text"
              icon="el-icon-delete"
              size="mini"
              style="color: #f56c6c;"
              @click="handleDeleteGroup(scope.row)"
              title="删除题目组"
            >
              删除
            </el-button>
          </template>
          <!-- 题目组内题目操作：隐藏删除按钮 (优先匹配) -->
          <template v-else-if="scope.row.type === 'question' && scope.row.groupId">
             <!-- 空白，不显示操作按钮 -->
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

    <!-- 题目组编辑对话框 -->
    <el-dialog
      :title="groupDialogTitle"
      :visible.sync="groupDialogVisible"
      width="800px"
      :close-on-click-modal="false"
    >
      <el-form ref="groupForm" :model="groupForm" label-width="100px">
        <el-form-item label="题目组名称">
          <el-input
            v-model="groupForm.groupName"
            placeholder="如：听下面一段独白，回答以下小题"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="题组音频">
          <oss-upload
            v-model="groupAudioUrl"
            :file-list="groupAudioFileList"
            :limit="1"
            accept=".mp3,.wav,.ogg,.m4a,.aac"
            :file-size="10"
            tip="上传组音频后，组内题目将共用此音频"
            path-prefix="exam/question"
            list-type="text"
            @change="handleGroupAudioChange"
            @preview="handleGroupAudioPreview"
          />
        </el-form-item>
        <el-form-item label="组答题时间">
          <el-input-number 
            v-model="groupForm.answerTime" 
            :min="1" 
            :max="300" 
            :placeholder="groupAnswerTimeDefaultValue ? String(groupAnswerTimeDefaultValue) : '秒'"
            style="width: 150px;"
          />
          <span style="margin-left: 10px; color: #909399; font-size: 12px;">秒（留空自动计算：{{ groupAnswerTimeHint }}）</span>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="groupDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="handleGroupSubmit">确 定</el-button>
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
              :row-class-name="getQuestionSelectRowClassName"
              max-height="400"
            >
              <el-table-column type="selection" width="55" align="center" :reserve-selection="true" :selectable="isQuestionSelectable" />
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

    <!-- 题目组选择对话框 -->
    <el-dialog
      title="选择题目组"
      :visible.sync="groupSelectVisible"
      width="1000px"
      append-to-body
    >
      <splitpanes :horizontal="false" class="default-theme">
        <pane size="25">
          <div class="category-tree-container">
            <el-tree
              ref="groupSelectCategoryTree"
              :data="questionSelectCategoryTree"
              :props="defaultProps"
              node-key="id"
              :default-expand-all="true"
              :filter-node-method="filterNode"
              highlight-current
              @node-click="handleGroupSelectCategoryClick"
            >
            </el-tree>
          </div>
        </pane>
        <pane size="75">
          <div class="question-list-container">
            <el-table
              v-loading="groupSelectLoading"
              :data="groupSelectList"
              @selection-change="handleGroupSelectSelectionChange"
              max-height="450"
            >
              <el-table-column type="selection" width="55" align="center" />
              <el-table-column label="题目组名称" prop="groupName" min-width="200" show-overflow-tooltip />
              <el-table-column label="题目数" width="80" align="center">
                <template slot-scope="scope">
                  {{ scope.row.questionCount || 0 }}
                </template>
              </el-table-column>
              <el-table-column label="音频" width="80" align="center">
                <template slot-scope="scope">
                  <el-tag v-if="scope.row.audioUrl" size="mini" type="success">有音频</el-tag>
                  <span v-else>-</span>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </pane>
      </splitpanes>
      <div slot="footer" class="dialog-footer">
        <el-button @click="groupSelectVisible = false">取 消</el-button>
        <el-button type="primary" @click="handleGroupSelectConfirm" :disabled="groupSelectSelected.length === 0">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  getPaperVolumeList, addPaperVolume, updatePaperVolume, deletePaperVolume,
  getPaperSectionList, addPaperSection, updatePaperSection, deletePaperSection,
  getPaperQuestionList, batchSavePaperQuestion,
  updatePaper, getPaper,
  listQuestionGroupsByPaperId, addQuestionGroup, updateQuestionGroup, deleteQuestionGroup
} from "@/api/exam/paper"
import { getQuestionMediaByVolumeId, getQuestionMediaBySectionId } from "@/api/exam/paper"
import { getQuestionList } from "@/api/exam/question"
import { listQuestionGroupByCategory, getQuestionGroupDetail } from "@/api/exam/questionGroup"
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
      questionList: [],
      questionGroupList: [], // 题目组列表
      expandedGroups: [], // 展开的题目组ID列表

      // 题目组编辑
      groupDialogVisible: false,
      groupDialogTitle: '新增题目组',
      groupCurrentSectionId: null, // 当前操作的大题ID
      groupForm: {
        id: undefined,
        sectionId: undefined,
        groupName: undefined, // 题目组名称/标题
        groupOrder: 1,
        selectedQuestionIds: [], // 选中的题目ID数组
        audioUrl: undefined,
        introText: undefined,
        answerTime: null // 组答题时间（秒），null表示使用大题的answer_time
      },
      groupAudioFileList: [],
      groupAudioUrl: null,

      // 题目组选择
      groupSelectVisible: false,
      groupSelectList: [],
      groupSelectLoading: false,
      groupSelectSelected: [],
      groupSelectCurrentSectionId: null
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
      let isVolumeExpanded = false
      let currentGroupId = null
      let isGroupExpanded = false

      for (const row of this.flatTableData) {
        if (row.type === 'volume') {
          currentVolumeId = row.volumeId
          isVolumeExpanded = this.isVolumeExpanded(currentVolumeId)
          currentGroupId = null
          result.push(row)
        } else if (row.type === 'section') {
          if (isVolumeExpanded && row.volumeId === currentVolumeId) {
            currentGroupId = null
            result.push(row)
          }
        } else if (row.type === 'question_group') {
          if (isVolumeExpanded && row.volumeId === currentVolumeId) {
            currentGroupId = row.groupId
            isGroupExpanded = this.isGroupExpanded(currentGroupId)
            result.push(row)
          }
        } else if (row.type === 'question') {
          if (isVolumeExpanded && row.volumeId === currentVolumeId) {
            // 如果题目属于某个题目组
            if (row.groupId) {
              // 只有当题目组展开时才显示组内题目
              if (row.groupId === currentGroupId && isGroupExpanded) {
                result.push(row)
              }
            } else {
              // 不属于题目组的题目直接显示
              result.push(row)
            }
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
      const targetSectionId = this.sectionForm.id !== undefined && this.sectionForm.id !== null
        ? String(this.sectionForm.id)
        : null
      return this.questionList.filter(q => {
        const qSectionId = q.sectionId !== undefined && q.sectionId !== null ? String(q.sectionId) : null
        return qSectionId === targetSectionId
      }).length
    },
    /** 题目组编辑对话框中可选的题目列表 */
    groupAvailableQuestions() {
      if (!this.groupCurrentSectionId) {
        return []
      }
      // 获取当前大题下的所有题目
      const targetSectionId = this.groupCurrentSectionId !== undefined && this.groupCurrentSectionId !== null
        ? String(this.groupCurrentSectionId)
        : null

      const sectionQuestions = this.questionList
        .filter(q => {
          // 将 sectionId 转为字符串比较
          const qSectionId = q.sectionId !== undefined && q.sectionId !== null ? String(q.sectionId) : null
          return qSectionId === targetSectionId
        })
        .sort((a, b) => (a.sectionOrder || 0) - (b.sectionOrder || 0))

      // 编辑模式下，只显示当前组的题目 + 未分组的题目
      // 新增模式下，只显示未分组的题目
      const isEdit = !!this.groupForm.id && !String(this.groupForm.id).startsWith('temp_')
      const currentGroupIdStr = this.groupForm.id !== undefined && this.groupForm.id !== null
        ? String(this.groupForm.id)
        : null

      return sectionQuestions.filter(q => {
        // 如果题目已经属于其他组，不显示
        const qId = String(q.questionId)
        const existingGroup = this.questionGroupList.find(g => {
          if (!g.selectedQuestionIds) return false
          const selectedIds = g.selectedQuestionIds.map(id => String(id))
          if (selectedIds.includes(qId)) {
            // 编辑模式下，当前组的题目要显示
            const gIdStr = g.id !== undefined && g.id !== null ? String(g.id) : null
            if (isEdit && gIdStr === currentGroupIdStr) {
              return false
            }
            return true
          }
          return false
        })
        return !existingGroup
      })
    },
    /** el-transfer 组件需要的数据格式 */
    groupTransferData() {
      // 使用索引重新生成序号，避免 sectionOrder 重复导致显示错误
      return this.groupAvailableQuestions.map((q, index) => ({
        questionId: q.questionId,
        label: `第${index + 1}题: ${q.title || '(无标题)'} (${q.score || 0}分)`,
        sectionOrder: q.sectionOrder,
        title: q.title,
        score: q.score
      }))
    },
    /** 题目组答题时间提示 */
    groupAnswerTimeHint() {
      // 1. 获取题目数量
      const count = this.groupForm.selectedQuestionIds ? this.groupForm.selectedQuestionIds.length : 0
      if (count === 0) return '(该组未包含题目)'

      // 2. 尝试获取大题设定的时间
      let sectionTime = 5
      let suffix = ''
      
      const sectionId = String(this.groupDialogSectionId)
      // 使用更宽松的查找逻辑
      const currentSection = this.sectionList.find(s => 
        (s.id && String(s.id) === sectionId) || 
        (s.tempId && String(s.tempId) === sectionId)
      )
      
      if (currentSection) {
        sectionTime = currentSection.answerTime || 5
      } else {
        // 如果未找到大题，使用默认值但不阻断显示
        suffix = ' (未关联大题, 按默认5秒/题计算)'
      }

      const totalTime = sectionTime * count
      return `默认: ${totalTime}秒 = ${sectionTime}秒/题 × ${count}题${suffix}`
    },
    
    /** 题目组答题时间默认值 (仅用于 placeholder 显示) */
    groupAnswerTimeDefaultValue() {
      const count = this.groupForm.selectedQuestionIds ? this.groupForm.selectedQuestionIds.length : 0
      if (count === 0) return 0

      let sectionTime = 5
      const sectionId = String(this.groupDialogSectionId)
      const currentSection = this.sectionList.find(s => 
        (s.id && String(s.id) === sectionId) || 
        (s.tempId && String(s.tempId) === sectionId)
      )
      
      if (currentSection) {
        sectionTime = currentSection.answerTime || 5
      }
      return sectionTime * count
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
    /**
     * 初始化试卷结构（用于从导入数据初始化）
     * @param {Array} volumes 嵌套结构的卷别数据
     */
    initStructure(volumes) {
      if (!volumes || !Array.isArray(volumes)) return

      this.resetData()
      console.log('初始化试卷结构:', volumes)

      volumes.forEach((vol, vIdx) => {
        // 1. 处理卷别
        const volumeId = `temp_vol_${Date.now()}_${vIdx}`
        // 兼容多种字段格式：camelCase, snake_case
        const audioUrl = vol.volumeAudioUrl || vol.volume_audio_url || vol.audioUrl || vol.audio_url
        const audioPath = vol.volumeAudioPath || vol.volume_audio_path || vol.audioPath || vol.audio_path
        const audioDuration = vol.volumeAudioDuration || vol.volume_audio_duration || vol.duration || vol.audio_duration
        
        console.log(`[initStructure] 卷别[${vIdx}] "${vol.volumeName || vol.name}": audioUrl="${audioUrl}"`)
        
        const volume = {
          id: volumeId,
          paperId: this.paperId,
          volumeName: vol.volumeName || vol.name || `卷${vIdx + 1}`,
          volumeOrder: vol.volumeOrder || (vIdx + 1),
          volumeCode: vol.volumeCode,
          volumeAudioUrl: audioUrl,
          volumeAudioPath: audioPath,
          volumeAudioDuration: audioDuration
        }
        this.volumeList.push(volume)

        // 缓存卷别音频
        if (volume.volumeAudioUrl) {
          this.$set(this.volumeAudioMap, volumeId, {
            url: volume.volumeAudioUrl,
            path: volume.volumeAudioPath,
            duration: volume.volumeAudioDuration
          })
          console.log(`[initStructure] 缓存卷别[${vIdx}]音频到 volumeAudioMap:`, this.volumeAudioMap[volumeId])
        }

        // 2. 处理大题
        const sections = vol.sections || []
        sections.forEach((sec, sIdx) => {
          const sectionId = `temp_sec_${Date.now()}_${vIdx}_${sIdx}`
          const section = {
            id: sectionId,
            paperId: this.paperId,
            volumeId: volumeId, // 关联到临时卷别ID
            volumeCode: volume.volumeCode,
            sectionName: sec.sectionName || sec.name || sec.title || `第${sIdx + 1}节`,
            sectionOrder: sec.sectionOrder || (sIdx + 1),
            instructionText: sec.instructionText || sec.instruction || sec.tintro,
            instructionAudioUrl: sec.instructionAudioUrl || sec.introAudioUrl || sec.intro_audio_url || sec.audioUrl || sec.audio_url,
            instructionAudioPath: sec.instructionAudioPath || sec.introAudioPath || sec.intro_audio_path || sec.audioPath || sec.audio_path,
            instructionAudioDuration: sec.instructionAudioDuration || sec.introAudioDuration || sec.intro_audio_duration || sec.duration,
            scorePerQuestion: sec.scorePerQuestion || 0,
            questionCount: 0, // 后续计算
            totalScore: 0,     // 后续计算
            answerTime: sec.answerTime || 5, // 默认为5秒
            audioPlayCount: sec.audioPlayCount || 1 // 默认为1次
          }
          this.sectionList.push(section)

          // 调试日志：检查大题音频字段
          console.log(`[initStructure] 大题[${sIdx}] "${section.sectionName}":`, {
            instructionAudioUrl: section.instructionAudioUrl,
            instructionAudioDuration: section.instructionAudioDuration
          })

          // 缓存大题音频（只要有 URL 或 duration 就缓存）
          if (section.instructionAudioUrl || section.instructionAudioDuration) {
            this.$set(this.sectionAudioMap, sectionId, {
              url: section.instructionAudioUrl || null,
              path: section.instructionAudioPath || null,
              duration: section.instructionAudioDuration
            })
          }

          // 3. 处理题目组
          // import.vue / add.vue 可能传过来 questionGroups (camelCase) 或 question_groups (snake_case)
          const groups = sec.questionGroups || sec.question_groups || []
          console.log(`[initStructure] 大题 "${section.sectionName}" 的题目组数量:`, groups.length)
          groups.forEach((grp, gIdx) => {
             const groupId = `temp_grp_${Date.now()}_${vIdx}_${sIdx}_${gIdx}`
             
             let libGroupId = grp.questionGroupId || grp.groupId
             if (!libGroupId && grp.id && !String(grp.id).startsWith('temp')) {
               libGroupId = grp.id
             }

             const group = {
               id: groupId,
               sectionId: sectionId, // 关联到临时大题ID
               questionGroupId: libGroupId, // 关联题库ID
               groupOrder: grp.groupOrder || (gIdx + 1),
               audioUrl: grp.audioUrl,
               audioPath: grp.audioPath,
               audioDuration: grp.audioDuration,
               // 优先使用 introText，其次 groupName，再次 title (raw data)
               introText: grp.introText || grp.groupName || grp.title || grp.intro_text,
               // 题目组标题通常就是 groupName，其次是 introText
               title: (grp.groupName || grp.introText || grp.title || '题目组').replace(/【.*?】/g, '').trim(),
               selectedQuestionIds: grp.selectedQuestionIds || grp.questionIds || 
                 (grp.questions ? grp.questions.map(q => q.questionId || q.id).filter(id => id) : []) 
             }
             this.questionGroupList.push(group)
             
             // 将题目组内的题目也添加到 questionList（确保组内题目能在表格中显示）
             if (grp.questions && grp.questions.length > 0) {
               grp.questions.forEach((q, qIdx) => {
                 if (!q.questionId && !q.id) return
                 const question = {
                   questionId: q.questionId || q.id,
                   sectionId: sectionId,
                   sectionOrder: q.sectionOrder || (qIdx + 1),
                   score: q.score || section.scorePerQuestion || 0,
                   title: q.title || q.preview,
                   type: q.type,
                   subjectId: q.subjectId
                 }
                 this.questionList.push(question)
               })
             }
          })
          
          // 4. 处理题目
          // Questions might be directly under section OR derived from groups
          // 导入的数据通常包含 questions 列表
          const questions = sec.questions || []
          questions.forEach((q, qIdx) => {
             // 题目已经入库，必须有 questionId
             if (!q.questionId && !q.id) return
             
             const question = {
               questionId: q.questionId || q.id,
               sectionId: sectionId, // 关联到临时大题ID
               sectionOrder: q.sectionOrder || (qIdx + 1),
               score: q.score || section.scorePerQuestion || 0,
               title: q.title || q.preview, // 可能是 title 或 content/preview
               type: q.type,
               subjectId: q.subjectId
             }
             this.questionList.push(question)
          })
        })
      })

      // 重新计算统计信息
      this.updateSectionQuestionCountFromList()
      this.buildTableData()
    },
    
    /** 更新大题的题目数量统计 */
    updateSectionQuestionCountFromList() {
      this.sectionList.forEach(section => {
        const count = this.questionList.filter(q => q.sectionId === section.id).length
        section.questionCount = count
        // 如果 totalScore 为 0，尝试自动计算
        if (!section.totalScore) {
             const score = this.questionList
                .filter(q => q.sectionId === section.id)
                .reduce((sum, q) => sum + (Number(q.score) || 0), 0)
             section.totalScore = score
        }
      })
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
        // 清除URL中的签名参数（阿里云OSS签名参数），避免重复签名
        let cleanUrl = rawUrl
        if (rawUrl.includes('?') && (rawUrl.includes('Expires=') || rawUrl.includes('Signature='))) {
          cleanUrl = rawUrl.split('?')[0]
          console.log('[resolveMediaPreviewUrl] 清除签名参数后的URL:', cleanUrl)
        }
        const response = await getMediaDownloadUrl({ url: cleanUrl })
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

        // 加载题目组（通过API获取）
        this.questionGroupList = []
        try {
          const groupRes = await listQuestionGroupsByPaperId(this.paperId)
          console.log('加载题目组 API 原始响应:', groupRes)
          if (groupRes.code === 200 && Array.isArray(groupRes.data)) {
            console.log('加载题目组 API 数据:', groupRes.data)
            this.questionGroupList = groupRes.data.map(group => {
              console.log('解析单个组:', group, '-> selectedQuestionIds:', group.selectedQuestionIds)
              return {
                id: group.id,
                sectionId: group.sectionId,
                groupName: group.groupName, // 题目组名称
                groupOrder: group.groupOrder || 1,
                selectedQuestionIds: group.selectedQuestionIds || [],
                audioUrl: group.audioUrl,
                introText: group.introText,
                answerTime: group.answerTime // 组答题时间
              }
            })
            console.log('最终 questionGroupList:', this.questionGroupList)
          }
        } catch (error) {
          console.warn('加载题目组失败:', error)
        }

        // 调试日志：检查数据是否正确加载
        console.log('编辑页面 - 加载数据完成:', {
          paperId: this.paperId,
          volumesCount: volumes.length,
          sectionsCount: this.sectionList.length,
          questionsCount: this.questionList.length,
          questionGroupsCount: this.questionGroupList.length,
          volumes: volumes.map(v => ({ id: v.id, volumeName: v.volumeName, volumeCode: v.volumeCode })),
          sections: this.sectionList.map(s => ({
            id: s.id,
            sectionName: s.sectionName,
            volumeId: s.volumeId,
            volumeIdType: typeof s.volumeId,
            volumeCode: s.volumeCode,
            questionGroupsCount: s.questionGroups ? s.questionGroups.length : 0
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
      // 保留已有的展开状态
      const existingExpanded = this.expandedVolumes || []
      const allVolumeIds = sortedVolumes.map(v => v.id)
      
      // 默认展开所有题目组（无论是新增的还是已有的）
      // 如果之前没有任何展开状态，或者有新加载的数据，确保它们都在展开列表中
      const allGroupIds = this.questionGroupList.map(g => g.id)
      if (this.expandedGroups.length === 0 && allGroupIds.length > 0) {
        this.expandedGroups = [...allGroupIds]
      } else {
        // 确保新增的 temp_ 组也被展开
        const tempGroupIds = allGroupIds.filter(id => String(id).startsWith('temp_'))
        tempGroupIds.forEach(id => {
          if (!this.expandedGroups.includes(id)) {
            this.expandedGroups.push(id)
          }
        })
      }

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
              // 将 sectionId 转为字符串比较，避免类型不匹配
              const qSectionId = q.sectionId !== undefined && q.sectionId !== null ? String(q.sectionId) : null
              const targetSectionId = section.id !== undefined && section.id !== null ? String(section.id) : null
              return qSectionId === targetSectionId
            })
            .sort((a, b) => (a.sectionOrder || 0) - (b.sectionOrder || 0))

          // 获取该大题下的题目组
          const sectionGroups = this.questionGroupList
            .filter(g => {
              const gSectionId = g.sectionId !== undefined && g.sectionId !== null ? String(g.sectionId) : null
              const targetSectionId = section.id !== undefined && section.id !== null ? String(section.id) : null
              return gSectionId === targetSectionId
            })
            .sort((a, b) => (a.groupOrder || 0) - (b.groupOrder || 0))

          this.flatTableData.push({
            id: `section-${section.id}`,
            type: 'section',
            level: 1,
            sectionId: section.id,
            sectionName: section.sectionName,
            sectionOrder: section.sectionOrder,
            questionCount: sectionQuestions.length,
            groupCount: sectionGroups.length,
            totalScore: section.totalScore,
            scorePerQuestion: section.scorePerQuestion,
            instructionText: section.instructionText,
            volumeId: section.volumeId,
            volumeCode: section.volumeCode,
            volumeName: volume.volumeName
          })

          // 如果有题目组，添加题目组行和组内题目
          if (sectionGroups.length > 0) {
            sectionGroups.forEach(group => {
              // 计算组内题目数量（基于 selectedQuestionIds）
              const groupQuestionCount = group.selectedQuestionIds ? group.selectedQuestionIds.length : 0

              this.flatTableData.push({
                id: `group-${group.id}`,
                type: 'question_group',
                level: 2,
                groupId: group.id,
                sectionId: section.id,
                groupOrder: group.groupOrder,
                questionCount: groupQuestionCount,
                audioUrl: group.audioUrl,
                introText: group.introText,
                groupName: group.groupName, // 保存原始 groupName
                title: (group.groupName || group.introText || '题目组').replace(/【.*?】/g, '').trim(), // 用于显示的标题，去除中括号内容
                answerTime: group.answerTime, // 答题时间
                volumeId: volume.id,
                volumeName: volume.volumeName,
                sectionName: section.sectionName
              })

              // 添加组内题目行（基于 selectedQuestionIds）
              const groupQuestions = sectionQuestions.filter(q => {
                if (!group.selectedQuestionIds) return false
                // 将 questionId 转为字符串进行比较
                const qId = String(q.questionId)
                return group.selectedQuestionIds.map(id => String(id)).includes(qId)
              })

              groupQuestions.forEach((q, index) => {
                this.flatTableData.push({
                  id: `question-${q.questionId}-${group.id}`,
                  type: 'question',
                  level: 3,
                  questionId: q.questionId,
                  title: q.title || `题目${index + 1}`,
                  subjectId: q.subjectId,
                  score: parseFloat(q.score) || 0,
                  scorePerQuestion: section.scorePerQuestion || parseFloat(q.score) || 0,
                  sectionOrder: q.sectionOrder || (index + 1),
                  sectionId: q.sectionId || section.id,
                  sectionName: section.sectionName,
                  groupId: group.id,
                  volumeId: volume.id,
                  volumeName: volume.volumeName
                })
              })
            })

            // 添加不属于任何题目组的题目
            const groupedQuestionIds = new Set()
            sectionGroups.forEach(g => {
              if (g.selectedQuestionIds) {
                // 将 questionId 转为字符串存储，避免类型不匹配
                g.selectedQuestionIds.forEach(qId => groupedQuestionIds.add(String(qId)))
              }
            })

            const ungroupedQuestions = sectionQuestions.filter(q => {
              // 将 questionId 转为字符串进行比较，避免类型不匹配
              const qId = String(q.questionId)
              return !groupedQuestionIds.has(qId)
            })

            ungroupedQuestions.forEach((q, index) => {
              this.flatTableData.push({
                id: `question-${q.questionId}-${section.id}`,
                type: 'question',
                level: 2,
                questionId: q.questionId,
                title: q.title || `题目${index + 1}`,
                subjectId: q.subjectId,
                score: parseFloat(q.score) || 0,
                scorePerQuestion: section.scorePerQuestion || parseFloat(q.score) || 0,
                sectionOrder: q.sectionOrder || (index + 1),
                sectionId: q.sectionId || section.id,
                sectionName: section.sectionName,
                volumeId: volume.id,
                volumeName: volume.volumeName
              })
            })
          } else {
            // 没有题目组，直接添加题目行
            sectionQuestions.forEach((q, index) => {
              this.flatTableData.push({
                id: `question-${q.questionId}-${section.id}`,
                type: 'question',
                level: 2,
                questionId: q.questionId,
                title: q.title || `题目${index + 1}`,
                subjectId: q.subjectId,
                score: parseFloat(q.score) || 0,
                scorePerQuestion: section.scorePerQuestion || parseFloat(q.score) || 0,
                sectionOrder: q.sectionOrder || (index + 1),
                sectionId: q.sectionId || section.id,
                sectionName: section.sectionName,
                volumeId: volume.id,
                volumeName: volume.volumeName
              })
            })
          }
        })
      })
    },

    /** 加载音频信息 - 从 paper_volume 和 paper_section 表加载，而不是 question_media */
    async loadAudioInfo(volumes) {
      const volumesToUse = volumes || this.volumeList || []
      console.log('PaperStructureTable: loadAudioInfo called with volumes:', volumesToUse.length)
      // 加载卷别音频
      for (const volume of volumesToUse) {
        let hasAudio = false
        // 1. 优先从 volume 对象中获取（后端已返回）
        if (volume.volumeAudioUrl) {
          console.log(`Volume ${volume.id} has audio in entity:`, volume.volumeAudioUrl)
          this.$set(this.volumeAudioMap, volume.id, {
            url: volume.volumeAudioUrl,
            path: volume.volumeAudioPath || volume.volumeAudioUrl,
            duration: volume.volumeAudioDuration || null
          })
          hasAudio = true
        }

        // 2. 如果没有，尝试从媒体表获取（兜底旧数据或编辑产生的数据）
        if (!hasAudio) {
          try {
            const res = await getQuestionMediaByVolumeId({ volumeId: volume.id })
            if (res.code === 200 && res.data && res.data.length > 0) {
              const audioMedia = res.data.find(m => m.mediaType === 7)
              if (audioMedia) {
                console.log(`Volume ${volume.id} found audio in media table:`, audioMedia.mediaUrl)
                this.$set(this.volumeAudioMap, volume.id, {
                  url: audioMedia.mediaUrl,
                  path: audioMedia.mediaPath,
                  duration: null // 媒体表可能没有时长
                })
              }
            }
          } catch (e) {
            console.error(`Failed to fetch media for volume ${volume.id}`, e)
          }
        }
      }

      // 加载大题音频
      for (const section of this.sectionList) {
        if (section.instructionAudioUrl) {
           // ... existing logic ...
           this.$set(this.sectionAudioMap, section.id, {
            url: section.instructionAudioUrl,
            path: section.instructionAudioPath || section.instructionAudioUrl,
            duration: section.instructionAudioDuration || null
          })
        } else {
             // 也可以为大题添加类似的兜底逻辑
             try {
                const res = await getQuestionMediaBySectionId({ sectionId: section.id })
                if (res.code === 200 && res.data && res.data.length > 0) {
                    const audioMedia = res.data.find(m => m.mediaType === 8) // 假设8是 section intro
                    if (audioMedia) {
                        this.$set(this.sectionAudioMap, section.id, {
                            url: audioMedia.mediaUrl,
                            path: audioMedia.mediaPath,
                            duration: null
                        })
                    }
                }
             } catch(e) {}
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

          // 判断是编辑还是新增：只要在列表中存在，就是编辑（包括 temp_ 开头的临时ID）
          const existingSection = this.sectionList.find(s => s.id === this.sectionForm.id)
          const isEdit = !!existingSection

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

    // ========== 题目组相关方法 ==========

    /** 是否题目组已展开 */
    isGroupExpanded(groupId) {
      return this.expandedGroups.includes(groupId)
    },

    /** 切换题目组展开/折叠 */
    toggleGroupExpand(groupId) {
      const idx = this.expandedGroups.indexOf(groupId)
      if (idx >= 0) {
        this.expandedGroups.splice(idx, 1)
      } else {
        this.expandedGroups.push(groupId)
      }
    },

    /** 新增题目组 */
    /** 新增题目组 */
    handleAddGroup(row) {
      if (!row || !row.sectionId) {
        this.$message.warning("请先保存大题信息");
        return;
      }
      this.groupSelectCurrentSectionId = row.sectionId;
      this.groupSelectVisible = true;
      this.groupSelectSelected = [];

      const initSelection = () => {
        // 默认加载第一个分类的题目组（忽略根节点 "所有分类"）
        if (this.questionSelectCategoryTree.length > 0) {
            const root = this.questionSelectCategoryTree[0];
            let targetNode = null;
            
            // 如果根节点有子节点，选择第一个子节点
            if (root.children && root.children.length > 0) {
               targetNode = root.children[0];
            } else if (this.questionSelectCategoryTree.length > 1) {
               // 如果有多个顶级节点，选择第二个（假设第一个是所有分类）
               targetNode = this.questionSelectCategoryTree[1];
            }
            
            if (targetNode) {
              this.handleGroupSelectCategoryClick(targetNode);
              this.$nextTick(() => {
                if (this.$refs.groupSelectCategoryTree) {
                  this.$refs.groupSelectCategoryTree.setCurrentKey(targetNode.id);
                  // 展开根节点以显示选中项
                  if (this.$refs.groupSelectCategoryTree.store && this.$refs.groupSelectCategoryTree.store.nodesMap[root.id]) {
                    this.$refs.groupSelectCategoryTree.store.nodesMap[root.id].expanded = true;
                  }
                }
              });
            }
        }
      };

      if (this.questionSelectCategoryTree.length === 0) {
        this.loadQuestionSelectCategoryTree().then(() => {
          initSelection();
        });
      } else {
        initSelection();
      }
    },

    /** 题目组选择 - 分类点击 */
    /** 题目组选择 - 分类点击 */
    handleGroupSelectCategoryClick(data) {
      // 禁止选中根节点（假设ID为0或label为所有分类，或者就是树的第一个节点）
      // 使用更稳健的判断：如果该节点是根节点（在tree的顶层，或者ID为0）
      const isRoot = this.questionSelectCategoryTree.length > 0 && this.questionSelectCategoryTree[0].id === data.id;
      if (isRoot || data.id === 0 || data.label === '所有分类') {
         return; 
      }
      
      console.log('handleGroupSelectCategoryClick 调试:', data);
      this.groupSelectLoading = true;
      listQuestionGroupByCategory(data.id).then(res => {
        console.log('listQuestionGroupByCategory 响应:', res);
        this.groupSelectList = res.data || [];
      }).catch(err => {
        console.error('listQuestionGroupByCategory 错误:', err);
      }).finally(() => {
        this.groupSelectLoading = false;
      });
    },

    /** 题目组选择 - 选择变化 */
    handleGroupSelectSelectionChange(selection) {
      this.groupSelectSelected = selection;
    },

    /** 题目组选择 - 确认 */
    /** 题目组选择 - 确认 */
    handleGroupSelectConfirm() {
      if (this.groupSelectSelected.length === 0) {
        this.$message.warning("请选择题目组");
        return;
      }
      
      this.groupSelectLoading = true;
      const loading = this.$loading({
        lock: true,
        text: '正在加载题目组数据...',
        spinner: 'el-icon-loading',
        background: 'rgba(0, 0, 0, 0.7)'
      });

      // 并行获取所选题目组的详细信息（包含题目）
      const promises = this.groupSelectSelected.map(group => {
        return getQuestionGroupDetail(group.id).then(res => res.data);
      });

      Promise.all(promises).then(groups => {
        // 更新本地数据，通过 temp_ 前缀标识为新增
        groups.forEach(groupDetail => {
           if (!groupDetail) return;
           
           // 检查是否已存在（防止重复添加）
           // 检查所有现有的题目组，看是否有相同的 questionGroupId
           const isDuplicate = this.questionGroupList.some(g => String(g.questionGroupId) === String(groupDetail.id));
           if (isDuplicate) {
             return;
           }
           
           const timestamp = Date.now();
           const tempGroupId = `temp_group_${groupDetail.id}_${timestamp}`; // 使用 temp ID 防止与现有 ID 冲突（如果是新保存）
           // 注意：这里我们保留原始 questionGroupId，但在 paperStructure 中使用 temp ID 作为主键
           // 实际上，如果是选择已有题目组，questionGroupId 就是 groupDetail.id
           // 但为了区分我们在 currentSection 下添加的 PaperQuestionGroup 行，我们通常不需要 temp ID 除非是新建 PaperQuestionGroup 记录
           // 后端 PaperQuestionGroupController.add 会返回新 ID。
           // 纯前端模式下，我们构造一个 PaperQuestionGroup 对象结构

           const newGroup = {
             id: 'temp_' + timestamp + '_' + Math.floor(Math.random() * 1000), // 临时 ID
             sectionId: this.groupSelectCurrentSectionId,
             questionGroupId: groupDetail.id,
             groupOrder: (this.questionGroupList.length + 1), // 简单排序，实际应基于 section 内
             selectedQuestionIds: groupDetail.questions ? groupDetail.questions.map(q => q.id) : [],
             audioUrl: groupDetail.audioUrl,
             audioPath: groupDetail.audioPath,
             audioDuration: groupDetail.audioDuration,
             introText: groupDetail.description
           };
           this.questionGroupList.push(newGroup);

           // 添加题目到 questionList
           if (groupDetail.questions && groupDetail.questions.length > 0) {
             groupDetail.questions.forEach((q, index) => {
               // 检查是否已存在（避免重复添加，虽然不同大题下可以重复？）
               // 题目实体通常是唯一的，但在试卷结构中，题目是通过 PaperQuestionGroup 关联的。
               // this.questionList 用于 buildTableData。
               // 如果是同一个题目引用，可以直接 push。但需要注意 q.id
               // buildTableData 获取题目是基于 sectionId 过滤。
               // 这里的题目 q 需要有 sectionId 属性吗？
               // 是的，buildTableData 中 sectionQuestions = this.questionList.filter(q => q.sectionId === section.id)
               // 所以我们需要把 fetch 下来的题目也是加上 sectionId
               
               // 注意：如果题目对象来自 QuestionGroupDetail，它可能没有 sectionId (因为它不属于任何 section until added)
               // 我们需要 clone 一份，并加上 sectionId
               
               const newQuestion = {
                 ...q,
                 questionId: q.id, // 确保有 questionId
                 sectionId: this.groupSelectCurrentSectionId, // 关联到当前大题
                 sectionOrder: index + 1
               };
               this.questionList.push(newQuestion);
             });
           }
           
           // 自动展开
           this.expandedGroups.push(newGroup.id);
        });

        this.$message.success("添加成功（待保存）");
        this.groupSelectVisible = false;
        this.buildTableData(); // 刷新表格视图
         
      }).catch(err => {
        this.$message.error("获取题目组详情失败：" + err.message);
      }).finally(() => {
        loading.close();
        this.groupSelectLoading = false;
      });
    },

    /** 编辑题目组 */
    /** 重置题目组表单 */
    resetGroupForm() {
      this.groupForm = {
        id: undefined,
        sectionId: undefined,
        groupName: undefined,
        groupOrder: 1,
        selectedQuestionIds: [],
        audioUrl: undefined,
        introText: undefined,
        answerTime: undefined
      }
      this.groupAudioFileList = []
      this.groupAudioUrl = null
      if (this.$refs.groupForm) {
        this.$refs.groupForm.resetFields()
      }
    },

    /** 编辑题目组 */
    handleEditGroup(row) {
      console.log('[Debug] handleEditGroup row:', row)
      const group = this.questionGroupList.find(g => g.id === row.groupId)
      console.log('[Debug] found group:', group)
      
      if (!group) return

      this.resetGroupForm()
      
      this.groupDialogTitle = "编辑题目组"
      // 优先使用 row.sectionId (来自 flatTableData)
      let sectionId = row.sectionId
      if (sectionId === undefined || sectionId === null) {
        sectionId = group.sectionId
      }
      
      // 最后的救命稻草：如果ID丢失，尝试通过 sectionName 匹配大题
      if ((sectionId === undefined || sectionId === null) && row.sectionName) {
         console.warn('[Debug] sectionId missing, trying to find by name:', row.sectionName)
         const foundSection = this.sectionList.find(s => s.sectionName === row.sectionName || s.name === row.sectionName)
         if (foundSection) {
            sectionId = foundSection.id
            console.log('[Debug] Recovered sectionId from name:', sectionId)
         }
      }

      console.log('[Debug] determined sectionId:', sectionId)
      
      this.groupDialogSectionId = sectionId 
      
      this.groupForm = {
        id: group.id,
        sectionId: sectionId,
        // 如果 groupName 为空，使用 cleaned introText 作为默认值，提升用户体验
        groupName: group.groupName || (group.introText || '').replace(/【.*?】/g, '').trim(), 
        groupOrder: group.groupOrder,
        selectedQuestionIds: [...(group.selectedQuestionIds || [])],
        audioUrl: group.audioUrl,
        // audioPath 通常与 url 一致或是后端字段，前端主要用 url
        introText: group.introText,
        answerTime: group.answerTime
      }
      
      // 初始化音频文件列表用于组件回显
      if (group.audioUrl) {
         this.groupAudioFileList = [{
           name: this.getFileNameFromUrl(group.audioUrl),
           url: group.audioUrl
         }]
         this.groupAudioUrl = group.audioUrl // 确保 oss-upload 组件 v-model 同步
      } else {
         this.groupAudioFileList = []
         this.groupAudioUrl = null
      }
      
      this.groupDialogVisible = true
    },

    /** 删除题目组 */
    handleDeleteGroup(row) {
      const group = this.questionGroupList.find(g => g.id === row.groupId)
      const questionCount = group && group.selectedQuestionIds ? group.selectedQuestionIds.length : 0
      this.$modal.confirm(`确定要删除该题目组吗？（包含${questionCount}道题目）`).then(() => {
        // 1. 从 questionList 中移除该题目组包含的题目
        if (group && group.selectedQuestionIds && group.selectedQuestionIds.length > 0) {
           const idsToRemove = group.selectedQuestionIds.map(id => String(id));
           // 过滤掉属于该组的题目
           this.questionList = this.questionList.filter(q => !idsToRemove.includes(String(q.questionId)));
        }

        // 2. 从本地数据中删除题目组
        const groupIndex = this.questionGroupList.findIndex(g => g.id === row.groupId)
        if (groupIndex >= 0) {
          this.questionGroupList.splice(groupIndex, 1)
        }
        // 更新表格数据
        this.buildTableData()
        this.$modal.msgSuccess("删除成功（待保存）")
      }).catch(() => {})
    },

    /** 获取下一个题目组序号 */
    getNextGroupOrder(sectionId) {
      const groups = this.questionGroupList.filter(g => g.sectionId === sectionId)
      if (groups.length === 0) return 1
      return Math.max(...groups.map(g => g.groupOrder || 0)) + 1
    },

    /** el-transfer 搜索过滤方法 */
    filterGroupQuestions(query, item) {
      if (!query) return true
      const lowerQuery = query.toLowerCase()
      return (
        item.label.toLowerCase().includes(lowerQuery) ||
        (item.title && item.title.toLowerCase().includes(lowerQuery))
      )
    },

    /** 题目组音频变更 */
    handleGroupAudioChange(fileList) {
      if (fileList && fileList.length > 0) {
        this.groupForm.audioUrl = fileList[0].url || fileList[0].response?.url
      } else {
        this.groupForm.audioUrl = null
      }
      this.groupAudioFileList = fileList
    },

    /** 预览题目组音频（从表格行点击） */
    async previewQuestionGroupAudio(row) {
      const audioUrl = row.audioUrl
      if (!audioUrl) {
        this.$modal.msgWarning('该题目组没有音频')
        return
      }
      try {
        const url = await this.resolveMediaPreviewUrl(audioUrl)
        if (url) {
          const previewFile = {
            url: url,
            name: this.getFileNameFromUrl(audioUrl)
          }
          console.log('题目组音频预览 - 触发 preview-audio 事件:', previewFile)
          this.$emit('preview-audio', previewFile)
        }
      } catch (error) {
        console.error('预览题目组音频失败:', error)
        this.$modal.msgError('音频预览失败')
      }
    },

    /** 预览题目组音频（从对话框点击） */
    async handleGroupAudioPreview(file) {
      try {
        const url = await this.resolveMediaPreviewUrl(file.url || file.response?.url)
        if (url) {
          const previewFile = {
            ...file,
            url: url,
            name: file?.name || this.getFileNameFromUrl(url)
          }
          console.log('题目组对话框音频预览 - 触发 preview-audio 事件:', previewFile)
          this.$emit('preview-audio', previewFile)
        }
      } catch (error) {
        console.error('预览音频失败:', error)
        this.$modal.msgError('音频预览失败')
      }
    },

    /** 题目组表单提交 */
    /** 题目组表单提交 */
    /** 题目组表单提交 */
    handleGroupSubmit() {
      // 移除题目数量验证，允许仅修改名称/时间
      // if (this.groupForm.selectedQuestionIds.length < 2) { ... }

      // 判断是否存在于列表中，存在即为编辑（即使是临时ID）
      const existingGroup = this.questionGroupList.find(g => g.id === this.groupForm.id)
      const isEdit = !!existingGroup

      if (isEdit) {
        // 编辑：更新本地数据
        if (existingGroup) {
          existingGroup.groupName = this.groupForm.groupName
          existingGroup.groupOrder = this.groupForm.groupOrder
          existingGroup.selectedQuestionIds = [...this.groupForm.selectedQuestionIds]
          existingGroup.audioUrl = this.groupForm.audioUrl
          existingGroup.introText = this.groupForm.introText
          existingGroup.answerTime = this.groupForm.answerTime
          existingGroup.answerTime = this.groupForm.answerTime
        }
        
        // 展开新增/编辑的题目组
        if (!this.expandedGroups.includes(this.groupForm.id)) {
          this.expandedGroups = [...this.expandedGroups, this.groupForm.id]
        }
        this.$modal.msgSuccess("修改成功（待保存）")
      } else {
         // ...
      }
      this.groupDialogVisible = false
      this.buildTableData()
    },


    /** 加载大题题目列表（带分页） */
    loadSectionQuestions(sectionId) {
      if (!sectionId) {
        sectionId = this.currentSectionId
      }
      if (!sectionId) return

      // 将传入的 sectionId 转为字符串，确保类型匹配
      const targetSectionId = sectionId !== undefined && sectionId !== null ? String(sectionId) : null

      // 调试日志：检查过滤条件
      console.log('loadSectionQuestions 调试:', {
        inputSectionId: sectionId,
        targetSectionId: targetSectionId,
        questionListLength: this.questionList.length,
        questionListSampleSectionIds: this.questionList.slice(0, 3).map(q => ({ 
          questionId: q.questionId, 
          sectionId: q.sectionId, 
          sectionIdType: typeof q.sectionId,
          sectionIdStr: String(q.sectionId)
        }))
      })

      // 确保使用正确的 sectionId 过滤（必须严格匹配，不能使用默认值）
      const allQuestions = this.questionList
        .filter(q => {
          // 严格匹配：题目必须有 sectionId，且必须等于传入的 sectionId
          const qSectionId = q.sectionId !== undefined && q.sectionId !== null ? String(q.sectionId) : null
          return qSectionId === targetSectionId
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

      console.log('loadSectionQuestions 结果:', {
        targetSectionId: targetSectionId,
        filteredQuestionsCount: allQuestions.length,
        pageNum: this.sectionQuestionsQueryParams.pageNum,
        pageSize: this.sectionQuestionsQueryParams.pageSize
      })

      // 更新总数
      this.sectionQuestionsTotal = allQuestions.length

      // 分页处理
      const { pageNum, pageSize } = this.sectionQuestionsQueryParams
      const start = (pageNum - 1) * pageSize
      const end = start + pageSize
      this.sectionQuestions = allQuestions.slice(start, end)
      
      console.log('loadSectionQuestions 最终:', {
        start: start,
        end: end,
        sectionQuestionsLength: this.sectionQuestions.length
      })
    },

    /** 管理题目（独立弹窗） */
    handleManageQuestions(sectionRow) {
      // 重置分页参数，确保从第一页开始
      this.sectionQuestionsQueryParams.pageNum = 1
      
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
      const targetQuestionId = String(row.questionId)
      
      // 弹出确认框
      this.$modal.confirm(`确定要删除该题目吗？`).then(() => {
        // 1. 从 questionGroupList 中移除该题目ID（如果存在于任何组中）
        this.questionGroupList.forEach(group => {
          if (group.selectedQuestionIds && group.selectedQuestionIds.length > 0) {
            const qIndex = group.selectedQuestionIds.findIndex(id => String(id) === targetQuestionId)
            if (qIndex >= 0) {
              group.selectedQuestionIds.splice(qIndex, 1)
            }
          }
        })
        
        // 2. 清理空的题目组（题目数为0的组自动删除）
        this.questionGroupList = this.questionGroupList.filter(group => {
          return group.selectedQuestionIds && group.selectedQuestionIds.length > 0
        })
        
        // 3. 从 questionList 中删除题目
        const qIndex = this.questionList.findIndex(q => String(q.questionId) === targetQuestionId)
        if (qIndex >= 0) {
          this.questionList.splice(qIndex, 1)
        }
        
        // 4.1 重新排序题目
        this.reorderSectionQuestions(row.sectionId)
        
        // 4.2 更新表格数据
        this.buildTableData()
        
        // 5. 更新大题的题目数量
        this.updateSectionQuestionCountFromList()
        
        // 6. 如果开启了自动计算总分，实时计算
        if (this.autoCalculateTotalScore) {
          this.calculateAndEmitTotalScore()
        }
        
        this.$modal.msgSuccess("删除成功（待保存）")
      }).catch(() => {})
    },

    /** 选择题库题目（独立弹窗） */
    handleSelectQuestions() {
      this.questionSelectVisible = true
      // 恢复已选择的题目（从所有题目中恢复，不仅仅是当前页）
      const targetSectionId = this.currentSectionId !== undefined && this.currentSectionId !== null
        ? String(this.currentSectionId)
        : null
      const allSectionQuestions = this.questionList
        .filter(q => {
          const qSectionId = q.sectionId !== undefined && q.sectionId !== null ? String(q.sectionId) : null
          return qSectionId === targetSectionId
        })
      this.questionSelectSelectedIds = allSectionQuestions.map(q => q.questionId)
      if (!this.questionSelectCategoryTree.length) {
        this.loadQuestionSelectCategoryTree()
      }
      this.getQuestionSelectList()
    },

    /** 加载题目选择分类树 */
    loadQuestionSelectCategoryTree() {
      return getCategoryTree({}).then(response => {
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
        return response
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

    /** 判断题目是否可选（已添加到任何大题的题目不可选） */
    isQuestionSelectable(row) {
      // 检查题目是否已经添加到任何大题（包括当前大题）
      const rowId = String(row.id)

      // 检查是否已经在任意大题中
      const alreadyAdded = this.questionList.some(q => {
        const qId = String(q.questionId)
        const qSectionId = q.sectionId !== undefined && q.sectionId !== null ? String(q.sectionId) : null
        // 如果题目在任何大题中，则不可选
        return qId === rowId && qSectionId !== null
      })

      return !alreadyAdded
    },

    /** 获取题目选择表格行样式 */
    getQuestionSelectRowClassName({ row }) {
      if (!this.isQuestionSelectable(row)) {
        return 'disabled-row'
      }
      return ''
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

      // 重新排序题目
      this.reorderSectionQuestions(this.currentSectionId)

      // 重新构建表格数据，确保新添加的题目显示出来
      this.buildTableData()

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

    /** 重新排序大题内的所有题目 */
    reorderSectionQuestions(sectionId) {
      if (!sectionId) return
      
      const targetSectionId = String(sectionId)
      // 获取该大题下所有题目
      const sectionQuestions = this.questionList.filter(q => {
        const qSectionId = q.sectionId !== undefined && q.sectionId !== null ? String(q.sectionId) : null
        return qSectionId === targetSectionId
      })
      
      // 按照现有顺序排序
      sectionQuestions.sort((a, b) => (a.sectionOrder || 0) - (b.sectionOrder || 0))
      
      // 重新生成序号
      sectionQuestions.forEach((q, index) => {
        q.sectionOrder = index + 1
      })
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
        }
        // 添加音频时长（优先从缓存读取，其次从 section 对象读取）
        const duration = (audioInfo && audioInfo.duration) || section.instructionAudioDuration
        console.log(`[getStructureData] section "${section.sectionName}" duration:`, {
          'audioInfo?.duration': audioInfo?.duration,
          'section.instructionAudioDuration': section.instructionAudioDuration,
          'final duration': duration
        })
        if (duration !== null && duration !== undefined) {
          sectionData.instructionAudioDuration = duration
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
          
          // 为每个 section 添加其 questionGroups（题目组）
          const sectionGroups = this.questionGroupList
            .filter(g => String(g.sectionId) === sectionKey)
            .map(g => {
              // 修复：如果 answerTime 为 null 且组内有题目，计算默认值
              let groupAnswerTime = g.answerTime
              if ((groupAnswerTime === null || groupAnswerTime === undefined) && g.selectedQuestionIds && g.selectedQuestionIds.length > 0) {
                // 使用大题设定的单题答题时间 * 题目数量
                groupAnswerTime = (section.answerTime || 5) * g.selectedQuestionIds.length
              }

              return {
                id: g.id && !String(g.id).startsWith('temp_') ? g.id : undefined,
                tempId: g.id && String(g.id).startsWith('temp_') ? g.id : undefined,
                sectionId: g.sectionId,
                questionGroupId: g.questionGroupId, // 关联题库题目组ID
                groupName: g.groupName || (g.introText || '').replace(/【.*?】/g, '').trim(), // 题目组名称 (如果未设置，使用introText作为默认值)
                groupOrder: g.groupOrder,
                startQuestionNum: g.startQuestionNum,
                endQuestionNum: g.endQuestionNum,
                selectedQuestionIds: g.selectedQuestionIds || [],
                audioUrl: g.audioUrl,
                audioPath: g.audioPath || g.audioUrl, // path 和 url 相同
                audioDuration: g.audioDuration, // 音频时长
                introText: g.introText,
                answerTime: groupAnswerTime // 组答题时间
              }
            })
          section.questionGroups = sectionGroups
          
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
        questionGroupsCount: this.questionGroupList.length,
        // 详细打印题目组数据，检查 selectedQuestionIds 是否存在
        questionGroupsCheck: this.questionGroupList.map(g => ({
          id: g.id,
          sectionId: g.sectionId,
          selectedQuestionIds: g.selectedQuestionIds,
          selectedIdsLen: g.selectedQuestionIds ? g.selectedQuestionIds.length : 0
        })),
        volumes: allVolumes.map(v => ({
          id: v.id,
          tempId: v.tempId,
          volumeName: v.volumeName,
          sectionsCount: v.sections ? v.sections.length : 0,
          sections: v.sections ? v.sections.map(s => ({
            id: s.id,
            tempId: s.tempId,
            sectionName: s.sectionName,
            questionsCount: s.questions ? s.questions.length : 0,
            questionGroupsCount: s.questionGroups ? s.questionGroups.length : 0
          })) : []
        })),
        totalQuestions: this.questionList.length,
        totalGroups: this.questionGroupList.length
      })

      return {
        volumes: allVolumes, // 嵌套结构：volumes -> sections -> questions -> questionGroups
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

/* 已添加到其他大题的题目禁用样式 */
::v-deep .el-table .disabled-row {
  background-color: #f5f5f5 !important;
  color: #c0c4cc;
  cursor: not-allowed;
}

::v-deep .el-table .disabled-row .el-checkbox {
  pointer-events: none;
}
</style>

