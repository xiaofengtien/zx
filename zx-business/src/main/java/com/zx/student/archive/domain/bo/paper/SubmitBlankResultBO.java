package com.zx.student.archive.domain.bo.paper;

import lombok.Data;

import java.io.Serializable;

/**
 * 提交填空题作答结果BO
 */
@Data
public class SubmitBlankResultBO implements Serializable {


    /**
     * 题目ID
     */
    
    private Integer questionId;

    
    private Integer blankAreaId;

    /**
     * 填空序号
     */
    
    private Integer blankIndex;

    /**
     * 用户选择的答案ID列表
     */
    
    private String answerIds;


    /**
     * 用户作答结果
     */
    
    private Integer result;
} 