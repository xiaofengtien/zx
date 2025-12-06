package com.ruoyi.student.archive.domain.dto.question;

import lombok.Data;

import java.io.Serializable;

@Data
public class WrongQuestionSubjectDTO implements Serializable {


    /**
     * 学科ID
     */
    
    private Integer subjectId;

    /**
     * 学科名称
     */
    
    private String subjectName;

}