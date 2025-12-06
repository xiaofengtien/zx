package com.ruoyi.student.archive.domain.dto.question;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 错题详情DTO
 */
@Data
public class WrongQuestionDetailDTO implements Serializable {

    /**
     * 问题id
     */
    
    private Integer questionId;

    /**
     * 题目内容
     */
    
    private String questionContent;

    /**
     * 题目类型
     */
    
    private String questionType;

    /**
     * 学科ID
     */
    
    private Integer subjectId;

    /**
     * 学科名称
     */
    
    private String subjectName;

    /**
     * 来源类型 1:用户答题 2:用户评测
     */
    
    private Integer sourceType;

    /**
     * 来源ID
     */
    
    private Integer sourceId;

    /**
     * 用户错误答案（仅普通题使用）
     */
    
    private String userAnswer;

    /**
     * 用户选择的答案ID（仅普通题使用，多个英文逗号隔开）
     */
    
    private String answerIds;

    /**
     * 完形填空空位详情列表（仅完形填空题使用）
     */
    
    private List<BlankWrongDetailDTO> blankDetails;
}