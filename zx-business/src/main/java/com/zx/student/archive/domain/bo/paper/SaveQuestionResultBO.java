package com.zx.student.archive.domain.bo.paper;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 保存试卷题目结果参数
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SaveQuestionResultBO extends BasicPaperBO implements Serializable {


    /**
     * 试卷ID
     */
    
    private Integer paperId;

    /**
     * 用户ID
     */
    
    private Integer appUserId;

    /**
     * 题目ID
     */
    
    private Integer questionId;

    /**
     * 用户选择的答案ID（多个英文逗号隔开）
     */
    
    private String answerIds;


    /**
     * 用户答案
     */
    
    private String userAnswer;

    /**
     * 结果(0:错误,1:正确)
     */
    
    private Integer result;

    /**
     * 题目序号
     */
    
    private Integer questionSort;
} 