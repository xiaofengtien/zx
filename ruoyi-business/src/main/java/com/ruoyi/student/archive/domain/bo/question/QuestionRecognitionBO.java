package com.ruoyi.student.archive.domain.bo.question;

import lombok.Data;

import java.io.Serializable;

/**
 * 题目辅助识图BO
 */
@Data
public class QuestionRecognitionBO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 素材库ID（兼容字段，映射到QuestionMedia.id）
     */
    private Integer materialId;
    
    /**
     * 素材库地址（媒体路径）
     */
    private String materialPath;
    
    /**
     * 素材名称（媒体文件名）
     */
    private String materialName;
    
    /**
     * 排序号（兼容字段，新表结构中没有此字段，使用默认值0）
     */
    private Integer sortNum;
}
