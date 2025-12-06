package com.ruoyi.student.archive.domain.paper;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 试卷实体
 * 
 * @author ruoyi
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("paper")
public class Paper extends BaseEntity implements Serializable {
    
    private static final long serialVersionUID = 1L;

    /**
     * 试卷ID
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 试卷名称
     */
    private String paperName;

    /**
     * 试卷编码（用于同步，格式：PAPER_YYYYMMDD_序号）
     */
    private String paperCode;

    /**
     * 试卷类型（字典编码：paper_type，如：junior_high_school_english_listening、high_school_english_listening）
     */
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
     * 自定义名称
     */
    private String customName;

    /**
     * 试卷描述
     */
    private String paperDesc;

    /**
     * 业务类型（5-题库）
     */
    private Integer businessType;

    /**
     * 业务ID（关联question_category_business_settings.id）
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
    private Integer duration;

    /**
     * 开场独白音频URL
     */
    private String introAudioUrl;

    /**
     * 开场独白音频路径（服务器）
     */
    private String introAudioPath;

    /**
     * 开场独白时长（秒）
     */
    private Integer introAudioDuration;

    /**
     * 开场独白文本内容（可选）
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
     * 限制练习次数（0表示不限制，按试卷提交次数计算）
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
     * 试卷启用开始时间（为空表示不限制开始时间）
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date enableStartTime;

    /**
     * 试卷启用结束时间（为空表示不限制结束时间）
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date enableEndTime;

    /**
     * 每题读题时长（秒，用于自动跳转）
     */
    private Integer questionReadDuration;

    /**
     * 版本号（用于增量同步）
     */
    private Integer version;

    /**
     * 数据包哈希值（SHA256）
     */
    private String packageHash;

    /**
     * 数据包大小（字节）
     */
    private Long packageSize;

    /**
     * 最后打包时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastPackageTime;

    /**
     * 状态：1-启用，0-禁用
     */
    private Integer status;
}

