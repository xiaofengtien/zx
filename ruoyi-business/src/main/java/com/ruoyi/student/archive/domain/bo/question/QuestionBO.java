package com.ruoyi.student.archive.domain.bo.question;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class QuestionBO implements Serializable {

    /**
     * 主键
     */
    
    private Integer id;

    /**
     * 分类id
     */
    
    private Integer questionCategoryId;

    
    private Integer subjectId;

    /**
     * 题目
     */
    
    private String title;

    /**
     * 媒体文件类型 2-图片 3-音频 4-视频 1- 文本
     * @see com.ruoyi.common.enums.question.OptionTypeEnum
     */
    
    private Integer mediaType;

    /**
     * 媒体文件url
     */
    
    private List<QuestionMediaBO> mediaUrl;

    
    private List<QuestionRecognitionBO> aidedRecognitionUrl;
    /**
     * 题目类型
     */
    
    private Integer type;

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
     * 答案列表
     */
    
    private List<QuestionAnswerBO> answers;

    /**
     * 完形填空区域列表
     */
    
    private List<QuestionBlankAreaBO> blankAreas;
} 