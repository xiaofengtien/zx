package com.zx.student.archive.domain.dto.question;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class QuestionAnswerDTO implements Serializable {


    /**
     * 主键
     */
    
    private Integer id;

    /**
     * 题目id
     */
    
    private Integer questionId;


    /**
     * 完形填空区域id
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
     * 媒体文件url
     */
    
    private List<QuestionMediaDTO> mediaUrl;


    /**
     * 是否答案（0-否 1-是）
     */
    
    private Integer isAnswer;

} 