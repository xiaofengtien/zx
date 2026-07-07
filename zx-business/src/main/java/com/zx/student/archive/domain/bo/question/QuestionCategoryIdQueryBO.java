package com.zx.student.archive.domain.bo.question;

import lombok.Data;

import java.io.Serializable;

@Data
public class QuestionCategoryIdQueryBO implements Serializable {


    /**
     * 分类ID
     */
    private Integer categoryId;
} 