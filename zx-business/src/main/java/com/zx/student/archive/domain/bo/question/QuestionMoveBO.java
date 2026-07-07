package com.zx.student.archive.domain.bo.question;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 题目移动业务对象
 */
@Data
public class QuestionMoveBO implements Serializable {

    /**
     * 源题目ID列表
     */
    private List<Integer> questionIds;

    /**
     * 目标分类ID
     */
    private Integer targetCategoryId;
} 