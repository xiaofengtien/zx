package com.zx.student.archive.domain.dto.question;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 错题统计DTO
 */
@Data
public class WrongQuestionStatsDTO implements Serializable {


    /**
     * 总错题数量
     */
    
    private Integer totalCount;

    /**
     * 普通题错题数量
     */
    
    private Integer normalCount;

    /**
     * 完形填空题错题数量
     */
    
    private Integer clozeCount;

    /**
     * 学科错题统计列表
     */
    
    private List<SubjectWrongStatsDTO> subjectStats;
} 