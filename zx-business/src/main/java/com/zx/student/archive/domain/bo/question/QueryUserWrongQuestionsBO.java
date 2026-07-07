package com.zx.student.archive.domain.bo.question;

import lombok.Data;

import java.io.Serializable;

/**
 * 查询用户错题列表参数
 */
@Data
public class QueryUserWrongQuestionsBO implements Serializable {


    /**
     * 用户id
     */
    
    private Integer appUserId;

    /**
     * 学科ID（可选）
     */
    
    private Integer subjectId;
} 