package com.zx.student.archive.domain.bo.question;

import lombok.Data;

import java.io.Serializable;

@Data
public class QuestionCategoryBO implements Serializable {
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

}