package com.zx.student.archive.domain.dto.question;

import lombok.Data;

import java.io.Serializable;

@Data
public class QuestionBusinessRefDTO implements Serializable {


    /**
     * 主键
     */
    
    private Integer id;

    /**
     * 题目
     */
    
    private Integer questionId;

    /**
     * 题目
     */
    
    private String title;

    /**
     * 题目类型 0-单选题 1-多选题 2-判断题 3-填空题 4-排序题，5-完形填空
     */
    
    private Integer type;

    /**
     * 题目类型名称
     */
    
    private String typeName;

    /**
     * 学科ID
     */
    
    private Integer subjectId;

    /**
     * 选项类型
     */
    
    private Integer mediaType;

    /**
     * 学科名称
     */
    
    private String subjectName;

    
    private Integer questionCategoryId;

    
    private Integer orderNum;
}