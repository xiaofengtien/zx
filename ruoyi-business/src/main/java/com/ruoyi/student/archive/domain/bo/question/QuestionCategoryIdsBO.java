package com.ruoyi.student.archive.domain.bo.question;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class QuestionCategoryIdsBO implements Serializable {

    /**
     * 主键
     */
    private List<Integer> ids;
} 