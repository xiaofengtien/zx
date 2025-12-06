package com.ruoyi.student.archive.domain.dto.question;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
public class QuestionBlankContentDTO implements Serializable {

    /**
     * 题目id
     */
    
    private Integer questionId;

    /**
     * 空位答案映射 Map<空位序号, 答案>
     */
    
    private Map<String, String> blankAnswerMap;

    /**
     * 空位选项映射 Map<空位序号, 选项列表>
     */
    
    private Map<String, List<String>> blankOptionsMap;

    /**
     * 空位内容列表
     */
    
    private List<QuestionBlankAreaDTO> blankAreaList;
} 