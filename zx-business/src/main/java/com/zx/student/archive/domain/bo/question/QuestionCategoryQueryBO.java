package com.zx.student.archive.domain.bo.question;

import lombok.Data;

/**
 * 分类查询业务对象
 */
@Data
public class QuestionCategoryQueryBO {

    /**
     * 父级ID
     */
    private Integer fatherId;

    /**
     * 名称
     */
    private String name;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 是否包含子级
     */
    private Boolean includeChildren;
} 