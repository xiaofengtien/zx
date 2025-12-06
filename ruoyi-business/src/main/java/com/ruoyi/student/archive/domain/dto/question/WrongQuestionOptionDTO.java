package com.ruoyi.student.archive.domain.dto.question;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class WrongQuestionOptionDTO implements Serializable {


    /**
     * ID
     */
    
    private Integer id;


    /**
     * 题目id
     */
    
    private Integer questionId;

    /**
     * 空位区域ID（仅完形填空题使用）
     */
    
    private Integer blankAreaId;

    /**
     * 序号
     */
    
    private Integer serialNo;

    /**
     * 选项名称
     */
    
    private String optionName;

    /**
     * 选项内容
     */
    
    private String optionContent;

    /**
     * 是否为答案
     */
    
    private Integer isAnswer;

    /**
     * 媒体文件URL
     */
    
    private List<QuestionMediaDTO> mediaUrl;
}