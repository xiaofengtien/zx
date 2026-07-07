package com.zx.student.archive.domain.bo.paper;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 查询试卷结果参数BO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class QueryPaperResultBO extends BasicPaperBO implements Serializable {

    /**
     * 试卷ID
     */
    
    private Integer paperId;

    /**
     * 用户ID
     */
    
    private Integer appUserId;
} 