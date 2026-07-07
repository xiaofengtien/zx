package com.zx.student.archive.domain.bo.question;

import lombok.Data;

import java.io.Serializable;
import java.util.Set;

@Data
public class QueryWrongQuestionsBO implements Serializable {

    /**
     * 用户id
     */
    
    private Integer appUserId;

    /**
     * 学科ID（可选）
     */
    
    private Integer subjectId;
    
    /**
     * 题目ID集合（可选，为空则查询全部）
     */
    
    private Set<Integer> questionIds;
    
    /**
     * 是否包含答案信息
     */
    
    private Boolean includeAnswers;
    
    /**
     * 题目类型（可选，为空则查询全部类型）
     */
    
    private Integer type;
    
    /**
     * 来源类型（可选，为空则查询全部来源）
     */
    
    private Integer sourceType;
    
    /**
     * 来源ID（可选）
     */
    
    private Integer sourceId;
}