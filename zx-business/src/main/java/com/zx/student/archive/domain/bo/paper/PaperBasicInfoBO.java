package com.zx.student.archive.domain.bo.paper;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 试卷基本信息BO
 * 
 * @author zx
 */
@Data
public class PaperBasicInfoBO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 试卷ID (编辑时必填,新增时为空)
     */
    private Integer id;

    /**
     * 试卷类型（字典编码：paper_type，必填）
     */
    @NotBlank(message = "试卷类型不能为空")
    private String paperType;

    /**
     * 年份（2000-2050）
     */
    private Integer year;

    /**
     * 月份（1-12）
     */
    private Integer month;

    /**
     * 省份编码（字典值：paper_province）
     */
    private String province;

    /**
     * 自定义名称（必填）
     */
    @NotBlank(message = "自定义试卷名称不能为空")
    private String customName;

    /**
     * 试卷描述
     */
    private String paperDesc;

    /**
     * 业务类型（默认5-题库）
     */
    private Integer businessType;

    /**
     * 业务ID
     */
    private Integer businessId;

    /**
     * 总分
     */
    private BigDecimal totalScore;

    /**
     * 题目总数
     */
    private Integer totalQuestions;

    /**
     * 考试时长（分钟）
     */
    @NotNull(message = "考试时长不能为空")
    private Integer duration;

    /**
     * 开场独白音频URL
     */
    private String introAudioUrl;

    /**
     * 开场独白音频路径
     */
    private String introAudioPath;

    /**
     * 开场独白时长（秒）
     */
    private Integer introAudioDuration;

    /**
     * 开场独白文本内容
     */
    private String introText;

    /**
     * 是否自动跳转下一题：1-是，0-否
     */
    private Integer autoNextQuestion;

    /**
     * 是否立即显示答案：1-是，0-否
     */
    private Integer showAnswerImmediately;

    /**
     * 是否允许回顾：1-是，0-否
     */
    private Integer allowReview;

    /**
     * 每题读题时长（秒）
     */
    private Integer questionReadDuration;

    /**
     * 限制练习次数（0表示不限制）
     */
    private Integer practiceLimit;

    /**
     * 是否启用试听（0-否，1-是）
     */
    private Integer trialListenEnabled;

    /**
     * 试听音频示例URL
     */
    private String trialListenAudioUrl;

    /**
     * 试听音频示例本地路径
     */
    private String trialListenAudioPath;

    /**
     * 试听音频时长（秒）
     */
    private Integer trialListenAudioDuration;

    /**
     * 试听文本内容（描述试听的内容）
     */
    private String trialListenAudioText;

    /**
     * 试听旁白音频URL（介绍音频，自动播放）
     */
    private String trialIntroAudioUrl;

    /**
     * 试听旁白音频本地路径
     */
    private String trialIntroAudioPath;

    /**
     * 试听旁白音频时长（秒）
     */
    private Integer trialIntroAudioDuration;

    /**
     * 操作提示文本
     */
    private String operateListenText;

    /**
     * 操作提示图片URL
     */
    private String operateListenImageUrl;

    /**
     * 操作提示图片本地路径
     */
    private String operateListenImagePath;

    /**
     * 注意事项（支持富文本）
     */
    @NotBlank(message = "注意事项不能为空")
    private String notes;

    /**
     * 注意事项显示时机（before_exam-考试前，before_section-每大题前）
     */
    private String notesDisplayMode;

    /**
     * 试卷启用开始时间（必填）
     */
    @NotNull(message = "试卷启用开始时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date enableStartTime;

    /**
     * 试卷启用结束时间（必填）
     */
    @NotNull(message = "试卷启用结束时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date enableEndTime;

    /**
     * 状态：1-启用，0-禁用
     */
    @NotNull(message = "状态不能为空")
    private Integer status;

    /**
     * 备注
     */
    private String remark;
}
