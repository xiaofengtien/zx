package com.ruoyi.student.archive.domain.dto.question;

import lombok.Data;

import java.io.Serializable;

@Data
public class BlankCorrectAnswerDTO implements Serializable {
    
    private Integer questionId;
    
    private Integer blankAreaId;
    
    private Integer blankIndex;
    
    private String correctAnswer;
    
    private String correctAnswerIds;
} 