package com.ruoyi.student.archive.domain.dto.question;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 错题DTO
 */
@Data
public class WrongQuestionDTO implements Serializable {


    /**
     * ID
     */
    
    private Integer id;

    /**
     * 用户id
     */
    
    private Integer appUserId;

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
    
    private String type;

    /**
     * 学科ID
     */
    
    private Integer subjectId;

    /**
     * 学科名称
     */
    
    private String subjectName;

    /**
     *来源类型 1:用户答题 2:用户评测
     */
    
    private Integer sourceType;

    /**
     * 来源ID
     */
    
    private Integer sourceId;

    /**
     * 空位数量（仅完形填空题使用）
     */
    
    private Integer blankCount;

    /**
     * 创建时间
     */
    
    private LocalDateTime createTime;
} 