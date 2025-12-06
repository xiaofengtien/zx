package com.ruoyi.student.archive.domain.dto.question;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class QuestionCorrectAnswerDTO implements Serializable {
    
    private Integer questionId;
    
    private String title;
    
    private Integer type;
    
    private String subjectName;
    
    private String correctAnswerIds;
    
    private String correctAnswer;
    
    private List<BlankCorrectAnswerDTO> blankResults;
} 