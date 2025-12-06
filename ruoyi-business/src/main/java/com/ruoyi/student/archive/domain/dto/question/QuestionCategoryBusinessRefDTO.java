package com.ruoyi.student.archive.domain.dto.question;

import lombok.Data;

import java.io.Serializable;

/**
 * 题库业务关联BO
 */
@Data
public class QuestionCategoryBusinessRefDTO implements Serializable {

    /**
     * 题目分类ID
     */
    
    private Integer questionCategoryId;
    /**
     * 题目分类名称
     */
    
    private String questionCategoryName;
    /**
     * 题目数量
     */
    
    private Integer questionNum;

    /**
     * 业务ID
     */
    
    private Integer businessId;

    /**
     * 业务类型（1-音频专辑 2-视频专辑 3-图书）
     */
    
    private Integer businessType;

    /**
     * 排序
     */
    
    private Integer sortNum;
} 