package com.zx.student.archive.domain.dto.paper;

import com.zx.student.archive.domain.dto.question.QuestionAnswerDTO;
import com.zx.student.archive.domain.dto.question.QuestionMediaDTO;
import com.zx.student.archive.domain.dto.question.QuestionRecognitionDTO;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 题目结果DTO
 */
@Data
public class QuestionResultDTO implements Serializable {

    /**
     * 题目ID
     */
    
    private Integer questionId;

    /**
     * 题目标题
     */
    
    private String title;

    /**
     * 题目类型
     */
    
    private Integer type;

    /**
     * 题目类型名称
     */
    
    private String typeName;

    /**
     * 媒体类型
     */
    
    private Integer mediaType;

    /**
     * 选项类型
     */
    
    private Integer optionType;
    /**
     * 媒体文件url
     */
    
    private List<QuestionMediaDTO> mediaUrl;

    
    private List<QuestionRecognitionDTO> aidedRecognitionUrl;

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
     * 解析
     */
    
    private String analyzes;

    /**
     * 结果 1 正确 0 错误
     */
    
    private Integer result;

    /**
     * 题目排序
     */
    
    private Integer questionSort;

    /**
     * 完形填空结果列表
     */
    
    private List<BlankResultDTO> blankResults;
} 