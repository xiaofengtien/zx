package com.ruoyi.student.archive.domain.dto.paper;

import lombok.Data;

import java.io.Serializable;

/**
 * 批改结果DTO
 */
@Data
public class MarkingResultDTO implements Serializable {

    
    private Integer paperId;

    
    private Integer appUserId;

    
    private Integer questionId;

    
    private Integer questionType;

    
    private Integer weight;

    
    private Integer score;

    
    private Integer totalScore;

    
    private String correctAnswer;

    
    private String userAnswer;

    
    private Integer result;

    
    private Integer status;

    
    private String remark;
} 