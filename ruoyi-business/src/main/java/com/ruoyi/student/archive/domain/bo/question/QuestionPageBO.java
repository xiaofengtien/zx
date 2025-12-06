package com.ruoyi.student.archive.domain.bo.question;

import lombok.Data;

import java.io.Serializable;

/**
 * 题目分页查询参数
 */
@Data
public class QuestionPageBO implements Serializable {

    /**
     * 当前页码
     */
    private Integer pageNum;

    /**
     * 每页显示数量
     */
    private Integer pageSize;

    /**
     * 学科ID
     */
    private Integer subjectId;

    /**
     * 分类ID
     */
    private Integer categoryId;

    /**
     * 题目类型
     */
    private Integer type;

    /**
     * 题目标题
     */
    private String title;

    /**
     * 状态：1-启用，0-禁用
     */
    private Integer status;

} 