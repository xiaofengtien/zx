package com.zx.student.archive.domain.dto.question;

import lombok.Data;

import java.io.Serializable;

/**
 * 完形填空错题详情DTO
 */
@Data
public class BlankWrongDetailDTO implements Serializable {

    /**
     * ID
     */
    
    private Integer id;

    /**
     * 填空题空位序号
     */
    
    private Integer blankIndex;

    /**
     * 空位区域ID
     */
    
    private Integer blankAreaId;

    /**
     * 用户错误答案
     */
    
    private String userAnswer;

    /**
     * 用户选择的答案ID（多个英文逗号隔开）
     */
    
    private String answerIds;
} 