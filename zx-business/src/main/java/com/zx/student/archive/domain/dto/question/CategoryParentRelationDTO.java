package com.zx.student.archive.domain.dto.question;

import lombok.Data;

/**
 * 分类父子关系DTO
 * 用于批量查询分类的父子关系
 */
@Data
public class CategoryParentRelationDTO {
    /**
     * 分类ID
     */
    private Integer categoryId;
    
    /**
     * 父分类ID
     */
    private Integer fatherId;
}



