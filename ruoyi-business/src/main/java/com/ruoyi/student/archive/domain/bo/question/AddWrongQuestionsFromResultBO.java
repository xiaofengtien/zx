package com.ruoyi.student.archive.domain.bo.question;

import lombok.Data;

import java.io.Serializable;

/**
 * 从答题结果添加错题参数
 */
@Data
public class AddWrongQuestionsFromResultBO implements Serializable {


    /**
     * 用户id
     */
    
    private Integer appUserId;

    /**
     * 试卷id
     */
    
    private Integer paperId;

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
} 