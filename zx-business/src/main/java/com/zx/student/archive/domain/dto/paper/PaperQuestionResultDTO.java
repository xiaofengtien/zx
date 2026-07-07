package com.zx.student.archive.domain.dto.paper;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 试卷结果DTO
 */
@Data
public class PaperQuestionResultDTO implements Serializable {

    /**
     * 题目结果列表
     */
    
    private List<QuestionResultDTO> questionResults;

    /**
     * 正确题目数量
     */
    
    private Integer correctCount;

    /**
     * 错误题目数量
     */
    
    private Integer wrongCount;
} 