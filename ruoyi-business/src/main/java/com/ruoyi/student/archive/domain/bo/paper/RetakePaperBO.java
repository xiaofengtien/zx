package com.ruoyi.student.archive.domain.bo.paper;

import lombok.Data;

import java.io.Serializable;

/**
 * 提交试卷参数BO
 */
@Data
public class RetakePaperBO extends BasicPaperBO implements Serializable {


    /**
     * 试卷ID
     */
    
    private Integer paperId;

    /**
     * 用户ID
     */
    
    private Integer appUserId;


}


