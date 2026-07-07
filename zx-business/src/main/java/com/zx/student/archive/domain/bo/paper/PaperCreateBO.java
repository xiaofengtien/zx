package com.zx.student.archive.domain.bo.paper;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 创建试卷参数
 * 
 * @author zx
 */
@Data
public class PaperCreateBO implements Serializable {
    
    private static final long serialVersionUID = 1L;

    /**
     * 试卷名称
     */
    private String paperName;

    /**
     * 试卷类型（字典编码：paper_type，必填）
     */
    private String paperType;

    /**
     * 年份（必填，2000-2050）
     */
    private Integer year;

    /**
     * 月份（必填，1-12）
     */
    private Integer month;

    /**
     * 省份（字典编码：paper_province，必填）
     */
    private String province;

    /**
     * 自定义名称（非必填，用于区分同一年月省份类型的试卷）
     */
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
     * 业务ID（关联question_category_business_settings.id）
     */
    private Integer businessId;

    /**
     * 考试时长（分钟）
     */
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
     * 是否自动跳转下一题：1-是，0-否（默认1）
     */
    private Integer autoNextQuestion;

    /**
     * 是否立即显示答案：1-是，0-否（默认0）
     */
    private Integer showAnswerImmediately;

    /**
     * 是否允许回顾：1-是，0-否（默认1）
     */
    private Integer allowReview;

    /**
     * 每题读题时长（秒）
     */
    private Integer questionReadDuration;

    /**
     * 练习次数限制（0表示不限制，按试卷提交次数计算）
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
    private String notes;

    /**
     * 注意事项显示时机（before_exam-考试前，before_section-每大题前）
     */
    private String notesDisplayMode;

    /**
     * 试卷启用开始时间（必填）
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date enableStartTime;

    /**
     * 试卷启用结束时间（必填）
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date enableEndTime;

    /**
     * 状态：1-启用，0-禁用（默认1）
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 题目ID列表（用于关联题目）
     */
    private List<Integer> questionIds;

    /**
     * 题目分值列表（与questionIds对应，可选）
     */
    private List<BigDecimal> scores;
}

