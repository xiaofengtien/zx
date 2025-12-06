package com.ruoyi.student.archive.domain.bo.question;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * 删除错题参数
 */
@Data
public class WrongQuestionIdsBO implements Serializable {

    /**
     * 用户id
     */
    private Integer appUserId;

    /**
     * 问题id列表
     */
    private Set<Integer> questionIds;

} 