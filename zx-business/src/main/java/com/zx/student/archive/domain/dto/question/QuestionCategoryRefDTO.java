package com.zx.student.archive.domain.dto.question;

import lombok.Data;

import java.io.Serializable;

@Data
public class QuestionCategoryRefDTO implements Serializable {

    
    private Integer id;

    
    private Integer businessId;

    
    private Integer businessType;

    
    private String businessTypeStr;

    
    private String businessName;

    
    private Integer appMaterialType;

    
    private String appMaterialTypeStr;

    
    private Integer booksId;

    
    private String bookName;

    
    private Integer rankId;

    
    private Integer contentsId;

    
    private Integer rankClassifyId;
} 