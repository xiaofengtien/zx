package com.ruoyi.student.archive.domain.dto.question;

import lombok.Data;

import java.io.Serializable;

/**
 * 学科错题统计DTO
 */
@Data
public class SubjectWrongStatsDTO implements Serializable {

    /**
     * 学科ID
     */
    
    private Integer subjectId;

    /**
     * 学科名称
     */
    
    private String subjectName;

    /**
     * 错题数量
     */
    
    private Integer count;
} 