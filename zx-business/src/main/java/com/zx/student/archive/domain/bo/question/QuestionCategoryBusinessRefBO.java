package com.zx.student.archive.domain.bo.question;

import lombok.Data;

import java.io.Serializable;

/**
 * 题库业务关联BO
 */
@Data
public class QuestionCategoryBusinessRefBO implements Serializable {

    /**
     * 题目分类ID
     */
    
    private Integer questionCategoryId;

    /**
     * 排序
     */
    
    private Integer sortNum;
} 