package com.zx.student.archive.domain.dto.paper;

import com.zx.student.archive.domain.dto.question.QuestionAnswerDTO;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 完形填空结果DTO
 */
@Data
public class BlankResultDTO implements Serializable {
    /**
     * 题目ID
     */
    
    private Integer questionId;


    /**
     * 空位区域ID
     */
    
    private Integer blankAreaId;

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
     * 答案选项列表
     */
    
    private List<QuestionAnswerDTO> options;

    /**
     * 结果 1 正确 0 错误
     */
    
    private Integer result;
}