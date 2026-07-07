package com.zx.student.archive.domain.bo.paper;

import lombok.Data;

import java.io.Serializable;

@Data
public class SubmitQuestionResultBO implements Serializable {

    
    private Integer questionId;

    
    private String answerIds;

    /**
     * 用户答案)
     */
    
    private String userAnswer;

    
    private Integer result;
}
