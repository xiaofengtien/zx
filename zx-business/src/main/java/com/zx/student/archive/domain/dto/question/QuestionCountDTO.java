package com.zx.student.archive.domain.dto.question;

import lombok.Data;

import java.io.Serializable;

@Data
public class QuestionCountDTO implements Serializable {
    /**
     * 分类id
     */
    
    private Integer questionCategoryId;

    /**
     * 题目
     */
    
    private Integer num;

}