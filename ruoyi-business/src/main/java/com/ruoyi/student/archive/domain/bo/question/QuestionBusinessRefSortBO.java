package com.ruoyi.student.archive.domain.bo.question;

import lombok.*;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * @author xuezhi
 * Copyright (C), 2025, com.dbj
 * FileName: QuestionBusinessRefSortBO
 * Date:     2025-04-15 13:35:46
 * Description: 表名： ,描述： 表
 */
@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@ToString
public class QuestionBusinessRefSortBO implements Serializable {

    
    private Integer appEvaluationNodeId;


    
    private Integer id;

    
    private Integer aimId;

    private Integer order;

}
