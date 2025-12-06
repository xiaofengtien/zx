package com.ruoyi.student.archive.domain.bo.question;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class QuestionAnswerBO implements Serializable {

    /**
     * 主键
     */
    
    private Integer id;

    /**
     * 题目id
     */
    
    private Integer questionId;
    /**
     * 媒体文件类型 1-图片 2-音频 3-视频
     */
    
    private Integer mediaType;
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
    
    private List<QuestionMediaBO> mediaUrl;

    /**
     * 是否答案（0-否 1-是）
     */
    
    private Integer isAnswer;

} 