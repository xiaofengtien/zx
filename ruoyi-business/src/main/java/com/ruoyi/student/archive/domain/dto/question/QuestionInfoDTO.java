package com.ruoyi.student.archive.domain.dto.question;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 题目信息DTO
 */
@Data
public class QuestionInfoDTO implements Serializable {

    /**
     * 主键
     */
    private Integer id;

    /**
     * 分类id
     */
    private Integer questionCategoryId;

    /**
     * 题目
     */
    private String title;

    /**
     * 媒体文件类型 1-文本 2-图片,3-音频,4-视频
     */
    private Integer mediaType;

    /**
     * 媒体文件url
     */
    private List<QuestionMediaDTO> mediaUrl;

    /**
     * 辅助识图
     */
    private List<QuestionRecognitionDTO> aidedRecognitionUrl;

    /**
     * 题目类型 0-单选题 1-多选题 2-判断题 3-填空题 4-排序题，5-完形填空
     */
    private Integer type;

    /**
     * 题目类型名称
     */
    private String typeName;

    /**
     * 选项类型 1-文本 2-图片,3-音频,4-视频
     */
    private Integer optionType;

    /**
     * 权重
     */
    private Integer weight;

    /**
     * 选择题答案id（多个英文逗号隔开）
     */
    private String answer;

    /**
     * 解析
     */
    private String analyzes;

    /**
     * 是否有答题讲解（0-否，1-是）
     */
    private Integer explanationEnabled;

    /**
     * 讲解文字
     */
    private String explanationText;

    /**
     * 讲解显示延迟（秒，播放完试卷名称音频后等待时间，默认2秒）
     */
    private Integer explanationDelaySeconds;

    /**
     * 答案列表
     */
    private List<QuestionAnswerDTO> answers;

    /**
     * 完形填空区域列表
     */
    private List<QuestionBlankAreaDTO> blankAreas;

    /**
     * 学科ID
     */
    private Integer subjectId;

    /**
     * 学科名称
     */
    private String subjectName;

    /**
     * 引用数量
     */
    private Integer referenceCount;

    /**
     * 缩略图URL
     */
    private String thumbnailUrl;

    // ========== 填空题字段 ==========

    /**
     * 填空题答案列表（从answer字段解析）
     */
    private List<String> blankAnswers;

    // ========== 作文题字段 ==========

    /**
     * 词数限制（0表示不限制）
     */
    private Integer wordLimit;

    /**
     * 写作要求列表
     */
    private List<String> requirements;

    /**
     * 参考范文
     */
    private String sampleAnswer;
}