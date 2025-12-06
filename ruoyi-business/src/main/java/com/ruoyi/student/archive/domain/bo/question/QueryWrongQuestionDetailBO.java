package com.ruoyi.student.archive.domain.bo.question;

import lombok.Data;

import java.io.Serializable;

/**
 * 查询错题详情参数
 */
@Data
public class QueryWrongQuestionDetailBO implements Serializable {

    /**
     * 用户id
     */
    
    private Integer appUserId;

    /**
     * 问题id
     */
    
    private Integer questionId;

    /**
     * 题目类型
     */
    
    private String questionType;
} 