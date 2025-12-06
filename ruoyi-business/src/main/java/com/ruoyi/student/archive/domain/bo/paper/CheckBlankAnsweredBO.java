package com.ruoyi.student.archive.domain.bo.paper;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 检查空位作答完成参数BO
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class CheckBlankAnsweredBO extends BasicPaperBO implements Serializable {

    
    private Integer questionId;

    
    private Integer appUserId;

    
    private Integer totalBlanks;
} 