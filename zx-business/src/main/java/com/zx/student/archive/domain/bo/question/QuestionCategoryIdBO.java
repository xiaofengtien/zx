package com.zx.student.archive.domain.bo.question;

import lombok.Data;

import java.io.Serializable;

@Data
public class QuestionCategoryIdBO implements Serializable {


    /**
     * 主键
     */
    
    private Integer id;
    /**
     * 父节点
     */
    
    private Integer fatherId;
} 