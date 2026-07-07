package com.zx.student.archive.domain.bo.paper;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 保存试卷题目空位作答结果参数
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SaveBlankResultBO extends BasicPaperBO implements Serializable {

    
    private Integer paperId;

    
    private Integer appUserId;

    
    private Integer questionId;

    
    private Integer blankIndex;
    /**
     * 用户选择的答案ID列表
     */
    
    private String answerIds;
    
    private Integer blankAreaId;


    
    private Integer result;
} 