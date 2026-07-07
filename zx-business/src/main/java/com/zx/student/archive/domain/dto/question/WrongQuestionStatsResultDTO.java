package com.zx.student.archive.domain.dto.question;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class WrongQuestionStatsResultDTO implements Serializable {


    /**
     * 总错题数量
     */
    
    private Integer totalCount;

    /**
     * 按题目类型统计
     */
    //
    //private List<TypeWrongStatsDTO> typeStats;

    /**
     * 按学科统计
     */
    
    private List<SubjectWrongStatsDTO> subjectStats;

    /**
     * 按来源类型统计
     */
    //
    //private List<SourceWrongStatsDTO> sourceStats;
}