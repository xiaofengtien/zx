package com.zx.student.archive.domain.bo.question;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class QuestionBlankAreaBO implements Serializable {

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
    /**
     * 答案列表
     */
    
    private List<QuestionAnswerBO> answers;

    /**
     * 是否启用 1-是 0-否
     */
    
    private Integer status;
} 