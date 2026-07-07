package com.zx.student.archive.domain.bo.question;

import lombok.Data;

import java.io.Serializable;

/**
 * 添加普通题错题参数
 */
@Data
public class AddNormalWrongQuestionBO implements Serializable {

    /**
     * 用户id
     */
    
    private Integer appUserId;

    /**
     * 问题id
     */
    
    private Integer questionId;

    /**
     * 来源类型 1:用户答题 2:用户评测
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
    
    private String answerIds;
    /**
     * 唯一标识一次错题事件
     */
    
    private String stateId;
} 