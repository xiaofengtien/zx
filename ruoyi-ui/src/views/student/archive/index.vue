<template>
  <div class="app-container">
    <el-row :gutter="20">
      <splitpanes :horizontal="this.$store.getters.device === 'mobile'" class="default-theme">
        <!--学员数据-->
        <pane size="84">
          <el-col>
            <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
              <el-form-item label="学员账号" prop="studentAccount">
                <el-input v-model="queryParams.studentAccount" placeholder="请输入学员账号" clearable style="width: 240px" @keyup.enter.native="handleQuery" />
              </el-form-item>
              <el-form-item label="学员姓名" prop="studentName">
                <el-input v-model="queryParams.studentName" placeholder="请输入学员姓名" clearable style="width: 240px" @keyup.enter.native="handleQuery" />
              </el-form-item>
              <el-form-item label="手机号码" prop="phoneNumber">
                <el-input v-model="queryParams.phoneNumber" placeholder="请输入手机号码" clearable style="width: 240px" @keyup.enter.native="handleQuery" />
              </el-form-item>
              <el-form-item label="学段" prop="grade">
                <el-select v-model="queryParams.grade" placeholder="请选择学段" clearable style="width: 240px">
                  <el-option v-for="dict in dict.type.grade" :key="dict.value" :label="dict.label" :value="dict.value" />
                </el-select>
              </el-form-item>
              <el-form-item label="当前年级" prop="currentGrade">
                <el-select v-model="queryParams.currentGrade" placeholder="请选择当前年级" clearable style="width: 240px">
                  <el-option v-for="dict in queryGradeOptions" :key="dict.value" :label="dict.label" :value="dict.value" />
                </el-select>
              </el-form-item>
<!--              <el-form-item label="适用考卷" prop="applicablePapers">-->
<!--                <el-select v-model="queryParams.applicablePapers" placeholder="请选择适用考卷" multiple clearable collapse-tags style="width: 240px">-->
<!--                  <el-option v-for="dict in dict.type.paper_type" :key="dict.value" :label="dict.label" :value="dict.value" />-->
<!--                </el-select>-->
<!--              </el-form-item>-->
              <el-form-item>
                <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
                <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
              </el-form-item>
            </el-form>

            <el-row :gutter="10" class="mb8">
              <el-col :span="1.5">
                <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['system:archive:add']">新增学员</el-button>
              </el-col>
              <el-col :span="1.5">
                <el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['system:archive:edit']">修改</el-button>
              </el-col>
              <el-col :span="1.5">
                <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['system:archive:remove']">删除</el-button>
              </el-col>
              <el-col :span="1.5">
                <el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['system:user:export']">导出</el-button>
              </el-col>
              <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" :columns="columns"></right-toolbar>
            </el-row>

            <el-table v-loading="loading" :data="archiveList" @selection-change="handleSelectionChange">
              <el-table-column type="selection" width="50" align="center" />
              <el-table-column label="序号" align="center" width="60">
                <template slot-scope="scope">
                  {{ scope.$index + 1 }}
                </template>
              </el-table-column>
              <el-table-column label="学员账号" align="center" key="studentAccount" prop="studentAccount" :show-overflow-tooltip="true">
                <template slot-scope="scope">
                  <el-link type="primary" :underline="false" @click="handleView(scope.row)">
                    {{ scope.row.studentAccount }}
                  </el-link>
                </template>
              </el-table-column>
              <el-table-column label="学员姓名" align="center" key="studentName" prop="studentName" :show-overflow-tooltip="true" />
              <el-table-column label="密码" align="center" key="password" prop="password">
                <template slot-scope="scope">
                  {{ scope.row.password ? '******' : '' }}
                </template>
              </el-table-column>
              <el-table-column label="性别" align="center" key="sex" prop="sex" width="120">
                <template slot-scope="scope">
                  <dict-tag :options="dict.type.sys_user_sex" :value="scope.row.sex"/>
                </template>
              </el-table-column>
              <el-table-column label="手机号" align="center" key="phoneNumber" prop="phoneNumber" width="120" />
              <el-table-column label="学段" align="center" key="grade" prop="grade" width="100">
                <template slot-scope="scope">
                  <dict-tag :options="dict.type.grade" :value="scope.row.grade"/>
                </template>
              </el-table-column>
              <el-table-column label="当前年级" align="center" key="currentGrade" prop="currentGrade" width="120">
                <template slot-scope="scope">
                  {{ getGradeDictLabel(scope.row.grade, scope.row.currentGrade) }}
                </template>
              </el-table-column>
              <el-table-column label="籍贯" align="center" key="hometown" prop="hometown" :show-overflow-tooltip="true" />
              <el-table-column label="考试机位" align="center" key="seatNumber" prop="seatNumber" width="120" />
<!--              <el-table-column label="适用考卷" align="center" key="applicablePapers" width="180">-->
<!--                <template slot-scope="scope">-->
<!--                  <el-tag v-if="scope.row.applicablePapers && scope.row.applicablePapers.length > 0"-->
<!--                    v-for="(paper, index) in (Array.isArray(scope.row.applicablePapers) ? scope.row.applicablePapers : scope.row.applicablePapers.split(','))"-->
<!--                    :key="index"-->
<!--                    size="mini"-->
<!--                    style="margin-right: 5px; margin-bottom: 2px;">-->
<!--                    {{ getDictLabel('paper_type', paper) }}-->
<!--                  </el-tag>-->
<!--                  <span v-else>-</span>-->
<!--                </template>-->
<!--              </el-table-column>-->
              <el-table-column label="操作" align="center" width="300" class-name="small-padding fixed-width">
                <template slot-scope="scope">
                  <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:archive:edit']">修改</el-button>
                  <el-button size="mini" type="text" icon="el-icon-key" @click="handleResetPwd(scope.row)" v-hasPermi="['system:archive:resetPwd']">重置密码</el-button>
                  <el-button size="mini" type="text" icon="el-icon-lock" @click="handleChangePwd(scope.row)" v-hasPermi="['system:archive:changePwd']">修改密码</el-button>
                  <el-button size="mini" type="text" icon="el-icon-refresh" @click="handleResetPractice(scope.row)" v-hasPermi="['student:paper:reset:add']">重置练习</el-button>
                  <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['system:archive:remove']">删除</el-button>
                </template>
              </el-table-column>
            </el-table>

            <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />
          </el-col>
        </pane>
      </splitpanes>
    </el-row>

    <!-- 添加或修改学员档案配置对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="60%" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-row>
          <el-col :span="8">
            <el-form-item label="学员账号" prop="studentAccount">
              <el-input v-model="form.studentAccount" placeholder="请输入学员账号" maxlength="30" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="学员姓名" prop="studentName">
              <el-input v-model="form.studentName" placeholder="请输入学员姓名" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="密码" prop="password">
              <el-input v-model="form.password" placeholder="请输入密码" type="password" maxlength="20" show-password />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="手机号" prop="phoneNumber">
              <el-input v-model="form.phoneNumber" placeholder="请输入手机号码" maxlength="11" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <!-- 占位 -->
          </el-col>
          <el-col :span="8">
            <!-- 占位 -->
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="性别" prop="sex">
              <el-select v-model="form.sex" placeholder="请选择性别" style="width: 100%">
                <el-option v-for="dict in dict.type.sys_user_sex" :key="dict.value" :label="dict.label" :value="dict.value"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="学段" prop="grade">
              <el-select v-model="form.grade" placeholder="请选择学段" style="width: 100%" @change="handleGradeChange">
                <el-option v-for="dict in dict.type.grade" :key="dict.value" :label="dict.label" :value="dict.value"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="当前年级" prop="currentGrade">
              <el-select v-model="form.currentGrade" placeholder="请先选择学段" style="width: 100%" :disabled="!form.grade">
                <el-option v-for="dict in currentGradeOptions" :key="dict.value" :label="dict.label" :value="dict.value"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="籍贯" prop="hometown">
              <el-input v-model="form.hometown" placeholder="请输入籍贯" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="考试机位" prop="seatNumber">
              <el-input v-model="form.seatNumber" placeholder="请输入考试机位号" maxlength="50" />
            </el-form-item>
          </el-col>
          <!-- 适用考卷下拉框已隐藏，改用试卷选择方式 -->
          <!-- <el-col :span="8">
            <el-form-item label="适用考卷" prop="applicablePapers">
              <el-select v-model="form.applicablePapers" placeholder="请选择适用考卷" multiple clearable collapse-tags style="width: 100%">
                <el-option v-for="dict in dict.type.paper_type" :key="dict.value" :label="dict.label" :value="dict.value"></el-option>
              </el-select>
            </el-form-item>
          </el-col> -->
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" placeholder="请输入内容"></el-input>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="适用试卷" prop="applicablePaperIds">
              <div style="margin-bottom: 10px;">
                <el-button type="primary" icon="el-icon-plus" size="small" @click="openPaperSelectDialog">选择试卷</el-button>
              </div>
              <!-- 已选试卷表格 -->
              <el-table
                v-if="selectedPapers.length > 0"
                :data="selectedPapers"
                border
                max-height="300"
                row-key="id"
                class="selected-papers-table"
                :show-overflow-tooltip="true"
              >
                <el-table-column label="序号" width="60" align="center" fixed="left">
                  <template slot-scope="scope">
                    <span>{{ scope.$index + 1 }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="试卷名称" prop="paperName" min-width="200" show-overflow-tooltip />
                <el-table-column label="启用时间" align="center" width="280">
                  <template slot-scope="scope">
                    <span v-if="scope.row.enableStartTime && scope.row.enableEndTime">
                      {{ parseTime(scope.row.enableStartTime, '{y}-{m}-{d} {h}:{i}:{s}') }} 至<br/>
                      {{ parseTime(scope.row.enableEndTime, '{y}-{m}-{d} {h}:{i}:{s}') }}
                    </span>
                    <span v-else-if="scope.row.enableStartTime">
                      {{ parseTime(scope.row.enableStartTime, '{y}-{m}-{d} {h}:{i}:{s}') }} 起
                    </span>
                    <span v-else-if="scope.row.enableEndTime">
                      至 {{ parseTime(scope.row.enableEndTime, '{y}-{m}-{d} {h}:{i}:{s}') }}
                    </span>
                    <span v-else>-</span>
                  </template>
                </el-table-column>
                <el-table-column label="类型" prop="paperType" align="center" width="150">
                  <template slot-scope="scope">
                    <dict-tag :options="dict.type.paper_type" :value="scope.row.paperType" />
                  </template>
                </el-table-column>
                <el-table-column label="版本" prop="version" align="center" width="80" />
                <el-table-column label="试卷包状态" align="center" width="150">
                  <template slot-scope="scope">
                    <el-tag v-if="scope.row.packageHash" type="success" size="small">
                      ✅ 已生成 (v{{ scope.row.version || 1 }})
                    </el-tag>
                    <el-tag v-else type="danger" size="small">❌ 未生成</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="题目总数" prop="totalQuestions" align="center" width="100">
                  <template slot-scope="scope">
                    {{ scope.row.totalQuestions || 0 }}
                  </template>
                </el-table-column>
                <el-table-column label="考试时长" align="center" width="100">
                  <template slot-scope="scope">
                    {{ scope.row.duration || 0 }}分钟
                  </template>
                </el-table-column>
                <el-table-column label="总分" prop="totalScore" align="center" width="100">
                  <template slot-scope="scope">
                    {{ scope.row.totalScore || 0 }}
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="80" align="center" fixed="right">
                  <template slot-scope="scope">
                    <el-button
                      size="mini"
                      type="text"
                      icon="el-icon-delete"
                      @click="handleRemovePaper(scope.$index)"
                    >删除</el-button>
                  </template>
                </el-table-column>
              </el-table>
              <el-empty v-else description="暂未选择试卷" :image-size="80"></el-empty>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 重置密码对话框 -->
    <el-dialog :title="resetPwdTitle" :visible.sync="resetPwdOpen" width="400px" append-to-body>
      <el-form ref="resetPwdForm" :model="resetPwdForm" :rules="resetPwdRules" label-width="100px">
        <el-form-item label="学员账号">
          <el-input v-model="resetPwdForm.studentAccount" :disabled="true" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="resetPwdForm.newPassword" placeholder="请输入新密码，留空则重置为默认密码123456" type="password" show-password maxlength="20" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitResetPwd">确 定</el-button>
        <el-button @click="resetPwdOpen = false">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 修改密码对话框 -->
    <el-dialog :title="changePwdTitle" :visible.sync="changePwdOpen" width="400px" append-to-body>
      <el-form ref="changePwdForm" :model="changePwdForm" :rules="changePwdRules" label-width="100px">
        <el-form-item label="学员账号">
          <el-input v-model="changePwdForm.studentAccount" :disabled="true" />
        </el-form-item>
        <el-form-item label="旧密码" prop="oldPassword">
          <el-input v-model="changePwdForm.oldPassword" placeholder="请输入旧密码" type="password" show-password maxlength="20" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="changePwdForm.newPassword" placeholder="请输入新密码" type="password" show-password maxlength="20" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="changePwdForm.confirmPassword" placeholder="请再次输入新密码" type="password" show-password maxlength="20" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitChangePwd">确 定</el-button>
        <el-button @click="changePwdOpen = false">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 试卷选择对话框 -->
    <el-dialog title="选择试卷" :visible.sync="paperSelectDialogVisible" width="80%" append-to-body>
      <el-table ref="paperTable" v-loading="paperListLoading" :data="paperList" @selection-change="handlePaperSelectionChange">
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="试卷名称" prop="paperName" :show-overflow-tooltip="true" min-width="200" />
        <el-table-column label="年月" align="center" width="120">
          <template slot-scope="scope">
            <span v-if="scope.row.year && scope.row.month">{{ scope.row.year }}年{{ scope.row.month }}月</span>
            <span v-else-if="scope.row.year">{{ scope.row.year }}年</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="省份" prop="province" align="center" width="100">
          <template slot-scope="scope">
            <dict-tag :options="dict.type.paper_province" :value="scope.row.province" v-if="dict.type.paper_province" />
            <span v-else>{{ scope.row.province || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="类型" prop="paperType" align="center" width="150">
          <template slot-scope="scope">
            <dict-tag :options="dict.type.paper_type" :value="scope.row.paperType" />
          </template>
        </el-table-column>
        <el-table-column label="版本" prop="version" align="center" width="80" />
        <el-table-column label="试卷包状态" align="center" width="180">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.packageHash" type="success" size="small">
              ✅ 已生成 (v{{ scope.row.version || 1 }})
            </el-tag>
            <el-tag v-else type="danger" size="small">❌ 未生成</el-tag>
            <div v-if="scope.row.packageHash && scope.row.lastPackageTime" style="font-size: 12px; color: #909399; margin-top: 4px;">
              {{ formatDate(scope.row.lastPackageTime) }}
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="150" fixed="right">
          <template slot-scope="scope">
            <el-button
              v-if="!scope.row.packageHash"
              type="primary"
              size="mini"
              @click="handleGeneratePackage(scope.row)"
              :loading="generatingPaperId === scope.row.id">
              生成试卷包
            </el-button>
            <el-button
              v-else
              type="info"
              size="mini"
              @click="handleRegeneratePackage(scope.row)"
              :loading="generatingPaperId === scope.row.id">
              重新生成
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div slot="footer" class="dialog-footer">
        <el-button @click="paperSelectDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="confirmPaperSelection">确 定</el-button>
      </div>
    </el-dialog>

    <!-- 查看学员详情对话框 -->
    <el-dialog title="查看学员" :visible.sync="viewOpen" width="60%" append-to-body>
      <el-form v-if="viewData" :model="viewData" label-width="80px">
        <el-row>
          <el-col :span="8">
            <el-form-item label="学员账号">
              <span>{{ viewData.studentAccount }}</span>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="学员姓名">
              <span>{{ viewData.studentName || '-' }}</span>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="手机号">
              <span>{{ viewData.phoneNumber }}</span>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="性别">
              <dict-tag :options="dict.type.sys_user_sex" :value="viewData.sex"/>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="学段">
              <dict-tag :options="dict.type.grade" :value="viewData.grade"/>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="当前年级">
              <span>{{ getGradeDictLabel(viewData.grade, viewData.currentGrade) }}</span>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="籍贯">
              <span>{{ viewData.hometown || '-' }}</span>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="考试机位">
              <span>{{ viewData.seatNumber || '-' }}</span>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="备注">
              <span>{{ viewData.remark || '-' }}</span>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="适用试卷">
              <template v-if="viewData.papers && viewData.papers.length > 0">
                <div style="overflow-x: auto;">
                  <el-table
                    :data="viewData.papers"
                    border
                    max-height="300"
                    row-key="id"
                    class="selected-papers-table"
                    :show-overflow-tooltip="true"
                  >
                    <el-table-column label="序号" width="60" align="center" fixed="left">
                      <template slot-scope="scope">
                        <span>{{ scope.$index + 1 }}</span>
                      </template>
                    </el-table-column>
                    <el-table-column label="试卷名称" prop="paperName" min-width="200" show-overflow-tooltip />
                    <el-table-column label="启用时间" align="center" width="280">
                      <template slot-scope="scope">
                        <span v-if="scope.row.enableStartTime && scope.row.enableEndTime">
                          {{ parseTime(scope.row.enableStartTime, '{y}-{m}-{d} {h}:{i}:{s}') }} 至<br/>
                          {{ parseTime(scope.row.enableEndTime, '{y}-{m}-{d} {h}:{i}:{s}') }}
                        </span>
                        <span v-else-if="scope.row.enableStartTime">
                          {{ parseTime(scope.row.enableStartTime, '{y}-{m}-{d} {h}:{i}:{s}') }} 起
                        </span>
                        <span v-else-if="scope.row.enableEndTime">
                          至 {{ parseTime(scope.row.enableEndTime, '{y}-{m}-{d} {h}:{i}:{s}') }}
                        </span>
                        <span v-else>-</span>
                      </template>
                    </el-table-column>
                    <el-table-column label="类型" prop="paperType" align="center" width="150">
                      <template slot-scope="scope">
                        <dict-tag :options="dict.type.paper_type" :value="scope.row.paperType" />
                      </template>
                    </el-table-column>
                    <el-table-column label="版本" prop="version" align="center" width="80" />
                    <el-table-column label="试卷包状态" align="center" width="150">
                      <template slot-scope="scope">
                        <el-tag v-if="scope.row.packageHash" type="success" size="small">
                          ✅ 已生成 (v{{ scope.row.version || 1 }})
                        </el-tag>
                        <el-tag v-else type="danger" size="small">❌ 未生成</el-tag>
                      </template>
                    </el-table-column>
                    <el-table-column label="题目总数" prop="totalQuestions" align="center" width="100">
                      <template slot-scope="scope">
                        {{ scope.row.totalQuestions || 0 }}
                      </template>
                    </el-table-column>
                    <el-table-column label="考试时长" align="center" width="100">
                      <template slot-scope="scope">
                        {{ scope.row.duration || 0 }}分钟
                      </template>
                    </el-table-column>
                    <el-table-column label="总分" prop="totalScore" align="center" width="100">
                      <template slot-scope="scope">
                        {{ scope.row.totalScore || 0 }}
                      </template>
                    </el-table-column>
                  </el-table>
                </div>
              </template>
              <span v-else>暂无绑定试卷</span>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="viewOpen = false">关 闭</el-button>
      </div>
    </el-dialog>

    <!-- 用户导入对话框 -->
    <el-dialog :title="upload.title" :visible.sync="upload.open" width="400px" append-to-body>
      <el-upload ref="upload" :limit="1" accept=".xlsx, .xls" :headers="upload.headers" :action="upload.url + '?updateSupport=' + upload.updateSupport" :disabled="upload.isUploading" :on-progress="handleFileUploadProgress" :on-success="handleFileSuccess" :auto-upload="false" drag>
        <i class="el-icon-upload"></i>
        <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
        <div class="el-upload__tip text-center" slot="tip">
          <div class="el-upload__tip" slot="tip">
            <el-checkbox v-model="upload.updateSupport" />是否更新已经存在的用户数据
          </div>
          <span>仅允许导入xls、xlsx格式文件。</span>
          <el-link type="primary" :underline="false" style="font-size: 12px; vertical-align: baseline" @click="importTemplate">下载模板</el-link>
        </div>
      </el-upload>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitFileForm">确 定</el-button>
        <el-button @click="upload.open = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listArchive, getArchive, getArchiveBatch, delArchive, delArchiveBatch, addArchive, updateArchive, resetArchivePwd, changeArchivePwd, resetPractice } from "@/api/student/archive"
import { getDicts } from "@/api/system/dict/data"
import { getToken } from "@/utils/auth"
import { getPaperList, getPaperListByIds, generatePaperPackage } from "@/api/exam/paper"
import Treeselect from "@riophae/vue-treeselect"
import "@riophae/vue-treeselect/dist/vue-treeselect.css"
import { Splitpanes, Pane } from "splitpanes"
import "splitpanes/dist/splitpanes.css"

export default {
  name: "Archive",
  dicts: ['sys_normal_disable', 'sys_user_sex', 'grade', 'paper_type', 'paper_province'],
  components: { Treeselect, Splitpanes, Pane },
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
      // 学员档案表格数据
      archiveList: null,
      // 弹出层标题
      title: "",
      // 年级下拉选项（根据学段动态加载，用于表单）
      currentGradeOptions: [],
      // 查询条件的年级下拉选项（根据学段动态加载）
      queryGradeOptions: [],
      // 年级字典缓存（key: 学段值, value: 年级选项数组）
      gradeDictCache: {},
      // 所有部门树选项
      deptOptions: undefined,
      // 过滤掉已禁用部门树选项
      enabledDeptOptions: undefined,
      // 是否显示弹出层
      open: false,
      // 是否显示重置密码弹出层
      resetPwdOpen: false,
      // 是否显示修改密码弹出层
      changePwdOpen: false,
      // 是否显示查看弹出层
      viewOpen: false,
      // 查看数据
      viewData: null,
      // 重置密码标题
      resetPwdTitle: "",
      // 修改密码标题
      changePwdTitle: "",
      // 重置密码表单
      resetPwdForm: {},
      // 修改密码表单
      changePwdForm: {},
      // 部门名称
      deptName: undefined,
      // 默认密码
      initPassword: undefined,
      // 日期范围
      dateRange: [],
      // 岗位选项
      postOptions: [],
      // 角色选项
      roleOptions: [],
      // 表单参数
      form: {},
      // 试卷选择相关
      paperSelectDialogVisible: false,
      paperList: [],
      paperListLoading: false,
      selectedPapers: [], // 选中的试卷列表
      generatingPaperId: null, // 正在生成试卷包的试卷ID
      defaultProps: {
        children: "children",
        label: "label"
      },
      // 用户导入参数
      upload: {
        // 是否显示弹出层（用户导入）
        open: false,
        // 弹出层标题（用户导入）
        title: "",
        // 是否禁用上传
        isUploading: false,
        // 是否更新已经存在的用户数据
        updateSupport: 0,
        // 设置上传的请求头部
        headers: { Authorization: "Bearer " + getToken() },
        // 上传的地址
        url: process.env.VUE_APP_BASE_API + "/system/user/importData"
      },
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        studentAccount: undefined,
        phoneNumber: undefined,
        grade: undefined,
        currentGrade: undefined,
        applicablePapers: [],
        status: undefined
      },
      // 列信息
      columns: {
        studentAccount: { label: '学员账号', visible: true },
        password: { label: '密码', visible: true },
        sex: { label: '性别', visible: true },
        phoneNumber: { label: '手机号', visible: true },
        grade: { label: '学段', visible: true },
        currentGrade: { label: '当前年级', visible: true },
        hometown: { label: '籍贯', visible: true }
        //,
        //applicablePapers: { label: '适用考卷', visible: true }
      },
      // 表单校验
      rules: {
        studentAccount: [
          { required: true, message: "学员账号不能为空", trigger: "blur" },
          { min: 2, max: 20, message: '学员账号长度必须介于 2 和 20 之间', trigger: 'blur' }
        ],
        studentName: [
          { required: true, message: "学员姓名不能为空", trigger: "blur" },
          { min: 2, max: 100, message: '学员姓名长度必须介于 2 和 100 之间', trigger: 'blur' }
        ],
        password: [
          {
            validator: (rule, value, callback) => {
              // 新增模式：密码可以为空（将使用默认密码123456）
              if (!this.form.id) {
                // 如果提供了密码，则验证长度
                if (value && value.trim() !== '') {
                  if (value.length < 6 || value.length > 20) {
                    callback(new Error('密码长度必须介于 6 和 20 之间'))
                    return
                  }
                }
                callback()
                return
              }
              // 编辑模式：
              // 1. 如果密码是密文占位符 "******"，表示不修改密码，验证通过
              // 2. 如果密码为空、null、undefined 或只包含空格，也表示不修改密码，验证通过
              // 3. 如果密码不为空且不是 "******"，表示要修改密码，需要验证长度
              if (this.form.id) {
                // 处理各种空值情况
                const passwordValue = value === null || value === undefined ? '' : String(value)

                // 如果是密文占位符，验证通过
                if (passwordValue === "******") {
                  callback()
                  return
                }
                // 如果为空或只包含空格，表示不修改密码，验证通过
                if (!passwordValue || passwordValue.trim() === '') {
                  callback()
                  return
                }
                // 如果提供了新密码，验证长度
                if (passwordValue.length < 6 || passwordValue.length > 20) {
                  callback(new Error('密码长度必须介于 6 和 20 之间'))
                  return
                }
                callback()
                return
              }
              callback()
            },
            trigger: "blur"
          }
        ],
        phoneNumber: [
          { required: true, message: "手机号码不能为空", trigger: "blur" },
          {
            pattern: /^1[3|4|5|6|7|8|9][0-9]\d{8}$/,
            message: "请输入正确的手机号码",
            trigger: "blur"
          }
        ],
        grade: [
          { required: true, message: "请选择学段", trigger: "change" }
        ],
        currentGrade: [
          { required: true, message: "请选择当前年级", trigger: "change" }
        ]
        // ,
        // applicablePapers: [
        //   {
        //     type: 'array',
        //     required: true,
        //     message: "请选择适用考卷",
        //     trigger: "change",
        //     validator: (rule, value, callback) => {
        //       if (!value || value.length === 0) {
        //         callback(new Error("请至少选择一个适用考卷"))
        //       } else {
        //         callback()
        //       }
        //     }
        //   }
        // ]
      },
      // 重置密码表单校验
      resetPwdRules: {
        newPassword: [
          {
            validator: (rule, value, callback) => {
              // 新密码可以为空（将使用默认密码123456）
              if (value && value.trim() !== '') {
                if (value.length < 6 || value.length > 20) {
                  callback(new Error('密码长度必须介于 6 和 20 之间'))
                  return
                }
              }
              callback()
            },
            trigger: "blur"
          }
        ]
      },
      // 修改密码表单校验
      changePwdRules: {
        oldPassword: [
          { required: true, message: "旧密码不能为空", trigger: "blur" }
        ],
        newPassword: [
          { required: true, message: "新密码不能为空", trigger: "blur" },
          { min: 6, max: 20, message: '密码长度必须介于 6 和 20 之间', trigger: 'blur' }
        ],
        confirmPassword: [
          { required: true, message: "确认密码不能为空", trigger: "blur" },
          {
            validator: (rule, value, callback) => {
              if (value !== this.changePwdForm.newPassword) {
                callback(new Error("两次输入的密码不一致"))
              } else {
                callback()
              }
            },
            trigger: "blur"
          }
        ]
      }
    }
  },
  watch: {
    // 根据名称筛选部门树
    deptName(val) {
      this.$refs.tree.filter(val)
    }
  },
  created() {
    this.getList()
    this.getConfigKey("sys.user.initPassword").then(response => {
      this.initPassword = response.msg
    })
    // 加载所有年级字典（用于查询条件，不依赖学段）
    this.loadAllGradeDicts()
  },
  methods: {
    /** 查询用户列表 */
    getList() {
      this.loading = true
      listArchive(this.addDateRange(this.queryParams, this.dateRange)).then(response => {
          this.archiveList = response.rows
          this.total = response.total
          this.loading = false
          // 批量加载年级字典（用于表格显示）
          this.batchLoadGradeDicts()
        }
      )
    },
    /** 批量加载年级字典（用于表格显示） */
    batchLoadGradeDicts() {
      if (!this.archiveList || this.archiveList.length === 0) {
        return
      }
      // 收集所有不同的学段值
      const gradeValues = [...new Set(this.archiveList.map(item => item.grade).filter(Boolean))]
      // 加载每个学段对应的年级字典
      gradeValues.forEach(gradeValue => {
        if (!this.gradeDictCache[gradeValue]) {
          this.loadGradeDict(gradeValue, 'query')
        }
      })
    },

    // 过滤禁用的部门
    filterDisabledDept(deptList) {
      return deptList.filter(dept => {
        if (dept.disabled) {
          return false
        }
        if (dept.children && dept.children.length) {
          dept.children = this.filterDisabledDept(dept.children)
        }
        return true
      })
    },
    // 筛选节点
    filterNode(value, data) {
      if (!value) return true
      return data.label.indexOf(value) !== -1
    },
    // 节点单击事件
    handleNodeClick(data) {
      this.queryParams.deptId = data.id
      this.handleQuery()
    },
    // 用户状态修改
    handleStatusChange(row) {
      let text = row.status === "0" ? "启用" : "停用"
      this.$modal.confirm('确认要"' + text + '""' + row.userName + '"用户吗？').then(function() {
        return changeUserStatus(row.userId, row.status)
      }).then(() => {
        this.$modal.msgSuccess(text + "成功")
      }).catch(function() {
        row.status = row.status === "0" ? "1" : "0"
      })
    },
    // 取消按钮
    cancel() {
      this.open = false
      this.reset()
    },
    // 表单重置
    reset() {
      this.form = {
        id: undefined,
        studentAccount: undefined,
        password: undefined,
        phoneNumber: undefined,
        sex: undefined,
        grade: undefined,
        currentGrade: undefined,
        hometown: undefined,
        applicablePapers: [],
        applicablePaperIds: []
      }
      this.currentGradeOptions = []
      this.selectedPapers = []
      this.resetForm("form")
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.dateRange = []
      this.resetForm("queryForm")
      this.queryParams.grade = undefined
      this.queryParams.currentGrade = undefined
      this.queryParams.applicablePapers = []
      this.queryGradeOptions = []
      this.handleQuery()
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.id)
      this.single = selection.length != 1
      this.multiple = !selection.length
    },
    // 更多操作触发
    handleCommand(command, row) {
      switch (command) {
        case "handleResetPwd":
          this.handleResetPwd(row)
          break
        case "handleAuthRole":
          this.handleAuthRole(row)
          break
        default:
          break
      }
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "添加学员"
    },
    /** 查看按钮操作 */
    handleView(row) {
      const archiveId = row.id
      if (!archiveId) {
        this.$modal.msgError("无法获取学员信息")
        return
      }
      getArchive(archiveId).then(response => {
        if (response.code === 200 && response.data) {
          this.viewData = response.data
          // 加载已选中的试卷信息
          if (this.viewData.applicablePaperIds && this.viewData.applicablePaperIds.length > 0) {
            this.loadSelectedPapersForView(this.viewData.applicablePaperIds)
          } else {
            this.viewData.papers = []
          }
          this.viewOpen = true
        } else {
          this.$modal.msgError(response.msg || "获取学员信息失败")
        }
      }).catch(() => {
        this.$modal.msgError("获取学员信息失败")
      })
    },
    /** 为查看页面加载已选中的试卷信息 */
    loadSelectedPapersForView(paperIds) {
      if (!paperIds || paperIds.length === 0) {
        this.viewData.papers = []
        return
      }
      getPaperListByIds({ ids: paperIds }).then(response => {
        if (response.code === 200) {
          this.$set(this.viewData, 'papers', response.data || [])
        }
      }).catch(() => {
        this.$set(this.viewData, 'papers', [])
      })
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      // 确保 archiveId 是单个值，不是数组
      let archiveId = row.id
      if (!archiveId && this.ids && this.ids.length > 0) {
        archiveId = this.ids[0]
      }
      if (!archiveId) {
        this.$modal.msgError("请选择要修改的学员")
        return
      }
      getArchive(archiveId).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改学员"
        // 密码回显为密文，表示已设置密码
        this.form.password = "******"
        // 如果有学段，加载对应的年级选项
        if (this.form.grade) {
          this.loadGradeDict(this.form.grade, 'form')
        }
        // 加载已选中的试卷信息
        if (this.form.applicablePaperIds && this.form.applicablePaperIds.length > 0) {
          this.loadSelectedPapers(this.form.applicablePaperIds)
        } else {
          this.selectedPapers = []
        }
        // 清除密码字段的验证错误（因为密码是占位符，不需要验证）
        this.$nextTick(() => {
          this.$refs["form"] && this.$refs["form"].clearValidate('password')
        })
      })
    },
    /** 重置密码按钮操作 */
    handleResetPwd(row) {
      this.$prompt('请输入"' + row.userName + '"的新密码', "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        closeOnClickModal: false,
        inputPattern: /^.{5,20}$/,
        inputErrorMessage: "用户密码长度必须介于 5 和 20 之间",
        inputValidator: (value) => {
          if (/<|>|"|'|\||\\/.test(value)) {
            return "不能包含非法字符：< > \" ' \\\ |"
          }
        },
      }).then(({ value }) => {
          resetUserPwd(row.userId, value).then(response => {
            this.$modal.msgSuccess("修改成功，新密码是：" + value)
          })
        }).catch(() => {})
    },
    /** 分配角色操作 */
    handleAuthRole: function(row) {
      const userId = row.userId
      this.$router.push("/system/user-auth/role/" + userId)
    },
    /** 提交按钮 */
    submitForm: function() {
      // 编辑模式下，如果密码是 "******" 或为空，表示未修改密码，完全跳过密码验证
      if (this.form.id != undefined) {
        const password = this.form.password || ''
        if (password === "******" || password.trim() === '') {
          // 先清除密码字段的验证错误
          this.$refs["form"].clearValidate('password')
          // 临时移除密码字段的验证规则
          const originalPasswordRules = this.rules.password
          this.rules.password = []
          // 验证其他字段
          this.$nextTick(() => {
            this.$refs["form"].validate((valid) => {
              // 恢复密码字段的验证规则
              this.rules.password = originalPasswordRules
              if (valid) {
                this.doSubmit()
              }
            })
          })
          return
        }
      }

      // 其他情况（新增模式或编辑模式下修改了密码）直接验证整个表单
      this.$refs["form"].validate(valid => {
        if (valid) {
          this.doSubmit()
        }
      })
    },
    /** 执行提交操作 */
    doSubmit: function() {
      // 复制表单数据，排除密码字段（稍后根据情况决定是否添加）
      const { password, ...formDataWithoutPassword } = this.form

      if (this.form.id != undefined) {
        // 编辑模式：
        // 1. 如果密码是 "******" 或为空，表示不修改密码，不传递密码字段到后端
        // 2. 如果密码不为空且不是 "******"，表示要修改密码，添加密码字段
        const passwordValue = password || ''
        if (passwordValue && passwordValue !== "******" && passwordValue.trim() !== '') {
          // 用户修改了密码，添加密码字段
          formDataWithoutPassword.password = passwordValue
        }
        // 注意：如果密码未修改，formDataWithoutPassword 中不包含 password 字段
        updateArchive(formDataWithoutPassword).then(response => {
          this.$modal.msgSuccess("修改成功")
          this.open = false
          this.getList()
        })
      } else {
        // 新增操作：如果密码为空，传递空字符串，后端会使用默认密码123456
        const formData = { ...this.form }
        if (!formData.password || formData.password.trim() === '') {
          formData.password = ""
        }
        addArchive(formData).then(response => {
          this.$modal.msgSuccess("新增成功")
          this.open = false
          this.getList()
        })
      }
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const ids = row.id || this.ids
      // 判断是单个删除还是批量删除
      const isBatch = Array.isArray(ids) && ids.length > 1
      const confirmText = isBatch ? `是否确认删除编号为"${ids.join(',')}"的数据项？` : `是否确认删除学员编号为"${Array.isArray(ids) ? ids[0] : ids}"的数据项？`

      this.$modal.confirm(confirmText).then(() => {
        if (isBatch) {
          // 批量删除
          return delArchiveBatch(ids)
        } else {
          // 单个删除
          const archiveId = Array.isArray(ids) ? ids[0] : ids
          return delArchive(archiveId)
        }
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    /** 重置密码按钮操作 */
    handleResetPwd(row) {
      const archiveId = row.id
      this.resetPwdForm = {
        archiveId: archiveId,
        studentAccount: row.studentAccount,
        newPassword: ""
      }
      this.resetPwdTitle = "重置密码"
      this.resetPwdOpen = true
    },
    /** 提交重置密码 */
    submitResetPwd() {
      this.$refs["resetPwdForm"].validate(valid => {
        if (valid) {
          resetArchivePwd(this.resetPwdForm.archiveId, this.resetPwdForm.newPassword).then(response => {
            this.$modal.msgSuccess("重置密码成功")
            this.resetPwdOpen = false
            this.getList()
          })
        }
      })
    },
    /** 修改密码按钮操作 */
    handleChangePwd(row) {
      const archiveId = row.id
      this.changePwdForm = {
        archiveId: archiveId,
        studentAccount: row.studentAccount,
        oldPassword: "",
        newPassword: "",
        confirmPassword: ""
      }
      this.changePwdTitle = "修改密码"
      this.changePwdOpen = true
    },
    /** 提交修改密码 */
    submitChangePwd() {
      this.$refs["changePwdForm"].validate(valid => {
        if (valid) {
          changeArchivePwd(this.changePwdForm.archiveId, this.changePwdForm.oldPassword, this.changePwdForm.newPassword).then(response => {
            this.$modal.msgSuccess("修改密码成功")
            this.changePwdOpen = false
            this.getList()
          }).catch(() => {})
        }
      })
    },
    /** 重置练习次数按钮操作 */
    handleResetPractice(row) {
      const archiveId = row.id
      const userId = row.userId
      const studentName = row.studentName || row.studentAccount
      this.$confirm(`确认要重置学员"${studentName}"的所有练习次数吗？重置后学员下次登录时将清除本地练习记录。`, "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning"
      }).then(() => {
        return resetPractice({ 
          userId: userId, 
          archiveId: archiveId,
          remark: '管理员手动重置' 
        })
      }).then(() => {
        this.$modal.msgSuccess("重置成功，学员下次登录后生效")
      }).catch(() => {})
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('system/user/export', {
        ...this.queryParams
      }, `user_${new Date().getTime()}.xlsx`)
    },
    /** 导入按钮操作 */
    handleImport() {
      this.upload.title = "用户导入"
      this.upload.open = true
    },
    /** 下载模板操作 */
    importTemplate() {
      this.download('system/user/importTemplate', {
      }, `user_template_${new Date().getTime()}.xlsx`)
    },
    // 文件上传中处理
    handleFileUploadProgress(event, file, fileList) {
      this.upload.isUploading = true
    },
    // 文件上传成功处理
    handleFileSuccess(response, file, fileList) {
      this.upload.open = false
      this.upload.isUploading = false
      this.$refs.upload.clearFiles()
      this.$alert("<div style='overflow: auto;overflow-x: hidden;max-height: 70vh;padding: 10px 20px 0;'>" + response.msg + "</div>", "导入结果", { dangerouslyUseHTMLString: true })
      this.getList()
    },
    // 提交上传文件
    submitFileForm() {
      const file = this.$refs.upload.uploadFiles
      if (!file || file.length === 0 || !file[0].name.toLowerCase().endsWith('.xls') && !file[0].name.toLowerCase().endsWith('.xlsx')) {
        this.$modal.msgError("请选择后缀为 “xls”或“xlsx”的文件。")
        return
      }
      this.$refs.upload.submit()
    },
    /** 表单中学段变化时触发 */
    handleGradeChange(value) {
      this.form.currentGrade = undefined
      this.loadGradeDict(value, 'form')
    },
    /** 加载所有年级字典（用于查询条件，不依赖学段） */
    loadAllGradeDicts() {
      // 年级字典类型：pupil（小学年级）、middle（中学年级）、high（高中年级）
      const gradeDictTypes = ['pupil', 'middle', 'high']
      const allGradeOptions = []

      // 并行加载所有年级字典
      const promises = gradeDictTypes.map(dictType => {
        return getDicts(dictType).then(response => {
          if (response.data && response.data.length > 0) {
            return response.data.map(item => ({
              value: item.dictValue,
              label: item.dictLabel
            }))
          }
          return []
        }).catch(() => {
          return []
        })
      })

      Promise.all(promises).then(results => {
        // 合并所有年级选项
        results.forEach(options => {
          allGradeOptions.push(...options)
        })
        this.queryGradeOptions = allGradeOptions
        console.log('所有年级字典加载完成，共', allGradeOptions.length, '条')
      })
    },
    /** 加载年级字典数据 */
    loadGradeDict(gradeValue, type) {
      if (!gradeValue) {
        if (type === 'form') {
          this.currentGradeOptions = []
        } else {
          this.queryGradeOptions = []
        }
        return
      }

      // 先检查缓存
      if (this.gradeDictCache[gradeValue]) {
        const options = this.gradeDictCache[gradeValue]
        if (type === 'form') {
          this.currentGradeOptions = options
        } else {
          this.queryGradeOptions = options
        }
        return
      }

      // 根据学段值获取对应的年级字典类型
      // 假设字典类型格式为：grade_primary（小学）、grade_middle（中学）、grade_high（高中）
      // 或者根据学段的dictValue来匹配
      const gradeDictType = this.getGradeDictType(gradeValue)

      if (gradeDictType) {
        getDicts(gradeDictType).then(response => {
          const options = response.data.map(item => ({
            value: item.dictValue,
            label: item.dictLabel
          }))
          // 缓存数据
          this.gradeDictCache[gradeValue] = options
          if (type === 'form') {
            this.currentGradeOptions = options
          } else {
            this.queryGradeOptions = options
          }
        })
      } else {
        // 如果没有找到对应的字典类型，尝试从所有年级字典中过滤
        // 假设年级字典的dictValue包含学段信息（如"小学一年级"、"中学七年级"）
        this.filterGradeByGrade(gradeValue, type)
      }
    },
    /** 根据学段值获取对应的年级字典类型 */
    getGradeDictType(gradeValue) {
      // 根据学段的 dictValue 直接匹配对应的年级字典类型
      // 学段字典：grade -> 小学(pupil)、中学(middle_school)、高中(high_school)
      // 年级字典类型：pupil（小学年级）、middle_school（中学年级）、high_school（高中年级）
      if (gradeValue === 'pupil') {
        return 'pupil'
      } else if (gradeValue === 'middle_school') {
        return 'middle_school'
      } else if (gradeValue === 'high_school') {
        return 'high_school'
      }
      return null
    },
    /** 从所有年级字典中根据学段过滤 */
    filterGradeByGrade(gradeValue, type) {
      // 获取学段的标签
      const gradeDict = this.dict.type.grade.find(item => item.value === gradeValue)
      if (!gradeDict) {
        if (type === 'form') {
          this.currentGradeOptions = []
        } else {
          this.queryGradeOptions = []
        }
        return
      }

      const gradeLabel = gradeDict.label
      // 假设所有年级在一个字典类型中，dictValue格式为"小学一年级"、"中学七年级"等
      // 这里需要根据实际情况调整字典类型名称
      getDicts('grade_detail').then(response => {
        const options = response.data
          .filter(item => item.dictValue && item.dictValue.startsWith(gradeLabel))
          .map(item => ({
            value: item.dictValue,
            label: item.dictLabel
          }))
        // 缓存数据
        this.gradeDictCache[gradeValue] = options
        if (type === 'form') {
          this.currentGradeOptions = options
        } else {
          this.queryGradeOptions = options
        }
      }).catch(() => {
        // 如果字典类型不存在，设置为空
        if (type === 'form') {
          this.currentGradeOptions = []
        } else {
          this.queryGradeOptions = []
        }
      })
    },
    /** 根据学段和年级值获取年级字典标签（用于表格显示） */
    getGradeDictLabel(gradeValue, currentGradeValue) {
      if (!gradeValue || !currentGradeValue) {
        return currentGradeValue || '-'
      }

      // 先检查缓存
      if (this.gradeDictCache[gradeValue]) {
        const gradeDict = this.gradeDictCache[gradeValue].find(item => item.value === currentGradeValue)
        return gradeDict ? gradeDict.label : currentGradeValue
      }

      // 如果缓存中没有，尝试加载（异步加载，先返回原值）
      this.loadGradeDict(gradeValue, 'query')
      return currentGradeValue
    },
    /** 获取字典标签 */
    getDictLabel(dictType, dictValue) {
      const dictOptions = this.dict.type[dictType]
      if (!dictOptions || !dictValue) {
        return dictValue || '-'
      }
      const dict = dictOptions.find(item => item.value === dictValue)
      return dict ? dict.label : dictValue
    },
    /** 打开试卷选择对话框 */
    openPaperSelectDialog() {
      this.paperSelectDialogVisible = true
      this.loadAllPapers()
      // 延迟设置选中状态，确保表格已渲染
      this.$nextTick(() => {
        if (this.selectedPapers && this.selectedPapers.length > 0) {
          const selectedIds = this.selectedPapers.map(p => p.id)
          this.paperList.forEach(paper => {
            if (selectedIds.includes(paper.id)) {
              this.$refs.paperTable && this.$refs.paperTable.toggleRowSelection(paper, true)
            }
          })
        }
      })
    },
    /** 加载所有试卷列表 */
    loadAllPapers() {
      this.paperListLoading = true
      // 查询所有启用的试卷
      getPaperList({
        pageNum: 1,
        pageSize: 1000,
        status: 1
      }).then(response => {
        if (response.code === 200) {
          this.paperList = response.rows || []
          // 如果有已选中的试卷，设置选中状态
          this.$nextTick(() => {
            if (this.selectedPapers && this.selectedPapers.length > 0) {
              const selectedIds = this.selectedPapers.map(p => p.id)
              this.paperList.forEach(paper => {
                if (selectedIds.includes(paper.id)) {
                  this.$refs.paperTable && this.$refs.paperTable.toggleRowSelection(paper, true)
                }
              })
            }
          })
        }
        this.paperListLoading = false
      }).catch(() => {
        this.paperListLoading = false
      })
    },
    /** 试卷选择变化 */
    handlePaperSelectionChange(selection) {
      // 这里先不更新，等用户点击确定后再更新
    },
    /** 确认试卷选择 */
    confirmPaperSelection() {
      // 获取表格中选中的试卷
      const table = this.$refs.paperTable
      if (!table) {
        this.$modal.msgError("无法获取选中状态，请刷新后重试")
        return
      }
      const selection = table.selection || []
      
      // 合并已选试卷（避免重复）
      const existingIds = this.selectedPapers.map(p => p.id)
      const newPapers = selection.filter(p => !existingIds.includes(p.id))
      this.selectedPapers = [...this.selectedPapers, ...newPapers]
      this.form.applicablePaperIds = this.selectedPapers.map(p => p.id)
      this.paperSelectDialogVisible = false
    },
    /** 删除已选试卷 */
    handleRemovePaper(index) {
      this.selectedPapers.splice(index, 1)
      this.form.applicablePaperIds = this.selectedPapers.map(p => p.id)
    },
    /** 加载已选中的试卷信息 */
    loadSelectedPapers(paperIds) {
      if (!paperIds || paperIds.length === 0) {
        this.selectedPapers = []
        return
      }
      getPaperListByIds({ ids: paperIds }).then(response => {
        if (response.code === 200) {
          this.selectedPapers = response.data || []
        }
      }).catch(() => {
        this.selectedPapers = []
      })
    },
    /** 生成试卷包 */
    handleGeneratePackage(paper) {
      this.$modal.confirm('是否确认生成试卷包？生成过程可能需要一些时间，请耐心等待。').then(() => {
        this.generatingPaperId = paper.id
        generatePaperPackage({ id: paper.id }).then(response => {
          this.$modal.msgSuccess("试卷包生成成功")
          // 刷新试卷列表
          this.loadAllPapers()
          this.generatingPaperId = null
        }).catch(error => {
          this.$modal.msgError("试卷包生成失败：" + (error.msg || error.message || "未知错误"))
          this.generatingPaperId = null
        })
      }).catch(() => {})
    },
    /** 重新生成试卷包 */
    handleRegeneratePackage(paper) {
      this.$modal.confirm('是否确认重新生成试卷包？这将覆盖现有的试卷包。').then(() => {
        this.generatingPaperId = paper.id
        generatePaperPackage({ id: paper.id }).then(response => {
          this.$modal.msgSuccess("试卷包重新生成成功")
          // 刷新试卷列表
          this.loadAllPapers()
          this.generatingPaperId = null
        }).catch(error => {
          this.$modal.msgError("试卷包生成失败：" + (error.msg || error.message || "未知错误"))
          this.generatingPaperId = null
        })
      }).catch(() => {})
    },
    /** 格式化日期 */
    formatDate(dateTime) {
      if (!dateTime) return '-'
      const date = new Date(dateTime)
      return date.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      })
    }
  }
}
</script>

<style scoped>
.selected-papers-table {
  margin-top: 10px;
}

.selected-papers-table ::v-deep .el-table__body-wrapper {
  max-height: 300px;
  overflow-y: auto;
}

.selected-papers-table ::v-deep .el-table {
  font-size: 12px;
}
</style>
