package com.ruoyi.student.archive.domain.dto.question;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class WrongQuestionBlankDTO implements Serializable {


    /**
     * 空位区域ID
     */
    
    private Integer blankAreaId;

    /**
     * 题目ID
     */
    
    private Integer questionId;

    /**
     * 空位序号
     */
    
    private Integer blankIndex;

    /**
     * 用户答案
     */
    
    private String userAnswer;

    /**
     * 用户选择的答案ID
     */
    
    private String userAnswerIds;

    /**
     * 正确答案
     */
    
    private String correctAnswer;

    /**
     * 正确答案ID
     */
    
    private String correctAnswerIds;

    /**
     * 结果（1-正确 2-错误）
     */
    
    private Integer result;
    /**
     * 唯一标识一次错题事件
     */
    
    private String stateId;
    /**
     * 选项列表
     */
    
    private List<WrongQuestionOptionDTO> options;
}