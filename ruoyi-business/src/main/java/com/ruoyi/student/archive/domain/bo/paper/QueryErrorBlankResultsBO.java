package com.ruoyi.student.archive.domain.bo.paper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 查询用户错误空位答题记录参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryErrorBlankResultsBO implements Serializable {

    /**
     * 用户ID
     */
    
    private Integer appUserId;

    /**
     * 题目ID列表
     */
    
    private List<Integer> questionIds;

    /**
     * 来源类型
     */
    
    private Integer sourceType;

    /**
     * 来源ID
     */
    
    private Integer sourceId;
} 