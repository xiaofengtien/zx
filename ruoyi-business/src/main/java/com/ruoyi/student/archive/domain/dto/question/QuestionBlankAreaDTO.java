package com.ruoyi.student.archive.domain.dto.question;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class QuestionBlankAreaDTO implements Serializable {


    /**
     * 主键
     */
    
    private Integer id;

    /**
     * 题目id
     */
    
    private Integer questionId;

    /**
     * 空位序号
     */
    
    private Integer blankIndex;

    /**
     * 正确答案ID（多个英文逗号隔开）
     */
    
    private String answerIds;


    
    private List<QuestionAnswerDTO> answers;
} 