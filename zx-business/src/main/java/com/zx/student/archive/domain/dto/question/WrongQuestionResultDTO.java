package com.zx.student.archive.domain.dto.question;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Data
public class WrongQuestionResultDTO implements Serializable {


    /**
     * ID
     */
    
    private Integer id;

    /**
     * 题目id
     */
    
    private Integer questionId;

    /**
     * 题目
     */
    
    private String title;

    /**
     * 题目类型
     */
    
    private Integer type;

    /**
     * 媒体文件类型 1-文本 2-图片 3-音频 4-视频
     */
    
    private Integer mediaType;

    /**
     * 媒体文件URL
     */
    
    private List<QuestionMediaDTO> mediaUrl;

    /**
     * 辅助识图
     */
    
    private List<QuestionRecognitionDTO> aidedRecognitionUrl;

    /**
     * 选项类型
     */
    
    private Integer optionType;

    /**
     * 学科ID
     */
    
    private Integer subjectId;

    /**
     * 学科名称
     */
    
    private String subjectName;

    /**
     * 来源类型
     */
    
    private Integer sourceType;

    /**
     * 来源ID
     */
    
    private Integer sourceId;

    /**
     * 用户答案
     */
    
    private String userAnswer;

    /**
     * 用户选择的答案ID
     */
    
    private String userAnswerIds;

    /**
     * 正确答案（当includeAnswers=true时返回）
     */
    
    private String correctAnswer;

    /**
     * 正确答案ID（当includeAnswers=true时返回）
     */
    
    private String correctAnswerIds;

    /**
     * 解析
     */
    
    private String analyzes;

    /**
     * 选项列表
     */
    
    private List<WrongQuestionOptionDTO> options;

    /**
     * 完形填空空位结果列表（仅完形填空题使用）
     */
    
    private List<WrongQuestionBlankDTO> blankResults;

    /**
     * 创建时间
     */
    
    private Date createTime;

    /**
     * 格式化后的日期（当年显示MM-DD，非当年显示YYYY-MM-DD）
     */
    
    private String formattedDate;
    /**
     * 唯一标识一次错题事件
     */
    
    private String stateId;

}