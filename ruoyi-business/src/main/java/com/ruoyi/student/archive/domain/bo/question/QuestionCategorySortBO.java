package com.ruoyi.student.archive.domain.bo.question;

import lombok.Data;

import java.io.Serializable;

/**
 * 题目分类排序BO
 */
@Data
public class QuestionCategorySortBO implements Serializable {

    
    private Integer id;

    
    private Integer fatherId;

    
    private Integer aimId;

    
    private Integer order;
}