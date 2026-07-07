package com.zx.student.archive.domain.dto.question;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class QuestionCategoryDTO implements Serializable {

    /**
     * 主键
     */
    private Integer id;

    /**
     * 类型名称
     */
    private String name;

    /**
     * 父级id
     */
    private Integer fatherId;

    /**
     * 是否默认分类：0 否 1 是
     */
    private Integer isDefault;

    private Integer sortNum;

    /**
     * 子节点数量
     */
    private Integer childrenCount;

    /**
     * 当前分类的题目
     */
    
    private Integer currentQuestionCount;

    /**
     * 题目数量（包含当前分类及所有子分类的题目）
     */
    private Integer questionCount;

    /**
     * 子节点列表
     */
    private List<QuestionCategoryDTO> children;
} 