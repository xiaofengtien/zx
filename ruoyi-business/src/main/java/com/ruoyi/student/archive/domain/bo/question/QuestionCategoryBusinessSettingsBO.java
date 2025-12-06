package com.ruoyi.student.archive.domain.bo.question;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * 题库业务设置BO
 *
 * @author chuyi
 * @since 2025-08-16
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class QuestionCategoryBusinessSettingsBO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * ID（更新时必填）
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
     * 题库集合
     */
    
    private List<QuestionCategoryBusinessRefBO> questionCategories;

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
