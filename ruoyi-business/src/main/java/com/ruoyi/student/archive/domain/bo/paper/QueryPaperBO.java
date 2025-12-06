package com.ruoyi.student.archive.domain.bo.paper;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 查询试卷题目参数BO
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class QueryPaperBO extends BasicPaperBO implements Serializable {

    
    private Integer paperId;

    
    private Integer appUserId;
} 