package com.zx.student.archive.domain.dto.question;

import lombok.Data;

import java.io.Serializable;
@Data
public class QuestionRecognitionDTO implements Serializable {
    /**
     * 素材库ID
     */
    
    private Integer materialId;
    /**
     * 素材库地址
     */
    
    private String materialPath;

    
    private String materialName;

    
    private Integer sortNum;
}
