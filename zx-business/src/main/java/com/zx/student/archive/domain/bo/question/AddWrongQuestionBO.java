package com.zx.student.archive.domain.bo.question;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class AddWrongQuestionBO implements Serializable {

    /**
     * 用户id
     */
    
    private Integer appUserId;

    /**
     * 题目id
     */
    
    private Integer questionId;


    /**
     * 1:用户答题练习,2:用户评测答题
     */
    
    private Integer sourceType;

    /**
     * 来源ID
     */
    
    private Integer sourceId;

    /**
     * 用户错误答案
     */
    
    private String userAnswer;

    /**
     * 用户选择的答案ID（多个英文逗号隔开）
     */
    
    private String userAnswerIds;

    /**
     * 完形填空空位信息（仅完形填空题使用）
     */
    
    private List<WrongQuestionBlankBO> blankResults;
}