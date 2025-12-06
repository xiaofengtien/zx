package com.ruoyi.student.archive.domain.bo.paper;

import lombok.*;

import java.io.Serializable;

/**
 * 查询试卷题目空位作答结果参数
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryBlankResultBO extends BasicPaperBO implements Serializable {

    
    private Integer paperId;

    
    private Integer appUserId;

    
    private Integer questionId;

}