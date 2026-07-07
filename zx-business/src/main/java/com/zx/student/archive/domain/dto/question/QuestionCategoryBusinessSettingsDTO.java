package com.zx.student.archive.domain.dto.question;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 题库业务设置DTO
 *
 * @author chuyi
 * @since 2025-08-16
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class QuestionCategoryBusinessSettingsDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    
    private Integer id;

    /**
     * 业务ID
     */
    
    private Integer businessId;

    /**
     * 业务类型 19-学习模块答题 4-图书书内码答题 25-点读答题
     */
    
    private Integer businessType;

    /**
     * 业务类型描述
     */
    
    private String businessTypeDesc;

    /**
     * 关联的题库分类列表
     */
    
    private List<QuestionCategoryBusinessRefDTO> questionCategories;

    /**
     * 出题方式
     */
    
    private Integer questionMethod;

    /**
     * 题目数量
     */
    
    private Integer questionNum;

    /**
     * 状态（1-启用 0-禁用）
     */
    
    private Integer status;
}
