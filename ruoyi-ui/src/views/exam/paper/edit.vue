<template>
  <div class="app-container">
    <el-card>
      <div slot="header" class="clearfix">
        <span class="card-title">修改试卷</span>
        <el-button style="float: right; padding: 3px 0" type="text" @click="handleCancel">返回</el-button>
      </div>

      <el-form ref="form" :model="form" :rules="rules" label-width="120px" v-loading="loading">
        <el-form-item label="试卷名称" prop="paperName">
          <el-row :gutter="8" style="flex-wrap: nowrap;">
            <el-col :span="3" style="padding-left: 0; flex: 0 0 auto;" v-if="false">
              <!-- 年份字段已隐藏，改为可选 -->
              <el-form-item prop="year" style="margin-bottom: 0;">
                <el-select v-model="form.year" placeholder="选择年（可选）" style="width: 100%">
                  <el-option
                    v-for="year in yearOptions"
                    :key="year"
                    :label="year + '年'"
                    :value="year"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="3" style="flex: 0 0 auto;" v-if="false">
              <!-- 月份字段已隐藏，改为可选 -->
              <el-form-item prop="month" style="margin-bottom: 0">
                <el-select v-model="form.month" placeholder="选择月（可选）" style="width: 100%">
                  <el-option
                    v-for="dict in dict.type.paper_month"
                    :key="dict.value"
                    :label="dict.label"
                    :value="parseInt(dict.value)"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="3" style="flex: 0 0 auto;" v-if="false">
              <!-- 省份字段已隐藏，改为可选 -->
              <el-form-item prop="province" style="margin-bottom: 0">
                <el-select v-model="form.province" placeholder="选择省（可选）" style="width: 100%">
                  <el-option
                    v-for="dict in dict.type.paper_province"
                    :key="dict.value"
                    :label="dict.label"
                    :value="dict.value"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="5" style="padding-left: 0;flex: 0 0 auto;">
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
            <el-col :span="17" style="flex: 1;">
              <el-form-item prop="customName" style="margin-bottom: 0;">
                <el-input
                  v-model="form.customName"
                  placeholder="自定义试卷名称（必填）"
                  clearable
                />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form-item>

        <el-form-item label="试卷启用时间" required>
          <el-row :gutter="20">
            <el-col :span="7" style="padding-left: 0">
              <el-form-item prop="enableStartTime" style="margin-bottom: 0;">
                <el-date-picker
                  v-model="form.enableStartTime"
                  type="datetime"
                  placeholder="选择启用开始时间"
                  format="yyyy-MM-dd HH:mm:ss"
                  value-format="yyyy-MM-dd HH:mm:ss"
                  style="width: 100%"
                  @change="handleStartTimeChange"
                />
              </el-form-item>
            </el-col>
            <el-col :span="7">
              <el-form-item prop="enableEndTime" style="margin-bottom: 0;">
                <el-date-picker
                  v-model="form.enableEndTime"
                  type="datetime"
                  placeholder="选择启用结束时间"
                  format="yyyy-MM-dd HH:mm:ss"
                  value-format="yyyy-MM-dd HH:mm:ss"
                  style="width: 100%"
                  :picker-options="endTimePickerOptions"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form-item>

        <el-form-item label="试卷描述" prop="paperDesc">
          <Editor v-model="form.paperDesc" :min-height="150" />
        </el-form-item>

        <el-form-item label="注意事项" prop="notes">
          <Editor v-model="form.notes" :min-height="200" />
          <div style="margin-top: 10px;">
            <span style="color: #909399; margin-right: 20px;">显示时机：</span>
            <el-radio-group v-model="form.notesDisplayMode">
              <el-radio label="before_exam">考试前显示一次</el-radio>
              <el-radio label="before_section">每大题前显示</el-radio>
            </el-radio-group>
          </div>
        </el-form-item>

        <el-form-item label="注意事项音频" prop="introAudioUrl" required>
          <oss-upload
            ref="notesAudioUpload"
            v-model="form.introAudioUrl"
            :limit="1"
            accept=".mp3,.wav,.ogg,.m4a,.aac"
            :file-size="10"
            tip="只能上传音频文件（MP3、WAV等），且不超过10MB，仅支持一个文件，二次上传将直接替换"
            path-prefix="exam/question"
            list-type="text"
            @change="handleNotesAudioChange"
            @progress="(event, file) => handleUploadProgress('notesAudio', event.percent < 100)"
            @preview="handleIntroAudioPreview"
          />
          <div v-if="uploadStatus.notesAudio" style="color: #E6A23C; font-size: 12px; margin-top: 5px;">
            请先等待上传完成
          </div>
        </el-form-item>

        <el-form-item label="放音测试">
          <el-switch
            v-model="form.trialListenEnabled"
            :active-value="1"
            :inactive-value="0"
            style="margin-bottom: 5px;"
          />
          <div v-if="form.trialListenEnabled === 1">
            <el-form-item label="试听旁白音频" prop="trialIntroAudioUrl">
              <oss-upload
                ref="trialIntroAudioUpload"
                v-model="form.trialIntroAudioUrl"
                :limit="1"
                accept=".mp3,.wav,.ogg,.m4a,.aac"
                :file-size="10"
                tip="只能上传音频文件（MP3、WAV等），且不超过10MB，仅支持一个文件，二次上传将直接替换。此音频会在进入试听页面时自动播放"
                path-prefix="exam/question"
                list-type="text"
                @change="handleTrialIntroAudioChange"
                @progress="(event, file) => handleUploadProgress('trialIntroAudio', event.percent < 100)"
                @preview="handleTrialIntroAudioPreview"
              />
              <div v-if="uploadStatus.trialIntroAudio" style="color: #E6A23C; font-size: 12px; margin-top: 5px;">
                请先等待上传完成
              </div>
            </el-form-item>
            <el-form-item label="试听音频" prop="trialListenAudioUrl">
              <oss-upload
                ref="trialListenAudioUpload"
                v-model="form.trialListenAudioUrl"
                :limit="1"
                accept=".mp3,.wav,.ogg,.m4a,.aac"
                :file-size="10"
                tip="只能上传音频文件，且不超过10MB，仅支持一个文件，二次上传将直接替换。此音频需要用户点击'播放试听音频'按钮时播放"
                path-prefix="exam/question"
                list-type="text"
                @change="handleTrialListenAudioChange"
                @progress="(event, file) => handleUploadProgress('trialListenAudio', event.percent < 100)"
                @preview="handleTrialListenAudioPreview"
              />
              <div v-if="uploadStatus.trialListenAudio" style="color: #E6A23C; font-size: 12px; margin-top: 5px;">
                请先等待上传完成
              </div>
            </el-form-item>
            <el-form-item label="试听文本" prop="trialListenAudioText">
              <Editor v-model="form.trialListenAudioText" :min-height="120" />
            </el-form-item>
          </div>
        </el-form-item>

        <el-form-item label="操作提示文本">
          <Editor v-model="form.operateListenText" :min-height="120" />
        </el-form-item>
        <el-form-item label="操作提示图片" prop="operateListenImage" required>
          <oss-upload
            ref="operateListenImageUpload"
            v-model="form.operateListenImageUrl"
            :limit="1"
            accept=".jpg,.jpeg,.png,.gif,.bmp,.webp"
            :file-size="10"
            tip="只能上传图片文件（JPG、PNG等），且不超过10MB，仅支持一个文件，二次上传将直接替换"
            path-prefix="exam/question"
            list-type="picture-card"
            @change="handleOperateListenImageChange"
            @progress="(event, file) => handleUploadProgress('operateListenImage', event.percent < 100)"
            @preview="handleOperateListenImagePreview"
          />
          <div v-if="uploadStatus.operateListenImage" style="color: #E6A23C; font-size: 12px; margin-top: 5px;">
            请先等待上传完成
          </div>
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="练习次数" prop="practiceLimit">
              <el-input-number v-model="form.practiceLimit" :min="0" :max="999" controls-position="right" style="width: 200px" />
              <span style="margin-left: 10px; color: #909399;">次（0表示不限制）</span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="每题读题时长" prop="questionReadDuration">
              <el-input-number v-model="form.questionReadDuration" :min="0" :max="300" controls-position="right" style="width: 200px" />
              <span style="margin-left: 10px; color: #909399;">秒（用于自动跳转）</span>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
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
          </el-col>
          <el-col :span="12">
            <el-form-item label="考试时长" prop="duration" required>
              <el-input-number v-model="form.duration" :min="1" :max="999" controls-position="right" style="width: 200px" />
              <span style="margin-left: 10px; color: #909399;">分钟</span>
        </el-form-item>
          </el-col>
        </el-row>

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

        <el-form-item label="状态" prop="status" required>
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="请输入备注" />
        </el-form-item>

        <!-- 标签页：试卷结构、中场配置 -->
        <el-tabs v-model="activeTab" type="border-card" style="margin-top: 20px;">
          <el-tab-pane label="试卷结构" name="structure">
      <paper-structure-table
        v-if="form.id"
        :key="`structure-table-${form.id}`"
        ref="paperStructureTable"
        :paper-id="form.id"
        :paper-name="form.paperName || form.customName"
        :auto-calculate-total-score="form.autoCalculateTotalScore"
        @update-total-score="handleUpdateTotalScore"
        @update-total-questions="handleUpdateTotalQuestions"
        @preview-audio="handleStructureAudioPreview"
      />
            <div v-else style="padding: 20px; text-align: center; color: #909399;">
              请先保存试卷基本信息
          </div>
          </el-tab-pane>
          <el-tab-pane label="中场配置" name="intermission">
            <intermission-management
              v-if="form.id"
              :key="`intermission-${form.id}`"
              ref="intermissionManagement"
              :paper-id="form.id"
              @preview-audio="handleStructureAudioPreview"
              @refresh="handleIntermissionRefresh"
            />
            <div v-else style="padding: 20px; text-align: center; color: #909399;">
              请先保存试卷基本信息
            </div>
          </el-tab-pane>
        </el-tabs>
      </el-form>

      <div class="form-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="handleCancel">取 消</el-button>
      </div>
    </el-card>



    <!-- 音频全屏遮盖播放器（复用题目管理样式） -->
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
              <button @click="toggleAudioPlay" class="audio-control-btn play-pause-btn" type="button">
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
                  <span class="custom-volume-icon" @click.stop="toggleMute">
                    <i v-if="audioMuted" class="el-icon-turn-off volume-icon"></i>
                    <i v-else class="el-icon-headset volume-icon"></i>
                  </span>
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
                      @input="handleAudioVolumeInput"
                      @change="handleAudioVolumeChange"
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
        <div class="audio-player-footer"></div>
      </div>
    </div>

    <!-- 图片预览对话框 -->
    <el-dialog
      :visible.sync="imagePreviewVisible"
      title="图片预览"
      width="800"
      append-to-body
    >
      <img
        :src="imagePreviewUrl"
        style="display: block; max-width: 100%; margin: 0 auto"
        alt="预览图片"
      />
    </el-dialog>
  </div>
</template>

<script>
import {
  getPaper, updatePaper, getPaperQuestionList
} from "@/api/exam/paper"
import { getToken } from "@/utils/auth"
import { encodeUrlFileName, normalizeMediaUrl } from "@/utils/media"
import { getMediaDownloadUrl } from "@/api/exam/question"
import IntermissionManagement from "./components/IntermissionManagement.vue"
import PaperStructureTable from "./components/PaperStructureTable.vue"
import OssUpload from "@/components/OssUpload"
import Editor from "@/components/Editor"

export default {
  name: "PaperEdit",
  components: { IntermissionManagement, PaperStructureTable, OssUpload, Editor },
  dicts: ['question_type', 'subject', 'question_type_default_score', 'paper_type', 'paper_month', 'paper_province'],
  data() {
      return {
        loading: false,
        pollingTimer: null, // 轮询定时器
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
        id: undefined,
        paperName: undefined, // 后端自动生成，前端只显示
        year: undefined,
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
        autoNextQuestion: 0,
        showAnswerImmediately: 0,
        allowReview: 0,
        questionReadDuration: 0,
        practiceLimit: 0,
        trialListenEnabled: 0,
        trialListenAudioUrl: undefined,
        trialListenAudioPath: undefined,
        trialListenAudioDuration: undefined,
        trialListenAudioText: undefined,
        trialIntroAudioUrl: undefined,
        trialIntroAudioPath: undefined,
        trialIntroAudioDuration: undefined,
        operateListenText: undefined,
        operateListenImage: undefined,
        operateListenImageUrl: undefined,
        operateListenImagePath: undefined,
        notes: undefined,
        notesDisplayMode: 'before_exam',
        enableStartTime: undefined,
        enableEndTime: undefined,
        status: 1,
        remark: undefined,
        autoCalculateTotalScore: false
      },
      // 表单校验
      rules: {
        paperName: [
          { required: true, message: "试卷名称不能为空", trigger: "blur" }
        ],
        customName: [
          { required: true, message: "自定义试卷名称不能为空", trigger: "blur" }
        ],
        year: [
          // 年份改为可选，不再必填
          { type: 'number', min: 2000, max: 2050, message: "年份必须在2000-2050之间", trigger: "blur" }
        ],
        month: [
          // 月份改为可选，不再必填
        ],
        province: [
          // 省份改为可选，不再必填
        ],
        paperType: [
          { required: true, message: "试卷类型不能为空", trigger: "change" }
        ],
        enableStartTime: [
          { required: true, message: "试卷启用开始时间不能为空", trigger: "change" }
        ],
        enableEndTime: [
          { required: true, message: "试卷启用结束时间不能为空", trigger: "change" },
          {
            validator: (rule, value, callback) => {
              if (this.form.enableStartTime && value) {
                const startTime = new Date(this.form.enableStartTime).getTime()
                const endTime = new Date(value).getTime()
                if (endTime <= startTime) {
                  callback(new Error('启用结束时间必须大于开始时间'))
                } else {
                  callback()
                }
              } else {
                callback()
              }
            },
            trigger: "change"
          }
        ],
        duration: [
          { required: true, message: "考试时长不能为空", trigger: "blur" }
        ],
        paperDesc: [
          { required: true, message: "试卷描述不能为空", trigger: "blur" }
        ],
        notes: [
          { required: true, message: "注意事项不能为空", trigger: "blur" }
        ],
        introAudioUrl: [
          { required: true, message: "注意事项音频不能为空", trigger: "change" }
        ],
        trialListenAudioUrl: [
          {
            validator: (rule, value, callback) => {
              if (this.form.trialListenEnabled === 1 && !value) {
                callback(new Error("试听音频不能为空"))
              } else {
                callback()
              }
            },
            trigger: "change"
          }
        ],
        operateListenImage: [
          { required: true, message: "操作提示图片不能为空", trigger: "change" }
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
      // 上传状态跟踪
      uploadStatus: {
        introAudio: false,
        notesAudio: false,
        operateListenImage: false,
        trialListenAudio: false,
        trialIntroAudio: false
      },
      // 文件列表（用于自动替换逻辑）
      // 图片预览
      imagePreviewVisible: false,
      imagePreviewUrl: '',
      // 音频预览播放器状态
      audioPlayerVisible: false,
      audioPlayerUrl: '',
      audioPlayerTitle: '',
      audioPlayerMimeType: '',
      audioPlaying: false,
      audioProgress: 0,
      currentTime: 0,
      duration: 0,
      audioVolume: 100,
      audioMuted: false,
      volumeSliderVisible: false,
      volumeTooltipVisible: false,
      volumeTooltipTimer: null,
      handleKeydown: null,
      // 已选择的题目列表（包含 id, title, type, subjectId, score 等字段）
      selectedQuestions: [],
      // 当前激活的标签页
      activeTab: 'structure'
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
    },
    /** 结束时间选择器的配置 */
    endTimePickerOptions() {
      return {
        disabledDate: (time) => {
          // 如果已选择开始时间，禁用开始时间之前的日期
          if (this.form.enableStartTime) {
            const startTime = new Date(this.form.enableStartTime).getTime()
            // 禁用开始时间之前的日期（不包括开始时间当天）
            return time.getTime() < startTime
          }
          return false
        }
      }
    }
  },
  created() {
    const id = this.$route.query.id
    if (id) {
      this.loadPaperData(id)
    } else {
      this.$modal.msgError("缺少试卷ID参数")
      this.handleCancel()
    }

  },
  mounted() {
    this.handleKeydown = (e) => {
      if (e.key === 'Escape' && this.audioPlayerVisible) {
        this.closeAudioPlayer()
      }
    }
    document.addEventListener('keydown', this.handleKeydown)
  },
  beforeDestroy() {
    if (this.handleKeydown) {
      document.removeEventListener('keydown', this.handleKeydown)
    }
    document.removeEventListener('click', this.handleClickOutsideVolumeSlider, true)
  },
  methods: {
    /**
     * 处理开始时间变化
     * 如果结束时间小于开始时间，清空结束时间
     */
    handleStartTimeChange(value) {
      if (value && this.form.enableEndTime) {
        const startTime = new Date(value).getTime()
        const endTime = new Date(this.form.enableEndTime).getTime()
        if (endTime <= startTime) {
          this.form.enableEndTime = undefined
          this.$message.warning('结束时间必须大于开始时间，已清空结束时间')
        }
      }
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
        const response = await getMediaDownloadUrl({ url: rawUrl })
        const signedUrl = response?.downloadUrl || response?.data?.downloadUrl
        if (response?.code === 200 && signedUrl) {
          return signedUrl
        }
      } catch (error) {
        console.error('获取媒体下载链接失败:', error)
      }
      return normalizeMediaUrl(rawUrl)
    },
    /** 加载试卷数据 */
    loadPaperData(id) {
      this.loading = true
      getPaper({ id }).then(response => {
        if (response.code === 200 && response.data) {
          // 直接使用后端返回的字段（year, month, province, customName）
          // 后端已经将这些字段存储在数据库中，不需要从 paper_code 解析
          const paperData = response.data
          Object.assign(this.form, paperData)
          // 如果后端返回的值为null或undefined，使用默认值0（否）
          if (this.form.autoNextQuestion === null || this.form.autoNextQuestion === undefined) {
            this.form.autoNextQuestion = 0
          }
          if (this.form.allowReview === null || this.form.allowReview === undefined) {
            this.form.allowReview = 0
          }

          // 确保字段正确设置（如果后端返回 null，保持为 null）
          // 不需要额外的解析逻辑

          // OssUpload 组件会自动处理文件列表，无需手动更新
          // 加载已选择的题目
          this.loadSelectedQuestions(id)
        } else {
          this.$modal.msgError(response.msg || "加载试卷数据失败")
          this.handleCancel()
        }
        this.loading = false
      }).catch(() => {
        this.$modal.msgError("加载试卷数据失败")
        this.loading = false
        this.handleCancel()
      })
    },
    /** 加载已选择的题目 */
    loadSelectedQuestions(paperId) {
      getPaperQuestionList({ paperId }).then(response => {
        if (response.code === 200 && response.data) {
          // 将试卷题目列表转换为已选题目格式
          this.selectedQuestions = response.data
            .filter(pq => pq.questionId != null) // 过滤掉questionId为null的项
            .map((pq, index) => ({
              id: pq.questionId || `temp_${index}`, // 确保id不为空
              title: pq.title || '暂无标题',
              type: pq.type,
              subjectId: pq.subjectId,
              score: parseFloat(pq.score) || 0
            }))
        } else {
          this.selectedQuestions = []
        }
      }).catch(() => {
        this.selectedQuestions = []
      })
    },
    /** 提交按钮 */
    async submitForm() {
      // 检查是否有正在上传的文件
      if (this.checkUploading()) {
        this.$modal.msgWarning("请先等待所有文件上传完成")
        return
      }

      // 在提交前，确保 operateListenImage 字段有值（如果已上传图片）
      if (this.form.operateListenImageUrl && !this.form.operateListenImage) {
        this.form.operateListenImage = this.form.operateListenImageUrl
      }

      this.$refs["form"].validate(async valid => {
        if (valid) {
          try {
            // 1. 收集试卷基本信息
            const paperData = {
              ...this.form,
              // 确保 customName 正确传递（如果为空字符串，转换为 null）
              customName: this.form.customName && this.form.customName.trim() ? this.form.customName.trim() : null,
              // 确保 introAudioDuration 正确传递（如果是 undefined，转换为 null，以便后端能正确更新）
              introAudioDuration: this.form.introAudioDuration !== undefined ? this.form.introAudioDuration : null
            }

            // 2. 获取试卷结构数据（卷别、大题、题目）- 嵌套结构
            let volumes = []
            let totalQuestions = 0
            if (this.$refs.paperStructureTable) {
              const structureData = this.$refs.paperStructureTable.getStructureData()
              volumes = structureData.volumes || []
              totalQuestions = structureData.totalQuestions || 0
            }

            // 3. 获取中场配置数据
            let intermissions = []
            if (this.$refs.intermissionManagement) {
              intermissions = this.$refs.intermissionManagement.getIntermissionData() || []
            }

            // 4. 组装所有数据，统一传给后端（嵌套结构）
            const submitData = {
              paper: {
                ...paperData,
                totalQuestions: totalQuestions > 0 ? totalQuestions : paperData.totalQuestions
              },
              volumes: volumes, // 嵌套结构：volumes -> sections -> questions
              intermissions: intermissions
            }

            // 5. 调用后端API统一保存（使用更新接口）
            const response = await updatePaper(submitData)
            if (response.code !== 200) {
              this.$modal.msgError(response.msg || "保存失败")
              return
            }

            this.$modal.msgSuccess("保存成功")
            // 提示是否生成试卷包
            this.$modal.confirm('是否立即生成试卷包？生成过程可能需要一些时间，请耐心等待。').then(() => {
              import('@/api/exam/paper').then(module => {
                const { generatePaperPackage } = module
                generatePaperPackage({ id: this.form.id }).then(response => {
                  // 从后端获取任务（不再手动添加）
                  // 立即加载一次任务列表，确保显示最新任务
                  this.$store.dispatch('task/loadAllTasks')
                  
                  // 打开任务抽屉
                  this.$store.dispatch('task/setDrawerVisible', true)
                  this.$store.dispatch('task/setActiveTab', 'inProgress')
                  
                  // 开始轮询任务状态（使用paperId作为标识）
                  this.startPolling(this.form.id)
                  
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
              })
            }).catch(() => {
              // 用户取消，不做任何操作
            })
          } catch (error) {
            console.error('保存失败:', error)
            this.$modal.msgError("保存失败：" + (error.message || "未知错误"))
          }
        }
      })
    },
    /** 取消按钮 */
    handleCancel() {
      // 清除轮询定时器
      if (this.pollingTimer) {
        clearInterval(this.pollingTimer)
        this.pollingTimer = null
      }
      this.$router.back()
    },
    /** 开始轮询任务状态（已废弃：使用MySQL数据库后不再需要定时轮询） */
    startPolling(paperId) {
      // 使用MySQL数据库后，不再需要定时轮询
      // 任务状态已持久化到数据库，用户点击任务通知图标时会查询
      // 只在提交任务后立即查询一次，让用户能看到任务已提交
      this.$store.dispatch('task/loadAllTasks')
    },
    /** 转换树形数据 */
    convertToTreeData(data) {
      return data.map(item => ({
        ...item,
        children: item.children && item.children.length > 0 ? this.convertToTreeData(item.children) : undefined
      }))
    },

    /** 总分变化 */
    handleTotalScoreChange() {
      // 编辑页面中，总分变化由试卷结构组件处理
    },
    /** 自动计算总分开关变化 */
    handleAutoCalculateChange() {
      // 通知子组件重新计算总分
      if (this.$refs.paperStructureTable) {
        this.$refs.paperStructureTable.calculateAndEmitTotalScore()
      }
    },
    /** 处理子组件更新总分的通知 */
    handleUpdateTotalScore(totalScore) {
      if (this.form.autoCalculateTotalScore) {
        this.form.totalScore = totalScore
      }
    },
    /** 处理子组件更新题目总数的通知 */
    handleUpdateTotalQuestions(totalQuestions) {
      this.form.totalQuestions = totalQuestions
    },
    /** 开场独白音频文件变化处理（OssUpload组件回调） */
    handleIntroAudioChange(urlOrData) {
      // OssUpload 组件上传成功后，会通过 @change 事件传递文件URL或包含url和duration的对象
      if (urlOrData) {
        const url = typeof urlOrData === 'object' ? urlOrData.url : urlOrData
        const duration = typeof urlOrData === 'object' ? urlOrData.duration : null
        // 更新表单字段
        this.form.introAudioUrl = url
        this.form.introAudioPath = url // OssUpload 返回的是完整URL，path 也使用 URL
        if (duration !== null && duration !== undefined) {
          this.form.introAudioDuration = duration
        }
      } else {
        // URL 为空表示移除
        this.form.introAudioUrl = undefined
        this.form.introAudioPath = undefined
        this.form.introAudioDuration = undefined
      }
    },
    /** 注意事项音频文件变化处理（OssUpload组件回调） */
    handleNotesAudioChange(urlOrData) {
      // 注意事项音频使用 introAudio 字段
      // OssUpload 组件上传成功后，会通过 @change 事件传递文件URL或包含url和duration的对象
      if (urlOrData) {
        const url = typeof urlOrData === 'object' ? urlOrData.url : urlOrData
        const duration = typeof urlOrData === 'object' ? urlOrData.duration : null
        // 更新表单字段
        this.form.introAudioUrl = url
        this.form.introAudioPath = url // OssUpload 返回的是完整URL，path 也使用 URL
        if (duration !== null && duration !== undefined) {
          this.form.introAudioDuration = duration
        }
      } else {
        // URL 为空表示移除
        this.form.introAudioUrl = undefined
        this.form.introAudioPath = undefined
        this.form.introAudioDuration = undefined
      }
    },
    async handleIntroAudioPreview(file) {
      // 优先使用 file.url，如果没有则从表单中获取
      const rawUrl = file?.url || this.form.introAudioUrl
      if (!rawUrl) {
        this.$modal.msgWarning('音频地址不存在')
        return
      }
      const previewUrl = await this.resolveMediaPreviewUrl(rawUrl)
      if (!previewUrl) {
        this.$modal.msgError('无法解析音频地址')
        return
      }
      const fileName = file?.name || this.getFileNameFromUrl(rawUrl) || '注意事项音频'
      this.playAudioPreview(previewUrl, fileName)
    },
    /** 试听旁白音频文件变化处理（OssUpload组件回调） */
    handleTrialIntroAudioChange(urlOrData) {
      // OssUpload 组件上传成功后，会通过 @change 事件传递文件URL或包含url和duration的对象
      if (urlOrData) {
        const url = typeof urlOrData === 'object' ? urlOrData.url : urlOrData
        const duration = typeof urlOrData === 'object' ? urlOrData.duration : null
        // 更新表单字段
        this.form.trialIntroAudioUrl = url
        this.form.trialIntroAudioPath = url // OssUpload 返回的是完整URL，path 也使用 URL
        if (duration !== null && duration !== undefined) {
          this.form.trialIntroAudioDuration = duration
        }
        // 清除验证错误
        this.$nextTick(() => {
          if (this.$refs.form) {
            this.$refs.form.clearValidate('trialIntroAudioUrl')
          }
        })
      } else {
        // URL 为空表示移除
        this.form.trialIntroAudioUrl = undefined
        this.form.trialIntroAudioPath = undefined
        this.form.trialIntroAudioDuration = undefined
      }
    },
    /** 试听旁白音频预览 */
    async handleTrialIntroAudioPreview(file) {
      // 优先使用 file.url，如果没有则从表单中获取
      const rawUrl = file?.url || this.form.trialIntroAudioUrl
      if (!rawUrl) {
        this.$modal.msgWarning('音频地址不存在')
        return
      }
      const previewUrl = await this.resolveMediaPreviewUrl(rawUrl)
      if (!previewUrl) {
        this.$modal.msgError('无法解析音频地址')
        return
      }
      const fileName = file?.name || this.getFileNameFromUrl(rawUrl) || '试听旁白音频'
      this.playAudioPreview(previewUrl, fileName)
    },
    /** 试听音频文件变化处理（OssUpload组件回调） */
    handleTrialListenAudioChange(urlOrData) {
      // OssUpload 组件上传成功后，会通过 @change 事件传递文件URL或包含url和duration的对象
      if (urlOrData) {
        const url = typeof urlOrData === 'object' ? urlOrData.url : urlOrData
        const duration = typeof urlOrData === 'object' ? urlOrData.duration : null
        // 更新表单字段
        this.form.trialListenAudioUrl = url
        this.form.trialListenAudioPath = url // OssUpload 返回的是完整URL，path 也使用 URL
        if (duration !== null && duration !== undefined) {
          this.form.trialListenAudioDuration = duration
        }
        // 清除验证错误
        this.$nextTick(() => {
          if (this.$refs.form) {
            this.$refs.form.clearValidate('trialListenAudioUrl')
          }
        })
      } else {
        // URL 为空表示移除
        this.form.trialListenAudioUrl = undefined
        this.form.trialListenAudioPath = undefined
        this.form.trialListenAudioDuration = undefined
      }
    },
    /** 试听音频预览 */
    async handleTrialListenAudioPreview(file) {
      // 优先使用 file.url，如果没有则从表单中获取
      const rawUrl = file?.url || this.form.trialListenAudioUrl
      if (!rawUrl) {
        this.$modal.msgWarning('音频地址不存在')
        return
      }
      const previewUrl = await this.resolveMediaPreviewUrl(rawUrl)
      if (!previewUrl) {
        this.$modal.msgError('无法解析音频地址')
        return
      }
      const fileName = file?.name || this.getFileNameFromUrl(rawUrl) || '试听音频'
      this.playAudioPreview(previewUrl, fileName)
    },
    /** 操作提示图片文件变化处理（OssUpload组件回调） */
    handleOperateListenImageChange(url) {
      // OssUpload 组件的 @change 事件传递的是 URL（字符串），而不是文件对象
      // URL 为空表示移除，URL 有值表示上传成功
      if (url) {
        // 更新表单字段
        this.form.operateListenImageUrl = url
        this.form.operateListenImagePath = url // OssUpload 返回的是完整URL，path 也使用 URL
        this.form.operateListenImage = url // 用于表单验证
        // 清除验证错误
        this.$nextTick(() => {
          if (this.$refs.form) {
            this.$refs.form.clearValidate('operateListenImage')
            this.$nextTick(() => {
              this.$refs.form.validateField('operateListenImage', () => {})
            })
          }
        })
      } else {
        // URL 为空表示移除
        this.form.operateListenImageUrl = undefined
        this.form.operateListenImagePath = undefined
        this.form.operateListenImage = undefined
        // 重新验证字段
        this.$nextTick(() => {
          if (this.$refs.form) {
            this.$refs.form.validateField('operateListenImage', () => {})
          }
        })
      }
    },
    /** 操作提示图片预览 */
    async handleOperateListenImagePreview(file) {
      // 优先使用 file.url，如果没有则从表单中获取
      const rawUrl = file?.url || this.form.operateListenImageUrl
      if (!rawUrl) {
        this.$modal.msgWarning('文件地址不存在')
        return
      }
      const previewUrl = await this.resolveMediaPreviewUrl(rawUrl)
      this.imagePreviewUrl = previewUrl
      this.imagePreviewVisible = true
    },
    /** 处理上传进度 */
    handleUploadProgress(type, isUploading) {
      this.$set(this.uploadStatus, type, isUploading)
    },
    /** 检查是否有正在上传的文件 */
    checkUploading() {
      // 检查本页面的上传状态
      const hasUploading = Object.values(this.uploadStatus).some(status => status === true)

      // 检查 Element UI 上传组件的内部状态
      if (this.$refs.introAudioUpload) {
        const introAudioFiles = this.$refs.introAudioUpload.uploadFiles || []
        if (introAudioFiles.some(file => file.status === 'uploading')) {
          this.handleUploadProgress('introAudio', true)
          return true
        }
      }

      if (this.$refs.notesAudioUpload) {
        const notesAudioFiles = this.$refs.notesAudioUpload.uploadFiles || []
        if (notesAudioFiles.some(file => file.status === 'uploading')) {
          this.handleUploadProgress('notesAudio', true)
          return true
        }
      }

      if (this.$refs.operateListenImageUpload) {
        const operateListenImageFiles = this.$refs.operateListenImageUpload.uploadFiles || []
        if (operateListenImageFiles.some(file => file.status === 'uploading')) {
          this.handleUploadProgress('operateListenImage', true)
          return true
        }
      }
      if (this.$refs.trialIntroAudioUpload) {
        const trialIntroAudioFiles = this.$refs.trialIntroAudioUpload.uploadFiles || []
        if (trialIntroAudioFiles.some(file => file.status === 'uploading')) {
          this.handleUploadProgress('trialIntroAudio', true)
          return true
        }
      }
      if (this.$refs.trialListenAudioUpload) {
        const trialListenAudioFiles = this.$refs.trialListenAudioUpload.uploadFiles || []
        if (trialListenAudioFiles.some(file => file.status === 'uploading')) {
          this.handleUploadProgress('trialListenAudio', true)
          return true
        }
      }

      // 检查试卷结构组件中的上传状态
      if (this.$refs.paperStructureTable) {
        const structureUploading = this.$refs.paperStructureTable.checkUploading()
        if (structureUploading) {
          return true
        }
      }

      if (hasUploading) {
        return true
      }

      return false
    },

    /** 获取表格行的唯一key */
    getRowKey(row) {
      return row.id || row.questionId || `row_${Date.now()}_${Math.random()}`
    },
    /** 卷别管理刷新回调 */
    handleVolumeRefresh() {
      // 可以在这里处理刷新逻辑
    },
    /** 大题管理刷新回调 */
    handleSectionRefresh() {
      // 可以在这里处理刷新逻辑
    },
    /** 中场配置刷新回调 */
    handleIntermissionRefresh() {
      // 可以在这里处理刷新逻辑
    },
    async previewIntroAudio() {
      if (!this.form.introAudioUrl) {
        this.$modal.msgWarning('请先上传测试音频')
        return
      }
      const previewUrl = await this.resolveMediaPreviewUrl(this.form.introAudioUrl)
      if (!previewUrl) {
        this.$modal.msgError('无法解析音频地址')
        return
      }
      const fileName = this.getFileNameFromUrl(this.form.introAudioUrl) || '测试音频'
      this.playAudioPreview(previewUrl, fileName)
    },
    async handleStructureAudioPreview(file) {
      const rawUrl = file?.url
      if (!rawUrl) {
        this.$modal.msgWarning('音频地址不存在')
        return
      }
      const previewUrl = await this.resolveMediaPreviewUrl(rawUrl)
      if (!previewUrl) {
        this.$modal.msgError('无法解析音频地址')
        return
      }
      const fileName = file?.name || this.getFileNameFromUrl(rawUrl) || '音频预览'
      this.playAudioPreview(previewUrl, fileName)
    },
    playAudioPreview(url, fileName) {
      if (!url) {
        this.$modal.msgError('音频地址不存在')
        return
      }
      let audioUrl = url
      if (!audioUrl.startsWith('http://') && !audioUrl.startsWith('https://')) {
        if (audioUrl.startsWith('/')) {
          audioUrl = process.env.VUE_APP_BASE_API + audioUrl
        } else {
          audioUrl = `${process.env.VUE_APP_BASE_API}/${audioUrl}`
        }
      }
      audioUrl = encodeUrlFileName(audioUrl)
      if (!audioUrl) {
        this.$modal.msgError('音频地址无效')
        return
      }
      const extension = (audioUrl.split('.').pop() || '').toLowerCase()
      let mimeType = 'audio/mpeg'
      if (extension === 'wav') mimeType = 'audio/wav'
      else if (extension === 'ogg') mimeType = 'audio/ogg'
      else if (['m4a', 'mp4', 'aac'].includes(extension)) mimeType = 'audio/mp4'
      this.audioPlayerUrl = audioUrl
      this.audioPlayerMimeType = mimeType
      this.audioPlayerTitle = fileName || '音频预览'
      this.audioPlayerVisible = true
      this.$nextTick(() => {
        const audioEl = this.$refs.audioPlayer
        if (audioEl) {
          audioEl.volume = this.audioMuted ? 0 : (this.audioVolume / 100)
          const play = () => {
            audioEl.play().then(() => {
              this.audioPlaying = true
            }).catch(err => {
              console.warn('音频自动播放失败:', err)
            })
          }
          audioEl.load()
          audioEl.addEventListener('canplay', play, { once: true })
          if (audioEl.readyState >= 2) {
            play()
          }
        }
      })
    },
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
    toggleAudioPlay() {
      const audioEl = this.$refs.audioPlayer
      if (!audioEl) return
      if (this.audioPlaying) {
        audioEl.pause()
        this.audioPlaying = false
      } else {
        audioEl.play().then(() => {
          this.audioPlaying = true
        }).catch(err => {
          console.error('播放失败:', err)
          this.$modal.msgError('音频播放失败')
        })
      }
    },
    stopAudio() {},
    handleAudioTimeUpdate() {
      const audioEl = this.$refs.audioPlayer
      if (audioEl) {
        this.currentTime = audioEl.currentTime
        if (this.duration > 0) {
          this.audioProgress = (audioEl.currentTime / this.duration) * 100
        }
      }
    },
    handleAudioLoaded() {
      if (this.$refs.audioPlayer) {
        this.duration = this.$refs.audioPlayer.duration
      }
    },
    handleAudioEnded() {
      this.audioPlaying = false
      this.currentTime = 0
      this.audioProgress = 0
    },
    handleAudioProgressChange(value) {
      if (this.$refs.audioPlayer && this.duration > 0) {
        this.$refs.audioPlayer.currentTime = (value / 100) * this.duration
        this.currentTime = this.$refs.audioPlayer.currentTime
      }
    },
    handleAudioVolumeInput(value) {
      if (this.$refs.audioPlayer) {
        this.$refs.audioPlayer.volume = value / 100
      }
      this.volumeTooltipVisible = true
      if (this.volumeTooltipTimer) {
        clearTimeout(this.volumeTooltipTimer)
      }
      this.volumeTooltipTimer = setTimeout(() => {
        this.volumeTooltipVisible = false
      }, 500)
    },
    handleAudioVolumeChange(value) {
      if (this.$refs.audioPlayer) {
        this.$refs.audioPlayer.volume = value / 100
        this.audioMuted = value === 0
      }
      this.volumeTooltipVisible = false
      if (this.volumeTooltipTimer) {
        clearTimeout(this.volumeTooltipTimer)
      }
    },
    toggleVolumeSlider(event) {
      if (event) event.stopPropagation()
      this.volumeSliderVisible = !this.volumeSliderVisible
      if (this.volumeSliderVisible) {
        this.$nextTick(() => {
          setTimeout(() => {
            document.addEventListener('click', this.handleClickOutsideVolumeSlider, true)
          }, 0)
        })
      } else {
        document.removeEventListener('click', this.handleClickOutsideVolumeSlider, true)
      }
    },
    handleClickOutsideVolumeSlider(event) {
      const wrapper = this.$el?.querySelector('.audio-volume-wrapper')
      const container = this.$el?.querySelector('.audio-volume-slider-container')
      const button = this.$el?.querySelector('.audio-volume-btn')
      if (!wrapper) return
      const isInside = wrapper.contains(event.target) ||
        (container && container.contains(event.target)) ||
        (button && button.contains(event.target))
      if (!isInside) {
        this.volumeSliderVisible = false
        document.removeEventListener('click', this.handleClickOutsideVolumeSlider, true)
      }
    },
    toggleMute() {},
    formatTime(seconds) {
      if (!seconds || isNaN(seconds)) return '0:00'
      const mins = Math.floor(seconds / 60)
      const secs = Math.floor(seconds % 60)
      return `${mins}:${secs.toString().padStart(2, '0')}`
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
.audio-preview-actions {
  margin-top: 8px;
}
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
  width: 32px;
  height: 32px;
  border-radius: 50%;
}
.audio-control-btn:hover {
  background-color: rgba(0, 0, 0, 0.1);
}
.audio-time-display {
  font-size: 13px;
  color: #606266;
  min-width: 80px;
  text-align: center;
}
.audio-progress-container {
  flex: 1;
  min-width: 150px;
  max-width: 300px;
}
.audio-volume-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}
.audio-volume-btn {
  background: none;
  border: 2px solid transparent;
  border-radius: 50%;
  width: 36px;
  height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.audio-volume-btn.volume-btn-active {
  border-color: #409EFF;
  background: rgba(64, 158, 255, 0.12);
}
.audio-volume-slider-container {
  position: absolute;
  right: 100%;
  top: 50%;
  transform: translateY(-50%);
  margin-right: 8px;
  background: #f5f7fa;
  border-radius: 20px;
  padding: 4px 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}
.audio-volume-slider {
  width: 120px;
}
.audio-volume-tooltip {
  position: absolute;
  top: -30px;
  left: 50%;
  transform: translateX(-50%);
  padding: 4px 8px;
  background: rgba(0, 0, 0, 0.75);
  color: #fff;
  border-radius: 4px;
  font-size: 12px;
}
.audio-player-footer {
  padding: 10px 20px 20px;
  text-align: right;
  border-top: 1px solid #e4e7ed;
}
</style>

