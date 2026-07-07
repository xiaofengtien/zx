package com.zx.student.archive.domain.bo.question;

import lombok.Data;

import java.io.Serializable;

/**
 * @author xuezhi
 * Copyright (C), 2025, com.dbj
 * FileName: QuestionBusinessRefBO
 * Date:     2025-05-19 11:12:52
 * Description: 表名： ,描述： 表
 */
@Data
public class QuestionBusinessRefPageBO implements Serializable {
    
    private Integer businessId;

    
    private Integer businessType;

}
