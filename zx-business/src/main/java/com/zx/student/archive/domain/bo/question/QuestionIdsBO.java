package com.zx.student.archive.domain.bo.question;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class QuestionIdsBO implements Serializable {
    /**
     * 主键
     */
    
    private List<Integer> ids;


} 