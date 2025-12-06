package com.ruoyi.student.archive.domain.bo.question;

import lombok.Data;

import java.io.Serializable;

@Data
public class WrongQuestionBlankBO implements Serializable {

    /**
     * 空位区域ID
     */
    
    private Integer blankAreaId;

    /**
     * 题目ID
     */
    
    private Integer questionId;

    /**
     * 空位序号
     */
    
    private Integer blankIndex;

    /**
     * 用户答案
     */
    
    private String userAnswer;

    /**
     * 用户选择的答案ID
     */
    
    private String userAnswerIds;
}