package com.ruoyi.student.archive.domain.bo.question;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 删除错题参数
 */
@Data
public class RemoveWrongQuestionBO implements Serializable {

    /**
     * 用户id
     */
    
    private Integer appUserId;

    /**
     * 问题id
     */
    
    private List<Integer> questionIds;

} 