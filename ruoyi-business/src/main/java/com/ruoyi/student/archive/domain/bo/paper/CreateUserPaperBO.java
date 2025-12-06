package com.ruoyi.student.archive.domain.bo.paper;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 创建用户试卷参数
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class CreateUserPaperBO extends BasicPaperBO implements Serializable {

    
    private Integer appUserId;

    
    private String paperName;

    
    private Integer isSubmit;

    /**
     * 题目顺序，逗号分隔的题目ID
     */
    
    private String questionOrder;
} 