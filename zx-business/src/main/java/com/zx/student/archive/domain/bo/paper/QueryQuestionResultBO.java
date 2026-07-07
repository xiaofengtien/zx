package com.zx.student.archive.domain.bo.paper;

import lombok.*;

import java.io.Serializable;

/**
 * 查询试卷题目结果参数
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryQuestionResultBO extends BasicPaperBO implements Serializable {


    
    private Integer paperId;

    
    private Integer appUserId;
}